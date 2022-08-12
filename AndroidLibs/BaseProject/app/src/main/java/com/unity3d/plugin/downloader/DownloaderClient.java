package com.unity3d.plugin.downloader;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Messenger;
import android.support.v4.content.IntentCompat;
import android.util.Log;

import com.google.android.vending.expansion.downloader.DownloadProgressInfo;
import com.google.android.vending.expansion.downloader.DownloaderClientMarshaller;
import com.google.android.vending.expansion.downloader.DownloaderServiceMarshaller;
import com.google.android.vending.expansion.downloader.Helpers;
import com.google.android.vending.expansion.downloader.IDownloaderClient;
import com.google.android.vending.expansion.downloader.IDownloaderService;
import com.google.android.vending.expansion.downloader.IStub;
import com.google.android.vending.expansion.downloader.impl.CustomNotificationFactory;
import com.google.android.vending.expansion.downloader.impl.DownloadInfo;
import com.google.android.vending.expansion.downloader.impl.DownloadNotification;
import com.google.android.vending.expansion.downloader.impl.DownloadsDB;

public class DownloaderClient implements IDownloaderClient {
    public  class DownloadProgress{
        public long mOverallTotal;
        public long mOverallProgress;
        public long mTimeRemaining; // time remaining
        public float mCurrentSpeed; // speed in KB/S
        public String getTimeRemainingStr()
        {
            return Helpers.getTimeRemaining(mTimeRemaining);
        }
        public Long getProgressPercent()
        {
            return mOverallProgress* 100 / mOverallTotal;
        }
    }

    public interface  PkgDownloaderCallback{
        void onDownloadProgress(DownloadProgress downloadProgress);
        void onDownloadStateChange(int newState);
    }
    private Context mContext;
    private Class<?> mMainActivityClass;
    private int mState;
    private IDownloaderService mRemoteService;
    private IStub mDownloaderClientStub;
    //保存当前进度
    private DownloadProgress mDownloadProgress;
    //处理回调
    public  PkgDownloaderCallback mPkgDownloaderCallback;
    //通知
    DownloadNotification.IDownloadNotification mDownloadNotification;
    CharSequence mLabel;
    PendingIntent mContentIntent;
    NotificationManager mNotificationManager;
    public  DownloaderClient(Context context,Class<?> mainActivityClass,PkgDownloaderCallback callback)
    {
        mContext=context;
        mMainActivityClass=mainActivityClass;
        mPkgDownloaderCallback=callback;
        mDownloadProgress =new DownloadProgress();
        ApplicationInfo ai = context.getApplicationInfo();
        mLabel = context.getPackageManager().getApplicationLabel(ai);
        mDownloadNotification=CustomNotificationFactory.createDownloadNitofication();
        mNotificationManager = (NotificationManager)
                mContext.getSystemService(Context.NOTIFICATION_SERVICE);
        if(android.os.Build.VERSION.SDK_INT>= Build.VERSION_CODES.O ){
            NotificationChannel channel = new NotificationChannel(
                    "ROPkgNotification",
                    mLabel,
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            mNotificationManager.createNotificationChannel(channel);
        }

    }

    //开始下载
    public  void StartDownload(){

        try {
            Intent intentToLaunchMainActivityFromNotification = new Intent(mContext, mMainActivityClass);
            intentToLaunchMainActivityFromNotification.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
            intentToLaunchMainActivityFromNotification.setAction("android.intent.action.MAIN");
            intentToLaunchMainActivityFromNotification.addCategory("android.intent.category.LAUNCHER");
            // Build PendingIntent used to open this activity from Notification
            mContentIntent = PendingIntent.getActivity(mContext, 0, intentToLaunchMainActivityFromNotification, PendingIntent.FLAG_UPDATE_CURRENT);

            // Request to start the download
            int startResult = DownloaderClientMarshaller.startDownloadServiceIfRequired(mContext, mContentIntent, UnityDownloaderService.class);


            if (startResult != DownloaderClientMarshaller.NO_DOWNLOAD_REQUIRED) {
                mDownloaderClientStub = DownloaderClientMarshaller.CreateStub(this, UnityDownloaderService.class);
                mDownloaderClientStub.connect(mContext);
                return;
            }
            else
            {
                mPkgDownloaderCallback.onDownloadStateChange(IDownloaderClient.STATE_COMPLETED);
            }
        }catch (PackageManager.NameNotFoundException e) {
            mPkgDownloaderCallback.onDownloadStateChange(IDownloaderClient.STATE_FAILED);
            e.printStackTrace();
        }
    }

    //暂停下载
    public  void PauseDownload()
    {
        mRemoteService.requestPauseDownload();
    }

    //继续下载
    public  void ContinueDownload()
    {
        mRemoteService.requestContinueDownload();
    }

    public void onResume() {
        if (null != mDownloaderClientStub) {
            mDownloaderClientStub.connect(mContext);
        }
    }

    public  void onStop(){
        if (null != mDownloaderClientStub) {
            mDownloaderClientStub.disconnect(mContext);
        }
        mNotificationManager.cancelAll();
    }

    private void setState(int newState) {
        mState = newState;
        if(mPkgDownloaderCallback!=null) {
            mPkgDownloaderCallback.onDownloadStateChange(newState);
        }
    }
    @Override
    public void onServiceConnected(Messenger m) {
        mRemoteService = DownloaderServiceMarshaller.CreateProxy(m);
        mRemoteService.onClientUpdated(mDownloaderClientStub.getMessenger());
    }

    public void resetAcvicity(){
        PackageManager packageManager = mContext.getPackageManager();
        Intent intent = packageManager.getLaunchIntentForPackage(mContext.getPackageName());
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        mContext.startActivity(intent);
        Runtime.getRuntime().exit(0);
    }

    @Override
    public void onDownloadStateChanged(int newState) {
        if(newState==IDownloaderClient.STATE_COMPLETED){
            setState(newState);
            try {
                int resId = Helpers.getDownloaderStringResourceIDFromState(mContext, newState);
                NotifyDownloadState(newState, mContext.getResources().getString(resId));
            }catch (Exception e){
                e.printStackTrace();
            }
            return;
        }
        setState(newState);
        try {
            int resId = Helpers.getDownloaderStringResourceIDFromState(mContext, newState);
            NotifyDownloadState(newState, mContext.getResources().getString(resId));
        }catch (Exception e){
            e.printStackTrace();
        }

    }



    public  void NotifyDownloadState(int newState,String contentInfo){
        // get the new title string and paused text
        int iconResource;
        boolean ongoingEvent;
        switch (newState) {
            case 0:
                iconResource = android.R.drawable.stat_sys_warning;
                ongoingEvent = false;
                break;

            case IDownloaderClient.STATE_DOWNLOADING:
                iconResource = android.R.drawable.stat_sys_download;
                ongoingEvent = true;
                break;

            case IDownloaderClient.STATE_FETCHING_URL:
            case IDownloaderClient.STATE_CONNECTING:
                iconResource = android.R.drawable.stat_sys_download_done;
                ongoingEvent = true;
                break;

            case IDownloaderClient.STATE_COMPLETED:
                iconResource = android.R.drawable.stat_sys_download_done;
                ongoingEvent = false;
            case IDownloaderClient.STATE_PAUSED_BY_REQUEST:
                return;

            case IDownloaderClient.STATE_FAILED:
            case IDownloaderClient.STATE_FAILED_CANCELED:
            case IDownloaderClient.STATE_FAILED_FETCHING_URL:
            case IDownloaderClient.STATE_FAILED_SDCARD_FULL:
            case IDownloaderClient.STATE_FAILED_UNLICENSED:
                iconResource = android.R.drawable.stat_sys_warning;
                ongoingEvent = false;
                break;
            case IDownloaderClient.STATE_FAILED_FILE_SIZE_MISMATCH:
                iconResource = android.R.drawable.stat_sys_warning;
                ongoingEvent = false;
                PauseDownload();
            default:
                iconResource = android.R.drawable.stat_sys_warning;
                ongoingEvent = true;
                break;
        }
//        try{
//            mDownloadNotification.setContentInfo(contentInfo);
//            mDownloadNotification.setIcon(iconResource);
//            mDownloadNotification.setTitle(mLabel);
//            mDownloadNotification.setPendingIntent(mContentIntent);
//            mDownloadNotification.setTicker(mLabel + ":"+contentInfo);
//            Notification mCurrentNotification =mDownloadNotification.updateNotification(mContext);
//
//            //mCurrentNotification.setLatestEventInfo(mContext, mCurrentTitle, mCurrentText,
//            //mContentIntent);
//            if (ongoingEvent) {
//                mCurrentNotification.flags |= Notification.FLAG_ONGOING_EVENT;
//            } else {
//                mCurrentNotification.flags &= ~Notification.FLAG_ONGOING_EVENT;
//                mCurrentNotification.flags |= Notification.FLAG_AUTO_CANCEL;
//            }
//            mNotificationManager.notify(DownloadNotification.NOTIFICATION_ID, mCurrentNotification);
//
//        }
//        catch (Exception e)
//        {
//            Log.e("DownloadNotification", "onDownloadStateChanged: "+e.toString() );
//        }
    }


    @Override
    public void onDownloadProgress(DownloadProgressInfo progress) {
        this.mDownloadProgress.mCurrentSpeed=progress.mCurrentSpeed;
        this.mDownloadProgress.mOverallProgress=progress.mOverallProgress;
        this.mDownloadProgress.mTimeRemaining=progress.mTimeRemaining;
        this.mDownloadProgress.mOverallTotal=progress.mOverallTotal;
        if(mPkgDownloaderCallback!=null)
        {
            mPkgDownloaderCallback.onDownloadProgress(this.mDownloadProgress);
        }
    }
}