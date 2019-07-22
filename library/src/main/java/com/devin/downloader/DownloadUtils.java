package com.devin.downloader;

import java.io.File;
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

    /**
     * 下载文件
     *
     * @param bean     参数包装类
     * @param callBack 回调
     */
    public static void downAsyncFile(final DownAsyncFileBean bean, final DownloadCallBack callBack) {
        // 多次请求只允许一次
        synchronized (requestUrls) {
            if (requestUrls.contains(bean.url)) {
                return;
            }
            requestUrls.add(bean.url);
        }
        Request request;
        if (null != bean.breakPoint && bean.breakPoint.progressLength != 0) {
            request = new Request.Builder()
                    .addHeader("RANGE", "bytes=" + bean.breakPoint.startPoint + "-" + bean.breakPoint.endPoint)
                    .url(bean.url)
                    .tag(bean.tag)
                    .build();
        } else {
            request = new Request.Builder()
                    .url(bean.url)
                    .tag(bean.tag)
                    .build();
        }
        request(bean, callBack, request);
    }

    private static void request(final DownAsyncFileBean data, final DownloadCallBack callBack, Request request) {

        MercuryDownloader.mOkHttpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                if (callBack != null) {
                    callBack.onResponse(null);
                }
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                assert response.body() != null;
                InputStream inputStream = response.body().byteStream();
                long contentLength = null != data.breakPoint ? data.breakPoint.contentLength : response.body().contentLength();
                RandomAccessFile randomAccessFile;
                String localPath = getLocalFilePath(data.fileName);
                PartCallBackBean bean;
                try {
                    randomAccessFile = new RandomAccessFile(localPath, "rwd");
                    if (null != data.breakPoint) {
                        // 设置从什么位置开始写入数据
                        randomAccessFile.seek(data.breakPoint.startPoint);
                    }
                    byte[] buffer = new byte[1024];
                    int len;
                    int progressLength = 0;
                    while ((len = inputStream.read(buffer)) != -1) {
                        randomAccessFile.write(buffer, 0, len);
                        progressLength += len;
                        if (callBack != null) {
                            bean = new PartCallBackBean();
                            bean.path = localPath;
                            bean.isNeedProgress = data.progress;
                            bean.contentLength = contentLength;
                            bean.progressLength = progressLength;
                            callBack.onResponse(bean);
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    // 发生了异常要Remove
                    requestUrls.remove(data.url);
                }
                LogUtils.d(">>>>>localPath:" + localPath);
                if (callBack != null && !data.progress) {
                    bean = new PartCallBackBean();
                    bean.path = localPath;
                    bean.isNeedProgress = data.progress;
                    callBack.onResponse(bean);
                }
                // 下载完了删除此Url
                requestUrls.remove(data.url);
            }
        });
    }

    /**
     * 获取线上File大小
     *
     * @param url
     * @param callBack
     */
    public static void getAsyncFileLength(final String url, final DownloadCallBack callBack) {
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
                    PartCallBackBean bean = new PartCallBackBean();
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
        void onResponse(PartCallBackBean bean);
    }

}
