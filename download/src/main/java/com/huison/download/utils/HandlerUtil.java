package com.huison.download.utils;

import android.os.Handler;
import android.os.Looper;

/**
 * Created by huisonma on 2019/5/15.
 */
public class HandlerUtil {

    private static final Handler sUiHandler = new Handler(Looper.getMainLooper());

    public static void postOnUiThread(Runnable runnable) {
        sUiHandler.post(runnable);
    }

    public static void postOnUiThreadDelayed(Runnable runnable, long delayMillis) {
        sUiHandler.postDelayed(runnable, delayMillis);
    }

    public static void remove(Runnable runnable) {
        sUiHandler.removeCallbacks(runnable);
    }
}
