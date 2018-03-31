package com.maple.rssreceiver;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.IBinder;
import android.text.Html;
import android.util.Log;

import com.maple.speind.R;
//import ru.lifenews.speind.R;

import me.speind.SpeindAPI;
import me.speind.SpeindAPI.InfoPoint;

public class SpeindDataFeed extends SpeindAPI.DatafeedService{
    private static final boolean isDebug = true;

    public SpeindAPI.LooperThread updateWorker = new SpeindAPI.LooperThread();

	public static final String PREFS_NAME = "rssReceiverConfig";
	public final static String BROADCAST_ACTION	="com.maple.RSSReceiver.servicebackbroadcast";

	public static final String RSS_FEED_SETTINGS_COMMAND = "rss_feed_settings_command";
	public static final int RSS_REFRESH_NEWS = 0;
	private static final String FEEDS_LIST_URL="http://speind.me/rsssources/RSS-FeedsList.csv";

	private final Handler handler = new Handler();
	private final Runnable updateNewsRunnable = new Runnable() { public void run() { refreshRssNews(); } };

	private boolean isStopReceived=false;
	private boolean isSuspended=true;
	
	private String curProfile="";	
	private boolean langListChanged=false;
	private boolean processingUpdateList=false;
	private boolean processingUpdateNews=false;

    public static class DatabaseManager {
        private static DatabaseManager instance = null;
        private static DBHelper mDatabaseHelper = null;

        public static class DBHelper extends SQLiteOpenHelper {
            private Context ctx = null;

            public DBHelper(Context context) {
                super(context, "speindRSSReceiverDB", null, 1);
                ctx = context;
            }

            @Override
            public void onCreate(SQLiteDatabase db) {
                db.execSQL( "CREATE TABLE rsssources (" +
                                "  profile text," +
                                "  link text," +
                                "  city text," +
                                "  provider text," +
                                "  category text," +
                                "  parentcategory text," +
                                "  vocalizing text," +
                                "  enabled integer," +
                                "  region text," +
                                "  country text," +
                                "  lang text," +
                                "  level text," +
                                "  PRIMARY KEY (profile, level, link)"+
                                ");"
                );

                final SharedPreferences settings = ctx.getSharedPreferences(SpeindDataFeed.PREFS_NAME, 0);
                SharedPreferences.Editor editor = settings.edit();
                editor.putBoolean("needLoadFromSettings", true);
                editor.apply();
            }

            @Override
            public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

            }

        }

        public static synchronized void initializeInstance(Context context) {
            if (instance == null) {
                instance = new DatabaseManager();
                mDatabaseHelper = new DBHelper(context);
            }
        }

        public static synchronized DatabaseManager getInstance() {
            return instance;
        }

        public synchronized SQLiteDatabase getWritableDatabase() {
            return mDatabaseHelper.getWritableDatabase();
        }

        public synchronized SQLiteDatabase getReadableDatabase() {
            return mDatabaseHelper.getReadableDatabase();
        }
    }

	public static class RSSFeed {
		String link;
		String city;
		String provider;
		String category;
		String parentcategory;
		String vocalizing;
		boolean enabled;
		
		String region;
		String country;
		String lang;
		
		RSSFeed(String city, String provider, String category, String parentcategory, String link, String vocalizing, String region, String country, String lang) {
			this.city = city;
			this.provider = provider;
			this.category = category;
			this.parentcategory = parentcategory;
			this.link = link;
			this.vocalizing = vocalizing;
			this.enabled=false;
			
			this.region = region;
			this.country = country;
			this.lang = lang;
		}

		RSSFeed(String city, String provider, String category, String parentcategory, String link, String vocalizing, Boolean enabled, String region, String country, String lang) {
			this.city = city;
			this.provider = provider;
			this.category = category;
			this.parentcategory = parentcategory;
			this.link = link;
			this.vocalizing = vocalizing;
			this.enabled=enabled;
			
			this.region = region;
			this.country = country;
			this.lang = lang;
		}
		
		public static Map<String, String> getFeedsListFromCSVURL(String feedUrl) {
			log("getFeedsListFromCSVURL: "+feedUrl);
			Map<String, String> res=new HashMap<>();
			try {
				URL url = new URL(feedUrl);
				HttpURLConnection conn = (HttpURLConnection) url.openConnection();
				if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
					InputStream is = conn.getInputStream();
					BufferedReader reader = new BufferedReader(new InputStreamReader(is));
					CSVReader csvReader = new CSVReader(reader, ',', '"', 1);
					String strings[];
					while ((strings=csvReader.readNext())!=null) {
						res.put(strings[0], strings[1]);
					}
					csvReader.close();
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			return res;
		}
		
		public static ArrayList<RSSFeed> getFeedsFromCSVURL(String feedUrl) {
			ArrayList<RSSFeed> rssFeedsAr = new ArrayList<>();
			try {
				URL url = new URL(feedUrl);
				HttpURLConnection conn = (HttpURLConnection) url.openConnection();
				if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
					InputStream is = conn.getInputStream();
					BufferedReader reader = new BufferedReader(new InputStreamReader(is));
					CSVReader csvReader = new CSVReader(reader, ',', '"', 1);
					String strings[];
					while ((strings=csvReader.readNext())!=null) {
						rssFeedsAr.add(new RSSFeed(strings[3].trim(), strings[6].trim(), strings[4].trim(), strings[5].trim(), strings[8].trim(), strings[7].trim(), strings[2].trim(), strings[1].trim(), strings[0].trim()));
					}
					csvReader.close();
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			return rssFeedsAr;
		}
		
	}
	
	private static void log(String mess) {
		if (isDebug) Log.e("[---RSSPlugin---]", Thread.currentThread().getName()+": "+mess);
	}
	
	@Override
	public void onCreate() {
		super.onCreate();
        log("onCreate");
        updateWorker.setThreadName("SRSSRworker");
        updateWorker.start();
        DatabaseManager.initializeInstance(this);
	}

    @Override
    public SpeindAPI.DataFeedSettingsInfo onInit(String service_package) {
        if (getPackageName().equals(service_package)) {
            SpeindAPI.DataFeedSettingsInfo info = new SpeindAPI.DataFeedSettingsInfo(this, getString(R.string.rssreceiver), BitmapFactory.decodeResource(getResources(), R.drawable.rss_logo), false);
            return info;
        }
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
    	log("onStartCommand");
    	if (intent!=null) {
			int cmd=intent.getIntExtra(RSS_FEED_SETTINGS_COMMAND, -1);
			switch (cmd) {
			case RSS_REFRESH_NEWS:
				refreshRssNews();			
				break;		
			}
    	}
        return super.onStartCommand(intent, flags, startId);
    }
 	
 	private boolean checkLocale(String locale) {
 		for (String lang : speindDFData.langList) {
	 		if (locale.toLowerCase().contains(lang.toLowerCase())) {
	 			return true;
	 		}
 		}
 		return false;
 	}
  	
	private void refreshRssNews() {
		log("refreshRssNews");
		handler.removeCallbacks(updateNewsRunnable);
        if (updateWorker.mHandler==null) {
            handler.postDelayed(updateNewsRunnable, 3000);
            return;
        }
		if (!(isStopReceived||isSuspended)) {
            if (receiver.worker.mHandler!=null) {
                receiver.worker.mHandler.post(new Runnable(){
                    @Override
                    public void run() {
                        final SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
                        int refreshInterval = settings.getInt(curProfile+"_refreshInterval", 15*60);

                        long lastRefreshTime = settings.getLong("lastRefreshTime", 0);
                        long delta=refreshInterval*1000-(new Date()).getTime()-lastRefreshTime;

                        final boolean nrrl=needRefreshRssList();
                        final boolean nurn=(delta<5000)||nrrl;

                        log("refreshRssNews "+nurn+" "+processingUpdateNews+" "+nrrl+" "+processingUpdateList+" "+(((nurn&&!processingUpdateNews)||nrrl)&&!processingUpdateList));

                        if (((nurn&&!processingUpdateNews)||nrrl)&&!processingUpdateList) {
                            processingUpdateNews=true;
                            if (nrrl) processingUpdateList=true;
                            updateWorker.mHandler.post(
                                    new Runnable() {
                                        public void run() {
                                            int aType = SpeindAPI.getConnectionStatus(SpeindDataFeed.this);
                                            if (aType!=-1) {
                                                SQLiteDatabase db = DatabaseManager.getInstance().getReadableDatabase();
                                                SpeindDataFeed.this.setProcessingState(true);
                                                Map<String, RSSFeed> rssFeedsMap = loadRSSFeeds(SpeindDataFeed.this, false, db, curProfile, "feed");
                                                if (nrrl) {
                                                    if (langListChanged) langListChanged=false;
                                                    db = DatabaseManager.getInstance().getWritableDatabase();
                                                    rssFeedsMap=refreshRssList(rssFeedsMap, db, curProfile);
                                                    processingUpdateList=false;
                                                }
                                                log("processingUpdateList: " + processingUpdateList);
                                                refreshRssNews(rssFeedsMap);
                                                processingUpdateNews=false;
                                                SpeindDataFeed.this.setProcessingState(false);
                                            }
                                            processingUpdateNews=false;
                                            processingUpdateList=false;
                                        }
                                    }
                            );
                        }
                        if (!(isStopReceived||isSuspended)) {
                            if (delta<0) delta=0;
                            if (refreshInterval*1000-delta<0) {
                                handler.postDelayed(updateNewsRunnable, (10*1000-delta));
                            } else {
                                handler.postDelayed(updateNewsRunnable, (refreshInterval*1000-delta));
                            }
                        }
                    }
                });
            }
		}
	}

/*
	public static void saveRSSFeeds(Context context, String profile, Map<String, RSSFeed> rssFeedsMap, String pref) {
		log("saveRSSFeeds");
    	SharedPreferences settings = context.getSharedPreferences(PREFS_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();        
    	int feedsCount = rssFeedsMap.size();
		int i=0;
		Iterator<String> myVeryOwnIterator = rssFeedsMap.keySet().iterator();
		while(myVeryOwnIterator.hasNext()) {
			String key=(String)myVeryOwnIterator.next();
		    RSSFeed feed=rssFeedsMap.get(key);
		    editor.putString(profile+"_"+pref+"_city_"+i, 			feed.city);
		    editor.putString(profile+"_"+pref+"_provider_"+i, 		feed.provider);
		    editor.putString(profile+"_"+pref+"_category_"+i, 		feed.category);
		    editor.putString(profile+"_"+pref+"_subcategory_"+i, 	feed.parentcategory);
		    editor.putString(profile+"_"+pref+"_link_"+i,			feed.link);
		    editor.putString(profile+"_"+pref+"_vocalizing_"+i,		feed.vocalizing);
		    editor.putBoolean(profile+"_"+pref+"_enabled_"+i,		feed.enabled);
		    editor.putString(profile+"_"+pref+"_region_"+i,			feed.region);
		    editor.putString(profile+"_"+pref+"_country_"+i,		feed.country);
			editor.putString(profile+"_"+pref+"_lang_"+i,			feed.lang);
		    i++;
		}
    	editor.putInt(profile+"_"+pref+"s_count", feedsCount);
        editor.apply();
	}
*/

	public static Map<String, RSSFeed> loadRSSFeeds(Context context, Boolean needLoadFromSettings, SQLiteDatabase db, String profile, String level) {
		log("loadRSSFeeds");
		Map<String, RSSFeed> rssFeedsMap = new HashMap<>();

        if (needLoadFromSettings) {
            SharedPreferences settings = context.getSharedPreferences(PREFS_NAME, 0);
            int feedsCount = settings.getInt(profile+"_"+level+"s_count", 0);
            for (int i=0;i<feedsCount;i++) {
                RSSFeed feed=new RSSFeed(
                        settings.getString(profile+"_"+level+"_city_"+i, "").trim(),
                        settings.getString(profile+"_"+level+"_provider_"+i, "").trim(),
                        settings.getString(profile+"_"+level+"_category_"+i, ""),
                        settings.getString(profile+"_"+level+"_subcategory_"+i, "").trim(),
                        settings.getString(profile+"_"+level+"_link_"+i, "").trim(),
                        settings.getString(profile+"_"+level+"_vocalizing_"+i, "").trim(),
                        settings.getBoolean(profile+"_"+level+"_enabled_"+i, false),
                        settings.getString(profile+"_"+level+"_region_"+i, "").trim(),
                        settings.getString(profile+"_"+level+"_country_"+i, "").trim(),
                        settings.getString(profile+"_"+level+"_lang_"+i, "").trim()

                );
                rssFeedsMap.put(feed.link, feed);

                ContentValues cv = new ContentValues();
                cv.put("profile", profile);
                cv.put("link", feed.link);
                cv.put("city", feed.city);
                cv.put("provider", feed.provider);
                cv.put("category", feed.category);
                cv.put("parentcategory", feed.parentcategory);
                cv.put("vocalizing", feed.vocalizing);
                cv.put("enabled", (feed.enabled ? 1 : 0));
                cv.put("region", feed.region);
                cv.put("country", feed.country);
                cv.put("lang", feed.lang);
                cv.put("level", level);
                db.insert("rsssources", null, cv);
            }
        } else {
            String selection = "profile = ? and level = ?";
            String[] selectionArgs = new String[] { profile, level };
            Cursor c = db.query("rsssources", null, selection, selectionArgs, null, null, null);
            if (c.moveToFirst()) {
                do {
                    RSSFeed feed=new RSSFeed(
                            c.getString(c.getColumnIndex("city")),
                            c.getString(c.getColumnIndex("provider")),
                            c.getString(c.getColumnIndex("category")),
                            c.getString(c.getColumnIndex("parentcategory")),
                            c.getString(c.getColumnIndex("link")),
                            c.getString(c.getColumnIndex("vocalizing")),
                            c.getInt(c.getColumnIndex("enabled"))!=0,
                            c.getString(c.getColumnIndex("region")),
                            c.getString(c.getColumnIndex("country")),
                            c.getString(c.getColumnIndex("lang"))
                    );
                    rssFeedsMap.put(feed.link, feed);
                } while (c.moveToNext());
            }
            c.close();
        }
    	return rssFeedsMap;
	}
	
	@Override
	public void onShowSettings(String service_package) {
		log("onShowSettings");
		Intent intent=new Intent(SpeindDataFeed.this, RSSSettings.class);
		intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);		
		intent.putExtra("profile", curProfile);
		intent.putStringArrayListExtra("langs", speindDFData.langList);
		getApplication().startActivity(intent);
	}
	
	@Override
	public void onSetProfile(String service_package, String profile) {
        log("onSetProfile " + profile);
		curProfile=profile;
        final SharedPreferences settings = getSharedPreferences(SpeindDataFeed.PREFS_NAME, 0);

        if (settings.getBoolean("needLoadFromSettings", false)) {
            final Runnable r = new Runnable() {
                @Override
                public void run() {
                    SQLiteDatabase db = DatabaseManager.getInstance().getWritableDatabase();
                    loadRSSFeeds(SpeindDataFeed.this, true, db, curProfile, "feed");
                    loadRSSFeeds(SpeindDataFeed.this, true, db, curProfile, "user_feed");
                    final SharedPreferences settings = getSharedPreferences(SpeindDataFeed.PREFS_NAME, 0);
                    SharedPreferences.Editor editor = settings.edit();
                    editor.putBoolean("needLoadFromSettings", false);
                    editor.apply();

                }
            };
            if (updateWorker.mHandler!=null) {
                updateWorker.mHandler.post(r);
            } else {
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        updateWorker.mHandler.post(r);
                    }
                }, 5000);
            }
        }
	}
  
	@Override
	public void onStop(String service_package) {
		log("onStop");
		isStopReceived=true;
		handler.removeCallbacks(updateNewsRunnable);
		this.stopSelf();
	}

	@Override
	public IBinder onBind(Intent arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void onInfoPointDetails(String service_package, InfoPoint arg0) {
		log("onInfoPointDetails");
		// TODO Auto-generated method stub
	}

	@Override
	public void onLike(String service_package, InfoPoint arg0) {
		log("onLike");
		// TODO Auto-generated method stub
	}

	@Override
	public void onLangListChanged(String service_package, ArrayList<String> langList) {
		log("onLangListChanged");
		langListChanged=true;
		if (!curProfile.equals("")) {
			refreshRssNews();
		}
	}
	
	private boolean needRefreshRssList() {
		log("needRefreshRssList");
		SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
		long lastFeedsListRefreshDate = settings.getLong(curProfile+"_lastFeedListRefresDate", 0);
		
		int feedsCount = settings.getInt(curProfile+"_feeds_count", 0);
		
		return (((new Date()).getTime()-lastFeedsListRefreshDate)>12*60*60*1000)||langListChanged||(feedsCount==0);
	}
	
	private Map<String, RSSFeed> refreshRssList(Map<String, RSSFeed> rssFeedsMap, SQLiteDatabase db, String profile) {
		if (profile.equals("")) return rssFeedsMap;
		log("refreshRssList: Start");

        db.beginTransactionNonExclusive();

		SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
    	ArrayList<RSSFeed> rssFeedsAr=new ArrayList<>();

        if ("com.maple.speind".equals(getPackageName())) {
            Map<String, String> feedListFiles=RSSFeed.getFeedsListFromCSVURL(FEEDS_LIST_URL);

            for (Map.Entry<String, String> entry : feedListFiles.entrySet()) {
                log("getFeedsFromCSVURL: "+entry.getKey()+" "+entry.getValue());
                if (checkLocale(entry.getKey())) {
                    log("getFeedsFromCSVURL checkLocale: "+entry.getValue());
                    rssFeedsAr.addAll(RSSFeed.getFeedsFromCSVURL(entry.getValue()));
                }
            }
        } else if ("ru.lifenews.speind".equals(getPackageName())) {
            rssFeedsAr.addAll(RSSFeed.getFeedsFromCSVURL("http://speind.me/rsssources/lifenews.csv"));
        }


		if (rssFeedsAr.size()>0) {
	        SharedPreferences.Editor editor = settings.edit();
	        editor.putLong(profile+"_lastFeedListRefresDate", (new Date()).getTime());	 
	        editor.apply();
		}
		   	
		for (RSSFeed rssFeed : rssFeedsAr) {
	    	if (SpeindDataFeed.this.isStopReceived) return rssFeedsMap;
			if (rssFeedsMap.containsKey(rssFeed.link.trim())) {
				RSSFeed rssFeedl=rssFeedsMap.get(rssFeed.link.trim());
                if (!rssFeedl.category.equals(rssFeed.category.trim())||
                    !rssFeedl.parentcategory.equals(rssFeed.parentcategory.trim())||
                    !rssFeedl.city.equals(rssFeed.city.trim())||
                    !rssFeedl.country.equals(rssFeed.country.trim())||
                    !rssFeedl.lang.equals(rssFeed.lang.trim().toLowerCase())||
                    !rssFeedl.link.equals(rssFeed.link.trim())||
                    !rssFeedl.provider.equals(rssFeed.provider.trim())||
                    !rssFeedl.region.equals(rssFeed.region.trim())||
                    !rssFeedl.vocalizing.equals(rssFeed.vocalizing.trim())
                ) {
                    rssFeedl.category=rssFeed.category.trim();
                    rssFeedl.parentcategory=rssFeed.parentcategory.trim();
                    rssFeedl.city=rssFeed.city.trim();
                    rssFeedl.country=rssFeed.country.trim();
                    rssFeedl.lang=rssFeed.lang.trim().toLowerCase();
                    rssFeedl.link=rssFeed.link.trim();
                    rssFeedl.provider=rssFeed.provider.trim();
                    rssFeedl.region=rssFeed.region.trim();
                    rssFeedl.vocalizing=rssFeed.vocalizing.trim();

                    ContentValues cv = new ContentValues();
                    cv.put("city", rssFeedl.city);
                    cv.put("provider", rssFeedl.provider);
                    cv.put("category", rssFeedl.category);
                    cv.put("parentcategory", rssFeedl.parentcategory);
                    cv.put("vocalizing", rssFeedl.vocalizing);
                    cv.put("region", rssFeedl.region);
                    cv.put("country", rssFeedl.country);
                    cv.put("lang", rssFeedl.lang);
                    db.update("rsssources", cv, "profile = ? and link = ?", new String[] { profile, rssFeedl.link });
                }
			} else {
                if ("com.maple.speind".equals(getPackageName())) {
                    if (rssFeed.provider.equals("Speind")||
                            rssFeed.provider.trim().equals("meduza.io")||
                            rssFeed.link.trim().compareToIgnoreCase("http://feeds.bbci.co.uk/news/world/rss.xml")==0||
                            rssFeed.link.trim().compareToIgnoreCase("http://ep00.epimg.net/rss/internacional/portada.xml")==0||
                            rssFeed.link.trim().compareToIgnoreCase("http://www.leparisien.fr/international/rss.xml")==0
                        ) {
                        Locale current = getResources().getConfiguration().locale;
                        if (rssFeed.lang.compareToIgnoreCase(current.getLanguage())==0) {
                            rssFeed.enabled=true;
                        }
                    }
                } else if ("ru.lifenews.speind".equals(getPackageName())) {
                    rssFeed.enabled=true;
                }

				rssFeed.lang=rssFeed.lang.toLowerCase();

                ContentValues cv = new ContentValues();
                cv.put("profile", profile);
                cv.put("link", rssFeed.link);
                cv.put("city", rssFeed.city);
                cv.put("provider", rssFeed.provider);
                cv.put("category", rssFeed.category);
                cv.put("parentcategory", rssFeed.parentcategory);
                cv.put("vocalizing", rssFeed.vocalizing);
                cv.put("enabled", (rssFeed.enabled ? 1 : 0));
                cv.put("region", rssFeed.region);
                cv.put("country", rssFeed.country);
                cv.put("lang", rssFeed.lang);
                cv.put("level", "feed");
                db.insert("rsssources", null, cv);

				rssFeedsMap.put(rssFeed.link.trim(), rssFeed);
			}			
		} 

        db.setTransactionSuccessful();
		db.endTransaction();

		Intent intent = new Intent(BROADCAST_ACTION).putExtra("cmd", 0);
		intent.putStringArrayListExtra("langs", speindDFData.langList);
		sendBroadcast(intent);   
				
		log("refreshRssList: End");

		return rssFeedsMap;
	}
	
	private void processFeedInfopoints(ArrayList<RssItem> newItems, boolean readArticle) {
		log("processFeedInfopoints");
		Collections.sort(newItems, new Comparator<RssItem>() {
	        @Override
	        public int compare(RssItem rssItem1, RssItem rssItem2) {			        							
	        	return (int)(rssItem2.getPubDate().getTime()/1000-rssItem1.getPubDate().getTime()/1000);
	        }
	    });
	    for (int i=newItems.size()-1;i>=0;i--) {
	    	if (isStopReceived||isSuspended||processingUpdateList) {
	    		log("refreshRssNews: Cancel");
	    		return;
	    	}
	    	final RssItem newItem = newItems.get(i);
	    	if (((new Date()).getTime()-newItem.getPubDate().getTime())>receiver.speindData.getMaxStoreInfopointTime()) continue;
	    	
	    	if (isStopReceived||isSuspended||processingUpdateList) {
	    		log("refreshRssNews: Cancel");
	    		return;
	    	}

	    	Configuration conf = getResources().getConfiguration();
	    	Locale localeOld=conf.locale;
	    	conf.locale = new Locale(newItem.getLang());
	    	Resources resources = new Resources(getAssets(), getResources().getDisplayMetrics(), conf);
	    	String detailsString = resources.getString(R.string.details);
	    	String reportString = resources.getString(R.string.report_what);
	    	conf.locale=localeOld;
	    	new Resources(getAssets(), getResources().getDisplayMetrics(), conf);
	    	
    		java.util.Random r = new  java.util.Random();
	        int pos = r.nextInt(3)+1;
	    	
	    	SpeindAPI.SendInfoPointParams sendInfoPointParams=new SpeindAPI.SendInfoPointParams(); 
	    	
	    	sendInfoPointParams.senderBmp=null;
	    	sendInfoPointParams.senderBmpURL=newItem.getSenderBmpURL();
	    	
        	sendInfoPointParams.postBmp=BitmapFactory.decodeResource(getResources(), ((pos == 1) ? R.drawable.rss1 : (pos == 2) ? R.drawable.rss2 : R.drawable.rss2));
	    	sendInfoPointParams.postBmpURL= newItem.getBmpURL();

	    	sendInfoPointParams.postTime=newItem.getPubDate();
	    	sendInfoPointParams.postSender= newItem.getSender();
	    	sendInfoPointParams.postTitle = newItem.getTitle();
	    	sendInfoPointParams.postOriginText=newItem.getDescription();
	    	sendInfoPointParams.postLink =newItem.getLink();
            sendInfoPointParams.postURL=newItem.getLink();
	    	sendInfoPointParams.postPluginData="";
	    				    				    	
	    	sendInfoPointParams.postSenderVocalizing=newItem.providerVocalizing+" "+reportString;
	    	sendInfoPointParams.postTitleVocalizing=newItem.getTitle();
	    	sendInfoPointParams.postTextVocalizing=""+Html.fromHtml("<p>"+detailsString+"</p><p>"+newItem.getDescription()+"</p>");
	    	if (newItem.getDescription().replaceAll("[^\\w]*", "").equals("")) sendInfoPointParams.postTextVocalizing="";
	    	sendInfoPointParams.lang=newItem.getLang();	   
	    	sendInfoPointParams.readArticle=readArticle;

	    	if (isStopReceived||isSuspended||processingUpdateList) {
	    		log("refreshRssNews: Cancel");
	    		return;
	    	}			    	
	    	sendInfoPoint(getPackageName(), sendInfoPointParams);
	    }		
	}
	
	private void refreshRssNews(final Map<String, RSSFeed> rssFeedsMap) {
		log("refreshRssNews: Start");		
		SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);		
		SharedPreferences.Editor editor = settings.edit();
		editor.putLong("lastRefreshTime", (new Date()).getTime());
		editor.apply();
	
    	Iterator<String> myVeryOwnIterator = rssFeedsMap.keySet().iterator();
		while(myVeryOwnIterator.hasNext()) {
	    	if (isStopReceived||isSuspended||processingUpdateList) {
	    		log("refreshRssNews: Cancel ["+isStopReceived+", "+isSuspended+", "+processingUpdateList+"]");
	    		return;
	    	}
			String key=myVeryOwnIterator.next();
		    RSSFeed feed=rssFeedsMap.get(key);
			if (!feed.enabled) continue;
			if (!checkLocale(feed.lang)) continue;
	    	log("rssFeedsGet: "+key);
			processFeedInfopoints(RssItem.getRssItems(feed.link, feed.vocalizing, feed.lang, feed.provider, feed.category), feed.provider.trim().equals("meduza.io"));	    			
		}
		
		Map<String, RSSFeed> rssFeedsMap1=loadRSSFeeds(SpeindDataFeed.this, false, DatabaseManager.getInstance().getWritableDatabase(), curProfile, "user_feed");
    	myVeryOwnIterator = rssFeedsMap1.keySet().iterator();
		while(myVeryOwnIterator.hasNext()) {
	    	if (isStopReceived||isSuspended||processingUpdateList) {
	    		log("refreshRssNews: Cancel");
	    		return;
	    	}
			String key=myVeryOwnIterator.next();
		    RSSFeed feed=rssFeedsMap1.get(key);
			if (!feed.enabled) continue;
			if (!checkLocale(feed.lang)) continue;
			processFeedInfopoints(RssItem.getRssItems(feed.link, feed.vocalizing, feed.lang, feed.provider, ""), false);   			
		}
	    log("refreshRssNews: End");
	}

	@Override
	public void onLoadImagesOnMobileInetChanged(String service_package, boolean arg0) {
		log("onLoadImagesOnMobileInetChanged "+arg0);
	}

	@Override
	public void onResume(String service_package) {
		log("onResume");
		isSuspended=false;
		refreshRssNews();
	}

	@Override
	public void onStart(String service_package, int state) {
		log("onStart");
        isSuspended = (state != SpeindAPI.DataFeedSettingsInfo.DATAFEED_STATE_READY);
		refreshRssNews();
	}

	@Override
	public void onSuspend(String service_package) {
		log("onSuspend");
		isSuspended=true;
		handler.removeCallbacks(updateNewsRunnable);
	}

	@Override
	public void onStoreInfopointTimeChanged(String service_package, long arg0) {
		log("onStoreInfopointTimeChanged " + arg0);
	}

    @Override
    public boolean isAuthorized(String servicePackage) {
        return true;
    }

    @Override
    public void onPost(String service_package, InfoPoint arg0) {}


}
