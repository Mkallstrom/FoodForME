package com.example.martin.foodforme;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.text.method.PasswordTransformationMethod;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;


public class MainActivity extends ActionBarActivity {

    private Context context;
    private static final String ip = "http://ffm.student.it.uu.se/cloud/"; // Ip address for database
    private static final String url_create_account = ip + "create_refrigerator.php"; //Create account
    JSONParser jsonParser = new JSONParser();
    //JSON names
    private static final String TAG_SUCCESS = "success";
    private static final String USERNAME = "name";
    private static final String PASSWORD = "password";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        context=this;
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    public void toInventory(View view){
        Intent intent = new Intent(this, InventoryActivity.class);
        startActivity(intent);
    }

    public void toList(View view){
        Intent intent = new Intent(this, ShoppingListActivity.class);
        startActivity(intent);
    }

    public void toRequirement(View view){
        Intent intent = new Intent(this, RequirementActivity.class);
        startActivity(intent);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item){

        switch(item.getItemId()){
            case R.id.refrigerator_connect:

                return true;
            case R.id.create_refrigerator:
                createAccount();
                return true;
            default:
                return false;

        }
    }


    public void createAccount(){
        final Context context = this;
        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);

        final EditText username = new EditText(context);
        username.setHint("Choose name:");
        layout.addView(username);

        final EditText password = new EditText(context);
        password.setHint("Choose password:");
        layout.addView(password);
        password.setTransformationMethod(new PasswordTransformationMethod());


        new AlertDialog.Builder(this)

                .setTitle("Create new inventory:")
                .setView(layout)
                .setPositiveButton("Create", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        String loginName = username.getText().toString();
                        String loginPassword = password.getText().toString();
                        SaveAccount sa = new SaveAccount();
                        sa.execute(new String[]{loginName, loginPassword});
                        while (sa.getCreatedAcc() == 0) {
                        };
                        if (sa.getCreatedAcc() == 1) {
                            //if successful
                            InfoDialog info = new InfoDialog("A new inventory was successfully created.", context);
                            info.message();
                        } else {
                            //if failed
                            InfoDialog info = new InfoDialog("Error, the database could not accept your request.", context);
                            info.message();
                        }

                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                    }
                })
                .show();
    }

    class SaveAccount extends AsyncTask<String, String, String> {
        private int createdAcc = 0;
        /**
         * Before starting background thread
         */
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected String doInBackground(String... args) {
            createAccountDB(args[0],args[1]);
            return null;

        }


        /**
         * Create account in database cloud.
         *
         * @param username - the chosen username for user.
         * @param password - the chosen password for the user.
         */
        private void createAccountDB(String username, String password) {
            // Building Parameters
            List<NameValuePair> params = new ArrayList<>();
            params.add(new BasicNameValuePair(USERNAME, username));
            params.add(new BasicNameValuePair(PASSWORD, password));

            // getting JSON Object
            // Note that create product url accepts POST method
            JSONObject json = jsonParser.makeHttpRequest(url_create_account,
                    "POST", params);
            // check log cat for response
            Log.d("Create Response", json.toString());

            // check for success tag
            try {
                int success = json.getInt(TAG_SUCCESS);

                if (success == 1) {
                    // successfully created product
                    createdAcc = 1; //Account been created

                    storeAccountOnPhone(username,password);

                    // closing this screen
                    //finish();

                } else {
                    createdAcc = -1; //Account failed to be created
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }


        }

        /**
         * Store the account info on phone
         * @param username - the username for the database
         * @param password - password for username to login with
         */
        public void storeAccountOnPhone(String username, String password){
            SharedPreferences account = getSharedPreferences("account",MODE_PRIVATE);
            SharedPreferences.Editor accountEditor = account.edit();
            accountEditor.putString("user", username);
            accountEditor.putString("password", password);
            accountEditor.commit();
        }

        public int getCreatedAcc(){
            return createdAcc;
        }
    }





}
