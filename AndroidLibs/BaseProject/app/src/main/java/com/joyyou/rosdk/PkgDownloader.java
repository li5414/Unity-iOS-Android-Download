package com.joyyou.rosdk;

import android.content.Context;
import android.os.Bundle;
import android.util.JsonReader;

import com.google.android.vending.expansion.downloader.impl.DownloadInfo;
import com.google.android.vending.expansion.downloader.impl.DownloadsDB;
import com.joyyou.rosdk.MainActivity;
import com.joyyou.rosdk.SDKManager;
import com.joyyou.rosdk.define.ISDK;
import com.joyyou.rosdk.define.NotifyMethodDefine;
import com.joyyou.rosdk.define.SDKEventDefine;
import com.unity3d.plugin.downloader.DownloaderClient;
import com.unity3d.plugin.downloader.UnityDownloaderService;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.function.ToIntFunction;

public class PkgDownloader extends ISDK {
    DownloaderClient downloaderClient;
    @Override
    public void OnCreate(Bundle savedInstanceState) {
        Context context = SDKManager.GetInstance().CurrentActivity.getApplicationContext();
        downloaderClient=new DownloaderClient(context, MainActivity.class, new DownloaderClient.PkgDownloaderCallback() {
            @Override
            public void onDownloadProgress(DownloaderClient.DownloadProgress downloadProgress) {
                try{
                    JSONObject jsonObject=new JSONObject();
                    JSONObject data=new JSONObject();
                    data.put("overallTotal",downloadProgress.mOverallTotal);
                    data.put("overallProgress",downloadProgress.mOverallProgress);
                    data.put("timeRemaining",downloadProgress.getTimeRemainingStr());
                    data.put("currentSpeed",downloadProgress.mCurrentSpeed);
                    jsonObject.put("state","Downloading");
                    jsonObject.put("data",data.toString());
                    SDKManager.GetInstance().MessageNotify(NotifyMethodDefine.PkgDownloaderCallBack, jsonObject.toString());
                }catch (JSONException e)
                {
                    e.printStackTrace();
                }
            }

            @Override
            public void onDownloadStateChange(int newState) {
                try{
                    JSONObject jsonObject=new JSONObject();
                    jsonObject.put("state","DownloadStateChange");
                    jsonObject.put("data",String.valueOf(newState));
                    SDKManager.GetInstance().MessageNotify(NotifyMethodDefine.PkgDownloaderCallBack, jsonObject.toString());
                }catch (JSONException e)
                {
                    e.printStackTrace();
                }
            }
        });
//        DownloadsDB.getDB(context).AddNewDownload(new DownloadInfo(
//                0,"1.pkg","https://art.123u.com/rorexue/arts/t_20200806174909665.apk",2127280862L,"a755cd2b3916b5c0014df0b37f71e107"));
//        DownloadsDB.getDB(context).AddNewDownload(new DownloadInfo(
//                0,"2.pkg","https://art.123u.com/rorexue/arts/t_20200806191842255.apk",2154055938L,"a1078f15dc322adb026420ef7b760aa8"));
//        downloaderClient.StartDownload();
    }


    public void CallNativeVoidFunc(String eventName, String data)
    {
        SDKManager.GetInstance().ULog("PkgDownloader eventName: " + eventName + " data: " + data);
        if(eventName.equals(SDKEventDefine.SDK_EVENT_START_DOWNLOAD_PKG))
        {
            SDKManager.GetInstance().ULog(SDKEventDefine.SDK_EVENT_START_DOWNLOAD_PKG);
            StartDownload();
        }
        else if(eventName.equals(SDKEventDefine.SDK_EVENT_PAUSE_DOWNLOAD_PKG))
        {
            SDKManager.GetInstance().ULog(SDKEventDefine.SDK_EVENT_PAUSE_DOWNLOAD_PKG);
            PauseDownload();
        }
        else if(eventName.equals(SDKEventDefine.SDK_EVENT_CONTINUE_DOWNLOAD_PKG))
        {
            SDKManager.GetInstance().ULog(SDKEventDefine.SDK_EVENT_CONTINUE_DOWNLOAD_PKG);
            ContinueDownload();
        }
        else if(eventName.equals(SDKEventDefine.SDK_EVENT_ENABLE_DOWNLOAD_WITHOUT_WIFI))
        {
            SDKManager.GetInstance().ULog(SDKEventDefine.SDK_EVENT_ENABLE_DOWNLOAD_WITHOUT_WIFI);
            EnableDownloadWithoutWifi();
        }
        else if(eventName.equals(SDKEventDefine.SDK_EVENT_ENABLE_NOTIFY_DOWNLOAD_STATE))
        {
            SDKManager.GetInstance().ULog(SDKEventDefine.SDK_EVENT_ENABLE_NOTIFY_DOWNLOAD_STATE);
            NotifyDownloadState(data);
        }
        else if(eventName.endsWith(SDKEventDefine.SDK_EVENT_SET_USE_CDN)){
            SDKManager.GetInstance().ULog(SDKEventDefine.SDK_EVENT_SET_USE_CDN);
            SetUseCdn(data);
        }
    }

    public void SetUseCdn(String jsonData){
        try {
            Context context = SDKManager.GetInstance().CurrentActivity.getApplicationContext();
            SDKManager.GetInstance().ULog(jsonData);
            JSONObject jsonObject = new JSONObject(jsonData);
            boolean useCdn = jsonObject.getBoolean("useCdn");
            String url = jsonObject.getString("url");
            long fileSize=jsonObject.getLong("fileSize");
            String fileMd5=jsonObject.getString("fileMd5");
            String fileName = jsonObject.getString("fileName");
            DownloadsDB.getDB(context).AddNewDownload(new DownloadInfo(
                    0,fileName,url,fileSize,fileMd5));
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public  void NotifyDownloadState(String jsonData){
        try {
            SDKManager.GetInstance().ULog(jsonData);
            JSONObject jsonObject = new JSONObject(jsonData);
            int newState = jsonObject.getInt("newState");
            String info = jsonObject.getString("stateInfo");
            downloaderClient.NotifyDownloadState(newState,info);
        } catch (JSONException e) {
            e.printStackTrace();
        }

    }

    public void EnableDownloadWithoutWifi()
    {
        UnityDownloaderService unityDownloaderService=new UnityDownloaderService();
        unityDownloaderService.setDownloadFlags(1);
    }

    public  void OnResume()
    {
        downloaderClient.onResume();
    }
    public  void OnStop()
    {
        downloaderClient.onStop();
    }
    public  void StartDownload()
    {
        if(downloaderClient!=null){
            downloaderClient.StartDownload();
        }
    }

    public  void PauseDownload()
    {
        if(downloaderClient!=null){
            downloaderClient.PauseDownload();
        }
    }

    public  void ContinueDownload()
    {
        if(downloaderClient!=null){
            downloaderClient.ContinueDownload();
        }
    }


}