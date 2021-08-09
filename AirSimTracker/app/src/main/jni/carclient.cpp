#include <jni.h>
#include "carclient.h"
#include <vehicles/car/api/CarRpcLibClient.hpp>
#include <opencv2/core.hpp>
#include <opencv2/core/core.hpp>
#include <opencv2/core/mat.hpp>
#include <opencv2/imgcodecs.hpp>
#include <opencv2/imgproc.hpp>
#include <opencv2/features2d.hpp>
#include <vector>
using namespace msr::airlib;
using std::cin;
using std::cout;
using std::endl;
using std::vector;
using namespace std;
using namespace cv;
typedef ImageCaptureBase::ImageRequest ImageRequest;
typedef ImageCaptureBase::ImageResponse ImageResponse;
typedef ImageCaptureBase::ImageType ImageType;

CarRpcLibClient * m_client;

JNIEXPORT jboolean JNICALL Java_com_pervasive_airsimtracker_MainActivity_CarConnect(JNIEnv *env, jobject)
{
    //m_client = new CarRpcLibClient("192.168.1.101");
    m_client = new CarRpcLibClient("192.168.1.89");
    m_client->confirmConnection();
    m_client->enableApiControl(true, "Car1");
    bool isEnabled = m_client -> isApiControlEnabled();
    return isEnabled;
}

/*JNIEXPORT void JNICALL Java_com_pervasive_airsimtracker_MainActivity_GetImage(JNIEnv *env, jobject javaThis, jlong addrImg, jlong addrDepth)
{
    // Check if a client has been instantiated
    if (!m_client)
        return;

    cv::Mat& img = *(Mat*)addrImg;
    cv::Mat& depth = *(Mat*)addrDepth;

    std::vector<ImageRequest> request = {
            ImageRequest("0", ImageType::Scene, false),
            ImageRequest("1", ImageType::DepthPerspective, true, true),
    };

    const std::vector<ImageResponse>& response = m_client -> simGetImages(request);
    // assert(response.size() > 0);
    if(response.size() > 0) {
        img = imdecode(response.at(0).image_data_uint8, ImreadModes::IMREAD_COLOR);
        cv::Mat temp(response.at(1).height, response.at(1).width, CV_32FC1,(void*)response.at(1).image_data_float.data());
        depth = temp;
    }
}*/

JNIEXPORT jfloatArray JNICALL Java_com_pervasive_airsimtracker_MainActivity_GetImage(JNIEnv *env, jobject javaThis, jlong addrImg)
{
    // Check if a client has been instantiated
    if (!m_client)
        return;

    cv::Mat& img = *(Mat*)addrImg;

    std::vector<ImageRequest> request = {
            ImageRequest("0", ImageType::Scene, false),
            ImageRequest("1", ImageType::DepthPerspective, true, true),
    };

    const std::vector<ImageResponse>& response = m_client -> simGetImages(request);
    // assert(response.size() > 0);
    if(response.size() > 0) {
        img = imdecode(response.at(0).image_data_uint8, ImreadModes::IMREAD_COLOR);

        const ImageResponse& image_info = response[1];
        std::vector<float> image_float = image_info.image_data_float;
        // Create a jbyteArray
        jfloatArray image_jbfloat_array = env->NewFloatArray(image_float.size());
        // Cast from uint8_t vector to uint8_t array
        float* image_float_array = &image_float[0];
        // Fill jbyteArray
        env -> SetFloatArrayRegion(image_jbfloat_array, 0, image_float.size(), reinterpret_cast<jfloat *>(image_float_array));

        return image_jbfloat_array;
    }
    return;
}

JNIEXPORT jobject JNICALL Java_com_pervasive_airsimtracker_MainActivity_GetImages(JNIEnv *env, jobject javaThis, jobject obj)
{
    // Check if a client has been instantiated
    if (!m_client)
        return;

    jclass clazz = env->FindClass("com/pervasive/airsimtracker/ReceivedImage");
    jmethodID constructor = env->GetMethodID(clazz, "<init>", "()V");
    jobject recImg = env->NewObject(clazz, constructor);

    std::vector<ImageRequest> request = {
            ImageRequest("0", ImageType::Scene),
            ImageRequest("1", ImageType::DepthPerspective, true),
    };

    const std::vector<ImageResponse>& response = m_client -> simGetImages(request);

    if(response.size() > 0) {
        const ImageResponse& image_info = response[0];

        std::vector<uint8_t> image_uint8 = image_info.image_data_uint8;
        // Create a jbyteArray
        jbyteArray image_byte_array = env->NewByteArray(image_uint8.size());
        // Cast from uint8_t vector to uint8_t array
        uint8_t* image_uint8_array = &image_uint8[0];
        // Fill jbyteArray
        env -> SetByteArrayRegion(image_byte_array, 0, image_uint8.size(), reinterpret_cast<jbyte *>(image_uint8_array));

        jfieldID param1Field = env->GetFieldID(clazz, "videoImage", "[B");
        env->SetObjectField(recImg, param1Field, image_byte_array);

        const ImageResponse& image_info1 = response[1];
        std::vector<float> image_float = image_info1.image_data_float;
        // Create a jfloatArray
        jfloatArray image_jbfloat_array = env->NewFloatArray(image_float.size());
        // Cast from uint8_t vector to uint8_t array
        float* image_float_array = &image_float[0];
        // Fill jbyteArray
        env -> SetFloatArrayRegion(image_jbfloat_array, 0, image_float.size(), reinterpret_cast<jfloat *>(image_float_array));

        jfieldID param2Field = env->GetFieldID(clazz, "depthImage", "[F");
        env->SetObjectField(recImg, param2Field, image_jbfloat_array);
    }
    return recImg;
}

JNIEXPORT jfloatArray JNICALL Java_com_pervasive_airsimtracker_MainActivity_GetDepth(JNIEnv *env, jobject javaThis, jobject obj)
{
    // Check if a client has been instantiated
    if (!m_client)
        return;

    std::vector<ImageRequest> request = {
            //png format
            //ImageRequest("0", ImageType::Scene),
            //uncompressed RGB array bytes
            //ImageRequest("1", ImageType::Scene, false, false)//,
            //floating point uncompressed image
            ImageRequest("1", ImageType::DepthPerspective, true),
        };

    const std::vector<ImageResponse>& response = m_client -> simGetImages(request);
    if(response.size() > 0) {
        const ImageResponse& image_info = response[0];

        std::vector<float> image_float = image_info.image_data_float;
        // Create a jbyteArray
        jfloatArray image_jbfloat_array = env->NewFloatArray(image_float.size());
        // Cast from uint8_t vector to uint8_t array
        float* image_float_array = &image_float[0];
        // Fill jbyteArray
        env -> SetFloatArrayRegion(image_jbfloat_array, 0, image_float.size(), reinterpret_cast<jfloat *>(image_float_array));

        return image_jbfloat_array;
    }
    return;
}

JNIEXPORT void JNICALL Java_com_pervasive_airsimtracker_MainActivity_CarSteering(JNIEnv *env, jobject javaThis, jfloat steeringAngle)
{
    if (!m_client)
        return;
    CarApiBase::CarControls controls;
    controls.steering = steeringAngle;
    m_client->setCarControls(controls);
}

JNIEXPORT void JNICALL Java_com_pervasive_airsimtracker_MainActivity_CarAccelerate(JNIEnv *env, jobject javaThis, jfloat throttle, jfloat steeringAngle)
{
    if (!m_client)
        return;
    CarApiBase::CarControls controls;
    controls.brake = 0;
    controls.throttle += throttle;
    controls.steering = steeringAngle;
    m_client->setCarControls(controls);
}

JNIEXPORT void JNICALL Java_com_pervasive_airsimtracker_MainActivity_CarDecelerate(JNIEnv *env, jobject javaThis, jfloat throttle, jfloat steeringAngle)
{
    if (!m_client)
        return;
    CarApiBase::CarControls controls;
    controls.throttle -= throttle;
    controls.steering = steeringAngle;
    m_client->setCarControls(controls);
}

JNIEXPORT void JNICALL Java_com_pervasive_airsimtracker_MainActivity_CarBrake(JNIEnv *env, jobject javaThis)
{
    if (!m_client)
        return;
    CarApiBase::CarControls controls;
    controls.brake = 1;
    controls.throttle = 0;
    m_client->setCarControls(controls);
}

JNIEXPORT jfloat JNICALL Java_com_pervasive_airsimtracker_MainActivity_GetCarSpeed(JNIEnv *env, jobject javaThis)
{
    if (!m_client)
        return;
    return m_client->getCarState().speed;
}
