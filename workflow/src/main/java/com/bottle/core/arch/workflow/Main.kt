package com.bottle.core.arch.workflow

/**
 * 测试代码，可以当作使用demo
 */
fun main(args: Array<String>) {
    var flag = false
    if (flag) {
        workflow()
    } else {
        procedure()
    }
}

private fun procedure() {
    val procedure = Procedure()
    val taskA = Task("taskA") {
        sleep(100)
        it.progress(10, "")
        sleep(100)
        it.progress(50, "")
        sleep(100)
        it.progress(90, "")
        sleep(100)
        it.complete()
    }
    val taskB = Task("taskB") {
        sleep(100)
        it.progress(10, "")
        sleep(100)
        it.progress(50, "")
        sleep(100)
        it.progress(90, "")
        sleep(100)
        it.complete()
    }
    val taskC = Task("taskC") {
        if (!it.canceled) {
            sleep(100)
            it.progress(10, "")
            sleep(100)
            it.progress(50, "")
            sleep(100)
            it.complete()
        }
    }
    val taskD = Task("taskD") {
        sleep(100)
        it.progress(10, "")
        sleep(100)
        it.progress(50, "")
        sleep(100)
        it.complete()
    }
    val taskE = Task("taskE") {
        if (!it.canceled) {
            sleep(100)
            it.progress(10, "")
            sleep(100)
            it.progress(50, "")
            sleep(100)
            it.complete()
        }
    }
    val taskF = Task("taskF") {
        if (!it.canceled) {
            sleep(100)
            it.progress(10, "")
            sleep(100)
            it.progress(50, "")
            sleep(100)
            it.complete()
        }
    }
    procedure.apply {
        addTask(null, taskA)
        addTask(arrayOf(taskA), taskB)
        addTask(arrayOf(taskA), taskC)
        addTask(arrayOf(taskB, taskC), taskD)
        addTask(arrayOf(taskB), taskE)
        addTask(arrayOf(taskD), taskF)
    }.start(object : OnProcedureListener {

        override fun onCompleted() {
            println("Procedure is Completed")
        }

        override fun onFailed(task: Task) {
            println("Procedure is Failed")
        }

    })
}

private fun workflow() {
    val taskA = Task("taskA") {
        sleep(100)
        it.progress(10, "")
        sleep(100)
        it.progress(50, "")
        sleep(100)
        it.complete()
    }
    val taskB = Task("taskB") {
        sleep(100)
        it.progress(10, "")
        sleep(100)
        it.progress(50, "")
        sleep(100)
        it.complete()
    }
    val taskC = Task("taskC") {
        if (!it.canceled) {
            sleep(100)
            it.progress(10, "")
            sleep(100)
            it.progress(50, "")
            sleep(100)
            it.complete()
        }
    }
    val taskD = Task("taskD") {
        sleep(100)
        it.progress(10, "")
        sleep(100)
        it.progress(50, "")
        sleep(100)
        it.complete()
    }
    val taskE = Task("taskE") {
        if (!it.canceled) {
            sleep(100)
            it.progress(10, "")
            sleep(100)
            it.progress(50, "")
            sleep(100)
            it.complete()
        }
    }
    val workFlow = WorkFlow()
    workFlow.addProcedure(Procedure().addTask(taskA).addTask(taskB))
    workFlow.addProcedure(Procedure().addTask(taskC).addTask(taskD))
    workFlow.addProcedure(Procedure().addTask(taskE))
    workFlow.start(object : OnProcedureListener {

        override fun onCompleted() {
            println("WorkFlow is Completed")
        }

        override fun onFailed(task: Task) {
            println("WorkFlow is Completed")
        }

    })
}

private fun sleep(time: Long) {
    try {
        Thread.sleep(time)
    } catch (e: Exception) {
        e.printStackTrace()
    }

}
