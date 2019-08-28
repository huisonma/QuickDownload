package com.huison.download.demo;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.huison.download.DownloadManager;
import com.huison.download.R;
import com.huison.download.entry.DownloadBean;
import com.huison.download.entry.DownloadProvider;
import com.huison.download.utils.FileUtil;
import com.huison.download.utils.HandlerUtil;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.List;

/**
 * Created by huisonma on 2019/5/15.
 */
public class DownloadActivity extends AppCompatActivity {

    private DownloadAdapter adapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_download_list);
        final RecyclerView rv = findViewById(R.id.recycler_view);
        rv.setLayoutManager(new LinearLayoutManager(this));

        findViewById(R.id.btn_delete_all).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FileUtil.deleteDir(getExternalFilesDir(null) + "/download/apk");
                DownloadManager.getInstance().deleteAllRecords();
                adapter.notifyDataSetChanged();
            }
        });

        new Thread(new Runnable() {
            @Override
            public void run() {
                final List<DownloadBean> downloadBeans = DownloadProvider.getDataFromAssets(DownloadActivity.this);
                HandlerUtil.postOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        adapter = new DownloadAdapter(DownloadActivity.this, downloadBeans);
                        rv.setAdapter(adapter);
                        adapter.notifyDataSetChanged();
                    }
                });
            }
        }).start();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(MessageEvent event) {
        String[] permissions = {Manifest.permission.WRITE_EXTERNAL_STORAGE};
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            requestPermissions(permissions, 111);
    }

    @Override
    protected void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        EventBus.getDefault().unregister(this);
        DownloadManager.getInstance().stopAll();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (Manifest.permission.WRITE_EXTERNAL_STORAGE.equals(permissions[0]) && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "读写SD卡权限受限将无法下载文件！", Toast.LENGTH_SHORT).show();
        }
    }
}
