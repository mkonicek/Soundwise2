package com.mobreactor.soundwiselib.send;

import java.util.Arrays;

import ca.uol.aig.fftpack.RealDoubleFFT;

import com.mobreactor.soundwiselib.IProgress;
import com.mobreactor.soundwiselib.Options;
import com.mobreactor.utils.Utils;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.os.Handler;
import android.os.SystemClock;
import android.util.Log;
import android.view.SurfaceHolder;

public class PlayThreadSin extends CancellablePlayThread 
{
	public Handler mHandler = null;
	public Runnable mSendingFinishedRunnable = null;
	private double scale;
	private Matrix scaleMatrix;
	private int surfaceWidth;	
	private IProgress genProgress;
	/** The generated audio. Filled after run() finishes. */
	public double[] generatedAudio = null;
	/** At which value the progress stops before doing the big IFFT. */
	public static final int FINAL_PROGRESS = 70;
	
	protected Bitmap bitmap;
	protected int bitmapW, bitmapH;
	protected SurfaceHolder holder;
	protected int offset = 0;
	
	boolean isJustPlayAudio = false;
	public void setJustPlayAudio(boolean value)
	{
		isJustPlayAudio = value;
	}
	
	/** Creates instance of PlayThreadSin which will just play given audio. Does not generate anything.
	 * HACK this should be a separate class! We are doing this quick hack
	 * so that it supports cancellation in SenderView. */
	public PlayThreadSin(double[] generatedAudio)
	{
		this.generatedAudio = generatedAudio;
		this.isJustPlayAudio = true;
	}
	
	/** Creates a copy of this thread, including the already generated audio. */
	public PlayThreadSin clone()
	{
		PlayThreadSin clone = new PlayThreadSin(bitmap, surfaceWidth, holder, mHandler, genProgress, mSendingFinishedRunnable);
		clone.generatedAudio = this.generatedAudio;
		return clone;
	}
	
	public PlayThreadSin(Bitmap bitmap, int surfaceWidth, SurfaceHolder holder, Handler sendingFinishedHandler, IProgress genProgress, Runnable sendingFinishedRunnable) 
	{
		super();
		this.bitmap = bitmap;
		this.bitmapW = bitmap.getWidth();
		this.bitmapH = bitmap.getHeight();
		this.holder = holder;
		
		mHandler = sendingFinishedHandler;
		mSendingFinishedRunnable = sendingFinishedRunnable;
		this.genProgress = genProgress;
		
    	this.surfaceWidth = surfaceWidth;
		scaleMatrix = new Matrix();
    	scale = (double) surfaceWidth / (double) bitmapW;
    	scaleMatrix.postTranslate(0, 0);
    	scaleMatrix.postScale((float) scale, (float) scale);
	}
	
	@Override
	public void run() 
	{
		if (isJustPlayAudio) {
			playWholeAudio(generatedAudio);
			return;
		}
		// nejdriv: zkusit 2-3 nasobky - zpomaleni?
		// pak: roztahovani ohrady asi dela artefakty! podivat se, o kolik jsem ohradu
		// umele roztahnul, a pak predstirat, ze obrazek ma o tolik pridany cerny pruh
		
		// posilat po radcich jako vzdycky - tj. skladat SLOUPCE. 
		// (sloupcu  bude 256-400 (sirka)). oseknout audio pri playbacku
		
		int sampleRate = 8000;	// should be an application-wide constant
		double minFreq = 300 / (double)sampleRate;
		// due to an FFT implementation detail, maxFreq has to be lower than 4000
		double maxFreq = 3800 / (double)sampleRate;
		double pixelsPerSample = Options.SENDER_PIXELS_PER_SEC  / (double)sampleRate;
		
		long startTime = SystemClock.uptimeMillis();
		// Get the whole audio at once.
		// Unfortunately, this needs to process the whole image before we can start output,
		// but maybe if we split the image into smaller images,
		// their sounds will fit together nicely?
		// Replace FFT by http://sites.google.com/site/piotrwendykier/software/jtransforms ?
		generatedAudio = getAudio(bitmap, sampleRate, minFreq, maxFreq, pixelsPerSample, this.genProgress);
		if (isCancelled()) {
			Log.i("Soundwise", "Sending cancelled.");
			return;
		} else {
			Log.i("Soundwise", "Audio generated in " + (SystemClock.uptimeMillis() - startTime) + " ms");
		}
		
		playWholeAudio(generatedAudio);
	}
	
	public void playWholeAudio(double[] generatedAudio)
	{
		if (generatedAudio == null) {
			return;
		}
		AudioOut audioOut = new AudioOut();
		audioOut.playWholeAudio(generatedAudio, this);	// pass this so that we can cancel
		// clear the progress line
		drawPlayProgress(-1);
	    // notify view that sending has been finished
	    // will be called on UI thread through handler and runnable
	    if (mHandler != null && mSendingFinishedRunnable != null)  {
	    	mHandler.post(mSendingFinishedRunnable);
	    }
	}
	
	public void drawPlayProgress(int percent)
	{
		// draw progress line
		Canvas c = null;
		try {
			c = holder.lockCanvas();
            if (c != null) synchronized (holder) {
            	c.drawColor(Color.BLACK);
            	
            	c.drawBitmap(bitmap, scaleMatrix, null);
            	
            	if (percent >= 0 && percent <= 100) {
	            	Paint p = new Paint();
	            	p.setColor(Color.YELLOW);
	            	p.setStrokeWidth(2);
	            	int lineY = (int) Math.round((double) percent / 100 * bitmapH * scale);
	            	c.drawLine(0, lineY, surfaceWidth - 1, lineY, p); 
            	}
            }
        } finally {
            if (c != null)
            	holder.unlockCanvasAndPost(c);
        }
	}
	
	double[] getAudio(Bitmap bitmap, int sampleRate, double minFreq, double maxFreq, double pixelsPerSample, IProgress progress)
	{
		int i; // reuse for various iterations
		
		int imgWidth = bitmap.getWidth();
		int imgHeight = bitmap.getHeight();

		// image is sent by horizontal lines from top to bottom (in image coordinate system)
		// on the phone in portrait mode, but rotated and seen as landscape, it will be from left to right
		// so we have to do single-band FFT for vertical lines
		
		// the frequencies for individual lines of the image
		double[] freqs = Utils.lerpArray(minFreq, maxFreq, imgWidth);
		// carrier audio wave
		double[] carrierSine = new double[4];

		// real length of final audio, will be used to return final result from this method
		int realSampleCount = Utils.round(imgHeight / pixelsPerSample);

		// when sampleCount is not a power of 2, IFFT is incredibly slow
		int virtualSampleCount = Utils.nextPowerOf2(Utils.round(imgHeight / pixelsPerSample));
		// huge spectrum ("ohrada") and also final sound signal (we do in-place inverse FFT)
		double[] resultSamples = new double[virtualSampleCount];
		
		// calculate number of pixels (= number of horizontal lines to send) from sampleCount
		// it will be >= imgHeight
		// also: what should be the height of the image to make sure that the sampleCount will be power of 2?
		int virtualImgHeight = (int) Math.ceil(virtualSampleCount * pixelsPerSample);
		int resultLineLen = virtualImgHeight * 2;
		// carrier wave modulated by the line of the image and also the band, transformed by the forward FFT (we do in-place FFT)
		double[] resultLine = new double[resultLineLen];
		
		// basically resultLineLen / 2, as resultLine contains complex numbers (pairs of doubles) 
		int bandSize = (resultLineLen + 1) >> 1;
		int bandSizeHalf = Utils.round(0.25 * (double)resultLineLen);

		// frequency-domain filter: just a smooth ramp 18 up, then all 1.0, then 18 down (256 total)
		// works OK even without this filter
		double[] bandFilter = cutoffBlackman(bandSize, 1.0/16.0);

		int[] pixelLine = new int[imgHeight];
		double[] lineIntensities = new double[imgHeight];
		
		RealDoubleFFT transformer = new RealDoubleFFT(resultLineLen);
		drawPlayProgress(-1);
		
		for (int ib = 0; ib < imgWidth; ib++)	
		{
			if (isCancelled()) {
				return new double[0];
			}
			progress.setProgress((int)((ib + 1) / (double)imgWidth * FINAL_PROGRESS));
			
			// get one vertical line from the bitmap, starting with those for lower bands = starting from the left side
			bitmap.getPixels(pixelLine, 0, 1, ib, 0, 1, imgHeight);
			for (int j = 0; j < imgHeight; j++) {
				int color = pixelLine[j];
				// 0xaarrggbb
				int red= (color & 0x00ff0000) >> 16;
				int green= (color & 0x0000ff00) >> 8;
				int blue= (color & 0x000000ff);
				lineIntensities[j] = (red + green + blue) / (double)(255 * 3);
				/*if (lineIntensities[j] < 0.001) {
					lineIntensities[j] = 0;
				}*/
			}
			
			Arrays.fill(resultLine, 0);

			// random phase between -pi and +pi
			double randPhase = Utils.rand(-Math.PI, Math.PI);

			// generate the carrier wave
			for (i = 0; i < 4; i++)	{			
				carrierSine[i] = Math.cos(i * 0.5 * Math.PI + randPhase);
			}
			// amplitude modulation of the line from the image,
			// carrier is sine wave with 2x higher sampling rate (and random phase)
			// result line is actually longer (imgHeight*2 next power of 2),
			// and here, we fill it only up to imgHeight*2 - the rest stays 0 (black)
			// -> same effect as if the image was actually larger with the bottom filled by black
			for (i = 0; i < imgHeight; i++)
			{
				if ((i & 1) == 0)
				{
					resultLine[i<<1] = lineIntensities[i] * carrierSine[0];
					resultLine[(i<<1) + 1] = lineIntensities[i] * carrierSine[1];
				}
				else
				{
					resultLine[i<<1] = lineIntensities[i] * carrierSine[2];
					resultLine[(i<<1) + 1] = lineIntensities[i] * carrierSine[3];
				}			
			}
			// transform the modulated carrier wave into one band in frequency spectrum
			transformer.ft(resultLine);
			
			// and place it at the right place into the final frequency spectrum
			int bandFinalPos = Utils.round(freqs[ib] * virtualSampleCount);
			for (i = 1; i < bandSize; i++)
			{
				int finalIndex = (bandFinalPos + i) * 2 - bandSizeHalf;
				// maxFreq should always be lower than 4000, otherwise i+bandFinalPos overflows
				resultSamples[finalIndex] += resultLine[i*2] * bandFilter[i];				// Real part
				resultSamples[finalIndex - 1] += resultLine[i*2-1] * bandFilter[i]; 	// Imaginary part, filter is symmetric
			}
		}
		
		// now the huge frequency spectrum is transformed into the whole final audio, all at once
		// his is why we have to keep the sampleCount at some nice number,
		// otherwise this would be too slow (at least with fftpack)
		RealDoubleFFT inverseTransform = new RealDoubleFFT(virtualSampleCount);
		inverseTransform.bt(resultSamples);  // IFFT of the final sound
		
		//samplecount = Utils.round(Xsize/pixpersec);	// chopping tails by ignoring them
		Utils.normalizeToOne(resultSamples);

		double[] finalAudioSamples = new double[realSampleCount];
		System.arraycopy(resultSamples, 0, finalAudioSamples, 0, realSampleCount);
		genProgress.setProgress(100);
		return finalAudioSamples;
	}
	
	double[] cutoffBlackman(int length, double bw)
	{
		int i;
		int bwl;	
		double tbw;	
		double[] h;	
		double x;	
		double coef;	

		tbw = bw * (double) (length-1);
		bwl = Utils.roundup(tbw);
		h = new double[length];

		for (i = 1; i < length; i++)
			h[i] = 1.0;

		double pi = Math.PI;
		for (i = 0; i < bwl; i++)
		{
			x = (double) i / tbw;								
			coef = 0.42*x - (0.5/(2.0*pi))*Math.sin(2.0*pi*x) + (0.08/(4.0*pi))*Math.sin(4.0*pi*x);
			coef *= 1.0/0.42;
			h[i+1] = coef;
			h[length-1-i] = coef;
		}
		return h;
	}
	
	// Should have almost the same effect as cutoffBlackman.
	// A little more testing and probably use this instead of Blackman.
	double[] cutoffRamp(int length, double rampRatio)
	{
		// rampRatio linear ramp up, then 1,1,1,...1, then rampRatio linear ramp down
		double[] result = new double[length];
		double rampLen = rampRatio * (double)length;
		double curAmplitude = 1;
		double endRampStart = length - rampLen;
		for (int i = 0; i < result.length; i++) {
			if (i < rampLen) {
				curAmplitude = i / (double)rampLen;
			} else if (i > endRampStart) {
				curAmplitude = (length - 1 - i) / (double)rampLen;	
			} else {
				curAmplitude = 1;
			}
			result[i] = curAmplitude;
		}
		return result;
	}
}
