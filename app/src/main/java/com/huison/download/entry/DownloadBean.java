package com.huison.download.entry;

/**
 * Created by huisonma on 2019/5/15.
 */
public class DownloadBean {

    public String url;
    public String icon;
    public String name;
    public String size;
    public String downloadTimes;
    public boolean isDownloadSuccess;

    public DownloadBean(String name, String icon, String url, String size, String downloadTimes) {
        this.name = name;
        this.icon = icon;
        this.url = url;
        this.size = size;
        this.downloadTimes = downloadTimes;
    }
}
