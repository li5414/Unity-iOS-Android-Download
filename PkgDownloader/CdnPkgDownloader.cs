using LitJson;
using MoonCommonLib;
using System;
using System.Collections;
using System.Collections.Generic;
using MoonCommonLib.Updater;
using UnityEngine;

namespace SDKLib
{
    internal class CdnPkgDownloader : PkgDownloader
    {


        private string m_ExpansionFilePath;
        private static AndroidJavaClass EnvironmentClass = new AndroidJavaClass("android.os.Environment");
        private const string Environment_MediaMounted = "mounted";
        private static string m_PkgPackage;

        private static string PkgPackage
        {
            get
            {
                if (m_PkgPackage == null)
                {
                    PopulatePKGProperties();
                }
                return m_PkgPackage;
            }
        }
        private static int m_PkgVersion;
        public static int PkgVersion
        {
            get
            {
                if (m_PkgVersion == 0)
                {
                    PopulatePKGProperties();
                }
                return m_PkgVersion;
            }
        }
        public override void Awake()
        {
            base.Awake();
            DirectoryEx.MakeDirectoryExist(GetExpansionFilePath());
        }
        public enum EPkgDownloadState
        {
            PKG_DOWNLOAD_STATE_NONE,
            PKG_DOWNLOAD_STATE_IDLE,
            PKG_DOWNLOAD_STATE_FETCHING_URL,
            PKG_DOWNLOAD_STATE_CONNECTING,
            PKG_DOWNLOAD_STATE_DOWNLOADING,
            PKG_DOWNLOAD_STATE_COMPLETED,
            PKG_DOWNLOAD_STATE_PAUSED_NETWORK_UNAVAILABLE,
            PKG_DOWNLOAD_STATE_PAUSED_BY_REQUEST,
            PKG_DOWNLOAD_STATE_PAUSED_WIFI_DISABLED_NEED_CELLULAR_PERMISSION,
            PKG_DOWNLOAD_STATE_PAUSED_NEED_CELLULAR_PERMISSION,
            PKG_DOWNLOAD_STATE_PAUSED_WIFI_DISABLED,
            PKG_DOWNLOAD_STATE_PAUSED_NEED_WIFI,
            PKG_DOWNLOAD_STATE_PAUSED_ROAMING,
            PKG_DOWNLOAD_STATE_PAUSED_NETWORK_SETUP_FAILURE,
            PKG_DOWNLOAD_STATE_PAUSED_SDCARD_UNAVAILABLE,
            PKG_DOWNLOAD_STATE_FAILED_UNLICENSED,
            PKG_DOWNLOAD_STATE_FAILED_FETCHING_URL,
            PKG_DOWNLOAD_STATE_FAILED_SDCARD_FULL,
            PKG_DOWNLOAD_STATE_FAILED_CANCELED,
            PKG_DOWNLOAD_STATE_FAILED,
            PKG_DOWNLOAD_STATE_FILE_SIZE_MISMATCH,
            PKG_DOWNLOAD_STATE_CHECK_MD5,
        }
        public string GetExpansionFilePath()
        {
            if (EnvironmentClass.CallStatic<string>("getExternalStorageState") != Environment_MediaMounted)
            {
                m_ExpansionFilePath = null;
                return m_ExpansionFilePath;
            }

            if (string.IsNullOrEmpty(m_ExpansionFilePath))
            {
                const string obbPath = "Android/obb";
                using (var externalStorageDirectory = EnvironmentClass.CallStatic<AndroidJavaObject>("getExternalStorageDirectory"))
                {
                    var externalRoot = externalStorageDirectory.Call<string>("getPath");
                    m_ExpansionFilePath = $"{externalRoot}/{obbPath}/{PkgPackage}";
                }
            }
            return m_ExpansionFilePath;
        }
        // This code will reuse the package version from the .apk when looking for the .obb Modify
        // as appropriate
        private static void PopulatePKGProperties()
        {
            using (var unityPlayerClass = new AndroidJavaClass("com.unity3d.player.UnityPlayer"))
            {
                var currentActivity = unityPlayerClass.GetStatic<AndroidJavaObject>("currentActivity");
                m_PkgPackage = currentActivity.Call<string>("getPackageName");
                var packageInfo = currentActivity.Call<AndroidJavaObject>("getPackageManager").Call<AndroidJavaObject>("getPackageInfo", m_PkgPackage, 0);
                m_PkgVersion = packageInfo.Get<int>("versionCode");
            }
        }
        /// <summary>
        /// 下载过程回调
        /// </summary>
        /// <param name="data">回调数据</param>
        protected override void PkgDownloaderCallBack(JsonData data)
        {
            string state = data["state"].ToString();
            switch (state)
            {
                case "DownloadStateChange":
                    {
                        EPkgDownloadState newState = (EPkgDownloadState)Convert.ToInt32(data["data"].ToString());
                        DownloadStateChange(newState);
                    }
                    break;

                case "Downloading":
                    {
                        JsonData downloadData = JsonMapper.ToObject(data["data"].ToString());
                        float speed = float.Parse(downloadData["currentSpeed"].ToString());
                        Int64 overallTotal = Convert.ToInt64(downloadData["overallTotal"].ToString());
                        Int64 overallProgress = Convert.ToInt64(downloadData["overallProgress"].ToString());
                        OnDownloadProgress(speed, overallProgress * 1.0f / overallTotal, overallTotal - overallProgress);
                    }
                    break;
                default:
                    break;
            }
        }
        /// <summary>
        /// 状态栏提示,上层本地化,底层实现通知
        /// </summary>
        /// <param name="newState">下载状态</param>
        /// <param name="notifyInfo">消息</param>
        private void NotifyDownloadState(EPkgDownloadState newState, string notifyInfo)
        {
            //暂时使用底层自带通知
            //             JsonData jsonData = new JsonData();
            //             jsonData["newState"] = (int)newState;
            //             jsonData["stateInfo"] = notifyInfo;
            //             SDKInterface2Device.CallNativeVoidFunc("NotifyDownloadState", jsonData.ToJson());
        }
        /// <summary>
        /// 下载状态发生改变
        /// </summary>
        /// <param name="newState">新的状态</param>
        private void DownloadStateChange(EPkgDownloadState newState)
        {
            DoPlayMP4(newState == EPkgDownloadState.PKG_DOWNLOAD_STATE_COMPLETED);
            switch (newState)
            {
                case EPkgDownloadState.PKG_DOWNLOAD_STATE_NONE:
                case EPkgDownloadState.PKG_DOWNLOAD_STATE_IDLE:
                case EPkgDownloadState.PKG_DOWNLOAD_STATE_FETCHING_URL:
                case EPkgDownloadState.PKG_DOWNLOAD_STATE_CONNECTING:
                    if (!MGameContext.singleton.isPADPkgMode)
                    {
                        MUpdaterUI.ShowLoadingBar(true, MiniStringPool.singleton.GetString(EPkgDownloadState.PKG_DOWNLOAD_STATE_CONNECTING.ToString()));
                    }
                    NotifyDownloadState(EPkgDownloadState.PKG_DOWNLOAD_STATE_CONNECTING, MiniStringPool.singleton.GetString(EPkgDownloadState.PKG_DOWNLOAD_STATE_CONNECTING.ToString()));
                    break;
                case EPkgDownloadState.PKG_DOWNLOAD_STATE_FAILED_SDCARD_FULL:
                case EPkgDownloadState.PKG_DOWNLOAD_STATE_PAUSED_SDCARD_UNAVAILABLE:
                case EPkgDownloadState.PKG_DOWNLOAD_STATE_FILE_SIZE_MISMATCH:
                case EPkgDownloadState.PKG_DOWNLOAD_STATE_PAUSED_NETWORK_SETUP_FAILURE:
                    {
                        PauseDownload();
                        string msg = MiniStringPool.singleton.GetString(newState.ToString());
                        MLua?.SendMessageToLua("PKGDOWNLOAD_NETWORK_SETUP_FAILURE", msg);
                        if (!MGameContext.singleton.isPADPkgMode)
                        {
                            MUpdaterUI.ShowSingleBtnDlg(msg, () => { SDKBridge.OnEvent(ESDKBridge.Device, ESDKName.None, SDKPublicDefine.BridgeEvent_Device_QuitApplication); });
                        }
                    }
                    NotifyDownloadState(newState, MiniStringPool.singleton.GetString(newState.ToString()));
                    break;

                case EPkgDownloadState.PKG_DOWNLOAD_STATE_PAUSED_NETWORK_UNAVAILABLE:
                    MLua?.SendMessageToLua("PKGDOWNLOAD_NETWORK_SETUP_FAILURE", MiniStringPool.singleton.GetString(newState.ToString()));
                    if (!MGameContext.singleton.isPADPkgMode)
                    {
                        MUpdaterUI.ShowLoadingBar(true, MiniStringPool.singleton.GetString(newState.ToString()));
                    }
                    NotifyDownloadState(newState, MiniStringPool.singleton.GetString(newState.ToString()));
                    break;

                case EPkgDownloadState.PKG_DOWNLOAD_STATE_DOWNLOADING:
                    NotifyDownloadState(newState, MiniStringPool.singleton.GetString(newState.ToString()));
                    break;

                case EPkgDownloadState.PKG_DOWNLOAD_STATE_COMPLETED:
                    if (!_isFinish)
                    {
                        PkgDownloadSucceed();
                    }
                    NotifyDownloadState(newState, MiniStringPool.singleton.GetString(newState.ToString()));
                    break;

                case EPkgDownloadState.PKG_DOWNLOAD_STATE_PAUSED_WIFI_DISABLED_NEED_CELLULAR_PERMISSION:
                case EPkgDownloadState.PKG_DOWNLOAD_STATE_PAUSED_WIFI_DISABLED:
                case EPkgDownloadState.PKG_DOWNLOAD_STATE_PAUSED_NEED_WIFI:
                    ConfirmForceDownload();
                    MLua?.SendMessageToLua("PKG_DOWNLOAD_STATE_PAUSED_NEED_WIFI");
                    NotifyDownloadState(newState, MiniStringPool.singleton.GetString(newState.ToString()));
                    break;

                case EPkgDownloadState.PKG_DOWNLOAD_STATE_PAUSED_BY_REQUEST:
                    break;
                case EPkgDownloadState.PKG_DOWNLOAD_STATE_PAUSED_ROAMING:
                    {
                        MLua?.SendMessageToLua("PKG_DOWNLOAD_STATE_PAUSED_NEED_WIFI");
                        if (!MGameContext.singleton.isPADPkgMode)
                        {
                            string msg = MiniStringPool.singleton.GetString(newState.ToString());
                            _isShowingForceDownloadDlg = true;
                            MUpdaterUI.ShowDoubleBtnDlg(msg, () =>
                            {
                                _isShowingForceDownloadDlg = false;
                                ContinueDownload();
                            }, () => {
                                SDKBridge.OnEvent(ESDKBridge.Device, ESDKName.None, SDKPublicDefine.BridgeEvent_Device_QuitApplication);
                            });
                        }
                    }
                    NotifyDownloadState(newState, MiniStringPool.singleton.GetString(newState.ToString()));
                    break;

                case EPkgDownloadState.PKG_DOWNLOAD_STATE_FAILED_UNLICENSED:
                case EPkgDownloadState.PKG_DOWNLOAD_STATE_FAILED_FETCHING_URL:
                case EPkgDownloadState.PKG_DOWNLOAD_STATE_FAILED_CANCELED:
                case EPkgDownloadState.PKG_DOWNLOAD_STATE_FAILED:
                case EPkgDownloadState.PKG_DOWNLOAD_STATE_PAUSED_NEED_CELLULAR_PERMISSION:
                    {
                        string msg = MiniStringPool.singleton.GetString(newState.ToString());
                        MLua?.SendMessageToLua("PKGDOWNLOAD_NETWORK_SETUP_FAILURE", msg);
                        if (!MGameContext.singleton.isPADPkgMode)
                        {
                            _isShowingForceDownloadDlg = true;
                            MUpdaterUI.ShowDoubleBtnDlg(msg, () =>
                            {
                                _isShowingForceDownloadDlg = false;
                                MUpdater.singleton.ResetCurrentState();
                            }, () => {
                                SDKBridge.OnEvent(ESDKBridge.Device, ESDKName.None, SDKPublicDefine.BridgeEvent_Device_QuitApplication);
                            });
                        }
                    }
                    NotifyDownloadState(newState, MiniStringPool.singleton.GetString(newState.ToString()));
                    break;
                case EPkgDownloadState.PKG_DOWNLOAD_STATE_CHECK_MD5:
                    var key = MiniStringPool.singleton.GetString(EPkgDownloadState.PKG_DOWNLOAD_STATE_CHECK_MD5.ToString());
                    if (!MGameContext.singleton.isPADPkgMode)
                    {
                        MUpdaterUI.ShowLoadingBar(true, key, -1, false);
                    }
                    NotifyDownloadState(newState, key);
                    break;
                default:
                    break;
            }
        }
        /// <summary>
        /// 二次确认界面
        /// </summary>
        private void ConfirmForceDownload()
        {
            if (_forceDownload)
            {
                EnableDownloadWithoutWifi();
                return;
            }
            PauseDownload();
            _isShowingForceDownloadDlg = true;
            string strinfo = MiniStringPool.singleton.GetString("PKG_DOWNLOAD_STATE_PAUSED_WIFI_DISABLED_NEED_CELLULAR_PERMISSION");
            MDebug.singleton.AddGreenLog(strinfo);
            MUpdaterUI.ShowDoubleBtnDlg(strinfo, () =>
            {
                EnableDownloadWithoutWifi();
                ContinueDownload();
                _forceDownload = true;
                _isShowingForceDownloadDlg = false;
            }, () =>
            {
                SDKBridge.OnEvent(ESDKBridge.Device, ESDKName.None, SDKPublicDefine.BridgeEvent_Device_QuitApplication);
            });
        }

        public override IEnumerator DoPkgDownload()
        {
            var it = base.DoPkgDownload();
            while (it.MoveNext())
            {
                yield return null;
            }
            if (_needDownloadList.Count == 0)
            {
                yield break;
            }
            // 通知Java层设置下载信息
            totalDownloadSize = 0;
            foreach (var info in _needDownloadList)
            {
                totalDownloadSize += info.FileSize;
                SetUseCdn(new JsonData
                {
                    ["useCdn"] = true,
                    ["url"] = info.Url,
                    ["fileSize"] = info.FileSize,
                    ["fileMd5"] = info.FileMd5,
                    ["fileName"] = info.SaveFileName
                }.ToJson());
            }
            MLua?.SendMessageToLua("PKGDOWNLOAD_START", new JsonData()
            {
                ["totalDownloadSize"] = totalDownloadSize,
            }.ToJson());
            // 通知Java层开始下载
            var ret = CdnPkgDownload();
            while (ret.MoveNext())
            {
                yield return null;
            }
            base.clear();
            yield return null;
        }

        private IEnumerator CdnPkgDownload()
        {
            yield return null;
            string expPath = GetExpansionFilePath();

            if (expPath == null)
            {
                MDebug.singleton.AddErrorLog("External storage is not available!");
            }
            else
            {
                StartDownloadPkg();
                while (!_isFinish)
                {
                    yield return null;
                }
            }
        }
        protected override void PauseDownload()
        {
            base.PauseDownload();
            MDebug.singleton.AddLogF("CdnPkgDownloader PauseDownload");
            MLua?.SendMessageToLua("PKG_DOWNLOAD_STATE_PAUSED");
        }
        protected override void ClearLocalCache()
        {
            MDebug.singleton.AddLogF("CdnPkgDownloader ClearLocalCache");
            MPkgMgr.singleton.PrepareReDownload();
        }
    }
}