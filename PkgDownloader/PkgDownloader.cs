using LitJson;
using MoonCommonLib;
using MoonCommonLib.Updater;
using System;
using System.Collections;
using System.IO;
using UnityEngine;
using System.Collections.Generic;

namespace SDKLib
{
    public class MPkgDownloadInfo
    {
        public long FileSize { get; private set; }
        public string Url { get; private set; }
        public string FileMd5 { get; private set; }
        public bool MainPkg { get; private set; }
        public string SaveFileName { get; private set; }

        public MPkgDownloadInfo(string url, long size, string fileMd5, string type, string external)
        {
            Url = url;
            FileSize = size;
            FileMd5 = fileMd5;
            MainPkg = type == "pkg";
            SaveFileName = MainPkg ? $"ro_{external}.pkg" : $"ro_{external}_{MPkgMgr.singleton.ApkVersion.To3String()}.pkg";
        }
    }
    internal abstract class PkgDownloader : SDKInterfaceBase
    {
        public const int MaxFetchCount = 4;
        protected long totalDownloadSize = 0;

        public List<MPkgDownloadInfo> PkgDownloadInfos = new List<MPkgDownloadInfo>();

        private bool _finishFetching;
        private int _fetchCount;
        private bool _needFetchingAgain;

        protected readonly List<string> _lackPkgList = new List<string>();
        protected readonly List<MPkgDownloadInfo> _needDownloadList = new List<MPkgDownloadInfo>();
        private string _videoUrl;
        public string VideoUrl
        {
            get
            {
                if (string.IsNullOrEmpty(_videoUrl))
                {
                    _videoUrl = MPlatformConfigManager.GetSDKConfig("PkgDownloader", "videoUrl");
                }

                return _videoUrl;
            }
        }

        /// <summary>
        /// 是否正在显示二次确认界面
        /// </summary>
        protected bool _isShowingForceDownloadDlg = false;
        /// <summary>
        /// 下载完成
        /// </summary>
        protected bool _isFinish;
        /// <summary>
        /// 是否开启手机网络下载
        /// </summary>
        protected bool _forceDownload = false;

        #region SDKInterfaceBase接口实现

        public override ESDKBridge GetSDKType()
        {
            return ESDKBridge.PkgDownloader;
        }
        public override void Awake()
        {
            base.Awake();
            _isFinish = false;
        }
        public override void OnEvent(string eventName, JsonData data = null, object obj1 = null, object obj2 = null, object obj3 = null)
        {
            switch (eventName)
            {
                case SDKPublicDefine.BridgeEvent_PkgDownloader_PauseDownload:
                    PauseDownload();
                    break; 
                case SDKPublicDefine.BridgeEvent_PkgDownloader_ContinueDownload:
                    ContinueDownload();
                    break;
                case SDKPublicDefine.BridgeEvent_PkgDownloader_EnableDownloadWithoutWifi:
                    EnableDownloadWithoutWifi();
                    break;
                case SDKPublicDefine.BridgeEvent_PkgDownloader_PkgDownloaderCallBack:
                    PkgDownloaderCallBack(data);
                    break;
                case SDKPublicDefine.BridgeEvent_PkgDownloader_ClearLocalCache:
                    ClearLocalCache();
                    break;
            }
        }

        public override object GetArgs(string eventName, JsonData data = null, object obj1 = null, object obj2 = null, object obj3 = null)
        {
            switch (eventName)
            {
                case SDKPublicDefine.BridgeEvent_PkgDownloader_DoPkgDownload:
                    return DoPkgDownload();
                default:
                    break;
            }
            return null;
        }

        #endregion SDKInterfaceBase接口实现
        protected void SetUseCdn(string json)
        {
            MDebug.singleton.AddLogF("PkgDownloader SetUseCdn json = {0}", json);
            SDKInterface2Device.CallNativeVoidFunc("SetUseCdn", json);
        }
        protected void StartDownloadPkg()
        {
            MDebug.singleton.AddLogF("PkgDownloader StartDownloadPkg");
            SDKInterface2Device.CallNativeVoidFunc("StartDownloadPkg", "");
        }
        protected virtual void PauseDownload()
        {
            MDebug.singleton.AddLogF("PkgDownloader PauseDownload");
            SDKInterface2Device.CallNativeVoidFunc("PauseDownloadPkg", "");
        }
        protected void ContinueDownload()
        {
            MDebug.singleton.AddLogF("PkgDownloader ContinueDownload");
            SDKInterface2Device.CallNativeVoidFunc("ContinueDownloadPkg", "");
            MLua?.SendMessageToLua("PKG_DOWNLOAD_STATE_RESUME");
        }
        protected void EnableDownloadWithoutWifi()
        {
            MDebug.singleton.AddLogF("PkgDownloader EnableDownloadWithoutWifi");
            SDKInterface2Device.CallNativeVoidFunc("EnableDownloadWithoutWifi", "");
        }
        protected abstract void PkgDownloaderCallBack(JsonData data);
        protected abstract void ClearLocalCache();
        /// <summary>
        /// 下载进度
        /// </summary>
        /// <param name="speed">速度, bytes/s</param>
        /// <param name="progress">下载进度</param>
        /// <param name="remainSize">剩余下载总大小</param>
        protected void OnDownloadProgress(float speed, float progress, Int64 remainSize)
        {
            string spdmsg;

            //下载4g切换wifi关闭弹窗
            if (_isShowingForceDownloadDlg)
            {
                if (!MGameContext.singleton.isPADPkgMode)
                {
                    MUpdaterUI.HideDoubleBtnDlg();
                }
            }
            if (speed >= 1024)
            {
                spdmsg = string.Format(MiniStringPool.singleton.GetString("UPDATE_CURRENT_SPEED_MESSAGE"),
                        speed / 1024, "MB");
            }
            else
            {
                spdmsg = string.Format(MiniStringPool.singleton.GetString("UPDATE_CURRENT_SPEED_MESSAGE"),
                        speed, "KB");
            }
            if (!MGameContext.singleton.isPADPkgMode)
            {
                MUpdaterUI.ShowLoadingBar(true, spdmsg, progress);
            }
            else
            {
                //MDebug.singleton.AddGreenLogF("speed:{0} progress:{1}", spdmsg, progress);
                JsonData jsondata = new JsonData()
                {
                    ["speed"] = speed,
                    ["progress"] = progress,
                    ["remainSize"] = remainSize,
                };
                MLua?.SendMessageToLua("PKGDOWNLOAD_DOWNLOADING", jsondata.ToJson());
            }
        }
        protected void PkgDownloadSucceed()
        {
            MDebug.singleton.AddLog("PkgDownloader PkgDownloadSucceed");
            _isFinish = true;
            MLua?.SendMessageToLua("PKGDOWNLOAD_COMPLETED");
            if (MGameContext.singleton.isPADPkgMode)
            {
                MPkgMgr.singleton.InitDownloadedPkg();
            }
        }
        protected void DoPlayMP4(bool isFinished)
        {
            MDebug.singleton.AddLogF($"PkgDownloader DoPlayMP4 isFinished: {isFinished}");
            if (!isFinished)
            {
                // --story=1095740 --user=李斌 【日本EP1】【RAO-921】将安卓版下载资源时的动画换成静态背景 https://www.tapd.cn/39782289/s/2133459
                if (MGameContext.singleton.CurrentChannel == MGameArea.Japan)
                {
                    if (!MGameContext.singleton.isPADPkgMode)
                    {
                        MUpdaterUI.ClosePkgDownloadingVideo();
                    }
                }
                else
                {
                    if (!MGameContext.singleton.isPADPkgMode)
                    {
                        MUpdaterUI.PlayPkgDownloadingVideo(VideoUrl);
                    }
                }
            }
            else
            {
                if (!MGameContext.singleton.isPADPkgMode)
                {
                    MUpdaterUI.ClosePkgDownloadingVideo();
                }
            }
        }
        #region 下载Pkg
        protected void clear()
        {
            _finishFetching = false;
            _fetchCount = 0;
            _needFetchingAgain = false;
            PkgDownloadInfos.Clear();
            _lackPkgList.Clear();
            _needDownloadList.Clear();
            // 状态清理
            MUpdaterUI.ClosePkgDownloadingVideo();
        }
        private void onHttpResult(HttpTask.HttpResult result, string res)
        {
            _finishFetching = true;
            if (result == HttpTask.HttpResult.OK)
            {
                try
                {
                    MDebug.singleton.AddLog($"[PkgDownloader] onHttpResult:{res}");
                    JsonData resData = JsonMapper.ToObject(res.ToString());
                    int code = Convert.ToInt32(resData["code"].ToString());
                    if (code == 200)
                    {
                        JsonData fileData = resData["data"];
                        for (int i = 0; i < fileData.Count; i++)
                        {
                            var o = fileData[i];
                            var url = o["url"].ToString();
                            var fileSize = Convert.ToInt64(o["file_size"].ToString());
                            var fileMd5 = o["file_hash"].ToString();
                            var type = o["type"].ToString();
                            var version = o["version"].ToString();
                            var info = new MPkgDownloadInfo(url, fileSize, fileMd5, type, version);
                            PkgDownloadInfos.Add(info);
                        }
                    }
                    else
                    {
                        string errorMsg = resData["message"].ToString();
                        MDebug.singleton.AddErrorLog($"[PkgDownloader onHttpResult errorMsg:{errorMsg}");
                        _needFetchingAgain = true;
                    }
                }
                catch (Exception e)
                {
                    MDebug.singleton.AddErrorLog(res.ToString());
                    MDebug.singleton.AddErrorLog(e.ToString());
                    _needFetchingAgain = true;
                }
            }
            else
            {
                MDebug.singleton.AddWarningLog("failed to get pkg file info");
                _needFetchingAgain = true;
            }
        }
        private IEnumerator fetchPkgInfo(string pkgVersion)
        {
            var config = MPlatformConfigManager.GetLocal();
            string channelCode = config.channel;
            string bundleId = config.bundleId;
            string apkVersion = config.version.To3String();
            int timeout = MInternalDefine.CheckVersionTimeout;
            string os = "";
#if UNITY_IOS
            os = "ios";
#else
            os = "android";
#endif
            string versionCode = "1";
            string cdnRequestUrl = $"{config.apiDomain}/pkg/file-info?channel_code={channelCode}&bundleid={bundleId}&version_code={versionCode}&apk_version={apkVersion}&pkg_version={pkgVersion}&game_code=ro&os={os}";
            string ts = MCommonFunctions.GetUtcNowTimeStamp();
            string sign = MCommonFunctions.EncryptWithMD5(ts + MInternalDefine.SecretKey).ToUpper();

            PkgDownloadInfos.Clear();
            _needFetchingAgain = false;
            _finishFetching = false;

            HttpTask task = HttpTask.Create(cdnRequestUrl);
            task.AddHeader("timestamp", ts).AddHeader("sign", sign).SetMethod("GET").TimeOut(timeout)
                .GetResponseAynsc(onHttpResult);

            MDebug.singleton.AddLog($"[PkgDownloader] cdnRequestUrl:{cdnRequestUrl}");

            while (!_finishFetching) yield return null;
        }
        // 根据服务器下载信息和本地缺失信息，整理需要下载列表
        private void getNeedDownloadList()
        {
            var set = new HashSet<string>(_lackPkgList);
            var noneMainPkg = MPkgMgr.singleton.PkgVersion == null;
            var localPkgMatchApk = MPkgMgr.CompareVersion(MPkgMgr.singleton.PkgVersion, MPkgMgr.singleton.ApkVersion) == 0;
            foreach (var info in PkgDownloadInfos)
            {
                if (noneMainPkg || (localPkgMatchApk && info.MainPkg && _lackPkgList.Contains(info.SaveFileName)))
                {
                    MDebug.singleton.AddLog($"[PkgDownloader] condition1 _needDownloadList add:{info.SaveFileName}");
                    _needDownloadList.Add(info);
                    set.Remove(info.SaveFileName);
                }
                else if (!localPkgMatchApk && !info.MainPkg)
                {
                    MDebug.singleton.AddLog($"[PkgDownloader] condition2 _needDownloadList add:{info.SaveFileName}");
                    _needDownloadList.Add(info);
                }
            }

            MDebug.singleton.AddLog($"[PkgDownloader] set.Count {set.Count} getNeedDownloadList:");
            foreach (var info in _needDownloadList)
            {
                MDebug.singleton.AddLog($"[PkgDownloader] {info.SaveFileName}");
            }
            if (set.Count <= 0) return;

            _needDownloadList.Clear();
        }

        // 与服务器通信获取下载信息，并做下载匹配
        private IEnumerator tryFetchPkgInfoEnumerator(string pkgVersionName)
        {
            _needDownloadList.Clear();

            while (_fetchCount < MaxFetchCount)
            {
                _fetchCount++;
                MDebug.singleton.AddLog($"[PkgDownloader] tryFetchPkgInfoEnumerator fetchCount:{_fetchCount}");
                IEnumerator it = fetchPkgInfo(pkgVersionName);

                while (it.MoveNext()) yield return null;

                if (!_needFetchingAgain) break;

                if (_fetchCount >= MaxFetchCount)
                {
                    MDebug.singleton.AddErrorLog($"[PkgDownloader] tryFetchPkgInfoEnumerator fetchCount more than {MaxFetchCount}");
                    MUpdater.singleton.Pause();
                    yield break;
                }
            }

            getNeedDownloadList();
        }

        // 下载Pkg
        public virtual IEnumerator DoPkgDownload()
        {
            clear();
            // 检查PKG路径
            var checkPathCount = 0;
            while (string.IsNullOrEmpty(PathEx.PkgPath.OuterPath) && checkPathCount++ < 1)
            {
                yield return null;
            }
            Action quit = MInterfaceMgr.singleton.GetInterface<IDevice>("MCommonLib2SDKLibBridge").QuitApplication;
            // 若还是没找到存储路径，则通知错误
            if (string.IsNullOrEmpty(PathEx.PkgPath.OuterPath))
            {
                if (!MGameContext.singleton.isPADPkgMode)
                {
                    MUpdaterUI.ShowSingleBtnDlg(MiniStringPool.singleton.GetString("PKG_DOWNLOAD_STATE_PAUSED_SDCARD_UNAVAILABLE"), quit);
                }
                yield return null;
            }

            // 检查本地PKG资源，判定是否需要下载更新
            if (!MPkgMgr.singleton.CheckPkgNeedUpdate(out var lackPkgList))
            {
                MDebug.singleton.AddLog($"[PkgDownloader] DoPkgDownload 无需更新");
                yield break;
            }
            _lackPkgList.AddRange(lackPkgList);

            // 透传Pkg参数，用于修补缺失的PKG或者获取Patch列表
            var pkgVersionName = string.Empty;
            if (MPkgMgr.singleton.PkgVersion != null && _lackPkgList.Count <= 0)
            {
                pkgVersionName = MPkgMgr.singleton.PkgVersion.To3String();
            }
            // 首次请求
            var it = tryFetchPkgInfoEnumerator(pkgVersionName);
            while (it.MoveNext()) yield return null;

            yield return null;


            // 若下载列表为空，则尝试从下载列表摘取全量列表
            if (_needDownloadList.Count <= 0)
            {
                //MUpdater.singleton.Pause();
                //// 请求用户重新下载
                //MUpdaterUI.ShowDoubleBtnDlg(MiniStringPool.singleton.GetString("PKG_SYSTEM_PKG_INFO_INVALID"), () =>
                //{
                // 同意则重新下载最新的PKG资源，放弃Patch
                MPkgMgr.singleton.PrepareReDownload();
                getNeedDownloadList();
                //MUpdater.singleton.Resume();
                //}, quit);

                yield return null;

                // 若下载列表为空，代表请求失败
                if (_needDownloadList.Count <= 0)
                {
                    // 服务器返回信息错误
                    if (!MGameContext.singleton.isPADPkgMode)
                    {
                        MUpdaterUI.ShowSingleBtnDlg(MiniStringPool.singleton.GetString("PKG_SYSTEM_PKG_NOT_FOUND_ERROR"), quit);
                    }
                    yield return null;
                }
            }
            yield return null;
        }
        #endregion 下载Pkg
        
    }
}