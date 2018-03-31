package com.maple.speind;

import me.speind.SpeindAPI;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

public class PackageChangeReceiver extends BroadcastReceiver {
	private final static String TAG	= "[PackageChangeReceiver]";
    @Override
    public void onReceive(final Context context, Intent intent) {
		Uri data = intent.getData();
		Log.e(TAG, "Package: " + "package:"+context.getPackageName()+" "+data.toString());
		if (data!=null&&data.toString().equals("package:"+context.getPackageName())) return;
		Log.e(TAG, "Action: " + intent.getAction());
		Log.e(TAG, "The DATA: " + data);
		SpeindAPI.executeIfSpeindStarted(context, context.getPackageName(), new Runnable(){
			@Override
			public void run() {
				Intent serviceIntent = new Intent();
				serviceIntent.setClassName(context.getPackageName(), SpeindAPI.SERVICE_NAME);
				serviceIntent.putExtra(SpeindAPI.SERVICE_CMD, SpeindAPI.SC_PACKAGE_CHANGED);
				context.startService(serviceIntent);
			}
		}, true);
    }
}
