package com.lhl.down;


import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;

class DownImpl implements Down {
    private int state = 0;//0表示没有下载，1表示下载中，2表示下载暂停了，3表示下载完成
    private File parent;
    private String url;
    private int threads;
    private ExecutorService executorService;
    private DownListener listener;
    private boolean init = false;
    private File saveFile;
    private File tem;
    private File progressFile;
    private long fileLen = 0;
    private long downLen = 0;
    private List<DownEntity> entities;
    private InputStreamFactory factory;
    private boolean error = false;
    private Runnable initRun = new Runnable() {
        @Override
        public void run() {
            synchronized (DownImpl.this) {
                saveFile = DownManager.saveFile(parent, url);
                if (saveFile.exists()) {//下载完成
                    state = 3;
                    init = true;
                    listener.downed(url, saveFile);
                    return;
                }
                tem = DownManager.temporaryFile(parent, url);
                progressFile = DownManager.saveProgress(parent, url);
                if (tem.exists()) {//临时文件存在，可能在下载中
                    if (progressFile.exists()) {
                        entities = DownManager.file2entity(progressFile);
                        if (entities.size() > 0) {
                            state = 2;
                            for (DownEntity entity : entities) {
                                if (entity.end > fileLen)
                                    fileLen = entity.end;
                                downLen += entity.len;
                            }
                            listener.onProgressChange(DownManager.progressFormat(downLen * 100 * 1.0f / fileLen));
                            init = true;
                            return;
                        }
                    }
                }
                DownManager.removeFile(progressFile);
                DownManager.removeFile(tem);
                //没下载过或者下载过但是中间文件或者进度文件删除了
                fileLen = DownManager.fileLength(url);
                if (fileLen <= 0) {//获取文件长度失败
                    init = true;
                    return;
                }
                entities = new ArrayList<>();
                long dx = fileLen / threads;
                for (int i = 0; i < threads; i++) {
                    DownEntity entity = new DownEntity();
                    entity.start = dx * i;
                    entity.end = entity.start + dx;
                    entities.add(entity);
                }
                entities.get(threads - 1).end = fileLen;
                if (state == 1) {
                    down_();
                }
                init = true;
            }
        }
    };

    public DownImpl(File parent, String url, int threads, ExecutorService service, DownListener listener, InputStreamFactory factory) {
        this.parent = parent;
        if (!parent.exists())
            parent.mkdirs();
        this.url = url;
        this.threads = threads;
        this.executorService = service;
        this.listener = listener;
        this.factory = factory;
        service.submit(initRun);
    }

    @Override
    public synchronized void down() {
        if (state == 3) {
            listener.downed(url, saveFile);
            return;
        }
        if (state == 1)
            return;
        state = 1;
        if (init)
            down_();
    }

    private void down_() {
        error = false;
        if (fileLen <= 0) {
            listener.downFailure(url);
            return;
        }
        for (DownEntity entity : entities) {
            executorService.submit(new DownRunnable(entity));
        }
    }

    @Override
    public synchronized void pause() {
        if (state == 3) {
            listener.downed(url, saveFile);
            return;
        }
        if (state == 2)
            return;
        state = 2;
    }

    @Override
    public boolean isPause() {
        return state == 2;
    }

    @Override
    public boolean isDown() {
        return state == 1;
    }

    @Override
    public boolean isEnd() {
        return state == 3;
    }

    @Override
    public float progress() {
        if (!init)
            return 0.0f;
        if (fileLen <= 0)
            return 0.0f;
        return DownManager.progressFormat(downLen * 100 * 1.0f / fileLen);
    }


    private synchronized void save() {
        boolean pause = true;
        for (DownEntity entity : entities)
            pause &= entity.pause;
        if (!pause)
            return;
//        for (DownEntity entity : entities)
//            Log.e("save=====", "entity.start:" + entity.start + " entity.end:" + entity.end + "  entity.len:" + entity.len);
        if (error) {
            listener.downFailure(url);
            state = 2;
            DownManager.saveEntity(progressFile, entities);
            return;
        }
        if (downLen < fileLen) {
            state = 2;
            DownManager.saveEntity(progressFile, entities);
            return;
        }
        state = 3;
        tem.renameTo(saveFile);
        DownManager.removeFile(progressFile);
        listener.downed(url, saveFile);
    }

    private float pro;

    private synchronized void downLenChange(long len) {
        downLen += len;
        float progress = downLen * 100.0f / fileLen;
        if (progress - pro < 0.01f)
            return;
        pro = progress;
        listener.onProgressChange(DownManager.progressFormat(progress));
    }


    private class DownRunnable implements Runnable {
        private DownEntity entity;

        DownRunnable(DownEntity entity) {
            entity.pause = false;
            this.entity = entity;
        }

        @Override
        public void run() {
            if (state != 1) {
                entity.pause = true;
                save();
                return;
            }
//            Log.e("=====", "entity.start:" + entity.start + " entity.end:" + entity.end + "  entity.len:" + entity.len);
            if (entity.start >= entity.end) {
//                Log.e("======", "下载完成");
                entity.pause = true;
                save();
                return;
            }
            RandomAccessFile raf = null;
            InputStream stream = factory.create(entity.start, entity.end, url);
            if (stream == null) {
                entity.pause = true;
                error = true;
                save();
                return;
            }
            byte[] buff = new byte[1024 * 1024];
            int len;
            try {
                synchronized (tem) {
                    raf = new RandomAccessFile(tem, "rwd");
                }
                raf.seek(entity.start);
                while (state == 1 && (len = stream.read(buff)) > 0) {
                    downLenChange(len);
                    synchronized (entity) {
                        entity.start += len;
                        entity.len += len;
                    }
                    raf.write(buff, 0, len);
                }
                entity.pause = true;
                save();
            } catch (Exception e) {
                entity.pause = true;
                error = true;
                save();
            } finally {
                try {
                    stream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                if (raf != null)
                    try {
                        raf.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
            }
        }
    }
}
