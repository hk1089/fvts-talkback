package com.hk1089.mettax.video;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.babelstar.gviewer.NetClient;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * VideoSearchActivity - Complete copy of RecordSearchActivity with search functionality
 * This activity matches RecordSearchActivity.java exactly
 */
public class VideoSearchActivity extends Activity {
	private static final String TAG = "VideoSearchActivity";
	private ListView mLstRecord;
	private String mDevIdno;
	private Button mBtnStart;
	private Button mBtnStop; 
	private TextView mTvStatus;
	private Handler mHandler = new Handler();
	private List<RecordFile> mFileList = new ArrayList<RecordFile>();
	private long mSearchHandle = 0;
	private SearchRunnable mSearchRunnable = new SearchRunnable();
	private ArrayAdapter<String> mSearchAdapter;
	private boolean mIsDirect;
	private String mServer;
	private int mPort;
	private int mStorageType; //1:设备录像 2:存储服务器录像  4：下载服务器
	
	// Playback components
	private VideoView mVideoView;
	private Playback mPlayback;
	private boolean mIsPlaying = false;
	private Button mBtnPlayVideo;
	private Button mBtnSpeed1x; // Reused as speed selector button
	private TextView mTvPlaybackTime;
	private boolean mIsPaused = false; // Track pause state
	private int mCurrentSpeed = 1; // Current playback speed (1x, 2x, 3x, 4x)
	private Context mContext;
	private byte[] mPlaybackFile = null;
	private int mPlaybackLength = 0;
	private int mPlaybackChannel = 0;
	private RecordFile mCurrentPlayingFile = null; // Currently playing file for duration info
	private int mTotalDuration = 0; // Total duration in seconds
	private int mCurrentPosition = 0; // Current position in seconds
	private long mLastUpdateTime = 0; // Last UI update time for throttling
	private static final long UI_UPDATE_INTERVAL = 200; // Update UI every 200ms for smoothness
	
	// NetClient instance for initialization (matching MainActivity pattern)
	private NetClient mNetClient;
	
	// Constants matching MainActivity
	private static final int GPS_FILE_LOCATION_DEVICE = 1;		//Device
	private static final int GPS_FILE_ATTRIBUTE_RECORD = 2;
	private static final int GPS_FILE_TYPE_ALL = -1;
	private static final int GPS_MEDIA_TYPE_AUDIO_VIDEO = 0;
	private static final int GPS_STREAM_TYPE_MAIN_SUB = -1;
	private static final int GPS_MEMORY_TYPE_MAIN_SUB = 0;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		mContext = this.getApplicationContext();
		
		// Initialize NetClient globally (matching MainActivity pattern exactly)
		// This ensures search works even when launched directly without going through MainActivity
		initializeNetClient();
		
		// Create UI programmatically (no XML)
		createUI();
		
		// Initialize Playback instance
		mPlayback = new Playback(this);
		mPlayback.setVideoView(mVideoView);
		mPlayback.setPlayerListener(new PlaybackListenerImpl());
		
		Intent intent = getIntent();

		if (intent.hasExtra("DevIDNO")) {
			mDevIdno = intent.getStringExtra("DevIDNO");
		}
		mIsDirect = intent.getBooleanExtra("direct", false);
		if(mIsDirect){
			mServer = intent.getStringExtra("serverIp");
			mPort = intent.getIntExtra("port", 0);
			mDevIdno = intent.getStringExtra("devIdno");
		} else {
			// For server-based, get from Intent or use defaults (matching RecordSearchActivity)
			if (intent.hasExtra("Server")) {
				mServer = intent.getStringExtra("Server");
			} else {
				mServer = "dashcam.fvts.in"; // Default server
			}
			if (intent.hasExtra("Port")) {
				mPort = intent.getIntExtra("Port", 0);
			} else {
				mPort = 6605; // Default port (matching MainActivity.updateServer)
			}
		}
		
		// Update server configuration (matching MainActivity.updateServer pattern)
		// This must be done after getting server/port from Intent
		updateServer();

		startSearch();
	}
	
	/**
	 * Initialize NetClient globally (matching MainActivity.java pattern exactly)
	 * MainActivity order: new NetClient() -> Initialize() -> SetJniEnv() -> SetSession() -> SetDirSvr()
	 * This ensures search works even when VideoSearchActivity is launched directly
	 */
	private void initializeNetClient() {
		try {
			String sdPath = mContext.getExternalFilesDir("").getAbsolutePath() + "/";
			Log.d(TAG, "========== INITIALIZING NETCLIENT (matching MainActivity) ==========");
			Log.d(TAG, "Path: " + sdPath);
			
			// Step 1: Create NetClient instance (matching MainActivity line 211)
			mNetClient = new NetClient();
			Log.d(TAG, "Step 1: Created NetClient instance");
			
			// Step 2: Initialize (matching MainActivity line 212 - called on instance)
			mNetClient.Initialize(sdPath);
			Log.d(TAG, "Step 2: Called Initialize()");
			
			// Step 3: SetJniEnv (matching MainActivity line 213 - instance method)
			mNetClient.SetJniEnv();
			Log.d(TAG, "Step 3: Called SetJniEnv()");
			
			// Step 4: SetSession (matching MainActivity line 215 - called on instance)
			mNetClient.SetSession("");  // Empty session like MainActivity
			Log.d(TAG, "Step 4: Called SetSession(\"\")");
			
			// Step 5: SetDirSvr will be called in updateServer() after getting server/port from Intent
			// This matches MainActivity.updateServer() pattern
			Log.d(TAG, "========== NETCLIENT INITIALIZED (SetDirSvr will be called in updateServer) ==========");
		} catch (Exception e) {
			Log.e(TAG, "========== ERROR INITIALIZING NETCLIENT ==========");
			Log.e(TAG, "Error: " + e.getMessage(), e);
			e.printStackTrace();
		}
	}
	
	/**
	 * Update server configuration (matching MainActivity.updateServer() pattern)
	 * This must be called after getting server/port from Intent
	 */
	private void updateServer() {
		if (!mIsDirect && mServer != null && !mServer.isEmpty()) {
			// Step 5: SetDirSvr for server-based (matching MainActivity.updateServer line 251 - called on instance)
			mNetClient.SetDirSvr(mServer, mServer, mPort, 0);
			Log.d(TAG, "Step 5: Called SetDirSvr(" + mServer + ", " + mServer + ", " + mPort + ", 0)");
			Log.d(TAG, "========== NETCLIENT FULLY INITIALIZED ==========");
		} else {
			Log.d(TAG, "Step 5: Skipped SetDirSvr (direct connection or server not set)");
		}
	}
	
	/**
	 * Create UI programmatically (matching RecordSearchActivity layout + video player)
	 */
	private void createUI() {
		// Main layout
		LinearLayout mainLayout = new LinearLayout(this);
		mainLayout.setOrientation(LinearLayout.VERTICAL);
		mainLayout.setPadding(8, 8, 8, 8);
		
		// Video player section
		DisplayMetrics dm = new DisplayMetrics();
		getWindowManager().getDefaultDisplay().getMetrics(dm);
		int screenWidth = dm.widthPixels;
		int picHeight = screenWidth / 4 * 3;
		
		mVideoView = new VideoView(this, 0);
		LayoutParams videoParams = new LinearLayout.LayoutParams(
			screenWidth, picHeight);
		mVideoView.setLayoutParams(videoParams);
		mainLayout.addView(mVideoView);
		
		// Bottom control bar (compact layout like the image)
		LinearLayout bottomControlLayout = new LinearLayout(this);
		bottomControlLayout.setOrientation(LinearLayout.HORIZONTAL);
		bottomControlLayout.setPadding(16, 12, 16, 12);
		bottomControlLayout.setBackgroundColor(0x80000000); // Semi-transparent black background
		
		// Playback time display (left side)
		mTvPlaybackTime = new TextView(this);
		mTvPlaybackTime.setText("00:00 / 00:00");
		mTvPlaybackTime.setTextColor(0xFFFFFFFF); // White text
		mTvPlaybackTime.setTextSize(14);
		mTvPlaybackTime.setPadding(0, 0, 16, 0);
		bottomControlLayout.addView(mTvPlaybackTime);
		
		// Play/Pause button
		mBtnPlayVideo = new Button(this);
		mBtnPlayVideo.setText("▶"); // Play icon
		mBtnPlayVideo.setLayoutParams(new LinearLayout.LayoutParams(
			LinearLayout.LayoutParams.WRAP_CONTENT, 
			LinearLayout.LayoutParams.WRAP_CONTENT));
		mBtnPlayVideo.setPadding(12, 8, 12, 8);
		mBtnPlayVideo.setBackgroundColor(0x00000000); // Transparent background
		mBtnPlayVideo.setTextColor(0xFFFFFFFF); // White text
		mBtnPlayVideo.setEnabled(false);
		bottomControlLayout.addView(mBtnPlayVideo);
		
		// Speed selector button (shows current speed with dropdown indicator)
		mBtnSpeed1x = new Button(this);
		mBtnSpeed1x.setText(mCurrentSpeed + ".0x ▼");
		mBtnSpeed1x.setLayoutParams(new LinearLayout.LayoutParams(
			LinearLayout.LayoutParams.WRAP_CONTENT, 
			LinearLayout.LayoutParams.WRAP_CONTENT));
		mBtnSpeed1x.setPadding(12, 8, 12, 8);
		mBtnSpeed1x.setBackgroundColor(0x00000000); // Transparent background
		mBtnSpeed1x.setTextColor(0xFFFFFFFF); // White text
		mBtnSpeed1x.setEnabled(false);
		mBtnSpeed1x.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				// Cycle through speeds: 1x -> 2x -> 3x -> 4x -> 1x
				int nextSpeed = mCurrentSpeed + 1;
				if (nextSpeed > 4) {
					nextSpeed = 1;
				}
				setPlaybackSpeed(nextSpeed);
			}
		});
		bottomControlLayout.addView(mBtnSpeed1x);
		
		mainLayout.addView(bottomControlLayout);
		
		// Search button layout
		LinearLayout buttonLayout = new LinearLayout(this);
		buttonLayout.setOrientation(LinearLayout.HORIZONTAL);
		buttonLayout.setPadding(0, 0, 0, 8);
		
		// Start button
		mBtnStart = new Button(this);
		mBtnStart.setText("Start Search");
		mBtnStart.setLayoutParams(new LinearLayout.LayoutParams(
			0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f));
		buttonLayout.addView(mBtnStart);
		
		// Stop button
		mBtnStop = new Button(this);
		mBtnStop.setText("Stop Search");
		mBtnStop.setLayoutParams(new LinearLayout.LayoutParams(
			0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f));
		buttonLayout.addView(mBtnStop);
		
		mainLayout.addView(buttonLayout);
		
		// Status text
		mTvStatus = new TextView(this);
		mTvStatus.setText("");
		mTvStatus.setPadding(0, 0, 0, 8);
		mainLayout.addView(mTvStatus);
		
		// List view
		mLstRecord = new ListView(this);
		mLstRecord.setLayoutParams(new LinearLayout.LayoutParams(
			LinearLayout.LayoutParams.MATCH_PARENT, 
			LinearLayout.LayoutParams.MATCH_PARENT));
		mainLayout.addView(mLstRecord);
		
		setContentView(mainLayout);
		
		// Set up click listeners
		mLstRecord.setOnItemClickListener(new OnItemClickListener() {  
			@Override  
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {  
				// Play selected video in the same screen
				RecordFile selectedFile = mFileList.get(arg2);
				Log.d(TAG, "Playing selected file: " + selectedFile.getFileInfo());
				
				// Stop current playback if playing
				if (mIsPlaying) {
					stopPlayback();
				}
				
				// Store playback parameters
				mPlaybackFile = selectedFile.getOrginalFile();
				mPlaybackLength = selectedFile.getOrginalLen();
				mPlaybackChannel = selectedFile.getChn();
				mCurrentPlayingFile = selectedFile; // Store for duration info
				
				// Calculate total duration from file's time range
				if (selectedFile.getEndTime() != null && selectedFile.getBeginTime() != null) {
					mTotalDuration = selectedFile.getEndTime() - selectedFile.getBeginTime();
					Log.d(TAG, "File duration: " + mTotalDuration + "s (from " + selectedFile.getBeginTime() + " to " + selectedFile.getEndTime() + ")");
				}
				
				// Enable playback button
				mBtnPlayVideo.setEnabled(true);
				
				// Start playback
				startPlayback();
			}  
		});
		
		mSearchAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, new ArrayList<String>());
		mLstRecord.setAdapter(mSearchAdapter);
		
		PlayClickListener playClickListen = new PlayClickListener();
		mBtnStart.setOnClickListener(playClickListen);
		mBtnStop.setOnClickListener(playClickListen);
		mBtnPlayVideo.setOnClickListener(playClickListen);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// No menu needed
		return true;
	}

	@Override
	protected void onDestroy() {
		stopSearch();
		stopPlayback();
		// Note: We don't call UnInitialize here because NetClient might be used by other components
		// Only uninitialize if this is the only activity using NetClient
		// mNetClient.UnInitialize();
		super.onDestroy();
	}
	
	protected void showSearching() {
		mTvStatus.setText("Searching");
	}

	protected void cancelSearch() {
		mTvStatus.setText("");
		stopSearch();
	}
	
	protected void startSearch() {
		if (0 == mSearchHandle) {
			showSearching();
			
			Calendar cal = Calendar.getInstance();
			//设备端录像搜索
			//Device video search
			mStorageType = GPS_FILE_LOCATION_DEVICE;
			mSearchHandle = NetClient.SFOpenSrchFile(mDevIdno, mStorageType, GPS_FILE_ATTRIBUTE_RECORD);
			//存储服务器录像搜索（依据设备"车牌号"，如下）
			//storageServer video search（According to the license plate number）
//			mSearchHandle = NetClient.SFOpenSrchFile("4429-HY", NetClient.GPS_FILE_LOCATION_STOSVR, NetClient.GPS_FILE_ATTRIBUTE_RECORD);
			
			mFileList.clear();
			//NetClient.SFStartSearchFile(mSearchHandle,2012, 12, 23, NetClient.GPS_FILE_TYPE_ALL, 0, 0, 86400);
			//NetClient.SFStartSearchFile(mSearchHandle, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH), NetClient.GPS_FILE_TYPE_ALL, 0, 0, 86400);
			if(mIsDirect){

				NetClient.SFSetRealServer(mSearchHandle, mServer, mPort, "");
				NetClient.SFStartSearchFile(mSearchHandle,cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1,  cal.get(Calendar.DAY_OF_MONTH), GPS_FILE_TYPE_ALL, 0, 0, 86400);
			}else{

				//1078设备
				boolean is1078 = false;

				if(is1078){
					NetClient.SFStartSearchFileEx(mSearchHandle, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH),
							cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH),
							GPS_FILE_TYPE_ALL, 0, 0, 86400, GPS_FILE_LOCATION_DEVICE, 0, GPS_MEDIA_TYPE_AUDIO_VIDEO,
							GPS_STREAM_TYPE_MAIN_SUB, GPS_MEMORY_TYPE_MAIN_SUB, 0, 0, 0);
				}else{
					NetClient.SFStartSearchFile(mSearchHandle, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH)+1, cal.get(Calendar.DAY_OF_MONTH)-1, GPS_FILE_TYPE_ALL, 0, 7242, 7543);

					Log.d(TAG, "Server-based search started>>> "+cal.get(Calendar.YEAR)+"-"+ cal.get(Calendar.MONTH)+1 + "-" + (cal.get(Calendar.DAY_OF_MONTH)-1));
				}

			}


			//NetClient.SFStartSearchFile(mSearchHandle, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH), NetClient.GPS_FILE_TYPE_ALL, 0, 0, 86400);
			mHandler.postDelayed(mSearchRunnable, 2000);
		}
	}
	
	protected void stopSearch() {
		if (0 != mSearchHandle) {
			NetClient.SFStopSearchFile(mSearchHandle);
			NetClient.SFCloseSearchFile(mSearchHandle);
			mHandler.removeCallbacks(mSearchRunnable);
			mSearchHandle = 0;
		}
	}
	
	final class SearchRunnable implements Runnable {
		public void run() {
			boolean isFinished = false;
			if (0 != mSearchHandle) {
				while (true) {
					byte[] result = new byte[1024];
					java.util.Arrays.fill(result, (byte)0);
					int ret = NetClient.SFGetSearchFile(mSearchHandle, result, 1024);
					if (ret == NetClient.NET_SUCCESS) {
						int i = 0;
						for (i = 0; i < result.length; ++ i) {
							if (result[i] == 0) {
								break;
							}
						}
						byte[] temp = new byte[i];
						System.arraycopy(result, 0, temp, 0, i);
						//szFileInfo:	szFile[256]:nYear:nMonth:nDay:uiBegintime:uiEndtime:szDevIDNO:nChn:nFileLen:nFileType:nLocation:nSvrID
						String fileInfo = new String(temp);
						String[] info = fileInfo.split(";");
						
						RecordFile search = new RecordFile();
						search.setOrginalFileInfo(result, i);

						search.setFileInfo(fileInfo);
						int index = 0;
						//确保发送过来的数据包含了设备id 如果没有手动添加设置
//						if(mStorageType == 1){
//							if(info[index ++].isEmpty()){
//								search.setDevIdno(mDevIdno);
//							}else{
//								search.setDevIdno(info[index ++]);
//							}
//
//						}else {
//							index ++; //这个是设备id
//							search.setDevIdno(mDevIdno);
//						}
//						index ++; //这个是设备id
						search.setDevIdno(mDevIdno);
//						search.setDevIdno(mDevIdno);
						search.setName(info[index ++]);
						search.setYear(Integer.parseInt(info[index ++]));
						search.setMonth(Integer.parseInt(info[index ++]));
						search.setDay(Integer.parseInt(info[index ++]));
						search.setBeginTime(Integer.parseInt(info[index ++]));
						search.setEndTime(Integer.parseInt(info[index ++]));
						index ++;
						search.setChn(Integer.parseInt(info[index ++]));
						search.setFileLength(Integer.parseInt(info[index ++]));
						search.setFileType(Integer.parseInt(info[index ++]));
						search.setLocation(Integer.parseInt(info[index ++]));
						search.setSvrId(Integer.parseInt(info[index ++]));

						search.setChnMask(Integer.parseInt(info[index ++]));
						search.setAlarmInfo(Integer.parseInt(info[index ++]));
						search.setFileOffset(Integer.parseInt(info[index ++]));
						search.setRecording(Integer.parseInt(info[index ++]) > 0 ? true : false);
						search.setStream(Integer.parseInt(info[index ++]) > 0 ? true : false);

						search.setIsPlaying(false);
						
						// Print/search model data
						printRecordFileModel(search, fileInfo, info);
						Log.d(TAG, "Device ID: " + search.getFileInfo());
						mFileList.add(search);
						continue;
					}
					else if (ret == NetClient.SEARCH_FINISHED) {
						if(mFileList.size() > 0){
							Collections.sort(mFileList, new Comparator<RecordFile>(){
								@Override
								public int compare(RecordFile lhs,
										RecordFile rhs) {
									// TODO Auto-generated method stub
									int i = lhs.getBeginTime() - rhs.getBeginTime();
									if(i == 0){
										int j = lhs.getChn() - lhs.getChn();
										return j;
									}
									return i;
								}
								
							});
						}
						isFinished = true;
						
						// Print summary of all found files
						Log.d(TAG, "========== SEARCH FINISHED ==========");
						Log.d(TAG, "Total files found: " + mFileList.size());
						Log.d(TAG, "Device ID: " + mDevIdno);
						Log.d(TAG, "Storage Type: " + mStorageType + " (1=Device, 2=Storage Server, 4=Download Server)");
						for (int idx = 0; idx < mFileList.size(); idx++) {
							RecordFile file = mFileList.get(idx);
							Log.d(TAG, "--- File #" + (idx + 1) + " ---");
							printRecordFileSummary(file);
						}
						Log.d(TAG, "=====================================");
						
						updateSearchResults();
						
						if (mFileList.size() == 0) {
							cancelSearch();
							Toast.makeText(getApplicationContext(), "File is empty", Toast.LENGTH_SHORT).show(); 
						} else {
							cancelSearch();
							// Automatically play the first result
							playFirstResult();
						}
						break;
					} 
					else if (ret == NetClient.SEARCH_FAILED) {
						isFinished = true;
						cancelSearch();
						Toast.makeText(getApplicationContext(), "Search Finished", Toast.LENGTH_SHORT).show(); 
						break;
					}
					else 
					{
						continue;
					}
				}
			}
			
			if (!isFinished) {
				mHandler.postDelayed(mSearchRunnable, 50);
			}
		}
	}
	
	private void updateSearchResults() {
		mSearchAdapter.clear();
		for (RecordFile file : mFileList) {
			String dateStr = file.getYear() + "-" + String.format("%02d", file.getMonth()) + "-" + String.format("%02d", file.getDay());
			String timeStr = formatSeconds(file.getBeginTime()) + " - " + formatSeconds(file.getEndTime());
			String itemText = "Chn: " + file.getChn() + " | Date: " + dateStr + " | Time: " + timeStr + " | Type: " + (file.getFileType() == 0 ? "Normal" : "Alarm");
			mSearchAdapter.add(itemText);
		}
		mSearchAdapter.notifyDataSetChanged();
	}
	
	final class PlayClickListener implements OnClickListener {
		public void onClick(View v) {
			if (v.equals(mBtnStart)) {
				startSearch();
			} else if (v.equals(mBtnStop)) {
				stopSearch();
			} else if (v.equals(mBtnPlayVideo)) {
				// Toggle play/pause
				if (!mIsPlaying) {
					startPlayback();
				} else {
					togglePause();
				}
			}
		}
	}
	
	/**
	 * Automatically play the first result from search in the same screen
	 */
	private void playFirstResult() {
		if (mFileList != null && mFileList.size() > 0) {
			RecordFile firstFile = mFileList.get(0);
			Log.d(TAG, "Auto-playing first result: " + firstFile.getFileInfo());
			
			// Store playback parameters
			mPlaybackFile = firstFile.getOrginalFile();
			mPlaybackLength = firstFile.getOrginalLen();
			mPlaybackChannel = firstFile.getChn();
			mCurrentPlayingFile = firstFile; // Store for duration info
			
			// Calculate total duration from file's time range
			if (firstFile.getEndTime() != null && firstFile.getBeginTime() != null) {
				mTotalDuration = firstFile.getEndTime() - firstFile.getBeginTime();
				Log.d(TAG, "File duration: " + mTotalDuration + "s (from " + firstFile.getBeginTime() + " to " + firstFile.getEndTime() + ")");
			}
			
			// Enable playback button
			mBtnPlayVideo.setEnabled(true);
			
			// Automatically start playback
			startPlayback();
		}
	}
	
	/**
	 * Start video playback (matching PlaybackActivity.StartPlayback())
	 */
	private void startPlayback() {
		if (!mIsPlaying && mPlaybackFile != null && mPlaybackFile.length > 0) {
			if (mPlayback == null) {
				mPlayback = new Playback(this);
				mPlayback.setVideoView(mVideoView);
				mPlayback.setPlayerListener(new PlaybackListenerImpl());
			}
			
			mPlayback.setPlayerDevIdno(mDevIdno);
			if (mIsDirect) {
				mPlayback.setLanInfo(mServer, mPort);
			}
			mPlayback.StartVod(mPlaybackFile, mPlaybackLength, mPlaybackChannel);
			mIsPlaying = true;
			mIsPaused = false;
			mCurrentPosition = 0;
			mCurrentSpeed = 1; // Reset to 1x when starting playback
			mLastUpdateTime = 0; // Reset update throttle
			// Don't reset mTotalDuration here - it's set from the file
			if (mTotalDuration > 0) {
				mTvPlaybackTime.setText("00:00 / " + formatTime(mTotalDuration));
			} else {
				mTvPlaybackTime.setText("00:00 / 00:00");
			}
			// Update play button to show pause icon
			mBtnPlayVideo.setText("⏸"); // Pause icon
			mBtnPlayVideo.setEnabled(true);
			// Enable speed button
			if (mBtnSpeed1x != null) {
				mBtnSpeed1x.setEnabled(true);
				mBtnSpeed1x.setText(mCurrentSpeed + ".0x ▼");
			}
		}
	}
	
	/**
	 * Stop video playback (matching PlaybackActivity.StopPlayback())
	 */
	private void stopPlayback() {
		if (mIsPlaying && mPlayback != null) {
			mPlayback.StopVod();
			mIsPlaying = false;
			mIsPaused = false;
			mTotalDuration = 0;
			mCurrentPosition = 0;
			mLastUpdateTime = 0;
			mCurrentSpeed = 1; // Reset to 1x
			mTvPlaybackTime.setText("00:00 / 00:00");
			// Update play button to show play icon
			mBtnPlayVideo.setText("▶"); // Play icon
			mBtnPlayVideo.setEnabled(false);
			// Disable speed button
			if (mBtnSpeed1x != null) {
				mBtnSpeed1x.setEnabled(false);
				mBtnSpeed1x.setText("1.0x ▼");
			}
		}
	}
	
	/**
	 * Toggle pause/resume playback
	 */
	private void togglePause() {
		if (mIsPlaying && mPlayback != null) {
			mIsPaused = !mIsPaused;
			mPlayback.pause(mIsPaused);
			// Update button icon
			if (mIsPaused) {
				mBtnPlayVideo.setText("▶"); // Play icon when paused
			} else {
				mBtnPlayVideo.setText("⏸"); // Pause icon when playing
			}
			Log.d(TAG, mIsPaused ? "Playback paused" : "Playback resumed");
		}
	}
	
	/**
	 * Set playback speed (1x, 2x, 3x, 4x)
	 */
	private void setPlaybackSpeed(int speed) {
		if (mIsPlaying && mPlayback != null && speed >= 1 && speed <= 4) {
			mPlayback.setPlayRate(speed);
			mCurrentSpeed = speed;
			// Update speed button text
			if (mBtnSpeed1x != null) {
				mBtnSpeed1x.setText(mCurrentSpeed + ".0x ▼");
			}
			Log.d(TAG, "Playback speed set to: " + speed + "x");
		}
	}
	
	/**
	 * Update time display text
	 */
	private void updateTimeDisplay(int currentSeconds, int totalSeconds) {
		String currentTime = formatTime(currentSeconds);
		String totalTime = formatTime(totalSeconds);
		mTvPlaybackTime.setText(currentTime + " / " + totalTime);
	}
	
	/**
	 * Format seconds to MM:SS or HH:MM:SS
	 */
	private String formatTime(int seconds) {
		int hours = seconds / 3600;
		int minutes = (seconds % 3600) / 60;
		int secs = seconds % 60;
		if (hours > 0) {
			return String.format("%d:%02d:%02d", hours, minutes, secs);
		} else {
			return String.format("%d:%02d", minutes, secs);
		}
	}
	
	/**
	 * PlaybackListener implementation to update SeekBar and time display
	 */
	private class PlaybackListenerImpl implements Playback.PlaybackListener {
		@Override
		public void onBeginPlay() {
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					mBtnPlayVideo.setText("⏸"); // Pause icon
					mBtnPlayVideo.setEnabled(true);
					if (mBtnSpeed1x != null) {
						mBtnSpeed1x.setEnabled(true);
						mBtnSpeed1x.setText(mCurrentSpeed + ".0x ▼");
					}
					Log.d(TAG, "Playback began");
				}
			});
		}
		
		@Override
		public void onUpdatePlay(int nDownSecond, int nPlaySecond) {
			// Throttle UI updates for smoothness
			long currentTime = System.currentTimeMillis();
			if (currentTime - mLastUpdateTime < UI_UPDATE_INTERVAL) {
				return; // Skip this update to maintain smooth playback
			}
			mLastUpdateTime = currentTime;
			
			// Use file's actual duration if available, otherwise use downloaded duration
			int totalDuration = mTotalDuration;
			if (totalDuration <= 0 && nDownSecond > 0) {
				// Fallback to downloaded duration if file duration not available
				totalDuration = nDownSecond;
				mTotalDuration = totalDuration;
			}
			
					// Calculate current position relative to file start
					int currentPosition = nPlaySecond;
					if (mCurrentPlayingFile != null && mCurrentPlayingFile.getBeginTime() != null) {
						// For now, assume nPlaySecond is relative to file start
						currentPosition = nPlaySecond;
					}
					
					if (totalDuration > 0) {
						mCurrentPosition = currentPosition;
						
						// Ensure current position doesn't exceed total duration
						if (currentPosition > totalDuration) {
							currentPosition = totalDuration;
							mCurrentPosition = totalDuration;
						}
						
						// Update time display (throttled, but always eventually updated)
						final int finalCurrentPos = currentPosition;
						final int finalTotalDur = totalDuration;
						runOnUiThread(new Runnable() {
							@Override
							public void run() {
								updateTimeDisplay(finalCurrentPos, finalTotalDur);
							}
						});
					}
		}
		
		@Override
		public void onEndPlay() {
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					mIsPlaying = false;
					mIsPaused = false;
					mBtnPlayVideo.setText("▶"); // Play icon
					mBtnPlayVideo.setEnabled(false);
					if (mBtnSpeed1x != null) {
						mBtnSpeed1x.setEnabled(false);
						mBtnSpeed1x.setText("1.0x ▼");
					}
					mCurrentSpeed = 1; // Reset to 1x
					mTvPlaybackTime.setText("00:00 / 00:00");
					Log.d(TAG, "Playback ended");
				}
			});
		}
		
		@Override
		public void onClick(VideoView view, int index) {
			// Handle video view click (e.g., pause/resume)
			// Can be implemented if needed
		}
		
		@Override
		public void onDbClick(VideoView view, int index) {
			// Handle video view double click (e.g., fullscreen)
			// Can be implemented if needed
		}
	}
	
	/**
	 * Print detailed RecordFile model data
	 */
	private void printRecordFileModel(RecordFile file, String fileInfo, String[] info) {
		Log.d(TAG, "========== NEW RECORD FILE MODEL ==========");
		Log.d(TAG, "Raw File Info String: " + fileInfo);
		Log.d(TAG, "Parsed Info Array Length: " + info.length);
		for (int i = 0; i < info.length; i++) {
			Log.d(TAG, "  info[" + i + "] = " + info[i]);
		}
		Log.d(TAG, "--- RecordFile Object Properties ---");
		Log.d(TAG, "  DevIdno: " + file.getDevIdno());
		Log.d(TAG, "  Name: " + file.getName());
		Log.d(TAG, "  Date: " + file.getYear() + "-" + file.getMonth() + "-" + file.getDay());
		Log.d(TAG, "  Time Range: " + file.getBeginTime() + "s - " + file.getEndTime() + "s");
		Log.d(TAG, "  Channel: " + file.getChn());
		Log.d(TAG, "  File Length: " + file.getFileLength() + " bytes");
		Log.d(TAG, "  File Type: " + file.getFileType() + " (0=Normal, 1=Alarm)");
		Log.d(TAG, "  Location: " + file.getLocation() + " (1=Device, 2=Storage Server, 4=Download Server)");
		Log.d(TAG, "  Server ID: " + file.getSvrId());
		Log.d(TAG, "  Channel Mask: " + file.getChnMask());
		Log.d(TAG, "  Alarm Info: " + file.getAlarmInfo());
		Log.d(TAG, "  File Offset: " + file.getFileOffset());
		Log.d(TAG, "  Recording: " + file.getRecording());
		Log.d(TAG, "  Stream: " + file.getStream());
		Log.d(TAG, "  File Time: " + file.getFileTime());
		Log.d(TAG, "  File Date: " + file.getFileDate());
		Log.d(TAG, "  Original File Info Length: " + file.getOrginalLen());
		Log.d(TAG, "===========================================");
	}
	
	/**
	 * Print summary of RecordFile model
	 */
	private void printRecordFileSummary(RecordFile file) {
		Log.d(TAG, "  DevIdno: " + file.getDevIdno() + 
				  " | Chn: " + file.getChn() + 
				  " | Date: " + file.getYear() + "-" + String.format("%02d", file.getMonth()) + "-" + String.format("%02d", file.getDay()) +
				  " | Time: " + formatSeconds(file.getBeginTime()) + " - " + formatSeconds(file.getEndTime()) +
				  " | Type: " + (file.getFileType() == 0 ? "Normal" : "Alarm") +
				  " | Size: " + file.getFileLength() + " bytes");
	}
	
	/**
	 * Format seconds to HH:MM:SS
	 */
	private String formatSeconds(int seconds) {
		int hours = seconds / 3600;
		int minutes = (seconds % 3600) / 60;
		int secs = seconds % 60;
		return String.format("%02d:%02d:%02d", hours, minutes, secs);
	}
}

