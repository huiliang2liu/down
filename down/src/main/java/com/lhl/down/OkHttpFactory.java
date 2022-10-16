package com.lhl.down;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class OkHttpFactory implements InputStreamFactory {
    static OkHttpClient okHttpClient;

    static {
        OkHttpClient.Builder builder = new OkHttpClient.Builder();
        builder.connectTimeout(8, TimeUnit.SECONDS); // 设置连接超时时间
        builder.readTimeout(20, TimeUnit.SECONDS);// 设置读取数据超时时间
        okHttpClient = builder.build();
    }

    @Override
    public InputStream create(long start, long end, String url) {
        Request.Builder builder = new Request.Builder();
        builder.url(url);
        builder.addHeader("Range", "bytes=" + start + "-" + end);
        final Request request = builder.build();
        try {
            Response response = okHttpClient.newCall(request).execute();
            return response.body().byteStream();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
