package com.example.martin.foodforme;

import android.app.Activity;
import android.support.v7.app.ActionBarActivity;

import com.google.zxing.integration.android.IntentIntegrator;

/**
 * Created by Andreas on 2015-04-14.
 */
public class Scanner extends ActionBarActivity {
    //Attributes
    //Constructors
    public Scanner(){
    }
    //Methods

     /*
    * Initiates the barcode scanner via intent
     */
    public void scan(Activity sendingActivity) {
        IntentIntegrator scanIntegrator = new IntentIntegrator(sendingActivity);
        scanIntegrator.initiateScan();
    }
}
