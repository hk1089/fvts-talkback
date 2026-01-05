package com.hk1089.mettax.video;

import android.app.Activity;
import android.content.Context;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.RelativeLayout;
import android.view.Gravity;
import android.widget.ImageView;
import android.graphics.Color;
import android.content.res.ColorStateList;

import androidx.annotation.DrawableRes;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.appcompat.widget.AppCompatImageButton;
import androidx.core.content.ContextCompat;
import androidx.core.widget.ImageViewCompat;

import com.hk1089.mettax.R;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

/**
 * PlaybackPlayer - A reusable video playback component with search functionality
 * Uses VideoSearchHelper for search operations and provides video playback controls
 * Includes: video playback, search, play/pause, speed control (1x-4x), time display
 */
public class PlaybackPlayer {
    private static final String TAG = "PlaybackPlayer";
    
    private Activity mActivity;
    private Context mContext;
    private String mDevIdno;
    private boolean mIsDirect;
    private String mServer;
    private int mPort;
    
    // UI Components
    private LinearLayout mMainLayout;
    private VideoView mVideoView;
    private TextView mTvPlaybackTime;
    private AppCompatImageButton mBtnPlayVideo;
    private Button mBtnSpeed1x;
    private ListView mLstRecord;
    private Button mBtnStart;
    private Button mBtnStop;
    private TextView mTvStatus;
    
    // Playback components
    private Playback mPlayback;
    private boolean mIsPlaying = false;
    private boolean mIsPaused = false;
    private int mCurrentSpeed = 1; // Current playback speed (1x, 2x, 3x, 4x)
    private byte[] mPlaybackFile = null;
    private int mPlaybackLength = 0;
    private int mPlaybackChannel = 0;
    private RecordFile mCurrentPlayingFile = null;
    private int mTotalDuration = 0; // Total duration in seconds
    private int mCurrentPosition = 0; // Current position in seconds
    private long mLastUpdateTime = 0;
    private static final long UI_UPDATE_INTERVAL = 200; // Update UI every 200ms for smoothness
    
    // Search helper
    private VideoSearchHelper mSearchHelper;
    private List<RecordFile> mFileList = new ArrayList<>();
    private ArrayAdapter<String> mAdapter;
    
    // Listener
    private PlaybackPlayerListener mListener;
    
    // Controls visibility management
    private boolean mControlsVisible = true;
    private android.os.Handler mControlsHandler = new android.os.Handler();
    private Runnable mHideControlsRunnable;
    private LinearLayout mBottomControlLayout;
    
    // Loading and placeholder indicators
    private android.widget.ProgressBar mLoadingIndicator;
    private android.widget.ImageView mPausePlaceholder;
    private boolean mIsLoading = false;
    
    // Fullscreen components
    private boolean mIsFullscreen = false;
    private android.widget.FrameLayout mFullscreenLayout;
    private VideoView mFullscreenVideoView;
    private Playback mFullscreenPlayback;
    private AppCompatImageButton mFullscreenPlayPauseBtn;
    private AppCompatImageButton mFullscreenCloseBtn;
    private Button mFullscreenSpeedBtn;
    private TextView mFullscreenTimeText;
    private android.widget.LinearLayout mFullscreenControlsLayout;
    private boolean mFullscreenControlsVisible = true;
    private android.os.Handler mFullscreenControlsHandler = new android.os.Handler();
    private Runnable mFullscreenHideControlsRunnable;
    private android.widget.ProgressBar mFullscreenLoadingIndicator;
    private android.widget.ImageView mFullscreenPausePlaceholder;
    
    /**
     * Listener interface for playback player events
     */
    public interface PlaybackPlayerListener {
        void onSearchStarted();
        void onFileFound(RecordFile file);
        void onSearchFinished(List<RecordFile> fileList);
        void onSearchFailed();
        void onPlaybackStarted(RecordFile file);
        void onPlaybackStopped();
    }
    
    /**
     * Constructor
     * @param activity Activity context
     * @param devIdno Device ID number
     * @param isDirect Whether using direct connection
     * @param server Server IP
     * @param port Server port
     */
    public PlaybackPlayer(Activity activity, String devIdno, boolean isDirect, String server, int port) {
        mActivity = activity;
        mContext = activity.getApplicationContext();
        mDevIdno = devIdno;
        mIsDirect = isDirect;
        mServer = server;
        mPort = port;
        
        // Create VideoSearchHelper (it initializes NetClient automatically)
        mSearchHelper = new VideoSearchHelper(mContext, mDevIdno, mIsDirect, mServer, mPort);
        mSearchHelper.setSearchListener(new VideoSearchHelper.VideoSearchListener() {
            @Override
            public void onSearchStarted() {
                mActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        showSearching();
                        if (mListener != null) {
                            mListener.onSearchStarted();
                        }
                    }
                });
            }
            
            @Override
            public void onFileFound(RecordFile file) {
                if (mListener != null) {
                    mListener.onFileFound(file);
                }
            }
            
            @Override
            public void onSearchFinished(List<RecordFile> fileList) {
                mActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mFileList.clear();
                        mFileList.addAll(fileList);
                        updateSearchResults();
                        cancelSearch();
                        
                        if (mFileList.isEmpty()) {
                            Toast.makeText(mContext, "File is empty", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(mContext, "Found " + mFileList.size() + " files", Toast.LENGTH_SHORT).show();
                            Log.d(TAG, "========== SEARCH FINISHED - CALLING playFirstResult() ==========");
                            // Automatically play the first result
                            playFirstResult();
                        }
                        
                        if (mListener != null) {
                            mListener.onSearchFinished(new ArrayList<>(mFileList));
                        }
                    }
                });
            }
            
            @Override
            public void onSearchFailed() {
                mActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        cancelSearch();
                        Toast.makeText(mContext, "Search Finished", Toast.LENGTH_SHORT).show();
                        if (mListener != null) {
                            mListener.onSearchFailed();
                        }
                    }
                });
            }
            
            @Override
            public void onSearchStopped() {
                mActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        cancelSearch();
                    }
                });
            }
        });
    }
    
    /**
     * Set playback player listener
     */
    public void setPlaybackPlayerListener(PlaybackPlayerListener listener) {
        mListener = listener;
    }
    
    /**
     * Set search date parameters
     * @param year Year (e.g., 2025)
     * @param month Month (1-12)
     * @param day Day (1-31)
     */
    public void setSearchDate(int year, int month, int day) {
        if (mSearchHelper != null) {
            mSearchHelper.setSearchDate(year, month, day);
        }
    }
    
    /**
     * Set search time range
     * @param beginTime Start time in seconds (0-86400)
     * @param endTime End time in seconds (0-86400)
     */
    public void setSearchTimeRange(int beginTime, int endTime) {
        if (mSearchHelper != null) {
            mSearchHelper.setSearchTimeRange(beginTime, endTime);
        }
    }
    
    /**
     * Set search channel
     * @param channel Channel number (0 = all channels)
     */
    public void setSearchChannel(int channel) {
        if (mSearchHelper != null) {
            mSearchHelper.setSearchChannel(channel);
        }
    }
    
    /**
     * Set all search parameters at once
     * @param year Year (e.g., 2025)
     * @param month Month (1-12)
     * @param day Day (1-31)
     * @param channel Channel number (0 = all channels)
     * @param beginTime Start time in seconds (0-86400)
     * @param endTime End time in seconds (0-86400)
     */
    public void setSearchParameters(int year, int month, int day, int channel, int beginTime, int endTime) {
        if (mSearchHelper != null) {
            mSearchHelper.setSearchParameters(year, month, day, channel, beginTime, endTime);
        }
    }
    
    /**
     * Helper method to create icon buttons (matching VideoPlayer style)
     */
    private AppCompatImageButton makeIconBtn(@DrawableRes int iconRes) {
        return makeIconBtn(iconRes, dp(14)); // Default size 14dp
    }

    private AppCompatImageButton makeIconBtn(@DrawableRes int iconRes, int sizeDp) {
        AppCompatImageButton b = new AppCompatImageButton(mActivity);
        b.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        b.setImageDrawable(AppCompatResources.getDrawable(mActivity, iconRes));
        ImageViewCompat.setImageTintList(b, ColorStateList.valueOf(Color.WHITE));
        b.setPadding(dp(4), dp(8), dp(4), dp(8));
        b.setBackgroundColor(Color.TRANSPARENT);
        b.setAdjustViewBounds(true);
        b.setFocusable(true);
        b.setClickable(true);
        b.setMaxWidth(sizeDp);
        b.setMaxHeight(sizeDp);
        return b;
    }

    private int dp(int px) {
        float density = mActivity.getResources().getDisplayMetrics().density;
        return Math.round(px * density);
    }

    /**
     * Create UI programmatically (matching VideoPlayer single player layout)
     */
    public View createView() {
        if (mActivity == null) {
            Log.e(TAG, "Activity is null, cannot create view");
            return null;
        }
        
        try {
            // Main layout
            mMainLayout = new LinearLayout(mActivity);
            mMainLayout.setOrientation(LinearLayout.VERTICAL);
            mMainLayout.setPadding(dp(0), dp(1), dp(0), dp(1));
            
            // Video container (RelativeLayout like VideoPlayer)
            RelativeLayout videoContainer = new RelativeLayout(mActivity);
            DisplayMetrics dm = new DisplayMetrics();
            mActivity.getWindowManager().getDefaultDisplay().getMetrics(dm);
            int screenWidth = dm.widthPixels;
            int picHeight = screenWidth / 7 * 4;
            
            LinearLayout.LayoutParams containerParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                picHeight);
            videoContainer.setLayoutParams(containerParams);
            
            // VideoView
            mVideoView = new VideoView(mActivity, 0);
            if (mVideoView != null) {
                RelativeLayout.LayoutParams videoParams = new RelativeLayout.LayoutParams(
                    RelativeLayout.LayoutParams.MATCH_PARENT,
                    RelativeLayout.LayoutParams.MATCH_PARENT);
                mVideoView.setLayoutParams(videoParams);
                
                // Add touch listener to toggle controls visibility
                mVideoView.setOnTouchListener(new View.OnTouchListener() {
                    @Override
                    public boolean onTouch(View v, android.view.MotionEvent event) {
                        if (event.getAction() == android.view.MotionEvent.ACTION_DOWN) {
                            toggleControlsVisibility();
                        }
                        return true;
                    }
                });
                
                videoContainer.addView(mVideoView);
            } else {
                Log.e(TAG, "Failed to create VideoView");
                return null;
            }
            
            // Create loading indicator
            createLoadingIndicator(videoContainer);
            
            // Create pause placeholder
            createPausePlaceholder(videoContainer);
            
            // Bottom control bar (matching VideoPlayer style exactly)
            mBottomControlLayout = new LinearLayout(mActivity);
            mBottomControlLayout.setOrientation(LinearLayout.HORIZONTAL);
            mBottomControlLayout.setGravity(Gravity.CENTER_VERTICAL);
            mBottomControlLayout.setPadding(dp(0), dp(0), dp(0), dp(0));
            mBottomControlLayout.setBackgroundColor(Color.parseColor("#80000000")); // subtle overlay
            
            RelativeLayout.LayoutParams barParams = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT,
                dp(32)); // tweak to taste
            barParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
            mBottomControlLayout.setLayoutParams(barParams);
            
            // Each child fills 1/4 width (matching VideoPlayer exactly)
            LinearLayout.LayoutParams slot = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f);
            
            // --- Play / Pause ---
            mBtnPlayVideo = makeIconBtn(R.drawable.ic_play);
            mBtnPlayVideo.setLayoutParams(new LinearLayout.LayoutParams(slot));
            mBtnPlayVideo.setEnabled(false);
            mBtnPlayVideo.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // Reset auto-hide timer when interacting with controls
                    if (!mControlsVisible) {
                        showControls();
                    } else {
                        startAutoHideTimer();
                    }
                    // Toggle play/pause
                    if (!mIsPlaying) {
                        startPlayback();
                    } else {
                        togglePause();
                    }
                }
            });
            
            // --- Speed selector (replacing Mute button) ---
            mBtnSpeed1x = new Button(mActivity);
            mBtnSpeed1x.setText(mCurrentSpeed + ".0x");
            mBtnSpeed1x.setTextColor(Color.WHITE);
            mBtnSpeed1x.setTextSize(12);
            mBtnSpeed1x.setPadding(dp(8), dp(8), dp(8), dp(8));
            mBtnSpeed1x.setBackgroundColor(Color.TRANSPARENT);
            mBtnSpeed1x.setLayoutParams(new LinearLayout.LayoutParams(slot));
            mBtnSpeed1x.setEnabled(false);
            mBtnSpeed1x.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // Reset auto-hide timer when interacting with controls
                    if (!mControlsVisible) {
                        showControls();
                    } else {
                        startAutoHideTimer();
                    }
                    // Cycle through speeds: 1x -> 2x -> 3x -> 4x -> 1x
                    int nextSpeed = mCurrentSpeed + 1;
                    if (nextSpeed > 4) {
                        nextSpeed = 1;
                    }
                    setPlaybackSpeed(nextSpeed);
                }
            });
            
            // --- Time display (replacing Snapshot button) ---
            mTvPlaybackTime = new TextView(mActivity);
            mTvPlaybackTime.setText("00:00 / 00:00");
            mTvPlaybackTime.setTextColor(Color.WHITE);
            mTvPlaybackTime.setTextSize(12);
            mTvPlaybackTime.setPadding(dp(8), 0, dp(8), 0);
            mTvPlaybackTime.setGravity(Gravity.CENTER);
            mTvPlaybackTime.setLayoutParams(new LinearLayout.LayoutParams(slot));
            
            // --- Fullscreen ---
            AppCompatImageButton fullscreenBtn = makeIconBtn(R.drawable.ic_fullscreen);
            fullscreenBtn.setLayoutParams(new LinearLayout.LayoutParams(slot));
            fullscreenBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    enterFullscreen();
                }
            });
            
            // Add to bar and attach (matching VideoPlayer order)
            mBottomControlLayout.addView(mBtnPlayVideo);
            mBottomControlLayout.addView(mBtnSpeed1x);
            mBottomControlLayout.addView(mTvPlaybackTime);
            mBottomControlLayout.addView(fullscreenBtn);
            
            // Add control bar to video container (after video view to ensure it's on top)
            videoContainer.addView(mBottomControlLayout);
            
            // Ensure control bar is above video view (matching VideoPlayer)
            mBottomControlLayout.bringToFront();
            // Set elevation to ensure proper z-order (API 21+)
            mBottomControlLayout.setElevation(10f);
            
            // Start auto-hide timer
            startAutoHideTimer();
            mMainLayout.addView(videoContainer);
        
        // Search button layout
        LinearLayout buttonLayout = new LinearLayout(mActivity);
        buttonLayout.setOrientation(LinearLayout.HORIZONTAL);
        buttonLayout.setPadding(0, 0, 0, 8);
        
        // Start button
        mBtnStart = new Button(mActivity);
        mBtnStart.setText("Start Search");
        mBtnStart.setLayoutParams(new LinearLayout.LayoutParams(
            0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f));
        mBtnStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startSearch();
            }
        });
        buttonLayout.addView(mBtnStart);
        
        // Stop button
        mBtnStop = new Button(mActivity);
        mBtnStop.setText("Stop Search");
        mBtnStop.setLayoutParams(new LinearLayout.LayoutParams(
            0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f));
        mBtnStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopSearch();
            }
        });
        buttonLayout.addView(mBtnStop);
        
        mMainLayout.addView(buttonLayout);
        
        // Status text
        mTvStatus = new TextView(mActivity);
        mTvStatus.setText("");
        mTvStatus.setPadding(0, 0, 0, 8);
        mMainLayout.addView(mTvStatus);
        
        // List view
        mLstRecord = new ListView(mActivity);
        mLstRecord.setLayoutParams(new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.MATCH_PARENT));
        mLstRecord.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                try {
                    if (mFileList == null || position < 0 || position >= mFileList.size()) {
                        return;
                    }
                    
                    // Play selected video
                    RecordFile selectedFile = mFileList.get(position);
                    if (selectedFile == null) {
                        return;
                    }
                    
                    // Stop current playback if playing
                    if (mIsPlaying) {
                        stopPlayback();
                    }
                    
                    // Store playback parameters
                    mPlaybackFile = selectedFile.getOrginalFile();
                    mPlaybackLength = selectedFile.getOrginalLen();
                    mPlaybackChannel = selectedFile.getChn();
                    mCurrentPlayingFile = selectedFile;
                    
                    // Calculate total duration from file's time range
                    if (selectedFile.getEndTime() != null && selectedFile.getBeginTime() != null) {
                        mTotalDuration = selectedFile.getEndTime() - selectedFile.getBeginTime();
                    }
                    
                    // Enable playback button
                    if (mBtnPlayVideo != null) {
                        mBtnPlayVideo.setEnabled(true);
                    }
                    
                    // Start playback
                    startPlayback();
                } catch (Exception e) {
                    Log.e(TAG, "Error handling list item click: " + e.getMessage(), e);
                    e.printStackTrace();
                }
            }
        });
        mMainLayout.addView(mLstRecord);
        
            // Initialize Playback instance (only if VideoView was created successfully)
            if (mVideoView != null) {
                try {
                    mPlayback = new Playback(mActivity);
                    if (mPlayback != null) {
                        mPlayback.setVideoView(mVideoView);
                        mPlayback.setPlayerListener(new PlaybackListenerImpl());
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error creating Playback instance: " + e.getMessage(), e);
                }
            }
            
            // Set up list adapter
            if (mLstRecord != null) {
                mAdapter = new ArrayAdapter<String>(mActivity, android.R.layout.simple_list_item_1, new ArrayList<String>());
                mLstRecord.setAdapter(mAdapter);
            }
            
            return mMainLayout;
        } catch (Exception e) {
            Log.e(TAG, "Error creating view: " + e.getMessage(), e);
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * Start searching for video files
     */
    public void startSearch() {
        if (mSearchHelper != null) {
            mSearchHelper.startSearch();
        }
    }
    
    /**
     * Stop searching
     */
    public void stopSearch() {
        if (mSearchHelper != null) {
            mSearchHelper.stopSearch();
        }
    }
    
    /**
     * Show searching status
     */
    private void showSearching() {
        if (mTvStatus != null) {
            mTvStatus.setText("Searching...");
        }
    }
    
    /**
     * Cancel search
     */
    private void cancelSearch() {
        if (mTvStatus != null) {
            mTvStatus.setText("");
        }
    }
    
    /**
     * Update UI with search results
     */
    private void updateSearchResults() {
        if (mAdapter != null) {
            mAdapter.clear();
            for (RecordFile file : mFileList) {
                String dateStr = file.getYear() + "-" + String.format("%02d", file.getMonth()) + "-" + String.format("%02d", file.getDay());
                String timeStr = formatSeconds(file.getBeginTime()) + " - " + formatSeconds(file.getEndTime());
                String itemText = "Chn: " + file.getChn() + " | Date: " + dateStr + " | Time: " + timeStr + " | Type: " + (file.getFileType() == 0 ? "Normal" : "Alarm");
                mAdapter.add(itemText);
            }
            mAdapter.notifyDataSetChanged();
        }
    }
    
    /**
     * Automatically play the first result from search
     */
    private void playFirstResult() {
        if (mFileList == null || mFileList.size() == 0) {
            return;
        }
        
        if (mPlayback == null) {
            // Try to create Playback instance if VideoView exists
            if (mVideoView != null) {
                try {
                    mPlayback = new Playback(mActivity);
                    mPlayback.setVideoView(mVideoView);
                    mPlayback.setPlayerListener(new PlaybackListenerImpl());
                } catch (Exception e) {
                    Log.e(TAG, "Failed to create Playback instance: " + e.getMessage(), e);
                    return;
                }
            } else {
                Log.e(TAG, "Cannot create Playback: VideoView is null");
                return;
            }
        }
        
        try {
            RecordFile firstFile = mFileList.get(0);
            if (firstFile == null) {
                return;
            }
            
            // Store playback parameters
            mPlaybackFile = firstFile.getOrginalFile();
            mPlaybackLength = firstFile.getOrginalLen();
            mPlaybackChannel = firstFile.getChn();
            mCurrentPlayingFile = firstFile;
            
            // Check if file data is valid
            if (mPlaybackFile == null || mPlaybackLength <= 0) {
                Log.e(TAG, "Invalid file data: file=" + (mPlaybackFile != null ? "not null" : "null") + ", length=" + mPlaybackLength);
                return;
            }
            
            // Calculate total duration from file's time range
            if (firstFile.getEndTime() != null && firstFile.getBeginTime() != null) {
                mTotalDuration = firstFile.getEndTime() - firstFile.getBeginTime();
            }
            
            // Enable playback button
            if (mBtnPlayVideo != null) {
                mBtnPlayVideo.setEnabled(true);
            }
            
            // Automatically start playback
            startPlayback();
        } catch (Exception e) {
            Log.e(TAG, "Error playing first result: " + e.getMessage(), e);
        }
    }
    
    /**
     * Start video playback (matching VideoSearchActivity.startPlayback())
     */
    private void startPlayback() {
        if (mPlayback == null || mPlaybackFile == null || mPlaybackLength <= 0) {
            Log.e(TAG, "Cannot start playback: Invalid state");
            return;
        }
        
        if (mDevIdno == null || mDevIdno.isEmpty()) {
            Log.e(TAG, "Cannot start playback: DevIdno is invalid");
            return;
        }
        
        if (!mIsPlaying) {
            try {
                // Show loading indicator while starting playback
                updateLoadingAndPlaceholder(true, false);
                
                mPlayback.setPlayerDevIdno(mDevIdno);
                if (mIsDirect) {
                    if (mServer != null && !mServer.isEmpty() && mPort > 0) {
                        mPlayback.setLanInfo(mServer, mPort);
                    } else {
                        Log.e(TAG, "Cannot set LAN info: Server or port is invalid");
                        updateLoadingAndPlaceholder(false, false);
                        return;
                    }
                }
                
                boolean startResult = mPlayback.StartVod(mPlaybackFile, mPlaybackLength, mPlaybackChannel);
                if (!startResult) {
                    Log.e(TAG, "StartVod failed");
                    mIsPlaying = false;
                    updateLoadingAndPlaceholder(false, false);
                    return;
                }
                
                mIsPlaying = true;
                mIsPaused = false;
                mCurrentPosition = 0;
                mCurrentSpeed = 1; // Reset to 1x when starting playback
                mLastUpdateTime = 0;
                
                if (mTvPlaybackTime != null) {
                    if (mTotalDuration > 0) {
                        mTvPlaybackTime.setText("00:00 / " + formatTime(mTotalDuration));
                    } else {
                        mTvPlaybackTime.setText("00:00 / 00:00");
                    }
                }
                
                // Update play button to show pause icon
                if (mBtnPlayVideo != null) {
                    mBtnPlayVideo.setImageDrawable(AppCompatResources.getDrawable(mActivity, R.drawable.ic_pause));
                    mBtnPlayVideo.setEnabled(true);
                }
                
                // Enable speed button
                if (mBtnSpeed1x != null) {
                    mBtnSpeed1x.setEnabled(true);
                    mBtnSpeed1x.setText(mCurrentSpeed + ".0x");
                }
                
                if (mListener != null && mCurrentPlayingFile != null) {
                    mListener.onPlaybackStarted(mCurrentPlayingFile);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error starting playback: " + e.getMessage(), e);
                mIsPlaying = false;
            }
        }
    }
    
    /**
     * Stop video playback
     */
    public void stopPlayback() {
        if (mIsPlaying && mPlayback != null) {
            try {
                mPlayback.StopVod();
            } catch (Exception e) {
                Log.e(TAG, "Error stopping playback: " + e.getMessage(), e);
            }
            mIsPlaying = false;
            mIsPaused = false;
            mTotalDuration = 0;
            mCurrentPosition = 0;
            mLastUpdateTime = 0;
            mCurrentSpeed = 1; // Reset to 1x
            
            // Hide loading indicator and show pause placeholder when stopped
            updateLoadingAndPlaceholder(false, true);
            
            if (mTvPlaybackTime != null) {
                mTvPlaybackTime.setText("00:00 / 00:00");
            }
            
            // Update play button to show play icon
            if (mBtnPlayVideo != null) {
                mBtnPlayVideo.setImageDrawable(AppCompatResources.getDrawable(mActivity, R.drawable.ic_play));
                mBtnPlayVideo.setEnabled(false);
            }
            
            // Disable speed button
            if (mBtnSpeed1x != null) {
                mBtnSpeed1x.setEnabled(false);
                mBtnSpeed1x.setText("1.0x");
            }
            
            if (mListener != null) {
                mListener.onPlaybackStopped();
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
            if (mBtnPlayVideo != null) {
                mBtnPlayVideo.setImageDrawable(AppCompatResources.getDrawable(mActivity, 
                    mIsPaused ? R.drawable.ic_play : R.drawable.ic_pause));
            }
            // Show pause placeholder when paused, hide when playing
            updateLoadingAndPlaceholder(false, mIsPaused);
            // Update fullscreen controls if in fullscreen
            if (mIsFullscreen) {
                updateFullscreenControls();
            }
        }
    }
    
    /**
     * Set playback speed (1x, 2x, 3x, 4x)
     */
    private void setPlaybackSpeed(int speed) {
        if (mIsPlaying && mPlayback != null && speed >= 1 && speed <= 4) {
            mPlayback.setPlayRate(speed);
            mCurrentSpeed = speed;
            if (mBtnSpeed1x != null) {
                mBtnSpeed1x.setText(mCurrentSpeed + ".0x");
            }
            // Update fullscreen speed button if in fullscreen
            if (mIsFullscreen && mFullscreenSpeedBtn != null) {
                mFullscreenSpeedBtn.setText(mCurrentSpeed + ".0x");
            }
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
     * Format seconds to HH:MM:SS
     */
    private String formatSeconds(int seconds) {
        int hours = seconds / 3600;
        int minutes = (seconds % 3600) / 60;
        int secs = seconds % 60;
        return String.format("%02d:%02d:%02d", hours, minutes, secs);
    }
    
    /**
     * PlaybackListener implementation to update time display
     */
    private class PlaybackListenerImpl implements Playback.PlaybackListener {
        @Override
        public void onBeginPlay() {
            mActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    // Hide loading indicator when playback begins
                    updateLoadingAndPlaceholder(false, false);
                    if (mIsFullscreen) {
                        updateFullscreenLoadingAndPlaceholder(false, false);
                    }
                    
                    if (mBtnPlayVideo != null) {
                        mBtnPlayVideo.setImageDrawable(AppCompatResources.getDrawable(mActivity, R.drawable.ic_pause));
                        mBtnPlayVideo.setEnabled(true);
                    }
                    if (mBtnSpeed1x != null) {
                        mBtnSpeed1x.setEnabled(true);
                        mBtnSpeed1x.setText(mCurrentSpeed + ".0x");
                    }
                    
                    // Update fullscreen controls if in fullscreen
                    if (mIsFullscreen) {
                        updateFullscreenControls();
                    }
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
                
                // Check if playback has reached the end (within 1 second tolerance)
                // If current position is at or very close to total duration, treat as completed
                boolean isPlaybackComplete = (currentPosition >= totalDuration - 1) && mIsPlaying;
                
                // Update time display (throttled, but always eventually updated)
                final int finalCurrentPos = currentPosition;
                final int finalTotalDur = totalDuration;
                final boolean playbackComplete = isPlaybackComplete;
                mActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        updateTimeDisplay(finalCurrentPos, finalTotalDur);
                        // Update fullscreen time if in fullscreen
                        if (mIsFullscreen && mFullscreenTimeText != null) {
                            mFullscreenTimeText.setText(formatTime(finalCurrentPos) + " / " + formatTime(finalTotalDur));
                        }
                        
                        // If playback has reached the end, manually trigger end-of-playback behavior
                        if (playbackComplete && mIsPlaying) {
                            Log.d(TAG, "Playback reached end (current: " + finalCurrentPos + " >= total: " + finalTotalDur + "), triggering onEndPlay behavior");
                            // Manually call the end-of-playback logic
                            mIsPlaying = false;
                            mIsPaused = false;
                            updateLoadingAndPlaceholder(false, true);
                            if (mIsFullscreen) {
                                updateFullscreenLoadingAndPlaceholder(false, true);
                            }
                            
                            // Update play button to show play icon (not pause)
                            if (mBtnPlayVideo != null) {
                                mBtnPlayVideo.setImageDrawable(AppCompatResources.getDrawable(mActivity, R.drawable.ic_play));
                                mBtnPlayVideo.setEnabled(true);
                                mBtnPlayVideo.invalidate();
                                mBtnPlayVideo.requestLayout();
                            }
                            
                            if (mBtnSpeed1x != null) {
                                mBtnSpeed1x.setEnabled(false);
                                mBtnSpeed1x.setText("1.0x");
                            }
                            mCurrentSpeed = 1; // Reset to 1x
                            
                            if (mTvPlaybackTime != null) {
                                mTvPlaybackTime.setText(formatTime(finalTotalDur) + " / " + formatTime(finalTotalDur));
                            }
                            
                            // Update fullscreen controls if in fullscreen
                            if (mIsFullscreen) {
                                updateFullscreenControls();
                            }
                            
                            // Stop playback to ensure clean state
                            if (mPlayback != null) {
                                try {
                                    mPlayback.StopVod();
                                } catch (Exception e) {
                                    Log.e(TAG, "Error stopping playback: " + e.getMessage());
                                }
                            }
                        }
                    }
                });
            }
        }
        
        @Override
        public void onEndPlay() {
            mActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mIsPlaying = false;
                    mIsPaused = false;
                    // Hide loading indicator and show pause placeholder when playback ends
                    updateLoadingAndPlaceholder(false, true);
                    if (mIsFullscreen) {
                        updateFullscreenLoadingAndPlaceholder(false, true);
                    }
                    
                    // Update play button to show play icon (not pause)
                    if (mBtnPlayVideo != null) {
                        mBtnPlayVideo.setImageDrawable(AppCompatResources.getDrawable(mActivity, R.drawable.ic_play));
                        mBtnPlayVideo.setEnabled(true); // Keep enabled so user can play again
                        // Force button to refresh
                        mBtnPlayVideo.invalidate();
                        mBtnPlayVideo.requestLayout();
                    }
                    
                    if (mBtnSpeed1x != null) {
                        mBtnSpeed1x.setEnabled(false);
                        mBtnSpeed1x.setText("1.0x");
                    }
                    mCurrentSpeed = 1; // Reset to 1x
                    
                    if (mTvPlaybackTime != null) {
                        mTvPlaybackTime.setText("00:00 / 00:00");
                    }
                    
                    // Update fullscreen controls if in fullscreen
                    if (mIsFullscreen) {
                        updateFullscreenControls();
                    }
                    
                    Log.d(TAG, "Playback ended - button icon changed to play, mIsPlaying: " + mIsPlaying);
                }
            });
        }
        
        @Override
        public void onClick(VideoView view, int index) {
            // Handle video view click (e.g., pause/resume)
        }
        
        @Override
        public void onDbClick(VideoView view, int index) {
            // Handle video view double click (e.g., fullscreen)
        }
    }
    
    /**
     * Create loading indicator (matching VideoPlayer style)
     */
    private void createLoadingIndicator(RelativeLayout container) {
        mLoadingIndicator = new android.widget.ProgressBar(mActivity);
        
        RelativeLayout.LayoutParams loadingParams = new RelativeLayout.LayoutParams(
            dp(30), dp(30));
        loadingParams.addRule(RelativeLayout.CENTER_IN_PARENT);
        mLoadingIndicator.setLayoutParams(loadingParams);
        
        // Set custom drawable for loading animation
        mLoadingIndicator.setIndeterminateDrawable(ContextCompat.getDrawable(mActivity, R.drawable.ic_loading));
        
        // Initially hidden (will show when playback starts)
        mLoadingIndicator.setVisibility(View.GONE);
        
        container.addView(mLoadingIndicator);
    }
    
    /**
     * Create pause placeholder (matching VideoPlayer style)
     */
    private void createPausePlaceholder(RelativeLayout container) {
        mPausePlaceholder = new android.widget.ImageView(mActivity);
        
        RelativeLayout.LayoutParams placeholderParams = new RelativeLayout.LayoutParams(
            RelativeLayout.LayoutParams.MATCH_PARENT,
            RelativeLayout.LayoutParams.MATCH_PARENT);
        placeholderParams.addRule(RelativeLayout.CENTER_IN_PARENT);
        mPausePlaceholder.setLayoutParams(placeholderParams);
        
        // Set placeholder image
        mPausePlaceholder.setImageDrawable(ContextCompat.getDrawable(mActivity, R.drawable.placeholder));
        mPausePlaceholder.setScaleType(android.widget.ImageView.ScaleType.CENTER_CROP);
        
        // Initially hidden (video is not paused initially)
        mPausePlaceholder.setVisibility(View.GONE);
        
        container.addView(mPausePlaceholder);
    }
    
    /**
     * Update loading indicator and pause placeholder visibility
     */
    private void updateLoadingAndPlaceholder(boolean showLoading, boolean showPause) {
        if (mLoadingIndicator != null) {
            mLoadingIndicator.setVisibility(showLoading ? View.VISIBLE : View.GONE);
        }
        if (mPausePlaceholder != null) {
            mPausePlaceholder.setVisibility(showPause ? View.VISIBLE : View.GONE);
        }
        mIsLoading = showLoading;
    }
    
    /**
     * Toggle controls visibility (called when video view is tapped)
     */
    private void toggleControlsVisibility() {
        if (mBottomControlLayout == null) return;
        
        // Cancel any pending hide operation
        if (mHideControlsRunnable != null) {
            mControlsHandler.removeCallbacks(mHideControlsRunnable);
        }
        
        if (mControlsVisible) {
            hideControls();
        } else {
            showControls();
        }
    }
    
    /**
     * Show controls with animation
     */
    private void showControls() {
        if (mBottomControlLayout == null || mControlsVisible) return;
        
        mControlsVisible = true;
        mBottomControlLayout.setVisibility(View.VISIBLE);
        mBottomControlLayout.setAlpha(0.0f); // Start from transparent
        
        // Animate fade in
        mBottomControlLayout.animate()
            .alpha(1.0f)
            .setDuration(300)
            .setListener(new android.animation.AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(android.animation.Animator animation) {
                    // Start auto-hide timer after showing
                    startAutoHideTimer();
                }
            });
    }
    
    /**
     * Hide controls with animation
     */
    private void hideControls() {
        if (mBottomControlLayout == null || !mControlsVisible) return;
        
        mControlsVisible = false;
        
        // Animate fade out
        mBottomControlLayout.animate()
            .alpha(0.0f)
            .setDuration(300)
            .setListener(new android.animation.AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(android.animation.Animator animation) {
                    mBottomControlLayout.setVisibility(View.GONE);
                }
            });
    }
    
    /**
     * Start auto-hide timer (hides controls after 5 seconds)
     */
    private void startAutoHideTimer() {
        // Cancel any existing timer
        if (mHideControlsRunnable != null) {
            mControlsHandler.removeCallbacks(mHideControlsRunnable);
        }
        
        // Auto-hide after 5 seconds
        mHideControlsRunnable = new Runnable() {
            @Override
            public void run() {
                if (mControlsVisible) {
                    hideControls();
                }
            }
        };
        mControlsHandler.postDelayed(mHideControlsRunnable, 5000);
    }
    
    /**
     * Stop auto-hide timer
     */
    private void stopAutoHideTimer() {
        if (mHideControlsRunnable != null) {
            mControlsHandler.removeCallbacks(mHideControlsRunnable);
            mHideControlsRunnable = null;
        }
    }
    
    /**
     * Enter fullscreen mode
     */
    public void enterFullscreen() {
        if (mIsFullscreen) {
            Log.d(TAG, "Already in fullscreen mode");
            return;
        }
        
        if (mPlayback == null) {
            Log.e(TAG, "Cannot enter fullscreen: Playback is null");
            return;
        }
        
        if (mVideoView == null) {
            Log.e(TAG, "Cannot enter fullscreen: VideoView is null");
            return;
        }
        
        mIsFullscreen = true;
        
        Log.d(TAG, "Starting fullscreen entry, mIsPlaying: " + mIsPlaying + ", mPlaybackFile: " + (mPlaybackFile != null) + ", mPlaybackLength: " + mPlaybackLength);
        
        // Create fullscreen layout
        createFullscreenLayout();
        
        Log.d(TAG, "Fullscreen layout created, mFullscreenLayout: " + (mFullscreenLayout != null) + ", mFullscreenVideoView: " + (mFullscreenVideoView != null));
        
        // Add fullscreen layout to activity's root view
        try {
            android.view.ViewGroup rootView = (android.view.ViewGroup) mActivity.findViewById(android.R.id.content);
            if (rootView != null) {
                android.widget.FrameLayout.LayoutParams fullscreenParams = new android.widget.FrameLayout.LayoutParams(
                    android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                    android.widget.FrameLayout.LayoutParams.MATCH_PARENT);
                fullscreenParams.gravity = Gravity.FILL;
                rootView.addView(mFullscreenLayout, fullscreenParams);
                
                mFullscreenLayout.bringToFront();
                mFullscreenLayout.setVisibility(View.VISIBLE);
                
                Log.d(TAG, "Fullscreen layout added to rootView");
            } else {
                Log.e(TAG, "Cannot find root view, trying addContentView");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error adding fullscreen layout to rootView: " + e.getMessage(), e);
        }
        
        // Fallback: Attach to window if not already attached (matching VideoPlayer)
        android.view.ViewGroup decor = (android.view.ViewGroup) mActivity.getWindow().getDecorView();
        decor.post(new Runnable() {
            @Override
            public void run() {
                try {
                    if (mFullscreenLayout != null && mFullscreenLayout.getParent() == null) {
                        mActivity.getWindow().addContentView(
                            mFullscreenLayout,
                            new android.view.ViewGroup.LayoutParams(
                                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                                android.view.ViewGroup.LayoutParams.MATCH_PARENT
                            )
                        );
                        Log.d(TAG, "Fullscreen layout added via addContentView");
                    }
                    
                    if (mFullscreenLayout != null) {
                        mFullscreenLayout.bringToFront();
                        mFullscreenLayout.setElevation(1000f);
                        mFullscreenLayout.setVisibility(View.VISIBLE);
                        
                        // Hide system UI for true fullscreen
                        hideSystemUI();
                        
                        // Transfer playback to fullscreen view (after layout is attached)
                        transferToFullscreen();
                        
                        // Animate fullscreen entry
                        animateFullscreenEntry();
                        
                        mFullscreenLayout.requestLayout();
                        mFullscreenLayout.invalidate();
                        
                        Log.d(TAG, "Fullscreen layout made visible and animated, mIsPlaying: " + mIsPlaying);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error in decor.post: " + e.getMessage(), e);
                }
            }
        });
        
        // Rotate video view to landscape
        mActivity.getWindow().getDecorView().post(new Runnable() {
            @Override
            public void run() {
                rotateVideoToLandscape();
                if (mFullscreenLayout != null) {
                    mFullscreenLayout.post(new Runnable() {
                        @Override
                        public void run() {
                            mFullscreenLayout.requestLayout();
                            mFullscreenLayout.invalidate();
                        }
                    });
                }
            }
        });
        
        Log.d(TAG, "Entered fullscreen mode");
    }
    
    /**
     * Exit fullscreen mode
     */
    public void exitFullscreen() {
        if (!mIsFullscreen) {
            return;
        }
        
        // Stop auto-hide timer
        stopFullscreenAutoHideTimer();
        
        // Reset controls visibility
        mFullscreenControlsVisible = true;
        if (mFullscreenControlsLayout != null) {
            mFullscreenControlsLayout.setAlpha(1.0f);
            mFullscreenControlsLayout.setVisibility(View.VISIBLE);
        }
        
        // Restore video view to portrait orientation
        restoreVideoToPortrait();
        
        // Transfer playback back to original view
        transferFromFullscreen();
        
        // Show system UI
        showSystemUI();
        
        // Animate fullscreen exit
        if (mFullscreenLayout != null) {
            mFullscreenLayout.animate()
                .scaleX(0.0f)
                .scaleY(0.0f)
                .alpha(0.0f)
                .setDuration(300)
                .setInterpolator(new android.view.animation.DecelerateInterpolator())
                .setListener(new android.animation.AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(android.animation.Animator animation) {
                        if (mFullscreenLayout != null && mFullscreenLayout.getParent() != null) {
                            try {
                                ((android.view.ViewGroup) mFullscreenLayout.getParent()).removeView(mFullscreenLayout);
                            } catch (Exception e) {
                                Log.e(TAG, "Error removing fullscreen layout: " + e.getMessage());
                            }
                        }
                    }
                });
        }
        
        mIsFullscreen = false;
        Log.d(TAG, "Exited fullscreen mode");
    }
    
    /**
     * Handle back button press to exit fullscreen
     */
    public boolean onBackPressed() {
        if (mIsFullscreen) {
            exitFullscreen();
            return true;
        }
        return false;
    }
    
    /**
     * Create fullscreen layout
     */
    private void createFullscreenLayout() {
        // Fullscreen container
        mFullscreenLayout = new android.widget.FrameLayout(mActivity);
        mFullscreenLayout.setBackgroundColor(Color.BLACK);
        mFullscreenLayout.setFitsSystemWindows(false);
        mFullscreenLayout.setVisibility(View.VISIBLE);
        
        // Fullscreen VideoView
        mFullscreenVideoView = new VideoView(mActivity, 0);
        android.widget.FrameLayout.LayoutParams videoParams = new android.widget.FrameLayout.LayoutParams(
            android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
            android.widget.FrameLayout.LayoutParams.MATCH_PARENT);
        videoParams.gravity = Gravity.CENTER;
        mFullscreenVideoView.setLayoutParams(videoParams);
        mFullscreenVideoView.setBackgroundColor(Color.BLACK);
        mFullscreenVideoView.setFitsSystemWindows(false);
        
        // Add touch listener for hide/show controls
        mFullscreenVideoView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, android.view.MotionEvent event) {
                if (event.getAction() == android.view.MotionEvent.ACTION_DOWN) {
                    toggleFullscreenControlsVisibility();
                }
                return true;
            }
        });
        
        // Create fullscreen loading indicator
        createFullscreenLoadingIndicator();
        
        // Create fullscreen pause placeholder
        createFullscreenPausePlaceholder();
        
        // Controls container (bottom bar) - matching VideoPlayer style exactly
        mFullscreenControlsLayout = new LinearLayout(mActivity);
        mFullscreenControlsLayout.setOrientation(LinearLayout.HORIZONTAL);
        mFullscreenControlsLayout.setGravity(Gravity.CENTER_VERTICAL);
        mFullscreenControlsLayout.setPadding(dp(0), dp(0), dp(0), dp(0));
        mFullscreenControlsLayout.setBackgroundColor(Color.parseColor("#80000000"));
        
        android.widget.FrameLayout.LayoutParams controlsParams = new android.widget.FrameLayout.LayoutParams(
            android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
            dp(32));
        controlsParams.gravity = Gravity.BOTTOM;
        mFullscreenControlsLayout.setLayoutParams(controlsParams);
        
        // Each child fills 1/4 width (matching VideoPlayer exactly)
        LinearLayout.LayoutParams slot = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f);
        
        // --- Play / Pause ---
        mFullscreenPlayPauseBtn = makeIconBtn(mIsPlaying ? R.drawable.ic_pause : R.drawable.ic_play);
        mFullscreenPlayPauseBtn.setLayoutParams(new LinearLayout.LayoutParams(slot));
        mFullscreenPlayPauseBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleFullscreenPlayPause();
            }
        });
        
        // --- Speed button ---
        mFullscreenSpeedBtn = new Button(mActivity);
        mFullscreenSpeedBtn.setText(mCurrentSpeed + ".0x");
        mFullscreenSpeedBtn.setTextColor(Color.WHITE);
        mFullscreenSpeedBtn.setTextSize(12);
        mFullscreenSpeedBtn.setPadding(dp(8), dp(8), dp(8), dp(8));
        mFullscreenSpeedBtn.setBackgroundColor(Color.TRANSPARENT);
        mFullscreenSpeedBtn.setLayoutParams(new LinearLayout.LayoutParams(slot));
        mFullscreenSpeedBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int nextSpeed = mCurrentSpeed + 1;
                if (nextSpeed > 4) {
                    nextSpeed = 1;
                }
                setPlaybackSpeed(nextSpeed);
                if (mFullscreenSpeedBtn != null) {
                    mFullscreenSpeedBtn.setText(mCurrentSpeed + ".0x");
                }
            }
        });
        
        // --- Time display ---
        mFullscreenTimeText = new TextView(mActivity);
        mFullscreenTimeText.setText("00:00 / 00:00");
        mFullscreenTimeText.setTextColor(Color.WHITE);
        mFullscreenTimeText.setTextSize(12);
        mFullscreenTimeText.setPadding(dp(8), 0, dp(8), 0);
        mFullscreenTimeText.setGravity(Gravity.CENTER);
        mFullscreenTimeText.setLayoutParams(new LinearLayout.LayoutParams(slot));
        
        // --- Close button (Fullscreen exit) ---
        mFullscreenCloseBtn = makeIconBtn(R.drawable.ic_fullscreen_exit);
        mFullscreenCloseBtn.setLayoutParams(new LinearLayout.LayoutParams(slot));
        mFullscreenCloseBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                exitFullscreen();
            }
        });
        
        // Add to bar and attach (matching VideoPlayer order)
        mFullscreenControlsLayout.addView(mFullscreenPlayPauseBtn);
        mFullscreenControlsLayout.addView(mFullscreenSpeedBtn);
        mFullscreenControlsLayout.addView(mFullscreenTimeText);
        mFullscreenControlsLayout.addView(mFullscreenCloseBtn);
        
        // Add to fullscreen layout
        mFullscreenLayout.addView(mFullscreenVideoView);
        mFullscreenLayout.addView(mFullscreenControlsLayout);
        
        // Ensure controls are above video
        mFullscreenControlsLayout.bringToFront();
        mFullscreenControlsLayout.setElevation(1000f);
        
        // Ensure controls are positioned correctly (matching VideoPlayer)
        mFullscreenLayout.post(new Runnable() {
            @Override
            public void run() {
                ensureFullscreenControlsPosition();
            }
        });
        
        // Start auto-hide timer
        mFullscreenControlsVisible = true;
        startFullscreenAutoHideTimer();
    }
    
    /**
     * Ensure fullscreen controls are positioned correctly (matching VideoPlayer)
     */
    private void ensureFullscreenControlsPosition() {
        if (mFullscreenLayout != null && mFullscreenControlsLayout != null) {
            // Find the controls layout and ensure it's positioned at BOTTOM (for horizontal bar)
            // This matches VideoPlayer's ensureControlsAtBottom but for horizontal layout
            android.widget.FrameLayout.LayoutParams controlsParams =
                (android.widget.FrameLayout.LayoutParams) mFullscreenControlsLayout.getLayoutParams();
            if (controlsParams != null) {
                // For horizontal bottom bar, keep it at BOTTOM
                controlsParams.gravity = Gravity.BOTTOM;
                controlsParams.width = android.widget.FrameLayout.LayoutParams.MATCH_PARENT;
                controlsParams.height = dp(32);
                controlsParams.setMargins(0, 0, 0, 0);
                mFullscreenControlsLayout.setLayoutParams(controlsParams);
                mFullscreenControlsLayout.bringToFront();
                mFullscreenControlsLayout.requestLayout();
                mFullscreenControlsLayout.invalidate();
                
                Log.d(TAG, "Fullscreen controls positioned at bottom");
            }
        }
    }
    
    /**
     * Transfer playback to fullscreen view (using same player instance)
     */
    private void transferToFullscreen() {
        if (mPlayback == null || mFullscreenVideoView == null) {
            Log.e(TAG, "Cannot transfer to fullscreen: Playback or VideoView is null");
            return;
        }
        
        // Store reference to original Playback for fullscreen (same instance)
        mFullscreenPlayback = mPlayback;
        
        // Simply change the video view to fullscreen view (same as VideoPlayer)
        // The Playback instance continues playing on the new view
        mFullscreenPlayback.setVideoView(mFullscreenVideoView);
        
        // Update control button states
        updateFullscreenControls();
        
        Log.d(TAG, "Transferred playback to fullscreen using same player instance, isPlaying: " + mIsPlaying);
    }
    
    /**
     * Transfer playback back to original view (using same player instance)
     */
    private void transferFromFullscreen() {
        if (mFullscreenPlayback != null && mVideoView != null) {
            // Simply change the video view back to original view (same as VideoPlayer)
            // The Playback instance continues playing on the original view
            mFullscreenPlayback.setVideoView(mVideoView);
            
            // Update placeholder state based on current video state
            boolean isVideoPlaying = mFullscreenPlayback.isViewing();
            updateLoadingAndPlaceholder(false, !isVideoPlaying);
            
            mFullscreenPlayback = null;
            
            Log.d(TAG, "Transferred playback back to original view using same player instance, isPlaying: " + isVideoPlaying);
        }
    }
    
    /**
     * Toggle play/pause in fullscreen
     */
    private void toggleFullscreenPlayPause() {
        if (mFullscreenPlayback != null) {
            if (mIsPlaying) {
                togglePause();
            } else {
                startPlayback();
            }
            updateFullscreenControls();
        }
    }
    
    /**
     * Update fullscreen controls
     */
    private void updateFullscreenControls() {
        if (mFullscreenPlayPauseBtn != null) {
            if (mIsPlaying) {
                mFullscreenPlayPauseBtn.setImageDrawable(AppCompatResources.getDrawable(mActivity, 
                    mIsPaused ? R.drawable.ic_play : R.drawable.ic_pause));
                updateFullscreenLoadingAndPlaceholder(false, mIsPaused);
            } else {
                // Playback is not playing (stopped or ended)
                mFullscreenPlayPauseBtn.setImageDrawable(AppCompatResources.getDrawable(mActivity, R.drawable.ic_play));
                updateFullscreenLoadingAndPlaceholder(false, true);
                // Force button to refresh
                mFullscreenPlayPauseBtn.invalidate();
                mFullscreenPlayPauseBtn.requestLayout();
            }
        }
        if (mFullscreenSpeedBtn != null) {
            mFullscreenSpeedBtn.setText(mCurrentSpeed + ".0x");
        }
        if (mFullscreenTimeText != null && mTvPlaybackTime != null) {
            mFullscreenTimeText.setText(mTvPlaybackTime.getText());
        }
    }
    
    /**
     * Create fullscreen loading indicator
     */
    private void createFullscreenLoadingIndicator() {
        mFullscreenLoadingIndicator = new android.widget.ProgressBar(mActivity);
        android.widget.FrameLayout.LayoutParams loadingParams = new android.widget.FrameLayout.LayoutParams(
            dp(60), dp(60));
        loadingParams.gravity = Gravity.CENTER;
        mFullscreenLoadingIndicator.setLayoutParams(loadingParams);
        mFullscreenLoadingIndicator.setIndeterminateDrawable(ContextCompat.getDrawable(mActivity, R.drawable.ic_loading));
        mFullscreenLoadingIndicator.setVisibility(View.GONE);
        mFullscreenLayout.addView(mFullscreenLoadingIndicator);
    }
    
    /**
     * Create fullscreen pause placeholder
     */
    private void createFullscreenPausePlaceholder() {
        mFullscreenPausePlaceholder = new android.widget.ImageView(mActivity);
        android.widget.FrameLayout.LayoutParams placeholderParams = new android.widget.FrameLayout.LayoutParams(
            android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
            android.widget.FrameLayout.LayoutParams.MATCH_PARENT);
        placeholderParams.gravity = Gravity.CENTER;
        mFullscreenPausePlaceholder.setLayoutParams(placeholderParams);
        mFullscreenPausePlaceholder.setImageDrawable(ContextCompat.getDrawable(mActivity, R.drawable.placeholder));
        mFullscreenPausePlaceholder.setScaleType(android.widget.ImageView.ScaleType.CENTER_CROP);
        mFullscreenPausePlaceholder.setRotation(90f);
        mFullscreenPausePlaceholder.setBackgroundColor(Color.BLACK);
        mFullscreenPausePlaceholder.setClickable(false);
        mFullscreenPausePlaceholder.setFocusable(false);
        mFullscreenPausePlaceholder.setElevation(500f);
        mFullscreenPausePlaceholder.setVisibility(View.GONE);
        mFullscreenLayout.addView(mFullscreenPausePlaceholder);
    }
    
    /**
     * Update fullscreen loading and placeholder
     */
    private void updateFullscreenLoadingAndPlaceholder(boolean isLoading, boolean isPaused) {
        if (mFullscreenLoadingIndicator != null) {
            mFullscreenLoadingIndicator.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        }
        if (mFullscreenPausePlaceholder != null) {
            mFullscreenPausePlaceholder.setVisibility(isPaused ? View.VISIBLE : View.GONE);
        }
    }
    
    /**
     * Toggle fullscreen controls visibility
     */
    private void toggleFullscreenControlsVisibility() {
        if (mFullscreenControlsLayout == null) return;
        
        if (mFullscreenHideControlsRunnable != null) {
            mFullscreenControlsHandler.removeCallbacks(mFullscreenHideControlsRunnable);
        }
        
        if (mFullscreenControlsVisible) {
            hideFullscreenControls();
        } else {
            showFullscreenControls();
        }
    }
    
    /**
     * Show fullscreen controls
     */
    private void showFullscreenControls() {
        if (mFullscreenControlsLayout == null || mFullscreenControlsVisible) return;
        
        mFullscreenControlsVisible = true;
        mFullscreenControlsLayout.setVisibility(View.VISIBLE);
        mFullscreenControlsLayout.setAlpha(0.0f);
        
        mFullscreenControlsLayout.animate()
            .alpha(1.0f)
            .setDuration(300)
            .setListener(new android.animation.AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(android.animation.Animator animation) {
                    startFullscreenAutoHideTimer();
                }
            });
    }
    
    /**
     * Hide fullscreen controls
     */
    private void hideFullscreenControls() {
        if (mFullscreenControlsLayout == null || !mFullscreenControlsVisible) return;
        
        mFullscreenControlsVisible = false;
        
        mFullscreenControlsLayout.animate()
            .alpha(0.0f)
            .setDuration(300)
            .setListener(new android.animation.AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(android.animation.Animator animation) {
                    mFullscreenControlsLayout.setVisibility(View.GONE);
                }
            });
    }
    
    /**
     * Start fullscreen auto-hide timer
     */
    private void startFullscreenAutoHideTimer() {
        if (mFullscreenHideControlsRunnable != null) {
            mFullscreenControlsHandler.removeCallbacks(mFullscreenHideControlsRunnable);
        }
        
        mFullscreenHideControlsRunnable = new Runnable() {
            @Override
            public void run() {
                if (mFullscreenControlsVisible) {
                    hideFullscreenControls();
                }
            }
        };
        mFullscreenControlsHandler.postDelayed(mFullscreenHideControlsRunnable, 5000);
    }
    
    /**
     * Stop fullscreen auto-hide timer
     */
    private void stopFullscreenAutoHideTimer() {
        if (mFullscreenHideControlsRunnable != null) {
            mFullscreenControlsHandler.removeCallbacks(mFullscreenHideControlsRunnable);
            mFullscreenHideControlsRunnable = null;
        }
    }
    
    /**
     * Hide system UI for fullscreen
     */
    private void hideSystemUI() {
        mActivity.getWindow().getDecorView().setSystemUiVisibility(
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            | View.SYSTEM_UI_FLAG_FULLSCREEN
            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
    }
    
    /**
     * Show system UI
     */
    private void showSystemUI() {
        mActivity.getWindow().getDecorView().setSystemUiVisibility(0);
        mActivity.getWindow().clearFlags(
            android.view.WindowManager.LayoutParams.FLAG_FULLSCREEN
            | android.view.WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
            | android.view.WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN);
        mActivity.getWindow().getDecorView().requestLayout();
    }
    
    /**
     * Rotate video to landscape
     */
    private void rotateVideoToLandscape() {
        if (mFullscreenVideoView != null) {
            // Rotate the video view 90 degrees to landscape
            mFullscreenVideoView.setRotation(90f);
            
            // Adjust the layout parameters to accommodate the rotation
            android.widget.FrameLayout.LayoutParams params = (android.widget.FrameLayout.LayoutParams) mFullscreenVideoView.getLayoutParams();
            if (params != null) {
                // Get screen dimensions
                android.util.DisplayMetrics displayMetrics = mActivity.getResources().getDisplayMetrics();
                int screenWidth = displayMetrics.widthPixels;
                int screenHeight = displayMetrics.heightPixels;
                
                // Swap width and height for landscape (since we're rotating 90 degrees)
                // When rotated, the view's width becomes the screen height and height becomes screen width
                params.width = screenHeight;  // Use height as width for rotated view
                params.height = screenWidth;  // Use width as height for rotated view
                params.gravity = Gravity.CENTER;
                
                mFullscreenVideoView.setLayoutParams(params);
                
                Log.d(TAG, "Video rotated to landscape, size: " + params.width + "x" + params.height);
            }
            
            // Ensure controls stay positioned correctly after rotation (matching VideoPlayer)
            if (mFullscreenLayout != null) {
                mFullscreenLayout.post(new Runnable() {
                    @Override
                    public void run() {
                        ensureFullscreenControlsPosition();
                    }
                });
            }
        }
    }
    
    /**
     * Restore video to portrait
     */
    private void restoreVideoToPortrait() {
        if (mFullscreenVideoView != null) {
            // Restore the video view to 0 degrees (portrait)
            mFullscreenVideoView.setRotation(0f);
            
            // Restore the layout parameters to original (full screen)
            android.widget.FrameLayout.LayoutParams params = (android.widget.FrameLayout.LayoutParams) mFullscreenVideoView.getLayoutParams();
            if (params != null) {
                params.width = android.widget.FrameLayout.LayoutParams.MATCH_PARENT;
                params.height = android.widget.FrameLayout.LayoutParams.MATCH_PARENT;
                params.gravity = Gravity.CENTER;
                
                mFullscreenVideoView.setLayoutParams(params);
                
                Log.d(TAG, "Video restored to portrait, size: MATCH_PARENT");
            }
        }
    }
    
    /**
     * Animate fullscreen entry
     */
    private void animateFullscreenEntry() {
        if (mFullscreenLayout == null) return;
        
        // Ensure layout is visible before animation
        mFullscreenLayout.setVisibility(View.VISIBLE);
        mFullscreenLayout.setAlpha(1.0f);
        mFullscreenLayout.setScaleX(1.0f);
        mFullscreenLayout.setScaleY(1.0f);
        
        // Start with slightly smaller scale for smooth entry
        mFullscreenLayout.setScaleX(0.95f);
        mFullscreenLayout.setScaleY(0.95f);
        mFullscreenLayout.setAlpha(0.8f);
        
        mFullscreenLayout.animate()
            .scaleX(1.0f)
            .scaleY(1.0f)
            .alpha(1.0f)
            .setDuration(300)
            .setInterpolator(new android.view.animation.DecelerateInterpolator())
            .setListener(new android.animation.AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(android.animation.Animator animation) {
                    Log.d(TAG, "Fullscreen entry animation completed");
                    // Ensure visibility after animation
                    if (mFullscreenLayout != null) {
                        mFullscreenLayout.setVisibility(View.VISIBLE);
                        mFullscreenLayout.setAlpha(1.0f);
                    }
                }
            });
    }
    
    /**
     * Check if in fullscreen mode
     */
    public boolean isFullscreen() {
        return mIsFullscreen;
    }
    
    /**
     * Cleanup resources
     */
    public void destroy() {
        if (mIsFullscreen) {
            exitFullscreen();
        }
        stopPlayback();
        stopAutoHideTimer();
        stopFullscreenAutoHideTimer();
        if (mSearchHelper != null) {
            mSearchHelper.destroy();
            mSearchHelper = null;
        }
        mPlayback = null;
    }
}
