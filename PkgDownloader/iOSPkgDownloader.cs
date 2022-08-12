using MoonCommonLib;
using System.Collections.Generic;
using System.Collections;
using System.IO;
using System;
using UnityEngine;
using MoonCommonLib.Updater;
using LitJson;

namespace SDKLib
{
    internal class iOSPkgDownloader : PkgDownloader
    {
        public enum HWDownloadState
        {
            HWDownloadStateDefault = 0,  // 默认
            HWDownloadStateDownloading,  // 正在下载
            HWDownloadStateWaiting,      // 等待
            HWDownloadStatePaused,       // 暂停
            HWDownloadStateFinish,       // 完成
            HWDownloadStateError,        // 错误
        }
        private float lastTime = 0;
        private float lastProgress = 0.0f;
        private readonly float DOWNLOAD_INTERVAL = 1f;
        public override void Awake()
        {
            base.Awake();
            // 如果不存在 Documents/assetpack 目录，则创建
            DirectoryEx.MakeDirectoryExist(PathEx.PkgPath.OuterPath);
        }
        float diff;
        protected override void PkgDownloaderCallBack(JsonData data)
        {
            string eventName = data["eventName"].ToString();
            switch (eventName)
            {
                case "DownloadStateChange":
                    HWDownloadState newState = (HWDownloadState)Convert.ToInt32(data["state"].ToString());
                    DownloadStateChange(newState);
                    break;
                case "Downloading":
                    diff = Time.time - lastTime;
                    if (diff > DOWNLOAD_INTERVAL)
                    {
                        lastTime = Time.time;
                        Int64 totalFileSize = Convert.ToInt64(data["totalFileSize"]); // B
                        float progress = float.Parse(data["progress"].ToString()); // 0~1
                        Int64 remainSize = (Int64)((1.0 - progress) * totalFileSize); // B
                        float speed = (progress - lastProgress) * totalFileSize / 1024.0f / diff; // kb/s
                        OnDownloadProgress(speed, progress, remainSize);
                        lastProgress = progress;
                    }
                    break;
                default:
                    break;
            }
        }
        private void DownloadStateChange(HWDownloadState newState)
        {
            DoPlayMP4(newState == HWDownloadState.HWDownloadStateFinish);
            switch (newState)
            {
                case HWDownloadState.HWDownloadStateDownloading:
                    break;
                case HWDownloadState.HWDownloadStateWaiting:
                case HWDownloadState.HWDownloadStatePaused:
                    MLua?.SendMessageToLua("PKG_DOWNLOAD_STATE_PAUSED");
                    break;
                case HWDownloadState.HWDownloadStateFinish:
                    if (!_isFinish)
                    {
                        PkgDownloadSucceed();
                    }
                    break;
                case HWDownloadState.HWDownloadStateError:
                    string msg = MiniStringPool.singleton.GetString("PKG_DOWNLOAD_STATE_FAILED_CANCELED");
                    MLua?.SendMessageToLua("PKGDOWNLOAD_NETWORK_SETUP_FAILURE", msg);
                    if (!MGameContext.singleton.isPADPkgMode)
                    {
                        _isShowingForceDownloadDlg = true;
                        MUpdaterUI.ShowDoubleBtnDlg(msg, () =>
                        {
                            _isShowingForceDownloadDlg = false;
                            ClearLocalCache();
                            MUpdater.singleton.ResetCurrentState();
                        }, () => {
                            SDKBridge.OnEvent(ESDKBridge.Device, ESDKName.None, SDKPublicDefine.BridgeEvent_Device_QuitApplication);
                        });
                    }
                    break;
            }
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
            // 通知iOS层设置下载信息
            totalDownloadSize = 0;
            foreach (var info in _needDownloadList)
            {
                totalDownloadSize += info.FileSize;
                SetUseCdn(new JsonData
                {
                    ["url"] = info.Url,
                    ["fileName"] = info.SaveFileName,
                    ["localPath"] = Path.Combine(PathEx.PkgPath.OuterPath, info.SaveFileName)
                }.ToJson());
            }
            MLua?.SendMessageToLua("PKGDOWNLOAD_START", new JsonData()
            {
                ["totalDownloadSize"] = totalDownloadSize,
            }.ToJson());
            // 通知 OC 层开始下载
            var ret = iOSPkgDownload();
            while (ret.MoveNext())
            {
                yield return null;
            }
            base.clear();
            yield return null;
        }
        private IEnumerator iOSPkgDownload()
        {
            yield return null;
            lastTime = Time.time;
            StartDownloadPkg();
            while (!_isFinish)
            {
                yield return null;
            }
        }
        protected override void ClearLocalCache()
        {
            MDebug.singleton.AddLogF("iOSPkgDownloader ClearLocalCache");
            SDKInterface2Device.CallNativeVoidFunc("ClearLocalCache", "");
        }
    }
}
