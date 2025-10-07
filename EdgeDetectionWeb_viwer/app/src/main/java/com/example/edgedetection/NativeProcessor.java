package com.example.edgedetection;

public class NativeProcessor {
    private byte[] processedData;
    private int processedWidth;
    private int processedHeight;
    
    public native void processFrame(byte[] data, int width, int height, int rotation);
    public native void release();
    
    public byte[] getProcessedData() {
        return processedData;
    }
    
    public int getProcessedWidth() {
        return processedWidth;
    }
    
    public int getProcessedHeight() {
        return processedHeight;
    }
    
    // Called from JNI to set processed data
    private void setProcessedData(byte[] data, int width, int height) {
        this.processedData = data;
        this.processedWidth = width;
        this.processedHeight = height;
    }
}