package com.mobreactor.soundwiselib;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.text.util.Linkify;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

import com.mobreactor.soundwiselib.receive.*;
import com.mobreactor.soundwiselib.send.*;
import com.mobreactor.utils.FlurryTrackedActivity;

public class MainActivity extends FlurryTrackedActivity {
	public static final String PAID_APP_PACKAGE_NAME = "com.mobreactor.soundwise";

	Button receiveButton;
	Button sendButton;
	
	boolean isPaidVersion()
	{
		return getPackageName().equals(PAID_APP_PACKAGE_NAME);
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		
		Linkify.addLinks((TextView)findViewById(R.id.comingSoonTextView), Linkify.ALL);

		this.receiveButton = (Button) this.findViewById(R.id.receiveButton);
		receiveButton.setOnClickListener(new OnClickListener() {
			public void onClick(View view) {
				// call activity that will receive the image
				Intent intent = new Intent();
				intent.setClass(view.getContext(), ReceiveActivity.class);
				startActivity(intent);
			}
		});

		this.sendButton = (Button) this.findViewById(R.id.sendButton);
		if (!isPaidVersion()) {
			sendButton.setText(Html.fromHtml("Broadcast<br/><small><small><small>now available!</small></small></small>"));
		}
		sendButton.setOnClickListener(new OnClickListener() {
			public void onClick(View view) {

				if (isPaidVersion()) {
					// PAID version
					Intent intent = new Intent();
					intent.setClass(view.getContext(), SendSourceActivity.class);
					startActivity(intent);
				} else {
					// FREE version
					AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
					// message box text should not be longer to fit onto Wildfire screen without scrollbar
					builder.setTitle("Broadcast now");
					builder.setMessage(Html.fromHtml(
							"Broadcasting is available in the <font color=\"yellow\">$1 version</font>. Go to Market to get it!<br><br>" +
							"Wanna try receiving?<br><br>" +
							"See <a href=\"http://bit.ly/soundwise\">bit.ly/soundwise</a> on your computer or another phone for examples of cool sounds to decode."));
					builder.setCancelable(true);
					builder.setPositiveButton("Go to Market", new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int id)
						{
							Intent i = new Intent(Intent.ACTION_VIEW);
							i.setData(Uri.parse("market://details?id=com.mobreactor.soundwise"));
							startActivity(i);
						}
					});
					builder.setNegativeButton("Check examples", new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int id)
						{
							Intent i = new Intent(Intent.ACTION_VIEW);
							i.setData(Uri.parse("http://bit.ly/soundwise"));
							startActivity(i);
						}
					});
					
					AlertDialog dialog = builder.create();

					dialog.show();
					
					// Make the textview clickable. Must be called after show()
					// http://stackoverflow.com/questions/1997328/android-clickable-hyperlinks-in-alertdialog 
				    TextView messageView = ((TextView) dialog.findViewById(android.R.id.message));
					messageView.setMovementMethod(LinkMovementMethod.getInstance());
					// without this the text (with the exception of links and colored parts) turns black on touch
					messageView.setTextColor(messageView.getTextColors().getDefaultColor());
				}
			}
		});
	}

}
