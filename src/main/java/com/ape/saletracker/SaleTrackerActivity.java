package com.ape.saletracker;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.SystemProperties;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.wrapper.stk.HideMethod;

import java.util.Map;

public class SaleTrackerActivity extends Activity {
	
	private static final String TAG = "SaleTracker";
	private static final String CLASS_NAME = "SaleTrackerActivity---->";
	private static String mVersion;
	private static String mStrSendResult = "unknow";

	private EditText mOpenTime;
	private EditText mSpaceTime;
	private EditText mDayTime;
	private CheckBox mNotify;
	private CheckBox mSwitchWhole;
	private Spinner mSpinner;
	public static SharedPreferences pre ;
	public static Editor ed ;

	private static Context mContext;

	private int DEFAULT_START_TIME = Contant.START_TIME;
	private int DEFAULT_SPACE_TIME = Contant.SPACE_TIME;
	private int DEFAULT_SEND_TYPE = Contant.MSG_SEND_BY_NET;

	private static final String CONFIG_SEND_TYPE = "send_type";
	private static final String CONFIG_START_TIME = "start_time";
	private static final String CONFIG_SPACE_TIME = "space_time";

	private static final String[] mStrings = {
        "sms", "net", "net and sms"
    };
	 
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.d(TAG, CLASS_NAME+"onCreate  SaleTrackerActivity");

		setContentView(R.layout.main);	
		mContext = getApplicationContext();

		// get version name
		try {
			PackageInfo packageInfo = mContext.getPackageManager().getPackageInfo(mContext.getPackageName(), 0);
			mVersion = "Version: "+packageInfo.versionName;
		} catch (PackageManager.NameNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		pickTimeConfigs();

		pre = getSharedPreferences(Contant.STSDATA_CONFIG, MODE_PRIVATE);
		ed = pre.edit();

		mOpenTime = (EditText)findViewById(R.id.editopentime);
		mSpaceTime = (EditText)findViewById(R.id.spacetime);
		mDayTime = (EditText)findViewById(R.id.daytime);

		mNotify = (CheckBox)findViewById(R.id.notify);
		mNotify.setChecked(pre.getBoolean(Contant.KEY_NOTIFY, getResources().getBoolean(R.bool.dialog_notify)));
		mNotify.setOnClickListener(new OnClickListener() {
		  @Override
		  public void onClick(View v) {
			  ed.putBoolean(Contant.KEY_NOTIFY, mNotify.isChecked());
			  ed.commit();
		  }
		});

		mSwitchWhole = (CheckBox)findViewById(R.id.switchSendType);
		if(SystemProperties.get("ro.project", "trunk").equals("oys_ru")){
				mSwitchWhole.setVisibility(View.INVISIBLE);
		}else{
				mSwitchWhole.setVisibility(View.VISIBLE);
		}
		mSwitchWhole.setChecked(pre.getBoolean(Contant.KEY_SWITCH_SENDTYPE, false));
		mSwitchWhole.setOnClickListener(new OnClickListener() {
			 @Override
			 public void onClick(View v) {
				 Log.d(TAG, CLASS_NAME+"mSwitchWhole onClick: isChecked = "+mSwitchWhole.isChecked());
				 ed.putBoolean(Contant.KEY_SWITCH_SENDTYPE, mSwitchWhole.isChecked());
				 ed.commit();

				 if (mSwitchWhole.isChecked() == false) {
					 mSpinner.setEnabled(false);
				 } else {
					 mSpinner.setEnabled(true);
				 }
				 getStatus();
			 }
		});

		mSpinner = (Spinner) findViewById(R.id.spinnerSendType);
		ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,android.R.layout.simple_spinner_item, mStrings);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		mSpinner.setAdapter(adapter);

		mSpinner.setOnItemSelectedListener( new OnItemSelectedListener() {
			public void onItemSelected(
						AdapterView<?> parent, View view, int position, long id) {
				ed.putInt(Contant.KEY_SELECT_SEND_TYPE, position);
				ed.commit();
				getStatus();
			}

			public void onNothingSelected(AdapterView<?> parent) {
			   ed.putInt(Contant.KEY_SELECT_SEND_TYPE, -1);
				ed.commit();
			}
		});

		/*if(mSwitchWhole.isChecked() == false)
		{
			mSpinner.setSelection(DEFAULT_SEND_TYPE);
			mSpinner.setEnabled(false);
		}
		else
		{
			mSpinner.setEnabled(true);
			mSpinner.setSelection(pre.getInt(Contant.KEY_SELECT_SEND_TYPE, DEFAULT_SEND_TYPE));
		}*/
		mSpinner.setEnabled(mSwitchWhole.isChecked() ? true : false);
		mSpinner.setSelection(pre.getInt(Contant.KEY_SELECT_SEND_TYPE, DEFAULT_SEND_TYPE));
		  
		TextView showVersionTv = (TextView)findViewById(R.id.tvShowVersion);
		showVersionTv.setText(mVersion);

		Button btSave = (Button)findViewById(R.id.btnSave);
		btSave.setOnClickListener(new OnClickListener() {			
			@Override
			public void onClick(View v) {
				if("0".equals(mOpenTime.getText().toString()) || "0".equals(mSpaceTime.getText().toString())
					 || "".equals(mOpenTime.getText().toString()) || "".equals(mSpaceTime.getText().toString()))
				{
					showToast(getResources().getString(R.string.sts_invalid_value));
				}
			  	else{
					ed.putInt(Contant.KEY_OPEN_TIME, Integer.parseInt(mOpenTime.getText().toString(), 10)); //mOpenTime.getText().toString());
					ed.putInt(Contant.KEY_SPACE_TIME, Integer.parseInt(mSpaceTime.getText().toString(),10));
					ed.putInt(Contant.KEY_DAY_TIME, Integer.parseInt(mDayTime.getText().toString(),10));
					ed.commit();	
					showToast("Save successful" );
				}
			}
		});

		Button btClear = (Button)findViewById(R.id.btnclear);
		btClear.setOnClickListener(new OnClickListener() {			
			@Override
			public void onClick(View v) {
				Log.d(TAG, CLASS_NAME+"setOnClickListener");
				if(Contant.STS_SP ==SaleTrackerService.STS_CONFIG_TYPE){
					SaleTrackerConfigSP stciSP = new SaleTrackerConfigSP();
					stciSP.init(mContext);
					stciSP.write_secro(0x00);
					stciSP.writeConfigForTmeWapAddr(false);
				}
				mSwitchWhole.setChecked(false);
				ed.putInt(Contant.KEY_OPEN_TIME, DEFAULT_START_TIME); //mOpenTime.getText().toString());
				ed.putInt(Contant.KEY_SPACE_TIME, DEFAULT_SPACE_TIME);
				ed.putBoolean(Contant.KEY_SWITCH_SENDTYPE, false);
				ed.commit();
				getStatus();
				showToast("Clear successful" );
			}
		});

		// Listen to the send result
		registerReceiver(refreshReceiver, new IntentFilter(Contant.ACTION_REFRESH_PANEL));
	}

	private void showToast(CharSequence msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }


	@Override
	protected void onDestroy() {
		Log.d(TAG, CLASS_NAME+"onDestroy");
		if(refreshReceiver != null){
			unregisterReceiver(refreshReceiver);
		}
		super.onDestroy();
	}

	@Override
	protected void onResume() {
		Log.d(TAG, CLASS_NAME+"onResume");
		super.onResume();
		getStatus();
		mOpenTime.setText(""+pre.getInt(Contant.KEY_OPEN_TIME,
				DEFAULT_START_TIME));
		mSpaceTime.setText(""+pre.getInt(Contant.KEY_SPACE_TIME,
				DEFAULT_SPACE_TIME));
		mDayTime.setText("" + pre.getInt(Contant.KEY_DAY_TIME,
				Contant.DAY_TIME));
	}

	public void getStatus(){
		Log.d(TAG, CLASS_NAME+"getStatus: ");
		TextView  showOpenFileTextView= (TextView)this.findViewById(R.id.tvShowOpenFile);
		TextView setResutTextView = (TextView)this.findViewById(R.id.tvShowSendResult);
		TextView sendTypeTextView = (TextView)this.findViewById(R.id.tvShowSendType);
		setResutTextView.setText("Send result : No"+ "    result2 : No");
		
		if(Contant.STS_JNI ==SaleTrackerService.STS_CONFIG_TYPE){
			showOpenFileTextView.setText ("Open Config File :  OK");
		}else if(Contant.STS_NV == SaleTrackerService.STS_CONFIG_TYPE){
			showOpenFileTextView.setText ("Open Config File :  OK (NVRAM VERSION)");
		}
		else if(Contant.STS_SP == SaleTrackerService.STS_CONFIG_TYPE){
			showOpenFileTextView.setText ("Open Config File :  OK (SP VERSION)");
		}

		int data;

		boolean bSendToTme = false;

		if(Contant.STS_SP == SaleTrackerService.STS_CONFIG_TYPE){
			SaleTrackerConfigSP stciSP = new SaleTrackerConfigSP();
			stciSP.init(mContext);
			data = stciSP.read_secro();

			showOpenFileTextView.setText ("\n IMEI1 :  " + HideMethod.TelephonyManager.getDefault().getDeviceId(0,mContext)+"\n");
			bSendToTme = stciSP.isSendedToTmeWapAddr();
		}

		/*******************SHOW SEND TYPE *****************/
		String strTmp;
		int iSendTypeTmp;

		//if first send time is ok, set mSwitchWhole unchecked
		if((data & 0x000000ff) == 0x01){
			mSpinner.setEnabled(false);
			mSwitchWhole.setChecked(false);
		}

		if(mSwitchWhole.isChecked()){
//			iSendTypeTmp = pre.getInt(Contant.KEY_SELECT_SEND_TYPE, 1);
			strTmp = "SendType(from the Switch control) : ";
		}else {
//			iSendTypeTmp = DEFAULT_SEND_TYPE;
			strTmp = "SendType : ";
		}
		iSendTypeTmp = pre.getInt(Contant.KEY_SELECT_SEND_TYPE, DEFAULT_SEND_TYPE);
			
		if(iSendTypeTmp == 0){
			sendTypeTextView.setText(strTmp + " sms");
		}else if(iSendTypeTmp == 1)
		{
			sendTypeTextView.setText(strTmp + " net");
		}else if(iSendTypeTmp == 2)
		{
			sendTypeTextView.setText(strTmp + " net and sms");
		}else{
			sendTypeTextView.setText(strTmp + " unknow");
		}

		/*******************SHOW SEND RESULT*****************/
		if((data & 0x000000ff) == 0x01)
		{
			mStrSendResult = "  OK ;";
		}
		else 
		{
			mStrSendResult = "  No ;";
		}
		if(bSendToTme) {
			setResutTextView.setText("Send result1 : " + mStrSendResult + "    result2:  OK");
		}
		else{
			setResutTextView.setText("Send result1 : " + mStrSendResult + "    result2:  No");
		}
	}

	public BroadcastReceiver refreshReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			Log.d(TAG, CLASS_NAME+"refreshReceiver onReceive: ");
			getStatus();
		}
	};

	private void pickTimeConfigs(){
		Log.d(TAG, CLASS_NAME+"pickTimeConfigs: ");

		Map<String, String> configMap = SaleTrackerUti.readSendParamFromXml(getApplicationContext());
		if(configMap != null){
			DEFAULT_SEND_TYPE = Integer.parseInt(configMap.get(CONFIG_SEND_TYPE));
			DEFAULT_START_TIME = Integer.parseInt(configMap.get(CONFIG_START_TIME));
			DEFAULT_SPACE_TIME = Integer.parseInt(configMap.get(CONFIG_SPACE_TIME));

			Log.w(TAG, CLASS_NAME+" pickCountryConfigs: "
					+ "\n   DEFAULT_SEND_TYPE =" + DEFAULT_SEND_TYPE
					+ "\n   DEFAULT_START_TIME ="+DEFAULT_START_TIME
					+ "\n   DEFAULT_SPACE_TIME =" +DEFAULT_SPACE_TIME
			);
		}else{
			Log.d(TAG,CLASS_NAME+" pickTimeConfigs: config doesn't exist");
		}
	}
}

