package com.example.martin.foodforme;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.IBinder;
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
 * Created by Martin on 2015-04-20.
 */
public class NotifyService extends Service {

    private boolean local;
    private ArrayList<Product> inventoryList = new ArrayList<>();
    private ArrayList<Product> expiringProducts = new ArrayList<>();
    private String title,
            details = "",
            username,
            password;
    int today = 0,
            tomorrow = 0,
            dayAfterTomorrow = 0,
            inventoryLoader = 0;
    private final static String TAG = "Notifications";
    private static final String ip = "http://ffm.student.it.uu.se/cloud/"; // Ip-address for database
    private static final String url_get_products = ip + "get_products.php"; //Get all products from a user

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        Log.v(TAG, "Service started");
        SharedPreferences account = getSharedPreferences("account", MODE_PRIVATE);
        local = !account.getBoolean("active", false);
        if(!local) {
            username = account.getString("user", "No user was found!");
            password = account.getString("password", "No password found!");
        }
        buildList();                                        // Make list of expiring products.
        if(local | inventoryLoader == 1)
        {
            Log.d("Service", "Building text for notification");
            buildText();                                        // Make title and detail strings for the notification.
        }
        if (!expiringProducts.isEmpty()) {
            Log.d("Service", "Sending notification");
            sendNotification();// Send notification.
        }
        Log.v(TAG, "Service stopped");
        stopSelf();
    }


    public void sendNotification()
    {
        Intent targetIntent = new Intent(this, MainActivity.class);
        @SuppressWarnings("deprecation")
        Notification noti = new Notification.Builder(this)
                .setAutoCancel(true)
                .setContentIntent(PendingIntent.getActivity(this, 131314, targetIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT))
                .setContentTitle(title)
                .setContentText(details)
                .setDefaults(Notification.DEFAULT_ALL)
                .setSmallIcon(R.drawable.logo)
                .setTicker(title)
                .setWhen(System.currentTimeMillis())
                .getNotification();

        NotificationManager notificationManager
                = (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(131315, noti);

        Log.v(TAG, "Notification sent");
    }
    public Product parseSharedPreferences(String string, String key)
    {
        String[] strings = string.split("\\|"); // The double backslash is needed for some characters
        // Namn, date, key, amount, code, expires
        return new Product(strings[0], strings[1], key, Integer.parseInt(strings[2]), strings[3], Boolean.valueOf(strings[4]));
    }
    public void buildText()
    {
        for(Product p : inventoryList)
        {
            Log.d("buildText","Found product " + p.toString() + " which is expiring in " + p.daysUntilExpired());
            if(p.daysUntilExpired() >= 0 && p.daysUntilExpired() < 3) {
                Log.d("buildText", "Adding to expiring products: " + p.toString());
                expiringProducts.add(p);
                switch (p.daysUntilExpired())
                {
                    case 0:
                        today++;
                        break;
                    case 1:
                        tomorrow++;
                        break;
                    case 2:
                        dayAfterTomorrow++;
                        break;
                }
            }
        }
        if(today+tomorrow+dayAfterTomorrow==1) {
            title = "FFM: 1 product expiring soon.";
        }
        else {
            title = "FFM: " + (today+tomorrow+dayAfterTomorrow) + " products expiring soon.";
        }
        String[] detailArray = new String[3];
        detailArray[0] = today + " today. ";
        detailArray[1] = tomorrow + " tomorrow. ";
        detailArray[2] = dayAfterTomorrow + " in two days.";
        if(today>0) details = details+detailArray[0];
        if(tomorrow>0) details = details+detailArray[1];
        if(dayAfterTomorrow>0) details = details+detailArray[2];

    }
    public void buildList()
    {
        if(local)
        {
            SharedPreferences inventorySP = getSharedPreferences("inventorySP", 0);
            Map<String, ?> keys = inventorySP.getAll();
            for (Map.Entry<String, ?> entry : keys.entrySet()) {
                if (!entry.getKey().equals("index")) {
                    inventoryList.add(parseSharedPreferences(entry.getValue().toString(), entry.getKey()));
                }
            }
        }
        else
        {
            new loadProducts().execute();
            while(inventoryLoader == 0){}
        }
    }
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
            Log.d("NotifyService","Loading inventory");
            inventoryLoader = 0;
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
            loadingParams.add(new BasicNameValuePair(USERNAME, username));
            loadingParams.add(new BasicNameValuePair(PASSWORD, password));
            loadingParams.add(new BasicNameValuePair(LIST, "inventory"));
            // getting JSON Object
            // Note that create product url accepts POST method
            JSONObject json = jsonParser.makeHttpRequest(url_get_products, "GET", loadingParams);

            if(json == null){
                inventoryLoader = -1;
                return null; //Failed HTTP request
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
                        inventoryList.add(parseSharedPreferences(product.getString("data"), product.getString("key"))); // sets databaseName to what was found in the database
                    }
                    inventoryLoader = 1;
                } else {
                    Log.d("AccountDB", "no success for get inventory");
                    inventoryLoader = -1;
                    //failed
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return null;
        }
    }
}