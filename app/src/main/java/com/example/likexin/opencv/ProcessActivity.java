package com.example.likexin.opencv;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.SeekBar;

import org.opencv.core.Mat;
import org.opencv.core.Rect;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Vector;

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

/**
 * Created by likexin on 2018/4/21.
 */

public class ProcessActivity extends AppCompatActivity implements SeekBar.OnSeekBarChangeListener {

    private ImageView myImageView;
    private Bitmap selectbp;
    private SeekBar sxBar, bhdBar, ldBar;
    private static int MIN_COLOR = 160;
    private static int MAX_COLOR = 255;
    private float sx, bhd, ld;
    private double max_size = 1024;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_process);
        onInit();
    }

    private void onInit() {
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

        myImageView = (ImageView) findViewById(R.id.imageView);
        Intent intent = getIntent();
        if (intent != null) {
            Bitmap bitmap = getBitmap(intent);
            selectbp = bitmap;
            myImageView.setImageBitmap(selectbp);
        }
        myImageView.setScaleType(ImageView.ScaleType.CENTER);

        Button processBtn = (Button) findViewById(R.id.process);
        processBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                singleProcess();
            }
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

                Request request = new Request.Builder().url("http://172.24.175.116:8080/ocr/image2pdf")
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
                    public void onResponse(Call call, Response response){
                        try {
                            byte[] bytes = response.body().bytes();
                            if (response.isSuccessful()) {
    //                            String filePath = Environment.getExternalStorageState();
                                File[] filePath = new File[0];
                                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                                    filePath = ProcessActivity.this.getExternalMediaDirs();
                                }
                                long time = System.currentTimeMillis();
                                String fileName = time + "处理结果.pdf";
                                PdfTools.savePDF(bytes, filePath[0].getAbsolutePath(), fileName, ProcessActivity.this);

                                Intent intent = new Intent(ProcessActivity.this, SaveSuccessActivity.class);
                                startActivity(intent);
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });
            }
        });
    }

    private Bitmap getBitmap(Intent intent) {
        try {
            Uri uri = intent.getParcelableExtra("photo");
            InputStream input = getContentResolver().openInputStream(uri);
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(input, null, options);
            int raw_width = options.outWidth;
            int raw_height = options.outHeight;
            int max = Math.max(raw_width, raw_height);
            int newWidth;
            int newHeight;
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
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private void singleProcess() {
        Bitmap bitmap = innerProcess(bitmap2Mat(this.selectbp));
        ProcessActivity.this.selectbp = bitmap;
        ProcessActivity.this.myImageView.setImageBitmap(bitmap);
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


