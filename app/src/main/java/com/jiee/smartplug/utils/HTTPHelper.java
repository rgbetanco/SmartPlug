package com.jiee.smartplug.utils;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.SystemClock;
import android.util.Log;

import com.jiee.smartplug.ListDevices;
import com.jiee.smartplug.M1;
import com.jiee.smartplug.MainActivity;
import com.jiee.smartplug.objects.Alarm;
import com.jiee.smartplug.objects.IRGroups;
import com.jiee.smartplug.objects.JSmartPlug;
import com.jiee.smartplug.services.RegistrationIntentService;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.IOException;
import java.io.InputStream;
import java.net.Inet4Address;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.spec.ECField;
import java.util.ArrayList;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Created by ronaldgarcia on 16/2/16.
 */
public class HTTPHelper {
    public static final MediaType TEXT = MediaType.parse("application/x-www-form-urlencoded; charset=utf-8");
    OkHttpClient client;
    Context a;
    protected static MySQLHelper mSQL;
    byte[] sMsg = new byte[24];
    UDPCommunication udp;
    ArrayList<Alarm> alarms = new ArrayList<>();
    String json;
    String idLocal;

    public HTTPHelper(Context a){
        this.a = a.getApplicationContext();
        this.client = new OkHttpClient();
        udp = new UDPCommunication();
        getDB();
    }

    public HTTPHelper(){
        this.client = new OkHttpClient();
    }

    public static MySQLHelper getDB(Context context) {
        if( mSQL==null )
            mSQL = new MySQLHelper(context);

        return mSQL;
    }

    public MySQLHelper getDB() {
        return getDB(a);
    }

    public boolean sendDeviceActivationKey(String param, String ip, String id, String name) throws Exception {

        boolean toReturn = false;
        String json = getJSONData(param);
        System.out.println(json);

        JSmartPlug js = new JSmartPlug();
        js.setId(id);
        js.setName(name);
        js.setIp(ip);
     //   sql.insertPlug(js, 1);

        JSONObject job = new JSONObject(json);
        int r = job.getInt("r");
        if (r == 0) {
            toReturn = true;
        }

        return toReturn;
/*
        boolean toReturn = false;
        String url = GlobalVariables.DOMAIN+param;
        RequestBody body = RequestBody.create(TEXT, GlobalVariables.DOMAIN+param);
        Request request = new Request.Builder().url(url).post(body).build();
        Response response = client.newCall(request).execute();
        InputStream inputStream = response.body().byteStream();
        System.out.println("MESSAGE WHEN ACTIVATING DEVICE: "+inputStream);
        try {
            int[] headerArray = new int[4];
            for(int i = 0; i < 4; i++) {
                headerArray[i] = inputStream.read();
            }
            int header = Miscellaneous.process_long((byte)headerArray[3], (byte)headerArray[2], (byte)headerArray[1], (byte)headerArray[0]);
//            if(header == 1397576275){
                for(int j = 0; j < 8; j++){
                    inputStream.read();                     //This right now is not doing anything, the MSGID and SEQ are zero
                }
//            }
            int[] sizeArray = new int[2];
            sizeArray[0] = inputStream.read();
            sizeArray[1] = inputStream.read();

            short size = Miscellaneous.process_short((byte)sizeArray[1], (byte)sizeArray[0]);

            int[] responseArray = new int[2];
            responseArray[0] = inputStream.read();
            responseArray[1] = inputStream.read();
            short server_response = Miscellaneous.process_short((byte)responseArray[1], (byte)responseArray[0]);
            int[] dataSizeArray = new int[2];
            dataSizeArray[0] = inputStream.read();
            dataSizeArray[1] = inputStream.read();
            short dataSize = Miscellaneous.process_short((byte)dataSizeArray[1], (byte)dataSizeArray[0]);

            byte[] dataArray = new byte[dataSize];
            for (int k = 0; k < dataSize; k++){
                dataArray[k] = (byte)inputStream.read();
            }

            //udp.sendActivationKey(ip, dataArray);
//            String tosend = "devlist?token="+ MainActivity.token+"&hl="+ Locale.getDefault().getLanguage()+"&res=1";
//            getDeviceList(tosend, ip, name);

        }catch(Exception e){
            e.printStackTrace();
        }

        return toReturn;
        */
    }
    //SETTINGS LIKE ICON, NOTIFICATION ETC
    public boolean setDeviceSettings(String param) throws Exception {

        try {
            String jsonData = getJSONData(param);
            System.out.println(jsonData);
        } catch (Exception e){
            e.printStackTrace();
        }

        return true;
    }

    public boolean saveGallery(String param) throws Exception {
        MySQLHelper sql = getDB();
        boolean toReturn = false;
        OkHttpClient c = new OkHttpClient();
        String url = GlobalVariables.DOMAIN+param;
        RequestBody body = RequestBody.create(TEXT, GlobalVariables.DOMAIN+param);
        Request request = new Request.Builder().url(url).post(body).build();
        Response response = c.newCall(request).execute();
        String jsonData = response.body().string().toString();
        System.out.println("GALLERY RESPONSE: "+jsonData);
        try {
            JSONObject Jobject = new JSONObject(jsonData);
            JSONArray Jarray = Jobject.getJSONArray("icons");
            for (int i = 0; i < Jarray.length(); i++){
                JSONObject childJSONObject = Jarray.getJSONObject(i);
                String urlParam = optString(childJSONObject, "url");
                if(urlParam != null && !urlParam.isEmpty()) {
                    Cursor cu = sql.getIconByUrl(childJSONObject.getString("url"));
                    if (cu.getCount() <= 0) {
                        String idParam = optString(childJSONObject, "id");
                        if(idParam != null && !idParam.isEmpty()) {
                            sql.insertIcons(urlParam, 0, idParam);
                        }
                    }
                    cu.close();
                }
            }

        }catch(Exception e){
            e.printStackTrace();
        }
        return toReturn;
    }

    public static String optString(JSONObject json, String key)
    {
        // http://code.google.com/p/android/issues/detail?id=13830
        if (json.isNull(key))
            return null;
        else
            return json.optString(key, null);
    }

    public static int optInt(JSONObject json, String key)
    {
        // http://code.google.com/p/android/issues/detail?id=13830
        if (json.isNull(key))
            return 0;
        else
            return json.optInt(key, 0);
    }

    public boolean getDeviceList(String param){
        MySQLHelper sql = getDB();
        boolean toReturn = false;
        ArrayList<JSmartPlug> jsplugs = new ArrayList<>();
        try {
            String jsonData = getJSONData(param);
            System.out.println(jsonData);
            try {
                JSONObject Jobject = new JSONObject(jsonData);
                JSONArray Jarray = Jobject.getJSONArray("devs");
                sql.deletePlugs();
                for (int i = 0; i < Jarray.length(); i++){
                    JSONObject childJSONObject = Jarray.getJSONObject(i);
                    Cursor c = sql.getPlugDataByID(childJSONObject.getString("devid"));
                    JSmartPlug js = new JSmartPlug();

                    js.setId(optString(childJSONObject, "devid"));
                    js.setName(optString(childJSONObject, "title_origin"));
                    js.setGivenName(optString(childJSONObject, "title"));
                    js.setIcon(optString(childJSONObject, "icon"));
                 //   js.setIp(childJSONObject.getString("ip"));

                    js.setRelay(optInt(childJSONObject, "relay"));
                    js.setRelay(optInt(childJSONObject, "nightlight"));
                    js.setRelay(optInt(childJSONObject, "cosensor"));
                    js.setRelay(optInt(childJSONObject, "hallsensor"));

                    //js.setRelay(Integer.parseInt((childJSONObject.getString("relay"))));
                    //js.setNightlight(Integer.parseInt(childJSONObject.getString("nightlight")));
                    //js.setCo_sensor(Integer.parseInt(childJSONObject.getString("cosensor")));
                    //js.setHall_sensor(Integer.parseInt(childJSONObject.getString("hallsensor")));

                    if(js.getName()==null || js.getName().isEmpty()){
                        js.setName(js.getGivenName());
                    }
                    if(c.getCount() <= 0) {
                        sql.insertPlug(js, 1);
                    }

                    jsplugs.add(js);
                    System.out.println("Title: " + childJSONObject.getString("title_origin") + " Devid:" + childJSONObject.getString("devid"));

                    c.close();
                }
                toReturn = true;
            } catch (Exception e){
                e.printStackTrace();
            }
        } catch (Exception e){
            e.printStackTrace();
        }
        //iterate over all the plugs found on the database and delete those that are not found on the array received from the server
        boolean found = false;
        if(jsplugs.size()>0) {
            Cursor cx = sql.getPlugData();
            if (cx.getCount() > 0) {
                cx.moveToFirst();
                for (int i = 0; i < cx.getCount(); i++) {
                    for (int j = 0; j < jsplugs.size(); j++) {
                        if (cx.getString(2).equals(jsplugs.get(j).getId())){
                            Log.i("FOUND","Device Found on the DB");
                            found = true;
                        }
                    }
                    if(!found) {
                        sql.deactivatePlug(cx.getString(2));
                    }
                    cx.moveToNext();
                }
                sql.deleteNonActivePlug("all");
            }
            cx.close();
        } else {
            sql.deletePlugs();
        }

        return toReturn;
    }

    public boolean getDeviceList(String param, String ip, String name){
        boolean toReturn = true;
        getDeviceList(param);
        return toReturn;
    }

    public String getJSONData(String param) throws Exception {
        OkHttpClient c = new OkHttpClient();
        c.newBuilder().connectTimeout(30, TimeUnit.SECONDS).readTimeout(30, TimeUnit.SECONDS).writeTimeout(30, TimeUnit.SECONDS);
        String url = GlobalVariables.DOMAIN+param;

        RequestBody body = RequestBody.create(TEXT, GlobalVariables.DOMAIN+param);
        Request request = new Request.Builder().url(url).post(body).build();
        Response response = c.newCall(request).execute();
        String jsonData = response.body().string().toString();
        System.out.println(jsonData);
        return jsonData;
    }

    public boolean removeDevice(String param, String devid) throws Exception {
        String json = getJSONData(param);
        boolean toReturn = false;
        if(devid != null && !devid.isEmpty()) {
            json = getJSONData(param);
            JSONObject job = new JSONObject(json);
            toReturn = checkReturn(job);
        }
        System.out.println(json);
        return toReturn;
    }

    public boolean login(String param) throws IOException {
        MySQLHelper sql = getDB();
        boolean toReturn = false;
        try {
            String jsonData = getJSONData(param);
            System.out.println(jsonData);
            JSONObject Jobject = new JSONObject(jsonData);
            String token = optString(Jobject, "token");
            String r = optString(Jobject, "r");
            System.out.println("R: "+r);
            if (token != null && !token.isEmpty()) {
                if(r != null && !r.isEmpty()) {
                    if (r.equals("0")) {
                        sql.updateToken(token);
                        toReturn = true;
                    }
                }
            }
        }catch(Exception e){
            e.printStackTrace();
        }
        return toReturn;
    }

    public boolean checkReturn(JSONObject Jobject) {
        String r = optString(Jobject, "r");
        if (r != null && !r.isEmpty()) {
            int ret = Integer.parseInt(r);
            if (ret == 0) {
                return true;
            } else if( ret==1 ) {
                Miscellaneous.logout(a, Miscellaneous.LogoutType.WARN_BADUSER);
            }
        }

        return false;
    }

    public boolean logout() throws Exception {
        boolean toReturn = false;
        String param = "logout?token="+Miscellaneous.getToken(a)+"&hl="+ Locale.getDefault().getLanguage()+"&devtoken="+ RegistrationIntentService.regToken;
        String json = getJSONData(param);
        System.out.println(json);
        try {
            JSONObject Jobject = new JSONObject(json);
            toReturn = checkReturn(Jobject);
        } catch (Exception e){
            e.printStackTrace();
        }
        return toReturn;
    }

    public boolean createAccount(String param) throws Exception {
        boolean toReturn = false;
        String json = getJSONData(param);
        System.out.println(json);
        try {
            JSONObject Jobject = new JSONObject(json);
            String r = optString(Jobject, "r");
            if (r != null && !r.isEmpty()) {
                if (Integer.parseInt(r) == 0) {
                    toReturn = true;
                }
            }
        } catch(Exception e){
            e.printStackTrace();
        }
        return toReturn;
    }

    public boolean resetPassword(String oldpass, String newpass) throws Exception {
        boolean toReturn = false;

        String param = "changepwd?token="+Miscellaneous.getToken(a)+"&hl="+Locale.getDefault().getLanguage()+"&pwd="+oldpass+"&newpwd="+newpass;
        String json = getJSONData(param);
        System.out.println(json);
        try {
            JSONObject Jobject = new JSONObject(json);
            toReturn = checkReturn(Jobject);
        } catch(Exception e){
            e.printStackTrace();
        }
        return toReturn;
    }

    public boolean forgotPassword(String username) throws Exception{
        boolean toReturn = false;

        String param = "forgotpwd?username=" + username + "&hl=" + Locale.getDefault().getLanguage();
        String json = getJSONData(param);
        System.out.println(json);
        try {
            JSONObject Jobject = new JSONObject(json);
            String r = optString(Jobject, "r");
            if (r != null && !r.isEmpty()) {
                if (Integer.parseInt(r) == 0) {
                    toReturn = true;
                }
            }
        } catch(Exception e){
            e.printStackTrace();
        }

        return toReturn;
    }

    public boolean getDeviceStatus(String param, String id, Context context) throws IOException{
        boolean toReturn = false;
        this.idLocal = id;
        try {
            json = getJSONData(param);
            System.out.println(json);
        } catch (Exception e){
            e.printStackTrace();
        }

        MySQLHelper sql = getDB();

        try {

                    JSONObject Jobject = new JSONObject(json);
                    toReturn = checkReturn(Jobject);
                    if(toReturn) {
                        String relay = optString(Jobject, "relay");
                        String nightlight = optString(Jobject, "nightlight");
                        String co_sensor = optString(Jobject, "cosensor");
                        String hall_sensor = optString(Jobject, "hallsensor");
                        String snooze = optString(Jobject, "snooze");
                        String ledsnooze = optString(Jobject, "nightlightsnooze");
                        System.out.println("RELAY=" + relay + " NIGHTLIGHT=" + nightlight + " CO SENSOR=" + co_sensor + " HALL SENSOR="+hall_sensor+" ID="+id+" SNOOZE="+snooze+" LED SNOOZE="+ledsnooze);
                        if(relay != null && !relay.isEmpty()) {
                            sql.updatePlugRelayService(Integer.parseInt(relay), idLocal);
                        } else {
                            sql.updatePlugRelayService(0, idLocal);
                        }
                        if(nightlight != null && !nightlight.isEmpty()) {
                            sql.updatePlugNightlightService(Integer.parseInt(nightlight), idLocal);
                        } else {
                            sql.updatePlugNightlightService(0, idLocal);
                        }
                        if(co_sensor != null && !co_sensor.isEmpty()) {
                            sql.updatePlugCoSensorService(Integer.parseInt(co_sensor), idLocal);
                        } else {
                            sql.updatePlugCoSensorService(0, idLocal);
                        }
                        if(hall_sensor != null && !hall_sensor.isEmpty()) {
                            sql.updatePlugHallSensorService(Integer.parseInt(hall_sensor), idLocal);
                        } else {
                            sql.updatePlugHallSensorService(0, idLocal);
                        }
                        if(snooze != null && !snooze.isEmpty()) {
                            sql.updateDeviceSnooze(idLocal, GlobalVariables.ALARM_RELAY_SERVICE, Integer.parseInt(snooze));
                        } else {
                            sql.updateDeviceSnooze(idLocal, GlobalVariables.ALARM_RELAY_SERVICE, 0);
                        }
                        if(ledsnooze != null && !ledsnooze.isEmpty()) {
                            sql.updateDeviceSnooze(idLocal, GlobalVariables.ALARM_NIGHLED_SERVICE, Integer.parseInt(ledsnooze));
                        } else {
                            sql.updateDeviceSnooze(idLocal, GlobalVariables.ALARM_NIGHLED_SERVICE, 0);
                        }

                    }

                }catch(Exception e){
                    e.printStackTrace();
                }

        return toReturn;
    }

    public boolean setDeviceStatus(String param, byte action, int serviceId) throws IOException {
        System.out.println(param);
        boolean toReturn = false;
        String url = GlobalVariables.DOMAIN+param;
        int header = 0x534D5254;
        sMsg[3] = (byte)(header);
        sMsg[2] = (byte)((header >> 8 ));
        sMsg[1] = (byte)((header >> 16 ));
        sMsg[0] = (byte)((header >> 24 ));

        int msid = (int)(Math.random()*4294967+1);
        sMsg[7] = (byte)(msid);
        sMsg[6] = (byte)((msid >> 8 ));
        sMsg[5] = (byte)((msid >> 16 ));
        sMsg[4] = (byte)((msid >> 24 ));
        int seq = 0x80000000;
        sMsg[11] = (byte)(seq);
        sMsg[10] = (byte)((seq >> 8 ));
        sMsg[9] = (byte)((seq >> 16 ));
        sMsg[8] = (byte)((seq >> 24 ));
        short command = 0x0008;
        sMsg[13] = (byte)(command);
        sMsg[12] = (byte)((command >> 8 ));
        //int serviceId = 0xD1000000;
        sMsg[17] = (byte)(serviceId);
        sMsg[16] = (byte)((serviceId >> 8 ));
        sMsg[15] = (byte)((serviceId >> 16 ));
        sMsg[14] = (byte)((serviceId >> 24 ));

        byte datatype = 0x01;
        sMsg[18] = datatype;
        byte data = action;
        sMsg[19] = data;
        int terminator = 0x00000000;
        sMsg[23] = (byte)(terminator & 0xff);
        sMsg[22] = (byte)((terminator >> 8 ) & 0xff);
        sMsg[21] = (byte)((terminator >> 16 ) & 0xff);
        sMsg[20] = (byte)((terminator >> 24 ) & 0xff);
        RequestBody body = RequestBody.create(TEXT, sMsg);
        Request request = new Request.Builder().url(url).post(body).build();
        String jsonData="";
        try {
            Response response = client.newCall(request).execute();
            jsonData = response.body().string().toString();
            System.out.println("SET DEVICE STATUS: "+jsonData);
        } catch (Exception e){
            e.printStackTrace();
            toReturn = false;
        }

        try {

            JSONObject Jobject = new JSONObject(jsonData);
            toReturn = checkReturn(Jobject);
        }catch(Exception e){
            e.printStackTrace();
        }
        return toReturn;
    }

    public boolean delDeviceTimers(String mac){
        boolean toReturn = false;
        String param = "alarmdel?token="+ Miscellaneous.getToken(a)+"&hl="+Locale.getDefault().getLanguage()+"&devid="+mac;
        System.out.println(param);
        String jsonData = "";
        try {
            jsonData = getJSONData(param);
            JSONObject job = new JSONObject(jsonData);
            toReturn = checkReturn(job);
            System.out.println(jsonData);
        } catch (Exception e){
            e.printStackTrace();
        }
        return toReturn;
    }

    public boolean updateAlarms(final String devId) throws Exception {

    //    new Thread(new Runnable() {
    //        @Override
    //        public void run() {
        MySQLHelper sql = getDB();
                String param = "alarmget?token="+Miscellaneous.getToken(a)+"&hl="+Locale.getDefault().getLanguage()+"&devid="+devId;
                alarms.clear();
                if(!sql.removeAlarms(devId)){
                    System.out.println("ALARM WAS NOT ABLE TO BE REMOVED WITH DEVID: "+devId);
                }
                OkHttpClient c = new OkHttpClient();
                String url = GlobalVariables.DOMAIN+param;
                System.out.println(url);
                RequestBody body = RequestBody.create(TEXT, GlobalVariables.DOMAIN + param);
                Request request = new Request.Builder().url(url).post(body).build();
                Response response = null;
                InputStream bstream = null;
                try {
                    response = c.newCall(request).execute();
                    bstream = response.body().byteStream();
                } catch (Exception e){
                    e.printStackTrace();
                }

                int data = 0;
                try {
                    data = bstream.read();
                } catch (Exception e){

                }

                byte[] array = new byte[512];

                int j = 0;
                while(data != -1) {
                    array[j] = (byte)data;
                    System.out.printf("0x%02X",data);
                    try {
                        data = bstream.read();
                    } catch (Exception e){
                        e.printStackTrace();
                    }
                    j++;
                }

                for (int i = 0; i < array.length -12 ; i+=12) {
                    int serviceId = process_long(array[i], array[i+1], array[i+2], array[i+3]);

                    if(serviceId != 0) {
                        Alarm a = new Alarm();
                        a.setDevice_id(devId);
                        if(serviceId == GlobalVariables.ALARM_RELAY_SERVICE) {
                            a.setService_id(GlobalVariables.ALARM_RELAY_SERVICE);
                        } else if(serviceId == GlobalVariables.ALARM_NIGHLED_SERVICE){
                            a.setService_id(GlobalVariables.ALARM_NIGHLED_SERVICE);
                        } else if(serviceId == GlobalVariables.ALARM_IR_SERVICE){
                            a.setService_id(GlobalVariables.ALARM_IR_SERVICE);
                        }

                        System.out.println("SERVICE FROM SERVER: "+a.getService_id());

                        a.setDow(array[i + 7]);
                        a.setInit_hour(array[i + 8]);
                        a.setInit_minute(array[i + 9]);
                        a.setEnd_hour(array[i + 10]);
                        a.setEnd_minute(array[i + 11]);
                        System.out.println("ALARM GET CONTROL - Service Id: "+a.getService_id()+", DOW: "+a.getDow()+", Init Hour: "+a.getInit_hour()+", Init Minute:"+a.getInit_minute()+", End Hour: "+a.getEnd_hour()+", End Minute: "+a.getEnd_minute());
                        alarms.add(a);
                    }
                }

                if(alarms.size() > 0){
    //                if(sql.removeAlarms(devId)) {
                        for (int i = 0; i < alarms.size(); i++) {
                            if (sql.insertAlarm(alarms.get(i))) {
                                System.out.println("ALARM INSERTED");
                            } else {
                                System.out.println("ALARM INSERTION FAILURE");
                            }
                        }
    //                }
                } else {
    //                sql.removeAlarms(devId);
                }
 //               Intent i = new Intent("status_changed_update_ui");
 //               a.sendBroadcast(i);
    //        }
    //    }).start();

        return true;
    }

    public boolean setDeviceTimers(byte[] array, int send) throws Exception{
        boolean toReturn = false;
        String param = "devctrl?token=" + Miscellaneous.getToken(a) + "&hl=" + Locale.getDefault().getLanguage() + "&devid=" + M1.mac +"&send="+send+"&ignoretoken="+ RegistrationIntentService.regToken;
        String url = GlobalVariables.DOMAIN+param;
        Log.i("SET DEVICE TIMES", url);
        RequestBody body = RequestBody.create(TEXT, array);
        Request request = new Request.Builder().url(url).post(body).build();
        Response response = null;
        try {
            response = client.newCall(request).execute();
        } catch (Exception e){
            toReturn = false;
            return toReturn;
        }
        String jsonData = response.body().string().toString();
        System.out.println(jsonData);
        try {

            JSONObject Jobject = new JSONObject(jsonData);
            toReturn = checkReturn(Jobject);
        }catch(Exception e){
            toReturn = false;
            e.printStackTrace();
        }

        return toReturn;
    }

    public boolean setTimerDelay(byte[] array, int send) throws Exception {
        boolean toReturn = false;
        String param = "devctrl?token=" + Miscellaneous.getToken(a) + "&hl=" + Locale.getDefault().getLanguage() + "&devid=" + M1.mac +"&send="+send+"&ignoretoken="+ RegistrationIntentService.regToken;
        System.out.println(param);

        String url = GlobalVariables.DOMAIN+param;
     //   Log.i("DELAY TIMER", url);
        RequestBody body = RequestBody.create(TEXT, array);
        Request request = new Request.Builder().url(url).post(body).build();
        Response response = client.newCall(request).execute();
        String json = response.body().string().toString();
        Log.i("SET TIMER DELAY", json);
        try {

            JSONObject Jobject = new JSONObject(json);
            toReturn = checkReturn(Jobject);
        }catch(Exception e){
            e.printStackTrace();
        }

        return toReturn;
    }

    public int process_long(byte a, byte b, byte c, byte d){

        ByteBuffer buffer = ByteBuffer.allocate(4);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        buffer.put(d);
        buffer.put(c);
        buffer.put(b);
        buffer.put(a);

        return buffer.getInt(0);
    }

    public short process_short(byte a, byte b){
        ByteBuffer buffer = ByteBuffer.allocate(2);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        buffer.put(b);
        buffer.put(a);
        return buffer.getShort(0);
    }

    public void manageIRGroup( String devid, int serviceId, String type, String action, int groupId, String name, int icon, int res, int tempId){
        MySQLHelper sql = getDB();
        String param = "devirset?token="+Miscellaneous.getToken(a)+"&hl="+Locale.getDefault().getLanguage()+"&devid="+devid+"&serviceid="+serviceId+"&type="+type+"&action="+action+"&groupid="+groupId+"&name="+name+"&icon="+icon+"&res="+res;
        System.out.println(param);
        String jsonData = "";
        try {
            jsonData = getJSONData(param);
            JSONObject job = new JSONObject(jsonData);
            boolean toReturn = checkReturn(job);
            if(toReturn){
                int id = job.getInt("id");
                sql.updateIRGroupID(tempId, id);
            }
            System.out.println(jsonData);
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    public void getServerIR( String devid, int serviceId, int res){
        MySQLHelper sql = getDB();
        String param = "devirget?token="+Miscellaneous.getToken(a)+"&hl="+Locale.getDefault().getLanguage()+"&devid="+devid+"&serviceid="+serviceId+"&res="+res;
        if(devid == null || devid.isEmpty()){
            devid = M1.mac;
        }
        System.out.println(param);
        String jsonData = "";
        try {
            if(devid != null && !devid.isEmpty()) {
                jsonData = getJSONData(param);
                System.out.println(jsonData);
                JSONObject job = new JSONObject(jsonData);
                int r = job.getInt("r");
                if (r == 0) {
                    sql.deleteIRGroups();
                    JSONArray groupArray = job.getJSONArray("groups");
                    for (int i = 0; i < groupArray.length(); i++) {
                        JSONObject group = groupArray.getJSONObject(i);
                        sql.deleteIRGroupBySID(Integer.parseInt(group.getString("id")));
                        sql.deleteIRCodes(Integer.parseInt(group.getString("id")));
                        sql.insertIRGroup(group.getString("title"), devid, group.getString("icon"), 0, Integer.parseInt(group.getString("id")));
                        JSONArray buttons = group.getJSONArray("buttons");
                        for (int j = 0; j < buttons.length(); j++) {
                            JSONObject button = buttons.getJSONObject(j);
                            try {
                                sql.insertIRCodes(Integer.parseInt(group.getString("id")), button.getString("title"), Integer.parseInt(button.getString("code")), button.getString("icon"), devid, Integer.parseInt(button.getString("id")));
                            } catch (Exception e) {
                                Log.i("DB", "Record not added because it exist on the db");
                            }
                        }
                    }
                } else {
                    String m = job.getString("m");
                    System.out.println(m);
                }
            }

        } catch (Exception e){
            e.printStackTrace();
        }
    }

    public boolean manageIRButton(String devId, int serviceId, String type, String action, int groupId, int buttonId, String name, int icon, int code, int res){
        MySQLHelper sql = getDB();
        boolean toReturn = false;
        String param = "devirset?token="+Miscellaneous.getToken(a)+"&hl="+Locale.getDefault().getLanguage()+"&devid="+devId+"&serviceid="+serviceId+"&type="
                +type+"&action="+action+"&groupid="+groupId+"&buttonid="+buttonId+"&name="+name+"&icon="+icon+"&code="+code+"&res="+res;
        System.out.println(param);
        if(devId == null || devId.isEmpty()){
            devId = M1.mac;
        }
        String jsonData = "";
        try {
            if(devId != null && !devId.isEmpty()) {
                jsonData = getJSONData(param);
                JSONObject job = new JSONObject(jsonData);
                toReturn = checkReturn(job);
                if (toReturn) {
                    int id = job.getInt("id");
                    sql.updateIRCodeSID(code, id);
                   // getServerIR(token, Locale.getDefault().getLanguage(), devId, serviceId, res);
                }
                System.out.println(jsonData);
            }
        } catch (Exception e){
            e.printStackTrace();
        }
        return toReturn;
    }

}
