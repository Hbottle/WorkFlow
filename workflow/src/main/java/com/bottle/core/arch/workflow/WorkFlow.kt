package com.bottle.core.arch.workflow

import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue


/**
 * @author hugo
 * @date   3/31/20
 * @desc  WorkFlow采用队列作为内部数据结构，Procedure按照先进先出的顺序执行，所以在构建
 * WorkFlow时，添加Procedure的顺序就是它的执行顺序，每个Procedure执行完以后会出队，继续下一个Procedure。
 * WorkFlow忽略Procedure的内部实现细节，对内部Task的顺序没有依赖关系，所以可以独立构建Procedure，
 * 但从简单的角度看，层级越少越好，比如Procedure的Task A B C没有先后关系，异步执行：
 * WorkFlow workFlow = new WorkFlow();
 * workFlow.addProcedure(new Procedure().addTask(taskA).addTask(taskB)); // taskA和taskB是异步执行
 * workFlow.addProcedure(new Procedure().addTask(taskC).addTask(new Task[]{taskC}, taskD)); // taskC先执行，然后taskD执行，同步关系
 * workFlow.addProcedure(new Procedure().addTask(taskE));
 * workFlow.start(new Procedure.OnProcedureListener() {})
 *
 * (Procedure可以看作是WorkFlow的一个任务)
 */
class WorkFlow : OnProcedureListener {
    private val mWorkNodes: Queue<Procedure> = ConcurrentLinkedQueue()
    private lateinit var mOnProcedureListener: OnProcedureListener
    private var mCurrentProcedure: Procedure? = null
    private var state: Int
    private var mLogger: ((log: String) -> Unit)

    @JvmOverloads
    constructor(workNodes: List<Procedure> = arrayListOf(),
                logger: ((log: String) -> Unit) = { println(it) }) {
        mLogger = logger
        if (workNodes.isNotEmpty()) {
            for (procedure in workNodes) {
                procedure.mLogger = mLogger
            }
            mWorkNodes.addAll(workNodes)
        }
        state = TaskState.WAITING
    }

    fun addProcedure(procedure: Procedure): WorkFlow {
        procedure.mLogger = mLogger
        mWorkNodes.add(procedure)
        return this
    }

    @Synchronized
    fun start(onProcedureListener: OnProcedureListener) {
        if (mWorkNodes.isEmpty()) {
            return
        }
        mOnProcedureListener = onProcedureListener
        mCurrentProcedure = mWorkNodes.poll()
        state = TaskState.RUNNING
        mCurrentProcedure?.start(this)
    }

    @Synchronized
    fun cancel(code: Int, msg: String) {
        // 在线程中执行，加入争夺锁，避免同时回调任务失败和任务取消
        Thread(Runnable {
            if (state < TaskState.FAILED) {
                synchronized(this) {
                    if (state < TaskState.FAILED) {
                        state = TaskState.CANCELED
                        mWorkNodes.clear()
                        if (mCurrentProcedure != null) {
                            mCurrentProcedure?.cancel()
                        }
                        mOnProcedureListener.onCancel(code, msg)
                    }
                }
            }
        }).start()
    }

    @Synchronized
    override fun onBlockCompleted(task: Task) {
        mOnProcedureListener.onBlockCompleted(task)
    }

    @Synchronized
    override fun onBlockFailed(task: Task) {
        mOnProcedureListener.onBlockFailed(task)
    }

    @Synchronized
    override fun onCompleted() {
        if (mWorkNodes.isEmpty()) {
            mOnProcedureListener.onCompleted()
            state = TaskState.SUCCESS
            mLogger.invoke("WorkFlow onCompleted")
        } else {
            mCurrentProcedure = mWorkNodes.poll()
            mCurrentProcedure?.start(this)
        }
    }

    @Synchronized
    override fun onFailed(task: Task) {
        if (mCurrentProcedure == null) {
            return
        }
        if (mCurrentProcedure!!.abortOnFailed) {
            mOnProcedureListener.onFailed(task)
            state = TaskState.FAILED
        } else {
            if (mWorkNodes.isEmpty()) {
                mOnProcedureListener.onCompleted()
                state = TaskState.SUCCESS
            } else {
                mCurrentProcedure = mWorkNodes.poll()
                mCurrentProcedure?.start(this)
            }
        }
    }

    @Synchronized
    override fun onCancel(code: Int, reason: String) {
        if (mCurrentProcedure == null) {
            return
        }
        if (mCurrentProcedure!!.abortOnFailed) {
            mOnProcedureListener.onCancel(code, reason)
            state = TaskState.CANCELED
        } else {
            if (mWorkNodes.isEmpty()) {
                mOnProcedureListener.onCompleted()
                state = TaskState.SUCCESS
            } else {
                mCurrentProcedure = mWorkNodes.poll()
                mCurrentProcedure?.start(this)
            }
        }
    }

    @Synchronized
    override fun onProgress(task: Task, progress: Int, desc: String?) {
        mOnProcedureListener.onProgress(task, progress, desc)
    }

}
