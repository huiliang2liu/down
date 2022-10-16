package com.lhl.down;

import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class DownManager {
    private static InputStreamFactory urlFactory = new URLConnectionFactory();
    private static InputStreamFactory okhttp = new OkHttpFactory();
    static ExecutorService executorService;

    static {
        int threadNum = Runtime.getRuntime().availableProcessors() * 3;
        threadNum = threadNum << 1;
        executorService = new ThreadPoolExecutor(threadNum, threadNum << 1, 1L, TimeUnit.MINUTES, new LinkedBlockingQueue<Runnable>());
    }

    static String end(String url) {
        int index = url.lastIndexOf('.');
        if (index < 0)
            return "";
        return url.substring(index);
    }

    static File saveFile(File parent, String url) {
        return new File(parent, String.format("%s%s", url.hashCode(), end(url)));
    }

    static long fileLength(String url) {
        try {
            HttpURLConnection connection = (HttpURLConnection) new URL(url)
                    .openConnection();
            long fileLen = -1;
            if (Build.VERSION.SDK_INT >= 24)
                fileLen = connection.getContentLengthLong();
            else
                fileLen = connection.getContentLength();
            return fileLen;
        } catch (IOException e) {
            return -1;
        }
    }

    static float progressFormat(float progress) {
        return new BigDecimal(progress).setScale(2, BigDecimal.ROUND_DOWN).floatValue();
    }

    static File saveProgress(File parent, String url) {
        return new File(parent, String.format("%s.xml", url.hashCode()));
    }

    static File temporaryFile(File prent, String url) {
        return new File(prent, String.valueOf(url.hashCode()));
    }

    static String downEntity2json(List<DownEntity> entities) {
        JSONArray array = new JSONArray();
        try {
            for (DownEntity entity : entities) {
                JSONObject object = new JSONObject();
                object.put("start", entity.start);
                object.put("end", entity.end);
                object.put("len", entity.len);
                array.put(object);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return array.toString();
    }

    static List<DownEntity> json2downEntity(String json) {
        List<DownEntity> entities = new ArrayList<>();
        try {
            JSONArray array = new JSONArray(json);
            for (int i = 0; i < array.length(); i++) {
                JSONObject object = array.optJSONObject(i);
                DownEntity entity = new DownEntity();
                entities.add(entity);
                entity.start = object.optLong("start");
                entity.end = object.optLong("end");
                entity.len = object.optLong("len");
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return entities;
    }

    static boolean saveEntity(File file, List<DownEntity> entities) {
        OutputStream os = null;
        String json = downEntity2json(entities);
        try {
            os = new FileOutputStream(file);
            os.write(json.getBytes());
            return true;
        } catch (Exception e) {
            return false;
        } finally {
            if (os != null)
                try {
                    os.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
        }
    }

    static void removeFile(File file) {
        if (file == null)
            return;
        if (file.exists())
            file.delete();
    }

    static List<DownEntity> file2entity(File file) {
        if (!file.exists())
            return new ArrayList<>();
        InputStream inputStream = null;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buff = new byte[1024 * 1024];
        int len;
        try {
            inputStream = new FileInputStream(file);
            while ((len = inputStream.read(buff)) > 0)
                baos.write(buff, 0, len);
            String json = new String(baos.toByteArray());
            return json2downEntity(json);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                baos.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (inputStream != null)
                try {
                    inputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
        }
        return new ArrayList<>();
    }

    public static boolean isDown(File parent, String url) {
        File file = saveFile(parent, url);
        return file.exists();
    }

    public static boolean isDowning(File parent, String url) {
        File file = temporaryFile(parent, url);
        if (!file.exists())
            return false;
        file = saveProgress(parent, url);
        return file.exists();
    }

    public static Down down(File parent, String url, int threads, ExecutorService service, DownListener listener, InputStreamFactory factory) {
        return new DownImpl(parent, url, threads, service, new DownListener() {
            private WeakReference<DownListener> downListener = new WeakReference(listener);
            private Handler handler = new Handler(Looper.getMainLooper());

            @Override
            public void downed(String url, File file) {
                DownListener dl = downListener.get();
                if (dl == null)
                    return;
                handler.post(() -> {
                    dl.downed(url, file);
                });
            }

            @Override
            public void downFailure(String url) {
                DownListener dl = downListener.get();
                if (dl == null)
                    return;
                handler.post(() -> {
                    dl.downFailure(url);
                });
            }

            @Override
            public void onProgressChange(float progress) {
                DownListener dl = downListener.get();
                if (dl == null)
                    return;
                handler.post(() -> {
                    dl.onProgressChange(progress);
                });
            }
        }, factory);
    }

    public static Down down(File parent, String url, ExecutorService service, DownListener listener, InputStreamFactory factory) {
        return down(parent, url, 1, service, listener, factory);
    }

    public static Down down(File parent, String url, int threads, DownListener listener, InputStreamFactory factory) {
        return down(parent, url, threads, executorService, listener, factory);
    }

    public static Down down(File parent, String url, DownListener listener, InputStreamFactory factory) {
        return down(parent, url, 1, executorService, listener, factory);
    }

    public static Down downURl(File parent, String url, ExecutorService service, DownListener listener) {
        return down(parent, url, 1, service, listener, urlFactory);
    }

    public static Down downURl(File parent, String url, int threads, DownListener listener) {
        return down(parent, url, threads, executorService, listener, urlFactory);
    }

    public static Down downURl(File parent, String url, DownListener listener) {
        return down(parent, url, 1, executorService, listener, urlFactory);
    }


    public static Down downOkhttp(File parent, String url, ExecutorService service, DownListener listener) {
        return down(parent, url, 1, service, listener, okhttp);
    }

    public static Down downOkhttp(File parent, String url, int threads, DownListener listener) {
        return down(parent, url, threads, executorService, listener, okhttp);
    }

    public static Down downOkhttp(File parent, String url, DownListener listener) {
        return down(parent, url, 1, executorService, listener, okhttp);
    }


}
