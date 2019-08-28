package com.huison.download.demo;

import android.app.Application;

import com.huison.download.DownloadManager;
import com.huison.download.utils.LogUtil;

/**
 * Created by huisonma on 2019/5/15.
 */
public class App extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        LogUtil.enable();
        DownloadManager.init(this);
    }
}
