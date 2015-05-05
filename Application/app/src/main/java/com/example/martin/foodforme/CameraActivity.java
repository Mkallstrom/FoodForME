package com.example.martin.foodforme;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
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
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

/**
 * SOURCE: http://examples.javacodegeeks.com/android/core/hardware/camera-hardware/android-camera-example
 */

@SuppressWarnings("deprecation")                // Camera is deprecated, this suppresses the warnings
public class CameraActivity extends Activity {
    private static final String TAG = "CameraActivity";

    private Camera mCamera;
    private CameraPreview mPreview;
    private PictureCallback mPicture;
    private Button capture, switchCamera;
    private Context myContext;
    private LinearLayout cameraPreview;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera_preview);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        myContext = this;
        initialize();
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
            mCamera = Camera.open(findBackFacingCamera());
            mPicture = getPictureCallback();
            mPreview.refreshCamera(mCamera);
        }
    }

    public void initialize() {
        mCamera = Camera.open(findBackFacingCamera());

        mPicture = getPictureCallback();

        cameraPreview = (LinearLayout) findViewById(R.id.camera_preview);
        mPreview = new CameraPreview(myContext, mCamera);
        cameraPreview.addView(mPreview);

        capture = (Button) findViewById(R.id.button_capture);
        capture.setOnClickListener(captureListener);
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

                saveImageByteArray(data, pictureFile);          // saves the image taken
                data = rotateAndCrop(data, pictureFile);        // rotates and crops the image
                saveImageByteArray(data, pictureFile);          // saves the rotated and cropped image

                Intent intent = new Intent();
                setResult(RESULT_OK, intent);
                finish();
            }
        };
        return picture;
    }

    /**
     * Rotates (if needed) and crops the image
     */
    private byte[] rotateAndCrop(byte[] data, File pictureFile) {
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
        return data;
    }

    private void saveImageByteArray(byte[] data, File pictureFile) {
        try {
            FileOutputStream fos = new FileOutputStream(pictureFile);
            fos.write(data);
            fos.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    OnClickListener captureListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            mCamera.takePicture(null, null, mPicture);                                              // take the picture
            ImageView rectangle = (ImageView) findViewById(R.id.imageViewDraw);
            rectangle.setImageResource(R.drawable.expiration_date_rectangle_separate_tiny_white);   // change the imageView to white stroked
            Toast.makeText(myContext, "Cropping image and extracting the date...", Toast.LENGTH_LONG).show();
        }
    };

    private byte[] cropImageByteArray(byte[] bytesImage) {
        Bitmap fullSizeBmp = BitmapFactory.decodeByteArray(bytesImage, 0, bytesImage.length); // This requires a lot of memory (apparently)

        // Image calculations
        int height = fullSizeBmp.getHeight();
        int width = fullSizeBmp.getWidth();
        int centerHeight = height / 2;
        int centerWidth = width / 2;
        // ------------------------------------------------------------------

        // Preview calculations
        int previewHeight = findViewById(R.id.camera_preview).getHeight();
        int previewWidth = findViewById(R.id.camera_preview).getWidth();
        // ------------------------------------------------------------------

        // TODO: Test on other devices.
        // TODO: Make sure to crop only inside the rectangle (currently using the full imageView size)

        double heightRatio = ((double) height) / previewHeight;                         // finds the height ratio between the full size image and the preview window
        double widthRatio = ((double) width) / previewWidth;                            // finds the width ratio between the full size image and the preview window

        int imageViewHeight = findViewById(R.id.imageViewDraw).getHeight();             // the height of the rectangle shown in the preview
        int imageViewWidth = findViewById(R.id.imageViewDraw).getWidth();               // the width of the rectangle shown in the preview

        // Calculate the size of the rectangle compared to the full size image
        int rectHeight = (int) (heightRatio * imageViewHeight);                         // the height of the rectangle in comparison to the full size image
        int rectWidth = (int) (widthRatio * imageViewWidth);                            // the width of the rectangle in comparison to the full size image

        int startingHeight = centerHeight - rectHeight/2;                               // finds the starting pixel to start cropping from (y-axis)
        int startingWidth = centerWidth - rectWidth/2;                                  // finds the starting pixel to start cropping from (x-axis)

        Bitmap croppedBmp = Bitmap.createBitmap(fullSizeBmp, startingWidth, startingHeight, rectWidth, rectHeight); // crops the image

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