package com.example.magiccamera;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

public class MainActivity extends Activity {

	private static final int REQUEST_TAKE_PHOTO = 1;

	private static final String JPEG_FILE_PREFIX = "MC_IMG_";
	private static final String JPEG_FILE_SUFFIX = ".jpg";

	private String mCurrentPhotoPath;
	private String albumAbsolutePath;

	private TextView startupTextView;
	private ImageView startupImageView;

	private AlbumStorageDirFactory albumStorageDirFactory;

	// ����ϵͳ�������
	private void TakePicture() {
		Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
		if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
			// Create the File where the photo should go
			File photoFile = null;
			try {
				// Create a temporary file to store the image
				photoFile = createImageFile();
			} catch (IOException ex) {
				// Do nothing when exception occurs
			}
			// Continue only if the File was successfully created
			if (photoFile != null) {
				takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT,
						Uri.fromFile(photoFile));
				startActivityForResult(takePictureIntent, REQUEST_TAKE_PHOTO);
			}
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		// ��ϵͳ�������֮�󷵻أ�ִ�����²���
		if (requestCode == REQUEST_TAKE_PHOTO && resultCode == RESULT_OK) {
			SaveCapturedPic();
			galleryAddPic();
			startMagicFactory(data);
		}
		// ���û��ڴ�ϵͳ���֮��ȡ�����գ����²���ɾ����ʱ�ļ�
		else if (resultCode == RESULT_CANCELED) {
			File tempFile = new File(mCurrentPhotoPath);
			if (tempFile != null)
				tempFile.delete();
		}
	}

	// ��������������Ƭ
	private void SaveCapturedPic() {
		BitmapFactory.decodeFile(mCurrentPhotoPath);
	}

	// ������Ƭ֮����MagicFactory����
	private void startMagicFactory(Intent intent) {
		Intent startMagicFactoryIntent = new Intent();
	}

	// ����ͼƬ�ļ�
	private File createImageFile() throws IOException {
		// Create an image file name
		String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss")
				.format(new Date());
		String imageFileName = JPEG_FILE_PREFIX + timeStamp;
		File storageDir = new File(albumAbsolutePath);
		File image = File.createTempFile(imageFileName, /* prefix */
				JPEG_FILE_SUFFIX, /* suffix */
				storageDir /* directory */
		);

		// Save a file: path for use with ACTION_VIEW intents
		mCurrentPhotoPath = image.getAbsolutePath();
		return image;
	}

	// ����ħ���������Ƭ��Ŀ¼
	private void CreateAlbumDir() {
		File storageDir = albumStorageDirFactory
				.getAlbumStorageDir(getAlbumName());
		if (storageDir != null) {
			if (!storageDir.mkdirs()) {
				if (!storageDir.exists()) {
					Log.d("CameraSample", "failed to create directory");
				}
			}
		}
		// ������Ƭ���ľ���·��
		albumAbsolutePath = storageDir.getAbsolutePath();
		Log.d("albumAbsolutePath", storageDir.getAbsolutePath());
	}

	/* Photo album for this application */
	private String getAlbumName() {
		return getString(R.string.album_name);
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		// ����û��ֻ���Android�汾������Ƭ������·��
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.FROYO) {
			albumStorageDirFactory = new FroyoAlbumDirFactory();
		} else {
			albumStorageDirFactory = new BaseAlbumDirFactory();
		}

		if (ExternalStorageMounted()) {
			SetUpButtons(true);
			CreateAlbumDir();
			SetImagePreview();
		} else {
			CancelStartup();
		}

	}

	// ȡ����������
	private void CancelStartup() {
		startupTextView = (TextView) findViewById(R.id.startupTextView);
		startupTextView.setText(R.string.external_storage_unmounted);
		SetUpButtons(false);
	}

	// ���SD���Ƿ����
	public boolean ExternalStorageMounted() {
		if (((Environment.MEDIA_MOUNTED).equals(Environment
				.getExternalStorageState()))) {
			return true;
		} else
			return false;
	}

	// ������ʼ�����ͼƬԤ��
	public void SetImagePreview() {
		startupImageView = (ImageView) findViewById(R.id.startupImageView);
		String albumDir = new File(albumAbsolutePath).getAbsolutePath();
		String[] fileNames = new File(albumAbsolutePath).list();
		Bitmap imageToPreview;
		for (int i = fileNames.length - 1; i >= 0; i--) {
			if ((fileNames[i].startsWith(JPEG_FILE_PREFIX))
					&& (fileNames[i].endsWith(JPEG_FILE_SUFFIX))) {
				startupTextView = (TextView) findViewById(R.id.startupTextView);
				startupTextView.setVisibility(View.GONE);
				imageToPreview = BitmapFactory.decodeFile(albumDir + "/"
						+ fileNames[i]);
				startupImageView.setImageBitmap(imageToPreview);
				break;
			}
		}
	}

	// ������ʼ���������Button
	private void SetUpButtons(boolean enabled) {
		Button fireLocalCamera = (Button) findViewById(R.id.localCamera);
		if (enabled) {
			if (isIntentAvailable(this, MediaStore.ACTION_IMAGE_CAPTURE)) {
				fireLocalCamera
						.setOnClickListener(new Button.OnClickListener() {
							@Override
							public void onClick(View v) {
								// TODO Auto-generated method stub
								TakePicture();
							}
						});
			}
		} else {
			fireLocalCamera.setEnabled(false);
		}
	}

	// �����µ���Ƭ���뵽ϵͳ��gallery��һ����˵�����������Ҳ����
	private void galleryAddPic() {
		Intent mediaScanIntent = new Intent(
				Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
		File f = new File(mCurrentPhotoPath);
		Uri contentUri = Uri.fromFile(f);
		mediaScanIntent.setData(contentUri);
		sendBroadcast(mediaScanIntent);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	/**
	 * Indicates whether the specified action can be used as an intent. This
	 * method queries the package manager for installed packages that can
	 * respond to an intent with the specified action. If no suitable package is
	 * found, this method returns false.
	 * http://android-developers.blogspot.com/2009/01/can-i-use-this-intent.html
	 * 
	 * @param context
	 *            The application's environment.
	 * @param action
	 *            The Intent action to check for availability.
	 * 
	 * @return True if an Intent with the specified action can be sent and
	 *         responded to, false otherwise.
	 */
	public static boolean isIntentAvailable(Context context, String action) {
		final PackageManager packageManager = context.getPackageManager();
		final Intent intent = new Intent(action);
		List<ResolveInfo> list = packageManager.queryIntentActivities(intent,
				PackageManager.MATCH_DEFAULT_ONLY);
		return list.size() > 0;
	}

}
