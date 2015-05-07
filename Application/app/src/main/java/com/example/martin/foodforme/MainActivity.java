package com.example.martin.foodforme;

import android.app.AlertDialog;
import android.app.ProgressDialog;
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
    private static final String url_create_account = ip + "create_account.php"; //Create account
    JSONParser jsonParser = new JSONParser();
    //JSON names
    private static final String TAG_SUCCESS = "success";
    private static final String USERNAME = "name";
    private static final String PASSWORD = "password";

    private boolean firstRun = true; //False if first time true if runned before

    ProgressDialog progressDialog;

    AccountDB accountDB;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        context=this;
        accountDB = (AccountDB) getApplicationContext();

        SharedPreferences account = getSharedPreferences("account",MODE_PRIVATE);
        boolean usesAccount = account.getBoolean("active", false);
        if(usesAccount)
        {
            Log.d("Main", "Uses account is true.");
            String username = account.getString("user", "No user was found!");
            String password = account.getString("password", "No password found!");
            accountDB.setDetails(username, password);
            setTitle(username);
            if(firstRun){
                loadProducts();
                firstRun = false;
            }


        }
        else
        {
            Log.d("Main", "Uses account is false.");
            accountDB.loadSharedPreferences();
        }
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
            case R.id.account_connect:
                //new connectToAccount().execute();
                this.connectAccount();
                return true;
            case R.id.create_account:
                createAccount();
                return true;
            case R.id.disconnect:
                disconnect();
                return true;
            case R.id.copy_to_local:
                copyToLocal();
                return true;
            default:
                return false;

}
}
    public void copyToLocal(){
        accountDB.copyToLocal();
    }
    public void disconnect(){
        SharedPreferences account = getSharedPreferences("account",MODE_PRIVATE);
        SharedPreferences.Editor accountEditor = account.edit();
        accountEditor.clear();
        accountEditor.commit();
        setTitle("FoodForME");
        accountDB.disconnect();
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
                        createDialog(loginName);
                        int savingAccount = 0;
                        while (savingAccount == 0) {
                            savingAccount = sa.getCreatedAcc();
                        }
                        progressDialog.dismiss();
                        if (savingAccount == 1) {
                            //if successful
                            InfoDialog info = new InfoDialog("A new inventory was successfully created.", context);
                            info.message();
                            accountDB.setDetails(sa.getName(),sa.getPassword());
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

    private void createDialog(String name){
        progressDialog = new ProgressDialog(MainActivity.this);
        progressDialog.setTitle(name);
        progressDialog.setMessage("Creating account...");
        progressDialog.show();
    }

    /**
     * Store the account info on phone
     * @param username - the username for the database
     * @param password - password for username to login with
     */
    public void storeAccountOnPhone(String username, String password){
        SharedPreferences account = getSharedPreferences("account",MODE_PRIVATE);
        SharedPreferences.Editor accountEditor = account.edit();
        accountEditor.putBoolean("active", true);
        accountEditor.putString("user", username);
        accountEditor.putString("password", password);
        accountEditor.commit();
    }



    public class SaveAccount extends AsyncTask<String, String, String> {
        private int createdAcc = 0;
        private String userName;
        private String password;
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
            this.userName = username;
            this.password = password;
            params.add(new BasicNameValuePair(USERNAME, username));
            params.add(new BasicNameValuePair(PASSWORD, password));
            params.add(new BasicNameValuePair("indexInventory",Integer.toString(accountDB.getIndexInventory())));
            params.add(new BasicNameValuePair("indexShoppingList",Integer.toString(accountDB.getIndexShoppingList())));
            params.add(new BasicNameValuePair("indexRequirements",Integer.toString(accountDB.getIndexRequirements())));

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
                    int oldConnection = accountDB.getConnection();
                    storeAccountOnPhone(username,password);
                    accountDB.connected(username,password);
                    if(oldConnection == 0) {
                        accountDB.storeProducts();
                    }
                    else {
                        accountDB.clearProducts();
                    }
                    // closing this screen
                    //finish();

                } else {
                    createdAcc = -1; //Account failed to be created
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }


        }

        public int getCreatedAcc(){
            return createdAcc;
        }
        public String getName() { return userName; }
        public String getPassword() { return password; }
    }



    /**
     * Connect to an existing account! If the user and password matches.
     */
    public void connectAccount(){
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

                .setTitle("Connect to account:")
                .setView(layout)
                .setPositiveButton("Connect", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        String loginName = username.getText().toString();
                        String loginPassword = password.getText().toString();
                        ConnectToAccount ca = new ConnectToAccount();
                        ca.execute(new String[]{loginName, loginPassword});
                        ca.resetConnect();
                        int connectionState = ca.getConnect();
                        while (connectionState == 0){
                            connectionState = ca.getConnect();
                        }
                        if (ca.getConnect() == 1) {
                            storeAccountOnPhone(loginName,loginPassword);
                            loadProducts();
                            InfoDialog info = new InfoDialog("You are now connected to " + loginName + ".", context);
                            setTitle(loginName);
                            info.message();
                            return;
                        }
                        else {
                            InfoDialog info = new InfoDialog("Could not connect to account. " +
                                    "Control that the username and password are correct.", context);
                            info.message();
                            return;
                        }

                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                    }
                })
                .show();
    }

    private void loadProducts(){
        progressDialog = new ProgressDialog(MainActivity.this);
        progressDialog.setTitle(accountDB.getUsername());
        progressDialog.setMessage("Loading products...");
        progressDialog.setProgressStyle(progressDialog.STYLE_HORIZONTAL);
        progressDialog.setProgress(0);
        progressDialog.setMax(3);
        progressDialog.show();

        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                accountDB.getProducts();
                int loading = 0;
                while(loading < 3){
                    loading = accountDB.getLoadingProducts();
                    progressDialog.setProgress(loading);
                }
                progressDialog.dismiss();
            }
        };

        new Thread(runnable).start();
    }

    //___________*Connect to account*_________//
    public class ConnectToAccount extends AsyncTask<String, String, String> {
        private int connect = 0; //0 waiting no result. 1 successfully connected. 0 Failed to connect
        /**
         * Before starting background thread
         */
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected String doInBackground(String... args) {
            existInDB(args[0], args[1]);
            return null;
        }

        public boolean existInDB(String user, String pass){
            accountDB.resetConnection();
            accountDB.existAccountInDatabase(user, pass);
            int existsInDBConnection = 0;
            while(existsInDBConnection == 0) {
                existsInDBConnection = accountDB.getConnection();
                Log.d("existsInDB", "Waiting for connection");
            }
            if(existsInDBConnection == 1) {
                //success to locate account
                accountDB.switchAccountOnPhone(user, pass);
                accountDB.connected(user, pass);
                connect = 1;
                return true;
            }
            //failed to connect and update
            connect = -1;
            return false;
        }

        public int getConnect(){
            return connect;
        }


        /**
         * Reset it to state 0. (loading state)
         */
        public void resetConnect(){
            connect = 0;
        }


    }

}
