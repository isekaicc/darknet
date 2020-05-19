package com.example.darknet;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.view.View;
import android.os.Bundle;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.util.Objects;

public class MainActivity extends AppCompatActivity {
    private static final int WRITE_EXTERNAL_STORAGE = 1;
    private static final int RESULT_LOAD_IMAGE = 2;
    @SuppressLint("SdCardPath")
    private static final String listpath = "/sdcard/yolo/config/detect.lists";

    static {
        System.loadLibrary("darknetlib");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //读写权限申请
        if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)){
                ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, WRITE_EXTERNAL_STORAGE);
            } else {
                ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, WRITE_EXTERNAL_STORAGE);
            }
        }
    }

    public static void copyAssets(Context context, String AsPath, String SdPath) {
        try{
            String[] fileNames = context.getAssets().list(AsPath);
            if (Objects.requireNonNull(fileNames).length > 0) {
                File file = new File(SdPath);
                if(!file.mkdirs()) {
                    System.out.println("ERROR");
                }
                for (String fileName : fileNames) {
                    copyAssets(context, AsPath + File.separator + fileName, SdPath + File.separator + fileName);
                }
            } else {
                InputStream is = context.getAssets().open(AsPath);
                FileOutputStream fos = new FileOutputStream(new File(SdPath));
                byte[] buffer = new byte[1024];
                int byteCount;
                while ((byteCount = is.read(buffer)) != -1) {
                    fos.write(buffer, 0, byteCount);
                }
                fos.flush();
                is.close();
                fos.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void releasefile(Context context) {
        File file = new File("sdcard/yolo/config");
        if (!file.exists()){
            copyAssets(context, "labels", "sdcard/yolo/labels");
            copyAssets(context, "yolo", "sdcard/yolo/config");
        }
    }

    private String URI2RealPath(Uri uri) {
        String realpath = null;
        Cursor cursor = getContentResolver().query(uri, null, null, null, null);
        if (null != cursor && cursor.moveToFirst()) {
            int index = cursor.getColumnIndex(MediaStore.Images.Media.DATA);
            realpath = cursor.getString(index);
            cursor.close();
        }
        return realpath;
    }

    private void writepath(File lists, String imgpath) {
        try {
            RandomAccessFile writer = new RandomAccessFile(lists, "rwd");
            writer.seek(lists.length());
            writer.write(imgpath.getBytes());
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RESULT_LOAD_IMAGE && resultCode == RESULT_OK && null != data) {
            Uri uri = data.getData();
            String imgpath = URI2RealPath(uri);
            File lists = new File(listpath);
            if (lists.exists()) {
                if (!lists.delete()) {
                    return;
                }
            }
            try {
                if (!lists.createNewFile()) {
                    return;
                }
                writepath(lists, imgpath);
            } catch (IOException e) {
                e.printStackTrace();
            }
            Toast.makeText(getApplicationContext(), imgpath, Toast.LENGTH_LONG).show();
        }
    }

    public void select_picture(View view) {
        releasefile(this);
        Intent i = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(i, RESULT_LOAD_IMAGE);
    }

    //获取外置存储内所有图片，参考 https://blog.csdn.net/liuye066/article/details/78708826
    public void select_allimg(View view) {
        releasefile(this);
        File lists = new File(listpath);
        if (lists.exists()) {
            if (!lists.delete()) {
                return;
            }
        }
        try {
            if (!lists.createNewFile()) {
                return;
            }
            Cursor cursor = getContentResolver().query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, null, null, null, null);
            int flag = 0;
            if (null != cursor) {
                while (cursor.moveToNext()) {
                    int index = cursor.getColumnIndex(MediaStore.Images.Media.DATA);
                    String imgpath = cursor.getString(index);
                    File file = new File(imgpath);
                    if (file.exists() && file.length() > 1024*1024) {   //全盘检测大于1MB的图片
                        writepath(lists, imgpath + "\n");
                        flag++;
                    }
                }
                cursor.close();
            }
            Toast.makeText(getApplicationContext(),"检测数量:" + flag, Toast.LENGTH_LONG).show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public native int detectimg();

    public void testapp(View view) {
        long startTime = System.currentTimeMillis();
        int flag = detectimg();
        long endTime = System.currentTimeMillis();
        long runtime = (endTime-startTime)/10;
        Toast.makeText(getApplicationContext(),"运行时间:" + runtime*.01 + "s\n目标数量:" + flag, Toast.LENGTH_LONG).show();
    }
}
