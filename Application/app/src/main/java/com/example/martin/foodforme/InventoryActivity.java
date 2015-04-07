package com.example.martin.foodforme;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;
import android.view.ContextMenu.ContextMenuInfo;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;


public class InventoryActivity extends ActionBarActivity {

    List<String> products;
    ArrayAdapter productsAdapter;

    SharedPreferences inventory;
    ArrayList inventoryKeys = new ArrayList();
    int index = 0;

    ListView listView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inventory);
        Context context = this;

        products = new ArrayList();
        productsAdapter = new ArrayAdapter(context,android.R.layout.simple_list_item_1,products);

        inventory = getSharedPreferences("inventory",0);
        SharedPreferences.Editor inventoryEditor = inventory.edit();

        listView = (ListView) findViewById(R.id.inventoryListView);
        listView.setAdapter(productsAdapter);
        registerForContextMenu(listView);

        if(!inventory.contains("index"))                            //If file does not contain the index, add it starting from 0.
        {
            inventoryEditor.putString("index", "0");
            inventoryEditor.commit();
        }
        index = Integer.parseInt(inventory.getString("index",""));  //Get and save the index.

        Map<String,?> keys = inventory.getAll();                    //Get the products into the product listview.
        for(Map.Entry<String,?> entry : keys.entrySet()){
            if(!entry.getKey().equals("index"))
            {
                products.add(entry.getValue().toString());
                inventoryKeys.add(entry.getKey());
            }
        }
        productsAdapter.notifyDataSetChanged();

        listView.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {

                openContextMenu(view);

            }
        });

    }

    public void onCreateContextMenu(ContextMenu menu, View v,ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo)menuInfo;
        menu.setHeaderTitle("Edit " + products.get(info.position));
        menu.add(0, 1, 0, "Delete");
        menu.add(0, 2, 0, "Edit");
    }

    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        int itemID = item.getItemId();

        if(itemID == 1){
            products.remove(info.position);
            productsAdapter.notifyDataSetChanged();
            SharedPreferences.Editor editor = inventory.edit();
            editor.remove(inventoryKeys.get(info.position) + "");
            editor.commit();
        } else if(itemID == 2) {
            // TODO: Implement some kind of edit functionality
            Toast.makeText(this, "Edit was pressed on " + products.get(info.position), Toast.LENGTH_SHORT).show();
        } else {return false;}
        return true;
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
    * Results from other activities needs to be handled here (ex. scanner)
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
                inventoryKeys.add(index);

                products.add(newProduct);                           // Adds to the inventory activity list
                productsAdapter.notifyDataSetChanged();
            } else if (resultCode == RESULT_CANCELED) {             // addProduct was canceled
                Toast.makeText(this, "The product was not added.", Toast.LENGTH_SHORT).show();
            }
        } else {                                                    // Result is from scanning
                IntentResult scanningResult =
                        IntentIntegrator.parseActivityResult(requestCode, resultCode, data);

                if (scanningResult != null) {
                    if (scanningResult.getContents() == null) {
                        Toast.makeText(this, "Canceled", Toast.LENGTH_SHORT).show();
                    } else {                                        // Start addProductActivity
                        Intent intent = new Intent(this, AddProductActivity.class);
                        String message = scanningResult.getContents();
                        intent.putExtra("result", message);         //Send the result to the add product activity.
                        startActivityForResult(intent, 100);
                    }
                } else { // scanningResult == null
                    Toast.makeText(this, "ERROR: No scan data received!", Toast.LENGTH_SHORT).show();
                }
            }

    }
}
