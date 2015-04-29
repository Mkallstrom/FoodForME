package com.example.martin.foodforme;

import android.app.Application;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.util.Log;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Andreas on 2015-04-24.
 */
public class AccountDB extends Application {
    //Attributes
    private String username;
    private String password;
    private ArrayList<Product> inventory = new ArrayList<>(), shoppingList = new ArrayList<>(), requirements = new ArrayList<>();
    private int loadInventory = 0; //0 not done, 1 successfully loaded, -1 failed to load
    private int loadShoppingList = 0;
    private int loadRequirements = 0;
    private int connection = 0; //1 successful, -1 failed, 0 nothing
    private static final String ip = "http://ffm.student.it.uu.se/cloud/"; // Ip-address for database
    private static final String url_get_products = ip + "get_products.php"; //Get all inventory from a user
    private static final String url_check_account = ip + "check_account.php"; //Check if password and user match and exist
    private static final String url_add_product = ip + "add_product.php"; //Check if password and user match and exist


    public void setDetails(String username, String password) {
        this.username = username;
        this.password = password;
        Log.d("AccountDB", "set details");
        new ConnectDB().execute();
    }
    public ArrayList<Product> returnInventory(){ return inventory; }
    public ArrayList<Product> returnShoppingList(){ return shoppingList; }
    public ArrayList<Product> returnRequirements(){ return requirements; }

    public void setInventory(ArrayList<Product> inventory){ this.inventory = inventory;}
    public void setShoppingList(ArrayList<Product> shoppingList){ this.shoppingList = shoppingList;}
    public void setrequirements(ArrayList<Product> requirements){ this.requirements = requirements;}

    public void storeProducts() {
        Log.d("AccountDB","attempting storeproducts with connection being: " + connection);
        if(connection==1) new SaveProducts().execute();
    }
    //Methods

    /**
     * Get the inventory for the user
     */
    public void getProducts(){
            new loadProducts().execute();
        }


    /**
     * Update the account info on phone.
     * @param user - the new value for username.
     * @param password - the new value for password.
     */
    public void switchAccountOnPhone(String user, String password){
        SharedPreferences account = getSharedPreferences("account",MODE_PRIVATE);
        SharedPreferences.Editor accountEditor = account.edit();
        accountEditor.putBoolean("active", true);
        accountEditor.putString("user", user);
        accountEditor.putString("password", password);
        accountEditor.commit();

        //TODO is it enough to update username and password on phone like now? or we need to
        //TODO update much more like products and remove the existing ones?
    }

    /**
     * Check the account information exist in DB and if that is valid.
     * @param username - username for account
     * @param password - password for account
     * @return true if there is an account with this username/password and match. False if not.
     */
    public boolean existAccountInDatabase(String username, String password){
        JSONParser jsonParser = new JSONParser();
        String TAG_SUCCESS = "success";
        String USERNAME = "name";
        String PASSWORD = "password";

        // Building Parameters
        List<NameValuePair> params = new ArrayList<>();
        params.add(new BasicNameValuePair(USERNAME, username));
        params.add(new BasicNameValuePair(PASSWORD, password));
        Log.d("checking account", username + password);

        // getting JSON Object
        // Note that create product url accepts POST method
        JSONObject json = jsonParser.makeHttpRequest(url_check_account, "GET", params);
        // check for success tag
        try {
            int success = json.getInt(TAG_SUCCESS);

            if (success == 1) {
                //successfully
                connection = 1; //Account was OK
                return true;
            } else {
                //failed
                connection = -1; //Account was not OK
                Log.d("AccountDB", "connection failed with " + username + ":" + password);
                return false;
            }
        } catch (JSONException e) {
            e.printStackTrace();
            return false;
        }

    }


    //__________*Inner class to connect and operate on database*_______________//
    private class ConnectDB extends AsyncTask<String, String, String> {

        //JSON
        private JSONParser jsonParser = new JSONParser();
        private static final String TAG_SUCCESS = "success";
        private static final String USERNAME = "name";
        private static final String PASSWORD = "password";

        //Methods
        @Override
        protected String doInBackground(String... params) {
            checkAccount();
            return null;
        }


        /**
         * Check if account exist with that name and password.
         * attribute connection is set to 1 if OK or -1 if failed.
         */
        public void checkAccount() {
            // Building Parameters
            List<NameValuePair> params = new ArrayList<>();
            params.add(new BasicNameValuePair(USERNAME, username));
            params.add(new BasicNameValuePair(PASSWORD, password));
            Log.d("checking account", username + password);

            // getting JSON Object
            // Note that create product url accepts POST method
            JSONObject json = jsonParser.makeHttpRequest(url_check_account, "GET", params);
            // check for success tag
            try {
                int success = json.getInt(TAG_SUCCESS);

                if (success == 1) {
                    //successfully
                    connection = 1; //Account was OK
                } else {
                    //failed
                    connection = -1; //Account was not OK
                    Log.d("AccountDB", "connection failed with " + username + ":" + password);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }
    private void addProduct(String name, String data, String key, String list, JSONParser parser){
        List<NameValuePair> params = new ArrayList<>();
        params.add(new BasicNameValuePair("name", name));
        params.add(new BasicNameValuePair("data", data));
        params.add(new BasicNameValuePair("key", key));
        params.add(new BasicNameValuePair("list", list));
        JSONObject json = parser.makeHttpRequest(url_add_product, "POST", params);
        try {
            int success = json.getInt("success");
            if (success == 1) {
                //successfully
                Log.d("AccountDB", "success for add product");

            } else {
                Log.d("AccountDB", "no success for add product");
                //failed
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
    private class SaveProducts extends AsyncTask<String, String, String>
    {
        private JSONParser jsonParser = new JSONParser();

        //Methods
        @Override
        protected String doInBackground(String... params) {
            // Building Parameters
            for(Product p : inventory)
            {
                Log.d("AccountDB", "adding product: " + p.toString());
                addProduct(username, p.toString(), p.getKey(),"inventory",jsonParser);
            }
            for(Product p : shoppingList)
            {
                addProduct(username, p.toString(), p.getKey(),"shoppinglist",jsonParser);
            }
            for(Product p : requirements)
            {
                addProduct(username, p.toString(), p.getKey(),"requirements",jsonParser);
            }
            return null;
        }

    }
        /**
         * Fill inventory, shopping list, and requirements with items from database for
         * the connected user.
         */
    private class loadProducts extends AsyncTask<String, String, String>
        {
            private JSONParser jsonParser = new JSONParser();
            private static final String TAG_SUCCESS = "success";
            private static final String USERNAME = "name";
            private static final String LIST = "list";
            List<NameValuePair> loadingParams;

            //Methods
            @Override
            protected String doInBackground(String... params) {
                // Building Parameters
                loadingParams = new ArrayList<>();
                loadingParams.add(new BasicNameValuePair(USERNAME, username));
                loadInventory();
                loadingParams = new ArrayList<>();
                loadingParams.add(new BasicNameValuePair(USERNAME, username));
                loadShoppingList();
                loadingParams = new ArrayList<>();
                loadingParams.add(new BasicNameValuePair(USERNAME, username));
                loadRequirements();
                return null;
            }
            protected int loadInventory()
            {
                loadingParams.add(new BasicNameValuePair(LIST, "inventory"));
                // getting JSON Object
                // Note that create product url accepts POST method
                JSONObject json = jsonParser.makeHttpRequest(url_get_products, "GET", loadingParams);
                // check for success tag
                try {
                    int success = json.getInt(TAG_SUCCESS);
                    if (success == 1) {
                        //successfully
                        Log.d("AccountDB", "success for get inventory");
                        //TODO fill and create the inventory array with all items
                        JSONArray productObj = json
                                .getJSONArray("inventory"); // JSON Array
                        for(int i = 0; i < productObj.length(); i++) {
                            JSONObject product = productObj.getJSONObject(0);   // get first product object from JSON Array
                            shoppingList.add(parseDatabase(product.getString("data"), product.getString("key"))); // sets databaseName to what was found in the database
                        }
                        loadInventory = 1;
                    } else {
                        Log.d("AccountDB", "no success for get inventory");
                        //failed
                        loadInventory = -1; //Loading inventory failed.
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                return 0;
            }
            protected int loadShoppingList()
            {
                loadingParams.add(new BasicNameValuePair(LIST, "shoppinglist"));
                // getting JSON Object
                // Note that create product url accepts POST method
                JSONObject json = jsonParser.makeHttpRequest(url_get_products, "GET", loadingParams);
                // check for success tag
                try {
                    int success = json.getInt(TAG_SUCCESS);
                    if (success == 1) {
                        //successfully
                        Log.d("AccountDB", "success for get shopping list");
                        //TODO fill and create the inventory array with all items
                        JSONArray productObj = json
                                .getJSONArray("shoppinglist"); // JSON Array
                        for(int i = 0; i < productObj.length(); i++) {
                            JSONObject product = productObj.getJSONObject(0);   // get first product object from JSON Array
                            shoppingList.add(parseDatabase(product.getString("data"), product.getString("key"))); // sets databaseName to what was found in the database
                        }
                        loadShoppingList = 1; //Loading inventory success.

                    } else {
                        Log.d("AccountDB", "no success for get shopping");
                        //failed
                        loadShoppingList = -1; //Loading inventory failed.
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            return 0;
            }
            protected int loadRequirements()
            {
                loadingParams.add(new BasicNameValuePair(LIST, "requirements"));
                // getting JSON Object
                // Note that create product url accepts POST method
                JSONObject json = jsonParser.makeHttpRequest(url_get_products, "GET", loadingParams);
                // check for success tag
                try {
                    int success = json.getInt(TAG_SUCCESS);
                    if (success == 1) {
                        //successfully
                        Log.d("AccountDB", "success for get reqs");
                        //TODO fill and create the inventory array with all items
                        JSONArray productObj = json
                                .getJSONArray("requirements"); // JSON Array
                        for(int i = 0; i < productObj.length(); i++) {
                            JSONObject product = productObj.getJSONObject(0);   // get first product object from JSON Array
                            requirements.add(parseDatabase(product.getString("data"), product.getString("key"))); // sets databaseName to what was found in the database
                        }
                        loadRequirements = 1; //Loading inventory success.

                    } else {
                        Log.d("AccountDB", "no success for get reqs");
                        //failed
                        loadRequirements = -1; //Loading inventory failed.
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                return 0;
            }







        }


    private Product parseDatabase(String string, String key) {
        String[] strings = string.split("\\|"); // The double backslash is needed for some characters
        // Namn, date, key, amount, code, expires
        return new Product(strings[0], strings[1], key, Integer.parseInt(strings[2]), strings[3], Boolean.valueOf(strings[4]));
    }

}
