package com.maple.speindui;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.ToggleButton;

import com.maple.speind.R;

import me.speind.SpeindAPI;

public class SpeindGeneralSettings extends ActionBarActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.speind_general_settings);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        if (toolbar!=null) {
            toolbar.setTitle("");
            setSupportActionBar(toolbar);
            toolbar.setNavigationIcon(R.drawable.btn_back);
            toolbar.setNavigationOnClickListener(new View.OnClickListener(){
                @Override
                public void onClick(View arg0) {
                    SpeindGeneralSettings.this.finish();
                }
            });
        }

        Intent intent=getIntent();
        final SpeindAPI.SpeindSettings config=SpeindAPI.SpeindSettings.getFromIntent(intent);
        if (config!=null) {
            Spinner store_time=(Spinner) findViewById(R.id.store_time);
            if (store_time!=null) {
                ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, R.array.store_times, android.R.layout.simple_spinner_item);
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                store_time.setAdapter(adapter);
                if (config.infopoints_store_time==24*60*60) {
                    store_time.setSelection(0);
                } else if (config.infopoints_store_time==3*24*60*60) {
                    store_time.setSelection(1);
                }

                store_time.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){
                    @Override
                    public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
                        int ipst=24*60*60;
                        switch (arg2) {
                            case 0:
                                ipst=24*60*60;
                                break;
                            case 1:
                                ipst=3*24*60*60;
                                break;
                        }
                        if (ipst!=config.infopoints_store_time) {
                            config.infopoints_store_time=ipst;
                            SpeindAPI.saveSettings(SpeindGeneralSettings.this, getPackageName(), config);
                        }
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> arg0) { }

                });

            }

            Spinner max_play_time=(Spinner) findViewById(R.id.max_play_time);
            if (max_play_time!=null) {
                ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, R.array.max_play_times, android.R.layout.simple_spinner_item);
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                max_play_time.setAdapter(adapter);
                switch (config.max_play_time) {
                    case -1:
                        max_play_time.setSelection(0);
                        break;
                    case 0:
                        max_play_time.setSelection(1);
                        break;
                    case 5*60:
                        max_play_time.setSelection(2);
                        break;
                    case 10*60:
                        max_play_time.setSelection(3);
                        break;
                    case 15*60:
                        max_play_time.setSelection(4);
                        break;
                }

                max_play_time.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){
                    @Override
                    public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
                        int mpt=-1;
                        switch (arg2) {
                            case 0:
                                mpt=-1;
                                break;
                            case 1:
                                mpt=0;
                                break;
                            case 2:
                                mpt=5*60;
                                break;
                            case 3:
                                mpt=10*60;
                                break;
                            case 4:
                                mpt=15*60;
                                break;
                        }
                        if (mpt!=config.max_play_time) {
                            config.max_play_time=mpt;
                            SpeindAPI.saveSettings(SpeindGeneralSettings.this, getPackageName(), config);
                        }
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> arg0) { }

                });
            }

            Spinner speech_rate=(Spinner) findViewById(R.id.speech_rate);
            if (speech_rate!=null) {
                ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, R.array.speech_rates, android.R.layout.simple_spinner_item);
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                speech_rate.setAdapter(adapter);
                if (config.speech_rate == 0.5f) {
                    speech_rate.setSelection(0);
                } else if (config.speech_rate == 0.75f) {
                    speech_rate.setSelection(1);
                } else if (config.speech_rate == 1.0f) {
                    speech_rate.setSelection(2);
                } else if (config.speech_rate == 1.25f) {
                    speech_rate.setSelection(3);
                } else if (config.speech_rate == 1.5f) {
                    speech_rate.setSelection(4);
                } else if (config.speech_rate == 2.0f) {
                    speech_rate.setSelection(5);
                } else {
                    speech_rate.setSelection(1);
                }

                speech_rate.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){
                    @Override
                    public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
                        float sr=1.0f;
                        switch (arg2) {
                            case 0:
                                sr=0.5f;
                                break;
                            case 1:
                                sr=0.75f;
                                break;
                            case 2:
                                sr=1.0f;
                                break;
                            case 3:
                                sr=1.25f;
                                break;
                            case 4:
                                sr=1.5f;
                                break;
                            case 5:
                                sr=2.0f;
                                break;
                        }
                        if (sr!=config.speech_rate) {
                            config.speech_rate=sr;
                            SpeindAPI.saveSettings(SpeindGeneralSettings.this, getPackageName(), config);
                        }
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> arg0) { }

                });

            }
            final ToggleButton read_full_article = (ToggleButton) findViewById(R.id.read_full_article);
            RelativeLayout read_full_article_wrap = (RelativeLayout) findViewById(R.id.read_full_article_wrap);
            if (read_full_article!=null) {
                read_full_article.setChecked(config.read_full_article);
                if (read_full_article_wrap!=null) read_full_article_wrap.setOnClickListener(new RelativeLayout.OnClickListener() {
                    @Override
                    public void onClick(View arg0) {
                        read_full_article.setChecked(!read_full_article.isChecked());
                    }
                });
                read_full_article.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener(){
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        if (isChecked!=config.read_full_article) {
                            config.read_full_article=isChecked;
                            SpeindAPI.saveSettings(SpeindGeneralSettings.this, getPackageName(), config);
                        }
                    }
                });
            }
            final ToggleButton download_images = (ToggleButton) findViewById(R.id.download_images_on_mobile_net);
            RelativeLayout download_images_wrap = (RelativeLayout) findViewById(R.id.download_images_on_mobile_net_wrap);
            if (download_images!=null) {
                download_images.setChecked(config.not_download_images_on_mobile_net);
                if (download_images_wrap!=null) download_images_wrap.setOnClickListener(new RelativeLayout.OnClickListener() {
                    @Override
                    public void onClick(View arg0) {
                        download_images.setChecked(!download_images.isChecked());
                    }
                });
                download_images.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener(){
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        if (isChecked!=config.not_download_images_on_mobile_net) {
                            config.not_download_images_on_mobile_net=isChecked;
                            SpeindAPI.saveSettings(SpeindGeneralSettings.this, getPackageName(), config);
                        }
                    }
                });
            }

            final ToggleButton not_off_screen = (ToggleButton) findViewById(R.id.not_off_screen);
            RelativeLayout not_off_screen_wrap = (RelativeLayout) findViewById(R.id.not_off_screen_wrap);
            if (not_off_screen!=null) {
                not_off_screen.setChecked(config.not_off_screen);
                if (not_off_screen_wrap!=null) not_off_screen_wrap.setOnClickListener(new RelativeLayout.OnClickListener() {
                    @Override
                    public void onClick(View arg0) {
                        not_off_screen.setChecked(!not_off_screen.isChecked());
                    }
                });
                not_off_screen.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener(){
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        if (isChecked!=config.not_off_screen) {
                            config.not_off_screen=isChecked;
                            SpeindAPI.saveSettings(SpeindGeneralSettings.this, getPackageName(), config);
                        }
                    }
                });
            }
        }

    }

}
