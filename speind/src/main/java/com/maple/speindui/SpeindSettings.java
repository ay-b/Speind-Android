package com.maple.speindui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import com.maple.speind.R;
import com.maple.speind.SpeindConfig;

import me.speind.SpeindAPI;
import me.speind.SpeindAPI.DataFeedSettingsInfo;

import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.ToggleButton;

public class SpeindSettings extends ActionBarActivity implements SpeindAPI.SpeindSettingsChangeListener {
	private ArrayList<PluginInfo> plugins = new ArrayList<>();
    private SpeindAPI.SpeindSettings config = null;
    private SpeindAPI.SpeindSettingsReceiver settingsReceiver = null;

    @Override
    public void onSettingsChanged(SpeindAPI.SpeindSettings oldConfig, SpeindAPI.SpeindSettings newConfig) {
        config = newConfig;
    }

    @Override
    public void onDataFeedsListChanged() {
        // TODO refresh plugins list
    }

    private class PluginInfo {
		public String packageName = "";
		public int title = 0;
		public int drawable = 0;
		PluginInfo(String packageName, int title, int drawable) {
			this.packageName=packageName;
			this.title=title;
			this.drawable=drawable;
		}
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.speind_settings);

		plugins.add(new PluginInfo("com.maple.smsplugin", R.string.smsreceiver, R.drawable.sms_logo));
		plugins.add(new PluginInfo("com.speind.twitterplugin", R.string.twitterreceiver, R.drawable.twitter_logo));
		plugins.add(new PluginInfo("com.speind.vkplugin", R.string.vk_title, R.drawable.vk_logo));
		
		Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
		if (toolbar!=null) {
	        toolbar.setTitle("");
	        setSupportActionBar(toolbar);
	        toolbar.setNavigationIcon(R.drawable.btn_back);
	        toolbar.setNavigationOnClickListener(new OnClickListener(){
				@Override
				public void onClick(View arg0) {
					SpeindSettings.this.finish();
				}				
			});
		}

        final Intent intent=getIntent();

		config=SpeindAPI.SpeindSettings.getFromIntent(intent);
        final ArrayList<DataFeedSettingsInfo> dataFeedsettingsInfos = SpeindAPI.DataFeedSettingsInfo.getListFromIntent(intent);

        settingsReceiver = new SpeindAPI.SpeindSettingsReceiver(this, config, dataFeedsettingsInfos);
        IntentFilter intFilt = new IntentFilter(SpeindAPI.BROADCAST_ACTION);
        registerReceiver(settingsReceiver, intFilt);

		LinearLayout settingsItems = (LinearLayout) findViewById(R.id.settingsItems);
		if (settingsItems!=null) {
			int cnt=dataFeedsettingsInfos.size();
			for (int i=0; i<cnt; i++) {
				final DataFeedSettingsInfo info=dataFeedsettingsInfos.get(i);
                for (int j=0; j<plugins.size(); j++) {
                    if (info.packageName.equalsIgnoreCase(plugins.get(j).packageName)) {
                        plugins.remove(j);
                        break;
                    }
				}
			}
			for (int j=0; j<plugins.size(); j++) {
				SpeindAPI.DataFeedSettingsInfo info = new SpeindAPI.DataFeedSettingsInfo(this, getString(plugins.get(j).title), BitmapFactory.decodeResource(getResources(), plugins.get(j).drawable), false);
				info.packageName=plugins.get(j).packageName;
                info.setState(-2);
				dataFeedsettingsInfos.add(info);
            }
            Collections.sort(dataFeedsettingsInfos, new Comparator<DataFeedSettingsInfo>() {
                @Override
                public int compare(DataFeedSettingsInfo c1, DataFeedSettingsInfo c2) {
                    return c1.getTitle().compareToIgnoreCase(c2.getTitle());
                }
		    });
            cnt=dataFeedsettingsInfos.size();
			for (int i=0; i<cnt; i++) {
				final DataFeedSettingsInfo info=dataFeedsettingsInfos.get(i);
				// TODO
				
				//LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
				//if (inflater!=null) {
					View item=getLayoutInflater().inflate(R.layout.speind_data_feed_item, null);
					if (item!=null) {						
						
						ImageView logo=(ImageView)item.findViewById(R.id.logo);
						TextView title=(TextView)item.findViewById(R.id.title);
						if (logo!=null) {
							logo.setImageBitmap(info.getBmp());
						}
						if (title!=null) {
							title.setText(info.getTitle());
						}
						if (info.getState()==-2) {
							item.setOnClickListener(new OnClickListener(){
								@Override
								public void onClick(View arg0) {
									Intent marketIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id="+info.packageName));
									marketIntent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY | Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
									startActivity(marketIntent);
									SpeindSettings.this.finish();									
								}
							});
						} else {
							item.setOnClickListener(new OnClickListener(){
								@Override
								public void onClick(View arg0) {
									SpeindAPI.requestPluginSettings(SpeindSettings.this, getPackageName(), info);
								}
							});
						}
						ToggleButton enPluginSwitch=(ToggleButton)item.findViewById(R.id.enable_plugin);
						if (enPluginSwitch!=null) {
							enPluginSwitch.setVisibility(View.GONE);
							enPluginSwitch.setChecked(info.getState()==SpeindAPI.DataFeedSettingsInfo.DATAFEED_STATE_READY);
							enPluginSwitch.setOnCheckedChangeListener(new OnCheckedChangeListener() {
								@Override
								public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
									if (isChecked) {
										SpeindAPI.requestResumePlugin(SpeindSettings.this, getPackageName(), info);
									} else {
										SpeindAPI.requestSuspendPlugin(SpeindSettings.this, getPackageName(), info);
									}
								}							
							});
						}
						Button settingsButton=(Button)item.findViewById(R.id.settings_button);
						if (settingsButton!=null) {
							if (info.getState()==-2) {
								settingsButton.setBackgroundResource(R.drawable.btn_buy);
								settingsButton.setVisibility(View.VISIBLE);
								settingsButton.setOnClickListener(new OnClickListener(){
									@Override
									public void onClick(View arg0) {
										Intent marketIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id="+info.packageName));
										marketIntent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY | Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
										startActivity(marketIntent);
										SpeindSettings.this.finish();
									}	
								});
							} else {
								settingsButton.setVisibility(View.VISIBLE);
								settingsButton.setOnClickListener(new OnClickListener(){
									@Override
									public void onClick(View arg0) {
										SpeindAPI.requestPluginSettings(SpeindSettings.this, getPackageName(), info);
									}
									
								});
							}
						}
						settingsItems.addView(item);
					}
				//}
			}
		}

        Button generalSettings = (Button) findViewById(R.id.general_settings_button);
        RelativeLayout general_settings_wrap = (RelativeLayout) findViewById(R.id.general_settings_wrap);

        if (generalSettings!=null) {
            if (general_settings_wrap!=null) general_settings_wrap.setOnClickListener(new RelativeLayout.OnClickListener() {
                @Override
                public void onClick(View arg0) {
                    Intent generalSettingsIntent = new Intent(SpeindSettings.this, SpeindGeneralSettings.class);
                    //SpeindAPI.DataFeedSettingsInfo.putListToIntent(generalSettingsIntent, dataFeedsettingsInfos);
                    config.putToIntent(generalSettingsIntent);
                    SpeindSettings.this.startActivity(generalSettingsIntent);
                }
            });
            generalSettings.setOnClickListener(new OnClickListener(){
                @Override
                public void onClick(View arg0) {
                    Intent generalSettingsIntent = new Intent(SpeindSettings.this, SpeindGeneralSettings.class);
                    config.putToIntent(generalSettingsIntent);
                    SpeindSettings.this.startActivity(generalSettingsIntent);
                }
            });
        }

        Button postingSettings = (Button) findViewById(R.id.post_settings_button);
        RelativeLayout posting_settings_wrap = (RelativeLayout) findViewById(R.id.post_settings_wrap);

        if (postingSettings!=null) {
            if (posting_settings_wrap!=null) posting_settings_wrap.setOnClickListener(new RelativeLayout.OnClickListener() {
                @Override
                public void onClick(View arg0) {
                    Intent postingSettingsIntent = new Intent(SpeindSettings.this, SpeindPostingSettings.class);
                    SpeindAPI.DataFeedSettingsInfo.putListToIntent(postingSettingsIntent, dataFeedsettingsInfos);
                    config.putToIntent(postingSettingsIntent);
                    SpeindSettings.this.startActivity(postingSettingsIntent);
                }
            });
            postingSettings.setOnClickListener(new OnClickListener(){
                @Override
                public void onClick(View arg0) {
                    Intent postingSettingsIntent = new Intent(SpeindSettings.this, SpeindPostingSettings.class);
                    SpeindAPI.DataFeedSettingsInfo.putListToIntent(postingSettingsIntent, dataFeedsettingsInfos);
                    config.putToIntent(postingSettingsIntent);
                    SpeindSettings.this.startActivity(postingSettingsIntent);
                }
            });
        }

        Button pinboardSettings = (Button) findViewById(R.id.pinboard_settings_button);
        RelativeLayout pinboard_settings_wrap = (RelativeLayout) findViewById(R.id.pinboard_settings_wrap);

        if (pinboardSettings!=null) {
			if (SpeindConfig.exclude_pinboard) {
				if (pinboard_settings_wrap != null) pinboard_settings_wrap.setVisibility(View.GONE);
			} else {
				if (pinboard_settings_wrap != null) pinboard_settings_wrap.setOnClickListener(new RelativeLayout.OnClickListener() {
					@Override
					public void onClick(View arg0) {
						SpeindAPI.openPinboardSettingsRequest(SpeindSettings.this, getPackageName());
					}
				});
				pinboardSettings.setOnClickListener(new OnClickListener(){
					@Override
					public void onClick(View arg0) {
						SpeindAPI.openPinboardSettingsRequest(SpeindSettings.this, getPackageName());
					}
				});
			}
        }

		Button voicesSettings = (Button) findViewById(R.id.langs_settings_button);
		RelativeLayout langs_settings_wrap = (RelativeLayout) findViewById(R.id.langs_settings_wrap);
		
		if (voicesSettings!=null) {
			if (langs_settings_wrap!=null) langs_settings_wrap.setOnClickListener(new RelativeLayout.OnClickListener() {
				@Override
				public void onClick(View arg0) {
					SpeindAPI.openVoicesSettingsRequest(SpeindSettings.this, getPackageName());
				}
			});
			voicesSettings.setOnClickListener(new OnClickListener(){
				@Override
				public void onClick(View arg0) {
					SpeindAPI.openVoicesSettingsRequest(SpeindSettings.this, getPackageName());
				}				
			});				
		}

	}

    @Override
    protected void onDestroy() {
        if (settingsReceiver!=null) {
            unregisterReceiver(settingsReceiver);
            settingsReceiver=null;
        }
        super.onDestroy();
    }

}
