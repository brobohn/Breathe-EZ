package com.bactrack.backtrack_mobile.breathezpackage;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Camera;
import android.media.FaceDetector;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import BACtrackAPI.API.BACtrackAPI;
import BACtrackAPI.API.BACtrackAPICallbacks;
import BACtrackAPI.DATech.Constants.Errors;
import BACtrackAPI.Exceptions.BluetoothLENotSupportedException;
import BACtrackAPI.Exceptions.BluetoothNotEnabledException;


public class MainActivity extends Activity implements SurfaceHolder.Callback {

    private static String TAG = "MainActivity";
    private TextView statusMessageTextView;
    private BACtrackAPI mAPI;
    Camera camera;
    SurfaceView surfaceView;
    SurfaceHolder surfaceHolder;
    Camera.PictureCallback rawCallback;
    Camera.ShutterCallback shutterCallback;
    Camera.PictureCallback jpegCallback;
    private final String tag = "Breathe-EZ";
    final int NUM_PICS = 3;
    int pics_taken = 0;

    byte[] pics[];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        pics = new byte[NUM_PICS][];

        this.statusMessageTextView = (TextView)this.findViewById(R.id.status_message_text_view_id);

        this.setStatus(R.string.TEXT_DISCONNECTED);

        try {
            mAPI = new BACtrackAPI(this, mCallbacks);
        } catch (BluetoothLENotSupportedException e) {
            e.printStackTrace();
            this.setStatus(R.string.TEXT_ERR_BLE_NOT_SUPPORTED);
        } catch (BluetoothNotEnabledException e) {
            e.printStackTrace();
            this.setStatus(R.string.TEXT_ERR_BT_NOT_ENABLED);
        }

        surfaceView = (SurfaceView)findViewById(R.id.cameraView);
        surfaceHolder = surfaceView.getHolder();
        surfaceHolder.addCallback(this);
        surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

        rawCallback = new Camera.PictureCallback() {
            public void onPictureTaken(byte[] data, Camera camera) {
                Log.d("Log", "onPictureTaken - raw");
            }
        };
        /** Handles data for jpeg picture */
        shutterCallback = new Camera.ShutterCallback() {
            public void onShutter() {
                Log.i("Log", "onShutter'd");
            }
        };
        jpegCallback = new Camera.PictureCallback() {
            public void onPictureTaken(byte[] data, Camera camera) {
                Toast toast;
                pics[pics_taken++] = data;

                Log.d("Log", "onPictureTaken - jpeg");
                camera.startPreview();
            }
        };

        pics = new byte[NUM_PICS][];
    }

    public void connectNearestClicked(View v) {
        if (mAPI != null) {
            setStatus(R.string.TEXT_CONNECTING);
            mAPI.connectToNearestBreathalyzer();
        }
    }

    public void disconnectClicked(View v) {
        if (mAPI != null) {
            mAPI.disconnect();
        }
    }

    public void getFirmwareVersionClicked(View v) {
        boolean result = false;
        if (mAPI != null) {
            result = mAPI.getFirmwareVersion();
        }
        if (!result)
            Log.e(TAG, "mAPI.getFirmwareVersion() failed");
        else
            Log.d(TAG, "Firmware version requested");
    }

    public void startBlowProcessClicked(View v) {
        boolean result = false;

        start_camera();

        if (mAPI != null) {
            result = mAPI.startCountdown();
        }
        if (!result)
            Log.e(TAG, "mAPI.startCountdown() failed");
        else
            Log.d(TAG, "Blow process start requested");
    }

    private void setStatus(int resourceId) {
        this.setStatus(this.getResources().getString(resourceId));
    }

    private void setStatus(final String message) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                statusMessageTextView.setText(String.format("Status:\n%s", message));
            }
        });
    }



    private void captureImage() {
        camera.takePicture(shutterCallback, rawCallback, jpegCallback);

        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        camera.takePicture(shutterCallback, rawCallback, jpegCallback);

        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        camera.takePicture(shutterCallback, rawCallback, jpegCallback);
    }

    private void findBestFace() {
        int bestDataIndex = 0;
        float bestConfidence = 0;

        for (int i = 0; i < pics.length; i++) {
            BitmapFactory.Options bitmapFactoryOptions = new BitmapFactory.Options();
            bitmapFactoryOptions.inPreferredConfig = Bitmap.Config.RGB_565;
            Bitmap bitmap;
            bitmap = BitmapFactory.decodeByteArray(pics[i], 0, pics[i].length, bitmapFactoryOptions);

            FaceDetector.Face[] faces = new FaceDetector.Face[1];

            FaceDetector faceDetector = new FaceDetector(bitmap.getWidth(), bitmap.getHeight(), faces.length);
            int faces_found = faceDetector.findFaces(bitmap, faces);

            if (faces[0] == null) {
                //EXIT MEASUREMENT AND NOTIFY NO FACES WERE FOUND
            }

            float confidence = 0;
            if (faces[0] != null) {
                confidence = faces[0].confidence();
            }

            if (confidence > bestConfidence) {
                bestDataIndex = i;
                bestConfidence = confidence;
            }
        }

        FileOutputStream outStream = null;
        String pic_dst = String.format("%s/%d.jpg",
                Environment.getExternalStorageDirectory() + "/DCIM/Camera",
                System.currentTimeMillis());

        try {
            outStream = new FileOutputStream(pic_dst);
            outStream.write(pics[bestDataIndex]);
            outStream.close();
            Log.d("Log", String.format("onPictureTaken - wrote bytes: %d", pics[bestDataIndex].length));
            Log.d("Log", String.format("onPictureTaken - %s", pic_dst));

            MediaScannerConnection.scanFile(getApplicationContext(), new String[]{pic_dst}, null,
                    new MediaScannerConnection.OnScanCompletedListener() {
                        public void onScanCompleted(String path, Uri uri) {
                            Log.i("ExternalStorage", "Scanned " + path + ":");
                            Log.i("ExternalStorage", "-> uri=" + uri);
                        }
                    });

            final float finalBestConfidence = bestConfidence;
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast toast = Toast.makeText(getApplicationContext(),
                            String.format("Best picture saved. Confidence level: %.2f", finalBestConfidence), Toast.LENGTH_LONG);
                    toast.show();
                }
            });

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {

        }
    }

    private void start_camera()
    {
        try{
            Log.i("start_camera", "Starting Camera...");
            camera = Camera.open(Camera.CameraInfo.CAMERA_FACING_FRONT);
        }catch(RuntimeException e){
            Log.e(tag, "init_camera: " + e);
            return;
        }

        android.hardware.Camera.CameraInfo info =
                new android.hardware.Camera.CameraInfo();
        Camera.getCameraInfo(Camera.CameraInfo.CAMERA_FACING_BACK, info);

        int rotation = this.getWindowManager().getDefaultDisplay().getRotation();
        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0: degrees = 0; break;
            case Surface.ROTATION_90: degrees = 90; break;
            case Surface.ROTATION_180: degrees = 180; break;
            case Surface.ROTATION_270: degrees = 270; break;
        }
        int rotate_camera = (info.orientation - degrees + 360) % 360;
        int rotate_display = (rotate_camera + 180) % 360;
        String manu = Build.MANUFACTURER;

        if (manu.equals("samsung")) {
            /*
             * Values are reversed on samsung devices. Add manufacturers as neccessary.
             */
            rotate_display = (rotate_display + 180) % 360;
            rotate_camera = (rotate_camera + 180) % 360;
        }

        Camera.Parameters param;
        param = camera.getParameters();
        //modify parameter
        param.setPreviewFrameRate(20);
        param.setPreviewSize(176, 144);
        //param.set("orientation", "portrait");
        param.setRotation(rotate_camera);
        camera.setDisplayOrientation(rotate_display);
        camera.setParameters(param);
        try {
            camera.setPreviewDisplay(surfaceHolder);
            camera.startPreview();
            //camera.takePicture(shutter, raw, jpeg);
        } catch (Exception e) {
            Log.e(tag, "init_camera: " + e);
            return;
        }
    }

    private void stop_camera()
    {
        camera.stopPreview();
        camera.release();
    }

    public void surfaceChanged(SurfaceHolder arg0, int arg1, int arg2, int arg3) {
        // TODO Auto-generated method stub
    }

    public void surfaceCreated(SurfaceHolder holder) {
        // TODO Auto-generated method stub
    }

    public void surfaceDestroyed(SurfaceHolder holder) {
        // TODO Auto-generated method stub
    }

    private final BACtrackAPICallbacks mCallbacks = new BACtrackAPICallbacks() {
        @Override
        public void BACtrackConnected() {
            setStatus(R.string.TEXT_CONNECTED);
        }

        @Override
        public void BACtrackDidConnect(String s) {
            setStatus(R.string.TEXT_DISCOVERING_SERVICES);
        }

        @Override
        public void BACtrackDisconnected() {
            setStatus(R.string.TEXT_DISCONNECTED);
        }

        @Override
        public void a() {

        }

        @Override
        public void BACtrackConnectionTimeout() {

        }

        @Override
        public void b() {

        }

        @Override
        public void BACtrackCountdown(int currentCountdownCount) {
            setStatus(getString(R.string.TEXT_COUNTDOWN) + " " + currentCountdownCount);
        }

        @Override
        public void BACtrackStart() {
            setStatus(R.string.TEXT_BLOW_NOW);

        }

        @Override
        public void BACtrackBlow() {
            setStatus(R.string.TEXT_KEEP_BLOWING);
            pics_taken = 0;
            captureImage();

        }

        @Override
        public void BACtrackAnalyzing() {
            setStatus(R.string.TEXT_ANALYZING);
            findBestFace();
        }

        @Override
        public void BACtrackResults(float measuredBac) {
            setStatus(getString(R.string.TEXT_FINISHED) + " " + String.format("%.02f", measuredBac));
        }

        @Override
        public void BACtrackFirmwareVersion(String version) {
            setStatus(getString(R.string.TEXT_FIRMWARE_VERSION) + " " + version);
        }

        @Override
        public void a(int i) {
        }

        @Override
        public void a(byte b) {
        }

        @Override
        public void b(byte b) {
        }

        @Override
        public void a(byte[] bytes) {
        }

        @Override
        public void b(byte[] bytes) {

        }

        @Override
        public void BACtrackError(int errorCode) {
            if (errorCode == Errors.ERROR_BLOW_ERROR)
                setStatus(R.string.TEXT_ERR_BLOW_ERROR);
        }
    };

    @Override
    protected void onDestroy() {
        stop_camera();
        super.onDestroy();
    }

    @Override
    protected void onPause() {
        stop_camera();
        super.onPause();
    }
}
