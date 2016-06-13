package com.ape.saletracker;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;


public class SaleTrackerBootReceiver extends BroadcastReceiver {

	private static final String TAG = "SaleTracker";
	private static final String CLASS_NAME = "SaleTrackerBootReceiver---->";

	private int spacetime;
		
	@Override
	public void onReceive(Context context, Intent intent) {
		if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
			Log.d(TAG, CLASS_NAME+" onReceive: ACTION_BOOT_COMPLETED");
			boolean isOpen = context.getResources().getBoolean(R.bool.is_open_function);
			if (!isOpen) {
				Log.e(TAG, CLASS_NAME+" onReceive: saleTrack setting is close");
				return;
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
                Log.e(TAG, CLASS_NAME+" onReceive: open config file error ");
                return;
            }

			// The 1st digit represent the flag of send success
			int temp_data = data & 0x000000ff;
			boolean isSended = temp_data == 0x01 ? true : false;
			Log.e(TAG, CLASS_NAME+" onReceive: read config value is data = "+data + " isSended = " + isSended);

			// The 9th to 24th digit represent the number sended
			int maxDay = (data >> 8) & 0x0000ffff;
			boolean dataFlag = maxDay < (Contant.MAX_SEND_CONUT_BY_NET) ? true :false ;
			SharedPreferences pre = context.getSharedPreferences(Contant.STSDATA_CONFIG, Context.MODE_PRIVATE);
			spacetime = pre.getInt(Contant.KEY_SPACE_TIME, Contant.SPACE_TIME);
			Log.e(TAG, CLASS_NAME+" onReceive: maxday ="+maxDay);

			if (!isSended && dataFlag) {
                sendPendingIntent(context, Contant.SEND_TO_CUSTOM);
			}else if(!bSendedToTmeNet && dataFlag){
                // add send content to tme wap address
                sendPendingIntent(context, Contant.SEND_TO_TME);
			}
		}
	}

    private void sendPendingIntent(Context context, String sendWho){
		Log.e(TAG, CLASS_NAME+" sendPendingIntent: sendWho = "+sendWho);
        Intent newIntent = new Intent(context,	SaleTrackerService.class);
        newIntent.putExtra(Contant.SEND_TO, sendWho);
        context.startService(newIntent);

        AlarmManager am = (AlarmManager)context.getApplicationContext().getSystemService(Context.ALARM_SERVICE);
        Intent stsIntent = new Intent(Contant.STS_REFRESH);
        PendingIntent alarmIntent = PendingIntent.getBroadcast(context.getApplicationContext(), 0, stsIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        am.cancel(alarmIntent);
        am.setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, 0, spacetime * 60*1000 , alarmIntent);
    }
}
