package com.hk1089.mettax.video;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.DisplayMetrics;
import android.util.Log;

import com.babelstar.gviewer.NetClient;
import com.hk1089.mettax.play.AudioPlay;
import com.hk1089.mettax.play.AudioPlay.AudioReader;
import com.hk1089.mettax.video.VideoView.VViewListener;


public class Playback extends UpdateThread implements AudioReader, VViewListener {

	//protected static final Logger logger = LoggerFactory.getLogger();
	private boolean isLoading = false;	//是否加载成功
	private boolean isBeginPlay = false;	//是否开始播放
	private Object lockHandle = new Object();	//RealHandle的锁
	private long mPlayHandle = 0;	//实时预览对象
	private int mPlayRate = 0;		//实时预览的码率
	//private String mFileInfo = null;
	private long mInvalidateTime = 0;			//刷新时间
	private long mUpdateTime = 0;
	private String mDevIdno;
	private Context mContext = null;
	private VideoView mVideoView = null;
	private VideoDraw mVideoDraw = null;
	private AudioPlay mAudioPlay = null;

	private PlaybackListener mPlayListener = null;

	private String mLanIp;
	private Integer mLanPort;
	private boolean mIsPause = false;	//是否暂停
	private int mBegMinSecond = 0;
	private int mEndMinSecond = 0;
	public Playback(Context context) {
		mContext = context;
	}
	
	public void setVideoView(VideoView videoView) {
		mVideoView = videoView;
		mVideoView.setVideoListener(this);
		// Also update VideoDraw if it exists (for fullscreen transitions)
		if (mVideoDraw != null) {
			mVideoDraw.setVideoView(mVideoView);
			// Force invalidate to ensure smooth transition
			mVideoDraw.postInvalidate();
		}
	}
	
	public void setPlayerListener(PlaybackListener playListener) {
		mPlayListener = playListener;
	}

	public void setPlayerDevIdno(String devIdno) {
		if(devIdno.length() > 32){
			devIdno = "123456789";
		}
		mDevIdno = devIdno;

	}

	public void setLanInfo(String lanIp, Integer lanPort) {
		this.mLanIp = lanIp;
		this.mLanPort = lanPort;
	}

	public void setBegAndEndMin(int nBegMinSecond, int nEndMinSecond) {
		this.mBegMinSecond = nBegMinSecond;
		this.mEndMinSecond = nEndMinSecond;
	}

	public boolean StartVod(byte[] fileInfo, int nFileInfoLength, int channel) {
    	//先停止
    	StopVod();
		boolean ret = false;
    	//启动视频预览
    	synchronized (lockHandle) {
			if (mVideoDraw == null) {
				mVideoDraw = new VideoDraw();
				mVideoDraw.setVideoView(mVideoView);
			}
			mUpdateTime = System.currentTimeMillis();
			//缓存文件需要用
			// 此接口。不然部分手机回放异常（小米）
			String tmpPath = mContext.getCacheDir().getPath() + "/";
			Log.d("cmsv7", "StartVod tmpPath: " + tmpPath);
//			String tmpPath = Environment.getExternalStorageDirectory().getPath() + "/";
			mPlayHandle = NetClient.PBOpenPlayBackEx(tmpPath);
			if(mLanIp != null){
				NetClient.PBSetRealServer(mPlayHandle, mLanIp, mLanPort, "");
			}
			NetClient.PBStartPlaybackEx(mPlayHandle, fileInfo, nFileInfoLength, channel, mBegMinSecond, mEndMinSecond, 0, mDevIdno);
			if (mPlayHandle != 0) {
				isLoading = true;
				isBeginPlay = false;
				mVideoDraw.postInvalidate();
				ret = true;
			}


    	}
		if (ret) {
			startUpdateThread();
		}

    	return ret;
    }
    
    public boolean StopVod() {
    	boolean ret = false;
    	synchronized (lockHandle) {
	    	if (mPlayHandle != 0) {
				NetClient.PBStopPlayback(mPlayHandle);
				NetClient.PBClosePlayback(mPlayHandle);
				mPlayHandle = 0;
				isLoading = false;
				mPlayRate = 0;
				mVideoDraw.resetVideoBuf();
				mVideoDraw.postInvalidate();
				ret = true;
				
				stopSound();
				
				stopUpdateThread();
			}
    	}
    	return ret;
    }
    
    /*
     * 判断是否正在预览
     */
    public boolean isViewing() {
    	boolean ret = false;
    	
    	synchronized (lockHandle) 
    	{
    		if (this.mPlayHandle != 0) {
    			ret = true;
    			//logger.debug("Playback  isViewing");
    		}
    	}
    	return ret;
    }
    
    /*
     * 判断是否正在预览
     */
    protected void updateView() {
     	synchronized (lockHandle) {
    		if (this.mPlayHandle != 0) {
    			boolean isPostInvalidate = false;
     			boolean suc = (NetClient.PBGetRPlayStatus(this.mPlayHandle) == NetClient.NET_SUCCESS ? true : false);
    			if (isLoading != !suc) {
    				mVideoDraw.postInvalidate();
    				isLoading = !suc;
    				isPostInvalidate = true;
    			}
    			if (!isLoading) {
    				int[] videoSize = new int[2];
    				videoSize[0] = 0;
    				videoSize[1] = 0;
    				if (NetClient.PBGetRPlayImage(this.mPlayHandle, mVideoDraw.getVideoRgbLength()
    						, mVideoDraw.getVideoPixel(), videoSize, mVideoDraw.getVideoRgbFormat()) != NetClient.NET_SUCCESS) {
    					if (videoSize[0] > 0 && videoSize[1] > 0) {
    						mVideoDraw.initVideoBuf(videoSize[0], videoSize[1]);
    					}
    				} else {
    					mVideoDraw.setVideoBmpValid(true);
    					mVideoDraw.postInvalidate();
    					isPostInvalidate = true;
    					if (!isBeginPlay) {
    						isBeginPlay = true;
    						if (mPlayListener != null) {
    	    					mPlayListener.onBeginPlay();
    	    				}
    					}
    				}
    			}
    			
    			mPlayRate = NetClient.GetRPlayRate(this.mPlayHandle);
    			if (isPostInvalidate) {
    				mInvalidateTime = System.currentTimeMillis();
    			} else {
    				if ((System.currentTimeMillis() - mInvalidateTime) > 1000) {
    					mVideoDraw.postInvalidate();
    					mInvalidateTime = System.currentTimeMillis();
    				}
    			}
    		}
    	}
    }
	
	/*
	 * 判断是否正在播放声音
	 */
	public boolean isSounding() {
		if (mPlayHandle != 0 && mAudioPlay != null) {
			return true;
		} else {
			return false;
		}
	}

	/*
	 * 取得音频格式
	 */
	protected boolean getWavFormat() {
		int format[] = new int[4];
		if (NetClient.NET_SUCCESS == NetClient.PBGetWavFormat(mPlayHandle, format)) {
			mAudioPlay.setWavFormat(format[0], format[1], format[2], format[3]);
			//	mAudioRecord.setFormat(1, 8000);		//aac 16k,24k会出问题
			return true;
		} else {
			return false;
		}
	}

	/*
	 * 开始声音播放
	 */
	public void playSound() {
		if (mPlayHandle != 0) {
			if (mAudioPlay == null) {
				mAudioPlay = new AudioPlay();
				if(getWavFormat()){
					//logger.debug("Playback  playSound");
					mAudioPlay.playSound(this);
				}
			}
		}
	}
	
	/*
	 * 停止声音播放
	 */
	public void stopSound() {
		if (mAudioPlay != null) {
			//logger.debug("Playback  stopSound");
			mAudioPlay.stopSound();
			mAudioPlay = null;
		}
	}
	
	/*
	 * 移动播放位置
	 */
	public void setPlayTime(int second) {
		NetClient.PBSetPlayTime(mPlayHandle, second * 1000);
	}
	/*
	 * 判断是否正在预览
	 */
	public boolean ismIsPause() {
		boolean ret = false;

		synchronized (lockHandle)
		{
			if (this.mPlayHandle != 0) {
				ret = mIsPause;
				//logger.debug("Playback  isViewing");
			}
		}
		return ret;
	}

	
	/*
	 * 暂停播放
	 */
	public void pause(boolean isPause) {
		mIsPause = isPause;
		NetClient.PBPause(mPlayHandle, isPause ? 1 : 0);
	}

	/*
	 * 快进 快退
	 */
	public void setPlayRate(int nPlayRate) {
		synchronized (lockHandle){
			if (this.mPlayHandle != 0) {
				NetClient.PBSetPlayRate(mPlayHandle, nPlayRate);
			}
		}

	}

	/*
	 * 帧进
	 */
	public void FrameOneByOne() {
		synchronized (lockHandle){
			if (this.mPlayHandle != 0) {
				NetClient.PBFrameOneByOne(mPlayHandle);
			}
		}
	}

	
	/*
	 * 保存成图片
	 */
	public boolean savePngFile(String fileName) {
		if (NetClient.NET_SUCCESS == NetClient.PBCaptureBMP(mPlayHandle, fileName)) {
    		return true;
    	} else {
    		return false;
    	}
	}
    
	/*
	 * 返回获取到的大小
	 */
	public int onReadWavData(byte[] pWavBuf, int nWavLen) {
		int nReadLen = 0;
		synchronized (lockHandle) {
			nReadLen = NetClient.PBGetWavData(mPlayHandle, pWavBuf, nWavLen);
		}
		return nReadLen;
	}
	
	/*
	 * 进行画图操作
	 */
	public void onDrawVideo(Canvas canvas, int width, int height, Paint paint) {
		boolean drawVideo = false;
		if (mVideoDraw != null) {
			drawVideo = mVideoDraw.onDrawVideo(canvas, width, height, paint);
		}
		
		boolean isPlaying = mPlayHandle != 0 ? true : false;
		boolean isPlayFinished = NetClient.PBIsPlayFinished(mPlayHandle) == NetClient.NET_SUCCESS ? true : false;
		boolean isDownFinished = NetClient.PBIsDownFinished(mPlayHandle) == NetClient.NET_SUCCESS ? true : false;
		if (drawVideo && isPlaying && !isDownFinished) {
			String title = "";
			if (isViewing()) {
				int nRate = this.mPlayRate;
				//显示码率
				title += Integer.toString(nRate) + "KB/S";
			}
			
			if (!title.isEmpty()) {
				paint.setStyle(Paint.Style.FILL);
				paint.setColor(Color.rgb(255, 255, 255));
				paint.setTextAlign(Paint.Align.LEFT);
		        
		        DisplayMetrics dm = mContext.getApplicationContext().getResources().getDisplayMetrics();
		        //((Activity)mContext).getWindowManager().getDefaultDisplay().getMetrics(dm);
		        int top = 120;
		        int left = 16;
		        int textSize = 16;
		        if ( dm.widthPixels <= 400) {
		        } else if ( dm.widthPixels <= 740) {
		        	top = 40;
		        	left = 20;
		        	textSize = 24;
		        } else {
		        	//top = 100;
		        	left = 24;
		        	textSize = 32;
		        }
		        paint.setTextSize(textSize);
			    canvas.drawText(title, left, top, paint);
			}
		}
		
		if ((System.currentTimeMillis() - mUpdateTime) >= 1000) {
			if (mPlayListener != null) {
				int playTime = 0;
				int downTime = 0;
				
				playTime = NetClient.PBGetPlayTime(mPlayHandle);
				downTime = NetClient.PBGetDownTime(mPlayHandle);
				if (isPlaying) {
					mPlayListener.onUpdatePlay(downTime/1000, playTime/1000);
				}
				
				if (isPlayFinished) {
					mPlayListener.onEndPlay();
				}
			}
			
			mUpdateTime = System.currentTimeMillis();
		}
	}
	
	/*
	 * 点击操作
	 */
	public void onClick(VideoView view, int index) {
		if (mPlayListener != null) {
			mPlayListener.onClick(view, index);
		}
	}
	
	/*
	 * 双击操作
	 */
	public void onDbClick(VideoView view, int index) {
		if (mPlayListener != null) {
			mPlayListener.onDbClick(view, index);
		}
	}
	
	/*
	 * 左移操作
	 */
	public void onMoveLeft(VideoView view, int index) {
	}
	
	/*
	 * 右移操作
	 */
	public void onMoveRight(VideoView view, int index) {
	}
	
	/*
	 * 左移操作
	 */
	public void onMoveUp(VideoView view, int index) {
	}
	
	/*
	 * 停止操作
	 */
	public void onMoveStop(VideoView view, int index) {
	}
	
	/*
	 * 右移操作
	 */
	public void onMoveDown(VideoView view, int index) {
	}
	
	public static interface PlaybackListener {
		/*
		 * 开始时行播放
		 */
		public abstract void onBeginPlay();
		
		/*
		 * 双击操作
		 */
		public abstract void onUpdatePlay(int nDownSecond, int nPlaySecond);
		
		/*
		 * 播放结束
		 */
		public abstract void onEndPlay();
		
		/*
		 * 点击操作
		 */
		public abstract void onClick(VideoView view, int index);
		
		/*
		 * 双击操作
		 */
		public abstract void onDbClick(VideoView view, int index);
	}
}
