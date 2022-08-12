package com.joyyou.rosdk.utility;

import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Build;
import android.text.TextUtils;

import com.joyyou.rosdk.SDKManager;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.FileReader;

import static android.content.Context.SENSOR_SERVICE;

/**
 * Created by markhu on 2018/8/14.
 */

public class SystemInfoUtil {
    public static boolean mayIsEmulator() {
        return mayOnEmulatorViaQEMU()
                || isEmulatorViaBuild()
                || isEmulatorFromAbi()
                || isEmulatorFromCpu()
                || NotHasLightSensorManager();

    }

    public static boolean isEmulatorViaBuild() {

        if (!TextUtils.isEmpty(PropertiesGet.getString("ro.product.model"))
                && PropertiesGet.getString("ro.product.model").toLowerCase().contains("sdk")) {
            return true;
        }

        /**
         * ro.product.manufacturer likes unknown
         */
        if (!TextUtils.isEmpty(PropertiesGet.getString("ro.product.manufacturer"))
                && PropertiesGet.getString("ro.product.manufacture").toLowerCase().contains("unknown")) {
            return true;
        }

        /**
         * ro.product.device likes generic
         */
        if (!TextUtils.isEmpty(PropertiesGet.getString("ro.product.device"))
                && PropertiesGet.getString("ro.product.device").toLowerCase().contains("generic")) {
            return true;
        }

        return false;
    }


    //  qemu模拟器特征
    public static boolean mayOnEmulatorViaQEMU() {
        String qemu = PropertiesGet.getString("ro.kernel.qemu");
        return "1".equals(qemu);
    }

    // 查杀比较严格，放在最后，直接pass x86
    private static boolean isEmulatorFromCpu() {
        ShellAdbUtil.CommandResult commandResult = ShellAdbUtil.execCommand("cat /proc/cpuinfo", false);
        String cpuInfo = commandResult.successMsg;
        return !TextUtils.isEmpty(cpuInfo) && ((cpuInfo.contains("intel") || cpuInfo.contains("amd")));
    }

    private static boolean isEmulatorFromAbi() {

        String abi= getCpuAbi();
        return !TextUtils.isEmpty(abi) && abi.contains("x86");
    }

    //@Deprecated
    // IMPORTANT: This field should be initialized via a function call to
    // prevent its value being inlined in the app during compilation because
    // we will later set it to the value based on the app's target SDK.
    //  public static final String SERIAL = getString("no.such.thing");


    //    序列号	 重新烧录flash
    public static String getSerialno() {
        String serialno = PropertiesGet.getString("ro.serialno");
        if (TextUtils.isEmpty(serialno)) {
            serialno = ShellAdbUtil.execCommand("cat /sys/class/android_usb/android0/iSerial", false).successMsg;
        }
        if (TextUtils.isEmpty(serialno)) {
            serialno = Build.SERIAL;
        }
        return serialno;
    }


    public static String getManufacturer() {
        return PropertiesGet.getString("ro.product.manufacturer");
    }

    /**
     * 获取手机型号
     *
     * @return  手机型号
     */
    public static String getDeviceModel() {
        return android.os.Build.MODEL;
    }

    public static String getBrand() {
        return PropertiesGet.getString("ro.product.brand");
    }

    public static String getModel() {
        return PropertiesGet.getString("ro.product.model");
    }


    public static String getCpuAbi() {
        return PropertiesGet.getString("ro.product.cpu.abi");
    }

    public static String getCPUName() {
        String cpuName = "";
        try {
            String str1 = "/proc/cpuinfo";
            String[] cpuInfo = { "", "" };
            FileReader fr = new FileReader(str1);
            BufferedReader localBufferedReader = new BufferedReader(fr, 8192);
            String line = null;
            while ((line = localBufferedReader.readLine()) != null) {
                if (line.toLowerCase().contains("hardware")) {
                    cpuInfo[0] = line;
                    break;
                }
            }
            cpuInfo[1] = Build.HARDWARE;
            localBufferedReader.close();
            cpuName = cpuInfo[0] + "&" + cpuInfo[1];
        }
        catch (Exception ignored)
        {
        }
        return cpuName;
    }

    public static String getDevice() {
        return PropertiesGet.getString("ro.product.device");
    }

    /*
    * 获取cpu主频
    */
    public static int getMaxCPUFreq() {
        String kCpuInfoMaxFreqFilePath = "/sys/devices/system/cpu/cpu0/cpufreq/cpuinfo_max_freq";

        int result = 1600000;
        FileReader fr = null;
        BufferedReader br = null;

        try {
            fr = new FileReader(kCpuInfoMaxFreqFilePath);
            br = new BufferedReader(fr);
            String text = br.readLine();
            result = Integer.parseInt(text.trim());
        } catch (FileNotFoundException e) {
            // e.printStackTrace();
            return 1600000;
        } catch (IOException e) {
            // e.printStackTrace();
            return result;
        } finally {
            if (fr != null)
                try {
                    fr.close();
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    // e.printStackTrace();
                    return result;
                }

            if (br != null)
                try {
                    br.close();

                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    // e.printStackTrace();
                    return result;
                }

        }
        return result;
    }

    /**
     * 判断是否存在光传感器来判断是否为模拟器
     * 部分真机也不存在温度和压力传感器。其余传感器模拟器也存在。
     * @return true 为模拟器*/
    public static boolean NotHasLightSensorManager() {
        SensorManager sensorManager = (SensorManager) SDKManager.GetInstance().CurrentActivity.getSystemService(SENSOR_SERVICE);
        Sensor sensor8 = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT); //光
        if (null == sensor8) {
            SDKManager.GetInstance().ULog("NotHasLightSensorManager");
            return true;
        } else {
            SDKManager.GetInstance().ULog("HasLightSensorManager");
            return false;
        }
    }

}
