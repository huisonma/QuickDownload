package com.huison.kotlin.download

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

/**
 * Created by huisonma 2019/9/3.
 */
class DownloadDBHelper internal constructor(context: Context, dir: String) : SQLiteOpenHelper(context, "$dir/$DB_NAME", null, DB_VERSION) {

    companion object {

        private const val DB_VERSION = 1

        private const val DB_NAME = "file_download.db"

        internal const val TABLE_NAME = "download_entries"

        private const val CREATE_TABLE_SQL = "create table if not exists $TABLE_NAME (\n" +
                " url text ,\n" +
                " path text ,\n" +
                " startByte integer,\n" +
                " endByte integer,\n" +
                " number integer,\n" +
                " downloadedLength integer,\n" +
                " totalLength integer\n" +
                ")"
    }

    override fun onCreate(db: SQLiteDatabase?) {
        db?.execSQL(CREATE_TABLE_SQL)
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
    }
}