package com.bottle.core.arch.workflow

/**
 * 流程的回调
 */
interface OnProcedureListener {
    /**
     * 完成某个任务
     * @param task 当前完成的任务
     */
    fun onBlockCompleted(task: Task){}

    /**
     * 某个任务失败了
     * @param task 当前失败的任务
     */
    fun onBlockFailed(task: Task){}

    /**
     * 整个流程的所有任务已完成
     */
    fun onCompleted()

    /**
     * 流程失败，某个必须完成的任务失败了
     * @param task 那个必须完成却失败了的任务
     */
    fun onFailed(task: Task)

    /**
     * 主动取消
     */
    fun onCancel(code: Int, reason: String){}

    /**
     * 可能需要进度
     * @param task Task
     * @param progress 0-100
     * @param desc 如果进度不是0-100，而是某些步骤的分段，也可以用这个字段，也可以配合progress字段
     */
    fun onProgress(task: Task, progress: Int, desc: String?){}
}
