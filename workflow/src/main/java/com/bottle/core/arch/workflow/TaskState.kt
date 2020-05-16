package com.bottle.core.arch.workflow

/**
 * @Date: 2020/3/24
 * @Author: hugo
 * @Description: 任务的的状态
 */
internal annotation class TaskState {
    companion object {
        const val NEW = 0      // 任务新建，还没加入流程
        const val WAITING = 1  // 任务已加入流程，但是前置任务还没有执行完，正在等待执行
        const val RUNNING = 2  // 任务正在运行
        const val FAILED = 3   // 任务失败
        const val SUCCESS = 4  // 任务执行完并且成功了
        const val CANCELED = 5 // 任务已取消
    }
}
