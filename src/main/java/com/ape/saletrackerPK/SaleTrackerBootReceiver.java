package com.ape.saletrackerPK;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

import java.util.Map;

import static android.media.AudioManager.ACTION_AUDIO_BECOMING_NOISY;


public class SaleTrackerBootReceiver extends BroadcastReceiver {

	private static final String TAG = "SaleTrackerPK";
	private static final String CLASS_NAME = "SaleTrackerBootReceiver---->";
	private static final String VERSION_NUMBER = "20170405";
	private static final String CONFIG_START_TIME = "start_time";
	private static final String CONFIG_SPACE_TIME = "space_time";

	private int mSpaceTime = Contant.SPACE_TIME;
	private int mStartTime = Contant.START_TIME;
	private int DEFAULT_SPACE_TIME = Contant.SPACE_TIME;
	private int DEFAULT_START_TIME = Contant.START_TIME;

	private static SaleTrackerConfigSP mStciSP;

	@Override
	public void onReceive(Context context, Intent intent) {
		if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())
				|| ACTION_AUDIO_BECOMING_NOISY.equals(intent.getAction())) {
			Log.d(TAG, CLASS_NAME + "onReceive: " + intent.getAction() + "; VERSION_NUMBER = " + VERSION_NUMBER);
			// init
			mStciSP = SaleTrackerConfigSP.init(context);
			mSpaceTime = mStciSP.readSpaceTime();
			mStartTime = mStciSP.readStartTime();

			// msg has been sended?
			boolean isSended = mStciSP.readSendedResult();
			// sended number
			int sendedNum = mStciSP.readSendedNumber();
			// need to send?
			boolean isSendFlag = (sendedNum < Contant.MAX_SEND_CONUT_BY_NET) ? true : false;


			Log.d(TAG, CLASS_NAME + "onReceive: isSended = " + isSended
					+ "; sendedNum = " + sendedNum
					+ "; mSpaceTime = " + mSpaceTime
					+ "; mStartTime " + mStartTime
					+ "; DEFAULT_SPACE_TIME = " + DEFAULT_SPACE_TIME
					+ "; DEFAULT_START_TIME = " + DEFAULT_START_TIME);

			if (!isSended && isSendFlag) {
                sendPendingIntent(context, Contant.SEND_TO_CUSTOM);
			}
		}
	}

    private void sendPendingIntent(Context context, String sendWho){
		Log.d(TAG, CLASS_NAME+"sendPendingIntent: sendWho = "+sendWho);
		// start SaleTrackerService
        Intent newIntent = new Intent(context,	SaleTrackerService.class);
        newIntent.putExtra(Contant.SEND_TO, sendWho);
        context.startService(newIntent);

		// start circular broadcast
        AlarmManager am = (AlarmManager)context.getApplicationContext().getSystemService(Context.ALARM_SERVICE);
        Intent stsIntent = new Intent(Contant.STS_REFRESH);
        PendingIntent alarmIntent = PendingIntent.getBroadcast(context.getApplicationContext(), 0, stsIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        am.cancel(alarmIntent);
        am.setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, mStartTime * 60*1000 , mSpaceTime * 60*1000 , alarmIntent);
    }

}
