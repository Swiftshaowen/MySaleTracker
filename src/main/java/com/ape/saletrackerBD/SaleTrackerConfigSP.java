package com.ape.saletrackerBD;

import android.content.Context;
import android.util.Log;

public class SaleTrackerConfigSP {
	private static final String TAG = "SaleTrackerBD";
	private static final String CLASS_NAME = "SaleTrackerConfigSP---->";


	private static Context mContext;
	private static SaleTrackerConfigSP mInstance;

	public static SaleTrackerConfigSP init(Context context){
		mContext = context;
		if (mInstance == null) {
			mInstance = new SaleTrackerConfigSP();
		}
		return mInstance;
	}

	/***********add send content to tme wap address **********/
//	public Boolean readConfigForTmeWapAddr() {
//		boolean  bRet = mContext.getSharedPreferences(Contant.STSDATA_CONFIG,mContext.MODE_PRIVATE)
//				.getBoolean(KEY_TMEWAP_SENDED, false);
//		Log.d(TAG, CLASS_NAME + "readConfigForTmeWapAddr() =" + bRet);
//		return bRet;
//	}

//	public void writeConfigForTmeWapAddr(boolean flag) {
//		mContext.getSharedPreferences(Contant.STSDATA_CONFIG,mContext.MODE_PRIVATE).edit().putBoolean(KEY_TMEWAP_SENDED, flag).commit();
//		Log.d(TAG, CLASS_NAME + " writeConfigForTmeWapAddr() end flag=" + flag);
//	}
	/***********add send content to tme wap address **********/

	public boolean readSendedResult(){
		boolean res = mContext.getSharedPreferences(Contant.STSDATA_CONFIG,mContext.MODE_PRIVATE)
				.getBoolean(Contant.KEY_SENDED_SUCCESS, false);
		Log.d(TAG, CLASS_NAME + "readSendedResult: res = " + res);
		return res;
	}

	public void writeSendedResult(boolean res){
		Log.d(TAG, CLASS_NAME + "writeSendedResult: res = " + res);
		mContext.getSharedPreferences(Contant.STSDATA_CONFIG,mContext.MODE_PRIVATE)
				.edit().putBoolean(Contant.KEY_SENDED_SUCCESS, res).commit();
	}

	public boolean readSendedToMeResult(){
		boolean res = mContext.getSharedPreferences(Contant.STSDATA_CONFIG,mContext.MODE_PRIVATE)
				.getBoolean(Contant.KEY_SENDED_TOME_SUCCESS, false);
		Log.d(TAG, CLASS_NAME + "readSendedToMeResult: res = " + res);
		return res;
	}

	public void writeSendedToMeResult(boolean res){
		Log.d(TAG, CLASS_NAME + "writeSendedToMeResult: res = " + res);
		mContext.getSharedPreferences(Contant.STSDATA_CONFIG,mContext.MODE_PRIVATE)
				.edit().putBoolean(Contant.KEY_SENDED_TOME_SUCCESS, res).commit();
	}

	public int readSendedNumber(){
		int res = mContext.getSharedPreferences(Contant.STSDATA_CONFIG,mContext.MODE_PRIVATE)
				.getInt(Contant.KEY_SENDED_NUMBER, 0);
		Log.d(TAG, CLASS_NAME + "readSendedNumber: res = " + res);
		return res;
	}

	public void writeSendedNumber(int num){
		Log.d(TAG, CLASS_NAME + "writeSendedNumber: res = " + num);
		mContext.getSharedPreferences(Contant.STSDATA_CONFIG,mContext.MODE_PRIVATE)
				.edit().putInt(Contant.KEY_SENDED_NUMBER, num).commit();
	}

	public int readStartTime(){
		int res = mContext.getSharedPreferences(Contant.STSDATA_CONFIG,mContext.MODE_PRIVATE)
				.getInt(Contant.KEY_OPEN_TIME, Contant.START_TIME);
		Log.d(TAG, CLASS_NAME + "readStartTime: res = " + res);
		return res;
	}

	public void writeStartTime(int num){
		Log.d(TAG, CLASS_NAME + "writeStartTime: res = " + num);
		mContext.getSharedPreferences(Contant.STSDATA_CONFIG,mContext.MODE_PRIVATE)
				.edit().putInt(Contant.KEY_OPEN_TIME, num).commit();
	}

	public int readSpaceTime(){
		int res = mContext.getSharedPreferences(Contant.STSDATA_CONFIG,mContext.MODE_PRIVATE)
				.getInt(Contant.KEY_SPACE_TIME, Contant.SPACE_TIME);
		Log.d(TAG, CLASS_NAME + "readSpaceTime: res = " + res);
		return res;
	}

	public void writeSpaceTime(int num){
		Log.d(TAG, CLASS_NAME + "writeSpaceTime: res = " + num);
		mContext.getSharedPreferences(Contant.STSDATA_CONFIG,mContext.MODE_PRIVATE)
				.edit().putInt(Contant.KEY_SPACE_TIME, num).commit();
	}

	public String readServerNumber(){
		String res = mContext.getSharedPreferences(Contant.STSDATA_CONFIG,mContext.MODE_PRIVATE)
				.getString(Contant.KEY_SERVER_NUMBER, Contant.SERVER_NUMBER);
		Log.d(TAG, CLASS_NAME + "readServerNumber: res = " + res);
		return res;
	}

	public void writeServerNumber(String num){
		Log.d(TAG, CLASS_NAME + "writeServerNumber: res = " + num);
		mContext.getSharedPreferences(Contant.STSDATA_CONFIG,mContext.MODE_PRIVATE)
				.edit().putString(Contant.KEY_SERVER_NUMBER, num).commit();
	}
}

