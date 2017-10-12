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
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.telephony.SmsManager;
import android.telephony.SubscriptionInfo;
import android.telephony.TelephonyManager;
import android.telephony.gsm.GsmCellLocation;
import android.text.TextUtils;
import android.util.Log;

import com.wrapper.stk.HideMethod.SubscriptionManager;

import java.util.List;
import java.util.Map;

//import com.wrapper.stk.HideMethod;
//import com.wrapper.stk.HideMethod.TelephonyManager;

public class SaleTrackerService extends Service {
	private static final String TAG = "SaleTracker";
	private static final String CLASS_NAME = "SaleTrackerService---->";

    private static final String DEFAULT_VALUE = "defaultSet";
	private static final String publicKey = "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQCY4gRmZHQimOWRr99Yi64jGDGMJSa7Awx05J9gpJuQz9tZPrP6QCWFJNpBxBxS_UMg-36FjFl_l8qLBWl-q7pVlyc4qdxq4HGQKJfdBm8aOFQ3Ekaylm1p2s5YKxvYTHDydKG72EXDdvbea8ZvXA1rKP-MpOWKA7XmkLpChQqrsQIDAQAB";
	private static String mHosturl = "http://eservice.tinno.com/eservice/stsReport?reptype=report";
    private static String mTmeHosturl = "http://eservice.tinno.com/eservice/stsReport?reptype=report";
    private static String mClientNo = "0000001000";
    private static String NUM_SMS = "18565857256";	//  15920026432; 18565856119

	private static Context mContext;
	private String url;

	public static final int STS_CONFIG_TYPE = Contant.STS_SP;
	private static int mStartTimeFromXML = Contant.START_TIME;
	private static int mSpaceTimeFromXML = Contant.SPACE_TIME;

    private static boolean mSwitchSendType = false;
    private static boolean mNotifyFromTestActivity = false; // this param get value
    private static boolean mIsSendSuccess = false;
    private static boolean mIsNeedNoticePop = false;
    private static boolean airplaneModeOn = false;

    private static int mMsgSendNum = 0;
	public  static int mDefaultSendType = Contant.MSG_SEND_BY_NET;
    private static int mDefaultSendTypeTmp  = Contant.MSG_SEND_BY_NET;
	public int mStartTime = Contant.START_TIME;
	public int mSpaceTime = Contant.SPACE_TIME;
	public  String mStrIMEI = Contant.NULL_IMEI;

	private String mStrPhoneNo = DEFAULT_VALUE;
	private String mStrCountry = DEFAULT_VALUE;
	private String mStrModel = DEFAULT_VALUE;

	private static SaleTrackerConfigSP mStciSP = new SaleTrackerConfigSP();
	private final BroadcastReceiver mSaleTrackerReceiver = new SaleTrackerReceiver();
	private final BroadcastReceiver mStsAirplanReceiver = new StsAirplanReceiver();
	private final BroadcastReceiver mNetConnectReceiver = new StsNetConnectReceiver();

	private static TelephonyManager mTm;

	private static final String CONFIG_CLIENT_NO = "client_no";
	private static final String CONFIG_SEND_TYPE = "send_type";
	private static final String CONFIG_NOTICE = "notice";
	private static final String CONFIG_HOST_URL = "host_url";
	private static final String CONFIG_START_TIME = "start_time";
	private static final String CONFIG_SPACE_TIME = "space_time";
	@Override
	public void onCreate() {
		super.onCreate();
		init();
	}

	private void init() {
		Log.d(TAG, CLASS_NAME+"init() start");
		mContext = getApplicationContext();
		mStciSP.init(mContext);
		mMsgSendNum = mStciSP.readSendedNumber();
		try {
			mTm = (TelephonyManager)mContext.getSystemService(Context.TELEPHONY_SERVICE);
		} catch (Exception e) {
			Log.d(TAG, CLASS_NAME + "init() ********error******** TelephonyManager.getDefault() = null ********error********");
			e.printStackTrace();
		}

		pickCountryConfigs();

		registerReceiver(mStsAirplanReceiver, new IntentFilter(Intent.ACTION_AIRPLANE_MODE_CHANGED));
		if(!airplaneModeOn)
		{
			Log.d(TAG, CLASS_NAME + "init()   registerReceiver mSaleTrackerReceiver");
			registerReceiver(mSaleTrackerReceiver, new IntentFilter(Contant.STS_REFRESH));
			registerReceiver(mSaleTrackerReceiver, new IntentFilter(Contant.ACTION_SMS_SEND));
			registerReceiver(mSaleTrackerReceiver, new IntentFilter(Contant.ACTION_SMS_DELIVERED));
			registerReceiver(mNetConnectReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
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
			Log.d(TAG, CLASS_NAME + "onStart()  ******************* intent = null*********************");
			super.onStart(intent, startId);
			return;
		}

		String type = intent.getStringExtra(Contant.SEND_TO);
		Log.d(TAG, CLASS_NAME + "onStart() this content sendto = " + type);
		if (Contant.SEND_TO_TME.equals(type)) {
			mDefaultSendType = Contant.MSG_SEND_BY_NET;
			mDefaultSendTypeTmp = Contant.MSG_SEND_BY_NET;
			mHosturl = mTmeHosturl;
		}

		super.onStart(intent, startId);
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		Log.e(TAG, CLASS_NAME + "onDestroy() unregisterReceiver");

		airplaneModeOn = false;
        try {
            unregisterReceiver(mSaleTrackerReceiver);
			unregisterReceiver(mStsAirplanReceiver);
			unregisterReceiver(mNetConnectReceiver);
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

	public static void resetMsgSendNum(){
		mStciSP.writeSendedNumber(0);
	}

	private class StsAirplanReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
			Log.d(TAG, CLASS_NAME + "StsAirplanReceiver()  onReceive start");
			if (intent.getAction().equals(Intent.ACTION_AIRPLANE_MODE_CHANGED)) {
				boolean airplaneMode = intent.getBooleanExtra("state", false);
				if (airplaneMode) {
					Log.d(TAG, CLASS_NAME + "StsAirplanReceiver()  : ACTION_AIRPLANE_MODE_CHANGED in airplane mSaleTrackerReceiver = "
							+ mSaleTrackerReceiver);
					// guchunhua,DATE20150720,modify for FADALFRA-75,START
					try {
						SaleTrackerService.this
								.unregisterReceiver(mSaleTrackerReceiver);
						SaleTrackerService.this
								.unregisterReceiver(mNetConnectReceiver);
					} catch (IllegalArgumentException e) {
						android.util.Log.e(TAG, CLASS_NAME+"StsAirplanReceiver()   registerReceiverSafe(), FAIL!");
					}
					// guchunhua,DATE20150720,modify for FADALFRA-75,END
				} else {
					Log.d(TAG,CLASS_NAME +
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
					registerReceiver(mNetConnectReceiver, new IntentFilter(
							ConnectivityManager.CONNECTIVITY_ACTION));
				}
			}
		}
	}


	private class SaleTrackerReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
			Log.d(TAG, CLASS_NAME + "SaleTrackerReceiver() onReceive start: action = " + intent.getAction()
					+ "; elapsed time = " + SystemClock.elapsedRealtime()/1000);

			if (intent.getAction().equals(Contant.STS_REFRESH)) {
				if (mIsSendSuccess || (mMsgSendNum > Contant.MAX_SEND_CONUT_BY_NET)) {
					Log.d(TAG, CLASS_NAME + "SaleTrackerReceiver()  The message is send success or the maximum sended number, stop SaleTrackerService");
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
					mDefaultSendType = pre.getInt(Contant.KEY_SELECT_SEND_TYPE, Contant.ACTION_SEND_BY_NET);
					Log.d(TAG, CLASS_NAME + "SaleTrackerReceiver() only for test ----- send type switch : "
							+ mDefaultSendType);
				}

				int MsgSendMode = -1;
				Log.d(TAG, CLASS_NAME + "SaleTrackerReceiver() mDefaultSendType= " + mDefaultSendType);

				switch (mDefaultSendType) {
					case Contant.MSG_SEND_BY_SMS:
						Log.d(TAG, CLASS_NAME + "SaleTrackerReceiver() send type by SMS  mMsgSendNum = "
								+ mMsgSendNum);
						if ((mMsgSendNum % 24) == 0) {
							MsgSendMode = Contant.ACTION_SEND_BY_SMS;
						}
						break;

					case Contant.MSG_SEND_BY_NET:
						Log.d(TAG, CLASS_NAME + "SaleTrackerReceiver() send type by NET  mMsgSendNum = "
								+ mMsgSendNum);
						MsgSendMode = Contant.ACTION_SEND_BY_NET;
						mIsSendOnNetConnected = true;
						break;

					case Contant.MSG_SEND_BY_NET_AND_SMS:
						if ((mMsgSendNum > 0) && ((mMsgSendNum % 24) == 0)) {
							Log.d(TAG,CLASS_NAME +
									"SaleTrackerReceiver() send type by NET_AND_SMS--sms  mMsgSendNum = "
											+ mMsgSendNum);
							MsgSendMode = Contant.ACTION_SEND_BY_SMS;
						} else {
							Log.d(TAG,CLASS_NAME +
									"SaleTrackerReceiver() MSG_SEND_BY_NET_AND_SMS--net  mMsgSendNum = "
											+ mMsgSendNum);
							MsgSendMode = Contant.ACTION_SEND_BY_NET;
							mIsSendOnNetConnected = true;
						}
						break;

				}

				popNotifyWindow(context);

				mStciSP.writeSendedNumber(++mMsgSendNum);

				Log.d(TAG, CLASS_NAME + "SaleTrackerReceiver() MsgSendMode = " + MsgSendMode);
				if (MsgSendMode != -1) {
					if (MessageHandler.hasMessages(Contant.ACTION_SEND_BY_NET)) {
						MessageHandler.removeMessages(Contant.ACTION_SEND_BY_NET);
					}
					MessageHandler.obtainMessage(MsgSendMode).sendToTarget();
				}
			} else if (intent.getAction().equals(Contant.ACTION_SMS_SEND)) {
				String type = intent.getStringExtra("send_by");
				Log.d(TAG, CLASS_NAME + "SaleTrackerReceiver() sended by SMS type and return  send by" + type);
				if ("TME".equals(type)) {
					switch (getResultCode()) {
						case Activity.RESULT_OK:
							Log.d(TAG, CLASS_NAME + "SaleTrackerReceiver() SMS is send OK ");
							//add send content to tme wap address
							mDefaultSendType = Contant.MSG_SEND_BY_NET;
							mDefaultSendTypeTmp = Contant.MSG_SEND_BY_NET;
							mHosturl = mTmeHosturl;

							if(mSwitchSendType)//cancel switch checkbox test , will send by net
							{
								getSharedPreferences(Contant.STSDATA_CONFIG,MODE_PRIVATE).edit()
										.putBoolean(Contant.KEY_SWITCH_SENDTYPE, false).commit();
								mSwitchSendType=false;

								Log.d(TAG, CLASS_NAME + "SaleTrackerReceiver() only for test -----open test send type switch  :   "
										+ mDefaultSendType);
							}
							mStciSP.writeSendedResult(true);
                            refreshPanelStatus();
							break;

						default:
							Log.d(TAG, CLASS_NAME + "SaleTrackerReceiver() SMS is send error ResultCode=" + getResultCode());
							break;
					}
				}
			} else if (intent.getAction().equals(Contant.ACTION_SMS_DELIVERED)) {
				Log.d(TAG, CLASS_NAME + "SaleTrackerReceiver() onReceive: ACTION_SMS_DELIVERED" +
						"; ResultCode = " + getResultCode());
			}
		}
	}


	private Handler MessageHandler = new Handler() {
		// @Override
		public void handleMessage(Message msg) {
			super.handleMessage(msg);
			switch (msg.what) {
				case Contant.ACTION_SEND_BY_SMS:
					Log.d(TAG, CLASS_NAME+"handleMessage() send type is  by SMS");
					if (isSmsAvailable()) {
						sendContentBySMS();
					}
					break;

				case Contant.ACTION_SEND_BY_NET:
					Log.d(TAG, CLASS_NAME+"handleMessage() send type is by NET");
					if (isNetworkAvailable()) {
						sendContentByNetwork();
					}
					break;

				case Contant.ACTION_SEND_RST_BY_NET:
					Boolean val = (Boolean) msg.obj;
					Log.d(TAG, CLASS_NAME + "handleMessage() sended by net and  return  =" + val);
					if (val) {
						mIsSendSuccess = true;
						mIsSendOnNetConnected = false;
						mStciSP.writeSendedResult(val);

						//add send content to tme wap address
						mStciSP.writeConfigForTmeWapAddr(true);

						// refresh SaleTrackerActivity panel
                        refreshPanelStatus();

						SaleTrackerService.this.stopSelf();
					}
					break;
			}
		}
	};

	private void sendContentBySMS() {
		Log.d(TAG, CLASS_NAME + "sendContentBySMS()  start");
		String msg_contents = setSendContent();

		if ("".equals(msg_contents)) {
			Log.e(TAG, CLASS_NAME + "sendContentBySMS()   GET msg_contents  faile");
			return;
		}
		Intent smsSend = new Intent(Contant.ACTION_SMS_SEND);
		smsSend.putExtra("send_by", "TME");
		PendingIntent sentPending = PendingIntent.getBroadcast(
				SaleTrackerService.this, 0, smsSend, 0);
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

			// weijie.wang created.  8/3/16 Add for STSPTHA-34 start
			final int defaultSubId = SmsManager.getDefault().getSubscriptionId();
			Log.d(TAG, CLASS_NAME + "sendContentBySMS() defaultSubId = " + defaultSubId);
			final Context context = getApplicationContext();
			final SubscriptionManager subscriptionManager = SubscriptionManager.getDefault();
			if (defaultSubId < 0) {
				List<SubscriptionInfo> subInfoList = subscriptionManager.getActiveSubscriptionInfoList(context);
				Log.d(TAG, "sendContentBySMS(): subInfoList = " + subInfoList);
				if (subInfoList != null && subInfoList.size() >= 1) {
					for (SubscriptionInfo subInfo : subInfoList) {
						Log.d(TAG, "sendContentBySMS(): subInfo = " + subInfo);
						int subId = subInfo.getSubscriptionId();
						if (subscriptionManager.isActiveSubId(subId, context)) {
							subscriptionManager.setDefaultSmsSubId(subId, context);
							break;
						}
					}
				}
			}
			// weijie.wang created.  8/3/16 Add for STSPTHA-34 end

			SmsManager.getDefault().sendTextMessage(mStrPhoneNo, null, msg_contents, sentPending,
					deliverPending);

			// weijie.wang created.  8/3/16 Add for STSPTHA-34 INLINE
			if (defaultSubId < 0) {
				subscriptionManager.setDefaultSmsSubId(defaultSubId, context);
			}
		} catch (SecurityException e) {
			Log.d(TAG, CLASS_NAME + " send sms fail");
		}
		Log.d(TAG, CLASS_NAME + "sendContentBySMS()  end");

	}

	private void sendContentByNetwork() {
		String msg_contents = setSendContent();

		if ("".equals(msg_contents)) {

			Log.e(TAG, CLASS_NAME +
					"sendContentByNetwork()  sendContentByNetwork--> send_sms GET msg_contents  faile");
			return;
		}
		try {
			int msgid = mMsgSendNum;
			String encryptContents = RSAHelper.encrypt(publicKey, msg_contents);
			url = mHosturl + "&msgid=" + msgid + "&repinfo=" + encryptContents;
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
				Log.d(TAG, CLASS_NAME + "run()  Runnable-->start");
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
					Log.d(TAG, CLASS_NAME + "run()   Runnable--->" + "hr.getCode() =" + hr.getCode());
					result = false;
				}

			} catch (Exception e) {
				Log.d(TAG, CLASS_NAME + "run()  Exception" + e.toString());
				result = false;
			} finally {
				Log.d(TAG, CLASS_NAME + "run()   Runnable--->" + "result");
				MessageHandler.obtainMessage(Contant.ACTION_SEND_RST_BY_NET, result).sendToTarget();
			}
		}
	};



	private void popNotifyWindow(Context context){

		if ((mNotifyFromTestActivity || mIsNeedNoticePop) && (mMsgSendNum == 0)) {
			Log.d(TAG, CLASS_NAME + "popNotifyWindow()  dialog start");
			Intent Dialog = new Intent(context, WIKOSTSScreen.class);
			Dialog.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			context.startActivity(Dialog);
			Log.d(TAG, CLASS_NAME + "popNotifyWindow()  dialog finish");
		}
	}

	private boolean isSmsAvailable() {
		int sim_state;
		String sim_name;

//		sim_name = getNetworkOperatorName();

		sim_state = mTm.getSimState();
		Log.d(TAG, CLASS_NAME+"isSmsAvailable(): sim_state  " + sim_state);
		return (TelephonyManager.SIM_STATE_READY == sim_state) ? true : false;
	}

	public String getNetworkOperatorName(){
		TelephonyManager tm =
				(TelephonyManager)mContext.getSystemService(Context.TELEPHONY_SERVICE);
		return(tm.getNetworkOperatorName());
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

	public static String getIMEI() {
		String imei = mTm.getDeviceId(0);
		Log.d(TAG, CLASS_NAME+"getIMEI()   imei = " + imei);

		if (imei == null || imei.isEmpty()) {
			return new String(Contant.NULL_IMEI);
		}
		return imei;
	}

	/**
	 * @param
	 * @return
	 * getIMEI for PK
	 */
	public static String getIMEIPK() {
		String imei1 = mTm.getDeviceId(0);
		String imei2 = mTm.getDeviceId(1);
		if (imei1 == null || imei1.isEmpty()) {
			imei1 = Contant.NULL_IMEI;
		}
		if (imei2 == null || imei2.isEmpty()) {
			imei2 = Contant.NULL_IMEI;
		}
		Log.d(TAG, CLASS_NAME+"getIMEI()   imei1 = " + imei1
			+"; imei2 = "+imei2);
		String imeiDisplay = imei1 + " " + imei2;
		return imeiDisplay;
	}


	private void setDestNum() {

		/*String sim_network, sim_operator;
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

		Log.d(TAG, CLASS_NAME + "setDestNum()  sim_network: " + sim_network + ";sim_operator :"
				+ sim_operator);*/

		/*if (sim_operator.equals(PLMNTest1)
				|| sim_operator.equals(PLMNTest2)
				|| sim_operator.equals(PLMNTest3)) {
			mStrPhoneNo = NUM_SMS;
		} else if (sim_operator.equals(PLMNTest4)) {
			mStrPhoneNo = NUM_SMS;
		}*/

		// weijie created. 17-3-13. Add for QMobile
		/*if (SaleTrackerUti.isQMobile()) {
			mStrPhoneNo = mContext.getSharedPreferences(Contant.STSDATA_CONFIG, MODE_PRIVATE)
					.getString(Contant.KEY_SERVER_NUMBER, Contant.SERVER_NUMBER);

		} else {
			mStrPhoneNo = NUM_SMS;
		}*/
		if ("WALTON".equalsIgnoreCase(Build.BRAND)) {
			mStrPhoneNo = "8801755610362";
		} else {
			mStrPhoneNo = NUM_SMS;
		}
		Log.d(TAG, CLASS_NAME + "setDestNum() =" + mStrPhoneNo);
	}

	public String setSendContent() {
		StringBuffer smsContent = new StringBuffer();

		mStrIMEI = getIMEI();
		if (mStrIMEI.equals(Contant.NULL_IMEI)) {
			Log.d(TAG, CLASS_NAME +
					"init()    ********error********getIMEI() = null ***********error******");
			return "";
		}
		// tishi
		String REG = mStrIMEI;

		// Client No
		String SAP_NO = mClientNo;

		// Client product model
		StringBuffer PRODUCT_NO = new StringBuffer();
		if(DEFAULT_VALUE.equals(mStrModel) || "".equals(mStrModel))
		{
			if (SaleTrackerUti.isQMobile()) {
				String model = SystemProperties.get("ro.product.model.pk", Build.MODEL);
				if (model.startsWith("QMobile ")) {
					String QMobile = "QMobile ";
					PRODUCT_NO.append(model.substring(QMobile.length()));
				} else {
					PRODUCT_NO.append(model);
				}
			} else {
				PRODUCT_NO.append(Build.MODEL);
			}
			// weijie created. 17-3-23. Modify for QMobile. delete QMobile prefix. end
		}
		else
		{
			PRODUCT_NO.append(mStrModel);
		}

		/*// Cell id
		StringBuffer CELL_ID = new StringBuffer(",CID:");
		GsmCellLocation loc = (GsmCellLocation) mTm.getCellLocation();
		Log.d(TAG, CLASS_NAME+"setSendContent()  loc= " + loc);

		int cellId = 0;
		if (loc != null) {
			cellId = loc.getCid();
		}
		if (cellId == -1) {
			cellId = 0;
		}
		CELL_ID.append(Integer.toHexString(cellId).toUpperCase());*/

		// add sn no 20150703
		String SN_NO = Build.SERIAL;

		// Soft version
		StringBuffer SOFTWARE_NO = new StringBuffer();
		String customVersion = SystemProperties.get("ro.custom.build.version");
		String strCountryName = SystemProperties.get("ro.project", "trunk");
		if (strCountryName.startsWith("wik_")) {
			customVersion = SystemProperties.get("ro.internal.build.version");
		}
		SOFTWARE_NO.append(customVersion);

		if ("WALTON".equalsIgnoreCase(Build.BRAND)) {
			smsContent.append("WALTON ").append(mStrIMEI).append(" Primo_").append(Build.DEVICE);
		} else {
			smsContent.append("TN:IMEI1,"+mStrIMEI).append(","+SAP_NO).append(","+PRODUCT_NO)
					.append(","+SOFTWARE_NO).append(","+SN_NO);
		}
		Log.d(TAG, CLASS_NAME+"setSendContent() SendString=" + smsContent.toString());

		return smsContent.toString();

	}

	/********************************** read config from xml end ************************************/

	private void pickCountryConfigs(){
		Log.d(TAG, CLASS_NAME + "pickCountryConfigs: ");

		String projectName = SystemProperties.get("ro.project", "trunk");
		Map<String, String> configMap = SaleTrackerUti.readSendParamFromXml(mContext);
		if(configMap != null){
			mClientNo = configMap.get(CONFIG_CLIENT_NO);
			mDefaultSendType = Integer.parseInt(configMap.get(CONFIG_SEND_TYPE));
			mIsNeedNoticePop = Boolean.parseBoolean(configMap.get(CONFIG_NOTICE));
			mHosturl = configMap.get(CONFIG_HOST_URL);
			mStartTimeFromXML = Integer.parseInt(configMap.get(CONFIG_START_TIME));
			mSpaceTimeFromXML = Integer.parseInt(configMap.get(CONFIG_SPACE_TIME));
			mDefaultSendTypeTmp = mDefaultSendType;

			Log.d(TAG, CLASS_NAME + " pickCountryConfigs: projectName = " + projectName
					+ "\n   mClientNo =" + mClientNo
					+ "\n   mDefaultSendType =" + mDefaultSendType
					+ "\n   mIsNeedNoticePop =" + mIsNeedNoticePop
					+ "\n   mHosturl =" + mHosturl
					+ "\n   mStartTimeFromXML =" + mStartTimeFromXML
					+ "\n   mSpaceTimeFromXML =" + mSpaceTimeFromXML
					+ "\n   mStrPhoneNo =" + mStrPhoneNo);
		}else{
			Log.d(TAG,CLASS_NAME+" pickCountryConfigs: config doesn't exist");
		}

	}
	/**********************************read config from xml  end************************************/

	public void refreshPanelStatus(){
		Log.d(TAG, CLASS_NAME + "refreshPanelStatus: ");
		Intent intent = new Intent(Contant.ACTION_REFRESH_PANEL);
		mContext.sendBroadcast(intent);
	}

	private static boolean mIsSendOnNetConnected = false;
	private class StsNetConnectReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
			Log.d(TAG, CLASS_NAME + " StsNetConnectReceiver() onReceive: start  intent=" + intent.getAction());

			if (intent.getAction().equals(ConnectivityManager.CONNECTIVITY_ACTION)) {
				boolean bMobile = checkNetworkConnection(context);//// TODO: 17-3-13
				if (mIsSendOnNetConnected && bMobile && !mIsSendSuccess) {
					Log.d(TAG, CLASS_NAME + " StsNetConnectReceiver()  net was connected and start to send ");
					if (MessageHandler.hasMessages(Contant.ACTION_SEND_BY_NET)) {
						MessageHandler.removeMessages(Contant.ACTION_SEND_BY_NET);
					}
					MessageHandler.obtainMessage(Contant.ACTION_SEND_BY_NET).sendToTarget();
				} else {
					Log.d(TAG, CLASS_NAME + " StsNetConnectReceiver()  net was connected");
				}
			}
		}

	}

	public static boolean checkNetworkConnection(Context context){
		final ConnectivityManager connMgr = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
		final android.net.NetworkInfo wifi =connMgr.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
		final android.net.NetworkInfo mobile =connMgr.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);

		Log.d(TAG, CLASS_NAME + " checkNetworkConnection():  wifi.getState()=" +wifi.getState()
				+ " wifi.getTypeName="+ wifi.getTypeName()
				+ " mobile.getState()="+ mobile.getState()
				+ " mobile.getTypeName="+ mobile.getTypeName());

		// 只考虑连接数据网络的情况
		if (mobile.getState() == NetworkInfo.State.CONNECTED
				|| wifi.getState() == NetworkInfo.State.CONNECTED) {  //getState()方法是查询是否连接了数据网络
			return true;
		}
		return false;
	}
}
