package com.lhl.down;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class URLConnectionFactory implements InputStreamFactory {
    @Override
    public InputStream create(long start, long end, String url) {
        try {
            HttpURLConnection connection = (HttpURLConnection) new URL(url)
                    .openConnection();
            connection.setReadTimeout(20000);
            connection.setConnectTimeout(8000);
            connection.setRequestProperty("Range", String.format("bytes=%s-%s", start, end));
            connection.connect();
            if (connection.getResponseCode() < 300)
                return connection.getInputStream();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}
