package com.ape.saletracker;

import android.content.Context;
import android.util.Log;

public class SaleTrackerConfigSP {
	private static final String TAG = "SaleTracker";
	private static final String CLASS_NAME = "SaleTrackerConfigSP---->";
	public static final String KEY_SENDED_SUCCESS = "KEY_SENDED_SUCCESS";
	public static final String KEY_SENDED_NUMBER = "KEY_SENDED_NUMBER";

	//add send content to tme wap address
	private static final String KEY_TMEWAP_SENDED = "KEY_TMEWAP_SENDED";
	private static Context mContext;

	public void init(Context context){
		mContext = context;
	}

	/***********add send content to tme wap address **********/
	public Boolean readConfigForTmeWapAddr() {
		boolean  bRet = mContext.getSharedPreferences(Contant.STSDATA_CONFIG,mContext.MODE_PRIVATE)
				.getBoolean(KEY_TMEWAP_SENDED, false);
		Log.d(TAG, CLASS_NAME + "readConfigForTmeWapAddr() =" + bRet);
		return bRet;
	}

	public void writeConfigForTmeWapAddr(boolean flag) {
		mContext.getSharedPreferences(Contant.STSDATA_CONFIG,mContext.MODE_PRIVATE).edit().putBoolean(KEY_TMEWAP_SENDED, flag).commit();
		Log.d(TAG, CLASS_NAME + " writeConfigForTmeWapAddr() end flag=" + flag);
	}
	/***********add send content to tme wap address **********/

	public boolean readSendedResult(){
		boolean res = mContext.getSharedPreferences(Contant.STSDATA_CONFIG,mContext.MODE_PRIVATE)
				.getBoolean(KEY_SENDED_SUCCESS, false);
		Log.d(TAG, CLASS_NAME + "readSendedResult: res = " + res);
		return res;
	}

	public void writeSendedResult(boolean res){
		Log.d(TAG, CLASS_NAME + "writeSendedResult: res = " + res);
		mContext.getSharedPreferences(Contant.STSDATA_CONFIG,mContext.MODE_PRIVATE)
				.edit().putBoolean(KEY_SENDED_SUCCESS, res).commit();
	}

	public int readSendedNumber(){
		int res = mContext.getSharedPreferences(Contant.STSDATA_CONFIG,mContext.MODE_PRIVATE)
				.getInt(KEY_SENDED_NUMBER, 0);
		Log.d(TAG, CLASS_NAME + "readSendedNumber: res = " + res);
		return res;
	}

	public void writeSendedNumber(int num){
		Log.d(TAG, CLASS_NAME + "writeSendedNumber: res = " + num);
		mContext.getSharedPreferences(Contant.STSDATA_CONFIG,mContext.MODE_PRIVATE)
				.edit().putInt(KEY_SENDED_NUMBER, num).commit();
	}
}

