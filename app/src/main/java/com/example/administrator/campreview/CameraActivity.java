package com.example.administrator.campreview;

import android.app.Activity;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.hardware.Camera;
//import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.FrameLayout;

public class CameraActivity extends Activity {
    private static final String TAG = "easyCamera";
    private Camera myCam = null;

    private FrameLayout frameLayout;
    private CameraPreview cameraPreview;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera_easy);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        myCam = getCameraInstance();
        cameraPreview = new CameraPreview(this, myCam);
        frameLayout = findViewById(R.id.frame_view);
        frameLayout.addView(cameraPreview);
    }

    /** Check if this device has a camera */
    private boolean checkCameraHardware(Context context) {
        if (context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)) {
            return true;
        } else {
            return false;
        }
    }

    /** A safe way to get an instance of the Camera object. */
    public static Camera getCameraInstance() {
        Camera c = null;
        try {
            c = Camera.open();
        }
        catch (Exception e) {
            // Camera is in use or does not exit
            Log.d(TAG, "Camera is in use or does not exit");
        }
        return c;
    }


}
