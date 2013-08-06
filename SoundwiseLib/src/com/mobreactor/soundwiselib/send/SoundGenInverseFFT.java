package com.mobreactor.soundwiselib.send;

import java.security.InvalidParameterException;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import ca.uol.aig.fftpack.RealDoubleFFT;

public class SoundGenInverseFFT implements ISoundGen {

	String TAG = "SoundGen";
	
	private double[] transformArray;
	private RealDoubleFFT transformer;
	private AudioTrack audioTrack;
	private int sampleRate = 8000;
	private byte[] generatedSnd;
	
	 private int minFreq = 0;	// Hz
	 private int maxFreq = 4000; // Hz
	
	public SoundGenInverseFFT(int freqCount) {
		transformArray = new double[freqCount * 2];
		transformer = new RealDoubleFFT(freqCount * 2);
		
		int numOutputSamples = freqCount * 2;
		int outputSizeInBytes = numOutputSamples * 2;	// short is 2 bytes
		generatedSnd = new byte[outputSizeInBytes];
		
		int audioTrackBufferSizeInBytes = 20*1024;
	    audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC,
	            sampleRate, AudioFormat.CHANNEL_CONFIGURATION_MONO,
	            AudioFormat.ENCODING_PCM_16BIT, audioTrackBufferSizeInBytes,
	            AudioTrack.MODE_STREAM);
	}
	
	public void genTone(float[] freqIntensities) {
		int len = freqIntensities.length;
		if (len * 2 != transformArray.length) {
			throw new InvalidParameterException("freqIntensities must have len " + transformArray.length / 2);
		}
		for (int i = 0; i < len; i++) {
			double toneFreq = minFreq + (maxFreq - minFreq) * (i / (double)len);
			transformArray[i * 2] = freqIntensities[i]; 			// amplitude
			//transformArray[i * 2 + 1] = -1 + ((i * 0.15f) % 2f); 	// phase
			transformArray[i * 2 + 1] = 0;
		}
		transformer.bt(transformArray);
		writeRampPCM(transformArray, generatedSnd);
		audioTrack.write(generatedSnd, 0, generatedSnd.length);
	}
	
	void writeRampPCM(double[] samples, byte[] pcmOutput)
	{
		int maxAmplitude = 32767 / 2;
		int idx = 0;
		int len = samples.length;
		int rampLen = len / 10;
		int endRampStart = len - rampLen;
		double amplitude = 1;
		for (int i = 0; i < len; i++) {
			if (i < rampLen) {
				amplitude = i / (double)rampLen;
			} else if (i > endRampStart) {
				amplitude = (i - endRampStart) / (double)rampLen;	
			} else {
				amplitude = 1;
			}
			// scale to maximum amplitude, convert to short
            final short val = (short)(samples[i] * amplitude * maxAmplitude);
            // in 16 bit wav PCM, first byte is the low order byte
            pcmOutput[idx++] = (byte)(val & 0x00ff);
            pcmOutput[idx++] = (byte)((val & 0xff00) >>> 8);
		}
	}

	public void startPlaying() {
		if (audioTrack.getState() == AudioTrack.STATE_INITIALIZED) {
			// play if initialized correctly
			audioTrack.play();
	    }
	}

	public void stop() {
		audioTrack.stop();
	}
}
