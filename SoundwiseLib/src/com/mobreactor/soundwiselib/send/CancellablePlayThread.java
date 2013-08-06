package com.mobreactor.soundwiselib.send;

class CancellablePlayThread extends Thread
{
	/** Whether the loop over the bitmap should run. 
    Default is true - you only need to set it to false to stop. */
	private boolean doRun = true;
	
	public void cancel()
	{
		this.doRun = false;
	}
	
	public boolean isCancelled() 
	{
		return !doRun;
	}
	
	protected CancellablePlayThread()
	{
		this.setName("Soundwise PlayThread");
	}
}
