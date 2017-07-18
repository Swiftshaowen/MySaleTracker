package com.ape.saletracker;

import android.content.Context;
import android.os.Environment;
import android.os.SystemProperties;
import android.util.Config;
import android.util.Log;
import android.util.Xml;

import com.ape.saletracker.ApeConfigParser;

import org.xmlpull.v1.XmlPullParser;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Created by android on 6/30/16.
 */
public class SaleTrackerUti {

    private static final String TAG = "SaleTracker";
    private static final String CLASS_NAME = "SaleTrackerUti---->";
    public static final String FEATURE_FILE_PATRH = "/etc/apps/ApeSaleTracker/config.xml";
    private static ApeConfigParser mApeConfigParser  = null;

    private static final String CONFIG_CLIENT_NO = "client_no";
    private static final String CONFIG_SEND_TYPE = "send_type";
    private static final String CONFIG_NOTICE = "notice";
    private static final String CONFIG_HOST_URL = "host_url";
    private static final String CONFIG_START_TIME = "start_time";
    private static final String CONFIG_SPACE_TIME = "space_time";

    private static Map<String, String> configMap = new HashMap<>();

    private static void initFeatureConfigIfNull(Context context) {
        if (mApeConfigParser == null) {
            mApeConfigParser = new ApeConfigParser(context, FEATURE_FILE_PATRH);
        }
    }

    public static Map<String, String> readSendParamFromXml(Context context) {
        initFeatureConfigIfNull(context);

        if(configMap != null){
            configMap.clear();
        }

        String client_no = mApeConfigParser.getString(CONFIG_CLIENT_NO, Contant.DEFAULT_CLIENT_NO);
        configMap.put(CONFIG_CLIENT_NO, client_no);

        String send_type = mApeConfigParser.getString(CONFIG_SEND_TYPE, String.valueOf(Contant.MSG_SEND_BY_NET));
        configMap.put(CONFIG_SEND_TYPE, send_type);

        String notice = mApeConfigParser.getString(CONFIG_NOTICE, "false");
        configMap.put(CONFIG_NOTICE, notice);

        String host_url = mApeConfigParser.getString(CONFIG_HOST_URL, "http://eservice.tinno.com/eservice/stsReport?reptype=report");
        configMap.put(CONFIG_HOST_URL, host_url);

        String start_time = mApeConfigParser.getString(CONFIG_START_TIME, String.valueOf(Contant.START_TIME));
        configMap.put(CONFIG_START_TIME, start_time);

        String space_time = mApeConfigParser.getString(CONFIG_SPACE_TIME, String.valueOf(Contant.SPACE_TIME));
        configMap.put(CONFIG_SPACE_TIME, space_time);

        Log.d(TAG, CLASS_NAME+"readSendParamFromXml: "
            +"\n client_no = "+client_no
            +"\n send_type = "+send_type
            +"\n notice = "+notice
            +"\n host_url = "+host_url
            +"\n start_time = "+start_time
            +"\n space_time = "+space_time);

        return  configMap;
    }

    /**
     * @param
     * @return
     *
     */
    public static boolean isQMobile(){
        if ("QMobile".equalsIgnoreCase(SystemProperties.get("ro.product.brand", "trunk"))) {
            return true;
        }
        return false;
    }






}
