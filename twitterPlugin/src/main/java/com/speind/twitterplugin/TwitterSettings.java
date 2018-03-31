package com.speind.twitterplugin;

import oauth.signpost.OAuthConsumer;
import oauth.signpost.OAuthProvider;
import oauth.signpost.commonshttp.CommonsHttpOAuthConsumer;
import oauth.signpost.commonshttp.CommonsHttpOAuthProvider;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.AdapterView.OnItemSelectedListener;

public class TwitterSettings extends ActionBarActivity {
	public static OAuthConsumer consumer = new CommonsHttpOAuthConsumer(TwitterConstants.CONSUMER_KEY, TwitterConstants.CONSUMER_SECRET);
	public static OAuthProvider provider = new CommonsHttpOAuthProvider(TwitterConstants.REQUEST_URL, TwitterConstants.ACCESS_URL, TwitterConstants.AUTHORIZE_URL);
	private String profile="";
	
	public boolean authenticated = false;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.twitter_settings);
		
		Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
		if (toolbar!=null) {
	        toolbar.setTitle("");
	        setSupportActionBar(toolbar);
	        toolbar.setNavigationIcon(R.drawable.btn_back);
	        toolbar.setNavigationOnClickListener(new OnClickListener(){
				@Override
				public void onClick(View arg0) {
					TwitterSettings.this.finish();
				}				
			});
		}

		Intent intent=getIntent();
		profile=intent.getStringExtra("profile");
		authenticated=SpeindDataFeed.isAuthenticated(this, profile);

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
			ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, R.array.twitter_refrash_intervals, android.R.layout.simple_spinner_item);
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

						SharedPreferences settings = getSharedPreferences(SpeindDataFeed.PREFS_NAME, 0);
						SharedPreferences.Editor editor = settings.edit();
						editor.putString(profile+"_token", "");
						editor.putString(profile+"_secret", "");
						editor.apply();

						authenticated=false;
						auth_label.setText(R.string.not_authenticated);				
						auth_button.setText(R.string.login);
					} else {
						new OAuthRequestTokenTask(consumer, provider).execute();
					}
				}
				
			});
		}
	}

	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
		if (intent != null && intent.getData() != null) {
			Uri uri = intent.getData();
			if (uri != null && uri.toString().startsWith(TwitterConstants.OAUTH_CALLBACK_URL)) {
				new RetrieveAccessTokenTask(consumer, provider).execute(uri);
			} else {
				final ProgressBar pb=(ProgressBar) findViewById(R.id.progressBar1);
				final LinearLayout bw=(LinearLayout) findViewById(R.id.buttons_wrap);
				if (pb!=null) {
					pb.setVisibility(View.GONE);
				}
				if (bw!=null) {
					bw.setVisibility(View.VISIBLE);
				}
			}
		}
	}	
	
	public class RetrieveAccessTokenTask extends AsyncTask<Uri, Void, Void> {
		final String TAG = getClass().getName(); 

		private OAuthProvider provider;
		private OAuthConsumer consumer;

		public RetrieveAccessTokenTask(OAuthConsumer consumer, OAuthProvider provider) {			
			this.consumer = consumer;
			this.provider = provider;
		}

		@Override
		protected Void doInBackground(Uri...params) {
			final Uri uri = params[0];
			final String oauth_verifier = uri.getQueryParameter(oauth.signpost.OAuth.OAUTH_VERIFIER);
			try {
				provider.retrieveAccessToken(consumer, oauth_verifier);				
				String token = consumer.getToken();
				String secret = consumer.getTokenSecret();
				
				SharedPreferences settings = getSharedPreferences(SpeindDataFeed.PREFS_NAME, 0);
		        SharedPreferences.Editor editor = settings.edit();
		        editor.putString(profile+"_token", token);
		        editor.putString(profile+"_secret", secret);
		        editor.apply();

		    	authenticated=true;
			} catch (Exception e) {
		    	authenticated=false;
				e.printStackTrace();
				//Toast.makeText(TwitterSettings.this, "Request access tokens fail"/*getString(R.string.invalid_account_data)*/, Toast.LENGTH_SHORT).show();
			}			
			return null;
		}
		
		@Override
		protected void onPostExecute(Void p) {
			final ProgressBar pb=(ProgressBar) findViewById(R.id.progressBar1);
			final LinearLayout bw=(LinearLayout) findViewById(R.id.buttons_wrap);
			if (pb!=null) {
				pb.setVisibility(View.GONE);
			}
			if (bw!=null) {
				bw.setVisibility(View.VISIBLE);
			}
			
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
		}
	}
		
	public class OAuthRequestTokenTask extends AsyncTask<Void, Void, Void> {
		final String TAG = getClass().getName();

		private OAuthProvider provider;
		private OAuthConsumer consumer;
		private String url;

		public OAuthRequestTokenTask(OAuthConsumer consumer, OAuthProvider provider) {
			this.consumer = consumer;
			this.provider = provider;
		}

		@Override
		protected void onPreExecute() {
			final ProgressBar pb=(ProgressBar) findViewById(R.id.progressBar1);
			final LinearLayout bw=(LinearLayout) findViewById(R.id.buttons_wrap);
			if (pb!=null) {
				pb.setVisibility(View.VISIBLE);
			}
			if (bw!=null) {
				bw.setVisibility(View.GONE);
			}

		}
		
		@Override
		protected Void doInBackground(Void... params) {
			try {
				url = provider.retrieveRequestToken(consumer, TwitterConstants.OAUTH_CALLBACK_URL);				
			} catch (Exception e) {
				//Toast.makeText(TwitterSettings.this, "Check internet connection"/*getString(R.string.invalid_account_data)*/, Toast.LENGTH_SHORT).show();
				e.printStackTrace();
			}
			return null;
		}

		@Override
		protected void onPostExecute(Void result) {
			if (!TextUtils.isEmpty(url)) {
				new TwitterDialog(TwitterSettings.this, url).show();
			} else {
				final ProgressBar pb=(ProgressBar) findViewById(R.id.progressBar1);
				final LinearLayout bw=(LinearLayout) findViewById(R.id.buttons_wrap);
				if (pb!=null) {
					pb.setVisibility(View.GONE);
				}
				if (bw!=null) {
					bw.setVisibility(View.VISIBLE);
				}
			}
		}
	}
	
	public class TwitterDialog extends Dialog {
		private FrameLayout.LayoutParams FILL = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);

		private LinearLayout webViewContainer;
		private WebView mWebView;
		private String mUrl;

		public TwitterDialog(Context context, String url) {
			super(context, android.R.style.Theme_Translucent_NoTitleBar);
			mUrl = url;
			//getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
			requestWindowFeature(Window.FEATURE_NO_TITLE);
			setContentView(R.layout.twitter_web_dialog);
			webViewContainer = (LinearLayout) findViewById(R.id.twitter_web_view_container);
			mWebView = new WebView(getContext());
			mWebView.setVerticalScrollBarEnabled(false);
			mWebView.setHorizontalScrollBarEnabled(false);
			mWebView.setWebViewClient(new TwitterDialog.TwWebViewClient());
			mWebView.getSettings().setJavaScriptEnabled(true);
			mWebView.loadUrl(mUrl);
			mWebView.setLayoutParams(FILL);
			webViewContainer.addView(mWebView);
		}

		private class TwWebViewClient extends WebViewClient {
			@Override
			public boolean shouldOverrideUrlLoading(WebView view, String url) {
				boolean isDenied = false;
				try {
					Uri uri = Uri.parse(url);
					String param = uri.getQuery();
					String name = param.split("=")[0];
					if ("denied".equals(name)) {
						isDenied = true;
					}
				} catch (Exception e) {
				}

				TwitterDialog.this.dismiss();
				if (!isDenied) {
					getContext().startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
				} 
				return true;
			}

			@Override
			public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
				super.onReceivedError(view, errorCode, description, failingUrl);
			}

			@Override
			public void onPageStarted(WebView view, String url, Bitmap favicon) {
				super.onPageStarted(view, url, favicon);
			}

			@Override
			public void onPageFinished(WebView view, String url) {
				super.onPageFinished(view, url);
				mWebView.setVisibility(View.VISIBLE);
			}
		}
	}
	
	@Override
	public void onDestroy() {
    	Intent intent = new Intent(this, SpeindDataFeed.class);
    	intent.putExtra(SpeindDataFeed.TWITTER_FEED_SETTINGS_COMMAND, SpeindDataFeed.TWITTER_REFRESH);
    	startService(intent);	  
    	super.onDestroy();
	}

}
