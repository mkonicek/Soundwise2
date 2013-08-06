package com.mobreactor.soundwiselib.receive;

import com.mobreactor.soundwiselib.Options;

import android.graphics.Bitmap;
import android.util.Log;

public class MicAnalyzer {
	
	private static final int sampleRate = 8000;		// max freq = sample rate / 2

    // Only 1 in sampleSkip blocks will actually be processed.
    private static final int sampleSkip = 1;
   
    // The desired histogram averaging window.  1 means no averaging.
    private static final int historyLen = Options.RECEIVER_AVERAGING_LEN;
	
	// Analysed audio spectrum data; history data for each frequency
    // in the spectrum; index into the history data;
    private float[] spectrumData;
    private float[][] spectrumHistory;
    private int spectrumHistoryIndex;
    
    // Buffered audio data, and sequence number of the latest block.
    private short[] audioData;
    private long audioReceived = 0;
    // If we got a read error, the error code.
    private int readError = AudioReader.Listener.ERR_OK;
    // Sequence number of the last block we processed.
    private long audioProcessed = 0;
    
    private long lastTime = 0;
    private long startTime = 0;
    public long perSec = 0;
    public long perSecFromStart = 0;
    public long droppedAudioBuffers = 0;
	private long buffersPassed = 0;
    
	AudioReader audioReader;
	FFTransform ffTransform;
	
    Drawer drawer = new Drawer();
	int[] lastPixels;
	double shouldBeAt = 0;
	double isAt = 0;
	double lineIncrement = 0;
	
	public MicAnalyzer()
	{
		this.audioReader = new AudioReader();
        this.ffTransform = new FFTransform(Options.RECEIVER_AUDIO_BUFFER_SIZE);
        
        // Allocate the spectrum data.
        spectrumData = new float[Options.RECEIVER_FFT_SPECTRUM_SIZE];
        spectrumHistory = new float[Options.RECEIVER_FFT_SPECTRUM_SIZE][historyLen];
        spectrumHistoryIndex = 0;
	}
	
	public void start() {
        audioProcessed = audioReceived = 0;
		startTime = lastTime = System.currentTimeMillis();
        readError = AudioReader.Listener.ERR_OK;
        
        audioReader.startReader(sampleRate, Options.RECEIVER_AUDIO_BUFFER_SIZE, new AudioReader.Listener() {
            @Override
            public final void onRead(short[] buffer) {
                receiveAudio(buffer);
            }
            @Override
            public void onError(int error) {
                handleError(error);
            }
        });
    }
    
    public void stop() {
        audioReader.stopReader();
    }
    
    /**
     * Handle audio input.  This is called on the thread of the audio
     * reader.
     * 
     * @param   buffer      Audio data that was just read.
     */
    private final void receiveAudio(short[] buffer) {
        // Lock to protect updates to these local variables.  See run().
        synchronized (this) {
        	audioData = buffer;
            audioReceived++;

        	if (Options.ANALYZE_PERF) {
            	//Log.d("MicAnalyzer", "Writing audio " + audioSequence + " to shared audioData");
        		long nowTime = System.currentTimeMillis();
        		if (nowTime - lastTime == 0) perSec = 999; else perSec = 1000 / (nowTime - lastTime);
        		if (nowTime - startTime == 0) perSecFromStart = 999; else perSecFromStart = 1000 * audioReceived / (nowTime - startTime);
        		lastTime = nowTime;
        	}

            // notify waiting doUpdate method that new data is available
            this.notify();
        }
    }
    
    
    /**
     * An error has occurred.  The reader has been terminated.
     * 
     * @param   error       ERR_XXX code describing the error.
     */
    private void handleError(int error) {
        synchronized (this) {
            readError = error;
        }
    }
    
    /**
     * Update the state of the instrument for the current frame.
     * This method must be invoked from the doUpdate() method of the
     * application's {@link SurfaceRunner}.
     * 
     * <p>Since this is called frequently, we first check whether new
     * audio data has actually arrived.
     * 
     * @param   now         Nominal time of the current frame in ms.
     */
    public final void drawFFT(AnalyzerView.DrawThread avdt) {
        short[] buffer = null;

        synchronized (this) {
            try {
				// no data received from last doUpdate? then wait for receiveAudio method to receive some data and place them into audioData
            	if (audioReceived <= audioProcessed) this.wait();
			} catch (InterruptedException e) {
				e.printStackTrace();
				// do not process anything
				return;
			}

            buffersPassed = audioReceived - audioProcessed;
			if (Options.ANALYZE_PERF) {
            	// buffersPassed > 1 means that some buffers were lost 
				droppedAudioBuffers += buffersPassed - 1;
            }
			
            audioProcessed = audioReceived;
            buffer = audioData;
        }

        if (buffer != null) processAudioFFT(buffer);
        
        // calculate sample to lines ratio based on old SoundReactor configuration (double resolution false = buffer 256, line increment 2; same picture width on landscaped phone on QVGA and WVGA)
        // new configuration is: double resolution true = buffer 512, line increment 2 * mCanvasWidth / 240
        if (lineIncrement == 0) lineIncrement = Options.RECEIVER_LINE_INCREMENT * avdt.mCanvasWidth / (double)240;  // 240 value is based on observation of decoded pictures on 240x320 Wildfire and 480x800 HD2
        
        // draw pixels from previous drawFFT pass, more lines of same pixels if neccessary
        shouldBeAt += buffersPassed * lineIncrement;
        int lines = (int) Math.floor(shouldBeAt - isAt);
        if (lastPixels != null && lines > 0) {
        	avdt.addLinesOfPixels(lastPixels, lines);
        	isAt += lines;
        }
        
        // prepare pixels for next drawFFT pass
        lastPixels = drawer.linearSpectrum(spectrumData, avdt.mCanvasWidth);
        
        // any errors?
        if (readError != AudioReader.Listener.ERR_OK) processError(readError);
    }


    /**
     * Handle audio input.  This is called on the thread of the
     * parent surface.
     * 
     * @param   buffer      Audio data that was just read.
     */
    private final void processAudioFFT(short[] buffer) {
        // Process the buffer.  While reading it, it needs to be locked.
        synchronized (buffer) {
            ffTransform.setInput(buffer, 0);
            buffer.notify();
        }
        
        ffTransform.transform();
        
        if (historyLen <= 1) {
            ffTransform.getResult(spectrumData);
        } else {
            spectrumHistoryIndex = ffTransform.getResult(spectrumData, spectrumHistory, spectrumHistoryIndex);
        }
    }

    /**
     * Handle an audio input error.
     * 
     * @param   error       ERR_XXX code describing the error.
     */
    private final void processError(int error) {
        Log.e("MIC", "Mic error: " + new Integer(error).toString());
    }
}
