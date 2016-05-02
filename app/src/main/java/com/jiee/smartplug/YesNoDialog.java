package com.jiee.smartplug;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.jiee.smartplug.services.RegistrationIntentService;
import com.jiee.smartplug.services.SmartConfigService;
import com.jiee.smartplug.utils.HTTPHelper;
import com.jiee.smartplug.utils.Miscellaneous;
import com.jiee.smartplug.utils.MySQLHelper;
import com.jiee.smartplug.utils.NetworkUtil;

import java.util.Locale;

/**
 * Created by ronaldgarcia on 2/12/15.
 */
public class YesNoDialog extends Dialog implements View.OnClickListener {

    public Dialog d;
    public Button ok;
    public Button cancel;
    int type;
    int alarm;
    String device_id;
    int service_id;
    MySQLHelper sql;
    HTTPHelper http;

    public YesNoDialog(Activity a, int type){        // 1 = S0 , 2 = M1, 3 = M2A_item_settings (delete), 4 = delete timer
        super(a);
        this.type = type;
        http = new HTTPHelper(a);
    }

    public YesNoDialog(Activity a, int type, int alarm){        // 1 = S0 , 2 = M1, 3 = M2A_item_settings (delete), 4 = delete timer
        super(a);
        this.type = type;
        this.alarm = alarm;
    }

    public YesNoDialog(Activity a, int type, String device_id, int service_id ){        // 1 = S0 , 2 = M1
        super(a);
        this.type = type;
        this.device_id = device_id;
        this.service_id = service_id;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.yesno_dialog);

        sql = HTTPHelper.getDB(getContext());

        TextView sub_toolbar = (TextView) findViewById(R.id.sub_toolbar);
        if(type == 1) {
            sub_toolbar.setText(R.string.title_logout);
        }
        if(type == 2){
            sub_toolbar.setText(R.string.no_timer_set);
        }
        if(type == 3){
            sub_toolbar.setText(R.string.title_removeAndReset);
        }
        if(type == 4){
            sub_toolbar.setText(R.string.title_removeAction);
        }
        ok = (Button) findViewById(R.id.btn_yes);
        cancel = (Button) findViewById(R.id.btn_no);
        ok.setOnClickListener(this);
        cancel.setOnClickListener(this);

        TextView dialog_content = (TextView) findViewById(R.id.dialog_content);
        if(type == 1) {
            dialog_content.setText(R.string.msg_logoutBtn);
        }
        if(type == 2){
            dialog_content.setText(R.string.add_timer);
        }
        if(type == 3){
            dialog_content.setText(R.string.msg_removeAndResetBtn);
        }
        if(type == 4){
            dialog_content.setText(R.string.msg_removeActionBtn);
        }
    }

    @Override
    public void onClick(View v){
        switch (v.getId()) {
            case R.id.btn_yes:
                if(type == 1){
                    Miscellaneous.logout(getContext(), Miscellaneous.LogoutType.NO_WARNING);
                }
                if(type == 2){
                    Intent i = new Intent(getContext(), S3.class);
                    i.putExtra("device_id", device_id);
                    i.putExtra("service_id", service_id);
                    getContext().startActivity(i);
                //    c.finish();
                }
                if(type == 3){
                    if (M1.mac != null) {
                        sql.deletePlugDataByID(M1.mac);
                    }
                    getOwnerActivity().finish();
                }
                if(type == 4){
                    sql.deleteAlarmData(alarm);
                    Intent i = new Intent("alarm_list_changed");
                    getContext().sendBroadcast(i);
                }
                break;
            case R.id.btn_no:
                dismiss();
                break;
            default:
                break;
        }
        dismiss();
    }
}
