package com.example.edgedetectionapp;

import android.opengl.GLSurfaceView;
import javax.microedition.khronos.opengles.GL10;
import javax.microedition.khronos.egl.EGLConfig;

public class MyGLRenderer implements GLSurfaceView.Renderer {
    @Override public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        onSurfaceCreatedNative();
    }
    @Override public void onSurfaceChanged(GL10 gl, int w, int h) {
        onSurfaceChangedNative(w, h);
    }
    @Override public void onDrawFrame(GL10 gl) {
        onDrawFrameNative();
    }
    public native void onSurfaceCreatedNative();
    public native void onSurfaceChangedNative(int w, int h);
    public native void onDrawFrameNative();
}
