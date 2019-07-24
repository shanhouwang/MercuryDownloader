package com.devin.downloader;

import java.io.File;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.Future;

/**
 * Created by Devin on 17/4/27.
 *
 * @author Devin
 */
public class DownloadUtils {

    /**
     * @param requestBean 请求参数封装
     * @param callBack    回调
     */
    public static Future downAsyncFile(final DownAsyncFileBean requestBean, final DownloadCallBack callBack) {
        return ExecutorServiceUtils.get(ExecutorServiceUtils.Type.CACHED).start(new Runnable() {
            @Override
            public void run() {
                HttpURLConnection conn = null;
                try {
                    URL url = new URL(requestBean.url);
                    conn = (HttpURLConnection) url.openConnection();
                    conn.setConnectTimeout(15 * 1000);
                    conn.setReadTimeout(15 * 1000);
                    conn.setRequestMethod("GET");
                    long seek = 0;
                    if (null != requestBean.breakPoint) {
                        seek = requestBean.breakPoint.startPoint + requestBean.breakPoint.progressLength;
                        if (requestBean.breakPoint.endPoint == 0) {
                            requestBean.breakPoint.endPoint = requestBean.contentLength;
                        }
                        conn.setRequestProperty("Range", "bytes=" + seek + "-" + requestBean.breakPoint.endPoint);
                    }
                    int responseCode = conn.getResponseCode();
                    LogUtils.d(">>>>>request, responseCode: " + responseCode);
                    if (responseCode == HttpURLConnection.HTTP_PARTIAL || responseCode == HttpURLConnection.HTTP_OK) {
                        String localPath = getLocalFilePath(requestBean.fileName);
                        PartCallBackBean bean = null;
                        RandomAccessFile randomAccessFile = new RandomAccessFile(localPath, "rwd");
                        long totalLength = 0;
                        if (null != requestBean.breakPoint) {
                            // 设置从什么位置开始写入数据
                            randomAccessFile.seek(seek);
                            totalLength = requestBean.breakPoint.progressLength;
                        }
                        InputStream inputStream = conn.getInputStream();
                        byte[] buffer = new byte[16 * 1024];
                        int length;
                        while ((length = inputStream.read(buffer)) != -1) {
                            randomAccessFile.write(buffer, 0, length);
                            LogUtils.d(">>>>>request, while: " + Thread.currentThread().getName() + ", write length: " + length + ", catche: " + totalLength + ", contentLength: " + requestBean.contentLength);
                            totalLength += length;
                            if (callBack != null) {
                                bean = new PartCallBackBean();
                                if (null != requestBean.breakPoint) {
                                    bean.index = requestBean.breakPoint.index;
                                    bean.startPoint = requestBean.breakPoint.startPoint;
                                    bean.endPoint = requestBean.breakPoint.endPoint;
                                }
                                bean.path = localPath;
                                bean.isNeedProgress = requestBean.progress;
                                bean.progressLength = totalLength;
                                callBack.onResponse(bean);
                            }
                        }
                        assert bean != null;
                        LogUtils.d(">>>>>request, callBack: " + Thread.currentThread().getId() + ", " + bean.toString());
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    callBack.onFailure(e);
                } finally {
                    if (null != conn) {
                        conn.disconnect();
                    }
                }
            }
        });
    }

    /**
     * 获取线上File大小
     *
     * @param path     路径
     * @param callBack 回调
     */
    public static Future getAsyncFileLength(final String path, final DownloadCallBack callBack) {
        return ExecutorServiceUtils.get(ExecutorServiceUtils.Type.CACHED).start(new Runnable() {
            @Override
            public void run() {
                HttpURLConnection conn = null;
                try {
                    URL url = new URL(path);
                    conn = (HttpURLConnection) url.openConnection();
                    conn.setConnectTimeout(15 * 1000);
                    conn.setReadTimeout(15 * 1000);
                    conn.setRequestMethod("GET");
                    int responseCode = conn.getResponseCode();
                    LogUtils.d(">>>>>request, responseCode: " + responseCode);
                    if (responseCode == HttpURLConnection.HTTP_PARTIAL || responseCode == HttpURLConnection.HTTP_OK) {
                        long contentLength = conn.getContentLength();
                        PartCallBackBean bean = new PartCallBackBean();
                        bean.contentLength = contentLength;
                        callBack.onResponse(bean);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    if (callBack != null) {
                        callBack.onResponse(null);
                    }
                } finally {
                    if (null != conn) {
                        conn.disconnect();
                    }
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

        void onFailure(Exception e);
    }

}
