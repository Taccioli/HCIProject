package com.pervasive.airsimtracker;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.ColorSpace;
import android.os.Build;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;
import android.widget.ToggleButton;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import com.example.helloairsim.TaskRunner;
import java.nio.IntBuffer;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Mat;

public class MainActivity extends AppCompatActivity {
    private ImageView imageView;
    private Bitmap bitmap;
    private static final String TAG = "AirClass::MainActivity";
    private static final int w = 256, h = 144; // Width and height of the car images
    private static ToggleButton connectButton;
    public native boolean CarConnect();
    public native byte[] GetImage(ImageSize intImage);

    static {
        System.loadLibrary("carclient");
    }

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        connectButton = (ToggleButton) findViewById(R.id.connectButton);
        bitmap = null;
    }

    @Override
    public void onResume(){
        super.onResume();
        // In realtà questa parte ora non ci serve più - perché un tempo le openCV dovevano essere installate con
        // Un pacchetto dal play store, ma ora non è più così
        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "internal OPenCV library not found. Using OpenCV Manager for initialization.");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, mLoaderCallback);
        } else {
            Log.d(TAG, "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
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
                    final int delay = 33; // ms
                    // Generating the object to control the Bitmap options
                    BitmapFactory.Options options = new BitmapFactory.Options();
                    options.inJustDecodeBounds = false;
                    options.outHeight = h;
                    options.outWidth = w;
                    options.inBitmap = bitmap;
                    // Needed to show the stream of images
                    ImageView imageView = (ImageView) findViewById(R.id.cameraImage);
                    handler.postDelayed(new Runnable() {
                        public void run() {
                            ImageSize intImage = new ImageSize();
                            byte[] imageVals = GetImage(intImage);
                            int length = imageVals.length;
                            // Converting the received byte[] into Bitmap (to display the image)
                            bitmap = BitmapFactory.decodeByteArray(imageVals, 0, length, options);
                            // Display the image
                            imageView.setImageBitmap(bitmap);
                            // Converting the Bitmap into Mat (to use opencv image classification)
                            Mat mat = new Mat();
                            Utils.bitmapToMat(bitmap, mat);
                            // .. Image classification and car controls? //
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
}