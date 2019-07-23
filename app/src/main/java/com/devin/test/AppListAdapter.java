package com.devin.test;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.content.FileProvider;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RatingBar;
import android.widget.TextView;

import com.devin.downloader.MercuryDownloader;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * App列表适配器
 *
 * @author Devin
 */
public class AppListAdapter extends RecyclerView.Adapter<AppListAdapter.ViewHolder> {

    private Activity context;

    private List<AppInfoDTO> data = new ArrayList<>();

    public Handler mHandler = new Handler(Looper.getMainLooper());

    public static int CLICK_POSITION;

    public void initData(List<AppInfoDTO> data) {
        this.data.clear();
        this.data.addAll(data);
        notifyDataSetChanged();
    }

    public void bindLoadMoreData(List<AppInfoDTO> data) {
        this.data.addAll(data);
        notifyDataSetChanged();
    }

    public AppListAdapter(Activity context) {
        this.context = context;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(context).inflate(R.layout.index_fragment_item, null));
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, final int position) {
        final AppInfoDTO model = data.get(position);
        holder.tv_app_name.setText(model.appName);
        holder.rating_bar.setRating(model.rating);
        holder.tv_install.setOnClickListener(v -> {

            CLICK_POSITION = position;

            if (AppInfoDTO.PREPARE_DOWNLOAD == model.downloadStatus || AppInfoDTO.PAUSE_DOWNLOAD == model.downloadStatus) {
                model.downloadStatus = AppInfoDTO.DOWNLOADING;
                holder.layout_progressbar.setVisibility(View.VISIBLE);
                MercuryDownloader.build()
                        .url(model.downloadUrl)
                        .useCache(false)
                        .activity(context)
                        .useMultiThread(true)
                        .setOnCancelListener(() -> {
                            model.downloadStatus = AppInfoDTO.PREPARE_DOWNLOAD;
                            notifyItemChanged(position, R.id.tv_progress);
                        })
                        .setOnProgressListener(bean -> mHandler.post(() -> {
                            int percent = (int) ((double) bean.progressLength / bean.contentLength * 100);
                            model.downloadProgress = percent;
                            notifyItemChanged(position, R.id.tv_progress);
                        }))
                        .setOnCompleteListener(backBean -> mHandler.post(() -> {
                            model.downloadStatus = AppInfoDTO.DOWNLOADED;
                            model.localPath = backBean.path;
                            model.downloadProgress = 100;
                            holder.layout_progressbar.setVisibility(View.GONE);
                            context.startActivity(getIntent(backBean.path));
                            notifyItemChanged(position);
                        }))
                        .start();
            } else if (model.downloadStatus == AppInfoDTO.DOWNLOADING) {
                model.downloadStatus = AppInfoDTO.PAUSE_DOWNLOAD;
                notifyItemChanged(position);
            } else if (model.downloadStatus == AppInfoDTO.DOWNLOADED) {
                if (!TextUtils.isEmpty(model.localPath)) {
                    context.startActivity(getIntent(model.localPath));
                }
            }
        });
        holder.tv_size.setText(model.appSize + "M");
        holder.tv_app_desc.setText(model.appDesc);
        setStatus(model, holder);

        if (position % DIVISOR == 0) {
            holder.itemView.setBackgroundColor(context.getResources().getColor(R.color._ffffff));
        } else {
            holder.itemView.setBackgroundColor(context.getResources().getColor(R.color._e8ebed));
        }
    }

    private static final int DIVISOR = 2;

    @Override
    public int getItemCount() {
        return data.size();
    }

    private void setStatus(final AppInfoDTO model, final ViewHolder holder) {
        switch (model.downloadStatus) {
            case AppInfoDTO.PREPARE_DOWNLOAD:
                holder.tv_install.setText("下载");
                holder.layout_progressbar.setVisibility(View.GONE);
                holder.layout_install.setBackground(context.getResources().getDrawable(R.drawable.index_item_install_bg));
                holder.tv_install.setTextColor(context.getResources().getColor(R.color._4dbe2e));
                break;
            case AppInfoDTO.DOWNLOADING:
                if (holder.layout_progressbar.getVisibility() != View.VISIBLE) {
                    holder.layout_progressbar.setVisibility(View.VISIBLE);
                }
                holder.progressbar.setVisibility(View.VISIBLE);
                holder.tv_progress.setText(model.downloadProgress + "%");
                holder.iv_pause.setVisibility(View.GONE);
                break;
            case AppInfoDTO.PAUSE_DOWNLOAD:
                holder.layout_progressbar.setVisibility(View.VISIBLE);
                holder.iv_pause.setVisibility(View.VISIBLE);
                holder.progressbar.setVisibility(View.GONE);
                holder.tv_progress.setText(model.downloadProgress + "%");
                break;
            case AppInfoDTO.DOWNLOADED:
                holder.layout_progressbar.setVisibility(View.GONE);
                holder.tv_install.setText("安装");
                holder.layout_install.setBackground(context.getResources().getDrawable(R.drawable.index_item_downloaded_bg));
                holder.tv_install.setTextColor(context.getResources().getColor(R.color._ffffff));
                break;
            default:
                holder.layout_install.setBackground(context.getResources().getDrawable(R.drawable.index_item_install_bg));
                holder.tv_install.setTextColor(context.getResources().getColor(R.color._4dbe2e));
                holder.layout_progressbar.setVisibility(View.GONE);
                holder.tv_install.setText("下载");
                break;
        }
    }

    /**
     * 跳转App安装页面
     *
     * @param path
     * @return
     */
    public Intent getIntent(String path) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            Uri contentUri = FileProvider.getUriForFile(context, "com.devin.downloader.FileProvider", new File(path));
            intent.setDataAndType(contentUri, "application/vnd.android.package-archive");
        } else {
            intent.setDataAndType(Uri.fromFile(new File(path)), "application/vnd.android.package-archive");
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        }
        return intent;
    }

    class ViewHolder extends RecyclerView.ViewHolder {

        View itemView;

        ImageView iv_app_cover;
        TextView tv_app_name;
        RatingBar rating_bar;
        TextView tv_classify_name;
        TextView tv_size;
        FrameLayout layout_install;
        LinearLayout layout_progressbar;
        ImageView iv_pause;
        ProgressBar progressbar;
        TextView tv_install;
        TextView tv_app_desc;
        TextView tv_progress;

        public ViewHolder(View itemView) {
            super(itemView);
            this.itemView = itemView;
            iv_app_cover = itemView.findViewById(R.id.iv_app_cover);
            tv_app_name = itemView.findViewById(R.id.tv_app_name);
            rating_bar = itemView.findViewById(R.id.rating_bar);
            tv_classify_name = itemView.findViewById(R.id.tv_classify_name);
            tv_size = itemView.findViewById(R.id.tv_size);
            tv_install = itemView.findViewById(R.id.tv_install);
            tv_app_desc = itemView.findViewById(R.id.tv_app_desc);
            layout_install = itemView.findViewById(R.id.layout_install);
            layout_progressbar = itemView.findViewById(R.id.layout_progressbar);
            iv_pause = itemView.findViewById(R.id.iv_pause);
            progressbar = itemView.findViewById(R.id.progressbar);
            tv_progress = itemView.findViewById(R.id.tv_progress);
        }
    }
}