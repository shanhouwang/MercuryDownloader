package com.devin.downloader;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;

/**
 * Created by Devin on 2018/1/29.
 *
 * @author Devin
 */
public class MercuryDownloader {

    // 参数初始化
    private static final int CPU_COUNT = Runtime.getRuntime().availableProcessors();

    // 核心线程数量
    private static final int NUM_THREADS = Math.max(2, Math.min(CPU_COUNT - 1, 4));

    private static final int DEFAULT_NUM_THREAD = 1;

    private Map<Integer, PartCallBackBean> progressMap = new Hashtable<>();

    private static final Map<String, List<Future>> futuresMap = new Hashtable<>();

    public static Context mContext;

    private Activity mActivity;

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

    // 是否使用多线程
    private boolean useMultiThread = false;

    private CallBackBean mCallBackBean = new CallBackBean();

    public static void init(Context context) {
        mContext = context;
        Utils.init(context);
        sp = new SPUtils("downloader.sp");
    }

    private MercuryDownloader() {
    }

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
                    activity.getApplication().unregisterActivityLifecycleCallbacks(this);
                }
            }
        });
        return this;
    }

    public MercuryDownloader useMultiThread(boolean useMultiThread) {
        this.useMultiThread = useMultiThread;
        return this;
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
     * pause download request
     *
     * @param url
     */
    public static void pause(String url) {
        if (TextUtils.isEmpty(url)) {
            return;
        }
        List<Future> futures = futuresMap.get(url);
        if (null == futures || futures.size() <= 0) {
            return;
        }
        for (Future f : futures) {
            f.cancel(true);
        }
        futures.clear();
        futuresMap.remove(url);
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
        List<Future> futures = futuresMap.get(url);
        if (futures != null && futures.size() > 0) {
            // 说明正在下载中
            return;
        }
        fileName = TextUtils.isEmpty(this.fileName) ? CommonUtils.getFileName(url) : this.fileName;
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
        DownloadUtils.getAsyncFileLength(url, new DownloadUtils.DownloadCallBack() {

            @Override
            public void onResponse(PartCallBackBean bean) {
                if (bean != null && (bean.contentLength == f.length())) {
                    if (null != mOnDownloaderListener) {
                        CallBackBean data = new CallBackBean();
                        data.path = path;
                        mOnDownloaderListener.onComplete(data);
                    }
                } else if (bean != null && (bean.contentLength != f.length())) {
                    showWarningDialogAndDownloadIt(url, fileName, isWarning);
                }
            }

            @Override
            public void onFailure(Exception e) {
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

    private void doIt(final String url, final String fileName) {
        Map<Integer, PartCallBackBean> parts = sp.getObject(url);
        if (useCache) {
            if (parts != null) {
                PartCallBackBean part = parts.get(0);
                if (part != null) {
                    File file = new File(part.path);
                    if (!file.exists()) {
                        parts = null;
                    }
                }
            }
        } else {
            parts = null;
        }
        final Map<Integer, PartCallBackBean> catchParts = parts;
        final List<Future> futures = new ArrayList<>();
        DownloadUtils.getAsyncFileLength(url, new DownloadUtils.DownloadCallBack() {
            @Override
            public void onResponse(PartCallBackBean bean) {
                if (bean != null) {
                    mCallBackBean.contentLength = bean.contentLength;
                    if (useMultiThread) {
                        long blockSize = bean.contentLength / NUM_THREADS;
                        for (int i = 0; i < NUM_THREADS; i++) {
                            PartCallBackBean part;
                            if (catchParts == null || catchParts.get(i) == null) {
                                long endPoint = (i + 1) != NUM_THREADS ? (i + 1) * blockSize - 1 : bean.contentLength;
                                part = new PartCallBackBean();
                                part.index = i;
                                part.startPoint = i * blockSize;
                                part.endPoint = endPoint;
                            } else {
                                part = catchParts.get(i);
                            }
                            futures.add(async(part, url, fileName, bean.contentLength));
                        }
                    } else {
                        futures.add(async(catchParts == null ? null : catchParts.get(0), url, fileName, bean.contentLength));
                    }
                    futuresMap.put(url, futures);
                }
            }

            @Override
            public void onFailure(Exception e) {
            }
        });
    }

    private Future async(PartCallBackBean bean, String url, String fileName, long contentLength) {
        final DownAsyncFileBean b = new DownAsyncFileBean();
        b.url = url;
        b.tag = tag;
        b.fileName = fileName;
        b.progress = true;
        b.breakPoint = bean;
        b.contentLength = contentLength;
        return DownloadUtils.downAsyncFile(b, new DownloadUtils.DownloadCallBack() {
            @Override
            public void onResponse(PartCallBackBean bean) {
                progressMap.put(bean.index, bean);
                synchronized (MercuryDownloader.this) {
                    mCallBackBean.progressLength = 0;
                    mCallBackBean.path = bean.path;
                    map();
                }
            }

            @Override
            public void onFailure(Exception e) {
            }
        });
    }

    private void map() {
        for (int i = 0; i < (useMultiThread ? NUM_THREADS : DEFAULT_NUM_THREAD); i++) {
            PartCallBackBean temp = progressMap.get(i);
            if (temp == null) {
                continue;
            }
            mCallBackBean.progressLength += temp.progressLength;
        }
        // 下载完成
        if (null != mOnDownloaderListener && mCallBackBean.contentLength != 0 && mCallBackBean.contentLength == mCallBackBean.progressLength) {
            mOnDownloaderListener.onComplete(mCallBackBean);
            sp.putString(MercuryDownloader.this.fileName, mCallBackBean.path);
            sp.putObject(url, progressMap);
            cancelFuture();
        }
        // 正在下载
        if (null != mOnProgressListener && mCallBackBean.progressLength != 0 && mCallBackBean.contentLength >= mCallBackBean.progressLength) {
            mOnProgressListener.onProgress(mCallBackBean);
            sp.putObject(url, progressMap);
        }
        LogUtils.d(">>>>>map: " + mCallBackBean.toString());
        // 出现错误
        if (null == mCallBackBean && null != mOnErrorListener) {
            mOnErrorListener.onError();
            cancelFuture();
        }
    }

    private void cancelFuture() {
        List<Future> futures = futuresMap.get(url);
        if (futures != null) futures.clear();
        futuresMap.remove(url);
    }
}
