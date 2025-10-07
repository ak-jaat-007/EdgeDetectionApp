cmake_minimum_required(VERSION 3.18.1)

project("edgedetection")

# Find OpenCV
find_package(OpenCV REQUIRED)

# Include directories
include_directories(${OpenCV_INCLUDE_DIRS})

# Create shared library
add_library(
    edgedetection
    SHARED
    native-lib.cpp
    edge-detector.cpp
)

# Link libraries
target_link_libraries(
    edgedetection
    android
    log
    jnigraphics
    ${OpenCV_LIBS}
)