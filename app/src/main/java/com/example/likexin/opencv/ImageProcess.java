package com.example.likexin.opencv;

import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.RotatedRect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Vector;

import static org.opencv.core.Core.BORDER_DEFAULT;
import static org.opencv.core.CvType.CV_32F;
import static org.opencv.core.CvType.CV_8UC3;
import static org.opencv.imgproc.Imgproc.Canny;
import static org.opencv.imgproc.Imgproc.HoughLinesP;
import static org.opencv.imgproc.Imgproc.INTER_LINEAR;
import static org.opencv.imgproc.Imgproc.MORPH_CLOSE;
import static org.opencv.imgproc.Imgproc.boundingRect;
import static org.opencv.imgproc.Imgproc.line;
import static org.opencv.imgproc.Imgproc.rectangle;

/**
 * Created by likexin on 2018/4/6.
 */

public class ImageProcess {
    //灰度
    public static Mat grayImg(Mat src) {
        Mat grayTemp = new Mat();
        Mat dst = new Mat();
        Imgproc.cvtColor(src, grayTemp, Imgproc.COLOR_BGRA2BGR);
        Imgproc.cvtColor(grayTemp, dst, Imgproc.COLOR_BGR2GRAY);
        return dst;
    }

    //双边滤波
    public static Mat bilateralFilterImg(Mat src) {
        Mat dst = new Mat(src.size(), src.type());
        Imgproc.bilateralFilter(src, dst, 10, 75, 75);
        return dst;
    }

    //二值化
    public static Mat thresholdImg(Mat src) {
        Mat dst = new Mat();
        Imgproc.threshold(src, dst, 100, 255, Imgproc.THRESH_BINARY);
        return dst;
    }

    //闭运算  先膨胀再腐蚀
    public static Mat closingImg(Mat src) {
        Mat dst = new Mat();
        Mat structuringElement = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(15, 15));//调参
        Imgproc.morphologyEx(src, dst, MORPH_CLOSE, structuringElement);
        return dst;
    }

    //Canny算子 边缘检测
    public static Mat cannyImg(Mat src) {
        Mat dst = new Mat();
        double threshold = 128;
        Canny(src, dst, threshold, threshold * 3, 3, true);
        return dst;
    }

    //Hough变换
    public static Mat houghImg(Mat src) {
        Mat dst = src;
        Mat lines = new Mat();
        HoughLinesP(dst, lines, 1, Math.PI / 180, 50, 50.0, 10.0);
        int[] a = new int[(int) lines.total() * lines.channels()]; //数组a存储检测出的直线端点坐标
        lines.get(0, 0, a);
        for (int i = 0; i < a.length; i += 4) {
            line(dst, new Point(a[i], a[i + 1]), new Point(a[i + 2], a[i + 3]), new Scalar(255, 0, 255), 4);
        }
        return dst;
    }

    //轮廓检测
    public static Vector<Rect> findContours(Mat origin, Mat src) {
        List<MatOfPoint> contours = new ArrayList<>();
        Mat hierarchy = new Mat();
        //轮廓检测
        Imgproc.findContours(src, contours, hierarchy, Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE);

        List<MatOfPoint2f> newContours = new ArrayList<>();
        for (MatOfPoint point : contours) {
            MatOfPoint2f newPoint = new MatOfPoint2f(point.toArray());
            newContours.add(newPoint);
        }
        //绘制轮廓
        Mat result = new Mat(src.size(), CV_8UC3, new Scalar(0));
        Imgproc.drawContours(result, contours, -1, new Scalar(255, 255, 255), 1);

        //boundRect存储计算得到的最小立式矩形
        Vector<Rect> boundRect = new Vector<>();
        boundRect.setSize(contours.size());

        for (int i = 0; i < contours.size(); i++) {
            // 计算最小外接立式矩形
            boundRect.set(i, boundingRect(contours.get(i)));
        }

        for (int i = 0; i < contours.size(); i++) {
            Scalar color = new Scalar(0, 255, 255);
            // 绘制最小外接立式矩形
            rectangle(origin, boundRect.get(i).tl(), boundRect.get(i).br(), color, 5, 8, 0);
        }
        return boundRect;
    }

    /**
     * 倾斜校正
     *
     * @param src
     * @return
     */
    public static Mat tiltCorrectionImg(Mat src) {
        Mat gray = grayImg(src);
        Core.bitwise_not(gray, gray);

        Mat threshold = thresholdImg(gray);

        List<MatOfPoint> contours = new ArrayList<>();
        Mat hierarchy = new Mat();
        //轮廓检测
        Imgproc.findContours(threshold, contours, hierarchy, Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE);

        RotatedRect rect = Imgproc.minAreaRect(new MatOfPoint2f(contours.get(0).toArray()));
        double agvAngle = rect.angle;
        if (agvAngle < -45) {
            agvAngle = -(90 + agvAngle);
        } else {
            agvAngle = -agvAngle;
        }

        int width = threshold.width();
        int height = threshold.height();
        Point center = new Point(width / 2, height / 2);

        Mat M = Imgproc.getRotationMatrix2D(center, agvAngle, 1.0);
        Mat dst = new Mat();
        Imgproc.warpAffine(src, dst, M, new Size(height, width));

        return dst;
    }

    public static List<Mat> cut(Mat src, Vector<Rect> boundRect) {
        List<Mat> result = new ArrayList<>();

        Collections.sort(boundRect, new Comparator<Rect>() {
            @Override
            public int compare(Rect t2, Rect t1) {
                return Long.valueOf(t1.width * t1.height).compareTo(Long.valueOf(t2.width * t2.height));
            }
        });

        Mat clone = src.clone();
        Rect rect = boundRect.get(0);
        Mat submat = clone.submat(new Rect(rect.x + 20, rect.y - 10, rect.width + 100, rect.height));
        Mat temp = new Mat();
        submat.copyTo(temp);
        result.add(temp);

        return result;
    }

    public static Mat resizeImg(Mat origin, Vector<Rect> boundRect) {
        Mat dst = new Mat();
        List<Mat> cutResult = cut(origin, boundRect);
        Mat mat = cutResult.get(0);

        Size size = new Size(1600, 1200);
        Imgproc.resize(mat, dst, size, 0, 0, INTER_LINEAR);
        return dst;
    }

    //均值滤波
//    private void blur() {
//        Mat src = new Mat();
//        Mat dst = new Mat();
//
//        Utils.bitmapToMat(selectbp, src);
//
//        Imgproc.blur(src, dst, new Size(9, 9));//均值滤波
//
//        Utils.matToBitmap(dst, selectbp);
//        myImageView.setImageBitmap(selectbp);
//    }
//
//    //高斯滤波
//    private void gaussianBlur() {
//        Mat src = new Mat();
//        Mat dst = new Mat();
//
//        Utils.bitmapToMat(selectbp, src);
//
//        Imgproc.GaussianBlur(src, dst, new Size(9, 9), 0, 0, BORDER_DEFAULT);//高斯滤波
//
//        Utils.matToBitmap(dst, selectbp);
//        myImageView.setImageBitmap(selectbp);
//    }
//
//    //中值滤波
//    private void mediaBlur() {
//        Mat src = new Mat();
//        Mat dst = new Mat();
//
//        Utils.bitmapToMat(selectbp, src);
//
//        Imgproc.medianBlur(src, dst, 7);
//
//        Utils.matToBitmap(dst, selectbp);
//        myImageView.setImageBitmap(selectbp);
//    }
//
//
//
//    //Sobel算子 边缘检测
//    private void sobel() {
//        Mat src = new Mat();
//        Mat gx = new Mat();
//        Mat gy = new Mat();
//        Mat dst = new Mat();
//
//        Utils.bitmapToMat(selectbp, src);
//
//        Imgproc.Sobel(src, gx, CV_32F, 1, 0);//x方向求导
//        Imgproc.Sobel(src, gy, CV_32F, 0, 1);//y方向求导
//
//        Core.subtract(gx, gy, dst);
//        Core.convertScaleAbs(dst, dst);
//
//        Utils.matToBitmap(dst, selectbp);
//        myImageView.setImageBitmap(selectbp);
//    }
}
