package com.mobreactor.soundwiselib.send;

public interface ISoundGen {

	public void startPlaying();

	public void stop();

	public void genTone(float[] freqIntensities);

}