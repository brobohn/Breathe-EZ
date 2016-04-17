package com.bactrack.backtrack_mobile.breathezpackage;

import android.hardware.Camera;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Created by Ben on 4/1/2016.
 */
public class CameraManager {

    //TextView testView;
    MainActivity ma;

    Camera camera;
    SurfaceView surfaceView;
    SurfaceHolder surfaceHolder;
    Camera.PictureCallback rawCallback;
    Camera.ShutterCallback shutterCallback;
    Camera.PictureCallback jpegCallback;
    private final String tag = "VideoServer";

    Button start, stop, capture;

    public CameraManager(MainActivity ma) {

        //setContentView(R.layout.activity_main);

        
    }


}
