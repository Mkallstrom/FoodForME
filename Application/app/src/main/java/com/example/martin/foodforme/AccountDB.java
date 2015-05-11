package com.example.martin.foodforme;

import android.annotation.SuppressLint;
import android.app.Application;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.ArrayAdapter;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class AccountDB extends Application {
    //Attributes
    private String accountUsername;
    private String accountPassword;

    private ArrayList<Product> inventoryList = new ArrayList<>(),
            shoppingList = new ArrayList<>(),
            requirementsList = new ArrayList<>();
    private ArrayAdapter inventoryAdapter,
            shoppinglistAdapter,
            requirementsAdapter;

    private int indexInventory,
            indexShoppingList,
            indexRequirements;

    private boolean gettingIndex = false,
            gettingIndexFailed = false;

    private int connection = 0; //1 successful, -1 failed, 0 nothing
    private boolean local = true;
    private boolean firstRun = false; // AccountDB has been initiated if firstRun is true

    private int loadingProgress = 3;
    private int savingProgress = 0;

    private static final String ip = "http://ffm.student.it.uu.se/cloud/"; // Ip-address for database
    private static final String url_get_products = ip + "get_products.php"; //Get all products from a user
    private static final String url_check_account = ip + "check_account.php"; //Check if accountPassword and user match and exist
    private static final String url_add_product = ip + "add_product.php"; //Adds a product from the database
    private static final String url_delete_product = ip + "delete_product.php"; //Deletes a product from the database
    private static final String url_get_index = ip + "get_index.php"; //Get the accounts next index.
    private static final String url_increase_index = ip + "increase_index.php"; //Increase the accounts index.
    private static final String url_create_account = ip + "create_account.php"; //Create account
    private static final String TAG_SUCCESS = "success";
    private static final String USERNAME = "name";
    private static final String PASSWORD = "password";
    private static final String LIST = "list";
    private static final String INDEX = "index";

    private SharedPreferences inventorySP,
                shoppingSP,
                requirementSP;
    private SharedPreferences.Editor inventoryEditor,
                shoppingEditor,
                requirementEditor;

    private JSONParser jsonParser = new JSONParser();

    private static final String noBarcode = "No barcode";

    //Methods


    /**
     * Sets the username and password and attempts a connection to the database.
     * If connection is successful, local is set to false.
     * @param username - The username of the account.
     * @param password - The password of the account.
     */
    public void connectToDatabase(String username, String password) {
        if(firstRun) //AccountDB already initiated
        {
            return;
        }
        firstRun = true;
        Log.d("AccountDB", "Loading data from database.");
        this.accountUsername = username;
        this.accountPassword = password;
        new ConnectDB().execute();
        while(connection == 0)
        {
        }
        if(connection == 1){
            local = false;
        }
        if(connection == -1) {
            local = true;
        }
    }

    /**
     * Sets the username and password, sets up database use by disabling local.
     * @param username - The username of the account.
     * @param password - The password of the account.
     */
    public void connected(String username, String password){
        this.accountUsername = username;
        this.accountPassword = password;
        local = false;
        connection = 1;
    }

    /**
     * Disconnects the application from the database, setting local to true and reloading the products from the shared preferences.
     */
    public void disconnect(){
        local = true;
        firstRun = false;
        connection = 0;
        clearProducts();
        loadSharedPreferences();
    }

    /**
     * Checks if there was a connection setup and then
     * sets the local to correct value.
     */
    public void checkLocal(){
        if(connection == 1){
            local = false;
        }
        if(connection == -1 || connection == 0){
            local = true;
        }
    }

    /**
     * Copies all products in the current inventory, shopping, and requirements lists to the shared preferences.
     */
    @SuppressLint("CommitPrefEdits")
    public void copyToLocal(){
        savingProgress = 0;

        int total = inventoryList.size() + shoppingList.size() + requirementsList.size();
        int tenPercent = total/10;
        int saved = 0;

        inventorySP = getSharedPreferences("inventorySP", 0);
        requirementSP = getSharedPreferences("requirementSP", 0);
        shoppingSP = getSharedPreferences("shoppingSP", 0);
        inventoryEditor = inventorySP.edit();
        shoppingEditor = shoppingSP.edit();
        requirementEditor = requirementSP.edit();
        inventoryEditor.clear();
        shoppingEditor.clear();
        requirementEditor.clear();

        getIndex("inventory");
        while(gettingIndex){}
        if(gettingIndexFailed) return;
        getIndex("shoppinglist");
        while(gettingIndex){}
        if(gettingIndexFailed) return;
        getIndex("requirements");
        while(gettingIndex){}
        if(gettingIndexFailed) return;

        inventoryEditor.putString(INDEX, Integer.toString(indexInventory));
        shoppingEditor.putString(INDEX, Integer.toString(indexShoppingList));
        requirementEditor.putString(INDEX, Integer.toString(indexRequirements));

        for(Product p : inventoryList){
            inventoryEditor.putString(p.getKey(),p.toString());
            Log.d("CopyLocal inventoryList", "Adding " + p.toString());
            saved++;
            if(saved > tenPercent*savingProgress) savingProgress+=10;
        }
        for(Product p : shoppingList){
            shoppingEditor.putString(p.getKey(),p.toString());
            Log.d("CopyLocal shoppinglist", "Adding " + p.toString());
            saved++;
            if(saved > tenPercent*savingProgress) savingProgress+=10;
        }
        for(Product p : requirementsList){
            requirementEditor.putString(p.getKey(), p.toString());
            Log.d("CopyLocal requirements", "Adding " + p.toString());
            saved++;
            if(saved > tenPercent*savingProgress) savingProgress+=10;
        }

        inventoryEditor.commit();
        shoppingEditor.commit();
        requirementEditor.commit();

        savingProgress = 100;
    }

    /**
     * Loads products from the shared preferences.
     */
    @SuppressWarnings("Annotator")
    public void loadSharedPreferences(){
        if(firstRun) //AccountDB already initiated
        {
            return;
        }
        firstRun = true;
        Log.d("AccountDB", "Loading data from shared preferences.");
        inventorySP = getSharedPreferences("inventorySP", 0);
        requirementSP = getSharedPreferences("requirementSP", 0);
        shoppingSP = getSharedPreferences("shoppingSP", 0);
        inventoryEditor = inventorySP.edit();
        shoppingEditor = shoppingSP.edit();
        requirementEditor = requirementSP.edit();
        if(!inventorySP.contains(INDEX))                            //If file does not contain the index, add it starting from 0.
        {
            inventoryEditor.putString(INDEX, "0");
            inventoryEditor.commit();
        }
        if(!requirementSP.contains(INDEX))                            //If file does not contain the index, add it starting from 0.
        {
            requirementEditor.putString(INDEX, "0");
            requirementEditor.commit();
        }
        if(!shoppingSP.contains(INDEX))                            //If file does not contain the index, add it starting from 0.
        {
            shoppingEditor.putString(INDEX, "0");
            shoppingEditor.commit();
        }

        indexInventory = Integer.parseInt(inventorySP.getString(INDEX,""));  //Get and save the index.
        indexRequirements = Integer.parseInt(requirementSP.getString(INDEX,""));
        indexShoppingList = Integer.parseInt(shoppingSP.getString(INDEX, ""));

        clearProducts();

        Map<String,?> keys = inventorySP.getAll();
        for(Map.Entry<String,?> entry : keys.entrySet()){
            if(!entry.getKey().equals(INDEX))
            {
                inventoryList.add(parseProduct(entry.getValue().toString(), entry.getKey()));
            }
        }
        keys = shoppingSP.getAll();
        for(Map.Entry<String,?> entry : keys.entrySet()){
            if(!entry.getKey().equals(INDEX))
            {
                shoppingList.add(parseProduct(entry.getValue().toString(), entry.getKey()));
            }
        }
        keys = requirementSP.getAll();
        for(Map.Entry<String,?> entry : keys.entrySet()){
            if(!entry.getKey().equals(INDEX))
            {
                requirementsList.add(parseProduct(entry.getValue().toString(), entry.getKey()));
            }
        }
    }

    /**
     * Attempts a connection to the database with a username and a password. Sets connection to the appropriate value afterwards.
     * @param username - The username of the account.
     * @param password - The password of the account.
     * @return - Success of connection.
     */
    public boolean existsAccountInDatabase(String username, String password){
        JSONParser jsonParser = new JSONParser();

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
                Log.d("AccountDB", "connection successful with " + username + ":" + password);
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

        //Methods
        @Override
        protected String doInBackground(String... params) {
            checkAccount();
            return null;
        }


        /**
         * Check if account exists with that name and accountPassword.
         * attribute connection is set to 1 if OK or -1 if failed.
         */
        public void checkAccount() {
            // Building Parameters
            List<NameValuePair> params = new ArrayList<>();
            params.add(new BasicNameValuePair(USERNAME, accountUsername));
            params.add(new BasicNameValuePair(PASSWORD, accountPassword));
            Log.d("checking account", accountUsername + accountPassword);

            // getting JSON Object
            // Note that create product url accepts POST method
            JSONObject json = jsonParser.makeHttpRequest(url_check_account, "GET", params);

            if(json == null){
                connection = -1;
                return;
            }
            // check for success tag
            try {
                int success = json.getInt(TAG_SUCCESS);

                if (success == 1) {
                    //successfully
                    connection = 1; //Account was OK
                } else {
                    //failed
                    connection = -1; //Account was not OK
                    Log.d("AccountDB", "connection failed with " + accountUsername + ":" + accountPassword);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }


    /**
     * Adds a product to a list. If local is true, the product is added to the shared preferences, else to the database.
     * @param name - Name of product.
     * @param date - Expiry date of product.
     * @param amount - Amount of product.
     * @param code - Barcode of product.
     * @param expires - Whether the product can expire or not.
     * @param list - List product is part of.
     */
    public void addProduct(String name, String date, int amount, String code, boolean expires, String list){
        Product newProduct;
        Log.d("addProduct", "Adding " + name + " to " + list);
        if(local)
        {
            SharedPreferences.Editor editor;
            Log.d("addProduct", "Locally");
            switch(list){
                case("inventory"):
                    indexInventory++;
                    newProduct = new Product(name, date, Integer.toString(indexInventory), amount, code, expires);
                    inventoryList.add(newProduct);
                    Collections.sort(inventoryList);
                    inventoryAdapter.notifyDataSetChanged();
                    editor = inventoryEditor;
                    editor.putString(Integer.toString(indexInventory),newProduct.toString());
                    editor.remove(INDEX);
                    editor.putString(INDEX, Integer.toString(indexInventory));
                    break;
                case("shoppinglist"):
                    indexShoppingList++;
                    newProduct = new Product(name, date, Integer.toString(indexShoppingList), amount, code, expires);
                    shoppingList.add(newProduct);
                    Collections.sort(shoppingList);
                    shoppinglistAdapter.notifyDataSetChanged();
                    editor = shoppingEditor;
                    editor.putString(Integer.toString(indexShoppingList),newProduct.toString());
                    editor.remove(INDEX);
                    editor.putString(INDEX, Integer.toString(indexShoppingList));
                    break;
                case("requirements"):
                    indexRequirements++;
                    newProduct = new Product(name, date, Integer.toString(indexRequirements), amount, code, expires);
                    requirementsList.add(newProduct);
                    requirementsAdapter.notifyDataSetChanged();
                    editor = requirementEditor;
                    editor.putString(Integer.toString(indexRequirements),newProduct.toString());
                    editor.remove(INDEX);
                    editor.putString(INDEX, Integer.toString(indexRequirements));
                    break;
                default:
                    Log.d("addProduct", "List invalid");
                    return;
            }
            editor.commit();

        }
        else
        {
            getIndex(list);
            while(gettingIndex){
                if(gettingIndexFailed) {
                    Log.d("Failure", "Getting index failed! Product not added.");
                    return;
                }
            }
            increaseIndex(list);
            switch(list)
            {
                case "inventory":
                    newProduct = new Product(name, date, Integer.toString(indexInventory), amount, code, expires);
                    break;
                case "shoppinglist":
                    newProduct = new Product(name, date, Integer.toString(indexShoppingList), amount, code, expires);
                    break;
                case "requirements":
                    newProduct = new Product(name, date, Integer.toString(indexRequirements), amount, code, expires);
                    break;
                default:
                    Log.d("addProduct", "List invalid.");
                    return;
            }
            AddToDatabase addToDatabase = new AddToDatabase(accountUsername, newProduct.toString(),newProduct.getKey(),list);
            addToDatabase.execute();

        }
    }

    /**
     * AsyncTask that is used to add a product to the database.
     */
    public class AddToDatabase extends AsyncTask<String, String, String>{

        String name;
        String data;
        String key;
        String list;


        /**
         * Adds a product to the database.
         * @param name - Name of account.
         * @param data - Data of product.
         * @param key - Key of product.
         * @param list - List product is part of.
         */
        public AddToDatabase(String name, String data, String key, String list){
            this.name = name;
            this.data = data;
            this.key = key;
            this.list = list;
        }

        @Override
        protected void onPostExecute(String result){
            switch(list)
            {
                case "inventory":
                    if(inventoryAdapter!=null) {
                        Collections.sort(inventoryList);
                        inventoryAdapter.notifyDataSetChanged();
                    }
                    break;
                case "shoppinglist":
                    if(shoppinglistAdapter!=null) {
                        Collections.sort(shoppingList);
                        shoppinglistAdapter.notifyDataSetChanged();
                    }
                    break;
                case "requirements":
                    if(requirementsAdapter!=null) {
                        requirementsAdapter.notifyDataSetChanged();
                    }
                    break;
            }
        }
        @Override
        protected String doInBackground(String... params) {
            List<NameValuePair> insertparams = new ArrayList<>();
            insertparams.add(new BasicNameValuePair("name", name));
            insertparams.add(new BasicNameValuePair("password", accountPassword));
            insertparams.add(new BasicNameValuePair("data", data));
            insertparams.add(new BasicNameValuePair("key", key));
            insertparams.add(new BasicNameValuePair("list", list));
            JSONObject json = jsonParser.makeHttpRequest(url_add_product, "POST", insertparams);
            try {
                int success = json.getInt("success");
                String message = json.getString("message");
                if (success == 1) {
                    //successfully
                    Log.d("AccountDB", "success for add product");

                    switch(list)
                    {
                        case "inventory":
                            inventoryList.add(parseProduct(data, key));
                            break;
                        case "shoppinglist":
                            shoppingList.add(parseProduct(data, key));
                            break;
                        case "requirements":
                            requirementsList.add(parseProduct(data, key));
                            break;
                    }


                } else {
                    Log.d("AccountDB", "no success for add product. Message: " + message);
                    //failed
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return null;
        }


    }

    /**
     * AsyncTask that saves a product to the database.
     */
    public class SaveInDatabase extends AsyncTask<String, String, String>{

        String name;
        String data;
        String key;
        String list;

        /**
         * Saves a product to the database.
         * @param name - Name of account.
         * @param data - Data of product.
         * @param key - Key of product.
         * @param list - List product is part of.
         */
        public SaveInDatabase(String name, String data, String key, String list){
            this.name = name;
            this.data = data;
            this.key = key;
            this.list = list;
        }

        @Override
        protected String doInBackground(String... params) {
            List<NameValuePair> insertparams = new ArrayList<>();
            insertparams.add(new BasicNameValuePair("name", name));
            insertparams.add(new BasicNameValuePair("password", accountPassword));
            insertparams.add(new BasicNameValuePair("data", data));
            insertparams.add(new BasicNameValuePair("key", key));
            insertparams.add(new BasicNameValuePair("list", list));
            JSONObject json = jsonParser.makeHttpRequest(url_add_product, "POST", insertparams);
            try {
                int success = json.getInt("success");
                String message = json.getString("message");
                if (success == 1) {
                    //successfully
                    Log.d("AccountDB", "success for add product");
                    savingProgress++;

                } else {
                    Log.d("AccountDB", "no success for add product. Message: " + message);
                    //failed
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return null;
        }


    }

    /**
     * Deletes a product. If local is true, it deletes the product from the shared preferences, else from the database.
     * @param deletedProduct - Product to be deleted.
     * @param list - List product is part of.
     */
    public void deleteProduct(Product deletedProduct, String list){
        if(local)
        {
            SharedPreferences.Editor editor;
            switch(list){
                case "inventory":
                    editor = inventoryEditor;
                    inventoryList.remove(deletedProduct);
                    break;
                case "shoppinglist":
                    editor = shoppingEditor;
                    shoppingList.remove(deletedProduct);
                    break;
                case "requirements":
                    editor = requirementEditor;
                    requirementsList.remove(deletedProduct);
                    break;
                default:
                    return;
            }
            editor.remove(deletedProduct.getKey());
            editor.commit();
        }
        else
        {
            DeleteFromDatabase delete = new DeleteFromDatabase(accountUsername, accountPassword, deletedProduct.getKey(),list);
            delete.execute();
        }
    }

    /**
     * AsyncTask used to delete a product from the database.
     */
    private class DeleteFromDatabase extends AsyncTask<String, String, String>{

        String name;
        String key;
        String list;
        String password;

        /**
         * Deletes a product from the database.
         * @param name - Name of account.
         * @param password - Password of account.
         * @param key - Key of product.
         * @param list - List product is part of.
         */
        public DeleteFromDatabase(String name, String password, String key, String list){
            this.name = name;
            this.key = key;
            this.list = list;
            this.password = password;
        }

        @Override
        protected void onPostExecute(String result){
            switch(list)
            {
                case "inventory":
                    Collections.sort(inventoryList);
                    inventoryAdapter.notifyDataSetChanged();
                    break;
                case "shoppinglist":
                    Collections.sort(shoppingList);
                    shoppinglistAdapter.notifyDataSetChanged();
                    break;
                case "requirements":
                    requirementsAdapter.notifyDataSetChanged();
                    break;
            }
        }

        @Override
        protected String doInBackground(String... params) {
            List<NameValuePair> deleteparams = new ArrayList<>();
            deleteparams.add(new BasicNameValuePair("name", name));
            deleteparams.add(new BasicNameValuePair("password", password));
            deleteparams.add(new BasicNameValuePair("key", key));
            deleteparams.add(new BasicNameValuePair("list", list));
            JSONObject json = jsonParser.makeHttpRequest(url_delete_product, "POST", deleteparams);
            try {
                int success = json.getInt("success");
                String message = json.getString("message");
                if (success == 1) {
                    //successfully
                    Log.d("AccountDB", "success for delete product");
                    switch(list)
                    {
                        case "inventory":
                            inventoryList.remove(keyToProduct(key, inventoryList));
                            break;
                        case "shoppinglist":
                            shoppingList.remove(keyToProduct(key, shoppingList));
                            break;
                        case "requirements":
                            requirementsList.remove(keyToProduct(key, requirementsList));
                            break;
                    }

                } else {
                    Log.d("AccountDB", "no success for delete product. Message: " + message);
                    //failed
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return null;
        }
    }

    /**
     * Starts a SaveProducts that saves all products to the database.
     */
    public void saveProducts() {
        if(connection==1) {
            new SaveProducts().execute();
        }
    }

    /**
     * AsyncTask that saves all products to the database.
     */
    private class SaveProducts extends AsyncTask<String, String, String>
    {
        @Override
        protected void onPreExecute(){
            savingProgress = 0;
        }

        //Methods
        @Override
        protected String doInBackground(String... params) {
            // Building Parameters
            Log.d("SaveProducts", "Saving " + inventoryList.size() + " products to inventoryList");
            for(Product p : inventoryList)
            {
                SaveInDatabase sp = new SaveInDatabase(accountUsername, p.toString(), p.getKey(), "inventory");
                sp.execute();
            }
            Log.d("SaveProducts", "Saving " + shoppingList.size() + " products to shopping list");
            for(Product p : shoppingList)
            {
                SaveInDatabase sp = new SaveInDatabase(accountUsername, p.toString(), p.getKey(), "shoppinglist");
                sp.execute();
            }
            Log.d("SaveProducts", "Saving " + requirementsList.size() + " products to requirementsList");
            for(Product p : requirementsList)
            {
                SaveInDatabase sp = new SaveInDatabase(accountUsername, p.toString(), p.getKey(), "requirements");
                sp.execute();
            }
            return null;
        }

    }

    /**
     * Gets the index of 'list' from the database.
     * @param list - The list to get the index for.
     */
    public void getIndex(String list)
    {
        gettingIndex = true;
        GetIndex getIndex = new GetIndex();
        getIndex.setList(list);
        getIndex.execute();
    }

    /**
     * Increases the index of 'list' in the database.
     * @param list - The list to increase the index for.
     */
    public void increaseIndex(String list)
    {
        IncreaseIndex increaseIndex = new IncreaseIndex();
        increaseIndex.setList(list);
        increaseIndex.execute();
    }

    /**
     * AsyncTask that gets an index from the database.
     */
    private class GetIndex extends AsyncTask<String, String, String>
    {
        private JSONParser jsonParser = new JSONParser();
        private String list;
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
            indexParams.add(new BasicNameValuePair(USERNAME, accountUsername));
            indexParams.add(new BasicNameValuePair(PASSWORD, accountPassword));
            indexParams.add(new BasicNameValuePair(LIST, list));

            JSONObject json = jsonParser.makeHttpRequest(url_get_index, "GET", indexParams);
            // check for success tag
            try {
                int success = json.getInt(TAG_SUCCESS);
                if (success == 1) {
                    //successfully
                    Log.d("AccountDB", "success for get " + list + " index");
                    gettingIndexFailed = false;
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
                    gettingIndexFailed = true;
                    //failed
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
            gettingIndex = false;
            return null;
        }

        public void setList(String list){ this.list = list;}
    }

    /**
     * AsyncTask that increases an index in the database.
     */
    private class IncreaseIndex extends AsyncTask<String, String, String>
    {
        private JSONParser jsonParser = new JSONParser();
        private String list;
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
            indexParams.add(new BasicNameValuePair(USERNAME, accountUsername));
            indexParams.add(new BasicNameValuePair(PASSWORD, accountPassword));
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
     * Starts a LoadProducts to load all products from the database.
     */
    public void loadProducts(){
        new LoadProducts().execute();
    }

    /**
     * AsyncTask that loads all products from the database.
     */
    private class LoadProducts extends AsyncTask<String, String, String>
        {
            private JSONParser jsonParser = new JSONParser();
            List<NameValuePair> loadingParams;

            @Override
            protected void onPreExecute(){
                clearProducts();
                Log.d("AccountDB","Loading products");
                loadingProgress = 0;
            }
            @Override
            protected void onPostExecute(String result){
            }
            //Methods
            @Override
            protected String doInBackground(String... params) {
                Log.d("loadProducts", "Initiating product loading...");
                // Building Parameters
                loadingParams = new ArrayList<>();
                loadingParams.add(new BasicNameValuePair(USERNAME, accountUsername));
                loadingParams.add(new BasicNameValuePair(PASSWORD, accountPassword));
                loadInventory();
                Collections.sort(inventoryList);
                loadingProgress++;
                loadingParams = new ArrayList<>();
                loadingParams.add(new BasicNameValuePair(USERNAME, accountUsername));
                loadingParams.add(new BasicNameValuePair(PASSWORD, accountPassword));
                loadShoppingList();
                Collections.sort(shoppingList);
                loadingProgress++;
                loadingParams = new ArrayList<>();
                loadingParams.add(new BasicNameValuePair(USERNAME, accountUsername));
                loadingParams.add(new BasicNameValuePair(PASSWORD, accountPassword));
                loadRequirements();
                loadingProgress++;
                Log.d("AccountDB", "Finished loading products");

                return null;
            }

            /**
             * Loads all products for the inventory from the database.
             * @return - 1 if success, -1 if fail.
             */
            protected int loadInventory()
            {
                loadingParams.add(new BasicNameValuePair(LIST, "inventory"));
                // getting JSON Object
                // Note that create product url accepts POST method
                JSONObject json = jsonParser.makeHttpRequest(url_get_products, "GET", loadingParams);

                if(json == null){
                    return -1; //Failed HTTP request
                }
                // check for success tag
                try {
                    int success = json.getInt(TAG_SUCCESS);
                    if (success == 1) {
                        //successfully
                        Log.d("AccountDB", "success for get inventoryList");
                        JSONArray productObj = json
                                .getJSONArray("inventory"); // JSON Array
                        for(int i = 0; i < productObj.length(); i++) {
                            JSONObject product = productObj.getJSONObject(i);   // get first product object from JSON Array
                            Log.d("AccountDB", "Adding product to inventoryList: " + product.getString("data"));
                            inventoryList.add(parseProduct(product.getString("data"), product.getString("key"))); // sets databaseName to what was found in the database
                        }
                    } else {
                        Log.d("AccountDB", "no success for get inventoryList");
                        return -1;
                        //failed
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                return 1;
            }

            /**
             * Loads all products for the shopping list from the database.
             * @return - 1 if success, -1 if fail.
             */
            protected int loadShoppingList()
            {
                loadingParams.add(new BasicNameValuePair(LIST, "shoppinglist"));
                // getting JSON Object
                // Note that create product url accepts POST method
                JSONObject json = jsonParser.makeHttpRequest(url_get_products, "GET", loadingParams);
                if(json == null){
                    return -1;
                }
                // check for success tag
                try {
                    int success = json.getInt(TAG_SUCCESS);
                    if (success == 1) {
                        //successfully
                        Log.d("AccountDB", "success for get shopping list");
                        JSONArray productObj = json
                                .getJSONArray("shoppinglist"); // JSON Array
                        for(int i = 0; i < productObj.length(); i++) {
                            JSONObject product = productObj.getJSONObject(i);   // get first product object from JSON Array
                            shoppingList.add(parseProduct(product.getString("data"), product.getString("key"))); // sets databaseName to what was found in the database
                        }

                    } else {
                        Log.d("AccountDB", "no success for get shopping");
                        return -1;
                        //failed
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            return 1;
            }

            /**
             * Loads all products for the requirements from the database.
             * @return - 1 if success, -1 if fail.
             */
            protected int loadRequirements()
            {
                loadingParams.add(new BasicNameValuePair(LIST, "requirements"));
                // getting JSON Object
                // Note that create product url accepts POST method
                JSONObject json = jsonParser.makeHttpRequest(url_get_products, "GET", loadingParams);
                if(json == null){
                    return -1; //Failed HTTP request
                }
                // check for success tag
                try {
                    int success = json.getInt(TAG_SUCCESS);
                    if (success == 1) {
                        //successfully
                        Log.d("AccountDB", "success for get reqs");
                        JSONArray productObj = json
                                .getJSONArray("requirements"); // JSON Array
                        for(int i = 0; i < productObj.length(); i++) {
                            JSONObject product = productObj.getJSONObject(i);   // get first product object from JSON Array
                            requirementsList.add(parseProduct(product.getString("data"), product.getString("key"))); // sets databaseName to what was found in the database
                        }

                    } else {
                        Log.d("AccountDB", "no success for get reqs");
                        return -1;
                        //failed
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                return 1;
            }

        }

    /**
     * Starts a SaveAccount to create and save an account in the database.
     * @param name - Username of account.
     * @param password - Password of account.
     */
    public void saveAccount(String name, String password)
    {
        new SaveAccount(name, password).execute();
    }

    /**
     * AsyncTask that creates and saves an account in the database.
     */
    public class SaveAccount extends AsyncTask<String, String, String> {
        private String username;
        private String password;

        public SaveAccount(String name, String password){
            this.username = name;
            this.password = password;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected String doInBackground(String... args) {
            createAccountDB();
            return null;

        }

        /**
         * Creates an account in the database.
         */
        private void createAccountDB() {
            // Building Parameters
            List<NameValuePair> params = new ArrayList<>();
            params.add(new BasicNameValuePair(USERNAME, username));
            params.add(new BasicNameValuePair(PASSWORD, password));
            params.add(new BasicNameValuePair("indexInventory",Integer.toString(indexInventory)));
            params.add(new BasicNameValuePair("indexShoppingList",Integer.toString(indexShoppingList)));
            params.add(new BasicNameValuePair("indexRequirements",Integer.toString(indexRequirements)));

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
                    local = false;
                    int oldConnection = connection;
                    storeAccountOnPhone(username,password);
                    connected(username,password);
                    if(oldConnection == 0) {
                        saveProducts();
                    }
                    else {
                        clearProducts();
                    }
                    // closing this screen
                    //finish();

                } else {
                    savingProgress = -1; //Account failed to be created
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }


        }
    }

    /**
     * Stores the account details in the shared preferences.
     * @param username - Username of account.
     * @param password - Password of account.
     */
    @SuppressLint("CommitPrefEdits")
    public void storeAccountOnPhone(String username, String password){
        SharedPreferences account = getSharedPreferences("account",MODE_PRIVATE);
        SharedPreferences.Editor accountEditor = account.edit();
        accountEditor.putBoolean("active", true);
        accountEditor.putString("user", username);
        accountEditor.putString("password", password);
        accountEditor.commit();
    }

    /**
     * Switches the account details stored in the shared preferences.
     * @param user - Username of account.
     * @param password - Password of account.
     */
    public void switchAccountOnPhone(String user, String password){
        SharedPreferences account = getSharedPreferences("account",MODE_PRIVATE);
        SharedPreferences.Editor accountEditor = account.edit();
        accountEditor.clear();
        accountEditor.commit();
        storeAccountOnPhone(user, password);
    }

    /**
     * Clears the current lists. Does not affect the data in the shared preferences or in the database, just in the current session.
     */
    public void clearProducts(){
        inventoryList.clear();
        shoppingList.clear();
        requirementsList.clear();
    }

    /**
     * Reloads products.
     */
    public void refreshProducts(){
        if(local){
            loadSharedPreferences();
        }
        else {
            loadProducts();
        }
        inventoryAdapter.notifyDataSetChanged();
        shoppinglistAdapter.notifyDataSetChanged();
        requirementsAdapter.notifyDataSetChanged();
    }

    /**
     * Sets a list adapter.
     * @param list - The list to set the adapter for.
     * @param adapter - The adapter.
     */
    public void setAdapter(String list, ArrayAdapter adapter){
        switch(list) {
            case "inventory":
                inventoryAdapter = adapter;
                break;
            case "shoppinglist":
                shoppinglistAdapter = adapter;
                break;
            case "requirements":
                requirementsAdapter = adapter;
                break;
            default:
                break;
        }
    }

    /**
     * Resets connection to 0 to show that it is unconnected.
     */
    public void resetConnection(){
        connection = 0;
    }

    /**
     * Gets whether a requirement is unfulfilled, partially fulfilled, or fulfilled.
     * @param requiredAmount - The amount of the product required.
     * @param code - The barcode of the product required.
     * @return - 0 if the amount of the item in the inventory is equal to the amount required. 1 if it is more than 0 but not equal. 2 if none are in the inventory.
     */
    public int getRequirementStatus(int requiredAmount, String code){
        int currentAmount = 0;
        for(Product inventoryItem : inventoryList)
        {
            if(inventoryItem.getCode().equals(code))
            {
                currentAmount += Integer.parseInt(inventoryItem.getAmount());
            }
        }
        if(currentAmount < requiredAmount)
        {
            if(currentAmount == 0)
            {
                return 2; // No products.
            }
            else
            {
                return 1; // Partial amount;
            }

        }
        else
        {
            return 0; // Enough items in inventory.
        }
    }

    /**
     * Gets the amount required of a product.
     * @param code - The code of the product.
     * @return - The amount required of the product.
     */
    public int getRequiredAmount (String code){
        int requiredAmount = 0;
        for(Product requirement : requirementsList)
        {
            if(requirement.getCode().equals(code))
            {
                requiredAmount += Integer.parseInt(requirement.getAmount());
            }
        }
        return requiredAmount;
    }

    /**
     * Creates and returns a product by parsing a string and giving it the given key.
     * @param string - The data of the product.
     * @param key - The key of the product.
     * @return - The created product.
     */
    private Product parseProduct(String string, String key) {
        String[] strings = string.split("\\|"); // The double backslash is needed for some characters
        return new Product(strings[0], strings[1], key, Integer.parseInt(strings[2]), strings[3], Boolean.valueOf(strings[4]));
    }

    /**
     * Finds a product in the list with the given key.
     * @param key - The key to look for.
     * @param list - The list to look in.
     * @return - The first found product in the list with the key. If none are found, returns null.
     */
    private Product keyToProduct (String key, ArrayList<Product> list)
    {
        for (Product p : list)
        {
            if(p.getKey().equals(key)) {
                return p;
            }
        }
        return null;
    }

    /**
     * Returns the amount of products in all lists combined.
     * @return - The amount of products in inventory + shopping list + requirements.
     */
    public int getTotalProducts(){
        return inventoryList.size()+shoppingList.size()+ requirementsList.size();
    }

    public String getNoBarcode(){ return noBarcode; }
    public ArrayList<Product> returnInventory(){ return inventoryList; }
    public ArrayList<Product> returnShoppingList(){ return shoppingList; }
    public ArrayList<Product> returnRequirements(){ return requirementsList; }
    public String getAccountUsername() { return accountUsername; }
    public boolean isLocal(){ return local; }
    public int getLoadingProgress(){ return loadingProgress; }
    public int getSavingProgress(){return savingProgress; }
    public int getConnection(){ return connection; }

}
