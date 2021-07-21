#include <jni.h>
#include "carclient.h"
#include <vehicles/car/api/CarRpcLibClient.hpp>
using namespace msr::airlib;
using std::cin;
using std::cout;
using std::endl;
using std::vector;
using namespace msr::airlib;
typedef ImageCaptureBase::ImageRequest ImageRequest;
typedef ImageCaptureBase::ImageResponse ImageResponse;
typedef ImageCaptureBase::ImageType ImageType;

CarRpcLibClient * m_client;

JNIEXPORT jboolean JNICALL Java_com_pervasive_airsimtracker_MainActivity_CarConnect(JNIEnv *env, jobject)
{
    m_client = new CarRpcLibClient("10.0.2.2");
    m_client->confirmConnection();
    m_client->enableApiControl(true, "Car1");
    bool isEnabled = m_client -> isApiControlEnabled();
    return isEnabled;
}

JNIEXPORT jbyteArray JNICALL Java_com_pervasive_airsimtracker_MainActivity_GetImage(JNIEnv *env, jobject javaThis, jobject obj)
{
    // Check if a client has been instantiated
    if (!m_client)
        return;

    std::vector<ImageRequest> request = {
            //png format
            ImageRequest("0", ImageType::Scene),
            //uncompressed RGB array bytes
            //ImageRequest("1", ImageType::Scene, false, false)//,
            //floating point uncompressed image
            //ImageRequest("1", ImageType::DepthPlanar, true)
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

        return image_byte_array;
    }
    return;
}

JNIEXPORT jbyteArray JNICALL Java_com_pervasive_airsimtracker_MainActivity_GetDepth(JNIEnv *env, jobject javaThis, jobject obj)
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
            ImageRequest("1", ImageType::DepthPerspective),
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

        return image_byte_array;
    }
    return;
}

