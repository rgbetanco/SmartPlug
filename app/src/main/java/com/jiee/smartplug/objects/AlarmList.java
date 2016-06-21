package com.jiee.smartplug.objects;


public class AlarmList {
    public AlarmList(){}
    String name;
    int alarm_id;
    int background;

    public int getBackground() {
        return background;
    }

    public void setBackground(int background) {
        this.background = background;
    }

    public int getAlarm_id() {
        return alarm_id;
    }

    public void setAlarm_id(int device_id) {
        this.alarm_id = device_id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
