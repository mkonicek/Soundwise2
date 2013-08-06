package com.mobreactor.soundwiselib.send;

import com.mobreactor.soundwiselib.R;
import com.mobreactor.soundwiselib.receive.ReceiveActivity;
import com.mobreactor.utils.FlurryTrackedActivity;
import com.mobreactor.utils.FullScreenActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.LinearLayout;

public class SendActivity extends FullScreenActivity {
	
	String imagePath;
	SenderView senderView;
	LinearLayout bottomPanel;
	Button sendAgainButton;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		getWindow().addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

		setContentView(R.layout.send);

		// Image passed from the gallery
		Bundle extras = getIntent().getExtras();
		imagePath = extras.getString("imagePath");

		// Do all the work in SenderView
		senderView = (SenderView)findViewById(R.id.senderView);
		senderView.setImagePath(imagePath);
		bottomPanel = (LinearLayout)findViewById(R.id.bottomPanel);
		sendAgainButton = (Button)findViewById(R.id.sendAgainButton);
		sendAgainButton.setOnClickListener(new OnClickListener() {
			public void onClick(View view) {
				senderView.sendAgain();
			}
		});
		
		senderView.setOnSendingFinishedListener(new SenderView.OnSendingFinishedListener() {
			public void onSendingFinished(SenderView v) {
				bottomPanel.setVisibility(View.VISIBLE);
			}
		});
	}
	
	@Override
	public void onPause()
	{
		if (!isFinishing()) finish();
		super.onPause();
	}
}
