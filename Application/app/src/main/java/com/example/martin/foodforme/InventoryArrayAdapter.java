package com.example.martin.foodforme;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.ArrayList;

/**
 * Created by Martin on 2015-04-09.
 */
public class InventoryArrayAdapter extends ArrayAdapter<Product> {

    private Context context;
    private int resource;
    private ArrayList<Product> products;
    private LayoutInflater inflater;
    private AccountDB accountDB;
    private static int expiredBar;
    private static int expiringBar;
    private static int notexpiringBar;
    private static int nonexpiringBar;

    public InventoryArrayAdapter(Context context, int resource, ArrayList products, AccountDB accountDB)
    {
        super(context, resource, products);
        this.context=context;
        this.resource=resource;
        this.products=products;
        inflater=((Activity)context).getLayoutInflater();
        this.accountDB = accountDB;
        expiredBar = (R.drawable.barred);
        expiringBar = (R.drawable.baryellow);
        notexpiringBar = (R.drawable.bargreen);
        nonexpiringBar = (R.drawable.barblue);

    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        View row = inflater.inflate(resource,parent,false);

        TextView title = (TextView)row.findViewById(R.id.title);
        TextView number = (TextView)row.findViewById(R.id.date);
        TextView remaining = (TextView)row.findViewById(R.id.daysremaining);
        TextView amount = (TextView)row.findViewById(R.id.amount);

        title.setText(products.get(position).getName());
        number.setText(products.get(position).getExpiryDate());

        int requiredAmount = accountDB.getRequiredAmount(products.get(position));
        amount.setText(products.get(position).getAmount());
        if(requiredAmount>0)
        {
            amount.setText(amount.getText() + "/" + requiredAmount);
        }

        int expiringIn = products.get(position).daysUntilExpired();

        if(products.get(position).expires())
        {
            if (expiringIn == 0) {
                remaining.setText("Expires today.");
                row.setBackgroundResource(expiringBar);
            } else {
                if (expiringIn > 0) {
                    remaining.setText("Expires in " + Integer.toString(expiringIn) + " days.");
                    if (expiringIn < 8) row.setBackgroundResource(expiringBar);
                    else row.setBackgroundResource(notexpiringBar);
                } else {
                    remaining.setText("Expired " + Integer.toString(-expiringIn) + " days ago.");
                    row.setBackgroundResource(expiredBar);
                }
            }
        }
        else
        {
            remaining.setText("");
            number.setText("");
            row.setBackgroundResource(nonexpiringBar);
        }
        return row;
    }


}
