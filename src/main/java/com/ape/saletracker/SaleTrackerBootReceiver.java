package com.ape.saletracker;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Environment;
import android.os.SystemProperties;
import android.util.Log;
import android.util.Xml;

import org.xmlpull.v1.XmlPullParser;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;


public class SaleTrackerBootReceiver extends BroadcastReceiver {

	private static final String TAG = "SaleTracker";
	private static final String CLASS_NAME = "SaleTrackerBootReceiver---->";

	private int mSpaceTime;
	private int DEFAULT_SPACE_TIME = Contant.SPACE_TIME;
	private int mStartTime;
	private int DEFAULT_START_TIME = Contant.START_TIME;

//	public static Map<String, SaleTrackerConfigs> map = new HashMap<String, SaleTrackerConfigs>();

	@Override
	public void onReceive(Context context, Intent intent) {
		if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
			Log.d(TAG, CLASS_NAME+"onReceive: ACTION_BOOT_COMPLETED");
			boolean isOpen = context.getResources().getBoolean(R.bool.is_open_function);
			if (!isOpen) {
				Log.e(TAG, CLASS_NAME+"onReceive: saleTrack setting is close");
				return;
			}

			SaleTrackerUti.readSendParamFromXml(context);

			String projectName = SystemProperties.get("ro.project", "trunk");
			if(SaleTrackerUti.map.get(projectName) != null){
				DEFAULT_SPACE_TIME = Integer.parseInt(SaleTrackerUti.map.get(projectName)._space_time);
				DEFAULT_START_TIME = Integer.parseInt(SaleTrackerUti.map.get(projectName)._start_time);
				Log.d(TAG, CLASS_NAME+"onReceive: DEFAULT_SPACE_TIME = "+DEFAULT_SPACE_TIME
					+"; DEFAULT_START_TIME = "+DEFAULT_START_TIME);
			}

			int data = 0;
			//add send content to tme wap address
			boolean bSendedToTmeNet = true;
			if(SaleTrackerService.STS_CONFIG_TYPE == Contant.STS_SP){
				SaleTrackerConfigSP stci = new SaleTrackerConfigSP();
				stci.init(context);
				data = stci.read_secro();
				//add send content to tme wap address
				bSendedToTmeNet = stci.isSendedToTmeWapAddr();
			}

			//if the config file was deleted,stop to send sts continue
			if(data == -1){
                Log.e(TAG, CLASS_NAME+"onReceive: open config file error ");
                return;
            }

			// The 1st digit represent the flag of send success
			int temp_data = data & 0x000000ff;
			boolean isSended = temp_data == 0x01 ? true : false;
			Log.e(TAG, CLASS_NAME+"onReceive: read config value is data = "+data + " isSended = " + isSended);

			// The 9th to 24th digit represent the number sended
			int maxDay = (data >> 8) & 0x0000ffff;
			boolean dataFlag = maxDay < (Contant.MAX_SEND_CONUT_BY_NET) ? true :false ;
			SharedPreferences pre = context.getSharedPreferences(Contant.STSDATA_CONFIG, Context.MODE_PRIVATE);
			mSpaceTime = pre.getInt(Contant.KEY_SPACE_TIME, DEFAULT_SPACE_TIME);
			mStartTime = pre.getInt(Contant.KEY_OPEN_TIME, DEFAULT_START_TIME);
			Log.e(TAG, CLASS_NAME+"onReceive: maxday ="+maxDay+"; mSpaceTime = "+mSpaceTime +"; mStartTime "+mStartTime);

			if (!isSended && dataFlag) {
                sendPendingIntent(context, Contant.SEND_TO_CUSTOM);
			}else if(!bSendedToTmeNet && dataFlag){
                // add send content to tme wap address
                sendPendingIntent(context, Contant.SEND_TO_TME);
			}
		}
	}

    private void sendPendingIntent(Context context, String sendWho){
		Log.e(TAG, CLASS_NAME+"sendPendingIntent: sendWho = "+sendWho);
        Intent newIntent = new Intent(context,	SaleTrackerService.class);
        newIntent.putExtra(Contant.SEND_TO, sendWho);
        context.startService(newIntent);

        AlarmManager am = (AlarmManager)context.getApplicationContext().getSystemService(Context.ALARM_SERVICE);
        Intent stsIntent = new Intent(Contant.STS_REFRESH);
        PendingIntent alarmIntent = PendingIntent.getBroadcast(context.getApplicationContext(), 0, stsIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        am.cancel(alarmIntent);
        am.setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, mStartTime * 60*1000 , mSpaceTime * 60*1000 , alarmIntent);
    }
/*
	private void readSendParamFromXml(){

		// weijie.wang created.  5/13/16 start
		*//*String strCountryName = SystemProperties.get("ro.project", "trunk");
		Log.w(TAG, CLASS_NAME+" readSendParamFromXml() strCountryName = "+strCountryName);
		String[] attArray = getResources().getStringArray(
				getResources().getIdentifier(strCountryName,"array",getPackageName()));

		if(attArray != null){
			mClientNo = attArray[0];
			mDefaultSendType = Integer.parseInt(attArray[1]);
			mIsNeedNoticePop = Boolean.parseBoolean(attArray[2]);
			mHosturl = attArray[3];
			mStartTimeFromXML = Integer.parseInt(attArray[4]);
			mSpaceTimeFromXML = Integer.parseInt(attArray[5]);
			mStrCountry = attArray[6];
			mStrModel = attArray[7];
			mDefaultSendTypeTmp = mDefaultSendType;

			Log.w(TAG, CLASS_NAME+" readSendParamFromXml()    "
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
			Log.w(TAG, CLASS_NAME+" readSendParamFromXml()  attArray is NULL ");
		}*//*
		// weijie.wang created.  5/13/16 end
		String path = Environment.getExternalStorageDirectory().toString();
		String fileName = "/system/etc/ApeSaleTrackerConfig.xml";

		String projectName = SystemProperties.get("ro.project", "trunk");
		Log.w(TAG, CLASS_NAME+" readSendParamFromXml() projectName = "+projectName
				+"; config file = "+fileName);
		String countryName = null;
		try{
			File xmlFlie = new File(fileName);
			InputStream inputStream = new FileInputStream(xmlFlie);
			XmlPullParser parser = Xml.newPullParser();
			parser.setInput(inputStream, "UTF-8");
			int eventCode = parser.getEventType();// 事件类型
			SaleTrackerConfigs config = null;
			while (eventCode != XmlPullParser.END_DOCUMENT) {
				switch (eventCode) {
					case XmlPullParser.START_DOCUMENT:// 开始文档事件
						break;
					case XmlPullParser.START_TAG:// 元素开始标志
						if ("SaleTracker".equals(parser.getName())) {
							config = new SaleTrackerConfigs();
						} else if (config != null) {
							if ("name".equals(parser.getName())) {
								config._name = parser.nextText();
								countryName = config._name;
							} else if ("client_no".equals(parser.getName())) {
								config._client_no = parser.nextText();
							} else if ("send_type".equals(parser.getName())) {
								config._send_type = parser.nextText();
							} else if ("notice".equals(parser.getName())) {
								config._notice = parser.nextText();
							} else if ("host_url".equals(parser.getName())) {
								config._host_url = parser.nextText();
							} else if ("start_time".equals(parser.getName())) {
								config._start_time = parser.nextText();
							} else if ("space_time".equals(parser.getName())) {
								config._space_time = parser.nextText();
							} else if ("country_type".equals(parser.getName())) {
								config._country_type = parser.nextText();
							} else if ("model_type".equals(parser.getName())) {
								config._model_type = parser.nextText();
							} else if ("phone_no".equals(parser.getName())) {
								config._phone_no = parser.nextText();
							}
						}
						break;
					case XmlPullParser.END_TAG://元素结束标志
						if ("SaleTracker".equals(parser.getName()) && config != null) {
							map.put(countryName,config);
							config = null;
						}
						break;
				}
				eventCode = parser.next();
			}
		}catch (FileNotFoundException e){
			Log.d(TAG,CLASS_NAME+" readSendParamFromXml(): FileNotFoundException "+fileName);
			e.printStackTrace();
		}catch (Exception e){
			e.printStackTrace();
		}
	}*/
}
