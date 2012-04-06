package net.yihabits.artwork.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class ArtDBOpenHelper extends SQLiteOpenHelper {
	
	public  static final int DATABASE_VERSION = 1;
    public  static final String ART_TABLE_NAME = "art";
	public  static final String IMAGE_URL = "URL";
	public  static final String IMAGE_LOCATOIN = "IMG_LOCATION";
	public  static final String PRE_IMAGE_URL = "PRE_URL";
	public  static final String PRE_IMAGE_LOCATOIN = "PRE_IMG_LOCATION";
	public  static final String CREATED_AT = "CREATED_AT";
	public  static final String ART_NAME = "ART_NAME";
	public  static final String AUTHOR = "AUTHOR";
	public  static final String AUTHOR_DETAILS = "AUTHOR_DETAILS";
	public  static final String YEAR = "YEAR";
	public  static final String DETAILS = "DETAILS";
	public  static final String LOCATION = "LOCATION";
    public  static final String ART_TABLE_CREATE =
                "CREATE TABLE " + ART_TABLE_NAME + " (" +
                "_id integer primary key autoincrement," +
                IMAGE_URL + " TEXT, " +
                IMAGE_LOCATOIN + " TEXT, " +
                PRE_IMAGE_URL + " TEXT, " +
                PRE_IMAGE_LOCATOIN + " TEXT, " +
                CREATED_AT + " TEXT, " +
                ART_NAME + " TEXT, " +
                AUTHOR + " TEXT,  " +
                AUTHOR_DETAILS + " TEXT, " +
                DETAILS + " TEXT, " +
                LOCATION + " TEXT, " +
    YEAR + " TEXT);";
	public static final String DATABASE_NAME = "arts";

    ArtDBOpenHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(ART_TABLE_CREATE);
    }

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		android.util.Log.w("Constants", "Upgrading database, which will destroy all old	data");
				db.execSQL("DROP TABLE IF EXISTS " + ART_TABLE_NAME);
				onCreate(db);
		
	}

}
