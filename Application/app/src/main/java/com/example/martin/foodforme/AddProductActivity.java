package com.example.martin.foodforme;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
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

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;


public class AddProductActivity extends ActionBarActivity {
    //Attributes
    SharedPreferences localBarcodes;
    TextView codeView;
    EditText productName;

    String productString;
    String barcode;
    String databaseName;

    Context context;
    Boolean databaseHasProduct = false;
    Boolean localHasProduct = false;
    Boolean connection = false;
    JSONObject json;
    JSONParser jsonParser = new JSONParser();

    public static final String DATA_PATH = Environment
            .getExternalStorageDirectory().toString() + "/FoodForMe/";
    public static final String lang = "swe";
    protected String _path;
    protected boolean _taken;
    protected static final String PHOTO_TAKEN = "photo_taken";
    private static final String TAG = "Tesseract: ";

    private static final String ip = "http://212.25.153.193:8080/"; // Ip address for database
    private static final String url_product_details = ip + "get_product_details.php"; // single product url
    private static final String url_update_product = ip + "update_product.php";  // url to update product
    private static final String url_create_product = ip + "create_product.php"; // url to create new product

    // JSON Node names
    private static final String TAG_SUCCESS = "success";
    private static final String TAG_PRODUCT_NAME = "product_name";
    private static final String TAG_PRODUCT = "product";
    private static final String TAG_BARCODE = "barcode";

    //Methods
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);
        setContentView(R.layout.activity_add_product);
        setTitle("Add Product");
        context = this;

        localBarcodes = getSharedPreferences("localBarcodes", 0);

        productName = (EditText)findViewById(R.id.productBox);

        Intent callingIntent = getIntent();
        barcode = callingIntent.getExtras().getString("result");
        codeView = (TextView)findViewById(R.id.codeView);
        codeView.setText(barcode);

        try {
            //make a URL to a known source
            URL url = new URL(ip);

            //open a connection to that source
            HttpURLConnection urlConnect = (HttpURLConnection)url.openConnection();

            //trying to retrieve data from the source. If there
            //is no connection, this line will fail
            Object objData = urlConnect.getContent();
            CheckBox checkBox = (CheckBox) findViewById(R.id.connectionCheck);
            checkBox.setChecked(true);
            connection = true;
            new GetProductDetails().execute(); // AsyncTask created searching the database
        }
        catch (Exception e)
        {
            e.printStackTrace();
            Log.d("Connection failed: ", ip);
        }

        if(localBarcodes.contains(barcode))                 //Local database has the code.
        {
            String foundName = localBarcodes.getString(barcode, "Not found");
            productName.setText(foundName);
            localHasProduct = true;
        }
        else
        {
            if(databaseHasProduct) // NOTE: This one seems to be false every time (but product.setText is done in the GetProductDetails AsyncTask)
            {
                productName.setText(databaseName);
            }
            else {
                //Nothing found at all!
            }
        }

        // Fills the spinner with years
        fillSpinnerYear();

        // Fills the spinner with months
        fillSpinnerMonth();

        // Fills the spinner with days
        fillSpinnerDay();
    }

    class CreateNewProduct extends AsyncTask<String, String, String> {

        /**
         * Before starting background thread
         * */
        @Override
        protected void onPreExecute() {

        }

        /**
         * Creating product
         * */
        protected String doInBackground(String... args) {

            // Building Parameters
            List<NameValuePair> params = new ArrayList<NameValuePair>();
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
                } else {
                    // failed to create product
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }

            return null;
        }

        /**
         * After completing background task
         * **/
        protected void onPostExecute(String file_url) {
            // dismiss the dialog once done
    }

    }

    class GetProductDetails extends AsyncTask<String, String, String>
    {
        /**
         * Before starting background thread
         * */
        @Override
        protected void onPreExecute() {

        }
        /**
         * Getting product details in background thread
         * */
        protected String doInBackground(String... params)
        {

            // updating UI from Background Thread
            runOnUiThread(new Runnable() {
                public void run() {
                    // Check for success tag
                    int success;
                    try {
                        // Building Parameters
                        List<NameValuePair> params = new ArrayList<NameValuePair>();
                        params.add(new BasicNameValuePair(TAG_BARCODE, barcode));

                        // getting product details by making HTTP request
                        // Note that product details url will use GET request

                                json = jsonParser.makeHttpRequest(
                                url_product_details, "GET", params);

                        if (json == null) {
                            Log.d("Product not found :", barcode);
                            if(!localHasProduct)
                            {
                                Toast.makeText(context, "Product not found. Please enter name.", Toast.LENGTH_SHORT).show();
                            }
                            return;
                        }
                        databaseHasProduct = true;

                        // json success tag
                        success = json.getInt(TAG_SUCCESS);
                        if (success == 1) {
                            // check your log for json response
                            Log.d("Single Product Details", json.toString());
                            // successfully received product details
                            JSONArray productObj = json
                                    .getJSONArray(TAG_PRODUCT); // JSON Array

                            JSONObject product = productObj.getJSONObject(0);   // get first product object from JSON Array
                            databaseName = product.getString(TAG_PRODUCT_NAME); // sets databaseName to what was found in the database

                            if(!localHasProduct)
                            {
                                productName.setText(databaseName);
                            }

                        } else {
                            // product with barcode not found

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
         * **/
        protected void onPostExecute(String file_url) {

        }

    }

    /**
     * Background Async Task to  Save product Details
     * */
    class SaveProductDetails extends AsyncTask<String, String, String> {

        /**
         * Before starting background thread
         * */
        @Override
        protected void onPreExecute() {
        }

        /**
         * Saving product
         * */
        protected String doInBackground(String... args) {

            // Building Parameters
            List<NameValuePair> params = new ArrayList<NameValuePair>();
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
                } else {
                    // failed to update product
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }

            return null;
        }

        /**
         * After completing background task
         * **/
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

        if (localBarcodes.contains(barcode) && !localBarcodes.getString(barcode, "").equals(productString))   //Local database has the barcode but the name does not match (user changed it)
        {                                                                                                    // -> replace with new name.
            editor.remove(codeView.getText().toString());
            editor.putString(barcode,productString);
            editor.apply();
        }
        else
            if(!localBarcodes.contains(barcode))                                                             //Local database does not have the barcode.
                {
                    editor.putString(barcode,productString);
                    editor.apply();
                }
        if(json != null)
        {
            if (databaseHasProduct && !databaseName.equals(productString)) {                                // if the product is in the database and the name does not match the new String
                Log.d("Saving: ", productString);
                new SaveProductDetails().execute();                                                         // Saves the new String to the database
            }
        }
        else
        {
            if (!databaseHasProduct && connection) {                                                        // if the product is not in the database and there is a connection
                new CreateNewProduct().execute();                                                           // Saves a new product to the database
            }
        }

        Intent intent = new Intent();
        intent.putExtra("product",productString);
        intent.putExtra("expDate",expDateString());
        intent.putExtra("code", barcode);
        CheckBox checkBox = (CheckBox) findViewById(R.id.expiresCheck);
        intent.putExtra("expires", checkBox.isChecked());
        setResult(RESULT_OK, intent);
        finish();
    }

    public void cancelAddProduct (View view) {
        setResult(RESULT_CANCELED);
        finish();
    }

    // Extracts the expiration date set by the user
    private String expDateString(){
        Spinner spinner = (Spinner)findViewById(R.id.spinnerYear);
        String selectedYear = spinner.getSelectedItem().toString();
        spinner = (Spinner)findViewById(R.id.spinnerMonth);
        String selectedMonth = spinner.getSelectedItem().toString();
        spinner = (Spinner)findViewById(R.id.spinnerDay);
        String selectedDay = spinner.getSelectedItem().toString();
        return selectedYear + "-" + selectedMonth + "-" + selectedDay;
    }

    private void fillSpinnerYear(){
        ArrayList<String> arrayListYears = new ArrayList<String>();
        int thisYear = Calendar.getInstance().get(Calendar.YEAR);
        for (int i = thisYear; i <= thisYear + 10; i++) {
            arrayListYears.add(Integer.toString(i));
        }
        ArrayAdapter<String> spinYearAdapter =
                new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, arrayListYears);
        Spinner spinYear = (Spinner)findViewById(R.id.spinnerYear);
        spinYear.setAdapter(spinYearAdapter);
    }

    private void fillSpinnerMonth(){
        ArrayList<String> arrayListMonths = new ArrayList<>();
        for(int i = 1; i <= 12; i++) {
            arrayListMonths.add(Integer.toString(i));
        }
        ArrayAdapter<String> spinMonthAdapter =
                new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, arrayListMonths);
        Spinner spinMonth = (Spinner)findViewById(R.id.spinnerMonth);
        spinMonth.setAdapter(spinMonthAdapter);
    }

    private void fillSpinnerDay(){
        ArrayList<String> arrayListDays = new ArrayList<>();
        for(int i = 1; i <= 31; i++) {
            arrayListDays.add(Integer.toString(i));
        }
        ArrayAdapter<String> spinDayAdapter =
                new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, arrayListDays);
        Spinner spinDay = (Spinner)findViewById(R.id.spinnerDay);
        spinDay.setAdapter(spinDayAdapter);
    }
    public void clickedConnection(View view)
    {
        CheckBox checkBox = (CheckBox) findViewById(R.id.expiresCheck);
        checkBox.toggle();
    }
}
