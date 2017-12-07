package com.example.lxd.servicebestpractice;

import android.os.AsyncTask;
import android.os.Environment;

import java.io.File;
import java.io.InputStream;
import java.io.RandomAccessFile;

import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Created by lxd on 2017/12/5.
 */

public class DownloadTask extends AsyncTask<String,Integer,Integer>
{
    //3个泛型参数：
    // 第一个泛型参数指定为String,表示在执行AsyncTask的时候需要传入一个字符串参数给后台任务
    //第二个泛型参数指定为Integer，表示使用整型数据来作为进度显示单位
    //第三个泛型参数指定为Integer，表示使用整型数据来反馈执行结果

    public static final int TYPE_SUCCESS = 0;//表示下载成功
    public static final int TYPE_FAILED  = 1;//表示下载失败
    public static final int TYPE_PAUSED  = 2;//表示暂停下载
    public static final int TYPE_CANCELED = 3;//表示取消下载

    private DownloadListener listener;
    private int last_progress;

    private boolean isCanceled = false;
    private boolean isPaused = false;
    public DownloadTask(DownloadListener listener)
    {
        //构造函数中传入一个刚刚定义的DownloadListener参数，待会就会将下载的状态通过这个参数进行回调
        this.listener = listener;
    }

    //调用get_content_length()方法来获取待下载文件的总长度
    private long get_content_length(String download_URL)
    {
        try
        {
            OkHttpClient client = new OkHttpClient();

            Request.Builder builder = new Request.Builder();
            builder.url(download_URL);
            Request request = builder.build();

            Call call  = client.newCall(request);

            Response response = call.execute();
            if (response != null && response.isSuccessful())
            {
                long content_length = response.body().contentLength();
                response.body().close();
                return content_length;
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        return 0;
    }

    //用于在后台执行具体的下载逻辑
    protected Integer doInBackground(String... strings)
    {
        File file = null;
        InputStream inputStream = null;
        RandomAccessFile saved_file = null;
        try
        {
            String download_url = strings[0];//从参数中获取到了下载的URL地址
            long downloaded_length = 0;//记录已下载的文件长度

            //download_url = https:raw.githubusercontent.com/guolindev/eclipse/master/eclipse-inst-win64.exe
            //file_name = /eclipse-inst-win64.exe
            String file_name = download_url.substring(download_url.lastIndexOf("/"));//根据URL地址解析出了下载的文件名

            //指定将文件下载到Environment.DIRECTORY_DOWNLOADS目录下，也就是SD卡的Download目录.
            String directory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getPath();

            //通过将 给定路径名字符串 转换成抽象路径名来创建一个新 File 实例
            //File(String pathname)
            file = new File(directory + file_name);

            //还要判断一下Download目录中是不是存在要下载的文件了
            if (file.exists())
            {
                //如果已经存在的话则读取已下载的字节数，这样就可以在后面启用断点续传的功能
                downloaded_length = file.length();
            }

            //调用get_content_length()方法来获取待下载文件的总长度
            long content_length = get_content_length(download_url);
            if (content_length == 0)
            {
                //如果文件长度等于0则说明文件有问题，直接返回TYPE_FAILED
                return TYPE_FAILED;
            }
            else if (content_length == downloaded_length)
            {
                //已下载字节和文件总字节相等，说明已经下载完成了
                return TYPE_SUCCESS;
            }

            OkHttpClient client = new OkHttpClient();
            Request.Builder builder = new Request.Builder();
            //在请求中添加了一个header,用于告诉服务器我们想要从哪个字节开始下载，因为已经下载过的部分就不需要再重新下载了。
            builder.url(download_url)
                    .addHeader("RANGE","bytes=" + downloaded_length + "-");
            //range:范围
            Request request = builder.build();
            Call call = client.newCall(request);
            Response response = call.execute();

            if (response != null)
            {
                inputStream = response.body().byteStream();
                saved_file = new RandomAccessFile(file,"rw");
                saved_file.seek(downloaded_length);//跳过已下载的字节

                byte[] bytes = new byte[1024];
                int total = 0;
                int len;
                while ( (len = inputStream.read(bytes)) != -1);
                {
                    if (isCanceled)//判断用户有没有触发取消操作
                    {
                        return TYPE_CANCELED;
                    }
                    else if(isPaused)//判断用户有没有触发暂停操作
                    {
                        return TYPE_PAUSED;
                    }
                    else
                    {
                        total += len;
                        saved_file.write(//不断从网络上读取数据，不断写入到本地
                                bytes,
                                0,
                                len);

                        //实时计算已经下载的百分比
                        int progress = (int)((total + downloaded_length) * 100 / content_length);
                        publishProgress(progress);//进行下载进度更新
                    }
                }
                response.body().close();
                return TYPE_SUCCESS;
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        finally
        {
            try
            {
                if (inputStream != null)
                {
                    inputStream.close();
                }
                if (saved_file != null)
                {
                    saved_file.close();
                }
                if (isCanceled && (file != null))
                {
                    file.delete();
                }
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }
        return TYPE_FAILED;
    }

    //用于在界面上更新当前的下载进度
    protected void onProgressUpdate(Integer...values)
    {
        int progress = values[0];//从参数中获取到当前的下载进度
        if (progress > last_progress)//和上一次的下载进度进行对比
        {
            listener.onProgress(progress);//如果有变化的话，则调用DownloadListener的onProgress（）方法来通知下载进度更新
            last_progress = progress;
        }
    }

    //用于通知最终的下载结果
    protected void onPostExecute(Integer status)
    {
        //根据参数中传入的下载状态来进行回调
        switch (status)
        {
            case TYPE_SUCCESS:
                listener.onSuccess();//下载成功就调用onSuccess（）方法
                break;
            case TYPE_FAILED:
                listener.onFailed();//下载失败就调用onFailed（）方法
                break;
            case TYPE_PAUSED:
                listener.onPaused();//暂停下载就调用onPaused（）方法
                break;
            case TYPE_CANCELED:
                listener.onCanceled();//取消下载就调用onCanceled()方法
                break;
            default:
                break;
        }
    }

    public void pause_download()
    {
        isPaused = true;
    }

    public void cancel_download()
    {
        isCanceled = true;
    }
}
