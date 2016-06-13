package com.ape.saletracker;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;

public class WIKOSTSScreen extends Activity {

	public static final int FLAG_HOMEKEY_DISPATCHED = 0x80000000;
	
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
		//guchunhua,DATE20150402,modify for FEAALFRA-235,END
		setContentView(R.layout.activity_wikostsscreen);
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
