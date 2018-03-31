package com.maple.speindui;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import com.maple.speind.R;
import com.maple.speind.SpeindConfig;
//import ru.lifenews.speind.R;

import me.speind.SpeindAPI;
import me.speind.SpeindAPI.DataFeedSettingsInfo;
import me.speind.SpeindAPI.InfoPoint;
import me.speind.SpeindAPI.LooperThread;
import me.speind.SpeindAPI.SpeindSettings;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.app.Dialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuffXfermode;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.SparseArray;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AbsListView.RecyclerListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView; 
import android.widget.ProgressBar;
import android.widget.Toast;
import android.widget.ToggleButton;


public class Speind extends SpeindAPI.SpeindActivivty implements SpeindAPI.SpeindUIReceiverListener {			
	public static final String PREFS_NAME = "uiConfig";
	
	private static int MODE_PLAYER=0;
    private static int MODE_LIST=1;

    public LooperThread dataFeedWorker = new LooperThread();
    
    private int mode = MODE_PLAYER;		
		    		
    private boolean main_layout_visible = false;
    
    private boolean isPaused = false;
    
	private boolean isPlayingMP3 = false;
	private int currentMP3position=0; 
	private String playerFileName="";
	private String nextPlayerFileName="";

	private int buttonsState = STATE_PLAY;
	private static int STATE_PLAY=0;
	private static int STATE_STOP_PREV=1;
	private static int STATE_STOP=2;

	private final Handler handler = new Handler();
	private final Runnable setStateStop = new Runnable() { public void run() { buttonsSwitch(STATE_STOP); } };
	private final Runnable refreshPostTimeAgo = new Runnable() { public void run() { refreshPostTimeAgoFunc(); } };
	
	private Map<String, ArrayList<String>> mediaData = new HashMap<String, ArrayList<String>>();
	
	private Map<String, ToggleButton> pluginSwithes = new HashMap<String, ToggleButton>();
	
	private ViewPager pager = null;
    private SpeindPagerAdapter pagerAdapter;
    private int pagesCount=1;
    private boolean slidingState=false;
    //private int lastSettedInfopoint=-1;
    private int pagerImageW=0;
	private int pagerImageH=0;
	private int densityDpi=160;
	
	private ListView list = null;
	private SpeindListAdapter listAdapter;
    private int itemsCount=0;
    private int listImageW=0;
	private int listImageH=0;
	private LooperThread cacheWorker = new LooperThread();
	private ArticleWindow articleWnd = null;
	
	private ArrayList<Runnable> runnableQueue=new ArrayList<Runnable>();
		
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

    public class SpeindListAdapter extends BaseAdapter implements RecyclerListener, AbsListView.OnScrollListener, OnItemClickListener {
    	private final static int OF_SCREEN_ITEMS = 3;
    	
    	private class SpeindListAdapterViewHolder {
    		TextView date = null;
        	TextView sender = null;
        	TextView text = null;
        	ImageView senderImage = null;
        	ImageView imageView = null;
    	}

    	private class SpeindListAdapterCacheItem {
    		Date date = new Date();
    		String sender="";
    		Spanned text=Html.fromHtml("");
    		Bitmap senderImageBmp=null;
    		Bitmap imageBmp=null;
    	}
    	//private SparseArray<SpeindListAdapterViewHolder> viewHolders = new SparseArray<SpeindListAdapterViewHolder>();
    	
    	private Map<View, SpeindListAdapterViewHolder> viewHolders = new HashMap<View, SpeindListAdapterViewHolder>();
    	private SparseArray<View> convertViews = new SparseArray<View>();
    	private SparseArray<SpeindListAdapterCacheItem> cache = new SparseArray<SpeindListAdapterCacheItem>();
    	private ArrayList<Integer> creatingCacheItems = new ArrayList<Integer>();
    	
    	private int startCacheInterval = 0;
    	private int endCacheInterval = 0;

		private int fvi = -1;
		private int vic = 0;

        public int getInfopointPosition(int position) {
            return getCount()-1-position;
        }

		@Override
		public void onScrollStateChanged(AbsListView view, int scrollState) {
			if (scrollState==OnScrollListener.SCROLL_STATE_IDLE) {
				slidingState = false;
				onSlidingStateIdle();
				if (fvi!=-1) {
                    int infopointPos = getInfopointPosition(fvi);
					SpeindAPI.playInfoPoint(Speind.this, speindData.service_package, infopointPos);
                    if ((itemsCount - 1 - speindData.currentInfoPoint) == 0) {
                        if (speindData.infopoints.size()!=getCount()) {
                            cache.clear();
                            itemsCount=speindData.infopoints.size();
                            notifyDataSetChanged();
                            if (list!=null) {
                                list.setSelection(getCount()-1-speindData.currentInfoPoint);
                            }
                        }
                    }
				}
			} else {
				slidingState = true;
			}
		}

		@Override
		public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
			fvi = firstVisibleItem;
			vic = visibleItemCount;			
			fixCache(Math.max(0, fvi-OF_SCREEN_ITEMS), Math.min(getCount()-1, fvi+vic+OF_SCREEN_ITEMS));
		}
    	    	
        @Override
        public int getCount() {        	
            return itemsCount;
        }

        @Override
        public Object getItem(int position) {
            return null;//viewHolders.get(position, null);
        }

        @Override
        public long getItemId(int arg0) {
            return arg0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
        	log("SpeindListAdapter.getView: "+position);
        	
        	if(convertView==null) {
                convertView = getLayoutInflater().inflate(R.layout.speind_list_item, parent, false);
                SpeindListAdapterViewHolder viewHolder = new SpeindListAdapterViewHolder();
                viewHolder.date=(TextView) convertView.findViewById(R.id.postDate);
	    		viewHolder.sender=(TextView) convertView.findViewById(R.id.postSender);
	    		viewHolder.text=(TextView) convertView.findViewById(R.id.postText);
	    		viewHolder.senderImage=(ImageView) convertView.findViewById(R.id.senderImage);
	    		viewHolder.imageView = (ImageView) convertView.findViewById(R.id.image);
	        	if (viewHolder.imageView!=null) viewHolder.imageView.setLayoutParams(new LinearLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, listImageH));
	        	viewHolders.put(convertView, viewHolder);
            }

        	int index = convertViews.indexOfValue(convertView);
        	if (index>=0) convertViews.removeAt(index);
        	convertViews.put(position, convertView);
        	
        	SpeindListAdapterViewHolder viewHolder = viewHolders.get(convertView);
        	if (viewHolder!=null) updateListViewAtPosition(position, viewHolder);
        	
            return convertView;
        }

		@Override
		public void onMovedToScrapHeap(View view) {
		}        
        
		public void updateCachedTimeAgo() {
			for (int i=0; i<convertViews.size();i++) {
				int pos = convertViews.keyAt(i); 
				View v = convertViews.valueAt(i);
				if (speindData.infopoints.size()>(pos)) {
					SpeindAPI.InfoPoint infopoint = speindData.infopoints.get(itemsCount - 1 - pos);
		        	final TextView date=(TextView) v.findViewById(R.id.postDate);
		        	if (date!=null) { date.setText(secoundsToTimeAgo(((new Date()).getTime()-infopoint.postTime.getTime())/1000)); }						
				}
			}
		}
		
		private void fixCache(int sp, int ep) {
			int delta = Math.max(Math.abs(sp-startCacheInterval), Math.abs(ep-endCacheInterval)); 
			if (delta<2) return;
			startCacheInterval=sp;
			endCacheInterval=ep;
			log("fixCache: ["+startCacheInterval+","+endCacheInterval+"]");
			ArrayList<Integer> delPos = new ArrayList<Integer>();
			for (int i=0; i<cache.size();i++) {
				int pos = cache.keyAt(i); 
				if (pos>endCacheInterval||pos<startCacheInterval) {
					log("ToRemove: "+pos);
					delPos.add(pos);
				}
			}
			for (Integer key : delPos) {
				log("Remove: "+key);
				cache.remove(key);
			}
			for (int position=endCacheInterval;position>=startCacheInterval; position--) {
				if (cache.get(position)==null) createCacheItem(position, position>=fvi&&position<fvi+vic);
			}
		}
		
        private void createCacheItem(final int position, final boolean sync) {
        	Runnable ccir = new Runnable(){
				@Override
				public void run() {
		        	final SpeindListAdapterCacheItem cacheItem = new SpeindListAdapterCacheItem(); 
					InfoPoint infopoint=speindData.infopoints.get(getInfopointPosition(position));
					if (infopoint!=null) {
						cacheItem.imageBmp=infopoint.getPostBmp(speindData.currentProfile, listImageW, listImageH);
						/*
			    		if (cacheItem.imageBmp!=null) {				
				    		for (SpeindAPI.DataFeedSettingsInfo pluginData : speindData.dataFeedsettingsInfos) {
				    			if (infopoint.processingPlugin.equalsIgnoreCase(pluginData.packageName)) {
				    				Bitmap result = Bitmap.createBitmap(listImageW, listImageH, Bitmap.Config.ARGB_8888);
									Canvas canvas = new Canvas(result);	    					
									canvas.drawBitmap(cacheItem.imageBmp, (listImageW-cacheItem.imageBmp.getScaledWidth(canvas))/2, (listImageH-cacheItem.imageBmp.getScaledHeight(canvas))/2, null);
									Bitmap pluginBmp=SpeindAPI.GetScaledBitmap(pluginData.bmpPath, (int)listImageW/8, (int)listImageH/8);
									int xpos=listImageW-(int)(10*densityDpi/160f)-pluginBmp.getScaledWidth(canvas);
									int ypos=listImageH-(int)(10*densityDpi/160f)-pluginBmp.getScaledHeight(canvas);
									Paint paint = new Paint();    
									paint.setAlpha(90);  
						    		canvas.drawBitmap(pluginBmp, xpos, ypos, paint);
						    		cacheItem.imageBmp=result;
				    				break;
				    			}
				    		}
						}		    	
			    		*/
						cacheItem.date=infopoint.postTime; 
						SpeindAPI.InfoPointData data=infopoint.getData(speindData.currentProfile);
			        	if (data!=null) {
			        		cacheItem.sender=data.postSender;
			        		cacheItem.text=(!infopoint.titleExists) ? cacheItem.text=Html.fromHtml(data.postOriginText) : Html.fromHtml(data.postTitle+"<br>"+data.postOriginText);
							Bitmap senderBmp=infopoint.getSenderBmp(speindData.currentProfile);
							if (senderBmp==null) {
								if (data.pluginBmpPath.equals("")) {
									//for (SpeindAPI.DataFeedSettingsInfo pluginData : speindData.dataFeedsettingsInfos) {
									//	if (infopoint.processingPlugin.equalsIgnoreCase(pluginData.packageName)) {
									//		senderBmp=pluginData.getBmp();
									//	}
									//}
								} else {
									senderBmp = SpeindAPI.GetScaledBitmap(data.pluginBmpPath, 96, 96);
								}
							}
							if (senderBmp!=null) {
								int l = Math.min(senderBmp.getWidth(), senderBmp.getHeight());
								Bitmap rounder = Bitmap.createBitmap(l,l,Bitmap.Config.ARGB_8888);
								Canvas canvas = new Canvas(rounder);
								Paint xferPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
								xferPaint.setColor(Color.RED);
								canvas.drawCircle(l/2, l/2, (l-1)/2, xferPaint); //drawRoundRect(new RectF(0,0,w,h), 20.0f, 20.0f, xferPaint);
								xferPaint.setXfermode(new PorterDuffXfermode(android.graphics.PorterDuff.Mode.DST_IN));
								
								Bitmap result = Bitmap.createBitmap(l, l, Bitmap.Config.ARGB_8888);
								Canvas resultCanvas = new Canvas(result);
								resultCanvas.drawBitmap(senderBmp, 0, 0, null);
								resultCanvas.drawBitmap(rounder, 0, 0, xferPaint);
								
								cacheItem.senderImageBmp=result;
							}
			        	}
					}
					Runnable func = new Runnable() {
						@Override
						public void run() {
							log("cacheCreated: "+position);
							cache.put(position, cacheItem);
							creatingCacheItems.remove(creatingCacheItems.indexOf(position));
							View convertView = convertViews.get(position);
							if (convertView!=null) {								
								SpeindListAdapterViewHolder viewHolder = viewHolders.get(convertView);
					        	if (viewHolder!=null) updateListViewAtPosition(position, viewHolder);
							}
						}
					};
					if (sync) {
						func.run();
					} else {
						handler.post(func);
					}
				}
        	};
        	if (!creatingCacheItems.contains(position)) {
            	log("createCacheItem: "+position);
        		creatingCacheItems.add(position);
        		if (sync) {
        			ccir.run();
        		} else {
        			(new Thread(ccir)).start();
        		}
        	}
        }
        
        private void updateListViewAtPosition(int position, SpeindListAdapterViewHolder viewHolder) {
        	log("updateListViewAtPosition: "+position);
        	if (viewHolder==null) return;
        	SpeindListAdapterCacheItem cacheItem = cache.get(position, null);
        	if (cacheItem!=null)  {
    			if (viewHolder.imageView!=null) {
    				if (cacheItem.imageBmp!=null) {
    					viewHolder.imageView.setImageBitmap(cacheItem.imageBmp);
    					viewHolder.imageView.setVisibility(ImageView.VISIBLE);
    				} else {
    					viewHolder.imageView.setVisibility(ImageView.GONE);
    				}
    			}
    			if (viewHolder.date!=null) { viewHolder.date.setText(secoundsToTimeAgo(((new Date()).getTime()-cacheItem.date.getTime())/1000));}
    	    	if (viewHolder.sender!=null) { viewHolder.sender.setText(cacheItem.sender); }
    	    	if (viewHolder.text!=null) { viewHolder.text.setText(cacheItem.text); /*viewHolder.text.setMovementMethod(LinkMovementMethod.getInstance());*/}
    			if (viewHolder.senderImage!=null) {
    				if (cacheItem.senderImageBmp!=null) {
    					viewHolder.senderImage.setImageBitmap(cacheItem.senderImageBmp);
    					viewHolder.senderImage.setVisibility(ImageView.VISIBLE);		                				
    				} else {
    					viewHolder.senderImage.setVisibility(ImageView.GONE);
    				}
    			}
        	} else {
        		createCacheItem(position, true);
        	}
        }

		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
			InfoPoint infopoint=speindData.infopoints.get(getInfopointPosition(position));
			if (infopoint!=null) {
				SpeindAPI.InfoPointData data=infopoint.getData(speindData.currentProfile);
	        	if (data!=null) {
	        		//if (!data.postArticle.isEmpty()) {
	        			articleWnd = new ArticleWindow(Speind.this, infopoint, speindData.currentProfile);
	        			articleWnd.setCancelable(false);
                        articleWnd.setOnOnDestroyListener(new ArticleWindow.OnDestroyViewListener() {
                            @Override
                            public void onDestroyView() {
                                articleWnd=null;
                            }
                        });
	        			FragmentManager fragmentManager = getSupportFragmentManager();
	        	    	FragmentTransaction transaction = fragmentManager.beginTransaction();
	        	        transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
	        	        transaction.setCustomAnimations(R.anim.slide_in_right, android.R.anim.slide_out_right);
	        	        transaction.add(R.id.main_wrap, articleWnd);
	        	        transaction.commit();
	        	        //supportInvalidateOptionsMenu();
	        		//}
	        	}
			}
		}
    } 
    	
    private class SpeindPagerAdapter extends PagerAdapter implements ViewPager.OnPageChangeListener  {
    	
    	private class PlayerViewHolder {
        	public TextView artistv = null;
        	public TextView albumv = null;
        	public TextView titlev = null;
        	public TextView full_lengthv = null;
        	public TextView current_timev = null;
        	public ProgressBar play_progress = null;
        	
        	PlayerViewHolder(View view) {
            	artistv=(TextView) view.findViewById(R.id.artist);
            	albumv=(TextView) view.findViewById(R.id.album);
            	titlev=(TextView) view.findViewById(R.id.title);
            	full_lengthv=(TextView) view.findViewById(R.id.full_length);
            	current_timev=(TextView) view.findViewById(R.id.current_time);
            	play_progress=(ProgressBar) view.findViewById(R.id.play_progress);
        	}
        	
    	}

    	private class ReaderViewHolder {
        	public TextView date = null;
        	public TextView sender = null;
        	public TextView text = null;
        	public ImageView senderImage = null;
        	
        	ReaderViewHolder(View view) {
            	date=(TextView) view.findViewById(R.id.postDate);
            	sender=(TextView) view.findViewById(R.id.postSender);
            	text=(TextView) view.findViewById(R.id.postText);
            	senderImage=(ImageView) view.findViewById(R.id.senderImage);
        	}
    	}

    	public class SpeindPagerAdapterObject {
    		public View view = null;
    		public String id = "";
    		public LinearLayout message = null;
    		public ImageView imageView = null;
    		public Object viewHolder = null;
    	}
    	
    	public ArrayList<SpeindPagerAdapterObject> objects = new ArrayList<SpeindPagerAdapterObject>();  
    	public SparseArray<SpeindPagerAdapterObject> objectsByPosition = new SparseArray<SpeindPagerAdapterObject>(); 

        @Override
        public void onPageSelected (int position){
        	//log("onPageSelected: "+position);
    		//if (position!=currentMP3position) {
        	//	if (position>currentMP3position) position=position-1;
        	//	lastSettedInfopoint=position;
    		//	SpeindAPI.playInfoPoint(Speind.this, position);
    		//} else {
    		//	lastSettedInfopoint=position;
    		//	SpeindAPI.skipNews(Speind.this);
    		//}
        	
        }
        @Override
        public void onPageScrolled (int position, float positionOffset, int positionOffsetPixels){}
        @Override
        public void onPageScrollStateChanged (int state) {
        	if (state==ViewPager.SCROLL_STATE_IDLE) {
        		if (pager!=null) {
        			int position=pager.getCurrentItem();
            		if (position!=currentMP3position) {
                		if (position>currentMP3position) position=position-1;
                		//lastSettedInfopoint=position;
            			SpeindAPI.playInfoPoint(Speind.this, speindData.service_package, position);
            		} else {
            			//lastSettedInfopoint=position;
            			SpeindAPI.skipNews(Speind.this, speindData.service_package);
            		}        		
        		}
        		slidingState = false;
        		onSlidingStateIdle();
        	} else {
        		slidingState = true;
        	}
        }
        
        @Override
        public Object instantiateItem(ViewGroup container, int position){
        	log("SpeindPagerAdapter.instantiateItem: "+position);
        	SpeindPagerAdapterObject object = new SpeindPagerAdapterObject();
        	objects.add(object);
        	objectsByPosition.put(position, object);
        	
        	boolean isPlayer = (position==currentMP3position); 
        	object.id = isPlayer ? "player" : "infopoint"; 
        	object.view = Speind.this.getLayoutInflater().inflate(R.layout.speind_pager_item, container, false);
        	
        	object.message = (LinearLayout) object.view.findViewById(R.id.Message);
        	if (object.message!=null) object.message.addView(isPlayer ? getLayoutInflater().inflate(R.layout.speind_message_player, null) : getLayoutInflater().inflate(R.layout.speind_message_infopoint, null));
        	object.viewHolder = (isPlayer ? new PlayerViewHolder(object.message) : new ReaderViewHolder(object.message));
        	
        	object.imageView = (ImageView) object.view.findViewById(R.id.image);
        	if (object.imageView!=null) object.imageView.setLayoutParams(new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, pagerImageH));
        	
        	// TODO current draw sunc
        	updatePagerViewAtPosition(position, true);
        	
        	container.addView(object.view);
            return object;
        }
        
        @Override
        public void destroyItem(ViewGroup container, int position, Object object){
        	log("SpeindPagerAdapter.destroyItem: "+position);
            container.removeView(((SpeindPagerAdapterObject)object).view);
            objects.remove((SpeindPagerAdapterObject)object);
            objectsByPosition.remove(position);
        }

        @Override
        public int getCount(){
        	return pagesCount;
        }

        @Override
        public boolean isViewFromObject(View view, Object object){
            return view == (((SpeindPagerAdapterObject)object).view);
        }      
        
        public void updateCachedTimeAgo() {
        	for (int index=0; index<objectsByPosition.size();index++) {
        		int position = objectsByPosition.keyAt(index);
    	    	SpeindPagerAdapter.SpeindPagerAdapterObject object = pagerAdapter.objectsByPosition.get(position);
    	    	if (object==null) continue;
    	    	if (object.id.equals("player")) continue;
    	    	int ipp = (position>currentMP3position) ? position-1 : position;
    	    	if (ipp<speindData.infopoints.size()) {
    	    		SpeindAPI.InfoPoint infopoint=speindData.infopoints.get(ipp);
        	    	if (((ReaderViewHolder)(object.viewHolder)).date!=null) ((ReaderViewHolder)(object.viewHolder)).date.setText(secoundsToTimeAgo(((new Date()).getTime()-infopoint.postTime.getTime())/1000)); 
    	    	}
        	}
        }
        
        private void updatePagerViewAtPosition(final int position, final boolean full, final boolean async) {
        	Runnable func = new Runnable(){
    			@Override
    			public void run() {
    		    	log("updatePagerViewAtPosition: "+position+" "+full);
    		    	final SpeindPagerAdapter.SpeindPagerAdapterObject object = pagerAdapter.objectsByPosition.get(position);
    		    	if (object==null||object.id.equals("player")) return;
    		    	{
    		    		int ipp = (position>currentMP3position) ? position-1 : position;
    		    		if (ipp<speindData.infopoints.size()) {                	
    	            		Spanned textText=Html.fromHtml("");	
    		    			SpeindAPI.InfoPoint infopoint=speindData.infopoints.get(ipp);		    				
    			    		SpeindAPI.InfoPointData data=infopoint.getData(speindData.currentProfile);
    	                	if (data!=null) {
            	        		String dt=data.postOriginText;
            	        		if ((full||speindData.speindConfig.read_full_article)&&infopoint.articleExists) {
            	        			dt=data.postArticle.replace(data.postTitle, "");
            	        		}
            	        		if (!infopoint.titleExists)
            	        			textText=Html.fromHtml(dt);
            	        		else
            	        			textText=Html.fromHtml("<b>"+data.postTitle+"</b>"+"<br>"+dt);
    	                	}
    	                	
    	            		final Spanned textTextF=textText;
    	                	final Runnable func1 = new Runnable(){
    							@Override
    							public void run() {
    								if (object.id.equals("player")) return;
    		        	        	if (((ReaderViewHolder)(object.viewHolder)).text!=null) { ((ReaderViewHolder)(object.viewHolder)).text.setText(textTextF); ((ReaderViewHolder)(object.viewHolder)).text.setMovementMethod(LinkMovementMethod.getInstance());}
    							}			    				
    		    			};
    	                	
    			    		if (async) {
    			    			handler.post(func1);
    			    		} else {
    				    		func1.run();
    			    		}
    		    		}
    		    	}
    			}
        	};
        	if (async) {
        		if (cacheWorker.mHandler!=null) cacheWorker.mHandler.post(func);
        	} else {
        		func.run();
        	}
        }
        
        private void updatePagerViewAtPosition(final int position, final boolean async) {
        	log("updatePagerViewAtPosition: "+position);
        	SpeindPagerAdapter.SpeindPagerAdapterObject obj = pagerAdapter.objectsByPosition.get(position);
        	if (obj==null) return;
        	boolean isPlayer = (position==currentMP3position);
        	if (obj.id.equals("player")&&!isPlayer) {
        		obj.id="infopoint";
        		obj.message.removeAllViews();
        		obj.message.addView(getLayoutInflater().inflate(R.layout.speind_message_infopoint, null));
        		obj.viewHolder = new ReaderViewHolder(obj.message);
        	} else if (obj.id.equals("infopoint")&&isPlayer) {
        		obj.id="player";
        		obj.message.removeAllViews();
        		obj.message.addView(getLayoutInflater().inflate(R.layout.speind_message_player, null));
        		obj.viewHolder = new PlayerViewHolder(obj.message);
        	}
        	Runnable func = new Runnable(){
    			@Override
    			public void run() {
    		    	log("updatePagerViewAtPosition: "+position);
    		    	final boolean isPlayer = (position==currentMP3position);
    		    	SpeindPagerAdapter.SpeindPagerAdapterObject object = pagerAdapter.objectsByPosition.get(position);

    		    	if (object==null||object.id.equals("player")&&!isPlayer||object.id.equals("infopoint")&&isPlayer) {
    		    		return;
    		    	}
    		    	
    		    	if (isPlayer) {
    		    		
    		    		Bitmap MP3CoverBMP=null;
        	    		String artist=null;
        	    		String album=null;
        	    		String title=null;
        	    		String length=null;
        	    		
        	    		if (mediaData.size()==0) {
        	    			title = getString(R.string.no_music_found);
        	    			artist = "";
        	    			album = "";
        	    		} else {
    	    	    		try {
    	        	    		MediaMetadataRetriever mmr = new MediaMetadataRetriever();
    	    	    			mmr.setDataSource(playerFileName);
    	    	    			length=mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
    	    	    			ArrayList<String> data=mediaData.get(playerFileName);
    	    	    			if (data!=null) {
    	        	    			title=data.get(0);
    	        	    			artist=data.get(1);
    	        	    			album=data.get(2);
    	    	    			}
    	    	    			byte[] img=mmr.getEmbeddedPicture();
    	    	    			if (img!=null) {
    	    	    				
    	    	    				MP3CoverBMP = SpeindAPI.GetScaledBitmap(img, 0,img.length, 512, 512);
    	    	    			}
    	    	    		} catch (Exception e) { }
        	    		}
        	    		if (artist==null) artist = "<Unknown>";
        	    		if (album==null) album = "<Unknown>";
        	    		if (title==null) title = "<Unknown>"; 

    	    			final Bitmap MP3CoverBMPF=MP3CoverBMP;
        	    		final String artistF=artist;
        	    		final String albumF=album;
        	    		final String titleF=title;
        	    		final String lengthF=length;
        	    		final Runnable func1 = new Runnable(){
    						@Override
    						public void run() {    		    	
    					    	SpeindPagerAdapter.SpeindPagerAdapterObject object = pagerAdapter.objectsByPosition.get(position);

    					    	if (object==null||object.id.equals("player")&&!isPlayer||object.id.equals("infopoint")&&isPlayer) {
    					    		return;
    					    	}
    							
    		    	    		if (((PlayerViewHolder)(object.viewHolder)).artistv!=null) { ((PlayerViewHolder)(object.viewHolder)).artistv.setText(artistF); }
    		    	        	if (((PlayerViewHolder)(object.viewHolder)).albumv!=null) { ((PlayerViewHolder)(object.viewHolder)).albumv.setText(albumF); }						
    		    	        	if (((PlayerViewHolder)(object.viewHolder)).titlev!=null) { ((PlayerViewHolder)(object.viewHolder)).titlev.setText(titleF); }    	        	
    		    	    		if (((PlayerViewHolder)(object.viewHolder)).play_progress!=null) ((PlayerViewHolder)(object.viewHolder)).play_progress.setProgress(0); 
    		    	            if (((PlayerViewHolder)(object.viewHolder)).full_lengthv!=null&&lengthF!=null) { ((PlayerViewHolder)(object.viewHolder)).full_lengthv.setText(secoundsToTimeString(Integer.parseInt(lengthF)/1000)); }
    		    	            if (((PlayerViewHolder)(object.viewHolder)).current_timev!=null) { ((PlayerViewHolder)(object.viewHolder)).current_timev.setText("00:00"); }	
    		    	            if (object.imageView!=null) {
    			    	            if (MP3CoverBMPF==null) {
    			    	            	object.imageView.setImageResource(R.drawable.placeholder);
    			        			} else {
    			        				object.imageView.setImageBitmap(MP3CoverBMPF);
    			        			}
    		    	            }
    						}	
        	    		};    	    		
        	    		if (async) {
            	    		handler.post(func1);
        	    		} else {    	    		
    	    	    		func1.run();
        	    		}
    		    	} else {
    		    		int ipp = (position>currentMP3position) ? position-1 : position;
    		    		if (ipp<speindData.infopoints.size()) {
    		    				                	
    	            		String dateText="";
    	            		String senderText="";
    	            		Spanned textText=Html.fromHtml("");
    	            		Bitmap senderImageBmp=null;
    		    			
    		    			SpeindAPI.InfoPoint infopoint=speindData.infopoints.get(ipp);		    				
    		    			Bitmap bmp=infopoint.getPostBmp(speindData.currentProfile, pagerImageW, pagerImageH);
    			    		if (bmp==null) {			
    			    			bmp=SpeindAPI.GetScaledResourceBitmap(Speind.this, R.drawable.placeholder, pagerImageW, pagerImageH);
    			    		}

							SpeindAPI.InfoPointData data=infopoint.getData(speindData.currentProfile);
							if (data.pluginBmpPath.equals("")) {
								//for (SpeindAPI.DataFeedSettingsInfo pluginData : speindData.dataFeedsettingsInfos) {
									//if (infopoint.processingPlugin.equalsIgnoreCase(pluginData.packageName)) {
									//	Bitmap result = Bitmap.createBitmap(pagerImageW, pagerImageH, Bitmap.Config.ARGB_8888);
									//	Canvas canvas = new Canvas(result);
									//	canvas.drawBitmap(bmp, (pagerImageW-bmp.getScaledWidth(canvas))/2, (pagerImageH-bmp.getScaledHeight(canvas))/2, null);
									//	Bitmap pluginBmp=SpeindAPI.GetScaledBitmap(pluginData.bmpPath, (int)pagerImageW/8, (int)pagerImageH/8);
									//	int xpos=pagerImageW-(int)(10*densityDpi/160f)-pluginBmp.getScaledWidth(canvas);
									//	int ypos=pagerImageH-(int)(10*densityDpi/160f)-pluginBmp.getScaledHeight(canvas);
									//	Paint paint = new Paint();
									//	paint.setAlpha(90);
									//	canvas.drawBitmap(pluginBmp, xpos, ypos, paint);
									//	bmp=result;
									//	break;
									//}
								//}
							} else {
								Bitmap result = Bitmap.createBitmap(pagerImageW, pagerImageH, Bitmap.Config.ARGB_8888);
								Canvas canvas = new Canvas(result);
								canvas.drawBitmap(bmp, (pagerImageW-bmp.getScaledWidth(canvas))/2, (pagerImageH-bmp.getScaledHeight(canvas))/2, null);
								Bitmap pluginBmp=SpeindAPI.GetScaledBitmap(data.pluginBmpPath, (int)pagerImageW/8, (int)pagerImageH/8);
								int xpos=pagerImageW-(int)(10*densityDpi/160f)-pluginBmp.getScaledWidth(canvas);
								int ypos=pagerImageH-(int)(10*densityDpi/160f)-pluginBmp.getScaledHeight(canvas);
								Paint paint = new Paint();
								paint.setAlpha(90);
								canvas.drawBitmap(pluginBmp, xpos, ypos, paint);
								bmp=result;
							}

    			    		dateText=secoundsToTimeAgo(((new Date()).getTime()-infopoint.postTime.getTime())/1000); 
    	                	if (data!=null) {
    	            			senderText=data.postSender;
            	        		String dt=data.postOriginText;
            	        		if (speindData.speindConfig.read_full_article&&infopoint.articleExists) {
            	        			dt=data.postArticle.replace(data.postTitle, "");
            	        		}
            	        		if (!infopoint.titleExists)
            	        			textText=Html.fromHtml(dt);
            	        		else
            	        			textText=Html.fromHtml("<b>"+data.postTitle+"</b>"+"<br>"+dt);
            	        		
                				Bitmap senderBmp=infopoint.getSenderBmp(speindData.currentProfile);
                				if (senderBmp==null) {
									if (data.pluginBmpPath.equals("")) {
										//for (SpeindAPI.DataFeedSettingsInfo pluginData : speindData.dataFeedsettingsInfos) {
										//	if (infopoint.processingPlugin.equalsIgnoreCase(pluginData.packageName)) {
										//		senderBmp=pluginData.getBmp();
										//	}
										//}
									} else {
										senderBmp = SpeindAPI.GetScaledBitmap(data.pluginBmpPath, 96, 96);
									}
                				}
                				if (senderBmp!=null) {
                					int l = Math.min(senderBmp.getWidth(), senderBmp.getHeight());
                					Bitmap rounder = Bitmap.createBitmap(l,l,Bitmap.Config.ARGB_8888);
                					Canvas canvas = new Canvas(rounder);
                					Paint xferPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
                					xferPaint.setColor(Color.RED);
                					canvas.drawCircle(l/2, l/2, (l-1)/2, xferPaint); //drawRoundRect(new RectF(0,0,w,h), 20.0f, 20.0f, xferPaint);
                					xferPaint.setXfermode(new PorterDuffXfermode(android.graphics.PorterDuff.Mode.DST_IN));
                					
                					Bitmap result = Bitmap.createBitmap(l, l, Bitmap.Config.ARGB_8888);
                					Canvas resultCanvas = new Canvas(result);
                					resultCanvas.drawBitmap(senderBmp, 0, 0, null);
                					resultCanvas.drawBitmap(rounder, 0, 0, xferPaint);
                					
                					senderImageBmp=result;
                				}
    	                	}
    	                	
    		    			final Bitmap bmpF=bmp;
    		    			final String dateTextF=dateText;
    	            		final String senderTextF=senderText;
    	            		final Spanned textTextF=textText;
    	            		final Bitmap senderImageF=senderImageBmp;
    	                	final Runnable func1 = new Runnable(){
    							@Override
    							public void run() {
    						    	SpeindPagerAdapter.SpeindPagerAdapterObject object = pagerAdapter.objectsByPosition.get(position);

    						    	if (object==null||object.id.equals("player")&&!isPlayer||object.id.equals("infopoint")&&isPlayer) {
    						    		return;
    						    	}
    								
    					    		if (object.imageView!=null) object.imageView.setImageBitmap(bmpF);
    					    		if (((ReaderViewHolder)(object.viewHolder)).date!=null) { ((ReaderViewHolder)(object.viewHolder)).date.setText(dateTextF);}
    		        	        	if (((ReaderViewHolder)(object.viewHolder)).sender!=null) { ((ReaderViewHolder)(object.viewHolder)).sender.setText(senderTextF); }
    		        	        	if (((ReaderViewHolder)(object.viewHolder)).text!=null) { ((ReaderViewHolder)(object.viewHolder)).text.setText(textTextF); ((ReaderViewHolder)(object.viewHolder)).text.setMovementMethod(LinkMovementMethod.getInstance());}
    		                		if (((ReaderViewHolder)(object.viewHolder)).senderImage!=null) {
    		                			if (senderImageF!=null) {
    		                				((ReaderViewHolder)(object.viewHolder)).senderImage.setImageBitmap(senderImageF);
    		                				((ReaderViewHolder)(object.viewHolder)).senderImage.setVisibility(ImageView.VISIBLE);		                				
    		                			} else {
    		                				((ReaderViewHolder)(object.viewHolder)).senderImage.setVisibility(ImageView.GONE);
    		                			}
    		                		}
    							}			    				
    		    			};
    	                	
    			    		if (async) {
    			    			handler.post(func1);
    			    		} else {
    				    		func1.run();
    			    		}
    		    		}
    		    	}
    			}
        	};
        	if (async) {
        		if (cacheWorker.mHandler!=null) cacheWorker.mHandler.post(func);
        	} else {
        		func.run();
        	}
        }

    }
        
	private static void log(String s) {
		//long max = Runtime.getRuntime().maxMemory(); //the maximum memory the app can use
		//long heapSize = Runtime.getRuntime().totalMemory(); //current heap size
		//long heapRemaining = Runtime.getRuntime().freeMemory(); //amount available in heap
		//long nativeUsage = Debug.getNativeHeapAllocatedSize(); //is this right? I only want to account for native memory that my app is being "charged" for.  Is this the proper way to account for that?
		//long remaining = max - (heapSize - heapRemaining + nativeUsage);

    	//Log.e("[---SpeindUI---]", Thread.currentThread().getName()+" [Memory available: "+remaining/(1024*1024)+"]: "+s);
    }
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
        log("onCreate");

		dataFeedWorker.setThreadName("SUIDFworker");
    	dataFeedWorker.start();
		cacheWorker.setThreadName("SUICworker");
    	cacheWorker.start();
    	
		super.onCreate(savedInstanceState);
    	setContentView(R.layout.speind_splash);
    	
    	getWindow().setFlags(WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED, WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED);
    	
    	SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
    	mode = settings.getInt("last_used_mode", MODE_PLAYER);
    	
    	pagerAdapter = new SpeindPagerAdapter();
    	listAdapter = new SpeindListAdapter();
    	
    	String[] projection = {MediaStore.Audio.Media._ID, MediaStore.Audio.Media.DATA, MediaStore.Audio.Media.TITLE, MediaStore.Audio.Media.ARTIST, MediaStore.Audio.Media.ALBUM};  
    	String selection = MediaStore.Audio.Media.IS_MUSIC + " != 0 ";
    	Cursor musicListSDCardCursor = myquery(this, MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, projection, selection , null, null,0);
    	Cursor musicListInternalMemoryCursor = myquery(this, MediaStore.Audio.Media.INTERNAL_CONTENT_URI, projection, selection , null, null,0);
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
    	        
	}
	 	
	@Override
    protected void onPause() {
        super.onPause();
        handler.removeCallbacks(refreshPostTimeAgo);
        isPaused=true;
        log("set play music only FALSE");
        SpeindAPI.setPlayerOnlyMode(this, speindData.service_package, false);
    }
    
	@Override
    protected void onResume() {
		super.onResume();
		isPaused=false;
		if (mode==MODE_LIST) {       
		    if (list!=null) {
		    	list.setSelection(itemsCount-1-speindData.currentInfoPoint);
		    }
		} else {
			if (isPlayingMP3&&pager!=null) {
				pager.setCurrentItem(currentMP3position, false);
			} else if (pager!=null){
				pager.setCurrentItem(speindData.currentInfoPoint, false);
			}
		}
		if (mode==MODE_LIST) {
	        log("set play music only TRUE");
	        SpeindAPI.setPlayerOnlyMode(this, speindData.service_package, true);
		}
        handler.postDelayed(refreshPostTimeAgo, 10000);
	}
	
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
    	if (main_layout_visible) {
    		if (articleWnd==null) {
	    		getMenuInflater().inflate(R.menu.menu, menu);    	
		        if (mode == MODE_PLAYER) {
		            menu.getItem(0).setIcon(R.drawable.btn_list_view);
		        } else {
		            menu.getItem(0).setIcon(R.drawable.btn_player_view);
		        }
    		}
        } else {
        	getMenuInflater().inflate(R.menu.menu_splash, menu);
        }
        return super.onCreateOptionsMenu(menu);
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
        case R.id.view_mode:
        	if (mode == MODE_LIST) {
                setMode(MODE_PLAYER);
            } else {
                setMode(MODE_LIST);
            }
            supportInvalidateOptionsMenu();
        	return true;
        case R.id.quit:
        	quit();
        	return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }
    	
    @Override
    public void onBackPressed() {
        if (articleWnd != null) {
        	articleWnd.dismiss();
        	articleWnd=null;
        } else {
            super.onBackPressed();
        }
    }
    
	@Override
	public void onStateChanged(int oldState, int newState) {
		log("onStateChanged");
		if (newState==SpeindAPI.SPEIND_STATE_NEED_DOWNLOAD) {
			showNeedDownloadDialog();
		} else if (newState==SpeindAPI.SPEIND_STATE_DOWNLOADING) {
			showDownloadingProgress();
		} else if (newState==SpeindAPI.SPEIND_STATE_UNPUCKING) {
			showUnpacking();
		} else if (newState==SpeindAPI.SPEIND_STATE_NEED_PROFILE) {
			// Nothing to do wait onProfiles
		} else if (newState==SpeindAPI.SPEIND_STATE_PREPARING) {
			// Nothing to do
		} else {
			// Nothing to do
		}
	}

    @Override
    public void onError(int code, String err) {
    	log("onError");
    	// Out Error message
		//stopService(new Intent(getApplicationContext(), SpeindService.class));
		//Speind.this.finish();			
    }

    private void showMessage(String msg) {
        Toast toast = Toast.makeText(this, msg, Toast.LENGTH_LONG);
        toast.setGravity(Gravity.CENTER_VERTICAL | Gravity.CENTER_HORIZONTAL, 0, 0);
        TextView v = (TextView) toast.getView().findViewById(android.R.id.message);
        if( v != null) v.setGravity(Gravity.CENTER);
        toast.show();
    }

    @Override
    public void onInfoMessage(String message) {
        showMessage(message);
    }

    public void showNeedDownloadDialog() {
    	log("showNeedDownloadDialog");
    	final DialogWindow ndw=new DialogWindow(this, R.id.logo, R.layout.speind_need_download_dialog);

    	Button download_tts=(Button) ndw.findViewById(R.id.download_tts);
    	Button open_store=(Button) ndw.findViewById(R.id.open_store);
    	
    	if (download_tts!=null) {
    		download_tts.setOnClickListener(new Button.OnClickListener() {
				@Override
				public void onClick(View arg0) {
					Intent marketIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=com.google.android.tts"));
					marketIntent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY | Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
					startActivity(marketIntent);
					ndw.hide();
					Speind.this.quit();
				} 
			});
    	}
    	if (open_store!=null) {
    		open_store.setOnClickListener(new Button.OnClickListener() {
				@Override
				public void onClick(View arg0) {
					SpeindAPI.openVoicesSettingsRequest(Speind.this, speindData.service_package);
					ndw.hide();
				} 
			});
    	}

    	ndw.show();  		
     }

    public void showDownloadingProgress() {
    	log("showDownloadingProgress");
    	ProgressBar pr = (ProgressBar)findViewById(R.id.progressBar1);
    	pr.setProgress(0);
    	TextView pr1 = (TextView)findViewById(R.id.textView2);
    	pr1.setText(R.string.downloading);
    	LinearLayout ll = (LinearLayout)findViewById(R.id.linearLayout1);
		ll.setVisibility(LinearLayout.VISIBLE);
    }

    private void showUnpacking() {
    	log("showUnpacking");
    	LinearLayout ll = (LinearLayout)findViewById(R.id.linearLayout1);
		ll.setVisibility(LinearLayout.INVISIBLE);	
    	ll = (LinearLayout)findViewById(R.id.linearLayout2);
		ll.setVisibility(LinearLayout.VISIBLE);	
	}

    @Override
    public void onDownloadProgress(double progress, int fileNumber, int fileTotal) {
    	log("onDownloadProgress");
    	ProgressBar pr = (ProgressBar)findViewById(R.id.progressBar1);
    	pr.setProgress((int)(progress*2));
    	TextView pr1 = (TextView)findViewById(R.id.textView2);
    	pr1.setText(getString(R.string.downloading) + " " + progress + "%");
    }
    
    @Override
    public void onProfiles(ArrayList<String> profiles) {
    	log("onProfiles");
    	// show select profile window

    	// on select profile
        String profile = "profile";
        if (!getPackageName().equals("com.maple.speind")) profile = getPackageName();
    	SpeindAPI.setProfile(Speind.this, speindData.service_package, profile, "password");
    }
        
    @Override
    public void onStartPlay() {
    	log("onStartPlay");
    	if (!slidingState) {
	    	Speind.this.buttonsSwitch(STATE_STOP_PREV);
		    handler.postDelayed(setStateStop, 5000);
    	} else {
    		runnableQueue.add(new Runnable(){
				@Override
				public void run() {
					onStartPlay();
				}
    		});
    	}
    }
    
    @Override
    public void onStopPlay() {
    	log("onStopPlay");
    	if (!slidingState) {
        	//if (mode==MODE_PLAYER) {
            	Speind.this.buttonsSwitch(STATE_PLAY);
        		//final ProgressBar play_progress=(ProgressBar) findViewById(R.id.play_progress);
            	//final TextView current_timev=(TextView) findViewById(R.id.current_time);
        		//if (play_progress!=null) play_progress.setProgress(0); 
                //if (current_timev!=null) { current_timev.setText("00:00"); }
        	//}
    	} else {
    		runnableQueue.add(new Runnable(){
				@Override
				public void run() {
					onStopPlay();
				}
    		});
    	}    	
    }
    
    private String secoundsToTimeString(int sec){
    	String result="";
    	int h=sec/3600;
    	int m=(sec-3600*h)/60;
    	int s=sec-3600*h-60*m;
    	if (h>0) {
    		result+=""+((h<10)? "0"+h : h)+":";
        	result+=""+((m<10)? "0"+m : m);
    	} else {
	    	result+=""+((m<10)? "0"+m : m)+":";
	    	result+=""+((s<10)? "0"+s : s);
    	}
    	return result;
    }
    
    public static String secoundsToTimeAgo(long l) {
    	String result="";
    	long d=l/(24*3600);
    	long h=(l-24*3600*d)/3600;
    	long m=(l-24*3600*d-3600*h)/60; 	
    	if (d>0) result+=""+d+"d ";
    	if (h>0||d>0) result+=""+h+"h ";
    	result+=""+m+"m ago";
    	return result;
    }
    
	@Override
	public void onPlayMP3Info(final String fileName, final String nextFileName) {
		log("onPlayMP3Info");
    	if (!slidingState) {
    		//handler.removeCallbacks(refreshPostTimeAgo);
        	isPlayingMP3=true;
        	nextPlayerFileName=nextFileName;
        	
        	int newCurrentMP3position=(pagesCount==1) ? 0 : speindData.currentInfoPoint+1;
        	
    		if (newCurrentMP3position!=currentMP3position) {
    			playerFileName=fileName;
    			int startPos=0;
    			int endPos=0;
    			if (newCurrentMP3position>currentMP3position) {
            		log("currentMP3position increase: "+currentMP3position+" "+newCurrentMP3position);
            		startPos = currentMP3position;
            		endPos = newCurrentMP3position;
    			} else {
            		log("currentMP3position decrease: "+currentMP3position+" "+newCurrentMP3position);
            		startPos = newCurrentMP3position;
            		endPos = currentMP3position;
    			}
    			currentMP3position = newCurrentMP3position;
    			// Refresh pager
    			{
        			startPos=Math.max(startPos, currentMP3position-2);
        			endPos=Math.min(endPos, currentMP3position+2);
        			for (int position=startPos; position<=endPos;position++) {
        				pagerAdapter.updatePagerViewAtPosition(position, true);
        			}
        			
            		if (mode==MODE_PLAYER&&pager!=null&&!isPaused) {
        				if (pager.getCurrentItem()!=currentMP3position) {
        					pager.setCurrentItem(currentMP3position, true);
        				}
        			}    				
    			}
    		} else {
    			// Refresh pager
    			{
                	if (playerFileName!=fileName) {
                		playerFileName=fileName;
                		log("currentMP3position not changed refreshing mp3 info");

                		if (mode==MODE_PLAYER&&pager!=null&&!isPaused) {
                			//if (lastSettedInfopoint==-1||lastSettedInfopoint==currentMP3position) {
    						//	lastSettedInfopoint=-1;
                				if (pager.getCurrentItem()!=currentMP3position) {
                					pager.setCurrentItem(currentMP3position, true);
                				}
    						//}                			
            			}
                		pagerAdapter.updatePagerViewAtPosition(currentMP3position, true);
                	} else {
                		log("info has no effect");
                	}
    			}
    		}
    	} else {
    		runnableQueue.add(new Runnable(){
				@Override
				public void run() {
					//onPlayMP3Info(fileName);
				}
    		});
    	}    			
    }
		
    @Override
    public void onPlayTextInfo(final int infopointPos, final boolean full) {
    	log("onPlayTextInfo: "+infopointPos+" "+full);
    	if (!slidingState) {
        	int newCurrentMP3position = pagesCount-1;
			isPlayingMP3=false;
			if (playerFileName!=nextPlayerFileName) {
	        	playerFileName=nextPlayerFileName;
	        	if (newCurrentMP3position==currentMP3position) {
	        		pagerAdapter.updatePagerViewAtPosition(currentMP3position, true);
	        	}
			}
        	if (newCurrentMP3position!=currentMP3position) {
        		log("currentMP3position increase");
        		int startPos = currentMP3position;
        		int endPos = newCurrentMP3position;
        		currentMP3position = newCurrentMP3position;
        		
        		// Refresh pager
        		{
            		startPos=Math.max(startPos, infopointPos-2);
        			endPos=Math.min(endPos, infopointPos+2);
            		for (int position=startPos; position<=endPos;position++) {
        				if (position==infopointPos) pagerAdapter.updatePagerViewAtPosition(position, false);
        				pagerAdapter.updatePagerViewAtPosition(position, true);
        			}
            		
            		if (mode==MODE_PLAYER&&pager!=null&&!isPaused) {
        				if (pager.getCurrentItem()!=infopointPos) {
        					pager.setCurrentItem(infopointPos, false);
        				}
        			}
        		}
        	} else {
        		// Refresh pager
        		{
	        		if (mode==MODE_PLAYER&&pager!=null&&!isPaused) {
						//if (lastSettedInfopoint==-1||lastSettedInfopoint==infopointPos) {
						//	lastSettedInfopoint=-1;
		    				if (pager.getCurrentItem()!=infopointPos) {
		    					log("scroll to current infopoint");
		    					pager.setCurrentItem(infopointPos, true);
			    			}
						//}
    				}
        		}
        	}
        	handler.postDelayed(new Runnable(){
				@Override
				public void run() {
					pagerAdapter.updatePagerViewAtPosition(infopointPos, full, true);
				}       		
        	}, 500);
    	} else {
    		runnableQueue.add(new Runnable(){
				@Override
				public void run() {
					//onPlayTextInfo(infopointPos, full);
				}
    		});
    	}    	    	
    }    

    @Override
    public void onInfopointsChanged(final int startPosition) {    
    	log("onInfopointsChanged");
    	if (!slidingState) {
			pagesCount=speindData.infopoints.size()+1;
			pagerAdapter.notifyDataSetChanged();
			// Refresh pager
			{
				int infopointPos=startPosition;
				int curPos = speindData.currentInfoPoint;
				if (isPlayingMP3) {
					curPos=currentMP3position;
					if (infopointPos>currentMP3position) infopointPos+=1; 
				} else {
					if (infopointPos==currentMP3position) {
						currentMP3position=pagesCount;
					}
				}
        		int startPos=Math.max(Math.max(curPos-2, 0), infopointPos-2);
        		int endPos=Math.min(curPos+2, infopointPos+2);
        		for (int position=startPos; position<=endPos;position++) {
        			pagerAdapter.updatePagerViewAtPosition(position, true);
    			}
			}

            //listAdapter.notifyDataSetChanged();
			// Refresh list
            if ((itemsCount - 1 - speindData.currentInfoPoint)==0) {
                listAdapter.cache.clear();
                itemsCount=speindData.infopoints.size();
                listAdapter.notifyDataSetChanged();
                if (list!=null) {
                    list.setSelection(itemsCount - 1 - speindData.currentInfoPoint);
                }
            } else {
        		int startPos=Math.max(itemsCount-1-startPosition, listAdapter.startCacheInterval);
        		int endPos=itemsCount-1-startPosition;
        		for (int position=startPos; position<=endPos;position++) {
        			final int pos=position;
        			handler.post(new Runnable(){
						@Override
						public void run() {
		        			listAdapter.createCacheItem(pos, false);
						}
                    });
    			}
			}
    	} else {
    		runnableQueue.add(new Runnable(){
				@Override
				public void run() {
					onInfopointsChanged(startPosition);
				}
    		});
    	}    		
    }	    
        
    public void onSwipeTop() {
    	if (!isPlayingMP3&&speindData.infopoints!=null&&speindData.infopoints.size()>0&&speindData.infopoints.size()>speindData.currentInfoPoint) {
            SpeindAPI.InfoPoint infopoint = speindData.infopoints.get(speindData.currentInfoPoint);
            SpeindAPI.InfoPointData data = infopoint.getData(speindData.currentProfile);
            if (data!=null) {
                if (speindData.speindConfig.post_settings.post_plugins_data.size() == 0) {
                    Intent intent = new Intent(Intent.ACTION_SEND);
                    intent.setType("text/plain");
                    intent.addFlags(0x00080000/*Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET*/);
                    intent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.post_by_speind));
                    intent.putExtra(Intent.EXTRA_TEXT, data.postURL);
                    intent.putExtra(Intent.EXTRA_HTML_TEXT, getString(R.string.post_by_speind) + "<br>" + data.postURL);
                    startActivity(Intent.createChooser(intent, getString(R.string.share_with)));
                } else {
                    if (speindData.speindConfig.post_settings.ask_before_post) {
                        Intent postingSettingsIntent = new Intent(Speind.this, SpeindPostingSettings.class);
                        SpeindAPI.DataFeedSettingsInfo.putListToIntent(postingSettingsIntent, speindData.dataFeedsettingsInfos);
                        speindData.speindConfig.putToIntent(postingSettingsIntent);
                        infopoint.putToIntent(postingSettingsIntent);
                        startActivity(postingSettingsIntent);
                    } else {
                        SpeindAPI.post(Speind.this, speindData.service_package, infopoint);
                    }
                }
            } else {
                // TODO Error: something wrong
            }
    	}
    }

    public void onSwipeBottom() {
        if (!isPlayingMP3&&speindData.infopoints!=null&&speindData.infopoints.size()>0&&speindData.infopoints.size()>speindData.currentInfoPoint) {
            SpeindAPI.addToPinboard(this, speindData.service_package, speindData.infopoints.get(speindData.currentInfoPoint).id);
        }
    }
    
    public void onTouch() {
    	if (!isPlayingMP3) {
    		SpeindAPI.readCurrentInfopointArticle(Speind.this, speindData.service_package);
    	}
    }
        
    public void quit() {
    	SpeindAPI.stopSpeindService(Speind.this, speindData.service_package);
		Speind.this.finish();			    	
    }
        
    public void buttonsSwitch(int state) {
    	if (buttonsState==state) return;
    	log("buttonsSwitch");
    	handler.removeCallbacks(setStateStop);
    	final Button playStop=(Button) findViewById(R.id.PlayStop);
		final Button prevReplay=(Button) findViewById(R.id.PrevReplay);
		if (state==STATE_PLAY) {
			buttonsState=state;
			if (playStop!=null) playStop.setBackgroundResource(R.drawable.btn_play);
			if (prevReplay!=null) prevReplay.setBackgroundResource(R.drawable.btn_prev);
		} else if (state==STATE_STOP_PREV) {
			buttonsState=state;
			if (playStop!=null) playStop.setBackgroundResource(R.drawable.btn_stop);
			if (prevReplay!=null) prevReplay.setBackgroundResource(R.drawable.btn_prev);
		} else if (state==STATE_STOP) {
			buttonsState=state;
			if (playStop!=null) playStop.setBackgroundResource(R.drawable.btn_stop);
			if (prevReplay!=null) prevReplay.setBackgroundResource(R.drawable.btn_replay);
		}
    }
    
    public void showScreen(int resource) {
    	log("showScreen");
    	if (resource==R.layout.speind_main) {
    		if (!main_layout_visible) {
    			setContentView(resource);
    			handler.postDelayed(refreshPostTimeAgo, 10000);
    		}
        	
    		RelativeLayout player_container = (RelativeLayout) findViewById(R.id.player_container);
    		RelativeLayout list_container = (RelativeLayout) findViewById(R.id.list_container);
    		if (mode==MODE_LIST) {       
    			if (player_container!=null) player_container.setVisibility(View.INVISIBLE);
    			if (list_container!=null) list_container.setVisibility(View.VISIBLE);
    		    if (list!=null) {
    		    	list.setSelection(itemsCount-1-speindData.currentInfoPoint);
    		    }
    		} else {
    			if (list_container!=null) list_container.setVisibility(View.INVISIBLE);
    			if (player_container!=null) player_container.setVisibility(View.VISIBLE);
    			if (isPlayingMP3&&pager!=null) {
    				pager.setCurrentItem(currentMP3position, true);
    			} else if (pager!=null){
    				pager.setCurrentItem(speindData.currentInfoPoint);
    			}
    		}

    		if (main_layout_visible) {
    			return;
    		}
    		
    		Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
            toolbar.setTitle("");
            setSupportActionBar(toolbar);
    		
            final DrawerLayout mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
            ActionBarDrawerToggle mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close) {
                public void onDrawerClosed(View view) {
                    super.onDrawerClosed(view);
                    supportInvalidateOptionsMenu();
                }
                public void onDrawerOpened(View drawerView) {
                    super.onDrawerOpened(drawerView);
                    supportInvalidateOptionsMenu();
                }
            };

            mDrawerLayout.setDrawerListener(mDrawerToggle);
            mDrawerToggle.syncState();

            toolbar.setNavigationIcon(R.drawable.btn_menu);
            
            onDataFeedsListChanged();

            TextView side_menu_logo_title = (TextView) findViewById(R.id.side_menu_logo_title);
            RelativeLayout side_menu_pinboard_wrap=(RelativeLayout) findViewById(R.id.side_menu_pinboard_wrap);
            RelativeLayout side_menu_settings_wrap=(RelativeLayout) findViewById(R.id.side_menu_settings_wrap);
            RelativeLayout side_menu_feedback_wrap=(RelativeLayout) findViewById(R.id.side_menu_feedback_wrap);
            RelativeLayout side_menu_give_5_wrap=(RelativeLayout) findViewById(R.id.side_menu_give_5_wrap);
            RelativeLayout side_menu_quit_wrap=(RelativeLayout) findViewById(R.id.side_menu_quit_wrap);

            if (side_menu_logo_title!=null) {
                PackageInfo pInfo;
                String versionName="";
                try {
                    pInfo = getPackageManager().getPackageInfo(speindData.service_package, 0);
                    versionName=pInfo.versionName;
                } catch (NameNotFoundException e) {
                    e.printStackTrace();
                }
                side_menu_logo_title.setText(getString(R.string.app_name_full)+" v"+versionName);
            }

            if (side_menu_pinboard_wrap!=null) {
				if (SpeindConfig.exclude_pinboard) {
					side_menu_pinboard_wrap.setVisibility(View.GONE);
				} else {
					side_menu_pinboard_wrap.setOnClickListener(new OnClickListener() {
						@Override
						public void onClick(View v) {
							mDrawerLayout.closeDrawers();
							SpeindAPI.openPinboardRequest(Speind.this, speindData.service_package);
						}
					});
				}
            }

            if (side_menu_settings_wrap!=null) {
            	side_menu_settings_wrap.setOnClickListener(new OnClickListener(){
					@Override
					public void onClick(View v) {
						mDrawerLayout.closeDrawers();
						SpeindAPI.openSettingsRequest(Speind.this, speindData.service_package);
					}
            	});
            }

            if (side_menu_feedback_wrap!=null) {
            	side_menu_feedback_wrap.setOnClickListener(new OnClickListener(){
					@Override
					public void onClick(View v) {
						mDrawerLayout.closeDrawers();
						openFeedBack();
					}
            	});
            }

            if (side_menu_give_5_wrap!=null) {
            	side_menu_give_5_wrap.setOnClickListener(new OnClickListener(){
					@Override
					public void onClick(View v) {
						mDrawerLayout.closeDrawers();
                        String appPackageName= getPackageName();
                        Intent marketIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id="+appPackageName));
                        marketIntent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY | Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
                        startActivity(marketIntent);
					}
                	});
            }

            if (side_menu_quit_wrap!=null) {
            	side_menu_quit_wrap.setOnClickListener(new OnClickListener(){
					@Override
					public void onClick(View v) {
						//mDrawerLayout.closeDrawers();
						quit();
					}
            	});
            }
            
    		buttonsSwitch(STATE_PLAY);
    		final Button playStop=(Button) findViewById(R.id.PlayStop);
    		final Button prevReplay=(Button) findViewById(R.id.PrevReplay);
    		final Button next=(Button) findViewById(R.id.Next);
	
			if (playStop!=null) {
				playStop.setOnClickListener(new OnClickListener() {
					public void onClick(View v) {
						if (Speind.this.buttonsState==Speind.STATE_STOP) {
							SpeindAPI.pause(Speind.this, speindData.service_package);
						} else if (Speind.this.buttonsState==Speind.STATE_STOP_PREV) {
							SpeindAPI.pause(Speind.this, speindData.service_package);
						} else if (Speind.this.buttonsState==Speind.STATE_PLAY) {
							SpeindAPI.play(Speind.this, speindData.service_package);
						}
					}					
				});
			}
			if (prevReplay!=null) {
				prevReplay.setOnClickListener(new OnClickListener() {
					public void onClick(View v) {						
						if (Speind.this.buttonsState==Speind.STATE_STOP_PREV) {
							SpeindAPI.prev(Speind.this, speindData.service_package);
						} else if (Speind.this.buttonsState==Speind.STATE_STOP) {
							SpeindAPI.replay(Speind.this, speindData.service_package);
						} else if (Speind.this.buttonsState==Speind.STATE_PLAY) {
							SpeindAPI.prev(Speind.this, speindData.service_package);
						}
					}
				});
			}    	 
			if (next!=null) {
				next.setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View v) {
						SpeindAPI.next(Speind.this, speindData.service_package);
					}
				});
				next.setOnLongClickListener(new OnLongClickListener(){
					@Override
					public boolean onLongClick(View arg0) {
						SpeindAPI.skipNews(Speind.this, speindData.service_package);
						return true;
					}
					
				});
			} 
			
		    WindowManager windowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
		    DisplayMetrics dm=new DisplayMetrics();
	        windowManager.getDefaultDisplay().getMetrics(dm);
	    	pagerImageW=Math.min(dm.widthPixels, dm.heightPixels);
	    	pagerImageH=(int)((float)Math.max(dm.widthPixels, dm.heightPixels)*0.4);
	    	densityDpi=dm.densityDpi;
			
	    	listImageW=Math.min(dm.widthPixels, dm.heightPixels)-(int)(10*((float)dm.densityDpi/160f));
	    	listImageH=listImageW/2;
	    	
			pager = (ViewPager) findViewById(R.id.pager);
			if (pager!=null) {
				pager.setOffscreenPageLimit(2);
				pagesCount=speindData.infopoints.size()+1;
				currentMP3position = pagesCount-1;
				pager.setAdapter(pagerAdapter);
				pager.setCurrentItem(speindData.currentInfoPoint);
			    pager.setOnPageChangeListener(pagerAdapter);
			    pager.setOnTouchListener(new OnTouchListener(){
    			    private GestureDetector gestureDetector=new GestureDetector( Speind.this, new SwipeDetector() );
        			class SwipeDetector extends SimpleOnGestureListener {    
        				private static final int SWIPE_MIN_DISTANCE = 50;
        			    private static final int SWIPE_MAX_OFF_PATH = 150;
        			    private static final int SWIPE_THRESHOLD_VELOCITY = 0;
        				private static final int TOUCH_MAX_DISTANCE = 1;
        			    
        		        @Override
        		        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                            if (e1==null||e2==null) return false;
        		        	if (Math.abs(e1.getY() - e2.getY()) <= SWIPE_MAX_OFF_PATH) {    		                		            
    	    		            if (e1.getX() - e2.getX() > SWIPE_MIN_DISTANCE && Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) {
    	    		            	//Speind.this.onSwipeLeft();
    	    		            	//gallery.onKeyDown(KeyEvent.KEYCODE_DPAD_RIGHT, null);
    	    		                return true;
    	    		            }
    	    		            if (e2.getX() - e1.getX() > SWIPE_MIN_DISTANCE && Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) {
    	    		            	//Speind.this.onSwipeRight();
    	    		            	//gallery.onKeyDown(KeyEvent.KEYCODE_DPAD_LEFT, null);
    	    		                return true;
    	    		            }
        		            }
        		        	        		        	
        		            if (Math.abs(e1.getX() - e2.getX()) <= SWIPE_MAX_OFF_PATH) {    		                		            
    	    		            if (e1.getY() - e2.getY() > SWIPE_MIN_DISTANCE && Math.abs(velocityY) > SWIPE_THRESHOLD_VELOCITY) {
    	    		            	Speind.this.onSwipeTop();
    	    		                return true;
    	    		            }
    	    		            if (e2.getY() - e1.getY() > SWIPE_MIN_DISTANCE && Math.abs(velocityY) > SWIPE_THRESHOLD_VELOCITY) {
    	    		            	Speind.this.onSwipeBottom();
    	    		                return true;
    	    		            }
        		            }    

        		        	if (Math.abs(e1.getY() - e2.getY()) >= TOUCH_MAX_DISTANCE) {    		                		            
        		        		return true;
        		        	}
        		        	if (Math.abs(e1.getX() - e2.getX()) >= TOUCH_MAX_DISTANCE) {    		                		            
        		        		return true;
        		        	}
        		            
        		            return false;
        		        }
        		    }
    				@Override
    				public boolean onTouch(View arg0, MotionEvent event) {
    					boolean res=gestureDetector.onTouchEvent(event);
		            	if (event.getAction()==MotionEvent.ACTION_UP) {
		            		if (!res) { 
		            			Speind.this.onTouch();
		            		}
		            	}
    					return false;			
    				}
        		});

			}
			
		    list = (ListView) findViewById(R.id.list);
		    if (list!=null) {
                itemsCount=speindData.infopoints.size();
		    	list.setFastScrollEnabled(false);
		    	list.setVelocityScale(0.8f);
		    	list.setRecyclerListener(listAdapter);
		    	list.setAdapter(listAdapter);
		    	list.setSelection(itemsCount-1-speindData.currentInfoPoint);
		    	list.setOnScrollListener(listAdapter);
		    	list.setOnItemClickListener(listAdapter);
		    }
		    
		    main_layout_visible=true;
		    supportInvalidateOptionsMenu();
    	}
    }

	@Override
	public void onPlayPositionChanged(final int current, final int max) {
		if (mode==MODE_PLAYER) {
    		if (pager!=null) {
	    		SpeindPagerAdapter.SpeindPagerAdapterObject object = pagerAdapter.objectsByPosition.get(pager.getCurrentItem());
        		ProgressBar play_progress=(ProgressBar) object.view.findViewById(R.id.play_progress);
        		if (play_progress!=null) {
                	if (max!=play_progress.getMax()) play_progress.setMax(max);
        			play_progress.setProgress(current);
                	final TextView current_timev=(TextView) findViewById(R.id.current_time);
                    if (current_timev!=null) current_timev.setText(secoundsToTimeString(current/1000)); 
        		}
    		}
		}
	}

	public void openFeedBack() {
		Intent intent=new Intent(Speind.this, SpeindAbout.class);
		Speind.this.startActivity(intent);		
	}
	
	public void refreshPostTimeAgoFunc() {
		log("refrashPostTimeAgoFunc");
		handler.removeCallbacks(refreshPostTimeAgo);
		if (!isPlayingMP3) {
			listAdapter.updateCachedTimeAgo();
			pagerAdapter.updateCachedTimeAgo();
		}
		handler.postDelayed(refreshPostTimeAgo, 10000);	
	}

	@Override
	public void onReady() {
		log("onReady");
    	showScreen(R.layout.speind_main);
    	onDataFeedsProcessingChanged();
	}

	@Override
	public void onDataFeedsProcessingChanged() {
		log("onDataFeedsProcessingChanged");
		
    	if (dataFeedWorker.mHandler!=null) {
    		dataFeedWorker.mHandler.post(new Runnable(){
				@Override
				public void run() {
					String pn="";
					int cnt=speindData.dataFeedsettingsInfos.size();
					for (int i=0; i<cnt; i++) {
						if (speindData.dataFeedsettingsInfos.get(i).isWorking()) {
							if (!pn.equals("")) pn+=", "; 
							pn+=speindData.dataFeedsettingsInfos.get(i).getTitle();
						}
					}
					final String pn1=pn;
					handler.post(new Runnable(){
						@Override
						public void run() {
							RelativeLayout process_bar=(RelativeLayout) findViewById(R.id.process_bar);	
							if (process_bar!=null) {
								if (!pn1.equals("")) {
									TextView plugin_names=(TextView) findViewById(R.id.plugin_names);
									if (plugin_names!=null) {
										plugin_names.setText(pn1);
									}
									process_bar.setVisibility(LinearLayout.VISIBLE); 
								} else {
									process_bar.setVisibility(LinearLayout.INVISIBLE);
								}
							}	
						}						
					});
				}
    		});
    	} else {
    		handler.postDelayed(new Runnable(){
				@Override
				public void run() {
					onDataFeedsProcessingChanged();
				}
    		}, 5000);
    	}    	
		
	}

	@Override
	public void onShowUpdates(String what_new_html) {
		log("onShowUpdates");
		final DialogWindow wnw=new DialogWindow(this, R.id.drawer_layout, R.layout.speind_what_new_dialog);
    	Button okButton=(Button) wnw.findViewById(R.id.ok_button);    	
    	TextView message=(TextView) wnw.findViewById(R.id.message);
    	if (okButton!=null) {
    		okButton.setOnClickListener(new Button.OnClickListener() {
				@Override
				public void onClick(View arg0) {
					wnw.hide();
				} 
			});
    	}
    	if (message!=null) {
    		message.setText(Html.fromHtml(what_new_html));
            message.setMovementMethod(LinkMovementMethod.getInstance());
    	}
    	wnw.show();
    }

	@Override
	public void onExit() {
		log("onExit");
		this.finish();	
	}

	@Override
	public void onSettingsChanged(SpeindSettings oldConfig, SpeindSettings newConfig) { 
		log("onSettingsChanged");
	}

	@Override
	public void onDataFeedsStateChanged(final String packageName, final int state, final boolean need_auth) {
		log("onDataFeedsStateChanged: "+packageName+" "+(state == SpeindAPI.DataFeedSettingsInfo.DATAFEED_STATE_READY)+" "+pluginSwithes.size());
		dataFeedWorker.mHandler.post(new Runnable() {
            @Override
            public void run() {
                final ToggleButton enPluginSwitch = pluginSwithes.get(packageName);
                if (enPluginSwitch != null) {
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            enPluginSwitch.setChecked(state == SpeindAPI.DataFeedSettingsInfo.DATAFEED_STATE_READY);
                            enPluginSwitch.setEnabled(true);
                            if (need_auth) {
                                DialogWindow.showConfirmDialog(Speind.this, R.id.main_wrap, R.layout.speind_confirm_dialog, R.string.plugin_need_auth,
                                        new Button.OnClickListener() {
                                            @Override
                                            public void onClick(View arg0) {
                                                ArrayList<DataFeedSettingsInfo> dataFeedsettingsInfos = new ArrayList<>();
                                                dataFeedsettingsInfos.addAll(speindData.dataFeedsettingsInfos);
                                                int cnt = dataFeedsettingsInfos.size();

                                                for (int i = 0; i < cnt; i++) {
                                                    final DataFeedSettingsInfo info = dataFeedsettingsInfos.get(i);
                                                    if (info.packageName.equals(packageName)) {
                                                        SpeindAPI.requestPluginSettings(Speind.this, speindData.service_package, info);
                                                        final DrawerLayout mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
                                                        if (mDrawerLayout!=null) mDrawerLayout.closeDrawers();
                                                        break;
                                                    }
                                                }
                                            }
                                        },
                                        new Button.OnClickListener() {
                                            @Override
                                            public void onClick(View arg0) {

                                            }
                                        },
                                        null
                                );
                            }
                        }
                    });
                }
            }
        });
		
	}

	@Override
	public void onDataFeedsListChanged() {
		log("onDataFeedsListChanged");
		if (dataFeedWorker.mHandler!=null) {
			dataFeedWorker.mHandler.post(new Runnable(){
				@Override
				public void run() {
					pluginSwithes.clear();
					final LinearLayout settingsItems = (LinearLayout) findViewById(R.id.settingsItems);
					if (settingsItems!=null) {
						handler.post(new Runnable(){
							@Override
							public void run() {
								settingsItems.removeAllViews();
							}
						});						
						ArrayList<DataFeedSettingsInfo> dataFeedsettingsInfos = new ArrayList<DataFeedSettingsInfo>();
						dataFeedsettingsInfos.addAll(speindData.dataFeedsettingsInfos);
				    	Collections.sort(dataFeedsettingsInfos, new Comparator<DataFeedSettingsInfo>() {
					        @Override
					        public int compare(DataFeedSettingsInfo c1, DataFeedSettingsInfo c2) {			        							
					        	return c1.getTitle().compareToIgnoreCase(c2.getTitle());
					        }
					    });
						int cnt=dataFeedsettingsInfos.size();
						
						for (int i=0; i<cnt; i++) {
							final DataFeedSettingsInfo info=dataFeedsettingsInfos.get(i);
							final View item=getLayoutInflater().inflate(R.layout.speind_data_feed_item, null);
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
									pluginSwithes.put(info.packageName, enPluginSwitch);
									item.setOnClickListener(new OnClickListener(){
										@Override
										public void onClick(View arg0) {
											//if (enPluginSwitch.isEnabled()) enPluginSwitch.setChecked(!enPluginSwitch.isChecked());
											final DrawerLayout mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
											if (mDrawerLayout!=null) mDrawerLayout.closeDrawers();
											SpeindAPI.requestPluginSettings(Speind.this, speindData.service_package, info);
										}							
									});
									enPluginSwitch.setChecked(info.getState()== DataFeedSettingsInfo.DATAFEED_STATE_READY);
									enPluginSwitch.setOnCheckedChangeListener(new OnCheckedChangeListener() {
										@Override
										public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
											enPluginSwitch.setEnabled(false);
											if (isChecked) {
												SpeindAPI.requestResumePlugin(Speind.this, speindData.service_package, info);
											} else {
												SpeindAPI.requestSuspendPlugin(Speind.this, speindData.service_package, info);
											}
										}							
									});
								}
								Button settingsButton=(Button)item.findViewById(R.id.settings_button);
								if (settingsButton!=null) {
									settingsButton.setOnClickListener(new OnClickListener(){
										@Override
										public void onClick(View arg0) {
											SpeindAPI.requestPluginSettings(Speind.this, speindData.service_package, info);
										}
										
									});
								}
								handler.post(new Runnable(){
									@Override
									public void run() {
										settingsItems.addView(item);
									}
								});						
							}
						}
					}
				}
			});			
		} else {
			handler.postDelayed(new Runnable(){
				@Override
				public void run() {
					onDataFeedsListChanged();
				}
    		}, 5000);
		}
	}
    
	private void onSlidingStateIdle() {
		log("onSlidingStateIdle");
		for (Runnable r : runnableQueue) {
			handler.post(r);
		}
		runnableQueue.clear();
	}
	
	private void setMode(int mode) {
		RelativeLayout player_container = (RelativeLayout) findViewById(R.id.player_container);
		RelativeLayout list_container = (RelativeLayout) findViewById(R.id.list_container);
		this.mode=mode;
		SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
    	SharedPreferences.Editor editor = settings.edit();
    	editor.putInt("last_used_mode", mode);
    	editor.apply();
		if (mode==MODE_LIST) {
            log("set play music only TRUE");
	        SpeindAPI.setPlayerOnlyMode(this, speindData.service_package, true);
			if (player_container!=null) player_container.setVisibility(View.INVISIBLE);
			if (list_container!=null) list_container.setVisibility(View.VISIBLE);
		    if (list!=null) {
                if (speindData.infopoints.size()!=itemsCount) {
                    listAdapter.cache.clear();
                    itemsCount=speindData.infopoints.size();
                    listAdapter.notifyDataSetChanged();
                    list.setSelection(itemsCount - 1 - speindData.currentInfoPoint);
                }
		    }
		} else {
            log("set play music only FALSE");
	        SpeindAPI.setPlayerOnlyMode(this, speindData.service_package, false);
			if (list_container!=null) list_container.setVisibility(View.INVISIBLE);
			if (player_container!=null) player_container.setVisibility(View.VISIBLE);
			if (isPlayingMP3&&pager!=null) {
				//currentMP3position=speindData.currentInfoPoint;
				pager.setCurrentItem(currentMP3position, false);
			} else if (pager!=null){
				pager.setCurrentItem(speindData.currentInfoPoint, false);
			}
		}
	}
}
