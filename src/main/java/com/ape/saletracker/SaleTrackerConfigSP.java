package com.ape.saletracker;

import android.content.Context;
import android.util.Log;

public class SaleTrackerConfigSP {
	private static final String TAG = "SaleTracker";
	private static final String CLASS_NAME = "SaleTrackerConfigSP---->";
	private static final String CONFIG_DATA = "CONFIG_DATA";

	//add send content to tme wap address
	private static final String DATA_TMEWAP_SENDED = "DATA_TMEWAP_SENDED";
	private static Context mContext;

	public void init(Context context){
		mContext = context;
	}

	/***********add send content to tme wap address **********/
	public Boolean isSendedToTmeWapAddr() {
		boolean  bRet = false;
		bRet = mContext.getSharedPreferences(Contant.STSDATA_CONFIG,mContext.MODE_PRIVATE).getBoolean(DATA_TMEWAP_SENDED, false);
		Log.d(TAG, CLASS_NAME+" isSendedToTmeWapAddr() end bRet=" + bRet);
		return bRet;
	}

	public Boolean writeConfigForTmeWapAddr(boolean flag) {
		boolean bRet = true;
		mContext.getSharedPreferences(Contant.STSDATA_CONFIG,mContext.MODE_PRIVATE).edit().putBoolean(DATA_TMEWAP_SENDED, flag).commit();
		Log.d(TAG, CLASS_NAME+" writeConfigForTmeWapAddr() end flag=" + flag);
		return bRet;
	}
	/***********add send content to tme wap address **********/

	public int read_secro() {
		int iRet = 0;		
		iRet = mContext.getSharedPreferences(Contant.STSDATA_CONFIG,mContext.MODE_PRIVATE).getInt(CONFIG_DATA, 0);
		Log.d(TAG, CLASS_NAME+"read_secro() end iRet=" + iRet);
		return iRet;

 	}
	public boolean write_secro(int data) {
		boolean bRet = true;
		mContext.getSharedPreferences(Contant.STSDATA_CONFIG,mContext.MODE_PRIVATE).edit().putInt(CONFIG_DATA, data).commit();
		Log.d(TAG, CLASS_NAME+"write_secro() end data=" + data);
		return bRet;
	}
}

