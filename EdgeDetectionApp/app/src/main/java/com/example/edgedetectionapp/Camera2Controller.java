package com.example.edgedetectionapp;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap; // Import this
import android.media.ImageReader;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import java.util.Arrays;

public class Camera2Controller {
    private static final String TAG = "Camera2Controller";

    private final Context context;
    // This can now be null
    private final SurfaceTexture surfaceTexture;
    private final ImageReader.OnImageAvailableListener imageAvailableListener;

    private CameraDevice cameraDevice;
    private CameraCaptureSession captureSession;
    private ImageReader imageReader;
    private Size previewSize;

    private HandlerThread backgroundThread;
    private Handler backgroundHandler;

    // The surfaceTexture parameter is now annotated as nullable
    public Camera2Controller(Context context, SurfaceTexture surfaceTexture, ImageReader.OnImageAvailableListener listener) {
        this.context = context;
        this.surfaceTexture = surfaceTexture;
        this.imageAvailableListener = listener;
    }

    // ... startCamera() and stopCamera() are unchanged ...
    public void startCamera() {
        startBackgroundThread();
        openCamera();
    }

    public void stopCamera() {
        closeCamera();
        stopBackgroundThread();
    }

    private void openCamera() {
        CameraManager manager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
        try {
            String cameraId = getBackFacingCameraId(manager);
            if (cameraId == null) {
                Log.e(TAG, "No back-facing camera found.");
                return;
            }

            CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);
            previewSize = chooseOptimalSize(characteristics);

            // This block is only needed if the texture is not null
            if (surfaceTexture != null) {
                surfaceTexture.setDefaultBufferSize(previewSize.getWidth(), previewSize.getHeight());
            }

            // ImageReader for processing frames (e.g., sending to native code)
            imageReader = ImageReader.newInstance(previewSize.getWidth(), previewSize.getHeight(), ImageFormat.YUV_420_888, 2);
            imageReader.setOnImageAvailableListener(imageAvailableListener, backgroundHandler);

            if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                Log.e(TAG, "Camera permission not granted.");
                return;
            }
            manager.openCamera(cameraId, cameraStateCallback, backgroundHandler);
        } catch (CameraAccessException e) {
            Log.e(TAG, "Failed to open camera", e);
        }
    }

    // ... cameraStateCallback is unchanged ...
    private final CameraDevice.StateCallback cameraStateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice camera) {
            cameraDevice = camera;
            createCameraPreviewSession();
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice camera) {
            camera.close();
            cameraDevice = null;
        }

        @Override
        public void onError(@NonNull CameraDevice camera, int error) {
            camera.close();
            cameraDevice = null;
            Log.e(TAG, "Camera error: " + error);
        }
    };


    private void createCameraPreviewSession() {
        try {
            Surface imageReaderSurface = imageReader.getSurface();
            final CaptureRequest.Builder captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);

            // CRITICAL CHANGE: Only target the ImageReader's surface
            captureRequestBuilder.addTarget(imageReaderSurface);

            // CRITICAL CHANGE: Only pass the ImageReader's surface to the session
            cameraDevice.createCaptureSession(Arrays.asList(imageReaderSurface), new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession session) {
                    if (cameraDevice == null) return;
                    captureSession = session;
                    try {
                        captureRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
                        captureRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON); // Use ON instead of ON_AUTO_FLASH
                        captureSession.setRepeatingRequest(captureRequestBuilder.build(), null, backgroundHandler);
                    } catch (CameraAccessException e) {
                        Log.e(TAG, "Failed to start camera preview", e);
                    }
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                    Log.e(TAG, "Failed to configure camera session");
                }
            }, null);
        } catch (CameraAccessException e) {
            Log.e(TAG, "Failed to create camera preview session", e);
        }
    }

    // ... closeCamera() and getBackFacingCameraId() are unchanged ...
    private void closeCamera() {
        if (captureSession != null) {
            captureSession.close();
            captureSession = null;
        }
        if (cameraDevice != null) {
            cameraDevice.close();
            cameraDevice = null;
        }
        if (imageReader != null) {
            imageReader.close();
            imageReader = null;
        }
    }

    private String getBackFacingCameraId(CameraManager manager) throws CameraAccessException {
        for (String cameraId : manager.getCameraIdList()) {
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);
            Integer facing = characteristics.get(CameraCharacteristics.LENS_FACING);
            if (facing != null && facing == CameraCharacteristics.LENS_FACING_BACK) {
                return cameraId;
            }
        }
        return null; // Return null if no back camera is found
    }

    private Size chooseOptimalSize(CameraCharacteristics characteristics) {
        StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
        // Get sizes for ImageFormat.YUV_420_888, which is what our ImageReader uses
        Size[] outputSizes = map.getOutputSizes(ImageFormat.YUV_420_888);

        for (Size size : outputSizes) {
            if (size.getWidth() == 1920 && size.getHeight() == 1080) {
                return size;
            }
        }
        // Fallback to the first available size for YUV format
        return outputSizes[0];
    }

    // ... startBackgroundThread() and stopBackgroundThread() are unchanged ...
    private void startBackgroundThread() {
        backgroundThread = new HandlerThread("CameraBackground");
        backgroundThread.start();
        backgroundHandler = new Handler(backgroundThread.getLooper());
    }

    private void stopBackgroundThread() {
        if (backgroundThread != null) {
            backgroundThread.quitSafely();
            try {
                backgroundThread.join();
                backgroundThread = null;
                backgroundHandler = null;
            } catch (InterruptedException e) {
                Log.e(TAG, "Failed to stop background thread", e);
            }
        }
    }
}
