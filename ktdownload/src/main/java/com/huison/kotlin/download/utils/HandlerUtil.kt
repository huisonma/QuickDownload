package com.huison.kotlin.download.utils

import android.os.Handler
import android.os.Looper

/**
 * Created by huisonma 2019/9/3.
 */
class HandlerUtil {

    companion object {

        private val sUiHandler: Handler = Handler(Looper.getMainLooper())

        @JvmStatic
        fun postOnUiThread(runnable: Runnable) {
            sUiHandler.post(runnable)
        }

        @JvmStatic
        fun postOnUiThreadDelay(runnable: Runnable, delayMillis: Long) {
            sUiHandler.postDelayed(runnable, delayMillis)
        }

        @JvmStatic
        fun remove(runnable: Runnable) {
            sUiHandler.removeCallbacks(runnable)
        }
    }
}