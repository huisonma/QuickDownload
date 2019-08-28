package com.huison.download;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.huison.download.utils.CloseUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by huisonma on 2019/5/15.
 */
class DownloadDBCenter {

    private DownloadDBHelper dbHelper;

    DownloadDBCenter(Context context, String dir) {
        File file = new File(dir);
        if (!file.exists()) {
            file.mkdirs();
        }
        dbHelper = new DownloadDBHelper(context, dir);
    }

    private static ContentValues toContentValues(DownloadComponent component) {
        ContentValues values = new ContentValues();
        values.put("url", component.getUrl());
        values.put("path", component.getPath());
        values.put("startByte", component.getStart());
        values.put("endByte", component.getEnd());
        values.put("number", component.getNumber());
        values.put("downloadedLength", component.getDownloadedLength());
        values.put("totalLength", component.getTotalLength());
        return values;
    }

    boolean saveComponent(DownloadComponent component) {
        if (component == null) {
            return false;
        }
        if (hasDownloaded(component)) {
            return updateComponent(component) != -1;
        } else {
            return insertComponent(component) != -1;
        }
    }

    private boolean hasDownloaded(DownloadComponent component) {
        if (component == null) {
            return false;
        }
        SQLiteDatabase database = dbHelper.getReadableDatabase();
        boolean result = false;
        Cursor cursor = null;
        try {
            String[] whereArgs = {component.getUrl(), String.valueOf(component.getNumber())};
            cursor = database.query(
                    DownloadDBHelper.TABLE_NAME,
                    new String[]{"url", "number"},
                    "url = ? and number = ?",
                    whereArgs,
                    null, null, null);
            result = cursor != null && cursor.getCount() > 0;
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            CloseUtil.close(cursor);
        }
        return result;
    }

    private long insertComponent(DownloadComponent component) {
        long rowId = -1;
        if (component == null) {
            return rowId;
        }
        try {
            rowId = dbHelper.getWritableDatabase().insert(
                    DownloadDBHelper.TABLE_NAME,
                    null,
                    toContentValues(component));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return rowId;
    }

    private int updateComponent(DownloadComponent component) {
        if (component == null) {
            return -1;
        }
        try {
            SQLiteDatabase database = dbHelper.getWritableDatabase();
            String[] whereArgs = {component.getUrl(), String.valueOf(component.getNumber())};
            return database.update(
                    DownloadDBHelper.TABLE_NAME,
                    toContentValues(component),
                    "url = ? and number = ?",
                    whereArgs);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return -1;
    }

    List<DownloadComponent> queryComponents(String url) {
        SQLiteDatabase database = dbHelper.getReadableDatabase();
        Cursor cursor = null;
        List<DownloadComponent> components = new ArrayList<>();
        try {
            cursor = database.query(
                    DownloadDBHelper.TABLE_NAME,
                    null,
                    "url = ?",
                    new String[]{url},
                    null, null, "number");
            while (cursor.moveToNext()) {
                String path = cursor.getString(cursor.getColumnIndex("path"));
                long start = cursor.getLong(cursor.getColumnIndex("startByte"));
                long end = cursor.getLong(cursor.getColumnIndex("endByte"));
                int threadId = cursor.getInt(cursor.getColumnIndex("number"));
                long progress = cursor.getLong(cursor.getColumnIndex("downloadedLength"));
                long contentLength = cursor.getLong(cursor.getColumnIndex("totalLength"));
                DownloadComponent component = new DownloadComponent(url, path, start, end, threadId, progress, contentLength);
                components.add(component);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            CloseUtil.close(cursor);
        }
        return components;
    }

    int deleteComponents(String url) {
        try {
            return dbHelper.getWritableDatabase().delete(
                    DownloadDBHelper.TABLE_NAME,
                    "url=?",
                    new String[]{url});
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    int deleteAllComponents() {
        try {
            return dbHelper.getWritableDatabase().delete(DownloadDBHelper.TABLE_NAME, null, null);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }
}
