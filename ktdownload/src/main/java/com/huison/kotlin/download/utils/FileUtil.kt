package com.huison.kotlin.download.utils

import java.io.File

/**
 * Created by huisonma 2019/9/3.
 */
class FileUtil {

    companion object {

        @JvmStatic
        fun deleteFile(file: File): Boolean {
            if (!file.exists()) {
                return true
            }
            return file.isFile && file.delete()
        }

        @JvmStatic
        fun deleteFile(path: String): Boolean {
            return deleteFile(File(path))
        }

        @JvmStatic
        fun deleteDir(path: String): Boolean {
            val dir = File(path)
            if (!dir.exists()) {
                return true
            }
            var result = true
            if (dir.isDirectory) {
                for (child: File in dir.listFiles()) {
                    if (result) {
                        result = deleteDir(child.absolutePath)
                    } else {
                        deleteFile(child)
                    }
                }
            } else {
                result = deleteFile(path)
            }
            return result
        }

        @JvmStatic
        fun isFileExists(path: String): Boolean {
            return isFileExists(File(path))
        }

        @JvmStatic
        fun isFileExists(file: File): Boolean {
            return file.exists()
        }

        @JvmStatic
        fun createDir(dirPath: String): File {
            val dir = File(dirPath)
            if (!dir.exists()) {
                dir.mkdirs()
            }
            return dir
        }
    }
}