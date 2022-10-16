package com.lhl.down;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import java.io.File;

public class MainActivity extends AppCompatActivity {
    private static final String TEST_URL = "https://s3.me-south-1.amazonaws.com/file.playbox.com/apk/EastBunny.apk";

    Down down;

    @SuppressLint("SuspiciousIndentation")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        File file_ = new File(getCacheDir(), "apk");
        File[] files = file_.listFiles();
        if (files != null)
            for (File f : files)
                Log.e("======", f.getAbsolutePath());
        down = DownManager.downURl(file_, TEST_URL, 3, new DownListener() {
            @Override
            public void downed(String url, File file) {
                Log.e("downed", file.getAbsolutePath());
                startActivity(install(file, getApplicationContext(), getPackageName() + ".owen.fileprovider"));
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
    }

    public static Intent install(File file, Context context, String authority) {
//        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
//
//        }
        // 安装应用
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        if (Build.VERSION.SDK_INT >= 24) { //判读版本是否在7.0以上
            //参数1 上下文, 参数2 Provider主机地址 和配置文件中保持一致   参数3  共享的文件
            Uri apkUri =
                    FileProvider.getUriForFile(context, authority, file);
            //添加这一句表示对目标应用临时授权该Uri所代表的文件
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent.setDataAndType(apkUri, "application/vnd.android.package-archive");
        } else {
            intent.setDataAndType(Uri.fromFile(file),
                    "application/vnd.android.package-archive");
        }
        return intent;
    }


    public void down(View view) {
        if (down.isDown())
            down.pause();
        else
            down.down();
    }
}