package com.example.edgedetectionapp;

import android.Manifest;
import android.content.pm.PackageManager;
import android.media.Image;
import android.media.ImageReader;
import android.os.Bundle;
import android.util.Log;
import android.opengl.GLSurfaceView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {
    private static final int REQ_CAMERA = 1001;
    private static final String TAG = "MainActivity";

    private GLSurfaceView glSurfaceView;
    private MyGLRenderer renderer;
    private Camera2Controller cameraController;
    private ExecutorService cameraExecutor;
    private TextView fpsText;

    static {
        System.loadLibrary("edgedetectionapp");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        glSurfaceView = findViewById(R.id.glSurfaceView);
        fpsText = findViewById(R.id.fpsText);

        glSurfaceView.setEGLContextClientVersion(2);
        renderer = new MyGLRenderer();
        glSurfaceView.setRenderer(renderer);
        glSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);

        cameraExecutor = Executors.newSingleThreadExecutor();

        // THIS IS THE CRITICAL FIX: We must wait for the view to be ready.
        glSurfaceView.post(() -> {
            if (!hasCameraPermission()) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, REQ_CAMERA);
            } else {
                openCameraAndStart();
            }
        });
    }

    private boolean hasCameraPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
    }

    private void openCameraAndStart() {
        cameraController = new Camera2Controller(this, null, reader -> {
            try (Image image = reader.acquireLatestImage()) {
                if (image == null) return;

                processFrame(image); // native method call
                glSurfaceView.requestRender(); // Request GL render

            } catch (Exception e) {
                Log.e(TAG, "Image handling", e);
            }
        });

        cameraExecutor.execute(() -> cameraController.startCamera());
    }

    @Override protected void onPause() {
        super.onPause();
        if (cameraController != null) cameraController.stopCamera();
        if (glSurfaceView != null) glSurfaceView.onPause();
    }
    @Override protected void onResume() {
        super.onResume();
        if (glSurfaceView != null) glSurfaceView.onResume();
        // If permissions were granted while paused, start camera on resume
        if (cameraController == null && hasCameraPermission()) {
            openCameraAndStart();
        }
    }
    @Override protected void onDestroy() {
        super.onDestroy();
        cameraExecutor.shutdown();
    }

    // Native function implemented in C++
    public native void processFrame(Image image);

    @Override
    public void onRequestPermissionsResult(int requestCode,@NonNull String[] permissions,@NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_CAMERA) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openCameraAndStart();
            } else {
                finish();
            }
        }
    }
}