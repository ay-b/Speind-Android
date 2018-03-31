package com.speind.vkplugin;

import com.vk.sdk.VKAccessToken;
import com.vk.sdk.VKSdk;
import com.vk.sdk.VKUIHelper;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.AdapterView.OnItemSelectedListener;

public class VKSettings extends ActionBarActivity {
	private BroadcastReceiver loginStateUpdatedReceiver;
	private String profile="";	
	public boolean authenticated = false;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.vk_settings);
		
		Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
		if (toolbar!=null) {
	        toolbar.setTitle("");
	        setSupportActionBar(toolbar);
	        toolbar.setNavigationIcon(R.drawable.btn_back);
	        toolbar.setNavigationOnClickListener(new OnClickListener(){
				@Override
				public void onClick(View arg0) {
					VKSettings.this.finish();
				}				
			});
		}
		
		Intent intent=getIntent();
		profile=intent.getStringExtra("profile");
		authenticated=VKSdk.isLoggedIn();

		final SharedPreferences settings = getSharedPreferences(SpeindDataFeed.PREFS_NAME, 0);
		
		final ProgressBar pb=(ProgressBar) findViewById(R.id.progressBar1);
		final LinearLayout bw=(LinearLayout) findViewById(R.id.buttons_wrap);
		if (pb!=null) {
			pb.setVisibility(View.GONE);
		}
		if (bw!=null) {
			bw.setVisibility(View.VISIBLE);
		}
		
		Spinner refrash_rate=(Spinner) findViewById(R.id.refrash_rate);
		if (refrash_rate!=null) {
			final int refreshInterval=settings.getInt(profile+"_refreshInterval", 15*60);
			ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, R.array.vk_refrash_intervals, android.R.layout.simple_spinner_item);
	        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
	        refrash_rate.setAdapter(adapter);
			switch (refreshInterval) {
			case 5*60:
				refrash_rate.setSelection(0);
				break;
			case 15*60:
				refrash_rate.setSelection(1);
				break;
			case 30*60:
				refrash_rate.setSelection(2);
				break;
			case 60*60:
				refrash_rate.setSelection(3);
				break;
			case 5*60*60:
				refrash_rate.setSelection(4);
				break;
			default:
				refrash_rate.setSelection(1);
				break;
			}
	        refrash_rate.setOnItemSelectedListener(new OnItemSelectedListener(){

				@Override
				public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
					int refreshInterval_new=15*60;
					switch (arg2) {
					case 0:
						refreshInterval_new=5*60;
						break;
					case 1:
						refreshInterval_new=15*60;
						break;
					case 2:
						refreshInterval_new=30*60;
						break;
					case 3:
						refreshInterval_new=60*60;
						break;
					case 4:
						refreshInterval_new=5*60*60;
						break;
					}
					if (refreshInterval!=refreshInterval_new) {						
						SharedPreferences.Editor editor = settings.edit();
				        editor.putInt(profile+"_refreshInterval", refreshInterval_new);
				        editor.commit();					        				    	
					}
				}

				@Override
				public void onNothingSelected(AdapterView<?> arg0) {}	        	
	        });
		}
		
		final TextView auth_label=(TextView) this.findViewById(R.id.auth_label);
		final Button auth_button=(Button) this.findViewById(R.id.auth_button);
		
		if (auth_label!=null) {
			if (authenticated)
				auth_label.setText(R.string.authenticated);
			else
				auth_label.setText(R.string.not_authenticated);				
		}
		if (auth_button!=null) {
			if (authenticated)
				auth_button.setText(R.string.logout);
			else
				auth_button.setText(R.string.login);
			auth_button.setOnClickListener(new OnClickListener(){
				@Override
				public void onClick(View arg0) {
					if (authenticated) {
						VKSdk.logout();
						VKAccessToken.removeTokenAtKey(VKSettings.this, SpeindDataFeed.sTokenKey+"_"+profile);
						authenticated=false;
						auth_label.setText(R.string.not_authenticated);				
						auth_button.setText(R.string.login);
					} else {
						//VKSdk.authorize(SpeindDataFeed.sMyScope, revoke, forceOAuth);
						VKSdk.authorize(SpeindDataFeed.sMyScope, true, false);
					}
				}
				
			});
		}
		loginStateUpdatedReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				 int cmd= intent.getIntExtra("cmd",0);
				 if (cmd==0) {
						authenticated=VKSdk.isLoggedIn();
						final TextView auth_label=(TextView) findViewById(R.id.auth_label);
						final Button auth_button=(Button) findViewById(R.id.auth_button);
						
						if (auth_label!=null) {
							if (authenticated)
								auth_label.setText(R.string.authenticated);
							else
								auth_label.setText(R.string.not_authenticated);				
						}
						if (auth_button!=null) {
							if (authenticated)
								auth_button.setText(R.string.logout);
							else
								auth_button.setText(R.string.login);
						}
						if (!authenticated) {
							// TODO display error 
						}
				 }
			}
		};
		IntentFilter ifilter = new IntentFilter(SpeindDataFeed.BROADCAST_ACTION);
		registerReceiver(loginStateUpdatedReceiver, ifilter);
	}

	@Override
	public void onDestroy() {
		Intent intent = new Intent(this, SpeindDataFeed.class);
		intent.putExtra(SpeindDataFeed.VK_FEED_SETTINGS_COMMAND, SpeindDataFeed.VK_REFRESH);
		startService(intent);
		super.onDestroy(); 
		VKUIHelper.onDestroy(this); 
	}
	
	@Override 
	protected void onResume() { 
		super.onResume(); 
		VKUIHelper.onResume(this); 
	} 

	@Override 
	protected void onActivityResult(int requestCode, int resultCode, Intent data) { 
		super.onActivityResult(requestCode, resultCode, data); 
		VKUIHelper.onActivityResult(this, requestCode, resultCode, data); 
	} 
	
}
