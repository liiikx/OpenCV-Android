package com.example.likexin.opencv;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;

/**
 * Created by likexin on 2018/4/1.
 */

public class ImageTools {
    public static Bitmap getColorImage(Bitmap bitmap, float sx, float bhd, float ld) {// 参数分别是色相，饱和度和亮度
        Bitmap bmp = bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bmp);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        ColorMatrix sxMatrix = new ColorMatrix();// 设置色调
        sxMatrix.setRotate(0, sx);
        sxMatrix.setRotate(1, sx);
        sxMatrix.setRotate(2, sx);

        ColorMatrix bhdMatrix = new ColorMatrix();// 设置饱和度
        bhdMatrix.setSaturation(bhd);

        ColorMatrix ldMatrix = new ColorMatrix();// 设置亮度
        ldMatrix.setScale(ld, ld, ld, 1);

        ColorMatrix mixMatrix = new ColorMatrix();// 设置整体效果
        mixMatrix.postConcat(sxMatrix);
        mixMatrix.postConcat(bhdMatrix);
        mixMatrix.postConcat(ldMatrix);
        paint.setColorFilter(new ColorMatrixColorFilter(mixMatrix));// 用颜色过滤器过滤

        canvas.drawBitmap(bitmap, 0, 0, paint);// 重新画图
        return bmp;
    }
}
