package com.huison.kotlin.download

import java.lang.Exception

/**
 * Created by huisonma 2019/9/3.
 */
interface DownloadCallback {

    fun onUpdate(url: String, progress: Int, downloadedLength: Long, totalLength: Long)

    fun onPause(url: String, downloadedLength: Long, totalLength: Long)

    fun onSuccess(url: String, filePath: String)

    fun onFailed(url: String?, e: Exception)
}