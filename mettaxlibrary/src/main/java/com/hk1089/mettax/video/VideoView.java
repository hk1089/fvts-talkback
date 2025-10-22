package com.hk1089.mettax.video;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;

import androidx.appcompat.widget.AppCompatImageView;

import java.util.Timer;
import java.util.TimerTask;

public class VideoView extends AppCompatImageView {
	private Timer mMoveTimer = null;
	private Context	mContext;
	private GestureDetector	mVideoGesture;		//手势
	private Integer mIndex = 0;
	private Boolean mDrawFocus = false;
	private boolean mIsFocus;		//是否处于焦点
	private VViewListener mViewListener;
	private Paint mPaint = null;			//画图对象
	
	public VideoView(Context context, int index) {
	    super(context);
	    this.mIndex = index;
	    this.mContext = context;
		this.mVideoGesture = new GestureDetector(context, new VideoViewGestureListener());	
		initPaint();
	}

	public Boolean getDrawFocus() {
		return mDrawFocus;
	}

	public void setDrawFocus(Boolean drawFocus) {
		this.mDrawFocus = drawFocus;
	}

	public VideoView(Context context, AttributeSet attr) {
	    super(context, attr);
	    this.mContext = context;
		this.mVideoGesture = new GestureDetector(context, new VideoViewGestureListener());	
		initPaint();
	}



	public Integer getIndex() {
		return this.mIndex;
	}
	
	public void setIsFocus(boolean bIsFocus) {
		this.mIsFocus = bIsFocus;
	}
	
	public boolean getIsFocus() {
		return this.mIsFocus;
	}
	
	public void setVideoListener(VViewListener viewListener) {
		this.mViewListener = viewListener;
	}
	
	private void initPaint(){
		this.mPaint = new Paint();
		this.mPaint.setAntiAlias(true);
	    this.mPaint.setColor(-65536);
	    this.mPaint.setStyle(Paint.Style.STROKE);
	    this.mPaint.setStrokeWidth(1.0F);

	}

	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		if (mViewListener != null) {
			int Width = getWidth();
			int Height = getHeight();
			mViewListener.onDrawVideo(canvas, Width, Height, mPaint);

		}
		if (mDrawFocus) {
			Rect rect = new Rect();
			rect.left = 0;
			rect.top = 0;
			rect.right = getWidth() - 1;
			rect.bottom = getHeight() - 1;
//		    this.mPaint.setStyle(Paint.Style.STROKE);
			this.mPaint.setStyle(Paint.Style.STROKE);
			if (mIsFocus) {
				this.mPaint.setColor(Color.RED);
			} else {
				this.mPaint.setColor(Color.rgb(72, 72, 72));
			}
			canvas.drawRect(rect, this.mPaint);
		}
	}

//	public void draw(Canvas canvas) {
//	    super.draw(canvas);
//
//	}
	
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		mVideoGesture.onTouchEvent(event); //通知手势识别方法 
		return true;
	}
	
	class PtzStopTask extends TimerTask {
	    @Override
	    public void run() {
	    	if (mViewListener != null) {
        		mViewListener.onMoveStop(VideoView.this, getIndex());
        	}
	    }
	}
	
	private void runStopTimer() {
		if (mMoveTimer != null) {
			mMoveTimer.cancel();
			mMoveTimer.purge();
			mMoveTimer = null;
		}
		mMoveTimer = new Timer();
		mMoveTimer.schedule(new PtzStopTask(), 1500);
	}
	
	private class VideoViewGestureListener extends GestureDetector.SimpleOnGestureListener{
        @Override
        public boolean onSingleTapUp(MotionEvent e) {
        	if (mViewListener != null) {
        		mViewListener.onMoveDown(VideoView.this, getIndex());
        	}
            return false;
        }

        @Override
        public void onLongPress(MotionEvent e) {
        }
        
        /**
         * @param e1 The first down motion event that started the scrolling.
           @param e2 The move motion event that triggered the current onScroll.
           @param distanceX The distance along the X axis(轴) that has been scrolled since the last call to onScroll. This is NOT the distance between e1 and e2.
           @param distanceY The distance along the Y axis that has been scrolled since the last call to onScroll. This is NOT the distance between e1 and e2.
                                   无论是用手拖动view，或者是以抛的动作滚动，都会多次触发 ,这个方法在ACTION_MOVE动作发生时就会触发 参看GestureDetector的onTouchEvent方法源码
         * */
        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2,
                float distanceX, float distanceY) {
            return false;
        }
        /**
         * @param e1 第1个ACTION_DOWN MotionEvent 并且只有一个
         * @param e2 最后一个ACTION_MOVE MotionEvent 
         * @param velocityX X轴上的移动速度，像素/秒  
         * @param velocityY Y轴上的移动速度，像素/秒
         * 这个方法发生在ACTION_UP时才会触发 参看GestureDetector的onTouchEvent方法源码
         * 
         * */
        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
                float velocityY) {
        	if (e1.getX() - e2.getX() > 100.0F) {
            	if (mViewListener != null) {
            		mViewListener.onMoveRight(VideoView.this, getIndex());
            		runStopTimer();
            	}
            }
        	else if (e1.getX() - e2.getX() < -100.0F) {
        		if (mViewListener != null) {
                	mViewListener.onMoveLeft(VideoView.this, getIndex());
                	runStopTimer();
        		}
            }
        	
        	if (e1.getY() - e2.getY() > 100.0F) {
            	if (mViewListener != null) {
            		mViewListener.onMoveUp(VideoView.this, getIndex());
            		runStopTimer();
            	}
            }
        	else if (e1.getY() - e2.getY() < -100.0F) {
            	if (mViewListener != null) {
            		mViewListener.onMoveDown(VideoView.this, getIndex());
            		runStopTimer();
            	}
            }
            return false;
        }

        @Override
        public void onShowPress(MotionEvent e) {
        }
        
        @Override
        public boolean onDown(MotionEvent e) {
        	if (mViewListener != null) {
        		mViewListener.onClick(VideoView.this, getIndex());
        	}
            return false;
        }
        
        //双击事件
        @Override
        public boolean onDoubleTap(MotionEvent e) {
        	if (mViewListener != null) {
        		mViewListener.onDbClick(VideoView.this, getIndex());
        	}
            return false;
        }
        
        @Override
        public boolean onDoubleTapEvent(MotionEvent e) {
            return false;
        }
        /**
         * 这个方法不同于onSingleTapUp，他是在GestureDetector确信用户在第一次触摸屏幕后，没有紧跟着第二次触摸屏幕，也就是不是“双击”的时候触发
         * */
        @Override
        public boolean onSingleTapConfirmed(MotionEvent e) {
            return false;
        }   
    }
	
	public static interface VViewListener {
		/*
		 * 进行画图操作
		 */
		public abstract void onDrawVideo(Canvas canvas, int width, int heigth, Paint paint);
		
		/*
		 * 点击操作
		 */
		public abstract void onClick(VideoView view, int index);
		
		/*
		 * 双击操作
		 */
		public abstract void onDbClick(VideoView view, int index);
		
		/*
		 * 左移操作
		 */
		public abstract void onMoveLeft(VideoView view, int index);
		
		/*
		 * 右移操作
		 */
		public abstract void onMoveRight(VideoView view, int index);
		
		/*
		 * 左移操作
		 */
		public abstract void onMoveUp(VideoView view, int index);
		
		/*
		 * 右移操作
		 */
		public abstract void onMoveDown(VideoView view, int index);
		
		/*
		 * 停止
		 */
		public abstract void onMoveStop(VideoView view, int index);
	}
}
