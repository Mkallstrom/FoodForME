package com.example.martin.foodforme;

import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.text.InputType;
import android.util.Log;
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
import java.util.Calendar;
import java.util.Collections;


public class InventoryActivity extends ActionBarActivity {

    private ProgressDialog progressDialog;
    private Scanner scanner;
    private AccountDB accountDB;

    private ArrayAdapter inventoryAdapter;
    private ArrayList<Product> shoppingList,
            requirementList,
            inventoryList;

    private static final int REMOVE = 1,
                        EDIT = 2,
                        REQUIREMENT = 3;

    private ListView listView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        this.scanner = new Scanner(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inventory);
        Context context = this;
        accountDB = (AccountDB) getApplicationContext();
        if(!accountDB.isLocal())setTitle(accountDB.getAccountUsername() + "'s Inventory");
        else setTitle("Inventory");

        inventoryList = accountDB.returnInventory();
        shoppingList = accountDB.returnShoppingList();
        requirementList = accountDB.returnRequirements();

        inventoryAdapter = new InventoryArrayAdapter(context,R.layout.productlayout, inventoryList, accountDB);
        accountDB.setAdapter("inventory", inventoryAdapter);

        listView = (ListView) findViewById(R.id.inventoryListView);
        listView.setAdapter(inventoryAdapter);
        registerForContextMenu(listView);
        listView.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {

                openContextMenu(view);

            }
        });

        Collections.sort(inventoryList);
        inventoryAdapter.notifyDataSetChanged();
        setEmptyText();
        setAlarm();

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

        if(itemID == REMOVE)
        {
            Product product = inventoryList.get(info.position);
            accountDB.deleteProduct(product, "inventory");
            inventoryAdapter.notifyDataSetChanged();
            setEmptyText();

        }
        else if(itemID == EDIT)
        {
            final EditText txtUrl = new EditText(this);
            txtUrl.setInputType(InputType.TYPE_CLASS_NUMBER);

            new AlertDialog.Builder(this)
                    .setTitle(inventoryList.get(info.position).getName())
                    .setView(txtUrl)
                    .setPositiveButton("Apply", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            int amount;
                            if(txtUrl.getText().toString().length() > 9 ) {
                                amount = 999999999;
                            }
                            else {
                                amount = Integer.parseInt(txtUrl.getText().toString());
                            }
                            Product item = inventoryList.get(info.position);
                            item.setAmount(Integer.toString(amount));
                            accountDB.deleteProduct(item, "inventory");
                            accountDB.addProduct(item.getName(), item.getExpiryDate(), Integer.parseInt(item.getAmount()), item.getCode(), item.expires(), "inventory");
                            inventoryAdapter.notifyDataSetChanged();
                            Collections.sort(inventoryList);
                        }
                    })
                    .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                        }
                    })
                    .show();
        }
        else if(itemID == REQUIREMENT)
        {

            Product requiredProduct = null;
            if(!requirementList.isEmpty())
            {
                for (Product p : requirementList)
                {
                    if(inventoryList.get(info.position).getCode().equals(accountDB.getNoBarcode()))
                    {
                        if (inventoryList.get(info.position).getName().equals(p.getName()))
                        {
                            requiredProduct = p;
                        }
                    }
                    else
                    {
                        if (inventoryList.get(info.position).getCode().equals(p.getCode()))
                        {
                            requiredProduct = p;
                        }
                    }
                }
            }

            if(requiredProduct!=null)
            {
                requiredProduct.setAmount(Integer.toString(Integer.parseInt(requiredProduct.getAmount())+1));
                accountDB.deleteProduct(requiredProduct, "requirements");
                accountDB.addProduct(requiredProduct.getName(), requiredProduct.getExpiryDate(), Integer.parseInt(requiredProduct.getAmount()), requiredProduct.getCode(), requiredProduct.expires(), "requirements");
            }
            else
            {
                Product reqProduct = inventoryList.get(info.position);
                accountDB.addProduct(reqProduct.getName(), reqProduct.getExpiryDate(), Integer.parseInt(reqProduct.getAmount()), reqProduct.getCode(), reqProduct.expires(), "requirements");
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
    public void scanBarcode(View view)
    {
        scanner.scan();
    }

    /*
    * Results from other activities needs to be handled here (ex. scanner)
    */

    private void addProduct(String name, String date, int amount, String code, boolean expires)
    {
        // Namn, date, key, amount, code
        accountDB.addProduct(name, date, amount, code, expires, "inventory");
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
            accountDB.deleteProduct(boughtItem, "shoppinglist");
            if(Integer.parseInt(boughtItem.getAmount()) > 0)
            {
                accountDB.addProduct(boughtItem.getName(),boughtItem.getExpiryDate(),Integer.parseInt(boughtItem.getAmount()),boughtItem.getCode(),boughtItem.expires(), "shoppinglist");
            }
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

                addProduct(newProduct, newProductExpDate, Integer.parseInt(newProductAmount), newCode, expires);

            } else if (resultCode == RESULT_CANCELED) {             // addProduct was canceled
                Toast.makeText(this, "The product was not added.", Toast.LENGTH_SHORT).show();
            }
        } else {                                                    // Result is from scanning
               scanner.scannerResult(requestCode, resultCode, data);
            }

    }

    private void setAlarm()
    {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.AM_PM, Calendar.PM);
        if(calendar.get(Calendar.HOUR)>=4){
            calendar.add(Calendar.DATE, 1);
        }
        calendar.set(Calendar.HOUR, 4);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND,0);
        Intent serviceIntent = new Intent(this, NotifyService.class);
        PendingIntent pi = PendingIntent.getService(this, 131313, serviceIntent,
                PendingIntent.FLAG_CANCEL_CURRENT);
        AlarmManager am = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        Long nextAlarm = calendar.getTimeInMillis();
        am.setRepeating(AlarmManager.RTC_WAKEUP, nextAlarm, 1000 * 60 * 60 * 24, pi);
        Log.v("Alarm", "Alarm set to " + calendar.toString() + " which is in " + Long.toString((nextAlarm - System.currentTimeMillis()) / (1000 * 60)) + " minutes.");
    }

    /**
     * Add the produt in list without haveing any barcode or in need
     * of useing the camera!
     */
    public void addProductNoBarcode(View view){
        Intent intent = new Intent(this, AddProductActivity.class);
        String message = accountDB.getNoBarcode();
        intent.putExtra("result", message);         //Send the result to the add product activity.
        this.startActivityForResult(intent, 100);

    }

    /**
     * Update all products in the list to the database.
     */
    public boolean refresh(MenuItem item){

        progressDialog = new ProgressDialog(InventoryActivity.this);
        progressDialog.setTitle(accountDB.getAccountUsername());
        progressDialog.setMessage("Refreshing...");
        progressDialog.setProgressStyle(progressDialog.STYLE_HORIZONTAL);
        progressDialog.setProgress(0);
        progressDialog.setMax(3);
        progressDialog.setCancelable(false);
        progressDialog.show();

        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                accountDB.refreshProducts();
                int loading = 0;
                while(loading < 3){
                    loading = accountDB.getLoadingProgress();
                    progressDialog.setProgress(loading);
                }
                progressDialog.dismiss();
            }
        };

        new Thread(runnable).start();
        this.progressDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {

            @Override
            public void onDismiss(DialogInterface arg0) {
                inventoryAdapter.notifyDataSetChanged();
            }
        });

        return true;
    }

}
