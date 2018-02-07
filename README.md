# MercuryDownloader

```
MercuryDownloader.build()
	.url(model.downloadUrl)
	.activity(context)
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
```


