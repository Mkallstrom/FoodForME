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


public class MainActivity extends ActionBarActivity {

    private Context context;

    private SharedPreferences account;

    private String username;
    private String password;

    private ProgressDialog progressDialog;

    private AccountDB accountDB;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        context=this;
        accountDB = (AccountDB) getApplicationContext();
        account = getSharedPreferences("account", MODE_PRIVATE);

        boolean usesAccount = account.getBoolean("active", false);
        if(usesAccount)
        {
            username = account.getString("user", "No user was found!");
            password = account.getString("password", "No password found!");
            Log.d("Main", "Uses account is true.");
            connect();
        }
        else
        {
            Log.d("Main", "Uses account is false.");
            accountDB.loadSharedPreferences();
        }
    }
    private void connect(){
        accountDB.connectToDatabase(username, password);
        int connecting = 0;
        while(connecting == 0){
            connecting = accountDB.getConnection();
        }
        if(connecting == -1){
            new AlertDialog.Builder(this)

                    .setTitle("Connection failed")
                    .setMessage("Try again?")
                    .setPositiveButton("Try again", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            connect();
                        }
                    })
                    .setNegativeButton("Use local data", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            accountDB.resetConnection();
                            disconnect();
                        }
                    })
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setCancelable(false)
                    .show();
        }
        else {
            setTitle(username);
            loadProducts();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    public void toInventory(View view){
        accountDB.checkLocal();
        Intent intent = new Intent(this, InventoryActivity.class);
        startActivity(intent);
    }

    public void toList(View view){
        accountDB.checkLocal();
        Intent intent = new Intent(this, ShoppingListActivity.class);
        startActivity(intent);
    }

    public void toRequirement(View view){
        accountDB.checkLocal();
        Intent intent = new Intent(this, RequirementActivity.class);
        startActivity(intent);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item){

        switch(item.getItemId()){
            case R.id.account_connect:
                this.connectAccount();
                return true;
            case R.id.create_account:
                createAccount();
                return true;
            case R.id.disconnect:
                if(!accountDB.isLocal()) {
                    disconnect();
                }
                return true;
            case R.id.copy_to_local:
                Log.d("options", String.valueOf(accountDB.isLocal()));
                if(!accountDB.isLocal()) {
                    copyToLocal();
                }
                return true;
            default:
                return false;

}
}
    public void copyToLocal(){
        progressDialog = new ProgressDialog(MainActivity.this);
        progressDialog.setTitle(accountDB.getAccountUsername());
        progressDialog.setMessage("Copying products...");
        progressDialog.setProgressStyle(progressDialog.STYLE_HORIZONTAL);
        progressDialog.setProgress(0);
        progressDialog.setMax(100);
        progressDialog.setCancelable(false);
        progressDialog.show();

        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                accountDB.copyToLocal();
                int loading = 0;
                while(loading < 100){
                    loading = accountDB.getSavingProgress();
                    progressDialog.setProgress(loading);
                }
                progressDialog.dismiss();
            }
        };

        new Thread(runnable).start();
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
        if(!Network.isNetworkAvailable(this)){
            accountDB.resetConnection();
            InfoDialog id = new InfoDialog("No connection.", this);
            id.message();
           return;
        }
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
                        saveAccount(loginName,loginPassword);

                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                    }
                })
                .show();
    }
    private void saveAccount(String name, String password){

        final String username = name, pass = password;

        progressDialog = new ProgressDialog(MainActivity.this);
        progressDialog.setTitle(name);
        progressDialog.setMessage("Creating account...");
        progressDialog.setProgressStyle(progressDialog.STYLE_HORIZONTAL);
        progressDialog.setProgress(0);
        progressDialog.setMax(accountDB.getTotalProducts());
        progressDialog.setCancelable(false);
        progressDialog.show();

        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                accountDB.saveAccount(username, pass);
                int loading = 0;
                while(loading < accountDB.getTotalProducts() && loading!= -1){
                    loading = accountDB.getSavingProgress();
                    progressDialog.setProgress(loading);
                }
                progressDialog.dismiss();
            }
        };

        new Thread(runnable).start();
        this.progressDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {

            @Override
            public void onDismiss(DialogInterface arg0) {
                if(progressDialog.getProgress()!=-1) {
                    InfoDialog id = new InfoDialog("Account successfully created.", context);
                    setTitle(username);
                    id.message();
                }
                else
                {
                    InfoDialog id = new InfoDialog("Account could not be created.", context);
                    id.message();
                }
            }
        });

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







    /**
     * Connect to an existing account! If the user and password matches.
     */
    public void connectAccount(){
        if(!Network.isNetworkAvailable(this)){
            accountDB.resetConnection();
            InfoDialog id = new InfoDialog("No connection.", this);
            id.message();
            return;
        }
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
        progressDialog.setTitle(accountDB.getAccountUsername());
        progressDialog.setMessage("Loading products...");
        progressDialog.setProgressStyle(progressDialog.STYLE_HORIZONTAL);
        progressDialog.setProgress(0);
        progressDialog.setMax(3);
        progressDialog.setCancelable(false);
        progressDialog.show();

        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                accountDB.loadProducts();
                int loading = 0;
                while(loading < 3){
                    loading = accountDB.getLoadingProgress();
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
            accountDB.existsAccountInDatabase(user, pass);
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
