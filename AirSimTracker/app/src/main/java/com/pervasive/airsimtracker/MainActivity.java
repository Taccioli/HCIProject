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
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;


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

import pl.droidsonroids.gif.GifImageView;

import static org.opencv.core.CvType.*;

public class MainActivity extends AppCompatActivity {
    private View imageView;
    private Bitmap bitmap;
    private static final String TAG = "AirClass::MainActivity";
    private static final String IMAGENET_CLASSES = "imagenet_classes.txt";
    //private static final String MODEL_FILE = "tinyyolov2-7.onnx";
    private static final int w = 480, h = 360;  // Width and height of the images received, used to compute the distance between the cars
    private static ToggleButton connectButton;
    public Mat imageMat;
    //public Mat depthMat;
    private float[] imageDepth;
    //private Net opencvNet;
    //private CNNExtractorService cnnService;
    private DetectorActivity detector;
    // Variable to store the point in which we found the car
    private Point actualPos;
    // Variable to store the point in which we last found the car
    private Point prevPos;
    // Variable to store the distance between the two cars
    private double distance;
    // Variables to handle the behavior when no car is found
    private int numFramesNoCar;
    private int numFramesNoCarThresh = 9;
    // Data
    public float accelerationValue = 0.3f;
    public float decelerationValue = 0.2f;
    // Flags to receive the images in different threads
    private static final int REQUEST = 0;
    private static final int WAIT = 1;
    private static final int PROCESS = 2;
    private static final int PAUSE = 3;
    private boolean imageFlag;
    private boolean depthFlag;
    private int state = REQUEST;
    private long startTime = 0;
    // Initial page views
    private TextView appNameTxt;
    private TextView loadingTxt;
    private GifImageView loadingGif;
    private ImageView cameraImage;

    // Function to retrieve data and send commands to the Airsim car
    public native boolean CarConnect();

    public native void GetScene(long imgAddr);

    //public native void GetImage(long imgAddr, long depthAddr);
    public native float[] GetImage(long imgAddr);

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

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                    Log.i(TAG, "OpenCV loaded succesfully");
                    break;
                default:
                    super.onManagerConnected(status);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "called onCreate");
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_main);

        appNameTxt = (TextView) findViewById(R.id.app_name);
        loadingTxt = (TextView) findViewById(R.id.connecting);
        loadingGif = (GifImageView) findViewById(R.id.loading);
        cameraImage = (ImageView) findViewById(R.id.camera_image);

        //connectButton = (ToggleButton) findViewById(R.id.connectButton);
        bitmap = null;
        imageMat = new Mat();
        //depthMat = new Mat(h,w,CV_32FC1);
        imageDepth = new float[]{};
        this.detector = new DetectorActivity();
        numFramesNoCar = 0;
    }

    public void CarControl(){
        // Initial page gone and camera image visible
        appNameTxt.setVisibility(View.GONE);
        loadingTxt.setVisibility(View.GONE);
        loadingGif.setVisibility(View.GONE);
        cameraImage.setVisibility(View.VISIBLE);


        final Handler handler = new Handler();
        // Defines the interval of time passing between each frame request
        final int delay = 1; // ms

        // Generating the object to control the Bitmap options
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = false;
        options.inBitmap = bitmap;
        options.inMutable = true;
        bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);

        handler.postDelayed(new Runnable() {
            public void run() {
                // Finite-state machine
                switch (state){
                    case REQUEST:
                        startTime = SystemClock.uptimeMillis(); // For debug purposes
                        // Make the image request
                        RequestImages();
                        // Update the state
                        state = WAIT;
                        break;
                    case WAIT:
                        // Waiting for the images to be received
                        if(depthFlag && imageFlag){
                            imageFlag = false;
                            depthFlag = false;
                            // Update the state
                            state = PROCESS;
                        }
                        break;
                    case PROCESS:
                        ProcessAndMove(imageMat, imageDepth);
                        // Display the image
                        cameraImage.setImageBitmap(bitmap);
                        // Update the state
                        state = REQUEST;
                        // For debug purposes
                        final long stopTime = SystemClock.uptimeMillis();
                        Log.i(TAG, String.format("Total time: %d", stopTime - startTime));
                        break;
                }
                handler.postDelayed(this, delay);
            }
        }, delay);
    }

    @Override
    public void onPause(){
        super.onPause();
        final Handler brakeHandler = new Handler();
        brakeHandler.post(new Runnable() { public void run() { CarBrake(); }});
        state = PAUSE;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "Internal OpenCV library not found. Using OpenCV manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, mLoaderCallback);
        } else {
            Log.d(TAG, "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
        state = REQUEST;
        Connect();
    }

    private void Connect() {
        TaskRunner runner = new TaskRunner();
        //if(connectButton.isChecked()){
        runner.executeAsync(new CustomCallable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                return CarConnect();
            }

            @Override
            public void postExecute(Boolean result) {
                if(!result)
                    // If the connection failed, try again
                    Connect();
                else
                    // If not, launch the car controls
                    CarControl();
            }

            @Override
            public void preExecute() {
            }
        });
    }

    private void ProcessAndMove(Mat imageMat, float[] imageDepth) {
        // Converting the received Mat image into Bitmap (to display and process the image)
        Imgproc.cvtColor(imageMat, imageMat, Imgproc.COLOR_BGR2RGBA);
        Utils.matToBitmap(imageMat, bitmap);

        // Converting the received byte[] into Bitmap (to display and process the image)
        //int length = imageVals.length;
        //bitmap = BitmapFactory.decodeByteArray(imageVals, 0, length, options);

        // .. Image classification and car controls //
        // This gives us the point in which we found the car
        //Point pos = detector.processImage(MainActivity.this, bitmap);
        actualPos = detector.processImage(MainActivity.this, bitmap);
        // If it recognizes the car inside the actual frame, move towards it
        if (actualPos != null) {
            Log.i(TAG, String.format("ACTUAL POS: %d, %d", actualPos.x, actualPos.y));
            Log.i(TAG, "Car DETECTED");
            Log.i(TAG, String.format("Point in screen of the car: %d, %d", actualPos.x, actualPos.y));
            numFramesNoCar = 0;
            distance = getDist(imageDepth, actualPos);
            Log.i(TAG, String.format("Distance from the car in meters: %f", distance));
            // Go towards the point in which the car should be
            float steeringAngle = (actualPos.x / (float) (w / 2)) - 1;

            // Choose the right amount of throttle
            if (distance < 4)
                CarBrake();
            else {
                /* float carSpeed = GetCarSpeed();
                if ((distance / carSpeed) < 1)
                    CarDecelerate(decelerationValue, steeringAngle);
                else
                    CarAccelerate(accelerationValue, steeringAngle); */
                CarAccelerate(accelerationValue, steeringAngle);
            }
            prevPos = actualPos;
            actualPos = null;
            // If the agent has not seen the car in the recent frames
        } else if (prevPos == null) {
            Log.i(TAG, "Car not detected PREVPOS NULL");
            CarBrake();
            // If no car is seen inside the actual frame but it has been recognized in the recent frames
        } else {
            // If not too many frames have passed after the last sighting of the car, keep on moving
            if(numFramesNoCar < numFramesNoCarThresh){
                Log.i(TAG, "Car not detected PREVPOS OK");
                // Go towards the point in which the car should be
                float steeringAngle = (prevPos.x / (float) (w / 2)) - 1;
                steeringAngle *= 1/(numFramesNoCar+1); // Adjusted for the movement
                // Getting the distance between the car and the object in front of it
                double distanceInFront = getDist(imageDepth, prevPos);
                // If the direction in which the car is moving has an obstacle, stop
                if (distanceInFront < 4)
                    CarBrake();
                else {
                    CarAccelerate(accelerationValue, steeringAngle);
                }
                numFramesNoCar++;
            }
            // If too many frames have passed, stop and wait for the car to appear again
            else {
                CarBrake();
            }
        }
    }

    private void RequestImages() {
        // Both depth and video images are received at the same time
        //ReceivedImage image = GetImages();
        //byte[] imageVals = image.videoImage;
        //float[] imageDepth = image.depthImage;
        //float[] depth = GetDepth();
        /// Test ///
        //GetImage(imageMat.getNativeObjAddr(), imageDepth.getNativeObjAddr());

        /*
        // Requesting the depth image
        final Handler depthHandler = new Handler();
        depthHandler.post(new Runnable() {
            public void run() {
                imageDepth = GetDepth();
                depthFlag = true;
            }
        });
        // Requesting the scene image
        final Handler imageHandler = new Handler();
        imageHandler.post(new Runnable() {
            public void run() {
                GetScene(imageMat.getNativeObjAddr());
                imageFlag = true;
            }
        });
        */
        // KMS
        final Handler imagesHandler = new Handler();
        imagesHandler.post(new Runnable() {
            public void run() {
                imageDepth = GetImage(imageMat.getNativeObjAddr());
                imageFlag = true;
                depthFlag = true;
            }
        });
    }

    private double getDist(float[] imageDepth, Point pos) {
        return imageDepth[w * h - 1 - (pos.x + w * pos.y)];
        //return imageDepth.get((int)pos.x,(int)pos.y)[0];
    }
}