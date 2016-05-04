package com.jiee.smartplug.utils;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.SystemClock;
import android.util.Log;

import com.jiee.smartplug.M1;
import com.jiee.smartplug.objects.JSmartPlug;
import com.jiee.smartplug.services.UDPListenerService;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.security.spec.ECField;
import java.util.ArrayList;

/**
 * Created by ronaldgarcia on 9/12/15.
 */
public class UDPCommunication {

    int MAX_UDP_DATAGRAM_LEN = 128;
    int UDP_SERVER_PORT = 20004;
    int UDP_TESTING_PORT = 20005;
    byte[] lMsg = new byte[MAX_UDP_DATAGRAM_LEN];
    byte[] hMsg = new byte[14];
    byte[] irHeader = new byte[15];
    byte[] timerHeader = new byte[20];
    byte[] timer = new byte[26];
    byte[] rMsg = new byte[14];
    byte[] sMsg = new byte[24];
    byte[] iMsg = new byte[22];
    byte[] kMsg = new byte[46];
    byte[] ir = new byte[128];
    byte[] delayT = new byte[22];
    byte[] ir2 = new byte[1];
    byte[] timers = new byte[512];
    public static ArrayList<Integer> IRCodes = new ArrayList<>();
    public static JSmartPlug js;
    public static short command;
    public static String macID;
    int previous_msgid = 0;
    boolean process_data = false;
    short code = 1;
    MySQLHelper sql;
    int IRFlag = 0;
    int IRSendFlag = 0;
    int irCode = 0;
    int sendFlag = 0;
    HTTPHelper http;

    public UDPCommunication(){

    }

    //DEVICE QUERY
    public JSmartPlug runUdpServer() {
        DatagramPacket dp = new DatagramPacket(lMsg, lMsg.length);
        DatagramSocket ds = null;
        try {
            ds = new DatagramSocket(UDP_SERVER_PORT);
            //disable timeout for testing
            ds.setSoTimeout(5000);
            ds.receive(dp);
            System.out.println("Message Length: " + dp.getLength());
            process_headers();
            if(process_data) {
                if(command == 0x0001) {
                    process_query_device_command();                                   //GET MAC, HARDWARE VERSION, etc...
                } else if(command == 0x0007){
                    process_get_device_status();                                    //GET ALL THE SERVICES AND THEIR STATUS FROM THE DEVICE
//                    Intent intent2 = new Intent("get_device_status 0x0007");
//                    context.sendBroadcast(intent2);
                } else if(command == 0x0008){
                    System.out.println("SEND SET DEVICE STATUS COMMAND SUCCESS");     //SET DEVICE SERVICE
//                    Intent intent1 = new Intent("set_device_status 0x0008");
//                    context.sendBroadcast(intent1);
                } else if(command == 0x0009){
                    if(code == 0x0000){
                        System.out.println("SUCCESS COMMAND RECEIVED");
                    }
                } else if(command == 0x000C){
                    if(code == 0x0000){
                        System.out.println("Entering IR Mode");
                        IRFlag = 1;
                        IRCodes.clear();
                        listenForIRCodes();
                    }
                } else if(command == 0x000A){
                    if(code == 0x0000){                                             //This is the feedback from the device
                        System.out.println("Sending IR Codes ....");
                        IRSendFlag = 1;
                    }
                }
            }
        } catch (SocketException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (ds != null) {
                ds.close();
            }
        }
        return js;
    }

    public boolean delayTimer(int seconds, int protocol, Context activity, int serviceId, int send){
        UDPListenerService.code = 1;
        boolean toReturn = false;
        http = new HTTPHelper(activity);
        this.command = 0x000B;
        String ip = M1.ip;
            if (protocol == 0) {
                generate_header_http();
            } else if (protocol == 1) {
                generate_header();
            }
            for (int i = 0; i < 14; i++) {
                delayT[i] = hMsg[i];
            }

            delayT[17] = (byte) (serviceId & 0xff);
            delayT[16] = (byte) ((serviceId >> 8) & 0xff);
            delayT[15] = (byte) ((serviceId >> 16) & 0xff);
            delayT[14] = (byte) ((serviceId >> 24) & 0xff);

            delayT[21] = (byte) (seconds & 0xff);
            delayT[20] = (byte) ((seconds >> 8) & 0xff);
            delayT[19] = (byte) ((seconds >> 16) & 0xff);
            delayT[18] = (byte) ((seconds >> 24) & 0xff);

            if(protocol == 0){
                try {
                    if(http != null) {
                        toReturn = http.setTimerDelay(delayT, send);
                    } else {
                        toReturn = false;
                        System.out.println("HTTP IS NULL");
                    }
                } catch (Exception e){
                    e.printStackTrace();
                }
            } else if (protocol == 1) {
                if(ip != null) {
                    sendUDP(delayT, ip);
                    toReturn = true;
                } else {
                    System.out.println("IP IS NULL WHILE SENDING TIMER DELAY");
                }
            }
            return toReturn;
    }

    public boolean listenForIRCodes() {
        DatagramPacket dp = new DatagramPacket(ir, ir.length);
        DatagramSocket ds = null;

        try {
            ds = new DatagramSocket(UDP_TESTING_PORT);
            ds.receive(dp);

            for(int i = 0; i < ir.length; i++){
                IRCodes.add((int)ir[i]);
                if(ir[i] == 0){
                    IRFlag = 0;
                }
            }
        } catch (SocketException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (ds != null) {
                ds.close();
            }
        }
        if(IRFlag == 1){
            listenForIRCodes();
        }
        return true;
    }

    public boolean sendActivationKey(String ip, byte[] key){
        this.command = 0x0001;
        DatagramSocket ds = null;
        try {
            ds = new DatagramSocket();
            InetAddress serverAddr = InetAddress.getByName(ip);
            DatagramPacket dp;
            generate_header();
            for(int i=0; i<14;i++){
                kMsg[i] = hMsg[i];
            }

            for (int k = 0; k < key.length; k++){
                kMsg[k+14] = key[k];
            }

            dp = new DatagramPacket(kMsg, kMsg.length, serverAddr, UDP_SERVER_PORT);
            ds.send(dp);
        } catch (SocketException e) {
            e.printStackTrace();
            return false;
        }catch (UnknownHostException e) {
            e.printStackTrace();
            return false;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        } finally {
            if (ds != null) {
                ds.close();
            }
        }
        return true;
    }

    public boolean queryDevices(String ip, short udpMsg_param, String macParam)  {
        this.macID = macParam;
        UDPListenerService.code = 1;
        this.command = udpMsg_param;

        try {
            final DatagramSocket ds = new DatagramSocket();
            InetAddress serverAddr = InetAddress.getByName(ip);

            generate_header();
            for(int i=0; i<14;i++){
                rMsg[i] = hMsg[i];
            }
            final DatagramPacket dp = new DatagramPacket(rMsg, rMsg.length, serverAddr, UDP_SERVER_PORT);
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        ds.send(dp);
                        Log.i("QUERY DEVICES", "Successfully sent");
                    } catch (Exception e){
                        Log.i("QUERY DEVICES", "I could not send it");
                        //e.printStackTrace();
                    } finally {
                        if (ds != null) {
                            ds.close();
                        }
                    }
                }
            }).start();

        } catch (SocketException e) {
            e.printStackTrace();
            return false;
        }catch (UnknownHostException e) {
            e.printStackTrace();
            return false;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        } finally {
        }
        return true;
    }

    public boolean sendIRMode(String ipParam){
        String ip = ipParam;
        this.command = 0x000C;
        DatagramSocket ds = null;
        try {
            ds = new DatagramSocket();
            InetAddress serverAddr = InetAddress.getByName(ip);
            DatagramPacket dp;
            generate_header();
            for (int i = 0; i < 14; i++){
                iMsg[i] = hMsg[i];
            }
            int service_id = 0xD1000003;
            iMsg[14] = (byte)(service_id & 0xff);
            iMsg[15] = (byte)((service_id >> 8) & 0xff);
            iMsg[16] = (byte)((service_id >> 16) & 0xff);
            iMsg[17] = (byte)((service_id >> 24) & 0xff);
            int flag = 0x00000000;
            iMsg[18] = (byte)(flag & 0xff);
            iMsg[19] = (byte)((flag >> 8) & 0xff);
            iMsg[20] = (byte)((flag >> 16) & 0xff);
            iMsg[21] = (byte)((flag >> 24) & 0xff);
            dp = new DatagramPacket(iMsg, iMsg.length, serverAddr, UDP_SERVER_PORT);
            ds.send(dp);
        } catch (SocketException e) {
            e.printStackTrace();
        }catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (ds != null) {
                ds.close();
                //runUdpServer();
            }
        }
        return true;
    }

    public boolean cancelIRMode(){

        String ip = M1.ip;
        this.command = 0x000C;
        DatagramSocket ds = null;
        try {
            ds = new DatagramSocket();
            InetAddress serverAddr = InetAddress.getByName(ip);
            DatagramPacket dp;
            generate_header();
            for (int i = 0; i < 14; i++){
                iMsg[i] = hMsg[i];
            }
            int service_id = 0xD1000004;
            iMsg[14] = (byte)(service_id & 0xff);
            iMsg[15] = (byte)((service_id >> 8) & 0xff);
            iMsg[16] = (byte)((service_id >> 16) & 0xff);
            iMsg[17] = (byte)((service_id >> 24) & 0xff);
            int flag = 0x00000000;
            iMsg[18] = (byte)(flag & 0xff);
            iMsg[19] = (byte)((flag >> 8) & 0xff);
            iMsg[20] = (byte)((flag >> 16) & 0xff);
            iMsg[21] = (byte)((flag >> 24) & 0xff);
            dp = new DatagramPacket(iMsg, iMsg.length, serverAddr, UDP_SERVER_PORT);
            ds.send(dp);
        } catch (SocketException e) {
            e.printStackTrace();
        }catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (ds != null) {
                ds.close();
                //runUdpServer();
            }
        }
        return true;
    }

    public boolean sendOTACommand(String ip){
        this.command = 0x000F;
        generate_header();
        DatagramSocket ds = null;
        try {
            ds = new DatagramSocket();
            InetAddress serverAddr = InetAddress.getByName(ip);
            DatagramPacket dp;
            dp = new DatagramPacket(hMsg, hMsg.length, serverAddr, UDP_SERVER_PORT);
            ds.send(dp);
        } catch (Exception e){
            e.printStackTrace();
        }
        ds.close();
        return true;
    }

    public boolean sendIRFileName(int filename){
        sendIRHeader(filename);
        return true;
    }

    public void sendIRHeader(int filename){
        String ip = M1.ip;
        this.command = 0x000A;
        generate_header();
        for(int i = 0; i < hMsg.length; i++){
            irHeader[i] = hMsg[i];
        }
        irHeader[14] = (byte)filename;
        DatagramSocket ds = null;
        try {
            ds = new DatagramSocket();
            InetAddress serverAddr = InetAddress.getByName(ip);
            DatagramPacket dp;
            dp = new DatagramPacket(irHeader, irHeader.length, serverAddr, UDP_SERVER_PORT);
            ds.send(dp);
            System.out.println("IR HEADERS SENT");
        } catch (Exception e){
            e.printStackTrace();
        }
        ds.close();
    }

    public boolean setDeviceTimersHTTP(String id, Context activity, int send){
        boolean toReturn = true;
        sendFlag = send;
        String ip = M1.ip;
        http = new HTTPHelper(activity);
        boolean header = sendTimerHeaders(ip, 0);
        boolean timer = sendTimers(id, activity, 0);
        boolean termi = sendTimerTerminator(ip, 0);
        return toReturn;
    }

    public boolean sendTimers(Context a, String id, String ip){

        this.command = 0x0009;

        generate_header();

        int i = 0;
        for(i = 0; i < hMsg.length; i++){
            timers[i] = hMsg[i];
        }

        long time = (long)(System.currentTimeMillis()/1000);

        timers[i++] = (byte) ((time >> 24) & 0xff);
        timers[i++] = (byte) ((time >> 16) & 0xff);
        timers[i++] = (byte) ((time >> 8) & 0xff);
        timers[i++] = (byte) (time & 0xff);

        sql = new MySQLHelper(a);
        Cursor c = sql.getAlarmData(id);
        if(c.getCount() > 0) {
            c.moveToFirst();
            for (int j = 0; j < c.getCount(); j++) {
                int serviceId = c.getInt(2);
                if(serviceId == GlobalVariables.ALARM_RELAY_SERVICE || serviceId == GlobalVariables.ALARM_NIGHLED_SERVICE || serviceId == GlobalVariables.ALARM_IR_SERVICE) {

                    timers[i++] = (byte) ((serviceId >> 24) & 0xff);
                    timers[i++] = (byte) ((serviceId >> 16) & 0xff);
                    timers[i++] = (byte) ((serviceId >> 8) & 0xff);
                    timers[i++] = (byte) (serviceId & 0xff);
                    timers[i++] = 0x01;
                    int init_ir = c.getInt(9);
                    timers[i++] = (byte)init_ir;
                    int end_ir = c.getInt(10);
                    timers[i++] = (byte)end_ir;
                    int dow = c.getInt(3);
                    timers[i++] = (byte) (dow & 0xff);
                    int initHour = c.getInt(4);
                    timers[i++] = (byte) (initHour & 0xff);
                    int initMin = c.getInt(5);
                    timers[i++] = (byte) (initMin & 0xff);
                    int endHour = c.getInt(6);
                    timers[i++] = (byte) (endHour & 0xff);
                    int endMinu = c.getInt(7);
                    timers[i++] = (byte) (endMinu & 0xff);
                }
                c.moveToNext();
            }
        } else {
            for (int ix = 0; ix < 11; ix++){
                timers[i++] = 0;
            }
        }
        c.close();
        sendUDP(timers, ip);

        return true;

    }

    public boolean sendTimersHTTP(Context a, String id, int send){
            boolean toReturn = false;
            this.command = 0x0009;
            http = new HTTPHelper(a);
            generate_header_http();

            int i = 0;
            for(i = 0; i < hMsg.length; i++){
                timers[i] = hMsg[i];
            }

            int time = (int)(System.currentTimeMillis()/1000);

            timers[i++] = (byte) (time & 0xff);
            timers[i++] = (byte) ((time >> 8) & 0xff);
            timers[i++] = (byte) ((time >> 16) & 0xff);
            timers[i++] = (byte) ((time >> 24) & 0xff);

            sql = new MySQLHelper(a);
            Cursor c = sql.getAlarmData(id);
            if(c.getCount() > 0) {
                c.moveToFirst();
                for (int j = 0; j < c.getCount(); j++) {
                    int serviceId = c.getInt(2);
                    if(serviceId == GlobalVariables.ALARM_RELAY_SERVICE || serviceId == GlobalVariables.ALARM_NIGHLED_SERVICE || serviceId == GlobalVariables.ALARM_IR_SERVICE) {

                        timers[i++] = (byte) ((serviceId >> 24) & 0xff);
                        timers[i++] = (byte) ((serviceId >> 16) & 0xff);
                        timers[i++] = (byte) ((serviceId >> 8) & 0xff);
                        timers[i++] = (byte) (serviceId & 0xff);
                        timers[i++] = 0x01;
                        int init_ir = c.getInt(9);
                        timers[i++] = (byte)init_ir;
                        int end_ir = c.getInt(10);
                        timers[i++] = (byte)end_ir;
                        int dow = c.getInt(3);
                        timers[i++] = (byte) (dow & 0xff);
                        int initHour = c.getInt(4);
                        timers[i++] = (byte) (initHour & 0xff);
                        int initMin = c.getInt(5);
                        timers[i++] = (byte) (initMin & 0xff);
                        int endHour = c.getInt(6);
                        timers[i++] = (byte) (endHour & 0xff);
                        int endMinu = c.getInt(7);
                        timers[i++] = (byte) (endMinu & 0xff);
                    }
                    c.moveToNext();
                }

                try {
                    toReturn = http.setDeviceTimers(timers, send);
                } catch (Exception e){
                    e.printStackTrace();
                    toReturn = false;
                }

            } else {
                System.out.println("SENDING ALL ZERO TIMER");
                for(int x = 0; x < 11; x++){
                    timers[i++] = 0;
                }

                try {
                    toReturn = http.setDeviceTimers(timers, send);
                } catch (Exception e){
                    e.printStackTrace();
                    toReturn = false;
                }

                /*
                try {
                    toReturn = http.delDeviceTimers(id, a);
                } catch (Exception e){
                    e.printStackTrace();
                    toReturn = false;
                }
                */
            }
        c.close();
         return toReturn;

        }

    public boolean setDeviceTimersUDP(String id, Context activity){
        UDPListenerService.code = 1;
        boolean toReturn = true;
        String ip = M1.ip;
        sendTimerHeaders(ip, 1);                            //The last parameter 0 = HTTP, 1 = UDP
        sendTimers(id, activity, 1);
        sendTimerTerminator(ip, 1);

        return toReturn;
    }

    public boolean sendTimerTerminator(String ip, int protocol){

        boolean toReturn = false;
        this.command = 0x0009;
        if(protocol == 0) {
            generate_header_http();
        } else {
            generate_header();
        }
        for(int i = 0; i < hMsg.length; i++){
            timerHeader[i] = hMsg[i];
        }
        int end = 0x00000000;
        if(protocol == 0) {
            timerHeader[14] = (byte) (end & 0xff);
            timerHeader[15] = (byte) ((end >> 8) & 0xff);
            timerHeader[16] = (byte) ((end >> 16) & 0xff);
            timerHeader[17] = (byte) ((end >> 24) & 0xff);
        }
        if(protocol == 1){
            timerHeader[14] = (byte) (end & 0xff);
            timerHeader[15] = (byte) ((end >> 8) & 0xff);
            timerHeader[16] = (byte) ((end >> 16) & 0xff);
            timerHeader[17] = (byte) ((end >> 24) & 0xff);
        }
        if(protocol == 0) {
            try {
                toReturn = http.setDeviceTimers(timerHeader, sendFlag);
            } catch (Exception e) {
                toReturn = false;
            }
        } else if(protocol == 1){
         //   sendUDP(timerHeader, ip);
        }
        return toReturn;
    }

    public boolean sendTimerHeaders(String ip, int protocol){
        boolean toReturn = false;
        this.command = 0x0009;
        if(protocol == 0) {
            generate_header_http();
        } else {
            generate_header();
        }
        for(int i = 0; i < hMsg.length; i++){
            timerHeader[i] = hMsg[i];
        }
        int time = (int)(System.currentTimeMillis()/1000);
        if(protocol == 0) {
            timerHeader[17] = (byte) (time & 0xff);
            timerHeader[16] = (byte) ((time >> 8) & 0xff);
            timerHeader[15] = (byte) ((time >> 16) & 0xff);
            timerHeader[14] = (byte) ((time >> 24) & 0xff);
        }
        if(protocol == 1){
            timerHeader[14] = (byte) (time & 0xff);
            timerHeader[15] = (byte) ((time >> 8) & 0xff);
            timerHeader[16] = (byte) ((time >> 16) & 0xff);
            timerHeader[17] = (byte) ((time >> 24) & 0xff);
        }
        if(protocol == 0) {
            try {
                toReturn = http.setDeviceTimers(timerHeader, sendFlag);
            } catch (Exception e) {
                toReturn = false;
            }
        } else if(protocol == 1){
         //   sendUDP(timerHeader, ip);
        }
        return toReturn;
    }

    public void sendUDP(byte[] array, String ip){
        try {
            final DatagramSocket ds = new DatagramSocket();
            InetAddress serverAddr = InetAddress.getByName(ip);

            final DatagramPacket dp = new DatagramPacket(array, array.length, serverAddr, UDP_SERVER_PORT);
       //     new Thread(new Runnable() {
       //         @Override
       //         public void run() {
                    try {
                        ds.send(dp);
                    } catch (Exception e){
                        e.printStackTrace();
                    } finally {
                        if (ds != null) {
                            ds.close();
                        }
                    }
        //        }
        //    }).start();

        } catch (Exception e){
            e.printStackTrace();
        }
    }

    public boolean sendTimers(String id, Context a, int protocol ){
        boolean toReturn = false;
        this.command = 0x0009;
        if(protocol == 0) {
            generate_header_http();
        } else {
            generate_header();
        }
        for(int i = 0; i < hMsg.length; i++){
            timer[i] = hMsg[i];
        }
        String ip = M1.ip;
        sql = new MySQLHelper(a);
        Cursor c = sql.getAlarmData(id);
        if(c.getCount() > 0) {
            c.moveToFirst();
            for (int j = 0; j < c.getCount(); j++) {
                int serviceId = c.getInt(2);
                System.out.println("SERVICE ID: "+serviceId);
                if(serviceId == GlobalVariables.ALARM_RELAY_SERVICE || serviceId == GlobalVariables.ALARM_NIGHLED_SERVICE) {
                    if (protocol == 0) {
                        timer[17] = (byte) (serviceId & 0xff);
                        timer[16] = (byte) ((serviceId >> 8) & 0xff);
                        timer[15] = (byte) ((serviceId >> 16) & 0xff);
                        timer[14] = (byte) ((serviceId >> 24) & 0xff);
                        timer[18] = 0x01;
                        timer[19] = 0x01;
                        timer[20] = 0x00;
                        int dow = c.getInt(3);
                        timer[21] = (byte) (dow & 0xff);
                        int initHour = c.getInt(4);
                        timer[22] = (byte) (initHour & 0xff);
                        int initMin = c.getInt(5);
                        timer[23] = (byte) (initMin & 0xff);
                        int endHour = c.getInt(6);
                        timer[24] = (byte) (endHour & 0xff);
                        int endMinu = c.getInt(7);
                        timer[25] = (byte) (endMinu & 0xff);
                    }
                    if (protocol == 1) {
                        timer[17] = (byte) (serviceId & 0xff);
                        timer[16] = (byte) ((serviceId >> 8) & 0xff);
                        timer[15] = (byte) ((serviceId >> 16) & 0xff);
                        timer[14] = (byte) ((serviceId >> 24) & 0xff);
                        timer[18] = 0x01;
                        timer[19] = 0x01;
                        timer[20] = 0x00;
                        int dow = c.getInt(3);
                        timer[21] = (byte) (dow & 0xff);
                        int initHour = c.getInt(4);
                        timer[22] = (byte) (initHour & 0xff);
                        int initMin = c.getInt(5);
                        timer[23] = (byte) (initMin & 0xff);
                        int endHour = c.getInt(6);
                        timer[24] = (byte) (endHour & 0xff);
                        int endMinu = c.getInt(7);
                        timer[25] = (byte) (endMinu & 0xff);
                    }

                    if (protocol == 0) {
                        try {
                            toReturn = http.setDeviceTimers(timer, sendFlag);
                        } catch (Exception e) {
                            toReturn = false;
                        }
                    } else if (protocol == 1) {
                     //   sendUDP(timer, ip);
                    }
                }
                c.moveToNext();
            }
        }
        c.close();
        return toReturn;
    }

    public boolean setDeviceStatus(String ip, int serviceId, byte action)  {
        this.command = 0x0008;     //to generate the header
        generate_header();
        DatagramSocket ds = null;
        try {
            ds = new DatagramSocket();
            InetAddress serverAddr = InetAddress.getByName(ip);
            DatagramPacket dp;
            generate_header();
            for(int i=0; i<14;i++){
                sMsg[i] = hMsg[i];
            }

            int service_id = serviceId;
            sMsg[14] = (byte)(service_id & 0xff);
            sMsg[15] = (byte)((service_id >> 8 ) & 0xff);
            sMsg[16] = (byte)((service_id >> 16 ) & 0xff);
            sMsg[17] = (byte)((service_id >> 24 ) & 0xff);
            byte datatype = 0x01;
            sMsg[18] = datatype;
            byte data = action;
            sMsg[19] = data;
            int terminator = 0x00000000;
            sMsg[20] = (byte)(terminator & 0xff);
            sMsg[21] = (byte)((terminator >> 8 ) & 0xff);
            sMsg[22] = (byte)((terminator >> 16 ) & 0xff);
            sMsg[23] = (byte)((terminator >> 24 ) & 0xff);

            dp = new DatagramPacket(sMsg, sMsg.length, serverAddr, UDP_SERVER_PORT);
            ds.send(dp);
        } catch (SocketException e) {
            e.printStackTrace();
            return false;
        }catch (UnknownHostException e) {
            e.printStackTrace();
            return false;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        } finally {
            if (ds != null) {
                ds.close();
                //        runUdpServer();
            }
            return true;
        }
    }

    public void process_headers(){
        /**********************************************/
        int header = Math.abs(process_long(lMsg[0],lMsg[1],lMsg[2],lMsg[3]));          //1397576276

        if (header != 1397576276) {
            process_data = true;
        }
        System.out.println("HEADER: " + header);
        /**********************************************/
        int msgid = Math.abs(process_long(lMsg[4],lMsg[5],lMsg[6],lMsg[7]));
        if (msgid != previous_msgid){
            previous_msgid = msgid;
            process_data = true;
        } else {
            process_data = false;
        }
        System.out.println("MSGID: " + msgid);
        /**********************************************/
        int seq = Math.abs(process_long(lMsg[8],lMsg[9],lMsg[10],lMsg[11]));
        System.out.println("SEQ: " + seq);
        /**********************************************/
        int size = process_long(lMsg[12], lMsg[13], lMsg[14], lMsg[15]);
        System.out.println("SIZE: " + size);
        /**********************************************/
        code = process_short(lMsg[16], lMsg[17]);
        System.out.println("CODE: " + code);
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
        if (service_id == 0xD1000000) {
            System.out.println("IS OUTLET SERVICE");
            int flag = process_long(lMsg[22], lMsg[23], lMsg[24], lMsg[25]);
            if(flag == 0x00000010){
                js.setHall_sensor(1);
                System.out.println("Relay warning");
            } else {
                js.setHall_sensor(0);
            }
            byte datatype = lMsg[26];
            byte data = lMsg[27];
            if(data == 0x01){
                js.setRelay(1);
                System.out.println("Relay is on");
            } else {
                js.setRelay(0);
                System.out.println("Relay is off");
            }

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
            if(data == 0x01){
                js.setNightlight(1);
                System.out.println("Nighlight is on");
            } else {
                js.setNightlight(0);
                System.out.println("Nighlight is off");
            }
        }
        /**********************************************/
    }

    void get_co_status(){
        /**********************************************/
        int service_id = process_long(lMsg[38], lMsg[39], lMsg[40], lMsg[41]);
        if(service_id == 0xD1000002) {
            int flag = process_long(lMsg[42], lMsg[43], lMsg[44], lMsg[45]);
            if(flag == 0x00000010){
                js.setCo_sensor(1);                                             //WARNING
            } else if (flag == 0x00000100){
                js.setCo_sensor(3);                                             //NOT PLUGGED
            } else {
                js.setCo_sensor(0);                                             //NORMAL
            }
            byte datatype = lMsg[46];
            byte data = lMsg[47];
        }
        /**********************************************/
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

    void generate_header(){
        int header = 0x534D5254;
        int msgid = (int)(Math.random() * 429496729) + 1;
        int seq = 0x80000000;
        hMsg[0] = (byte) header;
        hMsg[1] = (byte) (header >> 8);
        hMsg[2] = (byte) (header >> 16);
        hMsg[3] = (byte) (header >> 24);
        hMsg[4] = (byte) msgid;
        hMsg[5] = (byte) (msgid >> 8);
        hMsg[6] = (byte) (msgid >> 16);
        hMsg[7] = (byte) (msgid >> 24);
        hMsg[8] = (byte) seq;
        hMsg[9] = (byte) (seq >> 8);
        hMsg[10] = (byte) (seq >> 16);
        hMsg[11] = (byte) (seq >> 24);
        hMsg[12] = (byte) command;
        hMsg[13] = (byte) (command >> 8);
    }

    void generate_header_http(){
        int header = 0x534D5254;
        hMsg[3] = (byte)(header);
        hMsg[2] = (byte)((header >> 8 ));
        hMsg[1] = (byte)((header >> 16 ));
        hMsg[0] = (byte)((header >> 24 ));

        int msid = (int)(Math.random()*4294967+1);
        hMsg[7] = (byte)(msid);
        hMsg[6] = (byte)((msid >> 8 ));
        hMsg[5] = (byte)((msid >> 16 ));
        hMsg[4] = (byte)((msid >> 24 ));
        int seq = 0x80000000;
        hMsg[11] = (byte)(seq);
        hMsg[10] = (byte)((seq >> 8 ));
        hMsg[9] = (byte)((seq >> 16 ));
        hMsg[8] = (byte)((seq >> 24 ));
        short command = this.command;
        hMsg[13] = (byte)(command);
        hMsg[12] = (byte)((command >> 8 ));
    }

}
