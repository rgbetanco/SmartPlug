package com.jiee.smartplug;

/** TO DO:
 *
 /*
 IRListCommands.java
 Author: Chinsoft Ltd. | www.chinsoft.com
 List the IR codes to display in the S3 activity. The S3 activity allows the user to add alarms.
 */

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.Toast;

import com.jiee.smartplug.adapters.IRExpandableListAdapter;
import com.jiee.smartplug.utils.HTTPHelper;
import com.jiee.smartplug.utils.MySQLHelper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class IRListCommands extends Activity {

    ExpandableListAdapter listAdapter;
    ExpandableListView expListView;
    List<String> listDataHeader;
    HashMap<String, List<String>> listDataChild;
    MySQLHelper sql;
    int status = -1;
    Button btn_subtoolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sql = HTTPHelper.getDB(this);

        setContentView(R.layout.activity_irlist_commands);

        Intent i = getIntent();
        status = i.getIntExtra("status", -1);

        // get the listview
        expListView = (ExpandableListView) findViewById(R.id.lvExp);

        btn_subtoolbar = (Button) findViewById(R.id.subtoolbar);

        // preparing list data
        prepareListData();

        listAdapter = new IRExpandableListAdapter(this, listDataHeader, listDataChild);

        // setting list adapter
        expListView.setAdapter(listAdapter);

        expListView.setOnChildClickListener(new ExpandableListView.OnChildClickListener() {

            @Override
            public boolean onChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition, long id) {
                Intent i = new Intent();
                i.putExtra("status", status);
                i.putExtra("group", listDataHeader.get(groupPosition));
                i.putExtra("irName", listDataChild.get(listDataHeader.get(groupPosition)).get(childPosition));
                setResult(3, i);
                finish();
                return false;
            }
        });

        if (status == 0){
            btn_subtoolbar.setText(R.string.select_start_command);
        } else {
            btn_subtoolbar.setText(R.string.select_end_command);
        }

    }

    private void prepareListData() {
        listDataHeader = new ArrayList<String>();
        listDataChild = new HashMap<String, List<String>>();

        Cursor c = sql.getIRGroupByMac(M1.mac);
        if (c.getCount() > 0){
            c.moveToFirst();
            for (int i = 0; i < c.getCount(); i++){
                listDataHeader.add(c.getString(1));
                ArrayList[] list = new ArrayList[200];
                list[i] = new ArrayList<String>();
                Cursor cur = sql.getIRCodesByGroup(c.getInt(4));
                if(cur.getCount() > 0){
                    cur.moveToFirst();
                    for (int j = 0; j < cur.getCount(); j++){
                        list[i].add(cur.getString(2));
                        cur.moveToNext();
                    }
                    cur.close();
                }
                listDataChild.put(listDataHeader.get(i), list[i]);
                c.moveToNext();
            }
        }
        c.close();

    }

}
