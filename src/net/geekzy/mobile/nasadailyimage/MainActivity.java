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
import android.app.WallpaperManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.googlecode.androidannotations.annotations.AfterInject;
import com.googlecode.androidannotations.annotations.AfterViews;
import com.googlecode.androidannotations.annotations.Bean;
import com.googlecode.androidannotations.annotations.EActivity;
import com.googlecode.androidannotations.annotations.NoTitle;
import com.googlecode.androidannotations.annotations.ViewById;
import com.googlecode.androidannotations.annotations.res.StringRes;

/**
 * Main Activity of Nasa Daily Image
 * @author imam
 */
@NoTitle
@EActivity(R.layout.main)
public class MainActivity extends Activity implements IotdHandlerListener {
	protected final String NASA_FEED_URI = "http://www.nasa.gov/rss/image_of_the_day.rss";

	@ViewById(R.id.main_layout)
	protected LinearLayout mainLayout;
	@ViewById(R.id.imageTitle)
	protected TextView imageTitle;
	@ViewById(R.id.imageDate)
	protected TextView imageDate;
	@ViewById(R.id.imageDisplay)
	protected ImageView imageDisplay;
	@ViewById(R.id.imageDesc)
	protected TextView imageDesc;

	protected ProgressDialog dlgLoad;
	protected Bitmap nasaImg;

	// get loading dialog messages
	@StringRes(R.string.dlg_load_title)
	protected String dlgLoadTitle;
	@StringRes(R.string.dlg_load_msg)
	protected String dlgLoadMsg;

	@StringRes(R.string.notty_wallpaper_set_ok)
	protected String nottyWallpaperSetOk;
	@StringRes(R.string.notty_wallpaper_set_failed)
	protected String nottyWallpaperSetFailed;

	@Bean /* create the rss parser */
	protected IotdHandler iotdHandler;

	// create the UI handler thread to update from proc thread
	protected Handler handler = new Handler();

	@AfterViews
	public void initView() {
		// refresh initially
		refreshFeed();
	}

	@AfterInject
	public void initHandler() {
		// set the listener
		iotdHandler.setListener(this);
	}

	/**
	 * Refresh the feed by loading the rss URL, parse the result and display it.
	 */
	private void refreshFeed() {
		// show loading on intiail refresh
		dlgLoad = ProgressDialog.show(this, dlgLoadTitle, dlgLoadMsg);

		// initiate new thread to get the rss
		new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					// start processing rss feed
					iotdHandler.processFeed(new URL(NASA_FEED_URI));

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
				nasaImg = getImageDisplay(imageUrl);
				// update the image placeholder
				handler.post(new Runnable() {
					@Override
					public void run() {
						imageDisplay.setImageBitmap(nasaImg);
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
	 * to refresh the image of the day feed
	 * @param view the button as the source of click
	 */
	public void onRefresh(View view) {
		refreshFeed(); // refresh the image of the day
	}

	/**
	 * Callback when button set wallpaper is clicked
	 * to set current image as wallpaper
	 * @param view the button as the source of click
	 */
	public void onSetWallpaper(View view) {
		try {

			WallpaperManager wm = WallpaperManager.getInstance(this);
			wm.setBitmap(nasaImg);
			Toast.makeText(MainActivity.this, nottyWallpaperSetOk, Toast.LENGTH_SHORT).show();

		} catch (IOException ignored) {
			Toast.makeText(MainActivity.this, nottyWallpaperSetFailed, Toast.LENGTH_SHORT).show();
		}
	}

	/* (non-Javadoc)
	 * @see net.geekzy.mobile.sax.IotdHandlerListener#iotdParsed(java.lang.String, java.lang.String, java.lang.String, java.lang.String)
	 */
	@Override
	public void iotdParsed(final String url, final String title,
			final String description, final String date) {

		// update UI
		handler.post(new Runnable() {
			@Override
			public void run() {
				resetDisplay(title, date, url, description);
			}
		});
	}
}
