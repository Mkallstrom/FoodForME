package com.example.martin.foodforme;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
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

import java.util.ArrayList;


public class RequirementActivity extends ActionBarActivity {

    ArrayList<Product> requiredList;
    ListView requiredListView;
    ArrayAdapter requiredAdapter;
    AccountDB accountDB;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_requirement);
        Context context = this;
        setTitle("Requirements");
        accountDB = (AccountDB) getApplicationContext();

        requiredList = accountDB.returnRequirements();

        requiredAdapter = new ShoppingArrayAdapter(context,R.layout.shoppinglayout,requiredList);

        requiredListView = (ListView) findViewById(R.id.requirementlistView);
        requiredListView.setAdapter(requiredAdapter);
        accountDB.setAdapter("requirements", requiredAdapter);
        registerForContextMenu(requiredListView);

        requiredListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

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
        if(v == findViewById(R.id.requirementlistView))
        {
            menu.setHeaderTitle("Edit " + requiredList.get(info.position).getName());
            menu.add(0, 1, 0, "Edit Amount");
            menu.add(0, 2, 0, "Remove");
        }

    }

    public boolean onContextItemSelected(MenuItem item) {
        final AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        int itemID = item.getItemId();

        if(itemID == 1){
            final EditText txtUrl = new EditText(this);
            txtUrl.setInputType(InputType.TYPE_CLASS_NUMBER);

            new AlertDialog.Builder(this)
                    .setTitle(requiredList.get(info.position).getName())
                    .setView(txtUrl)
                    .setPositiveButton("Apply", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            int amount = Integer.parseInt(txtUrl.getText().toString());
                            Product item = requiredList.get(info.position);
                            item.setAmount(Integer.toString(amount));
                            accountDB.removeProduct(item, "requirements");
                            accountDB.addProduct(item.getName(), item.getExpiryDate(), Integer.parseInt(item.getAmount()), item.getCode(), item.expires(), "requirements");
                        }
                    })
                    .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                        }
                    })
                    .show();

        } else if(itemID == 2) {
            Product shoppingItem = requiredList.get(info.position);
            accountDB.removeProduct(shoppingItem, "requirements");


        } else {return false;}
        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_must_have_list, menu);
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
    private Product parseSharedPreferences(String string, String key)
    {  String[] strings = string.split("\\|");
        // Namn, date, key, amount, code
        return new Product(strings[0],strings[1],key,Integer.parseInt(strings[2]),strings[3], Boolean.parseBoolean(strings[4]));

    }

}
