package com.mobreactor.soundwiselib.send;

import com.mobreactor.soundwiselib.Options;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;

public class AudioOut 
{
	int sampleRate = 8000;
	AudioTrack audioTrack;
	int writeBufferSizeShorts = 1024;
	byte[] writeBuffer = new byte[writeBufferSizeShorts * 2];
	
	public AudioOut()
	{
		int audioTrackBufferSizeInBytes = Math.min(writeBufferSizeShorts * 16, AudioTrack.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_CONFIGURATION_MONO, AudioFormat.ENCODING_PCM_16BIT));

	    audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC,
	            sampleRate, AudioFormat.CHANNEL_CONFIGURATION_MONO,
	            AudioFormat.ENCODING_PCM_16BIT, audioTrackBufferSizeInBytes,
	            AudioTrack.MODE_STREAM);
	    
	    if (audioTrack.getState() != AudioTrack.STATE_INITIALIZED) {
	    	// TODO: doplnit, asi vyjimku a vyse ji zachytavat?
	    }
	}
	    
	public void playWholeAudio(double[] samples, PlayThreadSin playThread)
	{
		if (audioTrack.getState() == AudioTrack.STATE_INITIALIZED) {
			// play if initialized correctly
			audioTrack.play();
	    }
		
		try {
	        for (int i = 0; i < samples.length; i += writeBufferSizeShorts) {
				if (playThread.isCancelled()) return;

				int writeLen = Math.min(writeBufferSizeShorts, samples.length - i);
				
				writePCM(samples, writeBuffer, i, writeLen);
		        audioTrack.write(writeBuffer, 0, writeLen * 2);  // *2 because samples are 16-bit and write method requires length in bytes!
		        
		        playThread.drawPlayProgress((int) 100 * i / samples.length);
			}
		} finally {
	        if (!playThread.isCancelled()) audioTrack.flush();
			stop();
		}
	}
	
	void writePCM(double[] samples, byte[] pcmOutput, int startIndex, int len)
	{
		int idx = 0;
		double volume = Options.SENDER_OUTPUT_VOLUME * 32767;
		for (int i = startIndex; i < startIndex + len; i++) {
			// scale to maximum amplitude, convert to short
            final short val = (short)(samples[i] * volume);
            // in 16 bit wav PCM, first byte is the low order byte
            pcmOutput[idx++] = (byte)(val & 0x00ff);
            pcmOutput[idx++] = (byte)((val & 0xff00) >>> 8);
		}
	}
	
	public void stop()
	{
		audioTrack.stop();
		// seems that neither stop nor release stop the actual sound playing,
		// after it was sent to the native layer by write()
		audioTrack.release();
	}
}
