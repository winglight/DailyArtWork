package net.yihabits.artwork.db;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.util.Log;

public class ArtDAO {

	private SQLiteDatabase db;
	private final Context context;

	private static ArtDAO instance;
	private ArtDBOpenHelper sdbHelper;
	
	private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

	private ArtDAO(Context c) {
		this.context = c;
		this.sdbHelper = new ArtDBOpenHelper(this.context);
	}

	public void close() {
		db.close();
	}

	public void open() throws SQLiteException {
		try {
			db = sdbHelper.getWritableDatabase();
		} catch (SQLiteException ex) {
			Log.v("Open database exception caught", ex.getMessage());
			db = sdbHelper.getReadableDatabase();
		}
	}

	public static ArtDAO getInstance(Context c) {
		if (instance == null) {
			instance = new ArtDAO(c);
		}
		return instance;
	}

	public Cursor getAllArt() {
		Cursor c = db.query(ArtDBOpenHelper.ART_TABLE_NAME, null, null,
				null, null, null, null);
				
		return c;
	}
	
	public Cursor getAllArt(int page) {
		Cursor c = db.query(ArtDBOpenHelper.ART_TABLE_NAME, null, null, null,
				 null, null, ArtDBOpenHelper.CREATED_AT + " desc", ((page - 1)*10) + ", " + 10);
				
		return c;
	}
	
	public Cursor getArtByImgUrl(String imageUrl) {
		Cursor c = db.query(ArtDBOpenHelper.ART_TABLE_NAME, null, ArtDBOpenHelper.IMAGE_URL + " = ?", new String[]{imageUrl},
				 null, null, null);
				
		return c;
	}

	public int getMaxId() {
		return 0;
	}

	public long insert(ArtModel am) {

		try{
			ContentValues newArtValue = new ContentValues();
			newArtValue.put(ArtDBOpenHelper.IMAGE_URL, am.getImageUrl());
			newArtValue.put(ArtDBOpenHelper.IMAGE_LOCATOIN, am.getImageLocation());
			newArtValue.put(ArtDBOpenHelper.PRE_IMAGE_URL, am.getPreImageUrl());
			newArtValue.put(ArtDBOpenHelper.PRE_IMAGE_LOCATOIN, am.getPreImageLocation());
			newArtValue.put(ArtDBOpenHelper.CREATED_AT, dateFormat.format(new Date()));
			newArtValue.put(ArtDBOpenHelper.ART_NAME, am.getName());
			newArtValue.put(ArtDBOpenHelper.AUTHOR_DETAILS, am.getAuthorDetails());
			newArtValue.put(ArtDBOpenHelper.AUTHOR, am.getAuthor());
			newArtValue.put(ArtDBOpenHelper.YEAR, am.getYear());
			newArtValue.put(ArtDBOpenHelper.DETAILS, am.getDetails());
			newArtValue.put(ArtDBOpenHelper.LOCATION, am.getLocation());
			return db.insert(ArtDBOpenHelper.ART_TABLE_NAME, null, newArtValue);
			} catch(SQLiteException ex) {
				Log.v("Insert into database exception caught",
						ex.getMessage());
				return -1;
			}
	}
	
	public long update(ArtModel am) {

		try{
			ContentValues newArtValue = new ContentValues();
//			newServerValue.put("_id", sm.getId());
			newArtValue.put(ArtDBOpenHelper.IMAGE_URL, am.getImageUrl());
			newArtValue.put(ArtDBOpenHelper.IMAGE_LOCATOIN, am.getImageLocation());
			newArtValue.put(ArtDBOpenHelper.PRE_IMAGE_URL, am.getPreImageUrl());
			newArtValue.put(ArtDBOpenHelper.PRE_IMAGE_LOCATOIN, am.getPreImageLocation());
//			newArtValue.put(ArtDBOpenHelper.CREATED_AT, dateFormat.format(am.getCreatedAt()));
			newArtValue.put(ArtDBOpenHelper.ART_NAME, am.getName());
			newArtValue.put(ArtDBOpenHelper.AUTHOR_DETAILS, am.getAuthorDetails());
			newArtValue.put(ArtDBOpenHelper.AUTHOR, am.getAuthor());
			newArtValue.put(ArtDBOpenHelper.YEAR, am.getYear());
			newArtValue.put(ArtDBOpenHelper.DETAILS, am.getDetails());
			newArtValue.put(ArtDBOpenHelper.LOCATION, am.getLocation());
			return db.update(ArtDBOpenHelper.ART_TABLE_NAME, newArtValue, "_id=" + am.getId(), null);
			} catch(SQLiteException ex) {
				Log.v("update database exception caught",
						ex.getMessage());
				return -1;
			}
	}
	
	public long delete(long id) {
		try{
			return db.delete(ArtDBOpenHelper.ART_TABLE_NAME, "_id=" + id, null);
			} catch(SQLiteException ex) {
				Log.v("delete database exception caught",
						ex.getMessage());
				return -1;
			}
	}
}
