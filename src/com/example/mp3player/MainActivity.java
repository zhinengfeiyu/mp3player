package com.example.mp3player;

import java.util.ArrayList;
import java.util.List;

import android.os.Bundle;
import android.provider.MediaStore;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
	private SeekBar seekBar;
	private TextView timeText;
	private Button loopBtn;
	private Button playBtn;
	private Button nextBtn;
	private List<String> songList;
	private ListView listView;
	private String songName;
	private int loopStyle;
	private mReceiver receiver;
	private InitReceiver initReceiver;
	private ProgressReceiver progressReceiver;
	
	private final String[] styles = {"单曲循环","顺序循环","逆序循环","随机播放"};
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		seekBar = (SeekBar) findViewById(R.id.progress);
		seekBar.setMax(10000);
		timeText = (TextView) findViewById(R.id.timeText);
		loopBtn = (Button) findViewById(R.id.loop);
		playBtn = (Button) findViewById(R.id.play);
		nextBtn = (Button) findViewById(R.id.next);
		listView = (ListView) findViewById(R.id.list);
		songList = new ArrayList<String>();
		
		updateList();
		receiver = new mReceiver();
		registerReceiver(receiver,new IntentFilter("com.example.mp3player.changeActivityViews"));
		progressReceiver = new ProgressReceiver();
		registerReceiver(progressReceiver,new IntentFilter("com.example.mp3player.changeProgress"));
		if (songList.size()==0) {
			Toast.makeText(this, "搜索不到歌曲！", Toast.LENGTH_SHORT).show();
			return;
		}
		initReceiver = new InitReceiver();
		registerReceiver(initReceiver,new IntentFilter("com.example.mp3player.initActivity"));
		
		notifyMusicStatusChange(-999,-999);
		
		Intent initIntent = new Intent(this,MusicService.class);
		initIntent.putExtra("begForInit", 0);
		startService(initIntent);
		
		seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
			
			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {
				// TODO Auto-generated method stub
				notifyMusicStatusChange(-1,seekBar.getProgress());
			}
			
			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public void onProgressChanged(SeekBar seekBar, int progress,
					boolean fromUser) {
				
			}
		});
		playBtn.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				notifyMusicStatusChange(-1,-1);
			}
		});
		nextBtn.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				Intent intent = new Intent(MainActivity.this,MusicService.class);
				intent.putExtra("nextSong", 0);
				startService(intent);
			}
		});   
		loopBtn.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				if (loopStyle==4) loopStyle=1;
				else loopStyle++;
				loopBtn.setText(styles[loopStyle-1]);
				Intent intent = new Intent(MainActivity.this,MusicService.class);
				intent.putExtra("loopStyle", loopStyle);
				startService(intent);
			}
		});
		listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
                    long arg3) {
				songName = songList.get(arg2);
				notifyMusicStatusChange(arg2,-1);
			}
		});
	}   
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		unregisterReceiver(receiver);
		unregisterReceiver(progressReceiver);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.exit:
			stopService(new Intent(MainActivity.this,MusicService.class));
			finish();
			return true;
			
	/*	case R.id.add:
			final EditText editText = new EditText(this);
			AlertDialog.Builder builder2 = new AlertDialog.Builder(this);
			builder2.setTitle("请输入新增的歌名")
			       .setView(editText)
			       .setPositiveButton("确认", new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							String text = editText.getText().toString(); 
							if (text.equals("")) return;
							if (songList.contains(text)) {
								Toast.makeText(MainActivity.this, "歌曲已存在", Toast.LENGTH_SHORT).show();
								return;
							}
							DBHelper helper = new DBHelper(MainActivity.this);
							SQLiteDatabase db = helper.getWritableDatabase();
							ContentValues arg = new ContentValues();
							arg.put("SONG_NAME", editText.getText().toString());
							long rowNum = db.insert("SONG_TABLE", null, arg);
							db.close();
							Toast.makeText(MainActivity.this, "第"+rowNum+"首歌曲被添加",Toast.LENGTH_SHORT)
									.show();
							songList.add(text);
							adapter.notifyDataSetChanged();
							Intent intent = new Intent(MainActivity.this,MusicService.class);
							intent.putExtra("updateList", 0);
							startService(intent);
						}
			       })
			       .setNegativeButton("取消", new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							dialog.cancel();
						}
			       })
				   .create()
				   .show();
			return true;
			
		case R.id.delete:
			final List<String> selectedItems = new ArrayList<String>();
			new AlertDialog.Builder(this)
				   .setTitle("请选择要删除的项目")
				   .setMultiChoiceItems(songList.toArray(new String[]{}), null
						   				, new DialogInterface.OnMultiChoiceClickListener() {
					
					@Override
					public void onClick(DialogInterface dialog, int which, boolean isChecked) {
						if (isChecked) 
							selectedItems.add(songList.get(which));
						else
							selectedItems.remove(songList.get(which));
					}
				})
				   .setPositiveButton("确认删除", new DialogInterface.OnClickListener() {
					
					@Override
					public void onClick(DialogInterface dialog, int which) {
						if (selectedItems.size()==0) return;
						songList.removeAll(selectedItems);
						DBHelper helper = new DBHelper(MainActivity.this);
						SQLiteDatabase db = helper.getWritableDatabase();
						db.delete("SONG_TABLE", null, null);
						for (int i = 0;i<songList.size();i++) {
							ContentValues arg = new ContentValues();
							arg.put("SONG_NAME", songList.get(i));
							db.insert("SONG_TABLE", null, arg);
						}
						db.close();
						adapter.notifyDataSetChanged();
						Intent intent = new Intent(MainActivity.this,MusicService.class);
						intent.putExtra("updateList", 0);
						startService(intent);
					}
				})
				   .setNegativeButton("取消", new DialogInterface.OnClickListener() {
					
					@Override
					public void onClick(DialogInterface dialog, int which) {
						dialog.cancel();
					}
				})
				   .create()
				   .show();
			return true;			*/
			
		default:
			return super.onOptionsItemSelected(item);
		}
	}
	
	private void notifyMusicStatusChange(int _songIndex,int _position) {
		Intent intent = new Intent(this,MusicService.class);
		intent.putExtra("_songIndex", _songIndex);
		intent.putExtra("_position", _position);
		startService(intent);
	}
	
	private void updateList()
	{
	/*	DBHelper helper = new DBHelper(this);
		SQLiteDatabase db = helper.getReadableDatabase();
		Cursor cursor = db.query("SONG_TABLE", new String[]{"SONG_NAME"}, null, null, null, null,null);
		try {
			if (cursor.moveToFirst()) {
				do {
					songList.add(cursor.getString(0));
				} while (cursor.moveToNext());
			}
		} finally {
			cursor.close();
		}
		db.close();			*/
		ContentResolver resolver = getContentResolver();
		Cursor cursor = resolver.query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
				, new String[]{"_display_name"}, null, null, null);
		try {
			if (cursor.moveToFirst()) {
				do {
					String sourceName=cursor.getString(cursor.getColumnIndex("_display_name"));
					String songName=sourceName.substring(0, sourceName.length()-4);
					songList.add(songName);
				} while (cursor.moveToNext());
			}
		} finally {
			cursor.close();
		}
		listView.setAdapter(new ArrayAdapter<String>(MainActivity.this,R.layout.unit,songList));
	}
	
	private class InitReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(Context context,Intent intent) {
			songName = intent.getStringExtra("songName");
			setTitle(songName);
			loopStyle = intent.getIntExtra("loopStyle", 0);
			loopBtn.setText(styles[loopStyle-1]);
			unregisterReceiver(initReceiver);
		}
	}

	private class mReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(Context context,Intent intent) {
			if (intent.hasExtra("title")) {
				setTitle(intent.getStringExtra("title"));
				return;
			}
			if (intent.getStringExtra("music_status").equals("playing"))
				playBtn.setText("暂停");
			else
				playBtn.setText("播放");
		}
	}       
	
	private class ProgressReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(Context context,Intent intent) {
			int currentLength = intent.getIntExtra("currentLength", 0);
			int totalLength = intent.getIntExtra("totalLength", 0);
			seekBar.setProgress((int) (seekBar.getMax()*((double) currentLength/totalLength)));
			int minutes = currentLength/1000/60;
			int seconds = currentLength/1000%60;
			if (seconds<10)
				timeText.setText(minutes+":0"+seconds);
			else
				timeText.setText(minutes+":"+seconds);
		}
	}
}
