package com.huison.download;

/**
 * Created by huisonma on 2019/5/15.
 */
final class DownloadComponent {

    /**
     * 下载链接
     */
    private String url;

    /**
     * 下载文件路径
     */
    private String path;

    /**
     * 分块下载起始字节点
     */
    private long start;

    /**
     * 分块下载结束字节点
     */
    private long end;

    /**
     * 分块编号
     */
    private int number;

    /**
     * 分块已下载字节数
     */
    private long downloadedLength;

    /**
     * 文件总字节长度
     */
    private long totalLength;

    DownloadComponent(String url, String path, long start, long end, int number, long downloadedLength, long totalLength) {
        this.url = url;
        this.path = path;
        this.start = start;
        this.end = end;
        this.number = number;
        this.downloadedLength = downloadedLength;
        this.totalLength = totalLength;
    }

    public String getUrl() {
        return url;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public long getStart() {
        return start;
    }

    public void setStart(long start) {
        this.start = start;
    }

    public long getEnd() {
        return end;
    }

    public void setEnd(long end) {
        this.end = end;
    }

    public int getNumber() {
        return number;
    }

    public void setNumber(int number) {
        this.number = number;
    }

    public long getDownloadedLength() {
        return downloadedLength;
    }

    public void setDownloadedLength(long downloadedLength) {
        this.downloadedLength = downloadedLength;
    }

    public long getTotalLength() {
        return totalLength;
    }

    public void setTotalLength(long totalLength) {
        this.totalLength = totalLength;
    }
}
