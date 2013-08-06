package com.mobreactor.utils;

import com.mobreactor.soundwiselib.MainActivity;

import android.app.Activity;

public class FlurryTrackedActivity extends Activity {
	
	@Override
	public void onStart()
	{
	   super.onStart();
	   
	   String flurryId = null;
	   if (getPackageName().equals(MainActivity.PAID_APP_PACKAGE_NAME)) {
		   // PAID version
		   flurryId = "Y8JWM55QH3SIHTSJXU26";
	   } else {
		   // FREE version
		   flurryId = "81UIPJJ7MRUYEYBJGNNW";
	   }
	}
	
	@Override
	public void onStop()
	{
	   super.onStop();
	}
}
