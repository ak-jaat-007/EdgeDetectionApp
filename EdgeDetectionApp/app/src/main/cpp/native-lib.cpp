#include <jni.h>
#include <android/log.h>
#include <GLES2/gl2.h>
#include <GLES2/gl2ext.h>
#include <opencv2/core.hpp>
#include <opencv2/imgproc.hpp>
#include <android/media/NdkImage.h> // Required to handle android.media.Image

// Global variables for OpenGL
GLuint g_program;
GLuint g_textureId;
GLint g_positionHandle;
GLint g_texCoordHandle;
GLint g_samplerHandle;

// Shader code
const char* VERTEX_SHADER =
        "attribute vec4 vPosition;\n"
        "attribute vec2 vTexCoord;\n"
        "varying vec2 TexCoord;\n"
        "void main() {\n"
        "  gl_Position = vPosition;\n"
        "  TexCoord = vTexCoord;\n"
        "}\n";

const char* FRAGMENT_SHADER =
        "precision mediump float;\n"
        "varying vec2 TexCoord;\n"
        "uniform sampler2D sTexture;\n"
        "void main() {\n"
        "  gl_FragColor = texture2D(sTexture, TexCoord);\n"
        "}\n";

// Helper functions for OpenGL
GLuint loadShader(GLenum shaderType, const char* pSource) {
    GLuint shader = glCreateShader(shaderType);
    if (shader) {
        glShaderSource(shader, 1, &pSource, NULL);
        glCompileShader(shader);
        GLint compiled = 0;
        glGetShaderiv(shader, GL_COMPILE_STATUS, &compiled);
        if (!compiled) {
            glDeleteShader(shader);
            shader = 0;
        }
    }
    return shader;
}

GLuint createProgram(const char* pVertexSource, const char* pFragmentSource) {
    GLuint vertexShader = loadShader(GL_VERTEX_SHADER, pVertexSource);
    if (!vertexShader) return 0;
    GLuint pixelShader = loadShader(GL_FRAGMENT_SHADER, pFragmentSource);
    if (!pixelShader) return 0;

    GLuint program = glCreateProgram();
    if (program) {
        glAttachShader(program, vertexShader);
        glAttachShader(program, pixelShader);
        glLinkProgram(program);
        GLint linkStatus = GL_FALSE;
        glGetProgramiv(program, GL_LINK_STATUS, &linkStatus);
        if (linkStatus != GL_TRUE) {
            glDeleteProgram(program);
            program = 0;
        }
    }
    return program;
}

// JNI functions for the Renderer
extern "C" {
JNIEXPORT void JNICALL
Java_com_example_edgedetectionapp_MyGLRenderer_onSurfaceCreatedNative(JNIEnv* env, jobject obj) {
    g_program = createProgram(VERTEX_SHADER, FRAGMENT_SHADER);
    g_positionHandle = glGetAttribLocation(g_program, "vPosition");
    g_texCoordHandle = glGetAttribLocation(g_program, "vTexCoord");
    g_samplerHandle = glGetUniformLocation(g_program, "sTexture");

    glGenTextures(1, &g_textureId);
    glBindTexture(GL_TEXTURE_2D, g_textureId);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
    glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
}

JNIEXPORT void JNICALL
Java_com_example_edgedetectionapp_MyGLRenderer_onSurfaceChangedNative(JNIEnv* env, jobject obj, jint width, jint height) {
    glViewport(0, 0, width, height);
}

JNIEXPORT void JNICALL
Java_com_example_edgedetectionapp_MyGLRenderer_onDrawFrameNative(JNIEnv* env, jobject obj) {
    glClear(GL_COLOR_BUFFER_BIT);
    glUseProgram(g_program);

    GLfloat vertices[] = {-1.0f, -1.0f, 1.0f, -1.0f, -1.0f, 1.0f, 1.0f, 1.0f};
    GLfloat texCoords[] = {0.0f, 1.0f, 1.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f};

    glVertexAttribPointer(g_positionHandle, 2, GL_FLOAT, GL_FALSE, 0, vertices);
    glEnableVertexAttribArray(g_positionHandle);
    glVertexAttribPointer(g_texCoordHandle, 2, GL_FLOAT, GL_FALSE, 0, texCoords);
    glEnableVertexAttribArray(g_texCoordHandle);

    glActiveTexture(GL_TEXTURE0);
    glBindTexture(GL_TEXTURE_2D, g_textureId);
    glUniform1i(g_samplerHandle, 0);

    glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);
}

// JNI function for MainActivity
JNIEXPORT void JNICALL
Java_com_example_edgedetectionapp_MainActivity_processFrame(JNIEnv* env, jobject obj, jobject image) {
    if (image == nullptr) {
        return;
    }
    // Convert Java Image to NDK AImage
    AImage* aImage = AImage_fromJava(env, image);
    if (aImage == nullptr) {
        return;
    }

    // Get image properties
    int32_t width, height;
    AImage_getWidth(aImage, &width);
    AImage_getHeight(aImage, &height);

    // Get pointer to the Y (grayscale) plane data
    AImagePlane* planes;
    int32_t numPlanes;
    AImage_getNumberOfPlanes(aImage, &numPlanes);
    if(numPlanes <= 0) {
        AImage_delete(aImage);
        return;
    }
    uint8_t* y_plane_data;
    int dataLength;
    AImage_getPlaneData(aImage, 0, &y_plane_data, &dataLength);
    int32_t rowStride;
    AImage_getPlaneRowStride(aImage, 0, &rowStride);

    // Create an OpenCV Mat from the Y plane
    cv::Mat gray_mat(height, width, CV_8UC1, y_plane_data, rowStride);

    // Since phones are usually portrait, rotate the image
    cv::Mat rotated_mat;
    cv::rotate(gray_mat, rotated_mat, cv::ROTATE_90_CLOCKWISE);

    // Apply Canny edge detection
    cv::Mat edges_mat;
    cv::Canny(rotated_mat, edges_mat, 50, 150);

    // Convert the single-channel edges to a 4-channel RGBA image for OpenGL
    cv::Mat rgba_mat;
    cv::cvtColor(edges_mat, rgba_mat, cv::COLOR_GRAY2RGBA);

    // Upload the processed RGBA image to our OpenGL texture
    glBindTexture(GL_TEXTURE_2D, g_textureId);
    glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, rgba_mat.cols, rgba_mat.rows, 0, GL_RGBA, GL_UNSIGNED_BYTE, rgba_mat.data);

    // Clean up the AImage
    AImage_delete(aImage);
}

} // extern "C"