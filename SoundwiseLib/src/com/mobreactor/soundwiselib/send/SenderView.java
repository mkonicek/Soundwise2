package com.mobreactor.soundwiselib.send;

import java.util.TimerTask;

import com.mobreactor.soundwiselib.IProgress;
import com.mobreactor.soundwiselib.Options;

import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.os.Handler;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public class SenderView extends SurfaceView implements SurfaceHolder.Callback {

    public interface OnSendingFinishedListener {
        void onSendingFinished(SenderView v);
    }
    private OnSendingFinishedListener sendingFinishedListener;

    private Runnable mSendingFinishedRunnable = new Runnable() {
        public void run() {
            handleSendingFinishedNotificationFromPlayThread();
        }
    };

    private String imagePath;
    private PlayThreadSin playThread;
    
	public void setImagePath(String imagePath)
	{
		this.imagePath = imagePath;
	}
	
	public void setOnSendingFinishedListener(OnSendingFinishedListener l)
	{
		sendingFinishedListener = l;
	}
	
	private void handleSendingFinishedNotificationFromPlayThread() {
        // notify event subscriber that sending has been finished
		// called on UI thread, through view's handler and runnable, from the play thread 
		if (sendingFinishedListener != null) sendingFinishedListener.onSendingFinished(this);		
	}

	public SenderView(Context context, AttributeSet attrs) {
		super(context, attrs);

        // listen to changes to our surface
        SurfaceHolder holder = getHolder();
        holder.addCallback(this);
        
        setFocusable(true); // make sure we get key events
	}
	
	public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) 
	{
		if (this.imagePath == null) return;

		Bitmap b = BitmapFactory.decodeFile(imagePath);
		final ReportSoundGenProgress reportSoundGenProgress = new ReportSoundGenProgress(this);
		
		IProgress progress = new IProgress() {
	        public void setProgress(int value) {
	        	reportSoundGenProgress.postProgress(value);
	        }
	    };
		
        // start playing on another thread
        this.playThread = new PlayThreadSin(b, width, holder, getHandler(), progress, mSendingFinishedRunnable);
        this.playThread.start();
	}
	
	public void surfaceCreated(SurfaceHolder holder) {
		
	}

	public void surfaceDestroyed(SurfaceHolder holder) {
		if (this.playThread != null) {
			// we have to tell thread to shut down & wait for it to finish
	        boolean retry = true;
	        this.playThread.cancel();
	        while (retry) {
	            try {
	            	// wait for playThread to finish
	            	this.playThread.join();
	                retry = false;
	            } catch (InterruptedException e) {
	            }
	        }
		}
	}

	public void sendAgain() {
		if (this.playThread == null || this.playThread.generatedAudio == null) {
			return;
		}
		PlayThreadSin oldThread = this.playThread;
		// just take the already generated audio and play it again
		// (by reusing this.playThread we get the cancellation support - HACK)
		// we are cloning because we can't restart playThread
		this.playThread = this.playThread.clone();
		oldThread.cancel();
		this.playThread.setJustPlayAudio(true);
        this.playThread.start();
	}
}
