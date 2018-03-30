package com.example.a18350.opencvtest;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;

import com.yuyh.library.imgsel.ISNav;
import com.yuyh.library.imgsel.config.ISListConfig;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Vector;

import cn.lemon.multi.MultiView;

import static org.opencv.core.CvType.CV_32F;
import static org.opencv.core.CvType.CV_8UC3;
import static org.opencv.imgproc.Imgproc.COLOR_BGR2GRAY;
import static org.opencv.imgproc.Imgproc.COLOR_BGRA2BGR;
import static org.opencv.imgproc.Imgproc.INTER_LINEAR;
import static org.opencv.imgproc.Imgproc.THRESH_BINARY;
import static org.opencv.imgproc.Imgproc.boundingRect;
import static org.opencv.imgproc.Imgproc.rectangle;
import static org.opencv.imgproc.Imgproc.resize;

public class MainActivity extends AppCompatActivity {
    private double max_size = 1024;
    private int PICK_IMAGE_REQUEST = 1;
    private int REQUEST_LIST_CODE = 2;
    private ImageView myImageView;
    private ListView mListView;
    private MultiView multiView;
    private Bitmap selectbp;

    private List<Bitmap> selectImageList;
    private static final String TAG = "OpenCV4Android";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        myImageView = (ImageView) findViewById(R.id.imageView);
        myImageView.setScaleType(ImageView.ScaleType.CENTER);

        mListView = (ListView) findViewById(R.id.listView);
        mListView.setItemsCanFocus(false);
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                System.out.println(position);

                System.out.println(id);
            }
        });

        multiView = (MultiView) findViewById(R.id.multi_view);
        multiView.setLayoutParams(new LinearLayout.LayoutParams(900, ViewGroup.LayoutParams.WRAP_CONTENT));


        Button selectImageBtn = (Button) findViewById(R.id.select);
        selectImageBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectImage();
            }
        });

        Button multipleSelectBtn = (Button) findViewById(R.id.multipleSelect);
        multipleSelectBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                selectMultipleImage();
            }

            private void selectMultipleImage() {
                // 自由配置选项
                ISListConfig config = new ISListConfig.Builder()
                        // 是否多选, 默认true
                        .multiSelect(true)
                        // 是否记住上次选中记录, 仅当multiSelect为true的时候配置，默认为true
                        .rememberSelected(false)
                        // “确定”按钮背景色
                        .btnBgColor(Color.GRAY)
                        // “确定”按钮文字颜色
                        .btnTextColor(Color.BLUE)
                        // 使用沉浸式状态栏
                        .statusBarColor(Color.parseColor("#3F51B5"))
                        // 返回图标ResId
                        //.backResId(android.support.v7.appcompat.R.drawable.abc_ic_ab_back_mtrl_am_alpha)
                        // 标题
                        .title("图片")
                        // 标题文字颜色
                        .titleColor(Color.WHITE)
                        // TitleBar背景色
                        .titleBgColor(Color.parseColor("#3F51B5"))
                        // 裁剪大小。needCrop为true的时候配置
                        .cropSize(1, 1, 200, 200)
                        .needCrop(true)
                        // 第一个是否显示相机，默认true
                        .needCamera(false)
                        // 最大选择图片数量，默认9
                        .maxNum(9)
                        .build();

                // 跳转到图片选择器
                ISNav.getInstance().toListActivity(MainActivity.this, config, REQUEST_LIST_CODE);
            }
        });

        Button processBtn = (Button) findViewById(R.id.process);
        processBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //process();
                innerProcess();
            }
        });
    }

    //选多张图片，嘤不会用
    private void process() {
        List<Bitmap> selectImageList = this.selectImageList;

        List<Bitmap> result = new ArrayList<>();
        for (Bitmap bitmap : selectImageList) {
            this.selectbp = bitmap;
            innerProcess();
        }

//        this.selectImageList = result;
//
//        multiView.clear();
//        multiView.setBitmaps(this.selectImageList);
    }

    private void innerProcess() {
        Mat origin = new Mat();
        Utils.bitmapToMat(selectbp, origin);
        gray();
        sobel();
        blurAndThreshold();
        structure();
        erodeAndDilate();
        Bitmap finalBitmap = findContours(origin);

        myImageView.setImageBitmap(finalBitmap);
    }

    //灰度
    private void gray() {
        Mat src = new Mat();
        Mat grayTemp = new Mat();
        Mat dst = new Mat();

        Utils.bitmapToMat(selectbp, src);

        Imgproc.cvtColor(src, grayTemp, Imgproc.COLOR_BGRA2BGR);
        Imgproc.cvtColor(grayTemp, dst, Imgproc.COLOR_BGR2GRAY);

        Utils.matToBitmap(dst, selectbp);
        myImageView.setImageBitmap(selectbp);
    }

    //Sobel算子 边缘检测
    private void sobel() {
        Mat src = new Mat();
        Mat gx = new Mat();
        Mat gy = new Mat();
        Mat dst = new Mat();

        Utils.bitmapToMat(selectbp, src);

        Imgproc.Sobel(src, gx, CV_32F, 1, 0);//x方向求导
        Imgproc.Sobel(src, gy, CV_32F, 0, 1);//y方向求导

        Core.subtract(gx, gy, dst);
        Core.convertScaleAbs(dst, dst);

        Utils.matToBitmap(dst, selectbp);
        myImageView.setImageBitmap(selectbp);
    }

    //均值滤波and二值化
    private void blurAndThreshold() {
        Mat src = new Mat();
        Mat temp = new Mat();
        Mat dst = new Mat();

        Utils.bitmapToMat(selectbp, src);

        Imgproc.blur(src, temp, new Size(9, 9));//均值滤波
        Imgproc.threshold(temp, dst, 90, 255, Imgproc.THRESH_BINARY);

        Utils.matToBitmap(dst, selectbp);
        myImageView.setImageBitmap(selectbp);
    }

    //获取图片结构元素
    private void structure() {
        Mat src = new Mat();
        Mat dst = new Mat();

        Utils.bitmapToMat(selectbp, src);

        Mat structuringElement = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(55, 55));//调参
        Imgproc.morphologyEx(src, dst, Imgproc.MORPH_CLOSE, structuringElement);

        Utils.matToBitmap(dst, selectbp);
        myImageView.setImageBitmap(selectbp);
    }

    //腐蚀膨胀
    private void erodeAndDilate() {
        Mat src = new Mat();
        Mat temp = new Mat();
        Mat dst = new Mat();
        Mat kernel = new Mat();

        Utils.bitmapToMat(selectbp, src);

        Imgproc.erode(src, temp, kernel, new Point(-1, -1), 12);
        Imgproc.dilate(temp, dst, kernel, new Point(-1, -1), 12);
    }

    //轮廓检测
    private Bitmap findContours(Mat origin) {
        Mat src = new Mat();
        Mat temp = new Mat();
        Mat dst = new Mat();
        Mat img = new Mat();

        Utils.bitmapToMat(selectbp, src);

        Imgproc.cvtColor(src, temp, Imgproc.COLOR_BGRA2BGR);
        Imgproc.cvtColor(temp, img, Imgproc.COLOR_BGR2GRAY);

        List<MatOfPoint> contours = new ArrayList<>();
        Mat hierarchy = new Mat();
        //轮廓检测
        Imgproc.findContours(img, contours, hierarchy, Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE);

        List<MatOfPoint2f> newContours = new ArrayList<>();
        for (MatOfPoint point : contours) {
            MatOfPoint2f newPoint = new MatOfPoint2f(point.toArray());
            newContours.add(newPoint);
        }
        //绘制轮廓
        Mat result = new Mat(img.size(), CV_8UC3, new Scalar(0));
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

        List<Mat> cutResult = cut(origin, boundRect);
        Mat mat = cutResult.get(0);

        Size size = new Size(1600, 1200);
        resize(mat, dst, size, 0, 0, INTER_LINEAR);

        Mat tilt = tiltCorrection(dst);

        Bitmap bitmap1 = Bitmap.createBitmap(tilt.width(), tilt.height(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(tilt, bitmap1);

        return bitmap1;
    }

    /**
     * 倾斜校正
     *
     * @param qx_image
     * @return
     */
    private Mat tiltCorrection(Mat qx_image) {
        Mat grayTemp = new Mat();
        Mat gray = new Mat();

        Imgproc.cvtColor(qx_image, grayTemp, COLOR_BGRA2BGR);
        Imgproc.cvtColor(grayTemp, gray, COLOR_BGR2GRAY);

        Core.bitwise_not(gray, gray);

        Mat threshold = new Mat();
        double thresholdResult = Imgproc.threshold(gray, threshold, 0, 255, THRESH_BINARY);
        System.out.println("thresholdResult:" + thresholdResult);

        List<MatOfPoint> contours = new ArrayList<>();
        Mat hierarchy = new Mat();
        //轮廓检测
        Imgproc.findContours(threshold, contours, hierarchy, Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE);

        System.out.println("contours.size()->" + contours.size());

        MatOfPoint matOfPoint = contours.get(0);
        RotatedRect rect = Imgproc.minAreaRect(new MatOfPoint2f(matOfPoint.toArray()));
        double angle = rect.angle;

        System.out.println("angle:" + angle);

        if (angle < -45) {
            angle = -(90 + angle);
        } else {
            angle = -angle;
        }

        int width = threshold.width();
        int height = threshold.height();
        Point center = new Point(width / 2, height / 2);

        Mat M = Imgproc.getRotationMatrix2D(center, angle, 1.0);
        Mat dst = new Mat();
        Imgproc.warpAffine(qx_image, dst, M, new Size(height, width));

        return dst;
    }

    private List<Mat> cut(Mat src, Vector<Rect> boundRect) {
        List<Mat> result = new ArrayList<>();

        Collections.sort(boundRect, new Comparator<Rect>() {
            @Override
            public int compare(Rect t2, Rect t1) {
                return Long.valueOf(t1.width * t1.height).compareTo(Long.valueOf(t1.width * t2.height));
            }
        });

        Mat clone = src.clone();
        Mat submat = clone.submat(boundRect.get(0));
        Mat temp = new Mat();
        submat.copyTo(temp);
        result.add(temp);

        return result;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK) {
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
        } else if (requestCode == REQUEST_LIST_CODE && resultCode == RESULT_OK) {
            List<String> pathList = data.getStringArrayListExtra("result");

            List<Uri> uris = new ArrayList<>();
            for (String s : pathList) {
                uris.add(Uri.parse("file://" + s));
            }

            this.selectImageList = uris2Bitmaps(uris);

            multiView.setBitmaps(this.selectImageList);
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

    public List<Bitmap> uris2Bitmaps(List<Uri> uris) {
        List<Bitmap> res = new ArrayList<>();

        for (Uri uri : uris) {
            res.add(uri2Bitmap(uri));
        }
        return res;
    }

    public Bitmap uri2Bitmap(Uri uri) {
        try {
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
            return BitmapFactory.decodeStream(getContentResolver().openInputStream(uri), null, options);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

}