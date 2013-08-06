package com.mobreactor.soundwiselib.receive;

import com.mobreactor.soundwiselib.Options;

import android.content.Context;
import android.graphics.*;
import android.graphics.Bitmap.Config;
import android.os.Handler;
import android.util.*;
import android.view.*;

/**
 * View that draws the FFT spectrogram.
 * 
 * draw() renders the stuff and does an invalidate() 
 * to prompt another draw() as soon as possible by the system.
 */
public class AnalyzerView extends SurfaceView implements SurfaceHolder.Callback {

    class DrawThread extends Thread {

        public int mCanvasHeight = 2;
        public int mCanvasWidth = 1;

        /** Indicate whether the surface has been created & is ready to draw */
        private boolean doRun = false;

        /** Handle to the surface manager object we interact with */
        private SurfaceHolder surfaceHolder;

        MicAnalyzer mic;
        Bitmap bufferBitmap;
        Canvas cForBuffer;
        int lineY;
        Paint debugTextPaint;
        long totalTime = 0, minDrawTimespan = Long.MAX_VALUE, maxDrawTimespan = Long.MIN_VALUE;
        
        public DrawThread(SurfaceHolder surfaceHolder, Context context,
                Handler handler) {
            // get handles to some important objects
            this.surfaceHolder = surfaceHolder;
            this.debugTextPaint = new Paint();
            debugTextPaint.setColor(Color.GREEN);
            debugTextPaint.setTypeface(Typeface.DEFAULT);
            debugTextPaint.setTextSize(10);
        }
        
        
        @Override
        public void run() {
            drawThreadRun();
        }

         /** Starts / stops the drawing thread.
         */
        public void setRunning(boolean b) {
            doRun = b;
        }
        
        private void drawThreadRun()
        {
        	mic = new MicAnalyzer();
            mic.start();

            bufferBitmap = Bitmap.createBitmap(mCanvasWidth, mCanvasHeight, Config.ARGB_8888);
            cForBuffer = new Canvas(bufferBitmap);
        	lineY = 0;
        	
        	try {
	        	while (doRun) {
	        		drawStep();
	            }
        	} finally {
        		mic.stop();
        	}
        }
        
        /** One step of the drawing loop. */
        private void drawStep()
        {
        	long startTime = System.currentTimeMillis();

            Canvas c = null;
            try {
                c = surfaceHolder.lockCanvas(null);
                if (c != null) synchronized (surfaceHolder) {
                    mic.drawFFT(this);
                    
                    // draw two parts to perform scrolling
                    c.drawBitmap(bufferBitmap, 0.0f, 0.0f, null);
                    
                	if (Options.ANALYZE_PERF) {
                		c.drawText("incoming audio buffers: current: " + Long.toString(mic.perSec) + "/s, from start: " + Long.toString(mic.perSecFromStart) + "/s", 10, 20, debugTextPaint);
                		c.drawText("dropped audio buffers: " + Long.toString(mic.droppedAudioBuffers), 10, 35, debugTextPaint);
                		c.drawText("draw: " + Long.toString(minDrawTimespan) + "ms < " + Long.toString(totalTime) + "ms < " + Long.toString(maxDrawTimespan) + "ms", 10, 50, debugTextPaint);
                	}
                }
            } finally {
                // do this in a finally so that if an exception is thrown
                // during the above, we don't leave the Surface in an
                // inconsistent state
                if (c != null) {
                    surfaceHolder.unlockCanvasAndPost(c);
                }
            }

            totalTime = System.currentTimeMillis() - startTime;
            minDrawTimespan = Math.min(minDrawTimespan, totalTime);
            maxDrawTimespan = Math.max(maxDrawTimespan, totalTime);
        }
        
        public void addLinesOfPixels(int[] pixels, int numLines)
        {
        	while (lineY < mCanvasHeight && numLines > 0) {
        		bufferBitmap.setPixels(pixels, 0, mCanvasWidth, 0, lineY, mCanvasWidth, 1);
        		lineY++;
        		numLines--;
        	}
        	
        	if (numLines == 0) return;
        	
        	// entire bitmap filled and still some lines to add => scroll the content up and continue
            lineY -= numLines;
        	cForBuffer.drawBitmap(
        			bufferBitmap,
        			new Rect(0, numLines, cForBuffer.getWidth(), cForBuffer.getHeight()),
        			new Rect(0, 0, cForBuffer.getWidth(), cForBuffer.getHeight() - numLines),
        			null);

        	while (numLines > 0) {
        		bufferBitmap.setPixels(pixels, 0, mCanvasWidth, 0, lineY, mCanvasWidth, 1);
        		lineY++;
        		numLines--;
        	}
        }

        /** Callback invoked when the surface dimensions change. */
        public void setSurfaceSize(int width, int height) {
            // synchronized to make sure these all change atomically
            synchronized (surfaceHolder) {
                mCanvasWidth = width;
                mCanvasHeight = height;
            }
        }
	}
    
    /** The thread that actually draws the animation */
    private DrawThread thread;

    public AnalyzerView(Context context, AttributeSet attrs) {
        super(context, attrs);

        // register our interest in hearing about changes to our surface
        SurfaceHolder holder = getHolder();
        holder.addCallback(this);

        // create thread only; it's started in surfaceCreated()
        thread = new DrawThread(holder, context, new Handler() {});
        setFocusable(true); // make sure we get key events
    }
    
    ReceiverColorStyle[] receiverStyles = ReceiverColorStyle.values();
    static int currentReceiverStyle = 0;
    
    
    @Override
    public boolean onTouchEvent(MotionEvent motionEvent)
    {
    	currentReceiverStyle = (currentReceiverStyle + 1) % receiverStyles.length;
    	Options.RECEIVER_STYLE = receiverStyles[currentReceiverStyle];
		return false;
    }
    
    /**
     * Fetches the animation thread corresponding to this AirView.
     * 
     * @return the animation thread
     */
    public DrawThread getThread() {
        return thread;
    }

    /**
     * Standard window-focus override. Notice focus lost so we can pause on
     * focus lost. e.g. user switches to take a call.
     */
    @Override
    public void onWindowFocusChanged(boolean hasWindowFocus) {
//        if (!hasWindowFocus) 
//        	thread.pause();
    }

    /* Callback invoked when the surface dimensions change. */
    public void surfaceChanged(SurfaceHolder holder, int format, int width,
            int height) {
        thread.setSurfaceSize(width, height);
    }

    /*
     * Callback invoked when the Surface has been created and is ready to be
     * used.
     */
    public void surfaceCreated(SurfaceHolder holder) {
        // start the thread here so that we don't busy-wait in run()
        // waiting for the surface to be created
        thread.setRunning(true);
        thread.start();
    }

    /*
     * Callback invoked when the Surface has been destroyed and must no longer
     * be touched. WARNING: after this method returns, the Surface/Canvas must
     * never be touched again!
     */
    public void surfaceDestroyed(SurfaceHolder holder) {
        // we have to tell thread to shut down & wait for it to finish, or else
        // it might touch the Surface after we return and explode
        boolean retry = true;
        thread.setRunning(false);
        while (retry) {
            try {
                thread.join();
                retry = false;
            } catch (InterruptedException e) {
            }
        }
    }
}
