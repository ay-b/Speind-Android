package com.maple.speindui;

import android.content.Intent;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.maple.speind.R;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import me.speind.SpeindAPI;

public class SpeindPostingSettings extends ActionBarActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.speind_posting_settings);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        if (toolbar!=null) {
            toolbar.setTitle("");
            setSupportActionBar(toolbar);
            toolbar.setNavigationIcon(R.drawable.btn_back);
            toolbar.setNavigationOnClickListener(new View.OnClickListener(){
                @Override
                public void onClick(View arg0) {
                    SpeindPostingSettings.this.finish();
                }
            });
        }

        Intent intent=getIntent();
        final SpeindAPI.SpeindSettings config=SpeindAPI.SpeindSettings.getFromIntent(intent);
        final ArrayList<SpeindAPI.DataFeedSettingsInfo> dataFeedsettingsInfos = SpeindAPI.DataFeedSettingsInfo.getListFromIntent(intent);
        final SpeindAPI.InfoPoint infopoint = SpeindAPI.InfoPoint.getFromIntent(intent);

        if (infopoint!=null) {
            //toolbar.setNavigationIcon(null);
            //toolbar.setNavigationOnClickListener(null);
            toolbar.setVisibility(View.GONE);
        }

        if (config!=null) {
            if (infopoint == null) {
                final ToggleButton show_before_post = (ToggleButton) findViewById(R.id.show_before_post);
                RelativeLayout show_before_post_wrap = (RelativeLayout) findViewById(R.id.show_before_post_wrap);
                if (show_before_post != null) {
                    show_before_post.setChecked(config.post_settings.ask_before_post);
                    if (show_before_post_wrap != null)
                        show_before_post_wrap.setOnClickListener(new RelativeLayout.OnClickListener() {
                            @Override
                            public void onClick(View arg0) {
                                show_before_post.setChecked(!show_before_post.isChecked());
                            }
                        });
                    show_before_post.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                        @Override
                        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                            if (isChecked != config.post_settings.ask_before_post) {
                                config.post_settings.ask_before_post = isChecked;
                                SpeindAPI.saveSettings(SpeindPostingSettings.this, getPackageName(), config);
                            }
                        }
                    });
                }
            } else {
                final RelativeLayout show_before_post_wrap = (RelativeLayout) findViewById(R.id.show_before_post_wrap);
                final TextView message = (TextView) findViewById(R.id.message);
                final RelativeLayout keep_wrap = (RelativeLayout) findViewById(R.id.keep_wrap);
                final ToggleButton keep = (ToggleButton) findViewById(R.id.keep);
                final LinearLayout buttonsWrap = (LinearLayout) findViewById(R.id.buttons_wrap);
                final Button okButton=(Button) findViewById(R.id.ok_button);
                final Button cancelButton=(Button) findViewById(R.id.cancell_button);
                if (show_before_post_wrap != null) {
                    show_before_post_wrap.setVisibility(View.GONE);
                }
                if (message != null) {
                    message.setVisibility(View.VISIBLE);
                }
                if (keep_wrap != null) {
                    keep_wrap.setVisibility(View.VISIBLE);
                    if (keep != null) {
                        keep.setChecked(true);
                    }
                }

                if (buttonsWrap!=null) {
                    buttonsWrap.setVisibility(View.VISIBLE);
                    if (okButton != null) {
                        okButton.setOnClickListener(new Button.OnClickListener() {
                            @Override
                            public void onClick(View arg0) {
                                if (keep!=null) {
                                    if (config.post_settings.ask_before_post ==keep.isChecked()) {
                                        config.post_settings.ask_before_post = !keep.isChecked();
                                        SpeindAPI.saveSettings(SpeindPostingSettings.this, getPackageName(), config);
                                    }
                                }
                                SpeindAPI.post(SpeindPostingSettings.this, getPackageName(), infopoint);
                                SpeindPostingSettings.this.finish();
                            }
                        });
                    }
                    if (cancelButton != null) {
                        cancelButton.setOnClickListener(new Button.OnClickListener() {
                            @Override
                            public void onClick(View arg0) {
                                SpeindPostingSettings.this.finish();
                            }
                        });
                    }
                }

            }

            LinearLayout settingsItems = (LinearLayout) findViewById(R.id.settingsItems);
            if (settingsItems!=null) {
                Collections.sort(dataFeedsettingsInfos, new Comparator<SpeindAPI.DataFeedSettingsInfo>() {
                    @Override
                    public int compare(SpeindAPI.DataFeedSettingsInfo c1, SpeindAPI.DataFeedSettingsInfo c2) {
                        return c1.getTitle().compareToIgnoreCase(c2.getTitle());
                    }
                });
                int cnt=dataFeedsettingsInfos.size();
                for (int i=0; i<cnt; i++) {
                    final SpeindAPI.DataFeedSettingsInfo info=dataFeedsettingsInfos.get(i);
                    if (info.canPost()) {
                        if (config.post_settings.post_plugins_data.containsKey(info.packageName)) {
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

                                final ToggleButton enPluginSwitch=(ToggleButton)item.findViewById(R.id.enable_plugin);
                                if (enPluginSwitch!=null) {
                                    enPluginSwitch.setEnabled(!info.isNeedAuthorization());
                                    enPluginSwitch.setChecked(!info.isNeedAuthorization() && config.post_settings.post_plugins_data.get(info.packageName));
                                    enPluginSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                                        @Override
                                        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                                            if (isChecked != config.post_settings.post_plugins_data.get(info.packageName)) {
                                                config.post_settings.post_plugins_data.put(info.packageName, isChecked);
                                                SpeindAPI.saveSettings(SpeindPostingSettings.this, getPackageName(), config);
                                            }
                                        }
                                    });
                                }

                                settingsItems.addView(item);

                            }
                        }
                    }
                }
            }

        }
    }

}
