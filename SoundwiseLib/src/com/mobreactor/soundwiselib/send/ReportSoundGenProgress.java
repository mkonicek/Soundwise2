package com.mobreactor.soundwiselib.send;

import android.app.ProgressDialog;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;

import com.mobreactor.soundwiselib.Options;

/** The progress dialog when generating sound.
 *  Also simulates some progress when the final IFFT is being computed,
 *  for better user experience. */
public class ReportSoundGenProgress implements Runnable {

	boolean isSimulating = false;
	long startTime = SystemClock.uptimeMillis();
	int simulatedStepPercent = 2;
	int simProgress = 0;
	CrunchingQuotes quotes = new CrunchingQuotes();
	
	int genProgress = 0;
	View ownerView;
	ProgressDialog progressDialog;
	
	/** Initializes and shows a progress dialog. */
	public ReportSoundGenProgress(View ownerView)
	{
		this.ownerView = ownerView;
		progressDialog = new ProgressDialog(ownerView.getContext());
		progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
		progressDialog.setTitle("Crunching numbers...");
		progressDialog.show();
	}
	
	/** Updates progress for the progress dialog. */
	public void postProgress(int progressValue)
	{
		this.genProgress = progressValue;
		ownerView.getHandler().post(this);
	}
	
	public void run() {
    	progressDialog.setProgress(genProgress);
    	if (genProgress >= 100) {
    		progressDialog.hide();
    		return;
    	}
    	if (quotes.hasNewQuote(genProgress)) {
    		progressDialog.setTitle(quotes.getNewQuote());
    	}
    	if (isSimulating) {
    		simProgress += simulatedStepPercent;
    		progressDialog.setProgress(genProgress + simProgress);
    		if (genProgress + simProgress >= 100) {
    			progressDialog.hide();
    		}
    		return;
    	}
    	if (genProgress >= PlayThreadSin.FINAL_PROGRESS && !isSimulating) {
    		// start simulating
    		isSimulating = true;
    		int nSimSteps = 15;
    		long elapsedTime = SystemClock.uptimeMillis() - startTime;
    		double simulatedRemainingTime = (elapsedTime / 
    			(double)PlayThreadSin.FINAL_PROGRESS * 
    			(100 - PlayThreadSin.FINAL_PROGRESS)); 
    		long simulatedStepDelay = (long)(simulatedRemainingTime / (double)nSimSteps);
    		simulatedStepPercent = (100 - PlayThreadSin.FINAL_PROGRESS) / nSimSteps;
    		Log.d(Options.TAG, "Scheduled " + nSimSteps + " simulated progress steps, spanning " + (long)simulatedRemainingTime + "ms");
    		long now = SystemClock.uptimeMillis();
    		for (long i = 0; i < nSimSteps; i++) {
    			ownerView.getHandler().postAtTime(this, now + (i + 1) * simulatedStepDelay);
			}
    	}
    }

}
