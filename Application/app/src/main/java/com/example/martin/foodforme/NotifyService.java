package com.example.martin.foodforme;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.util.Log;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Map;

/**
 * Created by Martin on 2015-04-20.
 */
public class NotifyService extends Service {

    ArrayList<Product> inventoryList = new ArrayList<>();
    ArrayList<Product> expiringProducts = new ArrayList<>();
    String title, details = "";
    int today = 0, tomorrow = 0, dayAfterTomorrow = 0;
    private final static String TAG = "Notifications";

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        Log.v(TAG, "Service started");

        buildList();                                        // Make list of expiring products.
        buildText();                                        // Make title and detail strings for the notification.
        if(!expiringProducts.isEmpty()) sendNotification(); // Send notification.
        setAlarm();                                         // Set alarm for next day.

        Log.v(TAG, "Service stopped");
        stopSelf();
    }

    public void setAlarm() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR, 16);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND,0);
        calendar.add(Calendar.DATE, 1);

        Intent serviceIntent = new Intent(this, NotifyService.class);
        PendingIntent pi = PendingIntent.getService(this, 131313, serviceIntent,
                PendingIntent.FLAG_CANCEL_CURRENT);

        AlarmManager am = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        am.set(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis() - System.currentTimeMillis(), pi);
        Log.v(TAG, "Alarm set to " + calendar.toString() + " which is in " + Long.toString((calendar.getTimeInMillis()-System.currentTimeMillis())/(1000*60)) + " minutes.");
    }

    public void sendNotification() {
        Intent targetIntent = new Intent(this, InventoryActivity.class);
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
    public Product parseSharedPreferences(String string, String key) {
        String[] strings = string.split("\\|"); // The double backslash is needed for some characters
        // Namn, date, key, amount, code, expires
        return new Product(strings[0], strings[1], key, Integer.parseInt(strings[2]), strings[3], Boolean.valueOf(strings[4]));
    }
    public void buildText()
    {
        for(Product p : inventoryList)
        {
            if(p.daysUntilExpired() >= 0 && p.daysUntilExpired() < 3) {
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
        SharedPreferences inventorySP = getSharedPreferences("inventorySP",0);
        Map<String,?> keys = inventorySP.getAll();
        for(Map.Entry<String,?> entry : keys.entrySet()){
            if(!entry.getKey().equals("index"))
            {
                inventoryList.add(parseSharedPreferences(entry.getValue().toString(), entry.getKey()));
            }
        }
    }

}