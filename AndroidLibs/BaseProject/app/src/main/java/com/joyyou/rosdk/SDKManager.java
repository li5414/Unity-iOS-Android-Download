package com.joyyou.rosdk;

import com.joyyou.rosdk.define.ISDK;
import com.joyyou.rosdk.define.NotifyMethodDefine;
import com.unity3d.player.UnityPlayer;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.ClipData;
import android.content.ClipDescription;
import android.content.ClipboardManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;


public class SDKManager extends ISDK {

	private Context _context;
	//回调Unity的GameObject名
	private String notifyObjName = "";
	private boolean logEnable = true;
	public static String LogTag = "JoyYouSDK";
	private List<ISDK> list= new ArrayList<ISDK>();
	public Activity CurrentActivity = null;
	private static ClipboardManager clipboard;

	private static SDKManager _instance;
	public boolean isDebug = false;
	private SDKManager() {

	}
	public static SDKManager GetInstance() {
		if(_instance == null)
		{
			_instance = new SDKManager();
		}
		return _instance;
	}
	
	public void Init(String objName, String data)
	{
		this.notifyObjName = objName;
		logEnable = true;
		clipboard = (ClipboardManager) CurrentActivity.getSystemService(MainActivity.CLIPBOARD_SERVICE);
		ULog("SDKManager notifyObjName: " + this.notifyObjName, true);
	}

	public void InitSdkList()
    {
        ULog("InitSdkList");
        String packageName = getAppPackageName(CurrentActivity);
        try{
			ULog("packageName " + packageName);
            ApplicationInfo appInfo = CurrentActivity.getPackageManager().getApplicationInfo(packageName, PackageManager.GET_META_DATA);
            String msg = appInfo.metaData.getString("sdkList");
			ULog("GetSDKList:" + msg);
            if(msg != null && msg.length() > 0) {
                for (String sdkName: msg.split(",")) {
                    try {
                        Class c = Class.forName("com.joyyou.rosdk." + sdkName);
						ULog(sdkName + "|" + c.toString());
                        ISDK isdk =(ISDK) c.newInstance();
						ULog("Instance:" + isdk.toString());
                        list.add(isdk);
                    }
                    catch (Exception e)
                    {
                        ULogError("SDKManager Init " + e.toString());
                    }
                }
            }
        }catch (Exception e)
        {
			ULogError(e.toString());
        }
    }

    public static String getAppPackageName(Context context){
        String packageName = context.getPackageName();
		SDKManager.GetInstance().ULog("bundleid:" + packageName);
        return packageName;
    }

	public void MessageNotify(String callBackMethod, String data)
	{
		if(!notifyObjName.equals("")) {
			UnityPlayer.UnitySendMessage(notifyObjName, callBackMethod, data);
		}
	}
	
	public void WriteLog(String msg) {
		if(logEnable)
		{
			ULog(msg);
		}
	}

	public void ULog(String data){
		ULog(data, false);
	}

	public void ULog(String data, boolean SendToUnity){
		Log.i(LogTag, data);
		if (SendToUnity) MessageNotify(NotifyMethodDefine.Log, data);
	}

	public void ULogWarning(String data){
		ULogWarning(data, false);
	}

	public void ULogWarning(String data, boolean SendToUnity){
		Log.w(LogTag, data);
		if (SendToUnity) MessageNotify(NotifyMethodDefine.LogWarning, data);
	}

	public void ULogError(String data){
		ULogError(data, false);
	}

	public void ULogError(String data, boolean SendToUnity){
		Log.e(LogTag, data);
		if (SendToUnity) MessageNotify(NotifyMethodDefine.LogError, data);
	}

	public Bundle JsonToBundle(JSONObject jsonObject) throws JSONException {
		Bundle bundle = new Bundle();
		Iterator iTer = jsonObject.keys();
		while(iTer.hasNext()){
			String key = (String) iTer.next();
			String value = jsonObject.getString(key);
			bundle.putString(key,value);
		}
		return bundle;
	}

	public void SetMsgToClipBoard(String msg) {
		if (clipboard != null){
			ClipData cd = ClipData.newPlainText("data", msg);
			clipboard.setPrimaryClip(cd);
		}
	}

	public String GetMsgByClipBoard(){
		if(clipboard != null && clipboard.hasPrimaryClip()
				&& clipboard.getPrimaryClipDescription().hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN)) {
			ClipData cd = clipboard.getPrimaryClip();
			ClipData.Item item = cd.getItemAt(0);
			return item.getText().toString();
		}
		return "";
	}

	public void Toast(String str) {
		if (CurrentActivity != null){
			Toast.makeText(CurrentActivity, str, Toast.LENGTH_LONG).show();
		} else {
			ULogError("CurrentActivity is null");
		}
	}

	@Override
	public void LoadLibrary() {
		for (ISDK item:list) {
			item.LoadLibrary();
		}
	}

	@Override
	public void OnCreate(Bundle savedInstanceState) {
		for (ISDK item:list) {
			item.OnCreate(savedInstanceState);
		}
	}

	@Override
	public void OnStart() {
		for (ISDK item:list) {
			item.OnStart();
		}
	}

	@Override
	public void OnRestart(){
		for (ISDK item:list) {
			item.OnRestart();
		}
	}

	@Override
	public void OnResume(){
		for (ISDK item:list) {
			item.OnResume();
		}
	}

	@Override
	public void OnPause(){
		for (ISDK item:list) {
			item.OnPause();
		}
	}

	@Override
	public void OnStop(){
		for (ISDK item:list) {
			item.OnStop();
		}
	}

	@Override
	public void OnDestroy(){
		for (ISDK item:list) {
			item.OnDestroy();
		}
	}

	@Override
	public void OnNewIntent(Intent intent){
		for (ISDK item:list) {
			item.OnNewIntent(intent);
		}
	}

	@Override
	public void OnActivityResult(int requestCode, int resultCode, Intent data){
		for (ISDK item:list) {
			item.OnActivityResult(requestCode, resultCode, data);
		}
	}

	@Override
	public void CallNativeVoidFunc(String eventName, String data){
		for (ISDK item:list) {
			item.CallNativeVoidFunc(eventName, data);
		}
	}

	@Override
	public String CallNativeReturnFunc(String eventName, String data){
		String arg=null;
		for (ISDK item:list) {
			arg=item.CallNativeReturnFunc(eventName, data);
			if (arg!=null)
			{
				return arg;
			}
		}
		return arg;
	}

	@Override
	public void CallNativeBytesFunc(String eventName, String args, byte[] data){
		for (ISDK item:list) {
			item.CallNativeBytesFunc(eventName, args, data);
		}
	}

	@Override
	public void OnRequestPermissionsResult(int arg0, String[] arg1, int[] arg2){
		for (ISDK item:list) {
			item.OnRequestPermissionsResult(arg0, arg1, arg2);
		}
	}
}
