package com.example.likexin.opencv;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SeekBar;

import com.yuyh.library.imgsel.ISNav;
import com.yuyh.library.imgsel.config.ISListConfig;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Mat;
import org.opencv.core.Rect;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import cn.lemon.multi.MultiView;

public class MainActivity extends AppCompatActivity implements SeekBar.OnSeekBarChangeListener {
    private double max_size = 1024;
    private int PICK_IMAGE_REQUEST = 1;
    private int REQUEST_LIST_CODE = 2;
    private ImageView myImageView;
    private ListView mListView;
    private MultiView multiView;
    private Bitmap selectbp;

    private SeekBar sxBar, bhdBar, ldBar;
    private static int MIN_COLOR = 160;
    private static int MAX_COLOR = 255;
    private float sx, bhd, ld;

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

        sxBar = (SeekBar) findViewById(R.id.seekbar0);
        bhdBar = (SeekBar) findViewById(R.id.seekbar1);
        ldBar = (SeekBar) findViewById(R.id.seekbar2);
        sxBar.setOnSeekBarChangeListener(this);
        sxBar.setMax(MAX_COLOR);// 设置最大值
        sxBar.setProgress(MIN_COLOR);// 设置初始值（当前值）
        bhdBar.setOnSeekBarChangeListener(this);
        bhdBar.setMax(MAX_COLOR);
        bhdBar.setProgress(MIN_COLOR);
        ldBar.setOnSeekBarChangeListener(this);
        ldBar.setMax(MAX_COLOR);
        ldBar.setProgress(MIN_COLOR);

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
                // multiProcess();
                singleProcess();
            }
        });
    }

    private void singleProcess() {
        Bitmap bitmap = innerProcess(MatTools.bitmap2Mat(this.selectbp));
        MainActivity.this.selectbp = bitmap;
        MainActivity.this.myImageView.setImageBitmap(bitmap);
    }

    //选多张图片，嘤不会写
    private void multiProcess() {
        List<Bitmap> selectImageList = this.selectImageList;

        List<Bitmap> result = new ArrayList<>();
        for (Bitmap bitmap : selectImageList) {
            result.add(innerProcess(MatTools.bitmap2Mat(bitmap)));
        }
        this.selectImageList = result;

        multiView.clear();
        multiView.setBitmaps(this.selectImageList);
    }

    private Bitmap innerProcess(Mat origin) {
        Mat gray = ImageProcess.grayImg(origin);
        Mat bilateralFilter = ImageProcess.bilateralFilterImg(gray);
        Mat thresholdImg = ImageProcess.thresholdImg(bilateralFilter);
        Mat closingImg = ImageProcess.closingImg(thresholdImg);
        Mat cannyImg = ImageProcess.cannyImg(closingImg);
        Vector<Rect> boundRect = ImageProcess.findContours(origin, cannyImg);
        Mat resizeImg = ImageProcess.resizeImg(origin, boundRect);
//        Mat dst = ImageProcess.angleTransform(resizeImg);

//        Mat tiltCorrectionImg = ImageProcess.tiltCorrectionImg(resizeImg);
        return MatTools.mat2Bitmap(resizeImg);
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
            multiView.clear();
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

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        switch (seekBar.getId()) {
            case R.id.seekbar0:
                sx = (progress - MIN_COLOR) * 1.0f / MIN_COLOR * 180;
                break;
            case R.id.seekbar1:
                bhd = progress * 1.0f / MIN_COLOR;
                break;
            case R.id.seekbar2:
                ld = progress * 1.0f / MIN_COLOR;
                break;
        }

        if (null != this.selectbp) {
            this.selectbp = ImageTools.getColorImage(this.selectbp, sx, bhd, ld);
            myImageView.setImageBitmap(selectbp);
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {

    }
}