package com.speind.speindwidgetplugin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import me.speind.SpeindAPI;

import android.app.ActivityManager;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Html;
import android.text.Spanned;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;

public class SpeindWidget extends AppWidgetProvider {
	private static String UPDATE_ALL_WIDGETS	= "update";

    private String service_package = "";
	private int speindState=-3;
	private Spanned curPlayTitle=null;
	private Bitmap curPlayBitmap=null;
	
	private static void log(String s) {
    	//Log.e("[---SpeindWidget---]", s);
    }
	
	private static Cursor myquery(Context context, Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder, int limit) {
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
	
	private Map<String, ArrayList<String>> refreshMediaData(Context context) {
		Map<String, ArrayList<String>> mediaData = new HashMap<String, ArrayList<String>>();
    	String[] projection = {MediaStore.Audio.Media._ID, MediaStore.Audio.Media.DATA, MediaStore.Audio.Media.TITLE, MediaStore.Audio.Media.ARTIST, MediaStore.Audio.Media.ALBUM};  
    	String selection = MediaStore.Audio.Media.IS_MUSIC + " != 0 ";
    	Cursor musicListSDCardCursor = myquery(context, MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, projection, selection , null, null,0);
    	Cursor musicListInternalMemoryCursor = myquery(context, MediaStore.Audio.Media.INTERNAL_CONTENT_URI, projection, selection , null, null,0);
    	if( musicListSDCardCursor!= null ) {
    		for(int i=0;i<musicListSDCardCursor.getCount();i++) {
                musicListSDCardCursor.moveToPosition(i);
                String p = musicListSDCardCursor.getString(1);
                if(p.endsWith("mp3")) {
                	  String title = musicListSDCardCursor.getString(musicListSDCardCursor.getColumnIndex(MediaStore.Audio.Media.TITLE));
                	  String artist = musicListSDCardCursor.getString(musicListSDCardCursor.getColumnIndex(MediaStore.Audio.Media.ARTIST));
                	  String album = musicListSDCardCursor.getString(musicListSDCardCursor.getColumnIndex(MediaStore.Audio.Media.ALBUM));
                	  mediaData.put(p, new ArrayList<String>(Arrays.asList(title, artist, album)));
                }

            }
            musicListSDCardCursor.close();
        }
    			
    	if( musicListInternalMemoryCursor!= null ) {
            for(int i=0;i<musicListInternalMemoryCursor.getCount();i++) {
                musicListInternalMemoryCursor.moveToPosition(i);
                String p = musicListInternalMemoryCursor.getString(1);
                if(p.endsWith("mp3")) {
	              	  String title = musicListInternalMemoryCursor.getString(musicListInternalMemoryCursor.getColumnIndex(MediaStore.Audio.Media.TITLE));
	              	  String artist = musicListInternalMemoryCursor.getString(musicListInternalMemoryCursor.getColumnIndex(MediaStore.Audio.Media.ARTIST));
	              	  String album = musicListInternalMemoryCursor.getString(musicListInternalMemoryCursor.getColumnIndex(MediaStore.Audio.Media.ALBUM));
                	  mediaData.put(p, new ArrayList<String>(Arrays.asList(title, artist, album)));
                }
            }
            musicListInternalMemoryCursor.close();
        }
		return mediaData;
	}
	
	@Override
	public void onEnabled(Context context) {
	    super.onEnabled(context);
	    log("onEnabled");
	    Intent intent = new Intent(context, SpeindWidget.class);
	    intent.setAction(UPDATE_ALL_WIDGETS);
	    PendingIntent pIntent = PendingIntent.getBroadcast(context, 0, intent, 0);
	    AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
	    alarmManager.setRepeating(AlarmManager.RTC, System.currentTimeMillis(), 5000, pIntent);
	}
	 
	@Override
	public void onUpdate(final Context context, final AppWidgetManager appWidgetManager, int[] appWidgetIds) {
	    super.onUpdate(context, appWidgetManager, appWidgetIds);
		log("onUpdate");
	    if (speindState==-3) context.startService((new Intent(context, SpeindWidgetService.class)).setAction("start"));
	    for (final int id : appWidgetIds) {
	    	final RemoteViews widgetView = new RemoteViews(context.getPackageName(), R.layout.widget);
	    	if (speindState==-2) {
    			widgetView.setInt(R.id.downloading_wrap, "setVisibility", View.GONE);
    			widgetView.setInt(R.id.play_info_wrap, "setVisibility", View.GONE);

    			widgetView.setTextViewText(R.id.state_message, context.getString(R.string.speind_not_installed));
	    		widgetView.setInt(R.id.state_message_wrap, "setVisibility", View.VISIBLE);
    			widgetView.setInt(R.id.message_button, "setText", R.string.speind_install);
    			widgetView.setInt(R.id.message_button, "setVisibility", View.VISIBLE);
    			
    			Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=com.maple.speind"));
    			intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
    			PendingIntent pIntent = PendingIntent.getActivity(context, 0, intent, 0);
    			widgetView.setOnClickPendingIntent(R.id.message_button, pIntent);
    			    
	    	} else if (speindState==-1) {
    			widgetView.setInt(R.id.downloading_wrap, "setVisibility", View.GONE);
    			widgetView.setInt(R.id.play_info_wrap, "setVisibility", View.GONE);

	    		widgetView.setTextViewText(R.id.state_message, context.getString(R.string.speind_not_started));
    			widgetView.setInt(R.id.state_message_wrap, "setVisibility", View.VISIBLE);
    			widgetView.setInt(R.id.message_button, "setText", R.string.speind_start);
    			widgetView.setInt(R.id.message_button, "setVisibility", View.VISIBLE);

                (new AsyncTask<Void, Void, Boolean>(){
                    ArrayList<String> service_packages = new ArrayList();

                    @Override
                    protected Boolean doInBackground(Void... params) {
                        PackageManager mngr = context.getPackageManager();
                        List<PackageInfo> list = mngr.getInstalledPackages(PackageManager.GET_SERVICES);
                        for (PackageInfo packageInfo : list) {
                            ServiceInfo services[]=packageInfo.services;
                            if (services!=null) {
                                for (int i=0; i<services.length;i++) {
                                    if (services[i].name.endsWith(".SpeindService")) {
                                        service_packages.add(services[i].packageName);
                                    }
                                }
                            }
                        }
                        return service_packages.size()>0;
                    }
                    @Override
                    protected void onPostExecute(Boolean param) {
                        if (service_packages.size()==1) {
                            Intent intent = SpeindAPI.createIntent(service_packages.get(0));
                            intent.putExtra("isWidget", true);
                            PendingIntent pIntent = PendingIntent.getService(context, 0, intent, 0);
                            widgetView.setOnClickPendingIntent(R.id.message_button, pIntent);
                            appWidgetManager.updateAppWidget(id, widgetView);
                        } else if (service_packages.size()>1) {
                            Log.e("[---More one---]", "! " + service_packages.size());
                            Intent intent=new Intent(context, SpeindLaunchActivity.class);
                            PendingIntent pIntent = PendingIntent.getActivity(context, 0, intent, 0);
                            widgetView.setOnClickPendingIntent(R.id.message_button, pIntent);
                            appWidgetManager.updateAppWidget(id, widgetView);
                        }
                    }
                }).execute();
	    	} else if (speindState==SpeindAPI.SPEIND_STATE_NEED_PROFILE) {
	    		//SpeindAPI.setProfile(context, "profile", "password");
	    		
    			widgetView.setInt(R.id.downloading_wrap, "setVisibility", View.GONE);
    			widgetView.setInt(R.id.play_info_wrap, "setVisibility", View.GONE);

	    		widgetView.setTextViewText(R.id.state_message, context.getString(R.string.speind_need_input));
    			widgetView.setInt(R.id.state_message_wrap, "setVisibility", View.VISIBLE);
    			widgetView.setInt(R.id.message_button, "setText", R.string.speind_open);
    			widgetView.setInt(R.id.message_button, "setVisibility", View.VISIBLE);
    			
    			Intent intent = SpeindAPI.createIntent(service_package);
    			intent.putExtra(SpeindAPI.SERVICE_CMD, 1001);
    			PendingIntent pIntent = PendingIntent.getService(context, 1, intent, 0);
    			widgetView.setOnClickPendingIntent(R.id.message_button, pIntent);
    			
	    	} else if (speindState>SpeindAPI.SPEIND_STATE_NEED_PROFILE) {
    			widgetView.setInt(R.id.state_message_wrap, "setVisibility", View.GONE);
    			widgetView.setInt(R.id.downloading_wrap, "setVisibility", View.GONE);
    			
    			if (curPlayTitle!=null) {
	    			widgetView.setTextViewText(R.id.title, curPlayTitle);
    			}
    			if (curPlayBitmap!=null) {
    				Bundle b = appWidgetManager.getAppWidgetOptions(id);
    				int mw=b.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH);
    				int mh=b.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT);
    				Bitmap bmp = null; 
    				if (mw>0&&mh>0) {
    					bmp = SpeindAPI.GetScaledBitmap(curPlayBitmap, mw, mh);
    				} else {
    					bmp = SpeindAPI.GetScaledBitmap(curPlayBitmap, 512, 512);
    				}
    				log(""+mw+"x"+mh);
    				log(""+curPlayBitmap.getWidth()+"x"+curPlayBitmap.getHeight());
    				log(""+bmp.getWidth()+"x"+bmp.getHeight());
	    			widgetView.setBitmap(R.id.picture, "setImageBitmap", bmp);
    			}
    			
    			widgetView.setInt(R.id.play_info_wrap, "setVisibility", View.VISIBLE);	 
    			
    			Intent intent = SpeindAPI.createNextIntent(service_package);
    			PendingIntent pIntent = PendingIntent.getService(context, 2, intent, 0);
    			widgetView.setOnClickPendingIntent(R.id.next, pIntent);

    			intent = SpeindAPI.createPrevIntent(service_package);
    			pIntent = PendingIntent.getService(context, 3, intent, 0);
    			widgetView.setOnClickPendingIntent(R.id.prev, pIntent);

    			if (speindState==SpeindAPI.SPEIND_STATE_PLAY_PLAYER||speindState==SpeindAPI.SPEIND_STATE_PLAY_READER) {
        			widgetView.setInt(R.id.play_stop, "setBackgroundResource", android.R.drawable.ic_media_pause);	 
    			} else {
        			widgetView.setInt(R.id.play_stop, "setBackgroundResource", android.R.drawable.ic_media_play);	 
    			}
    			intent = SpeindAPI.createPlayPauseIntent(service_package);
    			pIntent = PendingIntent.getService(context, 4, intent, 0);
    			widgetView.setOnClickPendingIntent(R.id.play_stop, pIntent);
    			
    			intent=SpeindAPI.createReadCurrentInfopointArticleIntent(service_package);
    			pIntent = PendingIntent.getService(context, 5, intent, 0);
    			widgetView.setOnClickPendingIntent(R.id.picture, pIntent);
    			
	    	} else {
    			widgetView.setInt(R.id.downloading_wrap, "setVisibility", View.GONE);
    			widgetView.setInt(R.id.play_info_wrap, "setVisibility", View.GONE);

    			widgetView.setTextViewText(R.id.state_message, context.getString(R.string.please_wait));
    			widgetView.setInt(R.id.state_message_wrap, "setVisibility", View.VISIBLE);
    			widgetView.setInt(R.id.message_button, "setVisibility", View.GONE);
	    	}
	    	appWidgetManager.updateAppWidget(id, widgetView);
	    }
	}

	@Override
	public void onDeleted(Context context, int[] appWidgetIds) {
	    super.onDeleted(context, appWidgetIds);
		log("onDeleted");
	    ComponentName thisAppWidget = new ComponentName(context.getPackageName(), getClass().getName());
		AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
		int ids[] = appWidgetManager.getAppWidgetIds(thisAppWidget);
		if (ids.length==0) {
			context.stopService(new Intent(context, SpeindWidgetService.class));
		}
	}

	@Override
	public void onDisabled(Context context) {
	    super.onDisabled(context);
		log("onDisabled");
	    context.stopService(new Intent(context, SpeindWidgetService.class));
	    Intent intent = new Intent(context, SpeindWidget.class);
	    intent.setAction(UPDATE_ALL_WIDGETS);
	    PendingIntent pIntent = PendingIntent.getBroadcast(context, 0, intent, 0);
	    AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
	    alarmManager.cancel(pIntent);
	}
	
	public void onReceive(Context context, Intent intent) {
		log("onReceive");
	    super.onReceive(context, intent);
	    if (intent.getAction()!=null&&intent.getAction().equalsIgnoreCase(SpeindWidgetService.STATE_CHANGED)) {
	    	speindState=intent.getIntExtra(SpeindWidgetService.PARAM_SPEIND_STATE, -1);
			service_package = intent.getStringExtra(SpeindAPI.PARAM_SERVICE_PACKAGE_NAME);
	    	updateWidgets(context);
	    } else if (intent.getAction()!=null&&intent.getAction().equalsIgnoreCase(SpeindWidgetService.DATA_CHANGED)) {
	    	speindState=intent.getIntExtra(SpeindWidgetService.PARAM_SPEIND_STATE, -1);
			service_package = intent.getStringExtra(SpeindAPI.PARAM_SERVICE_PACKAGE_NAME);
	    	int playType=intent.getIntExtra(SpeindWidgetService.PARAM_SPEIND_DATATYPE, -1);
	    	if (playType==SpeindWidgetService.PLAYER) {
	    		String playerFileName=intent.getStringExtra(SpeindWidgetService.PARAM_SPEIND_FILENAME);
	    		try {
    	    		MediaMetadataRetriever mmr = new MediaMetadataRetriever();
	    			mmr.setDataSource(playerFileName);
	    			String artist=null;
    	    		String title=null;
    	    		Map<String, ArrayList<String>> mediaData = refreshMediaData(context);
	    			ArrayList<String> data=mediaData.get(playerFileName);
	    			if (data!=null) {
    	    			title=data.get(0);
    	    			artist=data.get(1);
	    			}
	    	    	curPlayTitle=Html.fromHtml("<b>"+artist+"</b><br>"+title);
	    			byte[] img=mmr.getEmbeddedPicture();
	    			if (img!=null) {
	    				curPlayBitmap = BitmapFactory.decodeByteArray(img, 0, img.length);//SpeindAPI.GetScaledBitmap(img, 0,img.length, 512, 512);		    		    	
	    			} else {
		    			curPlayBitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.placeholder);
	    			}
	    		} catch (Exception e) { }
	    	} else if (playType==SpeindWidgetService.READER) {
	    		curPlayTitle=Html.fromHtml(intent.getStringExtra(SpeindWidgetService.PARAM_SPEIND_TITLE));
	    		String bmp=intent.getStringExtra(SpeindWidgetService.PARAM_SPEIND_IMAGE);
	    		if (bmp!=null&&!bmp.equals("")) {
		    		curPlayBitmap = BitmapFactory.decodeFile(bmp);//SpeindAPI.GetScaledBitmap(bmp, 512, 512);
	    		} else {	    			
	    			curPlayBitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.placeholder);
	    		}
	    	} else {
	    		return;
	    	}
	    	updateWidgets(context);
	   } else if (intent.getAction()!=null&&intent.getAction().equalsIgnoreCase(UPDATE_ALL_WIDGETS)) {
			service_package = intent.getStringExtra(SpeindAPI.PARAM_SERVICE_PACKAGE_NAME);
			context.startService(new Intent(context, SpeindWidgetService.class));
	   }
	}
	private void updateWidgets(Context context) {
		log("updateWidgets");
		ComponentName thisAppWidget = new ComponentName(context.getPackageName(), getClass().getName());
		AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
		int ids[] = appWidgetManager.getAppWidgetIds(thisAppWidget);
		onUpdate(context, appWidgetManager, ids);		
	}
	
}
