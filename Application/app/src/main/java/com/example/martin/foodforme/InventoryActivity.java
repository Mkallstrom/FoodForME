package com.example.martin.foodforme;

import android.content.Intent;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;


public class InventoryActivity extends ActionBarActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inventory);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_inventory, menu);
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

    /*
    * Initiates the barcode scanner via intent
     */
    public void scanBarcode(View view) {
        IntentIntegrator scanIntegrator = new IntentIntegrator(this);
        scanIntegrator.initiateScan();
    }

    /*
    * Gets the scanning result
    */
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        IntentResult scanningResult =
                IntentIntegrator.parseActivityResult(requestCode,resultCode,data);

        if(scanningResult != null) {
            if (scanningResult.getContents() == null) {
                Toast.makeText(this, "Cancelled", Toast.LENGTH_SHORT).show();
            } else {
                // use the result for something
                Toast.makeText(this, "Scanned: " +
                        scanningResult.getContents(), Toast.LENGTH_SHORT).show();
            }
        } else { // scanningResult == null
             Toast.makeText(this, "ERROR: No scan data received!", Toast.LENGTH_SHORT).show();
        }
    }
}
