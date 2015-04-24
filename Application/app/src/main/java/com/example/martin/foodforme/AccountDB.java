package com.example.martin.foodforme;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.util.Log;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
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
    private ArrayList<Product> products;
    private int loadProducts = 0; //0 not done, 1 successfully loaded, -1 failed to load

    private static final String ip = "http://ffm.student.it.uu.se/cloud/"; // Ip-address for database
    private static final String url_get_products = ip + "get_products.php"; //Get all products from a user
    private static final String url_check_account = ip + "check_account.php"; //Check if password and user match and exist

    //Constructs
    public AccountDB(){
        SharedPreferences account = getSharedPreferences("account",MODE_PRIVATE);
        this.username = account.getString("user", "No user was found!");
        this.password = account.getString("password", "No password found!");
    }


    //Methods
    /**
     * Get the products for the user
     * @return - null if failed or nothing exist, a list of items otherwise.
     */
    public ArrayList<Product> getProducts(){
        new connectDB().execute();
        while(loadProducts == 0){
            if(loadProducts == -1){
                return null;
            }
            if(loadProducts == 1){
                break;
            }
        }
        return products;
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
         * Fill products with items from database for
         * the connected user.
         */
        public void loadProducts(){
            // Building Parameters
            List<NameValuePair> params = new ArrayList<>();
            params.add(new BasicNameValuePair(USERNAME, username));
            params.add(new BasicNameValuePair(PASSWORD, password));

            // getting JSON Object
            // Note that create product url accepts POST method
            JSONObject json = jsonParser.makeHttpRequest(url_get_products, "GET", params);

            // check for success tag
            try {
                int success = json.getInt(TAG_SUCCESS);

                if (success == 1) {
                    //successfully
                    //TODO fill and create the products array with all items
                    loadProducts = 1; //Loading products success.

                } else {
                    //failed
                    loadProducts = -1; //Loading products failed.
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }

        }

    }
}
