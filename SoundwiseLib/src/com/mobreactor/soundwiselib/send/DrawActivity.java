package com.mobreactor.soundwiselib.send;

import com.mobreactor.utils.FullScreenActivity;

import java.util.ArrayList;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.Window;

public class DrawActivity extends FullScreenActivity {
	 
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(new DrawView(this));
    }
	
	@Override
	public void onPause()
	{
		if (!isFinishing()) finish();
		super.onPause();
	}
	 
}
