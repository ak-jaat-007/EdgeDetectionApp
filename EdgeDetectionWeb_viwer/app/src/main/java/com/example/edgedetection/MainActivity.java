package com.example.edgedetection;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.common.util.concurrent.ListenableFuture;

import java.nio.ByteBuffer;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "EdgeDetectionApp";
    private static final int PERMISSION_REQUEST_CODE = 1001;

    private GLSurfaceView glSurfaceView;
    private Button toggleButton;
    private TextView fpsTextView;
    private TextView statusTextView;
    
    private CameraController cameraController;
    private GLRenderer glRenderer;
    private NativeProcessor nativeProcessor;
    
    private ExecutorService cameraExecutor;
    private boolean isEdgeDetectionEnabled = true;
    private long frameCount = 0;
    private long lastFpsUpdate = 0;
    
    // Load native library
    static {
        System.loadLibrary("edgedetection");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        initializeViews();
        setupNativeProcessor();
        checkPermissions();
    }
    
    private void initializeViews() {
        glSurfaceView = findViewById(R.id.glSurfaceView);
        toggleButton = findViewById(R.id.toggleButton);
        fpsTextView = findViewById(R.id.fpsTextView);
        statusTextView = findViewById(R.id.statusTextView);
        
        // Setup OpenGL ES 2.0
        glSurfaceView.setEGLContextClientVersion(2);
        glRenderer = new GLRenderer(this);
        glSurfaceView.setRenderer(glRenderer);
        glSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
        
        toggleButton.setOnClickListener(v -> toggleProcessingMode());
        
        cameraExecutor = Executors.newSingleThreadExecutor();
    }
    
    private void setupNativeProcessor() {
        nativeProcessor = new NativeProcessor();
    }
    
    private void checkPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) 
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA},
                    PERMISSION_REQUEST_CODE);
        } else {
            initializeCamera();
        }
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                         @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                initializeCamera();
            } else {
                Toast.makeText(this, "Camera permission required", Toast.LENGTH_LONG).show();
            }
        }
    }
    
    private void initializeCamera() {
        cameraController = new CameraController(this, new FrameProcessor() {
            @Override
            public void processFrame(byte[] data, int width, int height, int rotation) {
                processCameraFrame(data, width, height, rotation);
            }
        });
        cameraController.startCamera();
    }
    
    private void processCameraFrame(byte[] data, int width, int height, int rotation) {
        frameCount++;
        long currentTime = System.currentTimeMillis();
        
        if (currentTime - lastFpsUpdate >= 1000) {
            updateFps(currentTime);
        }
        
        if (isEdgeDetectionEnabled) {
            // Process frame with native code
            nativeProcessor.processFrame(data, width, height, rotation);
            
            // Get processed data and update OpenGL texture
            byte[] processedData = nativeProcessor.getProcessedData();
            int processedWidth = nativeProcessor.getProcessedWidth();
            int processedHeight = nativeProcessor.getProcessedHeight();
            
            if (processedData != null) {
                runOnUiThread(() -> {
                    glRenderer.updateTexture(processedData, processedWidth, processedHeight);
                    glSurfaceView.requestRender();
                });
            }
        } else {
            // Pass raw data to OpenGL
            runOnUiThread(() -> {
                glRenderer.updateTexture(data, width, height);
                glSurfaceView.requestRender();
            });
        }
    }
    
    private void updateFps(long currentTime) {
        final float fps = (frameCount * 1000f) / (currentTime - lastFpsUpdate);
        runOnUiThread(() -> fpsTextView.setText(String.format("FPS: %.1f", fps)));
        frameCount = 0;
        lastFpsUpdate = currentTime;
    }
    
    private void toggleProcessingMode() {
        isEdgeDetectionEnabled = !isEdgeDetectionEnabled;
        String mode = isEdgeDetectionEnabled ? "Edge Detection" : "Raw Feed";
        toggleButton.setText(mode);
        statusTextView.setText("Mode: " + mode);
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (cameraExecutor != null) {
            cameraExecutor.shutdown();
        }
        if (nativeProcessor != null) {
            nativeProcessor.release();
        }
    }
}