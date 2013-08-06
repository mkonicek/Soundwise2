package com.mobreactor.soundwiselib;

import com.mobreactor.soundwiselib.receive.ReceiverColorStyle;

/** Application-wide settings. */
public class Options 
{
	/** Global Soundwise tag used for logging */
	public static final String TAG = "Soundwise";
	
	/** If true, logs and draws performance info */
	public static final boolean ANALYZE_PERF = false;
	
	/** Rendering styles for the receiver, so that we can experiment easily. */
	public static ReceiverColorStyle RECEIVER_STYLE = ReceiverColorStyle.FUNKY;
	
	/** The width of line being drawn. The actual width depends also on display width
	 * (to maintain aspect ration across devices).
	 * value 1 = line width 1 at 240 pixels wide display, line width 2 at 480 pixels wide
	 * value 2 = line width 2 at 240 pixels wide display, line width 4 at 480 pixels wide  */
	public static final int RECEIVER_LINE_INCREMENT = 1;
	
	/** Audio input block size, in samples. */
    public static final int RECEIVER_AUDIO_BUFFER_SIZE = 512;  // 512 is former "double resolution"
	
    /** Number of values in FFT spectrum for one picture line */
	public static final int RECEIVER_FFT_SPECTRUM_SIZE = RECEIVER_AUDIO_BUFFER_SIZE / 2;
	
	/** The length of the averaging of FFT output (2 is the normal value we always used) */
	public static final int RECEIVER_AVERAGING_LEN = 2;
	
	/** The number of bands that the sender is composing to create the final sound.
	 *  The higher the value, the more accurate the sound and slower the sound computation. */
	public static final int SENDER_FFT_SPECTRUM_SIZE = 400;
	
	/** 15.625 is speed used by the receiver in "double resolution mode" (8000 samples per second, 512 samples for one FFT calculation = 1 line)
		31.25 is speed used by the receiver in (deprecated) "single resolution mode" (8000 samples per second, 512 samples for one FFT calculation = 2 lines)
		(double resolution should be 31.25*0.5 then, but 31.25*0.64 seems to maintain the ratio better) 
		Multiplying by SENDER_FFT_SPECTRUM_SIZE so that scaled pictures maintain sound length.
		*/
	public static final double SENDER_PIXELS_PER_SEC = 31.25 * 0.64 * SENDER_FFT_SPECTRUM_SIZE / 256.0;
	
	/** The volume of sender output, 0..1 */
	public static final float SENDER_OUTPUT_VOLUME = 1f;
}
