package com.example.martin.foodforme;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.Map;


public class ListActivity extends ActionBarActivity {

    ArrayList<Product> shoppingList;
    ArrayList<Product> requiredList;
    ArrayList<Product> inventoryList;
    ArrayAdapter shoppingAdapter;
    SharedPreferences shoppingSP;
    SharedPreferences requiredSP;
    SharedPreferences inventorySP;
    SharedPreferences localBarcodes;
    SharedPreferences.Editor shoppingEditor;
    ListView shoppingListView;
    int sindex = 0;
    private static final String TAG = "MyActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list);
        Context context = this;

        shoppingList = new ArrayList();
        requiredList = new ArrayList();
        inventoryList = new ArrayList();

        shoppingAdapter = new ShoppingArrayAdapter(context,R.layout.shoppinglayout,shoppingList);

        shoppingSP = getSharedPreferences("shoppingSP",0);
        requiredSP = getSharedPreferences("requiredSP",0);
        inventorySP = getSharedPreferences("inventory",0);
        localBarcodes = getSharedPreferences("localBarcodes", 0);

        shoppingEditor = shoppingSP.edit();

        shoppingListView = (ListView) findViewById(R.id.shoppinglistView);

        shoppingListView.setAdapter(shoppingAdapter);

        registerForContextMenu(shoppingListView);

        if(!shoppingSP.contains("index"))                            //If file does not contain the index, add it starting from 0.
        {
            shoppingEditor.putString("index", "0");
            shoppingEditor.commit();
        }
        sindex = Integer.parseInt(shoppingSP.getString("index",""));

        Map<String,?> keys = shoppingSP.getAll();                    //Get the products into the product listview.
        for(Map.Entry<String,?> entry : keys.entrySet()){
            if(!entry.getKey().equals("index"))
            {
                shoppingList.add(new Product(Integer.parseInt(entry.getValue().toString().substring(0, 3)), entry.getValue().toString().substring(3), entry.getKey()));
            }
        }
        keys = requiredSP.getAll();
        for(Map.Entry<String,?> entry : keys.entrySet()){
            if(!entry.getKey().equals("index"))
            {
                requiredList.add(new Product(Integer.parseInt(entry.getValue().toString().substring(0,3)), entry.getValue().toString().substring(3), entry.getKey()));
            }
        }

        keys = inventorySP.getAll();
        for(Map.Entry<String,?> entry : keys.entrySet()){
            if(!entry.getKey().equals("index"))
            {
                inventoryList.add(new Product(Integer.parseInt(entry.getValue().toString().substring(0,3)), entry.getValue().toString().substring(3), entry.getKey()));
            }
        }

        shoppingAdapter.notifyDataSetChanged();

        shoppingListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {

                openContextMenu(view);

            }
        });


    }

    public void onCreateContextMenu(ContextMenu menu, View v,ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo)menuInfo;
        if(v == findViewById(R.id.shoppinglistView))
        {
            menu.setHeaderTitle("Edit " + shoppingList.get(info.position).getName());
            menu.add(0, 1, 0, "Edit");
            menu.add(0, 2, 0, "Delete");
        }

    }

    public boolean onContextItemSelected(MenuItem item) {
        final AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        int itemID = item.getItemId();

        if(itemID == 1){
            final EditText txtUrl = new EditText(this);


            new AlertDialog.Builder(this)
                    .setTitle(shoppingList.get(info.position).getName())
                    .setView(txtUrl)
                    .setPositiveButton("Apply", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            int amount = Integer.parseInt(txtUrl.getText().toString());
                            Product item = shoppingList.get(info.position);
                            shoppingList.remove(item);
                            item.setAmount(Integer.toString(amount));
                            shoppingList.add(info.position,item);
                            shoppingAdapter.notifyDataSetChanged();
                            shoppingEditor.remove(item.getKey());
                            shoppingEditor.putString(item.getKey(), (String.format("%03d", amount) + item.getName()));
                            shoppingEditor.commit();
                        }
                    })
                    .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                        }
                    })
                    .show();

        } else if(itemID == 2) {
            Product shoppingItem = shoppingList.get(info.position);
            shoppingList.remove(shoppingItem);
            shoppingAdapter.notifyDataSetChanged();
            shoppingEditor.remove(shoppingItem.getKey());
            shoppingEditor.commit();


        } else {return false;}
        return true;
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_list, menu);
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
    public void addShopping(View view)
    {
        final EditText txtUrl = new EditText(this);

        new AlertDialog.Builder(this)
                .setTitle("Product Name")
                .setView(txtUrl)
                .setPositiveButton("Add", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        String name = txtUrl.getText().toString();
                        sindex++;
                        shoppingList.add(new Product(1, name, Integer.toString(sindex)));
                        shoppingAdapter.notifyDataSetChanged();
                        shoppingEditor.remove("index");
                        shoppingEditor.putString("index", Integer.toString(sindex));
                        shoppingEditor.putString(Integer.toString(sindex), "001" + name);
                        shoppingEditor.commit();
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                    }
                })
                .show();
    }
}
