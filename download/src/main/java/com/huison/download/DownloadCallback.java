package com.huison.download;

/**
 * Created by huisonma on 2019/5/15.
 */
public interface DownloadCallback {

    void onUpdate(String url, int progress, long downloadedLength, long totalLength);

    void onPause(String url, long downloadedLength, long totalLength);

    void onSuccess(String url, String filePath);

    void onFailed(String url, Exception e);
}
