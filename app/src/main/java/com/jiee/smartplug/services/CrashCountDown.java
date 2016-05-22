package com.jiee.smartplug.services;

import android.app.Activity;
import android.content.Intent;
import android.util.Log;

/**
 * Created by ronaldgarcia on 24/2/16.
 */
public class CrashCountDown {

    int wait = 0;                   //wait in seconds
    Activity a;
    Thread timing;

    public CrashCountDown(Activity a){
        this.a = a;
    }

    public void setTimer(int waitParam){
        if(waitParam == 0){
            waitParam = 3;
        }
        wait = waitParam;
        timing = new Thread(new Runnable() {
            @Override
            public void run() {
                while(wait > 0){
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e){
                        break;
                    }
                    wait--;
                }
                if(wait == 0){
                    Intent i = new Intent("timer_crash_reached");
                    a.sendBroadcast(i);
                }
            }
        });
    }

    public void setMicroTimer(){

        timing = new Thread(new Runnable() {
            @Override
            public void run() {
                    try {
                        Thread.sleep(300);
                    } catch (InterruptedException e){
                        e.printStackTrace();
                    }
                    Intent i = new Intent("timer_crash_reached");
                    a.sendBroadcast(i);
            }
        });
    }

    public void startTimer() {

        if (!timing.isAlive()){
            timing.start();
        }

    }

    public void stopTimer(){
        try {
            timing.interrupt();
        } catch (Exception e){
            Log.i("TIME-OUT", "STOPPING TIMING ERROR");
        }
    }
}
