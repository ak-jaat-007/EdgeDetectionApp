#include <jni.h>
#include <android/bitmap.h>
#include <android/log.h>
#include <opencv2/opencv.hpp>
#include "edge-detector.h"

#define LOG_TAG "EdgeDetection"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

using namespace cv;

EdgeDetector edgeDetector;

extern "C" {

JNIEXPORT void JNICALL
Java_com_example_edgedetection_NativeProcessor_processFrame(
        JNIEnv *env,
        jobject /* this */,
        jbyteArray data,
        jint width,
        jint height,
        jint rotation) {
    
    jbyte* frameData = env->GetByteArrayElements(data, nullptr);
    if (frameData == nullptr) {
        LOGE("Failed to get frame data");
        return;
    }
    
    // Convert NV21 to Mat (YUV format)
    Mat yuvFrame(height + height/2, width, CV_8UC1, (unsigned char*)frameData);
    Mat rgbFrame;
    cvtColor(yuvFrame, rgbFrame, COLOR_YUV2RGB_NV21);
    
    // Process frame
    Mat processedFrame = edgeDetector.processFrame(rgbFrame);
    
    // Convert back to byte array
    jbyteArray result = env->NewByteArray(processedFrame.total() * processedFrame.elemSize());
    env->SetByteArrayRegion(result, 0, processedFrame.total() * processedFrame.elemSize(), 
                          (jbyte*)processedFrame.data);
    
    // Call back to Java to set processed data
    jclass clazz = env->FindClass("com/example/edgedetection/NativeProcessor");
    jmethodID methodId = env->GetMethodID(clazz, "setProcessedData", "([BII)V");
    env->CallVoidMethod(env->GetObjectClass(env), methodId, result, 
                       processedFrame.cols, processedFrame.rows);
    
    env->ReleaseByteArrayElements(data, frameData, JNI_ABORT);
    env->DeleteLocalRef(result);
}

JNIEXPORT void JNICALL
Java_com_example_edgedetection_NativeProcessor_release(
        JNIEnv *env,
        jobject /* this */) {
    edgeDetector.release();
}

}