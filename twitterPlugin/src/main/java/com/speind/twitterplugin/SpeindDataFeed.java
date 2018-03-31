package com.speind.twitterplugin;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import twitter4j.MediaEntity;
import twitter4j.Paging;
import twitter4j.Status;
import twitter4j.StatusUpdate;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.URLEntity;
import twitter4j.auth.AccessToken;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.IBinder;
import android.text.Html;
import android.util.Log;

import me.speind.SpeindAPI;
import me.speind.SpeindAPI.InfoPoint;

public class SpeindDataFeed extends SpeindAPI.DatafeedService {
	public static final String PREFS_NAME = "twitterReceiverConfig";
	
	public static final String TWITTER_FEED_SETTINGS_COMMAND = "twitter_settings_command";
	public static final int TWITTER_REFRESH = 0;
	
	private String curProfile="profile";
    private Map<String, String> profiles = new HashMap<>();
 
	private final Handler handler = new Handler();
	private final Runnable updateTweetsRunnable = new Runnable() { public void run() { refressTweets(); } };

    private Map<String, Boolean> services = new HashMap<>();

	@Override
	public SpeindAPI.DataFeedSettingsInfo onInit(String service_package) {
        services.put(service_package, true);
		SpeindAPI.DataFeedSettingsInfo info = new SpeindAPI.DataFeedSettingsInfo(this, getString(R.string.twitterreceiver), BitmapFactory.decodeResource(getResources(), R.drawable.ic_launcher), true);
		return info;
	}

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
    	if (intent!=null) { 
			int cmd=intent.getIntExtra(TWITTER_FEED_SETTINGS_COMMAND, -1);
			switch (cmd) {
			case TWITTER_REFRESH:
                if (isAuthenticated(this, curProfile)) {
                    Map<String, Boolean> services_loc = new HashMap<>(services);
                    Collection<String> service_packages = services_loc.keySet();
                    for (String service_package : service_packages) {
                        if (speindDFData.setNeedAuthorization(this, service_package, false)) {
                            speindDFData.setState(this, service_package, SpeindAPI.DataFeedSettingsInfo.DATAFEED_STATE_READY, false);
                            services.put(service_package, false);
                        }
                    }
                } else {
                    Map<String, Boolean> services_loc = new HashMap<>(services);
                    Collection<String> service_packages = services_loc.keySet();
                    for (String service_package : service_packages) {
                        speindDFData.setNeedAuthorization(this, service_package, true);
                        speindDFData.setState(this, service_package, SpeindAPI.DataFeedSettingsInfo.DATAFEED_STATE_SUSPENDED, false);
                        services.put(service_package, true);
                    }
                }
				SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
				SharedPreferences.Editor editor = settings.edit();
				editor.putLong("lastRefreshTime", 0);
				editor.commit();
				refressTweets();		
				break;		
			}
		}
        return super.onStartCommand(intent, flags, startId);
    }

	@Override
	public void onInfoPointDetails(String service_package, InfoPoint arg0) {
		// TODO Auto-generated method stub		
	}

	@Override
	public void onLike(String service_package, InfoPoint arg0) {
		// TODO Auto-generated method stub		
	}

	@Override
	public void onSetProfile(String service_package, String profile) {
        profiles.put(service_package, profile);
	}

	@Override
	public void onStop(String service_package) {
        services.remove(service_package);
        profiles.remove(service_package);
        if (services.size()==0) {
            handler.removeCallbacks(updateTweetsRunnable);
            this.stopSelf();
        } else if (isSuspended()) {
            handler.removeCallbacks(updateTweetsRunnable);
        }
	}

	@Override
	public void onShowSettings(String service_package) {
		Intent intent=new Intent(SpeindDataFeed.this, TwitterSettings.class);
		intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		intent.putExtra("profile", curProfile);
		getApplication().startActivity(intent);
	}

	@Override
	public IBinder onBind(Intent arg0) {
		return null;
	}

	public Twitter PrepareAuthenticated() {
		SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
		String token = settings.getString(curProfile+"_token", "");
		String secret = settings.getString(curProfile+"_secret", "");
		try {
			AccessToken a = new AccessToken(token, secret);
			Twitter twitter = new TwitterFactory().getInstance();
			twitter.setOAuthConsumer(TwitterConstants.CONSUMER_KEY, TwitterConstants.CONSUMER_SECRET);
			twitter.setOAuthAccessToken(a);
			return twitter;
		} catch (IllegalArgumentException e) {
			return null;
		}		
	}
	
	public static final  boolean isAuthenticated(Context context, String profile) {
		SharedPreferences settings = context.getSharedPreferences(PREFS_NAME, 0);
		String token = settings.getString(profile+"_token", "");
		String secret = settings.getString(profile+"_secret", "");		
		if (token == null || token.length() == 0 || secret == null || secret.length() == 0) return false;
		return true;
	}		 	
 	
 	private String prepareText(String fulltext) {
 		String text="<p>";
 		fulltext=fulltext.replace(".", ". ");
 		fulltext=fulltext.replace("“", getApplicationContext().getString(R.string.quote) + " ");
 		fulltext=fulltext.replace("«", " «");
 		fulltext=fulltext.replace("»", "» ");
 		fulltext=fulltext.replace("  ", " ");
        fulltext=fulltext.replace("http", " http");
 		fulltext=fulltext.replace("://t. co/", "://t.co/");
 		int cnt=fulltext.length();
 		StringBuffer word=new StringBuffer();
 		for (int i=0;i<cnt;i++) {
			char ch = fulltext.charAt(i);
			if ((ch==' ')||(ch=='\n')||(i==cnt-1)) {
				word.append(ch);
				if (word.charAt(0)=='@') {
					word.deleteCharAt(0);
					text+=word.toString();	 
				} else if (word.charAt(0)=='#') {					
				} else {
					if (SpeindAPI.isURL(word.toString())) {
                        Log.e("[---Twitter---]", "Link: "+word.toString());
						text+="</p><p>"+getApplicationContext().getString(R.string.link)+"</p><p>";
					} else {
						text+=word.toString();
					}						 									
				}
				word=new StringBuffer();
			} else {
				word.append(ch);
			}
		}		
 		//Log.d("[---prepare tweet---]", "<-"+text);
 		return ""+Html.fromHtml(text+"</p>");
 	}
 	
	private void refressTweets() {
		handler.removeCallbacks(updateTweetsRunnable);
		if (!(services.size()==0||isSuspended())) {
		
			SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
			int refreshInterval = settings.getInt(curProfile+"_refreshInterval", 15*60);
			long lastRefreshTime = settings.getLong("lastRefreshTime", 0);
			long delta=refreshInterval*1000-(new Date()).getTime()-lastRefreshTime;
			
			if (delta<5000) {
				SharedPreferences.Editor editor = settings.edit();
				editor.putLong("lastRefreshTime", (new Date()).getTime());
				editor.commit();
				
				new Thread(new Runnable() {
			        public void run() {
			        	if (services.size()==0||isSuspended())  return;
		        		int aType = SpeindAPI.getConnectionStatus(SpeindDataFeed.this);
		    			if (aType!=-1) {    
		    				if (isAuthenticated(SpeindDataFeed.this, curProfile)) {
		    					Twitter twitter=PrepareAuthenticated();
		    					if (twitter!=null) {

                                    setProcessingState(true);

									try {
										//result = twitter.getHomeTimeline();
										Paging p=new Paging(1, 200); 
			    	    				List<Status> tweets = twitter.getHomeTimeline(p);
										//Log.d("[---twittsresp---]", "!"+tweets.size());								
			    	    				//List<JSONObject> jsonList = convertTimelineToJson(tweets);
										
			    	    				for (int i=tweets.size()-1;i>=0;i--){
									    	Status tweet = tweets.get(i);
			    	    					if (services.size()==0||isSuspended()) break;
		    	    						if (((new Date()).getTime()-tweet.getCreatedAt().getTime())>receiver.speindData.getMaxStoreInfopointTime()) continue;
		    	    						

		    	    						SpeindAPI.SendInfoPointParams sendInfoPointParams=new SpeindAPI.SendInfoPointParams();
											sendInfoPointParams.postURL = "https://twitter.com/"+tweet.getUser().getScreenName()+"/status/"+tweet.getId();
		    	    						sendInfoPointParams.postTime=tweet.getCreatedAt();
		    	    						sendInfoPointParams.postSender="@"+tweet.getUser().getScreenName();
		    	    				    	sendInfoPointParams.senderBmp=null;
		    	    				    	sendInfoPointParams.senderBmpURL=tweet.getUser().getProfileImageURL();
		    	    				    	sendInfoPointParams.postBmp=null;
		    	    				    	sendInfoPointParams.postBmpURL="";
		    	    				    	
		    	    				    	sendInfoPointParams.postTitle="";
		    	    				    	if (tweet.isRetweet()&&tweet.getRetweetedStatus()!=null) {
		    	    				    		sendInfoPointParams.postOriginText=tweet.getRetweetedStatus().getText();
		    	    				    	} else {
		    	    				    		sendInfoPointParams.postOriginText=tweet.getText();
		    	    				    	}		    	    				    			    	    				    	
		    	    				    	
		    	    				    	sendInfoPointParams.postPluginData="";
		    	    				    	sendInfoPointParams.postTitleVocalizing="";
			    	    					String lang=getLang(sendInfoPointParams.postOriginText);
			    	    					Configuration conf = getResources().getConfiguration();
			    	    			    	Locale localeOld=conf.locale;
			    	    			    	conf.locale = new Locale(lang);
			    	    			    	Resources resources = new Resources(getAssets(), getResources().getDisplayMetrics(), conf);
			    	    			    	MediaEntity[] media_entryes=null;
			    	    			    	URLEntity[] url_entryes=null;
											String in_reply_to=tweet.getInReplyToScreenName();
			    	    					if (tweet.isRetweet()&&tweet.getRetweetedStatus()!=null) {
			    	    				    	media_entryes=tweet.getRetweetedStatus().getMediaEntities();
			    	    				    	for (int ep=0; ep<media_entryes.length;ep++) {
			    	    				    		if (media_entryes[ep].getType().equals("photo")) {
			    	    				    			if (sendInfoPointParams.postBmpURL.isEmpty()) {
			    	    				    				sendInfoPointParams.postBmpURL=media_entryes[ep].getMediaURL();
			    	    				    			}
			    	    				    			sendInfoPointParams.postOriginText=sendInfoPointParams.postOriginText.replaceAll(media_entryes[ep].getURL(), "");
			    	    				    		}
			    	    				    	}
			    	    				    	url_entryes=tweet.getRetweetedStatus().getURLEntities();
			    	    				    	for (int ep=0; ep<url_entryes.length; ep++) {
			    	    				    		if (url_entryes[ep].getExpandedURL()!=null&&!url_entryes[ep].getExpandedURL().isEmpty()) {
			    	    				    			sendInfoPointParams.postLink=url_entryes[ep].getExpandedURL();
				    	    				    		break;
			    	    				    		}
			    	    				    	}
			    	    						if (sendInfoPointParams.postBmpURL.isEmpty())
			    	    							sendInfoPointParams.postBmpURL=tweet.getRetweetedStatus().getUser().getProfileImageURL().replaceAll("_normal\\.", ".");
			    	    						
			    	    						String toname=tweet.getRetweetedStatus().getUser().getName();
			    	    						if (toname==null||toname.equals("")||!lang.equals(getLang(toname)))  toname=tweet.getRetweetedStatus().getUser().getScreenName();
			    	    						String name=tweet.getUser().getName();
			    	    						if (name==null||name.equals("")||!lang.equals(getLang(name)))  name=tweet.getUser().getScreenName();
				    	    			    	
			    	    						sendInfoPointParams.postSender+=" RT "+"@"+tweet.getRetweetedStatus().getUser().getScreenName();
				    	    			    	sendInfoPointParams.postSenderVocalizing=name+" "+resources.getString(R.string.retwitted)+" "+toname;
			    	    					} else {
			    	    				    	media_entryes=tweet.getMediaEntities();
			    	    				    	for (int ep=0; ep<media_entryes.length;ep++) {
			    	    				    		if (media_entryes[ep].getType().equals("photo")) {
			    	    				    			if (sendInfoPointParams.postBmpURL.isEmpty()) {
			    	    				    				sendInfoPointParams.postBmpURL=media_entryes[ep].getMediaURL();
			    	    				    			}
			    	    				    			sendInfoPointParams.postOriginText=sendInfoPointParams.postOriginText.replaceAll(media_entryes[ep].getURL(), "");
			    	    				    		}
			    	    				    	}
			    	    				    	url_entryes=tweet.getURLEntities();
			    	    				    	for (int ep=0; ep<url_entryes.length; ep++) {
			    	    				    		if (url_entryes[ep].getExpandedURL()!=null&&!url_entryes[ep].getExpandedURL().isEmpty()) {
			    	    				    			sendInfoPointParams.postLink=url_entryes[ep].getExpandedURL();
				    	    				    		break;
			    	    				    		}
			    	    				    	}
			    	            		    	if (sendInfoPointParams.postBmpURL.isEmpty()) 
			    	            		    		sendInfoPointParams.postBmpURL=tweet.getUser().getProfileImageURL().replaceAll("_normal\\.", ".");
			    	            		    	
			    	    						String name=tweet.getUser().getName();
			    	    						if (name==null||name.equals("")||!lang.equals(getLang(name)))  name=tweet.getUser().getScreenName();
			    	    						if (in_reply_to!=null&&!in_reply_to.isEmpty()) {
					    	    			    	sendInfoPointParams.postSender+=" RPL "+"@"+in_reply_to;
					    	    			    	sendInfoPointParams.postSenderVocalizing=resources.getString(R.string.reply)+" "+in_reply_to+" "+resources.getString(R.string.from)+" "+name;
				    	    					} else { 	    	    						
					    	    			    	sendInfoPointParams.postSenderVocalizing=name+" "+resources.getString(R.string.write);	    	
				    	    					}
			    	    					}

											sendInfoPointParams.postTextVocalizing=prepareText(sendInfoPointParams.postOriginText);
		    	    				    	sendInfoPointParams.lang=lang;
		    	    				    	sendInfoPointParams.checkForDuplicate=false;	    	    						
		    	    						
			    	    			    	conf.locale=localeOld;
			    	    			    	resources = new Resources(getAssets(), getResources().getDisplayMetrics(), conf);

                                            Map<String, Boolean> services_loc = new HashMap<>(services);
                                            Collection<String> service_packages = services_loc.keySet();
                                            for (String service_package : service_packages) {
                                                if (!services_loc.get(service_package)) sendInfoPoint(service_package, sendInfoPointParams);
                                            }
			    	    				}
									} catch (TwitterException e) {
										e.printStackTrace();
									}
                                    setProcessingState(false);
		    					}
		    				}
		    				    				
		    			}
			        }
			    }).start();		
			}

			if (!(services.size()==0||isSuspended())) {
				if (delta<0) delta=0;
				handler.postDelayed(updateTweetsRunnable, (refreshInterval*1000-delta));
			}
		}
	}

	@Override
	public void onLangListChanged(String service_package, ArrayList<String> arg0) { }

	@Override
	public void onLoadImagesOnMobileInetChanged(String service_package, boolean arg0) { }

	@Override
	public void onResume(String service_package) {
        //isSuspended=false;
        services.put(service_package, false);
		refressTweets();
	}

	@Override
	public void onStart(String service_package, int state) {
		if (state==SpeindAPI.DataFeedSettingsInfo.DATAFEED_STATE_READY) {
			//isSuspended=false;
            services.put(service_package, false);
            refressTweets();
		} else {
            //isSuspended=true;
            services.put(service_package, true);
		}
	}

	@Override
	public void onSuspend(String service_package) {
        //isSuspended=true;
        services.put(service_package, true);
		if (isSuspended()) handler.removeCallbacks(updateTweetsRunnable);
	}

	@Override
	public void onStoreInfopointTimeChanged(String service_package, long arg0) {
		// TODO Auto-generated method stub
		
	}

    private boolean isSuspended() {
        Map<String, Boolean> services_loc = new HashMap<>(services);
        Collection<Boolean> suspendeds = services_loc.values();
        for (Boolean suspended : suspendeds) {
            if (!suspended) return false;
        }
        return true;
    }

    @Override
    public boolean isAuthorized(String servicePackage) {
        return isAuthenticated(this, curProfile);
    }

	@Override
	public void onPost(String service_package, InfoPoint infopoint) {
        final SpeindAPI.InfoPointData data = infopoint.getData(profiles.get(service_package));
        if (data!=null) {
            int aType = SpeindAPI.getConnectionStatus(SpeindDataFeed.this);
            if (aType != -1) {
                if (isAuthenticated(SpeindDataFeed.this, curProfile)) {
                    (new Thread(new Runnable() {
                        @Override
                        public void run() {
                            Twitter twitter = PrepareAuthenticated();
                            if (twitter != null) {
                                StatusUpdate status = new StatusUpdate(getString(R.string.shared_via_speind)+" "+data.postURL);
                                try {
                                    twitter.updateStatus(status);
                                } catch (TwitterException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    })).start();
                }
            }
        }
	}

}
