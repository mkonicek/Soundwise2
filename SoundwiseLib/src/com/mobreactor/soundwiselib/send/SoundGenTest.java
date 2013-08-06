package com.mobreactor.soundwiselib.send;

import android.media.*;

public class SoundGenTest {
	
    private int sampleRate;
    private double[] samples;
    private double[] lastSamples;
    private int toneFreq = 440;	// Hz
    private AudioTrack audioTrack;
    //boolean isPlaying = false;

    private byte[] generatedSnd;
	
	public SoundGenTest()
	{
	    sampleRate = 8000;
	    int numSamples = 320;
	    samples = new double[numSamples];
	    lastSamples = new double[numSamples];
	    generatedSnd = new byte[2 * numSamples];	// 16 bit PCM - 2 bytes per sample
	    
	    // 40kB buffer
	    audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC,
	            sampleRate, AudioFormat.CHANNEL_CONFIGURATION_MONO,
	            AudioFormat.ENCODING_PCM_16BIT, 40*1024,
	            AudioTrack.MODE_STREAM);
	    
	    if (audioTrack.getState() != AudioTrack.STATE_INITIALIZED) {
	    	//throw new InvalidParameterException("AudioTrack could not initialize");
	    }
	}

	public void startPlaying()
	{
		if (this.audioTrack.getState() == AudioTrack.STATE_INITIALIZED) {
			// play if initialized correctly
			this.audioTrack.play();
	    }
	}
	
	void writeOneGrain()
	{
		fillAudioSamples(samples);
        writePCM(samples, generatedSnd);
        audioTrack.write(generatedSnd, 0, generatedSnd.length);
        audioTrack.flush();
    }
	
	int numCalls = 0;
	
    private void fillAudioSamples(double[] samples) 
    {
    	numCalls++;
    	double intensity = 0.8;
    	double sineCoef = 2 * Math.PI / (sampleRate/(double)toneFreq);
    	int samplesLen = samples.length;
    	for	(int i = 0; i < samplesLen; i++) {
    		samples[i] = intensity * Math.sin(i * sineCoef);
    	}
    }
	
	/** Convert to 16 bit pcm sound array.
        Assumes the sample buffer is normalised (-1..1).
	*/
	void writePCM(double[] samples, byte[] pcmOutput)
	{
		int idx = 0;
        for (final double dVal : samples) {
            // scale to maximum amplitude, convert to short
            final short val = (short) ((dVal * 32767));
            // in 16 bit wav PCM, first byte is the low order byte
            pcmOutput[idx++] = (byte) (val & 0x00ff);
            pcmOutput[idx++] = (byte) ((val & 0xff00) >>> 8);
        }
	}
}