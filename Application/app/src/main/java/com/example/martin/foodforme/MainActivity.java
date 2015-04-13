package com.example.martin.foodforme;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;


public class MainActivity extends ActionBarActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
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

    public void toInventory(View view){
        Intent intent = new Intent(this, InventoryActivity.class);
        startActivity(intent);
    }

    public void toList(View view){
        Intent intent = new Intent(this, ListActivity.class);
        startActivity(intent);
    }

    public void toRequirement(View view){
        Intent intent = new Intent(this, RequirementActivity.class);
        startActivity(intent);
    }

    public void clearSharedPreferences(View view)
    {
        SharedPreferences inventory = getSharedPreferences("inventory",0);
        SharedPreferences inventory2 = getSharedPreferences("shoppingSP",0);
        SharedPreferences inventory3 = getSharedPreferences("requiredSP",0);
        SharedPreferences.Editor inventoryEditor = inventory.edit();
        SharedPreferences.Editor inventoryEditor2 = inventory2.edit();
        SharedPreferences.Editor inventoryEditor3 = inventory3.edit();
        inventoryEditor.clear();
        inventoryEditor.commit();
        inventoryEditor2.clear();
        inventoryEditor2.commit();
        inventoryEditor3.clear();
        inventoryEditor3.commit();
    }

}
