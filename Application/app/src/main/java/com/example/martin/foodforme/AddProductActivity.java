package com.example.martin.foodforme;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.StrictMode;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.googlecode.tesseract.android.TessBaseAPI;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;


public class AddProductActivity extends ActionBarActivity {
    //Attributes
    private SharedPreferences localBarcodes;
    private TextView codeView;
    private EditText productName;
    private EditText productAmount;

    private String productString;
    private String barcode;
    private String databaseName;

    private Context context;
    private Boolean databaseHasProduct = false;
    private Boolean localHasProduct = false;
    private Boolean connection = false;
    private JSONObject json;
    private JSONParser jsonParser = new JSONParser();

    public static final String DATA_PATH = Environment
            .getExternalStorageDirectory().toString() + "/FoodForME/";
    public static final String lang = "swe";
    protected String _path;
    protected boolean _taken;
    protected static final String PHOTO_TAKEN = "photo_taken";
    private static final String TAG = "Tesseract: ";

    private static final String ip = "http://ffm.student.it.uu.se/"; // Ip address for database
    private static final String url_product_details = ip + "get_product_details.php"; // single product url
    private static final String url_update_product = ip + "update_product.php";  // url to update product
    private static final String url_create_product = ip + "create_product.php"; // url to create new product

    // JSON Node names
    private static final String TAG_SUCCESS = "success";
    private static final String TAG_PRODUCT_NAME = "product_name";
    private static final String TAG_PRODUCT = "product";
    private static final String TAG_BARCODE = "barcode";

    private AccountDB accountDB;

    //Methods
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);
        setContentView(R.layout.activity_add_product);
        context = this;
        accountDB = (AccountDB) getApplicationContext();
        if(!accountDB.isLocal()) setTitle(accountDB.getAccountUsername() + " - Add Product");
        else setTitle("Add Product");

        String[] paths = new String[]{DATA_PATH, DATA_PATH + "tessdata/"};
        for (String path : paths) {
            File dir = new File(path);
            if (!dir.exists()) {
                if (!dir.mkdirs()) {
                    Log.v(TAG, "ERROR: Creation of directory " + path + " on sdcard failed");
                    return;
                } else {
                    Log.v(TAG, "Created directory " + path + " on sdcard");

                }
            }

        }
        if (!(new File(DATA_PATH + "tessdata/" + lang + ".traineddata")).exists()) {
            try {

                AssetManager assetManager = getAssets();
                InputStream in = assetManager.open("tessdata/" + lang + ".traineddata");
                //GZIPInputStream gin = new GZIPInputStream(in);
                OutputStream out = new FileOutputStream(DATA_PATH
                        + "tessdata/" + lang + ".traineddata");

                // Transfer bytes from in to out
                byte[] buf = new byte[1024];
                int len;
                //while ((lenf = gin.read(buff)) > 0) {
                while ((len = in.read(buf)) > 0) {
                    out.write(buf, 0, len);
                }
                in.close();
                //gin.close();
                out.close();

                Log.v(TAG, "Copied " + lang + " traineddata");
            } catch (IOException e) {
                Log.e(TAG, "Was unable to copy " + lang + " traineddata " + e.toString());
            }
        }
        _path = DATA_PATH + "/ocr.jpg";

        localBarcodes = getSharedPreferences("localBarcodes", 0);

        productName = (EditText) findViewById(R.id.productBox);
        productAmount = (EditText) findViewById(R.id.amountText);

        Intent callingIntent = getIntent();
        barcode = callingIntent.getExtras().getString("result");
        codeView = (TextView) findViewById(R.id.codeView);
        codeView.setText(barcode);
        if(barcode.equals(accountDB.getNoBarcode()))
        {}
        else {
            try {
                //make a URL to a known source
                URL url = new URL(ip);

                //open a connection to that source
                HttpURLConnection urlConnect = (HttpURLConnection) url.openConnection();
                urlConnect.setConnectTimeout(1000);

                //trying to retrieve data from the source. If there
                //is no connection, this line will fail
                Object objData = urlConnect.getContent();
                CheckBox checkBox = (CheckBox) findViewById(R.id.connectionCheck);
                checkBox.setChecked(true);
                connection = true;
                new GetProductDetails().execute(); // AsyncTask created searching the database
            } catch (Exception e) {
                e.printStackTrace();
                Log.d("Connection failed: ", ip);
            }

            if (localBarcodes.contains(barcode))                 //Local database has the code.
            {
                String foundName = localBarcodes.getString(barcode, "Not found");
                productName.setText(foundName);
                localHasProduct = true;
            }
        }

        // Fills the spinner with years
        fillSpinnerYear();

        // Fills the spinner with months
        fillSpinnerMonth();

        // Fills the spinner with days
        fillSpinnerDay();
    }

    public void startCameraActivity(View view) {
        File file = new File(_path);
        Uri outputFileUri = Uri.fromFile(file);

        //final Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        //intent.putExtra(MediaStore.EXTRA_OUTPUT, outputFileUri);

        //startActivityForResult(intent, 0);

         //This code can be used to call the custom Camera activity (not completed, but working)

        Intent intent = new Intent(this, CameraActivity.class);
        startActivityForResult(intent, 45);                     // The code can be changed, as long as it is handled in onActivityResult

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        Log.i(TAG, "resultCode: " + resultCode);
        if (resultCode == -1) {
            if (requestCode == 80) {
                this.productName.setText(data.getStringExtra("productName"));
            } else {
                onPhotoTaken();
            }
        } else {
            Log.v(TAG, "User cancelled");
        }
    }


    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putBoolean(AddProductActivity.PHOTO_TAKEN, _taken);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        Log.i(TAG, "onRestoreInstanceState()");
        if (savedInstanceState.getBoolean(AddProductActivity.PHOTO_TAKEN)) {
            onPhotoTaken();
        }
    }

    protected void onPhotoTaken() {
        _taken = true;

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = 4;

        Bitmap bitmap = BitmapFactory.decodeFile(_path, options);

        try {
            ExifInterface exif = new ExifInterface(_path);
            int exifOrientation = exif.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL);

            Log.v(TAG, "Orient: " + exifOrientation);

            int rotate = 0;

            switch (exifOrientation) {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    rotate = 90;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    rotate = 180;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    rotate = 270;
                    break;
            }

            Log.v(TAG, "Rotation: " + rotate);

            if (rotate != 0) {

                // Getting width & height of the given image.
                int w = bitmap.getWidth();
                int h = bitmap.getHeight();

                // Setting pre rotate
                Matrix mtx = new Matrix();
                mtx.preRotate(rotate);

                // Rotating Bitmap
                bitmap = Bitmap.createBitmap(bitmap, 0, 0, w, h, mtx, false);
            }

            // Convert to ARGB_8888, required by tess
            bitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true);

        } catch (IOException e) {
            Log.e(TAG, "Couldn't correct orientation: " + e.toString());
        }

        // _image.setImageBitmap( bitmap );

        Log.v(TAG, "Before baseApi");

        TessBaseAPI baseApi = new TessBaseAPI();
        baseApi.setDebug(true);
        baseApi.init(DATA_PATH, lang);
        baseApi.setImage(bitmap);
        //String whiteList = "ACEFJKLMNOPRSTU1234567890";                 // White list certain characters
        //baseApi.setVariable(TessBaseAPI.VAR_CHAR_WHITELIST, whiteList);  // Set the white list to baseApi
        String recognizedText = baseApi.getUTF8Text();                  // extracts the text from the image

        baseApi.end();

        // You now have the text in recognizedText var, you can do anything with it.
        // We will display a stripped out trimmed alpha-numeric version of it (if lang is eng [using swe now])
        // so that garbage doesn't make it to the display.

        Log.v(TAG, "OCRED TEXT: " + recognizedText);

        String unmodifiedString = recognizedText;

        if (lang.equalsIgnoreCase("swe")) {
            recognizedText = recognizedText.replaceAll("[^A-Z0-9]+", " ");
        }

        recognizedText = recognizedText.trim();

        String expirationDate = extractExpDate(recognizedText);
        if(expirationDate == null) {
            Log.e(TAG, "Failed to extract the expiration date");
            Toast.makeText(context, "Could not extract the date. Please try again or set it manually.", Toast.LENGTH_LONG).show();
        } else {
            int[] parsedDate = Product.dateStringToArray(expirationDate);
            setSpinnerDate(parsedDate);
        }

        if (recognizedText.length() != 0) {
            // Create a progressDialog to display the text
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage("Tesseract: " + recognizedText)
                    .setCancelable(false)
                    .setPositiveButton("OK", null);
            AlertDialog alert = builder.create();
            alert.show();
        }

        // Cycle done.
    }

    /**
     * Extracts the expiration date from a string (produced by Tesseract)
     * @param text - extracted text from image
     * @return - expiration date
     */
    private String extractExpDate(String text) {
        Log.i("extractExpDate", "Starting expiration date extraction in method extractExpDate");
        String[] monthStrings = {"JAN","FEB","MAR","APR","MAJ","JUN","JUL","AUG","SEP","OKT","NOV","DEC"};
        String yyyy, MM, dd;

        for(int i = 0; i < monthStrings.length; i++) {          // find month strings in the text and replace with month digits
            if(text.contains(monthStrings[i])) {
                if(i < 10) {
                    text = text.replace(monthStrings[i],"0" + i);
                    break;
                } else {
                    text = text.replace(monthStrings[i], "" + i);
                    break;
                }
            }
        }

        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (Character.isDigit(c)) {
                builder.append(c);
            }
        }
        String digits = builder.toString();
        if(digits.length() >= 8) {              // date format: dd MM yyyy
            dd = digits.substring(0, 2);
            MM = digits.substring(2, 4);
            yyyy = digits.substring(4, 8);
        } else if(digits.length() == 6){        // date format: dd MM yy
            dd = digits.substring(0, 2);
            MM = digits.substring(2, 4);
            int thisYear = Calendar.getInstance().get(Calendar.YEAR);
            yyyy = Integer.toString(thisYear).substring(0,2) + digits.substring(4, 6);
        }else {
            return null;
        }

        String extractedDateString = yyyy + "-" + MM + "-" + dd;
        Log.d("extractExpDate", "extracted date: " + extractedDateString);
        return extractedDateString;

        // TODO: Extend the functionality to cover more advanced dates
    }

    private void setSpinnerDate(int[] date) {
        int year = date[0];
        int month = date[1];
        int day = date[2];

        try {
            Spinner spinnerDay = (Spinner) findViewById(R.id.spinnerDay);
            int dayIndex = findDayIndex(day, spinnerDay);

            Spinner spinnerMonth = (Spinner) findViewById(R.id.spinnerMonth);
            int monthIndex = findMonthIndex(month, spinnerMonth);

            Spinner spinnerYear = (Spinner) findViewById(R.id.spinnerYear);
            int yearIndex = findYearIndex(year, spinnerYear);

            spinnerDay.setSelection(dayIndex);
            spinnerYear.setSelection(yearIndex);
            spinnerMonth.setSelection(monthIndex);
            Log.d("setSpinnerDate", "Date successfully set to: Day: " + day + ", Month: " + month + ", Year: " + year);
        } catch (Exception e) {
            Log.e("setSpinnerDate", e.getMessage());
            Toast.makeText(context, "ERROR: " + e.getMessage(),Toast.LENGTH_LONG).show();
        }
    }

    class CreateNewProduct extends AsyncTask<String, String, String> {

        /**
         * Before starting background thread
         */
        @Override
        protected void onPreExecute() {

        }

        /**
         * Creating product
         */
        protected String doInBackground(String... args) {

            // Building Parameters
            List<NameValuePair> params = new ArrayList<>();
            params.add(new BasicNameValuePair(TAG_PRODUCT_NAME, productString));
            params.add(new BasicNameValuePair(TAG_BARCODE, barcode));

            // getting JSON Object
            // Note that create product url accepts POST method
            JSONObject json = jsonParser.makeHttpRequest(url_create_product,
                    "POST", params);
            // check log cat for response
            Log.d("Create Response", json.toString());

            // check for success tag
            try {
                int success = json.getInt(TAG_SUCCESS);

                if (success == 1) {
                    // successfully created product
                    // closing this screen
                    finish();
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }

            return null;
        }

        /**
         * After completing background task
         * *
         */
        protected void onPostExecute(String file_url) {
            // dismiss the progressDialog once done
        }

    }

    class GetProductDetails extends AsyncTask<String, String, String> {
        private ProgressDialog pDialog;
        /**
         * Before starting background thread
         */
        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            // Creates a ProgressDialog that will be visible until the task is completed
            pDialog = new ProgressDialog(context);
            pDialog.setMessage("Searching the product database. Please wait...");
            pDialog.setIndeterminate(false);
            pDialog.setCancelable(true);
            pDialog.show();
        }

        /**
         * Getting product details in background thread
         */
        protected String doInBackground(String... params) {


            // updating UI from Background Thread
            runOnUiThread(new Runnable() {
                public void run() {
                    // Check for success tag
                    int success;
                    try {
                        // Building Parameters
                        List<NameValuePair> params = new ArrayList<>();
                        params.add(new BasicNameValuePair(TAG_BARCODE, barcode));

                        // getting product details by making HTTP request
                        // Note that product details url will use GET request

                        json = jsonParser.makeHttpRequest(
                                url_product_details, "GET", params);

                        if (json == null) {
                            Log.d("Product not found :", barcode);

                            return;
                        }


                        // json success tag
                        success = json.getInt(TAG_SUCCESS);
                        if (success == 1) {
                            // check your log for json response
                            Log.d("Single Product Details", json.toString());
                            // successfully received product details
                            JSONArray productObj = json
                                    .getJSONArray(TAG_PRODUCT); // JSON Array

                            JSONObject product = productObj.getJSONObject(0);   // get first product object from JSON Array
                            databaseHasProduct = true;
                            databaseName = product.getString(TAG_PRODUCT_NAME); // sets databaseName to what was found in the database

                            if (!localHasProduct) {
                                productName.setText(databaseName);
                                Log.d("Found name", databaseName);
                            }

                        } else {
                            // product with barcode not found
                            if (!localHasProduct) {
                                Toast.makeText(context, "Product not found. Please enter name.", Toast.LENGTH_SHORT).show();
                            }

                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            });

            return null;
        }

        /**
         * After completing background task
         * *
         */
        protected void onPostExecute(String file_url) {
            pDialog.dismiss(); // the progress progressDialog can be dismissed

            if (!databaseHasProduct && !localHasProduct) { // product was not in the local nor global database
                Intent intent = new Intent(context, Item_not_found.class);
                startActivityForResult(intent, 80);
            }
        }

    }

    /**
     * Background Async Task to  Save product Details
     */
    class SaveProductDetails extends AsyncTask<String, String, String> {

        /**
         * Before starting background thread
         */
        @Override
        protected void onPreExecute() {
        }

        /**
         * Saving product
         */
        protected String doInBackground(String... args) {

            // Building Parameters
            List<NameValuePair> params = new ArrayList<>();
            params.add(new BasicNameValuePair(TAG_PRODUCT_NAME, productString));
            params.add(new BasicNameValuePair(TAG_BARCODE, barcode));


            // sending modified data through http request
            // Notice that update product url accepts POST method
            JSONObject json = jsonParser.makeHttpRequest(url_update_product,
                    "POST", params);

            // check json success tag
            try {
                int success = json.getInt(TAG_SUCCESS);

                if (success == 1) {
                    // successfully updated
                    finish();
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }

            return null;
        }

        /**
         * After completing background task
         * *
         */
        protected void onPostExecute(String file_url) {
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_add_product, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void addProduct(View view) {
        productString = productName.getText().toString();
        String barcode = codeView.getText().toString();
        SharedPreferences.Editor editor = localBarcodes.edit();
        String amount = productAmount.getText().toString();
        if(barcode.equals(accountDB.getNoBarcode())){}
        else {
            if (localBarcodes.contains(barcode) && !localBarcodes.getString(barcode, "").equals(productString))   //Local database has the barcode but the name does not match (user changed it)
            {                                                                                                    // -> replace with new name.
                editor.remove(codeView.getText().toString());
                editor.putString(barcode, productString);
                editor.apply();
            } else if (!localBarcodes.contains(barcode))                                                             //Local database does not have the barcode.
            {
                editor.putString(barcode, productString);
                editor.apply();
            }

            int success = 0;
            if (json == null) {
                Intent intent = new Intent();
                intent.putExtra("product", "<<<Error! Connection failed in process.>>>");
                intent.putExtra("expDate", "0000-00-00");
                intent.putExtra("amount", "0");
                intent.putExtra("code", "failed");
                CheckBox checkBox = (CheckBox) findViewById(R.id.expiresCheck);
                intent.putExtra("expires", checkBox.isChecked());
                setResult(RESULT_OK, intent);
                finish();
                return;
                //TODO check if this actually works and intended and not makeing it worse
            }

            try {
                success = json.getInt(TAG_SUCCESS);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            if (success == 1) {
                if (databaseHasProduct) {
                    if (!databaseName.equals(productString))// if the product is in the database and the name does not match the new String
                    {
                        Log.d("AddProductAct", productString + " has different name in database.");
                        //new SaveProductDetails().execute();
                    }                                                                        // Saves the new String to the database
                }
            } else if (success == 0) {
                if (!databaseHasProduct && connection) {                                                        // if the product is not in the database and there is a connection
                    Log.d("Creating: ", productString);
                    new CreateNewProduct().execute();                                                           // Saves a new product to the database
                }
            }
        }
        Intent intent = new Intent();
        intent.putExtra("product", productString);
        intent.putExtra("expDate", expDateString());
        intent.putExtra("amount", amount);
        intent.putExtra("code", barcode);
        CheckBox checkBox = (CheckBox) findViewById(R.id.expiresCheck);
        intent.putExtra("expires", checkBox.isChecked());
        setResult(RESULT_OK, intent);
        finish();
    }

    public void cancelAddProduct(View view) {
        setResult(RESULT_CANCELED);
        finish();
    }

    public void clickedConnection(View view) {
        CheckBox checkBox = (CheckBox) findViewById(R.id.connectionCheck);
        checkBox.toggle();
        if (checkBox.isChecked()) {
            Toast.makeText(context, "You are connected to the database", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(context, "You are not connected to the database", Toast.LENGTH_SHORT).show();
        }
    }

    // Extracts the expiration date set in the spinners
    private String expDateString() {
        Spinner spinner = (Spinner) findViewById(R.id.spinnerYear);
        String selectedYear = spinner.getSelectedItem().toString();
        spinner = (Spinner) findViewById(R.id.spinnerMonth);
        String selectedMonth = spinner.getSelectedItem().toString();
        spinner = (Spinner) findViewById(R.id.spinnerDay);
        String selectedDay = spinner.getSelectedItem().toString();
        return selectedYear + "-" + selectedMonth + "-" + selectedDay;
    }

    private void fillSpinnerYear() {
        ArrayList<String> arrayListYears = new ArrayList<>();
        int thisYear = Calendar.getInstance().get(Calendar.YEAR);
        for (int i = thisYear; i <= thisYear + 10; i++) {
            arrayListYears.add(Integer.toString(i));
        }
        ArrayAdapter<String> spinYearAdapter =
                new ArrayAdapter<>(this, R.layout.simple_spinner_item, arrayListYears);
        Spinner spinYear = (Spinner) findViewById(R.id.spinnerYear);
        spinYear.setAdapter(spinYearAdapter);
    }

    private void fillSpinnerMonth() {
        ArrayList<String> arrayListMonths = new ArrayList<>();
        for (int i = 1; i <= 12; i++) {
            arrayListMonths.add(Integer.toString(i));
        }
        ArrayAdapter<String> spinMonthAdapter =
                new ArrayAdapter<>(this, R.layout.simple_spinner_item, arrayListMonths);
        Spinner spinMonth = (Spinner) findViewById(R.id.spinnerMonth);
        spinMonth.setAdapter(spinMonthAdapter);
        int thisMonth = Calendar.getInstance().get(Calendar.MONTH);
        spinMonth.setSelection(thisMonth);
    }

    private void fillSpinnerDay() {
        ArrayList<String> arrayListDays = new ArrayList<>();
        for (int i = 1; i <= 31; i++) {
            arrayListDays.add(Integer.toString(i));
        }
        ArrayAdapter<String> spinDayAdapter =
                new ArrayAdapter<>(this, R.layout.simple_spinner_item, arrayListDays);
        Spinner spinDay = (Spinner) findViewById(R.id.spinnerDay);
        spinDay.setAdapter(spinDayAdapter);
        int thisDay = Calendar.getInstance().get(Calendar.DATE);
        spinDay.setSelection(thisDay);
    }

    private int findDayIndex(int day, Spinner spinnerDay) throws Exception{
        int numOfSpinnerItems = spinnerDay.getAdapter().getCount();
        if(day >= 1 && day <= 31) {
            for (int i = 0; i < numOfSpinnerItems; i++) {
                int tempDay = Integer.parseInt(spinnerDay.getAdapter().getItem(i).toString());
                if (day == tempDay) {
                    return i;
                }
            }
        } else {
            throw new Exception("Could not set day: " + day);
        }
        return -1;
    }

    private int findMonthIndex(int month, Spinner spinnerMonth) throws Exception {
        int numOfSpinnerItems = spinnerMonth.getAdapter().getCount();
        if(month >= 1 && month <= 12) {
            for (int i = 0; i < numOfSpinnerItems; i++) {
                int tempMonth = Integer.parseInt(spinnerMonth.getAdapter().getItem(i).toString());
                if (month == tempMonth) {
                    return i;
                }
            }
        } else {
            throw new Exception("Could not set month: " + month);
        }
        return -1;
    }

    private int findYearIndex(int year, Spinner spinnerYear) throws Exception {
        int numOfSpinnerItems = spinnerYear.getAdapter().getCount();
        int thisYear = Calendar.getInstance().get(Calendar.YEAR);
        int lastAcceptedYear = Integer.parseInt(spinnerYear.getAdapter().getItem(numOfSpinnerItems - 1).toString());
        if(year >= thisYear && year <= lastAcceptedYear) {
            for (int i = 0; i < numOfSpinnerItems; i++) {
                int tempYear = Integer.parseInt(spinnerYear.getAdapter().getItem(i).toString());
                if (year == tempYear) {
                    return i;
                }
            }
        } else {
            throw new Exception("Could not set year: " + year);
        }
        return -1;
    }

    /**
     * Finds today's date and returns it as a String in the format "year-month-day" (yyyy-MM-dd).
     * @return  A string representing today's date. yyyy-MM-dd
     */
    private String getTodaysDate() {
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        Date date = new Date();
        return dateFormat.format(date);
    }

}
