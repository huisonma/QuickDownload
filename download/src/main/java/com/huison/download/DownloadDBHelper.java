package com.huison.download;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Created by huisonma on 2019/5/15.
 */
class DownloadDBHelper extends SQLiteOpenHelper {

    private static final int DB_VERSION = 1;

    private static final String DB_NAME = "file_download.db";

    static final String TABLE_NAME = "download_entries";

    private static final String CREATE_TABLE_SQL = "create table if not exists " + TABLE_NAME + "(\n"
            + " url text ,\n"
            + " path text ,\n"
            + " startByte integer,\n"
            + " endByte integer,\n"
            + " number integer,\n"
            + " downloadedLength integer,\n"
            + " totalLength integer\n"
            + ")";

    DownloadDBHelper(Context context, String dir) {
        super(context, dir + "/" + DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE_SQL);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }
}
