package com.google.android.vending.expansion.downloader.impl;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.support.v4.app.NotificationCompat;

import com.google.android.vending.expansion.downloader.Helpers;

public class V26DownloadNotification implements DownloadNotification.IDownloadNotification{

    CharSequence mTitle;
    CharSequence mTicker;
    CharSequence mContentInfo;
    int mIcon;
    PendingIntent mPendingIntent;
    long mProgress;
    long mMax;
    @Override
    public void setTitle(CharSequence title) {
        mTitle = title;
    }

    @Override
    public void setContentInfo(CharSequence contentInfo) {
        mContentInfo=contentInfo;
    }

    @Override
    public void setTicker(CharSequence ticker) {
        mTicker=ticker;
    }

    @Override
    public void setPendingIntent(PendingIntent mContentIntent) {
        mPendingIntent=mContentIntent;
    }

    @Override
    public void setIcon(int iconResource) {
        mIcon = iconResource;
    }

    @Override
    public void setProgress(long progress, long max) {
        mProgress=progress;
        mMax=max;
    }

    @Override
    public Notification updateNotification(Context c) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(c,"ROPkgNotification");
        builder.setContentTitle(mTitle);
        if (mMax > 0 && -1 != mProgress) {
            builder.setProgress((int) (mMax >> 8), (int) (mProgress >> 8), false);
        } else {
            builder.setProgress(0, 0, true);
        }
        builder.setContentText(mContentInfo);
        //builder.setContentInfo(String.format("%1$s left",Helpers.getTimeRemaining(mTimeRemaining)));
        if (mIcon != 0) {
            builder.setSmallIcon(mIcon);
        } else {
            int iconResource = android.R.drawable.stat_sys_download;
            builder.setSmallIcon(iconResource);
        }
        builder.setOngoing(true);
        builder.setTicker(mTicker);
        builder.setContentIntent(mPendingIntent);
        builder.setOnlyAlertOnce(true);

        return builder.getNotification();

    }
}
