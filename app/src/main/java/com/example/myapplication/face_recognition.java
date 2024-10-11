package com.example.myapplication;

import android.content.Context;
import android.content.res.AssetManager;

import org.opencv.objdetect.CascadeClassifier;
import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.gpu.GpuDelegate;

import java.io.IOException;

public class face_recognition {

    private Interpreter interpreter;
    private int INPUT_SIZE;
    private int height;
    private int width;
    private GpuDelegate gpuDelegate = null;
    private CascadeClassifier cascadeClassifier;

    face_recognition(AssetManager assetManager, Context context, String modelPath, int input_size) throws IOException{

    }
}
