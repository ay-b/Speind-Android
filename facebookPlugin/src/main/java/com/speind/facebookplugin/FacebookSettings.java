package com.speind.facebookplugin;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
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

import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.FacebookSdk;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;

import java.util.Arrays;

public class FacebookSettings  extends ActionBarActivity {
	private String profile="";
	public boolean authenticated = false;

	CallbackManager callbackManager;
	
	private FacebookCallback<LoginResult> callback = new FacebookCallback<LoginResult>() {
        @Override
        public void onSuccess(LoginResult loginResult) {
            AccessToken accessToken = loginResult.getAccessToken();
            //accessToken.
            updateUI();
        }

        @Override
        public void onCancel() {
            updateUI();
        }

        @Override
        public void onError(FacebookException e) {
            updateUI();
        }
    };
    
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.facebook_settings);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
		if (toolbar!=null) {
	        toolbar.setTitle("");
	        setSupportActionBar(toolbar);
	        toolbar.setNavigationIcon(R.drawable.btn_back);
	        toolbar.setNavigationOnClickListener(new OnClickListener(){
				@Override
				public void onClick(View arg0) {
					FacebookSettings.this.finish();
				}				
			});
		}
		
        Intent intent=getIntent();
		profile=intent.getStringExtra("profile");
		
        final SharedPreferences settings = getSharedPreferences(SpeindDataFeed.PREFS_NAME, 0);
		Spinner refrash_rate=(Spinner) findViewById(R.id.refrash_rate);
		if (refrash_rate!=null) {
			final int refreshInterval=settings.getInt(profile+"_refreshInterval", 15*60);
			ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, R.array.facebook_refrash_intervals, android.R.layout.simple_spinner_item);
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
        
		final ProgressBar pb=(ProgressBar) findViewById(R.id.progressBar1);
		final LinearLayout bw=(LinearLayout) findViewById(R.id.buttons_wrap);
		if (pb!=null) {
			pb.setVisibility(View.GONE);
		}
		if (bw!=null) {
			bw.setVisibility(View.VISIBLE);
		}

        if (!FacebookSdk.isInitialized()) FacebookSdk.sdkInitialize(getApplicationContext());
		callbackManager = CallbackManager.Factory.create();
        LoginManager.getInstance().registerCallback(callbackManager, callback);

        authenticated = AccessToken.getCurrentAccessToken() != null;

		final Button auth_button=(Button) this.findViewById(R.id.auth_button);
		if (auth_button!=null) {
			if (authenticated)
				auth_button.setText(R.string.logout);
			else
				auth_button.setText(R.string.login);
			auth_button.setOnClickListener(new OnClickListener(){
				@Override
				public void onClick(View arg0) {
					if (!authenticated) {
                        //LoginManager.getInstance().logInWithPublishPermissions(FacebookSettings.this, Arrays.asList("publish_actions"));
                        LoginManager.getInstance().logInWithReadPermissions(FacebookSettings.this, Arrays.asList("read_stream"));
					} else {
                        LoginManager.getInstance().logOut();
                        updateUI();
					}
				}
				
			});
		}
        updateUI();
	}
	
	
	private void updateUI() {
        authenticated = AccessToken.getCurrentAccessToken() != null;

        final TextView auth_label=(TextView) this.findViewById(R.id.auth_label);
        final Button auth_button=(Button) this.findViewById(R.id.auth_button);
        if (auth_button!=null) {
        	if (authenticated)
				auth_button.setText(R.string.logout);
			else
				auth_button.setText(R.string.login);
        }
		if (auth_label!=null) {
	        if (authenticated) {
				auth_label.setText(R.string.authenticated);
	        } else {
				auth_label.setText(R.string.not_authenticated);				
	        }
		}        
    } 
	
	@Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
		callbackManager.onActivityResult(requestCode, resultCode, data);
	}

	public void onDestroy() {
		Intent intent = new Intent(this, SpeindDataFeed.class);
		intent.putExtra(SpeindDataFeed.FACEBOOK_FEED_SETTINGS_COMMAND, SpeindDataFeed.FACEBOOK_REFRESH);
		startService(intent);
		super.onDestroy();
	}
}
