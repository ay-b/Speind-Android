package com.maple.rssreceiver;

import java.util.ArrayList;
import java.util.Locale;

import com.maple.rssreceiver.SpeindDataFeed.DatabaseManager;
import com.maple.rssreceiver.SpeindDataFeed.RSSFeed;

import com.maple.speind.R;
//import ru.lifenews.speind.R;

import com.maple.speindui.DialogWindow;

import android.app.Dialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.LinearLayout.LayoutParams;
import android.widget.TextView;

public class UserFeeds extends ActionBarActivity {
    private Handler handler = new Handler();

	private String profile="";
	private ArrayList<String> langList=null;

	private AddFeedWindow feedWnd=null;
	
	public class AddFeedWindow extends DialogFragment {
		private RSSFeed feed=null;
		private boolean add=false;
		
		public AddFeedWindow(RSSFeed pfeed, boolean add_feed) {
			feed=pfeed;
			add=add_feed;
		}
		
		@Override
		public void onDestroyView() {
			TextView nav_title=(TextView) UserFeeds.this.findViewById(R.id.toolbar_title); 
	    	if (nav_title!=null) {
	    		nav_title.setText(R.string.user_feeds_title);
	    	}
        	feedWnd=null;
			super.onDestroyView();
		}
		
	    @Override
	    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
	    	View feedView=inflater.inflate(R.layout.rss_plugin_add_user_feed_dialog, container, false);
	    	
	    	final Button checkButton=(Button) feedView.findViewById(R.id.check_feed_button);
	    	final Button addButton=(Button) feedView.findViewById(R.id.add_feed_button);
			final ProgressBar process=(ProgressBar) feedView.findViewById(R.id.processing);
			final TextView message=(TextView) feedView.findViewById(R.id.message);
			final EditText url=(EditText) feedView.findViewById(R.id.url);
			final EditText name=(EditText)feedView.findViewById(R.id.name);
			TextView title=(TextView)UserFeeds.this.findViewById(R.id.toolbar_title);
			final TextView instruction_title=(TextView)feedView.findViewById(R.id.instruction_title);
			
			if (instruction_title!=null) {
				instruction_title.setOnClickListener(new OnClickListener(){
					@Override
					public void onClick(View v) {
						Intent browseIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.add_user_feed_video_link)));
						startActivity(browseIntent);				
					}
				});
			}
			
			if (title!=null) {
				if (feed!=null) {
					title.setText(getString(R.string.edit_user_feed_title));
				} else {
					title.setText(getString(R.string.add_user_feed_title));
				}
			}
	    	final Spinner langs=(Spinner) feedView.findViewById(R.id.lang);
			if (langs!=null) {
				ArrayList<String> langsAr=new ArrayList<>();
				for (String lang : langList) {
					if (lang.equals("auto")) {
						langsAr.add(getString(R.string.auto_detect)); 
					} else {
						Locale locale=new Locale(lang);
						langsAr.add(locale.getDisplayName(locale));
					}
				}
				ArrayAdapter<String> spinnerArrayAdapter = new ArrayAdapter<>(UserFeeds.this, android.R.layout.simple_spinner_item, langsAr);
				spinnerArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
				langs.setAdapter(spinnerArrayAdapter);
		    }		
			
			
			if (url!=null) {
				url.addTextChangedListener(new TextWatcher(){
					@Override
					public void afterTextChanged(Editable arg0) {
						if (message!=null) message.setText("");
						if (addButton!=null) addButton.setVisibility(View.INVISIBLE);
						if (checkButton!=null) {
							checkButton.setVisibility(View.VISIBLE);
							checkButton.setEnabled(true);
						}
					}

					@Override
					public void beforeTextChanged(CharSequence arg0, int arg1,int arg2, int arg3) { }

					@Override
					public void onTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) { }});
			}
			
			if (process!=null) {
				process.setVisibility(View.INVISIBLE);
			}
			if (message!=null) {
				message.setText("");
				message.setVisibility(View.VISIBLE);
			}

			if (feed!=null) {
	    		
	    		if (url!=null) url.setText(feed.link, TextView.BufferType.EDITABLE);
	    		if (name!=null) name.setText(feed.category, TextView.BufferType.EDITABLE);
	    		if (langs!=null) langs.setSelection(langList.indexOf(feed.lang));

	    		if (message!=null) {
	    			message.setText("");
	    		}
			} else {
	    		if (url!=null) url.setText("", TextView.BufferType.EDITABLE);
	    		if (name!=null) name.setText("", TextView.BufferType.EDITABLE);
			}
			 
	    	if (checkButton!=null) {
	    		if (feed==null) checkButton.setVisibility(View.VISIBLE);
	    		else checkButton.setVisibility(View.INVISIBLE);
	    		
	    		checkButton.setOnClickListener(new Button.OnClickListener() {
					@Override
					public void onClick(View arg0) {
						String error="";
			    				
			    		if (message!=null) {
			    			message.setVisibility(View.INVISIBLE);
			    		}
			    		if (process!=null) {
			    			process.setVisibility(View.INVISIBLE);
			    		}

						if (url != null) {
							if (url.getText().toString().equals("")) {
                                error=getString(R.string.no_url);
                            }
						}
						if (name != null) {
							if (name.getText().toString().equals("")) {
                                error=getString(R.string.no_name);
                            }
						}

						if (error.equals("")) {
							checkButton.setEnabled(false);
							if (url != null) {
								if (!(url.getText().toString().startsWith("http://")||url.getText().toString().startsWith("https://"))) {
                                    url.setText("http://"+url.getText().toString());
                                }
							}
							if (url != null) {
								checkFeed(url.getText().toString());
							}
						} else {
				    		if (message!=null) {
				    			message.setText(error);
				    			message.setVisibility(View.VISIBLE);
				    		}
							
						}
					} 
				});
	    	}
	    	if (addButton!=null) {
	    		if (feed==null||add) {
	        		if (!add) addButton.setVisibility(View.INVISIBLE);
	        		else addButton.setVisibility(View.VISIBLE);
	    			addButton.setText(R.string.add_news_feed);
		    		addButton.setOnClickListener(new Button.OnClickListener() {
						@Override
						public void onClick(View arg0) {
				    		
				    		if (url!=null&&name!=null&&langs!=null) {
					    		//RSSFeed feed=new RSSFeed(
					    	    //		"",
					    	    //		name.getText().toString(),
					    	    //		name.getText().toString(),
					    	    //		"",
					    	    //		url.getText().toString(),
					    	    //		name.getText().toString(),
					    	    //		true,
					    	    //		"",
					    		//		"",
					    		//		langList.get(langs.getSelectedItemPosition())
					    		//);
                                (new Thread(new Runnable() {
                                    @Override
                                    public void run() {
                                        SQLiteDatabase db = DatabaseManager.getInstance().getWritableDatabase();
                                        ContentValues cv = new ContentValues();

                                        cv.put("profile", profile);
                                        cv.put("link", url.getText().toString());
                                        cv.put("city", "");
                                        cv.put("provider", name.getText().toString());
                                        cv.put("category", name.getText().toString());
                                        cv.put("parentcategory", "");
                                        cv.put("vocalizing", name.getText().toString());
                                        cv.put("enabled", 1);
                                        cv.put("region", "");
                                        cv.put("country", "");
                                        cv.put("lang", langList.get(langs.getSelectedItemPosition()));
                                        cv.put("level", "user_feed");

                                        if (db.update("rsssources", cv, "profile = ? and link = ? and level = ?", new String[] { profile, url.getText().toString(), "user_feed" })<=0) {
                                            db.insert("rsssources", null, cv);
                                        }
                                        handler.post(new Runnable() {
                                            @Override
                                            public void run() {
                                                refreshFeeds();
                                            }
                                        });
                                    }
                                })).start();


                                feedWnd.dismiss();
                                feedWnd=null;
				    		}
						} 
					});
	    		} else {
	        		addButton.setVisibility(View.VISIBLE);
	    			addButton.setText(R.string.save_news_feed);
		    		addButton.setOnClickListener(new Button.OnClickListener() {
						@Override
						public void onClick(View arg0) {
                            (new Thread(new Runnable() {
                                @Override
                                public void run() {
                                    SQLiteDatabase db = DatabaseManager.getInstance().getWritableDatabase();
                                    ContentValues cv = new ContentValues();
                                    cv.put("category", name != null ? name.getText().toString() : "");
                                    cv.put("provider", name != null ? name.getText().toString() : "");
                                    cv.put("vocalizing", name != null ? name.getText().toString() : "");
                                    cv.put("link", url != null ? url.getText().toString() : "");
                                    cv.put("lang", langList.get(langs.getSelectedItemPosition()));
                                    if (db.update("rsssources", cv, "profile = ? and link = ? and level = ?", new String[] { profile, feed.link, "user_feed" })>0) {
                                        handler.post(new Runnable() {
                                            @Override
                                            public void run() {
                                                refreshFeeds();
                                            }
                                        });
                                    }
                                }
                            })).start();

                            feedWnd.dismiss();
                            feedWnd=null;
						} 
					});    			
	    		}
	    	}
						
	        return feedView;
	    }
	  
	    /** The system calls this only when creating the layout in a dialog. */
	    @Override
	    public Dialog onCreateDialog(Bundle savedInstanceState) {
	        Dialog dialog = super.onCreateDialog(savedInstanceState);
	        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
	        return dialog;
	    }

	    public View findViewById(int id) {
			return (getView()!=null) ? getView().findViewById(id) : null;
		}
	    
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.rss_plugin_user_feeds);

        DatabaseManager.initializeInstance(this);

		Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
		if (toolbar!=null) {
	        toolbar.setTitle("");
	        setSupportActionBar(toolbar);
	        toolbar.setNavigationIcon(R.drawable.btn_back);
	        toolbar.setNavigationOnClickListener(new OnClickListener(){
				@Override
				public void onClick(View arg0) {
					if (feedWnd==null) {
						UserFeeds.this.finish();
					} else {
						feedWnd.dismiss();
			        	feedWnd=null;
					}
				}				
			});
		}
		
		
		Button add_feed_button = (Button) findViewById(R.id.add_feed_button);
		if (add_feed_button!=null) {
			add_feed_button.setOnClickListener(new OnClickListener(){
				@Override
				public void onClick(View arg0) {
					showFeedForm(null, false);
				}				
			});
		}
		
		
		Intent intent=getIntent();		
		profile=intent.getStringExtra("profile");
		langList=intent.getStringArrayListExtra("langs");
		langList.add(0, "auto");
		refreshFeeds();
		
		String feedName=intent.getStringExtra("feed_name");
		String lang=intent.getStringExtra("feed_lang");
		String url=intent.getStringExtra("feed_url");
		
		if (url!=null&&url.length()>0) {
			RSSFeed feed=new RSSFeed("", feedName, feedName, "", url, feedName, true, "", "", lang);
			showFeedForm(feed, true);
		}
	}
	
	private void refreshFeeds() {
		LinearLayout settingsItems=(LinearLayout) findViewById(R.id.items);
		if (settingsItems!=null) {
			settingsItems.removeAllViews();		
			ProgressBar pb=new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
			LayoutParams params=new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
			//params.gravity=Gravity.CENTER;
			pb.setLayoutParams(params);
			settingsItems.addView(pb);
		}
		
		(new AsyncTask<Void, Void, LinearLayout>(){
			@Override
			protected LinearLayout doInBackground(Void... params) {
                LinearLayout viewWrap=new LinearLayout(UserFeeds.this);
                viewWrap.setOrientation(LinearLayout.VERTICAL);
                viewWrap.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));

                SQLiteDatabase db = DatabaseManager.getInstance().getReadableDatabase();

                String selection = "profile = ? and level = ?";
                String[] selectionArgs = new String[] { profile, "user_feed"};
                String orderBy = "provider";
                Cursor cursor = db.query("rsssources", null, selection, selectionArgs, null, null, orderBy);
                if (cursor.moveToFirst()) {
                    do {
                        final RSSFeed rssFeed=new RSSFeed(
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
                        if (checkLocale(rssFeed.lang)) {
                            View view = UserFeeds.this.getLayoutInflater().inflate(R.layout.rss_plugin_user_feed_item, null);
                            CheckBox c = (CheckBox) view.findViewById(R.id.title);

                            Button e = (Button) view.findViewById(R.id.edit_button);
                            Button d = (Button) view.findViewById(R.id.delete_button);

                            if (c!=null) {
                                c.setText(rssFeed.category);
                                c.setOnCheckedChangeListener(new OnCheckedChangeListener() {
                                    @Override
                                    public void onCheckedChanged(CompoundButton arg0, final boolean arg1) {
                                        (new Thread(new Runnable() {
                                            @Override
                                            public void run() {
                                                SQLiteDatabase db = DatabaseManager.getInstance().getWritableDatabase();
                                                ContentValues cv = new ContentValues();
                                                cv.put("enabled", (arg1 ? 1 : 0));
                                                db.update("rsssources", cv, "profile = ? and link = ? and level = ?", new String[] { profile, rssFeed.link, "user_feed" });
                                            }
                                        })).start();
                                    }
                                });
                                c.setChecked(rssFeed.enabled);
                            }

                            if (e!=null) {
                                e.setOnClickListener(new OnClickListener(){
                                    @Override
                                    public void onClick(View arg0) {
                                        showFeedForm(rssFeed, false);
                                    }
                                });
                            }

                            if (d!=null) {
                                d.setOnClickListener(new OnClickListener(){
                                    @Override
                                    public void onClick(View arg0) {
                                        showConfirmDeletedialog(rssFeed);
                                    }
                                });
                            }

                            viewWrap.addView(view);
                        }
                    } while (cursor.moveToNext());
                }
                cursor.close();

				return viewWrap;
			}
			
			@Override
			protected void onPostExecute(LinearLayout viewWrap) {
				LinearLayout settingsItems=(LinearLayout) findViewById(R.id.items);
				if (settingsItems!=null) {
					settingsItems.removeAllViews();
					settingsItems.addView(viewWrap);					
				}
			}
		}).execute();
		
	}

 	private boolean checkLocale(String locale) {
 		if (langList!=null) {
	 		for (String lang : langList) {
		 		if (locale.toLowerCase().contains(lang.toLowerCase())) {
		 			return true;
		 		}
	 		}
 		}
 		return false;
 	}
	
	//@Override
	//public void onDestroy() {
	//	SpeindDataFeed.saveRSSFeeds(getApplicationContext(), profile, globalRssFeedsMap, "user_feed");
    //	super.onDestroy();
	//}
	 
    public void showFeedForm(RSSFeed feed, boolean add) {
    	FragmentManager fragmentManager = getSupportFragmentManager();
    	feedWnd = new AddFeedWindow(feed, add);
    	feedWnd.setCancelable(false);
    	FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
        transaction.add(R.id.user_feeds_wrap, feedWnd);//.addToBackStack("aufwnd");
        transaction.commit();
     }
    
	private class CheckTask extends AsyncTask<String, Integer, Integer> {
		String link="";
				
		@Override
		protected Integer doInBackground(String... arg0) {
			link=arg0[0];
			ArrayList<RssItem> items=RssItem.getRssItems(link, "", "", "", "");
			if (items==null||items.size()==0) {
				return -1;
			} else {
				return 0;
			}
		}
		
		@Override		
		protected void onPostExecute(Integer result) {
			if (result==0) {
	    		ProgressBar process=(ProgressBar) feedWnd.findViewById(R.id.processing);
	    		TextView message=(TextView) feedWnd.findViewById(R.id.message);
	        	Button addButton=(Button) feedWnd.findViewById(R.id.add_feed_button);
	        	Button checkButton=(Button) feedWnd.findViewById(R.id.check_feed_button);
	    		if (process!=null) {
	    			process.setVisibility(View.INVISIBLE);
	    		}
	    		if (message!=null) {
	    			message.setText(getString(R.string.all_ok));
	    			message.setVisibility(View.VISIBLE);
	    		}
	    		if (checkButton!=null) {
	    			checkButton.setVisibility(View.INVISIBLE);
	    		}
	    		if (addButton!=null) {
	    			addButton.setVisibility(View.VISIBLE);
	    		}
				
			} else {
	    		ProgressBar process=(ProgressBar) feedWnd.findViewById(R.id.processing);
	    		TextView message=(TextView) feedWnd.findViewById(R.id.message);
	    		Button checkButton=(Button) feedWnd.findViewById(R.id.check_feed_button);
	    		if (process!=null) {
	    			process.setVisibility(View.INVISIBLE);
	    		}
	    		if (message!=null) {
	    			message.setText(getString(R.string.bad_link));
	    			message.setVisibility(View.VISIBLE);
	    		}
	    		if (checkButton!=null) {
	    			checkButton.setEnabled(true);
	    		}
	    	}		
		}
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
    private void checkFeed(String link) {    	
    	if (getConnectionStatus()!=-1) {
    		ProgressBar process=(ProgressBar) feedWnd.findViewById(R.id.processing);
    		if (process!=null) {
    			process.setVisibility(View.VISIBLE);
    		}
	    	CheckTask checkTask=new CheckTask();
	    	checkTask.execute(link);
    	} else {
    		TextView message=(TextView) feedWnd.findViewById(R.id.message);
    		Button checkButton=(Button) feedWnd.findViewById(R.id.check_feed_button);
    		if (message!=null) {
    			message.setText(getString(R.string.no_internet_connection));
    			message.setVisibility(View.VISIBLE);
    		}
    		if (checkButton!=null) {
    			checkButton.setEnabled(true);
    		}
    	}    	
    }
    
    private void showConfirmDeletedialog(final RSSFeed feed) {
    	final DialogWindow cdw=new DialogWindow(this, R.id.user_feeds_wrap, R.layout.speind_confirm_dialog);
    	Button okButton=(Button) cdw.findViewById(R.id.ok_button);
    	Button cancellButton=(Button) cdw.findViewById(R.id.cancell_button);
    	TextView message=(TextView) cdw.findViewById(R.id.message);
    	if (message!=null) {
    		message.setText(getString(R.string.confirm_delete)+" \""+feed.provider+"\"?");
    	}
    	if (okButton!=null) {
    		okButton.setOnClickListener(new Button.OnClickListener() {
				@Override
				public void onClick(View arg0) {
					cdw.hide();
                    (new Thread(new Runnable() {
                        @Override
                        public void run() {
                            SQLiteDatabase db = DatabaseManager.getInstance().getWritableDatabase();
                            db.delete("rsssources", "profile = ? and link = ? and level = ?", new String[]{profile, feed.link, "user_feed"});
                            handler.post(new Runnable() {
                                @Override
                                public void run() {
                                    refreshFeeds();
                                }
                            });
                        }
                    })).start();
				}
			});
    	}
    	if (cancellButton!=null) {
    		cancellButton.setOnClickListener(new Button.OnClickListener() {
				@Override
				public void onClick(View arg0) {
					cdw.hide();
				} 
			});
    	}
    	
    	cdw.show();  		    	
    }
    
    @Override
    public void onBackPressed() {
        if (feedWnd != null) {
        	feedWnd.dismiss();
        	feedWnd=null;
        } else {
            super.onBackPressed();
        }
    }
}
