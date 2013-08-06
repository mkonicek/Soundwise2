package com.mobreactor.soundwiselib.receive;

import java.text.DecimalFormat;
import android.util.Log;
import com.mobreactor.utils.*;
import ca.uol.aig.fftpack.RealDoubleFFT;

public final class FFTransform {

	private RealDoubleFFT transformer;
	
	// The length of the input, output, and the working array
    private final int size;
    
    // Working data being processed.
    private final double[] workingArray;
    
    // To scale FFT output to (0, 1) range. Depends on size.
    private static float outputScale;
    
	
    public FFTransform(int size) {
        if (!Utils.isPowerOf2(size))
            throw new IllegalArgumentException("Input size must be a power of 2 (not " + size + ")" );
        this.size = size;
        workingArray = new double[size];
        transformer = new RealDoubleFFT(size);
        outputScale = 1.572f / (float)size;
    }

    public final void setInput(short[] input, int startOffsetInInput) {
        for (int i = 0; i < size; i++)
            workingArray[i] = (double) input[startOffsetInInput + i] / 32768.0;
    }

    public final void transform() {
        transformer.ft(workingArray);
    }

    public final float[] getResult(float[] buffer) {
        if (buffer.length != size / 2)
            throw new IllegalArgumentException("bad output buffer size in FFT:" +
                                               " must be " + (size / 2) +
                                               "; given " + buffer.length);
        //StringBuffer sb = new StringBuffer(100);
        //DecimalFormat df = new DecimalFormat("#.##");
        for (int i = 0; i < size / 2; i++) {
            double r = workingArray[i * 2];
            double im = i == 0 ? 0.0 : workingArray[i * 2 - 1];
            /*if (i > 20 && i < 30) {
            	sb.append("[");
	            sb.append(df.format(r));
	            sb.append(", ");
	            sb.append(df.format(im));
	            sb.append("] ");
            }*/
            buffer[i] = (float) (Math.sqrt(r * r + im * im)) * outputScale;
            //buffer[i] = (float) (Math.abs(r)) * outputScale;
        }
        //Log.i("Soundwise", "FFT out: " + sb.toString());
        return buffer;
    }

    public final int getResult(float[] spectrumData, float[][] spectrumHistory, int spectrumHistoryIndex) {
        if (spectrumData.length != size / 2)
            throw new IllegalArgumentException("Result data must be of size " + size / 2);
        if (spectrumHistory.length != size / 2)
            throw new IllegalArgumentException("History must be of size " + size / 2);
    
        int historyLen = spectrumHistory[0].length;
        if (++spectrumHistoryIndex >= historyLen)
            spectrumHistoryIndex = 0;
       
        for (int i = 0; i < size / 2; i++) {
            final float[] curHist = spectrumHistory[i];
            
            double r = workingArray[i * 2];
            double im = i == 0 ? 0.0 : workingArray[i * 2 - 1];
            final float val = (float)(Math.sqrt(r * r + im * im)) * outputScale;
            
            final float old = curHist[spectrumHistoryIndex];
            curHist[spectrumHistoryIndex] = val;
            
            spectrumData[i] = spectrumData[i] + (val - old) / historyLen;
        }
        return spectrumHistoryIndex;
    }
}

