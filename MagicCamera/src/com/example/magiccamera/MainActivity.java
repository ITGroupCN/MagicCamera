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

	// 调用系统相机拍照
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
		// 当系统相机拍照之后返回，执行以下操作
		if (requestCode == REQUEST_TAKE_PHOTO && resultCode == RESULT_OK) {
			SaveCapturedPic();
			galleryAddPic();
			startMagicFactory(data);
		}
		// 当用户在打开系统相机之后取消拍照，以下操作删除临时文件
		else if (resultCode == RESULT_CANCELED) {
			File tempFile = new File(mCurrentPhotoPath);
			if (tempFile != null)
				tempFile.delete();
		}
	}

	// 储存拍下来的照片
	private void SaveCapturedPic() {
		BitmapFactory.decodeFile(mCurrentPhotoPath);
	}

	// 拍下照片之后交由MagicFactory处理
	private void startMagicFactory(Intent intent) {
		Intent startMagicFactoryIntent = new Intent();
	}

	// 构建图片文件
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

	// 创建魔幻相机的照片集目录
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
		// 保存照片集的绝对路径
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

		// 检测用户手机的Android版本决定照片集储存路径
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

	// 取消启动界面
	private void CancelStartup() {
		startupTextView = (TextView) findViewById(R.id.startupTextView);
		startupTextView.setText(R.string.external_storage_unmounted);
		SetUpButtons(false);
	}

	// 检查SD卡是否挂载
	public boolean ExternalStorageMounted() {
		if (((Environment.MEDIA_MOUNTED).equals(Environment
				.getExternalStorageState()))) {
			return true;
		} else
			return false;
	}

	// 建立开始界面的图片预览
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

	// 建立开始界面的两个Button
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

	// 把拍下的照片加入到系统的gallery，一般来说这个方法不用也可以
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
