package com.devin.downloader;

/**
 * Created by Devin on 2018/1/29.
 *
 * @author Devin
 */
public interface OnDownloaderListener {

    void onComplete(CallBackBean backBean);

    void onError();
}
