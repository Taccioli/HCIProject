package com.pervasive.airsimtracker;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.*;
import android.os.Build;
import android.os.Handler;
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
    private ImageView imageView;
    private Bitmap bitmap;
    private static final String TAG = "AirClass::MainActivity";
    private static final String IMAGENET_CLASSES = "imagenet_classes.txt";
    //private static final String MODEL_FILE = "tinyyolov2-7.onnx";
    private static final int w = 640, h = 480;  // Width and height of the car images
    private static ToggleButton connectButton;
    private Net opencvNet;
    private CNNExtractorService cnnService;
    private DetectorActivity detector;

    public native boolean CarConnect();
    public native byte[] GetImage();
    public native byte[] GetDepth();
    public String classesPath;
    public String onnxModelPath;
    static {
        System.loadLibrary("carclient");
    }

    /*
    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                    Log.i(TAG, "OpenCV loaded successfully.");
                    break;
                default:
                    super.onManagerConnected(status);
            }
        }
    };
     */

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "called onCreate");
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_main);

        connectButton = (ToggleButton) findViewById(R.id.connectButton);
        bitmap = null;

        //this.cnnService = new CNNExtractorServiceImpl();
        //this.classesPath = getPath(IMAGENET_CLASSES, this);
        //this.onnxModelPath = getPath(MODEL_FILE, this);
        this.detector = new DetectorActivity();
    }

    @Override
    public void onResume(){
        super.onResume();
        /*
        // In realtà questa parte ora non ci serve più - perché un tempo le openCV dovevano essere installate con
        // Un pacchetto dal play store, ma ora non è più così
        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "internal OPenCV library not found. Using OpenCV Manager for initialization.");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, mLoaderCallback);
        } else {
            Log.d(TAG, "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
        */
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

                    /* Old implementation
                    // After connecting, the opencv cnn is loaded
                    if(onnxModelPath.trim().isEmpty()){
                        Log.i(TAG, "Failed to get model file");
                        return;
                    }
                    opencvNet = cnnService.getConvertedNet(onnxModelPath, TAG);
                    */

                    final Handler handler = new Handler();
                    // Defines the interval of time passing between each frame request
                    final int delay = 33; // ms
                    // Generating the object to control the Bitmap options
                    BitmapFactory.Options options = new BitmapFactory.Options();
                    options.inJustDecodeBounds = false;
                    options.outHeight = h;
                    options.outWidth = w;
                    options.inBitmap = bitmap;
                    options.inMutable = true;
                    // Needed to show the stream of images
                    ImageView imageView = (ImageView) findViewById(R.id.cameraImage);
                    handler.postDelayed(new Runnable() {
                        public void run() {
                            byte[] imageVals = GetImage();
                            byte[] imageDepth = GetDepth();
                            int length = imageVals.length;
                            // Converting the received byte[] into Bitmap (to display and process the image)
                            bitmap = BitmapFactory.decodeByteArray(imageVals, 0, length, options);
                            // .. Image classification and car controls? //
                            // This gives us the point in which we found the car
                            Point pos = detector.processImage(MainActivity.this, bitmap);
                            if(pos != null)
                                // Go towards the point in which there should be the car

                            /* Old way to do it (Not working)
                            // Converting the Bitmap into Mat (to use opencv image classification)
                            // Mat mat = new Mat();
                            // Utils.bitmapToMat(bitmap, mat);
                            // String predictedClass = cnnService.getPredictedLabel(mat, opencvNet, classesPath); */

                            // Display the image
                            imageView.setImageBitmap(bitmap);
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

    private static String getPath(String file, Context context){
        AssetManager assetManager = context.getAssets();
        BufferedInputStream inputStream;
        try {
            inputStream = new BufferedInputStream(assetManager.open(file));
            byte[] data = new byte[inputStream.available()];
            inputStream.read(data);
            inputStream.close();
            // Create copy in storage
            File outFile = new File(context.getFilesDir(), file);
            FileOutputStream os = new FileOutputStream(outFile);
            os.write(data);
            os.close();
            return outFile.getAbsolutePath();
        } catch (IOException ex){
            Log.i(TAG, "Failed to upload a file");
        }
        return "";
    }
}