package com.example.martin.foodforme;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.TimeUnit;

public class Product {

    private String name;
    private String expiryDate;
    private String key;
    private String amount;
    private String code;

    private int expiryYear;
    private int expiryMonth;
    private int expiryDay;

    // Public constructor
    public Product(String productName, String expiryDate, String key, String code) {
        name = productName;
        setExpiryDate(expiryDate);
        this.key = key;
        this.code = code;
    }

    // Constructor including amount
    public Product(String productName, String expiryDate, String key, int amount, String code) {
        name = productName;
        setExpiryDate(expiryDate);
        this.key = key;
        this.amount = Integer.toString(amount);
        this.code = code;
    }
    // Constructor for shopping and requirements
    public Product(int amount, String key, String name) {
        this.name = name;
        this.key = key;
        this.amount = Integer.toString(amount);
    }

    public void setName(String productName) {
        name = productName;
    }

    public void setExpiryDate(String expDate) {
        expiryDate = expDate;
        int[] extractedDate = dateStringToArray(expDate);
        expiryYear = extractedDate[0];
        expiryMonth = extractedDate[1];
        expiryDay = extractedDate[2];
    }

    public void setKey(String key) {
        this.key = key;
    }

    public void setAmount(String amount) {
        this.amount = amount;
    }

    public void setExpiryYear(int expiryYear) {
        this.expiryYear = expiryYear;
    }

    public void setExpiryMonth(int expiryMonth) {
        this.expiryMonth = expiryMonth;
    }

    public void setExpiryDay(int expiryDay) {
        this.expiryDay = expiryDay;
    }

    public void setCode(String code) { this.code = code; }

    public String getName() {
        return name;
    }

    public String getExpiryDate() {
        return expiryDate;
    }

    public String getKey() {
        return key;
    }

    public String getAmount() {
        return amount;
    }

    public int getExpiryYear() {
        return expiryYear;
    }

    public int getExpiryMonth() {
        return expiryMonth;
    }

    public int getExpiryDay() {
        return expiryDay;
    }

    public String getCode() { return code; }
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
        } catch(ParseException e) { // String could not get parsed to a date
            e.printStackTrace();
            return -9001; // Setting a trash value
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
        try {
            String[] splitDate = date.split("-");
            int parsedYear = Integer.parseInt(splitDate[0]);
            int parsedMonth = Integer.parseInt(splitDate[1]);
            int parsedDay = Integer.parseInt(splitDate[2]);

            return new int[] {parsedYear, parsedMonth, parsedDay};
        } catch (Exception e) {  // If it fails to parse the date, return [0,0,0]
            e.printStackTrace();
            return new int[]{0, 0, 0};
        }
    }
}
