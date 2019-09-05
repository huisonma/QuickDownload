package com.huison.kotlin.download

import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.SynchronousQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

/**
 * Created by huisonma 2019/9/3.
 */
class DownloadDispatcher {

    /**
     * 允许同时最大下载任务（每个任务由多个块组成）
     */
    @get:Synchronized
    var maxTasks = 5
        set(maxTasks) {
            require(maxTasks >= 1) { "maxTasks < 1 : $maxTasks" }
            synchronized(this) {
                field = maxTasks
            }
            promoteTasks()
        }

    /**
     * 允许同时最多下载块(默认最多允许5个任务同时下载，每个任务拆分成5个块下载，因此允许最多同时下载块为25)
     */
    @get:Synchronized
    var maxBlocks = 25
        set(maxBlocks) {
            require(maxBlocks >= 1) { "maxBlocks < 1 : $maxBlocks" }
            synchronized(this) {
                field = maxBlocks
            }
            promoteBlocks()
        }

    private var readyBlocks: Deque<DownloadBlock> = ArrayDeque()
    private var runningBlocks: Deque<DownloadBlock> = ArrayDeque()

    private var readyTasks: Deque<DownloadTask> = ArrayDeque()
    private var runningTasks: Deque<DownloadTask> = ArrayDeque()

    private var executorServiceOrNull: ExecutorService? = null

    @get:Synchronized
    val executorService: ExecutorService
        get() {
            if (executorServiceOrNull == null) {
                executorServiceOrNull = ThreadPoolExecutor(0, Int.MAX_VALUE, 60, TimeUnit.SECONDS, SynchronousQueue<Runnable>())
            }
            return executorServiceOrNull!!
        }

    constructor()

    constructor(executorService: ExecutorService) {
        this.executorServiceOrNull = executorService
    }

    internal fun enqueue(task: DownloadTask) {
        synchronized(this) {
            if (runningTasks.size < maxTasks) {
                runningTasks.add(task)
                executorService.execute(task)
            } else {
                readyTasks.add(task)
            }
        }
    }

    private fun promoteTasks() {
        if (runningTasks.size > maxTasks) return

        if (readyTasks.isEmpty()) return

        val it = readyTasks.iterator()
        while (it.hasNext()) {
            val task: DownloadTask = it.next()
            it.remove()

            runningTasks.add(task)
            executorService.execute(task)

            if (runningTasks.size >= maxTasks) break
        }
    }

    internal fun finished(task: DownloadTask) {
        synchronized(this) {
            if (!runningTasks.remove(task)) throw AssertionError("DownloadTask wan't in-flight!")
        }
        promoteTasks()
    }

    @Synchronized
    fun remove(task: DownloadTask?): Boolean {
        return readyTasks.remove(task)
    }

    internal fun enqueue(block: DownloadBlock) {
        synchronized(this) {
            if (runningBlocks.size < maxBlocks) {
                runningBlocks.add(block)
                executorService.execute(block)
            } else {
                readyBlocks.add(block)
            }
        }
    }

    private fun promoteBlocks() {
        if (runningBlocks.size >= maxBlocks) return

        if (readyBlocks.isEmpty()) return

        val it = readyBlocks.iterator()
        while (it.hasNext()) {
            val block = it.next()
            it.remove()

            runningBlocks.add(block)
            executorService.execute(block)

            if (runningBlocks.size >= maxBlocks) break
        }
    }

    internal fun finished(block: DownloadBlock) {
        synchronized(this) {
            if (!runningBlocks.remove(block)) throw AssertionError("DownloadBlock wan't in-flight!")
        }
        promoteBlocks()
    }

    @Synchronized
    fun remove(block: DownloadBlock?): Boolean {
        return readyBlocks.remove(block)
    }
}