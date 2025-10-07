#include "edge-detector.h"
#include <opencv2/opencv.hpp>

EdgeDetector::EdgeDetector() : initialized(false) {
}

EdgeDetector::~EdgeDetector() {
    release();
}

cv::Mat EdgeDetector::processFrame(const cv::Mat& inputFrame) {
    if (inputFrame.empty()) {
        return inputFrame;
    }
    
    // Resize for performance if needed
    cv::Mat resizedFrame;
    cv::resize(inputFrame, resizedFrame, cv::Size(640, 480));
    
    // Apply edge detection
    cv::Mat edges = applyCannyEdgeDetection(resizedFrame);
    
    // Convert single channel to 3 channels for display
    cv::Mat result;
    cv::cvtColor(edges, result, cv::COLOR_GRAY2BGR);
    
    return result;
}

cv::Mat EdgeDetector::applyCannyEdgeDetection(const cv::Mat& inputFrame) {
    cv::Mat gray, blur, edges;
    
    // Convert to grayscale
    cv::cvtColor(inputFrame, gray, cv::COLOR_BGR2GRAY);
    
    // Apply Gaussian blur to reduce noise
    cv::GaussianBlur(gray, blur, cv::Size(5, 5), 1.5);
    
    // Apply Canny edge detection
    cv::Canny(blur, edges, 50, 150);
    
    return edges;
}

cv::Mat EdgeDetector::applyGrayscale(const cv::Mat& inputFrame) {
    cv::Mat gray;
    cv::cvtColor(inputFrame, gray, cv::COLOR_BGR2GRAY);
    cv::cvtColor(gray, gray, cv::COLOR_GRAY2BGR);
    return gray;
}

void EdgeDetector::release() {
    previousFrame.release();
    initialized = false;
}