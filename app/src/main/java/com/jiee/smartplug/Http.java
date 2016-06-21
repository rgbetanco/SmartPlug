package com.jiee.smartplug;

/** TO DO: DELETE THIS FILE.
 *
/*
 NewDeviceList.java
 Author: Chinsoft Ltd. | www.chinsoft.com
 */

import android.app.Activity;
import android.util.Log;

import com.jiee.smartplug.utils.MySQLHelper;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

import org.json.JSONObject;

public class Http {

    byte[] sMsg = new byte[24];

    public String post(String param, byte action) throws Exception {
        String responseStr = "";
        String domainLogin = "http://118.150.148.105:7001/api/";
        String dataUrl = domainLogin+param;
        String dataUrlParameters = param.substring(0, param.indexOf("?"));
        URL url;
        HttpURLConnection connection = null;
        try {
            // Create connection
            url = new URL(dataUrl);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            connection.setRequestProperty("Content-Length","" + Integer.toString(sMsg.length));
            connection.setRequestProperty("Content-Language", "en-US");
            connection.setUseCaches(false);
            connection.setDoInput(true);
            connection.setDoOutput(true);

            // Send request
            DataOutputStream wr = new DataOutputStream(
                    connection.getOutputStream());

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
            int serviceId = 0x1D000000;
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

            for(int i = 0; i < sMsg.length; i++){
                wr.writeByte(sMsg[i]);
            }

            //wr.write(header);
            wr.flush();
            wr.close();
            // Get Response
            InputStream is = connection.getInputStream();
            BufferedReader rd = new BufferedReader(new InputStreamReader(is));
            String line;

            while ((line = rd.readLine()) != null) {
                responseStr += line;
            }
            rd.close();

            //JSONObject jsonObject = new JSONObject(responseStr);

            //if(jsonObject.getString("m").equals("Please Reply")){
              //  post("devctrlTest?token="+MainActivity.token+"&hl=zh&devid="+M1.mac+"&isReply=true&relay=0", (byte)0x01);
            //}
            System.out.println("Response: "+responseStr);

        } catch (Exception e) {
            Log.d("Server error", "Big Error in http class");
            e.printStackTrace();

        } finally {

            if (connection != null) {
                connection.disconnect();
            }
        }
        return responseStr;
    }

    public String postCTRL(String param) throws Exception {
        String responseStr = "";
        String domainLogin = "http://118.150.148.105:7001/api/";
        String dataUrl = domainLogin+param;
        URL url;
        HttpURLConnection connection = null;
        try {
// Create connection
            url = new URL(dataUrl);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            connection.setRequestProperty("Content-Length","" + Integer.toString(dataUrl.getBytes().length));
            connection.setRequestProperty("Content-Language", "en-US");
            connection.setUseCaches(false);
            connection.setDoInput(true);
            connection.setDoOutput(true);
// Send request
            DataOutputStream wr = new DataOutputStream(
                    connection.getOutputStream());
            wr.writeBytes(dataUrl);
            wr.flush();
            wr.close();
// Get Response
            InputStream is = connection.getInputStream();
            BufferedReader rd = new BufferedReader(new InputStreamReader(is));
            String line;

            while ((line = rd.readLine()) != null) {
                responseStr += line;
            }

            rd.close();
            //    Log.d("Response", responseStr);

        } catch (Exception e) {
            Log.d("Server error", "Big Error in http class");
            e.printStackTrace();

        } finally {

            if (connection != null) {
                connection.disconnect();
            }
        }
        return responseStr;
    }
}