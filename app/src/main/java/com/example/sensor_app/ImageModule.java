package com.example.sensor_app;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapRegionDecoder;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.graphics.Rect;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;

import java.io.IOException;
import java.io.InputStream;

public class ImageModule implements View.OnTouchListener {

    private ImageView imageView;
    private Bitmap bitmap, bitmap_altered, arrow;
    private Canvas canvas;
    private int img_w, img_h;

    // Touch event
    private Matrix matrix = new Matrix();
    private Matrix prev_matrix = new Matrix();

    private static final int NONE = 0;
    private static final int DRAG = 1;
    private static final int ZOOM = 2;
    private int mode = NONE;

    private PointF start = new PointF();
    private PointF mid = new PointF();
    private float[] lastEvent = null;
    private float oldDist = 1f;

    ImageModule(Activity activity){
        imageView = (ImageView) activity.findViewById(R.id.imageView);
        imageView.setOnTouchListener(this);
        imageView.setScaleType(ImageView.ScaleType.MATRIX);

        // Load map information
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;

        BitmapFactory.decodeResource(activity.getResources(), R.raw.it2_1f, options);
        img_h = options.outHeight;
        img_w = options.outWidth;

        // Load map image
        InputStream inputStream;
        inputStream = activity.getApplicationContext().getResources().openRawResource(R.raw.it2_1f);
        BitmapRegionDecoder decoder = null;
        try {
            decoder = BitmapRegionDecoder.newInstance(inputStream, false);
        } catch (IOException e) {
            e.printStackTrace();
        }
        Rect rect = new Rect(0, 0, img_h, img_w);
        options.inJustDecodeBounds = false; // 이미지 전체를 읽을 것이다.
        options.inSampleSize = 2;           // 크기를 줄여서 읽어오자

        bitmap = decoder.decodeRegion(rect, options);   // 원본
        bitmap_altered = Bitmap.createBitmap(bitmap.getHeight(), bitmap.getWidth(), bitmap.getConfig());

        // 비트맵을 그림판에 출력
        canvas = new Canvas(bitmap_altered);
        canvas.drawBitmap(bitmap, 0, 0, null);

        // 그림판을 스크린(imageView)에 출력
        imageView.setImageBitmap(bitmap_altered);

        // 화살표 그림을 가져온다
        arrow = BitmapFactory.decodeResource(activity.getResources(), R.raw.arrow);

        plot_arrow(100, 100, 120);
    }

    // 화살표 그리는 method
    public void plot_arrow(float x, float y, float deg){
        canvas.drawBitmap(bitmap, 0, 0, null);
        Matrix matrix = new Matrix();
        matrix.postScale(0.5f, 0.5f);
        matrix.postRotate(deg);
        Bitmap rotated_arrow = Bitmap.createBitmap(arrow, 0, 0, arrow.getWidth(), arrow.getHeight(), matrix, true);

        canvas.drawBitmap(rotated_arrow, x, y, null);
        imageView.invalidate();
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        switch (event.getAction() & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN:
                mode = DRAG;
                lastEvent = null;
                prev_matrix.set(matrix);
                start.set(event.getX(), event.getY());
                break;

            case MotionEvent.ACTION_POINTER_DOWN:
                oldDist = spacing(event);
                if (oldDist > 10f) {
                    prev_matrix.set(matrix);
                    midPoint(mid, event);
                    mode = ZOOM;
                }
                break;

            case MotionEvent.ACTION_MOVE:
                if (mode == DRAG) {
                    matrix.set(prev_matrix);
                    matrix.postTranslate(event.getX() - start.x, event.getY() - start.y);
                }
                else if (mode == ZOOM) {
                    float newDist = spacing(event);
                    if (newDist > 10f) {
                        matrix.set(prev_matrix);
                        float scale = (newDist / oldDist);
                        matrix.postScale(scale, scale, mid.x, mid.y);
                    }
                    //float newDist = spacing(event);
                }
                break;

            case MotionEvent.ACTION_UP:
                break;
            case MotionEvent.ACTION_POINTER_UP:
                mode = NONE;
                break;
        }

        imageView.setImageMatrix(matrix);

        //bitmap = Bitmap.createBitmap(view.getWidth(), view.getHeight(), Bitmap.Config.RGB_565);
        //Canvas canvas = new Canvas(bitmap);
        //view.draw(canvas);

        return true;
    }

    private float spacing(MotionEvent motionEvent) {
        float x = motionEvent.getX(0) - motionEvent.getX(1);
        float y = motionEvent.getY(0) - motionEvent.getY(1);
        return (float) Math.sqrt(x * x + y * y);
    }

    private void midPoint(PointF point, MotionEvent motionEvent) {
        float x = motionEvent.getX(0) + motionEvent.getX(1);
        float y = motionEvent.getY(0) + motionEvent.getY(1);
        point.set(x / 2, y / 2);
    }
}
