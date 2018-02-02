package com.devin.downloader;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;

import java.io.File;

import okhttp3.Call;
import okhttp3.OkHttpClient;

/**
 * Created by Devin on 2018/1/29.
 *
 * @author Devin
 */
public class MercuryDownloader {

    public static Context mContext;

    public Activity mActivity;

    public static OkHttpClient mOkHttpClient;

    private String url;

    private OnResumeListener mOnResumeListener;

    private OnPauseListener mOnPauseListener;

    private OnDownloaderListener mOnDownloaderListener;

    private OnProgressListener mOnProgressListener;

    private OnCancelListener mOnCancelListener;

    public static SPUtils sp;

    public String tag;

    public static void init(Context context, OkHttpClient client) {
        mContext = context;
        mOkHttpClient = client;
        Utils.init(context);
        sp = new SPUtils("downloader.sp");
    }

    public static MercuryDownloader url(String url) {
        MercuryDownloader downloader = new MercuryDownloader();
        downloader.url = url;
        return downloader;
    }

    public MercuryDownloader activity(Activity activity) {
        this.mActivity = activity;
        this.tag = this.mActivity.getClass().getSimpleName() + "|" + this.mActivity.hashCode() + "|" + url;
        this.mActivity.getApplication().registerActivityLifecycleCallbacks(new Application.ActivityLifecycleCallbacks() {
            @Override
            public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
            }

            @Override
            public void onActivityStarted(Activity activity) {
            }

            @Override
            public void onActivityResumed(Activity activity) {
            }

            @Override
            public void onActivityPaused(Activity activity) {
            }

            @Override
            public void onActivityStopped(Activity activity) {
            }

            @Override
            public void onActivitySaveInstanceState(Activity activity, Bundle outState) {
            }

            @Override
            public void onActivityDestroyed(Activity activity) {
                if (mActivity == activity) {
                    cancel();
                    activity.getApplication().unregisterActivityLifecycleCallbacks(this);
                }
            }
        });
        return this;
    }

    /**
     * cancel download request
     */
    private void cancel() {

        if (TextUtils.isEmpty(tag)) {
            return;
        }

        String tagWipeOffUrl = tag.substring(0, tag.lastIndexOf("|"));

        for (Call call : mOkHttpClient.dispatcher().runningCalls()) {
            String tag = call.request().tag().toString();
            String requestTagWipeOffUrl = tag.substring(0, tag.lastIndexOf("|"));
            if (TextUtils.equals(tagWipeOffUrl, requestTagWipeOffUrl)) {
                call.cancel();
            }
        }

        for (Call call : mOkHttpClient.dispatcher().queuedCalls()) {
            String tag = call.request().tag().toString();
            String requestTagWipeOffUrl = tag.substring(0, tag.lastIndexOf("|"));
            if (TextUtils.equals(tagWipeOffUrl, requestTagWipeOffUrl)) {
                call.cancel();
            }
        }
    }

    /**
     * pause download request
     *
     * @param url
     */
    public static void pause(String url) {

        if (TextUtils.isEmpty(url)) {
            return;
        }

        boolean pause = false;

        for (Call call : mOkHttpClient.dispatcher().runningCalls()) {
            String tag = call.request().tag().toString();
            String requestUrl = tag.substring(tag.lastIndexOf("|") + 1);
            if (TextUtils.equals(url, requestUrl)) {
                call.cancel();
                pause = true;
            }
        }

        if (pause) {
            return;
        }

        for (Call call : mOkHttpClient.dispatcher().queuedCalls()) {
            String tag = call.request().tag().toString();
            String requestUrl = tag.substring(tag.lastIndexOf("|") + 1);
            if (TextUtils.equals(url, requestUrl)) {
                call.cancel();
            }
        }
    }

    public MercuryDownloader setOnResumeListener(OnResumeListener onResume) {
        this.mOnResumeListener = onResume;
        return this;
    }

    public MercuryDownloader setOnPauseListener(OnPauseListener onPause) {
        this.mOnPauseListener = onPause;
        return this;
    }

    public void start(OnDownloaderListener onDownloader) {
        this.mOnDownloaderListener = onDownloader;
        download(url);
    }

    public MercuryDownloader setOnProgressListener(OnProgressListener onProgress) {
        this.mOnProgressListener = onProgress;
        return this;
    }

    public MercuryDownloader setOnCancelListener(OnCancelListener onCancelListener) {
        this.mOnCancelListener = onCancelListener;
        return this;
    }

    /**
     * @param url 下载的Url
     */
    public void download(final String url) {
        if (TextUtils.isEmpty(url)) {
            return;
        }
        if (!CommonUtils.isValidUrl(url)) {
            return;
        }
        String path = sp.getString(CommonUtils.getFileName(url));
        final String fileName = CommonUtils.getFileName(url);
        if (TextUtils.isEmpty(path)) {
            showWarningDialog(url, fileName);
            return;
        }

        final File apk = new File(path);
        if (!apk.exists()) {
            showWarningDialog(url, fileName);
            return;
        }
        DownloadUtils.getAsynFileLength(url, bean -> {
            if (bean != null && (bean.contentLength == apk.length())) {
                if (null != mOnDownloaderListener) {
                    bean.path = path;
                    mOnDownloaderListener.onComplete(bean);
                }
            } else if (bean != null && (bean.contentLength != apk.length())) {
                showWarningDialog(url, fileName);
            }
        });
    }

    /**
     * 非WIFI网络显示警告Dialog
     *
     * @param url
     * @param fileName
     */
    private void showWarningDialog(final String url, final String fileName) {
        if (NetworkUtils.isWifi()) {
            doIt(url, fileName);
            return;
        }
        new AlertDialog.Builder(mActivity)
                .setTitle("继续下载？")
                .setMessage("您的手机当前没有连接WIFI，现在下载会消耗您的手机流量，您确定要现在下载吗？")
                .setNegativeButton("待会儿再说", (d, w) -> LogUtils.d("取消下载"))
                .setPositiveButton("立即下载", (d, w) -> doIt(url, fileName))
                .create()
                .show();
    }

    private void doIt(String url, String fileName) {
        CallBackBean bp = sp.getObject(url);
        DownloadUtils.downAsynFile(url, tag, fileName, true, bp, bean -> {
            // 下载完成
            if (bean.contentLength == bean.progressLength && null != mOnDownloaderListener) {
                mOnDownloaderListener.onComplete(bean);
                sp.putString(CommonUtils.getFileName(url), bean.path);
            }
            // 正在下载
            if (bean.contentLength >= bean.progressLength && null != mOnProgressListener) {
                mOnProgressListener.onProgress(bean);
            }
            // 出现错误
            if (null == bean) {
                mOnDownloaderListener.onError();
            }
        });
    }
}
