/*package com.mobreactor.soundwiselib.send;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.SystemClock;
import android.util.Log;
import android.view.SurfaceHolder;

import com.mobreactor.soundwiselib.Options;*/

// /** Converts picture to sound by adding sinusoids. */
/* class PlayThreadNaive extends CancellablePlayThread
{
	public PlayThreadNaive(Bitmap bitmap, SurfaceHolder holder)
	{
		super(bitmap, holder);
	}
	
	@Override
	public void run() {
		
		int[] pixelLine = new int[bitmapW];
		float[] lineIntensities = new float[bitmapW];
		
		SoundGen soundGen = new SoundGen(bitmapW, Options.SENDER_LINE_DURATION_MS); // grain duration in ms
		//SoundGenInverseFFT soundGen = new SoundGenInverseFFT(bitmapW);
		soundGen.startPlaying();
		// get all bitmap pixels at once - does not speed this up
		//bitmap.getPixels(pixels, 0, bitmapW, 0, 0, bitmapW, bitmapH);
		
		long startTime = SystemClock.uptimeMillis();
		Paint linePaint = new Paint();
		linePaint.setColor(Color.WHITE);
		
		while (!isCancelled()) {
			// end of bitmap - done
			if (offset >= bitmapH)
				break;
			
			bitmap.getPixels(pixelLine, 0, bitmapW, 0, offset, bitmapW, 1);
			//Log.d("SendImage pixels", Arrays.toString(pixelLine).replace(", ", " "));
			
			// Pixel intensities
			for (int x = 0; x < bitmapW; x++) {
				int color = pixelLine[x];
				// 0xaarrggbb
				int red= (color & 0x00ff0000) >> 16;
				int green= (color & 0x0000ff00) >> 8;
				int blue= (color & 0x000000ff);
				lineIntensities[x] = (red + green + blue) / (float)(255 * 3);
			}
			
			// draw progress line
			Canvas c = null;
			try {
				c = holder.lockCanvas();
	            if (c != null) synchronized (holder) {
	            	c.drawBitmap(bitmap, 0f, 0f, null);
	            	c.drawLine(0, offset, bitmapW, offset, linePaint);
	            }
	        } finally {
	            if (c != null)
	            	holder.unlockCanvasAndPost(c);
	        }
	        
			try
			{
				soundGen.genTone(lineIntensities);
			} catch(Exception e)
			{
				Log.e("Soundwise", e.toString());
			}
			offset++;
		}
		long elapsedMs = SystemClock.uptimeMillis() - startTime;
		Log.d("Soundwise", "SendImage finished in " + (elapsedMs/1000.0) + " s");
		if (isCancelled()) {
			// Canceled by SenderView
			Log.i("Soundwise", "SendImage canceled.");
			soundGen.stop();
		}
	}
}*/
