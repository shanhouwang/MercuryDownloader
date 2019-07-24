package com.devin.downloader;

import java.io.Serializable;

public class DownAsyncFileBean implements Serializable {

    public String url;

    public String tag;

    public String fileName;

    public boolean progress;

    public PartCallBackBean breakPoint;

    public long contentLength;

}
