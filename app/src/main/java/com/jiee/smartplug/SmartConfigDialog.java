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

import com.jiee.smartplug.services.SmartConfigService;
import com.jiee.smartplug.utils.NetworkUtil;

/** TO DO:
 *
 /*
 SmartConfigDialog.java
 Author: Chinsoft Ltd. | www.chinsoft.com
 This dialog allows the user to provide SSID and password to the device to connect to the WiFi rounter.
 */

public class SmartConfigDialog extends Dialog implements View.OnClickListener {

    public Activity c;
    public Dialog d;
    public Button ok;
    public Button cancel;

    EditText txt_ssid;
    EditText txt_pass;

    public SmartConfigDialog(Activity a){        // 1 = create account , 2 = reset password
        super(a);
        this.c = a;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.smartconfig_dialog);

        TextView sub_toolbar = (TextView) findViewById(R.id.sub_toolbar);
        sub_toolbar.setText(R.string.startSmartConfig);

        ok = (Button) findViewById(R.id.btn_ok);
        cancel = (Button) findViewById(R.id.btn_cancel);
        ok.setOnClickListener(this);
        cancel.setOnClickListener(this);

        txt_ssid = (EditText) findViewById(R.id.txt_ssid);
        txt_ssid.setText(NetworkUtil.getWifiName(c));
        txt_pass = (EditText) findViewById(R.id.txt_password);

        TextView dialog_content = (TextView) findViewById(R.id.dialog_content);

        dialog_content.setText(R.string.account_sent);
    }

    @Override
    public void onClick(View v){
        switch (v.getId()) {
            case R.id.btn_ok:
                if(txt_pass.getText().toString().equals("")){
                    Toast.makeText(c, "Password cannot be blank", Toast.LENGTH_SHORT).show();
                } else {
                    Intent scintent = new Intent(c, SmartConfigService.class);
                    scintent.putExtra("ssid", txt_ssid.getText().toString());
                    scintent.putExtra("pass", txt_pass.getText().toString());
                    c.startService(scintent);
                    //show a spinning wheel before going to next screen
                }
                break;
            case R.id.btn_cancel:
                dismiss();
                break;
            default:
                break;
        }
        dismiss();
    }
}
