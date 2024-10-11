package com.example.myapplication;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;

import org.opencv.android.CameraActivity;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;

public class Camera_Activity extends CameraActivity {

    private static final String TAG = "OpenCV";
    private static final int CAMERA_INDEX = CameraBridgeViewBase.CAMERA_ID_FRONT; // Use front camera
    private static final String CASCADE_NAME = "haarcascade_frontalface_alt.xml";
    private static final String CASCADE_DIR = "cascade";

    private CameraBridgeViewBase cameraBridgeViewBase;
    private CascadeClassifier cascadeClassifier;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        initializeOpenCV();
        initializeCameraView();
        loadCascade();
    }

    private void initializeOpenCV() {
        if (!OpenCVLoader.initDebug()) {
            Log.e(TAG, "OpenCV initialization failed.");
        }
    }

    private void initializeCameraView() {
        cameraBridgeViewBase = findViewById(R.id.cameraView);
        progressBar = findViewById(R.id.progress_bar);
        cameraBridgeViewBase.setCameraIndex(CAMERA_INDEX);
        cameraBridgeViewBase.setCvCameraViewListener(new CameraBridgeViewBase.CvCameraViewListener2() {
            @Override
            public void onCameraViewStarted(int width, int height) {
//                cameraBridgeViewBase.setMaxFrameSize(480, 360);
            }

            @Override
            public void onCameraViewStopped() {
                // Clean up code
            }

            @Override
            public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
                return processFrame(inputFrame);
            }
        });
        cameraBridgeViewBase.enableView();
    }

    private Mat processFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        Mat rgb = inputFrame.rgba();
        Mat gray = inputFrame.gray();
        MatOfRect rects = new MatOfRect();

        Core.flip(rgb, rgb, 1); // Flip the RGB frame horizontally
        detectFaces(gray, rects);
        drawFaceRectangles(rgb, rects);

        return rgb;
    }

    private void detectFaces(Mat gray, MatOfRect rects) {
        if (cascadeClassifier != null) {
            cascadeClassifier.detectMultiScale(gray, rects, 1.15, 5); // Adjust parameters as needed
        }
    }

    private void drawFaceRectangles(Mat rgb, MatOfRect rects) {
        for (Rect rect : rects.toArray()) {
            Imgproc.rectangle(rgb, rect.tl(), rect.br(), new Scalar(0, 255, 0), 3); // Draw rectangles around detected faces
        }
    }

    private void loadCascade() {
        try (InputStream inputStream = getResources().openRawResource(R.raw.lbpcascade_frontalface);
             FileOutputStream fileOutputStream = new FileOutputStream(getDir(CASCADE_DIR, MODE_PRIVATE) + "/" + CASCADE_NAME)) {

            byte[] data = new byte[4096];
            int bytesRead;

            while ((bytesRead = inputStream.read(data)) != -1) {
                fileOutputStream.write(data, 0, bytesRead);
            }

            cascadeClassifier = new CascadeClassifier(getDir(CASCADE_DIR, MODE_PRIVATE) + "/" + CASCADE_NAME);
            if (cascadeClassifier.empty()) {
                cascadeClassifier = null;
                Log.e(TAG, "Failed to load cascade classifier.");
            }

        } catch (IOException e) {
            Log.e(TAG, "Error loading cascade file: " + e.getMessage());
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        cameraBridgeViewBase.enableView();
    }

    @Override
    protected void onPause() {
        super.onPause();
        cameraBridgeViewBase.disableView();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cameraBridgeViewBase.disableView();
    }

    @Override
    protected List<? extends CameraBridgeViewBase> getCameraViewList() {
        return Collections.singletonList(cameraBridgeViewBase);
    }
}
