package com.jiee.smartplug;

/* TO DO: DELETE THIS FILE
 *
/*
 NewDeviceList.java
 Author: Chinsoft Ltd. | www.chinsoft.com

 */

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.jiee.smartplug.utils.HTTPHelper;
import com.jiee.smartplug.utils.MySQLHelper;

import java.util.Locale;

public class HelpView extends Activity {

    String pageData[];	//Stores the text to swipe.
    LayoutInflater inflater;	//Used to create individual pages
    ViewPager vp;	//Reference to class to swipe views
    HTTPHelper httpHelper;
    MySQLHelper mySQLHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_help_view);

        httpHelper = new HTTPHelper(this);
        mySQLHelper = HTTPHelper.getDB(this);

        //Get the data to be swiped through
        pageData=getResources().getStringArray(R.array.backgrounds);
        //get an inflater to be used to create single pages
        inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        //Reference ViewPager defined in activity
        vp=(ViewPager)findViewById(R.id.viewPager);
        //set the adapter that will create the individual pages
        vp.setAdapter(new MyPagesAdapter());
    }

    @Override
    protected void onDestroy(){
        super.onDestroy();

    }

    //Implement PagerAdapter Class to handle individual page creation
    class MyPagesAdapter extends PagerAdapter {
        @Override
        public int getCount() {
            //Return total pages, here one for each data item
            return pageData.length;
        }
        //Create the given page (indicated by position)
        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            View page = inflater.inflate(R.layout.page, null);
            RadioButton r1 = (RadioButton)page.findViewById(R.id.h1);
            RadioButton r2 = (RadioButton)page.findViewById(R.id.h2);
            RadioButton r3 = (RadioButton)page.findViewById(R.id.h3);
            RadioButton r4 = (RadioButton)page.findViewById(R.id.h4);
            RadioButton r5 = (RadioButton)page.findViewById(R.id.h5);
            RadioButton r6 = (RadioButton)page.findViewById(R.id.h6);
            RadioButton r7 = (RadioButton)page.findViewById(R.id.h7);
                    ((TextView) page.findViewById(R.id.textMessage)).setText(pageData[position]);
            TextView description = (TextView)page.findViewById(R.id.txt_description);
            TextView login = (TextView)page.findViewById(R.id.btn_login);
            login.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    finish();
                }
            });

            RelativeLayout relativeLayout = (RelativeLayout)page.findViewById(R.id.help_layout);
            ImageView img = (ImageView)page.findViewById(R.id.background_image);
         //   if(Locale.getDefault().getLanguage().equals("en")) {
                switch (position) {
                    case 0:
                        relativeLayout.setBackgroundColor(Color.parseColor("#e8b646"));
                        img.setBackground(getResources().getDrawable(R.drawable.help_1));
                        description.setText(getApplicationContext().getString(R.string.help1));
                        r1.setChecked(true);
                        r2.setChecked(false);
                        r3.setChecked(false);
                        r4.setChecked(false);
                        r5.setChecked(false);
                        r6.setChecked(false);
                        r7.setChecked(false);
                        login.setVisibility(View.GONE);
                        break;
                    case 1:
                        relativeLayout.setBackgroundColor(Color.parseColor("#ea794f"));
                        img.setBackground(getResources().getDrawable(R.drawable.help_2));
                        description.setText(getApplicationContext().getString(R.string.help2));
                        r1.setChecked(false);
                        r2.setChecked(true);
                        r3.setChecked(false);
                        r4.setChecked(false);
                        r5.setChecked(false);
                        r6.setChecked(false);
                        r7.setChecked(false);
                        login.setVisibility(View.GONE);
                        break;
                    case 2:
                        relativeLayout.setBackgroundColor(Color.parseColor("#de5360"));
                        img.setBackground(getResources().getDrawable(R.drawable.help_3));
                        description.setText(getApplicationContext().getString(R.string.help3));
                        r1.setChecked(false);
                        r2.setChecked(false);
                        r3.setChecked(true);
                        r4.setChecked(false);
                        r5.setChecked(false);
                        r6.setChecked(false);
                        r7.setChecked(false);
                        login.setVisibility(View.GONE);
                        break;
                    case 3:
                        relativeLayout.setBackgroundColor(Color.parseColor("#b7c45a"));
                        img.setBackground(getResources().getDrawable(R.drawable.help_9));
                        description.setText(getApplicationContext().getString(R.string.help4));
                        r1.setChecked(false);
                        r2.setChecked(false);
                        r3.setChecked(false);
                        r4.setChecked(true);
                        r5.setChecked(false);
                        r6.setChecked(false);
                        r7.setChecked(false);
                        login.setVisibility(View.GONE);
                        break;
                    case 4:
                        relativeLayout.setBackgroundColor(Color.parseColor("#52b66b"));
                        img.setBackground(getResources().getDrawable(R.drawable.help_4));
                        description.setText(getApplicationContext().getString(R.string.help5));
                        r1.setChecked(false);
                        r2.setChecked(false);
                        r3.setChecked(false);
                        r4.setChecked(false);
                        r5.setChecked(true);
                        r6.setChecked(false);
                        r7.setChecked(false);
                        login.setVisibility(View.GONE);
                        break;
                    case 5:
                        relativeLayout.setBackgroundColor(Color.parseColor("#54b6d0"));
                        img.setBackground(getResources().getDrawable(R.drawable.help_5));
                        description.setText(getApplicationContext().getString(R.string.help6));
                        r1.setChecked(false);
                        r2.setChecked(false);
                        r3.setChecked(false);
                        r4.setChecked(false);
                        r5.setChecked(false);
                        r6.setChecked(true);
                        r7.setChecked(false);
                        login.setVisibility(View.GONE);
                        break;

                    case 6:
                        relativeLayout.setBackgroundColor(Color.parseColor("#6491c1"));
                        img.setBackground(getResources().getDrawable(R.drawable.help_6));
                        description.setText(getApplicationContext().getString(R.string.help7));
                        r1.setChecked(false);
                        r2.setChecked(false);
                        r3.setChecked(false);
                        r4.setChecked(false);
                        r5.setChecked(false);
                        r6.setChecked(false);
                        r7.setChecked(true);
                        login.setVisibility(View.VISIBLE);
                        break;
                }
     //       }
/*
            if(Locale.getDefault().getLanguage().equals("zh")) {
                switch (position) {
                    case 0:
                        relativeLayout.setBackgroundColor(Color.parseColor("#e8b646"));
                        img.setBackground(getResources().getDrawable(R.drawable.help_1));
                        description.setText(getApplicationContext().getString(R.string.help1));
                        r1.setChecked(true);
                        r2.setChecked(false);
                        r3.setChecked(false);
                        r4.setChecked(false);
                        r5.setChecked(false);
                        r6.setChecked(false);
                        r7.setChecked(false);
                        login.setVisibility(View.GONE);
                        break;
                    case 1:
                        relativeLayout.setBackgroundColor(Color.parseColor("#ea794f"));
                        img.setBackground(getResources().getDrawable(R.drawable.help_2));
                        description.setText(getApplicationContext().getString(R.string.help2));
                        r1.setChecked(false);
                        r2.setChecked(true);
                        r3.setChecked(false);
                        r4.setChecked(false);
                        r5.setChecked(false);
                        r6.setChecked(false);
                        r7.setChecked(false);
                        login.setVisibility(View.GONE);
                        break;
                    case 2:
                        relativeLayout.setBackgroundColor(Color.parseColor("#de5360"));
                        img.setBackground(getResources().getDrawable(R.drawable.help_3));
                        description.setText(getApplicationContext().getString(R.string.help3));
                        r1.setChecked(false);
                        r2.setChecked(false);
                        r3.setChecked(true);
                        r4.setChecked(false);
                        r5.setChecked(false);
                        r6.setChecked(false);
                        r7.setChecked(false);
                        login.setVisibility(View.GONE);
                        break;
                    case 3:
                        relativeLayout.setBackgroundColor(Color.parseColor("#b7c45a"));
                        img.setBackground(getResources().getDrawable(R.drawable.help_7));
                        description.setText(getApplicationContext().getString(R.string.help4));
                        r1.setChecked(false);
                        r2.setChecked(false);
                        r3.setChecked(false);
                        r4.setChecked(true);
                        r5.setChecked(false);
                        r6.setChecked(false);
                        r7.setChecked(false);
                        login.setVisibility(View.GONE);
                        break;
                    case 4:
                        relativeLayout.setBackgroundColor(Color.parseColor("#52b66b"));
                        img.setBackground(getResources().getDrawable(R.drawable.help_4));
                        description.setText(getApplicationContext().getString(R.string.help5));
                        r1.setChecked(false);
                        r2.setChecked(false);
                        r3.setChecked(false);
                        r4.setChecked(false);
                        r5.setChecked(true);
                        r6.setChecked(false);
                        r7.setChecked(false);
                        login.setVisibility(View.GONE);
                        break;
                    case 5:
                        relativeLayout.setBackgroundColor(Color.parseColor("#54b6d0"));
                        img.setBackground(getResources().getDrawable(R.drawable.help_5));
                        description.setText(getApplicationContext().getString(R.string.help6));
                        r1.setChecked(false);
                        r2.setChecked(false);
                        r3.setChecked(false);
                        r4.setChecked(false);
                        r5.setChecked(false);
                        r6.setChecked(true);
                        r7.setChecked(false);
                        login.setVisibility(View.GONE);
                        break;

                    case 6:
                        relativeLayout.setBackgroundColor(Color.parseColor("#6491c1"));
                        img.setBackground(getResources().getDrawable(R.drawable.help_6));
                        description.setText(getApplicationContext().getString(R.string.help7));
                        r1.setChecked(false);
                        r2.setChecked(false);
                        r3.setChecked(false);
                        r4.setChecked(false);
                        r5.setChecked(false);
                        r6.setChecked(false);
                        r7.setChecked(true);
                        login.setVisibility(View.VISIBLE);
                        break;
                }
            }
*/
            //img.setBackground(getResources().getDrawable(R.drawable.help_1_en));
            img.setScaleType(ImageView.ScaleType.FIT_CENTER);
            //Add the page to the front of the queue
            ((ViewPager) container).addView(page, 0);
            return page;
        }
        @Override
        public boolean isViewFromObject(View arg0, Object arg1) {
            //See if object from instantiateItem is related to the given view
            //required by API
            return arg0==(View)arg1;
        }
        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            ((ViewPager) container).removeView((View) object);
            object=null;
        }
    }
}
