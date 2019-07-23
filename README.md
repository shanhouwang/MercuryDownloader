# MercuryDownloader
##  加入项目中
```
implementation 'com.devin:downloader:1.0.0-beta'
```
##  初始化
```
MercuryDownloader.init(application);
```
##  下载设置
```
MercuryDownloader.build()
        .url(model.downloadUrl)
        .useCache(false)
        .activity(context)
        .useMultiThread(true)
        .setOnCancelListener(() -> {
            model.downloadStatus = AppInfoDTO.PREPARE_DOWNLOAD;
            notifyItemChanged(position, R.id.tv_progress);
        })
        // 子线程
        .setOnProgressListener(bean -> mHandler.post(() -> {
            int percent = (int) ((double) bean.progressLength / bean.contentLength * 100);
            model.downloadProgress = percent;
            notifyItemChanged(position, R.id.tv_progress);
        }))
        // 子线程
        .setOnCompleteListener(backBean -> mHandler.post(() -> {
            model.downloadStatus = AppInfoDTO.DOWNLOADED;
            model.localPath = backBean.path;
            model.downloadProgress = 100;
            holder.layout_progressbar.setVisibility(View.GONE);
            context.startActivity(getIntent(backBean.path));
            notifyItemChanged(position);
        }))
        .start();                                                                           
```
## 暂停/取消一个下载

```
MercuryDownloader.pause(model.downloadUrl);
```


