package com.joyyou.rosdk;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.content.res.Configuration;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import com.joyyou.rosdk.define.NotifyMethodDefine;
import com.joyyou.rosdk.define.PermissionReqCodeDefine;
import com.unity3d.player.UnityPlayer;
import com.unity3d.player.UnityPlayerActivity;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.io.FileOutputStream;
import android.os.Debug;
import android.os.Environment;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import android.os.Handler;
import android.os.StatFs;
import android.provider.MediaStore;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.content.FileProvider;
import android.telephony.TelephonyManager;
import android.text.format.Formatter;
import android.view.WindowManager;

import org.json.JSONException;
import org.json.JSONObject;

public class MainActivity extends UnityPlayerActivity {
    static {
        System.loadLibrary("rocommongamelibs");
    }

    protected Context mContext;
    protected AssetManager assetManager;
    private ByteArrayOutputStream outputStream;
    InputStream assetManagerInputStream = null;

    byte buf[] = new byte[1024 * 64];

    private ActivityManager mActivityManager = null;

    static boolean videoPlayed = false;

    private ScreenShotContentObserver screenShotContentObserver;
    private Handler handler = new Handler();
    private String appLinkDataStr = "";

    ConnectivityManager connectivityManager;
    ConnectivityManager.NetworkCallback networkCallback;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mContext = this;
        assetManager = getAssets();

        connectivityManager = getSystemService(ConnectivityManager.class);
        networkCallback = new ConnectivityManager.NetworkCallback(){
            @Override
            public void onLost(Network network) {
                SDKManager.GetInstance().ULog("The application no longer has a default network. The last default network was " + network);
                SDKManager.GetInstance().MessageNotify(NotifyMethodDefine.OnConfigurationChanged, "none");
            }
            @Override
            public void onCapabilitiesChanged(Network network, NetworkCapabilities networkCapabilities) {
                SDKManager.GetInstance().ULog("The default network changed capabilities: " + networkCapabilities);
                String ret = "none";
                if (networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)){
                    ret = "wifi";
                } else if(networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)){
                    ret = "mobile";
                } else {
                    ret = "none";
                }
                SDKManager.GetInstance().MessageNotify(NotifyMethodDefine.OnConfigurationChanged, ret);
            }
        };
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            connectivityManager.registerDefaultNetworkCallback(networkCallback);
        }

        this.mActivityManager = (ActivityManager)this.getSystemService(Context.ACTIVITY_SERVICE);
        SDKManager.GetInstance().CurrentActivity = this;
        SDKManager.GetInstance().InitSdkList();
        SDKManager.GetInstance().LoadLibrary();
        SDKManager.GetInstance().OnCreate(savedInstanceState);

        if (Build.VERSION.SDK_INT >= 28) {
            WindowManager.LayoutParams lp = getWindow().getAttributes();
            lp.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
            getWindow().setAttributes(lp);
        }

        screenShotContentObserver = new ScreenShotContentObserver(handler, this) {
            @Override
            protected void onScreenShot(String path, String fileName) {
                SDKManager.GetInstance().ULog("onScreenShot path: " + path);
                SDKManager.GetInstance().ULog("onScreenShot fileName: " + fileName);
                SDKManager.GetInstance().MessageNotify(NotifyMethodDefine.OnScreenShot,"");
            }
        };

        handleIntent(getIntent());
    }

    @Override
    protected void onStart() {
        super.onStart();
        if(!videoPlayed){
            StartMoviePlayer("launch", false);
            videoPlayed = true;
        }
        //修复 sdk后台切换后黑屏的问题
        //当前Activty不是MainActivity时手动调用MainActivity的onResume
        try{
            //获取最前面的Activity
            ComponentName componentName=((ActivityManager)getSystemService(Context.ACTIVITY_SERVICE)).
                    getRunningTasks(1).get(0).topActivity;
            if(!componentName.getClassName().equals(getClass().getName()))
            {
                mUnityPlayer.resume();
                onWindowFocusChanged(true);
            }
        }
        catch (Exception e)
        {
            SDKManager.GetInstance().ULog(e.toString());
        }

        SDKManager.GetInstance().OnStart();
    }

    // 游戏需要集成此方法并调用WGPlatform.onRestart()
    @Override
    protected void onRestart() {
        super.onRestart();
        SDKManager.GetInstance().OnRestart();
    }

    // 游戏需要集成此方法并调用WGPlatform.onResume()
    @Override
    protected void onResume() {
        super.onResume();
        SDKManager.GetInstance().OnResume();
        getContentResolver().registerContentObserver(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                true,
                screenShotContentObserver
        );
    }

    // 游戏需要集成此方法并调用WGPlatform.onPause()
    @Override
    protected void onPause() {
        super.onPause();
        SDKManager.GetInstance().OnPause();
    }

    // 游戏需要集成此方法并调用WGPlatform.onStop()
    @Override
    protected void onStop() {
        super.onStop();
        SDKManager.GetInstance().OnStop();
    }

    // 游戏需要集成此方法并调用WGPlatform.requestPermissionsonDestory()
    @Override
    protected void onDestroy() {
        super.onDestroy();
        SDKManager.GetInstance().OnDestroy();
        try {
            getContentResolver().unregisterContentObserver(screenShotContentObserver);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                connectivityManager.unregisterNetworkCallback(networkCallback);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 在onActivityResult中需要调用WGPlatform.onActivityResult
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        SDKManager.GetInstance().OnActivityResult(requestCode, resultCode, data);
    }

    // 在onNewIntent中需要调用handleCallback将平台带来的数据交给MSDK处理
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        SDKManager.GetInstance().OnNewIntent(intent);
        handleIntent(intent);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        SDKManager.GetInstance().MessageNotify(NotifyMethodDefine.OnConfigurationChanged, "");
    }

    public void QuitApplication(){
        SDKManager.GetInstance().ULog("QuitApplication");
        android.os.Process.killProcess(android.os.Process.myPid());    //获取PID
        System.exit(0); //常规    java、c#的标准退出法，返回值为0代表正常退出
    }

    public String GetPkgPath(){
        Context ctx = getApplicationContext();
        String path = ctx.getObbDir().getAbsolutePath();
        return path;
    }
    public void JoyYouSDKInit(String objName, String data) {
        SDKManager.GetInstance().Init(objName, data);
    }

    public void CallNativeBytesFunc(String eventName, String data, byte[] bytes) {
        int length = 0;
        if (bytes != null){
            length = bytes.length;
        }
        SDKManager.GetInstance().ULog("CallNativeBytesFunc eventName: " + eventName + " data: " + data + " length: " + length);
        SDKManager.GetInstance().CallNativeBytesFunc(eventName, data, bytes);
    }

    public void CallNativeVoidFunc(String eventName, String data) {
        SDKManager.GetInstance().ULog("CallNativeVoidFunc eventName: " + eventName + " data: " + data);
        SDKManager.GetInstance().CallNativeVoidFunc(eventName, data);
    }

    public String CallNativeReturnFunc(String eventName, String data) {
        SDKManager.GetInstance().ULog("CallNativeReturnFunc eventName: " + eventName + " data: " + data);
        return SDKManager.GetInstance().CallNativeReturnFunc(eventName, data);
    }

    //检测是否使用了http代理
    private Boolean IsUesingHttpProxy(){
        String host = System.getProperty("http.proxyHost");
        String port= System.getProperty("http.proxyPort");
        if (null != host && null != port) {
            SDKManager.GetInstance().WriteLog("use httpProxy : true");
            return true;
        }
        else{
            SDKManager.GetInstance().WriteLog("use httpProxy : false");
            return false;
        }
    }


    public void InsertPhots(byte[] bytes){
        Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        File appDir = new File(getExternalFilesDir(null).getPath());
        if (!appDir.exists()) {
            appDir.mkdirs();
        }
        String fileName = System.currentTimeMillis() + ".png";
        final File file = new File(appDir, fileName);
        try {
            FileOutputStream fos = new FileOutputStream(file);
            boolean ret = bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
            if(ret){
                fos.flush();
                fos.close();
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        // 其次把文件插入到系统图库
        try {
            MediaStore.Images.Media.insertImage(getApplication().getContentResolver(), file.getAbsolutePath(), fileName, null);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        // 删掉临时存储的文件
        file.delete();
        // 最后通知图库更新
        getApplication().sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(file)));
    }

    public long GetStorage(String data) {
        try {
            File file = Environment.getDataDirectory();
            StatFs sf = new StatFs(file.getPath());
            return sf.getAvailableBytes();
        } catch (Throwable e) {
            SDKManager.GetInstance().ULogError(e.getLocalizedMessage());
        }
        return 1024;
    }

    public byte[] LoadByAndroid(String path, long offset, long length) {
        try {

            assetManagerInputStream = assetManager.open(path);
            if (assetManagerInputStream == null)
                SDKManager.GetInstance().ULogError("LoadError:" + path);
            return readTextBytes(assetManagerInputStream, offset, length);

        } catch (IOException e) {

            SDKManager.GetInstance().ULogError(e.toString());

        } finally {
            try {
                assetManagerInputStream.close();
            } catch (Exception e) {
                SDKManager.GetInstance().ULogError(e.toString());
            }
            System.gc();
        }
        return null;
    }

    private byte[] readTextBytes(InputStream inputStream, long offset, long length) {

        if (outputStream == null)
            outputStream = new ByteArrayOutputStream();

        //长度这里暂时先写成1024 * 64
        byte[] result = null;

        int len = 0;
        long needToRead = length;//已读长度

        try {
            long at = offset;
            //反复skip以防止失败
            while (at > 0) {
                long amt = inputStream.skip(at);
                if (amt < 0) {
                    SDKManager.GetInstance().ULogError("Android ReadBytes unexpected EOF");
                    throw new RuntimeException("Android ReadBytes unexpected EOF");
                }
                at -= amt;
            }

            //读取所需长度
            while (needToRead > 0) {
                len = inputStream.read(buf, 0, (int) Math.min(needToRead, buf.length));
                if (len <= 0) {
                    SDKManager.GetInstance().ULogError("Android ReadBytes unexpected EOF When Reading");
                    throw new RuntimeException("Android ReadBytes unexpected EOF When Reading");
                }

                outputStream.write(buf, 0, len);
                needToRead -= len;
            }

            result = outputStream.toByteArray();
            return result;

        } catch (Exception e) {
            SDKManager.GetInstance().ULogError(e.toString());
        } finally {
            outputStream.reset();
        }
        return null;
    }

    public boolean IsAssetExists(String pathInAssetsDir) {
        InputStream inputStream = null;
        try {
            inputStream = assetManager.open(pathInAssetsDir);
            if (null != inputStream) {
                return true;
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (inputStream != null) {
                    inputStream.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    // 返回手机运营商名称，在调用支付前调用作判断
    public String getProvidersName() {
        TelephonyManager tm = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        String operator = tm != null ? tm.getSimOperator() : null;
        if (operator == null) return null;
        switch (operator) {
            case "46000":
            case "46002":
            case "46007":
                return "中国移动";
            case "46001":
                return "中国联通";
            case "46003":
                return "中国电信";
            default:
                return "Other";
        }
    }

    public String GetNetworkType(){
        String ret = "none";
        Network currentNetwork = connectivityManager.getActiveNetwork();
        if (currentNetwork == null) return ret;
        NetworkCapabilities caps = connectivityManager.getNetworkCapabilities(currentNetwork);
        if (caps == null) return ret;
        if (caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)){
            ret = "wifi";
        } else if (caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)){
            ret = "mobile";
        } else {
            ret = "none";
        }
        SDKManager.GetInstance().ULog("GetDeviceNetworkType = " + ret);
        return ret;
    }

	public void ReStart(){

        Intent mStartActivity = new Intent(mContext, MainActivity.class);
        int mPendingIntentId = 123456;
        PendingIntent mPendingIntent = PendingIntent.getActivity(mContext, mPendingIntentId,    mStartActivity, PendingIntent.FLAG_CANCEL_CURRENT);
        AlarmManager mgr = (AlarmManager)mContext.getSystemService(Context.ALARM_SERVICE);
        mgr.set(AlarmManager.RTC, System.currentTimeMillis() + 100, mPendingIntent);
        System.exit(0);
    }

    public String getMemoryInfo() {
        String message = this.getTotalMemory();
        message = message + "-" + this.getAvailMemory();
        message = message + "-" + this.getCurrentProcessMemory();
        return message;
    }

    @SuppressLint({"NewApi"})
    private String getCurrentProcessMemory() {
        String pkgName = this.getPackageName();
        List<ActivityManager.RunningAppProcessInfo> appList = this.mActivityManager.getRunningAppProcesses();
        Iterator var4 = appList.iterator();

        while(var4.hasNext()) {
            ActivityManager.RunningAppProcessInfo appInfo = (ActivityManager.RunningAppProcessInfo)var4.next();
            if (appInfo.processName.equals(pkgName)) {
                int[] pidArray = new int[]{appInfo.pid};
                Debug.MemoryInfo[] memoryInfo = this.mActivityManager.getProcessMemoryInfo(pidArray);
                float temp = (float)memoryInfo[0].getTotalPrivateDirty() / 1024.0F;
                return String.format("%.2f", temp) + "MB";
            }
        }

        return "获取失败";
    }

    private String getTotalMemory() {
        String str1 = "/proc/meminfo";

        try {
            FileReader localFileReader = new FileReader(str1);
            BufferedReader localBufferedReader = new BufferedReader(localFileReader, 8192);
            String str2 = localBufferedReader.readLine();
            String[] arrayOfString = str2.split("\\s+");
            localBufferedReader.close();
            float temp = (float)Integer.valueOf(arrayOfString[1]) / 1048576.0F;
            return String.format("%.2f", temp) + "GB";
        } catch (IOException var7) {
            return "获取失败";
        }
    }

    private String getAvailMemory() {
        android.app.ActivityManager.MemoryInfo mi = new android.app.ActivityManager.MemoryInfo();
        mActivityManager.getMemoryInfo(mi);
        return Formatter.formatFileSize(this, mi.availMem);
    }

    private String getMemoryLimit() {
        Runtime rt = Runtime.getRuntime();
        long maxMemory = rt.maxMemory();
        return Formatter.formatFileSize(this, maxMemory);
    }

    public void dumpLogcat(){
        String fileName = "logcat_"+System.currentTimeMillis()+".txt";
        File outputFile = new File(getExternalCacheDir(),fileName);
        try {
            Process process = Runtime.getRuntime().exec("logcat -df "+outputFile.getAbsolutePath());
        } catch (IOException e) {
            SDKManager.GetInstance().ULogError(e.toString());
        }
    }

    ArrayList<byte[]> JavaMemoryHeap = new ArrayList<>();
    public void AllocJavaHeap(int size){
        try{
            JavaMemoryHeap.add(new byte[1024 * 1024 * size]);
        }catch (Exception e){
            SDKManager.GetInstance().ULogError(e.toString());
        }
    }

    /**
     * 播放动画
     */
    public void StartMoviePlayer(String name, boolean canSkip) {
        String packageName = getApplicationContext().getPackageName();
        SDKManager.GetInstance().ULog("StartMoviePlayer packageName: " + packageName);
        // 国内需要播动画
        if(packageName.equals("com.joyyou.ro") || packageName.equals("com.tencent.ro")){

        }
        else
        {
            Intent intent = new Intent(this, VideoPlayerActivity.class);
            intent.putExtra("movieName", name);
            intent.putExtra("canSkip", canSkip);
            intent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
            this.startActivity(intent);
        }
    }

    public void NativeToast(String str) {
        SDKManager.GetInstance().ULog("NativeToast: " + str);
        SDKManager.GetInstance().Toast(str);
    }

    public void SetMsgToClipBoard(String str) {
        SDKManager.GetInstance().ULog("SetMsgToClipBoard: " + str);
        SDKManager.GetInstance().SetMsgToClipBoard(str);
    }

    public String GetMsgByClipBoard(String data) {
        String str = SDKManager.GetInstance().GetMsgByClipBoard();
        SDKManager.GetInstance().ULog("GetMsgByClipBoard: " + str);
        return str;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults)
    {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        SDKManager.GetInstance().OnRequestPermissionsResult(requestCode, permissions, grantResults);

        SDKManager.GetInstance().ULog("onRequestPermissionsResult requestCode = " + requestCode);
        String result = "None";
        for(int i = 0; i < permissions.length && i < grantResults.length; i++){
            if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                result = "Authorized";
            } else {
                if(!this.shouldShowRequestPermissionRationale(permissions[i])){
                    result = "DeniedAndNoAsk";  // 用户拒绝并且不在询问
                } else {
                    result = "Denied"; // 用户拒绝
                }
            }
        }
        String permission = "";
        if (PermissionReqCodeDefine.Code2String.containsKey(requestCode)){
            permission = PermissionReqCodeDefine.Code2String.get(requestCode);
        }
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("result", result);
            jsonObject.put("permission", permission);
            SDKManager.GetInstance().ULog("onRequestPermissionsResult jsonObject = " + jsonObject.toString());
            SDKManager.GetInstance().MessageNotify(NotifyMethodDefine.RequestPermissionCallBack, jsonObject.toString());
        } catch (JSONException e){
            e.printStackTrace();
        }
    }

    public boolean CheckPermissions(String[] permissions) {
        String pkgName = SDKManager.getAppPackageName(MainActivity.this);
        boolean permissionGranted = true;
        for (String permission : permissions) {
            boolean tmp = PackageManager.PERMISSION_DENIED != getPackageManager().checkPermission(permission, pkgName);
            permissionGranted = permissionGranted && tmp;
            SDKManager.GetInstance().ULog("CheckPermission: " + permission + " ret: " + tmp, true);
            if (!permissionGranted) {
                break;
            }
        }
        return permissionGranted;
    }

    public void RequestPermissions(String[] permissions, int requestCode) {
        ActivityCompat.requestPermissions(MainActivity.this, permissions, requestCode);
    }

    public void CheckPermissionByType(String type) {
        SDKManager.GetInstance().ULog("CheckPermissionByType type = " + type);
        String result = "None";
        if(type.equals(PermissionReqCodeDefine.Code2String.get(PermissionReqCodeDefine.LOCATION))){
            result = CheckPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}) ? "Authorized" : "Denied";
        } else if(type.equals(PermissionReqCodeDefine.Code2String.get(PermissionReqCodeDefine.NOTIFICATION))){
            NotificationManagerCompat notification = NotificationManagerCompat.from(this);
            result = notification.areNotificationsEnabled() ? "Authorized" : "Denied";
        } else if(type.equals(PermissionReqCodeDefine.Code2String.get(PermissionReqCodeDefine.CAMERA))){
            result = (CheckPermissions(new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE})) ? "Authorized" : "Denied";
        } else if(type.equals(PermissionReqCodeDefine.Code2String.get(PermissionReqCodeDefine.PHOTOS))){
            result = CheckPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE}) ? "Authorized" : "Denied";
        } else if(type.equals(PermissionReqCodeDefine.Code2String.get(PermissionReqCodeDefine.MICROPHONE))){
            result = CheckPermissions(new String[]{Manifest.permission.RECORD_AUDIO}) ? "Authorized" : "Denied";
        } else if(type.equals(PermissionReqCodeDefine.Code2String.get(PermissionReqCodeDefine.NETWORK))){
            result = CheckPermissions(new String[]{Manifest.permission.ACCESS_NETWORK_STATE}) ? "Authorized" : "Denied";
        } else if(type.equals(PermissionReqCodeDefine.Code2String.get(PermissionReqCodeDefine.PHONESTATE))){
            result = CheckPermissions(new String[]{Manifest.permission.READ_PHONE_STATE}) ? "Authorized" : "Denied";
        }

        // 这里没有使用函数直接返回值的原因是：iOS获取权限存在异步的情况，需要统一接口
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("result", result);
            jsonObject.put("permission", type);
            SDKManager.GetInstance().ULog("CheckPermissionByType jsonObject = " + jsonObject.toString(), true);
            SDKManager.GetInstance().MessageNotify(NotifyMethodDefine.CheckPermissionCallBack, jsonObject.toString());
        } catch (JSONException e){
            e.printStackTrace();
        }
    }

    public void RequestPermissionByType(String type) {
        SDKManager.GetInstance().ULog("RequestPermissionByType type = " + type);
        if(type.equals(PermissionReqCodeDefine.Code2String.get(PermissionReqCodeDefine.LOCATION))){
            RequestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, PermissionReqCodeDefine.LOCATION);
        } else if(type.equals(PermissionReqCodeDefine.Code2String.get(PermissionReqCodeDefine.CAMERA))){
            RequestPermissions(new String[]{Manifest.permission.CAMERA}, PermissionReqCodeDefine.CAMERA);
        } else if(type.equals(PermissionReqCodeDefine.Code2String.get(PermissionReqCodeDefine.PHOTOS))){
            RequestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE}, PermissionReqCodeDefine.PHOTOS);
        } else if(type.equals(PermissionReqCodeDefine.Code2String.get(PermissionReqCodeDefine.MICROPHONE))){
            RequestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, PermissionReqCodeDefine.MICROPHONE);
        } else if(type.equals(PermissionReqCodeDefine.Code2String.get(PermissionReqCodeDefine.NETWORK))){
            RequestPermissions(new String[]{Manifest.permission.ACCESS_NETWORK_STATE}, PermissionReqCodeDefine.NETWORK);
        } else if(type.equals(PermissionReqCodeDefine.Code2String.get(PermissionReqCodeDefine.PHONESTATE))){
            RequestPermissions(new String[]{Manifest.permission.READ_PHONE_STATE}, PermissionReqCodeDefine.PHONESTATE);
        }
    }

    public void OpenSettingPermission(){
        SDKManager.GetInstance().ULog("OpenSettingPermission");
        Intent intent = new Intent();
        try {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.setAction("android.settings.APPLICATION_DETAILS_SETTINGS");
            intent.setData(Uri.fromParts("package", getApplicationContext().getPackageName(), null));
            getApplicationContext().startActivity(intent);
        } catch (Exception e) {
            intent = new Intent(Settings.ACTION_SETTINGS);
            getApplicationContext().startActivity(intent);
        }
    }

    //判断文件是否存在
    public boolean fileIsExists(String strFile){
        try{
            File file=new File(strFile);
            if(!file.exists())  {
                return false;
            }
        }catch(Exception e){
            return false;
        }
        return true;
    }

    public int InstallAPK(String filePath) {
        //use the applicationContext(context.startActivity)
        SDKManager.GetInstance().ULog("InstallAPK",true );
        Context context = getApplicationContext();

        if( filePath == null || filePath.length() == 0)
        {
            SDKManager.GetInstance().ULogError("InstallAPK filePath 为null",true );
            return -1;
        }

        if(!fileIsExists(filePath)){
            SDKManager.GetInstance().ULogError("InstallAPK filePath not exist",true );
            return -1;
        }

        File file = new File(filePath);
        // if apk in phone, chmod 777
        if (!filePath.startsWith(context.getObbDir().getPath()))
        {
            String root = context.getFilesDir().getPath();
            String filename = file.getAbsolutePath();

            while (filename != null && !root.equals(filename)) {
                try {
                    String cmd = "chmod 777 " + filename;
                    Runtime.getRuntime().exec(cmd);
                    filename = new File(filename).getParent();
                } catch (IOException e) {
                    e.printStackTrace();
                    SDKManager.GetInstance().ULogError("InstallAPK chmod error",true );
                    return -2;
                }
            }
        }

        //targetSdkVersion>=24
        Intent intent = new Intent();
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.setAction(android.content.Intent.ACTION_VIEW);

        //targetSdkVersion
        int targetVersion = context.getApplicationInfo().targetSdkVersion;
        SDKManager.GetInstance().ULog("targetVersion=" + targetVersion);

        if ((targetVersion >= 24) && (Build.VERSION.SDK_INT >=24)) {  //android7.0:api=24
            String provideName=context.getPackageName()+".ApolloFileprovider";
            SDKManager.GetInstance().ULog(String.format("provideName %s", provideName),true );
            Uri contentUri = FileProvider.getUriForFile(context, provideName, file);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            context.grantUriPermission(context.getPackageName(), contentUri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent.setDataAndType(contentUri, "application/vnd.android.package-archive");
        } else {
            intent.setDataAndType(Uri.fromFile(file), "application/vnd.android.package-archive");
        }

        try {
            //deep hole:when use activity, some 7.0 phone the end of install process has problem
            context.startActivity(intent); //do not use activity.startActivity
        } catch (ActivityNotFoundException e) {
            e.printStackTrace();
            SDKManager.GetInstance().ULogError("InstallAPK ActivityNotFound error",true );
            return -3;
        }
        return 0;
    }

    private void handleIntent(Intent intent) {
        Uri appLinkData = intent.getData();
        if (appLinkData != null){
            appLinkDataStr = appLinkData.toString();
            SDKManager.GetInstance().ULog("handleIntent appLinkData: " + appLinkDataStr);
            GetAppLinkData("");
        }
    }

    public void GetAppLinkData(String data){
        SDKManager.GetInstance().ULog("GetAppLinkData: " + data);
        if(!appLinkDataStr.equals("")){
            SDKManager.GetInstance().MessageNotify(NotifyMethodDefine.HandleIntentAppLinkDataCallBack, appLinkDataStr);
        }
    }

    public void ResetAcvicity(){
        Intent intent = getPackageManager().getLaunchIntentForPackage(getPackageName());
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        Runtime.getRuntime().exit(0);
    }
}
