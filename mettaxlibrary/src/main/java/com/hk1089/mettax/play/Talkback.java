package com.hk1089.mettax.play;

import android.os.Handler;
import android.os.Message;
import android.util.Log;


import com.babelstar.gviewer.NetClient;
import com.hk1089.mettax.listener.TalkConnectionListener;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

public class Talkback implements AudioPlay.AudioReader {
	
	private TalkListener mTalkListener = null;
	private	long mTalkbackHandle = 0;
	private TalkConnectionListener mConnectionListener;
	private String mLanIp;
	private Integer mLanPort;
	private Object lockRecord = new Object();	//锁
	private AudioRecordThread mRecordThread = null;	//视频界面更新线程
	private MyAudioRecord mAudioRecord = new MyAudioRecord();	//声音捕获对象
	private Object mLockAudioPlay = new Object();	//mAudioPlay的锁
	private AudioPlay mAudioPlay = new AudioPlay();
	private boolean mIsAudioPlay = false;
	private boolean mIsAudioSounding = false;	//是否已经播放声音了
	public static final int MSG_TALK_STOP = 1;
	private int mSampleRate = 8000;
	private boolean mIs1078Dev = false;
	public void setTalkListener(TalkListener listener) {
		mTalkListener = listener;
	}
	
	public void setLanInfo(String lanIp, Integer lanPort) {
		this.mLanIp = lanIp;
		this.mLanPort = lanPort;
	}

	public void setIs1078Dev(boolean is1078Dev) {
		this.mIs1078Dev = is1078Dev;
	}

	public void sendStopMsg() {
		Message message = Message.obtain();
		message.what  = MSG_TALK_STOP;
		mHandler.sendMessage(message);
	}
	public void setTalkConnectionListener(TalkConnectionListener listener) {
		this.mConnectionListener = listener;
	}
	public  void  setTalkSampleRate(int sampleRate){
		this.mSampleRate = sampleRate;
	}
	// handler对象，用来接收消息~
    private Handler mHandler = new Handler() {
    	@Override
    	public void handleMessage(Message msg) {
    		//这个是发送过来的消
    		// 处理从子线程发送过来的消息
    		if (msg.what == MSG_TALK_STOP) {
    			if (mTalkListener != null) {
    				mTalkListener.onStop();
    			}
    		}
    	};
    };
	

	public boolean startTalkback(String devIdno, int nChannel) {
    	boolean ret = false;
		if (mTalkbackHandle == 0) {
    		mTalkbackHandle = NetClient.TBOpenTalkback(devIdno, nChannel, 0);
    		
    		if (mLanIp != null) {
				NetClient.TBSetRealServer(mTalkbackHandle, mLanIp, mLanPort, "");
			}
			NetClient.TBStartTalkback(mTalkbackHandle, mIs1078Dev);
			NetClient.TBSetTalkbackMsgCallBack(mTalkbackHandle);

			if (mTalkbackHandle != 0) {
				playSound();
				if (startRecord()) {
					ret = true;
				} else {
					stopTalkback();
				}
			}
		}
		return ret;
	}

	public boolean stopTalkback() {
		boolean ret = false;
    	if (mTalkbackHandle != 0) {
    		stopSound();
    		stopRecord();
			NetClient.TBStopTalkback(mTalkbackHandle);
			NetClient.TBCloseTalkback(mTalkbackHandle);
			mTalkbackHandle = 0;
			if (mConnectionListener != null) {
				mConnectionListener.onCallDisconnected(); // Trigger the callback
			}
			ret = true;
		}
    	return ret;
	}

	protected boolean getWavFormat() {
		int format[] = new int[4];
		if (NetClient.NET_SUCCESS == NetClient.TBGetWavFormat(mTalkbackHandle, format)) {
			mAudioPlay.setWavFormat(format[0], format[1], format[2], format[3]);
			//mAudioRecord.setFormat(1, 8000);		//aac 16k,24k会出问题
//			mAudioRecord.setFormat(1, mSampleRate);		//aac 16k,24k会出问题
			//mAudioRecord.setFormat(1, format[0]);
			return true;
		} else {
			return false;
		}
	}
	

	public void playSound() {
		if (mTalkbackHandle != 0) {
			synchronized (mLockAudioPlay) {
				mIsAudioPlay = true;
				mIsAudioSounding = false;
				initSound();
			}
		}
	}

	public void stopSound() {
		if (mAudioPlay != null) {
			mAudioPlay.stopSound();
			mIsAudioPlay = false;
		}
	}

	public void initSound() {
		if (getWavFormat()) {
			mIsAudioSounding = true;
			mAudioPlay.playSound(Talkback.this);
			Log.d("Talkbak", "initSound");
			if (mConnectionListener != null) {
				mConnectionListener.onCallConnected(); // Trigger the callback
			}
//			mAudioRecord.startRecord();
		}
	}

	public int onReadWavData(byte[] pWavBuf, int nWavLen){
		int nReadLen = NetClient.TBGetWavData(mTalkbackHandle, pWavBuf, nWavLen);
		return nReadLen;
	}

	public boolean startRecord() {
		boolean ret = false;
		if (mTalkbackHandle != 0) {
			mAudioRecord.setFormat(1, mSampleRate);		//aac 16k,24k会出问题
			ret = mAudioRecord.startRecord();
//			ret = true;
			if (ret) {
				startRecordThread();
			}
		}
		return ret;
	}

	public void stopRecord() {
		if (mAudioRecord != null) {
			stopRecordThread();
			
			mAudioRecord.stopRecord();
		}
	}


	class AudioRecordThread extends Thread{
		private boolean isExit = false;
		
		public void setExit(boolean isExit) {
			this.isExit = isExit;
		}
		
		public void run()   {
			int nBuffSize = 320;
			//int nBuffSize = 160;
			byte[] audiodata = new byte[nBuffSize]; 
			while (!isExit) {
				try {
					Thread.sleep(10);

					int nReadLen = 0;
					synchronized (lockRecord) {
						if (mAudioRecord != null) {
							nReadLen = mAudioRecord.readAudioData(audiodata, nBuffSize);
						}
					}
					if ( nReadLen > 0) {
//						// 近端录制声音 109 340	
//						int nOutLen = mAecm.AecmDoData(audiodata, nBuffSize, mOut, 20);
////						NetClient.DOAECM(audiodata, nBuffSize, mOut, 20);
//						nReadLen = NetClient.TBSendWavData(mTalkbackHandle, mOut, nOutLen);
						
						nReadLen = NetClient.TBSendWavData(mTalkbackHandle, audiodata, nBuffSize);
					}
					
					synchronized (mLockAudioPlay) {
			     		if (mIsAudioPlay && !mIsAudioSounding) {
			     			initSound();
			     		}
			     	}
				} catch (InterruptedException e) {
					e.printStackTrace();
				} 
				if (NetClient.TBGetStop(mTalkbackHandle) == NetClient.NET_SUCCESS) {
					sendStopMsg();
					break;
				}
			}
			this.isExit = true;
		}
	}
	
	public static float[] asFloatArray(byte[] input){
		if(null == input ){
			return null;
		}
		FloatBuffer buffer = ByteBuffer.wrap(input).asFloatBuffer();
		float[] res = new float[buffer.remaining()];
		buffer.get(res);
		return res;
	}

	private void startRecordThread() {
		if (mRecordThread == null) {
			mRecordThread = new AudioRecordThread();
			mRecordThread.start();
		}
	}

	private void stopRecordThread() {
		if (mRecordThread != null) {
			Thread dummy = mRecordThread;
			mRecordThread.setExit(true);
			mRecordThread = null;
			dummy.interrupt();
		}
	}

    public boolean isTalkback() {
    	boolean ret = false;
    	if (this.mTalkbackHandle != 0) {
    		ret = true;
    	}
    	return ret;
    }
    
    public static interface TalkListener {

		public abstract void onStop();
    }
}
