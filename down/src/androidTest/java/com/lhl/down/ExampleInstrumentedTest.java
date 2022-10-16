package com.lhl.down;

import android.content.Context;
import android.util.Log;

import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.*;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Instrumented test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(AndroidJUnit4.class)
public class ExampleInstrumentedTest {
    private static final String TEST_URL = "https://s3.me-south-1.amazonaws.com/file.playbox.com/apk/EastBunny.apk";
    private File parent;

    @Before
    public void useAppContext() {
        // Context of the app under test.
        Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        parent = appContext.getCacheDir();
        assertEquals("com.lhl.down.test", appContext.getPackageName());
    }

    @Test
    public void testJson2down() {
        List<DownEntity> entities = new ArrayList<>();
        DownEntity entity = new DownEntity();
        entities.add(entity);
        entity.start = 0;
        entity.end = 10000;
        entity.len = 0;
        entity.pause = true;
        String json = DownManager.downEntity2json(entities);
        Log.e("======", json);
        entities = DownManager.json2downEntity(json);
        DownEntity entity1 = entities.get(0);
        assertEquals(entity.start, entity1.start);
        assertEquals(entity.end, entity1.end);
        assertEquals(entity.len, entity1.len);
        assertEquals(entity.pause, entity1.pause);
    }

    @Test
    public void down() {
        Down down = DownManager.downOkhttp(parent, TEST_URL, 3, new DownListener() {
            @Override
            public void downed(String url, File file) {
                Log.e("downed", file.getAbsolutePath());
            }

            @Override
            public void downFailure(String url) {
                Log.e("downFailure", url);
            }

            @Override
            public void onProgressChange(float progress) {
                Log.e("onProgressChange", "progress:" + progress);
            }
        });
        down.down();
    }
}