package com.google.android.vending.expansion.downloader.impl;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.support.v4.app.NotificationCompat;

import com.google.android.vending.expansion.downloader.Helpers;

public class V26CustomNotification implements DownloadNotification.ICustomNotification {
    CharSequence mTitle;
    CharSequence mTicker;
    int mIcon;
    long mTotalKB = -1;
    long mCurrentKB = -1;
    long mTimeRemaining;
    PendingIntent mPendingIntent;

    @Override
    public void setIcon(int icon) {
        mIcon = icon;
    }

    @Override
    public void setTitle(CharSequence title) {
        mTitle = title;
    }

    @Override
    public void setTotalBytes(long totalBytes) {
        mTotalKB = totalBytes;
    }

    @Override
    public void setCurrentBytes(long currentBytes) {
        mCurrentKB = currentBytes;
    }

    void setProgress(Notification.Builder builder) {

    }

    @Override
    public Notification updateNotification(Context c) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(c,"ROPkgNotification");
        builder.setContentTitle(mTitle);
        if (mTotalKB > 0 && -1 != mCurrentKB) {
            builder.setProgress((int) (mTotalKB >> 8), (int) (mCurrentKB >> 8), false);
        } else {
            builder.setProgress(0, 0, true);
        }
        builder.setContentText(Helpers.getDownloadProgressString(mCurrentKB, mTotalKB));
        builder.setContentInfo(String.format("%1$s left",Helpers.getTimeRemaining(mTimeRemaining)));
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

    @Override
    public void setPendingIntent(PendingIntent contentIntent) {
        mPendingIntent = contentIntent;
    }

    @Override
    public void setTicker(CharSequence ticker) {
        mTicker = ticker;
    }

    @Override
    public void setTimeRemaining(long timeRemaining) {
        mTimeRemaining = timeRemaining;
    }
}
