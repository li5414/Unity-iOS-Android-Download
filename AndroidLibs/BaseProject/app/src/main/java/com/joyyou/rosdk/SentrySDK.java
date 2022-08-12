package com.joyyou.rosdk;
import android.os.Bundle;

import com.joyyou.rosdk.define.ISDK;
import com.joyyou.rosdk.define.SDKEventDefine;

import org.json.JSONException;
import org.json.JSONObject;

import io.sentry.Breadcrumb;
import io.sentry.Sentry;
import io.sentry.SentryLevel;
import io.sentry.android.core.SentryAndroid;
import io.sentry.protocol.User;

public class SentrySDK extends ISDK {
    @Override
    public void OnCreate(Bundle savedInstanceState) {
        SDKManager.GetInstance().ULog("SentrySdkInit");
        SentryAndroid.init(SDKManager.GetInstance().CurrentActivity, options -> {
            options.setShutdownTimeout(5000);
            options.setDebug(SDKManager.GetInstance().isDebug);
            options.setEnvironment(SDKManager.GetInstance().isDebug?"production":"releases");
        });
    }


    @Override
    public void CallNativeVoidFunc(String eventName, String data) {
        SDKManager.GetInstance().ULog("SentrySDK eventName: " + eventName + " data: " + data);
        if(eventName.equals(SDKEventDefine.SDK_EVENT_SENTRY_SET_EXTRA_DATA)) {
            SentrySetExtraData(data);
        }
    }

    public void SentrySetExtraData(String jsonData) {
        SDKManager.GetInstance().ULog("SentrySetExtraData: " + jsonData);
        try {
            JSONObject jsonObject = new JSONObject(jsonData);
            String key = jsonObject.getString("key");
            String value = jsonObject.getString("value");
            if("user_id".equals(key)) {
                User user = new User();
                user.setId(value);
                user.setUsername(value);
                user.setEmail(value);
                Sentry.setUser(user);
            } else if("version".equals(key)) {
                Sentry.setTag("version", value);
            } else {
                Sentry.configureScope(scope -> {
                    Breadcrumb breadcrumb = new Breadcrumb();
                    breadcrumb.setCategory("custom_data");
                    breadcrumb.setMessage(key +  ":" + value);
                    breadcrumb.setLevel(SentryLevel.INFO);
                    scope.addBreadcrumb(breadcrumb);
                });
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}
