package com.mobreactor.soundwiselib.send;

import java.util.Random;

import com.mobreactor.soundwiselib.Options;

import android.media.*;
import android.util.Log;

public class SoundGen implements ISoundGen {
	
	private static final String TAG = "SoundGen";
	private static final int minFreq = /*440*/ 440;	// Hz
    private static final int maxFreq = /*3600*/ 3600; // Hz
    private static final int numBand = 1;
	
    private int sampleRate;
    private int bufferSizeInBytes;
    private float[] samples;
    private float[] lastSamples;
    private AudioTrack audioTrack;
    //boolean isPlaying = false;

    private byte[] generatedSnd;
    private float[][] waveTable;
    private int[] waveTablePeriodLengths;
    private int[] waveRepeatCounts;
	
	public SoundGen(int freqsCount, int durationMs)
	{
		Log.i(TAG, "Initializing SoundGen");
	    sampleRate = 8000;
	    int numSamples = (int)((durationMs / (float)1000) * (float)sampleRate);
	    bufferSizeInBytes = 2 * numSamples;
	    samples = new float[numSamples];
	    lastSamples = new float[numSamples];
	    generatedSnd = new byte[bufferSizeInBytes];
	    waveTablePeriodLengths = new int[freqsCount];
	    //waveRepeatCounts = new int[freqsCount];
	    waveTable = precomputeWaveTable(freqsCount, numSamples, waveTablePeriodLengths, waveRepeatCounts);
	    
	    int minBufferSizeInBytes = AudioTrack.getMinBufferSize(sampleRate,
                AudioFormat.CHANNEL_CONFIGURATION_MONO,
                AudioFormat.ENCODING_PCM_16BIT);
	    int audioTrackBufferSizeInBytes = Math.max(minBufferSizeInBytes, this.bufferSizeInBytes);
	    // The smaller the buffer, the sooner the output starts.
	    // But if the buffer is too small and the output starts too soon,
	    // the buffer might play faster than we are generating.
	    // In that case, the sound will pause until there is enough data to play again
	    // - very bad!
	    // Bottom line: the faster we generate, the smaller we can set the buffer,
	    // and the sooner the sound will start to play.
	    audioTrackBufferSizeInBytes = 60*1024;
	    audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC,
	            sampleRate, AudioFormat.CHANNEL_CONFIGURATION_MONO,
	            AudioFormat.ENCODING_PCM_16BIT, audioTrackBufferSizeInBytes,
	            AudioTrack.MODE_STREAM);
	    
	    if (audioTrack.getState() != AudioTrack.STATE_INITIALIZED) {
	    	//throw new InvalidParameterException("AudioTrack could not initialize");
	    }
	    Log.i(TAG, "SoundGen initialized with " + (audioTrackBufferSizeInBytes / 1024) + " kB buffer");
	}
	
	private float[][] precomputeWaveTable(int freqsCount, int numSamples, int[] periodLengths, int[] waveRepeatCounts) {
		Log.i(TAG, "Precomputing wave table, freqs count: " + freqsCount);
		int avgPeriodLen = 0;
		float[][] waveTable = new float[freqsCount][numSamples];
		Random rand = new Random();
		for (int i = 0; i < freqsCount; ++i) {
			double curFreq = log_pos(i / (double)freqsCount, minFreq, maxFreq, 1);
			double nextFreq = log_pos((i + 1) / (double)freqsCount, minFreq, maxFreq, 1);
			double periodExactLen = sampleRate / (double)curFreq;
			int periodLen = (int)Math.round(periodExactLen * 10);	// old version
			//int periodLen = multiplyCloseToInteger(periodExactLen, 10);
			//periodLen = Math.min(periodLen, numSamples);
			periodLengths[i] = periodLen;
			//waveRepeatCounts[i] = numSamples / periodLen;
			avgPeriodLen += periodLen;
			double phase = -Math.PI + rand.nextDouble() * 2 * Math.PI;
			for (int j = 0; j < numBand; j++) {
				
				double toneFreq = curFreq + (nextFreq - curFreq) * (j / (double)numBand);
				double sinCoef = 2 * Math.PI * (toneFreq / (double)sampleRate);
				//double sinOffset = phase;
				for (int sampleI = 0; sampleI < periodLen; sampleI++) {
					//sinOffset += sinCoef * (Math.abs((sampleI % 10) - 5));
					//sinOffset += sinCoef * (1 + 0.03 * (Math.cos(sampleI*0.05)));// slight FM
					//double sinSample = (Math.sin(sinOffset));
					double sinSample = (Math.sin(sampleI * sinCoef));
					double noiseCoef = 1; //(1 - 0.2 + rand.nextDouble() * 0.4);
					waveTable[i][sampleI] += (float)(sinSample * noiseCoef);
				}
			}
        }
		Log.i(TAG, "Wave table finished");
		Log.i(TAG, "Avg period len " + avgPeriodLen / (double)freqsCount);
		return waveTable;
	}

	private static int multiplyCloseToInteger(double periodLen, int maxMultiplier) {
		int bestMultiplier = maxMultiplier;
		for (int i = 1; i <= maxMultiplier; i++) {
			double attempt = periodLen * i;
			double remainder = attempt - Math.floor(attempt);
			if (/*(remainder < 0.1) ||*/ (remainder > 0.95)) {
				bestMultiplier = i;
				break;
			}
		}
		return (int)Math.round(periodLen * bestMultiplier);
	}

	/* (non-Javadoc)
	 * @see com.mobreactor.soundwiselib.send.ISoundGen#startPlaying()
	 */
	public void startPlaying()
	{
		Log.i(TAG, "SoundGen starting to play output");
		if (audioTrack.getState() == AudioTrack.STATE_INITIALIZED) {
			// play if initialized correctly
			audioTrack.play();
	    }
	}
	
	/* (non-Javadoc)
	 * @see com.mobreactor.soundwiselib.send.ISoundGen#stop()
	 */
	public void stop()
	{
		audioTrack.stop();
		// seems that neither stop nor release stop the actual sound playing,
		// after it was sent to the native layer by write()
		audioTrack.release();
	}
	
	/* (non-Javadoc)
	 * @see com.mobreactor.soundwiselib.send.ISoundGen#genTone(float[])
	 */
	public void genTone(float[] freqIntensities)
	{
		fillAudioSamples2(samples, freqIntensities);
		normalizeFair(samples);
		writePCM(samples, generatedSnd);
        audioTrack.write(generatedSnd, 0, generatedSnd.length);
        audioTrack.flush();
    }
	
	int numSamplesTotal = 0;
	
    private void fillAudioSamples2(float[] samples, float[] freqIntensities) 
    {
    	// combine the sinusoids
    	int samplesLen = samples.length;
    	int numSinusoids = freqIntensities.length;
    	
    	int sinusoidLen;
    	int sinusoidStartIndex;
    	float sinusoidIntensity;
    	float[] sinusoid;
    	int remainingAfterEnd;
    	int repeatCount;
    	int takeFromStart;
    	int sampleI = 0;
    	
    	for (int s = 0; s < numSinusoids; s++) {
    		sinusoidIntensity = freqIntensities[s];
    		if (sinusoidIntensity < 0.05) {
    			continue;
    		}
    		
    		sinusoidLen = waveTablePeriodLengths[s];
    		sinusoidStartIndex = numSamplesTotal % sinusoidLen;
    		sinusoid = waveTable[s];
    		
    		// simple version, with modulo and without unwinding
    		for (int i = 0; i < samplesLen; i++) {
    			samples[i] += sinusoidIntensity * sinusoid[(sinusoidStartIndex + i) % sinusoidLen];
    		}
    		
    		remainingAfterEnd = (samplesLen - (sinusoidLen - sinusoidStartIndex));
    		repeatCount = remainingAfterEnd / sinusoidLen;
    		takeFromStart = remainingAfterEnd - (repeatCount * sinusoidLen); 
    		/*if (takeFromStart + repeatCounts * sinusoidLen + takeFromEnd != samplesLen) {
    			// just an ASSERT
    			Log.e(TAG, "Sequences count do not match");
    		}*/
    		/*sampleI = 0;
    		// Three phases, to avoid modulo operation:
    		// run until the end of the sinusoid
    		for (int i = sinusoidStartIndex; i < sinusoidLen; i++) {
    			samples[sampleI] += sinusoidIntensity * sinusoid[i];
    			sampleI++;
    		}
    		int sinLenMod8 = sinusoidLen & 7;
    		int sinLenWhole8 = sinusoidLen - sinLenMod8;
    		int sinLenMinus8 = sinusoidLen - 8;
    		// loop the whole sinusoid a few times
    		// this could be done by looping once and then memcpy?
    		for (int k = 0; k < repeatCount; k++) {
    			for	(int i = 0; i <= sinLenMinus8; i+=8) {
    				samples[sampleI] += sinusoidIntensity * sinusoid[i];
    				samples[sampleI+1] += sinusoidIntensity * sinusoid[i+1];
    				samples[sampleI+2] += sinusoidIntensity * sinusoid[i+2];
    				samples[sampleI+3] += sinusoidIntensity * sinusoid[i+3];
    				samples[sampleI+4] += sinusoidIntensity * sinusoid[i+4];
    				samples[sampleI+5] += sinusoidIntensity * sinusoid[i+5];
    				samples[sampleI+6] += sinusoidIntensity * sinusoid[i+6];
    				samples[sampleI+7] += sinusoidIntensity * sinusoid[i+7];
	    			sampleI += 8;
    			}
    			for	(int i = 0; i < sinLenMod8; i++) {
    				samples[sampleI] += sinusoidIntensity * sinusoid[i + sinLenWhole8];
    				sampleI++;
    			}
    		}
    		// and take the rest from the start of the sinusoid
    		for	(int i = 0; i < takeFromStart; i++) {
    			samples[sampleI] += sinusoidIntensity * sinusoid[i];
    			sampleI++;
    		}*/
    	}
    	numSamplesTotal += samplesLen;
    }
    
    void normalizeToOne(float[] sample)
    {
    	int len = sample.length;
    	float max = 1;
    	for	(int i = 0; i < len; i++) {
    		float absSample = Math.abs(sample[i]);
    		max = Math.max(max, absSample);
    	}
    	float maxInvert = 1f / max;	// multiplication is faster than division 
    	for	(int i = 0; i < len; i++) {
    		sample[i] *= maxInvert;
    	}
    }
    
    void normalizeFair(float[] sample)
    {
    	int numSinusoids = numBand * this.waveTable.length;
    	float numSinusoidsInvert = 2 * 1f / (float)numSinusoids;	// multiplication is faster than division
    	int len = sample.length;
    	for	(int i = 0; i < len; i++) {
    		sample[i] = Math.min(1, sample[i] * numSinusoidsInvert);
    	}
    }
    
    float volume = Options.SENDER_OUTPUT_VOLUME;

	/** Convert to 16 bit pcm sound array.
        Assumes the sample buffer is normalised (-1..1).
	*/
	void writePCM(float[] samples, byte[] pcmOutput)
	{
		int idx = 0;
		int len = samples.length;
		float vol = volume;
		for (int i = 0; i < len; i++) {
			// scale to maximum amplitude, convert to short
            final short val = (short)(samples[i] * vol * 32767);
            // in 16 bit wav PCM, first byte is the low order byte
            pcmOutput[idx++] = (byte)(val & 0x00ff);
            pcmOutput[idx++] = (byte)((val & 0xff00) >>> 8);
		}
	}
	
	void writeRampPCM(float[] samples, byte[] pcmOutput)
	{
		int idx = 0;
		int len = samples.length;
		float vol = volume;
		int rampLen = len / 10;
		int endRampStart = len - rampLen;
		
		double curAmplitude = 1;
		for (int i = 0; i < len; i++) {
			if (i < rampLen) {
				curAmplitude = i / (double)rampLen;
			} else if (i > endRampStart) {
				curAmplitude = (i - endRampStart) / (double)rampLen;	
			} else {
				curAmplitude = 1;
			}
			// scale to maximum amplitude, convert to short
            final short val = (short)(samples[i] * vol * curAmplitude * 32767);
            // in 16 bit wav PCM, first byte is the low order byte
            pcmOutput[idx++] = (byte)(val & 0x00ff);
            pcmOutput[idx++] = (byte)((val & 0xff00) >>> 8);
		}
	}
	
	double log_pos(double x, double min, double max, double logBase)	// turns a logarithmic position (i.e. band number/band count) to a frequency
	{
		if (logBase==1.0)
			return x*(max-min) + min;
		else
			return (max-min) * (min * Math.pow(logBase, x * (Math.log(max)-Math.log(min))/Math.log(2.0)) - min) / (min * Math.pow(logBase, (Math.log(max)-Math.log(min))/Math.log(2.0)) - min) + min;
	}
}

