package com.joyyou.rosdk;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

import com.joyyou.rosdk.define.ISDK;
import com.joyyou.rosdk.define.NotifyMethodDefine;
import com.joyyou.rosdk.define.SDKEventDefine;
import com.joyyou.sdk.JoyyouProxy;
import com.joyyou.sdkbase.define.JoyyouListener;
import com.tencent.bugly.crashreport.CrashReport;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class BuglySDK extends ISDK {
    private Map<String, String> appIdMap = new HashMap<String, String>(){
        {
            put("com.joyyou.ro", "032ea0d779");
            put("com.tencent.ro", "032ea0d779");
            put("com.gravity.ragnarokorigin.aos", "78f51409b3");
            put("com.gravity.ragnarokorigin.one", "78f51409b3");
            put("jp.gungho.rao", "5929bca15c");
            put("com.gravity.roona.aos", "6e4cae1e12");
            put("com.gravity.roo.tw", "c15f87e8a7");
            put("com.gravity.roo.tw.mycard", "c15f87e8a7");
        }
    };
    @Override
    public void OnCreate(Bundle savedInstanceState) {
        Context context = SDKManager.GetInstance().CurrentActivity.getApplicationContext();

        // 获取当前包名
        String packageName = context.getPackageName();
        // 获取当前进程名
        String processName = getProcessName(android.os.Process.myPid());
        // 设置是否为上报进程
        CrashReport.UserStrategy strategy = new CrashReport.UserStrategy(context);
        strategy.setUploadProcess(processName == null || processName.equals(packageName));
        String appId = "";
        if(appIdMap.containsKey(packageName)){
            appId = appIdMap.get(packageName);
        } else {
            appId = "032ea0d779";
            SDKManager.GetInstance().ULogError("BuglySDK init error, appId is null");
        }
        CrashReport.initCrashReport(context, appId, false, strategy);
        SDKManager.GetInstance().ULog("BuglySDK init: " + packageName + " appId: " + appId);
    }

    /**
     * 获取进程号对应的进程名
     *
     * @param pid 进程号
     * @return 进程名
     */
    private static String getProcessName(int pid) {
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader("/proc/" + pid + "/cmdline"));
            String processName = reader.readLine();
            if (!TextUtils.isEmpty(processName)) {
                processName = processName.trim();
            }
            return processName;
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        } finally {
            try {
                if (reader != null) {
                    reader.close();
                }
            } catch (IOException exception) {
                exception.printStackTrace();
            }
        }
        return null;
    }

    @Override
    public void CallNativeVoidFunc(String eventName, String data) {
        SDKManager.GetInstance().ULog("BuglySDK eventName: " + eventName + " data: " + data);
        if(eventName.equals(SDKEventDefine.SDK_EVENT_BUGLY_SET_CRASH_FILTER)) {
            BuglySetCrashFilter(data);
        } else if(eventName.equals(SDKEventDefine.SDK_EVENT_BUGLY_SET_CRASH_REG_FILTER)) {
            BuglySetCrashRegularFilter(data);
        }
    }

    public void BuglySetCrashFilter(String str) {
        SDKManager.GetInstance().ULog("BuglySetCrashFilter: " + str);
        com.tencent.bugly.crashreport.CrashReport.setCrashFilter(str);
    }

    public void BuglySetCrashRegularFilter(String str) {
        SDKManager.GetInstance().ULog("BuglySetCrashRegularFilter: " + str);
        com.tencent.bugly.crashreport.CrashReport.setCrashRegularFilter(str);
    }
}
