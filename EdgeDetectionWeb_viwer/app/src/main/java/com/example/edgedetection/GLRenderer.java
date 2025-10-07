package com.example.edgedetection;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.GLUtils;
import android.opengl.Matrix;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class GLRenderer implements GLSurfaceView.Renderer {
    private Context context;
    private int textureId;
    private FloatBuffer vertexBuffer;
    private FloatBuffer textureBuffer;
    
    private int program;
    private int positionHandle;
    private int textureHandle;
    private int textureCoordHandle;
    private int mvpMatrixHandle;
    
    private float[] mvpMatrix = new float[16];
    private float[] projectionMatrix = new float[16];
    private float[] viewMatrix = new float[16];
    
    private ByteBuffer textureBufferDirect;
    private int textureWidth = 1;
    private int textureHeight = 1;
    
    // Vertex coordinates
    private final float[] vertices = {
            -1.0f, -1.0f, 0.0f,
            1.0f, -1.0f, 0.0f,
            -1.0f, 1.0f, 0.0f,
            1.0f, 1.0f, 0.0f
    };
    
    // Texture coordinates
    private final float[] textureCoords = {
            0.0f, 1.0f,
            1.0f, 1.0f,
            0.0f, 0.0f,
            1.0f, 0.0f
    };
    
    // Vertex shader code
    private final String vertexShaderCode =
            "uniform mat4 uMVPMatrix;" +
            "attribute vec4 aPosition;" +
            "attribute vec2 aTexCoord;" +
            "varying vec2 vTexCoord;" +
            "void main() {" +
            "  gl_Position = uMVPMatrix * aPosition;" +
            "  vTexCoord = aTexCoord;" +
            "}";
    
    // Fragment shader code
    private final String fragmentShaderCode =
            "precision mediump float;" +
            "varying vec2 vTexCoord;" +
            "uniform sampler2D uTexture;" +
            "void main() {" +
            "  gl_FragColor = texture2D(uTexture, vTexCoord);" +
            "}";
    
    public GLRenderer(Context context) {
        this.context = context;
        
        // Initialize vertex buffer
        ByteBuffer bb = ByteBuffer.allocateDirect(vertices.length * 4);
        bb.order(ByteOrder.nativeOrder());
        vertexBuffer = bb.asFloatBuffer();
        vertexBuffer.put(vertices);
        vertexBuffer.position(0);
        
        // Initialize texture coordinate buffer
        ByteBuffer tb = ByteBuffer.allocateDirect(textureCoords.length * 4);
        tb.order(ByteOrder.nativeOrder());
        textureBuffer = tb.asFloatBuffer();
        textureBuffer.put(textureCoords);
        textureBuffer.position(0);
    }
    
    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
        
        // Create texture
        int[] textures = new int[1];
        GLES20.glGenTextures(1, textures, 0);
        textureId = textures[0];
        
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
        
        // Initialize with empty texture
        textureBufferDirect = ByteBuffer.allocateDirect(textureWidth * textureHeight * 4);
        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, 
                textureWidth, textureHeight, 0, GLES20.GL_RGBA, 
                GLES20.GL_UNSIGNED_BYTE, textureBufferDirect);
        
        // Load shaders
        int vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode);
        int fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode);
        
        // Create shader program
        program = GLES20.glCreateProgram();
        GLES20.glAttachShader(program, vertexShader);
        GLES20.glAttachShader(program, fragmentShader);
        GLES20.glLinkProgram(program);
        
        // Get attribute locations
        positionHandle = GLES20.glGetAttribLocation(program, "aPosition");
        textureCoordHandle = GLES20.glGetAttribLocation(program, "aTexCoord");
        textureHandle = GLES20.glGetUniformLocation(program, "uTexture");
        mvpMatrixHandle = GLES20.glGetUniformLocation(program, "uMVPMatrix");
        
        // Set up matrices
        Matrix.setIdentityM(mvpMatrix, 0);
    }
    
    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        GLES20.glViewport(0, 0, width, height);
        
        float ratio = (float) width / height;
        Matrix.orthoM(projectionMatrix, 0, -ratio, ratio, -1, 1, 3, 7);
        
        Matrix.setLookAtM(viewMatrix, 0, 0, 0, 5, 0f, 0f, 0f, 0f, 1.0f, 0.0f);
        Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, viewMatrix, 0);
    }
    
    @Override
    public void onDrawFrame(GL10 gl) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
        
        GLES20.glUseProgram(program);
        
        // Set vertices
        GLES20.glVertexAttribPointer(positionHandle, 3, GLES20.GL_FLOAT, false, 12, vertexBuffer);
        GLES20.glEnableVertexAttribArray(positionHandle);
        
        // Set texture coordinates
        GLES20.glVertexAttribPointer(textureCoordHandle, 2, GLES20.GL_FLOAT, false, 8, textureBuffer);
        GLES20.glEnableVertexAttribArray(textureCoordHandle);
        
        // Set matrix
        GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, mvpMatrix, 0);
        
        // Bind texture
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId);
        GLES20.glUniform1i(textureHandle, 0);
        
        // Draw
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
        
        GLES20.glDisableVertexAttribArray(positionHandle);
        GLES20.glDisableVertexAttribArray(textureCoordHandle);
    }
    
    public void updateTexture(byte[] data, int width, int height) {
        if (data == null) return;
        
        // Convert YUV to RGB (simplified - in real implementation use proper conversion)
        byte[] rgbData = convertYUVtoRGB(data, width, height);
        
        if (textureWidth != width || textureHeight != height) {
            textureWidth = width;
            textureHeight = height;
            textureBufferDirect = ByteBuffer.allocateDirect(width * height * 4);
        }
        
        textureBufferDirect.put(rgbData);
        textureBufferDirect.position(0);
        
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId);
        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, 
                width, height, 0, GLES20.GL_RGBA, 
                GLES20.GL_UNSIGNED_BYTE, textureBufferDirect);
    }
    
    private byte[] convertYUVtoRGB(byte[] yuv, int width, int height) {
        // Simplified conversion - in production use proper YUV to RGB conversion
        byte[] rgb = new byte[width * height * 4];
        for (int i = 0; i < width * height; i++) {
            int y = yuv[i] & 0xFF;
            rgb[i * 4] = (byte) y;     // R
            rgb[i * 4 + 1] = (byte) y; // G  
            rgb[i * 4 + 2] = (byte) y; // B
            rgb[i * 4 + 3] = (byte) 255; // A
        }
        return rgb;
    }
    
    private int loadShader(int type, String shaderCode) {
        int shader = GLES20.glCreateShader(type);
        GLES20.glShaderSource(shader, shaderCode);
        GLES20.glCompileShader(shader);
        return shader;
    }
}