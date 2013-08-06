package com.mobreactor.soundwiselib.send;

import java.util.ArrayList;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

// http://www.droidnova.com/playing-with-graphics-in-android-part-iv,182.html
// http://www.tutorialforandroid.com/2009/06/drawing-with-canvas-in-android.html
public class DrawView extends SurfaceView implements SurfaceHolder.Callback {

	private DrawThread _thread;
    private ArrayList<Path> _graphics = new ArrayList<Path>();
    private Paint mPaint;
    private Path path;

    public DrawView(Context context) {
        super(context);
        
        mPaint = new Paint();
        mPaint.setDither(true);
        mPaint.setColor(0xFFFFFF00);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeJoin(Paint.Join.ROUND);
        mPaint.setStrokeCap(Paint.Cap.ROUND);
        mPaint.setStrokeWidth(3);

        getHolder().addCallback(this);
        _thread = new DrawThread(getHolder(), this);
        setFocusable(true);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
		synchronized (_thread.getSurfaceHolder()) {
			if(event.getAction() == MotionEvent.ACTION_DOWN) {
				path = new Path();
				path.moveTo(event.getX(), event.getY());
				path.lineTo(event.getX(), event.getY());
	  	    } else if(event.getAction() == MotionEvent.ACTION_MOVE) {
	  	    	path.lineTo(event.getX(), event.getY());
	  	    } else if(event.getAction() == MotionEvent.ACTION_UP) {
	  	    	path.lineTo(event.getX(), event.getY());
	  	    	_graphics.add(path);
	  	    }
			return true;
		}
    }
  
    @Override
    public void onDraw(Canvas canvas) {
    	for (Path path : _graphics) {
    		canvas.drawPath(path, mPaint);
    	}
    }

    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        // TODO Auto-generated method stub
    }

    public void surfaceCreated(SurfaceHolder holder) {
        _thread.setRunning(true);
        _thread.start();
    }

    public void surfaceDestroyed(SurfaceHolder holder) {
        // simply copied from sample application LunarLander:
        // we have to tell thread to shut down & wait for it to finish, or else
        // it might touch the Surface after we return and explode
        boolean retry = true;
        _thread.setRunning(false);
        while (retry) {
            try {
                _thread.join();
                retry = false;
            } catch (InterruptedException e) {
                // we will try it again and again...
            }
        }
    }

    class DrawThread extends Thread {
        private SurfaceHolder _surfaceHolder;
        private DrawView _view;
        private boolean _run = false;
 
        public DrawThread(SurfaceHolder surfaceHolder, DrawView panel) {
            _surfaceHolder = surfaceHolder;
            _view = panel;
        }
 
        public void setRunning(boolean run) {
            _run = run;
        }
 
        public SurfaceHolder getSurfaceHolder() {
            return _surfaceHolder;
        }
 
        @Override
        public void run() {
            Canvas c;
            while (_run) {
                c = null;
                try {
                    c = _surfaceHolder.lockCanvas(null);
                    synchronized (_surfaceHolder) {
                        _view.onDraw(c);
                    }
                } finally {
                    // do this in a finally so that if an exception is thrown
                    // during the above, we don't leave the Surface in an
                    // inconsistent state
                    if (c != null) {
                        _surfaceHolder.unlockCanvasAndPost(c);
                    }
                }
            }
        }
    }
 
}
