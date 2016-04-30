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
import com.jiee.smartplug.utils.MySQLHelper;

import java.util.List;

/**
 * Created by ronaldgarcia on 29/12/15.
 */
public class ListAlarmsAdapter extends ArrayAdapter<AlarmList> {
    private final Context context;
    private final List<AlarmList> values;
    MySQLHelper sql;

    public ListAlarmsAdapter(Context c, List<AlarmList> values){
        super(c, R.layout.alarmrowlayout, values);
        this.context = c;
        this.values = values;

    }

    @Override
    public View getView(final int position, View v, ViewGroup vg){
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View row = inflater.inflate(R.layout.alarmrowlayout, vg, false);

        ImageButton btn_close = (ImageButton)row.findViewById(R.id.btn_warn_close);
        btn_close.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sql = new MySQLHelper(context);
                sql.deleteAlarmData(values.get(position).getAlarm_id());
                sql.close();

            }
        });


        TextView txt_alarm = (TextView)row.findViewById(R.id.txt_alarm);
        txt_alarm.setText(values.get(position).getName());

        return row;
    }
}
