package com.hk1089.mettax.video;


public class UpdateThread {
	
	private UpdateViewThread mUpdateViewThread = null;	//视频界面更新线程

	/*
	 * 开始界面更新线程
	 */
	protected void startUpdateThread() {
		if (mUpdateViewThread == null) {
			mUpdateViewThread = new UpdateViewThread();
			mUpdateViewThread.start();
		}
	}
	
	/*
	 * 停止界面更新线程
	 */
	protected void stopUpdateThread() {
		if (mUpdateViewThread != null) {
			Thread dummy = mUpdateViewThread;
			mUpdateViewThread.setExit(true);
			mUpdateViewThread = null;
			dummy.interrupt();
		}
	}
	
	/*
	 * 暂停
	 */
	protected void pauseUpdate(boolean isPause) {
		if (mUpdateViewThread != null) { 
			mUpdateViewThread.setPause(isPause);
		}
	}
	
	/*
	 * 更新界面
	 */
	protected void updateView() {
		
	}
	
	/*
	 * 播放状态检测线程
	 */
	private class UpdateViewThread extends Thread{
		private boolean isExit = false;
		private boolean isPause = false;
		
		public void setExit(boolean isExit) {
			this.isExit = isExit;
		}
		
		public void setPause(boolean isPause) {
			this.isPause = isPause;
		}
		
		public void run()   {  
			while (!isExit) {
				try {
					if (!isPause) {
						updateView();
						
						Thread.sleep(20);
					} else {
						Thread.sleep(100);
					}
				} catch (InterruptedException e) {
					// Normal shutdown path: stopUpdateThread() interrupts sleep to exit quickly.
					if (isExit) {
						break;
					}
					Thread.currentThread().interrupt();
					break;
				} 
			}
			this.isExit = true;
	    }
	}
}
