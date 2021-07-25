package com.pervasive.airsimtracker;

public class ReceivedImage {
    public byte[] videoImage;
    public float[] depthImage;

    public ReceivedImage(byte[] videoImage, float[] depthImage) {
        this.videoImage = videoImage;
        this.depthImage = depthImage;
    }

    public ReceivedImage() {
        this.videoImage = new byte[]{};
        this.depthImage = new float[]{};
    }
}
