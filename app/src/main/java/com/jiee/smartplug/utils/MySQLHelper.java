package com.jiee.smartplug.utils;

/** TO DO:
 *
 /*
 MySQLHelper.java
 Author: Chinsoft Ltd. | www.chinsoft.com

 This class handles everything related to the local database ( delete/update/select )

 */

import android.content.ContentValues;
import android.content.Context;
import android.database.CursorIndexOutOfBoundsException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.Cursor;
import android.util.Log;

import com.jiee.smartplug.objects.Alarm;
import com.jiee.smartplug.objects.JSmartPlug;

import java.net.InetAddress;
import java.sql.SQLClientInfoException;

public class MySQLHelper extends SQLiteOpenHelper {

    // plugs information table
    public static final String TABLE_SMARTPLUGS = "smartplugs";
    public static final String COLUMN_ID = "_id";
    public static final String COLUMN_NAME = "name";
    public static final String COLUMN_GIVEN_NAME = "given_name";
    public static final String COLUMN_SID = "sid";
    public static final String COLUMN_IP = "ip";
    public static final String COLUMN_SERVER = "server";
    public static final String COLUMN_SNOOZE = "snooze";
    public static final String COLUMN_MODEL = "model";
    public static final String COLUMN_BUILD_NO = "build_no";
    public static final String COLUMN_PROT_VER = "prot_ver";
    public static final String COLUMN_HW_VER = "hw_ver";
    public static final String COLUMN_FW_VER = "fw_ver";
    public static final String COLUMN_FW_DATE = "fw_date";
    public static final String COLUMN_FLAG = "flag";
    public static final String COLUMN_RELAY = "relay";                                  // On/Off
    public static final String COLUMN_HSENSOR = "hsensor";                              // 1 overcurrent, 2 normal
    public static final String COLUMN_CSENSOR = "csensor";                              // 1 smoke detected, 2 normal, 3 unplugged
    public static final String COLUMN_NIGHTLIGHT = "nightlight";                        // On/Off
    public static final String COLUMN_ACTIVE = "active";
    public static final String COLUMN_NOTIFY_POWER = "notify_power";
    public static final String COLUMN_NOTIFY_CO = "notify_co";
    public static final String COLUMN_NOTIFY_TIMER = "notify_timer";
    public static final String COLUMN_INIT_IR = "init_ir";
    public static final String COLUMN_END_IR = "end_ir";

    // alarm table
    public static final String TABLE_ALARMS = "alarms";
    public static final String COLUMN_DEVICE_ID = "device_id";
    public static final String COLUMN_SERVICE_ID = "service_id";                        // 1 = relay, 2 nightled
    public static final String COLUMN_DOW = "dow";
    public static final String COLUMN_INIT_HOUR = "init_hour";
    public static final String COLUMN_INIT_MINUTES = "init_minute";
    public static final String COLUMN_END_HOUR = "end_hour";
    public static final String COLUMN_END_MINUTES = "end_minute";
    // IR CODES
    public static final String TABLE_IRCODES = "ircodes";
    public static final String COLUMN_GROUPID = "group_id";
    public static final String COLUMN_FILENAME = "filename";
    public static final String COLUMN_MAC = "mac";
    public static final String COLUMN_ICON = "icon";
    public static final String COLUMN_IRBRAND = "brand";
    public static final String COLUMN_IRMODEL = "model";
    //IR GROUPS
    public static final String TABLE_IRGROUPS = "irgroups";
    public static final String COLUMN_POSITION = "position";
    //PARAMS
    public static final String TABLE_PARAMS = "params";
    public static final String COLUMN_TOKEN = "token";
    //ICONS
    public static final String TABLE_ICONS = "icons";
    public static final String COLUMN_URL = "url";
    public static final String COLUMN_SIZE = "size";
    public static final String COLUMN_RELAY_SNOOZE = "relay_snooze";
    public static final String COLUMN_LED_SNOOZE = "led_snooze";
    public static final String COLUMN_IR_SNOOZE = "ir_snooze";

    private static final String DATABASE_NAME = "jsplugs.db";
    private static final int DATABASE_VERSION = 1;

    //create table for IR Codes
    private static final String TABLE_CREATE_IRCODE = "create table "
            + TABLE_IRCODES + "( "+COLUMN_ID+" integer primary key autoincrement, "
            + COLUMN_GROUPID +" integer not null,"+ COLUMN_NAME + " text, " + COLUMN_FILENAME+" integer, "
            + COLUMN_ICON+" text, "+COLUMN_MAC+" text, "+COLUMN_POSITION+" integer, "+COLUMN_IRBRAND+" text, "
            + COLUMN_IRMODEL+" text, "+COLUMN_SID+" integer );";

    // Database creation sql statement
    private static final String TABLE_CREATE_SMARTPLUG = "create table "
            + TABLE_SMARTPLUGS + "(" + COLUMN_ID
            + " integer primary key autoincrement, " + COLUMN_NAME +" text not null, " + COLUMN_SID
            + " text, "+ COLUMN_IP + " text, "+ COLUMN_SERVER +" text, "
            + COLUMN_MODEL+" text, "+COLUMN_BUILD_NO+" integer, "+COLUMN_PROT_VER+" integer, "
            + COLUMN_HW_VER+" text, "+COLUMN_FW_VER+" text, "+COLUMN_FW_DATE+" integer, "
            + COLUMN_FLAG+" integer, "+COLUMN_RELAY+" integer, "+COLUMN_HSENSOR+" integer, "
            + COLUMN_CSENSOR+" integer, "+COLUMN_NIGHTLIGHT+" integer, "+COLUMN_ACTIVE+" integer, "
            + COLUMN_ICON +" text, "+ COLUMN_NOTIFY_POWER +" integer, "+ COLUMN_NOTIFY_CO +" integer, "
            + COLUMN_NOTIFY_TIMER +" integer, "+ COLUMN_GIVEN_NAME +" text, "+ COLUMN_SNOOZE+" integer, "
            + COLUMN_LED_SNOOZE+" integer, "+COLUMN_IR_SNOOZE+" integer );";

    // Database creation sql statement
    private static final String TABLE_CREATE_ALARM = "create table "
            + TABLE_ALARMS + "(" + COLUMN_ID
            + " integer primary key autoincrement, " + COLUMN_DEVICE_ID +" text not null, " + COLUMN_SERVICE_ID
            + " integer not null, "+ COLUMN_DOW + " integer not null, "+ COLUMN_INIT_HOUR +" integer not null, "+ COLUMN_INIT_MINUTES
            + " integer not null, "+COLUMN_END_HOUR+" integer not null, "+ COLUMN_END_MINUTES +" integer not null, "+ COLUMN_SNOOZE
            + " integer, "+COLUMN_INIT_IR+" integer, "+COLUMN_END_IR+" integer );";

    private static final String TABLE_CREATE_PARAMS = "create table "
            + TABLE_PARAMS + "(" + COLUMN_ID
            + " integer primary key autoincrement, " + COLUMN_TOKEN + " text, "+COLUMN_RELAY_SNOOZE
            + " integer, "+COLUMN_LED_SNOOZE+" integer);";

    private static final String TABLE_CREATE_IRGROUPS = "create table "
            + TABLE_IRGROUPS + "(" + COLUMN_ID
            + " integer primary key autoincrement, " + COLUMN_NAME + " text, " + COLUMN_ICON + " text, "+ COLUMN_POSITION
            + " integer, "+COLUMN_SID+" integer unique, "+COLUMN_MAC+" text); ";

    private static final String TABLE_CREATE_ICONS = "create table "
            + TABLE_ICONS + "(" + COLUMN_ID + " integer primary key autoincrement, "+ COLUMN_SID + " text, "
            + COLUMN_URL + " text unique, " + COLUMN_SIZE + " integer)";

    private static final String INSERT_TOKEN = "Insert into "+TABLE_PARAMS+" values(0, null, 0, 0)";


    public MySQLHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase database) {
        database.execSQL(TABLE_CREATE_SMARTPLUG);
        database.execSQL(TABLE_CREATE_ALARM);
        database.execSQL(TABLE_CREATE_PARAMS);
        database.execSQL(TABLE_CREATE_IRCODE);
        database.execSQL(TABLE_CREATE_IRGROUPS);
        database.execSQL(TABLE_CREATE_ICONS);
        database.execSQL(INSERT_TOKEN);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.w(MySQLHelper.class.getName(),
                "Upgrading database from version " + oldVersion + " to "
                        + newVersion + ", which will destroy all old data");
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_SMARTPLUGS);
        onCreate(db);
    }

    public boolean updateDeviceSnooze(String device_id, int service_id, int v){
        boolean toReturn = false;
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        if(service_id == GlobalVariables.ALARM_RELAY_SERVICE) {
            cv.put(COLUMN_SNOOZE, v);
        }
        if (service_id == GlobalVariables.ALARM_NIGHLED_SERVICE){
            cv.put(COLUMN_LED_SNOOZE, v);
        }
        if(service_id == GlobalVariables.ALARM_IR_SERVICE){
            cv.put(COLUMN_IR_SNOOZE, v);
        }
        String filter = COLUMN_SID+" = '"+device_id+"'";
        int i = db.update(TABLE_SMARTPLUGS, cv, filter, null);
        if(i > 0){
            toReturn = true;
        }
        return toReturn;
    }

    public int getRelaySnooze(String deviceId){
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery("select "+COLUMN_SNOOZE+" from "+TABLE_SMARTPLUGS+" where "+COLUMN_SID+" = '"+deviceId+"'", null);
        int snooze = 0;
        if(c.getCount()>0){
            c.moveToFirst();
            snooze = c.getInt(0);
        }
        c.close();
        return snooze;
    }

    public int getLedSnooze(String deviceId){
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery("select " + COLUMN_LED_SNOOZE + " from " + TABLE_SMARTPLUGS + " where " + COLUMN_SID + " = '" + deviceId+"'", null);
        int snooze = 0;
        if(c.getCount()>0){
            c.moveToFirst();
            snooze = c.getInt(0);
        }
        c.close();
        return snooze;
    }

    public int getIRSnooze(String deviceId){
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery("select " + COLUMN_IR_SNOOZE + " from " + TABLE_SMARTPLUGS + " where " + COLUMN_SID + " = '" + deviceId+"'", null);
        int snooze = 0;
        if(c.getCount()>0){
            c.moveToFirst();
            snooze = c.getInt(0);
        }
        c.close();
        return snooze;
    }

    public boolean insertIcons(String url, int size, String id){
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COLUMN_URL, url);
        cv.put(COLUMN_SID, id);
        cv.put(COLUMN_SIZE, size);
        try {
            db.insert(TABLE_ICONS, null, cv);
            Log.v("MySQLHelper", "ICON INSERTED");
        } catch (Exception e){
            Log.v("MySQLHelper", "DUPLICATED FIELD");
        }
        return true;
    }

    public Cursor getIcons(){
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("select * from " + TABLE_ICONS, null);
    }

    public Cursor getIconByUrl(String url){
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("select * from " + TABLE_ICONS + " where url = '" + url + "'", null);
    }

    public int insertIRGroup(String desc, String devid, String icon, int position, int sid){
        SQLiteDatabase read = this.getReadableDatabase();
        Cursor c = read.rawQuery("select * from "+TABLE_IRGROUPS+" where "+COLUMN_SID+" = "+sid, null );
        if(c.getCount() == 0) {
            SQLiteDatabase db = this.getWritableDatabase();
            ContentValues cv = new ContentValues();
            cv.put(COLUMN_NAME, desc);
            cv.put(COLUMN_ICON, icon);
            cv.put(COLUMN_POSITION, position);
            cv.put(COLUMN_SID, sid);
            cv.put(COLUMN_MAC, devid);
            try {
                db.insert(TABLE_IRGROUPS, null, cv);
            } catch (Exception e) {
                Log.i("RECORD EXIST", "GROUP NOT ADDED");
            }
        }
        c.close();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cx = db.rawQuery("select seq from sqlite_sequence where name='" + TABLE_IRGROUPS + "'", null);
        cx.moveToFirst();
        int i = cx.getInt(0);
        cx.close();
        return i;
    }

    public Cursor getIRGroupByName(String groupName){
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor res = db.rawQuery("select * from " + TABLE_IRGROUPS + " where " + COLUMN_NAME + " = '" + groupName + "'", null);
        return res;
    }

    public Cursor getIRGroups(){
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor res =  db.rawQuery("select * from " + TABLE_IRGROUPS, null);
        return res;
    }

    public boolean deleteIRGroups(){
        SQLiteDatabase db = this.getReadableDatabase();
        boolean toreturn = db.delete(TABLE_IRGROUPS, null, null) > 0;
        if (toreturn){
            toreturn = true;
        } else {
            toreturn = false;
        }
        return toreturn;
    }

    public Cursor getIRCodeById(int id){
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor res = db.rawQuery("select "+COLUMN_NAME+" from "+TABLE_IRCODES+" where "+COLUMN_FILENAME+" = "+id, null);
        return res;
    }

    public boolean deleteIRGroup(int id){
        SQLiteDatabase db = this.getReadableDatabase();
        boolean toreturn = db.delete(TABLE_IRGROUPS, COLUMN_ID+" = "+id, null) > 0;
        if (toreturn){
            if(deleteIRCodes(id)){
                toreturn = true;
            } else {
                toreturn = false;
            }
        }
        return toreturn;
    }

    public boolean deleteIRCodes(int groupid){
        SQLiteDatabase db = this.getReadableDatabase();
        boolean toreturn = db.delete(TABLE_IRCODES, COLUMN_GROUPID+" = "+groupid, null) > 0;
        return toreturn;
    }

    public boolean deleteIRGroupBySID(int id){
        SQLiteDatabase db = this.getReadableDatabase();
        boolean toreturn = db.delete(TABLE_IRGROUPS, COLUMN_SID+" = "+id, null) > 0;
        if (toreturn){
            if(deleteIRCodesBySID(id)){
                toreturn = true;
            } else {
                toreturn = false;
            }
        }
        return toreturn;
    }

    public boolean deleteIRCodesBySID(int groupid){
        SQLiteDatabase db = this.getReadableDatabase();
        boolean toreturn = db.delete(TABLE_IRCODES, COLUMN_GROUPID+" = "+groupid, null) > 0;
        return toreturn;
    }

    public boolean deleteIRCode(int sid){
        SQLiteDatabase db = this.getReadableDatabase();
        boolean toreturn = db.delete(TABLE_IRCODES, COLUMN_SID+" = "+sid, null) > 0;
        return toreturn;
    }

    public Cursor getIRGroups(int id){
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor res =  db.rawQuery("select * from " + TABLE_IRGROUPS + " where " + COLUMN_ID + " = " + id, null);
        return res;
    }

    public Cursor getIRGroupBySID(int id){
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor res =  db.rawQuery("select * from " + TABLE_IRGROUPS + " where " + COLUMN_SID + " = " + id, null);
        return res;
    }

    public Cursor getIRGroupByMac(String mac){
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor res = db.rawQuery("select * from "+TABLE_IRGROUPS+" where "+COLUMN_MAC+" = '"+mac+"'", null);
        return res;
    }

    public boolean insertIRCodes(int gid, String name, int filename, String icon, String mac, int id){
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COLUMN_GROUPID, gid);
        cv.put(COLUMN_NAME, name);
        cv.put(COLUMN_FILENAME, filename);
        cv.put(COLUMN_ICON, icon);
        cv.put(COLUMN_MAC, mac);
        cv.put(COLUMN_SID, id);
        db.insertOrThrow(TABLE_IRCODES, null, cv);
        return true;
    }

    public boolean updateIRCodeSID(int filename, int sid){
        boolean toReturn = false;
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COLUMN_SID, sid);
        String filter = COLUMN_FILENAME+"="+filename;
        db.update(TABLE_IRCODES, cv, filter, null);
        return toReturn;
    }

    public boolean updateIRGroupID(int groupId, int id){
        boolean toReturn = false;
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COLUMN_SID, id);
        String filter = COLUMN_ID+"="+groupId;
        db.update(TABLE_IRGROUPS, cv, filter, null);
        return toReturn;
    }

    public Cursor getIRCodes(){
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor res =  db.rawQuery("select * from " + TABLE_IRCODES, null);
        return res;
    }

    public Cursor getIRCodesByGroup(int id){
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor res =  db.rawQuery("select * from " + TABLE_IRCODES +" where "+COLUMN_GROUPID+" = "+id, null);
        return res;
    }

    public boolean insertPlug (JSmartPlug js, int active)
    {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        System.out.println("COLUMN_NAME:" + js.getName() + " COLUMN_GIVEN_NAME: " + js.getGivenName() + " COLUMN_IP: "+js.getIp() );
        contentValues.put(COLUMN_NAME, js.getName());
        contentValues.put(COLUMN_ICON, js.getIcon());
        contentValues.put(COLUMN_SID, js.getId());
        //if(js.getIp()!=null && !js.getIp().isEmpty() && !js.getIp().equals("null"))
        //    contentValues.put(COLUMN_IP, js.getIp());
        contentValues.put(COLUMN_MODEL, js.getModel());
        contentValues.put(COLUMN_BUILD_NO, js.getBuildno());
        contentValues.put(COLUMN_PROT_VER, js.getProt_ver());
        contentValues.put(COLUMN_HW_VER, js.getHw_ver());
        contentValues.put(COLUMN_FW_VER, js.getFw_ver());
        contentValues.put(COLUMN_FW_DATE, js.getFw_date());
        contentValues.put(COLUMN_FLAG, js.getFlag());
        contentValues.put(COLUMN_RELAY, js.getRelay());
        contentValues.put(COLUMN_HSENSOR, js.getHall_sensor());
        contentValues.put(COLUMN_CSENSOR, js.getCo_sensor());
        contentValues.put(COLUMN_NIGHTLIGHT, js.getNightlight());
        contentValues.put(COLUMN_GIVEN_NAME, js.getGivenName());
        contentValues.put(COLUMN_ACTIVE, active);
        db.insert(TABLE_SMARTPLUGS, null, contentValues);
        return true;
    }

    public boolean updatePlugID(String name, String id){
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COLUMN_SID, id);
        String filter = COLUMN_NAME+"='"+name+"'";
        db.update(TABLE_SMARTPLUGS, cv, filter, null);
        return true;
    }

    public boolean updatePlugIP(String name, String ip){
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        if(ip != null && !ip.isEmpty()) {
            cv.put(COLUMN_IP, ip);
            String filter = COLUMN_NAME + "='" + name + "'";
            int i = db.update(TABLE_SMARTPLUGS, cv, filter, null);
            System.out.println("NAME: " + name + " IP: " + ip + ", UPDATED: " + i);
        } else {
            System.out.println("IP NOT UPDATED BECAUSE IP IS NULL OR EMPTY");
        }

        return true;
    }

    public boolean updatePlugName(String data, String id){
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COLUMN_GIVEN_NAME, data);
        String filter = COLUMN_SID + " = '" + id + "'";
        db.update(TABLE_SMARTPLUGS, cv, filter, null);
        return true;
    }

    public boolean updatePlugNightlightService(int data, String id){
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COLUMN_NIGHTLIGHT, data);
        String filter = COLUMN_SID+" = '"+id+"'";
        db.update(TABLE_SMARTPLUGS, cv, filter, null);
        return true;
    }

    public boolean updatePlugCoSensorService(int data, String id){
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COLUMN_CSENSOR, data);
        String filter = COLUMN_SID+" = '"+id+"'";
        db.update(TABLE_SMARTPLUGS, cv, filter, null);
        return true;
    }

    public boolean updatePlugHallSensorService(int data, String id){
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COLUMN_HSENSOR, data);
        String filter = COLUMN_SID+" = '"+id+"'";
        db.update(TABLE_SMARTPLUGS, cv, filter, null);
        return true;
    }

    public boolean updateSnooze(int data, String id){
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COLUMN_SNOOZE, data);
        String filter = COLUMN_SID+" = '"+id+"'";
        db.update(TABLE_SMARTPLUGS, cv, filter, null);
        return true;
    }

    public boolean updatePlugRelayService(int data, String id){
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COLUMN_RELAY, data);
        String filter = COLUMN_SID+" = '"+id+"'";
        db.update(TABLE_SMARTPLUGS, cv, filter, null);
        return true;
    }

    public boolean updatePlugServicesByIP(JSmartPlug js){
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues args = new ContentValues();
        args.put(COLUMN_RELAY, js.getRelay());
        args.put(COLUMN_HSENSOR, js.getHall_sensor());
        args.put(COLUMN_CSENSOR, js.getCo_sensor());
        args.put(COLUMN_NIGHTLIGHT, js.getNightlight());
        args.put(COLUMN_HW_VER, js.getHw_ver());
        args.put(COLUMN_FW_VER, js.getFw_ver());
        String strFilter = COLUMN_IP + " = '" + js.getIp() + "'";
        db.update(TABLE_SMARTPLUGS, args, strFilter, null);
        return true;
    }

    public boolean updateDeviceVersions(String idLocal, String model, int buildnumber, int protocol, String hardware_version, String firmware_version, int firmwaredate){
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues args = new ContentValues();
        args.put(COLUMN_MODEL, model);
        args.put(COLUMN_BUILD_NO, buildnumber);
        args.put(COLUMN_PROT_VER, protocol);
        args.put(COLUMN_HW_VER, hardware_version);
        args.put(COLUMN_FW_VER, firmware_version);
        args.put(COLUMN_FW_DATE, firmwaredate);
        String strFilter = COLUMN_SID + " = '" + idLocal + "'";
        db.update(TABLE_SMARTPLUGS, args, strFilter, null);
        return true;
    }

    public boolean updatePlugServicesByID(JSmartPlug js){
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues args = new ContentValues();
    //    args.put(COLUMN_SID, js.getId());
        args.put(COLUMN_RELAY, js.getRelay());
        args.put(COLUMN_HSENSOR, js.getHall_sensor());
        args.put(COLUMN_CSENSOR, js.getCo_sensor());
        args.put(COLUMN_NIGHTLIGHT, js.getNightlight());
        args.put(COLUMN_HW_VER, js.getHw_ver());
        args.put(COLUMN_FW_VER, js.getFw_ver());
        if(js.getGivenName()!=null && !js.getGivenName().isEmpty()){
            System.out.println("UPDATING PLUG WITH GIVEN NAME: "+js.getGivenName()+" MAC: "+js.getId());
            args.put(COLUMN_GIVEN_NAME, js.getGivenName());
        }
        String strFilter = COLUMN_SID + " = '" + js.getId() + "'";
        if(db.update(TABLE_SMARTPLUGS, args, strFilter, null)>0){
            System.out.println("SERVICE UPDATED SUCCESSFULLY");
        }
        return true;
    }

    public boolean deactivatePlug(String sid){
        SQLiteDatabase db = this.getWritableDatabase();
        String strFilter = COLUMN_SID+" = '" + sid +"'";
        ContentValues args = new ContentValues();
        args.put(COLUMN_ACTIVE, 0);
        db.update(TABLE_SMARTPLUGS, args, strFilter, null);
        return true;
    }

    public boolean updateToken (String token){
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(COLUMN_TOKEN, token);
        if(db.update(TABLE_PARAMS, contentValues, COLUMN_ID +" = 0", null) == 1){
            return true;
        } else {
            return false;
        }
    }

    public Cursor getToken(){
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor res =  db.rawQuery("select * from " + TABLE_PARAMS, null);
        return res;
    }

    public boolean removeToken(){
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COLUMN_TOKEN, "");
        if (db.update(TABLE_PARAMS, cv, null, null) == 1){
            db.close();
            return true;
        } else {
            db.close();
            return false;
        }
    }

    public Cursor getPlugData(String ip){
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor res = db.rawQuery("select * from " + TABLE_SMARTPLUGS + " where " + COLUMN_IP + " = '" + ip + "' and active = 1", null);
        return res;
    }

    public Cursor getPlugDataByID(String id){
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor res =  db.rawQuery("select * from " + TABLE_SMARTPLUGS + " where " + COLUMN_SID + " = '" + id + "' and active = 1", null);
        return res;
    }

    public Cursor getPlugDataByName(String name){
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor res =  db.rawQuery("select * from " + TABLE_SMARTPLUGS + " where "+COLUMN_NAME+" = '" + name + "' and active = 1", null);
        System.out.println("Record "+name+" Found on DB");
        return res;
    }

    public Cursor getPlugData(){
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor res =  db.rawQuery("select * from " + TABLE_SMARTPLUGS + " where active = 1", null);
        return res;
    }

    public String getPlugMacFromIP(InetAddress ia) {
        return getPlugMacFromIP( ia.getHostAddress() );
    }

    public String getPlugMacFromIP(String ip) {
        String mac = null;
        Cursor res = getPlugData(ip);
        if (res.getCount() > 0) {
            res.moveToFirst();
            for (int i = 0; i < res.getCount(); i++) {
                mac = res.getString(2);
                if( mac.length()==0 )
                    mac = null;
                break;
            }
        }
        res.close();

        return mac;
    }

    public String getPlugIP(String mac) {
        String ip = null;
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor res =  db.rawQuery("select * from " + TABLE_SMARTPLUGS + " where active = 1 and " + COLUMN_SID + "='" + mac + "'", null);
        if (res.getCount() > 0) {
            res.moveToFirst();
            for (int i = 0; i < res.getCount(); i++) {
                ip = res.getString(3);
                if( ip.length()==0 )
                    ip = null;
                break;
            }
        }
        res.close();

        return ip;
    }

    public InetAddress getPlugInetAddress(String mac) {

        if( mac==null ) {
            Log.v("MySQLHelper" , "getPlugInetAddress called with null ID");
            return null;
        }

        String s = (mac.contains("."))?mac:getPlugIP(mac);
        if (s == null)
            return null;

        try {
            return InetAddress.getByName(s);
        } catch( Exception e ) {
            return null;
        }
    }


    public Cursor getNonActivePlugData(){
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor res =  db.rawQuery("select * from " + TABLE_SMARTPLUGS + " where active = 0", null);
        return res;
    }

    public boolean deletePlugData(String id){
        SQLiteDatabase db = this.getWritableDatabase();
        int ret = db.delete(TABLE_SMARTPLUGS, COLUMN_SID + "='" + id + "'", null);
        System.out.println("Return "+ret+" when deleting the smartplug: "+id);
        boolean toreturn = ret > 0;
        return toreturn;
    }

    public boolean deletePlugDataByID(String mac){
        SQLiteDatabase db = this.getWritableDatabase();
        boolean toreturn = db.delete(TABLE_SMARTPLUGS, COLUMN_SID+"='"+mac+"'", null) > 0;
        return toreturn;
    }

    public boolean updatePlugNameNotify(String mac, String name, int notify_on_power_outage, int notify_on_co_warning, int notify_on_timer_activated, String icon){
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COLUMN_NOTIFY_POWER, notify_on_power_outage);
        cv.put(COLUMN_NOTIFY_CO, notify_on_co_warning);
        cv.put(COLUMN_NOTIFY_TIMER, notify_on_timer_activated);
        cv.put(COLUMN_GIVEN_NAME, name);
        cv.put(COLUMN_ICON, icon);
        if(mac != null) {
             db.update(TABLE_SMARTPLUGS, cv, COLUMN_SID + "='" + mac + "'", null);
            return true;
        } else {
            return false;
        }
    }

    public boolean deletePlugs(){
        boolean toReturn = false;
        SQLiteDatabase db = this.getWritableDatabase();

        int ret = db.delete(TABLE_SMARTPLUGS, COLUMN_ACTIVE + " = 1", null);
        if(ret > 0){
            toReturn = true;
            System.out.println("Deleted Successfully");
        } else {
            toReturn = false;
            System.out.println("NOT Deleted Successfully");
        }
        return toReturn;
    }

    public boolean deleteNonActivePlug(String name){
        SQLiteDatabase db = this.getWritableDatabase();
        if(name.equals("all")){
            db.delete(TABLE_SMARTPLUGS, COLUMN_ACTIVE + " = 0", null);
        } else {
            db.delete(TABLE_SMARTPLUGS, COLUMN_NAME + " = '" + name + "' AND " + COLUMN_ACTIVE + " = 0", null);
        }
        return true;
    }

    public boolean deleteAlarmData(int id){
        SQLiteDatabase db = this.getWritableDatabase();
        boolean toreturn = db.delete(TABLE_ALARMS, COLUMN_ID+" = "+id, null) > 0;
        return toreturn;
    }

    public boolean removeAlarms(String mac){
        SQLiteDatabase db = this.getWritableDatabase();
        return db.delete(TABLE_ALARMS, COLUMN_DEVICE_ID+" = '"+mac+"'", null) > 0;
    }

    public boolean insertAlarm (Alarm a)
    {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(COLUMN_DEVICE_ID, a.getDevice_id());
        contentValues.put(COLUMN_SERVICE_ID, a.getService_id());
        contentValues.put(COLUMN_DOW, a.getDow());
        contentValues.put(COLUMN_INIT_HOUR, a.getInit_hour());
        contentValues.put(COLUMN_INIT_MINUTES, a.getInit_minute());
        contentValues.put(COLUMN_END_HOUR, a.getEnd_hour());
        contentValues.put(COLUMN_END_MINUTES, a.getEnd_minute());
        contentValues.put(COLUMN_SNOOZE, a.getSnooze());
        contentValues.put(COLUMN_INIT_IR, a.getInit_ir());
        contentValues.put(COLUMN_END_IR, a.getEnd_ir());
        db.insert(TABLE_ALARMS, null, contentValues);
        return true;
    }

    public boolean updateAlarm(Alarm a){
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(COLUMN_DEVICE_ID, a.getDevice_id());
        contentValues.put(COLUMN_SERVICE_ID, a.getService_id());
        contentValues.put(COLUMN_DOW, a.getDow());
        contentValues.put(COLUMN_INIT_HOUR, a.getInit_hour());
        contentValues.put(COLUMN_INIT_MINUTES, a.getInit_minute());
        contentValues.put(COLUMN_END_HOUR, a.getEnd_hour());
        contentValues.put(COLUMN_END_MINUTES, a.getEnd_minute());
        contentValues.put(COLUMN_SNOOZE, a.getSnooze());
        contentValues.put(COLUMN_INIT_IR, a.getInit_ir());
        contentValues.put(COLUMN_END_IR, a.getEnd_ir());
        if(db.update(TABLE_ALARMS, contentValues, COLUMN_ID + " = "+ a.getAlarm_id(), null) == 1){
            System.out.println("ALARM UPDATED SUCCESSFULLY");
        } else {
            System.out.println("ERROR UPDATING ALARM");
        }
        return true;
    }

    public Cursor getAlarmData(int alarm_id){
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor res =  db.rawQuery("select * from " + TABLE_ALARMS + " where "+COLUMN_ID+" ='" + alarm_id + "'", null);
        return res;
    }

    public Cursor getAlarmData(String device_id){
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor res =  db.rawQuery("select * from " + TABLE_ALARMS + " where "+COLUMN_DEVICE_ID+" ='" + device_id + "'", null);
        return res;
    }

    public Cursor getAlarmData(String device_id, int service_id){
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor res =  db.rawQuery("select * from " + TABLE_ALARMS + " where "+COLUMN_DEVICE_ID+" = '" + device_id + "' and "+COLUMN_SERVICE_ID+" = "+service_id, null);
        return res;
    }

    @Override
    protected void finalize() throws Throwable {
        this.close();
        super.finalize();
    }

}