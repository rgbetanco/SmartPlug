package com.jiee.smartplug.utils;

/** TO DO:
 *
 /*
 GlobalVariables.java
 Author: Chinsoft Ltd. | www.chinsoft.com

 Global variables use on the app

 */

public class GlobalVariables {
    public static int ALARM_RELAY_SERVICE = 0xD1000000;
    public static int ALARM_NIGHLED_SERVICE = 0xD1000001;
    public static int ALARM_CO_SERVICE = 0xD1000002;
    public static int ALARM_IR_SERVICE = 0xD1000003;
    public static int IR_SERVICE = 0xD1000003;

    public static int SERVICE_FLAGS_NORMAL = 0x00000000;        // not problems reported by service
    public static int SERVICE_FLAGS_ERROR = 0x00000001;         // service is in error
    public static int SERVICE_FLAGS_WARNING = 0x00000002;       // service has warning
    public static int SERVICE_FLAGS_DISABLED = 0x00000004;      // service disabled/not available

    public static String DOMAIN = "http://g-shines.com/api/";
//    public static String DOMAIN = "http://flutehuang-001-site6.btempurl.com/api/";
    public static final String SENT_TOKEN_TO_SERVER = "sentTokenToServer";
    public static final String REGISTRATION_COMPLETE = "registrationComplete";
    public static final int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;
    public static final String plugfile = "plugicon.jpg";
    public static final String irgroupfile = "irgroup.jpg";
    public static final String irfile = "ir.jpg";
}
