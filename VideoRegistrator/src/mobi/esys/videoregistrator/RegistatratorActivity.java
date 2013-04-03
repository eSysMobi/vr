package mobi.esys.videoregistrator;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.LocationManager;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.Spinner;

public class RegistatratorActivity extends Activity implements
		SensorEventListener {
	private Camera camera;
	private CameraSurfaceView cameraSurface;
	private MediaRecorder recorder;
	private CheckBox checkBox;
	private transient SensorManager sensorManager;
	private transient Sensor gyroSensor;

	ImageButton myButton;
	SurfaceHolder surfaceHolder;
	boolean recording;
	Spinner formatSpinner;
	String video_name;
	private String[] formats = { "3gp", "mp4" };
	StringBuilder log;
	SimpleDateFormat dateFormat;
	String date;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		dateFormat = new SimpleDateFormat("dd-MM-yyyy-HH-mm",
				Locale.getDefault());
		date = dateFormat.format(new Date());

		recording = false;

		log = new StringBuilder();
		sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
		gyroSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
		sensorManager.registerListener(this, gyroSensor,
				SensorManager.SENSOR_DELAY_NORMAL);

		Intent batteryIntent = getApplicationContext().registerReceiver(null,
				new IntentFilter(Intent.ACTION_BATTERY_CHANGED));

		LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
		if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
			log.append("GPS is enabled");
		} else
			log.append("GPS disabled");

		log.append("\n" + "Battery level:"
				+ String.valueOf(batteryIntent.getIntExtra("level", -1)));

		try {
			File root = new File(Environment.getExternalStorageDirectory(),
					"Registrator");
			if (!root.exists()) {
				root.mkdirs();
			}

			File logFile = new File(root, date + "_" + "log.txt");
			FileWriter writer = new FileWriter(logFile);
			writer.append(log.toString());
			writer.flush();
			writer.close();
		} catch (IOException e) {

		}

		setContentView(R.layout.activity_registatrator);
		getWindow().setFormat(PixelFormat.TRANSLUCENT);
		// Get Camera for preview
		camera = getCameraInstance();

		cameraSurface = new CameraSurfaceView(this, camera);
		FrameLayout myCameraPreview = (FrameLayout) findViewById(R.id.videoView);
		myCameraPreview.addView(cameraSurface);

		myButton = (ImageButton) findViewById(R.id.recBtn);
		myButton.setOnClickListener(myButtonOnClickListener);

		checkBox = (CheckBox) findViewById(R.id.hqBox);

		formatSpinner = (Spinner) findViewById(R.id.formatSpinner);

		ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
				android.R.layout.simple_spinner_item, formats);

		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		formatSpinner.setAdapter(adapter);

	}

	Button.OnClickListener myButtonOnClickListener = new Button.OnClickListener() {

		@Override
		public void onClick(View v) {
			recVideo();
		}

	};

	private void recVideo() {
		if (recording) {
			// stop recording and release camera
			recorder.stop(); // stop the recording
			releaseMediaRecorder(); // release the MediaRecorder object

			startActivity(new Intent(getApplicationContext(),
					VideoActivity.class).putExtra("video_file", video_name));
			finish();
		} else {

			// Release Camera before MediaRecorder start
			releaseCamera();

			if (!prepareMediaRecorder()) {

				finish();
			}

			recorder.start();
			recording = true;
			myButton.setImageDrawable(getResources().getDrawable(
					R.drawable.stop));
		}
	}

	private Camera getCameraInstance() {
		// TODO Auto-generated method stub
		Camera c = null;
		try {
			c = Camera.open(); // attempt to get a Camera instance
		} catch (Exception e) {
			// Camera is not available (in use or does not exist)
		}
		return c; // returns null if camera is unavailable
	}

	private boolean prepareMediaRecorder() {
		camera = getCameraInstance();
		recorder = new MediaRecorder();

		camera.unlock();
		recorder.setCamera(camera);

		initRecorder();

		try {
			recorder.prepare();
		} catch (final IllegalStateException e) {
			releaseMediaRecorder();
			return false;
		} catch (final IOException e) {
			releaseMediaRecorder();
			return false;
		}

		// recorder.setOnInfoListener(new OnInfoListener() {
		//
		// @Override
		// public void onInfo(MediaRecorder mr, int what, int extra) {
		// if (what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED) {
		// recorder.stop();
		// recorder.reset();
		//
		// Toast.makeText(getApplicationContext(),
		// "Запись остановлена", Toast.LENGTH_LONG).show();
		// myButton.setImageDrawable(getResources().getDrawable(
		// R.drawable.rec));
		//
		// try {
		// recorder.prepare();
		// } catch (IllegalStateException e) {
		// recorder.release();
		// } catch (IOException e) {
		// recorder.release();
		// }
		// initRecorder();
		// recorder.start();
		// myButton.setImageDrawable(getResources().getDrawable(
		// R.drawable.stop));
		//
		// }
		//
		// }
		// });
		return true;

	}

	private void initRecorder() {
		recorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
		recorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);

		if (checkBox.isChecked()) {
			recorder.setProfile(CamcorderProfile
					.get(CamcorderProfile.QUALITY_HIGH));
		} else {
			recorder.setProfile(CamcorderProfile
					.get(CamcorderProfile.QUALITY_LOW));
		}

		final File file = new File(
				(Environment.getExternalStorageDirectory().getAbsolutePath()
						+ File.separator + "Registrator" + File.separator));
		if (!file.exists()) {
			file.mkdir();
		}

		final SimpleDateFormat dateFormat = new SimpleDateFormat(
				"dd-MM-yyyy-HH-mm", Locale.getDefault());
		final CharSequence date = dateFormat.format(new Date());
		String file_name = date + ".mp4";

		if (formatSpinner.getSelectedItemPosition() == 1) {

			file_name = date + ".mp4";
		}

		if (formatSpinner.getSelectedItemPosition() == 0) {

			file_name = date + ".3gp";
		}

		final File video = new File(file.getAbsolutePath() + File.separator
				+ file_name);
		video_name = file.getAbsolutePath() + File.separator + file_name;

		recorder.setOutputFile(video.getAbsolutePath());
		recorder.setMaxDuration(10000);// set max video time 15 min
		recorder.setMaxFileSize(250000000); // Set max file size 250 Mb

		recorder.setPreviewDisplay(cameraSurface.getHolder().getSurface());
	}

	@Override
	protected void onPause() {
		super.onPause();
		releaseMediaRecorder(); // if you are using MediaRecorder, release it //
								// first
		releaseCamera(); // release the camera immediately on pause event
		sensorManager.unregisterListener(this);
	}

	private void releaseMediaRecorder() {
		if (recorder != null) {
			recorder.reset(); // clear recorder configuration
			recorder.release(); // release the recorder object
			recorder = null;
			camera.lock(); // lock camera for later use
		}
	}

	private void releaseCamera() {
		if (camera != null) {
			camera.release(); // release the camera for other applications
			camera = null;
		}
	}

	public class CameraSurfaceView extends SurfaceView implements
			SurfaceHolder.Callback {

		private SurfaceHolder holder;
		private Camera camera;

		@SuppressWarnings("deprecation")
		public CameraSurfaceView(Context context, Camera camera) {
			super(context);
			this.camera = camera;

			// Install a SurfaceHolder.Callback so we get notified when the
			// underlying surface is created and destroyed.
			holder = getHolder();
			holder.addCallback(this);
			// deprecated setting, but required on Android versions prior to 3.0
			holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
		}

		@Override
		public void surfaceChanged(SurfaceHolder holder, int format,
				int weight, int height) {
			// If your preview can change or rotate, take care of those events
			// here.
			// Make sure to stop the preview before resizing or reformatting it.

			if (holder.getSurface() == null) {
				// preview surface does not exist
				return;
			}

			// stop preview before making changes
			try {
				camera.stopPreview();
			} catch (Exception e) {
				// ignore: tried to stop a non-existent preview
			}

			// make any resize, rotate or reformatting changes here

			// start preview with new settings
			try {
				camera.setPreviewDisplay(holder);
				camera.startPreview();

			} catch (Exception e) {
			}
		}

		@Override
		public void surfaceCreated(SurfaceHolder holder) {
			// The Surface has been created, now tell the camera where to draw
			// the preview.
			try {
				camera.setPreviewDisplay(holder);
				camera.startPreview();
			} catch (IOException e) {
			}
		}

		@Override
		public void surfaceDestroyed(SurfaceHolder holder) {

		}
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		log.append("\n" + "Gyro used");
		try {
			File root = new File(Environment.getExternalStorageDirectory(),
					"Registrator");
			if (!root.exists()) {
				root.mkdirs();
			}

			File logFile = new File(root, date + "_" + "log.txt");
			FileWriter writer = new FileWriter(logFile);
			writer.append(log.toString());
			writer.flush();
			writer.close();
		} catch (IOException e) {

		}
	}

	@Override
	public void onSensorChanged(SensorEvent event) {

	}

	@Override
	protected void onResume() {
		super.onResume();
		sensorManager.unregisterListener(this);
	}

}
