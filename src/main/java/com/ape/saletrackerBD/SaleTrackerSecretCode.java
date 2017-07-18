package com.ape.saletrackerBD;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import static com.android.internal.telephony.TelephonyIntents.SECRET_CODE_ACTION;

public class SaleTrackerSecretCode extends BroadcastReceiver {

    private static final String TAG = "SaleTrackerBD";

    @Override
    public void onReceive(Context arg0, Intent arg1) {
        //if (arg1.getAction().equals("android.provider.Telephony.SECRET_CODE")) 
        if (arg1.getAction().equals(SECRET_CODE_ACTION)) {

            Log.d(TAG, "SaleTrackerSecretCode start");
            try {
                Intent intent = new Intent(arg0, SaleTrackerActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);  //must
                arg0.startActivity(intent);
                return;
            } catch (Exception e) {
                Log.d(TAG, "onReceive: start SaleTrackerActivity fail");
                e.printStackTrace(System.out);
                return;
            }
        }
    }

}
