package com.example.martin.foodforme;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.hardware.Camera.PictureCallback;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

/**
 * SOURCE: http://examples.javacodegeeks.com/android/core/hardware/camera-hardware/android-camera-example
 */

@SuppressWarnings("deprecation")                // Camera is deprecated, this suppresses the warnings
public class CameraActivity extends Activity {
    private Camera mCamera;
    private CameraPreview mPreview;
    private PictureCallback mPicture;
    private Button capture, switchCamera;
    private Context myContext;
    private LinearLayout cameraPreview;
    private boolean cameraFront = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera_preview);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        myContext = this;
        initialize();
    }

    private int findFrontFacingCamera() {
        int cameraId = -1;
        // Search for the front facing camera
        int numberOfCameras = Camera.getNumberOfCameras();
        for (int i = 0; i < numberOfCameras; i++) {
            CameraInfo info = new CameraInfo();
            Camera.getCameraInfo(i, info);
            if (info.facing == CameraInfo.CAMERA_FACING_FRONT) {
                cameraId = i;
                cameraFront = true;
                break;
            }
        }
        return cameraId;
    }

    private int findBackFacingCamera() {
        int cameraId = -1;
        //Search for the back facing camera
        //get the number of cameras
        int numberOfCameras = Camera.getNumberOfCameras();
        //for every camera check
        for (int i = 0; i < numberOfCameras; i++) {
            CameraInfo info = new CameraInfo();
            Camera.getCameraInfo(i, info);
            if (info.facing == CameraInfo.CAMERA_FACING_BACK) {
                cameraId = i;
                cameraFront = false;
                break;
            }
        }
        return cameraId;
    }

    public void onResume() {
        super.onResume();
        if (!hasCamera(myContext)) {
            Toast toast = Toast.makeText(myContext, "Sorry, your phone does not have a camera!", Toast.LENGTH_LONG);
            toast.show();
            finish();
        }
        if (mCamera == null) {
            //if the front facing camera does not exist
            if (findFrontFacingCamera() < 0) {
                Toast.makeText(this, "No front facing camera found.", Toast.LENGTH_LONG).show();
                switchCamera.setVisibility(View.GONE);
            }
            mCamera = Camera.open(findBackFacingCamera());
            mPicture = getPictureCallback();
            mPreview.refreshCamera(mCamera);
        }
    }

    public void initialize() {
        cameraPreview = (LinearLayout) findViewById(R.id.camera_preview);
        mPreview = new CameraPreview(myContext, mCamera);
        cameraPreview.addView(mPreview);

        capture = (Button) findViewById(R.id.button_capture);
        capture.setOnClickListener(captureListener);
    }

    OnClickListener switchCameraListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            //get the number of cameras
            int camerasNumber = Camera.getNumberOfCameras();
            if (camerasNumber > 1) {
                //release the old camera instance
                //switch camera, from the front and the back and vice versa

                releaseCamera();
                chooseCamera();
            } else {
                Toast toast = Toast.makeText(myContext, "Sorry, your phone has only one camera!", Toast.LENGTH_LONG);
                toast.show();
            }
        }
    };

    public void chooseCamera() {
        //if the camera preview is the front
        if (cameraFront) {
            int cameraId = findBackFacingCamera();
            if (cameraId >= 0) {
                //open the backFacingCamera
                //set a picture callback
                //refresh the preview

                mCamera = Camera.open(cameraId);
                mPicture = getPictureCallback();
                mPreview.refreshCamera(mCamera);
            }
        } else {
            int cameraId = findFrontFacingCamera();
            if (cameraId >= 0) {
                //open the backFacingCamera
                //set a picture callback
                //refresh the preview

                mCamera = Camera.open(cameraId);
                mPicture = getPictureCallback();
                mPreview.refreshCamera(mCamera);
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        //when on Pause, release camera in order to be used from other applications
        releaseCamera();
    }

    private boolean hasCamera(Context context) {
        //check if the device has camera
        if (context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)) {
            return true;
        } else {
            return false;
        }
    }

    private PictureCallback getPictureCallback() {
        final PictureCallback picture = new PictureCallback() {

            @Override
            public void onPictureTaken(byte[] data, Camera camera) {
                //make a new picture file
                File pictureFile = getOutputMediaFile();

                if (pictureFile == null) {
                    return;
                }

                saveImageByteArray(data, pictureFile);

                /**
                 * Rotate and crop the image
                 */
                try {
                    FileInputStream inputStream = new FileInputStream(pictureFile);
                    Bitmap realImage = BitmapFactory.decodeStream(inputStream);
                    ExifInterface exif = new ExifInterface(AddProductActivity.DATA_PATH + "/ocr.jpg");
                    Log.d("EXIF value", exif.getAttribute(ExifInterface.TAG_ORIENTATION));
                    if(exif.getAttribute(ExifInterface.TAG_ORIENTATION).equalsIgnoreCase("6")){

                        realImage = rotateBitmap(realImage, 90);
                    }else if(exif.getAttribute(ExifInterface.TAG_ORIENTATION).equalsIgnoreCase("8")){
                        realImage = rotateBitmap(realImage, 270);
                    }else if(exif.getAttribute(ExifInterface.TAG_ORIENTATION).equalsIgnoreCase("3")){
                        realImage = rotateBitmap(realImage, 180);
                    }

                    data = cropImageByteArray(bitmapToByteArray(realImage));         // uses a custom method to crop the image

                } catch (Exception e) {
                    Log.e("Camera Activity: ", "Failed to rotate or crop the saved image...");
                }

                saveImageByteArray(data, pictureFile);

                Intent intent = new Intent();
                setResult(RESULT_OK, intent);
                finish();
            }
        };
        return picture;
    }

    private void saveImageByteArray(byte[] data, File pictureFile) {
        try {
            //write the file
            FileOutputStream fos = new FileOutputStream(pictureFile);
            fos.write(data);
            fos.close();
            //Toast toast = Toast.makeText(myContext, "Picture saved: " + pictureFile.getName(), Toast.LENGTH_LONG);
            //toast.show();


        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    OnClickListener captureListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            mCamera.takePicture(null, null, mPicture);
        }
    };

    private byte[] cropImageByteArray(byte[] bytesImage) {
        Bitmap fullSizeBmp = BitmapFactory.decodeByteArray(bytesImage, 0, bytesImage.length);

        // Image calculations
        int height = fullSizeBmp.getHeight();
        int width = fullSizeBmp.getWidth();
        double imageRatio = ((double)height)/width;                                       // imageRatio: the actual ratio of the captured image
        int centerHeight = height / 2;
        int centerWidth = width / 2;
        // ------------------------------------------------------------------

        // Preview calculations
        int previewHeight = findViewById(R.id.camera_preview).getHeight();
        int previewWidth = findViewById(R.id.camera_preview).getWidth();
        double previewRatio = ((double)previewHeight)/previewWidth;                       // previewRatio: the ratio of the preview
        // ------------------------------------------------------------------

        // ImageView calculations (the rectangle). The image placed in the ImageView is 226x141 px (height = 141, width = 226)
        double removeTopRatio = 0.6;
        double removeSideRatio = 0.7;
        int imageViewHeight = findViewById(R.id.imageViewDraw).getHeight();
        int imageViewWidth = findViewById(R.id.imageViewDraw).getWidth();

        // Use the preview window width and height to calculate how much of the area is covered by the rectangle
        double comparedHeight = ((double)imageViewHeight)/previewHeight;
        double comparedWidth = ((double)imageViewWidth)/previewWidth;
        int calculatedHeight = (int) (removeTopRatio*comparedHeight*height);
        int calculatedWidth = (int) (removeSideRatio*comparedWidth*width);
        // ------------------------------------------------------------------

        //int inRectHeight = (int) (imageViewHeight - (removeTopRatio*imageViewHeight));  // height of the rectangle displayed in the camera preview. 96 px on the inside.
        //int inRectWidth = imageViewWidth; // (int) (((double)199/226)*imageViewWidth);  // width of the rectangle displayed in the camera preview. 199 px on the inside.

        int halfRectHeight = calculatedHeight / 2;
        int halfRectWidth = calculatedWidth / 2;

        int startingHeightFix = (int) (imageViewHeight*0.2);

        int startingHeight = centerHeight - halfRectHeight + startingHeightFix; // used as offset
        int startingWidth = centerWidth - halfRectWidth;    // used as offset

        Bitmap croppedBmp = Bitmap.createBitmap(fullSizeBmp, startingWidth, startingHeight, calculatedWidth, calculatedHeight);

        return bitmapToByteArray(croppedBmp);
    }

    private byte[] bitmapToByteArray(Bitmap bmp) {
        int fullQuality = 100;
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        bmp.compress(Bitmap.CompressFormat.PNG, fullQuality, outputStream);
        byte[] byteArray = outputStream.toByteArray();
        return byteArray;
    }

    /**
     * SOURCE: http://stackoverflow.com/a/11024837
     */
    private Bitmap rotateBitmap(Bitmap bitmap, int degree) {
        int w = bitmap.getWidth();
        int h = bitmap.getHeight();

        Matrix mtx = new Matrix();
        mtx.postRotate(degree);

        return Bitmap.createBitmap(bitmap, 0, 0, w, h, mtx, true);
    }

    //make picture and save to a folder
    private static File getOutputMediaFile() {
        File fileToOCR = new File(AddProductActivity.DATA_PATH + "/ocr.jpg"); // The image should be saved in this directory for Tesseract
        Log.d("CameraActivity: ", "The image will be saved to " + Uri.fromFile(fileToOCR));
        return fileToOCR;
    }

    private void releaseCamera() {
        // stop and release camera
        if (mCamera != null) {
            mCamera.release();
            mCamera = null;
        }
    }
}