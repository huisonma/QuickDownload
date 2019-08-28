package com.huison.download.demo;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.huison.download.R;
import com.huison.download.entry.DownloadBean;

import java.util.List;

/**
 * Created by huisonma on 2019/5/15.
 */
public class DownloadAdapter extends RecyclerView.Adapter<DownloadViewHolder> {

    private List<DownloadBean> apkList;

    private LayoutInflater inflater;

    public DownloadAdapter(Context context, List<DownloadBean> list) {
        this.apkList = list;
        inflater = LayoutInflater.from(context);
    }

    @NonNull
    @Override
    public DownloadViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int position) {
        View view = inflater.inflate(R.layout.item_download_apk, viewGroup, false);
        return new DownloadViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DownloadViewHolder downloadViewHolder, int position) {
        downloadViewHolder.bindData(apkList.get(position));
    }

    @Override
    public int getItemCount() {
        return apkList.size();
    }
}