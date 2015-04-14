package com.example.martin.foodforme;

import android.app.Activity;
import android.support.v7.app.ActionBarActivity;
import android.view.View;

import com.google.zxing.integration.android.IntentIntegrator;

import java.util.Objects;

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
}
