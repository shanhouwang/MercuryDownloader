package com.devin.downloader;

import java.io.Serializable;

/**
 * @author Devin
 */
public class PartCallBackBean implements Serializable {

    /**
     * 下载后 存储到本地的Url
     */
    public String path;

    /**
     * 是否需要进度
     */
    public boolean isNeedProgress;

    /**
     * 文件大小
     */
    public long contentLength;

    /**
     * 每次更新进度
     */
    public long progressLength;

    /**
     * 开始位置
     */
    public long startPoint = 0;

    /**
     * 结束位置
     */
    public long endPoint;

    /**
     * Part索引
     */
    public int index;

    @Override
    public String toString() {
        return "PartCallBackBean{" +
                "path='" + path + '\'' +
                ", isNeedProgress=" + isNeedProgress +
                ", contentLength=" + contentLength +
                ", progressLength=" + progressLength +
                ", startPoint=" + startPoint +
                ", endPoint=" + endPoint +
                ", index=" + index +
                '}';
    }
}