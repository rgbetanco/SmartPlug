package com.jiee.smartplug.services;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.os.SystemClock;
import android.util.Log;

import com.jiee.smartplug.objects.JSmartPlug;
import com.jiee.smartplug.utils.MySQLHelper;
import com.jiee.smartplug.utils.UDPCommunication;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceEvent;
import javax.jmdns.ServiceListener;
import javax.jmdns.ServiceTypeListener;

public class mDNSservice extends Service implements ServiceListener, ServiceTypeListener {

    private final IBinder mBinder = new MyBinder();
    private ArrayList<JSmartPlug> plugs = new ArrayList<JSmartPlug>();
    private JmDNS jmdns;
    WifiManager wm;
    private WifiManager.MulticastLock multicastLock;
    public static final String SERVICE_TYPE = "_http._tcp.";
    public static final String SMARTCONFIG_IDENTIFIER = "JSPlug";
    //UDPCommunication udp = new UDPCommunication();
    boolean runThread = false;
    MySQLHelper sql = new MySQLHelper(this);
    int count_max = 10;
    Thread mDNSBroadcastThread;

    private WifiManager.MulticastLock lock;
    private NsdManager.DiscoveryListener discoveryListener;
    private NsdManager mNsdManager;
    private static final String DNS_TYPE = "_http._tcp.";
    //private JmDNS jmdns = null;
    private ServiceListener listener;

    public mDNSservice() {
       // clearPlugArray();
    }

    public List<JSmartPlug> getDeviceList() {
        System.out.println("Item size:" + plugs.size());
        return plugs;
    }

    public void clearPlugArray(){
        plugs.clear();
    }

    public void init() {
        clearPlugArray();
        wm = (WifiManager) getSystemService(Context.WIFI_SERVICE);
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
        runThread = false;
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
         //   e.printStackTrace();
        }
    }

   // Thread mDNSBroadcastThread;

    public void lookForNewDevice() {
        startDiscovery();
        /*
        mDNSBroadcastThread = new Thread(new Runnable() {
            public void run() {
                try {
                    while (count_max > 0) {
                        synchronized (this) {
                            try {
                                startDiscovery();
                             //   count_max--;
                                Thread.sleep(5000);
                            } catch (Exception ex) {
                                ex.printStackTrace();
                            }
                        }
                    }

                }catch(Exception e){
                    Log.i("UDP", "no longer listening for UDP broadcasts cause of error " + e.getMessage());
                }
            }
            });
        mDNSBroadcastThread.start();
        */
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
   //         Log.w("MDNSHelper", String.format("getDeviceIpAddress Error: %s", ex.getMessage()));
        }

        return result;
    }

    @Override
    public void serviceAdded(ServiceEvent service) {
        jmdns.requestServiceInfo(service.getType(), service.getName(), 1);
    }

    @Override
    public void serviceRemoved(ServiceEvent service) {
        if (service.getName().equals(SMARTCONFIG_IDENTIFIER)) {
            System.out.println("JSPlug removed");
            plugs.clear();
            //broadcast back
            Intent intent2 = new Intent("mDNS_Device_Removed");
            sendBroadcast(intent2);
        }
    }

    @Override
    public void serviceResolved(ServiceEvent service) {
        System.out.println("mDNS Finding all devices");

        if (service.getName().equals(SMARTCONFIG_IDENTIFIER)){
            System.out.println("JSPlug Found: "+service.getInfo().getHostAddresses()[0]);
            try {
                String plug_id = "";
                JSmartPlug ob = new JSmartPlug();
                boolean add = true;

                for (int i = 0; i < plugs.size(); i++){
                    if ((plugs.get(i).getServer().toString()).equals(service.getInfo().getServer().toString())) {
                        add = false;
                    } else {
                        add = true;
                    }
                }

                if (add) {
                    ob.setName(service.getName().toString());
                    ob.setServer(service.getInfo().getServer());
                    ob.setIp(service.getInfo().getHostAddresses()[0]);
                    short device_query_command = 0x0001;
                //    ob = udp.queryDevices(service.getInfo().getHostAddresses()[0], device_query_command, ob);
                    Cursor c = sql.getPlugData(ob.getId());
                    if((c.getCount() < 1)) {
                //        sql.insertPlug(ob, 0);
                     //   System.out.println("Device Inserted In the DB: "+service.getInfo().getHostAddresses()[0]);
                    }
                    if(!c.isClosed()){
                        c.close();
                    }

                    plugs.add(ob);
                    System.out.println("New Device Added");
                    //broadcast back
                    Intent intent1 = new Intent("mDNS_New_Device_Found");
                    sendBroadcast(intent1);

                } else {
                    System.out.println("Device already in the list");
                }

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

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
    //    init();
        runThread = true;
    //    startDiscovery();
    //    lookForNewDevice();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
			setUpDiscoInfo();
		} else {
            // Set up Bonjour
            Thread t = new Thread(new Runnable() {

                @Override
                public void run() {
                    setUpDiscoInfoPreJellyBean();
                }
            });
            t.start();
		}

        if (jmdns != null) {
            jmdns.addServiceListener(DNS_TYPE, listener);
        }

        if(mNsdManager!=null){
            mNsdManager.discoverServices(DNS_TYPE, NsdManager.PROTOCOL_DNS_SD, discoveryListener);
        }

        System.out.println("mDNS service started");
        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy(){
    //    stopDiscovery();
        if (jmdns != null) {
            try {
                jmdns.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (lock != null) {
            lock.release();
        }
    }


    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        return mBinder;
    }

    public class MyBinder extends Binder {
        public mDNSservice getService() {
            return mDNSservice.this;
        }
    }


    private void setUpDiscoInfo() {
        System.out.println("Setting up Bonjour");
        discoveryListener = new NsdManager.DiscoveryListener() {

            @Override
            public void onStopDiscoveryFailed(String serviceType, int errorCode) {
                // TODO Auto-generated method stub

            }

            @Override
            public void onStartDiscoveryFailed(String serviceType, int errorCode) {
                // TODO Auto-generated method stub

            }

            @Override
            public void onServiceLost(NsdServiceInfo serviceInfo) {
                // TODO Auto-generated method stub
                System.out.println("JSPlug removed");
            }

            @Override
            public void onServiceFound(NsdServiceInfo serviceInfo) {
                System.out.println("Service resolved: " + serviceInfo.getServiceName() + " host:" + serviceInfo.getHost() + " port:"
                        + serviceInfo.getPort() + " type:" + serviceInfo.getServiceType());
            }

            @Override
            public void onDiscoveryStopped(String serviceType) {
                // TODO Auto-generated method stub

            }

            @Override
            public void onDiscoveryStarted(String serviceType) {
                // TODO Auto-generated method stub

            }
        };
        mNsdManager = (NsdManager) getSystemService(Context.NSD_SERVICE);
        mNsdManager.discoverServices(DNS_TYPE, NsdManager.PROTOCOL_DNS_SD, discoveryListener);
    }

    private void setUpDiscoInfoPreJellyBean() {
        System.out.println("Setting up Bonjour Pre-JellyBean");
        listener = new ServiceListener() {

            @Override
            public void serviceResolved(ServiceEvent event) {
                System.out.println("Service resolved: " + event.getInfo().getQualifiedName() + " port:" + event.getInfo().getPort()
                        + " domain:" + event.getInfo().getDomain());
            }

            @Override
            public void serviceRemoved(ServiceEvent event) {
                // TODO Auto-generated method stub

            }

            @Override
            public void serviceAdded(ServiceEvent event) {
                // TODO Auto-generated method stub

            }
        };

        WifiManager wifi = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        lock = wifi.createMulticastLock("SolarFighterBonjour");
        lock.setReferenceCounted(true);
        lock.acquire();
        try {
            // Bug http://stackoverflow.com/a/13677686/1143172
            int intaddr = wifi.getConnectionInfo().getIpAddress();

            byte[] byteaddr = new byte[] { (byte) (intaddr & 0xff), (byte) (intaddr >> 8 & 0xff),
                    (byte) (intaddr >> 16 & 0xff), (byte) (intaddr >> 24 & 0xff) };
            InetAddress addr = InetAddress.getByAddress(byteaddr); // Need to
            // process
            // UnknownHostException

            jmdns = JmDNS.create(addr);
            jmdns.addServiceListener(DNS_TYPE, listener);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
