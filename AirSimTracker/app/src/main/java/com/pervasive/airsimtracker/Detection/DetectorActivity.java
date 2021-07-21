package com.pervasive.airsimtracker.Detection;

import android.app.Activity;
import android.content.Context;
import android.graphics.*;
import android.os.Handler;
import android.os.SystemClock;
import android.util.Log;
import android.util.Size;
import android.util.TypedValue;
import android.view.Surface;
import android.view.WindowManager;
import android.widget.Toast;
import com.pervasive.airsimtracker.Detection.env.BorderedText;
import com.pervasive.airsimtracker.Detection.env.ImageUtils;
import com.pervasive.airsimtracker.Detection.tflite.Detector;
import com.pervasive.airsimtracker.Detection.tflite.TFLiteObjectDetectionAPIModel;
import com.pervasive.airsimtracker.Detection.tracking.MultiBoxTracker;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class DetectorActivity {
    // Configuration values for the prepackaged SSD model.
    private static final int TF_OD_API_INPUT_SIZE = 300;
    private static final boolean TF_OD_API_IS_QUANTIZED = true;
    private static final String TF_OD_API_MODEL_FILE = "detect.tflite";
    private static final String TF_OD_API_LABELS_FILE = "labelmap.txt";
    private static final DetectorMode MODE = DetectorMode.TF_OD_API;
    // Minimum detection confidence to track a detection.
    private static final float MINIMUM_CONFIDENCE_TF_OD_API = 0.4f;
    private static final boolean MAINTAIN_ASPECT = false;
    private static final Size DESIRED_PREVIEW_SIZE = new Size(640, 480);
    private static final boolean SAVE_PREVIEW_BITMAP = false;
    private static final float TEXT_SIZE_DIP = 10;
    private static final String TAG = "DetectorActivity";

    private Integer sensorOrientation;
    private Detector detector;
    // private Bitmap rgbFrameBitmap = null;
    private Bitmap rgbFrameBitmap = null;
    private Bitmap croppedBitmap = null;
    private Bitmap cropCopyBitmap = null;
    private boolean computingDetection = false;
    private long timestamp = 0;
    private Matrix frameToCropTransform;
    private Matrix cropToFrameTransform;
    private MultiBoxTracker tracker;
    private BorderedText borderedText;
    private Size size = new Size(640,360);
    private int rotation = 0;
    private Point pointInScreen;
    protected int previewWidth = 0;
    protected int previewHeight = 0;

    public Point processImage(Context context, Bitmap bitmap) {
        rgbFrameBitmap = bitmap;
        onPreviewSizeChosen(context, size, rotation);
        //rgbFrameBitmap.setPixels(getRgbBytes(), 0, previewWidth, 0, 0, previewWidth, previewHeight);

        final Canvas canvas = new Canvas(croppedBitmap);
        canvas.drawBitmap(rgbFrameBitmap, frameToCropTransform, null);
        // For examining the actual TF input.
        if (SAVE_PREVIEW_BITMAP) {
            ImageUtils.saveBitmap(croppedBitmap);
        }
        final Handler handler = new Handler();
        handler.post(new Runnable() {
            public void run() {
                final long startTime = SystemClock.uptimeMillis();
                final List<Detector.Recognition> results = detector.recognizeImage(croppedBitmap);

                cropCopyBitmap = Bitmap.createBitmap(croppedBitmap);
                final Canvas canvas = new Canvas(cropCopyBitmap);
                final Canvas truecanvas = new Canvas(bitmap);
                final Paint paint = new Paint();
                paint.setColor(Color.RED);
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(2.0f);

                float minimumConfidence = MINIMUM_CONFIDENCE_TF_OD_API;
                switch (MODE) {
                    case TF_OD_API:
                        minimumConfidence = MINIMUM_CONFIDENCE_TF_OD_API;
                        break;
                }

                final List<Detector.Recognition> mappedRecognitions =
                        new ArrayList<Detector.Recognition>();

                for (final Detector.Recognition result : results) {
                    final RectF location = result.getLocation();
                    // Only draws the rectangle over the car, if it is confident
                    if (location != null && result.getConfidence() >= minimumConfidence && result.getTitle().equals("car")) {
                        canvas.drawRect(location, paint);
                        pointInScreen = new Point((int) result.getLocation().centerX(), (int) result.getLocation().centerY());
                        cropToFrameTransform.mapRect(location); // Dà problemi perché cropToFrame è null (potrebbe servire per display immagine grossa)
                        truecanvas.drawRect(location, paint);
                        result.setLocation(location);
                        mappedRecognitions.add(result);
                    }
                }
                // tracker.trackResults(mappedRecognitions, currTimestamp);
                computingDetection = false;
            }
        });
        return pointInScreen;
    }

    public void onPreviewSizeChosen(Context context, final Size size, final int rotation) {
        final float textSizePx =
                TypedValue.applyDimension(
                        TypedValue.COMPLEX_UNIT_DIP, TEXT_SIZE_DIP, context.getResources().getDisplayMetrics());
        borderedText = new BorderedText(textSizePx);
        borderedText.setTypeface(Typeface.MONOSPACE);

        tracker = new MultiBoxTracker(context);

        int cropSize = TF_OD_API_INPUT_SIZE;

        try {
            detector =
                    TFLiteObjectDetectionAPIModel.create(
                            context,
                            TF_OD_API_MODEL_FILE,
                            TF_OD_API_LABELS_FILE,
                            TF_OD_API_INPUT_SIZE,
                            TF_OD_API_IS_QUANTIZED);
            cropSize = TF_OD_API_INPUT_SIZE;
        } catch (final IOException e) {
            e.printStackTrace();
            Toast toast =
                    Toast.makeText(
                            context, "Detector could not be initialized", Toast.LENGTH_SHORT);
            toast.show();
            ((Activity)context).finish();
        }

        previewWidth = size.getWidth();
        previewHeight = size.getHeight();
        sensorOrientation = rotation - getScreenOrientation();
        // rgbFrameBitmap = Bitmap.createBitmap(previewWidth, previewHeight, Bitmap.Config.ARGB_8888);
        croppedBitmap = Bitmap.createBitmap(cropSize, cropSize, Bitmap.Config.ARGB_8888);

        frameToCropTransform =
                ImageUtils.getTransformationMatrix(
                        previewWidth, previewHeight,
                        cropSize, cropSize,
                        sensorOrientation, MAINTAIN_ASPECT);

        cropToFrameTransform = new Matrix();
        frameToCropTransform.invert(cropToFrameTransform);
        tracker.setFrameConfiguration(previewWidth, previewHeight, sensorOrientation);
    }

    // Which detection model to use: by default uses Tensorflow Object Detection API frozen
    // checkpoints.
    private enum DetectorMode {
        TF_OD_API;
    }

    protected int getScreenOrientation() {
        // Da correggere quando si implementa la rotazione
        /*switch (getWindowManager().getDefaultDisplay().getRotation()) {
            case Surface.ROTATION_270:
                return 270;
            case Surface.ROTATION_180:
                return 180;
            case Surface.ROTATION_90:
                return 90;
            default:
                return 0;
        }*/
        return 0;
    }
}
