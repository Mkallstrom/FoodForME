package com.example.martin.foodforme;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.support.v7.app.ActionBarActivity;

/**
 * Created by Andreas on 2015-04-23.
 */
public class InfoDialog extends ActionBarActivity{
    //Attributes
    private String text;
    private Context context;

    //Constructor
    public InfoDialog(String text, Context context){
        this.text = text;
        this.context = context;
    }

    public void message(){
        new AlertDialog.Builder(context)

                .setTitle(text)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {

                    }
                })

                /*.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface progressDialog, int whichButton) {
                    }
                })*/
                .show();
    }
}
