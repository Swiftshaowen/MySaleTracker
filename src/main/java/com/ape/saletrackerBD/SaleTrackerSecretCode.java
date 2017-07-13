package com.ape.saletrackerBD;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import static com.android.internal.telephony.TelephonyIntents.SECRET_CODE_ACTION;

 public class SaleTrackerSecretCode extends BroadcastReceiver {   

	private static final String TAG = "SaleTrackerBD";
	
 @Override  
    public void onReceive(Context arg0, Intent arg1) {   
        //if (arg1.getAction().equals("android.provider.Telephony.SECRET_CODE")) 
	  if(arg1.getAction().equals(SECRET_CODE_ACTION)){

		Log.d(TAG, "SaleTrackerSecretCode start");
		   try{
				Intent intent = new Intent();
				intent.setClassName("com.ape.SaleTrackerBD", "com.ape.SaleTrackerBD.SaleTrackerActivity");
				intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);  //must
				arg0.startActivity(intent);
				return;
		}catch (Exception e) {
				return ;
			}

        }   
    }   
  
}
