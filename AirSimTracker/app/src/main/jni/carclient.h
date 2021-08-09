#include <jni.h>
#include <opencv2/core.hpp>
#include <opencv2/imgproc.hpp>
#include <opencv2/features2d.hpp>
#include <vector>

using namespace std;
using namespace cv;
extern "C" {
    JNIEXPORT jboolean JNICALL Java_com_pervasive_airsimtracker_MainActivity_CarConnect(JNIEnv *env, jobject);
    //JNIEXPORT void JNICALL Java_com_pervasive_airsimtracker_MainActivity_GetImage(JNIEnv *env, jobject javaThis, jlong addrImg, jlong addrDepth);
    JNIEXPORT jfloatArray JNICALL Java_com_pervasive_airsimtracker_MainActivity_GetImage(JNIEnv *env, jobject javaThis, jlong addrImg);
    JNIEXPORT jobject JNICALL Java_com_pervasive_airsimtracker_MainActivity_GetImages(JNIEnv *env, jobject javaThis, jobject obj);
    JNIEXPORT jfloatArray JNICALL Java_com_pervasive_airsimtracker_MainActivity_GetDepth(JNIEnv *env, jobject javaThis, jobject obj);
    JNIEXPORT void JNICALL Java_com_pervasive_airsimtracker_MainActivity_CarSteering(JNIEnv *env, jobject javaThis, jfloat steeringAngle);
    JNIEXPORT void JNICALL Java_com_pervasive_airsimtracker_MainActivity_CarAccelerate(JNIEnv *env, jobject javaThis, jfloat throttle, jfloat steeringAngle);
    JNIEXPORT void JNICALL Java_com_pervasive_airsimtracker_MainActivity_CarDecelerate(JNIEnv *env, jobject javaThis, jfloat throttle, jfloat steeringAngle);
    JNIEXPORT void JNICALL Java_com_pervasive_airsimtracker_MainActivity_CarBrake(JNIEnv *env, jobject javaThis);
    JNIEXPORT jfloat JNICALL Java_com_pervasive_airsimtracker_MainActivity_GetCarSpeed(JNIEnv *env, jobject javaThis);
}