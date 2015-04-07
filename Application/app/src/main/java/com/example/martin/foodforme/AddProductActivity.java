package com.example.martin.foodforme;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;


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
        setResult(RESULT_OK, intent);
        finish();
    }
}
