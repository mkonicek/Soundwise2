package com.mobreactor.soundwiselib.send;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.AssetManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.Bitmap.Config;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.provider.MediaStore.Images.Media;
import android.text.Html;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;

import com.mobreactor.soundwiselib.Options;
import com.mobreactor.soundwiselib.R;
import com.mobreactor.utils.FlurryTrackedActivity;

public class SendSourceActivity extends FlurryTrackedActivity {
	private static final int SELECT_PICTURE_FROM_GALLERY = 1;
	private static final int CAPTURE_PICTURE_BY_CAMERA = 2;

	Button galleryButton;
	Button cameraButton;
	Button drawButton;
	Button textButton;
	Button samplesButton;
	
	Uri cameraImageUri;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.sendsource);
		
		this.galleryButton = (Button) this.findViewById(R.id.galleryButton);
		galleryButton.setOnClickListener(new OnClickListener() {
			public void onClick(View view) {
				// http://stackoverflow.com/questions/2169649/open-an-image-in-androids-built-in-gallery-app-programmatically
				// call system intent to choose image from internal memory / SD card / camera
				//Intent intent = new Intent();
				//intent.setType("image/*");
				//intent.setAction(Intent.ACTION_GET_CONTENT);
				//
				//startActivityForResult(Intent.createChooser(intent, "Select picture"), SELECT_PICTURE_FROM_GALLERY);

				// http://stackoverflow.com/questions/550905/access-pictures-from-pictures-app-in-my-android-app
				// choose from images on SD card
				Intent intent = new Intent();
				intent.setAction(Intent.ACTION_GET_CONTENT);
				intent.setDataAndType(android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/jpeg");
			
				startActivityForResult(intent, SELECT_PICTURE_FROM_GALLERY);
			}
		});
		
		this.cameraButton = (Button) this.findViewById(R.id.cameraButton);
		cameraButton.setOnClickListener(new OnClickListener() {
			public void onClick(View view) {
				// http://achorniy.wordpress.com/2010/04/26/howto-launch-android-camera-using-intents/
				ContentValues values = new ContentValues();
				values.put(Media.MIME_TYPE, "image/jpeg");
				cameraImageUri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
				
				Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
				intent.putExtra(MediaStore.EXTRA_OUTPUT, cameraImageUri);
				intent.putExtra(MediaStore.EXTRA_VIDEO_QUALITY, 1);
			
				startActivityForResult(intent, CAPTURE_PICTURE_BY_CAMERA);
			}
		});
		
		this.samplesButton = (Button) this.findViewById(R.id.samplesButton);
		samplesButton.setOnClickListener(new OnClickListener() {
			public void onClick(View view) {
				AlertDialog.Builder builder = new AlertDialog.Builder(SendSourceActivity.this);
				builder.setTitle("Pick one");
				final AssetManager assets = getAssets();
				final String[] sampleItems;
				try {
					sampleItems = assets.list("samples");
					// strip the .jpg extension to display just the name
					for (int i = 0; i < sampleItems.length; i++) {
						sampleItems[i] = sampleItems[i].substring(0, sampleItems[i].length() - 4);
					}
				} catch (IOException e) {
					return;
				}
				//final String[] sampleItems = ss.clone();
				android.content.DialogInterface.OnClickListener sampleSelectedListener = new android.content.DialogInterface.OnClickListener() {
					public void onClick(android.content.DialogInterface dialog, int which) {
						InputStream imageInputStream;
						try {
							// assumes .jpg extension
							imageInputStream = assets.open("samples/" + sampleItems[which] + ".jpg");
						} catch (IOException e) {
							return;
						}
						sendPicture(imageInputStream);
					}
				};
				builder.setItems(sampleItems, sampleSelectedListener);
				builder.create().show();
			}
		});
		
		this.drawButton = (Button) this.findViewById(R.id.drawButton);
		drawButton.setOnClickListener(new OnClickListener() {
			public void onClick(View view) {
				Intent intent = new Intent();
//				intent.setClass(view.getContext(), DrawActivity.class);
				
				startActivity(intent);
			}
		});
		
		this.textButton = (Button) this.findViewById(R.id.textButton);
		textButton.setOnClickListener(new OnClickListener() {
			public void onClick(View view) {
				Intent intent = new Intent();
//				intent.setClass(view.getContext(), SendSourceText.class);
				
				startActivity(intent);
			}
		});
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		// http://stackoverflow.com/questions/2169649/open-an-image-in-androids-built-in-gallery-app-programmatically
		// get selected picture file system path
		if (resultCode == RESULT_OK) {
			if (requestCode == SELECT_PICTURE_FROM_GALLERY) {
				
				// picture selected from gallery
				
				Uri selectedImageUri = data.getData();

				// OI FILE Manager
				String filemanagerstring = selectedImageUri.getPath();

				// MEDIA GALLERY
				String selectedImagePath = getPathFromImageUri(selectedImageUri);

				// NOW WE HAVE OUR WANTED STRING
				String finalPath = selectedImagePath != null ? selectedImagePath : filemanagerstring;
				
				if (finalPath != null) sendPicture(finalPath);
				
			} else if (requestCode == CAPTURE_PICTURE_BY_CAMERA) {
				
				// picture captured by camera
				
				String selectedImagePath = getPathFromImageUri(cameraImageUri);
				
				if (selectedImagePath != null) sendPicture(selectedImagePath);
				
			}
		} else {
			Toast.makeText(this, "Picture was not selected", Toast.LENGTH_LONG);
		}
	}

	private void sendPicture(String imagePath) {
		String fileName = scaleAndRotateFile(imagePath);
		sendScreenFittingPicture(fileName);
	}
	
	private void sendPicture(InputStream imageInputStream) {
		String fileName = scaleAndRotateFile(imageInputStream);
		sendScreenFittingPicture(fileName);
	}
		
	private void sendScreenFittingPicture(String fileName)
	{
		if (fileName == null) return;

		// call activity that will transmit the image
		Intent intent = new Intent();
		intent.setClass(this, SendActivity.class);

		Bundle bundle = new Bundle();
		bundle.putString("imagePath", fileName);
		intent.putExtras(bundle);
		startActivity(intent);
		
		// finish image source selection after sending
		finish();
	}
	
	private String scaleAndRotateFile(String imagePath) {
		// load original bitmap
		Bitmap bUnscaled = BitmapFactory.decodeFile(imagePath);
		return scaleAndRotateFile(bUnscaled);
	}
	
	private String scaleAndRotateFile(InputStream imageInputStream) {
		// load original bitmap
		Bitmap bUnscaled = BitmapFactory.decodeStream(imageInputStream);
		return scaleAndRotateFile(bUnscaled);
	}

	/** Takes a bitmap, scales and rotates it to the form suitable for FFT sender,
	 *  saves it to a file and returns the filename.
	 *  
	 *   We are saving to a file instead of directly returning the modified bitmap
	 *   because we then need to pass the bitmap to the SendActivity,
	 *   and passing bitmaps between activities is not supported (just strings, byte arrays,...).
	 *   */
	private String scaleAndRotateFile(Bitmap bUnscaled) {
		// image will be sent FROM TOP TO BOTTOM (composing columns) and received the same way it was sent
		
		// scale the image to SENDER_FFT_SPECTRUM_SIZE (even if it is smaller)
		int smallerDimension = Math.min(bUnscaled.getWidth(), bUnscaled.getHeight());
		double scale = Options.SENDER_FFT_SPECTRUM_SIZE / (double)smallerDimension;
		Bitmap bScaled = Bitmap.createScaledBitmap(bUnscaled, (int)(bUnscaled.getWidth() * scale), (int)(bUnscaled.getHeight() * scale), true);
		// very important, otherwise the large bitmap stays in memory!
		bUnscaled.recycle();
		
		// if the bitmap is wide, rotate it 90 degrees, so that it fits on the display better
		Bitmap bScaledRotated = bScaled;
		if (bScaled.getWidth() > bScaled.getHeight()) {
			Matrix rotateMatrix = new Matrix();
	    	rotateMatrix.postRotate(90);
	    	bScaledRotated = Bitmap.createBitmap(bScaled, 0, 0, bScaled.getWidth(), bScaled.getHeight(), rotateMatrix, true);
	    	bScaled.recycle();
		}
		
		// write it to a file and return its filename
		String filename = getFilesDir() + "/final-image.png";
		File f = new File(filename);
		if (f.exists()) {
			f.delete();
		}
		try {
			FileOutputStream out = new FileOutputStream(filename);
			bScaledRotated.compress(Bitmap.CompressFormat.PNG, 90, out);
			out.flush();
			out.close();
		} catch (Exception e) {
			Log.e(Options.TAG, e.toString());
			Toast.makeText(this, "Cannot write to file " + filename, Toast.LENGTH_LONG);
			return null;
		}

		return filename;
	}

	private String getPathFromImageUri(Uri selectedImageUri) {
		// http://stackoverflow.com/questions/2169649/open-an-image-in-androids-built-in-gallery-app-programmatically
		// first way
		String[] projection = { MediaStore.Images.Media.DATA };
		Cursor cursor = null;
		try
		{
			cursor = managedQuery(selectedImageUri, projection, null, null, null);
			if (cursor != null) {
				// HERE YOU WILL GET A NULLPOINTER IF CURSOR IS NULL
				// THIS CAN BE, IF YOU USED OI FILE MANAGER FOR PICKING THE MEDIA
				int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
				cursor.moveToFirst();
				return cursor.getString(column_index);
			}
			return null;
		} finally {
			if (cursor != null) cursor.close();
		}

//		// http://stackoverflow.com/questions/2169649/open-an-image-in-androids-built-in-gallery-app-programmatically
//		// second way
//		if (selectedImageUri != null) {
//			Cursor cursor2 = getContentResolver().query(selectedImageUri, null,
//					null, null, null);
//			cursor2.moveToFirst();
//			String imageFilePath = cursor2.getString(0);
//			cursor2.close();
//			return imageFilePath;
//		}
//		return null;
	}

}
