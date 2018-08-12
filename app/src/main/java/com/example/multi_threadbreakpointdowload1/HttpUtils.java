package com.example.multi_threadbreakpointdowload1;

import okhttp3.OkHttpClient;
import okhttp3.Request;

public class HttpUtils {
    public static void sendOkHttpRequest(String path, okhttp3.Callback callback) {
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url(path)
                .build();
        client.newCall(request).enqueue(callback);
    }
}
