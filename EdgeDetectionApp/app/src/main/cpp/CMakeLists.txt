cmake_minimum_required(VERSION 3.22.1)
project("edgedetectionapp")

# The set(CMAKE_ANDROID_API 26) line is no longer needed

set(OpenCV_DIR C:/Users/amank/Downloads/opencv-4.12.0-android-sdk/OpenCV-android-sdk/sdk/native/jni)
list(APPEND CMAKE_FIND_ROOT_PATH ${OpenCV_DIR})
find_package(OpenCV REQUIRED)

add_library(${CMAKE_PROJECT_NAME} SHARED
        native-lib.cpp)

target_link_libraries(${CMAKE_PROJECT_NAME}
        ${OpenCV_LIBS}
        GLESv2
        mediandk
        android
        log)