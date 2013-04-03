package mobi.esys.videoregistrator;

import android.app.Activity;
import android.graphics.PixelFormat;
import android.os.Bundle;
import android.widget.MediaController;
import android.widget.TextView;
import android.widget.VideoView;

public class VideoActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.video_activity);

		TextView fileName = (TextView) findViewById(R.id.fileName);
		VideoView videoPlayer = (VideoView) findViewById(R.id.videoPlayer);
		MediaController controller = new MediaController(this);
		videoPlayer.setMediaController(controller);
		videoPlayer.setVideoPath(getIntent().getExtras()
				.getString("video_file"));
		getWindow().setFormat(PixelFormat.TRANSLUCENT);
		fileName.setText(getIntent().getExtras().getString("video_file"));
		videoPlayer.start();
	}

	@Override
	public void onBackPressed() {
		super.onBackPressed();
		finish();
	}

}
