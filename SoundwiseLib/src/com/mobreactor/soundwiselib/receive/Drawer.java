package com.mobreactor.soundwiselib.receive;

import com.mobreactor.soundwiselib.Options;

import android.graphics.Color;

public class Drawer {
	// the values from FFT are normally always between RANGE_LOW and RANGE_HIGH
	// (after we apply a log10 and +1 to them),
	// so we expand this range to the whole 0..1 interval
	private static final double RANGE_LOW = 0.5;
	private static final double RANGE_HIGH = 0.9;
	private static final double RANGE_WIDTH = RANGE_HIGH - RANGE_LOW;
	
	private static final int ADAPT_STEPS = 50;

	private float bw;
	private double[] adaptLimits;
	private int[] pixels;
	
	int numCalls = 0;
		 
    public Drawer()
	{
    	adaptLimits = new double[Options.RECEIVER_FFT_SPECTRUM_SIZE];
	}
    
	/**
	 * Draw a linear spectrum graph.
	 * 
     * @param   data        An array of floats defining the signal power
     *                      at each frequency in the spectrum.
	 * @param  bitmap       Bitmap to draw into.
	 */
    public int[] linearSpectrum(float[] data, int numPixels) {
    	numCalls++;
        
    	// calculate these values only once for performance reasons
        if (numCalls <= 1)
        {
	        // TODO: should move numPixels to constructor
        	// roztazeni spektrogramu na vysku displeje (kdyz se na nej koukama v "landscape", tzn. jde o width)
	        bw = (float)(numPixels - 2) / (float)Options.RECEIVER_FFT_SPECTRUM_SIZE;
	        
	        pixels = new int[numPixels];
        }
        
        int pixel = 0;
        int untilPixel = 0;
        int pixelValue = 0;
        // element 0 isn't a frequency bucket; skip it.
        for (int i = 1; i < Options.RECEIVER_FFT_SPECTRUM_SIZE; ++i) {
        	double value = (float)(Math.log10(data[i]) / RANGE_BELS + 1f);
        	// boost higher frequencies, because normally they are darker
        	double freqBoost = 0.8 + (i / (double)Options.RECEIVER_FFT_SPECTRUM_SIZE) * 0.6;
        	// normalize 
        	value = freqBoost * Math.max(value - RANGE_LOW, 0) / RANGE_WIDTH;
        	
        	// Adapt to background noise:
        	// First few steps, just measure the volume on each frequency
        	if (numCalls < ADAPT_STEPS) {
        		adaptLimits[i] += value;
        	}
        	// "Listening to noise" phase ends 
        	// - calculate value somewhat below the avg for each freq
        	if (numCalls == ADAPT_STEPS)
        	{
        		adaptLimits[i] /= (ADAPT_STEPS - 10);
        	}
        	// Then, always cutoff anything below the avg levels
        	if (numCalls > ADAPT_STEPS)
        	{
        		if (value < adaptLimits[i])
        			value = 0;
        	}
            
            // draw the dot or line
        	untilPixel = Math.min(Math.round((i + 1) * bw), numPixels);
        	pixelValue = getColorForValue(value);
        	while (pixel < untilPixel) pixels[pixel++] = pixelValue;
        }

        return pixels;
	}
    
	// Range in bels.
    private static final float RANGE_BELS = 6f;
    
    private int lerpColor(int c1, int c2, double ratio) {
    	ratio = Math.min(ratio, 1);
    	double ratio1 = (1 - ratio);
    	return Color.rgb(
    			(int) (ratio1*Color.red(c1) + ratio*Color.red(c2)),
    			(int) (ratio1*Color.green(c1) + ratio*Color.green(c2)),
    			(int) (ratio1*Color.blue(c1) + ratio*Color.blue(c2)));
    }

    float[] hsv = new float[3];
    
    private int getColorForValue(double value) {
    	switch (Options.RECEIVER_STYLE) {
    		case BURN:
    			hsv[0] = (-40 + (float)value*60 + (Math.abs((numCalls % 100) - 50))) % 360;
    			hsv[1] = 1;
    			hsv[2] = (float)(value*1.5);
    			return Color.HSVToColor(hsv);
    		case ULTRAVIOLET:
    			hsv[0] = (float) ((240 + (float)value*60 + 0.8*(Math.abs((numCalls/2 % 100) - 50))) % 360);
    			hsv[1] = 1;
    			hsv[2] = (float)(value*1.5);
    			return lerpColor(Color.HSVToColor(hsv), Color.WHITE, value*1.2);
    		case FUNKY:
    			hsv[0] = (float)(250 + (float)value*50 + 3*(Math.abs((numCalls/2 % 100) - 50))) % 360;
    			hsv[1] = (float)1;
    			hsv[2] = (float)(value*1.5);
    			return lerpColor(Color.HSVToColor(hsv), Color.WHITE, value*0.8);
			default:
				// alien green
				return Color.rgb(
	            		(int)(value * 150), 
	            		Math.min((int)(value * 255), 255), 
	            		(int)(value * 230));
		}
    }
    
    
    // Logarithmic frequency scale:
    
	// The Nyquist frequency -- the highest frequency
    // represented in the spectrum data we will be plotting.
    /*private int nyquistFreq;
    private final double log2(double x) {
        return Math.log(x) / LOG2;
    }*/
	//private static final double LOG2 = Math.log(2);
	
    
    /*public void linearSpectrum(float[] data, int lineY, Canvas canvas) {
    paint.setStyle(Style.FILL);
    
    final int len = data.length;
    int spectGraphWidth = canvas.getWidth();
    // data.lenght dots over width
    final float bw = (float)(spectGraphWidth - 2) / (float)len;
    
    // Determine the first and last frequencies we have.
    final float lf = nyquistFreq / len;
    final float rf = nyquistFreq;
    
    // Now, how many octaves is that.  Round down.  Calculate pixels/oct.
    final int octaves = (int) Math.floor(log2(rf / lf)) - 2;
    final float octWidth = (float) (spectGraphWidth - 2) / (float) octaves;
    
    // Calculate the base frequency for the graph, which isn't lf.
    final float bf = rf / (float) Math.pow(2, octaves);
//    // random lines - for testing in emulator
//    paint.setColor((int) Math.round(Math.random() * Integer.MAX_VALUE));
//    canvas.drawLine(0, lineY, spectGraphWidth - 1, lineY, paint);
    
    // Element 0 isn't a frequency bucket; skip it.
    for (int i = 1; i < len; ++i) {
    	// What frequency bucket are we in.
        final float f = lf * i;
        // For freq f, calculate x.
        final float x = (float) (log2(f) - log2(bf)) * octWidth;
        //Color.HSVToColor()
        //double value = (float)(Math.log10(data[i]) / RANGE_BELS + 1f);
    	double value = (float)(Math.log10(data[i]) / RANGE_BELS + 1f);
    	//double slerp = 0.8 + (i / (double)len) * 0.6;
    	double slerp = 1;
    	value = range(value, 0.50, 0.9) * slerp;
    	//double value = data[i] * 25;
        // make it more green
        paint.setColor(Color.rgb(
        		(int)(value * 150), 
        		(int)(value * 255), 
        		(int)(value * 230)));
        // draw the dot
        canvas.drawLine(x, lineY, x + bw, lineY, paint);
    }
	}*/
}
