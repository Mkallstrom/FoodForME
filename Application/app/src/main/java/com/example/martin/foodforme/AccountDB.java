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
import java.util.Map;

/**
 * Created by Andreas on 2015-04-24.
 */
public class AccountDB extends Application {
    //Attributes
    private String username;
    private String password;
    private ArrayList<Product> inventory = new ArrayList<>(), shoppingList = new ArrayList<>(), requirements = new ArrayList<>();
    private int indexInventory, indexShoppingList, indexRequirements;
    private int loadInventory = 0; //0 not done, 1 successfully loaded, -1 failed to load
    private int loadShoppingList = 0;
    private int loadRequirements = 0;
    private int connection = 0; //1 successful, -1 failed, 0 nothing
    private boolean local = true;
    private boolean firstRun = false;
    private static final String ip = "http://ffm.student.it.uu.se/cloud/"; // Ip-address for database
    private static final String url_get_products = ip + "get_products.php"; //Get all products from a user
    private static final String url_check_account = ip + "check_account.php"; //Check if password and user match and exist
    private static final String url_add_product = ip + "add_product.php"; //Adds a product from the database
    private static final String url_delete_product = ip + "delete_product.php"; //Deletes a product from the database
    private static final String url_get_index = ip + "get_index.php"; //Get the accounts next index.
    private static final String url_increase_index = ip + "increase_index.php"; //Increase the accounts index.
    SharedPreferences inventorySP, shoppingSP, requiredSP;
    SharedPreferences.Editor inventoryEditor, shoppingEditor, requiredEditor;
    JSONParser jsonParser = new JSONParser();


    public void setDetails(String username, String password) {
        if(firstRun) return;
        this.username = username;
        this.password = password;
        Log.d("AccountDB", "set details");
        new ConnectDB().execute();
        loadIndex("inventory");
        loadIndex("shoppinglist");
        loadIndex("requirements");
        local = false;
        firstRun = true;
    }
    public ArrayList<Product> returnInventory(){ return inventory; }
    public ArrayList<Product> returnShoppingList(){ return shoppingList; }
    public ArrayList<Product> returnRequirements(){ return requirements; }

    public void storeProducts() {
        Log.d("AccountDB","attempting storeproducts with connection being: " + connection);
        if(connection==1) new SaveProducts().execute();
    }

    public void loadSharedPreferences(){
        if(firstRun) return;
        firstRun = true;
        Log.d("AccountDB", "loading shared prefs");
        inventorySP = getSharedPreferences("inventorySP", 0);
        requiredSP = getSharedPreferences("requiredSP", 0);
        shoppingSP = getSharedPreferences("shoppingSP", 0);
        inventoryEditor = inventorySP.edit();
        shoppingEditor = shoppingSP.edit();
        requiredEditor = requiredSP.edit();
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

        indexInventory = Integer.parseInt(inventorySP.getString("index",""));  //Get and save the index.
        indexRequirements = Integer.parseInt(requiredSP.getString("index",""));
        indexShoppingList = Integer.parseInt(shoppingSP.getString("index",""));

        Map<String,?> keys = inventorySP.getAll();
        for(Map.Entry<String,?> entry : keys.entrySet()){
            if(!entry.getKey().equals("index"))
            {
                inventory.add(parseProduct(entry.getValue().toString(), entry.getKey()));
            }
        }
        keys = shoppingSP.getAll();
        for(Map.Entry<String,?> entry : keys.entrySet()){
            if(!entry.getKey().equals("index"))
            {
                shoppingList.add(parseProduct(entry.getValue().toString(), entry.getKey()));
            }
        }
        keys = requiredSP.getAll();
        for(Map.Entry<String,?> entry : keys.entrySet()){
            if(!entry.getKey().equals("index"))
            {
                requirements.add(parseProduct(entry.getValue().toString(), entry.getKey()));
            }
        }
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
    private void insertProduct(String name, String data, String key, String list, JSONParser parser){
        List<NameValuePair> params = new ArrayList<>();
        params.add(new BasicNameValuePair("name", name));
        params.add(new BasicNameValuePair("data", data));
        params.add(new BasicNameValuePair("key", key));
        params.add(new BasicNameValuePair("list", list));
        JSONObject json = parser.makeHttpRequest(url_add_product, "POST", params);
        try {
            int success = json.getInt("success");
            String message = json.getString("message");
            if (success == 1) {
                //successfully
                Log.d("AccountDB", "success for add product");

            } else {
                Log.d("AccountDB", "no success for add product. Message: " + message);
                //failed
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
    public void removeProduct(Product p, String list){
        switch(list){
            case "inventory":
                inventory.remove(p);
                break;
            case "shoppinglist":
                shoppingList.remove(p);
                break;
            default:
                requirements.remove(p);
                break;
        }
        if(local)
        {
            SharedPreferences.Editor editor;
            switch(list){
                case("inventory"):
                    editor = inventoryEditor;
                    break;
                case("shoppinglist"):
                    editor = shoppingEditor;
                    break;
                default:
                    editor = requiredEditor;
                    break;
            }
            editor.remove(p.getKey());
            editor.commit();
        }
        else
        {
            deleteProduct(username,p.getKey(),list,jsonParser);
        }
    }

    private void deleteProduct(String name, String key, String list, JSONParser parser){
        List<NameValuePair> params = new ArrayList<>();
        params.add(new BasicNameValuePair("name", name));
        params.add(new BasicNameValuePair("key", key));
        params.add(new BasicNameValuePair("list", list));
        JSONObject json = parser.makeHttpRequest(url_delete_product, "POST", params);
        try {
            int success = json.getInt("success");
            String message = json.getString("message");
            if (success == 1) {
                //successfully
                Log.d("AccountDB", "success for delete product");

            } else {
                Log.d("AccountDB", "no success for delete product. Message: " + message);
                //failed
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void printList(){
        for(Product p : shoppingList)
        {
            Log.d("Print list", p.toString() + " with key " + p.getKey());
        }
    }

    public void addProduct(String name, String date, int amount, String code, boolean expires, String list){
        Product newProduct;
        switch(list){
            case("inventory"):
                indexInventory++;
                newProduct = new Product(name, date, Integer.toString(indexInventory), amount, code, expires);
                inventory.add(newProduct);
                break;
            case("shoppinglist"):
                indexShoppingList++;
                newProduct = new Product(name, date, Integer.toString(indexShoppingList), amount, code, expires);
                shoppingList.add(newProduct);
                Log.d("Adding product", newProduct.toString() + " with key " + newProduct.getKey());
                break;
            default:
                indexRequirements++;
                newProduct = new Product(name, date, Integer.toString(indexRequirements), amount, code, expires);
                requirements.add(newProduct);
                break;
        }
        if(local)
        {
            SharedPreferences.Editor editor;

            switch(list){
                case("inventory"):
                    editor = inventoryEditor;
                    editor.putString(Integer.toString(indexInventory),newProduct.toString());
                    editor.remove("index");
                    editor.putString("index", Integer.toString(indexInventory));
                    break;
                case("shoppinglist"):
                    editor = shoppingEditor;
                    editor.putString(Integer.toString(indexShoppingList),newProduct.toString());
                    editor.remove("index");
                    editor.putString("index", Integer.toString(indexShoppingList));
                    break;
                default:
                    editor = requiredEditor;
                    editor.putString(Integer.toString(indexRequirements),newProduct.toString());
                    editor.remove("index");
                    editor.putString("index", Integer.toString(indexRequirements));
                    break;
            }
            editor.commit();

        }
        else
        {
            increaseIndex(list);
            insertProduct(username, newProduct.toString(), newProduct.getKey(), list, jsonParser);

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
                insertProduct(username, p.toString(), p.getKey(), "inventory", jsonParser);
            }
            for(Product p : shoppingList)
            {
                insertProduct(username, p.toString(), p.getKey(), "shoppinglist", jsonParser);
            }
            for(Product p : requirements)
            {
                insertProduct(username, p.toString(), p.getKey(), "requirements", jsonParser);
            }
            return null;
        }

    }

    private class ClearProducts extends AsyncTask<String, String, String>
    {
        private JSONParser jsonParser = new JSONParser();

        //Methods
        @Override
        protected String doInBackground(String... params) {
            // Building Parameters
            for(Product p : inventory)
            {
                Log.d("AccountDB", "deleting product: " + p.toString());
                deleteProduct(username,p.getKey(), "inventory", jsonParser);
            }
            for(Product p : shoppingList)
            {
                deleteProduct(username, p.getKey(), "shoppinglist", jsonParser);
            }
            for(Product p : requirements)
            {
                deleteProduct(username, p.getKey(), "requirements", jsonParser);
            }
            return null;
        }

    }
    public void clearAll()
    {
        new ClearProducts().execute();
    }

    public void loadIndex(String list)
    {
        GetIndex getIndex = new GetIndex();
        getIndex.setList(list);
        getIndex.execute();
    }
    public void increaseIndex(String list)
    {
        IncreaseIndex increaseIndex = new IncreaseIndex();
        increaseIndex.setList(list);
        increaseIndex.execute();
    }
    private class GetIndex extends AsyncTask<String, String, String>
    {
        private JSONParser jsonParser = new JSONParser();
        private String list;
        private static final String TAG_SUCCESS = "success";
        private static final String USERNAME = "name";
        private static final String INDEX = "index";
        List<NameValuePair> indexParams;

        //Methods
        @Override
        protected String doInBackground(String... params) {
            // Building Parameters
            if(list == null){
                Log.d("AccountDB", "list not been set!");
                return null;
            }
            indexParams = new ArrayList<>();
            indexParams.add(new BasicNameValuePair(USERNAME, username));

            JSONObject json = jsonParser.makeHttpRequest(url_get_index, "GET", indexParams);
            // check for success tag
            try {
                int success = json.getInt(TAG_SUCCESS);
                if (success == 1) {
                    //successfully
                    Log.d("AccountDB", "success for get " + list + " index");
                    JSONArray productObj = json
                            .getJSONArray(INDEX); // JSON Array
                    JSONObject product = productObj.getJSONObject(0);   // get first product object from JSON Array
                    switch(list){
                        case "inventory":
                            indexInventory = product.getInt(INDEX);
                            break;
                        case "shoppinglist":
                            indexShoppingList = product.getInt(INDEX);
                            break;
                        case "requirements":
                            indexRequirements = product.getInt(INDEX);
                            break;
                    }
                } else {
                    Log.d("AccountDB", "no success for get " + list + " index");
                    //failed
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }

            return null;
        }

        public void setList(String list){ this.list = list;}
    }

    private class IncreaseIndex extends AsyncTask<String, String, String>
    {
        private JSONParser jsonParser = new JSONParser();
        private String list;
        private static final String TAG_SUCCESS = "success";
        private static final String USERNAME = "name";
        private static final String LIST = "list";
        List<NameValuePair> indexParams;

        //Methods
        @Override
        protected String doInBackground(String... params) {
            // Building Parameters
            if(list == null){
                Log.d("AccountDB", "list not been set!");
                return null;
            }
            indexParams = new ArrayList<>();
            indexParams.add(new BasicNameValuePair(USERNAME, username));
            indexParams.add(new BasicNameValuePair(LIST, list));

            JSONObject json = jsonParser.makeHttpRequest(url_increase_index, "GET", indexParams);
            // check for success tag
            try {
                int success = json.getInt(TAG_SUCCESS);
                if (success == 1) {
                    //successfully
                    Log.d("AccountDB", "success for increase " + list + " index");

                } else {
                    Log.d("AccountDB", "no success for increase " + list + " index");
                    //failed
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }

            return null;
        }

        public void setList(String list){ this.list = list;}
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
                        JSONArray productObj = json
                                .getJSONArray("inventory"); // JSON Array
                        for(int i = 0; i < productObj.length(); i++) {
                            JSONObject product = productObj.getJSONObject(0);   // get first product object from JSON Array
                            shoppingList.add(parseProduct(product.getString("data"), product.getString("key"))); // sets databaseName to what was found in the database
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
                        JSONArray productObj = json
                                .getJSONArray("shoppinglist"); // JSON Array
                        for(int i = 0; i < productObj.length(); i++) {
                            JSONObject product = productObj.getJSONObject(0);   // get first product object from JSON Array
                            shoppingList.add(parseProduct(product.getString("data"), product.getString("key"))); // sets databaseName to what was found in the database
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
                        JSONArray productObj = json
                                .getJSONArray("requirements"); // JSON Array
                        for(int i = 0; i < productObj.length(); i++) {
                            JSONObject product = productObj.getJSONObject(0);   // get first product object from JSON Array
                            requirements.add(parseProduct(product.getString("data"), product.getString("key"))); // sets databaseName to what was found in the database
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


    private Product parseProduct(String string, String key) {
        String[] strings = string.split("\\|"); // The double backslash is needed for some characters
        // Namn, date, key, amount, code, expires
        return new Product(strings[0], strings[1], key, Integer.parseInt(strings[2]), strings[3], Boolean.valueOf(strings[4]));
    }

}
