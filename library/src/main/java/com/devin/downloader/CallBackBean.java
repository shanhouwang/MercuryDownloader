package com.devin.downloader;

import java.io.Serializable;

/**
 * @author Devin
 */
public class CallBackBean implements Serializable {

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

    @Override
    public String toString() {
        return "CallBackBean{" +
                "path='" + path + '\'' +
                ", isNeedProgress=" + isNeedProgress +
                ", contentLength=" + contentLength +
                ", progressLength=" + progressLength +
                '}';
    }
}