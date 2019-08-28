package com.huison.download;

import com.huison.download.utils.FileUtil;
import com.huison.download.utils.HandlerUtil;
import com.huison.download.utils.LogUtil;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by huisonma on 2019/5/15.
 */
class DownloadTask implements Runnable {

    private DownloadManager downloadManager;

    private String url;
    private String path;
    private long totalLength;
    /**
     * 单个文件分块数
     */
    private int blockCount;

    private DownloadCallback callback;

    private volatile int status = DownloadManager.STATUS_DOWNLOADING;
    private long totalDownloadedLength;
    /**
     * 用于记录单个文件下载线程计数，每个线程下载成功+1，当计数等于threadSize时表示文件下载成功
     */
    private AtomicInteger successNumber = new AtomicInteger();
    private Map<Integer, DownloadBlock> blockMap = new ConcurrentHashMap<>();

    DownloadTask(DownloadManager downloadManager, String url, String path, long totalLength, int blockCount,
                 DownloadCallback callback) {
        this.downloadManager = downloadManager;
        this.url = url;
        this.path = path;
        this.totalLength = totalLength;
        this.blockCount = blockCount;
        this.callback = callback;
    }

    @Override
    public void run() {
        prepare();
        execute();
        finished();
    }

    private void prepare() {
        // 若未下载完的文件被删除了，需要把数据库的数据也清除掉，重新下载
        if (!FileUtil.isFileExists(path)) {
            downloadManager.dbCenter().deleteComponents(url);
        }
    }

    private void finished() {
        downloadManager.dispatcher().finished(this);
    }

    private boolean isDownloadFinished() {
        boolean isFileExists = FileUtil.isFileExists(path);
        boolean isDownloadFinished = downloadManager.dbCenter().queryComponents(url).isEmpty();
        if (isFileExists && isDownloadFinished) {
            if (callback != null) {
                callback.onUpdate(url, 100, totalLength, totalLength);
                callback.onSuccess(url, path);
            }
            return true;
        }
        return false;
    }

    private void execute() {
        if (isDownloadFinished()) {
            return;
        }
        // 分块下载，每块需要下载的长度
        long unit = totalLength / blockCount;
        // 读取SQL获取已下载数据
        List<DownloadComponent> components = getComponentCache(url);
        if (!components.isEmpty()) {
            for (DownloadComponent component : components) {
                totalDownloadedLength += component.getDownloadedLength();
            }
            if (callback != null) {
                int progress = (int) (totalDownloadedLength * 100.0 / totalLength);
                callback.onUpdate(url, progress, totalDownloadedLength, totalLength);
            }
        }
        DownloadComponent component;
        for (int i = 0; i < blockCount; i++) {
            if (status == DownloadManager.STATUS_STOP) {
                break;
            }

            long start = i * unit;
            // 最后一个块为剩下的长度
            long end = (i == blockCount - 1) ? totalLength : start + unit - 1;

            // 初始化单块下载数据
            component = getComponent(components, i);
            if (component == null) {
                component = new DownloadComponent(url, path, start, end, i, 0, totalLength);
            } else {
                component.setStart(start + component.getDownloadedLength());
            }
            // 若本地单块数据已下载完成，则不需下载
            if (unit == component.getDownloadedLength()) {
                successNumber.incrementAndGet();
                continue;
            }
            final int key = i;
            final DownloadBlock downloadBlock = new DownloadBlock(downloadManager, component, new DownloadCallback() {
                @Override
                public void onUpdate(final String url, int progress, final long downloadedLength, final long totalLength) {
                    HandlerUtil.postOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            totalDownloadedLength += downloadedLength;
                            int progress = (int) (totalDownloadedLength * 100.0 / totalLength);
                            if (callback != null) {
                                callback.onUpdate(url, progress, totalDownloadedLength, totalLength);
                            }
                        }
                    });
                }

                @Override
                public void onPause(final String url, long downloadedLength, final long totalLength) {
                    HandlerUtil.postOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (callback != null) {
                                callback.onPause(url, totalDownloadedLength, totalLength);
                            }
                        }
                    });
                }

                @Override
                public void onSuccess(final String url, final String filePath) {
                    successNumber.incrementAndGet();

                    blockMap.remove(key);

                    if (successNumber.get() == blockCount) {
                        int count = downloadManager.dbCenter().deleteComponents(url);
                        LogUtil.d("onSuccess = delete count = " + count);

                        HandlerUtil.postOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (callback != null) {
                                    callback.onSuccess(url, filePath);
                                }
                            }
                        });
                    }
                }

                @Override
                public void onFailed(final String url, final Exception e) {
                    downloadManager.dbCenter().deleteComponents(url);

                    blockMap.remove(key);

                    HandlerUtil.postOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (callback != null) {
                                callback.onFailed(url, e);
                            }
                        }
                    });
                }
            });
            downloadManager.dispatcher().enqueue(downloadBlock);
            blockMap.put(i, downloadBlock);
        }
    }

    private List<DownloadComponent> getComponentCache(String url) {
        return downloadManager.dbCenter().queryComponents(url);
    }

    private DownloadComponent getComponent(List<DownloadComponent> components, int threadId) {
        if (components != null) {
            for (DownloadComponent component : components) {
                if (component.getNumber() == threadId) {
                    return component;
                }
            }
        }
        return null;
    }

    void stop() {
        status = DownloadManager.STATUS_STOP;
        for (DownloadBlock block : blockMap.values()) {
            if (block != null) {
                block.stop();
            }
            downloadManager.dispatcher().remove(block);
        }
        downloadManager.dispatcher().remove(this);
        blockMap.clear();
    }

    boolean isDownloading() {
        return !blockMap.isEmpty();
    }

    int getProgress() {
        return (int) (totalDownloadedLength * 100.0 / totalLength);
    }
}
