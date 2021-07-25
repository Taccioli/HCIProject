extern "C" {
    JNIEXPORT jboolean JNICALL Java_com_pervasive_airsimtracker_MainActivity_CarConnect(JNIEnv *env, jobject);
    JNIEXPORT jbyteArray JNICALL Java_com_pervasive_airsimtracker_MainActivity_GetImage(JNIEnv *env, jobject javaThis, jobject obj);
    JNIEXPORT jobject JNICALL Java_com_pervasive_airsimtracker_MainActivity_GetImages(JNIEnv *env, jobject javaThis, jobject obj);
    JNIEXPORT jfloatArray JNICALL Java_com_pervasive_airsimtracker_MainActivity_GetDepth(JNIEnv *env, jobject javaThis, jobject obj);
    JNIEXPORT void JNICALL Java_com_pervasive_airsimtracker_MainActivity_CarSteering(JNIEnv *env, jobject javaThis, jfloat steeringAngle);
    JNIEXPORT void JNICALL Java_com_pervasive_airsimtracker_MainActivity_CarAccelerate(JNIEnv *env, jobject javaThis, jfloat throttle, jfloat steeringAngle);
    JNIEXPORT void JNICALL Java_com_pervasive_airsimtracker_MainActivity_CarDecelerate(JNIEnv *env, jobject javaThis, jfloat throttle, jfloat steeringAngle);
    JNIEXPORT void JNICALL Java_com_pervasive_airsimtracker_MainActivity_CarBrake(JNIEnv *env, jobject javaThis);
    JNIEXPORT jfloat JNICALL Java_com_pervasive_airsimtracker_MainActivity_GetCarSpeed(JNIEnv *env, jobject javaThis);
}