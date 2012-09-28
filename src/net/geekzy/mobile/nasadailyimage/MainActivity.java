package net.geekzy.mobile.nasadailyimage;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import net.geekzy.mobile.sax.IotdHandler;
import net.geekzy.mobile.sax.IotdHandlerListener;
import android.app.Activity;
import android.app.ProgressDialog;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

public class MainActivity extends Activity implements IotdHandlerListener {
	protected final String NASA_FEED_URI = "http://www.nasa.gov/rss/image_of_the_day.rss";

	protected LinearLayout mainLayout;
	protected TextView imageTitle;
	protected TextView imageDate;
	protected ImageView imageDisplay;
	protected TextView imageDesc;
	protected ProgressDialog dlgLoad;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		// Connect interface elements to properties
		mainLayout = (LinearLayout) findViewById(R.id.main_layout);
		imageTitle = (TextView) findViewById(R.id.imageTitle);
		imageDate = (TextView) findViewById(R.id.imageDate);
		imageDisplay = (ImageView) findViewById(R.id.imageDisplay);
		imageDesc = (TextView) findViewById(R.id.imageDesc);

		// refresh initially
		refreshFeed();
	}

	private void refreshFeed() {
		final IotdHandlerListener iotdListener = this;
		final String dlgLoadTitle = getString(R.string.dlg_load_title);
		final String dlgLoadMsg = getString(R.string.dlg_load_msg);
		// show loading on intiail refresh
		dlgLoad = ProgressDialog.show(this, dlgLoadTitle, dlgLoadMsg);
		new Thread(new Runnable() {
			@Override
			public void run() {
				try {

					IotdHandler handler = new IotdHandler();
					handler.setListener(iotdListener);
					handler.processFeed(new URL(NASA_FEED_URI));

				} catch (Exception ignored) {
					dlgLoad.dismiss();
				}
			}
		}).start();
	}

	private void resetDisplay(final String title, final String date,
			final String imageUrl, final String desc) {

		new Thread(new Runnable() {
			@Override
			public void run() {
				final Bitmap bm = getImageDisplay(imageUrl);
				imageDisplay.post(new Runnable() {
					@Override
					public void run() {
						imageDisplay.setImageBitmap(bm);
						imageDisplay.setContentDescription(title);
						dlgLoad.dismiss();
					}
				});
			}
		}).start();

		imageTitle.post(new Runnable() {
			@Override
			public void run() {
				imageTitle.setText(title);
			}
		});

		imageDate.post(new Runnable() {
			@Override
			public void run() {
				imageDate.setText(date);
			}
		});

		imageDesc.post(new Runnable() {
			@Override
			public void run() {
				imageDesc.setText(desc);
			}
		});
	}

	private Bitmap getImageDisplay(String url) {
		try {

			HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
			conn.setDoInput(true);
			conn.connect();

			InputStream input = conn.getInputStream();
			Bitmap bitmap = BitmapFactory.decodeStream(input);
			return bitmap;
		}

		catch (MalformedURLException ignored) {
			mainLayout.setVisibility(View.VISIBLE);
		}
		catch (IOException ignored) {}

		return null;
	}

	public void onRefresh(View view) {
		refreshFeed();
	}

	@Override
	public void iotdParsed(String url, String title, String description,
			String date) {
		resetDisplay(title, date, url, description);
	}
}
