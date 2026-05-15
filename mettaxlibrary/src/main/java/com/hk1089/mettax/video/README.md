# Video Player Library

A comprehensive video player library for Android that supports multiple video channels with real-time streaming capabilities.

## Features

- **Multi-Channel Support**: Create multiple video channels with configurable count
- **Real-Time Streaming**: Live video streaming from surveillance devices
- **Gesture Support**: Touch gestures for video interaction (tap, double-tap, swipe)
- **Programmatic UI**: No XML layouts required - fully programmatic interface
- **NetClient Integration**: Built on top of the proven NetClient infrastructure
- **Customizable**: Easy to configure server, device ID, and channel count

## Components

### Core Classes

1. **VideoPlayer** - Main library class
2. **VideoView** - Custom video display component
3. **VideoDraw** - Video rendering and frame management
4. **RealPlay** - Real-time video streaming management
5. **UpdateThread** - Background thread for UI updates

## Usage

### Basic Setup

```kotlin
// Create VideoPlayer with server, device ID, and channel count
val videoPlayer = VideoPlayer(
    activity = this,
    server = "dashcam.fvts.in",
    deviceId = "299076935181",
    channelCount = 2  // Create 2 video channels
)

// Set up the UI
setContentView(videoPlayer.mainLayout)

// Start video streaming
videoPlayer.startVideo()
```

### Advanced Usage with Listeners

```kotlin
// Set video player listener for gesture handling
videoPlayer.setVideoPlayerListener(object : VideoPlayer.VideoPlayerListener {
    override fun onVideoClick(view: VideoView, index: Int) {
        Log.d("VideoPlayer", "Video $index clicked")
    }
    
    override fun onVideoDoubleClick(view: VideoView, index: Int) {
        Log.d("VideoPlayer", "Video $index double clicked")
    }
    
    override fun onVideoMoveLeft(view: VideoView, index: Int) {
        Log.d("VideoPlayer", "Video $index move left")
    }
    
    override fun onVideoMoveRight(view: VideoView, index: Int) {
        Log.d("VideoPlayer", "Video $index move right")
    }
    
    override fun onVideoMoveUp(view: VideoView, index: Int) {
        Log.d("VideoPlayer", "Video $index move up")
    }
    
    override fun onVideoMoveDown(view: VideoView, index: Int) {
        Log.d("VideoPlayer", "Video $index move down")
    }
    
    override fun onVideoMoveStop(view: VideoView, index: Int) {
        Log.d("VideoPlayer", "Video $index move stop")
    }
})
```

### Lifecycle Management

```kotlin
override fun onDestroy() {
    super.onDestroy()
    videoPlayer.destroy() // Clean up resources
}
```

## API Reference

### VideoPlayer Class

#### Constructor
```kotlin
VideoPlayer(Activity activity, String server, String deviceId, int channelCount)

// Optional audio UI at call site (defaults: initialMuted=true, showMuteButton=true)
VideoPlayer(Activity activity, String server, String deviceId, int channelCount,
            boolean initialMuted, boolean showMuteButton)
```

```kotlin
PlaybackPlayer(Activity activity, String devIdno, boolean isDirect, String server, int port)

// Optional audio UI at call site (defaults: initialMuted=true, showMuteButton=false)
PlaybackPlayer(Activity activity, String devIdno, boolean isDirect, String server, int port,
               boolean initialMuted, boolean showMuteButton)
```

#### Methods
- `startVideo()` - Start video streaming for all channels
- `stopVideo()` - Stop video streaming for all channels
- `destroy()` - Clean up resources and stop streaming
- `isPlaying()` - Check if video is currently playing
- `isInitialized()` - Check if NetClient is initialized
- `getVideoView(int index)` - Get VideoView for specific channel
- `getRealPlay(int index)` - Get RealPlay instance for specific channel
- `setVideoPlayerListener(VideoPlayerListener listener)` - Set gesture listener

#### Properties
- `mainLayout` - LinearLayout containing all video views
- `server` - Server address
- `deviceId` - Device identifier
- `channelCount` - Number of video channels

### VideoView Class

Custom AppCompatImageView with gesture support:
- Single tap
- Double tap
- Swipe gestures (left, right, up, down)
- Long press

### RealPlay Class

Manages real-time video streaming:
- `StartAV(boolean isAudio, boolean isVideo)` - Start streaming
- `StopAV()` - Stop streaming
- `setViewInfo(String devName, String devIdno, int channel, String chnName, int isUser)` - Set device info
- `setVideoView(VideoView videoView)` - Set video display component

## Configuration

### Server Configuration
The library automatically configures NetClient with:
- Server address and port (default: 6605)
- External storage path for temporary files
- Session management

### Channel Configuration
Each channel is automatically configured with:
- Device ID and channel number
- Channel name (CH1, CH2, etc.)
- Video streaming parameters

## Requirements

### Permissions
Add these permissions to your AndroidManifest.xml:
```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />
<uses-permission android:name="android.permission.MANAGE_EXTERNAL_STORAGE" />
```

### Dependencies
- NetClient library (included in mettaxlibrary)
- Android API level 21+ (Android 5.0+)

## Example Implementation

See `VideoPlayerExampleActivity.kt` for a complete working example.

## Troubleshooting

### Common Issues

1. **Video not playing**: Check server and device ID are correct
2. **Permission errors**: Ensure all required permissions are granted
3. **Build errors**: Make sure NetClient library is properly included

### Debug Logs

The library provides comprehensive logging:
- NetClient initialization
- Video streaming status
- Error messages and exceptions

## License

This library is part of the MettaxCall project and follows the same licensing terms.
