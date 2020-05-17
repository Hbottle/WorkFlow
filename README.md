# WorkFlow
kotlin 实现工作流，简化复杂工作流程构建，结果监听回调。可以设置某个子任务是必须/非必须的，这样在任务失败时就会终止
工作流，或者忽视失败，继续流程。可以让子任务报告进度，也可以在某些时候主动终止流程。只需要按照下面步骤，即可完成实现
工作流：
1. 创建子任务Task；
2. 将Task添加到工作流，并定义它们的前后依赖关系；
3. 开始执行工作流，并监听工作流的进度回调。


有时候完成一个任务可能需要执行A、B、C、D、E、F...若干子个任务，可能是这样子的：

![workflow1](./img/workflow1.jpeg)

可能是这样子的：

![workflow2](./img/workflow2.jpeg)

也有可能是这样子的：

![workflow3](./img/workflow3.jpeg)

WorkFlow，轻松构建工作流程，不管是图式流程：
```kotlin
    val procedure = Procedure()
    val taskA = Task("taskA") {
        sleep(100)
        it.progress(10, "")
        sleep(100)
        it.progress(20, "")
        sleep(100)
        it.progress(30, "")
        sleep(100)
        it.progress(50, "")
        sleep(100)
        it.progress(70, "")
        sleep(100)
        it.progress(90, "")
        sleep(100)
        it.complete()
    }
    val taskB = Task("taskB") {
        sleep(100)
        it.progress(10, "")
        sleep(100)
        it.progress(30, "")
        sleep(100)
        it.progress(50, "")
        sleep(100)
        it.progress(70, "")
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
            //it.failed()
            //it.progress(50, "")
        }
    }
    val taskD = Task("taskD") {
        sleep(100)
        it.progress(10, "")
        sleep(100)
        it.progress(50, "")
        sleep(100)
        it.complete()
        //it.cancel()
        //procedure.cancel(-1, "i cancel it")
        //it.progress(50, "")
        //it.failed()
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
        }

        override fun onFailed(task: Task) {
        }

    })
```

还是队列式流程：
```kotlin
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
            //it.failed()
            //it.progress(50, "")
        }
    }
    val taskD = Task("taskD") {
        sleep(100)
        it.progress(10, "")
        sleep(100)
        it.progress(50, "")
        sleep(100)
        it.complete()
        // it.cancel()
        //it.progress(50, "")
        //it.failed()
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
        }

        override fun onFailed(task: Task) {
        }

    })
```