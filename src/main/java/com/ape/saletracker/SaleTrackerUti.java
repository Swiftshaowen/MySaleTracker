package com.ape.saletracker;

import android.content.Context;
import android.os.Environment;
import android.os.SystemProperties;
import android.util.Log;
import android.util.Xml;

import org.xmlpull.v1.XmlPullParser;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by android on 6/30/16.
 */
public class SaleTrackerUti {

    private static final String TAG = "SaleTracker";
    private static final String CLASS_NAME = "SaleTrackerUti---->";

    private static Context mContext;
    public static Map<String, SaleTrackerConfigs> map = new HashMap<String, SaleTrackerConfigs>();

    public static void readSendParamFromXml(){

        // weijie.wang created.  5/13/16 start
		/*String strCountryName = SystemProperties.get("ro.project", "trunk");
		Log.w(TAG, CLASS_NAME+" readSendParamFromXml() strCountryName = "+strCountryName);
		String[] attArray = getResources().getStringArray(
				getResources().getIdentifier(strCountryName,"array",getPackageName()));

		if(attArray != null){
			mClientNo = attArray[0];
			mDefaultSendType = Integer.parseInt(attArray[1]);
			mIsNeedNoticePop = Boolean.parseBoolean(attArray[2]);
			mHosturl = attArray[3];
			mStartTimeFromXML = Integer.parseInt(attArray[4]);
			mSpaceTimeFromXML = Integer.parseInt(attArray[5]);
			mStrCountry = attArray[6];
			mStrModel = attArray[7];
			mDefaultSendTypeTmp = mDefaultSendType;

			Log.w(TAG, CLASS_NAME+" readSendParamFromXml()    "
					+ "\n   mClientNo =" + mClientNo
					+ "\n   mDefaultSendType =" + mDefaultSendType
					+ "\n   mIsNeedNoticePop =" + mIsNeedNoticePop
					+ "\n   mHosturl ="	+ mHosturl
					+ "\n   mStartTimeFromXML ="+mStartTimeFromXML
					+ "\n   mSpaceTimeFromXML =" +mSpaceTimeFromXML
					+ "\n   mStrPhoneNo =" + mStrPhoneNo
					+ "\n   mStrModel =" + mStrModel
					+ "\n   mStrCountry =" +mStrCountry );
		}else{
			Log.w(TAG, CLASS_NAME+" readSendParamFromXml()  attArray is NULL ");
		}*/
        // weijie.wang created.  5/13/16 end

        Log.w(TAG, CLASS_NAME+" readSendParamFromXml() map = "+map);
        if(!map.isEmpty()){
            return;
        }

        String path = Environment.getExternalStorageDirectory().toString();
        String fileName = "/system/etc/ApeSaleTrackerConfig.xml";

        String projectName = SystemProperties.get("ro.project", "trunk");
        Log.w(TAG, CLASS_NAME+" readSendParamFromXml() projectName = "+projectName
                +"; config file = "+fileName);
        String countryName = null;
        try{
            File xmlFileExter = new File(fileName);

            Log.w(TAG, CLASS_NAME+" readSendParamFromXml() is external xml file exist = "+xmlFileExter.exists());

            // Read parameters from external xml
            InputStream inputStream = null;
            if(xmlFileExter.exists()){
                inputStream = new FileInputStream(xmlFileExter);
            }else{
                Log.w(TAG, CLASS_NAME+" readSendParamFromXml() getAssets()");
                inputStream = mContext.getResources().getAssets().open("ApeSaleTrackerConfig.xml");
                Log.w(TAG, CLASS_NAME+" readSendParamFromXml() getAssets() inputStream = "+inputStream);
            }

            Log.w(TAG, CLASS_NAME+" readSendParamFromXml() inputStream = "+inputStream);
            XmlPullParser parser = Xml.newPullParser();
            parser.setInput(inputStream, "UTF-8");
            int eventCode = parser.getEventType();// 事件类型
            SaleTrackerConfigs config = null;
            while (eventCode != XmlPullParser.END_DOCUMENT) {
                switch (eventCode) {
                    case XmlPullParser.START_DOCUMENT:// 开始文档事件
                        break;
                    case XmlPullParser.START_TAG:// 元素开始标志
                        if ("SaleTracker".equals(parser.getName())) {
                            config = new SaleTrackerConfigs();
                        } else if (config != null) {
                            if ("name".equals(parser.getName())) {
                                config._name = parser.nextText();
                                countryName = config._name;
                            } else if ("client_no".equals(parser.getName())) {
                                config._client_no = parser.nextText();
                            } else if ("send_type".equals(parser.getName())) {
                                config._send_type = parser.nextText();
                            } else if ("notice".equals(parser.getName())) {
                                config._notice = parser.nextText();
                            } else if ("host_url".equals(parser.getName())) {
                                config._host_url = parser.nextText();
                            } else if ("start_time".equals(parser.getName())) {
                                config._start_time = parser.nextText();
                            } else if ("space_time".equals(parser.getName())) {
                                config._space_time = parser.nextText();
                            } else if ("country_type".equals(parser.getName())) {
                                config._country_type = parser.nextText();
                            } else if ("model_type".equals(parser.getName())) {
                                config._model_type = parser.nextText();
                            } else if ("phone_no".equals(parser.getName())) {
                                config._phone_no = parser.nextText();
                            }
                        }
                        break;
                    case XmlPullParser.END_TAG://元素结束标志
                        if ("SaleTracker".equals(parser.getName()) && config != null) {
                            map.put(countryName, config);
                            config = null;
                        }
                        break;
                }
                eventCode = parser.next();
            }

            if(inputStream != null){
                inputStream.close();
            }
        }catch (FileNotFoundException e){
            Log.d(TAG,CLASS_NAME+" readSendParamFromXml(): FileNotFoundException "+fileName);
            e.printStackTrace();
        }catch (Exception e){
            e.printStackTrace();
        }
    }

}
