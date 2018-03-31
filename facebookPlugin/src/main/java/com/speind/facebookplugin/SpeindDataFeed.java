package com.speind.facebookplugin;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.text.Html;
import android.util.Log;

import com.facebook.AccessToken;
import com.facebook.FacebookRequestError;
import com.facebook.FacebookSdk;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.facebook.HttpMethod;
import com.facebook.login.LoginManager;

import me.speind.SpeindAPI;
import me.speind.SpeindAPI.InfoPoint;

public class SpeindDataFeed extends SpeindAPI.DatafeedService {
    public static final String FACEBOOK_FEED_SETTINGS_COMMAND = "facebook_settings_command";
    public static final int FACEBOOK_REFRESH = 0;

	public static final String PREFS_NAME = "facebookReceiverConfig";
	public static final String TOKEN_NAME_KEY = "TokenName";

	//private boolean isStopReceived=false;
	//private boolean isSuspended=true;
	private Map<String, String> profiles = new HashMap<>();
	private String curProfile="profile";
    
	private final Handler handler = new Handler();
	private final Runnable updatePostsRunnable = new Runnable() { public void run() { refreshPosts(); } };

    private Map<String, Boolean> services = new HashMap<>();

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent!=null) {
            int cmd=intent.getIntExtra(FACEBOOK_FEED_SETTINGS_COMMAND, -1);
            switch (cmd) {
                case FACEBOOK_REFRESH:
                    if (!FacebookSdk.isInitialized()) FacebookSdk.sdkInitialize(getApplicationContext());
                    if (AccessToken.getCurrentAccessToken()!=null) {
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
                    refreshPosts();
                    break;
            }
        }
        return super.onStartCommand(intent, flags, startId);
    }

	@Override
    public SpeindAPI.DataFeedSettingsInfo onInit(String service_package) {
        services.put(service_package, true);
        SpeindAPI.DataFeedSettingsInfo info = new SpeindAPI.DataFeedSettingsInfo(this, getString(R.string.facebook_title), BitmapFactory.decodeResource(getResources(), R.drawable.ic_launcher), true);
        return info;
    }

	@Override
	public void onInfoPointDetails(String service_package, InfoPoint arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onLangListChanged(String service_package, ArrayList<String> arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onLike(String service_package, InfoPoint arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onLoadImagesOnMobileInetChanged(String service_package, boolean arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onSetProfile(String service_package, String profile) {
		profiles.put(service_package, profile);
        if (!FacebookSdk.isInitialized()) FacebookSdk.sdkInitialize(getApplicationContext());
	}

	@Override
	public void onStop(String service_package) {
        services.remove(service_package);
		profiles.remove(service_package);
        if (services.size()==0) {
            handler.removeCallbacks(updatePostsRunnable);
            //isStopReceived=true;
            this.stopSelf();
        } else if (isSuspended()) {
            handler.removeCallbacks(updatePostsRunnable);
        }
	}

	@Override
	public void onShowSettings(String service_package) {
		Intent intent=new Intent(SpeindDataFeed.this, FacebookSettings.class);
		intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		intent.putExtra("profile", curProfile);
		getApplication().startActivity(intent);
	}

	@Override
	public IBinder onBind(Intent intent) {
		// TODO Auto-generated method stub
		return null;
	}

	private String prepareText(String fulltext) {
 		String text="<p>";

 		fulltext=fulltext.replaceAll("\\[[^\\[]+\\|([^\\[]+)\\]", "$1");
 				
 		int cnt=fulltext.length();
 		StringBuffer word=new StringBuffer(cnt); 		
 		for (int i=0;i<cnt;i++) {
			char ch = fulltext.charAt(i);
			if ((ch==' ')||(ch=='\n')||(i==cnt-1)) {
				word.append(ch);
				if (word.charAt(0)=='#') {					
				} else {
					try {
						new URL(word.toString());
						text+="</p><p>"+getApplicationContext().getString(R.string.link)+"</p><p>";
					} catch (MalformedURLException e) {
						String tw=word.toString();
						//Pattern urlPattern = Pattern.compile("((https?)?://)?([a-zA-Z0-9-]{1,128}\\.)+([a-zA-Z]{2,4})+(:[0-9]{0,5})?(/[a-zA-Z0-9.,_@%&?+=\\~/#-]*)?");
						if (tw.matches("((https?)?://)?([a-zA-Z0-9-]{1,128}\\.)+([a-zA-Z]{2,4})+(:[0-9]{0,5})?(/[a-zA-Z0-9.,_@%&?+=\\~/#-]*)?")) {
							text+="</p><p>"+getApplicationContext().getString(R.string.link)+"</p><p>";
						} else {
							text+=word.toString();
						}
					} catch (Exception e) {
						text+=word.toString();
					}							 									
				}
				word=new StringBuffer(cnt);
			} else {
				word.append(ch);
			}
		}		
 		//Log.d("[---prepare tweet---]", "<-"+text);
 		return ""+Html.fromHtml(text+"</p>");
 	}
	
	public void refreshPosts() {
		//log("refreshPosts");
		handler.removeCallbacks(updatePostsRunnable);
        Log.e("[----Facebook--]", "! "+AccessToken.getCurrentAccessToken().getPermissions().contains("read_stream"));
        if (AccessToken.getCurrentAccessToken()!=null) {
            if (!(services.size()==0||isSuspended())) {
                SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
                int refreshInterval = settings.getInt(curProfile + "_refreshInterval", 15 * 60);
                long lastRefreshTime = settings.getLong("lastRefreshTime", 0);
                long delta = refreshInterval * 1000 - (new Date()).getTime() - lastRefreshTime;

                if (delta < 5000) {
                    //log("refreshPosts 1");
                    SharedPreferences.Editor editor = settings.edit();
                    editor.putLong("lastRefreshTime", (new Date()).getTime());
                    editor.commit();

                    int aType = SpeindAPI.getConnectionStatus(SpeindDataFeed.this);
                    if (aType != -1) {

                        setProcessingState(true);

                        Bundle params = new Bundle();
                        params.putString("limit", "100");
                        params.putString("locale", Locale.getDefault().getLanguage());
                        GraphRequest request = new GraphRequest(AccessToken.getCurrentAccessToken(), "/me/home", params, HttpMethod.GET, new GraphRequest.Callback() {
                            @Override
                            public void onCompleted(GraphResponse response) {
                                if (services.size() == 0 || isSuspended()) {
                                    setProcessingState(false);
                                    return;
                                }
                                // Process the returned response
                                Log.d("[------]", "" + response.getRawResponse());
                                JSONObject graphObject = response.getJSONObject();
                                FacebookRequestError error = response.getError();
                                if (graphObject != null) {

                                    try {
                                        // Get the data, parse info to get the key/value info
                                        final JSONArray dataArray = new JSONArray(graphObject.getJSONArray("data").toString());
                                        new Thread(new Runnable() {
                                            public void run() {
                                                if (services.size() == 0 || isSuspended()) {
                                                    setProcessingState(false);
                                                    return;
                                                }
                                                for (int i = 0; i < dataArray.length(); i++) {
                                                    if (services.size() == 0 || isSuspended()) {
                                                        setProcessingState(false);
                                                        return;
                                                    }
                                                    try {
                                                        JSONObject dataItem = dataArray.getJSONObject(i);
                                                        JSONObject from = dataItem.getJSONObject("from");

                                                        SpeindAPI.SendInfoPointParams sendInfoPointParams = new SpeindAPI.SendInfoPointParams();

                                                        sendInfoPointParams.postURL = "http://www.facebook.com/";

                                                        try {
                                                            sendInfoPointParams.postURL += dataItem.getString("id");
                                                        } catch (JSONException e) {
                                                        }

                                                        // Get date
                                                        try {
                                                            sendInfoPointParams.postTime = (new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ")).parse(dataItem.getString("created_time"));
                                                        } catch (ParseException e) {
                                                            sendInfoPointParams.postTime = new Date();
                                                        }

                                                        if (((new Date()).getTime() - sendInfoPointParams.postTime.getTime()) > receiver.speindData.getMaxStoreInfopointTime())
                                                            continue;

                                                        // Get sender id
                                                        String from_id = "";
                                                        try {
                                                            from_id = from.getString("id");
                                                        } catch (JSONException e) {
                                                        }
                                                        // Get post bitmap
                                                        String postBmpURL = null;
                                                        try {
                                                            postBmpURL = dataItem.getString("picture");
                                                            try {
                                                                List<NameValuePair> parameters = URLEncodedUtils.parse(new URI(postBmpURL), "UTF-8");
                                                                for (NameValuePair p : parameters) {
                                                                    if (p.getName().equals("url")) {
                                                                        postBmpURL = p.getValue();
                                                                        break;
                                                                    }
                                                                }
                                                            } catch (URISyntaxException e) {
                                                            }
                                                        } catch (JSONException e) {
                                                        }
                                                        Log.e("[------]", "https://graph.facebook.com/" + from_id + "/picture");

                                                        sendInfoPointParams.senderBmpURL = "https://graph.facebook.com/" + from_id + "/picture";
                                                        sendInfoPointParams.postBmpURL = sendInfoPointParams.senderBmpURL;

                                                        if (postBmpURL != null && !postBmpURL.equals(""))
                                                            sendInfoPointParams.postBmpURL = postBmpURL;
                                                        // Get post link
                                                        try {
                                                            sendInfoPointParams.postLink = dataItem.getString("link");
                                                        } catch (JSONException e) {
                                                        }
                                                        // Get sender name
                                                        String from_name = "";
                                                        try {
                                                            from_name = from.getString("name");
                                                        } catch (JSONException e) {
                                                        }
                                                        String fromNameLang = getLang(from_name + " " + from_name.toLowerCase() + " " + from_name + " " + from_name.toLowerCase());
                                                        // Get sender screen_name
                                                        String screen_name = from_id;
                                                        try {
                                                            URL url = new URL("https://graph.facebook.com/" + from_id + "/?fields=username");
                                                            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                                                            if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
                                                                BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                                                                StringBuilder response = new StringBuilder();
                                                                String inputLine;
                                                                while ((inputLine = in.readLine()) != null)
                                                                    response.append(inputLine);
                                                                in.close();
                                                                try {
                                                                    final JSONObject user_info = new JSONObject(response.toString());
                                                                    screen_name = user_info.getString("username");
                                                                } catch (JSONException e) {
                                                                }
                                                            }
                                                        } catch (Exception e) {
                                                        }

                                                        String lang = "";

                                                        String status_type = dataItem.getString("status_type");
                                                        //Log.e("[---postTime---]", sendInfoPointParams.postTime.toLocaleString());
                                                        //Log.e("[---status_type---]", status_type);
                                                        if (status_type == null) {
                                                            continue;
                                                        } else if (status_type.equals("mobile_status_update")) {
                                                            String message = "";
                                                            try {
                                                                message = dataItem.getString("message");
                                                            } catch (JSONException e) {
                                                            }

                                                            sendInfoPointParams.postOriginText = message;
                                                            lang = getLang(sendInfoPointParams.postOriginText);
                                                            if (lang.equalsIgnoreCase("")) {
                                                                lang = fromNameLang;
                                                            }

                                                            sendInfoPointParams.postSender = from_name;
                                                            try {
                                                                sendInfoPointParams.postSender = dataItem.getString("story");
                                                            } catch (JSONException e) {
                                                            }

                                                            if (!lang.equals(fromNameLang))
                                                                from_name = screen_name;

                                                            Configuration conf = getResources().getConfiguration();
                                                            Locale localeOld = conf.locale;
                                                            conf.locale = new Locale(lang);
                                                            Resources resources = new Resources(getAssets(), getResources().getDisplayMetrics(), conf);

                                                            sendInfoPointParams.postSenderVocalizing = resources.getString(R.string.new_status) + " " + from_name + ".";
                                                            sendInfoPointParams.postTextVocalizing = resources.getString(R.string.status_text) + ": " + Html.fromHtml(sendInfoPointParams.postOriginText);

                                                            conf.locale = localeOld;
                                                            resources = new Resources(getAssets(), getResources().getDisplayMetrics(), conf);

                                                        } else if (status_type.equals("created_note")) {
                                                            continue;
                                                        } else if (status_type.equals("added_photos")) {
                                                            String message = "";
                                                            try {
                                                                message = dataItem.getString("message");
                                                            } catch (JSONException e) {
                                                            }

                                                            sendInfoPointParams.postOriginText = message;
                                                            lang = getLang(sendInfoPointParams.postOriginText);
                                                            if (lang.equalsIgnoreCase("")) {
                                                                lang = fromNameLang;
                                                            }

                                                            sendInfoPointParams.postSender = from_name;
                                                            try {
                                                                sendInfoPointParams.postSender = dataItem.getString("story");
                                                            } catch (JSONException e) {
                                                            }

                                                            Configuration conf = getResources().getConfiguration();
                                                            Locale localeOld = conf.locale;
                                                            conf.locale = new Locale(lang);
                                                            Resources resources = new Resources(getAssets(), getResources().getDisplayMetrics(), conf);

                                                            sendInfoPointParams.postSenderVocalizing = resources.getString(R.string.new_photo_from) + " " + from_name + ".";
                                                            if (!sendInfoPointParams.postOriginText.equals(""))
                                                                sendInfoPointParams.postTextVocalizing = resources.getString(R.string.with_comment) + ": " + Html.fromHtml(sendInfoPointParams.postOriginText);

                                                            conf.locale = localeOld;
                                                            resources = new Resources(getAssets(), getResources().getDisplayMetrics(), conf);

                                                        } else if (status_type.equals("added_video")) {
                                                            String message = "";
                                                            try {
                                                                message = dataItem.getString("message");
                                                            } catch (JSONException e) {
                                                            }

                                                            sendInfoPointParams.postOriginText = message;
                                                            lang = getLang(sendInfoPointParams.postOriginText);
                                                            if (lang.equalsIgnoreCase("")) {
                                                                lang = fromNameLang;
                                                            }

                                                            sendInfoPointParams.postSender = from_name;
                                                            try {
                                                                sendInfoPointParams.postSender = dataItem.getString("story");
                                                            } catch (JSONException e) {
                                                            }

                                                            Configuration conf = getResources().getConfiguration();
                                                            Locale localeOld = conf.locale;
                                                            conf.locale = new Locale(lang);
                                                            Resources resources = new Resources(getAssets(), getResources().getDisplayMetrics(), conf);

                                                            sendInfoPointParams.postSenderVocalizing = resources.getString(R.string.new_video_from) + " " + from_name + ".";
                                                            if (!sendInfoPointParams.postOriginText.equals(""))
                                                                sendInfoPointParams.postTextVocalizing = resources.getString(R.string.with_comment) + ": " + Html.fromHtml(sendInfoPointParams.postOriginText);

                                                            conf.locale = localeOld;
                                                            resources = new Resources(getAssets(), getResources().getDisplayMetrics(), conf);
                                                        } else if (status_type.equals("shared_story")) {
                                                            String type = dataItem.getString("type");
                                                            if (type == null) {
                                                                continue;
                                                            } else if (type.equals("link")) {
                                                                String message = "";
                                                                try {
                                                                    message = dataItem.getString("message");
                                                                } catch (JSONException e) {
                                                                }

                                                                String name = "";
                                                                try {
                                                                    name = dataItem.getString("name");
                                                                } catch (JSONException e) {
                                                                }

                                                                String description = "";
                                                                try {
                                                                    description = dataItem.getString("description");
                                                                } catch (JSONException e) {
                                                                }

                                                                sendInfoPointParams.postTitle = message;
                                                                sendInfoPointParams.postOriginText = description;
                                                                if (!name.equals("")) {
                                                                    sendInfoPointParams.postOriginText = "" + Html.fromHtml("<p>" + name + "</p>" + "<br><p>" + description + "</p>");
                                                                }
                                                                lang = getLang(message + " " + name + " " + description);
                                                                if (lang.equalsIgnoreCase("")) {
                                                                    lang = fromNameLang;
                                                                }

                                                                sendInfoPointParams.postSender = from_name;
                                                                try {
                                                                    sendInfoPointParams.postSender = dataItem.getString("story");
                                                                } catch (JSONException e) {
                                                                }

                                                                Configuration conf = getResources().getConfiguration();
                                                                Locale localeOld = conf.locale;
                                                                conf.locale = new Locale(lang);
                                                                Resources resources = new Resources(getAssets(), getResources().getDisplayMetrics(), conf);

                                                                sendInfoPointParams.postSenderVocalizing = resources.getString(R.string.link_from) + " " + from_name + ".";
                                                                if (!sendInfoPointParams.postTitle.equals(""))
                                                                    sendInfoPointParams.postTitleVocalizing = resources.getString(R.string.with_comment) + ": " + Html.fromHtml(sendInfoPointParams.postTitle);
                                                                if (!name.equals(""))
                                                                    sendInfoPointParams.postTextVocalizing = resources.getString(R.string.title) + ": " + name + ". ";
                                                                if (!description.equals(""))
                                                                    sendInfoPointParams.postTextVocalizing += resources.getString(R.string.details) + ": " + description;
                                                                conf.locale = localeOld;
                                                                resources = new Resources(getAssets(), getResources().getDisplayMetrics(), conf);


                                                            } else if (type.equals("status")) {
                                                                continue;
                                                            } else if (type.equals("photo")) {
                                                                String message = "";
                                                                try {
                                                                    message = dataItem.getString("message");
                                                                } catch (JSONException e) {
                                                                }

                                                                String name = "";
                                                                try {
                                                                    name = dataItem.getString("caption");
                                                                } catch (JSONException e) {
                                                                    try {
                                                                        name = dataItem.getString("name");
                                                                    } catch (JSONException e1) {
                                                                    }
                                                                }

                                                                String description = "";
                                                                try {
                                                                    description = dataItem.getString("description");
                                                                } catch (JSONException e) {
                                                                }

                                                                sendInfoPointParams.postTitle = message;
                                                                sendInfoPointParams.postOriginText = description;
                                                                if (!name.equals("")) {
                                                                    sendInfoPointParams.postOriginText = "" + Html.fromHtml("<p>" + name + "</p>" + "<br><p>" + description + "</p>");
                                                                }
                                                                lang = getLang(message + " " + name + " " + description);
                                                                if (lang.equalsIgnoreCase("")) {
                                                                    lang = fromNameLang;
                                                                }

                                                                sendInfoPointParams.postSender = from_name;
                                                                try {
                                                                    sendInfoPointParams.postSender = dataItem.getString("story");
                                                                } catch (JSONException e) {
                                                                }

                                                                Configuration conf = getResources().getConfiguration();
                                                                Locale localeOld = conf.locale;
                                                                conf.locale = new Locale(lang);
                                                                Resources resources = new Resources(getAssets(), getResources().getDisplayMetrics(), conf);

                                                                sendInfoPointParams.postSenderVocalizing = resources.getString(R.string.photo_from) + " " + from_name + ".";
                                                                if (!sendInfoPointParams.postTitle.equals(""))
                                                                    sendInfoPointParams.postTitleVocalizing = resources.getString(R.string.with_comment) + ": " + Html.fromHtml(sendInfoPointParams.postTitle);
                                                                if (!name.equals(""))
                                                                    sendInfoPointParams.postTextVocalizing = resources.getString(R.string.title) + ": " + name + ". ";
                                                                if (!description.equals(""))
                                                                    sendInfoPointParams.postTextVocalizing += resources.getString(R.string.details) + ": " + description;
                                                                conf.locale = localeOld;
                                                                resources = new Resources(getAssets(), getResources().getDisplayMetrics(), conf);

                                                            } else if (type.equals("video")) {
                                                                String message = "";
                                                                try {
                                                                    message = dataItem.getString("message");
                                                                } catch (JSONException e) {
                                                                }

                                                                String name = "";
                                                                try {
                                                                    name = dataItem.getString("caption");
                                                                } catch (JSONException e) {
                                                                    try {
                                                                        name = dataItem.getString("name");
                                                                    } catch (JSONException e1) {
                                                                    }
                                                                }

                                                                String description = "";
                                                                try {
                                                                    description = dataItem.getString("description");
                                                                } catch (JSONException e) {
                                                                }

                                                                sendInfoPointParams.postTitle = message;
                                                                sendInfoPointParams.postOriginText = description;
                                                                if (!name.equals("")) {
                                                                    sendInfoPointParams.postOriginText = "" + Html.fromHtml("<p>" + name + "</p>" + "<br><p>" + description + "</p>");
                                                                }
                                                                lang = getLang(message + " " + name + " " + description);
                                                                if (lang.equalsIgnoreCase("")) {
                                                                    lang = fromNameLang;
                                                                }

                                                                sendInfoPointParams.postSender = from_name;
                                                                try {
                                                                    sendInfoPointParams.postSender = dataItem.getString("story");
                                                                } catch (JSONException e) {
                                                                }

                                                                Configuration conf = getResources().getConfiguration();
                                                                Locale localeOld = conf.locale;
                                                                conf.locale = new Locale(lang);
                                                                Resources resources = new Resources(getAssets(), getResources().getDisplayMetrics(), conf);

                                                                sendInfoPointParams.postSenderVocalizing = resources.getString(R.string.video_from) + " " + from_name + ".";
                                                                if (!sendInfoPointParams.postTitle.equals(""))
                                                                    sendInfoPointParams.postTitleVocalizing = resources.getString(R.string.with_comment) + ": " + Html.fromHtml(sendInfoPointParams.postTitle);
                                                                if (!name.equals(""))
                                                                    sendInfoPointParams.postTextVocalizing = resources.getString(R.string.title) + ": " + name + ". ";
                                                                if (!description.equals(""))
                                                                    sendInfoPointParams.postTextVocalizing += resources.getString(R.string.details) + ": " + description;
                                                                conf.locale = localeOld;
                                                                resources = new Resources(getAssets(), getResources().getDisplayMetrics(), conf);
                                                            } else {
                                                                continue;
                                                            }
                                                        } else if (status_type.equals("app_created_story") || status_type.equals("published_story")) {
                                                            String type = dataItem.getString("type");
                                                            if (type == null) {
                                                                continue;
                                                            } else if (type.equals("link")) {
                                                                String message = "";
                                                                try {
                                                                    message = dataItem.getString("message");
                                                                } catch (JSONException e) {
                                                                }

                                                                String name = "";
                                                                try {
                                                                    name = dataItem.getString("name");
                                                                } catch (JSONException e) {
                                                                }

                                                                String description = "";
                                                                try {
                                                                    description = dataItem.getString("description");
                                                                } catch (JSONException e) {
                                                                }

                                                                sendInfoPointParams.postTitle = message;
                                                                sendInfoPointParams.postOriginText = description;
                                                                if (!name.equals("")) {
                                                                    sendInfoPointParams.postOriginText = "" + Html.fromHtml("<p>" + name + "</p>" + "<br><p>" + description + "</p>");
                                                                }
                                                                lang = getLang(message + " " + name + " " + description);
                                                                if (lang.equalsIgnoreCase("")) {
                                                                    lang = fromNameLang;
                                                                }

                                                                String application_name = "";
                                                                try {
                                                                    application_name = dataItem.getJSONObject("application").getString("name");
                                                                } catch (JSONException e) {
                                                                }

                                                                sendInfoPointParams.postSender = from_name + ((application_name.equals("")) ? "" : (" via " + application_name));
                                                                try {
                                                                    sendInfoPointParams.postSender = dataItem.getString("story");
                                                                } catch (JSONException e) {
                                                                }

                                                                Configuration conf = getResources().getConfiguration();
                                                                Locale localeOld = conf.locale;
                                                                conf.locale = new Locale(lang);
                                                                Resources resources = new Resources(getAssets(), getResources().getDisplayMetrics(), conf);

                                                                sendInfoPointParams.postSenderVocalizing = resources.getString(R.string.link_from) + " " + from_name + ((application_name.equals("")) ? "" : (resources.getString(R.string.via) + application_name)) + ".";
                                                                if (!sendInfoPointParams.postTitle.equals(""))
                                                                    sendInfoPointParams.postTitleVocalizing = resources.getString(R.string.with_comment) + ": " + Html.fromHtml(sendInfoPointParams.postTitle);
                                                                if (!name.equals(""))
                                                                    sendInfoPointParams.postTextVocalizing = resources.getString(R.string.title) + ": " + name + ". ";
                                                                if (!description.equals(""))
                                                                    sendInfoPointParams.postTextVocalizing += resources.getString(R.string.details) + ": " + description;
                                                                conf.locale = localeOld;
                                                                resources = new Resources(getAssets(), getResources().getDisplayMetrics(), conf);

                                                            } else if (type.equals("status")) {
                                                                String message = "";
                                                                try {
                                                                    message = dataItem.getString("message");
                                                                } catch (JSONException e) {
                                                                }

                                                                String application_name = "";
                                                                try {
                                                                    application_name = dataItem.getJSONObject("application").getString("name");
                                                                } catch (JSONException e) {
                                                                }

                                                                sendInfoPointParams.postTitle = "";
                                                                sendInfoPointParams.postOriginText = message;

                                                                lang = getLang(sendInfoPointParams.postOriginText);
                                                                if (lang.equalsIgnoreCase("")) {
                                                                    lang = fromNameLang;
                                                                }

                                                                sendInfoPointParams.postSender = from_name + ((application_name.equals("")) ? "" : (" via " + application_name));
                                                                try {
                                                                    sendInfoPointParams.postSender = dataItem.getString("story");
                                                                } catch (JSONException e) {
                                                                }

                                                                Configuration conf = getResources().getConfiguration();
                                                                Locale localeOld = conf.locale;
                                                                conf.locale = new Locale(lang);
                                                                Resources resources = new Resources(getAssets(), getResources().getDisplayMetrics(), conf);

                                                                sendInfoPointParams.postSenderVocalizing = resources.getString(R.string.post_from) + " " + from_name + ((application_name.equals("")) ? "" : (resources.getString(R.string.via) + application_name)) + ".";
                                                                if (!message.equals(""))
                                                                    sendInfoPointParams.postTextVocalizing = resources.getString(R.string.post_text) + ": " + message + ". ";

                                                                conf.locale = localeOld;
                                                                resources = new Resources(getAssets(), getResources().getDisplayMetrics(), conf);

                                                            } else if (type.equals("photo")) {
                                                                String message = "";
                                                                try {
                                                                    message = dataItem.getString("message");
                                                                } catch (JSONException e) {
                                                                }

                                                                String name = "";
                                                                try {
                                                                    name = dataItem.getString("name");
                                                                } catch (JSONException e) {
                                                                }
                                                                try {
                                                                    name = dataItem.getString("caption");
                                                                } catch (JSONException e) {
                                                                }

                                                                String description = "";
                                                                try {
                                                                    description = dataItem.getString("description");
                                                                } catch (JSONException e) {
                                                                }

                                                                sendInfoPointParams.postTitle = message;
                                                                sendInfoPointParams.postOriginText = description;
                                                                if (!name.equals("")) {
                                                                    sendInfoPointParams.postOriginText = "" + Html.fromHtml("<p>" + name + "</p>" + "<br><p>" + description + "</p>");
                                                                }
                                                                lang = getLang(message + " " + name + " " + description);
                                                                if (lang.equalsIgnoreCase("")) {
                                                                    lang = fromNameLang;
                                                                }

                                                                String application_name = "";
                                                                try {
                                                                    application_name = dataItem.getJSONObject("application").getString("name");
                                                                } catch (JSONException e) {
                                                                }

                                                                sendInfoPointParams.postSender = from_name + ((application_name.equals("")) ? "" : (" via " + application_name));
                                                                try {
                                                                    sendInfoPointParams.postSender = dataItem.getString("story");
                                                                } catch (JSONException e) {
                                                                }

                                                                Configuration conf = getResources().getConfiguration();
                                                                Locale localeOld = conf.locale;
                                                                conf.locale = new Locale(lang);
                                                                Resources resources = new Resources(getAssets(), getResources().getDisplayMetrics(), conf);

                                                                sendInfoPointParams.postSenderVocalizing = resources.getString(R.string.photo_from) + " " + from_name + ((application_name.equals("")) ? "" : (resources.getString(R.string.via) + application_name)) + ".";
                                                                if (!sendInfoPointParams.postTitle.equals(""))
                                                                    sendInfoPointParams.postTitleVocalizing = resources.getString(R.string.with_comment) + ": " + Html.fromHtml(sendInfoPointParams.postTitle);
                                                                if (!name.equals(""))
                                                                    sendInfoPointParams.postTextVocalizing = resources.getString(R.string.title) + ": " + name + ". ";
                                                                if (!description.equals(""))
                                                                    sendInfoPointParams.postTextVocalizing += resources.getString(R.string.details) + ": " + description;
                                                                conf.locale = localeOld;
                                                                resources = new Resources(getAssets(), getResources().getDisplayMetrics(), conf);

                                                            } else if (type.equals("video")) {
                                                                String message = "";
                                                                try {
                                                                    message = dataItem.getString("message");
                                                                } catch (JSONException e) {
                                                                }

                                                                String name = "";
                                                                try {
                                                                    name = dataItem.getString("name");
                                                                } catch (JSONException e) {
                                                                }
                                                                try {
                                                                    name = dataItem.getString("caption");
                                                                } catch (JSONException e) {
                                                                }

                                                                String description = "";
                                                                try {
                                                                    description = dataItem.getString("description");
                                                                } catch (JSONException e) {
                                                                }

                                                                sendInfoPointParams.postTitle = message;
                                                                sendInfoPointParams.postOriginText = description;
                                                                if (!name.equals("")) {
                                                                    sendInfoPointParams.postOriginText = "" + Html.fromHtml("<p>" + name + "</p>" + "<br><p>" + description + "</p>");
                                                                }
                                                                lang = getLang(message + " " + name + " " + description);
                                                                if (lang.equalsIgnoreCase("")) {
                                                                    lang = fromNameLang;
                                                                }

                                                                String application_name = "";
                                                                try {
                                                                    application_name = dataItem.getJSONObject("application").getString("name");
                                                                } catch (JSONException e) {
                                                                }

                                                                sendInfoPointParams.postSender = from_name + ((application_name.equals("")) ? "" : (" via " + application_name));
                                                                try {
                                                                    sendInfoPointParams.postSender = dataItem.getString("story");
                                                                } catch (JSONException e) {
                                                                }

                                                                Configuration conf = getResources().getConfiguration();
                                                                Locale localeOld = conf.locale;
                                                                conf.locale = new Locale(lang);
                                                                Resources resources = new Resources(getAssets(), getResources().getDisplayMetrics(), conf);

                                                                sendInfoPointParams.postSenderVocalizing = resources.getString(R.string.video_from) + " " + from_name + ((application_name.equals("")) ? "" : (resources.getString(R.string.via) + application_name)) + ".";
                                                                if (!sendInfoPointParams.postTitle.equals(""))
                                                                    sendInfoPointParams.postTitleVocalizing = resources.getString(R.string.with_comment) + ": " + Html.fromHtml(sendInfoPointParams.postTitle);
                                                                if (!name.equals(""))
                                                                    sendInfoPointParams.postTextVocalizing = resources.getString(R.string.title) + ": " + name + ". ";
                                                                if (!description.equals(""))
                                                                    sendInfoPointParams.postTextVocalizing += resources.getString(R.string.details) + ": " + description;
                                                                conf.locale = localeOld;
                                                                resources = new Resources(getAssets(), getResources().getDisplayMetrics(), conf);

                                                            } else {
                                                                continue;
                                                            }
                                                            //} else if (status_type.equals("published_story")) {
                                                            //	continue;
                                                        } else if (status_type.equals("wall_post")) {
                                                            continue;
                                                        } else if (status_type.equals("created_event")) {
                                                            continue;
                                                        } else if (status_type.equals("created_group")) {
                                                            continue;
                                                        } else if (status_type.equals("tagged_in_photo")) {
                                                            continue;
                                                        } else if (status_type.equals("approved_friend")) {
                                                            continue;
                                                        } else {
                                                            continue;
                                                        }

                                                        Configuration conf = getResources().getConfiguration();
                                                        Locale localeOld = conf.locale;
                                                        conf.locale = new Locale(lang);
                                                        Resources resources = new Resources(getAssets(), getResources().getDisplayMetrics(), conf);

                                                        sendInfoPointParams.postTitleVocalizing = prepareText(sendInfoPointParams.postTitleVocalizing);
                                                        sendInfoPointParams.postTextVocalizing = prepareText(sendInfoPointParams.postTextVocalizing);

                                                        conf.locale = localeOld;
                                                        resources = new Resources(getAssets(), getResources().getDisplayMetrics(), conf);

                                                        sendInfoPointParams.postPluginData = "";
                                                        sendInfoPointParams.lang = lang;
                                                        sendInfoPointParams.checkForDuplicate = false;
                                                        sendInfoPointParams.priority = InfoPoint.PRIORITY_NORMAL;

                                                        if (services.size() == 0 || isSuspended()) {
                                                            setProcessingState(false);
                                                        }
                                                        Map<String, Boolean> services_loc = new HashMap<>(services);
                                                        Collection<String> service_packages = services_loc.keySet();

                                                        for (String service_package : service_packages) {
                                                            if (!services_loc.get(service_package))
                                                                sendInfoPoint(service_package, sendInfoPointParams);
                                                        }
                                                    } catch (JSONException e) {
                                                    }
                                                }
                                                setProcessingState(false);
                                            }
                                        }).start();
                                    } catch (JSONException e) {
                                        setProcessingState(false);
                                    }
                                } else {
                                    //message = "Error getting request info";
                                    setProcessingState(false);
                                }
                            }
                        });
                        // Execute the request asynchronously.
                        request.executeAsync();
                    }
                }
                if (!(services.size() == 0 || isSuspended())) {
                    if (delta < 0) delta = 0;
                    handler.postDelayed(updatePostsRunnable, (refreshInterval * 1000 - delta));
                }
            }
		}
	}
	
/*	
	private void getRequestData(final String inRequestId) {
	    // Create a new request for an HTTP GET with the
	    // request ID as the Graph path.
	    Request request = new Request(Session.getActiveSession(), inRequestId, null, HttpMethod.GET, new Request.Callback() {
            @Override
            public void onCompleted(Response response) {
                // Process the returned response
                GraphObject graphObject = response.getGraphObject();
                FacebookRequestError error = response.getError();
                // Default message
                String message = "Incoming request";
                if (graphObject != null) {
                    // Check if there is extra data
                    if (graphObject.getProperty("data") != null) {
                        try {
                            // Get the data, parse info to get the key/value info
                            JSONObject dataObject = new JSONObject((String)graphObject.getProperty("data"));
                            // Get the value for the key - badge_of_awesomeness
                            String badge = dataObject.getString("badge_of_awesomeness");
                            // Get the value for the key - social_karma
                            String karma = dataObject.getString("social_karma");
                            // Get the sender's name
                            JSONObject fromObject = (JSONObject) graphObject.getProperty("from");
                            String sender = fromObject.getString("name");
                            String title = sender+" sent you a gift";
                            // Create the text for the alert based on the sender and the data
                            message = title + "\n\n" +  "Badge: " + badge +  " Karma: " + karma;
                        } catch (JSONException e) {
                            message = "Error getting request info";
                        }
                    } else if (error != null) {
                        message = "Error getting request info";
                    }
                }
                //Toast.makeText(getActivity().getApplicationContext(), message, Toast.LENGTH_LONG).show();
            }
	    });
	    // Execute the request asynchronously.
	    Request.executeBatchAsync(request);
	}
	
	private void deleteRequest(String inRequestId) {
	    // Create a new request for an HTTP delete with the
	    // request ID as the Graph path.
	    Request request = new Request(Session.getActiveSession(), inRequestId, null, HttpMethod.DELETE, new Request.Callback() {
            @Override
            public void onCompleted(Response response) {
                // Show a confirmation of the deletion
                // when the API call completes successfully.
                //Toast.makeText(getActivity().getApplicationContext(), "Request deleted",
                //Toast.LENGTH_SHORT).show();
            }
        });
	    // Execute the request asynchronously.
	    Request.executeBatchAsync(request);
	}
*/	

	@Override
	public void onResume(String service_package) {
		//isSuspended=false;
        services.put(service_package, false);
		refreshPosts();
	}

	@Override
	public void onStart(String service_package, int state) {
		if (state==SpeindAPI.DataFeedSettingsInfo.DATAFEED_STATE_READY) {
			//isSuspended=false;
            services.put(service_package, false);
			refreshPosts();
		} else {
			//isSuspended=true;
            services.put(service_package, true);
		}	
	}

	@Override
	public void onSuspend(String service_package) {
		//isSuspended=true;
        services.put(service_package, true);
        if (isSuspended()) handler.removeCallbacks(updatePostsRunnable);
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

	public boolean isAuthorized(String service_package) {
        if (!FacebookSdk.isInitialized()) FacebookSdk.sdkInitialize(getApplicationContext());
		return AccessToken.getCurrentAccessToken() != null;
	}

	@Override
	public void onPost(String service_package, InfoPoint infopoint) {
		SpeindAPI.InfoPointData data = infopoint.getData(profiles.get(service_package));
		if (data!=null) {
            if (AccessToken.getCurrentAccessToken().getPermissions().contains("publish_actions")) {
                Bundle params = new Bundle();
                params.putString("message", getString(R.string.shared_via_speind));
                params.putString("link", data.postURL);
                GraphRequest request = new GraphRequest(AccessToken.getCurrentAccessToken(), "/me/feed", params, HttpMethod.POST, new GraphRequest.Callback() {
                    @Override
                    public void onCompleted(GraphResponse response) {

                    }
                });
                request.executeAsync();
            } else {
                //LoginManager.getInstance().logInWithPublishPermissions(this, Arrays.asList("publish_actions", ""));
                Intent intent=new Intent(SpeindDataFeed.this, PermissionRequest.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.putExtra("profile", profiles.get(service_package));
                infopoint.putToIntent(intent);
                intent.putExtra("permission", "publish_actions");
                getApplication().startActivity(intent);
            }
		}
	}

}
