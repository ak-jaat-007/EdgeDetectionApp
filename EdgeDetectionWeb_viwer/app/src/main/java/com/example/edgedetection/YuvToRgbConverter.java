package com.example.edgedetection;

import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;

import java.nio.ByteBuffer;

public class YuvToRgbConverter implements ImageAnalysis.Analyzer {
    private final CameraController.FrameProcessor frameProcessor;
    
    public YuvToRgbConverter(CameraController.FrameProcessor processor) {
        this.frameProcessor = processor;
    }
    
    @Override
    public void analyze(ImageProxy image) {
        try {
            ByteBuffer yBuffer = image.getPlanes()[0].getBuffer();
            ByteBuffer uBuffer = image.getPlanes()[1].getBuffer();
            ByteBuffer vBuffer = image.getPlanes()[2].getBuffer();
            
            int ySize = yBuffer.remaining();
            int uSize = uBuffer.remaining();
            int vSize = vBuffer.remaining();
            
            byte[] nv21 = new byte[ySize + uSize + vSize];
            
            // Copy Y plane
            yBuffer.get(nv21, 0, ySize);
            
            // Copy UV planes
            vBuffer.get(nv21, ySize, vSize);
            uBuffer.get(nv21, ySize + vSize, uSize);
            
            frameProcessor.processFrame(
                nv21, 
                image.getWidth(), 
                image.getHeight(), 
                image.getImageInfo().getRotationDegrees()
            );
            
        } finally {
            image.close();
        }
    }
}