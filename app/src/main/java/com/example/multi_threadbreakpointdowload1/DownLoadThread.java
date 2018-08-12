package com.example.multi_threadbreakpointdowload1;

import android.os.Environment;
import android.widget.ProgressBar;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import static com.example.multi_threadbreakpointdowload1.Utils.close;

// 定义线程去服务器下载文件
public class DownLoadThread extends Thread {
    // 通过构造方法把每个线程下载的开始和结束位置传进来
    private int startIndex;
    private int endIndex;
    private int threadId;
    private String path;
    private int PbMaxSize; // 代表当前线程下载的最大值
    private int pblastPositon; // 如果中断过，获取上次下载的位置
    private List<ProgressBar> pbLists; // 用来存进度条的引用

    private int runningThread;
    private int threadCount;
    InputStream in = null;
    RandomAccessFile raf = null;
    BufferedReader br = null;
    RandomAccessFile raff = null;
    RandomAccessFile breakpoint = null;

    public DownLoadThread(int startIndex, int endIndex, int threadId,
                          String path, List<ProgressBar> pbLists, int runningThread, int threadCount) {
        this.startIndex = startIndex;
        this.endIndex = endIndex;
        this.threadId = threadId;
        this.path = path;
        this.pbLists = pbLists;
        this.runningThread = runningThread;
        this.threadCount = threadCount;
    }


    @Override
    public void run() {
        try {
            // 计算当前进度条的最大值
            PbMaxSize = endIndex - startIndex;
            File file = new File(Environment.getExternalStorageDirectory().getPath() + "/" + threadId + ".txt");
            // ======这个条件一定要在请求头前面，不然会导致原文件损坏，因为请求的开始不对==========
            if (file.exists() && file.length() > 0) {
                br = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
                String lastPosition = br.readLine(); // 读出来的内容就是上次下载保存的位置
                int last = Integer.parseInt(lastPosition);
                // 给我们定义的进度条位置赋值
                pblastPositon = last - startIndex;
                // 要改变一下startIndex位置
                startIndex = last;
                System.out.println("线程id：" + threadId + "真实下载的位置" + startIndex + "=========" + endIndex);
                br.close();
                if (startIndex >= endIndex) return;
            }

            //HttpURLConnection conn = Utils.connectNetSettings(path);
            OkHttpClient client = new OkHttpClient();
            // 设置一个请求头Range，作用是告诉服务器每个线程下载的开始和结束位置
            Request request = new Request.Builder()
                    .url(path)
                    .addHeader("Range", "bytes=" + startIndex + "-" + endIndex)
                    .build();
            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    e.printStackTrace();
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {

                    if (response.code() == 206) {
                        // 创建随机读写文件对象
                        raf = new RandomAccessFile(Utils.getFileName(path), "rw");
                        // 每个线程要从自己的位置开始写
                        raf.seek(startIndex);
                        // 存的是abc.exe
                        in = response.body().byteStream();
                        // 把数据写到文件中
                        int len = -1;
                        byte[] buffer = new byte[2 * 1024 * 1024];

                        int total = 0; // 代表当前线程下载的大小
                        // 下面这句不要写在while里面，避免重复关联文件导致文件无法删除
                        raff = new RandomAccessFile(Environment.getExternalStorageDirectory().getPath() + "/" + threadId + ".txt", "rwd");
                        while ((len = in.read(buffer)) != -1) {
                            raf.write(buffer, 0, len);
                            total += len;
                            // 实现断点续传，就是把当前线程下载的位置存起来
                            // 下次再下载的时候，就是按照上次下载的位置继续下载就行
                            int currentThreadPosition = startIndex + total;
                            // 用FileOuputStream可能因为突然停止导致不能立即写到硬盘
                            raff.seek(0);// 避免每次写数据不断往后添加
                            raff.write(String.valueOf(currentThreadPosition).getBytes());
                            // 设置当前进度条的最大值和当前进度
                            pbLists.get(threadId).setMax(PbMaxSize); // 设置进度条的最大值
                            pbLists.get(threadId).setProgress(pblastPositon + total); // 设置当前进度条的当前进度
                        }
                        raff.close();
                        raf.close();
                        in.close();
                        System.out.println("线程id：" + threadId + "下载完成");
                        synchronized (DownLoadThread.class) {
                            breakpoint = new RandomAccessFile(Environment.getExternalStorageDirectory().getPath() + "/time.txt", "rwd");
                            breakpoint.seek(0); // 准备从time.txt开头读取未下载完成的线程个数
                            String s = null;
                            if ((s = breakpoint.readLine()) != null) {
                                runningThread = Integer.valueOf(s);
                            }
                            --runningThread;
                            breakpoint.seek(0); // 尝试读取后文件指针变化，再设置为0，从0处开始写入
                            breakpoint.write(String.valueOf(runningThread).getBytes());
                            breakpoint.close();
                            if (runningThread == 0) {
                                for (int i = 0; i < threadCount; ++i) {
                                    File deleteFile = new File(Environment.getExternalStorageDirectory().getPath() + "/" + i + ".txt");
                                    System.out.println(deleteFile.toString());
                                    deleteFile.delete();
                                }
                                new File(Environment.getExternalStorageDirectory().getPath() + "/time.txt").delete();
                            }
                        }
                    }
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            close(raff);
            close(raf);
            close(in);
            close(br);
        }
    }
}