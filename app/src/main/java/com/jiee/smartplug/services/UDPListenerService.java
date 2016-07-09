package com.jiee.smartplug.services;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.HashMap;

import android.os.SystemClock;
import android.util.Log;

import com.jiee.smartplug.M1;
import com.jiee.smartplug.objects.JSmartPlug;
import com.jiee.smartplug.utils.GlobalVariables;
import com.jiee.smartplug.utils.HTTPHelper;
import com.jiee.smartplug.utils.MySQLHelper;
import com.jiee.smartplug.utils.NetworkUtil;
import com.jiee.smartplug.utils.UDPCommunication;

/** TO DO:
 *
 /*
 UDPListenerService.java
 Author: Chinsoft Ltd. | www.chinsoft.com

 This class waits for UDP transmission, processing header, body and terminator according to documentation.

 */

public class UDPListenerService extends Service {
    static String UDP_BROADCAST = "UDPBroadcast";
    static final String TAG =  "UDPListenerService";

    InetAddress broadcastIP;
    int UDP_BROADCAST_PORT = 20004;
    byte[] lMsg = new byte[512];
    int previous_msgid = 0;
    public static short code = 1;
    NetworkUtil networkUtil;
    DatagramSocket ds = null;
    DatagramPacket dp = new DatagramPacket(lMsg, lMsg.length);
    boolean shouldRestartSocketListen = false;
    Thread UDPBroadcastThread;
    public static JSmartPlug js;
    UDPCommunication con;
    private IBinder mBinder = new MyBinder();
    int IRFlag = 0;
    byte[] ir = new byte[2];
    MySQLHelper sql;

    private void listenAndWaitAndThrowIntent() {
        shouldRestartSocketListen = false;
        UDPBroadcastThread = null;

        try {
            if(ds == null || ds.isClosed()) {
                ds = new DatagramSocket(UDP_BROADCAST_PORT);
                ds.setReuseAddress(true);
                ds.setBroadcast(true);
             //   ds.setSoTimeout(13000);
                System.out.println("waiting for new packages");
            }
            ds.receive(dp);
            if(dp.getLength() > 0) {
                System.out.println("Message Length: " + dp.getLength());
                process_headers();
            }

            shouldRestartSocketListen = true;

        } catch (SocketException e) {
            if (ds != null) {
                ds.close();
            }
            System.out.println("SOCKET EXCEPTION");
        } catch (IOException e) {
            System.out.println("SOCKET TIMEOUT");
        } catch (Exception e){
            e.printStackTrace();
        } finally {
        }
    }

    public boolean listenForIRFileName() {
        IRFlag = 0;
        int name = lMsg[18];
        if(name >= 0) {
            Intent i = new Intent("ir_filename");
            i.putExtra("filename", name);
            sendBroadcast(i);
        }

        if(name == 'x'){
            Intent i = new Intent("ir_filename");
            i.putExtra("filename", -1);
            sendBroadcast(i);
        }
        return true;
    }

    //HARDWARE VERSION, MAC ADDRESS ETC...

    void startListenForUDPBroadcast() {
        UDPBroadcastThread = new Thread(new Runnable() {
            public void run() {
                try {
                    synchronized (this) {
                        while (shouldRestartSocketListen == true) {
                                listenAndWaitAndThrowIntent();
                        }
                    }
                } catch (Exception e) {
                    startListenForUDPBroadcast();
                    Log.i( TAG, "no longer listening for UDP broadcasts cause of error " + e.getMessage());
                }
            }
        });
        UDPBroadcastThread.start();
    }

    void stopListen() {
        shouldRestartSocketListen = false;
        if(ds != null) {
            try {
                ds.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onCreate() {
        networkUtil = new NetworkUtil();
        if(networkUtil.getConnectionStatus(getApplicationContext()) == 1){
            shouldRestartSocketListen = true;
        }
    };

    @Override
    public void onDestroy() {
        System.out.println("I AM BEING DESTROYED");
        stopListen();
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        js = new JSmartPlug();
        con = new UDPCommunication(getApplicationContext());
        sql = HTTPHelper.getDB(this);
        shouldRestartSocketListen = true;
        try {
            broadcastIP = InetAddress.getByName("255.255.255.255");
        } catch (Exception e){
            e.printStackTrace();
        }
        //listenAndWaitAndThrowIntent(broadcastIP, port);
        startListenForUDPBroadcast();
        Log.i( TAG, "UPD Service started");
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    public class MyBinder extends Binder {
        public UDPListenerService getService() {
            return UDPListenerService.this;
        }
    }

    void updateRelayFlags(int flags, String id) {
        if((flags & GlobalVariables.SERVICE_FLAGS_WARNING) == GlobalVariables.SERVICE_FLAGS_WARNING){
            js.setHall_sensor(1);
            System.out.println("Relay warning");
            sql.updatePlugHallSensorService(js.getHall_sensor(), id);
        } else {
            System.out.println("Relay normal condition");
            js.setHall_sensor(0);
            sql.updatePlugHallSensorService(js.getHall_sensor(), id);
        }
    }

    void updateCOSensorFlags(int flags, String id) {
        int costatus = 0;
        if((flags & GlobalVariables.SERVICE_FLAGS_WARNING) == GlobalVariables.SERVICE_FLAGS_WARNING ){
            System.out.println("CO SENSOR WARNING");
            costatus = 1;
            js.setCo_sensor(costatus);                                             //WARNING
        } else if ((flags & GlobalVariables.SERVICE_FLAGS_DISABLED) == GlobalVariables.SERVICE_FLAGS_DISABLED){
            costatus = 3;
            System.out.println("CO SENSOR NOT PLUGGED IN");
            js.setCo_sensor(costatus);                                             //NOT PLUGGED
        } else {
            System.out.println("CO SENSOR NORMAL CONDITION = " + flags);
            js.setCo_sensor(costatus);                                             //NORMAL
        }

        sql.updatePlugCoSensorService(costatus, id);
    }

    Intent process_broadcast_info(){
        Intent ui = new Intent("m1updateui");
        String id = sql.getPlugMacFromIP( dp.getAddress() );
        ui.putExtra("id", id );

        HTTPHelper.mPollingDevices.remove(id);

        //ui.putExtra("mac", (String) broadcastValues.get("mac"));

        /**********************************************/

        int pos = 18;
        int serviceID;
        while( (serviceID = process_long( lMsg[pos], lMsg[pos+1], lMsg[pos+2], lMsg[pos+3] )) !=0 ) {
            pos+=4;
            int flags = process_long( lMsg[pos], lMsg[pos+1], lMsg[pos+2], lMsg[pos+3] );
            pos+=4;
            byte serviceFormat = lMsg[pos];
            pos++;
            byte serviceData = lMsg[pos];   // NOTE: currently only need 1 byte, but in future, make sure
                                            // this data size is based on serviceFormat!
            pos++;

            if( serviceFormat!=0x11 )
                break;  // unhandled service format (This will need to be updated for future devices!)

            if( serviceID == GlobalVariables.ALARM_RELAY_SERVICE) {
                sql.updatePlugRelayService(serviceData, id);
                updateRelayFlags(flags, id);
            } else if( serviceID == GlobalVariables.ALARM_NIGHLED_SERVICE) {
                sql.updatePlugNightlightService(serviceData, id);
            } else if( serviceID == GlobalVariables.ALARM_CO_SERVICE ) {
                updateCOSensorFlags(flags, id);
            } else {
                break; // stop processing when unknown services are reached
            }

        }

        /*
        StringBuffer mac = new StringBuffer("");
        for (int i = 18; i < 24; i++) {
            mac.append(String.format("%02x", lMsg[i]));
        }
        */
        /**********************************************/
        /*
        int outlet_service = process_long(lMsg[24], lMsg[25], lMsg[26], lMsg[27]);
        int outlet_value = -1;
        if(outlet_service == GlobalVariables.ALARM_RELAY_SERVICE) {
            outlet_value = lMsg[28];
        }
        int nightlight_service = process_long(lMsg[29], lMsg[30], lMsg[31], lMsg[32]);
        int nightlight_value = -1;
        if(nightlight_service == GlobalVariables.ALARM_NIGHLED_SERVICE) {
            nightlight_value = lMsg[33];
        }
        broadcastValues.put("mac", new String(mac));
        broadcastValues.put("outlet", outlet_value);
        broadcastValues.put("nightlight", nightlight_value);
        */

        return ui;
    }

    void process_query_device_command(){
        /**********************************************/
        StringBuffer mac = new StringBuffer("");
        for (int i = 18; i < 24; i++) {
            mac.append(String.format("%02x", lMsg[i]));
        }
        js.setId(mac.toString());
        System.out.println("MAC: " + mac);
        /**********************************************/
        StringBuffer model = new StringBuffer("");
        for (int i = 24; i < 40; i++) {
            model.append(String.format("%c", lMsg[i]));
        }
        js.setModel(model.toString());
        System.out.println("MODEL:" + model);
        /**********************************************/
        int buildno = process_long(lMsg[40], lMsg[41], lMsg[42], lMsg[43]);
        js.setBuildno(buildno);
        System.out.println("BUILD NO: " + buildno);
        /**********************************************/
        int prot_ver = process_long(lMsg[44], lMsg[45], lMsg[46], lMsg[47]);
        js.setProt_ver(prot_ver);
        System.out.println("PROTOCOL VER: " + prot_ver);
        /**********************************************/
        StringBuffer hw_ver = new StringBuffer("");
        for (int i = 48; i < 64; i++) {
            hw_ver.append(String.format("%c", lMsg[i]));
        }
        js.setHw_ver(hw_ver.toString());
        System.out.println("HARDWARE VERSION:" + hw_ver);
        /**********************************************/
        StringBuffer fw_ver = new StringBuffer("");
        for (int i = 64; i < 80; i++) {
            fw_ver.append(String.format("%c", lMsg[i]));
        }
        js.setFw_ver(fw_ver.toString());
        System.out.println("FIRMWARE VERSION:" + fw_ver);
        /**********************************************/
        int fw_date = process_long(lMsg[80], lMsg[81], lMsg[82], lMsg[83]);
        js.setFw_date(fw_date);
        System.out.println("FIRMWARE DATE: " + fw_date);
        /**********************************************/
        int flag = process_long(lMsg[84], lMsg[85], lMsg[86], lMsg[87]);
        js.setFlag(flag);
        System.out.println("FLAG: " + flag);
    }

    public void process_get_device_status(UDPCommunication.Command currentCommand){
        get_relay_status(currentCommand);
        get_nightlight_status(currentCommand);
        get_co_status(currentCommand);
        /**************TERMINATOR**************/
        int terminator = process_long(lMsg[48], lMsg[49], lMsg[50], lMsg[51]);
        System.out.println("TERMINATOR: " + terminator);
    }

    void get_relay_status( UDPCommunication.Command currentCommand ){
        /**********************************************/
        int service_id = process_long(lMsg[18], lMsg[19], lMsg[20], lMsg[21]);
        System.out.println("Service ID: "+service_id);
        if (service_id == GlobalVariables.ALARM_RELAY_SERVICE) {
            int flag = process_long(lMsg[22], lMsg[23], lMsg[24], lMsg[25]);
            updateRelayFlags(flag, currentCommand.macID);

            byte datatype = lMsg[26];
            byte data = lMsg[27];
            if(data == 0x01){
                js.setRelay(1);
            } else {
                js.setRelay(0);
            }

            Log.v( TAG, "relay data=" + ((data==1)?"on":"off") + " devid=" + currentCommand.macID );
            sql.updatePlugRelayService(js.getRelay(), currentCommand.macID);

        }
        /**********************************************/
    }

    void get_nightlight_status(UDPCommunication.Command currentCommand){
        /**********************************************/
        int service_id = process_long(lMsg[28], lMsg[29], lMsg[30], lMsg[31]);
        if(service_id == GlobalVariables.ALARM_NIGHLED_SERVICE) {
            int flag = process_long(lMsg[32], lMsg[33], lMsg[34], lMsg[35]);             //not used for this service
            byte datatype = lMsg[36];                                                    //always the same 0x01
            byte data = lMsg[37];
            if(data == 0x01){
                js.setNightlight(1);
            } else {
                js.setNightlight(0);
            }

            Log.v(TAG, "light data=" + ((data == 1) ? "on" : "off") + " devid=" + currentCommand.macID);
            sql.updatePlugNightlightService(data, currentCommand.macID);
        }
        /**********************************************/
    }

    void get_co_status(UDPCommunication.Command currentCommand){
        /**********************************************/
        int service_id = process_long(lMsg[38], lMsg[39], lMsg[40], lMsg[41]);
        int costatus = 0;
        if(service_id == GlobalVariables.ALARM_CO_SERVICE) {
            int flag = process_long(lMsg[42], lMsg[43], lMsg[44], lMsg[45]);
            updateCOSensorFlags(flag, currentCommand.macID);
            byte datatype = lMsg[46];
            byte data = lMsg[47];
        }
        /**********************************************/
    }

    public void process_headers() {
        code = 1;
        /**********************************************/
        int header = process_long(lMsg[0],lMsg[1],lMsg[2],lMsg[3]);          //1397576276

        boolean isCommand;

        if (header == 0x534D5253) {
            // reply
            isCommand = false;
        } else if( header== 0x534D5254 ) {
            // command
            isCommand = true;
        } else {
            // failed
            Log.v( TAG, "ignoring header=" + header );
            return;
        }

        // process header
        int msgid = process_long(lMsg[4],lMsg[5],lMsg[6],lMsg[7]);
        int seq = Math.abs(process_long(lMsg[8],lMsg[9],lMsg[10],lMsg[11]));
        int size = process_long(lMsg[12], lMsg[13], lMsg[14], lMsg[15]);
        code = process_short(lMsg[16], lMsg[17]);

        if( isCommand ) {

            if(code == 0x1000 && msgid != previous_msgid) {
                code = 1;
                Log.v( TAG, "command = BROADCAST");
                Log.v(TAG, "msid: "+msgid+" - previous_msgid: "+previous_msgid);
                Intent ui = process_broadcast_info();

                sendBroadcast(ui);
            }
            else if(code == 0x001F) {
                code = 1;
                Log.v( TAG, "command = OTA firmware OK" );
                Intent i = new Intent("ota_finished");
                i.putExtra("id", sql.getPlugMacFromIP( dp.getAddress() ) );
                sendBroadcast(i);
            }
            else if(code == 0x0F0F){
                int mac = 0;
                for (int i = 18; i < 24; i++) {
                    mac += lMsg[i] & 0xff;
                }
                code = 1;
                Intent i = new Intent("broadcasted_presence");
                System.out.println("DEVICE IS ALIVE");
                i.putExtra("ip",dp.getAddress().getHostAddress());
                i.putExtra("name", "JSPlug"+mac);
                sendBroadcast(i);
            }
            else {
                Log.v( TAG, "unknown command " + code );
            }

        } else {
            if(msgid == previous_msgid){
                Log.v( TAG, "ignoring duplicate msg#" + msgid );
                return; // ignore repeated command
            }

            Log.v( TAG, "Received header=" + header + " msg#" + msgid + " seq#" + seq + " size=" + size + " code=" + code );

            previous_msgid = msgid;

            UDPCommunication.Command    currentCommand = UDPCommunication.dequeueCommand(this.getApplicationContext(), dp.getAddress(), msgid );
            if( currentCommand==null ) {
                return;
            }

            switch( currentCommand.command ) {

                case 0x000C:
                    System.out.println("Entering IR Mode");
                    if(code == 0) {
                        listenForIRFileName();
                        code = 1;
                    }
                    break;
                case 0x0001:
                    if(code == 0){
                        process_query_device_command();
                        //    sql.updatePlugServicesByID(js);
                        Intent i = new Intent("device_info");
                        i.putExtra("ip",dp.getAddress().getHostAddress());
                        i.putExtra("id", currentCommand.macID);
                        sendBroadcast(i);
                        code = 1;
                    }
                    break;
                case 0x0003:
                    System.out.println("I got a broadcast .....");
                    break;
                case 0x000B:
                    if(code == 0){
                        code = 1;
                        Intent bi = new Intent("set_timer_delay");
                        bi.putExtra("id", currentCommand.macID);
                        sendBroadcast(bi);

                    }
                    break;

                case 0x0008:
                    if(code == 0){
                        code = 1;
                        System.out.println("DEVICE STATUS CHANGED");
                        con.finishDeviceStatus(currentCommand.msgID);
                        Intent i = new Intent("device_status_changed");
                        i.putExtra("id", currentCommand.macID);
                        i.putExtra("msgid", currentCommand.msgID);
                        sendBroadcast(i);
                    }
                    break;
                case 0x0007:
                    if(code == 0){
                        code = 1;
                        process_get_device_status(currentCommand);
                        Intent ui = new Intent("status_changed_update_ui");
                        ui.putExtra("id", currentCommand.macID);
                        ui.putExtra("msgid", currentCommand.msgID);
                        sendBroadcast(ui);
                    }
                    break;
                case 0x0009:
                    if(code == 0){
                        code = 1;
                        Intent ui = new Intent("timers_sent_successfully");
                        ui.putExtra("id", currentCommand.macID);
                        ui.putExtra("msgid", currentCommand.msgID);
                        sendBroadcast(ui);
                    }
                    break;
                case 0x000F:
                    if(code == 0){
                        System.out.println("OTA SENT SUCCESSFULLY");
                        code = 1;
                        Intent i = new Intent("ota_sent");
                        i.putExtra("id", currentCommand.macID);
                        sendBroadcast(i);
                    }
                    break;

                case 0x010F:
                    if(code == 0){
                        System.out.println("DELETE SEND SUCCESSFULLY");
                        code = 1;
                        Intent i = new Intent("delete_sent");
                        i.putExtra("id", currentCommand.macID);
                        sendBroadcast(i);
                    }
                    break;

                case 0x0F0F:
                    if(code == 0){
                        //do nothing if we already receive this command
                        code = 1;
                    }
                    break;
            }
        }

    }

    int process_long(byte a, byte b, byte c, byte d){

        ByteBuffer buffer = ByteBuffer.allocate(4);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        buffer.put(d);
        buffer.put(c);
        buffer.put(b);
        buffer.put(a);

        return buffer.getInt(0);
    }

    short process_short(byte a, byte b){
        ByteBuffer buffer = ByteBuffer.allocate(2);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        buffer.put(b);
        buffer.put(a);
        return buffer.getShort(0);
    }

}