package com.devin.downloader;

import android.text.TextUtils;
import android.util.Log;

/**
 * Created by Devin on 17/3/23.
 */
public class LogUtils {

    public static final String TAG = LogUtils.class.getSimpleName();
    public static final boolean DEBUG = true;

    public static void d(String msg) {
        if (DEBUG && !TextUtils.isEmpty(msg)) {
            Log.d(TAG, msg);
        }
    }

    /**
     * 点击Log跳转到指定源码位置
     *
     * @param tag
     * @param msg
     */
    public static void show(String tag, String msg) {
        if (DEBUG && !TextUtils.isEmpty(msg)) {
            StackTraceElement[] stackTraceElement = Thread.currentThread().getStackTrace();
            int currentIndex = -1;
            for (int i = 0; i < stackTraceElement.length; i++) {
                if (stackTraceElement[i].getMethodName().compareTo("show") == 0) {
                    currentIndex = i + 1;
                    break;
                }
            }
            if (currentIndex >= 0) {
                String fullClassName = stackTraceElement[currentIndex].getClassName();
                String className = fullClassName.substring(fullClassName
                        .lastIndexOf(".") + 1);
                String methodName = stackTraceElement[currentIndex].getMethodName();
                String lineNumber = String
                        .valueOf(stackTraceElement[currentIndex].getLineNumber());
                Log.d(tag, msg + "\n  >>>>>at " + className + "." + methodName + "("
                        + className + ".java:" + lineNumber + ")");
            } else {
                d(tag, msg);
            }
        }
    }

    public static void d(String tag, String msg) {
        if (DEBUG && !TextUtils.isEmpty(msg)) {
            Log.d(tag, msg);
        }
    }

    public static void e(String tag, String msg) {
        if (DEBUG && !TextUtils.isEmpty(msg)) {
            Log.e(tag, msg);
        }
    }

    public static void i(String tag, String msg) {
        if (DEBUG && !TextUtils.isEmpty(msg)) {
            Log.i(tag, msg);
        }
    }
}