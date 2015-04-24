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
public class ShoppingArrayAdapter extends ArrayAdapter<Product> {

    private Context context;
    private int resource;
    private ArrayList<Product> products;
    LayoutInflater inflater;

    int requiredColor = Color.rgb(255, 178, 178);
    int manualColor = Color.rgb(255, 255, 178);

    public ShoppingArrayAdapter (Context context, int resource, ArrayList products)
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
        TextView number = (TextView)row.findViewById(R.id.number);

        title.setText(products.get(position).getName());
        number.setText(products.get(position).getAmount());

        if(products.get(position).getCode().equals("0")) row.setBackgroundColor(manualColor);
        else row.setBackgroundColor(requiredColor);

        return row;
    }



}