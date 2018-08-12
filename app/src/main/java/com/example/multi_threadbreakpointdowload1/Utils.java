package com.example.multi_threadbreakpointdowload1;

import android.content.Context;
import android.os.Environment;
import android.support.v4.content.ContextCompat;
import android.widget.Toast;

import java.net.HttpURLConnection;
import java.net.URL;

import okhttp3.Callback;

public class Utils {
    /*    static HttpURLConnection connectNetSettings(String path) throws Exception {
            URL url = new URL(path);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);
            return conn;
        }*/
    static void showToast(Context context, String s) {
        Toast.makeText(context, s, Toast.LENGTH_SHORT).show();
    }

    static String getFileName(String path) {
        int index = path.lastIndexOf("/") + 1;
        return Environment.getExternalStorageDirectory().getPath() + "/" + path.substring(index);
    }

    static <T extends java.io.Closeable> void close(T t) {
        try {
            if (t != null) {
                t.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
