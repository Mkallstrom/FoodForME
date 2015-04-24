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
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Map;


public class ShoppingListActivity extends ActionBarActivity {

    private Scanner scanner;
    ArrayList<Product> shoppingList = new ArrayList<>(), requiredList = new ArrayList<>(), inventoryList = new ArrayList<>();
    ArrayAdapter shoppingAdapter;
    SharedPreferences shoppingSP, requiredSP, inventorySP, localBarcodes;
    SharedPreferences.Editor shoppingEditor, inventoryEditor;
    ListView shoppingListView;
    int sindex = 0;
    int index = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        this.scanner = new Scanner(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_shopping_list);
        Context context = this;
        setTitle("Shopping List");

        shoppingAdapter = new ShoppingArrayAdapter(context,R.layout.shoppinglayout,shoppingList);

        shoppingSP = getSharedPreferences("shoppingSP",0);
        requiredSP = getSharedPreferences("requiredSP",0);
        inventorySP = getSharedPreferences("inventorySP",0);
        localBarcodes = getSharedPreferences("localBarcodes", 0);


        shoppingEditor = shoppingSP.edit();
        inventoryEditor = inventorySP.edit();

        shoppingListView = (ListView) findViewById(R.id.shoppinglistView);

        shoppingListView.setAdapter(shoppingAdapter);

        registerForContextMenu(shoppingListView);

        setIndices();
        sindex = Integer.parseInt(shoppingSP.getString("index",""));
        index = Integer.parseInt(inventorySP.getString("index",""));

        fillLists();

        checkRequirements();

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
            menu.add(0, 1, 0, "Edit Amount");
            menu.add(0, 2, 0, "Delete");
        }

    }

    public boolean onContextItemSelected(MenuItem item) {
        final AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        int itemID = item.getItemId();

        if(itemID == 1){
            final EditText txtUrl = new EditText(this);
            txtUrl.setInputType(InputType.TYPE_CLASS_NUMBER);

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
                        shoppingEditor.putString(Integer.toString(sindex), name + "|" + newProduct.getExpiryDate() + "|1|" + newProduct.getCode() + "|" + newProduct.expires());
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
        // Namn, date, key, amount, code, expires
        return new Product(strings[0],strings[1],key,Integer.parseInt(strings[2]),strings[3], Boolean.parseBoolean(strings[4]));

    }

    /*
  * Initiates the barcode scanner via intent
   */
    public void scanBarcode(View view) {
        scanner.scan();
    }

    protected void addProduct(String name, String date, String code, boolean expires)
    {
        Product product = new Product(name, date, Integer.toString(index), 1, code, expires);
        // Namn, date, key, amount, code
        inventoryEditor.putString(Integer.toString(index), product.toString());
        inventoryEditor.commit();
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
            else
            {
                shoppingList.remove(boughtItem);
            }
            shoppingEditor.commit();
            shoppingAdapter.notifyDataSetChanged();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 100) {                                   // Result is from addproduct
            if(resultCode == RESULT_OK) {
                String newProduct = data.getStringExtra("product"); // Gets the name of the product
                String newProductExpDate = data.getStringExtra("expDate"); // Gets the expiration date
                String newCode = data.getStringExtra("code");
                boolean expires = data.getBooleanExtra("expires",true);

                index+=1;
                inventoryEditor.remove("index");
                inventoryEditor.putString("index",Integer.toString(index));
                inventoryEditor.commit();
                addProduct(newProduct, newProductExpDate, newCode, expires);

            } else if (resultCode == RESULT_CANCELED) {             // addProduct was canceled
                Toast.makeText(this, "The product was not added.", Toast.LENGTH_SHORT).show();
            }
        } else {                                                    // Result is from scanning
            scanner.scannerResult(requestCode, resultCode, data);
        }

    }
    private void setIndices()
    {
        if(!inventorySP.contains("index"))                            //If file does not contain the index, add it starting from 0.
        {
            inventoryEditor.putString("index", "0");
            inventoryEditor.commit();
        }
        if(!shoppingSP.contains("index"))                            //If file does not contain the index, add it starting from 0.
        {
            shoppingEditor.putString("index", "0");
            shoppingEditor.commit();
        }
    }
    private void fillLists()
    {
        Map<String,?> keys = inventorySP.getAll();                    //Get the inventoryList into the product listview.
        for(Map.Entry<String,?> entry : keys.entrySet()){
            if(!entry.getKey().equals("index"))
            {
                inventoryList.add(parseSharedPreferences(entry.getValue().toString(), entry.getKey()));
            }
        }
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
    }
    private void checkRequirements()
    {
        ArrayList shoppingCodes = new ArrayList();
        for(Product p : shoppingList) { shoppingCodes.add(p.getCode()); }
        for(Product requiredProduct : requiredList)
        {
            String requiredCode = requiredProduct.getCode();
            Product changedItem = null;
            int inventoryAmount = 0, shoppinglistAmount = 0;

            for(Product inventoryProduct : inventoryList)
            {
                if(inventoryProduct.getCode().equals(requiredCode))
                {
                    inventoryAmount += Integer.parseInt(inventoryProduct.getAmount());
                }
            }
            for(Product shoppinglistProduct : shoppingList)
            {
                if(shoppinglistProduct.getCode().equals(requiredCode))
                {
                    shoppinglistAmount += Integer.parseInt(shoppinglistProduct.getAmount());
                    changedItem = shoppinglistProduct;
                }
            }

            if(Integer.parseInt(requiredProduct.getAmount()) > (shoppinglistAmount+inventoryAmount)) // Increase amount
            {
                if(!shoppingCodes.contains(requiredCode))
                {
                    sindex++;
                    shoppingEditor.remove("index");
                    shoppingEditor.putString("index", Integer.toString(sindex));
                    Product newProduct = new Product(requiredProduct.getName(), requiredProduct.getExpiryDate(), requiredProduct.getKey(), Integer.parseInt(requiredProduct.getAmount())-inventoryAmount, requiredProduct.getCode(), false);
                    shoppingList.add(newProduct);
                    Toast.makeText(this,"Added " + newProduct.getAmount() + " of " + newProduct.getName() + " to shopping list.",Toast.LENGTH_SHORT).show();
                    shoppingEditor.putString(Integer.toString(sindex), newProduct.toString());
                }
                else if (changedItem != null)
                {
                    Toast.makeText(this,"Added " + Integer.toString(Integer.parseInt(requiredProduct.getAmount()) - Integer.parseInt(changedItem.getAmount())) + " of " + changedItem.getName() + " to shopping list.",Toast.LENGTH_SHORT).show();
                    changedItem.setAmount(Integer.toString(Integer.parseInt(requiredProduct.getAmount()) - inventoryAmount));
                    shoppingEditor.remove(changedItem.getKey());
                    shoppingEditor.putString(changedItem.getKey(), changedItem.toString());
                }
            }

        }
        shoppingEditor.commit();
        shoppingAdapter.notifyDataSetChanged();
    }
}
