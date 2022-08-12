package com.joyyou.rosdk.define;

public class SDKEventDefine {
    // 注：函数名称不能重名

    // joyyou sdk
    public static String SDK_EVENT_ANALYTICS_EVENT = "AnalyticsEvent";
    public static String SDK_EVENT_ANALYTICS_SET_USER_PROPERTY = "AnalyticsSetUserProperty";
    public static String SDK_EVENT_TEST_CRASH = "TestCrash";
    public static String SDK_EVENT_SET_CRASH_INFO = "SetCrashlyticsInfo";
    public static String SDK_EVENT_GET_MESSAGING_SERVICE_TOKEN = "GetMessagingServiceToken";
    public static String SDK_EVENT_GET_PUSH_DATA = "GetMessagingdata";
    public static String SDK_EVENT_LOGIN = "Login";
    public static String SDK_EVENT_ISLOGIN = "IsLogin";
    public static String SDK_EVENT_AUTOLOGIN = "AutoLogin";
    public static String SDK_EVENT_LOGOUT = "Logout";
    public static String SDK_EVENT_DELETE_ACCOUNT = "DeleteAccount";
    public static String SDK_EVENT_GET_USER_INFO = "GetUserInfo";
    public static String SDK_EVENT_BIND_ACCOUNT = "BindAccount";
    public static String SDK_EVENT_SHOW_USER_CENTER = "ShowUserCenter";
    public static String SDK_EVENT_SHOW_AGREEMENT = "ShowAgreement";
    public static String SDK_EVENT_SET_LANG = "SetLang";
    public static String SDK_EVENT_OPEN_WEBVIEW = "OpenWebView";
    public static String SDK_EVENT_PURCHASE = "Purchase";
    public static String SDK_EVENT_RE_VERIFY_PURCHASE = "ReVerifyPurchase";
    public static String SDK_EVENT_RECONNECT = "ReConnect";
    public static String SDK_EVENT_SHARE_LINK = "ShareLink";
    public static String SDK_EVENT_SHARE_IMG = "ShareImg";
    public static String SDK_EVENT_SET_CLIENT_CHANNEL_CODE = "SetClientChannelCode";
    public static String SDK_EVENT_GET_DEVICE_ID = "GetDeviceId";
    public static String SDK_EVENT_SET_SKUS_LIST = "SetSkusList";
    public static String SDK_EVENT_GET_STORE_IDFA = "GetStoreIDFA";

    // bugly
    public static String SDK_EVENT_BUGLY_SET_CRASH_FILTER = "BuglySetCrashFilter";
    public static String SDK_EVENT_BUGLY_SET_CRASH_REG_FILTER = "BuglySetCrashRegularFilter";
	//PKG
    public static String SDK_EVENT_SET_USE_CDN="SetUseCdn";
    public static String SDK_EVENT_START_DOWNLOAD_PKG="StartDownloadPkg";
    public static String SDK_EVENT_PAUSE_DOWNLOAD_PKG="PauseDownloadPkg";
    public static String SDK_EVENT_CONTINUE_DOWNLOAD_PKG="ContinueDownloadPkg";
    public static String SDK_EVENT_ENABLE_DOWNLOAD_WITHOUT_WIFI="EnableDownloadWithoutWifi";
    public static String SDK_EVENT_ENABLE_NOTIFY_DOWNLOAD_STATE="NotifyDownloadState";

    //Sentry
    public static String SDK_EVENT_SENTRY_SET_EXTRA_DATA="SentrySetExtraData";
}
