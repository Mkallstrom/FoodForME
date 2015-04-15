package com.example.martin.foodforme;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.StrictMode;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;


public class AddProductActivity extends ActionBarActivity {

    String productString;
    SharedPreferences localBarcodes;
    TextView codeView;
    EditText productName;
    String barcode;
    String databaseName;
    Context context;

    // Progress Dialog
    private ProgressDialog pDialog;

    JSONParser jsonParser = new JSONParser();
    // single product url
    private static final String url_product_details = "http://212.25.149.10/get_product_details.php";

    // url to update product
    private static final String url_update_product = "http://212.25.149.10/update_product.php";

    // JSON Node names
    private static final String TAG_SUCCESS = "success";
    private static final String TAG_PRODUCT_NAME = "product_name";
    private static final String TAG_PRODUCT = "product"; // tag for the db schema

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

        if(localBarcodes.contains(barcode))                 //Local database has the code.
        {
            String foundName = localBarcodes.getString(barcode, "Not found");
            productName.setText(foundName);
        }
        else
        {
            new GetProductDetails().execute();
            if(databaseName != null)
            {
                String foundName = databaseName;
                productName.setText(foundName);
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
    class GetProductDetails extends AsyncTask<String, String, String>
    {
        /**
         * Before starting background thread Show Progress Dialog
         * */
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            pDialog = new ProgressDialog(AddProductActivity.this);
            pDialog.setMessage("Loading product details. Please wait...");
            pDialog.setIndeterminate(false);
            pDialog.setCancelable(true);
            pDialog.show();
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
                        params.add(new BasicNameValuePair("barcode", barcode));

                        // getting product details by making HTTP request
                        // Note that product details url will use GET request
                        JSONObject json = jsonParser.makeHttpRequest(
                                url_product_details, "GET", params);

                        // check your log for json response
                        Log.d("Single Product Details", json.toString());


                        // json success tag
                        success = json.getInt(TAG_SUCCESS);
                        if (success == 1) {
                            // successfully received product details
                            JSONArray productObj = json
                                    .getJSONArray(TAG_PRODUCT); // JSON Array

                            // get first product object from JSON Array
                            JSONObject product = productObj.getJSONObject(0);

                            // product with this pid found

                            // display product data in EditText
                            productName.setText(product.getString(TAG_PRODUCT_NAME));

                        }else{
                            // product with pid not found
                            Toast.makeText(context, "Product not found. Please enter name.", Toast.LENGTH_SHORT).show();
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            });

            return null;
        }
        /**
         * After completing background task Dismiss the progress dialog
         * **/
        protected void onPostExecute(String file_url) {
            // dismiss the dialog once got all details
            pDialog.dismiss();
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
            editor.commit();
        }
        else
            if(!localBarcodes.contains(barcode))                                                             //Local database does not have the barcode.
                {
                    editor.putString(barcode,productString);
                    editor.commit();
                }
        //TODO: if(global database contains barcode && global database response to barcode != productString) global database update for barcode with productString;
        //TODO: if(global database does not contain barcode) insert barcode, productString;
        Intent intent = new Intent();
        intent.putExtra("product",productString);
        intent.putExtra("expDate",expDateString());
        intent.putExtra("code", barcode);
        setResult(RESULT_OK, intent);
        finish();
    }

    public void cancelAddProduct (View view) {
        setResult(RESULT_CANCELED);
        finish();
    }

    /**
     * This is a temporary method to open an Activity that displays all products in the database
     * @param view
     */
    public void toDatabase(View view) {
        Intent intent = new Intent(this, AllProductsActivity.class);
        startActivity(intent);
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
        for (int i = thisYear - 5; i <= thisYear + 5; i++) {
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
}
