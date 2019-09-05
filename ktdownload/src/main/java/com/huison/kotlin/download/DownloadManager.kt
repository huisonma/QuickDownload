package com.huison.kotlin.download

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.text.TextUtils
import com.huison.kotlin.download.utils.FileUtil
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Response
import java.io.File
import java.io.IOException
import java.lang.Exception
import java.lang.NullPointerException

/**
 * Created by huisonma 2019/9/3.
 */
class DownloadManager {

    companion object {

        const val STATUS_DOWNLOADING = 1
        const val STATUS_STOP = 2

        const val DOWNLOAD_BLOCKS_COUNT = 5

        @SuppressLint("StaticFieldLeak")
        var context: Context? = null

        @JvmStatic
        fun init(context: Context) {
            this.context = context
        }

        @SuppressLint("StaticFieldLeak")
        var instanceOrNull: DownloadManager? = null

        @JvmStatic
        val instance: DownloadManager
            get() {
                if (instanceOrNull == null) {
                    synchronized(this) {
                        if (instanceOrNull == null) {
                            instanceOrNull = DownloadManager()
                        }
                    }
                }
                return instanceOrNull!!
            }

        fun checkContext() {
            if (context !is Context) throw NullPointerException("sContext is null ! you must call init() first")
        }
    }

    private val downloadTaskMap: HashMap<String, DownloadTask> = HashMap()

    private val urlLengthMap: HashMap<String, Long> = HashMap()

    private var dbCenterOrNull: DownloadDBCenter? = null

    var dispatcher = DownloadDispatcher()

    @get:Synchronized
    val dbCenter: DownloadDBCenter
        get() {
            checkContext()
            if (dbCenterOrNull == null) {
                val path = "${context!!.filesDir}/download/db"
                dbCenterOrNull = DownloadDBCenter(context!!, path)
            }
            return dbCenterOrNull!!
        }

    private fun isPermissionGranted(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return true

        checkContext()
        return context!!.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
    }

    fun download(url: String, path: String, callback: DownloadCallback) {
        download(url, path, DOWNLOAD_BLOCKS_COUNT, callback)
    }

    fun download(url: String?, path: String?, downloadBlockCount: Int, callback: DownloadCallback?) {
        if (TextUtils.isEmpty(url) || TextUtils.isEmpty(path)) {
            callback?.onFailed(url, IllegalArgumentException("url is null or path is null!"))
            return
        }
        if (!isPermissionGranted()) {
            callback?.onFailed(url, SecurityException("Permission Denial: requires Manifest.permission.WRITE_EXTERNAL_STORAGE!"))
            return
        }
        val file = File(path)
        if (!file.parentFile.exists()) file.parentFile.mkdirs()

        var contentLength = urlLengthMap[url]?.toLong()
        if (contentLength is Long) {
            downloadInternal(url!!, path!!, contentLength, downloadBlockCount, callback)
        } else {
            OkNetwork.request(url!!, object : Callback {
                override fun onResponse(call: Call, response: Response) {
                    val body = response.body
                    if (body == null) {
                        callback?.onFailed(url, IOException("response body is null!"))
                        return
                    }
                    contentLength = body.contentLength()
                    if (contentLength!! <= -1) {
                        callback?.onFailed(url, IOException("contentLength <= -1!"))
                        return
                    }
                    downloadInternal(url, path!!, contentLength!!, downloadBlockCount, callback)
                }

                override fun onFailure(call: Call, e: IOException) {
                    callback?.onFailed(url, e)
                }
            })
        }
    }

    private fun downloadInternal(url: String, path: String, totalLength: Long, blockCount: Int, callback: DownloadCallback?) {
        val task = DownloadTask(instance, url, path, totalLength, blockCount, object : DownloadCallback {
            override fun onUpdate(url: String, progress: Int, downloadedLength: Long, totalLength: Long) {
                callback?.onUpdate(url, (downloadedLength * 100 / totalLength).toInt(), downloadedLength, totalLength)
            }

            override fun onPause(url: String, downloadedLength: Long, totalLength: Long) {
                callback?.onPause(url, downloadedLength, totalLength)
            }

            override fun onSuccess(url: String, filePath: String) {
                finished(url, filePath, true)

                callback?.onSuccess(url, filePath)
            }

            override fun onFailed(url: String?, e: Exception) {
                finished(url!!, path, false)

                callback?.onFailed(url, e)
            }
        })
        dispatcher.enqueue(task)
        downloadTaskMap[url] = task
    }

    private fun finished(url: String, path: String, success: Boolean) {
        urlLengthMap.remove(url)
        downloadTaskMap.remove(url)
        if (!success) {
            // 下载失败，删除下载文件
            FileUtil.deleteFile(path)
        }
    }

    fun stop(url: String) {
        downloadTaskMap[url]?.stop()
    }

    fun stopAll() {
        for (task in downloadTaskMap.values) {
            task.stop()
        }
        downloadTaskMap.clear()
    }

    fun isDownloading(url: String): Boolean {
        return downloadTaskMap[url]?.isDownloading() ?: false
    }

    fun deleteAllRecords() {
        dbCenter.deleteAllComponents()
    }

    fun queryProgress(url: String, path: String): Int {
        val task = downloadTaskMap[url]
        if (task != null && task.isDownloading()) {
            return task.getProgress()
        }
        val components = dbCenter.queryComponents(url)
        var totalDownloadedLength = 0L
        var totalLength = 0L
        if (components.isEmpty()) {
            if (FileUtil.isFileExists(path)) return 100
        } else {
            for (component in components) {
                totalDownloadedLength += component.downloadedLength
                // 每条记录的totalLength都是一样的
                totalLength = component.totalLength
            }
        }
        return if (totalLength == 0L) 0 else (totalDownloadedLength * 100 / totalLength).toInt()
    }
}