package com.lhl.down;

import java.io.File;

public interface DownListener {
    void downed(String url, File file);

    void downFailure(String url);

    void onProgressChange(float progress);
}
