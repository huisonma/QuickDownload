package com.huison.kotlin.download

import com.huison.kotlin.download.utils.FileUtil
import com.huison.kotlin.download.utils.HandlerUtil
import com.huison.kotlin.download.utils.LogUtil
import java.lang.Exception
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

/**
 * Created by huisonma 2019/9/3.
 */
class DownloadTask(private val downloadManager: DownloadManager,
                   private val url: String,
                   private val path: String,
                   private val totalLength: Long,
                   private val blockCount: Int,
                   private var callback: DownloadCallback?) : Runnable {

    private val successNumber: AtomicInteger = AtomicInteger()

    @Volatile
    private var status: Int = DownloadManager.STATUS_DOWNLOADING

    private var totalDownloadLength: Long = 0

    private val blockMap: ConcurrentHashMap<Int, DownloadBlock> = ConcurrentHashMap()

    override fun run() {
        prepare()
        execute()
        finished()
    }

    private fun prepare() {
        // 若未下载完的文件被删除了，需要把数据库的数据也清除掉，重新下载
        if (!FileUtil.isFileExists(path)) downloadManager.dbCenter.deleteComponents(url)
    }

    private fun isDownloadFinished(): Boolean {
        val isFileExists = FileUtil.isFileExists(path)
        val isDownloadFinished = downloadManager.dbCenter.queryComponents(url).isEmpty()
        if (isFileExists && isDownloadFinished) {
            callback?.onUpdate(url, 100, totalLength, totalLength)
            callback?.onSuccess(url, path)
            return true
        }
        return false
    }

    private fun execute() {
        if (isDownloadFinished()) return

        // 分块下载，每块需要下载的长度
        val unit: Long = totalLength / blockCount

        // 读取SQL获取已下载数据
        val components: List<DownloadComponent> = downloadManager.dbCenter.queryComponents(url)
        if (!components.isEmpty()) {
            for (component in components) {
                totalDownloadLength += component.downloadedLength
            }
            val progress = totalDownloadLength * 100.0 / totalLength
            callback?.onUpdate(url, progress.toInt(), totalDownloadLength, totalLength)
        }

        var component: DownloadComponent?
        for (i in 0 until blockCount) {
            if (status == DownloadManager.STATUS_STOP) break

            val start: Long = i * unit
            // 最后一个块为剩下的长度
            val end: Long = if (i == blockCount - 1) totalLength else start + unit - 1

            component = getComponent(components, i)
            if (component == null) {
                component = DownloadComponent(url, path, start, end, i, 0, totalLength)
            } else {
                component.start = start + component.downloadedLength
            }
            // 若本地单块数据已下载完成，则不需下载
            if (unit == component.downloadedLength) {
                successNumber.incrementAndGet()
                continue
            }
            val block = DownloadBlock(downloadManager, component, object : DownloadCallback {
                override fun onUpdate(url: String, progress: Int, downloadedLength: Long, totalLength: Long) {
                    HandlerUtil.postOnUiThread(Runnable {
                        totalDownloadLength += downloadedLength
                        val progress = totalDownloadLength * 100.0 / totalLength
                        callback?.onUpdate(url, progress.toInt(), totalDownloadLength, totalLength)
                    })
                }

                override fun onPause(url: String, downloadedLength: Long, totalLength: Long) {
                    HandlerUtil.postOnUiThread(Runnable {
                        callback?.onPause(url, totalDownloadLength, totalLength)
                    })
                }

                override fun onSuccess(url: String, filePath: String) {
                    successNumber.incrementAndGet()

                    blockMap.remove(i)

                    if (successNumber.get() == blockCount) {
                        val count = downloadManager.dbCenter.deleteComponents(url)
                        LogUtil.d("onSuccess: delete count = $count")

                        HandlerUtil.postOnUiThread(Runnable {
                            callback?.onSuccess(url, path)
                        })
                    }
                }

                override fun onFailed(url: String?, e: Exception) {
                    blockMap.remove(i)
                    downloadManager.dbCenter.deleteComponents(url)

                    HandlerUtil.postOnUiThread(Runnable {
                        callback?.onFailed(url, e)
                    })
                }
            })
            downloadManager.dispatcher.enqueue(block)
            blockMap[i] = block
        }
    }

    private fun getComponent(components: List<DownloadComponent>?, number: Int): DownloadComponent? {
        for (component in components!!) {
            if (component.number == number) {
                return component
            }
        }
        return null
    }

    private fun finished() {
        downloadManager.dispatcher.finished(this)
    }

    internal fun stop() {
        status = DownloadManager.STATUS_STOP

        for (block in blockMap.values) {
            block.stop()
            downloadManager.dispatcher.remove(block)
        }
        downloadManager.dispatcher.remove(this)
        blockMap.clear()
    }

    internal fun isDownloading(): Boolean {
        return !blockMap.isEmpty()
    }

    internal fun getProgress(): Int {
        return (totalDownloadLength * 100.0 / totalLength).toInt()
    }
}