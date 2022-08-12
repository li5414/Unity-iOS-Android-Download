package com.unity3d.plugin.downloader;

import com.google.android.vending.expansion.downloader.impl.DownloaderService;

public class UnityDownloaderService extends DownloaderService {
    // stuff for LVL -- MODIFIED FROM C# SCRIPTS!
    static String BASE64_PUBLIC_KEY = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAtpLeCDpCg9fJXKlnODb2wohWWJAeQI0kQMquuUgUEka8+HXqMoydIXVOBqtCwGEJ4KR7X9mUZHUxmxdt35zlmTd88keeU/XYhQZTuaKmXKZ/h4+yR0orj+Q8kppbYen6U/E7n/WNJANJELMbdDfKaBxTYskHnYCOlN7vdsXS/jd6AVYS29svOfTZQDv+XIYYb2VmjV6OkjSnvpKEv56MJNPUqePuh2QpVN6qF04fdC3KHrclI633k0zQwzSFRMWoPWbszdkO1Ez12UtsIBgtnkbGGQRE4dHaZhlf0/xpLIzkBOLwTOBnFzhd/tN7XLLH0qqP2WdE1d4S5Dbq4IdbywIDAQAB";
    // used by the preference obfuscater
    static byte[] SALT = new byte[] {
            1, 43, -12, -1, 54, 98,
            -100, -12, 43, 2, -8, -4, 9, 5, -106, -108, -33, 45, -1, 84
    };

    /**
     * This public key comes from your Android Market publisher account, and it
     * used by the LVL to validate responses from Market on your behalf.
     */
    @Override
    public String getPublicKey() {
        return BASE64_PUBLIC_KEY;
    }

    /**
     * This is used by the preference obfuscater to make sure that your
     * obfuscated preferences are different than the ones used by other
     * applications.
     */
    @Override
    public byte[] getSALT() {
        return SALT;
    }

    /**
     * Fill this in with the class name for your alarm receiver. We do this
     * because receivers must be unique across all of Android (it's a good idea
     * to make sure that your receiver is in your unique package)
     */
    @Override
    public String getAlarmReceiverClassName() {
        return UnityAlarmReceiver.class.getName();
    }

}
