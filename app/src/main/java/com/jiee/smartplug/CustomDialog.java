package com.jiee.smartplug;

/* TO DO:
 *
/*
 CustomDialog.cpp
 Author: Chinsoft Ltd. | www.chinsoft.com
 Display different dialogs with messages depending on the calling activity.

 */

import android.app.Activity;
import android.app.Dialog;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;

import com.jiee.smartplug.utils.HTTPHelper;

/**
 * Created by ronaldgarcia on 2/12/15.
 */
public class CustomDialog extends Dialog implements View.OnClickListener {

    public Activity c;
    public Dialog d;
    public Button ok;
    private int local_type;

    public CustomDialog(Activity a, int type){        // 1 = create account , 2 = reset password
        super(a);
        this.c = a;
        local_type = type;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.custom_dialog);

        ok = (Button) findViewById(R.id.btn_ok);
        ok.setOnClickListener(this);

        TextView dialog_content = (TextView) findViewById(R.id.dialog_content);
        if(local_type == 1) {
            dialog_content.setText(R.string.msg_accountCreatedBtn);
        }
        if(local_type == 2) {
            dialog_content.setText(R.string.msg_passwordResetBtn);
        }

        if(local_type == 3) {
            dialog_content.setText(R.string.password_reseted);
        }

        TextView txt_toolbar = (TextView) findViewById(R.id.sub_toolbar);
        if (local_type == 1) {
            txt_toolbar.setText(R.string.create_account_dialog_text);
        }
        if (local_type == 2) {
            txt_toolbar.setText(R.string.title_passwordResetSent);
        }

        if (local_type == 3) {
            txt_toolbar.setText(R.string.title_resetPassword);
        }

    }

    @Override
    public void onClick(View v){
        switch (v.getId()) {
            case R.id.btn_ok:
                if(local_type != 2) {
                    c.finish();
                }
                break;
            default:
                break;
        }
        dismiss();
    }
}
