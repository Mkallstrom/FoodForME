package com.example.martin.foodforme;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;


public class InventoryActivity extends ActionBarActivity {
    ArrayAdapter adapter;
    List<String> products;
    SharedPreferences inventory;
    int index = 0;
    ArrayList inventoryKeys = new ArrayList();
    ListView listView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inventory);
        Context context = this;

        inventory = getSharedPreferences("inventory",0);
        SharedPreferences.Editor editor = inventory.edit();

        products = new ArrayList();
        adapter = new ArrayAdapter(context,android.R.layout.simple_list_item_1,products);
        listView = (ListView) findViewById(R.id.inventoryListView);
        listView.setAdapter(adapter);

        if(!inventory.contains("index"))
        {
            editor.putString("index", "0");
            editor.commit();
        }
        index = Integer.parseInt(inventory.getString("index",""));

        Map<String,?> keys = inventory.getAll();
        for(Map.Entry<String,?> entry : keys.entrySet()){
            if(!entry.getKey().equals("index"))
            {
                products.add(entry.getValue().toString());
                inventoryKeys.add(entry.getKey());
            }
        }
        adapter.notifyDataSetChanged();

        listView.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {

                SharedPreferences.Editor editor = inventory.edit();

                products.remove(position);
                adapter.notifyDataSetChanged();

                editor.remove(inventoryKeys.get(position) + "");
                editor.commit();

            }
        });

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
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 100) {                                   // Result is from addproduct
            if(resultCode == RESULT_OK) {
                String newProduct = data.getStringExtra("product"); // Gets the name of the product

                SharedPreferences.Editor editor = inventory.edit();
                index+=1;
                editor.remove("index");
                editor.putString("index", index+"");
                editor.putString(index+"", newProduct);       // Adds the product to the saved inventory
                editor.commit();

                products.add(newProduct);                           // Adds to the inventory activity list
                adapter.notifyDataSetChanged();
            }
        } else {                                                    // Result is from scanning
                IntentResult scanningResult =
                        IntentIntegrator.parseActivityResult(requestCode, resultCode, data);


                if (scanningResult != null) {
                    if (scanningResult.getContents() == null) {
                        Toast.makeText(this, "Cancelled", Toast.LENGTH_SHORT).show();
                    } else {
                        // use the result for something
                        // Toast.makeText(this, "Scanned: " +
                        //         scanningResult.getContents(), Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(this, AddProductActivity.class);
                        String message = scanningResult.getContents();
                        intent.putExtra("result", message);
                        startActivityForResult(intent, 100);
                    }
                } else { // scanningResult == null
                    Toast.makeText(this, "ERROR: No scan data received!", Toast.LENGTH_SHORT).show();
                }
            }

    }
}
