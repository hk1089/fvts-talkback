package com.hk1089.mettax.video;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Environment;
import android.util.DisplayMetrics;

import com.babelstar.gviewer.NetClient;
import com.hk1089.mettax.play.AudioPlay;
import com.hk1089.mettax.utils.DateUtil;


import java.io.File;
import java.io.UnsupportedEncodingException;
import java.util.Date;

public class RealPlay extends UpdateThread implements AudioPlay.AudioReader, VideoView.VViewListener {

	public static final String RECORD_DIRECTORY = "gStorage/record";
	public static String RECORD_PATH = "";
	public static final String SNAPSHOT_DIRECTORY = "gStorage/snapshot";
	public static String SNAPSHOT_PATH = "";
	
	private boolean isLoading = false;	//是否加载成功
	private boolean isBeginPlay = false;	//是否开始播放
	private Object lockHandle = new Object();	//RealHandle的锁
	private long mRealHandle = 0;	//实时预览对象
	private int mRealRate = 0;		//实时预览的码率
	private boolean mRecording = false;	//录像状态
	private String mDevName;		//设备名称
	private String mDevIdno;		//设备编号信息
	private int mChannel = 0;		//通道号信息
	private String mChnName = "";		//通道名称
	private String mLanIp;
	private Integer mLanPort;
	private long mInvalidateTime = 0;			//刷新时间
	private int mRealCountDown = 0;		//实时预览倒计时分钟

	private Context mContext = null;
	private VideoView mVideoView = null;

	private VideoDraw mVideoDraw = new VideoDraw();
	private Object mLockAudioPlay = new Object();	//mAudioPlay的锁
	private AudioPlay mAudioPlay = new AudioPlay();
	private boolean mIsAudioPlay = false;
	private boolean mIsAudioSounding = false;	//是否已经播放声音了
	private PlayListener mPlayListener = null;
	private boolean mPtzCtrl;	
	private boolean mUpdateView = true;
	private byte[] mRecFilePath = new byte[1024];
	private String RECORD_FILE_PATH = null;
	private int mStreamType = 1; //码流类型 默认子码流：1 主码流：0
	private int mProtocolType = 0; //协议类型 1:私有协议 0 rpts协议

	private int mIsUser = 0; // 0 使用 1 禁用
	public RealPlay(Context context) {
		mContext = context;
		RECORD_PATH = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES) + "/" + RECORD_DIRECTORY;
		SNAPSHOT_PATH = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + "/" + SNAPSHOT_DIRECTORY;
	}
	

	
	public void setContext(Context context) {
		this.mContext = context;
	}
	
	public void setViewInfo(String devName, String devIdno, int channel, String chnName, int isUser) {
		this.mDevName = devName;
		this.mDevIdno = devIdno;
		this.mChannel = channel;
		this.mChnName = chnName;
		this.mIsUser = isUser;
	}
	
	public void setLanInfo(String lanIp, Integer lanPort) {
		this.mLanIp = lanIp;
		this.mLanPort = lanPort;
	}
	
	public void setVideoView(VideoView videoView) {
		mVideoView = videoView;
		if (videoView != null) {
			mVideoView.setVideoListener(this);
		}
		mVideoDraw.setVideoView(mVideoView);
	}


	
	public void setVideoBmpExtend(boolean isExtend){
		
		mVideoDraw.setVideoBmpExtend(isExtend);
	}
	
	public void setRealCountTime(int countTime){
		mRealCountDown = countTime;
	}

	public void setPtzCtrlStatus(boolean ptzctrl) {
		mPtzCtrl = ptzctrl;
	}
	
	public void setPlayerListener(PlayListener playListener) {
		mPlayListener = playListener;
	}
	
	//
	public void setUpdateViewStatus(boolean updateView){
		mUpdateView = updateView;
	}
	
	public void getRecFullFilePath(){		
		if (mRealHandle != 0) {
			NetClient.RPlayGetFileFullPath(mRealHandle, mRecFilePath, 1024);
			try {
				RECORD_FILE_PATH = new String(mRecFilePath,"UTF-8");
			} catch (UnsupportedEncodingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
				
	}
	
	public String getSaveRecFilePath(){
		
		return RECORD_FILE_PATH;				
	}

	public void setStreamType(int streamType) {
		if(streamType > 1 || streamType < 0){
			streamType = 1;
		}
		mStreamType = streamType;
	}

	public void setmProtocolType(int protocolType) {
		if(protocolType != 1){
			protocolType = 0;
		}
		mProtocolType = protocolType;
	}

	public boolean StartAV(boolean autoSelect, boolean realMode) {
    	//先停止
    	StopAV();
    	//判断通道号是否停用
		if(mIsUser > 0){
			return false;
		}
    	mVideoDraw.setVideoView(mVideoView);
    	boolean ret = false;
    	//启动视频预览
    	synchronized (lockHandle) {

			//子码流
//			mRealHandle = NetClient.OpenRealPlay(mDevIdno, mChannel, 1, 0);
			//主码流
    		//mRealHandle = NetClient.OpenRealPlay(mDevIdno, mChannel, 0, 1);
			if(mStreamType == 1){
				mRealHandle = NetClient.OpenRealPlay(mDevIdno, mChannel, 1, 0);
			}else{
				mRealHandle = NetClient.OpenRealPlay(mDevIdno, mChannel, 0, 0);
			}
			if (mRealHandle != 0){
				NetClient.SetVideoProcol(mRealHandle, mProtocolType);
			}
			if (mLanIp != null) {
				NetClient.SetRealAddress(mRealHandle, mLanIp, mLanPort);
			}
			NetClient.StartRealPlay(mRealHandle);
			if (realMode) {
				NetClient.SetStreamMode(mRealHandle, NetClient.GPS_STREAM_MODE_REAL);
			}

			if (mRealHandle != 0) {
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
    
    public boolean StopAV() {
    	boolean ret = false;
    	synchronized (lockHandle) {
	    	if (mRealHandle != 0) {
	    		stopRecord();
	    		
				NetClient.StopRealPlay(mRealHandle);
				NetClient.CloseRealPlay(mRealHandle);
				mRealHandle = 0;
				isLoading = false;
				mRealRate = 0;
				mRecording = false;
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
    	synchronized (lockHandle) {
    		if (this.mRealHandle != 0) {
    			ret = true;
    		}
    	}
    	return ret;
    }
    
    /*
     * 判断是否正在预览
     */
    protected void updateView() {
    	if(mUpdateView){
    		synchronized (lockHandle) {
        		if (this.mRealHandle != 0) {
        			boolean isPostInvalidate = false;
        			boolean suc = (NetClient.GetRPlayStatus(this.mRealHandle) == NetClient.NET_SUCCESS ? true : false);
        			//boolean isGetRate = true;
        			if (isLoading != !suc) {
        				mVideoDraw.postInvalidate();
        				isLoading = !suc;
        				isPostInvalidate = true;
        			}
        			if (!isLoading) {
        				
        				int[] videoSize = new int[2];
        				videoSize[0] = 0;
        				videoSize[1] = 0;
     
        				if (NetClient.GetRPlayImage(this.mRealHandle, mVideoDraw.getVideoRgbLength()
        						, mVideoDraw.getVideoPixel(), videoSize, mVideoDraw.getVideoRgbFormat()) != NetClient.NET_SUCCESS) {
        					if (videoSize[0] > 0 && videoSize[1] > 0) {
        						mVideoDraw.initVideoBuf(videoSize[0], videoSize[1]);         						
        					}
        					
        				} else {
        					mVideoDraw.setVideoBmpValid(true);
//							mVideoDraw.feedData();
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
        			
        			mRealRate = NetClient.GetRPlayRate(this.mRealHandle);        			        		
        			if (mRealRate > 1000) {
        				mRealRate = 0;
        			}
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
         	synchronized (mLockAudioPlay) {
         		if (mIsAudioPlay && !mIsAudioSounding) {
         			if (getWavFormat()) {
        				mIsAudioSounding = true;
        				mAudioPlay.playSound(this);
        			}
         		}
         	}
    	}
     	
    }
    
    /*
	 * 云台控制
	 */
	public void ptzControl(int command, int param) {
		synchronized (lockHandle) {
			NetClient.RPlayPtzCtrl(mRealHandle, command, NetClient.GPS_PTZ_SPEED_DEFAULT, param);
		}
	}
	
	 /*
     * 判断是否正在预览
     */
    public boolean isRecording() {
    	boolean ret = false;
    	synchronized (lockHandle) {
    		ret = mRecording;
    	}
    	return ret;
    }
	
	/*
	 * 启动或停止录像
	 */
	public void record() {
		synchronized (lockHandle) {
			if (mRealHandle != 0) {
				if (!mRecording) {
					NetClient.RPlayStartRecord(mRealHandle, RECORD_PATH, mDevName);
				} else {
					NetClient.RPlayStopRecord(mRealHandle);
				}
				mRecording = !mRecording;
			}
		}
	}
	
	/*
	 * 启动录像
	 */
	public boolean startRecord() {
		synchronized (lockHandle) {
			if (mRealHandle != 0) {
				if (!mRecording) {
					NetClient.RPlayStartRecord(mRealHandle, RECORD_PATH, mDevName);
					mRecording = true;
				} 
			}
		}
		return mRecording;
	}
	
	/*
	 * 停止录像
	 */
	public boolean stopRecord() {
		boolean ret = false;
		synchronized (lockHandle) {
			if (mRealHandle != 0) {
				if (mRecording) {
					NetClient.RPlayStopRecord(mRealHandle);
					mRecording = false;
					ret = true;
				} 
			}
		}
		return ret;
	}
	
	/*
	 * 判断是否正在播放声音
	 */
	public boolean isSounding() {
		if (mRealHandle != 0 && mIsAudioPlay) {
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
		if (NetClient.NET_SUCCESS == NetClient.GetWavFormat(mRealHandle, format)) {
			mAudioPlay.setWavFormat(format[0], format[1], format[2], format[3]);
			return true;
		} else {
			return false;
		}
	}
	
	/*
	 * 开始声音播放
	 */
	public void playSound() {
		if (mRealHandle != 0) {
			NetClient.RPlayPlaySound(mRealHandle, 1);
			synchronized (mLockAudioPlay) {
				mIsAudioPlay = true;
				mIsAudioSounding = false;
				if (getWavFormat()) {
					mIsAudioSounding = true;
					mAudioPlay.playSound(this);
				}
			}
		}
	}
	
	/*
	 * 开始声音播放
	 */
	public void stopSound() {
		synchronized (mLockAudioPlay) {
			mAudioPlay.stopSound();
			mIsAudioPlay = false;
		}
	}
	
	/*
	 * 保存成图片
	 */
	public boolean savePngFile(String fileName) {
		if (NetClient.NET_SUCCESS == NetClient.CaptureBMP(mRealHandle, fileName)) {
    		return true;
    	} else {
    		return false;
    	}
	}
	
	/*
     * 保存成BMP图片
     */
    public boolean savePngFile() {
    	boolean isSuccess = false;
    	createSnapshotPath();
    	
    	File file = new File(SNAPSHOT_PATH);
    	String pngName = file.getPath() + "/" + mDevName + " - " + mChnName + " - " + DateUtil.dateSwitchStringEx(new Date()) + ".jpg";
    	if(savePngFile(pngName)){
			isSuccess = true;
		}
    	return isSuccess;
    }
    
    static public String getDeviceTitlePage(String devIdno) {
    	File file = new File(SNAPSHOT_PATH);
    	String pngName = file.getPath() + "/" + devIdno + ".bmp";
    	return pngName;
    }
    
    static public void createSnapshotPath() {
    	File file = new File(SNAPSHOT_PATH);  
    	if(!file.exists())  {
    	    file.mkdirs();  
    	}
    }
	
    /*
     * 保存成封面图片
     */
    public boolean saveAsTitlePage() { 
    	createSnapshotPath();
  
    	String pngName = getDeviceTitlePage(mDevIdno);
    	return savePngFile(pngName);
    }
    
	/*
	 * 返回获取到的大小
	 */
	public int onReadWavData(byte[] pWavBuf, int nWavLen) {
		int nReadLen = 0;
		synchronized (lockHandle) {
			nReadLen = NetClient.GetWavData(mRealHandle, pWavBuf, nWavLen);
		}
		return nReadLen;
	}
	/*
	 * 进行画图操作
	 */
//	public void onDrawVideo(){
////		boolean drawVideo = false;
//////		if (mVideoDraw != null) {
//////			drawVideo = mVideoDraw.onDrawVideo();
//////		}
////	}
	/*
	 * 进行画图操作
	 */
	public void onDrawVideo(Canvas canvas, int width, int height, Paint paint) {
		boolean drawVideo = false;
		if (mVideoDraw != null) {
			drawVideo = mVideoDraw.onDrawVideo(canvas, width, height, paint);
		}
		//if (drawVideo) {
			String title = "";

			if (!mChnName.isEmpty()) {
				title = mChnName + "  ";
			} else {
				title = String.format("%d", mVideoView.getIndex() + 1);
			}
			if (isViewing()) {
				
				int nRate = this.mRealRate;
				//显示码率
				title += Integer.toString(nRate) + "KB/S";
				//显示倒计时（如果有）
				if(mRealCountDown > 0){
					title += "  --" + Integer.toString(mRealCountDown);
				}
			}
			
			if (!title.isEmpty()) {
				paint.setStyle(Paint.Style.FILL);
//				paint.setColor(Color.rgb(0x4c, 0x53, 0x66));
				paint.setColor(Color.rgb(255, 255, 255));
				paint.setTextAlign(Paint.Align.LEFT);
		        
		        DisplayMetrics dm = mContext.getApplicationContext().getResources().getDisplayMetrics();
		        int top = 60;
		        int left = 16;
		        int textSize = 16;
		        if (dm.widthPixels <= 400) {
		        } else if ( dm.widthPixels <= 740) {
		        	top = 40;
		        	left = 20;
		        	textSize = 24;
		        } else {
		        	//top = 100;
		        	top = 30;
		        	left = 24;
		        	//textSize = 32;
		        	textSize = 24;
		        }
		        paint.setTextSize(textSize);
			    canvas.drawText(title, left, top, paint);
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
	 * 处理PTZ操作
	 */
	protected void handelPtzStop(VideoView view, int index) {
		if (mPlayListener != null) {
			mPlayListener.onPtzCtrl(view, index);
		}
	}
	
	/*
	 * 左移操作
	 */
	public void onMoveLeft(VideoView view, int index) {
		if(mPtzCtrl){
			ptzControl(NetClient.GPS_PTZ_MOVE_LEFT, 0);
			handelPtzStop(view, index);
		}
		if (mPlayListener != null) {
			mPlayListener.onMoveLeft(view, index);
		}
	}
	
	/*
	 * 右移操作
	 */
	public void onMoveRight(VideoView view, int index) {
		if(mPtzCtrl){
			ptzControl(NetClient.GPS_PTZ_MOVE_RIGHT, 0);
			handelPtzStop(view, index);
		}

		if (mPlayListener != null) {
			mPlayListener.onMoveRight(view, index);
		}
	}
	
	/*
	 * 左移操作
	 */
	public void onMoveUp(VideoView view, int index) {
		if(mPtzCtrl){
			ptzControl(NetClient.GPS_PTZ_MOVE_TOP, 0);
			handelPtzStop(view, index);
		}
		

	}
	
	/*
	 * 右移操作
	 */
	public void onMoveDown(VideoView view, int index) {
		if(mPtzCtrl){
			ptzControl(NetClient.GPS_PTZ_MOVE_BOTTOM, 0);
			handelPtzStop(view, index);
		}		

	}
	
	/*
	 * 停止操作
	 */
	public void onMoveStop(VideoView view, int index) {
		if(mPtzCtrl){
			ptzControl(NetClient.GPS_PTZ_MOVE_STOP, 0);
		}

		//handelPtzStop(view, index);
	}
	
	public static interface PlayListener {
		/*
		 * 开始时行播放
		 */
		public abstract void onBeginPlay();
		
		/*
		 * PTZ控制
		 */
		public abstract void onPtzCtrl(VideoView view, int index);
		
		/*
		 * 点击操作
		 */
		public abstract void onClick(VideoView view, int index);
		
		/*
		 * 双击操作
		 */
		public abstract void onDbClick(VideoView view, int index);
		
		/*
		 * 左移
		 */
		public abstract void onMoveLeft(VideoView view, int index);
		
		/*
		 * 右移
		 */
		public abstract void onMoveRight(VideoView view, int index);
	}
}
