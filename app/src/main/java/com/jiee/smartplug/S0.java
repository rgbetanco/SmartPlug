package com.jiee.smartplug;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.media.Image;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.w3c.dom.Text;

public class S0 extends AppCompatActivity {

    final Context context = this;
    final Activity activity = this;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_s0);

        Toolbar toolbar = (Toolbar) findViewById(R.id.top_toolbar);
        setSupportActionBar(toolbar);
        this.getSupportActionBar().setDisplayShowTitleEnabled(false);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        ImageView img_view1 = (ImageView)findViewById(R.id.img_view1);
        img_view1.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                startActivity(new Intent(context, S1.class));
                finish();
            }
        });

        TextView txt_changePassword = (TextView)findViewById(R.id.txt_changePassword);
        txt_changePassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(context, S1.class));
                finish();
            }
        });

        TextView txt_view1 = (TextView)findViewById(R.id.id_logout);
        txt_view1.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                final YesNoDialog cd = new YesNoDialog(activity, 1);   // 1 = logout
                cd.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                cd.show();
            }
        });

    }

    public boolean onOptionsItemSelected(MenuItem item){
        Intent myIntent = new Intent(getApplicationContext(), ListDevices.class);
        startActivityForResult(myIntent, 0);
        return true;
    }
}
