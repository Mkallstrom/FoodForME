package com.example.martin.foodforme;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.text.InputType;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;


public class InventoryActivity extends ActionBarActivity {

    private Scanner scanner;

    ArrayAdapter inventoryAdapter;
    ArrayList<Product> shoppingList, requiredList, inventoryList;

    SharedPreferences inventorySP, requiredSP, shoppingSP;
    SharedPreferences.Editor inventoryEditor, requiredEditor, shoppingEditor;

    int index = 0, rindex = 0;

    ListView listView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        this.scanner = new Scanner(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inventory);
        Context context = this;
        setTitle("Inventory");

        inventoryList = new ArrayList<>();
        shoppingList = new ArrayList<>();
        requiredList = new ArrayList<>();
        inventoryAdapter = new ListArrayAdapter(context,R.layout.productlayout, inventoryList);

        inventorySP = getSharedPreferences("inventorySP", 0);
        requiredSP = getSharedPreferences("requiredSP", 0);
        shoppingSP = getSharedPreferences("shoppingSP", 0);
        inventoryEditor = inventorySP.edit();
        requiredEditor = requiredSP.edit();
        shoppingEditor = shoppingSP.edit();

        listView = (ListView) findViewById(R.id.inventoryListView);
        listView.setAdapter(inventoryAdapter);
        registerForContextMenu(listView);

        if(!inventorySP.contains("index"))                            //If file does not contain the index, add it starting from 0.
        {
            inventoryEditor.putString("index", "0");
            inventoryEditor.commit();
        }
        if(!requiredSP.contains("index"))                            //If file does not contain the index, add it starting from 0.
        {
            requiredEditor.putString("index", "0");
            requiredEditor.commit();
        }
        if(!shoppingSP.contains("index"))                            //If file does not contain the index, add it starting from 0.
        {
            shoppingEditor.putString("index", "0");
            shoppingEditor.commit();
        }
        index = Integer.parseInt(inventorySP.getString("index",""));  //Get and save the index.
        rindex = Integer.parseInt(requiredSP.getString("index",""));

        Map<String,?> keys = inventorySP.getAll();                    //Get the inventoryList into the product listview.
        for(Map.Entry<String,?> entry : keys.entrySet()){
            if(!entry.getKey().equals("index"))
            {
                inventoryList.add(parseSharedPreferences(entry.getValue().toString(), entry.getKey()));
            }
        }

        setEmptyText();

        keys = shoppingSP.getAll();                    //Get the inventoryList into the product listview.
        for(Map.Entry<String,?> entry : keys.entrySet()){
            if(!entry.getKey().equals("index"))
            {
                shoppingList.add(parseSharedPreferences(entry.getValue().toString(), entry.getKey()));
            }
        }
        keys = requiredSP.getAll();                    //Get the inventoryList into the product listview.
        for(Map.Entry<String,?> entry : keys.entrySet()){
            if(!entry.getKey().equals("index"))
            {
                requiredList.add(parseSharedPreferences(entry.getValue().toString(), entry.getKey()));
            }
        }


        Collections.sort(inventoryList);
        inventoryAdapter.notifyDataSetChanged();

        listView.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {

                openContextMenu(view);

            }
        });

        startService(new Intent(this, NotifyService.class));

    }


    /**
     * Check if inventorySP is empty and show text for it
     * or hide text if not empty.
     */
    private void setEmptyText() {
        TextView tv = (TextView)findViewById(R.id.emptyText);
        if(inventoryList.isEmpty()){
            tv.setVisibility(View.VISIBLE);
        }
        else{
            tv.setVisibility(View.INVISIBLE);
        }
    }

    public void onCreateContextMenu(ContextMenu menu, View v,ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo)menuInfo;
        menu.setHeaderTitle("Edit " + inventoryList.get(info.position).getName());
        menu.add(0, 1, 0, "Remove");
        menu.add(0, 2, 0, "Edit Amount");
        menu.add(0, 3, 0, "Add to requirements");
    }

    public boolean onContextItemSelected(MenuItem item) {
        final AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        int itemID = item.getItemId();

        if(itemID == 1){
            Product product = inventoryList.get(info.position);
            inventoryEditor.remove(product.getKey());
            inventoryEditor.commit();
            inventoryList.remove(product);
            inventoryAdapter.notifyDataSetChanged();
            setEmptyText();

        } else if(itemID == 2) {
            final EditText txtUrl = new EditText(this);
            txtUrl.setInputType(InputType.TYPE_CLASS_NUMBER);

            new AlertDialog.Builder(this)
                    .setTitle(inventoryList.get(info.position).getName())
                    .setView(txtUrl)
                    .setPositiveButton("Apply", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            int amount = Integer.parseInt(txtUrl.getText().toString());
                            Product item = inventoryList.get(info.position);
                            inventoryList.remove(item);
                            item.setAmount(Integer.toString(amount));
                            inventoryList.add(info.position, item);
                            inventoryAdapter.notifyDataSetChanged();
                            inventoryEditor.remove(item.getKey());
                            inventoryEditor.putString(item.getKey(), item.toString());
                            inventoryEditor.commit();
                        }
                    })
                    .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                        }
                    })
                    .show();
        }
        else if(itemID == 3) {

            Product requiredItem = null;
            if(!requiredList.isEmpty())
            {
                for (Product p : requiredList)
                {
                    if (inventoryList.get(info.position).getCode().equals(p.getCode()))
                    {
                        requiredItem = p;
                    }
                }
            }

            if(requiredItem!=null)
            {
                requiredItem.setAmount(Integer.toString(Integer.parseInt(requiredItem.getAmount())+1));
                requiredEditor.remove(requiredItem.getKey());
                requiredEditor.putString(requiredItem.getKey(), requiredItem.toString());
                requiredEditor.commit();
            }
            else
            {
                rindex++;
                requiredEditor.remove("index");
                requiredEditor.putString("index",Integer.toString(rindex));
                Product reqProduct = inventoryList.get(info.position);
                requiredList.add(new Product(reqProduct.getName(), reqProduct.getExpiryDate(), reqProduct.getKey(), 1, reqProduct.getCode(), false));
                requiredEditor.putString(Integer.toString(rindex), inventoryList.get(info.position).toString());
                requiredEditor.commit();
            }
        }
        else
        {
            return false;
        }
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
        scanner.scan();
    }

    /*
    * Results from other activities needs to be handled here (ex. scanner)
    */

    protected void addProduct(String name, String date, int amount, String code, boolean expires)
    {
        Product product = new Product(name, date, Integer.toString(index), amount, code, expires);
        // Namn, date, key, amount, code

        inventoryEditor.putString(Integer.toString(index), product.toString());
        inventoryEditor.commit();
        inventoryList.add(product);
        Collections.sort(inventoryList);
        inventoryAdapter.notifyDataSetChanged();
        Product boughtItem = null;
        if(!shoppingList.isEmpty())
        {
            for (Product p : shoppingList)
            {
                if (code.equals(p.getCode()))
                {
                    boughtItem = p;
                }
            }
        }

        if(boughtItem!=null)
        {
            boughtItem.setAmount(Integer.toString(Integer.parseInt(boughtItem.getAmount())-1));
            shoppingEditor.remove(boughtItem.getKey());
            if(Integer.parseInt(boughtItem.getAmount()) > 0)
            {
                shoppingEditor.putString(boughtItem.getKey(), boughtItem.toString());
            }
            shoppingEditor.commit();
        }
        setEmptyText();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 100) {                                   // Result is from addproduct
            if(resultCode == RESULT_OK) {
                String newProduct = data.getStringExtra("product"); // Gets the name of the product
                String newProductExpDate = data.getStringExtra("expDate"); // Gets the expiration date
                String newProductAmount = data.getStringExtra("amount");
                String newCode = data.getStringExtra("code");
                boolean expires = data.getBooleanExtra("expires", true);

                index+=1;
                inventoryEditor.remove("index");
                inventoryEditor.putString("index",Integer.toString(index));
                inventoryEditor.commit();
                addProduct(newProduct, newProductExpDate, Integer.parseInt(newProductAmount), newCode, expires);

            } else if (resultCode == RESULT_CANCELED) {             // addProduct was canceled
                Toast.makeText(this, "The product was not added.", Toast.LENGTH_SHORT).show();
            }
        } else {                                                    // Result is from scanning
               scanner.scannerResult(requestCode, resultCode, data);
            }

    }
    public Product parseSharedPreferences(String string, String key) {
        String[] strings = string.split("\\|"); // The double backslash is needed for some characters
        // Namn, date, key, amount, code, expires
        return new Product(strings[0], strings[1], key, Integer.parseInt(strings[2]), strings[3], Boolean.valueOf(strings[4]));
    }

}
