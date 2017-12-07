package com.example.lxd.servicebestpractice;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.Binder;
import android.os.Environment;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.widget.Toast;

import java.io.File;

//为了保证DownloadTask可以一直在后台运行，需要创建一个下载的服务
public class DownloadService extends Service
{
    private DownloadTask mDownloadTask;
    private String download_URL;
    public DownloadService()
    {
    }

    private Notification get_notification(String title,int progress)
    {
        Intent intent = new Intent(this,MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this,0,intent,0);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this,"");
        builder.setSmallIcon(R.mipmap.ic_launcher);
        builder.setLargeIcon(BitmapFactory.decodeResource(getResources(),R.mipmap.ic_launcher));
        builder.setContentTitle(title);
        builder.setContentIntent(pendingIntent);

        if (progress >= 0)
        {
            //当progress大于或等于0时才需显示下载进度
            builder.setContentText(progress + "%");

            //接受3个参数
            builder.setProgress(
                    100,//第一个传入通知的最大进度
                    progress,//第二个参数传入通知的当前进度
                    false);//第三个参数表示是否使用模糊进度条，这里传入false
        }
        return builder.build();
    }

    //private NotificationManager mNotificationManager;
    private NotificationManager get_notification_manager()
    {
        return (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
    }

    private DownloadListener mDownloadListener = new DownloadListener()
    {
        @Override
        public void onProgress(int progress)
        {
            NotificationManager mNotificationManager = get_notification_manager();
            Notification mNotification = get_notification("Downloading...",progress);
            mNotificationManager.notify(1,mNotification);
        }

        @Override
        public void onSuccess()
        {
            mDownloadTask = null;
            //下载成功时将前台服务通知关闭，并创建一个下载成功的通知
            stopForeground(true);
            NotificationManager mNotificationManager = get_notification_manager();
            Notification mNotification = get_notification("Download Success",-1);
            mNotificationManager.notify(1,mNotification);
            Toast.makeText(
                    DownloadService.this,
                    "Download Success",
                    Toast.LENGTH_SHORT
            ).show();
        }

        @Override
        public void onFailed()
        {
            mDownloadTask = null;
            //下载失败时将前台服务通知关闭，并创建一个下载失败的通知
            stopForeground(true);
            NotificationManager mNotificationManager = get_notification_manager();
            Notification mNotification = get_notification("Download Failed",-1);
            mNotificationManager.notify(1,mNotification);
            Toast.makeText(
                    DownloadService.this,
                    "Download Failed",
                    Toast.LENGTH_SHORT
            ).show();
        }

        @Override
        public void onPaused()
        {
            mDownloadTask = null;
            stopForeground(true);
            Toast.makeText(
                    DownloadService.this,
                    "Paused",
                    Toast.LENGTH_SHORT
            ).show();
        }

        @Override
        public void onCanceled()
        {
            mDownloadTask = null;
            stopForeground(true);
            Toast.makeText(
                    DownloadService.this,
                    "Cancelled",
                    Toast.LENGTH_SHORT
            ).show();
        }
    };

    //为了让DownloadService可以和活动进行通信，创建了这个DownloadBinder
    class DownloadBinder extends Binder
    {
        //其中提供了start_download(),pause_download()和cancel_download()这三个方法
        //分别用于开始，暂停，取消下载
        public void start_download(String url)
        {
            if (mDownloadTask == null)
            {
                download_URL = url;
                //创建一个DownloadTask的实例，把刚才的mDownloadListener作为参数传入
                mDownloadTask = new DownloadTask(mDownloadListener);
                //调用execute()开始下载
                mDownloadTask.execute(download_URL);

                //为了让这个下载服务成为一个前台服务，调用startForeground()方法，这样就会在系统状态栏中创建一个持续运行的通知了
                Notification mNotification = get_notification("Downloading...",0);
                startForeground(1,mNotification);
                Toast.makeText(
                        DownloadService.this,
                        "Downloading...",
                        Toast.LENGTH_SHORT
                ).show();
            }
        }

        public void pause_download()
        {
            if (mDownloadTask != null)
            {
                mDownloadTask.pause_download();
            }
        }

        public void cancel_download()
        {
            if (mDownloadTask != null)
            {
                mDownloadTask.cancel_download();
            }
            //取消下载时需将文件删除，并将通知关闭
          if (download_URL != null)
            {
                String directory = Environment.getExternalStoragePublicDirectory(
                        Environment.DIRECTORY_DOWNLOADS).getPath();
                String file_name = download_URL.substring(download_URL.lastIndexOf("/"));
                File file = new File(directory + file_name);
                if (file.exists())
                {
                    file.delete();
                }
                NotificationManager mNotificationManager = get_notification_manager();
                mNotificationManager.cancel(1);
                stopForeground(true);
                Toast.makeText(
                        DownloadService.this,
                        "Canceled",
                        Toast.LENGTH_SHORT
                ).show();
            }
        }
    }

    private DownloadBinder mDownloadBinder = new DownloadBinder();
    @Override
    public IBinder onBind(Intent intent)
    {
        // TODO: Return the communication channel to the service.
        return mDownloadBinder;
        //throw new UnsupportedOperationException("Not yet implemented");
    }
}
