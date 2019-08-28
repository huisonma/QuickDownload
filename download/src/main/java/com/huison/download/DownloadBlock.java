package com.huison.download;

import com.huison.download.utils.CloseUtil;
import com.huison.download.utils.LogUtil;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.RandomAccessFile;

import okhttp3.ResponseBody;

/**
 * Created by huisonma on 2019/5/15.
 * 分块下载每一块的下载内容
 */
class DownloadBlock implements Runnable {

    private DownloadManager downloadManager;

    private DownloadComponent component;

    private DownloadCallback callback;

    private volatile int status = DownloadManager.STATUS_DOWNLOADING;

    DownloadBlock(DownloadManager downloadManager, DownloadComponent component, DownloadCallback callback) {
        this.downloadManager = downloadManager;
        this.component = component;
        this.callback = callback;
    }

    @Override
    public void run() {
        // 块任务执行前先判断已被停止了
        if (status == DownloadManager.STATUS_STOP) {
            if (callback != null)
                callback.onPause(component.getUrl(), component.getDownloadedLength(), component.getTotalLength());
            return;
        }
        String url = component.getUrl();
        String path = component.getPath();
        long start = component.getStart();
        long end = component.getEnd();
        long contentLength = component.getTotalLength();
        long downloadedLength = component.getDownloadedLength();

        BufferedInputStream bis = null;
        RandomAccessFile randomAccessFile = null;
        try {
            // 分块请求服务端数据
            ResponseBody body = OkNetwork.execute(url, start, end).body();
            if (body == null) {
                if (callback != null)
                    callback.onFailed(url,
                            new IOException("body is null ! url = " + url + ", start = " + start + ", end = " + end));
                return;
            }
            bis = new BufferedInputStream(body.byteStream());

            randomAccessFile = new RandomAccessFile(path, "rwd");
            // 文件写入位置移到单块起始位置
            randomAccessFile.seek(start);

            int length;
            // 提高性能，减少磁盘I/O，可以适当加大每次读取缓存，此处设置为16K
            byte[] bytes = new byte[16 * 1024];
            while ((length = bis.read(bytes)) != -1) {
                if (status == DownloadManager.STATUS_STOP) {
                    if (callback != null)
                        callback.onPause(url, length, contentLength);
                    break;
                }

                // 写入文件
                randomAccessFile.write(bytes, 0, length);

                // 更新已下载长度
                component.setDownloadedLength(downloadedLength += length);

                // ！！！ 此处callback返回为单次写入的长度length，而非downloadedLength
                // 一个文件由多个块组成，每个块返回已写入文件的长度，在DownloadTask进行组装累计就是文件已下载的进度
                if (callback != null)
                    callback.onUpdate(url, 0, length, contentLength);
            }

            // 保存下载数据
            boolean success = downloadManager.dbCenter().saveComponent(component);
            if (success) {
                LogUtil.d(url + ", number = " + component.getNumber() + ", save success, downloadedLength = " + downloadedLength);
            } else {
                LogUtil.e(url + ", number = " + component.getNumber() + ", save failed !");
            }

            // 未被停止则单块数据下载写入完成
            if (status != DownloadManager.STATUS_STOP) {
                if (callback != null) {
                    callback.onSuccess(url, path);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            if (callback != null)
                callback.onFailed(url, e);
        } finally {
            CloseUtil.close(bis);
            CloseUtil.close(randomAccessFile);

            // 查询是否还有等待下载任务，激活下载
            downloadManager.dispatcher().finished(this);
        }
    }

    void stop() {
        status = DownloadManager.STATUS_STOP;
    }
}
