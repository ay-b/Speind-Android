package com.maple.speind;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Vector;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.DownloadManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ServiceInfo;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.media.AudioManager;
import android.media.AudioManager.OnAudioFocusChangeListener;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.os.RemoteException;
import android.provider.MediaStore;
import android.support.v4.app.NotificationCompat;
import android.support.v4.util.LongSparseArray;
import android.text.Html;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;

import com.android.vending.billing.IInAppBillingService;
import com.maple.rssreceiver.RssItem;
import com.maple.rssreceiver.UserFeeds;

import com.maple.speind.ITextReader.IReaderEventsCallback;

import me.speind.SpeindAPI;
import me.speind.SpeindAPI.DataFeedSettingsInfo;
import me.speind.SpeindAPI.InfoPoint;
import com.yandex.metrica.YandexMetrica;

//import ru.lifenews.speind.R;

public class SpeindService extends Service implements IReaderEventsCallback, OnCompletionListener, OnAudioFocusChangeListener {
    private static final boolean isDebug = true;

	public static final String PREFS_NAME = "appConfig";
	public static final String DOWNLOAD_PATH = "http://speind.me/";

	private LooperThread worker;
	
	private final Handler handler = new Handler();
    private WakeLock wakeLock = null;

	private int state=SpeindAPI.SPEIND_STATE_INITIALIZATION;		
	private int onPauseState=-1;
    private Boolean needResume = false;
    private boolean readFull=false;      
	private boolean playerOnly=false;

    private DownloadManager downloadManager=null;
    private LongSparseArray<String> downloadingVoices = null;
	private final Runnable updateDownloadProgressRunnable = new Runnable() { public void run() { updateDownloadProgress(); } };
    
	private ITextReader reader = null;
	private MediaPlayer backgroundPlayer = null;	
	private MediaPlayer player = null;
    private Vector<String> playerPaths = new Vector<>();
	private String currentPlayFile = "";
	private String nextPlayFile = "";
	private final Runnable updatePositionRunnable = new Runnable() { public void run() { updatePosition(); } };
	
	private long readerPlayTime=0;
	private long lastReaderStartTime=0;
	private long playerPlayTime=0;
	private long lastPlayerStartTime=0;

    private DBHelper dbHelper;
	private SpeindAPI.SpeindData speindData=new SpeindAPI.SpeindData();
    private ArrayList<InfoPoint> pinboard = new ArrayList<>();

    private String prevSender="";

	private boolean haveAudioFocus=false;
	
	private BroadcastReceiver connectionChangeReceiver=null;
	private boolean connectionChangeReceiverRegistered = false;
	private boolean haveNetworkConnection=false;
	
	public static final int BILLING_RESPONSE_RESULT_OK = 0;
	public static final String ITEM_TYPE_INAPP = "inapp";
    // Keys for the responses from InAppBillingService
    public static final String RESPONSE_CODE = "RESPONSE_CODE";
    //public static final String RESPONSE_GET_SKU_DETAILS_LIST = "DETAILS_LIST";
    public static final String RESPONSE_BUY_INTENT = "BUY_INTENT";
    public static final String RESPONSE_INAPP_PURCHASE_DATA = "INAPP_PURCHASE_DATA";
    public static final String RESPONSE_INAPP_SIGNATURE = "INAPP_DATA_SIGNATURE";
    public static final String RESPONSE_INAPP_ITEM_LIST = "INAPP_PURCHASE_ITEM_LIST";
    public static final String RESPONSE_INAPP_PURCHASE_DATA_LIST = "INAPP_PURCHASE_DATA_LIST";
    public static final String RESPONSE_INAPP_SIGNATURE_LIST = "INAPP_DATA_SIGNATURE_LIST";
    public static final String INAPP_CONTINUATION_TOKEN = "INAPP_CONTINUATION_TOKEN";
    // IAB Helper error codes
    public static final int IABHELPER_ERROR_BASE = -1000;
    //public static final int IABHELPER_REMOTE_EXCEPTION = -1001;
    //public static final int IABHELPER_BAD_RESPONSE = -1002;
    //public static final int IABHELPER_VERIFICATION_FAILED = -1003;
    //public static final int IABHELPER_SEND_INTENT_FAILED = -1004;
    //public static final int IABHELPER_USER_CANCELLED = -1005;
    //public static final int IABHELPER_UNKNOWN_PURCHASE_RESPONSE = -1006;
    //public static final int IABHELPER_MISSING_TOKEN = -1007;
    //public static final int IABHELPER_UNKNOWN_ERROR = -1008;

	private String googlePlaySignatureBase64="MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAxVoh8EmYt9buvrBMlTvt0F4edPqM7nW3Dd6M7PH9ZJ3tWIB0oecC7w6ymteBBsTX3bWeOjOkB+s1cOOMgPUXG6hzjOQ8EtdyfoAz2jZxMs3QlHOrjS3EJpsXJLkzYB5zuVc+EMEIAsPOq4FGy11cCXP7tmGNN5Xsw0/dpur/1NuUrMZIQFjLDBvUA0inijIQsN/fLOWNHtyfNkodc56Kkn87/k5Yg9Ce+e3q2BJUCfZC2IyEmA5DLmQ8U3gAoYg25cXNtmEvat7Q9w2J1ICIrsFOxNupXW6t7nnbqW9N+UZJUKYRjbO5MeJe1zvzVdsIMl4E3m0Qe9nnIR0+zuXRYwIDAQAB";
	private IInAppBillingService googlePlayService=null;
	private boolean googlePlaySetupDone=false;
	
	private Runnable playerNextRunnable = new Runnable() {
		@Override
		public void run() {
			handler.removeCallbacks(playerNextRunnable);
            SpeindApplication.reportEvent("Engine", "Player not report onCompletion", "");
			if (state!=SpeindAPI.SPEIND_STATE_PLAY_READER&&state!=SpeindAPI.SPEIND_STATE_STOP_READER) {
	    		SpeindAPI.next(SpeindService.this, getPackageName());
			}
		}
	};
	
    ServiceConnection googlePlayServiceConn = new ServiceConnection() {
    	@Override
    	public void onServiceDisconnected(ComponentName name) {
    		worker.mHandler.post(new Runnable(){
    			@Override
    			public void run() {
    				googlePlayService = null;
    			}
    		});
    	}
	    @Override
	    public void onServiceConnected(ComponentName name, final IBinder service) {
	    	worker.mHandler.post(new Runnable(){
				@Override
				public void run() {
	    	
				   googlePlayService = IInAppBillingService.Stub.asInterface(service);
				   String packageName = getPackageName();
		           try {
		               int response = googlePlayService.isBillingSupported(3, packageName, ITEM_TYPE_INAPP);
		               if (response != BILLING_RESPONSE_RESULT_OK) {
		                   //if (listener != null) listener.onIabSetupFinished(new IabResult(response, "Error checking for billing v3 support."));
		                   return;
		               }
		               googlePlaySetupDone = true;
		               checkPurchases(); 
		           } catch (RemoteException e) {
		               //if (listener != null) listener.onIabSetupFinished(new IabResult(IABHELPER_REMOTE_EXCEPTION, "RemoteException while setting up in-app billing."));
		               //e.printStackTrace();
		           }
		           //if (listener != null) listener.onIabSetupFinished(new IabResult(BILLING_RESPONSE_RESULT_OK, "Setup successful."));
				}
	    	});
	    }
    };

    private class LooperThread extends Thread {
        public Handler mHandler;
        public void run() {
        	setName("SSworker");
            Looper.prepare();
            mHandler = new Handler();
            handler.post(new Runnable(){
				@Override
				public void run() {
					onReady();
				}
            });
            Looper.loop();
        }
    }

    class DBHelper extends SQLiteOpenHelper {

        public DBHelper(Context context) {
            super(context, "speindDB", null, 2);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL( "CREATE TABLE speindinfopoints (" +
                        "  id text," +
                        "  position integer," +
                        "  profile text," +
                        "  processingPlugin text," +
                        "  postTime integer," +
                        "  priority integer," +
                        "  readArticle integer," +
                        "  titleExists integer," +
                        "  textExists integer," +
                        "  articleExists integer," +
                        "  PRIMARY KEY (profile, id)"+
                        ");"
            );
            db.execSQL( "CREATE TABLE speindpinboard (" +
                        "  id text," +
                        "  position integer," +
                        "  profile text," +
                        "  processingPlugin text," +
                        "  postTime integer," +
                        "  priority integer," +
                        "  readArticle integer," +
                        "  titleExists integer," +
                        "  textExists integer," +
                        "  articleExists integer," +
                        "  PRIMARY KEY (profile, id)"+
                        ");"
            );
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            switch (oldVersion) {
                case 1:
                    db.execSQL( "CREATE TABLE speindpinboard (" +
                            "  id text," +
                            "  position integer," +
                            "  profile text," +
                            "  processingPlugin text," +
                            "  postTime integer," +
                            "  priority integer," +
                            "  readArticle integer," +
                            "  titleExists integer," +
                            "  textExists integer," +
                            "  articleExists integer," +
                            "  PRIMARY KEY (profile, id)"+
                            ");"
                    );
                    break;
            }
        }

    }

    @Override
    public void onCreate() {   
    	//Thread.currentThread().setName("mainThread");
    	log("onCreate");
    	worker = new LooperThread();
    	worker.start();
    }
    
    public void onReady() {	
    	worker.mHandler.post(new Runnable() {
            @Override
            public void run() {
                // Create DBHelper
                dbHelper = new DBHelper(SpeindService.this);
                // Prepare Download Manager
                downloadManager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
                downloadingVoices = loadDownloadingVoices(SpeindService.this);
                if (downloadingVoices == null) downloadingVoices = new LongSparseArray<>();
                fixDownloadingVoices();
                // Unpucking data for language detection
                unpuckLangData();
                // Init TTS engine
                reader = new CompositeReader(SpeindService.this, SpeindService.this, 0);
                reader.init();
                // Prepare music list
                refrashPlaylist();
                // Prepare music player
                player = new MediaPlayer();
                player.setOnCompletionListener(SpeindService.this);
                player.reset();
                // Prepare background music player
                backgroundPlayer = new MediaPlayer();
                backgroundPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                    @Override
                    public void onCompletion(MediaPlayer mp) {
                        stopBackgroundMusic();
                        playBackgroundMusic();
                    }
                });
                // Preparing wakelock for work in background
                PowerManager mgr = (PowerManager) getSystemService(Context.POWER_SERVICE);
                wakeLock = mgr.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "SpeindWakeLock");
                // Init InApp billing service
                bindService((new Intent("com.android.vending.billing.InAppBillingService.BIND")).setPackage("com.android.vending"), googlePlayServiceConn, Context.BIND_AUTO_CREATE);

                ConnectivityManager aConnectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
                NetworkInfo aNetworkInfo = aConnectivityManager.getActiveNetworkInfo();
                haveNetworkConnection = (aNetworkInfo != null && aNetworkInfo.isConnected());
                connectionChangeReceiver = new BroadcastReceiver() {
                    @Override
                    public void onReceive(Context context, Intent intent) {
                        ConnectivityManager aConnectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
                        NetworkInfo aNetworkInfo = aConnectivityManager.getActiveNetworkInfo();
                        haveNetworkConnection = (aNetworkInfo != null && aNetworkInfo.isConnected());
                        log("Network state changed: " + haveNetworkConnection);
                        if (reader.onNetworkStateChanged()) sendVoicesDataChanged();
                        if (haveNetworkConnection) {
                            if (state > SpeindAPI.SPEIND_STATE_PREPARING) {
                                processSharedQueue();
                            }
                        }
                    }
                };

                IntentFilter ifilter = new IntentFilter("android.net.conn.CONNECTIVITY_CHANGE");
                registerReceiver(connectionChangeReceiver, ifilter);
                connectionChangeReceiverRegistered = true;

            }
        });
   }

    @Override
    public void onDestroy() {
    	if (connectionChangeReceiverRegistered) unregisterReceiver(connectionChangeReceiver);
    	log("OnDestroy");  	
    	if (wakeLock!=null&&wakeLock.isHeld()) wakeLock.release();
    	freeAudioFocus();
        if (googlePlayServiceConn != null && googlePlayService!=null) {
            unbindService(googlePlayServiceConn);
            googlePlayServiceConn = null;
        }    	
    	onStop();
    	stopPlugins();		
    	if (reader!=null) reader.unload();
    	stopForeground(true);
    }
    
	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}	
	
	@Override
    public int onStartCommand(final Intent intent, final int flags, final int startId) {
		if (intent!=null) {
			if (worker.mHandler==null) {
				handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        onStartCommand(intent, flags, startId);
                    }
                }, 1000);
			} else {
		    	worker.mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        log("onStartCommand: " + intent.getIntExtra(SpeindAPI.SERVICE_CMD, -1));
                        int cmd = intent.getIntExtra(SpeindAPI.SERVICE_CMD, -1);
                        if (cmd >= 0) {

                            if (state > SpeindAPI.SPEIND_STATE_NEED_PROFILE) requestAudioFocus();

                            switch (cmd) {
                                case SpeindAPI.SC_QUIT:
                                    log("SC_QUIT");
                                    onQuit();
                                    break;
                                case SpeindAPI.SC_VOICES_SETTINGS_REQUEST:
                                    log("SC_VOICES_SETTINGS_REQUEST");
                                    onVocesSettingsRequest();
                                    break;
                                case SpeindAPI.SC_BUY_VOICE_REQUEST:
                                    log("SC_BUY_VOICE_REQUEST");
                                    onBuyVoice(intent.getStringExtra(SpeindAPI.PARAM_VOICE_CODE));
                                    break;
                                case SpeindAPI.SC_BUY_VOICE_RESULT:
                                    log("SC_BUY_VOICE_RESULT");
                                    onBuyVoiceResult(intent);
                                    break;
                                case SpeindAPI.SC_DOWNLOAD_VOICE_REQUEST:
                                    log("SC_DOWNLOAD_VOICE_REQUEST");
                                    String voiceCode = intent.getStringExtra(SpeindAPI.PARAM_VOICE_CODE);
                                    onDownloadVoice(voiceCode, intent.getBooleanExtra(SpeindAPI.PARAM_WIFI_ONLY, false));
                                    break;
                                case SpeindAPI.SC_RESTORE_PURCHASES:
                                    log("SC_RESTORE_PURCHASES");
                                    onRestorePurchases(intent.getBooleanExtra(SpeindAPI.PARAM_WIFI_ONLY, false));
                                    break;
                                case 1000:
                                    long id = intent.getLongExtra("param_download_id", -1);
                                    if (id > -1) {
                                        onVoiceDownloadResult(id);
                                    }
                                    break;
                                case SpeindAPI.SC_SET_DEFAULT_VOICE_REQUEST:
                                    log("SC_SET_DEFAULT_VOICE_REQUEST");
                                    String code = intent.getStringExtra(SpeindAPI.PARAM_VOICE_CODE);
                                    onSetDefaultVoice(code);
                                    break;
                                case SpeindAPI.SC_GET_STATE:
                                    log("SC_INIT_UI");
                                    showNotification();
                                    onGetState();
                                    break;
                                case SpeindAPI.SC_SET_PROFILE:
                                    log("SC_SET_PROFILE");
                                    if (state == SpeindAPI.SPEIND_STATE_NEED_PROFILE) {
                                        String profile = intent.getStringExtra(SpeindAPI.PARAM_PROFILE_NAME);
                                        String pass = intent.getStringExtra(SpeindAPI.PARAM_PROFILE_PASS);
                                        onSetProfile(profile, pass);
                                    }
                                    break;
                                case SpeindAPI.SC_PLAY:
                                    log("SC_PLAY");
                                    if (state > SpeindAPI.SPEIND_STATE_NEED_PROFILE) {
                                        onPlay();
                                    }
                                    break;
                                case SpeindAPI.SC_PLAY_PAUSE:
                                    log("SC_PLAY_PAUSE");
                                    if (state > SpeindAPI.SPEIND_STATE_NEED_PROFILE) {
                                        onPlayPause();
                                    }
                                    break;
                                case SpeindAPI.SC_PLAY_INFOPOINT:
                                    log("SC_PLAY_INFOPOINT");
                                    if (state > SpeindAPI.SPEIND_STATE_NEED_PROFILE) {
                                        int pos = intent.getIntExtra(SpeindAPI.PARAM_INFOPOINT_POS, -1);
                                        if (pos > -1)
                                            onPlayInfoPoint(pos);
                                    }
                                    break;
                                case SpeindAPI.SC_PLAY_USER_FILE:
                                    log("SC_PLAY_USER_FILE");
                                    if (state > SpeindAPI.SPEIND_STATE_NEED_PROFILE) {
                                        String fileName = intent.getStringExtra(SpeindAPI.PARAM_FILE_NAME);
                                        onPlayUserFile(fileName);
                                    }
                                    break;
                                case SpeindAPI.SC_PLAY_USER_TEXT:
                                    log("SC_PLAY_USER_TEXT");
                                    if (state > SpeindAPI.SPEIND_STATE_NEED_PROFILE) {
                                        String text = intent.getStringExtra(SpeindAPI.PARAM_USER_TEXT);
                                        onPlayUserText(text);
                                    }
                                    break;
                                case SpeindAPI.SC_STOP:
                                    log("SC_STOP " + state);
                                    if (state > SpeindAPI.SPEIND_STATE_NEED_PROFILE)
                                        onStop();
                                    break;
                                case SpeindAPI.SC_PAUSE:
                                    log("SC_PAUSE " + state);
                                    if (state > SpeindAPI.SPEIND_STATE_NEED_PROFILE)
                                        onPause();
                                    break;
                                case SpeindAPI.SC_RESUME:
                                    log("SC_RESUME " + state);
                                    if (state > SpeindAPI.SPEIND_STATE_NEED_PROFILE)
                                        onResume();
                                    break;
                                case SpeindAPI.SC_READ_CURRENT_INFOPOINT_ARTICLE:
                                    log("SC_READ_CURRENT_INFOPOINT_ARTICLE");
                                    if (state > SpeindAPI.SPEIND_STATE_NEED_PROFILE) {
                                        onReadCurrentInfopointArticle();
                                    }
                                    break;
                                case SpeindAPI.SC_NEXT:
                                    log("SC_NEXT");
                                    if (state > SpeindAPI.SPEIND_STATE_NEED_PROFILE)
                                        onNext();
                                    break;
                                case SpeindAPI.SC_PREV:
                                    log("SC_PREV");
                                    if (state > SpeindAPI.SPEIND_STATE_NEED_PROFILE)
                                        onPrev();
                                    break;
                                case SpeindAPI.SC_REPLAY:
                                    log("SC_REPLAY");
                                    if (state > SpeindAPI.SPEIND_STATE_NEED_PROFILE)
                                        onReplay();
                                    break;
                                case SpeindAPI.SC_SKIPNEWS:
                                    log("SC_SKIPNEWS");
                                    if (state > SpeindAPI.SPEIND_STATE_NEED_PROFILE) {
                                        onSkipNews();
                                    }
                                    break;
                                case SpeindAPI.SC_SETTINGS_REQUEST:
                                    log("SC_SETTINGS_REQUEST");
                                    if (state > SpeindAPI.SPEIND_STATE_NEED_PROFILE)
                                        onSettingsRequest();
                                    break;
                                case SpeindAPI.SC_PINBOARD_REQUEST:
                                    log("SC_PINBOARD_REQUEST");
                                    if (state > SpeindAPI.SPEIND_STATE_NEED_PROFILE)
                                        onPinboardRequest();
                                    break;
                                case SpeindAPI.SC_PINBOARD_SETTINGS_REQUEST:
                                    log("SC_PINBOARD_SETTINGS_REQUEST");
                                    if (state > SpeindAPI.SPEIND_STATE_NEED_PROFILE)
                                        onPinboardSettingsRequest();
                                    break;
                                case SpeindAPI.SC_ADD_TO_PINBOARD:
                                    log("SC_ADD_TO_PINBOARD");
                                    if (state > SpeindAPI.SPEIND_STATE_NEED_PROFILE)
                                        onAddToPinboard(intent.getStringExtra(SpeindAPI.PARAM_INFOPOINT_ID));
                                    break;
                                case SpeindAPI.SC_REMOVE_FROM_PINBOARD:
                                    log("SC_REMOVE_FROM_PINBOARD");
                                    if (state > SpeindAPI.SPEIND_STATE_NEED_PROFILE)
                                        onRemoveFromPinboard(intent.getStringExtra(SpeindAPI.PARAM_INFOPOINT_ID));
                                    break;
                                case SpeindAPI.SC_CLEAR_PINBOARD:
                                    log("SC_CLEAR_PINBOARD");
                                    if (state > SpeindAPI.SPEIND_STATE_NEED_PROFILE)
                                        onClearPinboard();
                                    break;
                                case SpeindAPI.SC_SEND_PINBOARD:
                                    log("SC_SEND_PINBOARD: " + intent.getBooleanExtra(SpeindAPI.PARAM_CLEAR, true));
                                    if (state > SpeindAPI.SPEIND_STATE_NEED_PROFILE)
                                        onSendPinboard(intent.getStringExtra(SpeindAPI.PARAM_EMAIL), intent.getIntExtra(SpeindAPI.PARAM_FORMAT, SpeindAPI.SPEIND_EMAIL_FORMAT_HTML), intent.getBooleanExtra(SpeindAPI.PARAM_CLEAR, true));
                                    break;
                                case SpeindAPI.SC_SETTINGS_CHANGED:
                                    log("SC_SETTINGS_CHANGED");
                                    if (state > SpeindAPI.SPEIND_STATE_NEED_PROFILE) {
                                        SpeindAPI.SpeindSettings config = SpeindAPI.SpeindSettings.getFromIntent(intent);
                                        if (config != null) onSettingsChanged(config);
                                    }
                                    break;
                                case SpeindAPI.SC_DATA_FEED_SETTINGS_REQUEST:
                                    log("SC_DATA_FEED_SETTINGS_REQUEST");
                                    if (state > SpeindAPI.SPEIND_STATE_NEED_PROFILE)
                                        onDataFeedSettingsRequest(DataFeedSettingsInfo.getFromIntent(intent));
                                    break;
                                case SpeindAPI.SC_DATA_FEED_RESUME_REQUEST:
                                    log("SC_DATA_FEED_RESUME_REQUEST");
                                    if (state > SpeindAPI.SPEIND_STATE_NEED_PROFILE)
                                        onDataFeedResumeRequest(DataFeedSettingsInfo.getFromIntent(intent));
                                    break;
                                case SpeindAPI.SC_DATA_FEED_SUSPEND_REQUEST:
                                    log("SC_DATA_FEED_SUSPEND_REQUEST");
                                    if (state > SpeindAPI.SPEIND_STATE_NEED_PROFILE)
                                        onDataFeedSuspendRequest(DataFeedSettingsInfo.getFromIntent(intent));
                                    break;
                                case SpeindAPI.SC_MEDIA_BUTTON_PRESS:
                                    log("SC_MEDIA_BUTTON_PRESS");
                                    if (state > SpeindAPI.SPEIND_STATE_NEED_PROFILE) {
                                        onMediaButtonPress(intent.getIntExtra(SpeindAPI.PARAM_MEDIA_BUTTON_PRESS_COUNT, 0));
                                    }
                                    break;
                                case SpeindAPI.SC_PACKAGE_CHANGED:
                                    log("SC_PACKAGE_CHANGED");
                                    initPlugins();
                                    break;
                                case SpeindAPI.SC_PLAYER_ONLY:
                                    log("SC_PLAYER_ONLY");
                                    onSetPlayerOnly(intent.getBooleanExtra(SpeindAPI.PARAM_PLAYER_ONLY, false));
                                    break;
                                case SpeindAPI.SC_LIKE:
                                    log("SC_LIKE");
                                    onLike(SpeindAPI.InfoPoint.getFromIntent(intent));
                                    break;
                                case SpeindAPI.SC_POST:
                                    log("SC_POST");
                                    onPost(SpeindAPI.InfoPoint.getFromIntent(intent));
                                    break;
                                case 1001:
                                    onOpenMainActivity();
                                    break;
                                case 1002:
                                    onSharedText(intent.getStringExtra("textData"));
                                    break;
                            }
                        }
                        cmd = intent.getIntExtra(SpeindAPI.DATAFEED_SERVICE_CMD, -1);
                        if (cmd >= 0) {
                            if (state > SpeindAPI.SPEIND_STATE_NEED_PROFILE) {
                                switch (cmd) {
                                    case SpeindAPI.DF_SC_DATAFEED_INFO:
                                        log("DF_SC_DATAFEED_INFO");
                                        onAddPluginSettingsName(SpeindAPI.DataFeedSettingsInfo.getFromIntent(intent));
                                        break;
                                    case SpeindAPI.DF_SC_NEW_INFO_POINT:
                                        log("DF_SC_NEW_INFO_POINT");
                                        onPluginAddInfoPiont(intent);
                                        break;
                                    case SpeindAPI.DF_DATAFEED_INFO_CHANGED:
                                        log("DF_DATAFEED_INFO_CHANGED");
                                        onDataFeedInfoChanged(SpeindAPI.DataFeedSettingsInfo.getFromIntent(intent), intent.getBooleanExtra(SpeindAPI.PARAM_ERROR, false));
                                        break;
                                }
                            }
                        }
                    }
                });
			}
		} else {
			log("System restart service");
		}
		return super.onStartCommand(intent, flags, startId);
	}
	
// --------- Reader Events begin
	@Override
	public void onReaderStateChanged(int id, int state) {
		log("onReaderStateChanged: " + id + " " + state);
		if (state==ITextReader.STATE_NEED_DOWNLOAD) {
			if (downloadingVoices.size()>0) {
				setState(SpeindAPI.SPEIND_STATE_DOWNLOADING);
			} else {
				setState(SpeindAPI.SPEIND_STATE_NEED_DOWNLOAD);
			}
		} else if (state==ITextReader.STATE_READY) {
			
    		SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        	String profile=settings.getString("last_used_profile", "");
        	String pass=settings.getString("last_used_password", "");

			if (profile!=null&&profile.isEmpty()) {
				setState(SpeindAPI.SPEIND_STATE_NEED_PROFILE);
			} else {
				onSetProfile(profile, pass);
			}
			
		}		
	}

	@Override
	public void onReadPositionChanged(int pos, int len) {
		log("onReadPositionChanged: " + pos + " of " + len);
		sendPlayPosition(pos, len);
	}

	@Override
	public void onReaderBeforeStartRead() {
		log("onReaderBeforeStartRead");
    	if (state==SpeindAPI.SPEIND_STATE_STOP_READER||state==SpeindAPI.SPEIND_STATE_STOP_PLAYER||state==SpeindAPI.SPEIND_STATE_PLAY_PLAYER) {
			setState(SpeindAPI.SPEIND_STATE_PLAY_READER);
			playSound("start");
			playBackgroundMusic();
		} else {
			setState(SpeindAPI.SPEIND_STATE_PLAY_READER);
			playSound("next");
		}
    }

	@Override
	public void onReaderCompleteRead() {
		log("onReaderCompleteRead");
		if (state==SpeindAPI.SPEIND_STATE_PLAY_READER) {
	    	if (lastReaderStartTime>0) {
	    		readerPlayTime+=((new Date()).getTime()-lastReaderStartTime);
	    		lastReaderStartTime=0;
	    	}
			if (!checkForPlayNextText()||speindData.speindConfig.max_play_time==SpeindAPI.PLAY_ONE_TRACK||((speindData.speindConfig.max_play_time>1)&&(readerPlayTime>speindData.speindConfig.max_play_time*1000))) {
				stopBackgroundMusic();						
				playSound("end");	
				handler.postDelayed(new Runnable(){
					@Override
					public void run() {
						SpeindAPI.next(SpeindService.this, getPackageName());
					}
				}, 1500);
			} else {
        		SpeindAPI.next(this, getPackageName());
			}					
		}
    }

	@Override
	public void onReaderUnsupportedLang(String lang) {
		log("onReaderUnsupportedLang");
		setState(SpeindAPI.SPEIND_STATE_PLAY_READER);
		SpeindAPI.next(this, getPackageName());
	}
	
	@Override
	public void onReaderLangListChanged() {
		log("onReaderLangListChanged");
		reader.onVoiceDataChanged();
		sendLangListChanged("");
	}

	// --------- Reader Events end

	// --------- Player Events begin
	@Override
	public void onCompletion(MediaPlayer mp) {
		log("onPlayerCompletion");
		handler.removeCallbacks(playerNextRunnable);
		if (state!=SpeindAPI.SPEIND_STATE_PLAY_READER&&state!=SpeindAPI.SPEIND_STATE_STOP_READER) {
			log("onCompletePlayer");
    		SpeindAPI.next(this, getPackageName());
		}
	}
	// --------- Player Events end

	//--------- AudioFocus Events begin
	public void onAudioFocusChange(int focusChange) {
		log("onAudioFocusChange");
        if (focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT) {
            log("audio focus pause");
            if (state==SpeindAPI.SPEIND_STATE_PLAY_PLAYER||state==SpeindAPI.SPEIND_STATE_PLAY_READER) {
                onPause();
                needResume=true;
            } else {
                needResume = false;
            }
        	haveAudioFocus=false;
        } else if (focusChange == AudioManager.AUDIOFOCUS_GAIN) {
            log("audio focus play");
    		haveAudioFocus=true;
            if (needResume) {
                needResume=false;
                onResume();
            }
        } else if (focusChange == AudioManager.AUDIOFOCUS_LOSS) {
            log("audio focus stop");
        	freeAudioFocus();
            onPause();
        }
    }
	//--------- AudioFocus Events end

	
	// --------- Control Events begin
    private void onQuit() {
    	log("onQuit");
    	onStop();
    	stopPlugins();	
    	handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                stopSelf();
            }
        }, 1000);
    }
    
    private void onVocesSettingsRequest() {
    	log("onVocesSettingsRequest");
    	fixDownloadingVoices();
    	
		Intent intent=new Intent();
		
		// TODO make custtom
		intent.setClassName(getPackageName(), SpeindAPI.VOICES_SETTINGS_NAME);
		
		ArrayList<ArrayList<String>> voicesData=reader.getVoicesData();
		ArrayList<String> voices=new ArrayList<>();
		if (voicesData!=null) {
			for (ArrayList<String> voiceAr: voicesData) {
				if (voiceAr.get(4).equals("2")) {
					if (downloadingVoices.indexOfValue(voiceAr.get(2))==-1) {
						voiceAr.set(4, "1");
						reader.setVoiceState(voiceAr.get(2), "1");
					}
				}
				voices.add(voiceAr.get(0)+"|"+voiceAr.get(1)+"|"+voiceAr.get(2)+"|"+voiceAr.get(3)+"|"+voiceAr.get(4)+"|"+voiceAr.get(5));
			}
		}
		intent.putStringArrayListExtra(SpeindAPI.PARAM_VOICES_DATA, voices);
		intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		
		startActivity(intent);		
    	
    }

    private void onBuyVoice(String code) {
    	log("onBuyVoice");
    	launchPurchaseFlow(reader.getSKUForVoice(code), "");    	
    	//reader.setVoiceState(code, "1");
    	//sendVoicesDataChanged();
    }
    
    private void onBuyVoiceResult(Intent intent) {
    	log("onBuyVoiceResult");
    	int resultCode=intent.getIntExtra(SpeindAPI.PARAM_RESULT_CODE, Activity.RESULT_OK);
    	Intent data=intent.getParcelableExtra(SpeindAPI.PARAM_RESULT_DATA);

        if (!googlePlaySetupDone) return;

        if (data == null) {
            //log("Null data in IAB activity result.");
            return;
        }

        int responseCode = getResponseCodeFromIntent(data);
        String purchaseData = data.getStringExtra(RESPONSE_INAPP_PURCHASE_DATA);
        String dataSignature = data.getStringExtra(RESPONSE_INAPP_SIGNATURE);

        if (resultCode == Activity.RESULT_OK && responseCode == BILLING_RESPONSE_RESULT_OK) {
            //log("Successful resultcode from purchase activity.");
            //log("Purchase data: " + purchaseData);
            //log("Data signature: " + dataSignature);
            //log("Extras: " + data.getExtras());

            if (purchaseData == null || dataSignature == null) {
                //log("BUG: either purchaseData or dataSignature is null.");
                //log("Extras: " + data.getExtras().toString());
                return;
            }

            try {
            	JSONObject o = new JSONObject(purchaseData);
                String sku = o.optString("productId");
                if (!Security.verifyPurchase(googlePlaySignatureBase64, purchaseData, dataSignature)) {
                    log("Purchase signature verification FAILED for sku " + sku);
                    return;
                }
                //log("Purchase signature successfully verified: "+sku);
                
                checkPurchases();
                
            } catch (JSONException e) {
                //log("Failed to parse purchase data.");
                //e.printStackTrace();
                //return;
            }
        } //else if (resultCode == Activity.RESULT_OK) {
            //log("Result code was OK but in-app billing response was not OK: " + getResponseDesc(responseCode));
        //} else if (resultCode == Activity.RESULT_CANCELED) {
            //log("Purchase canceled - Response: " + getResponseDesc(responseCode));
        //} else {
            //log("Purchase failed. Result code: " + Integer.toString(resultCode) + ". Response: " + getResponseDesc(responseCode));
        //}
    }

    private void onVoiceDownloadStarted(String voiceCode) {
    	log("onVoiceDownloadStarted");
    	reader.setVoiceState(voiceCode, "2");
    	sendVoicesDataChanged();		
    	if (state==SpeindAPI.SPEIND_STATE_NEED_DOWNLOAD) {
    		setState(SpeindAPI.SPEIND_STATE_DOWNLOADING);	
    	}
    }
    
    private void onDownloadVoice(String voiceCode, boolean wifiOnly) {
    	log("onDownloadVoice");
    	if (downloadingVoices.indexOfValue(voiceCode)!=-1) {
    		onVoiceDownloadStarted(voiceCode);
    	} else {
	    	String file=reader.getDownloadFileByCode(voiceCode);	
	    	if (file!=null&&file.length()>0) {
	    		
	    		File exFile=new File(SpeindAPI.SPEIND_DIR+file);
	    		if (!exFile.delete()) {
                    log("");
                }
	    		
		    	DownloadManager.Request request = new DownloadManager.Request(Uri.parse(SpeindService.DOWNLOAD_PATH+"voices/"+file));
		    	if (wifiOnly) request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI);
		    	else request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI | DownloadManager.Request.NETWORK_MOBILE);
		    	request.setTitle(voiceCode);
		    	request.setDescription(getText(R.string.downloading)+" "+voiceCode);
		    	request.setVisibleInDownloadsUi(true);
		    	request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE);
		    	request.setDestinationUri(Uri.fromFile(new File(SpeindAPI.SPEIND_DIR+file)));
		    	long downloadID = downloadManager.enqueue(request);
		    	downloadingVoices.put(downloadID, voiceCode);
		    	saveDownloadingVoices();	
		    	onVoiceDownloadStarted(voiceCode);
	    	} else {
	    		sendVoicesDataChanged();
	    	}
    	}
    }

    private void onRestorePurchases( boolean wifiOnly) {
    	log("onRestorePurchases");
    	ArrayList<ArrayList<String>> voicesData=reader.getVoicesData();
		if (voicesData!=null) {
			for (ArrayList<String> voiceAr: voicesData) {
				onDownloadVoice(voiceAr.get(2), wifiOnly);
			}
		}
    } 
    
    private void onVoiceDownloadResult(long id) {
    	log("onVoiceDownloadResult " + id);
    	if (downloadingVoices.indexOfKey(id)!=-1) {
    		String voiceCode=downloadingVoices.get(id);			
        	DownloadManager.Query query = new DownloadManager.Query();
        	query.setFilterById(id);
        	Cursor cursor = downloadManager.query(query);
        	if (!cursor.moveToFirst()) return;
        	int statusIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS);
        	int status=cursor.getInt(statusIndex);
        	if (DownloadManager.STATUS_SUCCESSFUL == status) {
            	int fileNameIndex = cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_FILENAME);
            	String downloadedPath = cursor.getString(fileNameIndex); 
            	onVoiceDownloadSuccess(id, downloadedPath, voiceCode);
        	} else if (DownloadManager.STATUS_FAILED == status) {	
        		downloadingVoices.remove(id);
        		saveDownloadingVoices();
        	    onVoiceDownloadError(status, voiceCode);        		
        	}
        	cursor.close();
    	}    	
    }
    
    private void onVoiceDownloadError(int errCode, String voiceCode) {
    	log("onVoiceDownloadError");
    	// TODO send download voice error
        log("Error: " + errCode);
    	if (state==SpeindAPI.SPEIND_STATE_DOWNLOADING||state==SpeindAPI.SPEIND_STATE_UNPUCKING) {
    		if (downloadingVoices.size()==0) {
				setState(SpeindAPI.SPEIND_STATE_NEED_DOWNLOAD);
			} else if (state==SpeindAPI.SPEIND_STATE_UNPUCKING) {
				setState(SpeindAPI.SPEIND_STATE_DOWNLOADING);
			}
    	}
    	reader.setVoiceState(voiceCode, "1");
    	sendVoicesDataChanged();
    }

    private void onVoiceDownloadSuccess(final long id, final String downloadedUriString, final String voiceCode) {
    	log("onVoiceDownloadSuccess");
    	if (state==SpeindAPI.SPEIND_STATE_DOWNLOADING) {
    		setState(SpeindAPI.SPEIND_STATE_UNPUCKING);
    	}
    	(new AsyncTask<Void, Void, Boolean>() {    		
    		@Override
    		protected Boolean doInBackground(Void... arg0) {
    			return reader.onDownloadSuccess(voiceCode, downloadedUriString);
    		}
    		protected void onPostExecute(Boolean result) {
        		downloadingVoices.remove(id);
        		saveDownloadingVoices();
    			if (result) {
    		    	reader.setVoiceState(voiceCode, "3");
    		    	sendVoicesDataChanged();
    		    	sendLangListChanged("");
    			} else {
    				onVoiceDownloadError(-1, voiceCode);
    			}
    		}  
    	}).execute();
    }

    private void onSetDefaultVoice(String code) {
    	log("onSetDefaultVoice");
    	reader.setVoiceState(code, "4");
    	sendVoicesDataChanged();
    	sendLangListChanged("");
    }
    
    private void onGetState() {
    	log("onGetState");

    	Intent intent = new Intent(SpeindAPI.BROADCAST_ACTION).putExtra(SpeindAPI.CLIENT_CMD, SpeindAPI.CC_STATE_CHANGED);
        intent.putExtra(SpeindAPI.PARAM_SERVICE_PACKAGE_NAME, getPackageName());
    	intent.putExtra(SpeindAPI.PARAM_OLD_STATE, state);
    	intent.putExtra(SpeindAPI.PARAM_NEW_STATE, state);
		sendBroadcast(intent);

    	if (state==SpeindAPI.SPEIND_STATE_NEED_PROFILE) {
    		sendProfileReguest();
    	} else if (state>SpeindAPI.SPEIND_STATE_PREPARING) {
    		sendFullState();
    	}
    }

    private void onSetProfile(String profile, String pass) {    
    	log("onSetProfile: " + profile + " " + pass);
    	if (checkProfile(profile, pass)) { // password correct
    		
    		SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        	SharedPreferences.Editor editor = settings.edit();
        	editor.putString("last_used_profile", profile);
        	editor.putString("last_used_password", pass);
        	editor.apply();
    		
			setState(SpeindAPI.SPEIND_STATE_PREPARING);			
			speindData.currentProfile = profile;    		
    		(new AsyncTask<Void, Void, Void>(){
				@Override
				protected Void doInBackground(Void... params) {
					loadProfileData();
					processSharedQueue();
					return null;
				}
				@Override
				protected void onPostExecute(Void param) {
					if (getPackageName().equals("com.maple.speind")) YandexMetrica.startNewSessionManually();
                    SpeindApplication.reportEvent("Engine", "Speind launched", "");
                    reader.setSpeechRate(speindData.speindConfig.speech_rate);
                    setState(SpeindAPI.SPEIND_STATE_STOP_PLAYER);
					sendPlayerPlayInfo();
			    	setState(SpeindAPI.SPEIND_STATE_STOP_READER);
			    	requestAudioFocus();
					initPlugins();
					sendFullState();
				}
			}).execute();
    		
    	} //else {
    		//sendWrongProfileError();
    	//}
    }

    private void onPlay() {
    	log("onPlay");
    	if (onPauseState!=-1) {
            onResume();
    	} else {
            if (state == SpeindAPI.SPEIND_STATE_STOP_PLAYER || playerOnly) {
                playerPlay();
            } else if (state == SpeindAPI.SPEIND_STATE_STOP_READER) {
                if (checkForPlayText()) readerPlay();
                else playerPlay();
            }
        }
    }

    private void onStop() {
    	log("onStop");
        if (onPauseState != -1) {
            clearPause();
        } else {
            stopBackgroundMusic();
            if (state == SpeindAPI.SPEIND_STATE_PLAY_PLAYER) {
                playerStop();
                setState(SpeindAPI.SPEIND_STATE_STOP_PLAYER);
            } else if (state == SpeindAPI.SPEIND_STATE_PLAY_READER) {
                readerStop();
                setState(SpeindAPI.SPEIND_STATE_STOP_READER);
            }
        }
    }

    private void onPlayPause() {
    	log("onPlayPause");
		if (state==SpeindAPI.SPEIND_STATE_STOP_PLAYER||state==SpeindAPI.SPEIND_STATE_STOP_READER) {
			onPlay();
		} else {
			onPause();
		}
    } 
    
    private void onNext() {
    	log("onNext");
    	readFull=false;
        clearPause();
		if (state==SpeindAPI.SPEIND_STATE_PLAY_PLAYER) playerStop();		    	
		if (state==SpeindAPI.SPEIND_STATE_PLAY_READER) readerStop();
		
		if (!playerOnly&&(speindData.speindConfig.max_play_time==SpeindAPI.PLAY_NO_LIMIT||playerPaths.size()==0)) {
			boolean startplay=(state==SpeindAPI.SPEIND_STATE_PLAY_PLAYER||state==SpeindAPI.SPEIND_STATE_PLAY_READER);
	    	if (checkForPlayNextText())
	    		readerNext(startplay);
	    	else {
	    		playerNext(startplay);
	    	}
		} else if (!playerOnly&&speindData.speindConfig.max_play_time==SpeindAPI.PLAY_ONE_TRACK) {
			if (state==SpeindAPI.SPEIND_STATE_STOP_PLAYER||state==SpeindAPI.SPEIND_STATE_PLAY_PLAYER) {
    			boolean startplay=(state==SpeindAPI.SPEIND_STATE_PLAY_PLAYER);
		    	if (checkForPlayNextText())
		    		readerNext(startplay);
		    	else {
		    		playerNext(startplay);
		    	}
			} else if (state==SpeindAPI.SPEIND_STATE_PLAY_READER||state==SpeindAPI.SPEIND_STATE_STOP_READER) {
    			boolean startplay=(state==SpeindAPI.SPEIND_STATE_PLAY_READER);
	    		playerNext(startplay);
			}    			  
		} else {
	    	if (!playerOnly&&checkForPlayNextText()) {
	    		if (state==SpeindAPI.SPEIND_STATE_PLAY_PLAYER||state==SpeindAPI.SPEIND_STATE_STOP_PLAYER) {
        			boolean startplay=(state==SpeindAPI.SPEIND_STATE_PLAY_PLAYER||state==SpeindAPI.SPEIND_STATE_PLAY_READER);
			    	log("ppt: "+playerPlayTime);
	    			if ((playerPlayTime)<speindData.speindConfig.max_play_time*1000) {
	    				playerNext(startplay);
	    			} else {
		    			readerNext(startplay);  
	    			}
	    		} else { 
        			boolean startplay=(state==SpeindAPI.SPEIND_STATE_PLAY_PLAYER||state==SpeindAPI.SPEIND_STATE_PLAY_READER);
			    	log("rpt: "+readerPlayTime);
	    			if ((readerPlayTime)<speindData.speindConfig.max_play_time*1000) {
		    			readerNext(startplay);
	    			} else {
	    				playerNext(startplay);
	    			}
	    		}
	    	} else {
    			boolean startplay=(state==SpeindAPI.SPEIND_STATE_PLAY_PLAYER||state==SpeindAPI.SPEIND_STATE_PLAY_READER);
	    		playerNext(startplay);
	    	}
		}
    }
     
    private void onPrev() {		
    	log("onPrev");
        clearPause();
    	readFull=false;
    	if (state==SpeindAPI.SPEIND_STATE_PLAY_PLAYER||state==SpeindAPI.SPEIND_STATE_STOP_PLAYER||playerOnly) {
    		if (!playerOnly&&checkForPlayText()) {
    			boolean startplay=(state==SpeindAPI.SPEIND_STATE_PLAY_PLAYER||state==SpeindAPI.SPEIND_STATE_PLAY_READER);
				sendReaderPlayInfo(speindData.speindConfig.read_full_article || readFull || speindData.infopoints.get(speindData.currentInfoPoint).readArticle);
    			if (startplay) readerPlay();
    			else {
    				setState(SpeindAPI.SPEIND_STATE_STOP_READER);
    			}
    		}
    	} else {
			boolean startplay=(state==SpeindAPI.SPEIND_STATE_PLAY_READER);
	    	if (checkForPlayPrevText()) {
	    		readerPrev(startplay);
	    	}
    	}
    }
    
    private void onReplay() {
    	log("onReplay");
        clearPause();
    	if (state==SpeindAPI.SPEIND_STATE_PLAY_READER) {
    		readerStop();
    		readerPlay();
    	} else if (state==SpeindAPI.SPEIND_STATE_PLAY_PLAYER) {
    		if (player.isPlaying()) player.seekTo(0);
    		else player.start();
    	}
    }
        
    private void onSkipNews() {	
    	log("onSkipNews");
        clearPause();
    	readFull=false;
    	if (state==SpeindAPI.SPEIND_STATE_PLAY_READER||state==SpeindAPI.SPEIND_STATE_STOP_READER) {
    		if (state==SpeindAPI.SPEIND_STATE_PLAY_READER) readerStop(); 
    		boolean startplay=(state==SpeindAPI.SPEIND_STATE_PLAY_READER);
    		if (!startplay) setState(SpeindAPI.SPEIND_STATE_STOP_PLAYER);
			if (speindData.currentInfoPoint!=(speindData.infopoints.size()-1)) {
				speindData.currentInfoPoint=speindData.infopoints.size()-1;
	        	saveCurrentInfoPoint();
			}
			playerNext(startplay);
    	}
    }

    private void onPlayInfoPoint(int pos) {
        if (speindData.currentInfoPoint==pos&&state!=SpeindAPI.SPEIND_STATE_PLAY_PLAYER&&state!=SpeindAPI.SPEIND_STATE_STOP_PLAYER) return;
    	if (playerOnly&&state==SpeindAPI.SPEIND_STATE_PLAY_PLAYER) pos = Math.max(0, pos-1);
    	log("onPlayInfoPoint: "+pos);
    	readFull=false;
		boolean startplay=(state==SpeindAPI.SPEIND_STATE_PLAY_PLAYER||state==SpeindAPI.SPEIND_STATE_PLAY_READER);
        clearPause();
		if (!playerOnly&&state==SpeindAPI.SPEIND_STATE_PLAY_PLAYER) playerStop();		    	
		if (!playerOnly&&state==SpeindAPI.SPEIND_STATE_PLAY_READER) readerStop();
		if (speindData.currentInfoPoint!=pos) {
			speindData.currentInfoPoint=pos;
        	saveCurrentInfoPoint();
		}
    	sendReaderPlayInfo(speindData.speindConfig.read_full_article || readFull || speindData.infopoints.get(speindData.currentInfoPoint).readArticle);
    	if (playerOnly&&state==SpeindAPI.SPEIND_STATE_PLAY_PLAYER) sendPlayerPlayInfo();
    	if (playerOnly) return;
		if (startplay) readerPlay();
		else setState(SpeindAPI.SPEIND_STATE_STOP_READER);
    }

    private void onReadCurrentInfopointArticle() {
    	log("onReadCurrentInfopointArticle");
    	if (state==SpeindAPI.SPEIND_STATE_PLAY_READER&&!readFull) {
            clearPause();
            readFull=true;
	    	if (speindData.infopoints.size()>0&&speindData.infopoints.size()>speindData.currentInfoPoint) {
	        	InfoPoint infopoint = speindData.infopoints.get(speindData.currentInfoPoint);
	    		if (infopoint!=null) {
    				SpeindAPI.InfoPointData data=infopoint.getData(speindData.currentProfile);
    				if (data!=null) {
		    			boolean readConfirm=false;
		    			SpeindAPI.DataFeedSettingsInfo dfsi=findDataFeedSettingsInfo(infopoint.processingPlugin);
		    			if (dfsi!=null) {
		    				readConfirm=dfsi.isNeedConfirmRead();
		    			}		    			
		    			if (readConfirm) {
        					handler.removeCallbacks(updatePositionRunnable);
        					readerStop();
        					String readText=""+Html.fromHtml("<p>"+data.postTextVocalizing+"</p>"); 
		    				reader.speak(readText, data.postLang, false);		    				
		    			} else if (!speindData.speindConfig.read_full_article) {
	    					if (infopoint.articleExists) {
	        					handler.removeCallbacks(updatePositionRunnable);
	        					readerStop();
	        					sendReaderPlayInfo(true);	        							    					
					    		if (prevSender.equals(data.postSender)) {
		        					String readText=""+Html.fromHtml("<p>"+data.postArticle.replace(data.postTitle, "")+"</p>");
				    				reader.speak(readText, data.postLang, false);		    									    			
					    		}
	    					}    					
		    	    	}
    				}
	    		}
	    	}
    	} else if (state==SpeindAPI.SPEIND_STATE_STOP_READER&&!readFull) {
            clearPause();
        	readFull=true;
    		sendReaderPlayInfo(true);
    	}
    }

    private void onPause() {
    	log("onPause");
        if (state==SpeindAPI.SPEIND_STATE_PLAY_PLAYER||state==SpeindAPI.SPEIND_STATE_PLAY_READER) {
            onPauseState=state;
            stopBackgroundMusic();
            if (state==SpeindAPI.SPEIND_STATE_PLAY_PLAYER) {
                playerPause();
                setState(SpeindAPI.SPEIND_STATE_STOP_PLAYER);
            } else if (state==SpeindAPI.SPEIND_STATE_PLAY_READER) {
                readerPause();
                setState(SpeindAPI.SPEIND_STATE_STOP_READER);
            }
        }
    }
    
    private void onResume() {
    	log("onResume");
        if (onPauseState!=-1) {
            onPauseState=-1;
            needResume = false;
            if (state==SpeindAPI.SPEIND_STATE_STOP_PLAYER) {
                playerResume();
            } else if (state==SpeindAPI.SPEIND_STATE_STOP_READER) {
                if (playerOnly) {
                    readerStop();
                    playerPlay();
                } else {
                    if (checkForPlayText()) readerResume();
                    else playerPlay();
                }
            }
        } else {
            onPlay();
        }
    }
    
    private void onPlayUserText(String text) {
    	log("onPlayUserText: " + text);
        clearPause();
    	// TODO palyUserText
    }
    
    private void onPlayUserFile(String fileName) {
    	log("onPlayUserFile: " + fileName);
        clearPause();
    	// TODO palyUserAudioFile
    }

    private void onMediaButtonPress(int pressCount) {
        log("onMediaButtonPress: " + pressCount);
    	switch (pressCount) {
    	case 1:
    		onPlayPause();
    		break;
    	case 2:
    		onNext();
    		break;
    	case 3:
    		onPrev();
    		break;
    	}
    } 
    
	private void onOpenMainActivity() {
		log("onOpenMainActivity");
    	Intent activityIntent=new Intent();
    	// TODO make custom
        activityIntent.setClassName(getPackageName(), SpeindAPI.SPEIND_NAME);
        activityIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    	startActivity(activityIntent);
	}

    private void onPinboardRequest() {
        if (SpeindConfig.exclude_pinboard) return;

        log("onPinboardRequest");
        Intent intent=new Intent();

        // TODO make custtom
        intent.setClassName(getPackageName(), SpeindAPI.PINBOARD_NAME);

        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra(SpeindAPI.PARAM_PROFILE_NAME, speindData.currentProfile);
        SpeindAPI.InfoPoint.putListToIntent(intent, pinboard);

        startActivity(intent);
    }

    private void onPinboardSettingsRequest() {
        if (SpeindConfig.exclude_pinboard) return;

        log("onPinboardSettingsRequest");
        Intent intent=new Intent();

        // TODO make custtom
        intent.setClassName(getPackageName(), SpeindAPI.PINBOARD_SETTINGS_NAME);

        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra(SpeindAPI.PARAM_PROFILE_NAME, speindData.currentProfile);

        startActivity(intent);
    }

    private void onAddToPinboard(String infopoiniID) {
        addToPinboard(infopoiniID);
    }

    private void onRemoveFromPinboard(String infopoiniID) {
        removeFromPinboard(infopoiniID);
    }

    private void onClearPinboard() {
        clearPinboard();
    }

    private void onSendPinboard(String email, int format, boolean needClear) {
        sendPinboard(email, format, needClear);
    }

    private void onSettingsRequest() {
    	log("onSettingsRequest");
		Intent intent=new Intent();

        // TODO make custtom
        intent.setClassName(getPackageName(), SpeindAPI.SETTINGS_NAME);

        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        SpeindAPI.DataFeedSettingsInfo.putListToIntent(intent, speindData.dataFeedsettingsInfos);
		if (speindData.speindConfig!=null) speindData.speindConfig.putToIntent(intent);
		startActivity(intent);		
    	 
    }
    
    private void onSettingsChanged(SpeindAPI.SpeindSettings config) {
    	log("onSettingsChanged");
    	speindData.speindConfig=config;  
    	saveConfigForProfile();
    	sendConfig();
        sendLoadImagesOnMobileInetChanged(config.not_download_images_on_mobile_net);
        sendStoreInfopointTimeChanged(config.infopoints_store_time * 1000);
        reader.setSpeechRate(speindData.speindConfig.speech_rate);
    }
    
    private void onDataFeedSettingsRequest(DataFeedSettingsInfo info) {  
    	log("onDataFeedSettingsRequest");
    	Intent intent = new Intent(SpeindAPI.BROADCAST_ACTION).putExtra(SpeindAPI.DATAFEED_CLIENT_CMD, SpeindAPI.DF_CC_DATA_FEED_SETTINGS_REQUEST).putExtra(SpeindAPI.PARAM_SERVICE_PACKAGE_NAME, getPackageName());
    	info.putToIntent(intent);
    	sendBroadcast(intent);    	
    }

    private void onDataFeedResumeRequest(DataFeedSettingsInfo info) {  
    	log("onDataFeedResumeRequest");
    	Intent intent = new Intent(SpeindAPI.BROADCAST_ACTION).putExtra(SpeindAPI.DATAFEED_CLIENT_CMD, SpeindAPI.DF_CC_RESUME).putExtra(SpeindAPI.PARAM_SERVICE_PACKAGE_NAME, getPackageName());
    	info.putToIntent(intent);
    	sendBroadcast(intent);    	
    }

    private void onDataFeedSuspendRequest(DataFeedSettingsInfo info) {   
    	log("onDataFeedSuspendRequest");
    	Intent intent = new Intent(SpeindAPI.BROADCAST_ACTION).putExtra(SpeindAPI.DATAFEED_CLIENT_CMD, SpeindAPI.DF_CC_SUSPEND).putExtra(SpeindAPI.PARAM_SERVICE_PACKAGE_NAME, getPackageName());
        info.putToIntent(intent);
    	sendBroadcast(intent);    	
    }    

    private void onAddPluginSettingsName(SpeindAPI.DataFeedSettingsInfo info) {
    	log("onDataFeedSuspendRequest");
    	if (info!=null) {
            sendLoadImagesOnMobileInetChanged(speindData.speindConfig.not_download_images_on_mobile_net);
            sendStoreInfopointTimeChanged(speindData.speindConfig.infopoints_store_time*1000);
            sendLangListChanged(info.packageName);

    		int index=-1;
    		for (DataFeedSettingsInfo info1 : speindData.dataFeedsettingsInfos) {
    			if (info1.packageName.equals(info.packageName)) {
    				index = speindData.dataFeedsettingsInfos.indexOf(info1);
    				break;
    			}
    		}
    		if (index==-1) {
	    		speindData.dataFeedsettingsInfos.add(info);   	    	
	    		Intent intent=new Intent(SpeindAPI.BROADCAST_ACTION);
	    		intent.putExtra(SpeindAPI.CLIENT_CMD, SpeindAPI.CC_NEW_DATAFEED_SETTINGS_INFO);
                intent.putExtra(SpeindAPI.PARAM_SERVICE_PACKAGE_NAME, getPackageName());
	    		info.putToIntent(intent);
	    		sendBroadcast(intent);
    		} else {
                speindData.dataFeedsettingsInfos.set(index, info);
            }

            Intent intent = new Intent(SpeindAPI.BROADCAST_ACTION).putExtra(SpeindAPI.DATAFEED_CLIENT_CMD, SpeindAPI.DF_CC_SET_PROFILE).putExtra(SpeindAPI.PARAM_SERVICE_PACKAGE_NAME, getPackageName());
			intent.putExtra(SpeindAPI.PARAM_PROFILE_NAME, speindData.currentProfile);
			
			SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
			info.setState(settings.getInt("Pluginenable: " + info.packageName + " for " + speindData.currentProfile, DataFeedSettingsInfo.DATAFEED_STATE_READY));
			
			info.putToIntent(intent);
			sendBroadcast(intent);    		    	    			

            if (!speindData.speindConfig.post_settings.post_plugins_data.containsKey(info.packageName)) {
                speindData.speindConfig.post_settings.post_plugins_data.put(info.packageName, true);
                saveConfigForProfile();
                sendConfig();
            }

    	}
    }

    private void onDataFeedInfoChanged(SpeindAPI.DataFeedSettingsInfo info, boolean error) {
    	log("onDataFeedInfoChanged");
        int index = -1;
        int cnt=speindData.dataFeedsettingsInfos.size();
		for (int i=0; i<cnt; i++) {
			if (speindData.dataFeedsettingsInfos.get(i).packageName.equals(info.packageName)) {
                index = i;
				break;
			}
		}
        if (index!=-1) {
            speindData.dataFeedsettingsInfos.set(index, info);
            SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
            SharedPreferences.Editor editor = settings.edit();
            editor.putInt("Pluginenable: " + info.packageName + " for " + speindData.currentProfile, info.getState());
            editor.apply();

            Intent intent = new Intent(SpeindAPI.BROADCAST_ACTION).putExtra(SpeindAPI.CLIENT_CMD, SpeindAPI.CC_DATAFEED_INFO_CHANGED);
            intent.putExtra(SpeindAPI.PARAM_SERVICE_PACKAGE_NAME, getPackageName());
            intent.putExtra(SpeindAPI.PARAM_ERROR, error);
            info.putToIntent(intent);
            sendBroadcast(intent);
        }
    }    

    private void onPluginAddInfoPiont(Intent intent) {
		SpeindAPI.InfoPoint infopoint = SpeindAPI.InfoPoint.getFromIntent(intent);
		boolean startRead=intent.getBooleanExtra("start_read", false);
		SpeindAPI.InfoPointData data=infopoint.getData(speindData.currentProfile);
		if (data!=null) {
            infopoint.titleExists = !data.postTitle.equals("");
            infopoint.textExists = !data.postOriginText.equals("");
            infopoint.articleExists = !data.postArticle.equals("");
			addToInfoPoints(infopoint, startRead);
		} else {
			for (SpeindAPI.DataFeedSettingsInfo info : speindData.dataFeedsettingsInfos) {
				if (info.packageName.equals(infopoint.processingPlugin)) {
					infopoint.deleteData(speindData.currentProfile);
					onDataFeedSuspendRequest(info);
					break;
				}
			}
		}
    }

    private void onSharedText(String sharedText) {
    	if (sharedText!=null&&sharedText.length()>0) {
    		queueSharedText(sharedText);
    		if (state>SpeindAPI.SPEIND_STATE_PREPARING) {
    			processSharedQueue();
    		}
    	}
    }
    
    private void onSetPlayerOnly(boolean playerOnly) {
    	if (playerOnly!=this.playerOnly) {
	    	this.playerOnly=playerOnly;
			if (playerOnly&&state==SpeindAPI.SPEIND_STATE_PLAY_READER) {
				onNext();
			}
    	}
    }

    private void onLike(SpeindAPI.InfoPoint infopoint) {
        // TODO
    }

    private void onPost(SpeindAPI.InfoPoint infopoint) {
        boolean sended = false;
        for (SpeindAPI.DataFeedSettingsInfo info : speindData.dataFeedsettingsInfos) {
            if (info.canPost()&&!info.isNeedAuthorization()) {
                if (speindData.speindConfig.post_settings.post_plugins_data.containsKey(info.packageName)&&speindData.speindConfig.post_settings.post_plugins_data.get(info.packageName)) {
                    sended = true;
                    Intent intent = new Intent(SpeindAPI.BROADCAST_ACTION).putExtra(SpeindAPI.DATAFEED_CLIENT_CMD, SpeindAPI.DF_CC_POST);
                    intent.putExtra(SpeindAPI.PARAM_SERVICE_PACKAGE_NAME, getPackageName());
                    intent.putExtra(SpeindAPI.PARAM_PACKAGE_NAME, info.packageName);
                    infopoint.putToIntent(intent);
                    sendBroadcast(intent);
                }
            }
        }

        sendInfoMessage(sended ? getString(R.string.infopoint_posted) : getString(R.string.infopoint_posting_not_configured));
    }

	// --------- Control Events end

	// --------- Functions begin
	private static void log(String s) {
    	if (isDebug) Log.e("[---SpeindService---]", Thread.currentThread().getName()+": "+s);
    }
		  
	private static boolean deleteDir(File dir) {
		log("deleteDir: "+dir);
        if (dir==null) return true;
        if (dir.isDirectory()) {
            String[] children = dir.list();
            for (String aChildren : children) {
                boolean success = deleteDir(new File(dir, aChildren));
                if (!success) {
                    return false;
                }
            }
        }
        return dir.delete();
    }

    @SuppressWarnings("TryFinallyCanBeTryWithResources")
    private void unpuckLangData() {
    	log("unpuckLangData");
    	File checkDir = new File(SpeindAPI.SPEIND_DIR + "lang_profiles");  	
    	if (checkDir.exists()) {
    		File checkFile = new File(SpeindAPI.SPEIND_DIR + "lang_profiles" + File.separator + "af");
    		if (checkFile.exists()) {
    			deleteDir(checkDir);
    		} else {
    			return;
    		}
    	}
		try {
			InputStream is = getContentResolver().openInputStream(Uri.parse("android.resource://com.maple.speind/raw/lang_profiles"));
			ZipInputStream zis = new ZipInputStream(new BufferedInputStream(is));
			try {
			     ZipEntry ze;
			     while ((ze = zis.getNextEntry()) != null) {
			    	 int size;
		             byte[] buffer = new byte[2048];
		             if (ze.getName().charAt(ze.getName().length()-1)==File.separatorChar) {
		            	 File dirToSave = new File(SpeindAPI.SPEIND_DIR + ze.getName());
		            	 if (!dirToSave.mkdirs()) {
                             log("");
                         }
		             } else {
			             FileOutputStream fos = new FileOutputStream(SpeindAPI.SPEIND_DIR + ze.getName());
			             BufferedOutputStream bos = new BufferedOutputStream(fos, buffer.length);
			             while ((size = zis.read(buffer, 0, buffer.length)) != -1) {
			            	 bos.write(buffer, 0, size);
			             }
			             bos.flush();
			             bos.close();
		             }
			     }
			 } finally {
			     zis.close();
			 }
		} catch (Exception e) {
			e.printStackTrace();
		}
    }

    @SuppressWarnings("unchecked")
	public static LongSparseArray<String> loadDownloadingVoices(Context context) {
    	log("loadDownloadingVoices");
        LongSparseArray<String> res=null;
    	File downloadingVoicesFile = new File(context.getDir("data", MODE_PRIVATE), "downloadingVoices");    
    	ObjectInputStream inputStream;
		try {
			inputStream = new ObjectInputStream(new FileInputStream(downloadingVoicesFile));
            Object o = inputStream.readObject();
			if (o instanceof LongSparseArray) res=(LongSparseArray<String>) o;
	    	inputStream.close();
		} catch (ClassNotFoundException | IOException e) {
            e.printStackTrace();
		}
        return res;
    }
    
    private void fixDownloadingVoices() {
    	log("fixDownloadingVoices");
		ArrayList<Long> removeIds=new ArrayList<>();
		for (int index=0; index<downloadingVoices.size();index++) {
            long id = downloadingVoices.keyAt(index);
			log(""+id);
			DownloadManager.Query query = new DownloadManager.Query();
        	query.setFilterById(id);
        	Cursor cursor = downloadManager.query(query);
        	if (!cursor.moveToFirst()) {
        		removeIds.add(id);
        		cursor.close();
        		continue;
        	}
        	int statusIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS);
        	int status=cursor.getInt(statusIndex);
        	if (DownloadManager.STATUS_FAILED == status) {
        		removeIds.add(id);
        	}
        	cursor.close();
		}
		for (Long id : removeIds) {
    		downloadingVoices.remove(id);
		}
		saveDownloadingVoices();
    }
    
    private void saveDownloadingVoices() {
    	log("saveDownloadingVoices");
    	File downloadingVoicesFile = new File(getDir("data", MODE_PRIVATE), "downloadingVoices");    
    	ObjectOutputStream outputStream;
		try {
			outputStream = new ObjectOutputStream(new FileOutputStream(downloadingVoicesFile));
	    	outputStream.writeObject(downloadingVoices);
	    	outputStream.flush();
	    	outputStream.close();
		} catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    private void sendDownloadProgress(int progress[]) {
    	log("sendDownloadProgress");
		Intent intent = new Intent(SpeindAPI.BROADCAST_ACTION).putExtra(SpeindAPI.CLIENT_CMD, SpeindAPI.CC_DOWLOAD_PROGRESS).putExtra(SpeindAPI.PARAM_DWLD_PRG, progress);
        intent.putExtra(SpeindAPI.PARAM_SERVICE_PACKAGE_NAME, getPackageName());
		sendBroadcast(intent);			
    }
    
    private void updateDownloadProgress() {
    	log("updateDownloadProgress");
        handler.removeCallbacks(updateDownloadProgressRunnable);       
        int progress[]={0, 0, 0};

		ArrayList<Long> removeIds=new ArrayList<>();
        for (int idx=0; idx<downloadingVoices.size();idx++) {
            long id = downloadingVoices.keyAt(idx);
			log(""+id);
			DownloadManager.Query query = new DownloadManager.Query();
        	query.setFilterById(id);
        	Cursor cursor = downloadManager.query(query);
        	if (!cursor.moveToFirst()) {
        		reader.setVoiceState(downloadingVoices.get(id), "1");
        		removeIds.add(id);
        		cursor.close();
        		continue;
        	}
        	int statusIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS);
        	int status=cursor.getInt(statusIndex);
        	if (DownloadManager.STATUS_FAILED == status) {
        		reader.setVoiceState(downloadingVoices.get(id), "1");
        		removeIds.add(id);
        		cursor.close();
        		continue;
        	}
        	
        	int index = cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR);
        	long bytes=cursor.getLong(index);
        	
        	index = cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES);
        	long total=cursor.getLong(index);
        	
        	if (total!=0&&bytes!=0) {
        		int cur_progress=(int)((long)200*bytes/total);
        		if (cur_progress>progress[0]&&cur_progress<201) {
        			progress[0]=cur_progress;
        		}
        	}
        	cursor.close();
		}
        
		for (Long id : removeIds) {
    		downloadingVoices.remove(id);
		}
		saveDownloadingVoices();
		if (downloadingVoices.size()==0) {
			setState(SpeindAPI.SPEIND_STATE_NEED_DOWNLOAD);
		} else {
	        sendDownloadProgress(progress);		
	        handler.postDelayed(updateDownloadProgressRunnable, 1000);
		}
    }

    private void playBackgroundMusic() {
    	log("playBackgroundMusic");
    	try {
    		backgroundPlayer.reset();
    		backgroundPlayer.setDataSource(getApplicationContext(), Uri.parse("android.resource://com.maple.speind/raw/music"));	
    		backgroundPlayer.prepare();
    		backgroundPlayer.start();
    	} catch (IllegalArgumentException | IllegalStateException | IOException e) {
            backgroundPlayer.reset();
    	}
    }
    
    private void stopBackgroundMusic() {
    	log("stopBackgroundMusic");
    	if (backgroundPlayer!=null) if (backgroundPlayer.isPlaying()) backgroundPlayer.stop();
    }
    
    private void playSound(String res) {
    	log("playSound: "+res);
    	try {
    		player.reset();
            player.setDataSource(getApplicationContext(), Uri.parse("android.resource://com.maple.speind/raw/"+res));	
            player.prepare();
            player.start();
    	} catch (IllegalArgumentException | IllegalStateException | IOException e) {
    		player.reset();
            e.printStackTrace(); 
    	}
    }
    
	private void showNotification() {
		log("showNotification");

        PendingIntent contentIntent=PendingIntent.getService(this, 0, SpeindAPI.createIntent(getPackageName()).putExtra(SpeindAPI.SERVICE_CMD, 1001), 0);
		PendingIntent nextIntent=PendingIntent.getService(this, 1, SpeindAPI.createNextIntent(getPackageName()), 0);
		PendingIntent prevIntent=PendingIntent.getService(this, 2, SpeindAPI.createPrevIntent(getPackageName()), 0);
		PendingIntent playIntent=PendingIntent.getService(this, 3, SpeindAPI.createPlayIntent(getPackageName()), 0);
		PendingIntent pauseIntent=PendingIntent.getService(this, 4, SpeindAPI.createPauseIntent(getPackageName()), 0);
		 
		RemoteViews remoteView = new RemoteViews(getPackageName(), R.layout.test_notification_view);
        //remoteView.setImageViewResource(R.id.speind_notification_icon, R.drawable.notification_icon);
        // Prepare notification
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this);
		notificationBuilder.setSmallIcon(R.drawable.ic_launcher);
        notificationBuilder.setContentIntent(contentIntent);
		if (speindData!=null&&speindData.infopoints!=null&&speindData.infopoints.size()>0) {
			notificationBuilder.setContentText(getText(R.string.unread_label)+(": "+(speindData.infopoints.size()-speindData.currentInfoPoint-1)));
			remoteView.setTextViewText(R.id.text, getText(R.string.unread_label)+(": "+(speindData.infopoints.size()-speindData.currentInfoPoint-1)));
		} else {
			notificationBuilder.setContentText(getText(R.string.service_label));
			remoteView.setTextViewText(R.id.text, getText(R.string.service_label));
		}
		if (state==SpeindAPI.SPEIND_STATE_STOP_PLAYER||state==SpeindAPI.SPEIND_STATE_PLAY_PLAYER) {
			String fpath[]=currentPlayFile.split("\\"+File.separator);
			notificationBuilder.setContentTitle(fpath[fpath.length-1]);	  
			remoteView.setTextViewText(R.id.title, fpath[fpath.length-1]);
		} else if ((state==SpeindAPI.SPEIND_STATE_STOP_READER&&speindData!=null&&speindData.infopoints!=null&&speindData.infopoints.size()>0)||state==SpeindAPI.SPEIND_STATE_PLAY_READER) {
			SpeindAPI.InfoPoint ip=speindData.infopoints.get(speindData.currentInfoPoint);
			SpeindAPI.InfoPointData data=ip.getData(speindData.currentProfile);
			if (data!=null) {
				if (ip.titleExists) {
					notificationBuilder.setContentTitle(data.postTitle);
					remoteView.setTextViewText(R.id.title, data.postTitle);
				} else {
					notificationBuilder.setContentTitle(data.postSender);
					remoteView.setTextViewText(R.id.title, data.postSender);
				}
			}
		} else {
			notificationBuilder.setContentTitle(getText(R.string.app_name));	
			remoteView.setTextViewText(R.id.title, getText(R.string.app_name));
		}
		/*
		if (state>=SpeindAPI.SPEIND_STATE_STOP_PLAYER) {
			notificationBuilder.addAction(android.R.drawable.ic_media_previous, "Prev", prevIntent);
			if (state==SpeindAPI.SPEIND_STATE_STOP_PLAYER|| state==SpeindAPI.SPEIND_STATE_STOP_READER ) {
				notificationBuilder.addAction(android.R.drawable.ic_media_play,  "Play", playIntent);				
			} else {
				notificationBuilder.addAction(android.R.drawable.ic_media_pause, "Stop", stopIntent);
			}
			notificationBuilder.addAction(android.R.drawable.ic_media_next,      "Next", nextIntent);
		}
		*/
		//*
		if (state>=SpeindAPI.SPEIND_STATE_STOP_PLAYER) {
            remoteView.setViewVisibility(R.id.actions, View.VISIBLE);
			remoteView.setOnClickPendingIntent(R.id.action0, prevIntent);
			if (state==SpeindAPI.SPEIND_STATE_STOP_PLAYER|| state==SpeindAPI.SPEIND_STATE_STOP_READER ) {
				remoteView.setOnClickPendingIntent(R.id.action1, playIntent);
				remoteView.setInt(R.id.action1, "setImageResource", android.R.drawable.ic_media_play);
			} else {
				remoteView.setOnClickPendingIntent(R.id.action1, pauseIntent);
				remoteView.setInt(R.id.action1, "setImageResource", android.R.drawable.ic_media_pause);
			}
			remoteView.setOnClickPendingIntent(R.id.action2, nextIntent);
		} else {
            remoteView.setViewVisibility(R.id.actions, View.GONE);
        }
		notificationBuilder.setContent(remoteView);
		//*/
        notificationBuilder.setCategory(NotificationCompat.CATEGORY_SERVICE);

        int NOTIFICATION = R.id.toolbar;
        startForeground(NOTIFICATION, notificationBuilder.build());

    }

    private void sendProfileReguest() {
    	log("sendProfileReguest");
		Intent intent = new Intent(SpeindAPI.BROADCAST_ACTION).putExtra(SpeindAPI.CLIENT_CMD, SpeindAPI.CC_PROFILES);
        intent.putExtra(SpeindAPI.PARAM_SERVICE_PACKAGE_NAME, getPackageName());
		sendBroadcast(intent);    	
    }
    
    private void setState(int st) {
    	log("setState: " + st);
    	if (state!=st) {
    		log("State changed "+state+"->"+st);
    		int oldstate=state;
    		state=st;
    		
        	Intent intent = new Intent(SpeindAPI.BROADCAST_ACTION).putExtra(SpeindAPI.CLIENT_CMD, SpeindAPI.CC_STATE_CHANGED);
            intent.putExtra(SpeindAPI.PARAM_SERVICE_PACKAGE_NAME, getPackageName());
        	intent.putExtra(SpeindAPI.PARAM_OLD_STATE, oldstate);
        	intent.putExtra(SpeindAPI.PARAM_NEW_STATE, state);
    		sendBroadcast(intent);
    		
    		if (state==SpeindAPI.SPEIND_STATE_PLAY_PLAYER||state==SpeindAPI.SPEIND_STATE_PLAY_READER) {
    			if (!wakeLock.isHeld()) wakeLock.acquire();
    			sendPlayState();
    		} else {
    			if (wakeLock.isHeld()) wakeLock.release();
    		}
    		if (state==SpeindAPI.SPEIND_STATE_DOWNLOADING) {
    			updateDownloadProgress();
    		} else {
    			handler.removeCallbacks(updateDownloadProgressRunnable);
    		}
    		if (state==SpeindAPI.SPEIND_STATE_NEED_PROFILE) {
    			sendProfileReguest();
    		}
    		showNotification();
    	}
    }

    public static Cursor myquery(Context context, Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder, int limit) {
    	try {
    		ContentResolver resolver = context.getContentResolver();
    		if (resolver == null) {
    			return null;
    		}
    		if (limit > 0) {
    			uri = uri.buildUpon().appendQueryParameter("limit", "" + limit).build();
    		}
    		return resolver.query(uri, projection, selection, selectionArgs, sortOrder);
    	} catch (UnsupportedOperationException ex) {
    		return null;
    	}            
    } 
    
	private void refrashPlaylist() {
        log("refrashPlaylist");
    	playerPaths.clear();
    	String[] projection = {MediaStore.Audio.Media._ID, MediaStore.Audio.Media.DATA};  
    	String selection = MediaStore.Audio.Media.IS_MUSIC + " != 0 ";
    	Cursor musicListSDCardCursor = myquery(this, MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, projection, selection , null, null,0);
    	Cursor musicListInternalMemoryCursor = myquery(this, MediaStore.Audio.Media.INTERNAL_CONTENT_URI, projection, selection , null, null,0);
    	if( musicListSDCardCursor!= null ) {
    		for(int i=0;i<musicListSDCardCursor.getCount();i++) {
                musicListSDCardCursor.moveToPosition(i);
                String p = musicListSDCardCursor.getString(1);
                if(p.endsWith("mp3")) playerPaths.addElement(p);

            }
            musicListSDCardCursor.close();
        }
    	if( musicListInternalMemoryCursor!= null ) {
            for(int i=0;i<musicListInternalMemoryCursor.getCount();i++) {
                musicListInternalMemoryCursor.moveToPosition(i);
                String p = musicListInternalMemoryCursor.getString(1);
                if(p.endsWith("mp3")) playerPaths.addElement(p);
            }
            musicListInternalMemoryCursor.close();
        }
    	if (!playerPaths.isEmpty()) {
	    	java.util.Random r = new  java.util.Random();
	        int pos = 0;
	        int pos1 = 0;
	        if (playerPaths.size()>1) {
		        pos = r.nextInt(playerPaths.size());
		        pos1 = r.nextInt(playerPaths.size());
	        }
	    	currentPlayFile=playerPaths.elementAt(pos);
	    	nextPlayFile=playerPaths.elementAt(pos1);
	    	
    	}  else {
    		currentPlayFile="android.resource://com.maple.speind/raw/music";
    		nextPlayFile="android.resource://com.maple.speind/raw/music";
    	}
    	if (state==SpeindAPI.SPEIND_STATE_PLAY_PLAYER||state==SpeindAPI.SPEIND_STATE_STOP_PLAYER) {
    		sendPlayerPlayInfo();
    	}
	}

    private void sendPlayerPlayInfo() {
    	log("sendPlayerPlayInfo");
    	showNotification();
    	Intent intent = new Intent(SpeindAPI.BROADCAST_ACTION);
    	intent.putExtra(SpeindAPI.CLIENT_CMD, SpeindAPI.CC_PLAYING_INFO);
        intent.putExtra(SpeindAPI.PARAM_SERVICE_PACKAGE_NAME, getPackageName());
    	intent.putExtra(SpeindAPI.PARAM_STATE, state);    	
    	intent.putExtra(SpeindAPI.PARAM_PLAY_SORCE, SpeindAPI.SOURCE_MP3);
    	intent.putExtra(SpeindAPI.PARAM_PLAY_FILE, currentPlayFile);    	
    	intent.putExtra(SpeindAPI.PARAM_NEXT_PLAY_FILE, nextPlayFile);    	
    	sendBroadcast(intent);		
    }

    private void sendPlayPosition(int pos, int len) {
    	//log("sendPlayPosition");
		Intent intent = new Intent(SpeindAPI.BROADCAST_ACTION).putExtra(SpeindAPI.CLIENT_CMD, SpeindAPI.CC_PLAYING_POSITION);
        intent.putExtra(SpeindAPI.PARAM_SERVICE_PACKAGE_NAME, getPackageName());
		intent.putExtra(SpeindAPI.PARAM_PLAY_POSITION, pos);
		intent.putExtra(SpeindAPI.PARAM_PLAY_LENGTH, len);
		sendBroadcast(intent);    		    	    	
    }

    int getResponseCodeFromBundle(Bundle b) {
        Object o = b.get(RESPONSE_CODE); 
        if (o == null) {
            return BILLING_RESPONSE_RESULT_OK; 
        }
        else if (o instanceof Integer) return (Integer) o;
        else if (o instanceof Long) return (int)((Long)o).longValue();
        else {
            throw new RuntimeException("Unexpected type for bundle response code: " + o.getClass().getName());
        }
    }

    int getResponseCodeFromIntent(Intent i) {
        Object o = i.getExtras().get(RESPONSE_CODE);
        if (o == null) {
            return BILLING_RESPONSE_RESULT_OK;
        }
        else if (o instanceof Integer) return (Integer) o;
        else if (o instanceof Long) return (int)((Long)o).longValue();
        else {
            log("Unexpected type for intent response code.");
            log(o.getClass().getName());
            throw new RuntimeException("Unexpected type for intent response code: " + o.getClass().getName());
        }
    }
    
    public static String getResponseDesc(int code) {
        String[] iab_msgs = ("0:OK/1:User Canceled/2:Unknown/" +
                "3:Billing Unavailable/4:Item unavailable/" +
                "5:Developer Error/6:Error/7:Item Already Owned/" +
                "8:Item not owned").split("/");
        String[] iabhelper_msgs = ("0:OK/-1001:Remote exception during initialization/" +
                                   "-1002:Bad response received/" +
                                   "-1003:Purchase signature verification failed/" +
                                   "-1004:Send intent failed/" +
                                   "-1005:User cancelled/" +
                                   "-1006:Unknown purchase response/" +
                                   "-1007:Missing token/" +
                                   "-1008:Unknown error").split("/");

        if (code <= IABHELPER_ERROR_BASE) {
            int index = IABHELPER_ERROR_BASE - code;
            if (index >= 0 && index < iabhelper_msgs.length) return iabhelper_msgs[index];
            else return String.valueOf(code) + ":Unknown IAB Helper Error";
        }
        else if (code < 0 || code >= iab_msgs.length)
            return String.valueOf(code) + ":Unknown";
        else
            return iab_msgs[code];
    }

    void checkPurchases() {
        log("checkPurchases");
        boolean verificationFailed = false;
        String continueToken = null;
        ArrayList<String> purchaseSKUs = new ArrayList<>();
        
        do {
            Bundle ownedItems;
			try {
				ownedItems = googlePlayService.getPurchases(3, getPackageName(), ITEM_TYPE_INAPP, continueToken);
			} catch (RemoteException e) {
				break;
			}

            int response = getResponseCodeFromBundle(ownedItems);
            if (response != BILLING_RESPONSE_RESULT_OK) {
                return;
            }
            if (!ownedItems.containsKey(RESPONSE_INAPP_ITEM_LIST) || !ownedItems.containsKey(RESPONSE_INAPP_PURCHASE_DATA_LIST) || !ownedItems.containsKey(RESPONSE_INAPP_SIGNATURE_LIST)) {
                return;
            }

            ArrayList<String> ownedSkus = ownedItems.getStringArrayList(RESPONSE_INAPP_ITEM_LIST);
            ArrayList<String> purchaseDataList = ownedItems.getStringArrayList(RESPONSE_INAPP_PURCHASE_DATA_LIST);
            ArrayList<String> signatureList = ownedItems.getStringArrayList(RESPONSE_INAPP_SIGNATURE_LIST);

            for (int i = 0; i < purchaseDataList.size(); ++i) {
                String purchaseData = purchaseDataList.get(i);
                String signature = signatureList.get(i);
                String sku = ownedSkus.get(i);
                log("Sku check: " + sku);
                if (Security.verifyPurchase(googlePlaySignatureBase64, purchaseData, signature)) {
                    purchaseSKUs.add(sku);
                } else {
                    verificationFailed = true;
                }
            }
            continueToken = ownedItems.getString(INAPP_CONTINUATION_TOKEN);
        } while (!TextUtils.isEmpty(continueToken));

        if (!verificationFailed) {
        	reader.fixVoicesStates(purchaseSKUs);
        	sendVoicesDataChanged();
        }
    }

    private void sendVoicesDataChanged() {
    	log("sendVoicesDataChanged");
		ArrayList<ArrayList<String>> voicesData=reader.getVoicesData();
		ArrayList<String> voices=new ArrayList<>();
		if (voicesData!=null) {
			for (ArrayList<String> voiceAr: voicesData) {
				voices.add(voiceAr.get(0)+"|"+voiceAr.get(1)+"|"+voiceAr.get(2)+"|"+voiceAr.get(3)+"|"+voiceAr.get(4)+"|"+voiceAr.get(5));
			}
		}

		Intent intent = new Intent(SpeindAPI.BROADCAST_ACTION).putExtra(SpeindAPI.CLIENT_CMD, SpeindAPI.CC_VOICES_DATA_UPDATED).putStringArrayListExtra(SpeindAPI.PARAM_VOICES_DATA, voices);
        intent.putExtra(SpeindAPI.PARAM_SERVICE_PACKAGE_NAME, getPackageName());
		sendBroadcast(intent);
	}
    
    public void launchPurchaseFlow(String sku, String extraData) {
    	log("launchPurchaseFlow");
        if (googlePlaySetupDone) {
	        try {
	            Bundle buyIntentBundle = googlePlayService.getBuyIntent(3, getPackageName(), sku, ITEM_TYPE_INAPP, extraData);
	            int response = getResponseCodeFromBundle(buyIntentBundle);
	            if (response != BILLING_RESPONSE_RESULT_OK) {
	                log("Unable to buy item, Error response: " + getResponseDesc(response));
	            }

	            PendingIntent pendingIntent = buyIntentBundle.getParcelable(RESPONSE_BUY_INTENT);
	            
	    		Intent intent = new Intent(SpeindAPI.BROADCAST_ACTION).putExtra(SpeindAPI.CLIENT_CMD, SpeindAPI.CC_VOICES_BUY_CMD);
                intent.putExtra(SpeindAPI.PARAM_SERVICE_PACKAGE_NAME, getPackageName());
	    		intent.putExtra("buy_intent", pendingIntent);
	    		sendBroadcast(intent);	            	            
	        } catch (RemoteException e) {
	            log("RemoteException while launching purchase flow for sku " + sku);
	        }
        }
    }

    private boolean checkProfile(String profile, String pass) {
    	log("checkProfile: " + profile + " " + pass);
    	return true;
    } 
    
    private void loadProfileData() {
    	log("loadProfileData");
		readConfigForProfile(speindData.currentProfile);
    	loadInfopointsForProfile(speindData.currentProfile);    	
    }

    private void readConfigForProfile(String profile) {
    	log("readConfigForProfile");
    	SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
    	speindData.speindConfig.infopoints_store_time=settings.getInt(profile+"_infopoints_store_time", 24*60*60);
    	speindData.speindConfig.max_play_time=settings.getInt(profile + "_max_play_time", 15 * 60);
    	speindData.speindConfig.read_full_article=settings.getBoolean(profile + "_read_full_article", false);
    	speindData.speindConfig.not_download_images_on_mobile_net=settings.getBoolean(profile+"_not_download_images_on_mobile_net", true);
    	speindData.speindConfig.not_off_screen=settings.getBoolean(profile + "_not_off_screen", false);
		speindData.speindConfig.speech_rate=settings.getFloat(profile + "_speech_rate", 1.0f);
        SpeindAPI.SpeindSettings.PostSettings ps = new SpeindAPI.SpeindSettings.PostSettings();
        ps.ask_before_post = settings.getBoolean(speindData.currentProfile+"ask_before_post", true);
        int pdc = settings.getInt(speindData.currentProfile+"plugins_count", 0);
        for (int i=0;i<pdc; i++) {
            String plugin_package = settings.getString(speindData.currentProfile+"plugin_package_" + i, "");
            boolean enable = settings.getBoolean(speindData.currentProfile+"plugin_post_enable_" + i, true);
            ps.post_plugins_data.put(plugin_package, enable);
        }
        speindData.speindConfig.post_settings=ps;
	}

    private void saveConfigForProfile() {
    	log("saveConfigForProfile");
    	SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
    	SharedPreferences.Editor editor = settings.edit();
        editor.putInt(speindData.currentProfile+"_infopoints_store_time", speindData.speindConfig.infopoints_store_time);
        editor.putInt(speindData.currentProfile+"_max_play_time", speindData.speindConfig.max_play_time);
        editor.putBoolean(speindData.currentProfile + "_read_full_article", speindData.speindConfig.read_full_article);
        editor.putBoolean(speindData.currentProfile+"_not_download_images_on_mobile_net", speindData.speindConfig.not_download_images_on_mobile_net);
        editor.putBoolean(speindData.currentProfile+"_not_off_screen", speindData.speindConfig.not_off_screen);
		editor.putFloat(speindData.currentProfile+"_speech_rate", speindData.speindConfig.speech_rate);

        editor.putBoolean(speindData.currentProfile+"ask_before_post", speindData.speindConfig.post_settings.ask_before_post);
        editor.putInt(speindData.currentProfile+"plugins_count", speindData.speindConfig.post_settings.post_plugins_data.size());
        int i=0;
        for (Map.Entry<String, Boolean> entry : speindData.speindConfig.post_settings.post_plugins_data.entrySet()) {
            editor.putString(speindData.currentProfile+"plugin_package_"+i, entry.getKey());
            editor.putBoolean(speindData.currentProfile+"plugin_post_enable_" + i, entry.getValue());
            i++;
        }

        editor.apply();
    }

    private void sendLangListChanged(String package_name) {
    	log("sendLangListChanged");
    	if (state>SpeindAPI.SPEIND_STATE_NEED_PROFILE) {
	    	Intent intent = new Intent(SpeindAPI.BROADCAST_ACTION).putExtra(SpeindAPI.DATAFEED_CLIENT_CMD, SpeindAPI.DF_CC_LANG_LIST_CHANGED).putExtra(SpeindAPI.PARAM_SERVICE_PACKAGE_NAME, getPackageName());
            intent.putExtra(SpeindAPI.PARAM_PACKAGE_NAME, package_name);
	    	intent.putStringArrayListExtra(SpeindAPI.PARAM_LANG_LIST, reader.getLangList());
	    	sendBroadcast(intent);
    	}
    }
    
    private void sendReaderPlayInfo(boolean full) {
    	log("sendReaderPlayInfo");
    	showNotification();
    	Intent intent = new Intent(SpeindAPI.BROADCAST_ACTION);
    	intent.putExtra(SpeindAPI.CLIENT_CMD, SpeindAPI.CC_PLAYING_INFO);
        intent.putExtra(SpeindAPI.PARAM_SERVICE_PACKAGE_NAME, getPackageName());
    	intent.putExtra(SpeindAPI.PARAM_STATE, state);    	
    	intent.putExtra(SpeindAPI.PARAM_PLAY_SORCE, SpeindAPI.SOURCE_INFOPOINT);
    	intent.putExtra(SpeindAPI.PARAM_CURRENT_INFOPOINT, speindData.currentInfoPoint);    	
    	intent.putExtra(SpeindAPI.PARAM_INFOPOINT_PLAY_FULL, full);
    	sendBroadcast(intent);
    }
    
    private void sendPlayState() {
    	log("sendPlayState");
    	if (state==SpeindAPI.SPEIND_STATE_STOP_READER&&speindData.infopoints.size()==0) {
    		setState(SpeindAPI.SPEIND_STATE_STOP_PLAYER);
    	}
		if (state==SpeindAPI.SPEIND_STATE_PLAY_PLAYER||state==SpeindAPI.SPEIND_STATE_STOP_PLAYER) {
			sendPlayerPlayInfo();
		} else if (state==SpeindAPI.SPEIND_STATE_PLAY_READER||state==SpeindAPI.SPEIND_STATE_STOP_READER) {
			sendReaderPlayInfo(speindData.speindConfig.read_full_article||readFull||speindData.infopoints.get(speindData.currentInfoPoint).readArticle);
		}
	}
    
    private void sendFullState() {
    	log("sendFullState");
		Intent intent = new Intent(SpeindAPI.BROADCAST_ACTION).putExtra(SpeindAPI.CLIENT_CMD, SpeindAPI.CC_FULL_STATE);
        intent.putExtra(SpeindAPI.PARAM_SERVICE_PACKAGE_NAME, getPackageName());
		String whatNewStr="";
		
		SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
		int oldAppVersion=settings.getInt("appVersion", 0);
				
		PackageInfo pInfo;
		int versionCode=0;
		try {
			pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
			versionCode=pInfo.versionCode;
		} catch (NameNotFoundException e) {
            e.printStackTrace();
        }
		if (versionCode!=oldAppVersion) {
			whatNewStr=getString(R.string.whats_new);
	    	SharedPreferences.Editor editor = settings.edit();
	    	editor.putInt("appVersion", versionCode);
	    	editor.apply();
		}
		intent.putExtra(SpeindAPI.PARAM_WHATNEW_STR, whatNewStr);
        speindData.putToIintent(intent);
		sendBroadcast(intent);
		sendPlayState(); 
    }

    private void sendConfig() {
    	log("sendConfig");
		Intent intent = new Intent(SpeindAPI.BROADCAST_ACTION).putExtra(SpeindAPI.CLIENT_CMD, SpeindAPI.CC_SETTINGS);
        intent.putExtra(SpeindAPI.PARAM_SERVICE_PACKAGE_NAME, getPackageName());
		speindData.speindConfig.putToIntent(intent);
		sendBroadcast(intent);    	
    }    

    private void sendLoadImagesOnMobileInetChanged(boolean param) {
    	log("sendLoadImagesOnMobileInetChanged");
    	Intent intent = new Intent(SpeindAPI.BROADCAST_ACTION).putExtra(SpeindAPI.DATAFEED_CLIENT_CMD, SpeindAPI.DF_CC_LOAD_IMAGES_CHANGED).putExtra(SpeindAPI.PARAM_SERVICE_PACKAGE_NAME, getPackageName());
    	intent.putExtra(SpeindAPI.PARAM_LOAD_IMAGES, !param);
    	sendBroadcast(intent);    	    	
    } 

    private void sendStoreInfopointTimeChanged(long param) {
    	log("sendStoreInfopointTimeChanged");
    	Intent intent = new Intent(SpeindAPI.BROADCAST_ACTION).putExtra(SpeindAPI.DATAFEED_CLIENT_CMD, SpeindAPI.DF_CC_STORE_INFOPOINTS_TIME_CHANGED).putExtra(SpeindAPI.PARAM_SERVICE_PACKAGE_NAME, getPackageName());
        intent.putExtra(SpeindAPI.PARAM_STORE_TIME, param);
    	sendBroadcast(intent);    	    	
    } 
    
    private void updatePosition(){
    	//log("updatePosition");
        handler.removeCallbacks(playerNextRunnable);
        handler.removeCallbacks(updatePositionRunnable);
        sendPlayPosition(player.getCurrentPosition(), player.getDuration());		
        if (player.getCurrentPosition()==player.getDuration()) handler.postDelayed(playerNextRunnable, 1000);
        else handler.postDelayed(updatePositionRunnable, 500);
    }

    private void saveCurrentInfoPoint() {
    	log("saveCurrentInfoPoint");
    	showNotification();
    	if (state>SpeindAPI.SPEIND_STATE_PREPARING) {
    		Intent intent = new Intent(SpeindAPI.BROADCAST_ACTION).putExtra(SpeindAPI.CLIENT_CMD, SpeindAPI.CC_INFOPOIN_POSITION).putExtra(SpeindAPI.PARAM_CURRENT_INFOPOINT, speindData.currentInfoPoint);
            intent.putExtra(SpeindAPI.PARAM_SERVICE_PACKAGE_NAME, getPackageName());
    		sendBroadcast(intent);  		
    	}
    	SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putInt(speindData.currentProfile + "_infopoint_current", speindData.currentInfoPoint);
        editor.putInt(speindData.currentProfile + "_infopoint_count", speindData.infopoints.size());
        editor.apply();
    }
    
    private boolean checkForPlayText() {
    	log("checkForPlayText");
        return speindData.currentInfoPoint < speindData.infopoints.size();
    }

    private boolean checkForPlayNextText() {  
    	log("checkForPlayNextText");
    	if (speindData.currentInfoPoint+1<speindData.infopoints.size()) {
    		int cip=speindData.currentInfoPoint;
    		while (true) {
    	    	if ((cip+1)<speindData.infopoints.size()) {
    	    		cip+=1;
    	    		InfoPoint infopoint = speindData.infopoints.get(cip);
        			if (infopoint!=null) {	    				
                        boolean needReadFull=speindData.speindConfig.read_full_article||readFull||infopoint.readArticle;
                        if ((infopoint.textExists||infopoint.titleExists)||(needReadFull&&(infopoint.articleExists))) {
                            return true;
                        }
        			}
    	    	} else {
    	    		break;
    	    	}
        	}
    		return false;    		
    	} else {
    		return false;
    	}
    }

    private boolean checkForPlayPrevText() {
    	log("checkForPlayPrevText");
    	if (speindData.currentInfoPoint-1>=0) {
    		int cip=speindData.currentInfoPoint;
    		while (true) {
    	    	if ((cip-1)>0) {
    	    		cip-=1;
    	    		InfoPoint infopoint = speindData.infopoints.get(cip);
        			if (infopoint!=null) {	    				
                        boolean needReadFull=speindData.speindConfig.read_full_article||readFull||infopoint.readArticle;
                        if ((infopoint.textExists||infopoint.titleExists)||(needReadFull&&(infopoint.articleExists))) {
                            return true;
                        }
        			}
    	    	} else {
    	    		break;
    	    	}
        	}
    		return false;    		
    	} else {
    		return false;
    	}
    }

    private void readerPlay() {
    	log("readerPlay");
    	if (reader!=null) {
    		if (!reader.isSpeak()) {
	    		if (speindData.infopoints.size()>0&&speindData.infopoints.size()>speindData.currentInfoPoint) {
	    			InfoPoint infopoint = speindData.infopoints.get(speindData.currentInfoPoint);
	    			if (infopoint!=null) {	    				
	    				SpeindAPI.InfoPointData data=infopoint.getData(speindData.currentProfile);
	    				if (data!=null) {
	    					
	    					boolean waitAfterRead=false;
	    					SpeindAPI.DataFeedSettingsInfo dfsi=findDataFeedSettingsInfo(infopoint.processingPlugin);
	    					if (dfsi!=null) {
	    						waitAfterRead=dfsi.isNeedConfirmRead();
	    					}
	    					
	    					handler.removeCallbacks(updatePositionRunnable);
	    					if (speindData.speindConfig.max_play_time>1) {
					    		playerPlayTime=0;
					    		lastPlayerStartTime=0;
					    		if (lastReaderStartTime>0)
				    	    		readerPlayTime+=(new Date()).getTime()-lastReaderStartTime;					    			
				    			lastReaderStartTime=(new Date()).getTime();
	    					}
	    					
	    					if ((speindData.speindConfig.read_full_article||readFull||infopoint.readArticle)&&infopoint.articleExists) {
	    						data.postTextVocalizing=""+Html.fromHtml("<p>"+data.postArticle.replace(data.postTitle, "")+"</p>");
	    					}
	    					
	    					if (waitAfterRead) {
	        					Configuration conf = getResources().getConfiguration();
	        			    	Locale localeOld=conf.locale;
	        			    	conf.locale = new Locale(data.postLang);
	        			    	Resources resources = new Resources(getAssets(), getResources().getDisplayMetrics(), conf);
	    						data.postTextVocalizing=""+Html.fromHtml("<p>"+resources.getString(R.string.confirm_read)+"</p>");
	        					conf.locale=localeOld;
	        			    	new Resources(getAssets(), getResources().getDisplayMetrics(), conf);

	    					}
	    					
	    					String readText;
                            Log.e("!!!", prevSender+" "+data.postSender);
				    		if (prevSender.equals(data.postSender)) {
				    			if (data.postTitleVocalizing.equals(""))
				    				readText=data.postTextVocalizing;
				    			else
				    				readText=""+Html.fromHtml("<p>"+data.postTitleVocalizing+".</p>")+data.postTextVocalizing;
				    		} else {
				    			prevSender=data.postSender;
				    			if (data.postTitleVocalizing.equals(""))
				    				readText=""+Html.fromHtml("<p>"+data.postSenderVocalizing+".</p>")+data.postTextVocalizing;
				    			else
				    				readText=""+Html.fromHtml("<p>"+data.postSenderVocalizing+" "+data.postTitleVocalizing+".</p>")+data.postTextVocalizing;
				    		}
		    				reader.speak(readText, data.postLang, waitAfterRead);		    									    			

	    				}
	    			}
	    		}
    		}
    	}
    }

    private void readerResume() {
    	log("readerResume");
    	reader.resume();
    	setState(SpeindAPI.SPEIND_STATE_PLAY_READER);
    }
    
    private void readerStop() {
    	log("readerStop()");
    	if (lastReaderStartTime>0) {
    		readerPlayTime+=((new Date()).getTime()-lastReaderStartTime);
    		lastReaderStartTime=0;
    	}
    	if (reader!=null) {
			reader.stop();
    	}
    }
    
    private void readerPause() {
    	log("readerPause");
    	reader.pause();
    }
    
    private void readerNext(boolean startplay) {
    	log("readerNext");
    	readerStop();
    	while (true) {
	    	if ((speindData.currentInfoPoint+1)<speindData.infopoints.size()) {
	    		speindData.currentInfoPoint+=1;
	    		saveCurrentInfoPoint();
	    		
	    		InfoPoint infopoint = speindData.infopoints.get(speindData.currentInfoPoint);
    			if (infopoint!=null) {
                    boolean needReadFull=speindData.speindConfig.read_full_article||readFull||infopoint.readArticle;
                    if ((infopoint.textExists||infopoint.titleExists)||(needReadFull&&(infopoint.articleExists))) {
                        sendReaderPlayInfo(needReadFull);
                        if (startplay) readerPlay();
                        else setState(SpeindAPI.SPEIND_STATE_STOP_READER);
                        break;
                    }
    			}
	    	} else {
	    		break;
	    	}
    	}
    }

    private void readerPrev(boolean startplay) {
    	log("readerPrev");
    	readerStop();
    	while (true) {
	    	if ((speindData.currentInfoPoint-1)>=0) {
	    		speindData.currentInfoPoint-=1;
	    		saveCurrentInfoPoint();
	    		
	    		InfoPoint infopoint = speindData.infopoints.get(speindData.currentInfoPoint);
    			if (infopoint!=null) {	    				
                    boolean needReadFull=speindData.speindConfig.read_full_article||readFull||infopoint.readArticle;
                    if ((infopoint.textExists||infopoint.titleExists)||(needReadFull&&(infopoint.articleExists))) {
                        sendReaderPlayInfo(needReadFull);
                        if (startplay) readerPlay();
                        else setState(SpeindAPI.SPEIND_STATE_STOP_READER);
                        break;
                    }
    			}
	    	} else {
	    		break;
	    	}
        }
    }

    private void playerPlay() {
    	log("playerPlay");
    	if (player!=null) {
	    	if (!player.isPlaying()) { 
		    	if (!currentPlayFile.equals("")) {
			    	try {
		    			stopBackgroundMusic();
			    		prevSender="";
			    		player.reset();
			    		player.setDataSource(this, Uri.parse(currentPlayFile));
			    		player.prepare();
			    		player.start();
			    		if (speindData.speindConfig.max_play_time>1) {
				    		readerPlayTime=0;
				    		lastReaderStartTime=0;
				    		if (lastPlayerStartTime>0)
				    			playerPlayTime+=(new Date()).getTime()-lastPlayerStartTime;
				    		lastPlayerStartTime=(new Date()).getTime();
			    		}
				    	setState(SpeindAPI.SPEIND_STATE_PLAY_PLAYER);
				    	updatePosition();
			    	} catch (IllegalArgumentException e) {
                        SpeindApplication.reportEvent("Engine", "playerPlay IllegalArgumentException", "");
			        } catch (IllegalStateException e) {
                        SpeindApplication.reportEvent("Engine", "playerPlay IllegalStateException", "");
			        } catch (IOException e) {
                        SpeindApplication.reportEvent("Engine", "playerPlay IOException", "");
			        }
		    	} else {
                    SpeindApplication.reportEvent("Engine", "playerPlay play file empty", "");
		    	}
	    	} else {
                SpeindApplication.reportEvent("Engine", "playerPlay player in play state", "");
	    	}
    	} else {
            SpeindApplication.reportEvent("Engine", "playerPlay player is null", "");
    	}
    }

    private void playerResume() {
    	log("playerResume");
    	player.start();
    	setState(SpeindAPI.SPEIND_STATE_PLAY_PLAYER);
        updatePosition();
    }
    
    private void playerStop() {
    	log("playerStop");
    	if (lastPlayerStartTime>0) {
    		playerPlayTime+=(new Date()).getTime()-lastPlayerStartTime;
    		lastPlayerStartTime=0;
    	}
    	if (player!=null) {
	    	if (player.isPlaying()) {
	    		handler.removeCallbacks(updatePositionRunnable);
	    		player.stop();
	    	}
    	}
    }
    
    private void playerPause() {
    	log("playerPause");
        handler.removeCallbacks(updatePositionRunnable);
    	player.pause();
    }
        
    private void playerNext(boolean startplay) {
    	log("playerNext");
    	playerStop();
    	if (!playerPaths.isEmpty()) {
	    	java.util.Random r = new  java.util.Random();
	        int pos = 0;
	        if (playerPaths.size()>1) {
		        pos = r.nextInt(playerPaths.size());
	        }
	    	currentPlayFile=nextPlayFile;
	    	nextPlayFile=playerPaths.elementAt(pos);
    	} else {
    		currentPlayFile = "android.resource://com.maple.speind/raw/music";
    	}
		sendPlayerPlayInfo();
    	if (startplay) playerPlay();
    	else setState(SpeindAPI.SPEIND_STATE_STOP_PLAYER);
    }

    private SpeindAPI.DataFeedSettingsInfo findDataFeedSettingsInfo(String packageName) {
    	log("findDataFeedSettingsInfo");
        int cnt=speindData.dataFeedsettingsInfos.size();
		for (int i=0; i<cnt; i++) {
			if (speindData.dataFeedsettingsInfos.get(i).packageName.equals(packageName)) {
				return speindData.dataFeedsettingsInfos.get(i);
			}
		}
		return null;
    } 
   
    private void stopPlugins(){
    	log("stopPlugins");
		Intent intent = new Intent(SpeindAPI.BROADCAST_ACTION).putExtra(SpeindAPI.DATAFEED_CLIENT_CMD, SpeindAPI.DF_CC_STOP).putExtra(SpeindAPI.PARAM_SERVICE_PACKAGE_NAME, getPackageName());
		sendBroadcast(intent);    		    	
		intent = new Intent(SpeindAPI.BROADCAST_ACTION).putExtra(SpeindAPI.CLIENT_CMD, SpeindAPI.CC_EXIT);
        intent.putExtra(SpeindAPI.PARAM_SERVICE_PACKAGE_NAME, getPackageName());
		sendBroadcast(intent);    		    	
    }

    private void initPlugins(){
    	log("initPlugins"); 

    	ArrayList<SpeindAPI.DataFeedSettingsInfo> removePlugins = new ArrayList<>();
    	removePlugins.addAll(speindData.dataFeedsettingsInfos);
    	ArrayList<String> removePackages = new ArrayList<>();
    	ArrayList<Runnable> startPlugins = new ArrayList<>();
    	
    	PackageManager mngr = getApplicationContext().getPackageManager();
    	List<PackageInfo> list = mngr.getInstalledPackages(PackageManager.GET_SERVICES);
    	for (PackageInfo packageInfo : list) {
        	ServiceInfo services[]=packageInfo.services;
        	if (services!=null) {
                for (ServiceInfo service : services) {
                    //if (service.name.endsWith("rssreceiver.SpeindDataFeed")) {
                    if (service.name.endsWith("SpeindDataFeed")) {
                        DataFeedSettingsInfo info = findDataFeedSettingsInfo(packageInfo.packageName);
                        if (info != null) {
                            removePlugins.remove(info);
                        } else {
                            final String pn = packageInfo.packageName;
                            final String sn = service.name;
                            startPlugins.add(new Runnable() {
                                @Override
                                public void run() {
                                    startService(new Intent().setClassName(pn, sn));
                                    handler.postDelayed(new Runnable() {
                                        @Override
                                        public void run() {
                                            Intent intent = new Intent(SpeindAPI.BROADCAST_ACTION).putExtra(SpeindAPI.DATAFEED_CLIENT_CMD, SpeindAPI.DF_CC_INIT).putExtra(SpeindAPI.PARAM_SERVICE_PACKAGE_NAME, getPackageName()).putExtra(SpeindAPI.PARAM_PACKAGE_NAME, pn);
                                            sendBroadcast(intent);
                                        }
                                    }, 1000);
                                }
                            });
                        }
                    }
                }
        	}
    	}
    	if (removePlugins.size()>0) {
        	speindData.dataFeedsettingsInfos.removeAll(removePlugins);
        	for (SpeindAPI.DataFeedSettingsInfo info : removePlugins) {
    			removePackages.add(info.packageName);
        	}        	
        	Intent intent=new Intent(SpeindAPI.BROADCAST_ACTION);
    		intent.putExtra(SpeindAPI.CLIENT_CMD, SpeindAPI.CC_REMOVE_DATAFEED_SETTINGS_INFOS);
            intent.putExtra(SpeindAPI.PARAM_SERVICE_PACKAGE_NAME, getPackageName());
    		intent.putStringArrayListExtra(SpeindAPI.PARAM_DATAFEED_PACKAGES, removePackages);
    		sendBroadcast(intent);
    	}
    	for (Runnable ss : startPlugins) {
    		ss.run();
    	}
    }

    private void addToInfoPoints(SpeindAPI.InfoPoint infopoint, boolean startRead) {
    	log("addToInfoPoints");
    	if ((new Date()).getTime()-(infopoint.postTime.getTime())<=(speindData.speindConfig.infopoints_store_time*1000)) {
    		boolean interupt=false;

    		int infopointPos=speindData.infopoints.size();
    		int sp=speindData.currentInfoPoint+1;	

            if (infopoint.priority<SpeindAPI.InfoPoint.PRIORITY_MEDIUM) {
    			if ((state==SpeindAPI.SPEIND_STATE_PLAY_READER||state==SpeindAPI.SPEIND_STATE_STOP_READER)&&(speindData.infopoints.get(speindData.currentInfoPoint).priority>infopoint.priority)) { // 
    				sp=speindData.currentInfoPoint;
    				interupt=true;
    			} else if (state==SpeindAPI.SPEIND_STATE_PLAY_PLAYER||state==SpeindAPI.SPEIND_STATE_STOP_PLAYER||state==SpeindAPI.SPEIND_STATE_PREPARING) {
    				interupt=true;
    			}
    		}

            SQLiteDatabase db = dbHelper.getWritableDatabase();
            db.beginTransaction();
            //db.beginTransactionNonExclusive();
            String[] columns = new String[] { "position" };
            String selection = "profile=? and position >= ? and (priority > ? or (priority = ? and postTime > ?)) and not (position = ? and priority = ?)";
            String[] selectionArgs = new String[] {
                    speindData.currentProfile,
                    String.valueOf(sp),
                    String.valueOf(infopoint.priority),
                    String.valueOf(infopoint.priority),
                    String.valueOf(infopoint.postTime.getTime()/1000),
                    String.valueOf(speindData.currentInfoPoint),
                    String.valueOf(infopoint.priority),
            };
            String orderBy = "position";
            Cursor c = db.query("speindinfopoints", columns, selection, selectionArgs, null, null, orderBy, "1");
            if (c.moveToFirst()) {
                infopointPos = c.getInt(c.getColumnIndex("position"));
            }
            c.close();

    		//for (int i=sp;i<speindData.infopoints.size();i++) {
        	//	if (speindData.infopoints.get(i).priority>infopoint.priority||(speindData.infopoints.get(i).priority==infopoint.priority&&infopoint.postTime.getTime()<speindData.infopoints.get(i).postTime.getTime())) {
        	//		if (i==speindData.currentInfoPoint&&speindData.infopoints.get(i).priority==infopoint.priority) {
        	//			continue;
        	//		}
        	//		infopointPos=i;
        	//		break;
        	//	}
    		//}


            log("New pos: " + infopointPos);
    		speindData.infopoints.add(infopointPos, infopoint);

            db.execSQL("UPDATE speindinfopoints SET position = position+1 WHERE profile = ? and position >= ?", new String[]{speindData.currentProfile, String.valueOf(infopointPos)});
            ContentValues cv = new ContentValues();
            cv.put("id", infopoint.id);
            cv.put("position", infopointPos);
            cv.put("profile", speindData.currentProfile);
            cv.put("processingPlugin", infopoint.processingPlugin);
            cv.put("postTime", String.valueOf(infopoint.postTime.getTime() / 1000));
            cv.put("priority", String.valueOf(infopoint.priority));
            cv.put("readArticle", (infopoint.readArticle ? 1 : 0));
            cv.put("titleExists", (infopoint.titleExists ? 1 : 0));
            cv.put("textExists", (infopoint.textExists ? 1 : 0));
            cv.put("articleExists", (infopoint.articleExists ? 1 : 0));
            db.insert("speindinfopoints", null, cv);

            db.setTransactionSuccessful();
            db.endTransaction();

            //saveInfopoint(infopointPos);
	    	
	    	Intent intent=new Intent(SpeindAPI.BROADCAST_ACTION);
	    	intent.putExtra(SpeindAPI.CLIENT_CMD, SpeindAPI.CC_ADD_INFOPOINT);
            intent.putExtra(SpeindAPI.PARAM_SERVICE_PACKAGE_NAME, getPackageName());
	    	intent.putExtra(SpeindAPI.PARAM_ADD_POSITION, infopointPos);
	    	infopoint.putToIntent(intent);
	    	sendBroadcast(intent);
	    	
	    	showNotification();
	    	
	    	if (interupt) onPlayInfoPoint(infopointPos);
            if (!(state == SpeindAPI.SPEIND_STATE_PLAY_PLAYER||state==SpeindAPI.SPEIND_STATE_PLAY_READER)) if (startRead) onPlay();
    	} else {
    		infopoint.deleteData(speindData.currentProfile);
    	}
    }

    private void loadInfopointsForProfile(String profile) {
    	log("loadInfopointsForProfile");
        pinboard.clear();
    	speindData.infopoints.clear();
    	SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        speindData.currentInfoPoint = settings.getInt(profile+"_infopoint_current", 0);

        ArrayList<String> pinboardIds = new ArrayList<>();

        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.beginTransaction();
        //db.beginTransactionNonExclusive();;
        String selection = "profile = ?";
        String[] selectionArgs = new String[] { profile };

        String orderBy = "postTime";
        Cursor c = db.query("speindpinboard", null, selection, selectionArgs, null, null, orderBy);
        if (c.moveToFirst()) {
            do {
                String id = c.getString(c.getColumnIndex("id"));
                String processingPlugin = c.getString(c.getColumnIndex("processingPlugin"));
                Calendar calendar = Calendar.getInstance();
                calendar.setTimeInMillis((long)(c.getInt(c.getColumnIndex("postTime"))) * 1000);
                Date date = calendar.getTime();
                int priority = c.getInt(c.getColumnIndex("priority"));
                //int position = c.getInt(c.getColumnIndex("position"));
                boolean readArticle = c.getInt(c.getColumnIndex("readArticle"))!=0;
                boolean titleExists = c.getInt(c.getColumnIndex("titleExists"))!=0;
                boolean textExists = c.getInt(c.getColumnIndex("textExists"))!=0;
                boolean articleExists = c.getInt(c.getColumnIndex("articleExists"))!=0;
                InfoPoint infopoint=new InfoPoint(id, processingPlugin, date, priority, readArticle, titleExists, textExists, articleExists);
                SpeindAPI.InfoPointData data=infopoint.getData(profile);

                if (data!=null) {
                    pinboardIds.add(infopoint.id);
                    pinboard.add(infopoint);
                } else {
                    db.delete("speindpinboard", "id = ? and profile = ?", new String[]{infopoint.id, profile});
                    infopoint.deleteData(profile);
                }
            } while (c.moveToNext());
        }
        c.close();

        orderBy = "position";
        c = db.query("speindinfopoints", null, selection, selectionArgs, null, null, orderBy);
        int decPos=0;
        if (c.moveToFirst()) {
            do {
                String id = c.getString(c.getColumnIndex("id"));
                String processingPlugin = c.getString(c.getColumnIndex("processingPlugin"));
                Calendar calendar = Calendar.getInstance();
                calendar.setTimeInMillis((long)(c.getInt(c.getColumnIndex("postTime"))) * 1000);
                Date date = calendar.getTime();
                int priority = c.getInt(c.getColumnIndex("priority"));
                int position = c.getInt(c.getColumnIndex("position"));
                boolean readArticle = c.getInt(c.getColumnIndex("readArticle"))!=0;
                boolean titleExists = c.getInt(c.getColumnIndex("titleExists"))!=0;
                boolean textExists = c.getInt(c.getColumnIndex("textExists"))!=0;
                boolean articleExists = c.getInt(c.getColumnIndex("articleExists"))!=0;
                InfoPoint infopoint=new InfoPoint(id, processingPlugin, date, priority, readArticle, titleExists, textExists, articleExists);
                SpeindAPI.InfoPointData data=infopoint.getData(profile);
                if (data!=null&&(new Date()).getTime()-(date.getTime())<=(speindData.speindConfig.infopoints_store_time*1000)) {
                    speindData.infopoints.add(infopoint);
                } else {
                    db.delete("speindinfopoints", "id = ? and profile = ?", new String[]{infopoint.id, profile});
                    db.execSQL("UPDATE speindinfopoints SET position = position-1 WHERE profile = ? and position >= ?", new String[]{profile, String.valueOf(position - decPos)});
                    if (!pinboardIds.contains(infopoint.id)) infopoint.deleteData(profile);
                    if (speindData.currentInfoPoint>position-decPos) {
                        speindData.currentInfoPoint-=1;
                    }
                    decPos+=1;
                }

            } while (c.moveToNext());
        }
        c.close();

        db.setTransactionSuccessful();
        db.endTransaction();

        if (speindData.currentInfoPoint>=speindData.infopoints.size()) speindData.currentInfoPoint=speindData.infopoints.size()-1;
        if (speindData.currentInfoPoint<0) speindData.currentInfoPoint=0;
        saveCurrentInfoPoint();

        if (speindData.infopoints.size()==0&&pinboard.size()==0) {
            //deleteDir(new File(SpeindAPI.SPEIND_IMAGES_DIR));
            deleteDir(new File(SpeindAPI.SPEIND_DIR + "infopoints"+File.separator+profile));
        }

        showNotification();
    }

    @SuppressWarnings("unchecked")
    private ArrayList<String> readSharedQueue() {
    	ArrayList<String> sharedQueue=null; 
    	File sharedQueueFile = new File(getDir("data", MODE_PRIVATE), "sharedQueue");    
    	ObjectInputStream inputStream;
		try {
			inputStream = new ObjectInputStream(new FileInputStream(sharedQueueFile));
            Object o = inputStream.readObject();
			if (o instanceof ArrayList) sharedQueue = (ArrayList<String>) o;
	    	inputStream.close();
		} catch (IOException | ClassNotFoundException e) {
			sharedQueue=null;
		}
		if (sharedQueue==null) sharedQueue=new ArrayList<>();
		return sharedQueue;
    }
    
    private void saveSharedQueue(ArrayList<String> sharedQueue) {
    	File sharedQueueFile = new File(getDir("data", MODE_PRIVATE), "sharedQueue");  
    	ObjectOutputStream outputStream;
		try {
			outputStream = new ObjectOutputStream(new FileOutputStream(sharedQueueFile));
	    	outputStream.writeObject(sharedQueue);
	    	outputStream.flush();
	    	outputStream.close();
		} catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    private void queueSharedText(String sharedText) {
    	log("queueSharedText");    	
    	ArrayList<String> sharedQueue=readSharedQueue();
    	if (!sharedQueue.contains(sharedText)) sharedQueue.add(sharedText);    	
    	saveSharedQueue(sharedQueue);
    }
    
    private void processSharedQueue() {
    	log("processSharedQueue");
    	ArrayList<String> sharedQueue=readSharedQueue();
    	ArrayList<String> processedTexts=new ArrayList<>();
    	for (String sharedText : sharedQueue) {
        	if (SpeindAPI.isURL(sharedText)) {
        		if (haveNetworkConnection) {
        			log("Shared URL: "+sharedText);
        			processSharedURL(sharedText);
        			processedTexts.add(sharedText);
        		}
            } else {
            	log("Shared text: "+sharedText);
            	processSharedText(sharedText);
    			processedTexts.add(sharedText);
            }
        }
    	for (String removeText : processedTexts) {
    		sharedQueue.remove(removeText);
    	}
    	saveSharedQueue(sharedQueue);
    }
    
    private void processSharedText(String sharedText) {
    	log("processSharedText");
    	
    	SpeindAPI.SendInfoPointParams sendInfoPointParams=new SpeindAPI.SendInfoPointParams(); 
    	sendInfoPointParams.postTime=new Date();
    	sendInfoPointParams.postSender=getString(R.string.shared_text);
    	sendInfoPointParams.senderBmp=null;
    	sendInfoPointParams.postBmp=null;
    	sendInfoPointParams.postTitle="";
    	sendInfoPointParams.postOriginText=sharedText;
    	sendInfoPointParams.postLink="";
    	sendInfoPointParams.postPluginData="";
    	sendInfoPointParams.postTitleVocalizing="";
    	sendInfoPointParams.postTextVocalizing=sharedText;
    	sendInfoPointParams.lang=SpeindAPI.getLang(sharedText, reader.getLangList());	   
    	sendInfoPointParams.readArticle=false;

    	sendInfoPointParams.priority=InfoPoint.PRIORITY_HIGHEST;
    	
    	Configuration conf = getResources().getConfiguration();
    	Locale localeOld=conf.locale;
    	conf.locale = new Locale(sendInfoPointParams.lang);
    	Resources resources = new Resources(getAssets(), getResources().getDisplayMetrics(), conf);

    	sendInfoPointParams.postSenderVocalizing=resources.getString(R.string.shared_text_vocalizing);

    	conf.locale=localeOld;
    	new Resources(getAssets(), getResources().getDisplayMetrics(), conf);
    	
    	sendInfoPointParams.startRead=true;

        DataFeedSettingsInfo info = new DataFeedSettingsInfo(this, "", null, false);
    	SpeindAPI.sendInfoPoint(this, getPackageName(), speindData.currentProfile, reader.getLangList(), info, sendInfoPointParams, !speindData.speindConfig.not_download_images_on_mobile_net);
    }

    
    private class ProcessURLCheckTask extends AsyncTask<String, Integer, Integer> {
		@Override
		protected Integer doInBackground(String... arg0) {
			log("ProcessURLCheckTask");
			String link=arg0[0];
			if (!(link.startsWith("http://")||link.startsWith("https://"))) {
				link="http://"+link;
    		}
			ArrayList<RssItem> items=RssItem.getRssItems(link, "", "", "", "");
			if (items==null||items.size()==0) {
				log("processArticleURL");
				
				String sharedText=SpeindAPI.getArticleFromURL(link);
				
				if (sharedText.length()>0) {
					SpeindAPI.SendInfoPointParams sendInfoPointParams=new SpeindAPI.SendInfoPointParams(); 
			    	sendInfoPointParams.postTime=new Date();
			    	
			    	sendInfoPointParams.postSender=getString(R.string.shared_text);
			    	try {
						URI uri = new URI(link);
				    	sendInfoPointParams.postSender=uri.getHost();
					} catch (URISyntaxException e) {
                        e.printStackTrace();
                    }
			    	
			    	sendInfoPointParams.senderBmp=null;
			    	sendInfoPointParams.postBmp=null;
			    	sendInfoPointParams.postTitle="";
			    	sendInfoPointParams.postOriginText=sharedText;
			    	sendInfoPointParams.postLink="";
			    	sendInfoPointParams.postPluginData="";
			    	sendInfoPointParams.postTitleVocalizing="";
			    	sendInfoPointParams.postTextVocalizing=""+Html.fromHtml(sharedText);
			    	sendInfoPointParams.lang=SpeindAPI.getLang(""+Html.fromHtml(sharedText), reader.getLangList());	   
			    	sendInfoPointParams.readArticle=false;

			    	sendInfoPointParams.priority=InfoPoint.PRIORITY_HIGHEST;
			    	
			    	Configuration conf = getResources().getConfiguration();
			    	Locale localeOld=conf.locale;
			    	conf.locale = new Locale(sendInfoPointParams.lang);
			    	Resources resources = new Resources(getAssets(), getResources().getDisplayMetrics(), conf);

			    	sendInfoPointParams.postSenderVocalizing=resources.getString(R.string.shared_url_from)+": "+(sendInfoPointParams.postSender.startsWith("www.") ? sendInfoPointParams.postSender.substring(4) : sendInfoPointParams.postSender);

			    	conf.locale=localeOld;
			    	new Resources(getAssets(), getResources().getDisplayMetrics(), conf);
			    	
			    	sendInfoPointParams.startRead=true;

                    DataFeedSettingsInfo info = new DataFeedSettingsInfo(SpeindService.this, "", null, false);
                    SpeindAPI.sendInfoPoint(SpeindService.this, getPackageName(), speindData.currentProfile, reader.getLangList(), info, sendInfoPointParams, !speindData.speindConfig.not_download_images_on_mobile_net);
					
				}

				return -1;
			} else {
				log("processRSSURL");
				
				String lang=SpeindAPI.getLang(items.get(0).getDescription(), reader.getLangList());
				String name=items.get(0).getSender();
				
				
				Intent intent=new Intent(SpeindService.this, UserFeeds.class);
				intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);		
				intent.putExtra("profile", speindData.currentProfile);
				intent.putStringArrayListExtra("langs", reader.getLangList());
				intent.putExtra("feed_name", name);
				intent.putExtra("feed_lang", lang);
				intent.putExtra("feed_url", link);
				startActivity(intent);
				
				return 0;
			}
		}
	}
    
    private void processSharedURL(String sharedURL) {
    	log("processSharedURL");
    	ProcessURLCheckTask checkTask=new ProcessURLCheckTask();
    	checkTask.execute(sharedURL);
    }

    @SuppressWarnings("deprecation")
    private void requestAudioFocus() {
    	if (!haveAudioFocus) {
	    	int result = ((AudioManager)getSystemService(Context.AUDIO_SERVICE)).requestAudioFocus(SpeindService.this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);    	   
	    	if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
	    		((AudioManager)getSystemService(AUDIO_SERVICE)).registerMediaButtonEventReceiver(new ComponentName(getPackageName(), MediaButtonReceiver.class.getName()));
	    		haveAudioFocus=true;
	        	Log.e("[---!!!---]", "requestAudioFocus OK!!!");
	    	}
    	}    	
    }

    @SuppressWarnings("deprecation")
    private void freeAudioFocus() {
    	if (haveAudioFocus) {
			((AudioManager)getSystemService(AUDIO_SERVICE)).unregisterMediaButtonEventReceiver(new ComponentName(getPackageName(), MediaButtonReceiver.class.getName()));
			((AudioManager)getSystemService(AUDIO_SERVICE)).abandonAudioFocus(this);
			haveAudioFocus=false;
    	}
    }

    private void clearPause() {
        if (onPauseState!=-1) {
            onPauseState=-1;
            needResume = false;
            if (state==SpeindAPI.SPEIND_STATE_STOP_PLAYER) {
                playerStop();
            } else if (state==SpeindAPI.SPEIND_STATE_STOP_READER) {
                readerStop();
            }
        }
    }

    private void addToPinboard(String infopointID) {
        if (SpeindConfig.exclude_pinboard) return;

        SQLiteDatabase db = dbHelper.getWritableDatabase();

        SpeindAPI.InfoPoint infopoint = null;

        String selection = "profile = ? and id = ?";
        String[] selectionArgs = new String[] { speindData.currentProfile, infopointID};
        Cursor c = db.query("speindinfopoints", null, selection, selectionArgs, null, null, null);
        if (c.moveToFirst()) {
            String id = c.getString(c.getColumnIndex("id"));
            String processingPlugin = c.getString(c.getColumnIndex("processingPlugin"));
            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis((long)(c.getInt(c.getColumnIndex("postTime"))) * 1000);
            Date date = calendar.getTime();
            int priority = c.getInt(c.getColumnIndex("priority"));
            int position = c.getInt(c.getColumnIndex("position"));
            boolean readArticle = c.getInt(c.getColumnIndex("readArticle"))!=0;
            boolean titleExists = c.getInt(c.getColumnIndex("titleExists"))!=0;
            boolean textExists = c.getInt(c.getColumnIndex("textExists"))!=0;
            boolean articleExists = c.getInt(c.getColumnIndex("articleExists"))!=0;
            infopoint=new InfoPoint(id, processingPlugin, date, priority, readArticle, titleExists, textExists, articleExists);
            SpeindAPI.InfoPointData data=infopoint.getData(speindData.currentProfile);
            if (data==null) {
                infopoint=null;
            }
        }
        c.close();

        if (infopoint!=null) {
            ContentValues cv = new ContentValues();
            cv.put("id", infopoint.id);
            cv.put("position", 0);
            cv.put("profile", speindData.currentProfile);
            cv.put("processingPlugin", infopoint.processingPlugin);
            cv.put("postTime", String.valueOf(infopoint.postTime.getTime() / 1000));
            cv.put("priority", String.valueOf(infopoint.priority));
            cv.put("readArticle", (infopoint.readArticle ? 1 : 0));
            cv.put("titleExists", (infopoint.titleExists ? 1 : 0));
            cv.put("textExists", (infopoint.textExists ? 1 : 0));
            cv.put("articleExists", (infopoint.articleExists ? 1 : 0));
            if (db.insert("speindpinboard", null, cv)!=-1) {
                pinboard.add(infopoint);
                sendInfoMessage(getString(R.string.infopoint_successfully_added_to_pinboard));
            } else {
                sendInfoMessage(getString(R.string.infopoint_already_in_pinboard));
            }
        } else {
            sendInfoMessage(getString(R.string.something_wrong_bad_infopoint));
        }
    }

    private void removeFromPinboard(String infopointID) {
        if (SpeindConfig.exclude_pinboard) return;

        for (int i=0; i<pinboard.size();i++) {
            if (pinboard.get(i).id.equals(infopointID)) {
                pinboard.remove(i);
                SQLiteDatabase db = dbHelper.getWritableDatabase();
                db.delete("speindpinboard", "id = ? and profile = ?", new String[]{infopointID, speindData.currentProfile});
                sendInfoMessage(getString(R.string.infopoint_successfully_removed_from_pinboard));
                break;
            }
        }
    }

    private void clearPinboard() {
        if (SpeindConfig.exclude_pinboard) return;

        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.delete("speindpinboard", "profile = ?", new String[]{speindData.currentProfile});
        pinboard.clear();
    }

    private void sendPinboard(String email, int format, boolean needClear) {
        if (SpeindConfig.exclude_pinboard) return;

        final List<NameValuePair> nameValuePairs = new ArrayList<>(2);
        nameValuePairs.add(new BasicNameValuePair("email", email));
        nameValuePairs.add(new BasicNameValuePair("format", "" + format));
        nameValuePairs.add(new BasicNameValuePair("datetime", (new Date()).toString()));

        int p=0;
        for (int i=0; i<pinboard.size();i++) {
            SpeindAPI.InfoPoint infoPoint = pinboard.get(i);
            if (infoPoint!=null) {
                SpeindAPI.InfoPointData data = infoPoint.getData(speindData.currentProfile);
                if (data!=null) {
                    nameValuePairs.add(new BasicNameValuePair("infopoints["+p+"][postTime]", ""+infoPoint.postTime.getTime()/1000));
                    nameValuePairs.add(new BasicNameValuePair("infopoints["+p+"][postSender]", data.postSender));
                    nameValuePairs.add(new BasicNameValuePair("infopoints["+p+"][senderBmpURL]", data.senderBmpURL));
                    nameValuePairs.add(new BasicNameValuePair("infopoints["+p+"][postBmpURL]", data.postBmpURL));
                    nameValuePairs.add(new BasicNameValuePair("infopoints["+p+"][postTitle]", data.postTitle));
                    nameValuePairs.add(new BasicNameValuePair("infopoints["+p+"][postOriginText]", data.postOriginText));
                    nameValuePairs.add(new BasicNameValuePair("infopoints["+p+"][postLink]", data.postLink));
                    nameValuePairs.add(new BasicNameValuePair("infopoints["+p+"][postURL]", (data.postURL.isEmpty()) ? data.postLink : data.postURL));
                    nameValuePairs.add(new BasicNameValuePair("infopoints["+p+"][postLang]", data.postLang));
                    p+=1;
                }
            }
        }

        if (postData("/test.php", nameValuePairs)) {

            sendInfoMessage(getString(R.string.pinboard_successfully_sended));

            if (needClear) {
                clearPinboard();
            }
        } else {

            sendInfoMessage(getString(R.string.pinboard_send_error));

        }
    }

    public boolean postData(String cmd, List<NameValuePair> nameValuePairs) {
        HttpClient httpclient = new DefaultHttpClient();
        HttpPost httppost = new HttpPost("http://192.168.1.2"+cmd);
        try {
            httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs, "UTF-8"));
            HttpResponse response = httpclient.execute(httppost);

            HttpEntity entity = response.getEntity();
            StringBuilder sb = new StringBuilder();
            try {
                BufferedReader reader = new BufferedReader(new InputStreamReader(entity.getContent()), 65728);
                String line;

                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            Log.d("[---!!!---]", sb.toString());
            return response.getStatusLine().getStatusCode() == 200;
        } catch (ClientProtocolException e) {
            return false;
        } catch (IOException e) {
            return false;
        }
    }

    public void sendInfoMessage(String message) {
        Intent intent = new Intent(SpeindAPI.BROADCAST_ACTION).putExtra(SpeindAPI.CLIENT_CMD, SpeindAPI.CC_INFO_MESSAGE).putExtra(SpeindAPI.PARAM_MESSAGE_STR, message);
        intent.putExtra(SpeindAPI.PARAM_SERVICE_PACKAGE_NAME, getPackageName());
        sendBroadcast(intent);

    }

    // --------- Functions end
	
	
/*						

                	           
    private void sendWrongProfileError() {
    	Intent intent = new Intent(SpeindAPI.BROADCAST_ACTION).putExtra(SpeindAPI.CLIENT_CMD, SpeindAPI.CC_WRONG_PROFILE_DATA);
		sendBroadcast(intent);
		sendProfileReguest();
    }               
        
    public void sendError(int code, String message) {
		Intent intent = new Intent(SpeindAPI.BROADCAST_ACTION).putExtra(SpeindAPI.CLIENT_CMD, SpeindAPI.CC_ERROR).putExtra(SpeindAPI.PARAM_ERROR_CODE, code).putExtra(SpeindAPI.PARAM_ERROR_STR, message);
		sendBroadcast(intent);

    }	
*/	
    
}
	