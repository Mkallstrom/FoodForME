package com.example.martin.foodforme;

import android.app.Activity;
import android.content.Intent;
import android.support.v7.app.ActionBarActivity;
import android.widget.Toast;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

/**
 * Created by Andreas on 2015-04-14.
 */
public class Scanner extends ActionBarActivity {
    //Attributes
    private Activity sendingActivity;
    //Constructors
    public Scanner(Activity sendingActivity){
        this.sendingActivity = sendingActivity;
    }
    //Methods

     /*
    * Initiates the barcode scanner via intent
     */
    public void scan() {
        IntentIntegrator scanIntegrator = new IntentIntegrator(sendingActivity);
        scanIntegrator.initiateScan();
    }

    protected void scannerResult(int requestCode, int resultCode, Intent data){
        IntentResult scanningResult =
                IntentIntegrator.parseActivityResult(requestCode, resultCode, data);

        if (scanningResult != null) {
            if (scanningResult.getContents() == null) {
                Toast.makeText(this, "Canceled", Toast.LENGTH_SHORT).show();
            } else {                                        // Start addProductActivity
                Intent intent = new Intent(sendingActivity, AddProductActivity.class);
                String message = scanningResult.getContents();
                intent.putExtra("result", message);         //Send the result to the add product activity.
                startActivityForResult(intent, 100);
            }
        } else { // scanningResult == null
            Toast.makeText(this, "ERROR: No scan data received!", Toast.LENGTH_SHORT).show();
        }

    }

}
