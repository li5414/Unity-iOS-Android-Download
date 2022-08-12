package com.joyyou.rosdk;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.InstanceIdResult;
import com.joyyou.rosdk.define.ISDK;
import com.joyyou.rosdk.define.NotifyMethodDefine;
import com.joyyou.rosdk.define.SDKEventDefine;
import com.joyyou.rosdk.utility.Utility;
import com.joyyou.sdk.JoyyouProxy;
import com.joyyou.rosdk.R;
import com.joyyou.sdkbase.JoyyouConf;
import com.joyyou.sdkbase.define.JoyyouAccountInfo;
import com.joyyou.sdkbase.define.JoyyouListener;
import com.joyyou.sdkbase.util.JoyLogger;

import org.json.JSONException;
import org.json.JSONObject;

import androidx.annotation.NonNull;

public class JoyyouSDK extends ISDK {

    private JSONObject messagingData;
    private String lastShareType = "";
    private final String oneStoreAppKey = "{\"oneStoreAppKey\":\"MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQDbvL9rBryy0OUrDNOW7BGDJwXrgpyInRnGnY+wj0cyvXgArqGd2RSl+T07oCvfR3uSzmYO0DMdkfvekhKZkcKaq0I0MYtahaW8W833Jhp5iJ4bvMQkmxUaDaE+lU1URoWDA79mknH6xL9/F+9EqYdsLLDKHrWlfb3B9dct0HhxOQIDAQAB\"}";

    @Override
    public void OnCreate(Bundle savedInstanceState) {
        try {
            ApplicationInfo appInfo = SDKManager.GetInstance().CurrentActivity.getPackageManager().getApplicationInfo(SDKManager.GetInstance().getAppPackageName(SDKManager.GetInstance().CurrentActivity), PackageManager.GET_META_DATA);
            String env = appInfo.metaData.getString("Joyyou.Env");
            SDKManager.GetInstance().ULog("JoyyouSDK env = " + env);
            SDKManager.GetInstance().isDebug = "ready".equals(env) || "kr-pre".equals(env) || "jp-pre".equals(env) || "hmt-pre".equals(env);
            JoyLogger.setIsDebug(SDKManager.GetInstance().isDebug); // ready开启日志调试模式
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        JoyyouProxy.getInstance().init(SDKManager.GetInstance().CurrentActivity, oneStoreAppKey);

        JoyyouProxy.getInstance().registerLoginCallback(new JoyyouListener.LoginListener() {
            @Override
            public void onComplete(JoyyouListener.ResultDefine resultDefine, String json) {
                try {
                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put("result", String.valueOf(resultDefine.value()));
                    if (resultDefine == JoyyouListener.ResultDefine.SUCCESS) {
                        JSONObject jsonData = new JSONObject(json);
                        String token = jsonData.getString("token");
                        String channel = jsonData.getString("channel");
                        jsonObject.put("token", token);
                        jsonObject.put("channel", channel);
                    } else {
                        jsonObject.put("msg", json);
                    }
                    SDKManager.GetInstance().MessageNotify(NotifyMethodDefine.LoginCallback, jsonObject.toString());
                } catch (JSONException e){
                    e.printStackTrace();
                }
            }
        });

        JoyyouProxy.getInstance().setSkusListListener(new JoyyouListener.SkusListListener() {
            @Override
            public void onComplete(JoyyouListener.ResultDefine resultDefine, String data) {
                SDKManager.GetInstance().ULog("resultDefine: " + resultDefine + " data: " + data);
                JSONObject jsonObject = new JSONObject();
                try {
                    jsonObject.put("result", String.valueOf(resultDefine.value()));
                    if (resultDefine == JoyyouListener.ResultDefine.SUCCESS) {
                        jsonObject.put("data", data);
                    } else{
                        jsonObject.put("msg", data);
                    }
                    SDKManager.GetInstance().MessageNotify(NotifyMethodDefine.SetSkusListCallback, jsonObject.toString());
                } catch (JSONException e){
                    e.printStackTrace();
                }
            }
        });

        try {
            messagingData = new JSONObject();
            if (SDKManager.GetInstance().CurrentActivity.getIntent().getExtras() != null) {
                for (String key : SDKManager.GetInstance().CurrentActivity.getIntent().getExtras().keySet()) {
                    Object value = SDKManager.GetInstance().CurrentActivity.getIntent().getExtras().get(key);
                    if (value != null && value.toString() != null){
                        SDKManager.GetInstance().ULog("Key: " + key + " Value: " + value.toString());
                        messagingData.put(key, value.toString());
                    }
                }
            }
        } catch (JSONException e){
            e.printStackTrace();
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            String channelId  = SDKManager.GetInstance().CurrentActivity.getString(Utility.getR(SDKManager.GetInstance().CurrentActivity, "string", "default_notification_channel_id"));
            String channelName = SDKManager.GetInstance().CurrentActivity.getString(Utility.getR(SDKManager.GetInstance().CurrentActivity, "string", "default_notification_channel_name"));
            SDKManager.GetInstance().ULog("channelId: " + channelId + " channelName: " + channelName);
            NotificationManager notificationManager = SDKManager.GetInstance().CurrentActivity.getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(new NotificationChannel(channelId,
                    channelName, NotificationManager.IMPORTANCE_LOW));
        }
    }

    @Override
    public void OnStart(){
        JoyyouProxy.getInstance().onRestart();
    }

    @Override
    public void OnNewIntent(Intent intent) {
        JoyyouProxy.getInstance().onNewIntent(intent);
        if (intent != null && intent.getExtras() != null) {
            try {
                messagingData = new JSONObject();
                for (String key : intent.getExtras().keySet()) {
                    Object value = intent.getExtras().get(key);
                    if (value != null && value.toString() != null){
                        SDKManager.GetInstance().ULog("Key: " + key + " Value: " + value.toString());
                        messagingData.put(key, value.toString());
                    }
                }
                SDKManager.GetInstance().MessageNotify(NotifyMethodDefine.OnPushNotificationData, messagingData.toString());
            } catch (JSONException e){
                e.printStackTrace();
            }
        }
    }

    @Override
    public void OnResume() {
        JoyyouProxy.getInstance().onResume();
    }

    @Override
    public void OnPause() {
        JoyyouProxy.getInstance().onPause();
    }

    @Override
    public void OnStop() {
        JoyyouProxy.getInstance().onStop();
    }

    @Override
    public void OnDestroy () {
        JoyyouProxy.getInstance().onDestroy();
    }

    @Override
    public void OnActivityResult(int requestCode, int resultCode, Intent data) {
        JoyyouProxy.getInstance().onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void CallNativeVoidFunc(String eventName, String data) {
        SDKManager.GetInstance().ULog("JoyyouSDK eventName: " + eventName + " data: " + data);
        if(eventName.equals(SDKEventDefine.SDK_EVENT_ANALYTICS_EVENT)) {
            try {
                SDKManager.GetInstance().ULog(data);
                JSONObject jsonObject = new JSONObject(data);
                AnalyticsEvent(jsonObject.getString("eventName"), jsonObject);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        } else if(eventName.equals(SDKEventDefine.SDK_EVENT_ANALYTICS_SET_USER_PROPERTY)){
            AnalyticsSetUserProperty(data);
        } else if(eventName.equals(SDKEventDefine.SDK_EVENT_TEST_CRASH)) {
            TestCrash();
        } else if(eventName.equals(SDKEventDefine.SDK_EVENT_SET_CRASH_INFO)) {
            SetCrashlyticsInfo(data);
        } else if(eventName.equals(SDKEventDefine.SDK_EVENT_GET_MESSAGING_SERVICE_TOKEN)) {
            GetMessagingServiceToken();
        } else if(eventName.equals(SDKEventDefine.SDK_EVENT_GET_PUSH_DATA)) {
            SDKManager.GetInstance().MessageNotify(NotifyMethodDefine.OnPushNotificationData, messagingData.toString());
        } else if(eventName.equals(SDKEventDefine.SDK_EVENT_LOGIN)) {
            Login(data);
        } else if(eventName.equals(SDKEventDefine.SDK_EVENT_AUTOLOGIN)) {
            AutoLogin();
        } else if(eventName.equals(SDKEventDefine.SDK_EVENT_LOGOUT)) {
            Logout();
        }  else if(eventName.equals(SDKEventDefine.SDK_EVENT_DELETE_ACCOUNT)) {
            DeleteAccount(data);
        } else if(eventName.equals(SDKEventDefine.SDK_EVENT_GET_USER_INFO)) {
            GetUserInfo();
        } else if(eventName.equals(SDKEventDefine.SDK_EVENT_BIND_ACCOUNT)) {
            BindAccount(data);
        } else if(eventName.equals(SDKEventDefine.SDK_EVENT_SHOW_USER_CENTER)) {
            JoyyouProxy.getInstance().showUserCenter();
        } else if(eventName.equals(SDKEventDefine.SDK_EVENT_OPEN_WEBVIEW)) {
            OpenWebView(data);
        } else if(eventName.equals(SDKEventDefine.SDK_EVENT_PURCHASE)) {
            Purchase(data);
        } else if(eventName.equals(SDKEventDefine.SDK_EVENT_RE_VERIFY_PURCHASE)) {
            JoyyouProxy.getInstance().reVerifyPurchas();
        } else if(eventName.equals(SDKEventDefine.SDK_EVENT_RECONNECT)) {
            JoyyouProxy.getInstance().reConnect();
        } else if(eventName.equals(SDKEventDefine.SDK_EVENT_SHOW_AGREEMENT)) {
            JoyyouProxy.getInstance().showAgreement();
        } else if(eventName.equals(SDKEventDefine.SDK_EVENT_SET_LANG)) {
            JoyyouProxy.getInstance().setLang(data);
        } else if(eventName.equals(SDKEventDefine.SDK_EVENT_SHARE_LINK)) {
            ShareLink(data);
        } else if(eventName.equals(SDKEventDefine.SDK_EVENT_SET_CLIENT_CHANNEL_CODE)) {
            JoyyouProxy.getInstance().setClientChannelCode(data);
        } else if(eventName.equals(SDKEventDefine.SDK_EVENT_SET_SKUS_LIST)) {
            JoyyouProxy.getInstance().setSkusList(data);
        }
    }

    @Override
    public void CallNativeBytesFunc(String eventName, String jsondata, byte[] data){
        SDKManager.GetInstance().ULog("JoyyouSDK eventName: " + eventName + " data: " + jsondata);
        if(eventName.equals(SDKEventDefine.SDK_EVENT_SHARE_IMG)) {
            ShareImg(jsondata, data);
        }
    }

    @Override
    public String CallNativeReturnFunc(String eventName, String data){
        SDKManager.GetInstance().ULog("JoyyouSDK eventName: " + eventName + " data: " + data);
        if(eventName.equals(SDKEventDefine.SDK_EVENT_ISLOGIN)) {
            return IsLogin();
        } else if(eventName.equals(SDKEventDefine.SDK_EVENT_GET_DEVICE_ID)) {
            return JoyyouConf.getInstance().getDeviceID();
        } else if(eventName.equals(SDKEventDefine.SDK_EVENT_GET_STORE_IDFA)) {
            return JoyyouConf.getInstance().getGoogleADID();
        }
        return "";
    }

    public void AnalyticsEvent(String eventName, JSONObject jsonObject) {
        try {
            FirebaseAnalytics mFirebaseAnalytics = FirebaseAnalytics.getInstance(SDKManager.GetInstance().CurrentActivity);
            jsonObject.remove("eventName");
            mFirebaseAnalytics.logEvent(eventName, SDKManager.GetInstance().JsonToBundle(jsonObject));
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void AnalyticsSetUserProperty(String jsonData){
        try {
            FirebaseAnalytics mFirebaseAnalytics = FirebaseAnalytics.getInstance(SDKManager.GetInstance().CurrentActivity);
            JSONObject jsonObject = new JSONObject(jsonData);
            String property = jsonObject.getString("property");
            String value = jsonObject.getString("value");
            if (property.equals("userId")) {
                mFirebaseAnalytics.setUserId(value);
            } else {
                mFirebaseAnalytics.setUserProperty(property, value);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void TestCrash(){
        FirebaseCrashlytics crashlytics = FirebaseCrashlytics.getInstance();
        crashlytics.log("Crash button clicked.");
        try {
            crashlytics.recordException(new Exception("Non-fatal exception: something went wrong!"));
            throw new NullPointerException();
        } catch (NullPointerException ex) {
            // [START crashlytics_log_and_report]
            crashlytics.log("NPE caught!");
            crashlytics.recordException(ex);
            // [END crashlytics_log_and_report]
        }
    }

    public void SetCrashlyticsInfo(String jsonData){
        try {
            FirebaseCrashlytics crashlytics = FirebaseCrashlytics.getInstance();
            JSONObject jsonObject = new JSONObject(jsonData);
            String type = jsonObject.getString("type");
            String data = jsonObject.getString("data");
            if (type.equals("userId")) {
                crashlytics.setUserId(data);
            } else {
                crashlytics.setCustomKey(type, data);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void GetMessagingServiceToken(){
        FirebaseInstanceId.getInstance().getInstanceId()
                .addOnCompleteListener(new OnCompleteListener<InstanceIdResult>() {
                    @Override
                    public void onComplete(@NonNull Task<InstanceIdResult> task) {
                        if (!task.isSuccessful()) {
                            SDKManager.GetInstance().ULogWarning("getInstanceId failed",false);
                            SDKManager.GetInstance().ULogWarning(task.getException().toString());
                            return;
                        }
                        String token = task.getResult().getToken();
//                        String msg = "InstanceID Token: " + token;
                        SDKManager.GetInstance().ULog(token, true);
//                        SDKManager.GetInstance().Toast(msg);
//                        SDKManager.GetInstance().SetMsgToClipBoard(token);
                        SDKManager.GetInstance().MessageNotify(NotifyMethodDefine.PushTokenCallback, token);
                    }
                });
    }

    public void Login(String jsonData){
        try {
            SDKManager.GetInstance().ULog(jsonData);
            JSONObject jsonObject = new JSONObject(jsonData);
            String loginMode = jsonObject.getString("loginMode");
            JoyyouProxy.getInstance().login(loginMode);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public String IsLogin(){
        if(JoyyouProxy.getInstance().isLogin()){
            return "true";
        }
        return "false";
    }

    public void AutoLogin(){
        if(JoyyouProxy.getInstance().isLogin()){
            JoyyouProxy.getInstance().login();
        }
    }

    public void Logout(){
        JoyyouProxy.getInstance().logout();
    }

    public void DeleteAccount(String jsonData){
        SDKManager.GetInstance().ULog(jsonData);
        JoyyouProxy.getInstance().logOff(jsonData, new JoyyouListener.LogOffListener() {
            @Override
            public void onComplete(JoyyouListener.ResultDefine resultDefine, String data) {
                JSONObject jsonObject = new JSONObject();
                try {
                    jsonObject.put("result", String.valueOf(resultDefine.value()));
                    jsonObject.put("data", data);
                    SDKManager.GetInstance().MessageNotify(NotifyMethodDefine.DeleteAccountCallback, jsonObject.toString());
                } catch (JSONException e){
                    e.printStackTrace();
                }
            }
        });
    }

    public void GetUserInfo(){
        JoyyouProxy.getInstance().getUserInfo(new JoyyouListener.GetAccountInfoListener() {
            @Override
            public void onComplete(JoyyouListener.ResultDefine resultDefine, String accountInfo) {
                JSONObject jsonObject = new JSONObject();
                try {
                    jsonObject.put("result", String.valueOf(resultDefine.value()));
                    if (resultDefine == JoyyouListener.ResultDefine.SUCCESS) {
                        jsonObject.put("data", accountInfo);
                    }
                    SDKManager.GetInstance().MessageNotify(NotifyMethodDefine.GetUserInfoCallback, jsonObject.toString());
                } catch (JSONException e){
                    e.printStackTrace();
                }
            }
        });
    }

    public void BindAccount(String loginType){
        JoyyouProxy.getInstance().bindAccount(Integer.parseInt(loginType), new JoyyouListener.BindListener() {
            @Override
            public void onComplete(JoyyouListener.ResultDefine resultDefine, String paramString) {
                JSONObject jsonObject = new JSONObject();
                try {
                    jsonObject.put("result", String.valueOf(resultDefine.value()));
                    jsonObject.put("data", paramString);
                    SDKManager.GetInstance().MessageNotify(NotifyMethodDefine.BindAccountCallback, jsonObject.toString());
                } catch (JSONException e){
                    e.printStackTrace();
                }
            }
        });
    }

    public void OpenWebView(String jsonData){
        try {
            SDKManager.GetInstance().ULog(jsonData);
            JSONObject jsonObject = new JSONObject(jsonData);
            String url = jsonObject.getString("url");
            String title = jsonObject.getString("title");
            JoyyouProxy.getInstance().openUrl(url, title);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void Purchase(String jsonData){
        SDKManager.GetInstance().ULog(jsonData);
        try{
            JSONObject jsonObject = new JSONObject(jsonData);
            String orderid = jsonObject.getString("orderid");
            String productName = jsonObject.has("productName") ? jsonObject.getString("productName") : "";
            String authCode = jsonObject.has("authCode") ? jsonObject.getString("authCode") : "";
            if (productName.isEmpty() || productName.equals("")) { // 如果没有传 productName，则表示 mycard 支付渠道
                productName = authCode;
            }
            SDKManager.GetInstance().ULog("Purchase productName = " + productName);
            JoyyouProxy.getInstance().purchase(productName, orderid, new JoyyouListener.PurchaseListener() {
                @Override
                public void onComplete(JoyyouListener.ResultDefine resultDefine, String paramString) {
                    JSONObject jsonObject = new JSONObject();
                    try {
                        jsonObject.put("result", String.valueOf(resultDefine.value()));
                        if (resultDefine == JoyyouListener.ResultDefine.SUCCESS) {
                            jsonObject.put("orderid", paramString);
                        } else{
                            jsonObject.put("msg", paramString);
                        }
                        SDKManager.GetInstance().MessageNotify(NotifyMethodDefine.PayCallback, jsonObject.toString());
                    } catch (JSONException e){
                        e.printStackTrace();
                    }
                }
            });
        }catch (JSONException e){
            e.printStackTrace();
        }
    }

    public void ShareLink(String jsonData) {
        SDKManager.GetInstance().ULog("ShareLink: " + jsonData);
        try {
            JSONObject jsonObject = new JSONObject(jsonData);
            String shareSDKType = jsonObject.getString("shareSDKType");
            String url = jsonObject.getString("url");
            String shareChannel = jsonObject.getString("shareChannel");
            lastShareType = shareSDKType;
            JoyyouProxy.getInstance().share(shareChannel, url
                    , new JoyyouListener.ShareListener() {
                        @Override
                        public void onComplete(JoyyouListener.ResultDefine resultDefine, String paramString) {
                            JSONObject jsonObject = new JSONObject();
                            try {
                                jsonObject.put("result", String.valueOf(resultDefine.value()));
                                jsonObject.put("shareSDKType", lastShareType);
                                jsonObject.put("msg", paramString);
                                SDKManager.GetInstance().MessageNotify(NotifyMethodDefine.ShareCallback, jsonObject.toString());
                            } catch (JSONException e){
                                e.printStackTrace();
                            }
                        }
                    });
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void ShareImg(String jsonData, byte[] bytes){
        SDKManager.GetInstance().ULog("ShareImg: " + jsonData + " -> " + bytes.length);
        try {
            JSONObject jsonObject = new JSONObject(jsonData);
            String shareSDKType = jsonObject.getString("shareSDKType");
            String shareChannel = jsonObject.getString("shareChannel");
            lastShareType = shareSDKType;
            JoyyouProxy.getInstance().share(shareChannel, bytes, new JoyyouListener.ShareListener() {
                @Override
                public void onComplete(JoyyouListener.ResultDefine resultDefine, String paramString) {
                    JSONObject jsonObject = new JSONObject();
                    try {
                        jsonObject.put("result", String.valueOf(resultDefine.value()));
                        jsonObject.put("shareSDKType", lastShareType);
                        jsonObject.put("msg", paramString);
                        SDKManager.GetInstance().MessageNotify(NotifyMethodDefine.ShareCallback, jsonObject.toString());
                    } catch (JSONException e){
                        e.printStackTrace();
                    }
                }
            });
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}
