package com.example.a18350.opencvtest;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import static org.opencv.core.Core.rectangle;
import static org.opencv.core.CvType.CV_8UC3;
import static org.opencv.imgproc.Imgproc.CHAIN_APPROX_SIMPLE;
import static org.opencv.imgproc.Imgproc.approxPolyDP;
import static org.opencv.imgproc.Imgproc.boundingRect;

public class MainActivity extends AppCompatActivity {
    private double max_size = 1024;
    private int PICK_IMAGE_REQUEST = 1;
    private ImageView myImageView;
    private Bitmap selectbp;
    private static final String TAG = "OpenCV4Android";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        myImageView = (ImageView) findViewById(R.id.imageView);
        myImageView.setScaleType(ImageView.ScaleType.CENTER);
        Button selectImageBtn = (Button) findViewById(R.id.select);
        selectImageBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                makeText(MainActivity.this.getApplicationContext(), "start to browser image", Toast.LENGTH_SHORT).show();
                selectImage();
            }
        });

        Button processBtn = (Button) findViewById(R.id.process);
        processBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                // makeText(MainActivity.this.getApplicationContext(), "hello, image process", Toast.LENGTH_SHORT).show();
//                convertGray();
//                findThreshold();
//                Mat img = new Mat();
//                Utils.bitmapToMat(selectbp,img);
//                //adjustImage(img,300);
//                //isTextImage(selectbp, selectbp.getWidth(), selectbp.getHeight());
                process();
            }
        });

    }

    private void process() {
        //灰度图片
        convert2Gray();
        sobel();
        //二值化
        blurAndThreshold();
        step4();
        //查找轮廓绘制矩形
        findContours();
//        step5();
    }

    private void findContours() {
        Mat src = new Mat();
        Mat dst = new Mat();
        Mat temp = new Mat();

        Utils.bitmapToMat(selectbp, src);
        Imgproc.cvtColor(src, temp, Imgproc.COLOR_BGRA2BGR);
        Imgproc.cvtColor(temp, dst, Imgproc.COLOR_BGR2GRAY);

        List<MatOfPoint> contours = new ArrayList<>();
        Mat hierarchy = new Mat();
        //轮廓检测
        Imgproc.findContours(dst, contours, hierarchy, Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE);

        List<MatOfPoint2f> newContours = new ArrayList<>();
        for (MatOfPoint point : contours) {
            MatOfPoint2f newPoint = new MatOfPoint2f(point.toArray());
            newContours.add(newPoint);
        }
        //绘制轮廓
        Mat result =new Mat(dst.size(), CV_8UC3, new Scalar(0));
        Imgproc.drawContours(result, contours, -1, new Scalar(255,255,255), 1);
        Mat result_boundingRect = result.clone();

        //conPoint存储计算得到的外接多边形
        Vector<Vector<MatOfPoint2f>> conPoint =new Vector<>();
        conPoint.setSize(contours.size());

        //boundRect存储计算得到的最小立式矩形
        Vector<Rect> boundRect = new Vector<>();
        boundRect.setSize(contours.size());

        for (int i = 0; i < contours.size(); i++)
        {

//           // 计算外接多边形
//          conPoint.set(i,approxPolyDP(newContours.get(i), conPoint.get(i).get(i), 3, true);

            // 计算最小外接立式矩形
            boundRect.set(i, boundingRect(contours.get(i)));
        }

        for (int i = 0; i< contours.size(); i++)
        {
            Scalar color = new Scalar(0, 255, 255);
            // 绘制最小外接立式矩形
            rectangle(result_boundingRect, boundRect.get(i).tl(), boundRect.get(i).br(), color, 5, 8, 0);

        }
        Utils.matToBitmap(result_boundingRect, selectbp);
        myImageView.setImageBitmap(selectbp);
    }

    private void step5() {
        Mat src = new Mat();
        Mat dst = new Mat();
        Mat temp = new Mat();
        Mat img = new Mat();

        Utils.bitmapToMat(selectbp, src);
        Imgproc.cvtColor(src,temp,Imgproc.COLOR_BGRA2BGR);
        Imgproc.cvtColor(temp, img, Imgproc.COLOR_BGR2GRAY);

        List<MatOfPoint> matOfPoints = new ArrayList<MatOfPoint>();
        Mat hierarchy = new Mat();
        Imgproc.findContours(img, matOfPoints, hierarchy, Imgproc.RETR_CCOMP, CHAIN_APPROX_SIMPLE);

        MatOfPoint2f matOfPoint2fSrc = null, matOfPoint2fDst = null;
        Scalar scalar = new Scalar(0, 255, 0);
        //Imgproc.minAreaRect()

        for (int i = 0; i < matOfPoints.size(); i++) {
            MatOfPoint matOfPoint = matOfPoints.get(i);

            matOfPoint.convertTo(matOfPoint2fSrc,CvType.CV_32FC2);
            approxPolyDP(matOfPoint2fSrc,matOfPoint2fDst,0.01*Imgproc.arcLength(matOfPoint2fSrc,true),true);

            matOfPoint2fDst.convertTo(matOfPoint,CvType.CV_32S);
            Imgproc.drawContours(dst, matOfPoints, i, scalar, 2, 8, hierarchy, 0, new Point());
        }

        Utils.matToBitmap(dst,selectbp);
        myImageView.setImageBitmap(selectbp);
    }

    private void step4() {
        Mat src = new Mat();
        Mat dst = new Mat();
        Utils.bitmapToMat(selectbp, src);

        Mat structuringElement = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(25, 25));
        Imgproc.morphologyEx(src, dst, Imgproc.MORPH_CLOSE, structuringElement);

        Utils.matToBitmap(dst,selectbp);
        myImageView.setImageBitmap(selectbp);
    }

    private void blurAndThreshold() {
        Mat src = new Mat();
        Mat tmp = new Mat();
        Mat dst = new Mat();
        Utils.bitmapToMat(selectbp, src);

        Imgproc.blur(src, tmp, new Size(9, 9));
        Imgproc.threshold(tmp, dst, 40, 255, Imgproc.THRESH_BINARY);

        Utils.matToBitmap(dst,selectbp);
        myImageView.setImageBitmap(selectbp);
    }

    private void sobel() {
        Mat src = new Mat();
        Mat gx = new Mat();
        Mat gy = new Mat();
        Mat dst = new Mat();
        Utils.bitmapToMat(selectbp, src);

        Imgproc.Sobel(src,gx,-1,1,0);//x方向求导
        Imgproc.Sobel(src,gy,-1,0,1);//x方向求导
        Core.addWeighted(gx,0.5,gy,0.5,0,dst);
        Utils.matToBitmap(dst,selectbp);
        myImageView.setImageBitmap(selectbp);
    }

    private void convert2Gray() {
        Mat src = new Mat();
        Mat temp = new Mat();
        Mat dst = new Mat();
        Utils.bitmapToMat(selectbp, src);

        Imgproc.cvtColor(src,temp,Imgproc.COLOR_BGRA2BGR);
        Imgproc.cvtColor(temp, dst, Imgproc.COLOR_BGR2GRAY);

        Utils.matToBitmap(dst, selectbp);
        myImageView.setImageBitmap(selectbp);
    }

    private void convertGray() {
        Mat src = new Mat();
        Mat temp = new Mat();
        Mat dst = new Mat();
        Mat mat = new Mat();
        Utils.bitmapToMat(selectbp, src);
        Imgproc.cvtColor(src, temp, Imgproc.COLOR_BGRA2BGR);
        Log.i("CV", "image type:" + (temp.type() == CV_8UC3));
        Imgproc.cvtColor(temp, dst, Imgproc.COLOR_BGR2GRAY);
        Imgproc.Canny(dst, mat, 60, 180);
        Utils.matToBitmap(mat, selectbp);
        myImageView.setImageBitmap(selectbp);
    }

    private void findThreshold() {
        Mat dst = new Mat();
        Mat src = new Mat();
        Utils.bitmapToMat(selectbp, src);
        Imgproc.threshold(src, dst, 0, 255, Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C);

        Mat img = new Mat();
        Size size = new Size(640, 480);
        Imgproc.resize(dst, img, new Size(dst.width() * 1.5f, dst.height() * 1f));

        //Imgproc.resize(dst, img, size, 0.5, 0.5, Imgproc.INTER_AREA);
        Utils.matToBitmap(img, selectbp);
        myImageView.setImageBitmap(selectbp);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            Uri uri = data.getData();
            try {
                Log.d("image-tag", "start to decode selected image now...");
                InputStream input = getContentResolver().openInputStream(uri);
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inJustDecodeBounds = true;
                BitmapFactory.decodeStream(input, null, options);
                int raw_width = options.outWidth;
                int raw_height = options.outHeight;
                int max = Math.max(raw_width, raw_height);
                int newWidth = raw_width;
                int newHeight = raw_height;
                int inSampleSize = 1;
                if (max > max_size) {
                    newWidth = raw_width / 2;
                    newHeight = raw_height / 2;
                    while ((newWidth / inSampleSize) > max_size || (newHeight / inSampleSize) > max_size) {
                        inSampleSize *= 2;
                    }
                }

                options.inSampleSize = inSampleSize;
                options.inJustDecodeBounds = false;
                options.inPreferredConfig = Bitmap.Config.ARGB_8888;
                selectbp = BitmapFactory.decodeStream(getContentResolver().openInputStream(uri), null, options);

                myImageView.setImageBitmap(selectbp);

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void selectImage() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "选择图像..."), PICK_IMAGE_REQUEST);
    }

    //openCV4Android 需要加载用到
    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS: {
                    Log.i(TAG, "OpenCV loaded successfully");
                    //  mOpenCvCameraView.enableView();
//                    mOpenCvCameraView.setOnTouchListener(ColorBlobDetectionActivity.this);
                }
                break;
                default: {
                    super.onManagerConnected(status);
                }
                break;
            }
        }
    };

    @Override
    protected void onResume() {
        System.out.println("likexin");
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, mLoaderCallback);
        } else {
            Log.d(TAG, "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    boolean isTextImage(Bitmap bitmap, int width, int height) {
        int y = 0;
        int line = 0;
        while (y < height) {
            int x = 0;
            int whiteNum = 0;
            while (x < width) {
                int pixel = bitmap.getPixel(x, y);
                if (pixel == -1) {
                    whiteNum++;
                }
                x++;
            }
            float scale = whiteNum / x;
            if (scale > 0.15) {
                line++;
            }
            y += 10;
        }
        float ratio = line / height;
        if (ratio > 0.4 && ratio < 1.0) {
            Log.d(TAG, "isTextImage: 是是是是是是是！！");
            return true;
        }
        Log.d(TAG, "isTextImage: 不是嘤嘤嘤");
        return false;
    }
}