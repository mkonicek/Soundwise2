package com.mobreactor.soundwiselib.receive;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;

/**
 * Reads audio from the microphone in a background thread and
 * notifies about reads.
 * 
 * To use this class, the application needs RECORD_AUDIO permission.
 */
public class AudioReader
{
	private static final String TAG = "Soundwise";

    // Android recording class.
    private AudioRecord audioRecord;

    private short[][] inputBuffer = null;
    private int inputBufferFlip = 0;
    private int inputBufferIndex = 0;

    // Size of the block to read each time.
    private int inputBlockSize = 0;
    
    // Time in ms to sleep between blocks, to meter the supply rate.
    private long sleepTime = 0;
    
    // Notifies the user.
    private Listener eventsListener = null;
    
    // Flag whether the thread should be running.
    private boolean doRun = false;
    
    // The thread which is doing the work.  Null if not running.
    private Thread readerThread = null;
	
    public AudioReader() {
    }

    /**
     * @param   readBlockSize   Number of input samples to read at a time.
     *                      	This is different from the system buffer size.
     */
    public void startReader(int samplingRate, int readBlockSize, Listener listener) {
        synchronized (this) {
            // Required buffer size.
            int bufferSize = AudioRecord.getMinBufferSize(samplingRate,
                                         AudioFormat.CHANNEL_CONFIGURATION_MONO,
                                         AudioFormat.ENCODING_PCM_16BIT) * 2;

            audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, samplingRate,
                                         AudioFormat.CHANNEL_CONFIGURATION_MONO,
                                         AudioFormat.ENCODING_PCM_16BIT,
                                         bufferSize);
            //audioInput.startRecording();
            inputBlockSize = readBlockSize;
            sleepTime = (long) (1000f / ((float) samplingRate / (float) readBlockSize));
            inputBuffer = new short[2][inputBlockSize];
            inputBufferIndex = 0;
            inputBufferFlip = 0;
            eventsListener = listener;
            doRun = true;
            readerThread = new Thread(new Runnable() {
                public void run() { threadRun(); }
            }, "Soundwise AudioReader");
            Log.i(TAG, "AudioReader: Starting thread");
            readerThread.start();
        }
    }
    
    public void stopReader() {
        Log.i(TAG, "AudioReader.stopReader()");
        synchronized (this) {
            doRun = false;
        }
        try {
            if (readerThread != null)
                readerThread.join();
        } catch (InterruptedException e) {
        }
        readerThread = null;
        
        // Kill the audio input.
        synchronized (this) {
            if (audioRecord != null) {
                audioRecord.release();
                audioRecord = null;
            }
        }
        Log.i(TAG, "AudioReader - stopped thread");
    }

    private void threadRun() {
        int timeout = 1500;
        try {
            while (timeout > 0 && audioRecord.getState() != AudioRecord.STATE_INITIALIZED) {
                Thread.sleep(50);
                timeout -= 50;
            }
        } catch (InterruptedException e) { }

        if (audioRecord.getState() != AudioRecord.STATE_INITIALIZED) {
            Log.e("Soundwise", "Audio reader failed to initialize");
            eventsListener.onError(Listener.ERR_INIT_FAILED);
            doRun = false;
            return;
        }

        try {
        	audioReaderThreadMain();
        } finally {
            Log.i(TAG, "AudioReader - end of recording");
            if (audioRecord.getState() == AudioRecord.RECORDSTATE_RECORDING)
                audioRecord.stop();
        }
    }
    
    private void audioReaderThreadMain()
    {
    	short[] buffer;
        int index, readSize;
    	
        audioRecord.startRecording();
        while (doRun) {
            long stime = System.currentTimeMillis();

            if (!doRun)
                break;

            readSize = inputBlockSize;
            int space = inputBlockSize - inputBufferIndex;
            if (readSize > space)
                readSize = space;
            buffer = inputBuffer[inputBufferFlip];
            index = inputBufferIndex;

            synchronized (buffer) {
                int readShortsCount = audioRecord.read(buffer, index, readSize);

                boolean isBufferFilled = false;
                if (!doRun)
                    break;
                
                if (readShortsCount < 0) {
                    Log.e(TAG, "audioRecord.read error");
                    eventsListener.onError(Listener.ERR_READ_FAILED);
                    doRun = false;
                    break;
                }
                if (inputBufferIndex + readShortsCount >= inputBlockSize) {
                    inputBufferFlip = (inputBufferFlip + 1) % 2;
                    inputBufferIndex = 0;
                    isBufferFilled = true;
                } else
                    inputBufferIndex = inputBufferIndex + readShortsCount;

                if (isBufferFilled) {
                	eventsListener.onRead(buffer);

                    long sleep = sleepTime - (System.currentTimeMillis() - stime);
                    if (sleep < 5)
                        sleep = 5;
                    try {
                        buffer.wait(sleep);
                    } catch (InterruptedException e) { }
                }
            }
        }// while(isRunning)
    }
    
    /**
     * Handles audio read events.
     */
    public static abstract class Listener {
        public static final int ERR_OK = 0;
        
        public static final int ERR_INIT_FAILED = 1;
        
        public static final int ERR_READ_FAILED = 2;
        
        /**
         * One audio read has completed.
         */
        public abstract void onRead(short[] buffer);
        
        /**
         * An error has occurred.  The reader has been terminated.
         * @param   error       Error code. One of Listener.{ERR_OK,...}
         */
        public abstract void onError(int error);
    }
}

