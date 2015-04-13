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
                shoppingList.add(parseSharedPreferences(entry.getValue().toString(), entry.getKey().toString()));
            }
        }
        keys = requiredSP.getAll();
        for(Map.Entry<String,?> entry : keys.entrySet()){
            if(!entry.getKey().equals("index"))
            {
                requiredList.add(parseSharedPreferences(entry.getValue().toString(), entry.getKey().toString()));
            }
        }

        keys = inventorySP.getAll();
        for(Map.Entry<String,?> entry : keys.entrySet()){
            if(!entry.getKey().equals("index"))
            {
                inventoryList.add(parseSharedPreferences(entry.getValue().toString(), entry.getKey().toString()));
            }
        }

        ArrayList codes = new ArrayList();
        for(Product p : inventoryList) codes.add(p.getCode());
        for(Product p : shoppingList) codes.add(p.getCode());
        for(Product p : requiredList)
        {
            String code = p.getCode();
            Product changedItem = null;
            if(!codes.contains(code))
            {
                Product newProduct = new Product(p.getName(), p.getExpiryDate(), p.getKey(), Integer.parseInt(p.getAmount()), p.getCode());
                shoppingList.add(newProduct);
                changedItem = newProduct;
                shoppingEditor.putString(Integer.toString(sindex), changedItem.toString());
            }

            int inventoryAmount = 0;
            int shoppinglistAmount = 0;

            for(Product r : inventoryList)
            {
                if(r.getCode().equals(code))
                {
                    inventoryAmount += Integer.parseInt(r.getAmount());
                }
            }
            for(Product r : shoppingList)
            {
                if(r.getCode().equals(code))
                {
                    shoppinglistAmount += Integer.parseInt(r.getAmount());
                    changedItem = r;
                }
            }
            if(Integer.parseInt(p.getAmount()) > (shoppinglistAmount+inventoryAmount)) // Increase amount
            {
                changedItem.setAmount(Integer.toString(Integer.parseInt(p.getAmount())-inventoryAmount));
                shoppingEditor.remove(changedItem.getKey());
                shoppingEditor.putString(changedItem.getKey(), changedItem.toString());
            }

        }
        shoppingEditor.commit();
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
                            shoppingEditor.putString(item.getKey(), item.toString());
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
                        Product newProduct = new Product(name, Integer.toString(sindex), 1);
                        shoppingList.add(newProduct);
                        shoppingAdapter.notifyDataSetChanged();
                        shoppingEditor.remove("index");
                        shoppingEditor.putString("index", Integer.toString(sindex));
                        shoppingEditor.putString(Integer.toString(sindex), name + "|" + newProduct.getExpiryDate() + "|1|" + newProduct.getCode());
                        shoppingEditor.commit();
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                    }
                })
                .show();
    }
    public Product parseSharedPreferences(String string, String key)
    {  String[] strings = string.split("\\|");
        // Namn, date, key, amount, code
        return new Product(strings[0],strings[1],key,Integer.parseInt(strings[2]),strings[3]);

    }
}
