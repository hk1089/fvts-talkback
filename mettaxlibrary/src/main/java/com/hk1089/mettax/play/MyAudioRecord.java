package com.hk1089.mettax.play;


import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.media.audiofx.AcousticEchoCanceler;
import android.media.audiofx.NoiseSuppressor;
import android.os.Build;
import android.util.Log;

import androidx.annotation.RequiresApi;


public class MyAudioRecord {
	// 音频获取源
	private static final String TAG = "ttxcommon";
	private int audioSource = MediaRecorder.AudioSource.MIC;
	// 设置音频采样率，44100是目前的标准，但是某些设备仍然支持22050，16000，11025
	private int sampleRateInHz = 8000;	
	// 设置音频的录制的声道CHANNEL_IN_STEREO为双声道，CHANNEL_CONFIGURATION_MONO为单声道
	//private static int channelConfig = AudioFormat.CHANNEL_IN_STEREO;
	private static int channelConfig = AudioFormat.CHANNEL_OUT_DEFAULT;
	// 音频数据格式:PCM 16位每个样本。保证设备支持。PCM 8位每个样本。不一定能得到设备支持。
	private static int audioFormat = AudioFormat.ENCODING_PCM_16BIT;
	// 缓冲区字节大小
	private int bufferSizeInBytes = 0;
	private AudioRecord audioRecord = null;
	private double audioAmplitude = 0;
	private boolean isCalAmplitude = true;
	private boolean isRecord = false;// 设置正在录制的状态
	//add qulixiang 20170712
	private AcousticEchoCanceler canceler;//audio callback
	private NoiseSuppressor noise;//audio noise
	private static final int SAMPLES_PER_FRAME = 1024;	// AAC, bytes/frame/channel
	private static final int FRAMES_PER_BUFFER = 25; 	// AAC, frame/buffer/sec
	
	public MyAudioRecord() {}
	
	public void setFormat(int nChannes, int nSamplePerSec) {
		if (nChannes == 1) {
			channelConfig = AudioFormat.CHANNEL_OUT_DEFAULT;
        } else {
        	channelConfig = AudioFormat.CHANNEL_OUT_STEREO;
        }
		sampleRateInHz = nSamplePerSec;
	}
	
	public void setCalAmplitude(boolean bCal) {
		isCalAmplitude = bCal;
	}
	
	private void creatAudioRecord() {
		if (audioRecord == null) {
			// 获得缓冲区字节大小
			/*bufferSizeInBytes = AudioRecord.getMinBufferSize(sampleRateInHz,
					channelConfig, audioFormat) ;*/
			//sampleRateInHz = 44100;
			int min_buffer_size = AudioRecord.getMinBufferSize(sampleRateInHz, AudioFormat.CHANNEL_IN_MONO,
					AudioFormat.ENCODING_PCM_16BIT);
			bufferSizeInBytes = SAMPLES_PER_FRAME * FRAMES_PER_BUFFER;
			//bufferSizeInBytes = min_buffer_size * 10;
			if (bufferSizeInBytes < min_buffer_size){
				bufferSizeInBytes = ((min_buffer_size / SAMPLES_PER_FRAME) + 1) * SAMPLES_PER_FRAME * 2;
			}
						
			int audioSession;
			if (isDeviceSupport()) {
			      audioRecord = new AudioRecord(MediaRecorder.AudioSource.VOICE_COMMUNICATION, sampleRateInHz,
			              AudioFormat.CHANNEL_IN_DEFAULT,
			              AudioFormat.ENCODING_PCM_16BIT, bufferSizeInBytes);
			} else {
				  // MediaRecorder.AudioSource.MIC CHANNEL_IN_DEFAULT ENCODING_PCM_16BIT CHANNEL_IN_MONO
			      audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, sampleRateInHz,
			              AudioFormat.CHANNEL_IN_MONO,
			              AudioFormat.ENCODING_PCM_16BIT, bufferSizeInBytes);
			}
			audioSession = audioRecord.getAudioSessionId();
			if(AcousticEchoCanceler.isAvailable()){
				//回声消除
				boolean tmp = initAEC(audioSession);
			}			
			  //噪声抑制
			boolean tmp1 = initNic(audioSession);
		      
		      //SessionId = audioSession;
			// 创建AudioRecord对象
			//audioRecord = new AudioRecord(audioSource, sampleRateInHz
			//		, channelConfig, audioFormat, bufferSizeInBytes);
		}
	}

	//add qulixiang 20170712 start
	@RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
	private boolean initNic(int audioSession) {
        if (noise != null) {
            return false;
        }
        Log.i(TAG, "audioSession = " + audioSession);
        noise = NoiseSuppressor.create(audioSession);
        Log.i(TAG, "noise = " + noise);
        if(noise == null){
        	//Log.i(TAG, "noise = " + noise);
        	return false;
        }
        noise.setEnabled(true);
        return noise.getEnabled();
    }
	
	@RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
	private boolean initAEC(int audioSession) {
        if (canceler != null) {
            return false;
        }
        Log.i(TAG, "audioSession = " + audioSession);
        canceler = AcousticEchoCanceler.create(audioSession);
        Log.i(TAG, "canceler = " + canceler);
        if(canceler == null){
        	//Log.i(TAG, "canceler = " + canceler);
        	return false;
        }
        canceler.setEnabled(true);
        return canceler.getEnabled();
    }
	//add qulixiang 20170712 end
	
	public static boolean isDeviceSupport() {
		//return false;
        return AcousticEchoCanceler.isAvailable();
    }
	
	public int getBufferSize() {
		return bufferSizeInBytes;
	}
	
	//获取音量大小
	public synchronized int GetAmplitude() {
		if (audioRecord != null) {
			return (int)audioAmplitude;
		} else {
			return 0;
		}
	}

	public boolean startRecord() {
		creatAudioRecord();
		try {
			audioRecord.startRecording();
			// 让录制状态为true
			isRecord = true;
		} catch(IllegalStateException e) {
		}
		return isRecord;
	}

	public void stopRecord() {
		stopNoiceCancler();
		if (isRecord) {
			try {
				close();
			} catch(IllegalStateException e) {
			}
			isRecord = false;
		}
	}
	
	//add qulixiang 20170712 start
	private void stopNoiceCancler(){
		if(noise != null){
			//Log.d(TAG, "stopNoiceCancler noise");
			noise.setEnabled(false);
			noise = null;
		}
		if(canceler != null){
			//Log.d(TAG, "stopNoiceCancler canceler");
			canceler.setEnabled(false);
			canceler = null;
		}
	}
	
	private void CalVolume(byte[] audiodata, int audiolen) {
		if (audiolen > 0) {
			long v = 0;  
		    // 将 buffer 内容取出，进行平方和运算  
		    for (int i = 0; i < audiolen;) {  
		    	short val = (short) (audiodata[i + 1] << 8 | audiodata[i] & 0xff);
		        v += val * val;  
		        i += 2;
		    }  
		    // 平方和除以数据总长度，得到音量大小。  
		    double mean = v / (double) (audiolen/2);  
		    audioAmplitude = 10 * Math.log10(mean); 
		    
		}
	}
	
	public int readAudioData(byte[] audiodata, int bufferSizeInBytes) {
		if (audioRecord != null) {
			int readLen = audioRecord.read(audiodata, 0, bufferSizeInBytes);

			
			return readLen;
		} else {
			return 0;
		}
	}

	private void close() {
		if (audioRecord != null) {
			isRecord = false;//停止文件写入
			audioRecord.stop();
			audioRecord.release();//释放资源
			audioRecord = null;
		}
	}
}

