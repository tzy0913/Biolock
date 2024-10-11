//package com.example.myapplication;
//
//import android.content.Intent;
//import android.content.pm.PackageManager;
//import android.graphics.Bitmap;
//import android.os.Build;
//import android.os.Bundle;
//import android.provider.MediaStore;
//import android.util.Log;
//import android.view.View;
//import android.widget.Button;
//import android.widget.ImageView;
//
//import androidx.annotation.NonNull;
//import androidx.annotation.Nullable;
//import androidx.appcompat.app.AppCompatActivity;
//import android.Manifest;
//
//import org.opencv.android.CameraActivity;
//import org.opencv.android.CameraBridgeViewBase;
//import org.opencv.android.OpenCVLoader;
//import org.opencv.android.Utils;
//import org.opencv.core.Mat;
//import org.opencv.core.MatOfRect;
//import org.opencv.core.Rect;
//import org.opencv.core.Scalar;
//import org.opencv.imgproc.Imgproc;
//import org.opencv.core.Core; // Import Core class
//
//import java.io.File;
//import java.io.FileNotFoundException;
//import java.io.FileOutputStream;
//import java.io.IOException;
//import java.io.InputStream;
//import java.util.ArrayList;
//import java.util.Collection;
//import java.util.Collections;
//import java.util.List;
//import org.opencv.dnn.Net;
//import org.opencv.dnn.Dnn;
//import org.opencv.objdetect.CascadeClassifier;
//
//public class MainActivity extends CameraActivity {
//
//    CameraBridgeViewBase cameraBridgeViewBase;
//    CascadeClassifier cascadeClassifier;
//    Mat gray, rgb;
//    MatOfRect rects;
//    private static final int CAMERA_INDEX = CameraBridgeViewBase.CAMERA_ID_FRONT; // Store the camera index
//
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_camera);
//
//        getPermission();
//
//        cameraBridgeViewBase = findViewById(R.id.cameraView);
//        cameraBridgeViewBase.setCameraIndex(CAMERA_INDEX);
//        cameraBridgeViewBase.setMaxFrameSize(320, 240);// Use front camera
//        cameraBridgeViewBase.setCvCameraViewListener(new CameraBridgeViewBase.CvCameraViewListener2() {
//            @Override
//            public void onCameraViewStarted(int width, int height) {
//                rgb = new Mat();
//                gray = new Mat();
//                rects = new MatOfRect();
//            }
//
//
//            @Override
//            public void onCameraViewStopped() {
//                rgb.release();
//                gray.release();
//                rects.release();
//            }
//
//            private int frameCount = 0;
//            private List<Rect> previousRects = new ArrayList<>();
//
//            @Override
//            public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
//                rgb = inputFrame.rgba();
//                gray = inputFrame.gray();
//
//                // Flip the frame horizontally to correct the mirrored effect
//                if (CAMERA_INDEX == CameraBridgeViewBase.CAMERA_ID_FRONT) {
//                    Core.flip(rgb, rgb, 1);  // Flip around y-axis
//                }
//
//                // Process only every nth frame
//                if (frameCount++ % 3 == 0) {
//                    cascadeClassifier.detectMultiScale(gray, rects, 1.05, 5, 0);
//
//                    // Store detected rectangles for consistency
//                    previousRects.clear();
//                    previousRects.addAll(rects.toList());
//
//                    // Draw rectangles for detected faces
//                    for (Rect rect : previousRects) {
//                        // Draw rectangle around detected face
//                        Imgproc.rectangle(rgb, rect, new Scalar(0, 255, 0), 5);
//                    }
//                } else {
//                    // If not processing, draw previous rectangles to avoid flashing
//                    for (Rect rect : previousRects) {
//                        Imgproc.rectangle(rgb, rect, new Scalar(0, 255, 0), 5);
//                    }
//                }
//
//                return rgb;
//            }
//        });
//
//        if (OpenCVLoader.initDebug()) {
//            cameraBridgeViewBase.enableView();
//            try {
//                InputStream inputStream = getResources().openRawResource(R.raw.haarcascade_frontalface_alt);
//                File file = new File(getDir("cascade", MODE_PRIVATE), "haarcascade_frontalface_alt.xml");
//                FileOutputStream fileOutputStream = new FileOutputStream(file);
//
//                byte[] data = new byte[4096];
//                int read_bytes;
//
//                while ((read_bytes = inputStream.read(data)) != -1) {
//                    fileOutputStream.write(data, 0, read_bytes);
//                }
//
//                cascadeClassifier = new CascadeClassifier(file.getAbsolutePath());
//                if (cascadeClassifier.empty()) cascadeClassifier = null;
//
//                inputStream.close();
//                fileOutputStream.close();
//                file.delete();
//
//            } catch (FileNotFoundException e) {
//                throw new RuntimeException(e);
//            } catch (IOException e) {
//                throw new RuntimeException(e);
//            }
//        }
//    }
//
//    @Override
//    protected void onResume() {
//        super.onResume();
//        cameraBridgeViewBase.enableView();
//    }
//
//    @Override
//    protected void onDestroy() {
//        super.onDestroy();
//        cameraBridgeViewBase.disableView();
//    }
//
//    @Override
//    protected void onPause() {
//        super.onPause();
//        cameraBridgeViewBase.disableView();
//    }
//
//    @Override
//    protected List<? extends CameraBridgeViewBase> getCameraViewList() {
//        return Collections.singletonList(cameraBridgeViewBase);
//    }
//
//    void getPermission() {
//        if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
//            requestPermissions(new String[]{Manifest.permission.CAMERA}, 101);
//        }
//    }
//
//    @Override
//    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
//        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
//        if (grantResults.length > 0 && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
//            getPermission();
//        }
//    }
//}
package com.example.myapplication;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button registerButton = findViewById(R.id.button_register_face_id);
        Button loginButton = findViewById(R.id.button_login_face_id);

        registerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Start CameraActivity for Face ID Registration
                Intent intent = new Intent(MainActivity.this, Camera_Activity.class);
                intent.putExtra("mode", "register"); // Extra to indicate registration
                startActivity(intent);
            }
        });

        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Start CameraActivity for Face ID Login
                Intent intent = new Intent(MainActivity.this, Camera_Activity.class);
                intent.putExtra("mode", "login"); // Extra to indicate login
                startActivity(intent);
            }
        });
    }
}
