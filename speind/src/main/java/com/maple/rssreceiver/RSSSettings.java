package com.maple.rssreceiver;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import com.maple.rssreceiver.SpeindDataFeed.RSSFeed;
import com.maple.rssreceiver.SpeindDataFeed.DatabaseManager;

import com.maple.speind.R;
//import ru.lifenews.speind.R;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.telephony.TelephonyManager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.ToggleButton;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;

public class RSSSettings extends ActionBarActivity {
	private BroadcastReceiver feedsUpdatedReceiver;
	private String profile="";
	private ArrayList<String> langList=null;
	
	//private Map<String, RSSFeed> globalRssFeedsMap = new HashMap<>();
	//private ArrayList<RSSFeed> globalRssFeedsAr = null;

	//private FeedNode feedTree=null;
	private ArrayList<String> curLangs=null;
	private Map<String, ArrayList<String>> curCountries=null;
	private String curCountry="";
	private String curLang="";
	
	private Handler handler = new Handler();
	
	private class FeedItem {
		RSSFeed feed;
		FeedNode parent;
		View view;
		FeedItem (RSSFeed feed, FeedNode parent) {
			this.feed=feed;
			this.parent=parent;
            //LayoutInflater inflater = (LayoutInflater) getApplicationContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            this.view = getLayoutInflater().inflate(R.layout.rss_plugin_feed_item_view, null);
            final ToggleButton c = (ToggleButton) view.findViewById(R.id.title);
            TextView t = (TextView) view.findViewById(R.id.item_title);
            if (t!=null) {
            	t.setText(this.feed.category);
            	t.setOnClickListener(new OnClickListener(){
					@Override
					public void onClick(View v) {
						if (c!=null) {
							c.setChecked(!c.isChecked());
						}
					}});
            }
            if (c!=null) {            	
            	c.setOnCheckedChangeListener(new OnCheckedChangeListener() {
					@Override
					public void onCheckedChanged(CompoundButton arg0, boolean arg1) {
						if (FeedItem.this.feed.provider.equals("Speind")) {
							if (!arg1) c.setChecked(true);
							arg1=true;
						}
						FeedNode nodeParent=FeedItem.this.parent;
						if (nodeParent!=null) {
							if (arg1) {
								ToggleButton c = (ToggleButton) nodeParent.view.findViewById(R.id.title);
								if (c!=null) {
									if (!c.isChecked()) {
										nodeParent.processChildOnclick=false;
										c.setChecked(true);
									}
								}
							} else {
								boolean setChecked= false;
								int cnt=nodeParent.nodes.size();
								for (int i=0;i<cnt;i++) {
									FeedNode node=nodeParent.nodes.get(i);
									ToggleButton c1 = (ToggleButton) node.view.findViewById(R.id.title);
									if (c1!=null) {
										if (c1.isChecked()) {
											setChecked=true;
											break;
										}
									}
								}
								if (!setChecked) {
									cnt=nodeParent.items.size();
									for (int i=0;i<cnt;i++) {
										FeedItem item=nodeParent.items.get(i);
										ToggleButton c1 = (ToggleButton) item.view.findViewById(R.id.title);
										if (c1!=null) {
											if (c1.isChecked()) {
												setChecked=true;
												break;
											}
										}
									}
								}
								ToggleButton c = (ToggleButton) nodeParent.view.findViewById(R.id.title);
								if (c!=null) {
									if (c.isChecked()!=setChecked) {
										nodeParent.processChildOnclick=false;
										c.setChecked(setChecked);
									}
								}
							}
						}
						if (FeedItem.this.feed.enabled!=arg1) {
							FeedItem.this.feed.enabled=arg1;
							(new Thread(new Runnable() {
								@Override
								public void run() {
									SQLiteDatabase db = DatabaseManager.getInstance().getWritableDatabase();
									ContentValues cv = new ContentValues();
									cv.put("enabled", (FeedItem.this.feed.enabled ? 1 : 0));
									db.update("rsssources", cv, "profile = ? and link = ? and level = ?", new String[] { profile, FeedItem.this.feed.link, "feed" });
								}
							})).start();
						}
					}
				});
            	handler.post(new Runnable() {
					@Override
					public void run() {
						c.setChecked(FeedItem.this.feed.enabled);
					}
				});
            }
		}
	}
	
	private class FeedNode {
		public FeedNode parent=null;
		public String name;
		public boolean processChildOnclick;
		ArrayList<FeedNode> nodes=new ArrayList<>();
		ArrayList<FeedItem> items=new ArrayList<>();
		View view;

		private Boolean sce = true;

		FeedNode(String name, FeedNode parent) {
			this.name=name;
			this.parent=parent;
			this.processChildOnclick=true;
            //LayoutInflater inflater = (LayoutInflater) getApplicationContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            this.view = getLayoutInflater().inflate(R.layout.rss_plugin_feed_group, null);
            RelativeLayout w = (RelativeLayout) view.findViewById(R.id.groupWrap);
            ToggleButton c = (ToggleButton) view.findViewById(R.id.title);
            TextView t = (TextView) view.findViewById(R.id.group_title);
            final ToggleButton tb = (ToggleButton) view.findViewById(R.id.show);
            final LinearLayout ll = (LinearLayout) view.findViewById(R.id.content);
            if (t!=null) {
            	t.setText(name);
            }
            if (c!=null) {
            	c.setOnCheckedChangeListener(new OnCheckedChangeListener() {
					@Override
					public void onCheckedChanged(CompoundButton arg0, boolean arg1) {
						if (FeedNode.this.processChildOnclick && (sce || !arg1)) {
							int cnt = FeedNode.this.nodes.size();
							for (int i = 0; i < cnt; i++) {
								FeedNode node = FeedNode.this.nodes.get(i);
								ToggleButton c1 = (ToggleButton) node.view.findViewById(R.id.title);
								if (c1 != null) {
									c1.setChecked(arg1);
								}
							}
							cnt = FeedNode.this.items.size();
							for (int i = 0; i < cnt; i++) {
								FeedItem item = FeedNode.this.items.get(i);
								ToggleButton c1 = (ToggleButton) item.view.findViewById(R.id.title);
								if (c1 != null) {
									c1.setChecked(arg1);
								}
							}
						}
						FeedNode nodeParent = FeedNode.this.parent;
						if (nodeParent != null) {
							if (arg1) {
								ToggleButton c = (ToggleButton) nodeParent.view.findViewById(R.id.title);
								if (c != null) {
									if (!c.isChecked()) {
										nodeParent.processChildOnclick = false;
										c.setChecked(true);
									}
								}
							} else {
								boolean setChecked = false;
								int cnt = nodeParent.nodes.size();
								for (int i = 0; i < cnt; i++) {
									FeedNode node = nodeParent.nodes.get(i);
									ToggleButton c1 = (ToggleButton) node.view.findViewById(R.id.title);
									if (c1 != null) {
										if (c1.isChecked()) {
											setChecked = true;
											break;
										}
									}
								}
								if (!setChecked) {
									cnt = nodeParent.items.size();
									for (int i = 0; i < cnt; i++) {
										FeedItem item = nodeParent.items.get(i);
										ToggleButton c1 = (ToggleButton) item.view.findViewById(R.id.title);
										if (c1 != null) {
											if (c1.isChecked()) {
												setChecked = true;
												break;
											}
										}
									}
								}
								ToggleButton c = (ToggleButton) nodeParent.view.findViewById(R.id.title);
								if (c != null) {
									if (c.isChecked() != setChecked) {
										nodeParent.processChildOnclick = false;
										c.setChecked(setChecked);
									}
								}
							}
						}
						if (FeedNode.this.processChildOnclick && !sce && arg1) {
							new Handler().post(new Runnable() {
								@Override
								public void run() {
									FeedNode.this.processChildOnclick = false;
									ToggleButton c = (ToggleButton) FeedNode.this.view.findViewById(R.id.title);
									c.setChecked(false);
								}
							});
						}
						FeedNode.this.processChildOnclick = true;
					}
				});
            }
            if (tb!=null) {
            	if (w!=null) {
            		w.setOnClickListener(new OnClickListener(){
						@Override
						public void onClick(View arg0) {
							tb.setChecked(!tb.isChecked());
						}});
            	}
            	tb.setOnCheckedChangeListener(new OnCheckedChangeListener() {

					@Override
					public void onCheckedChanged(CompoundButton arg0, boolean arg1) {
						if (ll != null) {
							if (arg1) {
								for (FeedNode node : FeedNode.this.nodes) {
									ll.addView(node.view);
								}
								for (FeedItem item : FeedNode.this.items) {
									ll.addView(item.view);
								}
							} else {
								ll.removeAllViews();
							}
						}
					}

				});
            }
            
		}

		FeedNode(String name, FeedNode parent, Boolean selectChildsEnable) {
			this(name, parent);
			sce = selectChildsEnable;
		}

		public int getIndexByNodeName(String name) {
			int res=-1;
			int cnt=nodes.size();
			for (int i=0;i<cnt;i++) {
				if (nodes.get(i).name.equals(name)) {
					res=i;
					break;
				}
			}
			return res;
		}
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.rss_plugin_settings);

		DatabaseManager.initializeInstance(this);

		Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
		if (toolbar!=null) {
	        toolbar.setTitle("");
	        setSupportActionBar(toolbar);
	        toolbar.setNavigationIcon(R.drawable.btn_back);
	        toolbar.setNavigationOnClickListener(new OnClickListener(){
				@Override
				public void onClick(View arg0) {
					RSSSettings.this.finish();
				}				
			});
		}
    	
		feedsUpdatedReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				 int cmd= intent.getIntExtra("cmd",0);
				 if (cmd==0) {
					 langList=intent.getStringArrayListExtra("langs");
					 refreshFeeds(true);
				 }
			}
		};
		IntentFilter ifilter = new IntentFilter(SpeindDataFeed.BROADCAST_ACTION);
		registerReceiver(feedsUpdatedReceiver, ifilter);
		
		final SharedPreferences settings = getSharedPreferences(SpeindDataFeed.PREFS_NAME, 0);

		Intent intent=getIntent();		
		profile=intent.getStringExtra("profile");
		
		Spinner refrash_rate=(Spinner) findViewById(R.id.refrash_rate);
		if (refrash_rate!=null) {		
			final int refreshInterval=settings.getInt(profile+"_refreshInterval", 15*60);		
			ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, R.array.rss_refrash_intervals, android.R.layout.simple_spinner_item);
	        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
	        refrash_rate.setAdapter(adapter);
			switch (refreshInterval) {
			case 5*60:
				refrash_rate.setSelection(0);
				break;
			case 15*60:
				refrash_rate.setSelection(1);
				break;
			case 30*60:
				refrash_rate.setSelection(2);
				break;
			case 60*60:
				refrash_rate.setSelection(3);
				break;
			case 5*60*60:
				refrash_rate.setSelection(4);
				break;
			default:
				refrash_rate.setSelection(1);
				break;
			}
	        refrash_rate.setOnItemSelectedListener(new OnItemSelectedListener(){
				@Override
				public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
					int refreshInterval_new=15*60;
					switch (arg2) {
					case 0:
						refreshInterval_new=5*60;
						break;
					case 1:
						refreshInterval_new=15*60;
						break;
					case 2:
						refreshInterval_new=30*60;
						break;
					case 3:
						refreshInterval_new=60*60;
						break;
					case 4:
						refreshInterval_new=5*60*60;
						break;
					}
					if (refreshInterval!=refreshInterval_new) {
						SharedPreferences.Editor editor = settings.edit();
				        editor.putInt(profile+"_refreshInterval", refreshInterval_new);
				        editor.apply();
					}
				}

				@Override
				public void onNothingSelected(AdapterView<?> arg0) {}
	        	
	        });
		}
		
		Spinner countries=(Spinner) findViewById(R.id.countries);
		if (countries!=null) {
			countries.setOnItemSelectedListener(new OnItemSelectedListener(){
				@Override
				public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
					if (curCountries.containsKey(curLang)) {
						ArrayList<String> countriesAr=curCountries.get(curLang);
						if (arg2>=0&&countriesAr.size()>0&&arg2<countriesAr.size()) {
							if (!curCountry.equals(countriesAr.get(arg2))) {
								curCountry=countriesAr.get(arg2);
						    	SharedPreferences.Editor editor = settings.edit();
						        editor.putString("lastCountry", curCountry);
						        editor.apply();
								refreshFeeds(false);				
							}
						}
					}					
				}
				@Override
				public void onNothingSelected(AdapterView<?> arg0) {}	        	
	        });
			countries.setEnabled(false);
		}
	
		Spinner langs=(Spinner) findViewById(R.id.langs);
		if (langs!=null) {
			langs.setOnItemSelectedListener(new OnItemSelectedListener(){

				@Override
				public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
					if (arg2>=0&&curLangs.size()>0&&arg2<curLangs.size()) {
						if (!curLang.equals(curLangs.get(arg2))) {
							curLang=curLangs.get(arg2);
					    	Spinner countries=(Spinner) findViewById(R.id.countries);
							if (countries!=null) {
								countries.setEnabled(false);
								if (curCountries.containsKey(curLang)) {
									ArrayList<String> countriesAr=curCountries.get(curLang);
									curCountry=countriesAr.get(0);
									ArrayAdapter<String> spinnerArrayAdapter = new ArrayAdapter<>(RSSSettings.this, android.R.layout.simple_spinner_item, countriesAr);
									spinnerArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
									countries.setAdapter(spinnerArrayAdapter);
								}
							}
					    	SharedPreferences.Editor editor = settings.edit();
					        editor.putString("lastLang", curLang);
					        editor.putString("lastCountry", curCountry);
					        editor.apply();							
							refreshFeeds(false);				
						}
					}
										
				}

				@Override
				public void onNothingSelected(AdapterView<?> arg0) {}
	        	
	        });
			langs.setEnabled(false);
		}
		
		langList=intent.getStringArrayListExtra("langs");	
		
		Locale current = getResources().getConfiguration().locale;
		
		TelephonyManager tm = (TelephonyManager)getSystemService(Context.TELEPHONY_SERVICE);
		curCountry=tm.getNetworkCountryIso();
		
		curLang=settings.getString("lastLang", current.getLanguage());
		curCountry=settings.getString("lastCountry", curCountry.isEmpty() ? current.getCountry().toLowerCase() : curCountry.toLowerCase());
		
		Log.e("[---!!!---]", "! "+curLang+" "+curCountry+" "+current.getLanguage()+" "+tm.getNetworkCountryIso()+" "+current.getCountry().toLowerCase());
		
		refreshFeeds(true);
		
		Button userFeeds = (Button) findViewById(R.id.user_feeds_button);
		RelativeLayout userFeeds_wrap = (RelativeLayout) findViewById(R.id.user_feeds_wrap);
		if (userFeeds!=null) {
			if (userFeeds_wrap!=null) {
				userFeeds_wrap.setOnClickListener(new OnClickListener(){

					@Override
					public void onClick(View v) {
						Intent intent=new Intent(RSSSettings.this,UserFeeds.class);
						intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);		
						intent.putExtra("profile", profile);
						intent.putStringArrayListExtra("langs", langList);
						startActivity(intent);
					}});
			}
			userFeeds.setOnClickListener(new OnClickListener(){
				@Override
				public void onClick(View arg0) {
					Intent intent=new Intent(RSSSettings.this,UserFeeds.class);
					intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);		
					intent.putExtra("profile", profile);
					intent.putStringArrayListExtra("langs", langList);
					startActivity(intent);
				}				
			});
		}
		
    }
	
	private void refreshFeeds(boolean pInit) {
		LinearLayout settingsItems=(LinearLayout) findViewById(R.id.settingsItems);
		if (settingsItems!=null) {
			settingsItems.removeAllViews();
			ProgressBar pb=new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
			LayoutParams params=new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
			//params.gravity=Gravity.CENTER;
	    	WindowManager mWindowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
	        DisplayMetrics dm=new DisplayMetrics();
	        mWindowManager.getDefaultDisplay().getMetrics(dm); 	    	

	        params.setMargins((int)(20*dm.densityDpi/160f), (int)(8*dm.densityDpi/160f), 0, (int)(8*dm.densityDpi/160f));
			pb.setLayoutParams(params);
			pb.setIndeterminate(true);
			settingsItems.addView(pb);
		}
		
		final boolean init=pInit;
		
		Spinner countries=(Spinner) findViewById(R.id.countries);
		if (countries!=null) {
			countries.setEnabled(false);
		}
		Spinner langs=(Spinner) findViewById(R.id.langs);
		if (langs!=null) {
			langs.setEnabled(false);
		}
		(new AsyncTask<Void, Void, FeedNode>(){
			@Override
			protected FeedNode doInBackground(Void... params) {
				SQLiteDatabase db = DatabaseManager.getInstance().getReadableDatabase();

				if (init) {
					curLangs=new ArrayList<>();
					curCountries=new HashMap<>();

					String[] columns = new String[] { "distinct lang, country" };
					String selection = "profile = ? and level = ?";
					String[] selectionArgs = new String[] { profile, "feed"};
					String orderBy = "lang, country";
					Cursor langcursor = db.query("rsssources", columns, selection, selectionArgs, null, null, orderBy);
					if (langcursor.moveToFirst()) {
						do {
							String lang = langcursor.getString(langcursor.getColumnIndex("lang"));
							String country = langcursor.getString(langcursor.getColumnIndex("country"));
							if (checkLocale(lang)) {
								if (!curLangs.contains(lang))curLangs.add(lang);
								if (!curCountries.containsKey(lang)) curCountries.put(lang, new ArrayList<String>());
								if (!curCountries.get(lang).contains(country)) curCountries.get(lang).add(country);
							}
						} while (langcursor.moveToNext());
					}
					langcursor.close();
			    	
			    	if (curLang.isEmpty()||!curLangs.contains(curLang)) {
			    		if (curLangs.size()>0) {
			    			curLang=curLangs.get(0);
			    		}
			    	}
			    	
			    	if (curCountries.get(curLang)!=null) {
			    		if (!curCountries.get(curLang).contains(curCountry)) {
				    		curCountry=curCountries.get(curLang).get(0);
			    		}
				    	publishProgress((Void)null);
			    	}
				}

				final SharedPreferences settings = getSharedPreferences(SpeindDataFeed.PREFS_NAME, 0);
				SharedPreferences.Editor editor = settings.edit();
				editor.putString("lastLang", curLang);
				editor.putString("lastCountry", curCountry);
				editor.apply();

				FeedNode feedTree=new FeedNode("root", null);
				feedTree.nodes.add(new FeedNode(getString(R.string.main_news), feedTree, false));
				feedTree.nodes.add(new FeedNode(getString(R.string.regional_news), feedTree, false));

				String selection = "profile = ? and level = ? and country = ? and lang = ?";
				String[] selectionArgs = new String[] { profile, "feed", curCountry, curLang };
				String orderBy = "city, provider, parentcategory, category";
				Cursor cursor = db.query("rsssources", null, selection, selectionArgs, null, null, orderBy);
				if (cursor.moveToFirst()) {
					do {
						RSSFeed rssFeed=new RSSFeed(
								cursor.getString(cursor.getColumnIndex("city")),
								cursor.getString(cursor.getColumnIndex("provider")),
								cursor.getString(cursor.getColumnIndex("category")),
								cursor.getString(cursor.getColumnIndex("parentcategory")),
								cursor.getString(cursor.getColumnIndex("link")),
								cursor.getString(cursor.getColumnIndex("vocalizing")),
								cursor.getInt(cursor.getColumnIndex("enabled"))!=0,
								cursor.getString(cursor.getColumnIndex("region")),
								cursor.getString(cursor.getColumnIndex("country")),
								cursor.getString(cursor.getColumnIndex("lang"))
						);
						if (rssFeed.city.equals("")) {
							int index=feedTree.getIndexByNodeName(getString(R.string.main_news));
							if (index>-1) {
								int index1=feedTree.nodes.get(index).getIndexByNodeName(rssFeed.provider);
								if (index1==-1) {
									feedTree.nodes.get(index).nodes.add(new FeedNode(rssFeed.provider, feedTree.nodes.get(index)));
									index1=feedTree.nodes.get(index).nodes.size()-1;
								}
								if (rssFeed.parentcategory.equals("")) {
									feedTree.nodes.get(index).nodes.get(index1).items.add(new FeedItem(rssFeed, feedTree.nodes.get(index).nodes.get(index1)));
								} else {
									int index2=feedTree.nodes.get(index).nodes.get(index1).getIndexByNodeName(rssFeed.parentcategory);
									if (index2==-1) {
										feedTree.nodes.get(index).nodes.get(index1).nodes.add(new FeedNode(rssFeed.parentcategory, feedTree.nodes.get(index).nodes.get(index1)));
										index2=feedTree.nodes.get(index).nodes.get(index1).nodes.size()-1;
									}
									feedTree.nodes.get(index).nodes.get(index1).nodes.get(index2).items.add(new FeedItem(rssFeed, feedTree.nodes.get(index).nodes.get(index1).nodes.get(index2)));
								}
							}
						} else {
							int index=feedTree.getIndexByNodeName(getString(R.string.regional_news));
							if (index>-1) {
								int index1=feedTree.nodes.get(index).getIndexByNodeName(rssFeed.city);
								if (index1==-1) {
									feedTree.nodes.get(index).nodes.add(new FeedNode(rssFeed.city, feedTree.nodes.get(index)));
									index1=feedTree.nodes.get(index).nodes.size()-1;
									feedTree.nodes.get(index).nodes.get(index1).nodes.add(new FeedNode(rssFeed.provider, feedTree.nodes.get(index).nodes.get(index1)));
									int index2=feedTree.nodes.get(index).nodes.get(index1).nodes.size()-1;
									feedTree.nodes.get(index).nodes.get(index1).nodes.get(index2).items.add(new FeedItem(rssFeed, feedTree.nodes.get(index).nodes.get(index1).nodes.get(index2)));
								} else {
									int index2=feedTree.nodes.get(index).nodes.get(index1).getIndexByNodeName(rssFeed.provider);
									if (index2==-1) {
										feedTree.nodes.get(index).nodes.get(index1).nodes.add(new FeedNode(rssFeed.provider, feedTree.nodes.get(index).nodes.get(index1)));
										index2=feedTree.nodes.get(index).nodes.get(index1).nodes.size()-1;
										feedTree.nodes.get(index).nodes.get(index1).nodes.get(index2).items.add(new FeedItem(rssFeed, feedTree.nodes.get(index).nodes.get(index1).nodes.get(index2)));
									} else {
										feedTree.nodes.get(index).nodes.get(index1).nodes.get(index2).items.add(new FeedItem(rssFeed, feedTree.nodes.get(index).nodes.get(index1).nodes.get(index2)));
									}

								}
							}
						}
					} while (cursor.moveToNext());
				}
				cursor.close();

				return feedTree;
			}
			
			@Override
			protected void onProgressUpdate(Void... param) {
		    	Spinner langs=(Spinner) findViewById(R.id.langs);
				if (langs!=null) {
					ArrayList<String> langsAr=new ArrayList<>();
					for (String lang : curLangs) {
						Locale locale=new Locale(lang);
						langsAr.add(locale.getDisplayName(locale));
					}
					
					ArrayAdapter<String> spinnerArrayAdapter = new ArrayAdapter<>(RSSSettings.this, android.R.layout.simple_spinner_item, langsAr);
					spinnerArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
					langs.setAdapter(spinnerArrayAdapter);
					langs.setSelection(curLangs.indexOf(curLang));
				}
		    	Spinner countries=(Spinner) findViewById(R.id.countries);
				if (countries!=null) {
					if (curCountries.containsKey(curLang)) {
						ArrayList<String> countriesAr=curCountries.get(curLang);
						ArrayAdapter<String> spinnerArrayAdapter = new ArrayAdapter<>(RSSSettings.this, android.R.layout.simple_spinner_item, countriesAr);
						spinnerArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
						countries.setAdapter(spinnerArrayAdapter);
						countries.setSelection(countriesAr.indexOf(curCountry));
					}
				}
			}
			
			@Override
			protected void onPostExecute(FeedNode feedTree) {
				LinearLayout settingsItems=(LinearLayout) findViewById(R.id.settingsItems);
				if (settingsItems!=null) {
					settingsItems.removeAllViews();
					if (feedTree.nodes.size()>0) {
						if (feedTree.nodes.get(0)!=null&&feedTree.nodes.get(0).nodes!=null&&feedTree.nodes.get(0).nodes.size()!=0) {
							settingsItems.addView(feedTree.nodes.get(0).view);
						}
						if (feedTree.nodes.size()>1&&feedTree.nodes.get(1)!=null&&feedTree.nodes.get(1).nodes!=null&&feedTree.nodes.get(1).nodes.size()!=0) {					
							settingsItems.addView(feedTree.nodes.get(1).view);
						}
					}
				}
				Spinner langs=(Spinner) findViewById(R.id.langs);
				if (langs!=null) {
					if (!(curLang.equals("")||curCountry.equals(""))) {
						langs.setEnabled(true);
						Spinner countries=(Spinner) findViewById(R.id.countries);
						if (countries!=null) {
							countries.setEnabled(true);
						}			
					}
				}
			}
		}).execute();
		
	}

 	private boolean checkLocale(String locale) {
 		if (langList!=null) {
	 		for (String lang : langList) {
		 		if (locale.compareToIgnoreCase(lang)>-1) {
		 			return true;
		 		}
	 		}
 		}
 		return false;
 	}
 	
	@Override
	public void onDestroy() {
		unregisterReceiver(feedsUpdatedReceiver);		
	
//		if (globalRssFeedsMap.size()>0)
//			SpeindDataFeed.saveRSSFeeds(getApplicationContext(), profile, globalRssFeedsMap, "feed");
		
    	Intent intent = new Intent(RSSSettings.this, SpeindDataFeed.class);
    	intent.putExtra(SpeindDataFeed.RSS_FEED_SETTINGS_COMMAND, SpeindDataFeed.RSS_REFRESH_NEWS);
    	startService(intent);	  
    	super.onDestroy();
	}
		
}
