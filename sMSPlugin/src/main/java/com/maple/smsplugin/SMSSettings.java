package com.maple.smsplugin;

import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.RelativeLayout;
import android.widget.ToggleButton;
import android.content.Intent;
import android.content.SharedPreferences;

public class SMSSettings extends ActionBarActivity {
	private String profile="";
	private boolean ar=false;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.sms_settings);
		
		Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
		if (toolbar!=null) {
	        toolbar.setTitle("");
	        setSupportActionBar(toolbar);
	        toolbar.setNavigationIcon(R.drawable.btn_back);
	        toolbar.setNavigationOnClickListener(new OnClickListener(){
				@Override
				public void onClick(View arg0) {
					SMSSettings.this.finish();
				}				
			});
		}

		Intent intent=getIntent();
		profile=intent.getStringExtra("profile");

		final SharedPreferences settings = getSharedPreferences(SpeindDataFeed.PREFS_NAME, 0);
		
		final ToggleButton auto_read = (ToggleButton) findViewById(R.id.auto_read);
		RelativeLayout auto_read_wrap = (RelativeLayout) findViewById(R.id.auto_read_wrap);
		if (auto_read!=null) {
			ar=settings.getBoolean(profile+"_auto_read", false);
			auto_read.setChecked(ar);
			if (auto_read_wrap!=null) {
				auto_read_wrap.setOnClickListener(new OnClickListener(){
					@Override
					public void onClick(View v) {
						auto_read.setChecked(!auto_read.isChecked());
					}});
			}
			auto_read.setOnCheckedChangeListener(new OnCheckedChangeListener(){
				@Override
				public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
					if (isChecked!=ar) {
						ar=isChecked;
						SharedPreferences.Editor editor = settings.edit();
				        editor.putBoolean(profile+"_auto_read", isChecked);
				        editor.commit();
				    	Intent intent = new Intent(SMSSettings.this, SpeindDataFeed.class);
				    	intent.putExtra(SpeindDataFeed.SMS_FEED_SETTINGS_COMMAND, SpeindDataFeed.AUTO_READ_CHANGED);
				    	startService(intent);	 
					}
				}				
			});
		}
		
	}

}
