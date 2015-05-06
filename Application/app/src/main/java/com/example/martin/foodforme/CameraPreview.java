package com.example.martin.foodforme;

import java.util.List;
import android.content.Context;
import android.hardware.Camera;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

/**
 * SOURCE: http://examples.javacodegeeks.com/android/core/hardware/camera-hardware/android-camera-example
 * Modifications based on: http://stackoverflow.com/questions/19577299/android-camera-preview-stretched/22758359#22758359
 */

@SuppressWarnings("deprecation")                // Camera is deprecated, this suppresses the warnings
public class CameraPreview extends SurfaceView implements SurfaceHolder.Callback {

    private static final String TAG = "CameraPreview";

    private SurfaceHolder mHolder;
    private Camera mCamera;
    private final int PORTRAIT_ANGLE = 90;

    private List<Camera.Size> mSupportedPreviewSizes;
    private List<Camera.Size> mSupportedPictureSizes;
    private Camera.Size mPreviewSize;
    private Context mContext;

    public CameraPreview(Context context, Camera camera) {
        super(context);
        mContext = context;

        setCamera(camera);                          // set camera parameters
        setPreviewAndPictureSize();                 // set preview and picture sizes based on display size and camera hardware

        mHolder = getHolder();
        mHolder.addCallback(this);
        // deprecated setting, but required on Android versions prior to 3.0
        mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    }

    public void surfaceCreated(SurfaceHolder holder) {
        // empty. surfaceChanged will take care of stuff
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        // empty. CameraActivity will take care of releasing the camera
    }

    public void setCamera(Camera camera) {
        //method to set a camera instance
        mCamera = camera;
        Camera.Parameters parameters = mCamera.getParameters();                     // Gets the current camera parameters
        parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);   // adds continuous auto focus to parameters
        parameters.setRotation(PORTRAIT_ANGLE);                                     // Sets the camera parameter in portrait
        parameters.set("orientation", "portrait");

        mCamera.setParameters(parameters);                                          // sets the parameters to mCamera

    }

    private void setPreviewAndPictureSize() {
        mSupportedPreviewSizes = mCamera.getParameters().getSupportedPreviewSizes(); // supported camera preview sizes
        mSupportedPictureSizes = mCamera.getParameters().getSupportedPictureSizes(); // supported camera picture sizes

        Camera.Parameters parameters = mCamera.getParameters();
        Camera.Size optimalSize = getOptimalPreviewSize(mSupportedPreviewSizes, getResources().getDisplayMetrics().widthPixels, getResources().getDisplayMetrics().heightPixels);
        if(optimalSize != null) {
            Log.i(TAG, "Setting camera preview size to: " + optimalSize.width + "x" + optimalSize.height);
            parameters.setPreviewSize(optimalSize.width, optimalSize.height);
            if(mSupportedPictureSizes.contains(optimalSize)) {
                Log.i(TAG, "Setting camera picture size to: " + optimalSize.width + "x" + optimalSize.height);
                parameters.setPictureSize(optimalSize.width, optimalSize.height);           // This sets the picture size to the same as the preview size
            } else {
                Log.e(TAG, "setPreviewAndPictureSize. Failed to set picture size to " + optimalSize.width + "x" + optimalSize.height);
            }
        } else {
            Log.e(TAG, "setPreviewAndPictureSize. optimalSize == null");
        }

        mCamera.setParameters(parameters);
    }

    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
        Log.d(TAG, "surfaceChanged => w=" + w + ", h=" + h);
        // If your preview can change or rotate, take care of those events here.
        // Make sure to stop the preview before resizing or reformatting it.
        refreshCamera(mCamera);
    }

    public void refreshCamera(Camera camera) {
        if (mHolder.getSurface() == null) {
            // preview surface does not exist
            return;
        }
        // stop preview before making changes
        try {
            mCamera.stopPreview();
        } catch (Exception e) {
            // ignore: tried to stop a non-existent preview
        }
        // set preview size and make any resize, rotate or reformatting changes here
        // start preview with new settings
        setCamera(camera);
        try {
            mCamera.setDisplayOrientation(PORTRAIT_ANGLE); // Sets camera in portrait mode (90deg)
            mCamera.setPreviewDisplay(mHolder);
            mCamera.startPreview();
        } catch (Exception e) {
            Log.e(VIEW_LOG_TAG, "Error starting camera preview: " + e.getMessage());
        }
    }

    private Camera.Size getOptimalPreviewSize(List<Camera.Size> sizes, int w, int h) {
        final double ASPECT_TOLERANCE = 0.1;
        double targetRatio = (double) h / w;

        if (sizes == null)
            return null;

        Camera.Size optimalSize = null;
        double minDiff = Double.MAX_VALUE;

        int targetHeight = h;

        for (Camera.Size size : sizes) {
            double ratio = (double) size.height / size.width;
            if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE)
                continue;

            if (Math.abs(size.height - targetHeight) < minDiff) {
                optimalSize = size;
                minDiff = Math.abs(size.height - targetHeight);
            }
        }

        if (optimalSize == null) {
            minDiff = Double.MAX_VALUE;
            for (Camera.Size size : sizes) {
                if (Math.abs(size.height - targetHeight) < minDiff) {
                    optimalSize = size;
                    minDiff = Math.abs(size.height - targetHeight);
                }
            }
        }

        return optimalSize;
    }
}
