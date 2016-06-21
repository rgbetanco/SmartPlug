package com.jiee.smartplug.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.jiee.smartplug.R;
import com.jiee.smartplug.objects.AlarmList;
import com.jiee.smartplug.utils.HTTPHelper;
import com.jiee.smartplug.utils.MySQLHelper;

import java.util.List;

/** TO DO:
 *
 /*
 ListAlarmAdapter.java
 Author: Chinsoft Ltd. | www.chinsoft.com

 Adapter for the M1SnoozeDialog dialog's list

 */

public class ListAlarmsAdapter extends ArrayAdapter<AlarmList> {
    private final List<AlarmList> values;
    MySQLHelper sql;

    private final LayoutInflater inflater;

    public ListAlarmsAdapter(Context c, List<AlarmList> values){
        super(c, R.layout.alarmrowlayout, values);
        this.values = values;

        inflater = (LayoutInflater)c.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public View getView(final int position, View v, ViewGroup vg){
        View row = inflater.inflate(R.layout.alarmrowlayout, vg, false);

        ImageButton btn_close = (ImageButton)row.findViewById(R.id.btn_warn_close);
        btn_close.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sql = HTTPHelper.getDB(getContext());
                sql.deleteAlarmData(values.get(position).getAlarm_id());
            }
        });


        TextView txt_alarm = (TextView)row.findViewById(R.id.txt_alarm);
        txt_alarm.setText(values.get(position).getName());

        return row;
    }
}
