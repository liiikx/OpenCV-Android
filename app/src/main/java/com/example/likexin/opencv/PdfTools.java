package com.example.likexin.opencv;

import android.app.DownloadManager;
import android.content.Context;
import android.net.Uri;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;


/**
 * Created by likexin on 2018/3/25.
 */

public class PdfTools {
    //保存pdf文件
    public static void savePDF(byte[] bytes, String path, String name, Context context) {
        BufferedOutputStream bufferedOutputStream = null;
        FileOutputStream fileOutputStream = null;

        try {
            File catalog = new File(path);
            if (!catalog.exists()) {
                catalog.mkdirs();
            }
            File file = new File(path, name);
            if (file.exists()) {
                file.delete();
            }

            fileOutputStream = new FileOutputStream(file);
            bufferedOutputStream = new BufferedOutputStream(fileOutputStream);
            //写入SD卡
            bufferedOutputStream.write(bytes);

//            fileOutputStream = context.openFileOutput(name, Context.MODE_PRIVATE);
//            fileOutputStream.write(bytes);

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (bufferedOutputStream != null) {
                try {
                    bufferedOutputStream.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            if (fileOutputStream != null) {
                try {
                    fileOutputStream.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
