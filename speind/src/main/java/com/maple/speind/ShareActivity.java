package com.maple.speind;

import java.net.MalformedURLException;
import java.net.URL;

import me.speind.SpeindAPI;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.Gravity;
import android.widget.TextView;
import android.widget.Toast;

//import ru.lifenews.speind.R;

public class ShareActivity extends Activity {
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		Intent intent = getIntent();
	    String action = intent.getAction();
	    String type = intent.getType();
	    
	    if (Intent.ACTION_SEND.equals(action) && type != null) {
	        if ("text/plain".equals(type)) {
	            handleSendText(intent);
	        }
	    }
	    
	    (new Handler()).postDelayed(new Runnable(){
			@Override
			public void run() {
				finish();
			}
		}, 1000);
	    
	}		
	
	private void handleSendText(Intent intent) {
	    String sharedText = intent.getStringExtra(Intent.EXTRA_TEXT);
	    if (sharedText != null&&sharedText.length()>0) {
	    	showMessage(getString(R.string.share_text_processing));
	        Intent serviceIntent=SpeindAPI.createIntent(getPackageName());
	        serviceIntent.putExtra(SpeindAPI.SERVICE_CMD, 1002);
	        serviceIntent.putExtra("textData", sharedText);
	        startService(serviceIntent);
	    }
	}
	
	private void showMessage(String msg) {
		Toast toast = Toast.makeText(this, msg, Toast.LENGTH_LONG);
		toast.setGravity(Gravity.CENTER_VERTICAL|Gravity.CENTER_HORIZONTAL, 0, 20);
		TextView v = (TextView) toast.getView().findViewById(android.R.id.message);
		if( v != null) v.setGravity(Gravity.CENTER);
		toast.show();
    }
}
