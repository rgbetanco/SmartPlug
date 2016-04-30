package com.jiee.smartplug;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.jiee.smartplug.utils.HTTPHelper;

import org.json.JSONObject;

import java.util.Locale;

public class CreateAccount extends AppCompatActivity {

    final Activity context = this;
    boolean response;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_account);
        Toolbar toolbar = (Toolbar) findViewById(R.id.top_toolbar);
        setSupportActionBar(toolbar);
        this.getSupportActionBar().setDisplayShowTitleEnabled(false);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        ImageButton btn_settings = (ImageButton)findViewById(R.id.btn_settings);
        btn_settings.setVisibility(View.GONE);

        TextView toolbar_text = (TextView) findViewById(R.id.sub_toolbar);
        toolbar_text.setText("Create Account");

        final EditText txt_email = (EditText) findViewById(R.id.txt_email);
        final EditText txt_username = (EditText) findViewById(R.id.txt_username);
        final EditText txt_password = (EditText) findViewById(R.id.txt_password);
        final EditText txt_confirm_password = (EditText) findViewById(R.id.txt_confirm_password);

        txt_email.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (!hasFocus){
                    hideKeyboard(v);
                }
            }
        });

        txt_username.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if(!hasFocus){
                    hideKeyboard(v);
                }
            }
        });

        txt_password.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if(!hasFocus){
                    hideKeyboard(v);
                }
            }
        });

        txt_confirm_password.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if(!hasFocus){
                    hideKeyboard(v);
                }
            }
        });

        Button btn_submit = (Button) findViewById(R.id.btn_submit);
        btn_submit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(txt_confirm_password.getText().toString().equals(txt_password.getText().toString())){
                    HTTPHelper http = new HTTPHelper(CreateAccount.this);
                    try {
                        response = http.createAccount("newuser?user="+txt_username.getText().toString().trim()+"&pwd="+txt_password.getText().toString().trim()+"&email="+txt_email.getText().toString().trim()+"&hl="+ Locale.getDefault().getLanguage());
                        if(response){
                            Toast.makeText(context, getResources().getString(R.string.account_sent), Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(context, getResources().getString(R.string.create_account_error), Toast.LENGTH_SHORT).show();
                        }
                    } catch (Exception e){
                        e.printStackTrace();
                    }
                } else {
                    Toast.makeText(context, getResources().getString(R.string.password_compare_error), Toast.LENGTH_SHORT).show();
                }
            }
        });

    }


    //hide the keyboard when the edit text field loose focus
    public void hideKeyboard(View view) {
        InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(Activity.INPUT_METHOD_SERVICE);
        inputMethodManager.toggleSoftInput(InputMethodManager.HIDE_IMPLICIT_ONLY, 0);
    }

}
