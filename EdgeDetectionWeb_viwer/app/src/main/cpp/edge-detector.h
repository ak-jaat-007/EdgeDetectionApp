#ifndef EDGE_DETECTOR_H
#define EDGE_DETECTOR_H

#include <opencv2/opencv.hpp>

class EdgeDetector {
public:
    EdgeDetector();
    ~EdgeDetector();
    
    cv::Mat processFrame(const cv::Mat& inputFrame);
    void release();
    
private:
    cv::Mat applyCannyEdgeDetection(const cv::Mat& inputFrame);
    cv::Mat applyGrayscale(const cv::Mat& inputFrame);
    
    bool initialized;
    cv::Mat previousFrame;
};

#endif // EDGE_DETECTOR_H