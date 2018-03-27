package com.example.administrator.campreview;

import android.app.Activity;
import android.content.Context;

import android.hardware.Camera;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceView;
import android.view.SurfaceHolder;
import android.widget.LinearLayout;

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
    private Context mContext;
    private static final String TAG = "CameraPreview";

    public CameraPreview(Context context, Camera camera) {
        super(context);
        mCamera = camera;
        // Install a Surfaceholder.Callback so we get notified when
        // the underlying surface is created and destroyed.
        mHolder = getHolder();
        mHolder.addCallback(this);
        mContext = context;
        // Deprecated setting, bug required on Android versions prior to 3.0
        mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    }

    /**
     * 适配相机旋转
     *
     * @param activity
     * @param cameraId
     * @param camera
     */
    public void setCameraDisplayOrientation(Activity activity, int cameraId, Camera camera) {
        Camera.CameraInfo info = new Camera.CameraInfo();
        Camera.getCameraInfo(cameraId, info);
        int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0:
                degrees = 0;
                break;
            case Surface.ROTATION_90:
                degrees = 90;
                break;
            case Surface.ROTATION_180:
                degrees = 180;
                break;
            case Surface.ROTATION_270:
                degrees = 270;
                break;
        }
        int result;
        //前置
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation + degrees) % 360;
            result = (360 - result) % 360;
        }
        //后置
        else {
            result = (info.orientation - degrees + 360) % 360;
        }
        //orientationDegree = result;
        camera.setDisplayOrientation(result);
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
            setCameraDisplayOrientation((Activity)mContext, 0, mCamera);
            //mCamera.setDisplayOrientation(0);
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
