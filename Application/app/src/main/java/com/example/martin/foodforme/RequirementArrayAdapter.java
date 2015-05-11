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
public class RequirementArrayAdapter extends ArrayAdapter<Product> {

    private Context context;
    private int resource;
    private ArrayList<Product> products;
    private LayoutInflater inflater;
    private AccountDB accountDB;

    private static int haveAllBar;
    private static int haveSomeBar;
    private static int haveNoneBar;

    private int[] bars = new int[3];

    public RequirementArrayAdapter (Context context, int resource, ArrayList products, AccountDB accountDB)
    {
        super(context, resource, products);
        this.context=context;
        this.resource=resource;
        this.products=products;
        this.accountDB = accountDB;
        inflater=((Activity)context).getLayoutInflater();
        haveAllBar = (R.drawable.bargreen);
        haveSomeBar = (R.drawable.baryellow);
        haveNoneBar = (R.drawable.barred);
        bars[0] = haveAllBar;
        bars[1] = haveSomeBar;
        bars[2] = haveNoneBar;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        View row = inflater.inflate(resource,parent,false);

        TextView title = (TextView)row.findViewById(R.id.title);
        TextView number = (TextView)row.findViewById(R.id.number);

        title.setText(products.get(position).getName());
        number.setText(products.get(position).getAmount());

        row.setBackgroundResource(bars[accountDB.getRequirementStatus(products.get(position))]);

        return row;
    }



}