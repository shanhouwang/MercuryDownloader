package com.devin.downloader;

import android.text.TextUtils;

/**
 * Created by Devin on 2018/1/29.
 */

public class CommonUtils {

    /**
     * 检测Url的合法性
     *
     * @param url
     * @return
     */
    public static boolean isValidUrl(String url) {
        if (TextUtils.isEmpty(url)) {
            return false;
        }
        if ((url.startsWith("http://") || url.startsWith("https://"))) {
            return true;
        }
        return false;
    }

    /**
     * 传入格式 http://www.tp.com/tp.apk
     * <p>
     * 返回 tp.apk
     */
    public static String getFileName(String url) {
        if (TextUtils.isEmpty(url)) {
            return null;
        }
        String fileName = null;
        int begin = url.lastIndexOf("/");
        if (begin != -1) {
            fileName = url.substring(begin + 1, url.length());
        }
        return fileName;
    }
}
