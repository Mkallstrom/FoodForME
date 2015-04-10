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
public class ListArrayAdapter extends ArrayAdapter<Product> {

    private Context context;
    private int resource;
    private ArrayList<Product> products;
    LayoutInflater inflater;

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

        title.setText(products.get(position).name);
        number.setText(products.get(position).expiryDate);
        int expiringIn = products.get(position).daysUntilExpired();
        if(expiringIn == 0)
        {
            remaining.setText("Expires today.");
        }
        else
        {
            if(expiringIn > 0)
            {
                remaining.setText("Expires in " + Integer.toString(expiringIn) + " days.");
            }
            else
            {
                remaining.setText("Expired " + Integer.toString(-expiringIn) + " days ago.");
            }
        }
        return row;
    }



}
