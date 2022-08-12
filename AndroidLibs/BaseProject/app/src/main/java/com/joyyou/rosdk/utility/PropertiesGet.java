package com.joyyou.rosdk.utility;

import  java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;

public class PropertiesGet {
/*
    static{
        System.loadLibrary("property_get");
    }

    private static native String native_get(String key);
    private static native String native_get(String key,String def);
    */
    public static String getString(String key){
        return getSystemProperty(key);
    }
   // public static String getString(String key,String def){
   //     return native_get(key,def);
   // }

    public String getSonyEricssonDeviceName() {
        String model = getSystemProperty("ro.semc.product.model");
        return (model == null) ? "" : model;
    }


    private static String getSystemProperty(String propName) {
        Class<?> clsSystemProperties = tryClassForName("android.os.SystemProperties");
        Method mtdGet = tryGetMethod(clsSystemProperties, "get", String.class);
        return tryInvoke(mtdGet, null, propName);
    }

    private static Class<?> tryClassForName(String className) {
        try {
            return Class.forName(className);
        } catch (ClassNotFoundException e) {
            return null;
        }
    }

    private static Method tryGetMethod(Class<?> cls, String name, Class<?>... parameterTypes) {

        try {
            return cls.getDeclaredMethod(name, parameterTypes);
        } catch (Exception e) {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> T tryInvoke(Method m, Object object, Object... args) {
        try {
            return (T) m.invoke(object, args);
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e);
        } catch (Exception e) {
            return null;
        }
    }
}
