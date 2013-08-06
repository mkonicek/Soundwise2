package com.mobreactor.soundwiselib.receive;

import com.mobreactor.soundwiselib.R;
import com.mobreactor.utils.FullScreenActivity;

import android.os.Bundle;
import android.view.*;

public class ReceiveActivity extends FullScreenActivity {

	AnalyzerView analyzerView;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		getWindow().addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

		setContentView(R.layout.receive);
		
		// all the work is done in AnalyzerView.java
	}
	
	
	
	@Override
	public void onPause()
	{
		if (!isFinishing()) finish();
		super.onPause();
	}
}
