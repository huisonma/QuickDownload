package com.huison.download;

import com.huison.download.utils.HandlerUtil;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Created by huisonma on 2019/5/15.
 */
class OkNetwork {

    private static final OkNetwork sInstance = new OkNetwork();

    private OkHttpClient okHttpClient;

    private OkNetwork() {
    }

    private static OkNetwork instance() {
        return sInstance;
    }

    private synchronized OkHttpClient okHttpClient() {
        if (okHttpClient == null) {
            okHttpClient = new OkHttpClient.Builder().build();
        }
        return okHttpClient;
    }

    static Call newCall(String url) {
        Request request = new Request.Builder().url(url).build();
        return instance().okHttpClient().newCall(request);
    }

    static void request(String url, final Callback callback) {
        Call call = newCall(url);
        call.enqueue(new Callback() {
            @Override
            public void onFailure(final Call call, final IOException e) {
                HandlerUtil.postOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (callback != null) {
                            callback.onFailure(call, e);
                        }
                    }
                });
            }

            @Override
            public void onResponse(final Call call, final Response response) throws IOException {
                HandlerUtil.postOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (callback != null) {
                            try {
                                callback.onResponse(call, response);
                            } catch (IOException e) {
                                e.printStackTrace();
                                callback.onFailure(call, e);
                            }
                        }
                    }
                });
            }
        });
    }

    static Response execute(String url, long start, long end) throws IOException {
        Request request = new Request.Builder()
                .url(url)
                .addHeader("Range", "bytes=" + start + "-" + end)
                .build();
        return instance().okHttpClient().newCall(request).execute();
    }
}
