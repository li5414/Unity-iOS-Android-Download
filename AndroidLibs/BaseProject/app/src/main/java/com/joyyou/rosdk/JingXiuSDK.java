package com.joyyou.rosdk;

import android.content.Intent;
import android.os.Bundle;

import com.joyyou.rosdk.define.ISDK;

import cn.ruyi.game.sdk.JxGameSdkManager;

public class JingXiuSDK extends ISDK {
    @Override
    public void OnActivityResult(int requestCode, int resultCode, Intent data){
        JxGameSdkManager.onActivityResult(requestCode, resultCode, data);
    }
}
