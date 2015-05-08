package com.example.martin.foodforme;

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

/**
 * Created by Andreas on 2015-04-24.
 */
public class AccountDB extends Application {
    //Attributes
    private String username;
    private String password;

    private ArrayList<Product> inventory = new ArrayList<>(), shoppingList = new ArrayList<>(), requirements = new ArrayList<>();
    private ArrayAdapter inventoryAdapter, shoppinglistAdapter, requirementsAdapter;

    private int indexInventory, indexShoppingList, indexRequirements;

    private int loadInventory = 0; //0 not done, 1 successfully loaded, -1 failed to load
    private int loadShoppingList = 0;
    private int loadRequirements = 0;

    private boolean gettingIndex = false;
    private boolean gettingIndexFailed = false;

    private int connection = 0; //1 successful, -1 failed, 0 nothing
    private boolean local = true;
    private boolean firstRun = false;

    private int loadingProgress = 3;
    private int savingProgress = 0;

    private static final String ip = "http://ffm.student.it.uu.se/cloud/"; // Ip-address for database
    private static final String url_get_products = ip + "get_products.php"; //Get all products from a user
    private static final String url_check_account = ip + "check_account.php"; //Check if password and user match and exist
    private static final String url_add_product = ip + "add_product.php"; //Adds a product from the database
    private static final String url_delete_product = ip + "delete_product.php"; //Deletes a product from the database
    private static final String url_get_index = ip + "get_index.php"; //Get the accounts next index.
    private static final String url_increase_index = ip + "increase_index.php"; //Increase the accounts index.
    private static final String url_create_account = ip + "create_account.php"; //Create account
    private static final String TAG_SUCCESS = "success";
    private static final String USERNAME = "name";
    private static final String PASSWORD = "password";

    SharedPreferences inventorySP, shoppingSP, requiredSP;
    SharedPreferences.Editor inventoryEditor, shoppingEditor, requiredEditor;

    JSONParser jsonParser = new JSONParser();
    private String noBarcode = "No barcode";


    public void setDetails(String username, String password) {
        if(firstRun) return;
        this.username = username;
        this.password = password;
        Log.d("AccountDB", "set details");
        new ConnectDB().execute();
        while(connection == 0){}
        if(connection == 1){
            local = false;
        }
        if(connection == -1) {
            local = true;
        }
        firstRun = true;
    }

    /**
     * Check if there was a connection setup and then
     * set the local to correct value;
     */
    public void checkLocal(){
        if(connection == 1){
            local = false;
        }
        if(connection == -1 || connection == 0){
            local = true;
        }
    }

    public ArrayList<Product> returnInventory(){ return inventory; }
    public ArrayList<Product> returnShoppingList(){ return shoppingList; }
    public ArrayList<Product> returnRequirements(){ return requirements; }
    public String getUsername() { return username; }
    public boolean isLocal(){ return local; }
    public int getLoadingProgress(){ return loadingProgress; }
    public int getSavingProgress(){return savingProgress; }

    public int getTotalProducts(){
        return inventory.size()+shoppingList.size()+requirements.size();
    }

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

    public void setLocal(Boolean bool){
        local = bool;
    }

    public void storeProducts() {
        Log.d("AccountDB", "attempting storeproducts with connection being: " + connection);
        if(connection==1) new SaveProducts().execute();
    }

    public void disconnect(){
        local = true;
        firstRun = false;
        clearProducts();
        loadSharedPreferences();
        connection = 0;
    }

    public void copyToLocal(){
        savingProgress = 0;
        int total = inventory.size() + shoppingList.size() + requirements.size();
        Log.d("Total products", Integer.toString(total));
        int tenPercent = total/10;
        Log.d("Ten percent", Integer.toString(tenPercent));
        int saved = 0;
        inventorySP = getSharedPreferences("inventorySP", 0);
        requiredSP = getSharedPreferences("requiredSP", 0);
        shoppingSP = getSharedPreferences("shoppingSP", 0);
        inventoryEditor = inventorySP.edit();
        shoppingEditor = shoppingSP.edit();
        requiredEditor = requiredSP.edit();
        inventoryEditor.clear();
        shoppingEditor.clear();
        requiredEditor.clear();
        loadIndex("inventory");
        while(gettingIndex){}
        if(gettingIndexFailed) return;
        loadIndex("shoppinglist");
        while(gettingIndex){}
        if(gettingIndexFailed) return;
        loadIndex("requirements");
        while(gettingIndex){}
        if(gettingIndexFailed) return;
        inventoryEditor.putString("index", Integer.toString(indexInventory));
        shoppingEditor.putString("index", Integer.toString(indexShoppingList));
        requiredEditor.putString("index", Integer.toString(indexRequirements));
        for(Product p : inventory){
            inventoryEditor.putString(p.getKey(),p.toString());
            Log.d("CopyLocal inventory", "Adding " + p.toString());
            saved++;
            if(saved > tenPercent*savingProgress) savingProgress+=10;
        }
        for(Product p : shoppingList){
            shoppingEditor.putString(p.getKey(),p.toString());
            Log.d("CopyLocal shoppinglist", "Adding " + p.toString());
            saved++;
            if(saved > tenPercent*savingProgress) savingProgress+=10;
        }
        for(Product p : requirements){
            requiredEditor.putString(p.getKey(),p.toString());
            Log.d("CopyLocal requirements", "Adding " + p.toString());
            saved++;
            if(saved > tenPercent*savingProgress) savingProgress+=10;
        }
        inventoryEditor.commit();
        shoppingEditor.commit();
        requiredEditor.commit();
        savingProgress = 100;
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
        accountEditor.clear();
        accountEditor.putBoolean("active", true);
        accountEditor.putString("user", user);
        accountEditor.putString("password", password);
        accountEditor.commit();
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
                    Log.d("AccountDB", "connection failed with " + username + ":" + password);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }
    public class insertProduct extends AsyncTask<String, String, String>{

        String name;
        String data;
        String key;
        String list;

        public insertProduct(String name, String data, String key, String list){
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
                        Collections.sort(inventory);
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
            insertparams.add(new BasicNameValuePair("password", password));
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
                            inventory.add(parseProduct(data, key));
                            break;
                        case "shoppinglist":
                            shoppingList.add(parseProduct(data, key));
                            break;
                        case "requirements":
                            requirements.add(parseProduct(data, key));
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
    public class SaveProduct extends AsyncTask<String, String, String>{

        String name;
        String data;
        String key;
        String list;

        public SaveProduct(String name, String data, String key, String list){
            this.name = name;
            this.data = data;
            this.key = key;
            this.list = list;
        }

        @Override
        protected void onPostExecute(String result){

        }
        @Override
        protected String doInBackground(String... params) {
            List<NameValuePair> insertparams = new ArrayList<>();
            insertparams.add(new BasicNameValuePair("name", name));
            insertparams.add(new BasicNameValuePair("password", password));
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
    public void removeProduct(Product p, String list){
        if(local)
        {
            SharedPreferences.Editor editor;
            switch(list){
                case "inventory":
                    editor = inventoryEditor;
                    inventory.remove(p);
                    break;
                case "shoppinglist":
                    editor = shoppingEditor;
                    shoppingList.remove(p);
                    break;
                case "requirements":
                    editor = requiredEditor;
                    requirements.remove(p);
                    break;
                default:
                    return;
            }
            editor.remove(p.getKey());
            editor.commit();
        }
        else
        {
            deleteProduct dp = new deleteProduct(username, password, p.getKey(),list);
            dp.execute();
        }
    }

    private class deleteProduct extends AsyncTask<String, String, String>{

        String name;
        String key;
        String list;
        String password;

        public deleteProduct(String name, String password, String key, String list){
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
                    Collections.sort(inventory);
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
                            inventory.remove(keyToProduct(key, inventory));
                            break;
                        case "shoppinglist":
                            shoppingList.remove(keyToProduct(key, shoppingList));
                            break;
                        case "requirements":
                            requirements.remove(keyToProduct(key, requirements));
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
                    inventory.add(newProduct);
                    Collections.sort(inventory);
                    inventoryAdapter.notifyDataSetChanged();
                    editor = inventoryEditor;
                    editor.putString(Integer.toString(indexInventory),newProduct.toString());
                    editor.remove("index");
                    editor.putString("index", Integer.toString(indexInventory));
                    break;
                case("shoppinglist"):
                    indexShoppingList++;
                    newProduct = new Product(name, date, Integer.toString(indexShoppingList), amount, code, expires);
                    shoppingList.add(newProduct);
                    Collections.sort(shoppingList);
                    shoppinglistAdapter.notifyDataSetChanged();
                    editor = shoppingEditor;
                    editor.putString(Integer.toString(indexShoppingList),newProduct.toString());
                    editor.remove("index");
                    editor.putString("index", Integer.toString(indexShoppingList));
                    break;
                default:
                    indexRequirements++;
                    newProduct = new Product(name, date, Integer.toString(indexRequirements), amount, code, expires);
                    requirements.add(newProduct);
                    requirementsAdapter.notifyDataSetChanged();
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
            loadIndex(list);
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
            insertProduct ip = new insertProduct(username, newProduct.toString(),newProduct.getKey(),list);
            ip.execute();

        }
    }

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
            Log.d("SaveProducts", "Saving " + inventory.size() + " products to inventory");
            for(Product p : inventory)
            {
                SaveProduct sp = new SaveProduct(username, p.toString(), p.getKey(), "inventory");
                sp.execute();
            }
            Log.d("SaveProducts", "Saving " + shoppingList.size() + " products to shopping list");
            for(Product p : shoppingList)
            {
                SaveProduct sp = new SaveProduct(username, p.toString(), p.getKey(), "shoppinglist");
                sp.execute();
            }
            Log.d("SaveProducts", "Saving " + requirements.size() + " products to requirements");
            for(Product p : requirements)
            {
                SaveProduct sp = new SaveProduct(username, p.toString(), p.getKey(), "requirements");
                sp.execute();
            }
            return null;
        }

    }

    public void clearProducts(){
        inventory.clear();
        shoppingList.clear();
        requirements.clear();
    }

    public void connected(String username, String password){
        this.username = username;
        this.password = password;
        local = false;
        connection = 1; }

    public void loadIndex(String list)
    {
        gettingIndex = true;
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
        private static final String PASSWORD = "password";
        private static final String INDEX = "index";
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
            indexParams.add(new BasicNameValuePair(PASSWORD, password));
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

    private class IncreaseIndex extends AsyncTask<String, String, String>
    {
        private JSONParser jsonParser = new JSONParser();
        private String list;
        private static final String TAG_SUCCESS = "success";
        private static final String USERNAME = "name";
        private static final String PASSWORD = "password";
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
            indexParams.add(new BasicNameValuePair(PASSWORD, password));
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
            private static final String PASSWORD = "password";
            private static final String LIST = "list";
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
                Log.d("getProducts", "Initiating product loading...");
                // Building Parameters
                loadingParams = new ArrayList<>();
                loadingParams.add(new BasicNameValuePair(USERNAME, username));
                loadingParams.add(new BasicNameValuePair(PASSWORD, password));
                loadInventory();
                Collections.sort(inventory);
                loadingProgress++;
                loadingParams = new ArrayList<>();
                loadingParams.add(new BasicNameValuePair(USERNAME, username));
                loadingParams.add(new BasicNameValuePair(PASSWORD, password));
                loadShoppingList();
                Collections.sort(shoppingList);
                loadingProgress++;
                loadingParams = new ArrayList<>();
                loadingParams.add(new BasicNameValuePair(USERNAME, username));
                loadingParams.add(new BasicNameValuePair(PASSWORD, password));
                loadRequirements();
                loadingProgress++;
                Log.d("AccountDB", "Finished loading products");

                return null;
            }

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
                        Log.d("AccountDB", "success for get inventory");
                        JSONArray productObj = json
                                .getJSONArray("inventory"); // JSON Array
                        for(int i = 0; i < productObj.length(); i++) {
                            JSONObject product = productObj.getJSONObject(i);   // get first product object from JSON Array
                            Log.d("AccountDB", "Adding product to inventory: " + product.getString("data"));
                            inventory.add(parseProduct(product.getString("data"), product.getString("key"))); // sets databaseName to what was found in the database
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
                if(json == null){
                    return -1; //Failed HTTP request
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


    public int getIndexInventory() {
        return indexInventory;
    }

    public int getIndexShoppingList() {
        return indexShoppingList;
    }

    public int getIndexRequirements() {
        return indexRequirements;
    }


    private Product parseProduct(String string, String key) {
        String[] strings = string.split("\\|"); // The double backslash is needed for some characters
        // Namn, date, key, amount, code, expires
        return new Product(strings[0], strings[1], key, Integer.parseInt(strings[2]), strings[3], Boolean.valueOf(strings[4]));
    }

    /**
     * Give connection back to a state where it is loading. Reseting connection.
     */
    public void resetConnection(){
        connection = 0;
    }

    public int getConnection(){
        return connection;
    }

    private Product keyToProduct (String key, ArrayList<Product> list)
    {
                for (Product p : list)
                {
                    if(p.getKey().equals(key)) return p;
                }
        return null;
    }

    public void saveAccount(String name, String password){ new SaveAccount(name, password).execute();}
    public class SaveAccount extends AsyncTask<String, String, String> {
        private String username;
        private String password;
        /**
         * Before starting background thread
         */

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
        public void storeAccountOnPhone(String username, String password){
            SharedPreferences account = getSharedPreferences("account",MODE_PRIVATE);
            SharedPreferences.Editor accountEditor = account.edit();
            accountEditor.putBoolean("active", true);
            accountEditor.putString("user", username);
            accountEditor.putString("password", password);
            accountEditor.commit();
        }


        /**
         * Create account in database cloud.
         *
         *
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
                        storeProducts();
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

    public String getNoBarcode(){
        return noBarcode;
    }

}
