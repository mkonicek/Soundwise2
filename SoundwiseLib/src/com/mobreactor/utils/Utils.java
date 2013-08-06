package com.mobreactor.utils;

import java.util.Random;

public class Utils
{
	private Utils() {
	}
    
    public static final boolean isPowerOf2(int n) {
        return n > 0 && (n & (n - 1)) == 0;
    }

	public static final int bitrev(int j, int n) {
        int r = 0;
        for (int i = 0; i < n; ++i, j >>= 1)
            r = (r << 1) | (j & 0x0001);
        return r;
    }
	
	/** Linear interpolation from min to max, stored in an array. */
	public static final double[] lerpArray(double min, double max, int steps)
	{
		double[] array = new double[steps];
		for (int i=0; i < steps; i++)
		{
			array[i] = lerp(min, max, (double) i/(double) (steps-1));
		}
		return array;
	}
	
	/** Linear interpolation between 2 numbers. */
	public static final double lerp(double min, double max, double t)
	{
		return min + t*(max-min);
	}
	
	/** Rounds to nearest int. */
	public static final int round(double x)
	{
		if (x>0)
			return (int)(x + 0.5);
		else
			return (int)(x - 0.5);
	}
	
	/** Returns the next number higher than x that is a power of 2. 
	 * (Suitable for FFT input size.) */
	public static final int nextPowerOf2(int x)	
	{
		int pow2 = 2;
		while(pow2 < x)
			pow2 *= 2;
		return pow2;
	}
	
	/** Returns the next number higher than x that is 
	 * composed of small primes. (Suitable for FFT input size.) */
	public static final int nextSmallPrimes(int x)
	{
		int[] p = {2, 3};
		int testSmallPrimes = x;
		// just test numbers x, x+1, ...
		while(true) {
			int test = testSmallPrimes;
			for (int i=0; i < p.length; i++) {
				while (test%p[i] == 0) {
					test/=p[i];
				}
			}
			if (test == 1) {
				// test was composed out of small primes
				break;
			}
			testSmallPrimes++; 
		}
		return testSmallPrimes;
	}
	
	public static final double rand(double min, double max)
	{
		Random r = new Random();
		return min + r.nextDouble() * (max - min);
	}
	
	public static final int roundup(double x)
	{
		if (Math.floor(x) == x)
			return (int) x;
		else
			return (int)x + 1;
	}
	
	/** Normalizes the values in the array to <-1,+1> range. */
	public static final void normalizeToOne(double[] sample)
    {
    	int len = sample.length;
    	double max = 1;
    	for	(int i = 0; i < len; i++) {
    		double absSample = Math.abs(sample[i]);
    		max = Math.max(max, absSample);
    	}
    	double maxInvert = 1f / max;	// multiplication is faster than division 
    	for	(int i = 0; i < len; i++) {
    		sample[i] *= maxInvert;
    	}
    }
}

