package com.example.edgedetection;

import android.content.Context;
import android.util.Size;

import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CameraController {
    private Context context;
    private FrameProcessor frameProcessor;
    private ExecutorService cameraExecutor;
    
    public interface FrameProcessor {
        void processFrame(byte[] data, int width, int height, int rotation);
    }
    
    public CameraController(Context context, FrameProcessor processor) {
        this.context = context;
        this.frameProcessor = processor;
        this.cameraExecutor = Executors.newSingleThreadExecutor();
    }
    
    public void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = 
                ProcessCameraProvider.getInstance(context);
                
        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                
                Preview preview = new Preview.Builder().build();
                
                ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                        .setTargetResolution(new Size(640, 480))
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build();
                        
                imageAnalysis.setAnalyzer(cameraExecutor, new YuvToRgbConverter(frameProcessor));
                
                CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;
                
                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(
                        (LifecycleOwner) context,
                        cameraSelector,
                        preview,
                        imageAnalysis);
                        
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, ContextCompat.getMainExecutor(context));
    }
}