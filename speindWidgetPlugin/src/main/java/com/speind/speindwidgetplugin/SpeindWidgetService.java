package com.speind.speindwidgetplugin;

import java.util.ArrayList;
import java.util.List;

import me.speind.SpeindAPI;
import me.speind.SpeindAPI.SpeindData;
import me.speind.SpeindAPI.SpeindSettings;
import me.speind.SpeindAPI.SpeindUIReceiver;
import me.speind.SpeindAPI.SpeindUIReceiverListener;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

public class SpeindWidgetService extends Service{
	public static int PLAYER							= 0;
	public static int READER							= 1;
	public static String STATE_CHANGED			= "speind_state_changed";
	public static String PARAM_SPEIND_STATE		= "speind_state";
	public static String DATA_CHANGED			= "speind_play_data_changed";
	public static String PARAM_SPEIND_DATATYPE	= "speind_datatype";
	public static String PARAM_SPEIND_FILENAME	= "speind_filename";
	public static String PARAM_SPEIND_TITLE		= "speind_title";
	public static String PARAM_SPEIND_IMAGE		= "speind_image";

	private int state=-3;
	private String lFileName="";
	private String lTitle="";
	private String lImage="";
	
	private boolean isSpeindExists=false;
	private SpeindUIReceiver uiReceiver=null;
	public SpeindData speindData =new SpeindData();
	public SpeindUIReceiverListener speindUIReceiverListener = new SpeindUIReceiverListener() {

		@Override
		public void onDataFeedsProcessingChanged() {}

		@Override
		public void onDataFeedsStateChanged(String packageName, int state, boolean error) {}

		@Override
		public void onDownloadProgress(double arg0, int arg1, int arg2) {}

		@Override
		public void onError(int arg0, String arg1) {}

		@Override
		public void onInfoMessage(String message) {}

		@Override
		public void onExit() {
            if (uiReceiver!=null) {
                unregisterReceiver(uiReceiver);
                uiReceiver=null;
                checkForSpeind();
            }
            Intent intent = new Intent(SpeindWidgetService.this, SpeindWidget.class);
		    intent.setAction(STATE_CHANGED);
		    intent.putExtra(PARAM_SPEIND_STATE, -1);
            intent.putExtra(SpeindAPI.PARAM_SERVICE_PACKAGE_NAME, speindData.service_package);
		    state=-1;
		    sendBroadcast(intent);
		}

		@Override
		public void onInfopointsChanged(int startPosition) {}

		@Override
		public void onPlayMP3Info(String fileName, String nextFileName) {
			lTitle="";
		    lFileName=fileName;
			Intent intent = new Intent(SpeindWidgetService.this, SpeindWidget.class);
		    intent.setAction(DATA_CHANGED);
		    intent.putExtra(PARAM_SPEIND_STATE, state);
		    intent.putExtra(PARAM_SPEIND_DATATYPE, PLAYER);
		    intent.putExtra(PARAM_SPEIND_FILENAME, fileName);
            intent.putExtra(SpeindAPI.PARAM_SERVICE_PACKAGE_NAME, speindData.service_package);
		    sendBroadcast(intent);	
		}

		@Override
		public void onPlayPositionChanged(int arg0, int arg1) {}

		@Override
		public void onPlayTextInfo(int pos, boolean arg1) {
			String title="";
			String image="";
			SpeindAPI.InfoPointData data=speindData.infopoints.get(pos).getData(speindData.currentProfile);
			if (data!=null) {
				title=data.postTitle;
				if (title.equals("")) {
					title=data.postSender;
				}
				image=data.postBmpPath;
			}
			lFileName="";
			lTitle=title;
			lImage=image;
			Intent intent = new Intent(SpeindWidgetService.this, SpeindWidget.class);
		    intent.setAction(DATA_CHANGED);
		    intent.putExtra(PARAM_SPEIND_STATE, state);
		    intent.putExtra(PARAM_SPEIND_DATATYPE, READER);
		    intent.putExtra(PARAM_SPEIND_TITLE, title);
		    intent.putExtra(PARAM_SPEIND_IMAGE, image);
            intent.putExtra(SpeindAPI.PARAM_SERVICE_PACKAGE_NAME, speindData.service_package);
		    sendBroadcast(intent);	
		}

		@Override
		public void onProfiles(ArrayList<String> arg0) {}

		@Override
		public void onReady() {}

		@Override
		public void onSettingsChanged(SpeindSettings arg0, SpeindSettings arg1) {}

		@Override
		public void onShowUpdates(String arg0) {}

		@Override
		public void onStartPlay() {}

		@Override
		public void onStateChanged(int oldState, int newState) {
			Intent intent = new Intent(SpeindWidgetService.this, SpeindWidget.class);
		    intent.setAction(STATE_CHANGED);
		    intent.putExtra(PARAM_SPEIND_STATE, newState);
            intent.putExtra(SpeindAPI.PARAM_SERVICE_PACKAGE_NAME, speindData.service_package);
		    sendBroadcast(intent);	
		    state=newState;
		}

		@Override
		public void onStopPlay() {}

		@Override
		public void onDataFeedsListChanged() {
			// TODO Auto-generated method stub
			
		}
		
	};
	
	private Handler handler = new Handler();
	private Runnable checkForSpeindRunnable = new Runnable(){
		@Override
		public void run() {
			checkForSpeind();
		}
	};
	
	@Override
	public IBinder onBind(Intent arg0) {
		return null;
	}

	@Override
    public void onCreate() {
		super.onCreate();
        speindData.service_package = "";
		checkForSpeind();
	}
	
	@Override
    public void onDestroy() {
	    if (uiReceiver!=null) {
    		unregisterReceiver(uiReceiver);
    		uiReceiver=null;
    	} 		
		super.onDestroy();
	}
	
	@Override
    public int onStartCommand(Intent intent, int flags, int startId) {
    	if (intent!=null) {
    		if (intent.getAction()!=null&&intent.getAction().equals("start")) {
    	    	if (isSpeindExists&&uiReceiver!=null) {
    	    		Intent intent1 = new Intent(SpeindWidgetService.this, SpeindWidget.class);
    	    		intent1.putExtra(PARAM_SPEIND_STATE, state);
                    intent1.putExtra(SpeindAPI.PARAM_SERVICE_PACKAGE_NAME, speindData.service_package);
    	    		if (!lFileName.equals("")) {
    	        		intent1.setAction(DATA_CHANGED);
    	    		    intent1.putExtra(PARAM_SPEIND_DATATYPE, PLAYER);
    	    		    intent1.putExtra(PARAM_SPEIND_FILENAME, lFileName);    			
    	    		} else if (!lTitle.equals("")) {
    	        		intent1.setAction(DATA_CHANGED);
    	    			intent1.putExtra(PARAM_SPEIND_DATATYPE, READER);
    	    		    intent1.putExtra(PARAM_SPEIND_TITLE, lTitle);
    	    		    intent1.putExtra(PARAM_SPEIND_IMAGE, lImage);
    	    		} else {
    	        		intent1.setAction(STATE_CHANGED);    			
    	    		}
    	    		sendBroadcast(intent1);
    	    	}
    		}
    	}
    	return super.onStartCommand(intent, flags, startId);
	}
	
	private void checkForSpeind() {
		if (isSpeindExists&&uiReceiver!=null) return;
		handler.removeCallbacks(checkForSpeindRunnable);
		(new AsyncTask<Void, Void, Boolean>(){
			ArrayList<String> service_packages = new ArrayList();

			@Override
			protected Boolean doInBackground(Void... params) {
				isSpeindExists=false;
				PackageManager mngr = getPackageManager();
		    	List<PackageInfo> list = mngr.getInstalledPackages(PackageManager.GET_SERVICES);    	
		    	for (PackageInfo packageInfo : list) {
		        	ServiceInfo services[]=packageInfo.services;
		        	if (services!=null) {
			        	for (int i=0; i<services.length;i++) {
							if (services[i].name.endsWith(".SpeindService")) {
								service_packages.add(services[i].name);
							}
			        	}
		        	}
		    	}
                isSpeindExists = service_packages.size()>0;
				return isSpeindExists;
			}
			@Override
			protected void onPostExecute(Boolean param) {
				if (isSpeindExists) {					
					if (uiReceiver==null) {
						uiReceiver = new SpeindUIReceiver(speindUIReceiverListener, speindData);
                        speindData.service_package = "";
						IntentFilter intFilt = new IntentFilter(SpeindAPI.BROADCAST_ACTION);
						registerReceiver(uiReceiver, intFilt);

						(new AsyncTask<Void, Void, Boolean>(){
							@Override
							protected Boolean doInBackground(Void... params) {
								boolean res=false;
								ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
						    	List<RunningServiceInfo> list = manager.getRunningServices(Integer.MAX_VALUE);    	
						    	for (RunningServiceInfo service : list) {
                                    if (service.service.getClassName().endsWith(".SpeindService")) {
                                        speindData.service_package = service.service.getPackageName();
					        			res=true;
					        			break;
					        		} 
						    	}
								return res;
							}
							@Override
							protected void onPostExecute(Boolean param) {
								if (param) {
									Intent intent = new Intent(SpeindWidgetService.this, SpeindWidget.class);
								    intent.setAction(STATE_CHANGED);
								    intent.putExtra(PARAM_SPEIND_STATE, 0);
                                    intent.putExtra(SpeindAPI.PARAM_SERVICE_PACKAGE_NAME, speindData.service_package);
								    sendBroadcast(intent);
								    state=0;
									Intent intent1 = SpeindAPI.createIntent(speindData.service_package);
									intent1.putExtra(SpeindAPI.SERVICE_CMD, SpeindAPI.SC_GET_STATE);
							    	startService(intent1);
								    Log.e("[!!!!!!!!!!!!!!!]", "! "+speindData.service_package);
								} else {
									Intent intent = new Intent(SpeindWidgetService.this, SpeindWidget.class);
								    intent.setAction(STATE_CHANGED);
								    intent.putExtra(PARAM_SPEIND_STATE, -1);
                                    intent.putExtra(SpeindAPI.PARAM_SERVICE_PACKAGE_NAME, speindData.service_package);
								    sendBroadcast(intent);
                                    Log.e("[!!!!!!!!!!!!!!!]", "! " + speindData.service_package);
								    state=-1;
								}
							}
						}).execute();
					}
				} else {
					Intent intent = new Intent(SpeindWidgetService.this, SpeindWidget.class);
				    intent.setAction(STATE_CHANGED);
				    intent.putExtra(PARAM_SPEIND_STATE, -2);
                    intent.putExtra(SpeindAPI.PARAM_SERVICE_PACKAGE_NAME, speindData.service_package);
				    sendBroadcast(intent);
				    state=-2;
					handler.postDelayed(checkForSpeindRunnable, 60*1000);
				}
			}
		}).execute();			
	}
}
