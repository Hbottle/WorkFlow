package com.bottle.core.arch.workflow

/**
 * @Date: 2020/3/23
 * @Author: hugo
 * @Description: 流程中的任务，在doWork中完成任务，然后调用complete()
 * or failed()来继续下一个任务，过程中可以调用progress()报告进度。在
 */
class Task(
    val name: String, // 任务的名称，作为key保存在Procedure的tasks中
    val abortOnFailed: Boolean = true, // 设置为true时任务失败则流程失败，默认true
    inline val onCancel: ((task: Task) -> Unit) = {}, // 需要监听取消时实现
    inline val doWork: ((task: Task) -> Unit) // 任务的代码，在子线程中执行
) : Runnable {

    internal val prevTasks: MutableList<Task> = ArrayList() // 前置任务(需要等待它们执行完才执行自己)
    internal val nextTasks: MutableList<Task> = ArrayList() // 后置任务(执行完之后要执行的任务，具体要不要执行，看还依赖哪些任务)
    internal lateinit var onTaskListener: OnTaskListener
    internal var mLogger: ((log: String) -> Unit) = { println(it) }

    @Volatile
    var taskState: Int
        internal set
    @Volatile
    var canceled = false // 在doWork()中应该检测这个标志位，一旦监测到设置为true，则停止任务
        internal set

    init {
        taskState = TaskState.NEW
    }

    override fun run() {
        if (canceled || isRunningOrCompleted("run()1")) {
            return
        }
        synchronized(onTaskListener) {
            if (canceled || isRunningOrCompleted("run()2")) {
                return
            }
            if (prevTasks.isNotEmpty()) {
                var ready = true
                for (pre in prevTasks) {
                    if (pre.taskState < TaskState.FAILED) {
                        ready = false
                        break
                    }
                }
                if (!ready) {
                    mLogger.invoke("$name is not ready")
                    return
                }
                mLogger.invoke("$name is ready")
            }
            if (canceled || isRunningOrCompleted("run()3")) {
                return
            }
            taskState = TaskState.RUNNING
        }

        mLogger.invoke("ready to run task ${name}, work thread is ${Thread.currentThread().name}")
        doWork(this)
    }

    fun progress(progress: Int, desc: String?) {
        if (isCanceledOrCompleted("progress()1")) {
            return
        }
        synchronized(onTaskListener) {
            if (isCanceledOrCompleted("progress()2")) {
                return
            }
            onTaskListener.onProgress(this, progress, desc)
        }
    }

    /**
     * 任务完成时必须调用，否则后面的任务无法执行
     */
    fun complete() {
        if (isCanceledOrCompleted("complete()1")) {
            return
        }
        synchronized(onTaskListener) {
            if (isCanceledOrCompleted("complete()2")) {
                return
            }
            taskState = TaskState.SUCCESS
            onTaskListener.onTaskComplete(this)
        }
    }

    /**
     * 任务失败时必须调用，否则后面的任务无法执行
     */
    fun failed() {
        if (isCanceledOrCompleted("failed()1")) {
            return
        }
        synchronized(onTaskListener) {
            if (isCanceledOrCompleted("failed()2")) {
                return
            }
            taskState = TaskState.FAILED
            onTaskListener.onTaskFailed(this)
        }
    }

    /**
     * 取消任务，只是设置一个标志位，每个任务在进入任务前都需要检查这个标志位，看是否已取消
     */
    internal fun cancel() {
        if (isCanceledOrCompleted("cancel()1")) {
            return
        }
        synchronized(onTaskListener) {
            if (isCanceledOrCompleted("cancel()2")) {
                return
            }
            canceled = true
            taskState = TaskState.CANCELED
            onCancel(this)
            onTaskListener.onTaskFailed(this)
        }
    }

    internal fun addPrevTask(pre: Task) {
        prevTasks.add(pre)
    }

    internal fun addNextTask(next: Task) {
        nextTasks.add(next)
    }

    private fun isRunningOrCompleted(msg: String): Boolean {
        val isRunningOrCompleted = taskState > TaskState.WAITING
        if (isRunningOrCompleted) {
            mLogger.invoke("$name is running or already or completed($msg): canceled=$canceled, taskState=$taskState")
        }
        return isRunningOrCompleted
    }

    private fun isCanceledOrCompleted(msg: String = ""): Boolean {
        val isCancelOrCompleted = canceled || taskState > TaskState.RUNNING
        if (isCancelOrCompleted) {
            mLogger.invoke("$name is canceled or already or completed($msg): canceled=$canceled, taskState=$taskState")
        }
        return isCancelOrCompleted
    }
}

interface OnTaskListener {
    /**
     * 某个任务完成
     *
     * @param task 当前完成的任务
     */
    fun onTaskComplete(task: Task)

    /**
     * 某个任务失败
     *
     * @param task 当前失败的任务
     */
    fun onTaskFailed(task: Task)

    /**
     * 如果任务需要上报进度，可调用此方法
     * @param task 报告进度的Task
     * @param progress 0-100
     * @param desc     描述，如果任务是多个步骤，没有具体进度，也可以使用这个字段
     */
    fun onProgress(task: Task, progress: Int, desc: String?)
}
