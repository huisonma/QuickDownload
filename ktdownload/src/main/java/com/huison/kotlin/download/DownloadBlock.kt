package com.huison.kotlin.download

import com.huison.kotlin.download.utils.LogUtil
import okhttp3.ResponseBody
import java.io.BufferedInputStream
import java.io.IOException
import java.io.RandomAccessFile

/**
 * Created by huisonma 2019/9/3.
 */
class DownloadBlock internal constructor(private var downloadManager: DownloadManager,
                                         private var component: DownloadComponent,
                                         private var callback: DownloadCallback?) : Runnable {

    @Volatile
    private var status: Int = DownloadManager.STATUS_DOWNLOADING

    override fun run() {
        // 块任务执行前先判断已被停止了
        if (status == DownloadManager.STATUS_STOP) {
            callback?.onPause(component.url, component.downloadedLength, component.totalLength)
            return
        }
        val url = component.url
        val path = component.path
        val start = component.start
        val end = component.end
        val totalLength = component.totalLength

        var bis: BufferedInputStream? = null
        var randomAccessFile: RandomAccessFile? = null
        try {
            // 分块请求服务端数据
            val requestBody: ResponseBody? = OkNetwork.execute(url, start, end).body
            if (requestBody == null) {
                callback?.onFailed(component.url, IOException("body is null ! url = $url, start = $start, end = $end"))
                return
            }
            randomAccessFile = RandomAccessFile(path, "rwd")
            // 文件写入位置移到单块起始位置
            randomAccessFile.seek(start)

            bis = BufferedInputStream(requestBody.byteStream())
            // 提高性能，减少磁盘I/O，可以适当加大每次读取缓存，此处设置为16K
            val bytes = ByteArray(16 * 1024)
            var read: Int = bis.read(bytes)
            var len: Long = 0
            bis.use { it ->
                while (read != -1) {
                    if (status == DownloadManager.STATUS_STOP) {
                        callback?.onPause(url, len, totalLength)
                        break
                    }
                    // 写入文件
                    randomAccessFile.write(bytes, 0, read)

                    len = read.toLong()
                    // 更新已下载长度
                    component.downloadedLength += len
                    // ！！！ 此处callback返回为单次写入的长度length，而非downloadedLength
                    // 一个文件由多个块组成，每个块返回已写入文件的长度，在DownloadTask进行组装累计就是文件已下载的进度
                    callback?.onUpdate(url, 0, len, totalLength)

                    read = it.read(bytes)
                }
            }

            // 保存下载数据
            val success = downloadManager.dbCenter.saveComponent(component)
            if (success) {
                LogUtil.d("$url, number = ${component.number}, save success, downloadedLength = ${component.downloadedLength}")
            } else {
                LogUtil.d("$url, number = ${component.number}, save failed, downloadedLength = ${component.downloadedLength}")
            }

            // 未被停止则单块数据下载写入完成
            if (status != DownloadManager.STATUS_STOP) {
                callback?.onSuccess(url, path)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            callback?.onFailed(url, e)
        } finally {
            bis?.close()
            randomAccessFile?.close()

            // 查询是否还有等待下载任务，激活下载
            downloadManager.dispatcher.finished(this)
        }
    }

    internal fun stop() {
        status = DownloadManager.STATUS_STOP
    }
}