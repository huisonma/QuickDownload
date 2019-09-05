package com.huison.download;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.text.TextUtils;

import com.huison.download.utils.FileUtil;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * Created by huisonma on 2019/5/15.
 */
public class DownloadManager {

    static final int STATUS_DOWNLOADING = 1;
    static final int STATUS_STOP = 2;

    private static DownloadManager sInstance;

    private static Context sContext;
    private final Map<String, DownloadTask> downloadTaskMap;
    private final Map<String, Long> urlLengthMap;
    private DownloadDispatcher dispatcher;
    private DownloadDBCenter dbCenter;
    /**
     * the count for split the file into blocks to download
     */
    private static final int DOWNLOAD_BLOCKS_COUNT = 5;

    private DownloadManager() {
        dispatcher = new DownloadDispatcher();
        downloadTaskMap = new HashMap<>();
        urlLengthMap = new HashMap<>();
    }

    /**
     * call this method at Application.onCreate()
     */
    public static void init(Context context) {
        sContext = context;
    }

    private static void checkContext() {
        if (sContext == null) {
            throw new IllegalStateException("sContext is null ! you must call init() first");
        }
    }

    public static DownloadManager getInstance() {
        if (sInstance == null) {
            synchronized (DownloadManager.class) {
                if (sInstance == null) {
                    sInstance = new DownloadManager();
                }
            }
        }
        return sInstance;
    }

    synchronized DownloadDBCenter dbCenter() {
        if (dbCenter == null) {
            checkContext();
            String path = sContext.getFilesDir() + "/download/db";
            dbCenter = new DownloadDBCenter(sContext, path);
        }
        return dbCenter;
    }

    DownloadDispatcher dispatcher() {
        return dispatcher;
    }

    public void setDispatcher(DownloadDispatcher dispatcher) {
        this.dispatcher = dispatcher;
    }

    private boolean isPermissionGranted() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return true;
        }
        checkContext();
        return sContext.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED;
    }

    public void download(final String url, final String path, final DownloadCallback callback) {
        download(url, path, DOWNLOAD_BLOCKS_COUNT, callback);
    }

    /**
     * @param url                download url
     * @param path               download file path
     * @param downloadBlockCount split the file into blocks to download
     * @param callback           callback
     */
    public void download(final String url, final String path, final int downloadBlockCount,
                         final DownloadCallback callback) {
        if (TextUtils.isEmpty(url) || TextUtils.isEmpty(path)) {
            if (callback != null) {
                callback.onFailed(url, new IllegalArgumentException("url is null or path is null !"));
            }
            return;
        }
        if (!isPermissionGranted()) {
            if (callback != null) {
                callback.onFailed(url, new SecurityException(
                        "Permission Denial: requires Manifest.permission.WRITE_EXTERNAL_STORAGE !"));
            }
            return;
        }
        File file = new File(path);
        if (!file.getParentFile().exists()) {
            file.getParentFile().mkdirs();
        }

        Long contentLength = urlLengthMap.get(url);
        if (contentLength != null) {
            downloadInternal(url, path, contentLength, downloadBlockCount, callback);
        } else {
            // 请求url，获取下载文件总长度
            OkNetwork.request(url, new Callback() {
                @Override
                public void onFailure(Call call, final IOException e) {
                    if (callback != null) {
                        callback.onFailed(url, e);
                    }
                }

                @Override
                public void onResponse(final Call call, Response response) {
                    ResponseBody body = response.body();
                    if (body == null) {
                        if (callback != null) {
                            callback.onFailed(url, new IOException("response body is null !"));
                        }
                        return;
                    }
                    long contentLength = body.contentLength();
                    if (contentLength <= -1) {
                        if (callback != null) {
                            callback.onFailed(url, new IOException("contentLength <= -1 !"));
                        }
                        return;
                    }

                    downloadInternal(url, path, contentLength, downloadBlockCount, callback);
                }
            });
        }
    }

    private void downloadInternal(final String url, final String path, long contentLength, int downloadBlockCount,
                                  final DownloadCallback callback) {
        DownloadTask downloadTask = new DownloadTask(getInstance(), url, path, contentLength, downloadBlockCount,
                new DownloadCallback() {
                    @Override
                    public void onUpdate(final String url, int progress, final long downloadedLength,
                                         final long totalLength) {
                        if (callback != null) {
                            progress = (int) (downloadedLength * 100 / totalLength);
                            callback.onUpdate(url, progress, downloadedLength, totalLength);
                        }
                    }

                    @Override
                    public void onPause(final String url, final long downloadedLength, final long totalLength) {
                        if (callback != null) {
                            callback.onPause(url, downloadedLength, totalLength);
                        }
                    }

                    @Override
                    public void onSuccess(final String url, final String filePath) {
                        finished(url, path, true);
                        if (callback != null) {
                            callback.onSuccess(url, filePath);
                        }
                    }

                    @Override
                    public void onFailed(final String url, final Exception e) {
                        finished(url, path, false);
                        if (callback != null) {
                            callback.onFailed(url, e);
                        }
                    }
                });
        dispatcher.enqueue(downloadTask);
        downloadTaskMap.put(url, downloadTask);
    }

    private void finished(String url, String path, boolean success) {
        urlLengthMap.remove(url);
        downloadTaskMap.remove(url);
        if (!success) {
            // 下载失败，删除下载文件
            FileUtil.deleteFile(path);
        }
    }

    public boolean isDownloading(String url) {
        if (TextUtils.isEmpty(url)) return false;

        DownloadTask downloadTask = downloadTaskMap.get(url);
        return downloadTask != null && downloadTask.isDownloading();
    }

    /**
     * It need query from DB and will cost time when it's not downloading.
     */
    public int queryProgress(String url, String path) {
        if (TextUtils.isEmpty(url)) return 0;

        DownloadTask downloadTask = downloadTaskMap.get(url);
        if (downloadTask != null && downloadTask.isDownloading()) {
            return downloadTask.getProgress();
        }
        List<DownloadComponent> components = dbCenter().queryComponents(url);
        long totalDownloadedLength = 0;
        long totalLength = 0;
        if (components.isEmpty()) {
            if (FileUtil.isFileExists(path)) return 100;
        } else {
            for (DownloadComponent component : components) {
                totalDownloadedLength += component.getDownloadedLength();
                // 每条记录的totalLength都是一样的
                totalLength = component.getTotalLength();
            }
        }
        return totalLength == 0 ? 0 : (int) (totalDownloadedLength * 100 / totalLength);
    }

    public void stop(String url) {
        if (TextUtils.isEmpty(url)) return;

        DownloadTask downloadTask = downloadTaskMap.remove(url);
        if (downloadTask != null) {
            downloadTask.stop();
        }
    }

    public void stopAll() {
        if (downloadTaskMap.isEmpty()) return;

        for (DownloadTask downloadTask : downloadTaskMap.values()) {
            if (downloadTask != null) {
                downloadTask.stop();
            }
        }
        downloadTaskMap.clear();
    }

    /**
     * 清空db所有记录
     */
    @Deprecated
    public void deleteAllRecords() {
        dbCenter().deleteAllComponents();
    }
}
