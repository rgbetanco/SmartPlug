package com.jiee.smartplug;

import android.content.Context;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.SystemClock;
import android.util.Log;

import com.jiee.smartplug.objects.JSmartPlug;
import com.jiee.smartplug.utils.UDPCommunication;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceEvent;
import javax.jmdns.ServiceListener;
import javax.jmdns.ServiceTypeListener;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

public class mDNSsearch implements ServiceListener, ServiceTypeListener {

    private ArrayList<JSmartPlug> plugs = new ArrayList<JSmartPlug>();
    private JmDNS jmdns;
    WifiManager wm;
    private WifiManager.MulticastLock multicastLock;
    public static final String SERVICE_TYPE = "_http._tcp.";
    public static final String SMARTCONFIG_IDENTIFIER = "CC3200";
    Context l;
    //UDPCommunication udp = new UDPCommunication();

    public mDNSsearch(Context c) {
        this.l = c;
    }

    public List<JSmartPlug> getDeviceList() {
        return plugs;
    }

    public void clearPlugArray(){
        plugs.clear();
    }

    public void init() {
        wm = (WifiManager)l.getSystemService(Context.WIFI_SERVICE);
        multicastLock = wm.createMulticastLock(getClass().getName());
        multicastLock.setReferenceCounted(true);
    }

    public void startDiscovery() {

        final InetAddress deviceIpAddress = getDeviceIpAddress(wm);
        if (!multicastLock.isHeld()){
            multicastLock.acquire();
        } else {
            System.out.println(" Muticast lock already held...");
        }
        try {
            jmdns = JmDNS.create(deviceIpAddress, "SmartConfig");
            jmdns.addServiceTypeListener(this);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (jmdns != null) {
                System.out.println("discovering services");
            }
        }
    }

    public void stopDiscovery() {
        try {

            if (multicastLock.isHeld()){
                multicastLock.release();
            } else {
                System.out.println("Multicast lock already released");
            }
            try {
                jmdns.unregisterAllServices();
                jmdns.close();
                jmdns = null;
                System.out.println("MDNS discovery stopped");
            } catch (Exception ex){
                ex.printStackTrace();
            }
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public void lookForNewDevice() {
        try {
            init();
            startDiscovery();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private InetAddress getDeviceIpAddress(WifiManager wifi) {
        InetAddress result = null;
        try {
            // default to Android localhost
            result = InetAddress.getByName("10.0.0.2");
            // figure out our wifi address, otherwise bail
            WifiInfo wifiinfo = wifi.getConnectionInfo();
            int intaddr = wifiinfo.getIpAddress();
            byte[] byteaddr = new byte[] { (byte) (intaddr & 0xff), (byte) (intaddr >> 8 & 0xff), (byte) (intaddr >> 16 & 0xff), (byte) (intaddr >> 24 & 0xff) };
            result = InetAddress.getByAddress(byteaddr);
        } catch (UnknownHostException ex) {
            Log.w("MDNSHelper", String.format("getDeviceIpAddress Error: %s", ex.getMessage()));
        }

        return result;
    }

    @Override
    public void serviceAdded(ServiceEvent service) {}

    @Override
    public void serviceRemoved(ServiceEvent service) {}

    @Override
    public void serviceResolved(ServiceEvent service) {
        if (service.getName().equals(SMARTCONFIG_IDENTIFIER)){
            System.out.println("server: " + service.getInfo().getServer());
            try {
                System.out.println(service.getInfo().getServer());
                JSmartPlug ob = new JSmartPlug();
                ob.setName(service.getName().toString());
                ob.setServer(service.getInfo().getServer());
                ob.setIp(service.getInfo().getHostAddresses()[0]);
                //udp.runUdpClient(service.getInfo().getServer(), "ID?");  // this need to be change to Chin's protocol
            //    ob.setId(udp.runUdpServer());
                SystemClock.sleep(200);
                plugs.add(ob);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void serviceTypeAdded(ServiceEvent event) {
        // TODO Auto-generated method stub
        if (event.getType().contains(SERVICE_TYPE)) {
            jmdns.addServiceListener(event.getType(), this);
        }
    }

    @Override
    public void subTypeForServiceTypeAdded(ServiceEvent event) {}
}
