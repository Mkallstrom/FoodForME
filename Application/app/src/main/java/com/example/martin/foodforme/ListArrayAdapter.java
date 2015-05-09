package com.example.martin.foodforme;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.ArrayList;

/**
 * Created by Martin on 2015-04-09.
 */
public class ListArrayAdapter extends ArrayAdapter<Product> {

    private Context context;
    private int resource;
    private ArrayList<Product> products;
    LayoutInflater inflater;
    private static final int expiredColor = Color.rgb(255, 178, 178);
    private static final int expiringShortColor = Color.rgb(255, 255, 178);
    private static final int expiringLongColor = Color.rgb(178, 255, 178);
    private static final int nonExpiringColor = Color.rgb(178, 178, 255);

    public ListArrayAdapter (Context context, int resource, ArrayList products)
    {
        super(context, resource, products);
        this.context=context;
        this.resource=resource;
        this.products=products;
        inflater=((Activity)context).getLayoutInflater();
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
        amount.setText(products.get(position).getAmount());
        int expiringIn = products.get(position).daysUntilExpired();

        if(products.get(position).expires())
        {
            if (expiringIn == 0) {
                remaining.setText("Expires today.");
                row.setBackgroundColor(expiringShortColor);
            } else {
                if (expiringIn > 0) {
                    remaining.setText("Expires in " + Integer.toString(expiringIn) + " days.");
                    if (expiringIn < 8) row.setBackgroundColor(expiringShortColor);
                    else row.setBackgroundColor(expiringLongColor);
                } else {
                    remaining.setText("Expired " + Integer.toString(-expiringIn) + " days ago.");
                    row.setBackgroundColor(expiredColor);
                }
            }
        }
        else
        {
            remaining.setText("");
            number.setText("");
            row.setBackgroundColor(nonExpiringColor);
        }
        return row;
    }


}
