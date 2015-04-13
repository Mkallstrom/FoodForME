package com.example.martin.foodforme;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Calendar;


public class AddProductActivity extends ActionBarActivity {

    String productString;
    SharedPreferences localBarcodes;
    TextView codeView;
    EditText productName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_product);

        localBarcodes = getSharedPreferences("localBarcodes", 0);

        productName = (EditText)findViewById(R.id.productBox);

        Intent callingIntent = getIntent();
        String str = callingIntent.getExtras().getString("result");
        codeView = (TextView)findViewById(R.id.codeView);
        codeView.setText(str);

        if(localBarcodes.contains(str))                 //Local database has the code.
        {
            String foundName = localBarcodes.getString(str, "Not found");
            productName.setText(foundName);
        }
        else
        {
            //CHECK GLOBAL DATABASE HERE
            Toast.makeText(this, "Product not found. Please enter name.", Toast.LENGTH_SHORT).show();
        }

        // Fills the spinner with years
        fillSpinnerYear();

        // Fills the spinner with months
        fillSpinnerMonth();

        // Fills the spinner with days
        fillSpinnerDay();
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
