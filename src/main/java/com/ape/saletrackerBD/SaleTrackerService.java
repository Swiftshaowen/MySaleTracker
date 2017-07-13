package com.ape.saletrackerBD;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.provider.Settings;
import android.telephony.CellLocation;
import android.telephony.SmsManager;
import android.telephony.SubscriptionInfo;
import android.telephony.TelephonyManager;
import android.telephony.gsm.GsmCellLocation;
import android.text.TextUtils;
import android.util.Log;

import com.wrapper.stk.HideMethod.SubscriptionManager;
//import com.wrapper.stk.HideMethod.Settings;

import java.util.ArrayList;
import java.util.List;

import static android.R.attr.publicKey;


public class SaleTrackerService extends Service {
	private static final String TAG = "SaleTrackerBD";
	private static final String CLASS_NAME = "SaleTrackerService---->";

	private static Context mContext;

    private static String mClientNo = Contant.CLIENT_NO;
	private static String mStrPhoneNo;

    private static boolean mIsSendSuccess = false;
    private static boolean airplaneModeOn = false;

    private static int mMsgSendNum = 0;
	public  static int mDefaultSendType = Contant.MSG_SEND_BY_SMS;
	public int mStartTime = Contant.START_TIME;
	public int mSpaceTime = Contant.SPACE_TIME;
	public  String mStrIMEI = Contant.NULL_IMEI;

	private static SaleTrackerConfigSP mStciSP;
	private final BroadcastReceiver mSaleTrackerReceiver = new SaleTrackerReceiver();
	private final BroadcastReceiver mStsAirplanReceiver = new StsAirplanReceiver();

	private static TelephonyManager mTm;
	private String url;
	private static final String publicKey = "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQCY4gRmZHQimOWRr99Yi64jGDGMJSa7Awx05J9gpJuQz9tZPrP6QCWFJNpBxBxS_UMg-36FjFl_l8qLBWl-q7pVlyc4qdxq4HGQKJfdBm8aOFQ3Ekaylm1p2s5YKxvYTHDydKG72EXDdvbea8ZvXA1rKP-MpOWKA7XmkLpChQqrsQIDAQAB";
	private static String mHosturl = "http://eservice.tinno.com/eservice/stsReport?reptype=report";

	@Override
	public void onCreate() {
		super.onCreate();
		init();
	}

	private void init() {
		Log.d(TAG, CLASS_NAME+"init() start");
		mContext = getApplicationContext();
		mStciSP = SaleTrackerConfigSP.init(mContext);

		mMsgSendNum = mStciSP.readSendedNumber();
		try {
			mTm = (TelephonyManager)mContext.getSystemService(Context.TELEPHONY_SERVICE);
		} catch (Exception e) {
			Log.d(TAG, CLASS_NAME + "init() ********error******** TelephonyManager.getDefault() = null ********error********");
			e.printStackTrace();
		}


		registerReceiver(mStsAirplanReceiver, new IntentFilter(Intent.ACTION_AIRPLANE_MODE_CHANGED));
		if(!airplaneModeOn)
		{
			Log.d(TAG, CLASS_NAME + "init()   registerReceiver mSaleTrackerReceiver");
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
			Log.d(TAG, CLASS_NAME + "onStart()  ******************* intent = null*********************");
			super.onStart(intent, startId);
			return;
		}

		String type = intent.getStringExtra(Contant.SEND_TO);
		Log.d(TAG, CLASS_NAME + "onStart() this content sendto = " + type);
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
		} catch (Exception e) {
			Log.e(TAG, CLASS_NAME+"onDestroy() Exception" + e.getMessage());
			e.printStackTrace();
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
					} catch (IllegalArgumentException e) {
						Log.d(TAG, CLASS_NAME+"StsAirplanReceiver()   registerReceiverSafe(), FAIL!");
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
				}
			}
		}
	}


	private class SaleTrackerReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
			Log.d(TAG, CLASS_NAME + "SaleTrackerReceiver() onReceive start: action = " + intent.getAction()
					+ "; elapsed time = " + SystemClock.elapsedRealtime()/1000);

			mIsSendSuccess = mStciSP.readSendedResult();
			if (intent.getAction().equals(Contant.STS_REFRESH)) {
				if (mIsSendSuccess || (mMsgSendNum > Contant.MAX_SEND_CONUT_BY_NET)) {
					Log.d(TAG, CLASS_NAME + "SaleTrackerReceiver()  The message is send success or the maximum sended number, stop SaleTrackerService");
					SaleTrackerService.this.stopSelf();
					return;
				}

				//read test value from testActivity,for quickly test
				mSpaceTime = mStciSP.readSpaceTime();
				mStartTime = mStciSP.readStartTime();

				int MsgSendMode = -1;
				Log.d(TAG, CLASS_NAME + "SaleTrackerReceiver() mDefaultSendType= " + mDefaultSendType);

				Log.d(TAG, CLASS_NAME + "SaleTrackerReceiver() send type by SMS  mMsgSendNum = "
						+ mMsgSendNum);
				if ((mMsgSendNum % 24) == 0) {
					MsgSendMode = Contant.ACTION_SEND_BY_SMS;
				}

				mStciSP.writeSendedNumber(++mMsgSendNum);

				if (MsgSendMode != -1) {
					if (MessageHandler.hasMessages(Contant.ACTION_SEND_BY_SMS)) {
						MessageHandler.removeMessages(Contant.ACTION_SEND_BY_SMS);
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

							mStciSP.writeSendedResult(true);

							refreshPanelStatus();

							SaleTrackerService.this.stopSelf();
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
				case Contant.ACTION_SEND_RST_BY_NET:
					Boolean val = (Boolean) msg.obj;
					Log.d(TAG, CLASS_NAME + "handleMessage() sended by net and  return  =" + val);
					if (val) {
						mIsSendSuccess = true;
						mStciSP.writeSendedToMeResult(val);

						// refresh SaleTrackerActivity panel
						refreshPanelStatus();

						SaleTrackerService.this.stopSelf();
					}
				default:
					Log.d(TAG, CLASS_NAME+"handleMessage() send type null");
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
			final SubscriptionManager subscriptionManager = SubscriptionManager.getDefault();
			if (defaultSubId < 0) {
				List<SubscriptionInfo> subInfoList = subscriptionManager.getActiveSubscriptionInfoList(mContext);
				Log.d(TAG, "sendContentBySMS(): subInfoList = " + subInfoList);
				if (subInfoList != null && subInfoList.size() >= 1) {
					for (SubscriptionInfo subInfo : subInfoList) {
						Log.d(TAG, "sendContentBySMS(): subInfo = " + subInfo);
						int subId = subInfo.getSubscriptionId();
						if (subscriptionManager.isActiveSubId(subId,mContext)) {
							subscriptionManager.setDefaultSmsSubId(subId,mContext);
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
				subscriptionManager.setDefaultSmsSubId(defaultSubId, mContext);
			}
		} catch (SecurityException e) {
			Log.d(TAG, CLASS_NAME + " send sms fail");
		}
		Log.d(TAG, CLASS_NAME + "sendContentBySMS()  end");

	}

	private boolean isSmsAvailable() {
		int sim_state;

		sim_state = mTm.getSimState();
		Log.d(TAG, CLASS_NAME+"isSmsAvailable(): sim_state  " + sim_state);
		return (TelephonyManager.SIM_STATE_READY == sim_state) ? true : false;
	}

	/**
	 * @param
	 * @return
	 * getIMEI for PK
	 */
	public static String getIMEIBD() {
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

		String sim_network, sim_operator;

		String PLMNTest1= new String("46000");//China Mobile

		String PLMNTest2= new String("46002");//China Mobile

		String PLMNTest3= new String("46007");//China Mobile

		String PLMNTest4= new String("46001");//China Unicom

		List<String> operatorList = new ArrayList<>(4);
		operatorList.add("46000");//China Mobile
		operatorList.add("46002");//China Mobile
		operatorList.add("46007");//China Mobile
		operatorList.add("46001");//China Unicom

		sim_network = mTm.getNetworkOperatorName();
		sim_operator = mTm.getSimOperator();
		String testServerNum = mStciSP.readServerNumber();


		Log.d(TAG, CLASS_NAME + "setDestNum()  sim_network: " + sim_network + ";sim_operator :"
				+ sim_operator);

		String defServerNum = "15920026432";
		if((operatorList.contains(sim_operator)) && (Contant.SERVER_NUMBER.equals(testServerNum))) {
			mStciSP.writeServerNumber(defServerNum);
		}

		mStrPhoneNo = mStciSP.readServerNumber();
		Log.d(TAG, CLASS_NAME + "setDestNum() =" + mStrPhoneNo);
	}

	public String setSendContent() {
		StringBuffer smsContent = new StringBuffer();

		mStrIMEI = getIMEIBD();

		// Client No
		String SAP_NO = mClientNo;

		// Client product model
		StringBuffer PRODUCT_NO = new StringBuffer();
		String model = Build.MODEL;
		PRODUCT_NO.append(model);

		// weijie created. 17-3-8. Modify for symphony
		smsContent.append("SYST").append(" " + mStrIMEI).append(" " + PRODUCT_NO);

		Log.d(TAG, CLASS_NAME+"setSendContent() SendString=" + smsContent.toString());

		return smsContent.toString();

	}

	/********************************** read config from xml end ************************************/

	public void refreshPanelStatus(){
		Log.d(TAG, CLASS_NAME + "refreshPanelStatus: ");
		Intent intent = new Intent(Contant.ACTION_REFRESH_PANEL);
		mContext.sendBroadcast(intent);
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

	private void sendContentByNetwork() {
		String msg_contents = setSendContent();

		if ("".equals(msg_contents)) {
			Log.e(TAG, CLASS_NAME +
					"sendContentByNetwork()  sendContentByNetwork--> send_sms GET msg_contents  fail");
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
}
