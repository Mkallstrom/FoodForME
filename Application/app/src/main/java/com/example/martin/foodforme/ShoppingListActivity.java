package com.example.martin.foodforme;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
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
import java.util.Collections;


public class ShoppingListActivity extends ActionBarActivity {

    private Scanner scanner;
    AccountDB accountDB;
    ArrayList<Product> shoppingList, requiredList, inventoryList;
    ArrayAdapter shoppingAdapter;

    ListView shoppingListView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        this.scanner = new Scanner(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_shopping_list);
        Context context = this;
        accountDB = (AccountDB) getApplicationContext();
        setTitle(accountDB.getUsername() + "'s Shopping List");

        inventoryList = accountDB.returnInventory();
        shoppingList = accountDB.returnShoppingList();
        requiredList = accountDB.returnRequirements();

        shoppingAdapter = new ShoppingArrayAdapter(context,R.layout.shoppinglayout,shoppingList);
        accountDB.setAdapter("shoppinglist", shoppingAdapter);

        shoppingListView = (ListView) findViewById(R.id.shoppinglistView);

        shoppingListView.setAdapter(shoppingAdapter);

        registerForContextMenu(shoppingListView);

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
                            accountDB.removeProduct(item, "shoppinglist");
                            item.setAmount(Integer.toString(amount));
                            accountDB.addProduct(item.getName(),item.getExpiryDate(),Integer.parseInt(item.getAmount()),item.getCode(),item.expires(),"shoppinglist");
                            shoppingAdapter.notifyDataSetChanged();
                        }
                    })
                    .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                        }
                    })
                    .show();

        } else if(itemID == 2) {
            Product shoppingItem = shoppingList.get(info.position);
            accountDB.removeProduct(shoppingItem,"shoppinglist");
            shoppingAdapter.notifyDataSetChanged();



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
                        Product newProduct = new Product(name, "0", 1);
                        accountDB.addProduct(newProduct.getName(),newProduct.getExpiryDate(),Integer.parseInt(newProduct.getAmount()),newProduct.getCode(),newProduct.expires(),"shoppinglist");
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                    }
                })
                .show();
    }

    /*
  * Initiates the barcode scanner via intent
   */
    public void scanBarcode(View view) {
        scanner.scan();
    }

    protected void addProduct(String name, String date, int amount, String code, boolean expires)
    {
        // Namn, date, key, amount, code
        accountDB.addProduct(name, date, amount, code, expires, "inventory");

        Collections.sort(inventoryList);
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
            accountDB.removeProduct(boughtItem, "shoppinglist");
            if(Integer.parseInt(boughtItem.getAmount()) > 0)
            {
                accountDB.addProduct(boughtItem.getName(), boughtItem.getExpiryDate(), Integer.parseInt(boughtItem.getAmount()), boughtItem.getCode(), boughtItem.expires(), "shoppinglist");
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 100) {                                   // Result is from addproduct
            if(resultCode == RESULT_OK) {
                String newProduct = data.getStringExtra("product"); // Gets the name of the product
                String newProductExpDate = data.getStringExtra("expDate"); // Gets the expiration date
                String newProductAmount = data.getStringExtra("amount");
                String newCode = data.getStringExtra("code");
                boolean expires = data.getBooleanExtra("expires",true);

                addProduct(newProduct, newProductExpDate, Integer.parseInt(newProductAmount), newCode, expires);

            } else if (resultCode == RESULT_CANCELED) {             // addProduct was canceled
                Toast.makeText(this, "The product was not added.", Toast.LENGTH_SHORT).show();
            }
        } else {                                                    // Result is from scanning
            scanner.scannerResult(requestCode, resultCode, data);
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
                    accountDB.addProduct(requiredProduct.getName(), requiredProduct.getExpiryDate(), Integer.parseInt(requiredProduct.getAmount())-inventoryAmount, requiredProduct.getCode(),requiredProduct.expires(), "shoppinglist");
                    Toast.makeText(this,"Added " + Integer.toString(Integer.parseInt(requiredProduct.getAmount())-inventoryAmount) + " of " + requiredProduct.getName() + " to shopping list.",Toast.LENGTH_SHORT).show();
                }
                else if (changedItem != null)
                {
                    Toast.makeText(this,"Added " + Integer.toString(Integer.parseInt(requiredProduct.getAmount()) - Integer.parseInt(changedItem.getAmount())) + " of " + changedItem.getName() + " to shopping list.",Toast.LENGTH_SHORT).show();
                    changedItem.setAmount(Integer.toString(Integer.parseInt(requiredProduct.getAmount()) - inventoryAmount));
                    accountDB.removeProduct(changedItem,"shoppinglist");
                    accountDB.addProduct(changedItem.getName(),changedItem.getExpiryDate(),Integer.parseInt(changedItem.getAmount()),changedItem.getCode(),changedItem.expires(),"shoppinglist");
                }
            }

        }
        shoppingAdapter.notifyDataSetChanged();
    }
}
