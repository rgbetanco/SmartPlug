package com.jiee.smartplug;

/** TO DO:
 *
 /*
 S1.java
 Author: Chinsoft Ltd. | www.chinsoft.com
 This class allow the user to change the account password. The required parameters are old password, new password and re-entry the new password.
 */

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.media.Image;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.daimajia.swipe.SwipeLayout;
import com.jiee.smartplug.utils.HTTPHelper;
import com.jiee.smartplug.utils.Miscellaneous;

import java.security.spec.ECField;

public class S1 extends AppCompatActivity {

    EditText oldpass;
    EditText newpass;
    EditText confirmpass;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_s1);
        final Context context = this;

        TextView toolbar_text = (TextView)findViewById(R.id.sub_toolbar_red);
        toolbar_text.setText(R.string.title_changePassword);

        Toolbar toolbar = (Toolbar) findViewById(R.id.top_toolbar);
        setSupportActionBar(toolbar);
        this.getSupportActionBar().setDisplayShowTitleEnabled(false);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        oldpass = (EditText)findViewById(R.id.txt_oldPassword);
        newpass = (EditText)findViewById(R.id.txt_newPassword);
        confirmpass = (EditText)findViewById(R.id.txt_confirmPassword);

        Button btn_settings = (Button)findViewById(R.id.btn_settings);
        btn_settings.getBackground().setAlpha(64);
        btn_settings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                final String pwdNew = newpass.getText().toString().trim();
                final String pwdNewConfirm = confirmpass.getText().toString().trim();

                if(Miscellaneous.validatePassword( S1.this, pwdNew, pwdNewConfirm ) ) {
                    HTTPHelper httpHelper = new HTTPHelper(S1.this);

                    try {
                        if( !httpHelper.resetPassword( oldpass.getText().toString(), pwdNew) ) {
                            throw new Exception();
                        }

                        //instantiate dialog
                        final CustomDialog cd = new CustomDialog(S1.this, 3);   // 1 = create Account
                        cd.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                        cd.show();

                    } catch (Exception e){
                        e.printStackTrace();
                        Toast.makeText(S1.this, getResources().getString(R.string.password_change_failed), Toast.LENGTH_SHORT).show();
                    }
                }

            }
        });

    }

    /**
     * react to the user tapping the back/up icon in the action bar
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                // this takes the user 'back', as if they pressed the left-facing triangle icon on the main android toolbar.
                // if this doesn't work as desired, another possibility is to call `finish()` here.
                onBackPressed();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }}
