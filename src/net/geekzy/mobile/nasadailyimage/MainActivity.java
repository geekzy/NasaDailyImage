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
import android.os.Handler;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * Main Activity of Nasa Daily Image
 * @author imam
 */
public class MainActivity extends Activity implements IotdHandlerListener {
	protected final String NASA_FEED_URI = "http://www.nasa.gov/rss/image_of_the_day.rss";

	protected LinearLayout mainLayout;
	protected TextView imageTitle;
	protected TextView imageDate;
	protected ImageView imageDisplay;
	protected TextView imageDesc;
	protected ProgressDialog dlgLoad;
	protected Handler handler;

	/* (non-Javadoc)
	 * @see android.app.Activity#onCreate(android.os.Bundle)
	 */
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

		// create the UI handler thread to update from proc thread
		handler = new Handler();
		// refresh initially
		refreshFeed();
	}

	/**
	 * Refresh the feed by loading the rss URL, parse the result and display it.
	 */
	private void refreshFeed() {
		// get iotdHandler listener reference
		final IotdHandlerListener iotdListener = this;

		// get loading dialog messages
		final String dlgLoadTitle = getString(R.string.dlg_load_title);
		final String dlgLoadMsg = getString(R.string.dlg_load_msg);
		// show loading on intiail refresh
		dlgLoad = ProgressDialog.show(this, dlgLoadTitle, dlgLoadMsg);

		// initiate new thread to get the rss
		new Thread(new Runnable() {
			@Override
			public void run() {
				try {

					// create the rss parser
					IotdHandler handler = new IotdHandler();
					// set the listener
					handler.setListener(iotdListener);
					// start processing rss feed
					handler.processFeed(new URL(NASA_FEED_URI));

				} catch (Exception ignored) {
					// dismiss loading dialog on exception
					dlgLoad.dismiss();
				}
			}
		}).start(); // start the thread immidiately
	}

	/**
	 * Update the display from the latest feed
	 * @param title the image title
	 * @param date the image taken timestamp
	 * @param imageUrl the image URL
	 * @param desc the image description
	 */
	private void resetDisplay(final String title, final String date,
			final String imageUrl, final String desc) {

		// initiate new thread to fetch the image from a URL
		new Thread(new Runnable() {
			@Override
			public void run() {
				// save it as bitmap
				final Bitmap bm = getImageDisplay(imageUrl);
				// update the image placeholder
				handler.post(new Runnable() {
					@Override
					public void run() {
						imageDisplay.setImageBitmap(bm);
						imageDisplay.setContentDescription(title);
						// dismiss the loading dialog once the image is loaded
						dlgLoad.dismiss();
					}
				});
			}
		}).start(); // start the thread immidiately

		// update the title
		imageTitle.setText(title);

		// update the date
		imageDate.setText(date);

		// update the image description
		imageDesc.setText(desc);
	}

	/**
	 * Fetch the image from the given URL
	 * @param url the image URL
	 * @return the image as Bitmap
	 */
	private Bitmap getImageDisplay(String url) {
		try {

			// crate a connection to fetch the URL
			HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
			conn.setDoInput(true); // allows input
			conn.connect(); // connect

			// get the data stream
			InputStream input = conn.getInputStream();
			Bitmap bitmap = BitmapFactory.decodeStream(input); // decode as Bitmap
			return bitmap;
		}

		catch (MalformedURLException ignored) {
			// dismiss loading dialog on exception
			dlgLoad.dismiss();
		}
		catch (IOException ignored) {}
		return null;
	}

	/**
	 * Callback when button refresh is clicked
	 * @param view the object of the clicked source
	 */
	public void onRefresh(View view) {
		refreshFeed(); // refresh the image of the day
	}

	/* (non-Javadoc)
	 * @see net.geekzy.mobile.sax.IotdHandlerListener#iotdParsed(java.lang.String, java.lang.String, java.lang.String, java.lang.String)
	 */
	@Override
	public void iotdParsed(final String url, final String title,
			final String description, final String date) {

		handler.post(new Runnable() {
			@Override
			public void run() {
				resetDisplay(title, date, url, description);
			}
		});
	}
}
