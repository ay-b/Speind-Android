package com.maple.speindui;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

import com.maple.speind.R;
//import ru.lifenews.speind.R;

import me.speind.SpeindAPI;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender.SendIntentException;
import android.media.MediaPlayer;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.Button;
import android.widget.ExpandableListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

public class VoicesSettings extends ActionBarActivity {
	private BroadcastReceiver voicesUpdatedReceiver;
    private ArrayList<ArrayList<ArrayList<String>>> mGroups;

    private Handler handler = new Handler();
    
	private MediaPlayer player = null;
	String cFileName="";
	Button cPlayButton=null;
	
	ExpListAdapter adapter=null;
	
	public class ExpListAdapter extends BaseExpandableListAdapter {

	    private Context mContext;
	    
	    public ExpListAdapter (Context context){
	        mContext = context;
	    }
	    
	    @Override
	    public int getGroupCount() {
	        return mGroups.size();
	    }

	    @Override
	    public int getChildrenCount(int groupPosition) {
	        return mGroups.get(groupPosition).size();
	    }

	    @Override
	    public Object getGroup(int groupPosition) {
	        return mGroups.get(groupPosition);
	    }

	    @Override
	    public Object getChild(int groupPosition, int childPosition) {
	        return mGroups.get(groupPosition).get(childPosition);
	    }

	    @Override
	    public long getGroupId(int groupPosition) {
	        return groupPosition;
	    }

	    @Override
	    public long getChildId(int groupPosition, int childPosition) {
	        return childPosition;
	    }

	    @Override
	    public boolean hasStableIds() {
	        return true;
	    }

	    @Override
	    public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
	        if (convertView == null) {
	            //LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	            convertView = getLayoutInflater().inflate(R.layout.speind_lang_item, null);
	        }

	        if (isExpanded){
	           // Р�Р·РјРµРЅСЏРµРј С‡С‚Рѕ-РЅРёР±СѓРґСЊ, РµСЃР»Рё С‚РµРєСѓС‰Р°СЏ Group СЂР°СЃРєСЂС‹С‚Р°
	        } else{
	           // Р�Р·РјРµРЅСЏРµРј С‡С‚Рѕ-РЅРёР±СѓРґСЊ, РµСЃР»Рё С‚РµРєСѓС‰Р°СЏ Group СЃРєСЂС‹С‚Р°
	        }

	        TextView textGroup = (TextView) convertView.findViewById(R.id.langName);
	        textGroup.setText(mGroups.get(groupPosition).get(0).get(0));

	        ToggleButton indicator=(ToggleButton) convertView.findViewById(R.id.indicator);
	        if (indicator!=null) {
	        	indicator.setChecked(isExpanded);
	        }
	        		
	        return convertView;

	    }

	    @Override
	    public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {
	        if (convertView == null) {
	            //LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	            convertView = getLayoutInflater().inflate(R.layout.speind_voice_item, null);
	        }

	        TextView textChild = (TextView) convertView.findViewById(R.id.voiceName);
	        textChild.setText(mGroups.get(groupPosition).get(childPosition).get(1));
	        
	        Button button = (Button)convertView.findViewById(R.id.voice_action);
	        if (mGroups.get(groupPosition).get(childPosition).get(4).equals("0")) {
	        	final String code=mGroups.get(groupPosition).get(childPosition).get(2);
	        	button.setBackgroundResource(R.drawable.btn_buy);
		        button.setOnClickListener(new View.OnClickListener() {
		            @Override
		            public void onClick(View view) {
		            	SpeindAPI.buyVoiceRequest(VoicesSettings.this, getPackageName(), code);
		            }
		        });
	        } else if (mGroups.get(groupPosition).get(childPosition).get(4).equals("1")) {
	        	final String code=mGroups.get(groupPosition).get(childPosition).get(2);
	        	button.setBackgroundResource(R.drawable.btn_download);
		        button.setOnClickListener(new View.OnClickListener() {
		            @Override
		            public void onClick(View view) {
		            	ToggleButton allow_only_wifi_download=(ToggleButton) findViewById(R.id.allow_only_wifi_download);
						boolean wifi_only=false;
						if (allow_only_wifi_download!=null)  wifi_only=allow_only_wifi_download.isChecked();
		            	SpeindAPI.downloadVoiceRequest(VoicesSettings.this, getPackageName(), code, wifi_only);
		            }
		        });
	        } else if (mGroups.get(groupPosition).get(childPosition).get(4).equals("2")) {
	        	button.setBackgroundResource(R.drawable.wait);
	        	button.setClickable(false);
	        } else if (mGroups.get(groupPosition).get(childPosition).get(4).equals("3")) {
	        	final String code=mGroups.get(groupPosition).get(childPosition).get(2);
	        	button.setBackgroundResource(R.drawable.voice);
		        button.setOnClickListener(new View.OnClickListener() {
		            @Override
		            public void onClick(View view) {
		            	SpeindAPI.setDefaultVoiceRequest(VoicesSettings.this, getPackageName(), code);
		            }
		        });	        	
	        } else {
	        	button.setBackgroundResource(R.drawable.voice_selected);
	        	button.setClickable(false);
	        }
			final Button playButton=(Button)convertView.findViewById(R.id.playButton);
			if (playButton!=null) {
				playButton.setBackgroundResource(R.drawable.btn_play_small);
				final String fileName=mGroups.get(groupPosition).get(childPosition).get(5);
				if (!fileName.equals(" ")) {
					playButton.setOnClickListener(new OnClickListener(){
						@Override
						public void onClick(View v) {
							playVoiceDemo(fileName, playButton);
						}
					});
					playButton.setEnabled(true);
				} else {
					//playButton.setVisibility(View.INVISIBLE);
					playButton.setEnabled(false);
				}
			}
	        return convertView;
	    }

	    @Override
	    public boolean isChildSelectable(int groupPosition, int childPosition) {
	        return true;
	    }
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.speind_voices);
		
		Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
		if (toolbar!=null) {
	        toolbar.setTitle("");
	        setSupportActionBar(toolbar);
	        toolbar.setNavigationIcon(R.drawable.btn_back);
	        toolbar.setNavigationOnClickListener(new OnClickListener(){
				@Override
				public void onClick(View arg0) {
					VoicesSettings.this.finish();
				}				
			});
		}
    	
		player = new MediaPlayer();
    	player.setOnCompletionListener(onCompletePlayer);
    	player.reset();
  
		voicesUpdatedReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				 int cmd= intent.getIntExtra(SpeindAPI.CLIENT_CMD, 0);
				 if (cmd==SpeindAPI.CC_VOICES_DATA_UPDATED) {
					 processVoicesData(intent);
				 }
				 if (cmd==SpeindAPI.CC_VOICES_BUY_CMD) {
					 processBuyVoice(intent);
				 }
			}
		};
		IntentFilter ifilter = new IntentFilter(SpeindAPI.BROADCAST_ACTION);
		registerReceiver(voicesUpdatedReceiver, ifilter);
		
		
		Intent intent=getIntent();
		processVoicesData(intent);
		
		Button restore_purchases=(Button) findViewById(R.id.restore_purchases);
		if (restore_purchases!=null) {
			restore_purchases.setOnClickListener(new OnClickListener(){
				@Override
				public void onClick(View v) {
					final ToggleButton allow_only_wifi_download=(ToggleButton) findViewById(R.id.allow_only_wifi_download);
					boolean wifi_only=false;
					if (allow_only_wifi_download!=null)  wifi_only=allow_only_wifi_download.isChecked();
					SpeindAPI.restorePurchasesRequest(VoicesSettings.this, getPackageName(), wifi_only);
				}
			});
		}
	}
	
	@Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d("[---------]", "onActivityResult(" + requestCode + "," + resultCode + "," + data);
        if (!handleActivityResult(requestCode, resultCode, data)) super.onActivityResult(requestCode, resultCode, data);
    }
	
	public void processBuyVoice(Intent intent) {
		PendingIntent pendingIntent = intent.getParcelableExtra("buy_intent");
		try {
			startIntentSenderForResult(pendingIntent.getIntentSender(), 1, new Intent(), Integer.valueOf(0), Integer.valueOf(0), Integer.valueOf(0));
		} catch (SendIntentException e) {
			e.printStackTrace();
		}
	}
	
	public boolean handleActivityResult(int requestCode, int resultCode, Intent data) {
		Intent intent=SpeindAPI.createIntent(getPackageName());
		intent.putExtra(SpeindAPI.SERVICE_CMD, SpeindAPI.SC_BUY_VOICE_RESULT);
		intent.putExtra(SpeindAPI.PARAM_RESULT_CODE, resultCode);
		intent.putExtra(SpeindAPI.PARAM_RESULT_DATA, data);
		this.startService(intent);
        return true;
    }
	
	public void processVoicesData(Intent intent) {
		boolean restorePurchasesAvail=false;
		boolean downloadingNow=false;
		ArrayList<String> voices=intent.getStringArrayListExtra(SpeindAPI.PARAM_VOICES_DATA);
		ArrayList<ArrayList<String>> voicesData=new ArrayList<ArrayList<String>>();
		for (String voice : voices) {
			String voiceAr[]=voice.split("\\|");
			ArrayList<String> voiceArL=new ArrayList<String>(Arrays.asList(voiceAr));
			voicesData.add(voiceArL);
			if (voiceArL.get(4).equals("1")) {
				restorePurchasesAvail=true;
			}
			if (voiceArL.get(4).equals("2")) {
				downloadingNow=true;
			}
		}

		ExpandableListView listView = (ExpandableListView)findViewById(R.id.langList);
		
		ArrayList<ArrayList<ArrayList<String>>> langs = new ArrayList<ArrayList<ArrayList<String>>>();
		String curLang="";
		ArrayList<ArrayList<String>> lang=new ArrayList<ArrayList<String>>();
		for (ArrayList<String> voiceData : voicesData) {
			if (!curLang.equals(voiceData.get(0))) {
				curLang=voiceData.get(0);
				if (lang.size()>0) {
					langs.add(lang);
					lang=new ArrayList<ArrayList<String>>();
				}
			}
			lang.add(voiceData);
		}		
		if (lang.size()>0) langs.add(lang);
		
		mGroups=langs;
		if (adapter==null) {
	        adapter = new ExpListAdapter(VoicesSettings.this);
	        listView.setAdapter(adapter);
		} else {
			adapter.notifyDataSetChanged();
		}
		if (downloadingNow) {
			Button restore_purchases=(Button) findViewById(R.id.restore_purchases);
			if (restore_purchases!=null) {
				restore_purchases.setBackgroundResource(R.drawable.btn_green);
				restore_purchases.setText(R.string.downloading);
				restore_purchases.setEnabled(false);
			}
		} else {
			if (restorePurchasesAvail) {
				Button restore_purchases=(Button) findViewById(R.id.restore_purchases);
				if (restore_purchases!=null) {
					restore_purchases.setBackgroundResource(R.drawable.btn_simple);
					restore_purchases.setText(R.string.restore_purchases);
					restore_purchases.setEnabled(true);
					restore_purchases.setVisibility(View.VISIBLE);
				}
			} else {
				Button restore_purchases=(Button) findViewById(R.id.restore_purchases);
				if (restore_purchases!=null) {
					restore_purchases.setVisibility(View.GONE);
				}
			}
		}
	}
	
	@Override
	public void onDestroy() {
		stopVoiceDemo(cFileName, cPlayButton);  
		unregisterReceiver(voicesUpdatedReceiver);
		super.onDestroy();
	}
	
 	public int getConnectionStatus() {
		ConnectivityManager aConnectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
	    NetworkInfo aNetworkInfo = aConnectivityManager.getActiveNetworkInfo();
	    if (aNetworkInfo != null && aNetworkInfo.isConnected()){
	        return aNetworkInfo.getType();
	    }else{
	        return -1;
	    }
		
	}
 	
	public void playVoiceDemo(final String fileName, final Button playButton) {
		SpeindAPI.stop(this, getPackageName());
		stopVoiceDemo(cFileName, cPlayButton);			
        cFileName=fileName;
        cPlayButton=playButton;
		playButton.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				stopVoiceDemo(fileName, playButton);
			}
		});		
		playButton.setBackgroundResource(R.drawable.btn_stop_small);
		
		handler.post(new Runnable(){
			@Override
			public void run() {
				int aType = getConnectionStatus();
				if (aType!=-1) {
			    	try {
			    		player.reset();
			            player.setDataSource(getApplicationContext(), Uri.parse(SpeindAPI.VOICE_DEMOS_URL+fileName));	
			            player.prepare();
			            player.start();
			            return;
			    	} catch (IllegalArgumentException e) {
			    		player.reset();
			            e.printStackTrace(); 
			    	} catch (IllegalStateException e) {
			    		player.reset();
			            e.printStackTrace();
			    	} catch (IOException e) {
			    		player.reset();
			            e.printStackTrace();
			    	}
				} else {
					Toast toast = Toast.makeText(VoicesSettings.this, getString(R.string.need_internet_connection_to_operation), Toast.LENGTH_SHORT);
					toast.show();
				}
	    		playButton.setBackgroundResource(R.drawable.btn_play_small);			
			}
		});		
	}
	
	public void stopVoiceDemo(final String fileName, final Button playButton) {
		if (player.isPlaying()) player.stop();
		if (playButton!=null) {
			playButton.setOnClickListener(new OnClickListener(){
				@Override
				public void onClick(View v) {
					playVoiceDemo(fileName, playButton);
				}
			});			
	    	playButton.setBackgroundResource(R.drawable.btn_play_small);
		}
	}
	
    private MediaPlayer.OnCompletionListener onCompletePlayer = new MediaPlayer.OnCompletionListener() {        
        @Override
        public void onCompletion(MediaPlayer mp) {
        	stopVoiceDemo(cFileName, cPlayButton);
        	cPlayButton.setBackgroundResource(R.drawable.btn_play_small);
        } 
    };
	
}
