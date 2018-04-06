package com.example.likexin.opencv;

import android.graphics.Bitmap;
import android.os.Environment;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.util.List;


/**
 * Created by likexin on 2018/3/25.
 */

public class PdfTools {
    //转pdf
    private void writePDF(List<Bitmap> bitmapList) {
        try {
            if (null != bitmapList && bitmapList.size() > 0) {
//                Document doc = new Document();
//
//                File parentPath = Environment.getExternalStorageDirectory();
//                File file = new File(parentPath.getAbsoluteFile(), System.currentTimeMillis() + "处理结果Result.pdf");
//                file.createNewFile();
//
//                FileOutputStream fos = new FileOutputStream(file);
//                PdfWriter.getInstance(doc, fos);
//                doc.open();
//                doc.setPageCount(1);
//                for (Bitmap bitmap : bitmapList) {
//                    doc.add(Image.getInstance(bitmat2Bytes(bitmap)));
//                }
//                fos.flush();
//                fos.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private byte[] bitmat2Bytes(Bitmap bitmap) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos);
        return baos.toByteArray();
    }
}
