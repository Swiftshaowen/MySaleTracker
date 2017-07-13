package com.ape.saletrackerBD;

/**
 * Created by android on 5/18/16.
 */
public class Contant {
    public static final String PACKAGENAME = "com.ape.saletrackerBD_";
    public static final String CLIENT_NO = "Z0010086";
    public static final int START_TIME = 180;
    public static final int SPACE_TIME = 60;
    public static final String SERVER_NUMBER = "6969";

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
    public static final String KEY_SERVER_NUMBER = "8646";

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

    public static final String KEY_SENDED_SUCCESS = "KEY_SENDED_SUCCESS";
    public static final String KEY_SENDED_TOME_SUCCESS = "KEY_SENDED_SUCCESS";
    public static final String KEY_SENDED_NUMBER = "KEY_SENDED_NUMBER";
}
