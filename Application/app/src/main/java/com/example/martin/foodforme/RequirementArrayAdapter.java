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
public class RequirementArrayAdapter extends ArrayAdapter<Product> {

    private Context context;
    private int resource;
    private ArrayList<Product> products;
    private LayoutInflater inflater;
    private AccountDB accountDB;

    private static final int haveNoneColor = Color.rgb(255, 178, 178);
    private static final int haveSomeColor = Color.rgb(255, 255, 178);
    private static final int haveAllColor = Color.rgb(178, 255, 178);

    private int[] colors = {haveAllColor, haveSomeColor, haveNoneColor};

    public RequirementArrayAdapter (Context context, int resource, ArrayList products, AccountDB accountDB)
    {
        super(context, resource, products);
        this.context=context;
        this.resource=resource;
        this.products=products;
        this.accountDB = accountDB;
        inflater=((Activity)context).getLayoutInflater();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        View row = inflater.inflate(resource,parent,false);

        TextView title = (TextView)row.findViewById(R.id.title);
        TextView number = (TextView)row.findViewById(R.id.number);

        title.setText(products.get(position).getName());
        number.setText(products.get(position).getAmount());

        row.setBackgroundColor(colors[accountDB.getRequirementStatus(Integer.parseInt(products.get(position).getAmount()), products.get(position).getCode())]);

        return row;
    }



}