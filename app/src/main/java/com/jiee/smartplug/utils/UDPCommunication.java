package com.jiee.smartplug.utils;

import android.content.Context;
import android.database.Cursor;
import android.util.Log;

import com.jiee.smartplug.M1;
import com.jiee.smartplug.objects.JSmartPlug;
import com.jiee.smartplug.services.UDPListenerService;

import org.json.HTTP;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashMap;

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
    int previous_msgid = 0;
    boolean process_data = false;
    short code = 1;
    int IRFlag = 0;
    int IRSendFlag = 0;
    int irCode = 0;
    int sendFlag = 0;
    HTTPHelper http;

    Context mContext;

    static int mLastMsgID =  0;//(new SecureRandom()).nextInt();

    public static class Command {
        public String macID;
        public short command;
        public int   msgID;

        public Command( String macID, short command ) {
            this.macID = macID;
            this.command = command;
        }
    }

    // map Device to Command
    static HashMap<String, Command> mQueuedCommands = new HashMap<String, Command>();

    public static void addCommand(Command command) {
        Log.v( "UDPCommunication", "addCommand: msg#" + command.msgID + " for ID " + command.macID + " code=" + command.command);

        mQueuedCommands.put( command.macID, command );
    }

    public static Command dequeueCommand(Context c, InetAddress ia, int msgID) {
        MySQLHelper sql = HTTPHelper.getDB(c);

        String mac = sql.getPlugMacFromIP(ia);
        if( mac==null ) {
            mac = ia.getHostAddress();
            Log.v( "UDPCommunication", "dequeueCommand: No ID associated with IP; using IP "+ mac + " directly.");
        }

        // convert ia to string
        return dequeueCommand( mac, msgID );
    }

    public static Command dequeueCommand(String macID, int msgID) {
        if( macID==null )
            return null;

        Command cmd = mQueuedCommands.get(macID);
        if( cmd==null ) {
            Log.v( "UDPCommunication", "dequeueCommand: No command associated with ID " + macID);
            return null;
        }

        if( cmd.msgID!=msgID ) {
            Log.v( "UDPCommunication", "dequeueCommand: Got msg#" + msgID + " from device, but was expecting " + cmd.msgID + " for ID " + macID );
            return null;
        }

        Log.v( "UDPCommunication", "dequeueCommand: msg#" + msgID + " for ID " + macID);

        mQueuedCommands.remove(macID);
        return cmd;
    }

    public UDPCommunication(Context c) {
        this.mContext = c.getApplicationContext();
    }

    //DEVICE QUERY

    public boolean delayTimer( String macID, int seconds, int protocol, int serviceId, int send){

        Command command = null;

        UDPListenerService.code = 1;
        boolean toReturn = false;
        http = new HTTPHelper(mContext);
        String ip = M1.ip;
            if (protocol == 0) {
                generate_header_http((short) 0x000B);
            } else if (protocol == 1) {
                command = generate_header( macID, (short)0x000B);
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
                    sendUDP(delayT, command);
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

    public boolean queryDevices(String macID, short command )  {
        try {
            Command cmd = generate_header( macID, command );
            for(int i=0; i<14;i++){
                rMsg[i] = hMsg[i];
            }

            sendUDP(rMsg, cmd);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        } finally {
        }
        return true;
    }

    public boolean sendIRMode(String macID, boolean temp) {
        try {
            Command cmd = generate_header(macID, (short)0x000C);
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

            sendUDP(iMsg, cmd);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return true;
    }

    public boolean cancelIRMode(String macID, boolean temp){
        try {
            Command cmd = generate_header(macID, (short)0x000C);
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

            sendUDP(iMsg, cmd);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return true;
    }

    public boolean sendOTACommand(String macID, boolean temp){
        Command cmd = generate_header(macID, (short)0x000F);
        sendUDP(hMsg, cmd);
        return true;
    }

    public boolean sendReformatCommand(String macID, boolean temp){
        Command cmd = generate_header(macID, (short)0x010F);
        sendUDP(hMsg, cmd);
        return true;
    }

    public boolean sendResetCommand(String macID, boolean temp){
        Command cmd = generate_header(macID, (short)0x0FFF);
        sendUDP(hMsg, cmd);
        return true;
    }

    public boolean sendIRFileName(String macID, int filename){
        sendIRHeader(macID, filename);
        return true;
    }

    public void sendIRHeader(String macID, int filename){
        Command cmd = generate_header(macID, (short)0x000A);
        for(int i = 0; i < hMsg.length; i++){
            irHeader[i] = hMsg[i];
        }
        irHeader[14] = (byte)filename;

        sendUDP(irHeader, cmd);
    }

    public boolean sendTimers(String macID){
        for(int j = 0; j < 512; j++){
            timers[j] = 0;
        }

        Command cmd = generate_header(macID, (short)0x0009);

        int i = 0;
        for(i = 0; i < hMsg.length; i++){
            timers[i] = hMsg[i];
        }

        long time = (long)(System.currentTimeMillis()/1000);

        timers[i++] = (byte) ((time >> 24) & 0xff);
        timers[i++] = (byte) ((time >> 16) & 0xff);
        timers[i++] = (byte) ((time >> 8) & 0xff);
        timers[i++] = (byte) (time & 0xff);

        MySQLHelper sql = HTTPHelper.getDB(mContext);
        Cursor c = sql.getAlarmData(macID);
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
        sendUDP(timers, cmd);

        return true;

    }

    public boolean sendTimersHTTP(String macID, int send){
            boolean toReturn = false;
            http = new HTTPHelper(mContext);
            generate_header_http((short)0x0009);

            int i = 0;
            for(i = 0; i < hMsg.length; i++){
                timers[i] = hMsg[i];
            }

            int time = (int)(System.currentTimeMillis()/1000);

            timers[i++] = (byte) (time & 0xff);
            timers[i++] = (byte) ((time >> 8) & 0xff);
            timers[i++] = (byte) ((time >> 16) & 0xff);
            timers[i++] = (byte) ((time >> 24) & 0xff);

            MySQLHelper sql = HTTPHelper.getDB(mContext);
            Cursor c = sql.getAlarmData(macID);
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

    public void sendUDP(byte[] array, final Command command ){
        try {
            final DatagramSocket ds = new DatagramSocket();

            InetAddress serverAddr = HTTPHelper.getDB(mContext).getPlugInetAddress(command.macID);

            if( serverAddr!=null ) {
                addCommand(command);

                final DatagramPacket dp = new DatagramPacket(array, array.length, serverAddr, UDP_SERVER_PORT);
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            ds.send(dp);
                        } catch (Exception e){
                            e.printStackTrace();
                        } finally {
                            if (ds != null) {
                                ds.close();
                            }
                        }
                    }
                }).start();

            }
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    public boolean sendTimers(String macID, int protocol ){
        boolean toReturn = false;

        Command cmd = null;
        if(protocol == 0) {
            generate_header_http((short)0x0009);
        } else {
            cmd = generate_header(macID, (short)0x0009);
        }
        for(int i = 0; i < hMsg.length; i++){
            timer[i] = hMsg[i];
        }
        MySQLHelper sql = HTTPHelper.getDB(mContext);
        Cursor c = sql.getAlarmData(macID);
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

    public boolean setDeviceStatus( String macID, int serviceId, byte action, boolean temp)  {
        Command cmd = generate_header(macID, (short)0x0008);

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

        sendUDP(sMsg, cmd);

        return true;
    }

    Command generate_header(String macID, short command){
        Command c = new Command(macID, command);
        int header = 0x534D5254;
        int msgid = c.msgID = (mLastMsgID++);
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
        hMsg[12] = (byte) c.command;
        hMsg[13] = (byte) (c.command >> 8);

        return c;
    }

    void generate_header_http(short command){
        int header = 0x534D5254;
        hMsg[3] = (byte)(header);
        hMsg[2] = (byte)((header >> 8 ));
        hMsg[1] = (byte)((header >> 16 ));
        hMsg[0] = (byte)((header >> 24 ));

        int msid = (mLastMsgID++);
        hMsg[7] = (byte)(msid);
        hMsg[6] = (byte)((msid >> 8 ));
        hMsg[5] = (byte)((msid >> 16 ));
        hMsg[4] = (byte)((msid >> 24 ));
        int seq = 0x80000000;
        hMsg[11] = (byte)(seq);
        hMsg[10] = (byte)((seq >> 8 ));
        hMsg[9] = (byte)((seq >> 16 ));
        hMsg[8] = (byte)((seq >> 24 ));
        hMsg[13] = (byte)(command);
        hMsg[12] = (byte)((command >> 8 ));
    }

}
