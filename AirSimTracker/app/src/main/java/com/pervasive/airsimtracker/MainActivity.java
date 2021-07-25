package com.pervasive.airsimtracker;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.*;
import android.os.Build;
import android.os.Handler;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.Toast;
import android.widget.ToggleButton;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import com.example.helloairsim.TaskRunner;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.IntBuffer;

import com.pervasive.airsimtracker.Detection.DetectorActivity;
import com.pervasive.airsimtracker.cnn.CNNExtractorService;
import com.pervasive.airsimtracker.cnn.impl.CNNExtractorServiceImpl;
import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;
import org.opencv.dnn.Net;
import org.opencv.imgproc.Imgproc;
import org.tensorflow.lite.schema.Tensor;

public class MainActivity extends AppCompatActivity {
    private View imageView;
    private Bitmap bitmap;
    private static final String TAG = "AirClass::MainActivity";
    private static final String IMAGENET_CLASSES = "imagenet_classes.txt";
    //private static final String MODEL_FILE = "tinyyolov2-7.onnx";
    private static final int w = 480, h = 360;  // Width and height of the images received, used to compute the distance between the cars
    private static ToggleButton connectButton;
    //private Net opencvNet;
    //private CNNExtractorService cnnService;
    private DetectorActivity detector;

    // Data
    public float accelerationValue = 0.3f;
    public float decelerationValue = 0.2f;


    // Function to retrieve data and send commands to the Airsim car
    public native boolean CarConnect();
    public native byte[] GetImage();
    public native ReceivedImage GetImages();
    public native float[] GetDepth();
    public native void CarSteering(float steeringAngle);
    public native void CarAccelerate(float throttle, float steeringAngle);
    public native void CarDecelerate(float throttle, float steeringAngle);
    public native void CarBrake();
    public native float GetCarSpeed();

    static {
        System.loadLibrary("carclient");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "called onCreate");
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_main);

        connectButton = (ToggleButton) findViewById(R.id.connectButton);
        bitmap = null;
        this.detector = new DetectorActivity();
    }

    public void onButtonConnect(View view) {
        TaskRunner runner = new TaskRunner();
        if(connectButton.isChecked()){
            runner.executeAsync(new CustomCallable<Boolean>() {
                @Override
                public Boolean call() throws Exception {
                    return CarConnect();
                }
                @RequiresApi(api = Build.VERSION_CODES.O)
                @Override
                public void postExecute(Boolean result) {
                    Toast.makeText(getApplicationContext(),
                            "connection result " + result, Toast.LENGTH_LONG).show();

                    final Handler handler = new Handler();
                    // Defines the interval of time passing between each frame request
                    final int delay = 1; // ms
                    // Generating the object to control the Bitmap options
                    BitmapFactory.Options options = new BitmapFactory.Options();
                    options.inJustDecodeBounds = false;
                    options.inBitmap = bitmap;
                    options.inMutable = true;
                    // Needed to show the stream of images
                    ImageView imageView = (ImageView) findViewById(R.id.cameraImage);
                    handler.postDelayed(new Runnable() {
                        public void run() {
                            final long startTime = SystemClock.uptimeMillis(); // For debug purposes

                            // Both depth and video images are received at the same time
                            ReceivedImage image = GetImages();
                            byte[] imageVals = image.videoImage;
                            float[] imageDepth = image.depthImage;

                            // Converting the received byte[] into Bitmap (to display and process the image)
                            int length = imageVals.length;
                            bitmap = BitmapFactory.decodeByteArray(imageVals, 0, length, options);

                            // .. Image classification and car controls //
                            // This gives us the point in which we found the car
                            Point pos = detector.processImage(MainActivity.this, bitmap);
                            if(pos != null) {
                                Log.i(TAG, String.format("Point in screen of the car: %d, %d", pos.x, pos.y));
                                float distance = getDist(imageDepth, pos);
                                Log.i(TAG, String.format("Distance from the car in meters: %f", distance));
                                // Go towards the point in which the car should be
                                float steeringAngle = (pos.x/(w/2)) - 1;

                                // Choose the right amount of throttle
                                if(distance < 4 || distance > 100)
                                    CarBrake();
                                else {
                                    float carSpeed = GetCarSpeed();
                                    if((distance/carSpeed) < 1)
                                        CarDecelerate(decelerationValue, steeringAngle);
                                    else
                                        CarAccelerate(accelerationValue, steeringAngle);
                                }
                                pos = null;
                            }
                            else // Can't find the car
                                CarBrake();

                            // Display the image
                            imageView.setImageBitmap(bitmap);
                            final  long stopTime = SystemClock.uptimeMillis();
                            Log.i(TAG, String.format("Total time: %d", stopTime-startTime));
                            handler.postDelayed(this, delay);
                        }
                    }, delay);
                }

                @Override
                public void preExecute() {
                    imageView = findViewById(R.id.cameraImage);
                }
            });
        }
        else {
            // Specificare cosa fa nel caso in cui il toggleButton passasse da ON a OFF
        }
    }

    private float getDist(float[] imageDepth, Point pos) {
        return imageDepth[w*h-1-(pos.y+w*pos.x)];
    }
}