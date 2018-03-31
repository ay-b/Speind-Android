package com.maple.speind;

import me.speind.SpeindAPI;

import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.support.v4.util.LongSparseArray;

public class DownloadCompleteReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		String action = intent.getAction();
		if (DownloadManager.ACTION_NOTIFICATION_CLICKED.equals(action)) {	
			Intent speindIntent=SpeindAPI.createIntent(context.getPackageName());
			speindIntent.putExtra(SpeindAPI.SERVICE_CMD, 1001);
			context.startService(speindIntent);
		} else if (DownloadManager.ACTION_DOWNLOAD_COMPLETE.equals(action)) {
			LongSparseArray<String> downloadingVoices = SpeindService.loadDownloadingVoices(context);
			long id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, 0L);
			if (downloadingVoices.indexOfKey(id)!=-1) {
				Intent speindIntent=SpeindAPI.createIntent(context.getPackageName());
				speindIntent.putExtra(SpeindAPI.SERVICE_CMD, 1000);
				speindIntent.putExtra("param_download_id", id);
				context.startService(speindIntent);
			}
		}
		
	}
	
}
