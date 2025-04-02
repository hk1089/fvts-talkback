package com.hk1089.mettax.play;

public class AudioPlay {
//	protected static final Logger logger = LoggerFactory.getLogger();
	private Object lockAudio = new Object();	//RealHandle的锁
	private SoundThread mSoundThread = null;	//视频界面更新线程
	private MyAudioTrack mAudioTrack = null;	//声音播放对象
	private byte[] mWavBuffer = new byte[8192];	//声音缓存 // 16384 以前是8192 
	private AudioReader	mAudioReader = null;
	private AudioFormater mAudioFormater = null;
	private int mChannels = 1;
	private int mSamplePerSec = 8000;
	private int mBitPerSample = 16;
//	private WebRtcAecm mAudioPlayAecm = null;
	/*
	 * 设置音频参数
	 */
	public void setWavFormat(int nChannes, int nSamplePerSec, int nBitPerSample, int nBufLen) {
		mChannels = nChannes;
		mSamplePerSec = nSamplePerSec;
		mBitPerSample = nBitPerSample;
		mWavBuffer = new byte[nBufLen];	//声音缓存
		
//		nWavBufferTmpLen = 0;
//		logger.debug("AudioPlay  playSound nBufLen:" +nBufLen +"nSamplePerSec:"+nSamplePerSec+"nBitPerSample:"+nBitPerSample
//				+"mChannels:"+mChannels);
		
	}
	
//	public void setAudioPlayAecm(WebRtcAecm aecm){
//		
//		mAudioPlayAecm = aecm;
//	}
	/*
	 * 开始声音播放
	 */
	public void startPlaySound(AudioReader reader, AudioFormater formater) {
		if (mAudioTrack == null) {
			mAudioReader = reader;
			mAudioFormater = formater;
			//logger.debug("AudioPlay  startPlaySound nSamplePerSec:" +mSamplePerSec+"mBitPerSample:"+mBitPerSample+"mChannels:"+mChannels);
			startSoundThread();
		}
	}
	
	/*
	 * 开始声音播放
	 */
	public void playSound(AudioReader reader) {
		if (mAudioTrack == null) {
			mAudioReader = reader;
			mAudioTrack = new MyAudioTrack(mSamplePerSec, mChannels, mBitPerSample, mWavBuffer.length);
			//logger.debug("AudioPlay  playSound");
			mAudioTrack.init();			
			startSoundThread();
		}
	}
	
	/*
	 * 开始声音播放
	 */
	public void stopSound() {
		if (mAudioTrack != null) {
			//logger.debug("AudioPlay  stopSound");
			stopSoundThread();
			
			synchronized (lockAudio) {
				mAudioTrack.release();
				mAudioTrack = null;
			}
		}
	}
	
	/*
	 * 播放状态检测线程
	 */
	private class SoundThread extends Thread{
		private boolean isExit = false;
		
		public void setExit(boolean isExit) {
			this.isExit = isExit;
		}
		
		public void run()   {  
			while (!isExit) {
				try {
					Thread.sleep(10);

					if (mAudioTrack == null) {
						if (mAudioFormater != null) {
							if (mAudioFormater.doInitFormat()) {
								mAudioTrack = new MyAudioTrack(mSamplePerSec, mChannels, mBitPerSample, mWavBuffer.length);
								mAudioTrack.init();
							}
						}
					} else {
						int nReadLen = 0;
						if (mAudioReader != null) {
							nReadLen = mAudioReader.onReadWavData(mWavBuffer, mWavBuffer.length);
						}
						synchronized (lockAudio) {
							if (nReadLen > 0 && mAudioTrack != null) {
								
								//远端声音(正在播放的声音)	
//								NetClient.SETAECMFAREND(mWavBuffer, nReadLen);
//								if(mAudioPlayAecm != null){
//									mAudioPlayAecm.AecmFarentSet(mWavBuffer, nReadLen);
//									//logger.debug("AudioPlay  mAudioPlayAecm nReadLen:" +nReadLen);
//								}
								
								mAudioTrack.playAudioTrack(mWavBuffer, 0, nReadLen);
								
							}
						}
					}
					
					
				} catch (InterruptedException e) {
					e.printStackTrace();
				} 
			}
			this.isExit = true;
	    }
	}
	
	/*
	 * 开始声音播放线程
	 */
	private void startSoundThread() {
		if (mSoundThread == null) {
			mSoundThread = new SoundThread();
			mSoundThread.start();
		}
	}
	
	/*
	 * 停止声音播放线程
	 */
	private void stopSoundThread() {
		if (mSoundThread != null) {
			Thread dummy = mSoundThread;
			mSoundThread.setExit(true);
			mSoundThread = null;
			dummy.interrupt();
		}
	}

	public boolean isSounding() {
		return mAudioTrack != null ? true : false;
	}
	
	public static interface AudioFormater {
		/*
		 * 返回获取到的大小
		 */
		public abstract boolean doInitFormat();
	}
	
	public static interface AudioReader {
		/*
		 * 返回获取到的大小
		 */
		public abstract int onReadWavData(byte[] pWavBuf, int nWavLen);
	}
}
