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
    public static final int TYPE_SUCCESS = 0;
    public static final int TYPE_FAILED = 1;
    public static final int TYPE_PAUSED = 2;
    public static final int TYPE_CANCELED = 3;

    private DownloadListener listener;
    private int last_progress;

    private boolean isCanceled = false;
    private boolean isPaused = false;
    public DownloadTask(DownloadListener listener)
    {
        this.listener = listener;
    }

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
    @Override
    protected Integer doInBackground(String... strings)
    {
        File file = null;
        InputStream inputStream = null;
        RandomAccessFile saved_file = null;
        try
        {
            String download_url = strings[0];
            long downloaded_length = 0;//记录已下载的文件长度

            String file_name = download_url.substring(download_url.lastIndexOf("/"));
            String directory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getPath();

            file = new File(directory + file_name);
            if (file.exists())
            {
                downloaded_length = file.length();
            }

            OkHttpClient client = new OkHttpClient();
            Request.Builder builder = new Request.Builder();
            builder.url(download_url)
                    .addHeader("RANGE","bytes=" + downloaded_length + "-");
            Request request = builder.build();
            Call call = client.newCall(request);
            Response response = call.execute();

            long content_length = get_content_length(download_url);
            if (content_length == 0)
            {
                return TYPE_FAILED;
            }
            else if (content_length == downloaded_length)
            {
                //已下载字节和文件总字节相等，说明已经下载完成了
                return TYPE_SUCCESS;
            }

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
                    if (isCanceled)
                    {
                        return TYPE_CANCELED;
                    }
                    else if(isPaused)
                    {
                        return TYPE_PAUSED;
                    }
                    else
                    {
                        total += len;
                        saved_file.write(
                                bytes,
                                0,
                                len);

                        //计算已经下载的百分比
                        int progress = (int)((total + downloaded_length) * 100 / content_length);
                        publishProgress(progress);
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

    protected void onProgressUpdate(Integer...values)
    {
        int progress = values[0];
        if (progress > last_progress)
        {
            listener.onProgress(progress);
            last_progress = progress;
        }
    }

    protected void onPostExecute(Integer status)
    {
        switch (status)
        {
            case TYPE_SUCCESS:
                listener.onSuccess();
                break;
            case TYPE_FAILED:
                listener.onFailed();
                break;
            case TYPE_PAUSED:
                listener.onPaused();
                break;
            case TYPE_CANCELED:
                listener.onCanceled();
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
