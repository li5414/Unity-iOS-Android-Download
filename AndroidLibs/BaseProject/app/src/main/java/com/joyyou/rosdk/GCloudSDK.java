package com.joyyou.rosdk;

import java.lang.reflect.Method;

import com.joyyou.rosdk.define.ISDK;
import com.tencent.gcloud.plugin.GCloudAppLifecycle;
import android.content.Intent;
import android.os.Bundle;
import java.util.ArrayList;
import java.util.List;
import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;

//Android Support
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
// //AndroidX Support
// import androidx.core.app.ActivityCompat;
// import androidx.core.content.ContextCompat;

public class GCloudSDK extends ISDK {
    @Override
    public void LoadLibrary(){
        loadPluginByReflection("TDataMaster");
        loadPluginByReflection("GPM");
        loadPluginByReflection("tgpa");
        loadPluginByReflection("gcloud");
        loadPluginByReflection("MSDKCore");
        loadPluginByReflection("MSDKUnityAdapter");
        loadPluginByReflection("tersafe");
        loadPluginByReflection("HttpDnsPlugin");
        loadPluginByReflection("GVoice");

    }
    private final int PermissionRequestCode = 100;

    //replace System.loadLibrary with this block
    public static void loadPluginByReflection(String libName)
    {
        try {
            Class<?> clz = Class.forName("com.tencent.gcloud.plugin.PluginUtils");
            Method m = clz.getDeclaredMethod("loadLibrary", String.class);
            m.invoke(clz, libName);
        }
        catch (Exception e){
            e.printStackTrace();
            try{

                System.loadLibrary(libName);
            }
            catch(Exception ex){
                ex.printStackTrace();
            }
        }
    }

    private void RequestPermission(Activity activity, List<String> permissionList, int requestCode)
    {
        if(null==activity || null==permissionList){
            return;
        }
        List<String> permissionNeeded = new ArrayList<String>();

        for(int i=0; i<permissionList.size(); i++){

            String permission = permissionList.get(i);

            if(null!=permission && PackageManager.PERMISSION_GRANTED != ContextCompat.checkSelfPermission(activity.getApplicationContext(), permission)){
                permissionNeeded.add(permission);
            }
        }

        if(permissionNeeded.size()>0){
            ActivityCompat.requestPermissions(activity, permissionNeeded.toArray(new String[permissionNeeded.size()]), requestCode);
        }
    }

    private void GCloudSDKRequestDynamicPermissions()
    {
        List<String> permissions = new ArrayList<String>();
        //Add_Permissions
        //GVoice
        permissions.add(Manifest.permission.MODIFY_AUDIO_SETTINGS);
        permissions.add(Manifest.permission.RECORD_AUDIO);
        permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);


        this.RequestPermission(SDKManager.GetInstance().CurrentActivity, permissions, PermissionRequestCode);
    }

    @Override
    public void OnCreate(Bundle savedInstanceState) {
        // Add Lifecycle
        //GCloud
        GCloudAppLifecycle.Instance.addObserverByName("com.tencent.gcloud.GCloudAppLifecycleListener");
        //TDM
        GCloudAppLifecycle.Instance.addObserverByName("com.tdatamaster.tdm.gcloud.lifecycle.PluginReportLifecycle");
        //MSDKCore
        GCloudAppLifecycle.Instance.addObserverByName("com.tencent.gcloud.msdk.core.lifecycle.PluginMSDKLifecycle");

        // Add Dynamic Permissions
        GCloudSDKRequestDynamicPermissions();

        GCloudAppLifecycle.Instance.onCreate(SDKManager.GetInstance().CurrentActivity, savedInstanceState);
    }



    @Override
    public void OnStart() {
        GCloudAppLifecycle.Instance.onStart();
    }

    @Override
    public void OnResume() {
        GCloudAppLifecycle.Instance.onResume();
    }

    @Override
    public void OnPause() {
        GCloudAppLifecycle.Instance.onPause();
    }

    @Override
    public void OnStop() {
        GCloudAppLifecycle.Instance.onStop();
    }

    @Override
    public void OnDestroy() {
        GCloudAppLifecycle.Instance.onDestroy();
    }

    @Override
    public void OnRestart() {
        GCloudAppLifecycle.Instance.onRestart();
    }

    @Override
    public void OnNewIntent(Intent intent) {
        GCloudAppLifecycle.Instance.onNewIntent(intent);
    }

    @Override
    public void OnActivityResult(int requestCode, int resultCode, Intent data) {
        GCloudAppLifecycle.Instance.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void OnRequestPermissionsResult(int arg0, String[] arg1, int[] arg2) {
        GCloudAppLifecycle.Instance.onRequestPermissionsResult(arg0, arg1, arg2);
    }
}
