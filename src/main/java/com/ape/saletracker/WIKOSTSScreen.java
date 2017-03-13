package com.ape.saletracker;

import android.app.Activity;
import android.os.Bundle;
import android.os.SystemProperties;
import android.util.Log;
import android.view.KeyEvent;
import android.widget.Button;
import android.view.View;

public class WIKOSTSScreen extends Activity {

	public static final int FLAG_HOMEKEY_DISPATCHED = 0x80000000;

	public static final int STATUS_BAR_DISABLE_RECENT = 0x01000000;
	public static final int STATUS_BAR_DISABLE_HOME = 0x00200000;
	public static final int STATUS_BAR_DISABLE_BACK = 0x00400000;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		//guchunhua,DATE20150402,modify for FEAALFRA-235,START
        boolean hasNavBar = true ; // this.getResources().getBoolean(
               // com.android.internal.R.bool.config_showNavigationBar);
		if ("1".equals(hasNavBar)) {
            hasNavBar = false;
        } else if ("0".equals(hasNavBar)) {
            hasNavBar = true;
        }
		Log.d("guchunhua","onCreate hasNavBar = "+ hasNavBar);
		/*if(hasNavBar && SystemProperties.get("ro.wiko", "trunk").equals("wiko"))
		{
			this.getWindow().getDecorView().setSystemUiVisibility
				(View.STATUS_BAR_DISABLE_RECENT|View.STATUS_BAR_DISABLE_HOME| View.STATUS_BAR_DISABLE_BACK);
		}*///// TODO: 16-1-26
		//guchunhua,DATE20150402,modify for FEAAL

		this.getWindow().getDecorView().setSystemUiVisibility
				(STATUS_BAR_DISABLE_RECENT|STATUS_BAR_DISABLE_HOME|STATUS_BAR_DISABLE_BACK);

		//guchunhua,DATE20150402,modify for FEAALFRA-235,END
		if ("QMobile".equalsIgnoreCase(SystemProperties.get("ro.product.brand", "trunk"))) {
			setContentView(R.layout.activity_qmobilescreen);
		} else {
			setContentView(R.layout.activity_wikostsscreen);
		}
		Button button_yes = (Button)findViewById(R.id.button_ok);
		//Button button_no = (Button)findViewById(R.id.button_no);
		button_yes.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				finish();
			}
		});
	}
	
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
        Log.d("guchunhua","onKeyDown keyCode = "+ keyCode);
		Log.d("guchunhua","onKeyDown event = "+ event);
	    switch (keyCode) {
	        case KeyEvent.KEYCODE_BACK:	
	        return true;
	    }
	    return super.onKeyDown(keyCode, event);
	}	
}
