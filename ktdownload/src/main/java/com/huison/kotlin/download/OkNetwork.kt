package com.huison.kotlin.download

import com.huison.kotlin.download.utils.HandlerUtil
import okhttp3.*
import java.io.IOException

/**
 * Created by huisonma 2019/9/3.
 */
class OkNetwork {

    private var okHttpClient: OkHttpClient? = null

    @Synchronized
    private fun okHttpClient(): OkHttpClient? {
        if (okHttpClient == null) {
            okHttpClient = OkHttpClient.Builder().build()
        }
        return okHttpClient
    }

    companion object {

        private val sInstance = OkNetwork()

        fun instance(): OkNetwork {
            return sInstance
        }

        private fun newCall(url: String): Call {
            val request = Request.Builder().url(url).build()
            return instance().okHttpClient()!!.newCall(request)
        }

        internal fun request(url: String, callback: Callback) {
            val call = newCall(url)
            call.enqueue(object : Callback {
                override fun onResponse(call: Call, response: Response) {
                    HandlerUtil.postOnUiThread(Runnable { callback.onResponse(call, response) })
                }

                override fun onFailure(call: Call, e: IOException) {
                    HandlerUtil.postOnUiThread(Runnable { callback.onFailure(call, e) })
                }
            })
        }

        @Throws(IOException::class)
        internal fun execute(url: String, start: Long, end: Long): Response {
            val request = Request.Builder().url(url).addHeader("Range", "bytes=$start-$end").build()
            return instance().okHttpClient()!!.newCall(request).execute()
        }
    }
}