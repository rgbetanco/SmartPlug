package com.jiee.smartplug.objects;

public class JSmartPlug {
    int dbid;
    String name;
    String server;
    String id;
    String ip;
    String model;
    int buildno;
    int prot_ver;
    String hw_ver;
    String fw_ver;
    int fw_date;
    int flag;
    int relay;
    int hall_sensor;
    int nightlight;
    int co_sensor;
    String givenName;
    String icon;
    int snooze;

    public int getSnooze() {
        return snooze;
    }

    public void setSnooze(int snooze) {
        this.snooze = snooze;
    }


    public int getDbid() {
        return dbid;
    }

    public void setDbid(int dbid) {
        this.dbid = dbid;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public String getGivenName() {
        return givenName;
    }

    public void setGivenName(String givenName) {
        this.givenName = givenName;
    }

    public int getBackground() {
        return background;
    }

    public void setBackground(int background) {
        this.background = background;
    }

    int background;

    public int getRelay() {
        return relay;
    }

    public void setRelay(int relay) {
        this.relay = relay;
    }

    public int getHall_sensor() {
        return hall_sensor;
    }

    public void setHall_sensor(int hall_sensor) {
        this.hall_sensor = hall_sensor;
    }

    public int getNightlight() {
        return nightlight;
    }

    public void setNightlight(int nightlight) {
        this.nightlight = nightlight;
    }

    public int getCo_sensor() {
        return co_sensor;
    }

    public void setCo_sensor(int co_sensor) {
        this.co_sensor = co_sensor;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public int getBuildno() {
        return buildno;
    }

    public void setBuildno(int buildno) {
        this.buildno = buildno;
    }

    public int getProt_ver() {
        return prot_ver;
    }

    public void setProt_ver(int prot_ver) {
        this.prot_ver = prot_ver;
    }

    public String getHw_ver() {
        return hw_ver;
    }

    public void setHw_ver(String hw_ver) {
        this.hw_ver = hw_ver;
    }

    public String getFw_ver() {
        return fw_ver;
    }

    public void setFw_ver(String fw_ver) {
        this.fw_ver = fw_ver;
    }

    public int getFw_date() {
        return fw_date;
    }

    public void setFw_date(int fw_date) {
        this.fw_date = fw_date;
    }

    public int getFlag() {
        return flag;
    }

    public void setFlag(int flag) {
        this.flag = flag;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public JSmartPlug(){}

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getServer() {
        return server;
    }

    public void setServer(String server) {
        this.server = server;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

}
