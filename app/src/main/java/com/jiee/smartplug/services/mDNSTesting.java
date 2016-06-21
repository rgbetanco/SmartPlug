package com.jiee.smartplug.services;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.net.wifi.WifiManager;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.os.SystemClock;
import android.util.Log;

import com.jiee.smartplug.objects.JSmartPlug;
import com.jiee.smartplug.utils.HTTPHelper;
import com.jiee.smartplug.utils.MySQLHelper;
import com.jiee.smartplug.utils.UDPCommunication;

import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;

import javax.jmdns.ServiceEvent;
import javax.jmdns.ServiceListener;
import javax.jmdns.JmDNS;

/** TO DO:
 *
 /*
 mDNSTesting.java
 Author: Chinsoft Ltd. | www.chinsoft.com
 This is a background service use to discover the SmartPlug through mDNS. The library has compatibility for after and before Jellybeans version
 of Android.

 */


public class mDNSTesting extends Service {

    private WifiManager.MulticastLock lock;
    private NsdManager.DiscoveryListener discoveryListener;
    private NsdManager mNsdManager;
    public static final String SMARTCONFIG_IDENTIFIER = "JSPlug";
    private static final String DNS_TYPE = "_http._tcp.";
    private JmDNS jmdns = null;
    private ServiceListener listener;
    MySQLHelper sql;
    private final IBinder mBinder = new MyBinder();
    NsdManager.ResolveListener mResolveListener;
    public static ArrayList<JSmartPlug> plugs = new ArrayList<JSmartPlug>();

    @Override
    public void onCreate() {
        super.onCreate();
        sql = HTTPHelper.getDB(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        sql.deleteNonActivePlug("all");
    //    sql.removePlugsIP();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            plugs.clear();
            setUpDiscoInfo();
        } else {
            // Set up Bonjour
            Thread t = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        setUpDiscoInfoPreJellyBean();
                    }catch (Exception e){
                        e.printStackTrace();
                    }
                }
            });
            t.start();
        }

        /*

        if (jmdns != null) {
            jmdns.addServiceListener(DNS_TYPE, listener);
        }

        if(mNsdManager!=null){
            mNsdManager.discoverServices(DNS_TYPE, NsdManager.PROTOCOL_DNS_SD, discoveryListener);
        }

        */

        System.out.println("mDNS service started");
        return START_STICKY;
    }

    @Override
    public void onDestroy(){
        if(mNsdManager != null) {
            try {
                mNsdManager.stopServiceDiscovery(discoveryListener);
            } catch( Exception e ) {
                e.printStackTrace();
            }
        }
        if (jmdns != null) {
            try {
                jmdns.close();
                System.out.println("jmDNS CLOSED");
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
        public mDNSTesting getService() {
            return mDNSTesting.this;
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

                String name = serviceInfo.getServiceName();

                name.substring(0, 5);

                if (name.substring(0, 5).equals("JSPlug")) {
                    System.out.println("JSPlug removed: " + serviceInfo);
                    Intent intent1 = new Intent("mDNS_Device_Removed");
                    intent1.putExtra("name", name);
                    sendBroadcast(intent1);

                }

                /*
                if(plugs.size() > 1) {
                    for (int i = 0; i < plugs.size(); i++) {
                        if (serviceInfo.getServiceName().equals(plugs.get(i).getName())) {
                    //        plugs.remove(i);
                        }
                    }
                } else {
                    plugs.clear();
                }
                */
             //   Intent intent1 = new Intent("mDNS_Device_Removed");
             //   intent1.putExtra("serviceName",serviceInfo.getServiceName().toString());
             //   sendBroadcast(intent1);
            }

            @Override
            public void onServiceFound(NsdServiceInfo serviceInfo) {
                System.out.println("Service resolved: " + serviceInfo);
                try {
                    String name = serviceInfo.getServiceName().toString();
                    if(name.length() > 6) {
                        name = serviceInfo.getServiceName().substring(0, 6);
                        if (name.equals(SMARTCONFIG_IDENTIFIER)) {
                            initializeResolveListener(serviceInfo);
                        }
                    }
                } catch (Exception e){
                    e.printStackTrace();
                }
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
            //THIS IS FOR OLD ANDROID
            @Override
            public void serviceResolved(ServiceEvent event) {
                System.out.println("Service resolved: " + event.getInfo().getQualifiedName() + " port:" + event.getInfo().getPort() + " domain:" + event.getInfo().getDomain());
                String ip = event.getInfo().getHostAddresses()[0];
                if(ip != null && !ip.isEmpty() && !ip.equals("null")) {
                    sql.updatePlugIP(event.getName(), ip);
                }
                boolean found = false;
                if (event.getName().substring(0, 6).equals(SMARTCONFIG_IDENTIFIER)){
                    try {
                        if (plugs.size() > 0) {
                            for (int i = 0; i < plugs.size(); i++) {
                                if (ip.equals(plugs.get(i).getIp())) {
                                    found = true;
                                }
                            }
                            if(!found) {
                                JSmartPlug ob = new JSmartPlug();
                                ob.setIp(ip);
                                ob.setName(event.getName());
                                plugs.add(ob);
                            }
                        } else {
                            JSmartPlug ob = new JSmartPlug();
                            ob.setIp(ip);
                            ob.setName(event.getName());
                            plugs.add(ob);
                        }
                        if(ip != null && !ip.isEmpty() && !ip.equals("null")) {
                            if(event.getName() != null && !event.getName().isEmpty()) {
                                Intent intent1 = new Intent("mDNS_New_Device_Found");
                                intent1.putExtra("ip", ip);
                                intent1.putExtra("name", event.getName());
                                sendBroadcast(intent1);
                            }
                        }

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

            }

            @Override
            public void serviceRemoved(ServiceEvent event) {
                // TODO Auto-generated method stub

                String name = event.getName();

                name.substring(0, 5);

                if (name.substring(0, 5).equals("JSPlug")) {

                    System.out.println("JSPlug removed: " + event);
                    Intent intent1 = new Intent("mDNS_Device_Removed");
                    intent1.putExtra("name", name);
                    sendBroadcast(intent1);

                }

                /*
                if(plugs.size() > 1) {
                    for (int i = 0; i < plugs.size(); i++) {
                        if (event.getName().equals(plugs.get(i).getName())) {
                            plugs.remove(i);
                        }
                    }
                } else {
                    plugs.clear();
                }
                sql.updatePlugIP(event.getName(), "");
                Intent intent1 = new Intent("mDNS_Device_Removed");
                sendBroadcast(intent1);
                */
            }

            @Override
            public void serviceAdded(ServiceEvent event) {
                // TODO Auto-generated method stub

            }
        };

        WifiManager wifi = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        lock = wifi.createMulticastLock("JSPLUGLOCK");
        lock.setReferenceCounted(true);
        lock.acquire();
        try {
            // Bug http://stackoverflow.com/a/13677686/1143172
            int intaddr = wifi.getConnectionInfo().getIpAddress();

            byte[] byteaddr = new byte[] { (byte) (intaddr & 0xff), (byte) (intaddr >> 8 & 0xff),
                    (byte) (intaddr >> 16 & 0xff), (byte) (intaddr >> 24 & 0xff) };
            InetAddress addr = InetAddress.getByAddress(byteaddr); // Need to

            jmdns = JmDNS.create(addr);
            jmdns.addServiceListener(DNS_TYPE, listener);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void initializeResolveListener(NsdServiceInfo serviceInfo) {
        NsdManager.ResolveListener newResolveListener = new NsdManager.ResolveListener() {
            @Override
            public void onResolveFailed(NsdServiceInfo serviceInfo, int errorCode) {
                System.out.println("Resolve failed" + errorCode);
                if (errorCode == 3) {
                    SystemClock.sleep(500);
                    initializeResolveListener(serviceInfo);
                }
            }

            @Override
            public void onServiceResolved(NsdServiceInfo serviceInfo) {
                System.out.println("Resolve Succeeded. " + serviceInfo);
                InetAddress host = serviceInfo.getHost();
                String ip = host.toString().substring(1);
                String name = serviceInfo.getServiceName().toString();
                if(ip != null && !ip.isEmpty() && !ip.equals("null")) {
                    sql.updatePlugIP(name, ip);
                }
                boolean found = false;
                System.out.println("IP: " + ip+ ", NAME: "+name);

                if(ip != null && !ip.isEmpty() &&  !ip.equals("null")) {
                    Intent intent1 = new Intent("mDNS_New_Device_Found");
                    intent1.putExtra("name", serviceInfo.getServiceName());
                    intent1.putExtra("ip", ip);
                    sendBroadcast(intent1);
                }
            }
        };
        mNsdManager.resolveService(serviceInfo, newResolveListener);
        }
}
