package com.ape.saletracker;

//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

import android.content.Context;
import android.content.res.AssetManager;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;
import android.util.Xml;
import android.widget.Toast;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;

public class ApeConfigParser {
    private static final String TAG = "ApeConfigParser";
    private static final boolean DEBUG = false;
    private static final int MAX_INCLUDE_DEPTH = 10;
    private static final String CUSTOMER_COUNTRY_PROP = "ro.product.locale.region";
    private static final String CUSTOMER_OPERATOR_PROP = "ro.product.operator";
    private static final String PRODUCT_NAME_PROP = "ro.target";
    private static final String BRAND_NAME_PROP = "ro.product.brand";
    private static Class<?> mClassType = null;
    private static Method mGetMethod = null;
    private final Context mContext;
    private String FEATURE_FILE_PATH;
    private String FEATURE_FILE_PATH_DEBUG;
    private boolean isDebugMode = false;
    private boolean isConfigInApk = false;
    private boolean showDebugToast = true;
    private boolean isConfigFileExist = false;
    private boolean searchConfigfromTrunk = false;
    private boolean searchConfigfromOperator = false;
    private String module;
    private String configVersion;
    private String requirementsVersion;
    private File configFile = null;
    private InputStream fileStream;
    private XmlPullParser xmlParser;
    private int includeDepth;
    private String customerCountry;
    private String customerOperator;
    private String productName;
    private String brandName;
    private String configFilePathInApk;
    private static final String[] validType = new String[]{"bool", "integer", "string", "string-array", "integer-array"};
    private final HashMap<String, String> mValueMap = new HashMap();
    private final HashMap<String, String> mTypeMap = new HashMap();
    private static final String PLATEFORM_INFO_HARDWARE = "ro.hardware";
    private static final String MTK_HARDWARE = "mt";
    private static final String QUALCOMM_HARDWARE = "qcom";
    private static final String PLATEFORM_INFO_RIL = "gsm.version.ril-impl";
    private static final String MTK_RIL = "mtk";
    private static final String QUALCOMM_RIL = "qualcomm";
    private com.ape.saletracker.ApeConfigParser.PLATEFORM plateform = null;

    public ApeConfigParser(Context context, String path) {
        this.mContext = context;
        this.getCustomerInfo();
        this.getConfigFilePath(path);
        Log.i("ApeConfigParser", "ApeConfigParser init, config path: " + this.FEATURE_FILE_PATH);
        this.initFeatureConfigIfNull();
        if(this.xmlParser != null) {
            this.configParserAll();
        }

    }

    public ApeConfigParser(Context context, String path, boolean showtoast) {
        this.mContext = context;
        this.showDebugToast = showtoast;
        this.getCustomerInfo();
        this.getConfigFilePath(path);
        Log.i("ApeConfigParser", "ApeConfigParser init, config path: " + this.FEATURE_FILE_PATH);
        this.initFeatureConfigIfNull();
        if(this.xmlParser != null) {
            Log.i("ApeConfigParser", "ApeConfigParser configParserAll start...");
            this.configParserAll();
        }

    }

    private void getConfigFilePath(String path) {
        if(path != null) {
            if(path.startsWith("/")) {
                this.FEATURE_FILE_PATH = Environment.getRootDirectory() + path;
                this.FEATURE_FILE_PATH_DEBUG = Environment.getExternalStorageDirectory().getAbsolutePath() + path;
            } else {
                this.FEATURE_FILE_PATH = Environment.getRootDirectory() + "/" + path;
                this.FEATURE_FILE_PATH_DEBUG = Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + path;
            }

        }
    }

    private void initFeatureConfig() {
        if(this.xmlParser == null) {
            if(this.FEATURE_FILE_PATH_DEBUG != null && this.FEATURE_FILE_PATH != null) {
                this.configFile = new File(this.FEATURE_FILE_PATH_DEBUG);
                if(this.configFile.exists() && this.configFile.isFile()) {
                    this.isDebugMode = true;
                    this.isConfigInApk = false;
                    if(this.showDebugToast && this.mContext != null) {
                        Toast.makeText(this.mContext, "Current is debug mode...", 1).show();
                    }

                    Log.i("ApeConfigParser", "ApeConfigParser is debug mode.");
                } else {
                    this.isDebugMode = false;
                    this.isConfigInApk = this.getExactConfigFilePathInApk();
                    if(!this.isConfigInApk) {
                        this.configFile = new File(this.FEATURE_FILE_PATH);
                    }
                }
            } else {
                this.isConfigInApk = this.getConfigFilePathInApk(true);
            }

            if(this.isConfigInApk) {
                this.isConfigFileExist = true;
                this.xmlParser = Xml.newPullParser();
                Log.i("ApeConfigParser", "ApeConfigParser isConfigInApk, configFilePathInApk:" + this.configFilePathInApk);
            } else if(this.configFile != null && this.configFile.exists() && this.configFile.isFile()) {
                this.isConfigFileExist = true;
                this.xmlParser = Xml.newPullParser();
            } else if(this.getConfigFilePathInApk(true)) {
                this.isConfigInApk = true;
                this.isConfigFileExist = true;
                this.xmlParser = Xml.newPullParser();
                Log.i("ApeConfigParser", "ApeConfigParser isConfigInApk, configFilePathInApk:" + this.configFilePathInApk);
            } else {
                this.isConfigFileExist = false;
                Log.e("ApeConfigParser", "ApeConfigParser initFeatureConfig configFile not exist...");
            }
        }

    }

    private boolean isValidType(String type) {
        return type != null && Arrays.asList(validType).contains(type);
    }

    private void initFeatureConfigIfNull() {
        if(this.xmlParser == null) {
            this.initFeatureConfig();
        }

    }

    private boolean getConfigInApk() {
        if(this.mContext != null) {
            AssetManager am = this.mContext.getAssets();

            try {
                String[] e = am.list("config");
                return e.length != 0;
            } catch (IOException var3) {
                var3.printStackTrace();
                return false;
            }
        } else {
            return false;
        }
    }

    private boolean isAssetFileExist(String path) {
        if(this.mContext != null && path != null) {
            AssetManager am = this.mContext.getAssets();
            InputStream inputStream = null;

            try {
                inputStream = am.open(path);
                inputStream.close();
                return true;
            } catch (Exception var5) {
                return false;
            }
        } else {
            return false;
        }
    }

    private boolean isTrunkProject(String brandName) {
        return TextUtils.isEmpty(brandName) || brandName.equalsIgnoreCase("tinno") || brandName.equalsIgnoreCase("Android") || brandName.equalsIgnoreCase("alps");
    }

    private boolean getExactConfigFilePathInApk() {
        if(this.mContext == null) {
            return false;
        } else {
            String directoryName = this.brandName;
            if(this.isTrunkProject(directoryName)) {
                directoryName = "trunk";
            }

            if(!TextUtils.isEmpty(this.customerOperator)) {
                directoryName = this.customerOperator;
            }

            directoryName = directoryName.toLowerCase();
            if(directoryName.equalsIgnoreCase("trunk")) {
                if(!TextUtils.isEmpty(this.productName)) {
                    this.configFilePathInApk = "config/trunk/config_" + this.productName.toLowerCase() + ".xml";
                }
            } else if(!TextUtils.isEmpty(this.customerCountry) && !TextUtils.isEmpty(this.productName)) {
                this.configFilePathInApk = "config/" + directoryName + "/config_" + this.customerCountry.toLowerCase() + "_" + this.productName.toLowerCase() + ".xml";
                if(!this.isAssetFileExist(this.configFilePathInApk)) {
                    this.configFilePathInApk = "config/" + directoryName + "/config_" + this.productName.toLowerCase() + ".xml";
                }
            } else if(!TextUtils.isEmpty(this.productName)) {
                this.configFilePathInApk = "config/" + directoryName + "/config_" + this.productName.toLowerCase() + ".xml";
            }

            return this.isAssetFileExist(this.configFilePathInApk);
        }
    }

    private boolean getConfigFilePathInApk(boolean operatorSearch) {
        if(this.mContext == null) {
            return false;
        } else {
            this.searchConfigfromTrunk = false;
            this.searchConfigfromOperator = false;
            String directoryName = this.brandName;
            if(this.isTrunkProject(directoryName)) {
                directoryName = "trunk";
            }

            if(operatorSearch && !TextUtils.isEmpty(this.customerOperator)) {
                directoryName = this.customerOperator;
                this.searchConfigfromOperator = true;
            }

            directoryName = directoryName.toLowerCase();
            if(directoryName.equalsIgnoreCase("trunk")) {
                this.searchConfigfromTrunk = true;
                if(!TextUtils.isEmpty(this.productName)) {
                    this.configFilePathInApk = "config/trunk/config_" + this.productName.toLowerCase() + ".xml";
                    if(!this.isAssetFileExist(this.configFilePathInApk)) {
                        this.configFilePathInApk = "config/trunk/config.xml";
                    }
                } else {
                    this.configFilePathInApk = "config/trunk/config.xml";
                }
            } else if(!TextUtils.isEmpty(this.customerCountry) && !TextUtils.isEmpty(this.productName)) {
                this.configFilePathInApk = "config/" + directoryName + "/config_" + this.customerCountry.toLowerCase() + "_" + this.productName.toLowerCase() + ".xml";
                if(!this.isAssetFileExist(this.configFilePathInApk)) {
                    this.configFilePathInApk = "config/" + directoryName + "/config_" + this.productName.toLowerCase() + ".xml";
                }

                if(!this.isAssetFileExist(this.configFilePathInApk)) {
                    this.configFilePathInApk = "config/" + directoryName + "/config_" + this.customerCountry.toLowerCase() + ".xml";
                }

                if(!this.isAssetFileExist(this.configFilePathInApk)) {
                    this.configFilePathInApk = "config/" + directoryName + "/config.xml";
                }
            } else if(!TextUtils.isEmpty(this.customerCountry)) {
                this.configFilePathInApk = "config/" + directoryName + "/config_" + this.customerCountry.toLowerCase() + ".xml";
                if(!this.isAssetFileExist(this.configFilePathInApk)) {
                    this.configFilePathInApk = "config/" + directoryName + "/config.xml";
                }
            } else if(!TextUtils.isEmpty(this.productName)) {
                this.configFilePathInApk = "config/" + directoryName + "/config_" + this.productName.toLowerCase() + ".xml";
                if(!this.isAssetFileExist(this.configFilePathInApk)) {
                    this.configFilePathInApk = "config/" + directoryName + "/config.xml";
                }
            } else {
                this.configFilePathInApk = "config/" + directoryName + "/config.xml";
            }

            if(this.isAssetFileExist(this.configFilePathInApk)) {
                return true;
            } else if(this.searchConfigfromTrunk) {
                return false;
            } else if(this.searchConfigfromOperator) {
                Log.i("ApeConfigParser", "ApeConfigParser getConfigFilePathInApk not found in operator search...");
                return this.getConfigFilePathInApk(false);
            } else {
                if(!TextUtils.isEmpty(this.productName)) {
                    this.configFilePathInApk = "config/trunk/config_" + this.productName.toLowerCase() + ".xml";
                    if(!this.isAssetFileExist(this.configFilePathInApk)) {
                        this.configFilePathInApk = "config/trunk/config.xml";
                    }
                } else {
                    this.configFilePathInApk = "config/trunk/config.xml";
                }

                return this.isAssetFileExist(this.configFilePathInApk);
            }
        }
    }

    private void configParserAll() {
        this.includeDepth = 0;

        try {
            if(this.isConfigInApk) {
                AssetManager e = this.mContext.getAssets();
                this.fileStream = e.open(this.configFilePathInApk);
            } else {
                this.fileStream = new FileInputStream(this.configFile);
            }

            this.xmlParser.setInput(this.fileStream, "UTF-8");
            int e1 = this.xmlParser.getEventType();

            while(e1 != 1) {
                switch(e1) {
                    case 2:
                        String mtype = this.xmlParser.getName();
                        String key_name;
                        if(mtype.equals("include")) {
                            key_name = this.xmlParser.getAttributeValue((String)null, "file");
                            this.subConfigParserAll(key_name);
                        } else if(this.isValidType(mtype)) {
                            key_name = this.xmlParser.getAttributeValue((String)null, "name");
                            String value = this.xmlParser.getAttributeValue((String)null, "value");
                            if(key_name != null) {
                                this.mValueMap.put(key_name, value);
                                this.mTypeMap.put(key_name, mtype);
                            }
                        }
                    case 0:
                    case 1:
                    case 3:
                    default:
                        e1 = this.xmlParser.next();
                }
            }

            if(this.fileStream != null) {
                this.fileStream.close();
            }
        } catch (FileNotFoundException var5) {
            Log.i("ApeConfigParser", "FileNotFoundException-file " + this.FEATURE_FILE_PATH);
            var5.printStackTrace();
        } catch (XmlPullParserException var6) {
            Log.i("ApeConfigParser", "Exception in config xml parser ");
            var6.printStackTrace();
        } catch (IOException var7) {
            Log.i("ApeConfigParser", "initFeatureConfig--IOException");
            var7.printStackTrace();
        }

    }

    private void subConfigParserAll(String filename) {
        if(filename != null) {
            File file = null;
            String filePath = null;
            String filePathInApk = null;
            int e;
            if(this.isConfigInApk) {
                if(filename.startsWith("/")) {
                    filePathInApk = "config" + filename;
                } else {
                    e = this.configFilePathInApk.lastIndexOf(47);
                    filePathInApk = this.configFilePathInApk.substring(0, e) + "/" + filename;
                }

                if(!this.isAssetFileExist(filePathInApk)) {
                    return;
                }

                Log.i("ApeConfigParser", "ApeConfigParser configParserAll sub-file-InApk:" + filePathInApk);
            } else {
                if(this.isDebugMode) {
                    e = this.FEATURE_FILE_PATH_DEBUG.lastIndexOf(47);
                    filePath = this.FEATURE_FILE_PATH_DEBUG.substring(0, e) + "/" + filename;
                } else {
                    e = this.FEATURE_FILE_PATH.lastIndexOf(47);
                    filePath = this.FEATURE_FILE_PATH.substring(0, e) + "/" + filename;
                }

                file = new File(filePath);
                if(!file.exists() || !file.isFile()) {
                    return;
                }

                Log.i("ApeConfigParser", "ApeConfigParser configParserAll sub-file:" + filePath);
            }

            ++this.includeDepth;
            if(this.includeDepth > 10) {
                throw new IllegalStateException("The include depth is more than MAX_INCLUDE_DEPTH: 10");
            } else {
                try {
                    Object var14;
                    if(this.isConfigInApk) {
                        AssetManager subXmlParser = this.mContext.getAssets();
                        var14 = subXmlParser.open(filePathInApk);
                    } else {
                        var14 = new FileInputStream(file);
                    }

                    XmlPullParser var15 = Xml.newPullParser();
                    var15.setInput((InputStream)var14, "UTF-8");
                    int eventType = var15.getEventType();

                    while(eventType != 1) {
                        switch(eventType) {
                            case 2:
                                String mtype = var15.getName();
                                String key_name;
                                if(mtype.equals("include")) {
                                    key_name = var15.getAttributeValue((String)null, "file");
                                    this.subConfigParserAll(key_name);
                                } else if(this.isValidType(mtype)) {
                                    key_name = var15.getAttributeValue((String)null, "name");
                                    String value = var15.getAttributeValue((String)null, "value");
                                    if(key_name != null) {
                                        this.mValueMap.put(key_name, value);
                                        this.mTypeMap.put(key_name, mtype);
                                    }
                                }
                            case 0:
                            case 1:
                            case 3:
                            default:
                                eventType = var15.next();
                        }
                    }

                    if(var14 != null) {
                        ((InputStream)var14).close();
                    }
                } catch (FileNotFoundException var11) {
                    Log.i("ApeConfigParser", "FileNotFoundException-file " + filePath);
                    var11.printStackTrace();
                } catch (XmlPullParserException var12) {
                    Log.i("ApeConfigParser", "Exception in config xml parser ");
                    var12.printStackTrace();
                } catch (IOException var13) {
                    Log.i("ApeConfigParser", "initFeatureConfig--IOException");
                    var13.printStackTrace();
                }

            }
        }
    }

    private void configInfoParser() {
        try {
            if(this.isConfigInApk) {
                AssetManager e = this.mContext.getAssets();
                this.fileStream = e.open(this.configFilePathInApk);
            } else {
                this.fileStream = new FileInputStream(this.configFile);
            }

            this.xmlParser.setInput(this.fileStream, "UTF-8");
            int e1 = this.xmlParser.getEventType();

            while(true) {
                if(e1 == 1) {
                    if(this.fileStream != null) {
                        this.fileStream.close();
                    }
                    break;
                }

                switch(e1) {
                    case 2:
                        String mtype = this.xmlParser.getName();
                        if(mtype != null && (mtype.equalsIgnoreCase("resources") || mtype.equalsIgnoreCase("configs"))) {
                            this.module = this.xmlParser.getAttributeValue((String)null, "module");
                            this.configVersion = this.xmlParser.getAttributeValue((String)null, "configVersion");
                            this.requirementsVersion = this.xmlParser.getAttributeValue((String)null, "requirementsVersion");
                            Log.i("ApeConfigParser", "ApeConfigParser configInfoParser, module: " + this.module + ",configVersion:" + this.configVersion + ",requirementsVersion:" + this.requirementsVersion);
                            if(this.fileStream != null) {
                                this.fileStream.close();
                            }

                            return;
                        }
                    case 0:
                    case 1:
                    case 3:
                    default:
                        e1 = this.xmlParser.next();
                }
            }
        } catch (FileNotFoundException var3) {
            Log.i("ApeConfigParser", "FileNotFoundException-file " + this.FEATURE_FILE_PATH);
            var3.printStackTrace();
        } catch (XmlPullParserException var4) {
            Log.i("ApeConfigParser", "Exception in config xml parser ");
            var4.printStackTrace();
        } catch (IOException var5) {
            Log.i("ApeConfigParser", "initFeatureConfig--IOException");
            var5.printStackTrace();
        }

    }

    public boolean getBoolean(String name) {
        String value = null;
        this.initFeatureConfigIfNull();
        if(this.xmlParser == null) {
            return false;
        } else {
            if(this.mValueMap.containsKey(name)) {
                value = (String)this.mValueMap.get(name);
            }

            return value == null?false:value.equalsIgnoreCase("yes") || value.equalsIgnoreCase("true");
        }
    }

    public boolean getBoolean(String name, boolean defvalue) {
        String value = null;
        this.initFeatureConfigIfNull();
        if(this.xmlParser == null) {
            return defvalue;
        } else {
            if(this.mValueMap.containsKey(name)) {
                value = (String)this.mValueMap.get(name);
            }

            return value == null?defvalue:value.equalsIgnoreCase("yes") || value.equalsIgnoreCase("true");
        }
    }

    public int getInteger(String name) {
        String value = null;
        this.initFeatureConfigIfNull();
        if(this.xmlParser == null) {
            return -1;
        } else {
            if(this.mValueMap.containsKey(name)) {
                value = (String)this.mValueMap.get(name);
            }

            return value == null?-1:Integer.valueOf(value).intValue();
        }
    }

    public int getInteger(String name, int defvalue) {
        String value = null;
        this.initFeatureConfigIfNull();
        if(this.xmlParser == null) {
            return defvalue;
        } else {
            if(this.mValueMap.containsKey(name)) {
                value = (String)this.mValueMap.get(name);
            }

            return value == null?defvalue:Integer.valueOf(value).intValue();
        }
    }

    public String getString(String name) {
        String value = null;
        this.initFeatureConfigIfNull();
        if(this.xmlParser == null) {
            return null;
        } else if(this.mValueMap.containsKey(name)) {
            value = (String)this.mValueMap.get(name);
            return value;
        } else {
            return null;
        }
    }

    public String getString(String name, String defvalue) {
        String value = null;
        this.initFeatureConfigIfNull();
        if(this.xmlParser == null) {
            return defvalue;
        } else if(this.mValueMap.containsKey(name)) {
            value = (String)this.mValueMap.get(name);
            Log.i("ApeConfigParser", "ApeConfigParser, getString-name: " + name + ",value:" + value);
            return value;
        } else {
            return defvalue;
        }
    }

    public int[] getIntArray(String name) {
        String[] strarray = null;
        this.initFeatureConfigIfNull();
        if(this.xmlParser == null) {
            return null;
        } else if(!this.mValueMap.containsKey(name)) {
            return null;
        } else {
            String value = (String)this.mValueMap.get(name);
            if(value == null) {
                return null;
            } else {
                if(value.contains(",")) {
                    strarray = value.split(",");
                } else if(value.contains(";")) {
                    strarray = value.split(";");
                } else if(value.contains("/")) {
                    strarray = value.split("/");
                } else {
                    strarray = new String[]{value};
                }

                if(strarray == null) {
                    return null;
                } else {
                    int len = strarray.length;
                    int[] result = new int[len];

                    for(int i = 0; i < len; ++i) {
                        result[i] = Integer.valueOf(strarray[i]).intValue();
                    }

                    return result;
                }
            }
        }
    }

    public String[] getStringArray(String name) {
        String[] strarray = null;
        this.initFeatureConfigIfNull();
        if(this.xmlParser == null) {
            return null;
        } else if(this.mValueMap.containsKey(name)) {
            String value = (String)this.mValueMap.get(name);
            if(value == null) {
                return null;
            } else {
                if(value.contains(",")) {
                    strarray = value.split(",");
                } else if(value.contains(";")) {
                    strarray = value.split(";");
                } else if(value.contains("/")) {
                    strarray = value.split("/");
                } else {
                    strarray = new String[]{value};
                }

                return strarray;
            }
        } else {
            return null;
        }
    }

    public HashMap<String, String> getValueMap() {
        this.initFeatureConfigIfNull();
        if(this.xmlParser == null) {
            Log.e("ApeConfigParser", "ApeConfigParser -getValueMap failed,xmlParser is null");
            return null;
        } else {
            if(this.mValueMap.isEmpty()) {
                this.configParserAll();
            }

            return this.mValueMap;
        }
    }

    public HashMap<String, String> getTypeMap() {
        this.initFeatureConfigIfNull();
        if(this.xmlParser == null) {
            Log.e("ApeConfigParser", "ApeConfigParser -getTypeMap failed,xmlParser is null");
            return null;
        } else {
            if(this.mTypeMap.isEmpty()) {
                this.configParserAll();
            }

            return this.mTypeMap;
        }
    }

    public boolean isDebugMode() {
        return this.isDebugMode;
    }

    public boolean isConfigFileExist() {
        return this.isConfigFileExist;
    }

    public boolean isHaveItem(String name) {
        Object strarray = null;
        this.initFeatureConfigIfNull();
        return this.xmlParser == null?false:this.mValueMap != null && this.mValueMap.containsKey(name);
    }

    public String getMoudleName() {
        this.initFeatureConfigIfNull();
        if(this.xmlParser == null) {
            Log.e("ApeConfigParser", "ApeConfigParser -getMoudleName failed,xmlParser is null");
            return null;
        } else {
            this.configInfoParser();
            return this.module;
        }
    }

    public String getConfigVersion() {
        this.initFeatureConfigIfNull();
        if(this.xmlParser == null) {
            Log.e("ApeConfigParser", "ApeConfigParser -getConfigVersion failed,xmlParser is null");
            return null;
        } else {
            this.configInfoParser();
            return this.configVersion;
        }
    }

    public String getRequirementsVersion() {
        this.initFeatureConfigIfNull();
        if(this.xmlParser == null) {
            Log.e("ApeConfigParser", "ApeConfigParser -getRequirementsVersion failed,xmlParser is null");
            return null;
        } else {
            this.configInfoParser();
            return this.requirementsVersion;
        }
    }

    public String getCustomerCountry() {
        return this.customerCountry;
    }

    public String getCustomerOperator() {
        return this.customerOperator;
    }

    public String getProductName() {
        return this.productName;
    }

    public String getCustomerBrand() {
        return this.brandName;
    }

    private void getCustomerInfo() {
        this.customerCountry = this.getProperty("ro.product.locale.region");
        this.customerOperator = this.getProperty("ro.tinno.operator");
        this.productName = this.getProperty("ro.target");
        this.brandName = this.getProperty("ro.product.brand");
        if(this.brandName != null && this.brandName.equalsIgnoreCase("wiko")) {
            String wikoUnify = this.getProperty("ro.wiko.type");
            if(wikoUnify != null && wikoUnify.equalsIgnoreCase("unify")) {
                // 为了兼容之前使用该字段的项目
                String country = this.getProperty("persist.ro.customer.country");
                if (country != null) {
                    this.customerCountry = country;
                }
                /*this.customerCountry = this.getProperty("persist.ro.customer.country");
                if(this.customerCountry == null || this.customerCountry.length() == 0) {
                    this.customerCountry = this.getProperty("persist.sys.area.current");
                }*/
            }
        }

        Log.i("ApeConfigParser", "ApeConfigParser -brandName:" + this.brandName + " ,Country:" + this.customerCountry + " ,Operator:" + this.customerOperator + " ,productName:" + this.productName);
    }

    private String getProperty(String key) {
        String value = null;

        try {
            if(mClassType == null) {
                mClassType = Class.forName("android.os.SystemProperties");
                mGetMethod = mClassType.getDeclaredMethod("get", new Class[]{String.class});
            }

            if(mGetMethod != null) {
                value = (String)mGetMethod.invoke(mClassType, new Object[]{key});
            }
        } catch (Exception var4) {
            var4.printStackTrace();
        }

        return value;
    }

    private com.ape.saletracker.ApeConfigParser.PLATEFORM getPlatform() {
        if(this.plateform == null) {
            String plateformInfoHardware = this.getProperty("ro.hardware");
            String plateformInfoRIL = this.getProperty("gsm.version.ril-impl");
            Log.i("ApeConfigParser", "getPlatform, plateformInfoHardware : " + plateformInfoHardware + " , plateformInfoRIL : " + plateformInfoRIL);
            if(plateformInfoRIL != null) {
                if(plateformInfoRIL.toLowerCase().contains("qualcomm")) {
                    this.plateform = com.ape.saletracker.ApeConfigParser.PLATEFORM.QUALCOMM;
                } else if(plateformInfoRIL.toLowerCase().contains("mtk")) {
                    this.plateform = com.ape.saletracker.ApeConfigParser.PLATEFORM.MTK;
                } else {
                    this.plateform = com.ape.saletracker.ApeConfigParser.PLATEFORM.OTHER;
                }
            }

            if((this.plateform == null || this.plateform == com.ape.saletracker.ApeConfigParser.PLATEFORM.OTHER) && plateformInfoHardware != null) {
                if(plateformInfoHardware.toLowerCase().contains("qcom")) {
                    this.plateform = com.ape.saletracker.ApeConfigParser.PLATEFORM.QUALCOMM;
                } else if(plateformInfoHardware.toLowerCase().contains("mt")) {
                    this.plateform = com.ape.saletracker.ApeConfigParser.PLATEFORM.MTK;
                } else {
                    this.plateform = com.ape.saletracker.ApeConfigParser.PLATEFORM.OTHER;
                }
            }

            Log.i("ApeConfigParser", "getPlatform, plateform : " + this.plateform);
        }

        return this.plateform;
    }

    public boolean isQualcommPlatform() {
        return com.ape.saletracker.ApeConfigParser.PLATEFORM.QUALCOMM == this.getPlatform();
    }

    public boolean isMtkPlatform() {
        return com.ape.saletracker.ApeConfigParser.PLATEFORM.MTK == this.getPlatform();
    }

    private static enum PLATEFORM {
        QUALCOMM,
        MTK,
        OTHER;

        private PLATEFORM() {
        }
    }
}

