package com.joyyou.rosdk;

import android.app.Activity;
import android.app.Application;

public class Utility {

    public static int GetR(Activity activity, String field, String name){
        return activity != null ? activity.getResources().getIdentifier(name, field, activity.getPackageName()) : 0;
    }

    public static int GetR(Application app, String field, String name){
        return app != null ? app.getResources().getIdentifier(name, field, app.getPackageName()) : 0;
    }

}
