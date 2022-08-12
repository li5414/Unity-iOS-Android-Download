package com.joyyou.rosdk.define;

import android.content.Intent;
import android.os.Bundle;

/**
 * Created by chukiezou on 2018/7/10.
 */

public class ISDK {
    public void LoadLibrary() { }
    public void OnCreate(Bundle savedInstanceState) {}
    public void OnStart() {}
    public void OnRestart(){}
    public void OnResume(){}
    public void OnPause(){}
    public void OnStop(){}
    public void OnDestroy(){}
    public void OnNewIntent(Intent intent){}
    public void OnActivityResult(int requestCode, int resultCode, Intent data){}
    public void CallNativeVoidFunc(String eventName, String data){}
    public String CallNativeReturnFunc(String eventName, String data){return null;}
    public void CallNativeBytesFunc(String eventName, String args, byte[] data){}
    public void OnRequestPermissionsResult(int arg0, String[] arg1, int[] arg2){}
}
