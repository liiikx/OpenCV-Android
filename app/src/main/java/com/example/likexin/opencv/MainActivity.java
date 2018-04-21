package com.example.likexin.opencv;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.MemoryFile;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SeekBar;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Mat;
import org.opencv.core.Rect;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Vector;

import cn.lemon.multi.MultiView;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Headers;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import static com.example.likexin.opencv.ImageProcess.angleTransform;
import static com.example.likexin.opencv.ImageProcess.bilateralFilterImg;
import static com.example.likexin.opencv.ImageProcess.cannyImg;
import static com.example.likexin.opencv.ImageProcess.closingImg;
import static com.example.likexin.opencv.ImageProcess.findContours;
import static com.example.likexin.opencv.ImageProcess.grayImg;
import static com.example.likexin.opencv.ImageProcess.resizeImg;
import static com.example.likexin.opencv.ImageProcess.thresholdImg;
import static com.example.likexin.opencv.MatTools.bitmap2Mat;
import static com.example.likexin.opencv.MatTools.mat2Bitmap;


public class MainActivity extends AppCompatActivity implements SeekBar.OnSeekBarChangeListener {
    private double max_size = 1024;
    private int PICK_IMAGE_REQUEST = 1;
    private int REQUEST_LIST_CODE = 2;
    private ImageView myImageView;
    private ListView mListView;
    private MultiView multiView;
    private Bitmap selectbp;
    private Context context = this;

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
//
        Button selectImageBtn = (Button) findViewById(R.id.select);
        selectImageBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectImage();
            }
        });

        Button processBtn = (Button) findViewById(R.id.process);
        processBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {singleProcess();}
        });

        Button saveBtn = (Button) findViewById(R.id.save);
        saveBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                selectbp.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream);
                byte[] arrayByte = byteArrayOutputStream.toByteArray();

                OkHttpClient client = new OkHttpClient();
                MediaType mediaType = MediaType.parse("application/octet-stream");
                RequestBody requestBody = RequestBody.create(mediaType, arrayByte);

                MultipartBody body = new MultipartBody.Builder().setType(MultipartBody.ALTERNATIVE)
                        .addPart(Headers.of("Content-Disposition", "form-data;" +
                                "name=\"file\";filename=\"ocr.jpg\""), requestBody)
                        .build();

                Request request = new Request.Builder().url("http://192.168.1.64:8080/ocr/image2pdf")
                        .addHeader("User-Agent", "android")
                        .header("Content-Type", "text/html; charset=utf-8;")
                        .post(body)
                        .build();

                client.newCall(request).enqueue(new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        Log.i("save", "message:" + e);
                    }

                    @Override
                    public void onResponse(Call call, Response response) throws IOException {
                        byte[] bytes = response.body().bytes();
                        if (response.isSuccessful()) {
                            String filePath= Environment.getExternalStorageState();
                            long time = System.currentTimeMillis();
                            String fileName = time + "处理结果";
                            PdfTools.savePDF(bytes, filePath, fileName, context);
                        }
                    }
                });
            }
        });
    }

    private void singleProcess() {
        Bitmap bitmap = innerProcess(bitmap2Mat(this.selectbp));
        MainActivity.this.selectbp = bitmap;
        MainActivity.this.myImageView.setImageBitmap(bitmap);
    }

    private Bitmap innerProcess(Mat origin) {
        Mat gray = grayImg(origin);
        Mat bilateralFilter = bilateralFilterImg(gray);
        Mat threshold = thresholdImg(bilateralFilter);
        Mat closing = closingImg(threshold);
        Mat canny = cannyImg(closing);
        Vector<Rect> boundRect = findContours(origin, canny);
        Mat resize = resizeImg(origin, boundRect);
        Mat tiltCorrection = angleTransform(resize);
        return mat2Bitmap(tiltCorrection);
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
        System.out.println("likexinTest");
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, mLoaderCallback);
        } else {
            Log.d(TAG, "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
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