package com.jiee.smartplug;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import com.jiee.smartplug.utils.HTTPHelper;

import org.apache.http.client.methods.HttpOptions;
import org.w3c.dom.Text;

public class ResetPassword extends AppCompatActivity {

    final Activity context = this;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reset_password);
        Toolbar toolbar = (Toolbar) findViewById(R.id.top_toolbar);
        setSupportActionBar(toolbar);
        ImageButton btn_settings = (ImageButton)findViewById(R.id.btn_settings);
        btn_settings.setVisibility(View.GONE);

        this.getSupportActionBar().setDisplayShowTitleEnabled(false);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        TextView toolbar_text = (TextView) findViewById(R.id.sub_toolbar);
        toolbar_text.setText(getResources().getString(R.string.title_activity_reset_password));
        toolbar_text.setTextColor(getResources().getColor(R.color.black));

        final EditText txt_username = (EditText) findViewById(R.id.txt_new_username);
        txt_username.setTextColor(getResources().getColor(R.color.black));

        txt_username.setOnFocusChangeListener(new View.OnFocusChangeListener() {
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
                HTTPHelper http = new HTTPHelper(ResetPassword.this);
                try {
                    http.forgotPassword(txt_username.getText().toString());
                } catch (Exception e){
                    e.printStackTrace();
                }
                //instantiate dialog
                final CustomDialog cd = new CustomDialog(context, 2);   // 1 = create Account
                cd.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                cd.show();
            }
        });

    }

    //hide the keyboard when the edit text field loose focus
    public void hideKeyboard(View view) {
        InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(Activity.INPUT_METHOD_SERVICE);
        inputMethodManager.toggleSoftInput(InputMethodManager.HIDE_IMPLICIT_ONLY, 0);
    }

}
