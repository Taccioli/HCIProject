package com.pervasive.airsimtracker.cnn.impl;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.util.Log;
import androidx.annotation.RequiresApi;
import com.pervasive.airsimtracker.cnn.CNNExtractorService;
import org.opencv.android.Utils;
import org.opencv.core.*;
import org.opencv.dnn.Dnn;
import org.opencv.dnn.Net;
import org.opencv.imgproc.Imgproc;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CNNExtractorServiceImpl implements CNNExtractorService {

    public native void suppress(float[] dr, float[] ds, float overlap_thresh);
    private static final int TARGET_IMG_WIDTH = 416; // 416
    private static final int TARGET_IMG_HEIGHT = 416; // 416
    private static final double SCALE_FACTOR = 1/255.0;
    private static final Scalar MEAN =  new Scalar(0.485, 0.456, 0.406);
    private static final Scalar STD =  new Scalar(0.229, 0.224, 0.225);
    private static final String[] labels = new String[]
            {
                    "aeroplane", "bicycle", "bird", "boat", "bottle",
                    "bus", "car", "cat", "chair", "cow",
                    "diningtable", "dog", "horse", "motorbike", "person",
                    "pottedplant", "sheep", "sofa", "train", "tvmonitor"
            };

    public int gridHeight = 13;
    public int gridWidth = 13;
    public float confidenceThreshold = 0.60f;
    public float overlapThresh = 0.1f;

    private String TAG;
    @Override
    public Net getConvertedNet(String clsModelPath, String tag) {
        TAG = tag;
        Net convertedNet = Dnn.readNetFromONNX(clsModelPath);
        Log.i(TAG, "Network was successfully loaded");
        return convertedNet;
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public String getPredictedLabel(Mat inputImage, Net dnnNet, String classesPath) {
        //preprocess input frame
        Mat inputBlob = getPreprocessedImage(inputImage);
        // set OpenCV model input
        dnnNet.setInput(inputBlob);
        // provide inference
        Mat classification = dnnNet.forward();
        // Mat adjBlob = classification.reshape(1,125);
        return getPredictedBoxes(classification, classesPath);
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private String getPredictedBoxes(Mat features, String classesPath) {
        int numClasses = 20;
        int blockSize  = 32;
        float[] anchors = {1.08f, 1.19f, 3.42f, 4.41f, 6.63f, 11.38f, 9.42f, 5.11f, 16.62f, 10.52f};

        // ArrayList<Prediction> predictions = new ArrayList<Prediction>();
        ArrayList<Integer> classIndexes = new ArrayList<>();
        ArrayList<Double> scores = new ArrayList<>();
        ArrayList<Rect> rectangles = new ArrayList<Rect>();
        // For each square part which divides the whole image ..
        for (int c = 0; c < 169; c++) {
            int rows = features.rows();
            int cols = features.cols();
            Mat boxFeatures = features.col(c);
            Mat loxFeatures = features.row(c);
            Mat feat =  new Mat();
            features.convertTo(feat, CvType.CV_32FC3);
            // ..and for each bounding box, we have to check every data element
                for (int b = 0; b < 5; b++) {
                    int channel = b * (numClasses + 5);

                    double tx = boxFeatures.get(25*b,0)[0];
                    double ty = boxFeatures.get(25*b+1,0)[0];
                    double tw = boxFeatures.get(25*b+2,0)[0];
                    double th = boxFeatures.get(25*b+3,0)[0];
                    double tc = boxFeatures.get(25*b+4,0)[0];
                    // tc = Math.abs(tc);
                    int x = (int)(c-c%13 + sigmoid(tx)) * blockSize;
                    int y = (int)((int)(c/13) + sigmoid(ty)) * blockSize;

                    int w = (int)(Math.exp(tw) * anchors[2*b    ] * blockSize);
                    int h = (int)(Math.exp(th) * anchors[2*b + 1] * blockSize);

                    double confidence = sigmoid(tc);


                    // Gather the predicted classes for this anchor box and softmax them,
                    // so we can interpret these numbers as percentages.
                    double[] classes = new double[20];
                    Mat prob = boxFeatures.submat(b*25+5,b*25+25,0,1);
                    for (int cl = 0; cl < numClasses; cl++) {
                        classes[cl] = prob.get(cl, 0)[0];
                    }
                    classes = softMax(classes);

                    // Find the index of the class with the largest score.
                    int detectedClass = 0;
                    for (int i = 0; i < classes.length; i++) {
                        detectedClass = classes[i] > classes[detectedClass] ? i : detectedClass;
                    }
                    double bestClassScore = classes[detectedClass];

                    // Combine the confidence score for the bounding box, which tells us
                    // how likely it is that there is an object in this box (but not what
                    // kind of object it is), with the largest class prediction, which
                    // tells us what kind of object it detected (but not where).
                    double confidenceInClass = bestClassScore * confidence;

                    // Since we compute 13x13x5 = 845 bounding boxes, we only want to
                    // keep the ones whose combined score is over a certain threshold.
                    if (confidenceInClass > confidenceThreshold) {
                        Rect rect = new Rect(x-w/2, y - h/2, w, h);
                        // Prediction prediction = new Prediction(detectedClass, confidenceInClass, rect);
                        // predictions.add(prediction);
                        classIndexes.add(detectedClass);
                        scores.add(confidenceInClass);
                        rectangles.add(rect);
                    }
                }
        }
        // We already filtered out any bounding boxes that have very low scores,
        // but there still may be boxes that overlap too much with others. We'll
        // use "non-maximum suppression" to prune those duplicate bounding boxes.
        int[] dr = new int[classIndexes.size()];
        int i = 0;
        for (Integer f : classIndexes) {
            dr[i++] = (f != null ? f : 0); // Or whatever default you want.
        }

        /*int[] ds = new int[classIndexes.size()];
        int i = 0;
        for (Integer f : classIndexes) {
            ds[i++] = (f != null ? f : 0); // Or whatever default you want.
        }
        suppress(dr, ds, overlapThresh);
        result.predictions = nonMaxSuppression(boxes: predictions, limit: YOLO.maxBoundingBoxes, threshold: iouThreshold)
        //result.debugTexture = model.image(for: resized, inflightIndex: inflightIndex).texture
        return result*/
        return null;
    }

    class Prediction{
        int classIndex;
        double score;
        Rect rect;

        public Prediction(int classIndex, double score, Rect rect) {
            this.classIndex = classIndex;
            this.score = score;
            this.rect = rect;
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    public double[] softMax(double[] arr) {
        int length = arr.length;
        double max = Arrays.stream(arr).max().getAsDouble();
        double[] exp_a = new double[arr.length];
        double sum = 0;
        for (int i = 0; i < length; i++) {
            exp_a[i] = Math.exp(arr[i] - max);
            sum += exp_a[i];
        }
        double[] result = new double[length];
        for (int i = 0; i < length; ++i) {
            result[i] = exp_a[i]/sum;
        }
        return result;
    }


    private static double sigmoid(double x) {
        return 1 / (1 + Math.exp(-x));
    }

    private String getPredictedClass(Mat classificationResult, String classesPath) {
        ArrayList<String> imgLabels = getImgLabels(classesPath);
        if(imgLabels.isEmpty()){
            return "Empty labels";
        }
        Core.MinMaxLocResult mm = Core.minMaxLoc(classificationResult);
        double maxValIndex = mm.maxLoc.x;
        return imgLabels.get((int)maxValIndex);
    }

    private ArrayList<String> getImgLabels(String imgLabelsFilePath) {
        ArrayList<String> imgLabels = new ArrayList();
        BufferedReader bufferReader;
        try {
            bufferReader = new BufferedReader(new FileReader(imgLabelsFilePath));
            String fileLine;
            while((fileLine = bufferReader.readLine())!=null)
                imgLabels.add(fileLine);
        } catch (IOException ex) {
            Log.i(TAG, "ImageNet classes were not found");
        }
        return imgLabels;
    }

    private Mat getPreprocessedImage(Mat image) {
        Imgproc.cvtColor(image, image, Imgproc.COLOR_RGBA2RGB);
        Mat imgFloat = new Mat(image.rows(), image.cols(), CvType.CV_32FC3);
        image.convertTo(imgFloat, CvType.CV_32FC3, SCALE_FACTOR);
        Imgproc.resize(imgFloat, imgFloat, new Size(416,416));
        imgFloat = centerCrop(imgFloat);
        Mat blob = Dnn.blobFromImage(
                imgFloat,
                1.0,
                new Size(TARGET_IMG_WIDTH, TARGET_IMG_HEIGHT),
                MEAN,
                true,
                false
        );
        Core.divide(blob, STD, blob);
        return blob;
    }

    private Mat centerCrop(Mat inputImage) {
        int y1 = Math.round((inputImage.rows() - TARGET_IMG_HEIGHT)/2);
        int y2 = Math.round(y1 + TARGET_IMG_HEIGHT);
        int x1 = Math.round((inputImage.cols() - TARGET_IMG_WIDTH)/2);
        int x2 = Math.round(x1 + TARGET_IMG_WIDTH);
        Rect centerRect = new Rect(x1, y1, (x2-x1), (y2-y1));
        Mat croppedImage = new Mat(inputImage, centerRect);
        return croppedImage;
    }
}



