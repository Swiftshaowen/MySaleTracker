package com.ape.saletracker;

/**
 * Created by android on 5/18/16.
 */
public class Contant {
    public static final String PACKAGENAME = "com.ape.saletracker_";
    public static String DEFAULT_CLIENT_NO = "0000001000";
    public static int START_TIME = 180;
    public static int SPACE_TIME = 60;
    public static int DAY_TIME = 24*60;
    public static String SERVER_NUMBER = "8646";

    public static final int STS_JNI = 1;
    public static final int STS_NV = 2;
    public static final int STS_SP = 3;

    public static final int MSG_SEND_BY_SMS = 0;
    public static final int MSG_SEND_BY_NET = 1;
    public static final int MSG_SEND_BY_NET_AND_SMS = 2;

    public static final int MAX_SEND_COUNT_BY_SMS = 3; // this value must > 1
    public static final int MAX_SEND_CONUT_BY_NET = (365 * 24);

    public static final int ACTION_SEND_BY_SMS = 0;
    public static final int ACTION_SEND_BY_NET = 1;
    public static final int ACTION_SEND_RST_BY_NET = 2;

    public static final String KEY_OPEN_TIME = "KEY_OPEN_TIME";
    public static final String KEY_SPACE_TIME = "KEY_SPACE_TIME";
    public static final String KEY_DAY_TIME = "KEY_DAY_TIME";
    public static final String KEY_NOTIFY = "KEY_NOTIFY";
    public static final String KEY_SWITCH_SENDTYPE = "KEY_SWITCH_SENDTYPE";
    public static final String KEY_SELECT_SEND_TYPE = "KEY_SELECT_SEND_TYPE";

    public static final String STS_REFRESH = PACKAGENAME+"STS_REFRESH";
    public static final String STSDATA_CONFIG = PACKAGENAME+"STSDATA_CONFIG";
    public static final String ACTION_SMS_SEND = PACKAGENAME+"SMS_SEND_ACTION";
    public static final String ACTION_SMS_DELIVERED = PACKAGENAME+"SMS_DELIVERED_ACTION";
    public static final String NULL_IMEI = "000000000000000";


    //add send content to tme wap address
    public  static final String SEND_TO = "send_to";
    public  static final String SEND_TO_CUSTOM = "send_to_custom";
    public  static final String SEND_TO_TME = "send_to_TME";


    public  static final String ACTION_REFRESH_PANEL = PACKAGENAME+"ACTION_REFRESH_PANEL";

}
