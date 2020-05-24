package com.bottle.core.arch.workflow

import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * @author hugo
 * @date 3/22/20
 * @desc 流程，如何使用：
 * 1.创建任务；
 * 2.设计流程；
 * 3.按照流程连接任务；
 * 4.开始流程
 *
 * 如果图结构的流程不够简单，可以使用WorkFlow，构建链式的流程
 */
class Procedure : OnTaskListener {

    private val mTaskMap = HashMap<String, Task>()
    private lateinit var mOnProcedureListener: OnProcedureListener
    @Volatile
    private var failed = false // 这是一个标志位，表示流程是否因为某个必须的任务失败而失败了
    @Volatile
    var abortOnFailed = false // 和Task的一样，表示WorkFlow是否会因为Procedure失败而终止
    @Volatile
    private var canceled = false // 流程被取消
    private val mExecutor: ExecutorService
    var mLogger: ((log: String) -> Unit)

    @JvmOverloads
    constructor(
        executor: ExecutorService = Executors.newCachedThreadPool(),
        abortOnFailed: Boolean = true,
        logger: ((log: String) -> Unit) = { println(it) }
    ) {
        this.mExecutor = executor
        this.abortOnFailed = abortOnFailed
        this.mLogger = logger
    }

    fun addTask(task: Task): Procedure {
        return addTask(null, task)
    }

    fun addTask(preTasks: Array<Task>?, task: Task): Procedure {
        if (preTasks != null && preTasks.isNotEmpty()) {
            for (pre in preTasks) {
                // 建立任务的先后关系
                pre.addNextTask(task)
                task.addPrevTask(pre)
            }
        }
        task.onTaskListener = this
        mTaskMap[task.name] = task
        task.taskState = TaskState.WAITING
        task.mLogger = mLogger
        return this
    }

    fun addTaskByPreTaskName(preTasks: Array<String>?, task: Task): Procedure {
        if (preTasks != null && preTasks.isNotEmpty()) {
            var temp: Task?
            for (pre in preTasks) {
                // 建立任务的先后关系
                temp = mTaskMap[pre]
                temp?.addNextTask(task)
                temp?.let { task.addPrevTask(it) }
            }
        }
        task.onTaskListener = this
        mTaskMap[task.name] = task
        task.taskState = TaskState.WAITING
        task.mLogger = mLogger
        return this
    }

    fun start(listener: OnProcedureListener, inputParam: MutableMap<String, Any>? = null) {
        mOnProcedureListener = listener
        var temp: Task
        for ((key, value) in mTaskMap) {
            temp = value
            // 没有前置任务的都执行，它们就是第一批要执行的任务
            if (temp.prevTasks.isEmpty()) {
                // 如果有输入参数则
                inputParam?.let { temp.inputParam.putAll(inputParam) }
                mExecutor.execute(temp)
            }
        }
    }

    @Synchronized
    fun cancel(code: Int, reason: String) {
        cancel()
        // 通知任务已被取消
        mOnProcedureListener.onCancel(code, reason)
    }

    fun cancel() {
        if (canceled || failed) {
            mLogger.invoke("cancel() failed, because canceled or failed")
            return
        }
        canceled = true
        for ((_, value) in mTaskMap) {
            if (value.taskState < TaskState.FAILED) {
                mExecutor.execute{ value.cancel() }
            }
        }
    }

    /**
     * 重置所有任务状态，重置后可以再次执行，调用前请确保已经调用成功，失败或者取消，并且所有任务已停止
     * (这得让Task自己保证，收到取消执行后马上停止，请重写{@link Task#onCancel()})总之，此方法不靠谱
     */
    @Synchronized
    fun reset() {
        failed = false
        canceled = false
        for ((_, value) in mTaskMap) {
            value.canceled = false
            value.taskState = TaskState.WAITING
        }
    }

    override fun onTaskComplete(task: Task, output: Any?) {
        mLogger.invoke("onTaskComplete(): ${task.name}")
        if (failed || canceled) {
            mLogger.invoke("onTaskComplete() failed, already canceled or failed")
            return
        }
        if (task.nextTasks.isEmpty()) {
            var allDone = true
            for ((_, value) in mTaskMap) {
                if (value.taskState < TaskState.SUCCESS) {
                    allDone = false
                    break
                }
            }
            if (allDone) {
                mOnProcedureListener.onBlockCompleted(task)
                mLogger.invoke("Procedure onCompleted(): ${task.name}")
                mOnProcedureListener.onCompleted()
            } else {
                mOnProcedureListener.onBlockCompleted(task)
            }
        } else {
            mOnProcedureListener.onBlockCompleted(task)
            for (next in task.nextTasks) {
                // 多线程环境下可能会重入某个任务，这里设置一个关卡
                if (next.taskState < TaskState.RUNNING) {
                    output?.let { next.inputParam[task.name] = output }
                    mExecutor.execute(next)
                }
            }
        }
    }

    @Synchronized
    override fun onTaskFailed(task: Task) {
        mLogger.invoke("onTaskFailed()")
        if (failed || canceled) {
            mLogger.invoke("onTaskFailed() failed, already canceled or failed")
            return
        }
        // 1.如果是一个必须的任务，则取消所有正在执行，或者还没执行的任务
        if (task.abortOnFailed) {
            for ((key, value) in mTaskMap) {
                if (value.taskState < TaskState.FAILED) {
                    // 必须在线程中，否则可能会多次调用onFailed()
                    mExecutor.execute { value.cancel() }
                }
            }
            failed = true
            mOnProcedureListener.onBlockFailed(task)
            mOnProcedureListener.onFailed(task)
            return
        }
        // 2.如果是一个非必须的任务，则先判断是否已经完成所有任务，是则完成任务，否则执行这个任务下面的任务
        if (task.nextTasks.isEmpty()) {
            var allDone = true
            for ((key, value) in mTaskMap) {
                if (value.taskState < TaskState.FAILED) {
                    allDone = false
                    break
                }
            }
            mOnProcedureListener.onBlockFailed(task)
            if (allDone) {
                mOnProcedureListener.onCompleted()
            }
        } else {
            mOnProcedureListener.onBlockFailed(task)
            for (next in task.nextTasks) {
                mExecutor.execute(next)
            }
        }
    }

    override fun onProgress(task: Task, progress: Int, desc: String?) {
        if (failed || canceled) {
            mLogger.invoke("onProgress() failed, already canceled or failed")
            return
        }
        if (desc.isNullOrBlank()) {
            mLogger.invoke("${task.name} progress=$progress")
        } else {
            mLogger.invoke("${task.name} $desc progress=$progress")
        }
        mOnProcedureListener.onProgress(task, progress, desc)
    }

}
