package com.joyyou.rosdk;

import android.content.Context;
import android.os.VibrationEffect;
import android.os.Vibrator;

import com.unity3d.player.UnityPlayer;

public final class VibrateManager {

    private static Vibrator vibrator = null;

    static {
        vibrator = (Vibrator) UnityPlayer.currentActivity.getSystemService(Context.VIBRATOR_SERVICE);
    }


    public static void Vibrate(long[] parttern, int repeat){
        vibrator.vibrate(parttern, repeat);
    }

    public static void Cancel(){
        vibrator.cancel();
    }
}
