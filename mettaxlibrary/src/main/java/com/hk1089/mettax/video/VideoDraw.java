package com.hk1089.mettax.video;

import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;

import com.babelstar.gviewer.NetClient;

import java.nio.ByteBuffer;

public class VideoDraw {

	private Object lockDraw = new Object();	//RealHandle的锁
	private int	mVideoWidth = 0;			//视频宽度和高度
	private int mVideoHeight = 0;			//视频高度
	private int mVideoRgbLength = 0;		
	private int mVideoRgbFormat = NetClient.GPS_RGB_FORMAT_8888;
//	private int mVideoRgbFormat = NetClient.GPS_RGB_FORMAT_888;
//	private int mVideoRgbFormat = NetClient.GPS_YUV420P_FORMAT;
	private byte[] mVideoPixel = null;
	private ByteBuffer mVideoBuffer = null;
	private Bitmap mVideoBmp = null;
	private boolean isVideoBmpVaild = false;
	private final Paint prFramePaint = new Paint(Paint.FILTER_BITMAP_FLAG);
	private VideoView mVideoView = null;
	private Boolean mDrawScale = true;
	private boolean isVideoBmpExtend = false;



	public VideoDraw() {
	}


	public void setVideoView(VideoView videoView) {
		mVideoView = videoView;
	}
	
	public Boolean getDrawScale() {
		return mDrawScale;
	}

	public void setDrawScale(Boolean drawScale) {
		this.mDrawScale = drawScale;
	}
	
	/*
	 * 刷新界面
	 */
	public void postInvalidate() {
		if(mVideoRgbFormat == NetClient.GPS_YUV420P_FORMAT){

		}else{
			if (mVideoView != null) {
				mVideoView.postInvalidate();
			}
		}


	}
	
	public int getVideoRgbLength() {
		return mVideoRgbLength;
	}
	
	public int getVideoRgbFormat() {
		return mVideoRgbFormat;
	}
	
	public byte[] getVideoPixel() {
		return mVideoPixel;
	}
	
	public void setVideoBmpValid(boolean isValid) {
		isVideoBmpVaild = isValid;
	}
	
	public void setVideoBmpExtend(boolean isExtend){
		isVideoBmpExtend = isExtend;
	}

	/*
	 * 返回是否显示了视频信息
	 */
	public boolean onDrawVideo(Canvas canvas, int width, int height, Paint paint) {	    
		Rect rect = new Rect();
		rect.left = 0;
		rect.top = 0;
		rect.right = width;
		rect.bottom = height;
		canvas.drawRect(rect, paint);
		boolean drawVideo = false;
		synchronized (lockDraw) {
			if (isVideoBmpVaild && mVideoBmp != null) {
				drawVideo = true;
				Rect rc = new Rect();
				rc.left = 1;
				rc.top = 1;
				rc.right = width - 1;
				rc.bottom = height - 1;

				if (mDrawScale) {
					int nWndWidth = rc.right - rc.left;
					int nWndHeight = rc.bottom - rc.top;
					int nWndRate = (int)(nWndWidth * 100.0 / nWndHeight);
					int nRealRate = (int)(mVideoWidth * 100.0 / mVideoHeight);
					
					int nWidth = 0;
					int nHeight = 0;
					if (nWndRate > nRealRate) {
						nHeight = nWndHeight;
						nWidth = (int)(mVideoWidth * 1.0 * nHeight / mVideoHeight);
					} else {
						nWidth = nWndWidth;
						nHeight = (int)(mVideoHeight * 1.0 * nWidth / mVideoWidth);
					}
					
					if (nWidth > nWndWidth) {
						nWidth = nWndWidth;
					}
					
					if (nHeight > nWndHeight) {
						nHeight = nWndHeight;
					}
					
					Rect dest = new Rect();
					//dest.left = rc.left + (nWndWidth - nWidth) / 2;
					dest.left = rc.left;
					//dest.right = dest.left + nWidth;
					dest.right = dest.left + nWndWidth;
					
					if(isVideoBmpExtend){
						dest.top = rc.top + (nWndHeight - nHeight) / 8;
						dest.bottom = dest.top + nWndHeight - nHeight/8;						
					}else{
						dest.top = rc.top + (nWndHeight - nHeight) / 2;
						dest.bottom = dest.top + nHeight;
					}
					
					rc = dest;
					
				}
				
				Rect src = new Rect();
				src.left = 0;
				src.top = 0;
				src.bottom = mVideoHeight;
				src.right = mVideoWidth;
				
				paint.setStyle(Paint.Style.FILL);
				paint.setColor(Color.rgb(0x0, 0x0, 0x0));
				canvas.drawRect(rc, paint);
				
				mVideoBmp.copyPixelsFromBuffer(mVideoBuffer);
				mVideoBuffer.position(0);
		        canvas.drawBitmap(mVideoBmp, src, rc, prFramePaint);
		        
		        drawVideo = true;
			}
		}
		return drawVideo;
	}
	
	/*
     * 初始化图像
     */
    public void initVideoBuf(int nWidth, int nHeight) {
    	mVideoWidth = nWidth;
		mVideoHeight = nHeight;
		if(NetClient.GPS_YUV420P_FORMAT == mVideoRgbFormat){
			mVideoRgbLength = mVideoWidth * mVideoHeight * 3 / 2;
			mVideoPixel = new byte[mVideoRgbLength];
			mVideoBuffer = ByteBuffer.wrap( mVideoPixel );
			isVideoBmpVaild = false;
			return;
		}
		if (NetClient.GPS_RGB_FORMAT_8888 == mVideoRgbFormat) {
			mVideoRgbLength = mVideoWidth * mVideoHeight * 4;
			mVideoBmp = Bitmap.createBitmap(mVideoWidth, mVideoHeight, Config.ARGB_8888);
		} else if (NetClient.GPS_RGB_FORMAT_888 == mVideoRgbFormat) {
			mVideoRgbLength = mVideoWidth * mVideoHeight * 3;
			mVideoBmp = Bitmap.createBitmap(mVideoWidth, mVideoHeight, Config.ARGB_8888);
		} else if(NetClient.GPS_RGB_FORMAT_565 == mVideoRgbFormat){
			mVideoRgbLength = mVideoWidth * mVideoHeight * 2;
			mVideoBmp = Bitmap.createBitmap(mVideoWidth, mVideoHeight, Config.RGB_565);
		}
		
		mVideoPixel = new byte[mVideoRgbLength];
		mVideoBuffer = ByteBuffer.wrap( mVideoPixel );
		isVideoBmpVaild = false;
    }
    
    /*
     * 重置图像缓存
     */
    public void resetVideoBuf() {
    	mVideoWidth = 0;
		mVideoHeight = 0;
		mVideoRgbLength = 0;
		mVideoPixel = null;
		mVideoBuffer = null;
		if (mVideoBmp != null) {
			mVideoBmp.recycle();
		}
		mVideoBmp = null;
		isVideoBmpVaild = false;
    }
    
    /*
     * 保存成BMP图片
    public boolean savePngFile(String pngName) {    	              
    	boolean ret = false;
    	synchronized (lockDraw) {
		    try {  
		    	if (isVideoBmpVaild && mVideoBmp != null) {
//		    		BitmapUtil.save2BmpFile(mVideoBmp, pngName);
		    		FileOutputStream fos = new FileOutputStream(pngName);  
		    		mVideoBmp.compress(Bitmap.CompressFormat.JPEG, 100, fos);  
			        fos.close(); 
			        ret = true;
		    	}
		    } catch (Exception e) {  
		    	e.printStackTrace();  
		    }
    	}
	    return ret;
    }
     */
}
