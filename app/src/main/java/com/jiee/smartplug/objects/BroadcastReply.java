package com.jiee.smartplug.objects;

public class BroadcastReply {

    int id;
    int relay;
    int nightled;
    int co;
    int ha;

    public BroadcastReply(){}

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getRelay() {
        return relay;
    }

    public void setRelay(int relay) {
        this.relay = relay;
    }

    public int getNightled() {
        return nightled;
    }

    public void setNightled(int nightled) {
        this.nightled = nightled;
    }

    public int getCo() {
        return co;
    }

    public void setCo(int co) {
        this.co = co;
    }

    public int getHa() {
        return ha;
    }

    public void setHa(int ha) {
        this.ha = ha;
    }

}
