package com.example.lxd.servicebestpractice;

/**
 * Created by lxd on 2017/12/5.
 */

public interface DownloadListener
{
    void onProgress(int progress);
    void onSuccess();
    void onFailed();
    void onPaused();
    void onCanceled();
}
