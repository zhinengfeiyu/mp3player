package com.example.mp3player;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DBHelper extends SQLiteOpenHelper {
	private static final int DATABASE_VERSION = 1;
	private static final String DATABASE_NAME = "songs.db";
	private static final String CREATE_DDL = "CREATE TABLE SONG_TABLE (ID INTEGER PRIMARY KEY," +
												"SONG_NAME TEXT);";
	private static final String DELETE_DDL = "DROP TABLE IF EXISTS SONG_TABLE;";
	
	public DBHelper(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}

	@Override
	public void onCreate(SQLiteDatabase arg) {
		arg.execSQL(CREATE_DDL);
	}

	@Override
	public void onUpgrade(SQLiteDatabase arg0, int arg1, int arg2) {
		arg0.execSQL(DELETE_DDL);
		arg0.execSQL(CREATE_DDL);
	}

}
