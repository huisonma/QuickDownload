package com.huison.kotlin.download

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import java.io.File

/**
 * Created by huisonma 2019/9/3.
 */
class DownloadDBCenter internal constructor(context: Context, dir: String) {

    companion object {

        const val TABLE_NAME = DownloadDBHelper.TABLE_NAME

        fun toContentValues(component: DownloadComponent): ContentValues {
            val values = ContentValues()
            values.put("url", component.url)
            values.put("path", component.path)
            values.put("startByte", component.start)
            values.put("endByte", component.end)
            values.put("number", component.number)
            values.put("downloadedLength", component.downloadedLength)
            values.put("totalLength", component.totalLength)
            return values
        }
    }

    private val database: SQLiteDatabase

    init {
        val file = File(dir)
        if (!file.exists()) {
            file.mkdirs()
        }
        database = DownloadDBHelper(context, dir).writableDatabase
    }

    internal fun saveComponent(component: DownloadComponent): Boolean {
        if (hadDownload(component)) return updateComponent(component) != -1

        return insertComponent(component) != -1L
    }

    private fun hadDownload(component: DownloadComponent?): Boolean {
        if (component == null) return false

        val cursor = database.query(TABLE_NAME,
                arrayOf("url", "number"),
                "url = ? and number = ?",
                arrayOf(component.url, component.number.toString()),
                null, null, null)

        val result: Boolean = cursor?.count!! > 0
        cursor.close()
        return result
    }

    private fun insertComponent(component: DownloadComponent?): Long {
        if (component == null) return -1
        return database.insert(TABLE_NAME, null, toContentValues(component))
    }

    private fun updateComponent(component: DownloadComponent?): Int {
        if (component == null) return -1
        return database.update(TABLE_NAME, toContentValues(component), "url = ? and number = ?",
                arrayOf(component.url, component.number.toString()))
    }


    internal fun queryComponents(url: String): List<DownloadComponent> {
        val components = ArrayList<DownloadComponent>()
        val cursor = database.query(TABLE_NAME, null, "url = ?", arrayOf(url), null, null, "number")
        while (cursor.moveToNext()) {
            val path = cursor.getString(cursor.getColumnIndex("path"))
            val start = cursor.getLong(cursor.getColumnIndex("startByte"))
            val end = cursor.getLong(cursor.getColumnIndex("endByte"))
            val number = cursor.getInt(cursor.getColumnIndex("number"))
            val downloadedLength = cursor.getLong(cursor.getColumnIndex("downloadedLength"))
            val totalLength = cursor.getLong(cursor.getColumnIndex("totalLength"))
            val component = DownloadComponent(url, path, start, end, number, downloadedLength, totalLength)
            components.add(component)
        }
        cursor.close()
        return components
    }

    internal fun deleteComponents(url: String?): Int {
        if (url == null) return 0
        return database.delete(TABLE_NAME, "url = ?", arrayOf(url))
    }

    internal fun deleteAllComponents(): Int {
        return database.delete(TABLE_NAME, null, null)
    }
}