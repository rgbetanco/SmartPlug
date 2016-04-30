package com.jiee.smartplug;

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

import java.security.spec.ECField;

public class S1 extends AppCompatActivity {

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

        newpass = (EditText)findViewById(R.id.txt_newPassword);
        confirmpass = (EditText)findViewById(R.id.txt_confirmPassword);

        Button btn_settings = (Button)findViewById(R.id.btn_settings);
        btn_settings.getBackground().setAlpha(64);
        btn_settings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                HTTPHelper httpHelper = new HTTPHelper(S1.this);
                try {
                    if(newpass.getText().toString().equals(confirmpass.getText().toString())) {
                        httpHelper.resetPassword(newpass.getText().toString());
                    } else {
                        Toast.makeText(S1.this, R.string.password_compare_error, Toast.LENGTH_SHORT).show();
                    }
                } catch (Exception e){
                    e.printStackTrace();
                }

                //instantiate dialog
                final CustomDialog cd = new CustomDialog(S1.this, 3);   // 1 = create Account
                cd.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                cd.show();

            }
        });

    }

    public boolean onOptionsItemSelected(MenuItem item){
        Intent myIntent = new Intent(getApplicationContext(), S0.class);
        startActivityForResult(myIntent, 0);
        return true;
    }
}
