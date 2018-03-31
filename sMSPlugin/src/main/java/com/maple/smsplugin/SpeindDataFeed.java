package com.maple.smsplugin;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Configuration;  
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.IBinder;
import android.telephony.SmsMessage;
import android.util.Log;

import me.speind.SpeindAPI;
import me.speind.SpeindAPI.InfoPoint;

public class SpeindDataFeed extends SpeindAPI.DatafeedService {
	public static final String PREFS_NAME = "SMSReceiverConfig";
	
	public static final String SMS_FEED_SETTINGS_COMMAND = "SMS_settings_command";
	public static final int AUTO_READ_CHANGED = 0;
	
	//private boolean isSuspended=false;
	private boolean isRegistered=false;
	
	private BroadcastReceiver SMSMonitor=null;
	private String curProfile="profile";

    private Map<String, Boolean> services = new HashMap<>();

	@Override
	public void onCreate() {
		super.onCreate();
		
		SMSMonitor=new BroadcastReceiver() {
			private static final String ACTION = "android.provider.Telephony.SMS_RECEIVED";
			@Override
			public void onReceive(Context context, Intent intent) {
				if (!curProfile.equals("")) {
					if (intent != null && intent.getAction() != null && ACTION.compareToIgnoreCase(intent.getAction()) == 0) {
					    Object[] pduArray = (Object[]) intent.getExtras().get("pdus");
					    SmsMessage[] messages = new SmsMessage[pduArray.length];
					    for (int i = 0; i < pduArray.length; i++) {
					        messages[i] = SmsMessage.createFromPdu((byte[]) pduArray[i]);
					    }
					    processSMS(messages);
					}		
				}
			}			
		};
			
		Log.e("[---SMSPlugin---]", "SMSMonitor created");
	}
	
	/*
	@Override
	public void onDestroy() {
		if (SMSMonitor!=null) unregisterReceiver(SMSMonitor);
		super.onDestroy();
	}
	*/

	@Override
	public SpeindAPI.DataFeedSettingsInfo onInit(String service_package) {
        services.put(service_package, true);
        SpeindAPI.DataFeedSettingsInfo info = new SpeindAPI.DataFeedSettingsInfo(this, getString(R.string.smsreceiver), BitmapFactory.decodeResource(getResources(), R.drawable.ic_launcher), false);
		return info;
	}

	@Override
    public int onStartCommand(Intent intent, int flags, int startId) {
    	if (intent!=null) { 
			int cmd=intent.getIntExtra(SMS_FEED_SETTINGS_COMMAND, -1);
			switch (cmd) {
			case AUTO_READ_CHANGED:
				onAutoReadChanged();
				break;		
			}
		}
        return super.onStartCommand(intent, flags, startId);
    }
	
	@Override
	public void onInfoPointDetails(String service_package, InfoPoint arg0) {
	}

	@Override
	public void onLike(String service_package, InfoPoint arg0) {
	}

	@Override
	public void onSetProfile(String service_package, String profile) {
		//curProfile=profile;
		onAutoReadChanged();
	}

	@Override
	public void onStop(String service_package) {
        Log.e("[---SMSPlugin---]", ""+isSuspended());
        services.remove(service_package);
        if (services.size()==0) {
            if (isRegistered&&!isSuspended()) unregisterReceiver(SMSMonitor);
            stopSelf();
        } else {
            if (isSuspended()) {
                if (isRegistered) {
                    unregisterReceiver(SMSMonitor);
                    isRegistered=false;
                }
            }
        }
	}

	@Override
	public void onShowSettings(String service_package) {
		Intent intent=new Intent(SpeindDataFeed.this, SMSSettings.class);
		intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		intent.putExtra("profile", curProfile);
		getApplication().startActivity(intent);	
	}

	@Override
	public IBinder onBind(Intent arg0) {
		return null;
	}

	private void processSMS(final SmsMessage[] messages) {
		new Thread(new Runnable() {
			private void reportSMS(String from, String text) {
		        //Log.d("[---SMS---]", ""+from+"\n B: "+text);
				String vfrom="";
				int cnt=from.length();
				boolean isDigits=true;
				for (int i=0;i<cnt;i++) {
					if (!((from.charAt(i)=='+'&&(i==0))||(from.charAt(i)=='0')||(from.charAt(i)=='1')||(from.charAt(i)=='2')||(from.charAt(i)=='3')||(from.charAt(i)=='4')||(from.charAt(i)=='5')||(from.charAt(i)=='6')||(from.charAt(i)=='7')||(from.charAt(i)=='8')||(from.charAt(i)=='9'))) {
						isDigits=false;
						break;
					}
				}
				
				String lang=getLang(text);
				Configuration conf = getResources().getConfiguration();
		    	Locale localeOld=conf.locale;
		    	conf.locale = new Locale(lang);
		    	Resources resources = new Resources(getAssets(), getResources().getDisplayMetrics(), conf);
		    	
				if (isDigits) {
					if (from.length()>=11) {
						for (int i=cnt-1;i>cnt-5;i--) {
							if (i==cnt-3) vfrom=" "+vfrom; 
							vfrom=from.charAt(i)+vfrom;
						}
						vfrom=resources.getString(R.string.from_number)+" \""+vfrom+"\" "+resources.getString(R.string.send_sms)+".";
					} else {
						for (int i=0;i<cnt;i++) {
							if (i!=0&&(i+1)%2==1) vfrom+=" "; 
							vfrom+=from.charAt(i);
						}
						vfrom=resources.getString(R.string.from_short_number)+" \""+vfrom+"\".";
					}
				} else {
					vfrom=from+" "+resources.getString(R.string.send_sms)+".";
				}
				
				conf.locale=localeOld;
		    	resources = new Resources(getAssets(), getResources().getDisplayMetrics(), conf);
		    	
				Log.d("[------]","Lang: "+lang);
				
		    	SpeindAPI.SendInfoPointParams sendInfoPointParams=new SpeindAPI.SendInfoPointParams();
		    	sendInfoPointParams.postTime=new Date();
		    	sendInfoPointParams.postSender=from;
		    	sendInfoPointParams.senderBmp=null;
		    	sendInfoPointParams.postBmp=null;
		    	sendInfoPointParams.postTitle="";
		    	sendInfoPointParams.postOriginText=text;
		    	sendInfoPointParams.postLink="";
		    	sendInfoPointParams.postPluginData="";
		    	sendInfoPointParams.postSenderVocalizing=vfrom;
		    	sendInfoPointParams.postTitleVocalizing="";
		    	sendInfoPointParams.postTextVocalizing=text;
		    	sendInfoPointParams.lang=lang;	
		    	sendInfoPointParams.checkForDuplicate=false;
		    	sendInfoPointParams.priority=0;

                final Map<String, Boolean> services_loc = new HashMap<>(services);
                final Collection<String> service_packages = services_loc.keySet();
                for (String service_package : service_packages) {
                    if (!services_loc.get(service_package)) sendInfoPoint(service_package, sendInfoPointParams);
                }
		    	
		        //sendInfoPoint(new Date(), from, null, null, "", text, "", "", vfrom, "", text);
			}
			
			@Override
			public void run() {
			    String lastFrom="";
			    String body="";
			    for (int i = 0; i < messages.length; i++) {
			    	SmsMessage sms = messages[i];
			        String from = sms.getDisplayOriginatingAddress();
			        if (lastFrom.equals("")) lastFrom=from;
			        if (from.equals(lastFrom)) {
			        	body+=sms.getMessageBody();
				        if (i==(messages.length-1)) {
				        	reportSMS(lastFrom, body);
				        }
			        } else {
			        	reportSMS(lastFrom, body);				        
				        lastFrom=from;
				        body=sms.getMessageBody();
				        if (i==(messages.length-1)) {
				        	reportSMS(lastFrom, body);
				        }
			        }
			    }
				
			}			
		}).start();
		
	}
	
	private void onAutoReadChanged() {
		SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);		
		boolean readConfirm=!settings.getBoolean(curProfile+"_auto_read", false);

        final Map<String, Boolean> services_loc = new HashMap<>(services);
        final Collection<String> service_packages = services_loc.keySet();
        for (String service_package : service_packages) {
            setReadConfirm(service_package, readConfirm);
        }
	}

	@Override
	public void onLangListChanged(String service_package, ArrayList<String> arg0) { }

	@Override
	public void onLoadImagesOnMobileInetChanged(String service_package, boolean arg0) { }

	@Override
	public void onResume(String service_package) {
		IntentFilter ifilter = new IntentFilter("android.provider.Telephony.SMS_RECEIVED");
		ifilter.setPriority(1000);
		registerReceiver(SMSMonitor, ifilter);
		isRegistered=true;
		//isSuspended=false;
        services.put(service_package, false);
		Log.e("[---SMSPlugin---]", "SMSMonitor regirtered");
	}

	@Override
	public void onStart(String service_package, int state) {
		if (state==SpeindAPI.DataFeedSettingsInfo.DATAFEED_STATE_READY) {
			IntentFilter ifilter = new IntentFilter("android.provider.Telephony.SMS_RECEIVED");
			ifilter.setPriority(1000);
			registerReceiver(SMSMonitor, ifilter);
			isRegistered=true;
			//isSuspended=false;
            services.put(service_package, false);
			Log.e("[---SMSPlugin---]", "SMSMonitor regirtered");
		} else {
			//isSuspended=true;
            services.put(service_package, true);
		}
	}

	@Override
	public void onSuspend(String service_package) {
		//isSuspended=true;
        services.put(service_package, true);
        if (isSuspended()) {
            if (isRegistered) {
                unregisterReceiver(SMSMonitor);
                isRegistered=false;
                Log.e("[---SMSPlugin---]", "SMSMonitor unregirtered");
            }
        }
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
	public boolean isAuthorized(String service_puckage) {
		return true;
	}

	@Override
	public void onPost(String service_package, InfoPoint arg0) {
		// TODO Auto-generated method stub

	}

}
