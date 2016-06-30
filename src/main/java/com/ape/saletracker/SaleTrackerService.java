package com.ape.saletracker;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.telephony.SmsManager;
import android.telephony.gsm.GsmCellLocation;
import android.text.TextUtils;
import android.util.Log;
import android.util.Xml;

import com.wrapper.stk.HideMethod;
import com.wrapper.stk.HideMethod.TelephonyManager;

import org.xmlpull.v1.XmlPullParser;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class SaleTrackerService extends Service {
	private static final String TAG = "SaleTracker";
	private static final String CLASS_NAME = "SaleTrackerService---->";

    private static final String DEFAULT_VALUE = "defaultSet";
	private static final String publicKey = "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQCY4gRmZHQimOWRr99Yi64jGDGMJSa7Awx05J9gpJuQz9tZPrP6QCWFJNpBxBxS_UMg-36FjFl_l8qLBWl-q7pVlyc4qdxq4HGQKJfdBm8aOFQ3Ekaylm1p2s5YKxvYTHDydKG72EXDdvbea8ZvXA1rKP-MpOWKA7XmkLpChQqrsQIDAQAB";
	private static String mHosturl = "http://eservice.tinno.com/eservice/stsReport?reptype=report";
    private static String mTmeHosturl = "http://eservice.tinno.com/eservice/stsReport?reptype=report";
    private static String mClientNo = "0000000001";
    private static String NUM_SMS = "18565857256";	//  15920026432; 18565856119

	private static Context mContext;
	private String url;

	public static final int STS_CONFIG_TYPE = Contant.STS_SP;
	private static int mStartTimeFromXML = Contant.START_TIME;
	private static int mSpaceTimeFromXML = Contant.SPACE_TIME;

    private static boolean mSwitchSendType = false;
    private static boolean mNotifyFromTestActivity = false; // this param get value
    private static boolean mIsNeedSend = false;
    private static boolean mIsSendSuccess = false;
    private static boolean mIsNeedNoticePop = false;
    private static boolean airplaneModeOn = false;

    private long mCurrHour = 0;
    private long mCurrMinute = 0;
    private short mMsgSendNum = 0;
	public  static int mDefaultSendType = Contant.MSG_SEND_BY_NET;
    private static int mDefaultSendTypeTmp  = Contant.MSG_SEND_BY_NET;
	public  int  mStartTime = Contant.START_TIME;
	public  int  mSpaceTime = Contant.SPACE_TIME;
    public  String mStrIMEI = Contant.NULL_IMEI;

	private String mStrPhoneNo = DEFAULT_VALUE;
	private String mStrCountry = DEFAULT_VALUE;
	private String mStrModel = DEFAULT_VALUE;

	private static SaleTrackerConfigSP mStciSP = new SaleTrackerConfigSP();
	private final BroadcastReceiver mSaleTrackerReceiver = new SaleTrackerReceiver();
	private final BroadcastReceiver mStsAirplanReceiver = new StsAirplanReceiver();

	private static TelephonyManager mTm = TelephonyManager.getDefault();

	@Override
	public void onCreate() {
		super.onCreate();
		init();
	}

	private void init() {
		Log.d(TAG, CLASS_NAME+"init() start");
		mContext = getApplicationContext();
		mStciSP.init(mContext);

		initConfig();

		if (null == mTm) {
			Log.d(TAG, CLASS_NAME+"init() ********error******** TelephonyManager.getDefault() = null ********error********");
			return;
		}

		pickCountryConfigs();

		registerReceiver(mStsAirplanReceiver, new IntentFilter(Intent.ACTION_AIRPLANE_MODE_CHANGED));
		if(!airplaneModeOn)
		{
			Log.d(TAG, CLASS_NAME+"init()   registerReceiver mSaleTrackerReceiver");
			registerReceiver(mSaleTrackerReceiver, new IntentFilter(Contant.STS_REFRESH));
			registerReceiver(mSaleTrackerReceiver, new IntentFilter(Contant.ACTION_SMS_SEND));
			registerReceiver(mSaleTrackerReceiver, new IntentFilter(Contant.ACTION_SMS_DELIVERED));
		}
	}


	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	//add send content to tme wap address
	@Override
	public void onStart(Intent intent, int startId) {
		if(intent == null){
			Log.d(TAG, CLASS_NAME+"onStart()  ******************* intent = null*********************");
			super.onStart(intent, startId);
			return;
		}

		String type = intent.getStringExtra(Contant.SEND_TO);
		Log.d(TAG, CLASS_NAME+"onStart() this content sendto =" + type);
		if(Contant.SEND_TO_TME.equals(type)){
			mDefaultSendType = Contant.MSG_SEND_BY_NET;
			mDefaultSendTypeTmp = Contant.MSG_SEND_BY_NET;
			mHosturl = mTmeHosturl;
		}

		super.onStart(intent, startId);
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		Log.e(TAG, CLASS_NAME+"onDestroy() unregisterReceiver");

		airplaneModeOn = false;
        try {
            unregisterReceiver(mSaleTrackerReceiver);
        } catch (Exception e) {
            Log.e(TAG, CLASS_NAME+"onDestroy() Exception" + e.getMessage());
        }

		try {
			unregisterReceiver(mStsAirplanReceiver);
		} catch (Exception e) {
			Log.e(TAG, CLASS_NAME+"onDestroy() Exception" + e.getMessage());
		}

		AlarmManager am = (AlarmManager) mContext
				.getSystemService(Context.ALARM_SERVICE);
		Intent stsIntent = new Intent(Contant.STS_REFRESH);
		PendingIntent alarmIntent = PendingIntent.getBroadcast(
				getApplicationContext(), 0, stsIntent,
				PendingIntent.FLAG_UPDATE_CURRENT);
		am.cancel(alarmIntent);
	}

	private class StsAirplanReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
			Log.d(TAG, CLASS_NAME+"StsAirplanReceiver()  onReceive start");
			if (intent.getAction().equals(Intent.ACTION_AIRPLANE_MODE_CHANGED)) {
				boolean airplaneMode = intent.getBooleanExtra("state", false);
				if (airplaneMode) {
					Log.d(TAG,
							"StsAirplanReceiver()  : ACTION_AIRPLANE_MODE_CHANGED in airplane mSaleTrackerReceiver="
									+ mSaleTrackerReceiver);
					// guchunhua,DATE20150720,modify for FADALFRA-75,START
					try {
						SaleTrackerService.this
								.unregisterReceiver(mSaleTrackerReceiver);
					} catch (IllegalArgumentException e) {
						android.util.Log.e(TAG, CLASS_NAME+"StsAirplanReceiver()   registerReceiverSafe(), FAIL!");
					}
					// guchunhua,DATE20150720,modify for FADALFRA-75,END
				} else {
					Log.d(TAG,
							"StsAirplanReceiver() : ACTION_AIRPLANE_MODE_CHANGED out airplane");
					SaleTrackerService.this.registerReceiver(
							mSaleTrackerReceiver, new IntentFilter(
                                    Contant.STS_REFRESH));
					SaleTrackerService.this.registerReceiver(
							mSaleTrackerReceiver, new IntentFilter(
                                    Contant.ACTION_SMS_SEND));
					SaleTrackerService.this.registerReceiver(
							mSaleTrackerReceiver, new IntentFilter(
                                    Contant.ACTION_SMS_DELIVERED));
				}
			}
		}
	}


	private class SaleTrackerReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
			Log.d(TAG, CLASS_NAME+"SaleTrackerReceiver() onReceive start: action = "+intent.getAction());

			if (intent.getAction().equals(Contant.STS_REFRESH)) {
				long usedtime = SystemClock.elapsedRealtime() / 1000; //
				long sPassedMinute = usedtime / 60;
				Log.d(TAG,
						CLASS_NAME+"SaleTrackerReceiver()  usedtime = "+usedtime);
				long sPassedHour;

				if (mIsSendSuccess
						|| (mDefaultSendType == Contant.MSG_SEND_BY_SMS && mMsgSendNum > ((readConfigSms()
						+ Contant.MAX_SEND_COUNT_BY_SMS - 1) * 24))
						|| (mDefaultSendType != Contant.MSG_SEND_BY_SMS && mMsgSendNum > Contant.MAX_SEND_CONUT_BY_NET)) {
					Log.d(TAG,
							CLASS_NAME+"SaleTrackerReceiver()  The message is send success or the maximum sended number, stop SaleTrackerService");
					SaleTrackerService.this.stopSelf();
					return;
				}

				//read test value from testActivity,for quickly test
				SharedPreferences pre = getSharedPreferences(Contant.STSDATA_CONFIG,
						MODE_PRIVATE);
				mStartTime = pre.getInt(Contant.KEY_OPEN_TIME, mStartTimeFromXML);
				mSpaceTime = pre.getInt(Contant.KEY_SPACE_TIME, mSpaceTimeFromXML);
				mNotifyFromTestActivity = pre.getBoolean("KEY_NOTIFY",
						getResources().getBoolean(R.bool.dialog_notify));

				// for test send type only
				mSwitchSendType = pre.getBoolean(Contant.KEY_SWITCH_SENDTYPE, false);
				if (mSwitchSendType == true) {
					mDefaultSendType = pre.getInt(Contant.KEY_SELECT_SEND_TYPE, 0);
					Log.d(TAG,
							CLASS_NAME+"SaleTrackerReceiver() ***only for test -----open test send type switch  :   "
									+ mDefaultSendType);
				} else {
					mDefaultSendType = mDefaultSendTypeTmp;
				}

//				sPassedMinute += mStartTimeFromXML - mStartTime;
//				sPassedHour = sPassedMinute / 60;
//				// end
//				Log.d(TAG, CLASS_NAME+"SaleTrackerReceiver()   sPassedMinute= " + sPassedMinute + ",sPassedHour= " + sPassedHour
//						+ ",mCurrHour= " + mCurrHour + ",mCurrMinute= "
//						+ mCurrMinute+", mStartTime = "+mStartTime+"; mSpaceTime = "+mSpaceTime);

				/*if (sPassedMinute > (mCurrMinute + mSpaceTime - 1)) // test code
				{
					mIsNeedSend = ((sPassedMinute >= mStartTimeFromXML) || (mMsgSendNum >0)) ? true : false;
					if (0 == (mStartTime % mSpaceTime))//yu shu
						mCurrMinute = sPassedMinute - sPassedMinute % mSpaceTime;
					else
						mCurrMinute = sPassedMinute;

					Log.d(TAG, CLASS_NAME+"SaleTrackerReceiver() sPassedMinute % mSpaceTime= " + sPassedMinute % mSpaceTime
							+ "  mCurrMinute: " + mCurrMinute);
				} else {
					mIsNeedSend = false;
				}*/
				mIsNeedSend = true;

				if (mIsNeedSend) {
					int MsgSendMode = -1;
					Log.d(TAG, CLASS_NAME+"SaleTrackerReceiver() mDefaultSendType= " + mDefaultSendType
                        +"; readConfigSms = "+readConfigSms());

					switch (mDefaultSendType) {
						case Contant.MSG_SEND_BY_SMS:
							Log.d(TAG, CLASS_NAME+"SaleTrackerReceiver() send type by SMS  mMsgSendNum = "
									+ mMsgSendNum);
							if (((mMsgSendNum / 24) < readConfigSms()
									+ Contant.MAX_SEND_COUNT_BY_SMS)
									&& ((mMsgSendNum % 24) == 0)){
								MsgSendMode = Contant.ACTION_SEND_BY_SMS;
							}
							break;

						case Contant.MSG_SEND_BY_NET:
							Log.d(TAG, CLASS_NAME+"SaleTrackerReceiver() send type by NET  mMsgSendNum = "
									+ mMsgSendNum);
							MsgSendMode = Contant.ACTION_SEND_BY_NET;
							break;

						case Contant.MSG_SEND_BY_NET_AND_SMS:
							if (((mMsgSendNum / 24) < readConfigSms()
									+ Contant.MAX_SEND_COUNT_BY_SMS)
									&& ((mMsgSendNum % 24) == 0)){
								Log.d(TAG,
										"SaleTrackerReceiver() send type by NET_AND_SMS  mMsgSendNum = "
												+ mMsgSendNum);
								MsgSendMode = Contant.ACTION_SEND_BY_SMS;
							} else {
								Log.d(TAG,
										"SaleTrackerReceiver() MSG_SEND_BY_NET_AND_SMS-- net  mMsgSendNum = "
												+ mMsgSendNum);
								MsgSendMode = Contant.ACTION_SEND_BY_NET;
							}
							break;

					}

					popNotifyWindow(context, mDefaultSendType);

					writeConfigDay(++mMsgSendNum);

					Log.d(TAG, CLASS_NAME+"SaleTrackerReceiver() MsgSendMode = "+MsgSendMode);
					if (MsgSendMode > 0) {
						Message m = new Message();
						m.what = MsgSendMode;
						SaleTrackerService.this.MessageHandler.sendMessage(m);
					}

				}

			} else if (intent.getAction().equals(Contant.ACTION_SMS_SEND)) {
				String type = intent.getStringExtra("send_by");
				Log.d(TAG, CLASS_NAME+"SaleTrackerReceiver() sended by SMS type and return  send by" + type);
				if ("TME".equals(type)) {
					switch (getResultCode()) {
						case Activity.RESULT_OK:
							Log.d(TAG, CLASS_NAME+"SaleTrackerReceiver() SMS is send OK ");
							writeConfig(true);
							//add send content to tme wap address
							mDefaultSendType = Contant.MSG_SEND_BY_NET;
							mDefaultSendTypeTmp = Contant.MSG_SEND_BY_NET;
							mHosturl = mTmeHosturl;

							if(mSwitchSendType)//cancel switch checkbox test , will send by net
							{
								getSharedPreferences(Contant.STSDATA_CONFIG,MODE_PRIVATE).edit()
										.putBoolean(Contant.KEY_SWITCH_SENDTYPE, false).commit();
								mSwitchSendType=false;

								Log.d(TAG, CLASS_NAME+"SaleTrackerReceiver() ***only for test -----open test send type switch  :   "
										+ mDefaultSendType);
							}

                            refreshPanelStatus();
							break;

						default:
							Log.d(TAG, CLASS_NAME+"SaleTrackerReceiver() SMS is send error ResultCode=" + getResultCode());
							break;

					}
				}
			} else if (intent.getAction().equals(Contant.ACTION_SMS_DELIVERED)) {
				Log.d(TAG, CLASS_NAME+"SaleTrackerReceiver() onReceive: ACTION_SMS_DELIVERED"+
						"; ResultCode = "+getResultCode());
			}
		}
	}


	Handler MessageHandler = new Handler() {
		// @Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
				case Contant.ACTION_SEND_BY_SMS:
					Log.d(TAG, CLASS_NAME+"handleMessage()  send type is  by SMS");
					if (isSmsAvailable()) {
						sendContentBySMS();
					}
					break;

				case Contant.ACTION_SEND_BY_NET:
					Log.d(TAG, CLASS_NAME+"handleMessage()  send type is by NET");
					if (isNetworkAvailable()) {
						sendContentByNetwork();
					}
					break;

				case Contant.ACTION_SEND_RST_BY_NET:
					Log.d(TAG, CLASS_NAME+"handleMessage()  ACTION_SEND_RST_BY_NET");
					Bundle data = msg.getData();
					Boolean val = data.getBoolean("value", false);
					Log.d(TAG, CLASS_NAME+"handleMessage()  sended by net and  return  =" + val);
					if (val) {
						mIsSendSuccess = true;
						writeConfig(true);

						//add send content to tme wap address
						writeConfigForTmeWapAddr(true);

						// refresh SaleTrackerActivity panel
                        refreshPanelStatus();

						SaleTrackerService.this.stopSelf();
					}
					break;
			}

			super.handleMessage(msg);
		}
	};

	private void sendContentBySMS() {

		Log.d(TAG, CLASS_NAME+"sendContentBySMS()  start");
		String msg_contents = setSendContent();

		if ("".equals(msg_contents)) {
			Log.e(TAG, CLASS_NAME+"sendContentBySMS()   GET msg_contents  faile");
			return;
		}
		Intent smssend = new Intent(Contant.ACTION_SMS_SEND);
		smssend.putExtra("send_by", "TME");
		PendingIntent sentPending = PendingIntent.getBroadcast(
				SaleTrackerService.this, 0, smssend, 0);
		PendingIntent deliverPending = PendingIntent
				.getBroadcast(SaleTrackerService.this, 0, new Intent(
                        Contant.ACTION_SMS_DELIVERED), 0);

		setDestNum();

		try {
			if (TextUtils.isEmpty(mStrPhoneNo)) {
				throw new IllegalArgumentException("Invalid destinationAddress");
			}

			if (TextUtils.isEmpty(msg_contents)) {
				throw new IllegalArgumentException("Invalid message body");
			}

			SmsManager.getDefault().sendTextMessage(mStrPhoneNo,null,msg_contents,sentPending,
					deliverPending);
		} catch (SecurityException e) {
			Log.d(TAG, CLASS_NAME+" send sms fail");
		}
		Log.d(TAG, CLASS_NAME+"sendContentBySMS()  end");

	}

	private void sendContentByNetwork() {
		String msg_contents = setSendContent();

		if ("".equals(msg_contents)) {
			Log.e(TAG,
					"sendContentByNetwork()  sendContentByNetwork--> send_sms GET msg_contents  faile");
			return;
		}
		try {
			Log.d(TAG,
					"sendContentByNetwork()   mMsgSendNum = "
							+ mMsgSendNum);
			int msgid = mMsgSendNum;
			String encryptContents = RSAHelper.encrypt(publicKey, msg_contents);
			url = mHosturl + "&msgid=" + msgid + "&repinfo=" + encryptContents;
//			Log.e(TAG, CLASS_NAME+"sendContentByNetwork()   encryptContents = " + encryptContents);
//			Log.e(TAG, CLASS_NAME+"sendContentByNetwork()    url = " + url);
		} catch (Exception e) {
			Log.d(TAG, CLASS_NAME+"sendContentByNetwork()  **************** err****************");
			return;
		}
		new Thread(runnable).start();
	}


	private Runnable runnable = new Runnable() {
		@Override
		public void run() {
			Boolean result = false;
			try {
				Log.d(TAG, CLASS_NAME+"run()  Runnable-->start");
				HttpRequester request = new HttpRequester();
				HttpRespons hr = request.sendGet(url);

				if (hr.getContentCollection() != null
						&& hr.getContentCollection().get(0) != null) {
//					Log.d(TAG, CLASS_NAME+"run()   hr.getContentCollection().get(0)"
//							+ hr.getContentCollection().get(0));
					if (hr.getContentCollection().get(0).equals("0")) {
						result = true;
					}
				}
				if (hr.getCode() != 200) {
					Log.d(TAG, CLASS_NAME+"run()   Runnable--->" + "hr.getCode() =" + hr.getCode());
					result = false;
				}

			} catch (Exception e) {
				Log.d(TAG, CLASS_NAME+"run()  Exception" + e.toString());
				result = false;
			} finally {
				Log.d(TAG, CLASS_NAME+"run()   Runnable--->" + "result");
				Message m = new Message();
				Bundle data = new Bundle();
				data.putBoolean("value", result);
				m.setData(data);
				m.what = Contant.ACTION_SEND_RST_BY_NET;
				SaleTrackerService.this.MessageHandler.sendMessage(m);
			}
		}
	};



	private void popNotifyWindow(Context context,int sendType){

		if ((mNotifyFromTestActivity || mIsNeedNoticePop) && (mMsgSendNum == 0)) {
			Log.d(TAG, CLASS_NAME+"popNotifyWindow()  dialog start");
			Intent Dialog = new Intent(context, WIKOSTSScreen.class);
			Dialog.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			context.startActivity(Dialog);
			Log.d(TAG, CLASS_NAME+"popNotifyWindow()   dialog finish");
		}
	}


	private int  getSubID(){

		int sim0_state=-1, sim1_state=-1;
		String sim0_name="", sim1_name="";
		int[] sub0=null;
		int[] sub1=null;
		int sSub = -1;

		Log.d(TAG, CLASS_NAME+"getSubID()  **** start getSubID ");


		sub0 = HideMethod.SubscriptionManager.getSubId(0);
		sub1 = HideMethod.SubscriptionManager.getSubId(1);

		if (sub0 != null) {
			sim0_name = TelephonyManager.getDefault().getNetworkOperatorName(sub0[0]);
			sim0_state = mTm.getSimState(0);
			Log.d(TAG, CLASS_NAME+"getSubID()  **** sub0[0]=" + sub0[0]);

		}
		if (sub1 != null) {
			sim1_name = TelephonyManager.getDefault().getNetworkOperatorName(sub1[0]);
			sim1_state = mTm.getSimState(1);
			Log.d(TAG, CLASS_NAME+"getSubID()  **** sub1[0]=" + sub1[0]);

		}

		Log.d(TAG, CLASS_NAME+"getSubID()   **** start sim0_name=  " + sim0_name + " sim1_name = " + sim1_name);
		Log.d(TAG, CLASS_NAME+"getSubID()  **** start sim0_state=  " + sim0_state + " sim1_state = " + sim1_state);

		if ((TelephonyManager.SIM_STATE_READY == sim0_state)&& (!sim0_name.isEmpty()))
		{
			sSub = sub0[0];
		} else if ((TelephonyManager.SIM_STATE_READY == sim1_state)&& (!sim1_name.isEmpty())) {
			sSub = sub1[0];
		}
		else
		{
			Log.d(TAG, CLASS_NAME+"getSubID()  **** SIM STATE NOT READY  ");
		}

		Log.d(TAG, CLASS_NAME+"getSubID()  ****  sSub= " + sSub);//20150117 modify android 5.0

		return sSub;


	}

	private boolean isSmsAvailable() {
		boolean enable = false;
		int sim_state;
		String sim_name;
		boolean sim_isSmsReady = false;

		sim_name = mTm.getNetworkOperatorName();

		sim_state = mTm.getSimState();
		Log.d(TAG, CLASS_NAME+"isSmsAvailable()   ------> ,  sim_state  " + sim_state
				+ " sim_name =  " + sim_name + "  sim_state  " + sim_state);
		if ((TelephonyManager.SIM_STATE_READY == sim_state)
				&& (!sim_name.isEmpty())) {
			enable = true;
		}

		return enable;
	}

	public boolean isNetworkAvailable() {
		ConnectivityManager connectivity = (ConnectivityManager) mContext
				.getSystemService(Context.CONNECTIVITY_SERVICE);
		if (connectivity == null) {

		} else {
			NetworkInfo[] info = connectivity.getAllNetworkInfo();
			if (info != null) {
				for (int i = 0; i < info.length; i++) {
					if (info[i].getState() == NetworkInfo.State.CONNECTED) {
						Log.d(TAG, CLASS_NAME+"isNetworkAvailable()   return true");
						return true;
					}
				}
			}
		}
		Log.d(TAG, CLASS_NAME+"isNetworkAvailable() return false");
		return false;
	}

	public String getIMEI() {
		String imei = mTm.getDeviceId(0,mContext);
		Log.d(TAG, CLASS_NAME+"getIMEI()   imei=" + imei);

		if (imei == null || imei.isEmpty()) {
			return new String(Contant.NULL_IMEI);
		}
		return imei;
	}


	private void setDestNum() {

		String sim_network, sim_operator;
		String PLMNTest1 = new String("46000");// China Mobile
		String PLMNTest2 = new String("46002");// China Mobile
		String PLMNTest3 = new String("46007");// China Mobile
		String PLMNTest4 = new String("46001");// China Unicom

		if(DEFAULT_VALUE.equals(mStrPhoneNo))
		{
			mStrPhoneNo = new String("09064991311");
		}

		sim_network = mTm.getNetworkOperatorName();
		sim_operator = mTm.getSimOperator();

		Log.d(TAG, CLASS_NAME+"setDestNum()  sim_network: " + sim_network + ";sim_operator :"
				+ sim_operator);

		if (sim_operator.equals(PLMNTest1)
				|| sim_operator.equals(PLMNTest2)
				|| sim_operator.equals(PLMNTest3)) {
			mStrPhoneNo = NUM_SMS;
		} else if (sim_operator.equals(PLMNTest4)) {
			mStrPhoneNo = NUM_SMS;
		}

		Log.d(TAG, CLASS_NAME+"setDestNum()   sim_network= " + sim_network
				+ " mStrPhoneNo=" + mStrPhoneNo);
	}

	public String setSendContent() {
		StringBuffer smsContent = new StringBuffer();

		String software_version;
		String phone_type;
		String msg_contents;

		mStrIMEI = getIMEI();
		if ((Contant.NULL_IMEI).compareTo(mStrIMEI) == 0) {
			Log.d(TAG,
					"init()    ********error********getIMEI() = null ***********error******");
			return "";
		}
		// tishi
		StringBuffer REG = new StringBuffer("TN:IMEI1,");
		REG.append(mStrIMEI);

		// Client No
		StringBuffer SAP_NO = new StringBuffer(",");
		SAP_NO.append(mClientNo);

		// Client product model
		StringBuffer PRODUCT_NO = new StringBuffer(",");
		if(DEFAULT_VALUE.equals(mStrModel) || "".equals(mStrModel))
		{
			PRODUCT_NO.append(Build.MODEL);
		}
		else
		{
			PRODUCT_NO.append(mStrModel);
		}

		// Cell id
		StringBuffer CELL_ID = new StringBuffer(",CID:");
		// GsmCellLocation loc =
		// (GsmCellLocation)tm.getCellLocationGemini(simId); //20150117 modify
		// android 5.0
		GsmCellLocation loc = (GsmCellLocation) mTm.getCellLocation();
		Log.d(TAG, CLASS_NAME+"setSendContent()  loc= " + loc);

		int cellId = 0;
		if (loc != null) {
			cellId = loc.getCid();
		}
		if (cellId == -1) {
			cellId = 0;
		}
		CELL_ID.append(Integer.toHexString(cellId).toUpperCase());

		// add sn no 20150703
		StringBuffer SN_NO = new StringBuffer(",");
		SN_NO.append(Build.SERIAL);

		// Soft version
		StringBuffer SOFTWARE_NO = new StringBuffer(",");
		// SOFTWARE_NO.append(Build.DISPLAY);
		String customVersion = SystemProperties.get("ro.custom.build.version");

		SOFTWARE_NO.append(customVersion);

		smsContent.append(REG).append(SAP_NO).append(PRODUCT_NO)
				.append(CELL_ID).append(SOFTWARE_NO).append(SN_NO);
		Log.d(TAG, CLASS_NAME+"setSendContent() SendString=" + smsContent.toString());

		return smsContent.toString();

	}

	/********************************** read config from xml end ************************************/

	private void pickCountryConfigs(){
		Log.d(TAG, CLASS_NAME+"pickCountryConfigs: ");

		SaleTrackerUti.readSendParamFromXml();

		String projectName = SystemProperties.get("ro.project", "trunk");
		SaleTrackerConfigs config = SaleTrackerUti.map.get(projectName);
		if(config != null){
			mClientNo = config._client_no;
			mDefaultSendType = Integer.parseInt(config._send_type);
			mIsNeedNoticePop = Boolean.parseBoolean(config._notice);
			mHosturl = config._host_url;
			mStartTimeFromXML = Integer.parseInt(config._start_time);
			mSpaceTimeFromXML = Integer.parseInt(config._space_time);
			mStrCountry = config._country_type;
			mStrModel = config._model_type;
			mDefaultSendTypeTmp = mDefaultSendType;

			Log.w(TAG, CLASS_NAME+" pickCountryConfigs: "
					+ "\n   mClientNo =" + mClientNo
					+ "\n   mDefaultSendType =" + mDefaultSendType
					+ "\n   mIsNeedNoticePop =" + mIsNeedNoticePop
					+ "\n   mHosturl ="	+ mHosturl
					+ "\n   mStartTimeFromXML ="+mStartTimeFromXML
					+ "\n   mSpaceTimeFromXML =" +mSpaceTimeFromXML
					+ "\n   mStrPhoneNo =" + mStrPhoneNo
					+ "\n   mStrModel =" + mStrModel
					+ "\n   mStrCountry =" +mStrCountry );
		}else{
			Log.d(TAG,CLASS_NAME+" pickCountryConfigs: config doesn't exist");
		}

	}
	/**********************************read config from xml  end************************************/

	/**********************************data fun begin************************************/
	private Boolean initConfig()
	{
		int data = 0;
		int temp_data = 0;
		if(Contant.STS_SP == STS_CONFIG_TYPE)
		{
			data = mStciSP.read_secro();
		}

		temp_data = data >> 8;
		mMsgSendNum = (short) (temp_data & 0x0000ffff);
		return ((data & 0x000000ff) == 0x01) ? true : false;
	}

	private int readConfigSms() {
		int data = 0;
		int temp_data = 0;
		if(Contant.STS_SP == STS_CONFIG_TYPE)
		{
			data = mStciSP.read_secro();
		}
		temp_data = data >> 24;
		temp_data = temp_data & 0x000000ff;
		Log.d(TAG, CLASS_NAME+"readConfigSms: data = "+temp_data);
		return temp_data;
	}

	private Boolean writeConfigSms(short sms) {
		boolean ret;
		int data = 0;
		int temp_data = 0;
		if(Contant.STS_SP == STS_CONFIG_TYPE){
			data = mStciSP.read_secro();
			data = data & 0x00ffffff;
			temp_data = sms;
			temp_data = temp_data << 24;
			data = data | temp_data;
			ret = mStciSP.write_secro(data);
		}

		return ret;
	}

	private int readConfigDay() {
		int data = 0;
		int temp_data = 0;
		if(Contant.STS_SP == STS_CONFIG_TYPE)
		{
			data = mStciSP.read_secro();
		}
		temp_data = data >> 8;
		temp_data = temp_data & 0x0000ffff;
		return temp_data;
	}

	private Boolean writeConfigDay(short day) {
		boolean ret;
		int data = 0;
		int temp_data = 0;
		if(Contant.STS_SP == STS_CONFIG_TYPE)  {
			data = mStciSP.read_secro();
			data = data & 0xff0000ff;
			temp_data = day;
			temp_data = temp_data << 8;
			data = data | temp_data;
			ret = mStciSP.write_secro(data);
		}

		return ret;
	}

	private Boolean readConfig() {
		int data;
		int temp_data;
		if(Contant.STS_SP == STS_CONFIG_TYPE)
		{
			data = mStciSP.read_secro();
		}
		temp_data = data & 0x000000ff;
		return temp_data == 0x01 ? true : false;
	}

	private Boolean writeConfig(Boolean flag) {
		boolean ret;
		int data = 0;
		int temp_data = 0;
		if(Contant.STS_SP== STS_CONFIG_TYPE){
			temp_data = mStciSP.read_secro();
			temp_data = temp_data & 0xffffff00;
			if (flag) {
				data = 0x01 | temp_data;
			} else {
				data = 0x00 | temp_data;
			}
			ret = mStciSP.write_secro(data);
		}
		return ret;
	}
	/**********************************data fun end************************************/



	/***********add send content to tme wap address **********/
	public Boolean isTmeWapAddr()
	{
		boolean ret = false;
		if(mTmeHosturl.equals(mHosturl))
		{
			ret = true;
		}

		Log.w(TAG, CLASS_NAME+" isTmeWapAddr()    get value:  ret =" + ret);
		return ret;
	}
	public Boolean isSendedToTmeWapAddr()
	{
		boolean ret = false;

		ret = mStciSP.isSendedToTmeWapAddr();
		Log.w(TAG, CLASS_NAME+" isSendedToTmeWapAddr()    get value:  ret =" + ret);

		return ret;
	}
	public Boolean writeConfigForTmeWapAddr(boolean flag) {

		Log.w(TAG, CLASS_NAME+" writeConfigForTmeWapAddr()    set value : flag =" +flag );
		return  mStciSP.writeConfigForTmeWapAddr(flag);
	}

	public void refreshPanelStatus(){
		Log.d(TAG, CLASS_NAME+"refreshPanelStatus: ");
		Intent intent = new Intent(Contant.ACTION_REFRESH_PANEL);
		mContext.sendBroadcast(intent);
	}
/***********add send content to tme wap address **********/
}
