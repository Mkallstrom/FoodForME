package com.example.martin.foodforme;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.TimeUnit;

public class Product implements Comparable<Product>{

    private String name;
    private String expiryDate;
    private String key;
    private String amount;
    private String code;

    private boolean expiring = true;

    private int expiryYear;
    private int expiryMonth;
    private int expiryDay;

    private String DUMMY_DATE = "9999-99-99"; // sets a dummy date for products without expiration

    /**
     * Public constructor for a Product object
     * @param productName   the name of the Product.
     * @param key           the key (used in the SharedPreferences) of the Product.
     * @param amount        the amount of the same Product.
     */
    public Product(String productName, String key, int amount) {
        this.name = productName;
        setExpiryDate(DUMMY_DATE);
        this.expiring = false;
        this.key = key;
        this.code = "0";
        this.amount = Integer.toString(amount);
    }

    /**
     * Public constructor for a Product object.
     * @param productName   the name of the Product.
     * @param expiryDate    the expiration date of the Product.
     * @param key           the key (used in the SharedPreferences) of the Product.
     * @param amount        the amount of the same Product.
     * @param code          the barcode of the Product.
     */
    public Product(String productName, String expiryDate, String key, int amount, String code, boolean expires) {
        this.name = productName;
        if(expires)
        {
            setExpiryDate(expiryDate);
        }
        else
        {
            setExpiryDate(DUMMY_DATE);
            expiring = false;
        }
        this.key = key;
        this.amount = Integer.toString(amount);
        this.code = code;
    }

    /**
     * Sets the name of the Product
     * @param productName   the name of the Product.
     */
    public void setName(String productName) {
        name = productName;
    }

    /**
     * Sets the expiration date of the Product
     * @param expDate   the expiration date as a String "year-month-day" (yyyy-MM-dd).
     */
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

    public boolean expires() { return expiring; }

    public String getCode() { return code; }

    /**
     * Calculates how many days left until the product will pass it's expiration date.
     * @return  days until the product expires.
     */
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

    /**
     * Finds today's date and returns it as a String in the format "year-month-day" (yyyy-MM-dd).
     * @return  A string representing today's date.
     */
    private String getTodaysDate() {
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        Date date = new Date();
        return dateFormat.format(date);
    }

    /**
     * Splits the date string representation into three integers returned as an int array.
     * @param date  A date in the format "year-month-day" (yyyy-MM-dd).
     * @return      if successful int array [year,month,day], else [0,0,0]
     */
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

    /**
     * Compare the expiration dates between two inventoryList
     * @param product   The Product to compare this Product with
     * @return          Positive if the Product (this) expires after the Product it is compared with,
     *                  else the returned value is negative.
     */
    @Override
    public int compareTo(Product product){
        int thisExpireDate = this.daysUntilExpired();
        int otherExpireDate = product.daysUntilExpired();
        return thisExpireDate - otherExpireDate;
    }

    /**
     * Returns this Product in string format.
     * @return  String with name, expiryDate, amount and code separated with '|'.
     */
    public String toString()
    {
        return name + "|" + expiryDate + "|" + amount + "|" + code + "|" + Boolean.toString(expiring);
    }
}
