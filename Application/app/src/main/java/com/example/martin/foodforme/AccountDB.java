package com.example.martin.foodforme;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.AsyncTask;

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
public class AccountDB extends Activity {
    //Attributes
    private String username;
    private String password;
    private ArrayList<Product> inventory;
    private int loadInventory = 0; //0 not done, 1 successfully loaded, -1 failed to load

    private static final String ip = "http://ffm.student.it.uu.se/cloud/"; // Ip-address for database
    private static final String url_get_inventory = ip + "get_inventory.php"; //Get all inventory from a user
    private static final String url_check_account = ip + "check_account.php"; //Check if password and user match and exist

    //Constructs
    public AccountDB(){
        SharedPreferences account = getSharedPreferences("account",MODE_PRIVATE);
        this.username = account.getString("user", "No user was found!");
        this.password = account.getString("password", "No password found!");
    }


    //Methods
    /**
     * Get the inventory for the user
     * @return - null if failed or nothing exist, a list of items otherwise.
     */
    public ArrayList<Product> getInventory(){
        new connectDB().execute();
        while(loadInventory == 0){
            if(loadInventory == -1){
                return null;
            }
            if(loadInventory == 1){
                break;
            }
        }
        return inventory;
    }

    //__________*Inner class to connect and operate on database*_______________//
    private class connectDB extends AsyncTask<String, String, String> {
        //Attributes
        private int connection = 0; //1 successful, -1 failed, 0 nothing
        //JSON
        private JSONParser jsonParser = new JSONParser();
        private static final String TAG_SUCCESS = "success";
        private static final String USERNAME = "name";
        private static final String PASSWORD = "password";

        //Methods
        @Override
        protected String doInBackground(String... params) {
            checkAccount();
            if(connection == 1){

            }
            return null;
        }


        /**
         * Check if account exist with that name and password.
         * attribute connection is set to 1 if OK or -1 if failed.
         */
        public void checkAccount(){
            // Building Parameters
            List<NameValuePair> params = new ArrayList<>();
            params.add(new BasicNameValuePair(USERNAME, username));
            params.add(new BasicNameValuePair(PASSWORD, password));

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
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        /**
         * Fill inventory with items from database for
         * the connected user.
         */
        public void loadInventory(){
            // Building Parameters
            List<NameValuePair> params = new ArrayList<>();
            params.add(new BasicNameValuePair(USERNAME, username));
            params.add(new BasicNameValuePair(PASSWORD, password));

            // getting JSON Object
            // Note that create product url accepts POST method
            JSONObject json = jsonParser.makeHttpRequest(url_get_inventory, "GET", params);

            // check for success tag
            try {
                int success = json.getInt(TAG_SUCCESS);

                if (success == 1) {
                    //successfully
                    //TODO fill and create the inventory array with all items
                    JSONArray productObj = json
                            .getJSONArray("inventory"); // JSON Array
                    for(int i = 0; i < productObj.length(); i++) {
                        JSONObject product = productObj.getJSONObject(0);   // get first product object from JSON Array
                        inventory.add(parseDatabase(product.getString("data"), product.getString("key"))); // sets databaseName to what was found in the database
                    }
                    loadInventory = 1; //Loading inventory success.

                } else {
                    //failed
                    loadInventory = -1; //Loading inventory failed.
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }

        }

    }
    private Product parseDatabase(String string, String key) {
        String[] strings = string.split("\\|"); // The double backslash is needed for some characters
        // Namn, date, key, amount, code, expires
        return new Product(strings[0], strings[1], key, Integer.parseInt(strings[2]), strings[3], Boolean.valueOf(strings[4]));
    }
}
