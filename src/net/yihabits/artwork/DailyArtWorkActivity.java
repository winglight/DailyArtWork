package net.yihabits.artwork;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;

import org.apache.commons.io.output.ByteArrayOutputStream;

import net.yihabits.artwork.db.ArtDAO;
import net.yihabits.artwork.db.ArtDBOpenHelper;
import net.yihabits.artwork.db.ArtModel;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.GestureDetector;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.AbsSpinner;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.Gallery;
import android.widget.Gallery.LayoutParams;
import android.widget.ImageSwitcher;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewSwitcher.ViewFactory;

import com.google.ads.AdRequest;
import com.google.ads.AdSize;
import com.google.ads.AdView;

public class DailyArtWorkActivity extends Activity implements ViewFactory {

	private static String TAG = "DailyArtWorkActivity";

	private ImageSwitcher imageSwitcher;

	private ArrayList<ArtModel> artList = new ArrayList<ArtModel>();

	private ArrayList<ArtModel> dailyArtList = new ArrayList<ArtModel>();

	private DownloadUtil util;
	
	private int imageSequence = 0;

	private int mode = 2; // 1 - online ; 2 - offline

	private int source = 1; // 1 - random ; 2 - search ; 3 - local files

	private Gallery gallery;

	private int page = 0; // current page of local source

	private int zoom = 0; // 0 - zoom in ; 1 - zoom out

	private LinearLayout downloadPanel;

	private LinearLayout mainPanel;

	private LinearLayout detailsPanel;

	private LinearLayout btnPanel;

	private TextView nameLbl;

	private TextView authorLbl;

	private TextView detailsLbl;

	private TextView yearLbl;

	private TextView locationLbl;

	private MyScrollView scroll;

	private Button zoomBtn;

	private GestureDetector mGestureDetector;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		
		// initialize panels
		downloadPanel = (LinearLayout) findViewById(R.id.downloadPanel);
		downloadPanel.setVisibility(View.GONE);

		btnPanel = (LinearLayout) findViewById(R.id.btn_layout);
		btnPanel.setVisibility(View.GONE);

		detailsPanel = (LinearLayout) findViewById(R.id.detailsPanel);
		detailsPanel.setVisibility(View.GONE);

		nameLbl = (TextView) findViewById(R.id.nameLbl);
		authorLbl = (TextView) findViewById(R.id.authorLbl);
		detailsLbl = (TextView) findViewById(R.id.detailsLbl);
		yearLbl = (TextView) findViewById(R.id.yearLbl);
		locationLbl = (TextView) findViewById(R.id.locationLbl);

		mainPanel = (LinearLayout) findViewById(R.id.mainLayout);

		imageSwitcher = (ImageSwitcher) findViewById(R.id.switcher1);
		scroll = (MyScrollView) findViewById(R.id.myscroll);
		mainPanel.removeView(scroll);
		scroll.removeView(imageSwitcher);
		mainPanel.addView(imageSwitcher);
		// set OnTouchListener on TextView
		mainPanel.setLongClickable(true);
		mGestureDetector = new GestureDetector(this, new MyGesture()); 
		mainPanel.setOnTouchListener(new OnTouchListener(){
			
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				// OnGestureListener will analyzes the given motion event
				return mGestureDetector.onTouchEvent(event);
			}
			
		});

		// initialize DownloadUtil
		if (util == null) {
			util = new DownloadUtil(this);
		}

		// initialize data
		addMore();

		// ad initialization
		// Create the adView
		AdView adView = new AdView(this, AdSize.BANNER, "a14e48881ca1afa");
		// Lookup your LinearLayout assuming it��s been given
		// the attribute android:id="@+id/mainLayout"
		LinearLayout layout = (LinearLayout) findViewById(R.id.ad_layout);
		// Add the adView to it
		layout.addView(adView);
		// Initiate a generic request to load it with an ad
		adView.loadAd(new AdRequest());

		zoomBtn = (Button) findViewById(R.id.zoomBtn);
		zoomBtn.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				resetBtn();

			}
		});

		Button detailsBtn = (Button) findViewById(R.id.detailsBtn);
		detailsBtn.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				showDetails();

			}
		});

		imageSwitcher.setFactory(this);
		imageSwitcher.setInAnimation(AnimationUtils.loadAnimation(this,
				android.R.anim.fade_in));
		imageSwitcher.setOutAnimation(AnimationUtils.loadAnimation(this,
				android.R.anim.fade_out));

		gallery = (Gallery) findViewById(R.id.gallery1);
		gallery.setAdapter(new ImageAdapter(this));
		gallery.setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(AdapterView parent, View v, int position,
					long id) {
				setGallerySelection(position);
			}
		});

	}
	
	private void setGallerySelection(int position){
		if (position == artList.size()) {

			// imageSwitcher.setImageResource(R.drawable.ic_launcher_gallery);
			if (source == 1 && util.getTaskAmount() > 0) {
				// confirm to exit
				// Ask the user if they want to quit
				new AlertDialog.Builder(DailyArtWorkActivity.this)
						.setIcon(android.R.drawable.ic_dialog_alert)
						.setTitle(R.string.confirm)
						.setMessage(
								DailyArtWorkActivity.this
										.getString(
												R.string.confirmDownload)
										.replace(
												"%1",
												String.valueOf(util
														.getTaskAmount())))
						.setPositiveButton(R.string.yes,
								new DialogInterface.OnClickListener() {

									@Override
									public void onClick(
											DialogInterface dialog,
											int which) {

										addMore();
									}

								})
						.setNegativeButton(R.string.cancel, null)
						.show();
			} else {

				// add more images
				addMore();
			}
		} else {
			//set current image position
			imageSequence = position;
			
			// normal image
			String url = "";
			if (mode == 1) {
				url = artList.get(position).getImageUrl();
				imageSwitcher.setImageDrawable(loadDrawable(url,
						artList.get(position).getName()));
			} else {
				url = artList.get(position).getImageLocation();

				imageSwitcher.setImageURI(Uri.parse(url));

				File tmp = new File(url);
				if (!tmp.exists()) {
					toastMsg(R.string.waitOriginal);
					if (source == 3) {
						ArrayList<String> urls = new ArrayList<String>();
						urls.add(url);
						util.downloadImages(urls, false);
					}
				}

			}

			// fill in details of art
			nameLbl.setText(artList.get(position).getName());
			authorLbl.setText(artList.get(position).getAuthor());
			detailsLbl.setText(artList.get(position).getDetails());
			yearLbl.setText(artList.get(position).getYear());
			locationLbl.setText(artList.get(position).getLocation());

		}
	}

	@Override
	protected void onStart() {
		// TODO Auto-generated method stub
		super.onStart();

	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		
		//destroy all of fields
		this.artList.clear();
		this.dailyArtList.clear();
		this.util.stopAllThreads();
		this.util = null;
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		// We do nothing here. We're only handling this to keep orientation
		// or keyboard hiding from causing the WebView activity to restart.
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		MenuInflater mi = getMenuInflater();
		mi.inflate(R.menu.main_menu, menu);
		return true;
	}

	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_random: {
			if (source != 1) {
				resetData();
				source = 1;

				if (dailyArtList.size() > 0) {
					if (artList == null) {
						artList = new ArrayList<ArtModel>();
					}
					
					artList.addAll(dailyArtList);
					dailyArtList.clear();
					
					// refresh the gallery view
					refreshGallery();
				} else {
					addMore();
				}
				
				//set the title of activity
				this.setTitle(this.getString(R.string.app_name_daily));
			}

			return true;
		}
			// case R.id.menu_search: {
			//
			// return true;
			// }
		case R.id.menu_gallery: {
			if (source != 3) {
				dailyArtList.clear();
				dailyArtList.addAll(artList);
				
				resetData();
				source = 3;
				
				addMore();
				
				//set the title of activity
				this.setTitle(this.getString(R.string.app_name_local));
			}

			return true;
		}
		case R.id.menu_help: {
			
			// popup the about window
			Intent intent = new Intent(Intent.ACTION_VIEW);
			intent.setClass(DailyArtWorkActivity.this, AboutActivity.class);
			startActivity(intent);
			return true;
		}
		}
		return super.onMenuItemSelected(featureId, item);
	}

	public void resetBtn() {
		runOnUiThread(new Runnable() {

			@Override
			public void run() {
				if (zoom == 0) {
					// button is zoom in status, then change it to zoom out
					zoom = 1;
					zoomBtn.setBackgroundResource(R.drawable.zoom_out_48);
					mainPanel.removeView(imageSwitcher);
					mainPanel.addView(scroll);
					scroll.addView(imageSwitcher);
				} else {
					// button is zoom out status, then change it to zoom in
					zoom = 0;
					zoomBtn.setBackgroundResource(R.drawable.zoom_in_48);
					mainPanel.removeView(scroll);
					scroll.removeView(imageSwitcher);
					mainPanel.addView(imageSwitcher);
				}
				imageSwitcher.refreshDrawableState();
			}
		});
	}

	public void showDetails() {
		runOnUiThread(new Runnable() {

			@Override
			public void run() {
				if (detailsPanel.getVisibility() == View.GONE) {
					detailsPanel.setVisibility(View.VISIBLE);
				} else {
					detailsPanel.setVisibility(View.GONE);
				}
			}
		});
	}

	public void addMore() {

		Runnable saveUrl = new Runnable() {
			public void run() {

				if (artList == null) {
					artList = new ArrayList<ArtModel>();
				}

				// show hold downloadPanel
				if (artList.size() == 0 && source == 1) {
					showHoldPanel(true);

				}

				switch (source) {

				case 1: {
					// get 10 random images
					page++;
					artList.addAll(util.getRandomImages());

					break;
				}
				case 2: {
					// search 10 images once

					break;
				}
				case 3: {
					// get 10 images from local db
					page++;
					ArrayList<ArtModel> tmp = getLocalArts();
					if(tmp.size() > 0){
					artList.addAll(tmp);
					}else{
						toastMsg(R.string.noMore);
					}

					break;
				}
				default:
					;
				}

				// hide hold downloadPanel
					showHoldPanel(false);

				// refresh the gallery view
				refreshGallery();
			}
		};
		new Thread(saveUrl).start();

	}

	public ArrayList<ArtModel> getLocalArts() {
		ArrayList<ArtModel> list = new ArrayList<ArtModel>();
		ArtDAO dba = ArtDAO.getInstance(this);
		dba.open();

		Cursor c = dba.getAllArt(page);
		startManagingCursor(c);
		if (c.moveToFirst()) {
			do {
				long id = c.getLong(0);
				String name = c.getString(c
						.getColumnIndex(ArtDBOpenHelper.ART_NAME));
				String author = c.getString(c
						.getColumnIndex(ArtDBOpenHelper.AUTHOR));
				String authorDetails = c.getString(c
						.getColumnIndex(ArtDBOpenHelper.AUTHOR_DETAILS));
				String details = c.getString(c
						.getColumnIndex(ArtDBOpenHelper.DETAILS));
				String imageLocation = c.getString(c
						.getColumnIndex(ArtDBOpenHelper.IMAGE_LOCATOIN));
				String imageUrl = c.getString(c
						.getColumnIndex(ArtDBOpenHelper.IMAGE_URL));
				String location = c.getString(c
						.getColumnIndex(ArtDBOpenHelper.LOCATION));
				String preImageLocation = c.getString(c
						.getColumnIndex(ArtDBOpenHelper.PRE_IMAGE_LOCATOIN));
				String preImageUrl = c.getString(c
						.getColumnIndex(ArtDBOpenHelper.PRE_IMAGE_URL));
				String year = c.getString(c
						.getColumnIndex(ArtDBOpenHelper.YEAR));

				ArtModel temp = new ArtModel();
				temp.setId(id);
				temp.setName(name);
				temp.setAuthor(author);
				temp.setAuthorDetails(authorDetails);
				temp.setDetails(details);
				temp.setImageLocation(imageLocation);
				temp.setImageUrl(imageUrl);
				temp.setLocation(location);
				temp.setPreImageLocation(preImageLocation);
				temp.setPreImageUrl(preImageUrl);
				temp.setYear(year);

				list.add(temp);
			} while (c.moveToNext());
		}

		c.close();

		dba.close();

		return list;
	}

	public void resetData() {
		artList.clear();
		page = 0;
		gallery.setAdapter(new ImageAdapter(this));
		imageSwitcher.reset();

		nameLbl.setText("");
		authorLbl.setText("");
		detailsLbl.setText("");
		yearLbl.setText("");
		locationLbl.setText("");

		detailsPanel.setVisibility(View.GONE);
		
	}

	public void refreshGallery() {
		runOnUiThread(new Runnable() {

			@Override
			public void run() {
				((BaseAdapter) gallery.getAdapter()).notifyDataSetChanged();
			}
		});

	}

	public void showHoldPanel(final boolean show) {
		runOnUiThread(new Runnable() {

			@Override
			public void run() {
				if (show) {
					downloadPanel.setVisibility(View.VISIBLE);
					btnPanel.setVisibility(View.GONE);
				} else {
					downloadPanel.setVisibility(View.GONE);
					btnPanel.setVisibility(View.VISIBLE);
				}
			}
		});

	}

	public static Bitmap loadBitmap(String url) {
		Bitmap bm = null;
		try {
			URL aURL = new URL(url);
			URLConnection conn = aURL.openConnection();
			conn.connect();
			InputStream is = conn.getInputStream();
			BufferedInputStream bis = new BufferedInputStream(is);
			bm = BitmapFactory.decodeStream(bis);
			bis.close();
			is.close();
		} catch (IOException e) {
			Log.e(TAG, "Error getting bitmap", e);
		}
		return bm;
	}

	public static Drawable loadDrawable(String url, String srcName) {
		try {
			URL aURL = new URL(url);
			URLConnection conn = aURL.openConnection();
			conn.connect();
			InputStream is = conn.getInputStream();
			return Drawable.createFromStream(is, srcName);
		} catch (IOException e) {
			Log.e(TAG, "Error getting drawable", e);
		}
		return null;
	}

	public void toastMsg(int resId) {
		final String msg = this.getString(resId);
		runOnUiThread(new Runnable() {

			@Override
			public void run() {
				Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_LONG)
						.show();
			}
		});
	}

	@Override
	public View makeView() {
		ImageView imageView = new ImageView(this);
		imageView.setBackgroundColor(0xFF000000);
		imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);

		imageView.setLayoutParams(new ImageSwitcher.LayoutParams(
				LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT));

		return imageView;
	}

	public class ImageAdapter extends BaseAdapter {
		private Context context;
		private int itemBackground;

		public ImageAdapter(Context c) {
			context = c;

			// ---setting the style---
			TypedArray a = obtainStyledAttributes(R.styleable.Gallery1);
			itemBackground = a.getResourceId(
					R.styleable.Gallery1_android_galleryItemBackground, 0);
			a.recycle();
		}

		// ---returns the number of images---
		public int getCount() {
			return artList.size() + 1;
		}

		// ---returns the ID of an item---
		public Object getItem(int position) {
			return position;
		}

		public long getItemId(int position) {
			return position;
		}

		// ---returns an ImageView view---
		public View getView(int position, View convertView, ViewGroup parent) {

			ImageView imageView = new ImageView(context);
			if (artList == null || position == artList.size()) {
				// the last image for add more
				imageView.setImageResource(R.drawable.navigate_right256);
			} else {
				// normal image
				String url = "";
				if (mode == 1) {
					url = artList.get(position).getPreImageUrl();

					imageView.setImageBitmap(loadBitmap(url));
				} else {
					url = artList.get(position).getPreImageLocation();
					imageView.setImageURI(Uri.parse(url));
				}

			}
			imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
			imageView.setLayoutParams(new Gallery.LayoutParams(150, 120));
			imageView.setBackgroundResource(itemBackground);

			return imageView;
		}
	}
	
	public class MyGesture extends SimpleOnGestureListener {
		private static final float FLING_MIN_DISTANCE = 50;
		private static final float FLING_MIN_VELOCITY = 100;

		@Override
		public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
		        float velocityY) {
		    // 参数解释：
		    // e1：第1个ACTION_DOWN MotionEvent
		    // e2：最后一个ACTION_MOVE MotionEvent
		    // velocityX：X轴上的移动速度，像素/秒
		    // velocityY：Y轴上的移动速度，像素/秒
		 
		    // 触发条件 ：
		    // X轴的坐标位移大于FLING_MIN_DISTANCE，且移动速度大于FLING_MIN_VELOCITY个像素/秒
		 
		    if (e1.getX() - e2.getX() > FLING_MIN_DISTANCE
		            && Math.abs(velocityX) > FLING_MIN_VELOCITY) {
		        // Fling left
		    	if (imageSequence < artList.size() - 1) {
					// back to the next image
					imageSequence++;
					gallery.setSelection(imageSequence);
					setGallerySelection(imageSequence);
				} else {
					// give a tip to user
					toastMsg(R.string.alreadyLast);
				}
		    } else if (e2.getX() - e1.getX() > FLING_MIN_DISTANCE
		            && Math.abs(velocityX) > FLING_MIN_VELOCITY) {
		        // Fling right
		    	if (imageSequence > 0) {
					// back to the previous image
					imageSequence--;
					gallery.setSelection(imageSequence);
					setGallerySelection(imageSequence);
				} else {
					// give a tip to user
					toastMsg(R.string.alreadyFirst);
				}
		    	
		    }
		 
		    return false;
		}

		@Override
		public boolean onDown(MotionEvent e) {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public void onLongPress(MotionEvent e) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public boolean onScroll(MotionEvent e1, MotionEvent e2,
				float distanceX, float distanceY) {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public void onShowPress(MotionEvent e) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public boolean onSingleTapUp(MotionEvent e) {
			showDetails();
			return false;
		}
		
		@Override
		public boolean onDoubleTap(MotionEvent e) {
			resetBtn();
			
			return super.onDoubleTap(e);
		}
}
}