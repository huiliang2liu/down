package com.lhl.down;

import org.junit.Test;

import static org.junit.Assert.*;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class ExampleUnitTest {
    private static final String TEST_URL = "https://s3.me-south-1.amazonaws.com/file.playbox.com/apk/EastBunny.apk";
    @Test
    public void addition_isCorrect() {
        assertEquals(4, 2 + 2);
        String test = "aaaaa.apk";
        System.out.println(test.substring(test.lastIndexOf('.')));
    }

    @Test
    public void end(){
        assertEquals(DownManager.end(TEST_URL),".apk");
    }

    @Test
    public void saveFile(){
        File  parent = new File("/Users/liuhuiliang/work/down");
        System.out.println(DownManager.saveFile(parent,TEST_URL));
    }

    @Test
    public void fileLength(){
        System.out.println(DownManager.fileLength(TEST_URL));
    }

    @Test
    public void progressFormat(){
        System.out.println(DownManager.progressFormat(10.1154f));
        assertTrue(10.11f==DownManager.progressFormat(10.111f));
    }
    
}