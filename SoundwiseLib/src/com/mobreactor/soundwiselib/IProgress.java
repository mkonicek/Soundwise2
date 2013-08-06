package com.mobreactor.soundwiselib;

/** Generic interface for reporting progress. */
public interface IProgress {
	
	/** Sets progress, in range 0..100. */
	void setProgress(int value);
}
