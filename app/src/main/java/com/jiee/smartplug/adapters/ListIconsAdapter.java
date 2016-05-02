package com.jiee.smartplug.adapters;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;

import com.jiee.smartplug.R;
import com.jiee.smartplug.R2_EditItem;
import com.jiee.smartplug.utils.MySQLHelper;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.zip.Inflater;

/**
 * Created by ronaldgarcia on 11/2/16.
 */
public class ListIconsAdapter extends BaseAdapter {
    Activity a;
    MySQLHelper sql;
    ArrayList<String> icons = new ArrayList<>();
    ImageView iv;

    final LayoutInflater inflate;

    public ListIconsAdapter(Activity a){
        this.a = a;
        inflate = (LayoutInflater)a.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        populateIcons();
    }

    @Override
    public int getCount() {
        return icons.size();
    }

    @Override
    public Object getItem(int position) {
        return icons.get(position).toString();
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        if(convertView == null){
            convertView = inflate.inflate(R.layout.list_icons, parent, false);
        }

        iv = (ImageView)convertView.findViewById(R.id.iconImage);

        Picasso.with(a).load(icons.get(position).toString()).into(iv);

        iv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent();
                i.putExtra("url", icons.get(position).toString());
                a.setResult(R2_EditItem.SELECT_ICON_CODE, i);
                a.finish();
            }
        });

        return convertView;
    }

    private void populateIcons(){
        sql = new MySQLHelper(a);
        Cursor c = sql.getIcons();
        if(c.getCount() > 0) {
            c.moveToFirst();
            for(int i = 0; i < c.getCount(); i++) {
                icons.add(c.getString(2));
                c.moveToNext();
            }
        }
        c.close();
        sql.close();
    }
}
