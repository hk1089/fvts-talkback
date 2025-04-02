package com.hk1089.mettax;

import android.Manifest;
import android.content.Context;
import android.media.AudioManager;

import androidx.fragment.app.FragmentActivity;

import com.babelstar.gviewer.NetClient;
import com.hk1089.mettax.listener.PermissionListener;
import com.hk1089.mettax.listener.TalkConnectionListener;
import com.hk1089.mettax.play.Talkback;
import com.hk1089.mettax.utils.Extensions;

import java.util.ArrayList;
import java.util.List;

public class Channel {
    private Context mContext;
    private NetClient mNetClient;
    private String mServer;
    private String mDevIdno;
    private Talkback mTalkback;
    private boolean isConnected = false;
    public Channel(Context context) {
        mContext = context;
    }

    public boolean initialize(String ip, String id){
        mServer = ip;
        mDevIdno = id;
        String sdPath = mContext.getFilesDir().getAbsolutePath() + "/";
        mNetClient = new NetClient();
        mNetClient.Initialize(sdPath);
        mNetClient.SetJniEnv();
        return updateServer();
    }
    public void checkPermission(FragmentActivity activity, PermissionListener listener){
        List<String> permissions = new ArrayList<>();
        permissions.add(Manifest.permission.RECORD_AUDIO);
        permissions.add(Manifest.permission.MODIFY_AUDIO_SETTINGS);
        new Extensions().requestPermissions(activity, permissions, (allGranted, grantedList, deniedList) -> {
            listener.onResult(allGranted && deniedList.isEmpty());
        });
    }

    protected boolean updateServer() {
        mNetClient.SetDirSvr(mServer, mServer, 6605, 0);
        return true;
    }

    public boolean startCall(){
        onTalkStart();
        return true;
    }
    public boolean checkConnected(){
        return isConnected;
    }
    public boolean stopCall(){
        onTalkStop();
        mNetClient.UnInitialize();
        return true;
    }
    protected void onTalkStart() {
        if (mTalkback == null) {
            if (!updateServer()) {
                return;
            }

            mTalkback = new Talkback();
            mTalkback.startTalkback(mDevIdno, 1);
            mTalkback.setTalkConnectionListener(new TalkConnectionListener() {
                @Override
                public void onCallConnected() {
                    isConnected = true;
                }

                @Override
                public void onCallDisconnected() {
                    isConnected = false;
                }
            });
        }
    }

    protected void onTalkStop() {
        if (mTalkback != null) {
            mTalkback.stopTalkback();
            mTalkback = null;
        }
    }


    public void muteMicrophone(boolean mute) {
        AudioManager audioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
        if (audioManager != null) {
            audioManager.setMicrophoneMute(mute);
        }
    }
    public void muteSpeaker(boolean mute) {
        AudioManager audioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
        if (audioManager != null) {
            if (mute) {
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 0, 0);
            } else {
                int maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, maxVolume, 0);
            }
        }
    }

}
