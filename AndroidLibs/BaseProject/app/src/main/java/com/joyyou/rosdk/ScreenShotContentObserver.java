package com.joyyou.rosdk;

import android.content.Context;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.provider.MediaStore;
import android.util.Log;

import com.joyyou.rosdk.SDKManager;

import java.io.File;

public abstract class ScreenShotContentObserver extends ContentObserver {
    private Context context;
    private boolean isFromEdit = false;
    private String previousPath = "";
    private String preUriPath = "";

    public ScreenShotContentObserver(Handler handler, Context context) {
        super(handler);
        this.context = context;
    }

    @Override
    public boolean deliverSelfNotifications() {
        return super.deliverSelfNotifications();
    }

    @Override
    public void onChange(boolean selfChange, Uri uri) {
        SDKManager.GetInstance().ULog("onChange Uri:" + uri.toString());
        if(!uri.toString().contains("content://media/external/images/media")){
            return;
        }
        if(preUriPath.equals(uri.toString())){
            return;
        }
        preUriPath = uri.toString();
        Cursor cursor = null;
        try {
            cursor = context.getContentResolver().query(uri, new String[]{
                    MediaStore.Images.Media.DISPLAY_NAME,
                    MediaStore.Images.Media.DATA
            }, null, null, null);
            if (cursor != null && cursor.moveToLast()) {
                int displayNameColumnIndex = cursor.getColumnIndex(MediaStore.Images.Media.DISPLAY_NAME);
                int dataColumnIndex = cursor.getColumnIndex(MediaStore.Images.Media.DATA);
                String fileName = cursor.getString(displayNameColumnIndex);
                String path = cursor.getString(dataColumnIndex);
                if (new File(path).lastModified() + 3000 >= System.currentTimeMillis()) {
                    if (isScreenshot(path) && !isFromEdit && !(previousPath != null && previousPath.equals(path))) {
                        onScreenShot(path, fileName);
                    }
                    previousPath = path;
                    isFromEdit = false;
                } else {
                    cursor.close();
                    return;
                }
                SDKManager.GetInstance().ULog("take pic " + fileName + "----" + path);
            }
        } catch (Exception e) {
            e.printStackTrace();
            isFromEdit = true;
            SDKManager.GetInstance().ULog("onScreenShot isFromEdit true");
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        super.onChange(selfChange, uri);
    }

    private boolean isScreenshot(String path) {
        return path != null && path.toLowerCase().contains("screenshot");
    }

    protected abstract void onScreenShot(String path, String fileName);

}