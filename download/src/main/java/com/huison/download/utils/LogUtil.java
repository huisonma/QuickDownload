package com.huison.download.utils;

import android.util.Log;

/**
 * Created by huisonma on 2019/5/15.
 */
public class LogUtil {

    private static final String TAG = "LogUtil";

    private static boolean sEnable = false;

    public static void enable() {
        sEnable = true;
    }

    public static void d(String msg) {
        d(TAG, msg);
    }

    public static void d(String tag, String msg) {
        if (sEnable) {
            Log.d(tag, msg);
        }
    }

    public static void d(String tag, String msg, Throwable e) {
        if (sEnable) {
            Log.d(tag, msg, e);
        }
    }

    public static void e(String msg) {
        e(TAG, msg);
    }

    public static void e(String tag, String msg) {
        if (sEnable) {
            Log.e(tag, msg);
        }
    }

    public static void e(String tag, String msg, Throwable e) {
        if (sEnable) {
            Log.e(tag, msg, e);
        }
    }
}
