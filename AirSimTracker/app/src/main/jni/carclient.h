extern "C" {
    JNIEXPORT jboolean JNICALL Java_com_pervasive_airsimtracker_MainActivity_CarConnect(JNIEnv *env, jobject);
    JNIEXPORT jbyteArray JNICALL Java_com_pervasive_airsimtracker_MainActivity_GetImage(JNIEnv *env, jobject javaThis, jobject obj);
    JNIEXPORT jbyteArray JNICALL Java_com_pervasive_airsimtracker_MainActivity_GetDepth(JNIEnv *env, jobject javaThis, jobject obj);
}