package com.example.lxd.servicebestpractice;

import android.Manifest;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.IBinder;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity
{
    Button start_download;
    Button pause_download;
    Button cancel_download;

    private DownloadService.DownloadBinder downloadBinder;
    private ServiceConnection serviceConnection = new ServiceConnection()
    {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service)
        {
            downloadBinder = (DownloadService.DownloadBinder) service;
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName)
        {

        }
    };

    public void onRequestPermissionsResult(int request_code,String[] permissions, int[] grant_results)
    {
        switch (request_code)
        {
            case 1:
                if (grant_results.length > 0 && grant_results[0] != PackageManager.PERMISSION_GRANTED)
                {
                    Toast.makeText(
                            MainActivity.this,
                            "拒绝权限将无法使用程序",
                            Toast.LENGTH_SHORT
                    ).show();
                    finish();
                }
                break;
                default:
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Intent intent = new Intent(this,DownloadService.class);
        startService(intent);//启动服务
        bindService(intent,serviceConnection,BIND_AUTO_CREATE);//绑定服务

        //以下代码进行了WRITE_EXTERNAL_STORAGE的运行时权限申请，因为下载文件是要下载到SD 卡的Download目录下的。
        int permission = ContextCompat.checkSelfPermission(
                MainActivity.this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (permission != PackageManager.PERMISSION_GRANTED)
        {
            ActivityCompat.requestPermissions(
                    MainActivity.this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    1);
        }

        start_download = findViewById(R.id.start_download);
        start_download.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                String url = "https:raw.githubusercontent.com/guolindev/eclipse/master/eclipse-inst-win64.exe";
                downloadBinder.start_download(url);
            }
        });

        pause_download = findViewById(R.id.pause_download);
        pause_download.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                downloadBinder.pause_download();
            }
        });

        cancel_download = findViewById(R.id.cancel_download);
        cancel_download.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                downloadBinder.cancel_download();
            }
        });
    }

    protected void onDestroy()
    {
        super.onDestroy();
        unbindService(serviceConnection);
    }
}
