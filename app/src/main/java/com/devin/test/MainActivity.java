package com.devin.test;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.devin.downloader.MercuryDownloader;
import com.devin.refreshview.MarsRefreshView;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;

/**
 * @author Devin
 */
public class MainActivity extends AppCompatActivity {

    public static OkHttpClient mOkHttpClient;

    private MarsRefreshView marsRefreshView;
    private AppListAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mOkHttpClient = new OkHttpClient.Builder()
                .connectTimeout(30000L, TimeUnit.MILLISECONDS)
                .readTimeout(30000L, TimeUnit.MILLISECONDS)
//                .addInterceptor(new HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY))
                .build();
        MercuryDownloader.init(this, mOkHttpClient);

        marsRefreshView = findViewById(R.id.marsRefreshView);
        marsRefreshView
                .setLinearLayoutManager()
                .setAdapter(adapter = new AppListAdapter(this))
                .setMercuryOnLoadMoreListener(1, page -> {
                    if (page == 5) {
                        marsRefreshView.onComplete();
                        return;
                    }
                    ThreadUtils.get(ThreadUtils.Type.SCHEDULED).callBack(obj -> adapter.bindLoadMoreData((List<AppInfoDTO>) obj))
                            .schedule(new ThreadUtils.MyRunnable() {
                                @Override
                                public Object execute() {
                                    List<AppInfoDTO> models = new ArrayList<>();
                                    for (int i = 10 * page; i < 10 * page + 10; i++) {
                                        AppInfoDTO appInfoDTO = new AppInfoDTO();
                                        appInfoDTO.id = i;
                                        appInfoDTO.appName = "应用名称 " + i;
                                        appInfoDTO.rating = i % 5;
                                        appInfoDTO.appSize = 10 + i;
                                        appInfoDTO.appClassify = "金融理财";
                                        appInfoDTO.appDesc = "金融理财的好帮手，年利率10%以上产品很多";
                                        appInfoDTO.downloadUrl = "http://imtt.dd.qq.com/16891/F85076B8EA32D933089CEA797CF38C30.apk";
                                        models.add(appInfoDTO);
                                    }
                                    return models;
                                }
                            }, 1 * 1000, TimeUnit.MILLISECONDS);

                });

        initData();
    }

    private void initData() {
        ThreadUtils.get(ThreadUtils.Type.SCHEDULED).callBack(obj -> adapter.initData((List<AppInfoDTO>) obj))
                .schedule(new ThreadUtils.MyRunnable() {
                    @Override
                    public Object execute() {
                        List<AppInfoDTO> models = new ArrayList<>();
                        for (int i = 1; i < 11; i++) {
                            AppInfoDTO appInfoDTO = new AppInfoDTO();
                            appInfoDTO.id = i;
                            appInfoDTO.appName = "应用名称 " + i;
                            appInfoDTO.rating = i % 5;
                            appInfoDTO.appSize = 10 + i;
                            appInfoDTO.appClassify = "金融理财";
                            appInfoDTO.appDesc = "金融理财的好帮手，年利率10%以上产品很多";
                            appInfoDTO.downloadUrl = "http://imtt.dd.qq.com/16891/F85076B8EA32D933089CEA797CF38C30.apk";
                            if (i == 1) {
                                appInfoDTO.packageName = "com.tencent.qqlive";
                                appInfoDTO.downloadUrl = "http://imtt.dd.qq.com/16891/110A36BF492C6672528F40A4FFDB22B4.apk";
                            }
                            if (i == 2) {
                                appInfoDTO.packageName = "com.cleanmaster.mguard_cn";
                                appInfoDTO.downloadUrl = "http://imtt.dd.qq.com/16891/3CC768370B43EDF35F56BB3948C77BA8.apk";
                            }
                            models.add(appInfoDTO);
                        }
                        return models;
                    }
                }, 1 * 1000, TimeUnit.MILLISECONDS);
    }
}
