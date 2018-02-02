package com.devin.test;

/**
 * Created by Devin on 2018/1/11.
 */

public class AppInfoDTO {

    public long id;

    public String appName;

    public String packageName;

    /**
     * App分类
     */
    public String appClassify;
    public String appDesc;
    public long appSize;

    /**
     * 评分
     */
    public int rating;

    /**
     * App 下载的 Url
     */
    public String downloadUrl;

    /**
     * 下载状态
     * <p>
     * 0：可以下载
     * <p>
     * 1：正在下载
     * <p>
     * 2：已经下载完成
     */
    public int downloadStatus;

    /**
     * 本地地址
     */
    public String localPath;

    /**
     * 下载进度百分比
     */
    public int downloadPercent;

    /**
     * 下载进度大小
     */
    public long downloadProgress;

    public static final int PREPARE_DOWNLOAD = 0;

    public static final int DOWNLOADING = 1;

    public static final int PAUSE_DOWNLOAD = 2;

    public static final int DOWNLOADED = 3;
}
