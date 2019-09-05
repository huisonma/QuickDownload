package com.huison.kotlin.download

/**
 * Created by huisonma 2019/9/3.
 */
class DownloadComponent internal constructor(
        /**
         * 下载链接
         */
        var url: String,
        /**
         * 下载文件路径
         */
        var path: String,
        /**
         * 分块下载起始字节点
         */
        var start: Long,
        /**
         * 分块下载结束字节点
         */
        var end: Long,
        /**
         * 分块编号
         */
        var number: Int,
        /**
         * 分块已下载字节数
         */
        var downloadedLength: Long,
        /**
         * 文件总字节长度
         */
        var totalLength: Long
)