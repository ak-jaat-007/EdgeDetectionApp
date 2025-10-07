# Real-Time Edge Detection Viewer

This project is a submission for the RnD Intern technical assessment. It features a native Android application that performs real-time edge detection on a live camera feed using OpenCV and OpenGL ES, and a separate TypeScript-based web viewer to display a sample output.

## 📸 App in Action

### Android App
This screenshot shows the core data pipeline running successfully. The Logcat at the bottom confirms that frames from the live camera feed (visible in the emulator) are being passed via JNI and received by the C++ code in real-time.

[<img width="216" height="457" alt="Screenshot 2025-10-07 174044" src="https://github.com/user-attachments/assets/16b8da72-f5d1-4222-9f6b-56dcff76e5fd" />
]

### Web Viewer
The standalone TypeScript-based web viewer displaying sample frame statistics.

[<img width="1886" height="918" alt="Screenshot 2025-10-05 221041" src="https://github.com/user-attachments/assets/40808dda-f178-4b91-b62f-bdf65cdfeb09" />
]

## ✅ Features Implemented

### Android Application
- **Real-time Camera Feed:** Captures a live video stream using the Camera2 API.
- **JNI Bridge:** Successfully sends camera frames from Java to native C++ code for processing.
- **OpenCV Processing:** The C++ code uses the OpenCV library to:
    1.  Convert the camera's YUV image plane to grayscale.
    2.  Rotate the image to the correct orientation.
    3.  Apply a Canny edge detection filter.
    4.  Convert the final black-and-white edge map to RGBA format.
- **OpenGL ES Rendering:** The processed RGBA texture is rendered in real-time to a `GLSurfaceView`, providing a smooth video output.

### Web Viewer
- **TypeScript Project:** A simple but complete TypeScript project, buildable with `tsc`.
- **Static Frame Display:** Renders a sample edge-detected frame and demonstrates DOM manipulation.

## 🧠 Architecture & Frame Flow

The project is structured into two main components: the native Android app and the web viewer.

1.  **Android App Flow:**
    * `MainActivity.java` sets up the `GLSurfaceView` and the `Camera2Controller`. It is responsible for handling permissions and the app's lifecycle.
    * `Camera2Controller.java` directly manages the Camera2 API, setting up an `ImageReader` to capture frames in `YUV_420_888` format on a background thread.
    * For each frame, `MainActivity` calls the native JNI function `processFrame(Image image)`.
    * `native-lib.cpp` receives the Java `Image` object. It uses the NDK's media library to access the raw pixel data (the Y-plane for grayscale).
    * The raw data is wrapped in an OpenCV `cv::Mat`, rotated, processed with `cv::Canny`, and converted to `RGBA`.
    * This final `RGBA` mat is uploaded to an OpenGL texture using `glTexImage2D`.
    * `MainActivity` calls `glSurfaceView.requestRender()`, which in turn calls the native `onDrawFrame` function.
    * The `onDrawFrame` function in C++ draws a simple square textured with the processed frame.

2.  **Web Viewer:**
    * The `web` directory contains a standard TypeScript project.
    * `src/app.ts` contains the logic to load a static image and display it on an HTML canvas.
    * The project is compiled using the `tsc` command defined in `package.json`.

## ⚙️ Setup Instructions

### Android Project (`EdgeDetectionApp`)

1.  Clone the repository.
2.  Open the `EdgeDetectionApp` directory in Android Studio.
3.  **IMPORTANT:** Open the file `app/src/main/cpp/CMakeLists.txt`.
4.  Update the path in the `set(OpenCV_DIR ...)` command to point to the `sdk/native/jni` folder of your local OpenCV Android SDK download.
5.  Let Gradle sync the project.
6.  Build and run on a physical Android device (API 26 or higher).

### Web Project (`web`)

1.  Navigate into the `web` directory in your terminal.
2.  Run `npm install` to install dependencies.
3.  Run `npm run build` to compile the TypeScript code.
4.  Open the `index.html` file in a web browser.
