package com.ape.saletrackerPK;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
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
import java.util.Map;



public class SaleTrackerService extends Service {
	private static final String TAG = "SaleTrackerPK";
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

		resetUserSetupObserver(mContext);

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
			mContext.getContentResolver().unregisterContentObserver(mUserSetupObserver);
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

	private void popNotifyWindow(Context context){
		Log.d(TAG, CLASS_NAME + "popNotifyWindow()  dialog start");
		Intent Dialog = new Intent(context, WIKOSTSScreen.class);
		Dialog.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		context.startActivity(Dialog);
		Log.d(TAG, CLASS_NAME + "popNotifyWindow()  dialog finish");
	}

	private boolean isSmsAvailable() {
		int sim_state;

		sim_state = mTm.getSimState();
		Log.d(TAG, CLASS_NAME+"isSmsAvailable(): sim_state  " + sim_state);
		return (TelephonyManager.SIM_STATE_READY == sim_state) ? true : false;
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

		mStrIMEI = getIMEI();
		if (mStrIMEI.equals(Contant.NULL_IMEI)) {
			Log.d(TAG, CLASS_NAME +
					"init()    ********error********getIMEI() = null ***********error******");
		}
		// tishi
		String REG = mStrIMEI;

		// Client No
		String SAP_NO = mClientNo;

		// Client product model
		StringBuffer PRODUCT_NO = new StringBuffer();
		String model = SystemProperties.get("ro.product.model.sts", Build.MODEL);
		PRODUCT_NO.append(model);

		// add sn no 20150703
		String SN_NO = Build.SERIAL;

		// Soft version
		StringBuffer SOFTWARE_NO = new StringBuffer();
		String customVersion = SystemProperties.get("ro.custom.build.version");
		SOFTWARE_NO.append(customVersion);

		// Cell ID
		int CELL_ID = 0;
		CellLocation cellLocation = mTm.getCellLocation();
		if (cellLocation instanceof GsmCellLocation) {
			CELL_ID = ((GsmCellLocation) cellLocation).getCid();
		}

		// weijie created. 17-3-8. Modify for QMbile
		smsContent.append("NOIR IMEI ").append(PRODUCT_NO).append(" " + getIMEIPK()).append(" " + CELL_ID);

		Log.d(TAG, CLASS_NAME+"setSendContent() SendString=" + smsContent.toString());

		return smsContent.toString();

	}

	/********************************** read config from xml end ************************************/

	public void refreshPanelStatus(){
		Log.d(TAG, CLASS_NAME + "refreshPanelStatus: ");
		Intent intent = new Intent(Contant.ACTION_REFRESH_PANEL);
		mContext.sendBroadcast(intent);
	}

	// ensure quick settings is disabled until the current user makes it through the setup wizard
	private ContentObserver mUserSetupObserver = new ContentObserver(new Handler()) {
		@Override
		public void onChange(boolean selfChange, Uri uri) {
			super.onChange(selfChange, uri);
			Log.d(TAG, CLASS_NAME + "onChange: selfChange = " + selfChange
					+ "; uri = " + uri);

//			Log.d(TAG, CLASS_NAME + "onChange: USER_SETUP_COMPLETE = " + Settings.Secure.getInt(
//					mContext.getContentResolver(),"user_setup_complete", 0));
			boolean needNotify = mStciSP.readNotifyNeed();
			int deviceProvisioned = Settings.Secure.getInt(mContext.getContentResolver()
					,Settings.Global.DEVICE_PROVISIONED, 0);
			Log.d(TAG, CLASS_NAME + "onChange: DEVICE_PROVISIONED = " + deviceProvisioned);
			Log.d(TAG, CLASS_NAME + "onChange: needNotify =" + needNotify);
			if (needNotify && (deviceProvisioned == 1)) {
				// need to pop up notify after setup wizard finished
				new Handler().postDelayed(new Runnable() {
					@Override
					public void run() {
						Log.d(TAG, CLASS_NAME + "start popup dialog start");
						popNotifyWindow(mContext);
						mStciSP.writeNotifyNeed(false);
						Log.d(TAG, CLASS_NAME + "start popup dialog finish");
					}
				}, 10000);
			}
		}
	};

	private void resetUserSetupObserver(Context context ) {
		Log.d(TAG, CLASS_NAME + "resetUserSetupObserver");

		if (mStciSP.readNotifyNeed()) {
			context.getContentResolver().unregisterContentObserver(mUserSetupObserver);
//			context.getContentResolver().registerContentObserver(
//                    Settings.Secure.getUriFor("user_setup_complete"), true,
//                    mUserSetupObserver);

			context.getContentResolver().registerContentObserver(
					Settings.Global.getUriFor(Settings.Global.DEVICE_PROVISIONED), true,
					mUserSetupObserver);
			mUserSetupObserver.onChange(false, Settings.Global.getUriFor(Settings.Global.DEVICE_PROVISIONED));
		}
	}
}
