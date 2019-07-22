package com.devin.downloader;

import android.system.OsConstants;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Created by Devin on 17/4/27.
 *
 * @author Devin
 */
public class DownloadUtils {

    public static List<String> requestUrls = new ArrayList<>();

    public static final String TAG = DownloadUtils.class.getSimpleName();

    /**
     * 下载文件
     *
     * @param url        地址
     * @param tag        请求标记
     * @param fileName   文件名称
     * @param progress   进度
     * @param breakPoint 是否断点下载
     * @param callBack   回调
     */
    public static void downAsynFile(final String url, String tag, final String fileName, boolean progress, CallBackBean breakPoint, final DownloadCallBack callBack) {
        // 多次请求只允许一次
        synchronized (requestUrls) {
            if (requestUrls.contains(url)) {
                return;
            }
            requestUrls.add(url);
        }
        Request request;
        if (null != breakPoint && breakPoint.progressLength != 0) {
            request = new Request.Builder()
                    .addHeader("RANGE", "bytes=" + breakPoint.progressLength + "-" + breakPoint.contentLength)
                    .url(url)
                    .tag(tag)
                    .build();
        } else {
            request = new Request.Builder()
                    .url(url)
                    .tag(tag)
                    .build();
        }
        request(url, fileName, progress, breakPoint, callBack, request);
    }

    private static void request(final String url, final String fileName, final boolean progress, final CallBackBean breakPoint, final DownloadCallBack callBack, Request request) {
        MercuryDownloader.mOkHttpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                if (callBack != null) {
                    callBack.onResponse(null);
                }
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                InputStream inputStream = response.body().byteStream();
                long contentLength = null != breakPoint ? breakPoint.contentLength : response.body().contentLength();
                RandomAccessFile randomAccessFile;
                String localPath = getLocalFilePath(fileName);
                CallBackBean bean = null;
                try {
                    long total = 0;
                    randomAccessFile = new RandomAccessFile(localPath, "rwd");
                    if (null != breakPoint && breakPoint.progressLength > 0) {
                        randomAccessFile.seek(breakPoint.progressLength);
                        total = breakPoint.progressLength;
                    }
                    byte[] buffer = new byte[1024 * 1024];
                    int len;
                    while ((len = inputStream.read(buffer)) != -1) {
                        randomAccessFile.write(buffer, 0, len);
                        if (progress) {
                            total += len;
                            if (callBack != null) {
                                bean = new CallBackBean();
                                bean.path = localPath;
                                bean.isNeedProgress = progress;
                                bean.contentLength = contentLength;
                                bean.progressLength = total;
                                callBack.onResponse(bean);
                            }

                            // 断点下载 记录 已下载大小
                            MercuryDownloader.sp.putObject(url, bean);
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    // 发生了异常要Remove
                    requestUrls.remove(url);
                }
                LogUtils.d(">>>>>localPath:" + localPath);
                if (callBack != null && !progress) {
                    bean = new CallBackBean();
                    bean.path = localPath;
                    bean.isNeedProgress = progress;
                    callBack.onResponse(bean);
                }
                // 下载完了删除此Url
                requestUrls.remove(url);
            }
        });
    }

    /**
     * 获取线上File大小
     *
     * @param url
     * @param callBack
     */
    public static void getAsynFileLength(final String url, final DownloadCallBack callBack) {
        Request request = new Request.Builder().url(url).build();
        MercuryDownloader.mOkHttpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                if (callBack != null) {
                    callBack.onResponse(null);
                }
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (callBack != null) {
                    CallBackBean bean = new CallBackBean();
                    bean.path = getLocalFilePath(url);
                    bean.contentLength = (int) response.body().contentLength();
                    callBack.onResponse(bean);
                }
            }
        });
    }

    /**
     * 本地文件缓存地址
     *
     * @param fileName
     * @return
     */
    public static String getLocalFilePath(String fileName) {
        return MercuryDownloader.mContext.getExternalCacheDir().getAbsolutePath() + File.separator + fileName;
    }

    public interface DownloadCallBack {

        /**
         * progress 为 true 会被频繁调用
         *
         * @param bean
         */
        void onResponse(CallBackBean bean);
    }

}
