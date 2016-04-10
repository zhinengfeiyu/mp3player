package com.example.mp3player;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.media.MediaPlayer;
import android.os.IBinder;
import android.provider.MediaStore;
import android.widget.Toast;


public class MusicService extends Service {
	
	private MediaPlayer mp;
	private String songPath;
	private int loopStyle;
	private List<String> songList;
	private Notification notification;
	private PendingIntent pendingIntent;
	private Timer timer;
	private TimerTask timerTask;
	
	private final String PREF_FILENAME = "play_info";
	private final int SINGLE_PLAY = 1;       //单曲循环
	private final int DOWN_PLAY = 2;	     //顺序播放
	private final int UP_PLAY = 3;			 //逆序播放
	private final int RANDOM_PLAY = 4;       //随机播放
	
	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}
	
	@Override
	public void onCreate() {
		super.onCreate();
		songList = new ArrayList<String>();
		updateList();
		mp = new MediaPlayer();
		SharedPreferences pref = getSharedPreferences(PREF_FILENAME,0);
		
		songPath = pref.getString("songPath", "");
		int position = pref.getInt("position", 0);
		if (songPath.equals("")) {
			songPath = songList.get(0);
			position = 0;
		}
		mp.reset();
		try {
			mp.setDataSource(songPath);
			mp.prepare();
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (SecurityException e) {
			e.printStackTrace();
		} catch (IllegalStateException e) {
			e.printStackTrace();
		} catch (IOException e) {
			Toast.makeText(this, "无法找到音乐文件", Toast.LENGTH_SHORT).show();
			e.printStackTrace();
		}
		mp.seekTo(position);
		loopStyle = pref.getInt("loopStyle", 1);
			
		mp.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
			
			@Override
			public void onCompletion(MediaPlayer mp) {
				chooseNewSong();
				playNewSong();
			}
		});
		
		notification = new Notification();
		String showText = songPath.substring(songPath.lastIndexOf("/")+1,songPath.lastIndexOf("."));
		notification.icon = R.drawable.ic_launcher;
		notification.tickerText = showText;
		notification.when = System.currentTimeMillis();
		Intent intent = new Intent(this,MainActivity.class);
		pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);
		notification.setLatestEventInfo(this, "音乐播放器", showText, pendingIntent);
		startForeground(1,notification);
		
		Toast.makeText(this, "Service onCreate!  loopStyle:"+loopStyle, Toast.LENGTH_SHORT).show();
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Toast.makeText(this, "onStartCommand!    "+startId+"\n"+(mp==null), Toast.LENGTH_SHORT).show();
		
		if (intent.hasExtra("begForInit")) {
			initActivity();
			return super.onStartCommand(intent, flags, startId);
		}
//		if (intent.hasExtra("updateList")) {
//			updateList();
//			return super.onStartCommand(intent, flags, startId);
//		}
		if (intent.hasExtra("loopStyle")) {
			loopStyle = intent.getIntExtra("loopStyle", 0);
			Toast.makeText(this,"loopStyle change to: "+loopStyle, Toast.LENGTH_SHORT).show();
			return super.onStartCommand(intent, flags, startId);
		}
		if (intent.hasExtra("nextSong")) {
			chooseNewSong();
			boolean playing = mp.isPlaying();
			mp.reset();
			try {
				mp.setDataSource(songPath);
				mp.prepare();
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
			} catch (SecurityException e) {
				e.printStackTrace();
			} catch (IllegalStateException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
			changeNotification();
			if (playing) mp.start();
			return super.onStartCommand(intent, flags, startId);
		}
		
		int _songIndex = intent.getIntExtra("_songIndex",-1);
		int _position = intent.getIntExtra("_position", -1);
		//未传入歌名和位置，即原地播放或暂停
		if (_songIndex==-1 && _position==-1) {
			if (mp.isPlaying()) {
				mp.pause();
				stopTimer();
			}
			else {
				mp.start();
				startTimer();
			}
			notifyButtonTextChange();
		}
		//仅传入歌名，即指定一首歌从头播放
		else if (_songIndex!=-1 && _position==-1) {
			songPath = songList.get(_songIndex);
			notifyTitleChange();
			playNewSong();
			startTimer();
			notifyButtonTextChange();
		}
		
		//仅传入位置，即从原歌曲的指定位置继续播放
		else if (_songIndex==-1 && _position!=-1) {
			mp.seekTo((int) (mp.getDuration()*(_position/10000.0)));
		}
		//传入歌名和位置，即在指定歌曲的指定位置暂停，仅用于刚打开软件
		else if (_songIndex!=-1 && _position!=-1) {
			notifyButtonTextChange();
		}	
		
		return super.onStartCommand(intent, flags, startId);
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		savefile();
		mp.stop();
		stopTimer();
		mp.release();
		stopForeground(true);
		Toast.makeText(this, "Service onDestroy!", Toast.LENGTH_SHORT).show();
	}
	
	private void notifyTitleChange() {
		Intent intent = new Intent("com.example.mp3player.changeActivityViews");
		String showText = songPath.substring(songPath.lastIndexOf("/")+1,songPath.lastIndexOf("."));
		intent.putExtra("title",showText);
		sendBroadcast(intent);
	}
	
	private void notifyButtonTextChange() {
		Intent intent = new Intent("com.example.mp3player.changeActivityViews");
		if (mp.isPlaying()) 
			intent.putExtra("music_status", "playing");
		else 
			intent.putExtra("music_status", "pausing");
		sendBroadcast(intent);
	}
	
	private void notifyProgressChange() {
		Intent intent = new Intent("com.example.mp3player.changeProgress");
		intent.putExtra("currentLength", mp.getCurrentPosition());
		intent.putExtra("totalLength", mp.getDuration());
		sendBroadcast(intent);
	}
	
	private void initActivity() {
		Intent intent = new Intent("com.example.mp3player.initActivity");
		String songName = songPath.substring(songPath.lastIndexOf("/")+1,songPath.lastIndexOf("."));
		intent.putExtra("songName", songName);
		intent.putExtra("loopStyle", loopStyle);
		sendBroadcast(intent);
		notifyProgressChange();
	}
	
	private void chooseNewSong() {
		
		int currentIndex = songList.indexOf(songPath);
		switch(loopStyle) {
		case SINGLE_PLAY:
			songPath = songList.get(currentIndex);
			break;
		case DOWN_PLAY:
			if (currentIndex < songList.size()-1) 
				songPath = songList.get(currentIndex+1);
			else
				songPath = songList.get(0);
			break;
		case UP_PLAY:
			if (currentIndex > 0)
				songPath = songList.get(currentIndex-1);
			else
				songPath = songList.get(songList.size()-1);
			break;
		case RANDOM_PLAY:
			int index = (int) Math.floor(Math.random()*songList.size());
			songPath = songList.get(index);
		}
		
		notifyTitleChange();
	}
	
	private void playNewSong() {
		mp.reset();
		try {
			mp.setDataSource(songPath);
			mp.prepare();
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (SecurityException e) {
			e.printStackTrace();
		} catch (IllegalStateException e) {
			e.printStackTrace();
		} catch (IOException e) {
			Toast.makeText(this, "音乐文件无法打开！", Toast.LENGTH_SHORT).show();
			e.printStackTrace();
		}
		changeNotification();
		mp.start();
	}
	
	private void changeNotification() {
		stopForeground(true);
		String showText = songPath.substring(songPath.lastIndexOf("/")+1,songPath.lastIndexOf("."));
		notification.tickerText = showText;
		notification.setLatestEventInfo(this, "音乐播放器", showText, pendingIntent);
		startForeground(1,notification);
	}
	
	private void startTimer() {
		if (timer != null) return;
		timerTask = new TimerTask() {
			@Override
			public void run() {
				notifyProgressChange();
			}
		};
		timer = new Timer();
		timer.schedule(timerTask, 1000, 1000);
	}
	
	private void stopTimer() {
		if (timer == null) return;
		timerTask.cancel();
		timerTask = null;
		timer.cancel();
		timer = null;
	}
	
	private void updateList()
	{
		ContentResolver resolver = getContentResolver();
		Cursor cursor = resolver.query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
				, new String[]{"_data"}, null, null, null);
		try {
			if (cursor.moveToFirst()) {
				do {
					String sourceName=cursor.getString(cursor.getColumnIndex("_data"));
					songList.add(sourceName);
				} while (cursor.moveToNext());
			}
		} finally {
			cursor.close();
		}
	}
	
	private void savefile()
	{
		SharedPreferences pref = getSharedPreferences(PREF_FILENAME,0);
		SharedPreferences.Editor editor = pref.edit();
		editor.putString("songPath", songPath);
		editor.putInt("position", mp.getCurrentPosition());
		editor.putInt("loopStyle", loopStyle);
		editor.commit();
	}
}
