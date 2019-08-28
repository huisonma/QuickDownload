package com.huison.download.demo;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v4.content.FileProvider;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.huison.download.BuildConfig;
import com.huison.download.DownloadCallback;
import com.huison.download.DownloadManager;
import com.huison.download.R;
import com.huison.download.entry.DownloadBean;
import com.huison.download.utils.FileUtil;
import com.huison.download.utils.LogUtil;

import org.greenrobot.eventbus.EventBus;

import java.io.File;

/**
 * Created by huisonma on 2019/5/15.
 */
public class DownloadViewHolder extends RecyclerView.ViewHolder implements DownloadCallback {

    private Context context;
    private ImageView iconView;
    private TextView nameView;
    private TextView sizeView;
    private TextView downloadCountView;
    private ProgressBar progressBar;
    private Button downloadButton;

    private DownloadBean downloadBean;

    public DownloadViewHolder(@NonNull View itemView) {
        super(itemView);
        context = itemView.getContext();
        iconView = itemView.findViewById(R.id.icon);
        nameView = itemView.findViewById(R.id.name);
        sizeView = itemView.findViewById(R.id.size);
        downloadCountView = itemView.findViewById(R.id.download_count);
        progressBar = itemView.findViewById(R.id.progress_bar);
        downloadButton = itemView.findViewById(R.id.download_button);

        downloadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (downloadBean.isDownloadSuccess) {
                    onInstallClick();
                } else {
                    onDownloadClick();
                }
            }
        });
    }

    void bindData(DownloadBean downloadBean) {
        this.downloadBean = downloadBean;

        Glide.with(context).load(downloadBean.icon).into(iconView);
        nameView.setText(downloadBean.name);
        sizeView.setText(downloadBean.size);
        downloadCountView.setText(downloadBean.downloadTimes);
        int progress = DownloadManager.getInstance().queryProgress(downloadBean.url, createPath(downloadBean.url));
        progressBar.setProgress(progress);
        if (progress == 100) {
            this.downloadBean.isDownloadSuccess = true;
            downloadButton.setText("安装");
            downloadButton.setTextColor(context.getResources().getColor(R.color.colorPrimary));
        } else {
            this.downloadBean.isDownloadSuccess = false;
            downloadButton.setText("下载");
            downloadButton.setTextColor(Color.WHITE);
        }
    }

    private void onInstallClick() {
        File file = new File(createPath(downloadBean.url));
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            Uri contentUri = FileProvider.getUriForFile(context, BuildConfig.APPLICATION_ID + ".provider", file);
            intent.setDataAndType(contentUri, "application/vnd.android.package-archive");
        } else {
            intent.setDataAndType(Uri.fromFile(file), "application/vnd.android.package-archive");
        }
        context.startActivity(intent);
    }

    private void onDownloadClick() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                && context.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            EventBus.getDefault().post(new MessageEvent());
        } else {
            String url = downloadBean.url;
            if (DownloadManager.getInstance().isDownloading(url)) {
                DownloadManager.getInstance().stop(url);
            } else {
                if (TextUtils.isEmpty(url)) {
                    return;
                }
                DownloadManager.getInstance().download(url, createPath(url), this);
            }
        }
    }

    private String createPath(String url) {
        final String fileName = url.substring(url.lastIndexOf("/") + 1);
        return FileUtil.createDir(context.getExternalFilesDir(null) + "/download/apk") + "/" + fileName;
    }

    @Override
    public void onUpdate(final String url, int progress, long downloadedLength, long totalLength) {
        if (!TextUtils.equals(downloadBean.url, url)) {
            return;
        }
        progressBar.setProgress(progress);
        downloadButton.setText("下载中");
        downloadButton.setTextColor(Color.WHITE);
    }

    @Override
    public void onPause(final String url, long downloadedLength, long totalLength) {
        if (!TextUtils.equals(downloadBean.url, url)) {
            return;
        }
        downloadButton.setText("暂停中");
        downloadButton.setTextColor(context.getResources().getColor(R.color.colorAccent));
    }

    @Override
    public void onSuccess(final String url, String filePath) {
        if (!TextUtils.equals(downloadBean.url, url)) {
            return;
        }
        downloadButton.setText("安装");
        downloadButton.setTextColor(context.getResources().getColor(R.color.colorPrimary));
        downloadBean.isDownloadSuccess = true;
    }

    @Override
    public void onFailed(final String url, Exception e) {
        if (!TextUtils.equals(downloadBean.url, url)) {
            return;
        }
        LogUtil.d("download failed!", e.getMessage());
        downloadButton.setText("下载失败");
        downloadBean.isDownloadSuccess = false;
    }
}
