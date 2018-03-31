package com.maple.speind;

import me.speind.SpeindAPI;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.os.Handler;
import android.util.Log;
import android.view.KeyEvent;

public class MediaButtonReceiver extends BroadcastReceiver {
	private static long LONG_PRESS_TIME	= 1000;
	private static long WAIT_PRESS_TIME = 1000;
	private static long mStartClickTime	= 0;
	private static Handler handler= new Handler();
	private static int pressCounter = 0;
	private static Context mContext= null;
	private static Runnable processClicksRunnable= new Runnable() {
		@Override
		public void run() {
			//Log.e("---[MediaButtonReceiver]---", "processClicksRunnable. clicks: "+pressCounter);
			if (mContext!=null) {
				Intent intent=SpeindAPI.createIntent(mContext.getPackageName());
				intent.putExtra(SpeindAPI.SERVICE_CMD, SpeindAPI.SC_MEDIA_BUTTON_PRESS);
				intent.putExtra(SpeindAPI.PARAM_MEDIA_BUTTON_PRESS_COUNT, pressCounter);
				mContext.startService(intent);
			}
			pressCounter=0;
		}
	};
	
	@Override
	public void onReceive(final Context context, Intent intent) {
		String intentAction = intent.getAction();
		if (AudioManager.ACTION_AUDIO_BECOMING_NOISY.equals(intentAction)) {
			SpeindAPI.executeIfSpeindStarted(context, context.getPackageName(), new Runnable(){
				@Override
				public void run() {
					SpeindAPI.pause(context, context.getPackageName());
				}
			}, true);
		} else if (Intent.ACTION_MEDIA_BUTTON.equals(intentAction)) {
			KeyEvent event = (KeyEvent) intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);
			if (event.getAction()==KeyEvent.ACTION_DOWN) {
				if (mStartClickTime==0) mStartClickTime=event.getEventTime();
			} else if (event.getAction()==KeyEvent.ACTION_UP) {
				long eventtime = event.getEventTime();
				switch (event.getKeyCode()) {
				case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
				case KeyEvent.KEYCODE_HEADSETHOOK:					
					//Log.e("---[MediaButtonReceiver]---", "KEYCODE_MEDIA_PLAY_PAUSE|KEYCODE_HEADSETHOOK");
					mContext=context;
					handler.removeCallbacks(processClicksRunnable);					
					if ((eventtime - mStartClickTime) > LONG_PRESS_TIME) {
						SpeindAPI.readCurrentInfopointArticle(context, context.getPackageName());
						break;
					} else {
						pressCounter++;
						handler.postDelayed(processClicksRunnable, WAIT_PRESS_TIME);
					}
					break;
				case 126: //KEYCODE_MEDIA_PLAY
				case 127: //KEYCODE_MEDIA_PAUSE				
					//Log.e("---[MediaButtonReceiver]---", "KEYCODE_MEDIA_PLAY|KEYCODE_MEDIA_PAUSE");
					SpeindAPI.playPause(context, context.getPackageName());
					break;
				case KeyEvent.KEYCODE_MEDIA_STOP:
					SpeindAPI.pause(context, context.getPackageName());
					break;
				case KeyEvent.KEYCODE_MEDIA_PREVIOUS:
					SpeindAPI.prev(context, context.getPackageName());
					break;
				case KeyEvent.KEYCODE_MEDIA_NEXT:
					if ((eventtime - mStartClickTime) > LONG_PRESS_TIME) {
						SpeindAPI.skipNews(context, context.getPackageName());
					} else {
						SpeindAPI.next(context, context.getPackageName());
					}
					break;
				}
				mStartClickTime = 0;
			}
			if (isOrderedBroadcast()) abortBroadcast();
		}
	}
};
