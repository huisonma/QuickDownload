package com.huison.download;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import okhttp3.internal.Util;

/**
 * Created by huisonma on 2019/5/15.
 * 下载分发器，参考自{@link okhttp3.Dispatcher}
 */
public final class DownloadDispatcher {

    /**
     * 允许同时最大下载任务（每个任务由多个块组成）
     */
    private int maxTask = 5;

    /**
     * 允许同时最多下载块(默认最多允许5个任务同时下载，每个任务拆分成5个块下载，因此允许最多同时下载块为25)
     */
    private int maxBlocks = 25;

    private Deque<DownloadBlock> readyBlocks = new ArrayDeque<>();
    private Deque<DownloadBlock> runningBlocks = new ArrayDeque<>();

    private Deque<DownloadTask> readyTasks = new ArrayDeque<>();
    private Deque<DownloadTask> runningTasks = new ArrayDeque<>();

    private ExecutorService executorService;

    DownloadDispatcher() {
    }

    public DownloadDispatcher(ExecutorService executorService) {
        this.executorService = executorService;
    }

    public DownloadDispatcher(ExecutorService executorService, int maxTask, int maxBlocks) {
        this(executorService);
        setMaxTasks(maxTask);
        setMaxBlocks(maxBlocks);
    }

    private synchronized ExecutorService executorService() {
        if (executorService == null) {
            executorService = new ThreadPoolExecutor(0, Integer.MAX_VALUE, 60, TimeUnit.SECONDS,
                    new SynchronousQueue<Runnable>(), Util.threadFactory("Download Dispatcher", false));
        }
        return executorService;
    }

    synchronized void setMaxTasks(int maxTasks) {
        if (maxTasks < 1) {
            throw new IllegalArgumentException("maxTasks < 1 : " + maxTasks);
        }
        if (this.maxTask != maxTasks) {
            this.maxTask = maxTasks;
            promoteTasks();
        }
    }

    synchronized void enqueue(DownloadTask downloadTask) {
        if (runningTasks.size() < maxTask) {
            runningTasks.add(downloadTask);
            executorService().execute(downloadTask);
        } else {
            readyTasks.add(downloadTask);
        }
    }

    private void promoteTasks() {
        if (runningTasks.size() > maxTask) {
            return;
        }
        if (readyTasks.isEmpty()) {
            return;
        }
        for (Iterator<DownloadTask> i = readyTasks.iterator(); i.hasNext(); ) {
            DownloadTask downloadTask = i.next();
            i.remove();

            runningTasks.add(downloadTask);
            executorService().execute(downloadTask);

            if (runningTasks.size() >= maxTask) {
                return;
            }
        }
    }

    void finished(DownloadTask downloadTask) {
        synchronized (this) {
            if (!runningTasks.remove(downloadTask))
                throw new AssertionError("DownloadTask wasn't in-flight!");

            promoteTasks();
        }
    }

    synchronized boolean remove(DownloadTask downloadTask) {
        if (downloadTask != null) {
            return readyTasks.remove(downloadTask);
        }
        return false;
    }

    synchronized void setMaxBlocks(int maxBlocks) {
        if (maxBlocks < 1) {
            throw new IllegalArgumentException("maxBlocks < 1 : " + maxBlocks);
        }
        if (this.maxBlocks != maxBlocks) {
            this.maxBlocks = maxBlocks;
            promoteBlocks();
        }
    }

    synchronized void enqueue(DownloadBlock downloadBlock) {
        if (runningBlocks.size() < maxBlocks) {
            runningBlocks.add(downloadBlock);
            executorService().execute(downloadBlock);
        } else {
            readyBlocks.add(downloadBlock);
        }
    }

    private void promoteBlocks() {
        if (runningBlocks.size() >= maxBlocks) {
            return;
        }
        if (readyBlocks.isEmpty()) {
            return;
        }
        for (Iterator<DownloadBlock> i = readyBlocks.iterator(); i.hasNext(); ) {
            DownloadBlock downloadBlock = i.next();
            i.remove();

            runningBlocks.add(downloadBlock);
            executorService().execute(downloadBlock);

            if (runningBlocks.size() >= maxBlocks) {
                return;
            }
        }
    }

    void finished(DownloadBlock downloadBlock) {
        synchronized (this) {
            if (!runningBlocks.remove(downloadBlock))
                throw new AssertionError("DownloadBlock wasn't in-flight!");

            promoteBlocks();
        }
    }

    synchronized boolean remove(DownloadBlock downloadBlock) {
        if (downloadBlock != null) {
            return readyBlocks.remove(downloadBlock);
        }
        return false;
    }
}
