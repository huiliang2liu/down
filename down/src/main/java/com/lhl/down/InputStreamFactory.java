package com.lhl.down;

import java.io.InputStream;

public interface InputStreamFactory {
    InputStream create(long start,long end,String url);
}
