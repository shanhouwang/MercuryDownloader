package com.devin.downloader;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.DialogInterface;
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

    private Activity mActivity;

    public static OkHttpClient mOkHttpClient;

    private String url;

    private String fileName;

    private boolean isWarning = false;

    private OnResumeListener mOnResumeListener;

    private OnPauseListener mOnPauseListener;

    private OnCompleteListener mOnDownloaderListener;

    private OnErrorListener mOnErrorListener;

    private OnProgressListener mOnProgressListener;

    private OnCancelListener mOnCancelListener;

    public static SPUtils sp;

    private String tag;

    // 默认使用缓存（断点下载）
    private boolean useCache = true;

    public static void init(Context context, OkHttpClient client) {
        mContext = context;
        mOkHttpClient = client;
        Utils.init(context);
        sp = new SPUtils("downloader.sp");
    }

    private MercuryDownloader() {}

    public static MercuryDownloader build() {
        return new MercuryDownloader();
    }

    public MercuryDownloader url(String url) {
        this.url = url;
        return this;
    }

    public MercuryDownloader fileName(String fileName) {
        this.fileName = fileName;
        return this;
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

    public MercuryDownloader useCache(boolean cache) {
        this.useCache = cache;
        return this;
    }

    public MercuryDownloader warningTip(boolean isWarning) {
        this.isWarning = isWarning;
        return this;
    }

    public MercuryDownloader setOnResumeListener(OnResumeListener onResume) {
        this.mOnResumeListener = onResume;
        return this;
    }

    public MercuryDownloader setOnPauseListener(OnPauseListener onPause) {
        this.mOnPauseListener = onPause;
        return this;
    }

    public MercuryDownloader setOnCompleteListener(OnCompleteListener onDownloader) {
        this.mOnDownloaderListener = onDownloader;
        return this;
    }

    public MercuryDownloader setOnErrorListener(OnErrorListener listener) {
        this.mOnErrorListener = listener;
        return this;
    }

    public MercuryDownloader setOnProgressListener(OnProgressListener onProgress) {
        this.mOnProgressListener = onProgress;
        return this;
    }

    public MercuryDownloader setOnCancelListener(OnCancelListener onCancelListener) {
        this.mOnCancelListener = onCancelListener;
        return this;
    }

    public void start() {
        download(url);
    }

    /**
     * @param url 下载的Url
     */
    private void download(final String url) {
        if (TextUtils.isEmpty(url)) {
            return;
        }
        if (!CommonUtils.isValidUrl(url)) {
            return;
        }
        final String fileName = TextUtils.isEmpty(this.fileName) ? CommonUtils.getFileName(url) : this.fileName;
        final String path = sp.getString(fileName);
        if (TextUtils.isEmpty(path)) {
            showWarningDialogAndDownloadIt(url, fileName, isWarning);
            return;
        }
        final File f = new File(path);
        if (!f.exists()) {
            showWarningDialogAndDownloadIt(url, fileName, isWarning);
            return;
        }
        if (!useCache) {
            // 删除本地文件（不使用缓存）
            f.delete();
            showWarningDialogAndDownloadIt(url, fileName, isWarning);
            return;
        }
        DownloadUtils.getAsynFileLength(url, new DownloadUtils.DownloadCallBack() {

            @Override
            public void onResponse(CallBackBean bean) {
                if (bean != null && (bean.contentLength == f.length())) {
                    if (null != mOnDownloaderListener) {
                        bean.path = path;
                        mOnDownloaderListener.onComplete(bean);
                    }
                } else if (bean != null && (bean.contentLength != f.length())) {
                    showWarningDialogAndDownloadIt(url, fileName, isWarning);
                }
            }
        });
    }

    /**
     * 非WIFI网络显示警告Dialog
     *
     * @param url
     * @param fileName
     */
    private void showWarningDialogAndDownloadIt(final String url, final String fileName, boolean isWarning) {
        if (!isWarning) {
            doIt(url, fileName);
            return;
        }
        if (NetworkUtils.isWifi()) {
            doIt(url, fileName);
            return;
        }
        new AlertDialog.Builder(mActivity)
                .setTitle("继续下载？")
                .setMessage("您的手机当前没有连接Wifi，现在下载会消耗您的手机流量，您确定要现在下载吗？")
                .setNegativeButton("待会儿再说", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        if (null != mOnCancelListener) {
                            mOnCancelListener.onCancel();
                        }
                    }
                })
                .setCancelable(false)
                .setPositiveButton("立即下载", new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        doIt(url, fileName);
                    }
                })
                .create()
                .show();
    }

    private void doIt(final String url, String fileName) {
        CallBackBean bp = sp.getObject(url);
        if (useCache && bp != null) {
            File file = new File(bp.path);
            if (!file.exists()) {
                bp = null;
            }
        } else {
            bp = null;
        }
        DownloadUtils.downAsynFile(url, tag, fileName, true, bp, new DownloadUtils.DownloadCallBack() {

            @Override
            public void onResponse(CallBackBean bean) {
                // 下载完成
                if (bean.contentLength == bean.progressLength && null != mOnDownloaderListener) {
                    mOnDownloaderListener.onComplete(bean);
                    sp.putString(TextUtils.isEmpty(MercuryDownloader.this.fileName) ? CommonUtils.getFileName(url) : MercuryDownloader.this.fileName, bean.path);
                }
                // 正在下载
                if (bean.contentLength >= bean.progressLength && null != mOnProgressListener) {
                    mOnProgressListener.onProgress(bean);
                }
                // 出现错误
                if (null == bean && null != mOnErrorListener) {
                    mOnErrorListener.onError();
                }
            }
        });
    }
}
