package com.joyyou.rosdk.define;

import java.util.HashMap;
import java.util.Map;

public class PermissionReqCodeDefine {
    public static Integer LOCATION = 10001;
    public static Integer NOTIFICATION = 10002;
    public static Integer CAMERA = 10003;
    public static Integer PHOTOS = 10004;
    public static Integer MICROPHONE = 10005;
    public static Integer NETWORK = 10006;
    public static Integer PHONESTATE = 10007;

    public static Map<Integer, String> Code2String = new HashMap<Integer, String>(){
        {
            put(LOCATION, "Location");
            put(NOTIFICATION, "Notification");
            put(CAMERA, "Camera");
            put(PHOTOS, "Photos");
            put(MICROPHONE, "Microphone");
            put(NETWORK, "Network");
            put(PHONESTATE, "PhoneState");
        }
    };
}
