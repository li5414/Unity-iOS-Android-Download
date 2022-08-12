package com.joyyou.rosdk;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.VideoView;

import com.unity3d.player.UnityPlayer;

public class VideoPlayerActivity extends Activity {

    VideoView videoView;
    int moviepos = -1;
    String movieName = null;
    boolean canSkip = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(GetR("layout", "movie_activity"));

        Intent intent = getIntent();
        movieName = intent.getStringExtra("movieName");
        canSkip = intent.getBooleanExtra("canSkip", false);

        if (Build.VERSION.SDK_INT >= 28) {
            WindowManager.LayoutParams lp = getWindow().getAttributes();
            lp.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
            getWindow().setAttributes(lp);
        }

        SDKManager.GetInstance().ULog("VideoPlayerActivity onCreate");
        videoView = (VideoView)findViewById(GetR("id", "videoView"));
        if(videoView == null){
            SDKManager.GetInstance().ULogError("videoView not found");
        }
    }

    private int GetR(String field, String name){
        return Utility.GetR(this, field, name);
    }

    // 必须使用此方法回到上一个activity，不然在miui的全面屏操作模式下会无法切换成功，导致卡住
    // 参考：https://www.tapd.cn/20332331/bugtrace/bugs/view?bug_id=1120332331001049267
    private void finishActivity()
    {
        // 获取启动当前Activity的上一个Intent
        Intent intent = new Intent(VideoPlayerActivity.this,
                MainActivity.class);
        // 启动intent对应的Activity
        startActivity(intent);
        // 结束当前Activity
        VideoPlayerActivity.this.finish();
    }

    @Override
    protected void onStart() {
        super.onStart();
        SDKManager.GetInstance().ULog("VideoPlayerActivity onStart");
        Uri uri = resourceToUri(this, GetR("raw", movieName));
        SDKManager.GetInstance().ULog("url is " + uri.toString());
        videoView.setVideoURI(uri);
        videoView.requestFocus();
        videoView.start();
        videoView.setOnCompletionListener(new MediaPlayer.OnCompletionListener()
        {
            @Override
            public void onCompletion(MediaPlayer mp)
            {
                UnityPlayer.UnitySendMessage("Canvas", "Hide", "");

                finishActivity();

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ECLAIR) {
                    overridePendingTransition(0, 0);
                }
            }
        });
        videoView.setOnErrorListener(new MediaPlayer.OnErrorListener() {

            @Override
            public boolean onError(MediaPlayer mp, int what, int extra) {
                // TODO Auto-generated method stub
                SDKManager.GetInstance().ULog("on media player error : " + what + ", " + extra);
                return false;
            }
        });

        videoView.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                SDKManager.GetInstance().ULog("on media player focus change : " + hasFocus);
            }
        });
    }

    @Override
    public void onPause() {
        moviepos = videoView.getCurrentPosition();
        //videoView.stopPlayback();
        videoView.pause();

        SDKManager.GetInstance().ULog("On Movie Player Pause, pos : " + moviepos);
        super.onPause();
    }

    @Override
    public void onResume() {
        SDKManager.GetInstance().ULog("On Movie Player Resume, pos : " + moviepos);
        // Resume video player
        if(moviepos >= 0) {
            videoView.start();
            videoView.seekTo(moviepos);
            moviepos = -1;
        }

        super.onResume();
    }

    @Override
    public void onDestroy() {
        SDKManager.GetInstance().ULog("On Movie Player Destroy");
        super.onDestroy();
    }

    /**
     * 拦截返回键消息
     */
    @Override
    public void onBackPressed() { }

    public static Uri resourceToUri(Context context, int resID) {
        return Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE + "://" +
                context.getResources().getResourcePackageName(resID) + '/' +
                context.getResources().getResourceTypeName(resID) + '/' +
                context.getResources().getResourceEntryName(resID) );
    }
}
