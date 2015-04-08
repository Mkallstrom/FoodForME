package com.example.martin.foodforme;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.TimeUnit;

public class Product {

    public String name;

    public String expiryDate;
    public int expiryYear;
    public int expiryMonth;
    public int expiryDay;

    // Public constructor
    public Product(String productName, String expiryDate) {
        name = productName;
        setExpiryDate(expiryDate);
    }

    public void setName(String productName) {
        name = productName;
    }

    public void setExpiryDate(String expiryDate) {
        this.expiryDate = expiryDate;
        int[] extractedDate = dateStringToArray(expiryDate);
        expiryYear = extractedDate[0];
        expiryMonth = extractedDate[1];
        expiryDay = extractedDate[2];
    }

    // Calculates days until the product expires (assuming format: yyyy-MM-dd)
    // If already expired it will return a negative number
    public int daysUntilExpired() {
        String todaysDate = getTodaysDate();
        int[] todaysDateArray = dateStringToArray(todaysDate);
        int todaysYear = todaysDateArray[0];
        int todaysMonth = todaysDateArray[1];
        int todaysDay = todaysDateArray[2];

        // Based on: http://stackoverflow.com/a/20165708
        SimpleDateFormat myDateFormat = new SimpleDateFormat("yyyy-MM-dd");
        try {
            Date date1 = myDateFormat.parse(todaysDate);
            Date date2 = myDateFormat.parse(expiryDate);
            long diff = date2.getTime() - date1.getTime();
            return (int)TimeUnit.DAYS.convert(diff, TimeUnit.MILLISECONDS);
        } catch(ParseException e) {
            e.printStackTrace();
            return -9001;
        }
    }

    // Gets todays date in string format yyyy-MM-dd
    private String getTodaysDate() {
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        Date date = new Date();
        return dateFormat.format(date);
    }

    // Extracts year, month and day into an array (assuming format: yyyy-MM-dd)
    private int[] dateStringToArray(String date) {
        String[] splitDate = date.split("-");
        int yearToday = Integer.parseInt(splitDate[0]);
        int monthToday = Integer.parseInt(splitDate[1]);
        int dayToday = Integer.parseInt(splitDate[2]);

        int[] expDate = {yearToday, monthToday, dayToday};

        return expDate;

    }
}
