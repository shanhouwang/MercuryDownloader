package com.devin.downloader;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static java.util.concurrent.Executors.*;

/**
 * Created by Devin on 17/3/15.
 */
public class ExecutorServiceUtils {

    private static ExecutorService mCachedThreadPool;
    private static ScheduledExecutorService mScheduledThreadPool;
    private static ExecutorService mSingleThreadPool;

    private Type type;
    private CallBack callBack;

    private ExecutorServiceUtils() {
    }

    public static ExecutorServiceUtils get(Type type) {
        ExecutorServiceUtils util = new ExecutorServiceUtils();
        util.type = type;
        return util;
    }

    /**
     * 关闭所有定时及周期性任务
     *
     * @return
     */
    public static boolean shut() {
        if (mScheduledThreadPool == null) {
            return false;
        }
        mScheduledThreadPool.shutdownNow();
        return mScheduledThreadPool.isShutdown();
    }

    private ExecutorService build() {
        ExecutorService service = null;
        switch (type) {
            case CACHED:
                if (mCachedThreadPool == null || mCachedThreadPool.isShutdown()) {
                    mCachedThreadPool = newCachedThreadPool();
                }
                service = mCachedThreadPool;
                break;
            case CHAIN:
                if (mSingleThreadPool == null || mSingleThreadPool.isShutdown()) {
                    mSingleThreadPool = newSingleThreadExecutor();
                }
                service = mSingleThreadPool;
                break;
            case SCHEDULED:
                if (mScheduledThreadPool == null || mScheduledThreadPool.isShutdown()) {
                    mScheduledThreadPool = newScheduledThreadPool(10);
                }
                service = mScheduledThreadPool;
                break;
            default:
                break;
        }
        return service;
    }

    public Future start(MercuryRunnable runnable) {
        runnable.setCallBack(callBack);
        return build().submit(runnable);
    }

    public Future start(Runnable runnable) {
        return build().submit(runnable);
    }

    /**
     * 延迟 initialDelay 后 每 period 执行一次
     */
    public ScheduledFuture scheduleWithFixedDelay(MercuryRunnable runnable, long initialDelay, long period, TimeUnit unit) {
        runnable.setCallBack(callBack);
        build();
        return mScheduledThreadPool.scheduleWithFixedDelay(runnable, initialDelay, period, unit);
    }

    /**
     * 延迟 initialDelay 后 每 period 执行一次
     */
    public ScheduledFuture scheduleWithFixedDelay(Runnable runnable, long initialDelay, long period, TimeUnit unit) {
        build();
        return mScheduledThreadPool.scheduleWithFixedDelay(runnable, initialDelay, period, unit);
    }

    /**
     * 延迟 delay 后执行
     */
    public ScheduledFuture schedule(MercuryRunnable runnable, long delay, TimeUnit unit) {
        runnable.setCallBack(callBack);
        build();
        return mScheduledThreadPool.schedule(runnable, delay, unit);
    }

    public ExecutorServiceUtils callBack(CallBack callBack) {
        this.callBack = callBack;
        return this;
    }

    public enum Type {
        /**
         * 可灵活回收空闲线程，若无可回收，则新建线程
         */
        CACHED,

        /**
         * 它只会用唯一的工作线程来执行任务 保证所有任务按照指定顺序(FIFO, LIFO, 优先级)执行
         */
        CHAIN,

        /**
         * 支持定时及周期性任务执行
         */
        SCHEDULED
    }

    public interface CallBack<T> {

        void onResponse(T obj);
    }

    public abstract static class MercuryRunnable<T> implements Runnable {

        private CallBack callBack;

        public void setCallBack(CallBack callBack) {
            this.callBack = callBack;
        }

        public abstract T execute();

        @Override
        public void run() {
            callBack.onResponse(execute());
        }
    }
}
