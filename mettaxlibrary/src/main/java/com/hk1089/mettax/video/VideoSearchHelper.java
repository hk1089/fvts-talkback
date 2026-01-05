package com.hk1089.mettax.video;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.babelstar.gviewer.NetClient;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * VideoSearchHelper - Handles video file search operations
 * Matches RecordSearchActivity.java search functionality exactly
 */
public class VideoSearchHelper {
    private static final String TAG = "VideoSearchHelper";
    
    // Search parameters
    private String mDevIdno;
    private boolean mIsDirect;
    private String mServer;
    private int mPort;
    private int mStorageType;
    
    // Search date/time parameters
    private int mSearchYear = -1;  // -1 means use current date
    private int mSearchMonth = -1;  // -1 means use current date
    private int mSearchDay = -1;  // -1 means use current date
    private int mSearchChannel = 0;  // 0 means all channels
    private int mSearchBeginTime = 0;  // Start time in seconds (0-86400)
    private int mSearchEndTime = 86400;  // End time in seconds (0-86400)
    
    // Search state
    private long mSearchHandle = 0;
    private Handler mHandler;
    private List<RecordFile> mFileList;
    private SearchRunnable mSearchRunnable;
    private boolean mIsSearching = false;
    
    // NetClient instance for initialization (matching MainActivity pattern)
    private NetClient mNetClient;
    
    // Listener
    private VideoSearchListener mListener;
    
    /**
     * Listener interface for search events
     */
    public interface VideoSearchListener {
        void onSearchStarted();
        void onFileFound(RecordFile file);
        void onSearchFinished(List<RecordFile> fileList);
        void onSearchFailed();
        void onSearchStopped();
    }
    
    /**
     * Constructor
     * @param context Application context
     * @param devIdno Device ID number
     * @param isDirect Whether using direct connection
     * @param server Server IP
     * @param port Server port
     */
    public VideoSearchHelper(Context context, String devIdno, boolean isDirect, String server, int port) {
        mContext = context.getApplicationContext();
        mDevIdno = devIdno;
        mIsDirect = isDirect;
        mServer = server;
        mPort = port;
        mStorageType = NetClient.GPS_FILE_LOCATION_DEVICE;
        
        mHandler = new Handler(Looper.getMainLooper());
        mFileList = new ArrayList<>();
        mSearchRunnable = new SearchRunnable();
        
        // Initialize NetClient globally (matching MainActivity pattern exactly)
        // This ensures search works even when VideoSearchHelper is used standalone
        initializeNetClient();
    }
    
    private Context mContext;
    
    /**
     * Initialize NetClient globally (matching MainActivity.java pattern exactly)
     * MainActivity order: new NetClient() -> Initialize() -> SetJniEnv() -> SetSession() -> SetDirSvr()
     * This ensures search works even when VideoSearchHelper is used standalone
     */
    private void initializeNetClient() {
        try {
            String sdPath = mContext.getExternalFilesDir("") != null 
                ? mContext.getExternalFilesDir("").getAbsolutePath() + "/"
                : mContext.getFilesDir().getAbsolutePath() + "/";
            Log.d(TAG, "========== INITIALIZING NETCLIENT (matching MainActivity) ==========");
            Log.d(TAG, "Path: " + sdPath);
            Log.d(TAG, "Server: " + mServer);
            Log.d(TAG, "Port: " + mPort);
            Log.d(TAG, "DevIDNO: " + mDevIdno);
            
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
            
            // Step 5: SetDirSvr will be called in updateServer() after getting server/port
            // This matches MainActivity.updateServer() pattern
            updateServer();
            
            Log.d(TAG, "========== NETCLIENT INITIALIZED SUCCESSFULLY ==========");
        } catch (Exception e) {
            Log.e(TAG, "========== ERROR INITIALIZING NETCLIENT ==========");
            Log.e(TAG, "Error: " + e.getMessage(), e);
            e.printStackTrace();
        }
    }
    
    /**
     * Update server configuration (matching MainActivity.updateServer() pattern)
     * This must be called after getting server/port
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
     * Set search listener
     */
    public void setSearchListener(VideoSearchListener listener) {
        mListener = listener;
    }
    
    /**
     * Set search date parameters
     * @param year Year (e.g., 2025)
     * @param month Month (1-12)
     * @param day Day (1-31)
     */
    public void setSearchDate(int year, int month, int day) {
        mSearchYear = year;
        mSearchMonth = month;
        mSearchDay = day;
        Log.d(TAG, "Set search date: Year=" + year + ", Month=" + month + ", Day=" + day);
    }
    
    /**
     * Set search time range
     * @param beginTime Start time in seconds (0-86400)
     * @param endTime End time in seconds (0-86400)
     */
    public void setSearchTimeRange(int beginTime, int endTime) {
        mSearchBeginTime = beginTime;
        mSearchEndTime = endTime;
        Log.d(TAG, "Set search time range: BeginTime=" + beginTime + ", EndTime=" + endTime);
    }
    
    /**
     * Set search channel
     * @param channel Channel number (0 = all channels)
     */
    public void setSearchChannel(int channel) {
        mSearchChannel = channel;
        Log.d(TAG, "Set search channel: " + channel);
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
        mSearchYear = year;
        mSearchMonth = month;
        mSearchDay = day;
        mSearchChannel = channel;
        mSearchBeginTime = beginTime;
        mSearchEndTime = endTime;
        Log.d(TAG, "Set search parameters: Year=" + year + ", Month=" + month + ", Day=" + day + 
              ", Channel=" + channel + ", BeginTime=" + beginTime + ", EndTime=" + endTime);
    }
    
    /**
     * Start searching for video files (matching RecordSearchActivity.startSearch() exactly)
     */
    public void startSearch() {
        if (mSearchHandle == 0 && !mIsSearching) {
            mIsSearching = true;
            
            Calendar cal = Calendar.getInstance();
            //设备端录像搜索
            //Device video search
            mStorageType = NetClient.GPS_FILE_LOCATION_DEVICE;
            Log.d(TAG, "========== STARTING SEARCH ==========");
            Log.d(TAG, "DevIDNO=" + mDevIdno);
            Log.d(TAG, "StorageType=" + mStorageType + " (1=Device, 2=Storage Server, 4=Download Server)");
            Log.d(TAG, "IsDirect=" + mIsDirect);
            Log.d(TAG, "Server=" + mServer);
            Log.d(TAG, "Port=" + mPort);
            Log.d(TAG, "NOTE: NetClient must be initialized globally before calling startSearch() (matching RecordSearchActivity behavior)");
            
            mSearchHandle = NetClient.SFOpenSrchFile(mDevIdno, mStorageType, 2);  // Matching RecordSearchActivity: MainActivity.GPS_FILE_ATTRIBUTE_RECORD = 2
            Log.d(TAG, "SFOpenSrchFile returned handle: " + mSearchHandle + " for DevIDNO: " + mDevIdno);
            
            if (mSearchHandle == 0) {
                Log.e(TAG, "========== FAILED TO OPEN SEARCH HANDLE ==========");
                Log.e(TAG, "SFOpenSrchFile returned 0 - this means the search handle could not be created");
                Log.e(TAG, "Possible causes:");
                Log.e(TAG, "  1. NetClient not initialized (Initialize, SetJniEnv, SetSession, SetDirSvr)");
                Log.e(TAG, "  2. Invalid DevIDNO: " + mDevIdno);
                Log.e(TAG, "  3. Invalid StorageType: " + mStorageType);
                Log.e(TAG, "  4. Server not configured: " + mServer + ":" + mPort);
                mIsSearching = false;
                if (mListener != null) {
                    mListener.onSearchFailed();
                }
                return;
            }
            
            Log.d(TAG, "Search handle opened successfully: " + mSearchHandle);
            
            mFileList.clear();
            
            // Use provided search parameters or fall back to current date/time
            int year = (mSearchYear > 0) ? mSearchYear : cal.get(Calendar.YEAR);
            int month = (mSearchMonth > 0) ? mSearchMonth : (cal.get(Calendar.MONTH) + 1);
            // For day: if user provided explicit day, use it as-is; if using Calendar, apply offset
            int day;
            boolean isUserProvidedDay = (mSearchDay > 0);
            if (isUserProvidedDay) {
                day = mSearchDay;  // Use user-provided day as-is
            } else {
                day = cal.get(Calendar.DAY_OF_MONTH) - 1;  // Apply offset only for Calendar date (matching RecordSearchActivity)
            }
            int channel = mSearchChannel;
            int beginTime = mSearchBeginTime;
            int endTime = mSearchEndTime;
            
            if (mIsDirect) {
                NetClient.SFSetRealServer(mSearchHandle, mServer, mPort, "");
                int startRet = NetClient.SFStartSearchFile(mSearchHandle, year, month, day, -1, channel, beginTime, endTime);  // Matching RecordSearchActivity: MainActivity.GPS_FILE_TYPE_ALL = -1
                Log.d(TAG, "SFStartSearchFile (direct) returned: " + startRet);
                Log.d(TAG, "Direct search parameters: Year=" + year + ", Month=" + month + ", Day=" + day + ", Channel=" + channel + ", BeginTime=" + beginTime + ", EndTime=" + endTime);
            } else {
                //1078设备
                boolean is1078 = false;
                
                if (is1078) {
                    int startRet = NetClient.SFStartSearchFileEx(
                        mSearchHandle, year, month, day,
                        year, month, day,
                        -1, channel, beginTime, endTime, NetClient.GPS_FILE_LOCATION_DEVICE, 0, NetClient.GPS_MEDIA_TYPE_AUDIO_VIDEO,  // Matching RecordSearchActivity: MainActivity.GPS_FILE_TYPE_ALL = -1
                        NetClient.GPS_STREAM_TYPE_MAIN_SUB, NetClient.GPS_MEMORY_TYPE_MAIN_SUB, 0, 0, 0
                    );
                    Log.d(TAG, "SFStartSearchFileEx returned: " + startRet);
                } else {
                    // For server-based: if user provided day, use it as-is; if using Calendar, apply offset
                    int searchDay;
                    if (isUserProvidedDay) {
                        searchDay = day;  // User provided day, use as-is
                    } else {
                        searchDay = day;  // Already has offset applied from Calendar
                    }
                    Log.d(TAG, "========== STARTING SERVER-BASED SEARCH ==========");
                    Log.d(TAG, "Date: Year=" + year + ", Month=" + month + ", Day=" + searchDay + (isUserProvidedDay ? " (user-provided)" : " (from Calendar, offset applied)"));
                    Log.d(TAG, "Time Range: BeginTime=" + beginTime + ", EndTime=" + endTime);
                    Log.d(TAG, "FileType: -1 (GPS_FILE_TYPE_ALL)");
                    Log.d(TAG, "Channel: " + channel + (channel == 0 ? " (all channels)" : ""));
                    Log.d(TAG, "Search Handle: " + mSearchHandle);
                    Log.d(TAG, "Server: " + mServer + ":" + mPort);
                    Log.d(TAG, "DevIDNO: " + mDevIdno);
                    
                    int startRet = NetClient.SFStartSearchFile(mSearchHandle, year, month, searchDay, -1, channel, beginTime, endTime);  // Matching RecordSearchActivity: MainActivity.GPS_FILE_TYPE_ALL = -1
                    Log.d(TAG, "SFStartSearchFile returned: " + startRet);
                    
                    if (startRet == 0) {
                        Log.d(TAG, "========== SEARCH STARTED SUCCESSFULLY ==========");
                        Log.d(TAG, "Waiting 2000ms before polling for results (matching RecordSearchActivity)");
                    } else {
                        Log.e(TAG, "========== SEARCH START FAILED ==========");
                        Log.e(TAG, "SFStartSearchFile returned: " + startRet + " (expected 0 for success)");
                        Log.e(TAG, "This means the search did not start properly!");
                        Log.e(TAG, "Troubleshooting:");
                        Log.e(TAG, "  1. Verify NetClient.Initialize() was called");
                        Log.e(TAG, "  2. Verify NetClient.SetJniEnv() was called on an instance");
                        Log.e(TAG, "  3. Verify NetClient.SetSession(\"\") was called");
                        Log.e(TAG, "  4. Verify NetClient.SetDirSvr(" + mServer + ", " + mServer + ", " + mPort + ", 0) was called");
                        Log.e(TAG, "  5. Verify search handle is valid: " + mSearchHandle);
                        Log.e(TAG, "  6. Verify server is reachable: " + mServer + ":" + mPort);
                        Log.e(TAG, "  7. Verify DevIDNO is correct: " + mDevIdno);
                        mIsSearching = false;
                        if (mListener != null) {
                            mListener.onSearchFailed();
                        }
                        return;
                    }
                }
            }
            
            // Post search runnable with 2000ms delay (matching RecordSearchActivity)
            mHandler.postDelayed(mSearchRunnable, 2000);
            
            if (mListener != null) {
                mListener.onSearchStarted();
            }
        }
    }
    
    /**
     * Stop searching (matching RecordSearchActivity.stopSearch() exactly)
     */
    public void stopSearch() {
        if (mSearchHandle != 0) {  // Matching RecordSearchActivity: if (0 != mSearchHandle)
            NetClient.SFStopSearchFile(mSearchHandle);
            NetClient.SFCloseSearchFile(mSearchHandle);
            mHandler.removeCallbacks(mSearchRunnable);
            mSearchHandle = 0;
            mIsSearching = false;
            
            if (mListener != null) {
                mListener.onSearchStopped();
            }
        }
    }
    
    /**
     * Check if search is in progress
     */
    public boolean isSearching() {
        return mIsSearching;
    }
    
    /**
     * Get current file list
     */
    public List<RecordFile> getFileList() {
        return new ArrayList<>(mFileList);
    }
    
    /**
     * Search runnable to handle search results (matching RecordSearchActivity.SearchRunnable exactly)
     */
    private class SearchRunnable implements Runnable {
        private int pollCount = 0;
        
        @Override
        public void run() {
            boolean isFinished = false;
            if (mSearchHandle != 0) {  // Matching RecordSearchActivity: if (0 != mSearchHandle)
                while (true) {
                    byte[] result = new byte[1024];
                    java.util.Arrays.fill(result, (byte)0);
                    int ret = NetClient.SFGetSearchFile(mSearchHandle, result, 1024);
                    pollCount++;
                    
                    // Log every 20 polls to show we're still searching (reduces spam)
                    if (pollCount % 20 == 0 && ret != 0 && ret != 99 && ret != 100) {
                        Log.d(TAG, "Still searching... (poll #" + pollCount + ", ret=" + ret + ", -1=not ready yet)");
                    }
                    
                    if (ret == 0) {  // Matching RecordSearchActivity: MainActivity.NET_SUCCESS = 0
                        int i = 0;
                        for (i = 0; i < result.length; ++i) {
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
                        search.setDevIdno(mDevIdno);
                        search.setName(info[index++]);
                        search.setYear(Integer.parseInt(info[index++]));
                        search.setMonth(Integer.parseInt(info[index++]));
                        search.setDay(Integer.parseInt(info[index++]));
                        search.setBeginTime(Integer.parseInt(info[index++]));
                        search.setEndTime(Integer.parseInt(info[index++]));
                        index++; // Skip DevIDNO field
                        search.setChn(Integer.parseInt(info[index++]));
                        search.setFileLength(Integer.parseInt(info[index++]));
                        search.setFileType(Integer.parseInt(info[index++]));
                        search.setLocation(Integer.parseInt(info[index++]));
                        search.setSvrId(Integer.parseInt(info[index++]));
                        search.setChnMask(Integer.parseInt(info[index++]));
                        search.setAlarmInfo(Integer.parseInt(info[index++]));
                        search.setFileOffset(Integer.parseInt(info[index++]));
                        search.setRecording(Integer.parseInt(info[index++]) > 0);
                        search.setStream(Integer.parseInt(info[index++]) > 0);
                        search.setIsPlaying(false);
                        
                        // Print/search model data
                        printRecordFileModel(search, fileInfo, info);
                        Log.d(TAG, "Device ID: " + search.getFileInfo());
                        mFileList.add(search);
                        
                        // Notify listener
                        if (mListener != null) {
                            mListener.onFileFound(search);
                        }
                        
                        continue;
                    } else if (ret == 99) {  // Matching RecordSearchActivity: MainActivity.SEARCH_FINISHED = 99
                        if (mFileList.size() > 0) {
                            Collections.sort(mFileList, new Comparator<RecordFile>(){
                                @Override
                                public int compare(RecordFile lhs, RecordFile rhs) {
                                    // TODO Auto-generated method stub
                                    int i = lhs.getBeginTime() - rhs.getBeginTime();
                                    if(i == 0){
                                        int j = lhs.getChn() - lhs.getChn();  // Matching RecordSearchActivity bug exactly
                                        return j;
                                    }
                                    return i;
                                }
                            });
                        }
                        isFinished = true;
                        mIsSearching = false;
                        
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
                        
                        // Notify listener
                        if (mListener != null) {
                            mListener.onSearchFinished(new ArrayList<>(mFileList));
                        }
                        break;
                    } else if (ret == 100) {  // Matching RecordSearchActivity: MainActivity.SEARCH_FAILED = 100
                        isFinished = true;
                        mIsSearching = false;
                        Log.d(TAG, "Search Finished");
                        
                        // Notify listener
                        if (mListener != null) {
                            mListener.onSearchFailed();
                        }
                        break;
                    } else {
                        // For -1 or other values, break out of while loop to allow delay before next poll
                        // This prevents tight looping and gives the search time to process
                        break;
                    }
                }
            }
            
            if (!isFinished) {
                // Repost with delay to avoid tight looping (matching RecordSearchActivity pattern)
                mHandler.postDelayed(mSearchRunnable, 50);
            }
        }
    }
    
    /**
     * Print detailed RecordFile model data (matching RecordSearchActivity.printRecordFileModel)
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
     * Print summary of RecordFile model (matching RecordSearchActivity.printRecordFileSummary)
     */
    private void printRecordFileSummary(RecordFile file) {
        String dateStr = file.getYear() + "-" + String.format("%02d", file.getMonth()) + "-" + String.format("%02d", file.getDay());
        String timeStr = formatSeconds(file.getBeginTime()) + " - " + formatSeconds(file.getEndTime());
        Log.d(TAG, "  DevIdno: " + file.getDevIdno() + " | Chn: " + file.getChn() + " | Date: " + dateStr + " | Time: " + timeStr + " | Type: " + (file.getFileType() == 0 ? "Normal" : "Alarm") + " | Size: " + file.getFileLength() + " bytes");
    }
    
    /**
     * Format seconds to HH:MM:SS (matching RecordSearchActivity.formatSeconds)
     */
    private String formatSeconds(int seconds) {
        int hours = seconds / 3600;
        int minutes = (seconds % 3600) / 60;
        int secs = seconds % 60;
        return String.format("%02d:%02d:%02d", hours, minutes, secs);
    }
    
    /**
     * Cleanup resources
     */
    public void destroy() {
        stopSearch();
        // Note: We don't call NetClient.UnInitialize() here because NetClient might be used by other components
        // Only uninitialize if this is the only component using NetClient
        // mNetClient.UnInitialize();
        mNetClient = null;
    }
}

