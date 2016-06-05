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

import android.os.SystemClock;
import android.util.Log;

import com.jiee.smartplug.M1;
import com.jiee.smartplug.objects.JSmartPlug;
import com.jiee.smartplug.utils.HTTPHelper;
import com.jiee.smartplug.utils.MySQLHelper;
import com.jiee.smartplug.utils.NetworkUtil;
import com.jiee.smartplug.utils.UDPCommunication;

public class UDPListenerService extends Service {
    static String UDP_BROADCAST = "UDPBroadcast";

    InetAddress broadcastIP;
    int UDP_BROADCAST_PORT = 20004;
    byte[] lMsg = new byte[512];
    int previous_msgid = 0;
    boolean process_data = false;
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

    UDPCommunication.Command    mCurrentCommand;

    private void listenAndWaitAndThrowIntent() {
        shouldRestartSocketListen = false;
        UDPBroadcastThread = null;
        process_data = false;

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
        if (process_data == true) {
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
                    Log.i("UDP", "no longer listening for UDP broadcasts cause of error " + e.getMessage());
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
        Log.i("UDP", "UPD Service started");
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

    public void process_get_device_status(){
        get_relay_status();
        get_nightlight_status();
        get_co_status();
        /**************TERMINATOR**************/
        int terminator = process_long(lMsg[48], lMsg[49], lMsg[50], lMsg[51]);
        System.out.println("TERMINATOR: " + terminator);
    }

    void get_relay_status(){
        /**********************************************/
        int service_id = process_long(lMsg[18], lMsg[19], lMsg[20], lMsg[21]);
        System.out.println("Service ID: "+service_id);
        if (service_id == 0xD1000000) {
            System.out.println("IS OUTLET SERVICE");
            int flag = process_long(lMsg[22], lMsg[23], lMsg[24], lMsg[25]);
            if(flag == 0x00000010){
                js.setHall_sensor(1);
                System.out.println("Relay warning");
            } else {
                System.out.println("Relay normal condition");
                js.setHall_sensor(0);
            }
            byte datatype = lMsg[26];
            byte data = lMsg[27];
            System.out.println("DATA: " + data);
            if(data == 0x01){
                js.setRelay(1);
                System.out.println("Relay is on");
            } else {
                js.setRelay(0);
                System.out.println("Relay is off");
            }
            System.out.println("MAC: "+mCurrentCommand.macID);
            sql.updatePlugRelayService(js.getRelay(), mCurrentCommand.macID);
            sql.updatePlugHallSensorService(js.getHall_sensor(), mCurrentCommand.macID);

        }
        /**********************************************/
    }

    void get_nightlight_status(){
        /**********************************************/
        int service_id = process_long(lMsg[28], lMsg[29], lMsg[30], lMsg[31]);
        if(service_id == 0xD1000001) {
            System.out.println("NIGHT LIGHT SERVICE");
            int flag = process_long(lMsg[32], lMsg[33], lMsg[34], lMsg[35]);             //not used for this service
            byte datatype = lMsg[36];                                                    //always the same 0x01
            byte data = lMsg[37];
            System.out.println("DATA: "+data);
            if(data == 0x01){
                js.setNightlight(1);
                System.out.println("Nighlight is on");
            } else {
                js.setNightlight(0);
                System.out.println("Nighlight is off");
            }
            sql.updatePlugNightlightService(data, mCurrentCommand.macID);
        }
        /**********************************************/
    }

    void get_co_status(){
        /**********************************************/
        int service_id = process_long(lMsg[38], lMsg[39], lMsg[40], lMsg[41]);
        int costatus = 0;
        if(service_id == 0xD1000002) {
            int flag = process_long(lMsg[42], lMsg[43], lMsg[44], lMsg[45]);
            if(flag == 0x00000010){
                System.out.println("CO SENSOR WARNING");
                costatus = 1;
                js.setCo_sensor(costatus);                                             //WARNING
            } else if (flag == 0x00000100){
                costatus = 3;
                System.out.println("CO SENSOR NOT PLUGGED IN");
                js.setCo_sensor(costatus);                                             //NOT PLUGGED
            } else {
                costatus = 0;
                System.out.println(flag);
                System.out.println("CO SENSOR NORMAL CONDITION");
                js.setCo_sensor(costatus);                                             //NORMAL
            }
            sql.updatePlugCoSensorService(costatus, mCurrentCommand.macID);
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
            Log.v( "UDPListenerService", "ignoring header=" + header );
            return;
        }

        // process header
        int msgid = process_long(lMsg[4],lMsg[5],lMsg[6],lMsg[7]);
        int seq = Math.abs(process_long(lMsg[8],lMsg[9],lMsg[10],lMsg[11]));
        int size = process_long(lMsg[12], lMsg[13], lMsg[14], lMsg[15]);
        code = process_short(lMsg[16], lMsg[17]);

        if( isCommand ) {

            if(code == 0x1000) {
                code = 1;
                Log.v( "UDPListenerService", "command = BROADCAST" );
                Intent ui = new Intent("m1updateui");
                ui.putExtra("id", sql.getPlugMacFromIP( dp.getAddress() ) );
                sendBroadcast(ui);
            }
            else if(code == 0x001F) {
                code = 1;
                Log.v( "UDPListenerService", "command = OTA firmware OK" );
                Intent i = new Intent("ota_finished");
                i.putExtra("id", sql.getPlugMacFromIP( dp.getAddress() ) );
                sendBroadcast(i);
            }
            else {
                Log.v( "UDPListenerService", "unknown command " + code );
            }

        } else {
            if(msgid == previous_msgid){
                Log.v( "UDPListenerService", "ignoring duplicate msg#" + msgid );
                return; // ignore repeated command
            }

            Log.v( "UDPListenerService", "Received header=" + header + " msg#" + msgid + " seq#" + seq + " size=" + size + " code=" + code );

            previous_msgid = msgid;

            mCurrentCommand = UDPCommunication.dequeueCommand(this.getApplicationContext(), dp.getAddress(), msgid );
            if( mCurrentCommand==null ) {
                return;
            }

            switch( mCurrentCommand.command ) {

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
                        i.putExtra("id", mCurrentCommand.macID);
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
                        bi.putExtra("id", mCurrentCommand.macID);
                        sendBroadcast(bi);

                    }
                    break;

                case 0x0008:
                    if(code == 0){
                        code = 1;
                        System.out.println("DEVICE STATUS CHANGED");
                        Intent i = new Intent("device_status_changed");
                        i.putExtra("id", mCurrentCommand.macID);
                        sendBroadcast(i);
                    }
                    break;
                case 0x0007:
                    if(code == 0){
                        code = 1;
                        process_get_device_status();
                        Intent ui = new Intent("status_changed_update_ui");
                        ui.putExtra("id", mCurrentCommand.macID);
                        sendBroadcast(ui);
                    }
                    break;
                case 0x0009:
                    if(code == 0){
                        code = 1;
                        Intent ui = new Intent("timers_sent_successfully");
                        ui.putExtra("id", mCurrentCommand.macID);
                        sendBroadcast(ui);
                    }
                    break;
                case 0x000F:
                    if(code == 0){
                        System.out.println("OTA SENT SUCCESSFULLY");
                        code = 1;
                        Intent i = new Intent("ota_sent");
                        i.putExtra("id", mCurrentCommand.macID);
                        sendBroadcast(i);
                    }
                    break;

                case 0x010F:
                    if(code == 0){
                        System.out.println("DELETE SEND SUCCESSFULLY");
                        code = 1;
                        Intent i = new Intent("delete_sent");
                        i.putExtra("id", mCurrentCommand.macID);
                        sendBroadcast(i);
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