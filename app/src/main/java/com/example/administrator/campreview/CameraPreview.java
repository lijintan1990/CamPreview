package com.example.administrator.campreview;

import android.content.Context;

import android.hardware.Camera;
import android.util.Log;
import android.view.SurfaceView;
import android.view.SurfaceHolder;

import java.io.IOException;
import java.util.List;

/**
 * Created by ljt on 2018/2/28.
 */

/**
 * This class implements SurfaceHolder.Callback in order to capture the callback
 * events for creating and destroying the view, which are needed for assigning the camera
 */
public class CameraPreview extends SurfaceView implements SurfaceHolder.Callback {
    private SurfaceHolder mHolder;
    private Camera mCamera;
    private static final String TAG = "CameraPreview";

    public CameraPreview(Context context, Camera camera) {
        super(context);
        mCamera = camera;
        // Install a Surfaceholder.Callback so we get notified when
        // the underlying surface is created and destroyed.
        mHolder = getHolder();
        mHolder.addCallback(this);
        // Deprecated setting, bug required on Android versions prior to 3.0
        mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    }

    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
        // The Surface has been created, now tell the camera where to draw the preview
        try {
            mCamera.setPreviewDisplay(surfaceHolder);
        } catch (IOException e) {
            Log.d(TAG, "Error setting camera preview: " + e.getMessage());
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {
        // If your preview can change or rotate, take care of those events here.
        // Make sure to stop the preview befor resizing or reformatting it.
        if (mHolder.getSurface() == null) {
            // preview surface does not exit
            return;
        }

        // Stop preview before making changes
        try {
            mCamera.stopPreview();
        } catch (Exception e) {
            // Ignore: tried to stop a non-existent preview
        }

        // Set preview size and make any resize, rotate or
        // reformatting changes here

        // start preview with new settings
        try {
            mCamera.setPreviewDisplay(mHolder);
            mCamera.startPreview();
            Camera.Parameters params = null;
            List<Camera.Size> preivewSizes = params.getSupportedPreviewSizes();
            params.setPreviewSize(preivewSizes.get(1).width, preivewSizes.get(1).height);
            Log.d(TAG, "preview size width:"+preivewSizes.get(1).width +
                    " height:"+preivewSizes.get(1).height);
        } catch (Exception e) {
            Log.d(TAG, "Error staring camera preview: " + e.getMessage());
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
        // empty. Take care of releasing the Camera preview in your activity.
    }
}
