package com.example.multi_threadbreakpointdowload1;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

import static com.example.multi_threadbreakpointdowload1.Utils.close;

public class MainActivity extends AppCompatActivity {

    private LinearLayout ll_pb_layout;
    private EditText et_threadCount;
    private EditText et_path;
    private String path;
    private int runningThread;
    private int threadCount;
    private List<ProgressBar> pbLists; // 用来存进度条的引用
    RandomAccessFile rafAccessFile = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        et_path = (EditText) findViewById(R.id.et_path);
        et_threadCount = (EditText) findViewById(R.id.et_threadCount);
        ll_pb_layout = (LinearLayout) findViewById(R.id.ll_pb);

        // 添加一个集合，用来存进度条的引用
        pbLists = new ArrayList<ProgressBar>();
    }


    // 点击按钮实现下载的逻辑
    public void onclick(View v) throws IOException {
        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
        } else {
            switch (v.getId()) {
                case R.id.btn_01:
                    runDownLoad();
                    break;
                case R.id.btn_02:
                    clearReady();
                    runDownLoad();
                    break;
            }
        }
    }

    private void clearReady() {
        for (int i = 0; i < threadCount; ++i) {
            File deleteFile = new File(Environment.getExternalStorageDirectory().getPath() + "/" + i + ".txt");
            deleteFile.delete();
        }
        new File(Environment.getExternalStorageDirectory().getPath() + "/time.txt").delete();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case 1:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    runDownLoad();
                } else {
                    Toast.makeText(this, "You denied the permission", Toast.LENGTH_SHORT).show();
                }
                break;
        }
    }

    private void runDownLoad() {
        // 获取下载的路径
        path = et_path.getText().toString().trim();
        // 获取线程的数量
        threadCount = Integer.parseInt(et_threadCount.getText().toString().trim());
        // 先移除上次进度条再添加
        ll_pb_layout.removeAllViews();
        pbLists.clear();
        for (int i = 0; i < threadCount; ++i) {
            // 把定义的item布局转换成一个View对象
            // item布局的父布局是ll_pb_layout对象对应的布局，然后false就是这个view按照子布局item的形式来
            ProgressBar pbView = (ProgressBar) LayoutInflater.from(MainActivity.this).inflate(R.layout.item, ll_pb_layout, false);

            // 把pbView添加到集合中
            pbLists.add(pbView);

            // 动态添加进度条
            ll_pb_layout.addView(pbView);
        }

        // 获取服务器文件的大小
        try {
            HttpUtils.sendOkHttpRequest(path, new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Utils.showToast(MainActivity.this, "请求失败");
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    // 获取服务器文件的大小
                    long length = response.body().contentLength();
                    // 把线程的数量赋值给正在运行的线程
                    runningThread = threadCount;
                    rafAccessFile = new RandomAccessFile(Utils.getFileName(path), "rw");
                    // 创建一个和服务器大小一样的的文件，提前申请好空间
                    rafAccessFile.setLength(length);
                    rafAccessFile.close();
                    int blockSize = (int) (length / threadCount);

                    // 计算每个线程下载的开始位置和结束位置
                    for (int i = 0; i < threadCount; ++i) {
                        int startIndex = i * blockSize; // 每个线程下载的开始位置
                        int endIndex; // 每个线程下载的结束位置
                        if (i + 1 == threadCount) { // 如果是最后一个线程
                            endIndex = (int) (length - 1);
                        } else {
                            endIndex = (i + 1) * blockSize - 1;
                        }
                        System.out.println("线程id：" + i + "理论下载的位置" + startIndex + "=========" + endIndex);
                        // 四 开启线程去服务器下载文件
                        DownLoadThread downLoadThread = new DownLoadThread(startIndex, endIndex,
                                i, path, pbLists, runningThread, threadCount);
                        downLoadThread.start();
                    }
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            close(rafAccessFile);
        }
    }
}
