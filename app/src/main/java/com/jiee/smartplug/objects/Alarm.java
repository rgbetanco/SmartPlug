package com.jiee.smartplug.objects;

/**
 * Created by ronaldgarcia on 28/12/15.
 */
public class Alarm {

    public Alarm(){}

    int alarm_id;
    String device_id;
    int service_id;
    int dow;
    int init_hour;
    int init_minute;
    int end_hour;
    int end_minute;
    int init_ir;
    int end_ir;
    int snooze;

    public int getInit_ir() {
        return init_ir;
    }

    public void setInit_ir(int init_ir) {
        this.init_ir = init_ir;
    }

    public int getEnd_ir() {
        return end_ir;
    }

    public void setEnd_ir(int end_ir) {
        this.end_ir = end_ir;
    }

    public int getAlarm_id() {
        return alarm_id;
    }

    public void setAlarm_id(int alarm_id) {
        this.alarm_id = alarm_id;
    }


    public String getDevice_id() {
        return device_id;
    }

    public void setDevice_id(String device_id) {
        this.device_id = device_id;
    }

    public int getService_id() {
        return service_id;
    }

    public void setService_id(int service_id) {
        this.service_id = service_id;
    }

    public int getDow() {
        return dow;
    }

    public void setDow(int dow) {
        this.dow = dow;
    }

    public int getInit_hour() {
        return init_hour;
    }

    public void setInit_hour(int init_hour) {
        this.init_hour = init_hour;
    }

    public int getInit_minute() {
        return init_minute;
    }

    public void setInit_minute(int init_minute) {
        this.init_minute = init_minute;
    }

    public int getEnd_hour() {
        return end_hour;
    }

    public void setEnd_hour(int end_hour) {
        this.end_hour = end_hour;
    }

    public int getEnd_minute() {
        return end_minute;
    }

    public void setEnd_minute(int end_minute) {
        this.end_minute = end_minute;
    }

    public int getSnooze() {
        return snooze;
    }

    public void setSnooze(int snooze) {
        this.snooze = snooze;
    }
}
