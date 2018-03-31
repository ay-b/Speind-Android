package com.maple.speindui;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuffXfermode;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.text.Spanned;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.SparseArray;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.maple.speind.R;
//import ru.lifenews.speind.R;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import me.speind.SpeindAPI;

public class SpeindPinboard extends ActionBarActivity {

    private final Handler handler = new Handler();
    private ArrayList<SpeindAPI.InfoPoint> pinboard = new ArrayList<>();

    private ListView list = null;
    private SpeindPinboardListAdapter listAdapter = new SpeindPinboardListAdapter();
    private int listImageW=0;
    private int listImageH=0;

    String currentProfile="";

    private ArticleWindow articleWnd = null;

    private final Runnable refreshPostTimeAgo = new Runnable() { public void run() { refreshPostTimeAgoFunc(); } };

    public class SpeindPinboardListAdapter extends BaseAdapter implements AbsListView.RecyclerListener, AbsListView.OnScrollListener, AdapterView.OnItemClickListener {
        private final static int OF_SCREEN_ITEMS = 3;

        private class SpeindListAdapterViewHolder {
            TextView date = null;
            TextView sender = null;
            TextView text = null;
            ImageView senderImage = null;
            ImageView imageView = null;
            Button deleteButton = null;
        }

        private class SpeindListAdapterCacheItem {
            String id = "";
            Date date = new Date();
            String sender="";
            Spanned text= Html.fromHtml("");
            Bitmap senderImageBmp=null;
            Bitmap imageBmp=null;
        }

        private Map<View, SpeindListAdapterViewHolder> viewHolders = new HashMap<>();
        private SparseArray<View> convertViews = new SparseArray<>();
        private SparseArray<SpeindListAdapterCacheItem> cache = new SparseArray<>();
        private ArrayList<Integer> creatingCacheItems = new ArrayList<>();

        private int startCacheInterval = 0;
        private int endCacheInterval = 0;

        private int fvi = -1;
        private int vic = 0;

        @Override
        public void onScrollStateChanged(AbsListView view, int scrollState) { }

        @Override
        public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
            fvi = firstVisibleItem;
            vic = visibleItemCount;
            if (vic>0) fixCache(Math.max(0, fvi-OF_SCREEN_ITEMS), Math.min(getCount()-1, fvi+vic+OF_SCREEN_ITEMS));
        }

        @Override
        public int getCount() {
            return pinboard.size();
        }

        @Override
        public Object getItem(int position) {
            return null;
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
                viewHolder.deleteButton = (Button) convertView.findViewById(R.id.delete_button);
                LinearLayout buttons_wrap = (LinearLayout) convertView.findViewById(R.id.buttons_wrap);
                if (buttons_wrap!=null) {
                    buttons_wrap.setVisibility(View.VISIBLE);
                }

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
                if (pinboard.size()>(pos)) {
                    SpeindAPI.InfoPoint infopoint = pinboard.get(pos);
                    final TextView date=(TextView) v.findViewById(R.id.postDate);
                    if (date!=null) { date.setText(secoundsToTimeAgo(((new Date()).getTime()-infopoint.postTime.getTime())/1000)); }
                }
            }
        }

        private void fixCache(int sp, int ep) {
            int delta = Math.max(Math.abs(sp - startCacheInterval), Math.abs(ep - endCacheInterval));
            if (delta<OF_SCREEN_ITEMS-1) return;
            log("Fix cache: ["+sp+","+ep+"]");
            startCacheInterval=sp;
            endCacheInterval=ep;
            ArrayList<Integer> delPos = new ArrayList<>();
            for (int i=0; i<cache.size();i++) {
                int pos = cache.keyAt(i);
                if (pos>endCacheInterval||pos<startCacheInterval) {
                    delPos.add(pos);
                }
            }
            for (Integer key : delPos) {
                cache.remove(key);
            }
            for (int position=startCacheInterval;position<=endCacheInterval; position++) {
                if (cache.get(position)==null) createCacheItem(position, (position>=fvi&&position<fvi+vic));
            }
        }

        private void createCacheItem(final int position, final boolean sync) {
            Runnable ccir = new Runnable(){
                @Override
                public void run() {
                    log("createCacheItem: "+position);
                    final SpeindListAdapterCacheItem cacheItem = new SpeindListAdapterCacheItem();
                    SpeindAPI.InfoPoint infopoint=pinboard.get(position);
                    if (infopoint!=null) {
                        cacheItem.id = infopoint.id;
                        cacheItem.imageBmp=infopoint.getPostBmp(currentProfile, listImageW, listImageH);
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
                        SpeindAPI.InfoPointData data=infopoint.getData(currentProfile);
                        if (data!=null) {
                            cacheItem.sender=data.postSender;
                            cacheItem.text=(!infopoint.titleExists) ? cacheItem.text=Html.fromHtml(data.postOriginText) : Html.fromHtml(data.postTitle+"<br>"+data.postOriginText);
                            Bitmap senderBmp=infopoint.getSenderBmp(currentProfile);
                            if (senderBmp==null) {
                                if (!data.pluginBmpPath.equals("")) {
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
                            //log("cacheCreated: "+position);
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
                creatingCacheItems.add(position);
                if (sync) {
                    ccir.run();
                } else {
                    (new Thread(ccir)).start();
                }
            }
        }

        private void updateListViewAtPosition(final int position, SpeindListAdapterViewHolder viewHolder) {
            log("updateListViewAtPosition: "+position);
            if (viewHolder==null) return;
            final SpeindListAdapterCacheItem cacheItem = cache.get(position, null);
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
                if (viewHolder.text!=null) { viewHolder.text.setText(cacheItem.text);}
                if (viewHolder.senderImage!=null) {
                    if (cacheItem.senderImageBmp!=null) {
                        viewHolder.senderImage.setImageBitmap(cacheItem.senderImageBmp);
                        viewHolder.senderImage.setVisibility(ImageView.VISIBLE);
                    } else {
                        viewHolder.senderImage.setVisibility(ImageView.GONE);
                    }
                }
                if (viewHolder.deleteButton!=null) {
                    viewHolder.deleteButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            SharedPreferences settings = getSharedPreferences(SpeindPinboardSettings.PREFS_NAME, 0);
                            final boolean confirmDelete = settings.getBoolean(SpeindPinboardSettings.DELETE_CONFIRM_NANE, true);
                            if (confirmDelete) {
                                DialogWindow.showConfirmDialog(SpeindPinboard.this, R.id.main_wrap, R.layout.speind_confirm_dialog, R.string.remove_from_pinboard_confirm, new Button.OnClickListener() {
                                    @Override
                                    public void onClick(View arg0) {
                                        removeListItem(position);
                                        SpeindAPI.removeFromPinboard(SpeindPinboard.this, getPackageName(), cacheItem.id);
                                    }
                                }, null, SpeindPinboardSettings.DELETE_CONFIRM_NANE);
                            } else {
                                removeListItem(position);
                                SpeindAPI.removeFromPinboard(SpeindPinboard.this, getPackageName(), cacheItem.id);
                            }
                        }
                    });
                }
            } else {
                createCacheItem(position, true);
            }
        }

        private void removeListItem(int position) {
            pinboard.remove(position);
            for (int p = position; p<=endCacheInterval;p++) {
                if (p<getCount()) {
                    SpeindListAdapterCacheItem item = cache.get(p+1);
                    if (item!=null) {
                        cache.setValueAt(p, item);
                        View convertView = convertViews.get(p);
                        if (convertView!=null) {
                            SpeindListAdapterViewHolder viewHolder = viewHolders.get(convertView);
                            if (viewHolder!=null) updateListViewAtPosition(p, viewHolder);
                        }
                    } else {
                        createCacheItem(p, false);
                    }
                }
            }
            listAdapter.notifyDataSetChanged();
        }

        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            SpeindAPI.InfoPoint infopoint=pinboard.get(position);
            if (infopoint!=null) {
                SpeindAPI.InfoPointData data=infopoint.getData(currentProfile);
                if (data!=null) {
                    //if (!data.postArticle.isEmpty()) {
                    articleWnd = new ArticleWindow(SpeindPinboard.this, infopoint, currentProfile);
                    articleWnd.setCancelable(false);
                    articleWnd.setOnOnDestroyListener(new ArticleWindow.OnDestroyViewListener() {
                        @Override
                        public void onDestroyView() {
                            articleWnd = null;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.speind_pinboard);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        if (toolbar!=null) {
            toolbar.setTitle("");
            setSupportActionBar(toolbar);
            toolbar.setNavigationIcon(R.drawable.btn_back);
            toolbar.setNavigationOnClickListener(new View.OnClickListener(){
                @Override
                public void onClick(View arg0) {
                    SpeindPinboard.this.finish();
                }
            });
        }

        WindowManager windowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics dm=new DisplayMetrics();
        windowManager.getDefaultDisplay().getMetrics(dm);

        listImageW=Math.min(dm.widthPixels, dm.heightPixels)-(int)(10*((float)dm.densityDpi/160f));
        listImageH=listImageW/2;

        // TODO show please wait

        Intent intent=getIntent();
        currentProfile = intent.getStringExtra(SpeindAPI.PARAM_PROFILE_NAME);
        (new AsyncTask<Intent, Void, Void>(){
            @Override
            protected Void doInBackground(Intent... params) {
                Intent intent = params[0];
                pinboard = SpeindAPI.InfoPoint.getListFromIntent(intent);

                list = (ListView) findViewById(R.id.list);
                if (list!=null) {
                    list.setFastScrollEnabled(false);
                    list.setVelocityScale(0.8f);
                    list.setRecyclerListener(listAdapter);
                    list.setOnScrollListener(listAdapter);
                    list.setOnItemClickListener(listAdapter);
                }

                return null;
            }
            @Override
            protected void onPostExecute(Void param) {

                // TODO hide please wait

                list.setAdapter(listAdapter);
            }
        }).execute(intent);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_pinboard, menu);
        return super.onCreateOptionsMenu(menu);
    }

    public boolean onOptionsItemSelected(MenuItem item) {

        SharedPreferences settings = getSharedPreferences(SpeindPinboardSettings.PREFS_NAME, 0);

        final String email = settings.getString(SpeindPinboardSettings.EMAIL_NANE, "");
        final int format = settings.getInt(SpeindPinboardSettings.FORMAT_NANE, SpeindAPI.SPEIND_EMAIL_FORMAT_HTML);
        final boolean confirmClear = settings.getBoolean(SpeindPinboardSettings.CLEAR_CONFIRM_NANE, true);

        switch (item.getItemId()) {
            case R.id.send_pinboard:
                if (email.isEmpty()) {
                    DialogWindow.showConfirmDialog(this, R.id.main_wrap, R.layout.speind_confirm_dialog, R.string.pinboard_email_empty, new Button.OnClickListener() {
                        @Override
                        public void onClick(View arg0) {
                            // TODO start activity for result
                            SpeindAPI.openPinboardSettingsRequest(SpeindPinboard.this, getPackageName());
                        }
                    }, null, null);
                } else {
                    DialogWindow.showConfirmDialog(this, R.id.main_wrap, R.layout.speind_confirm_dialog, R.string.clear_pinboard_request,
                            new Button.OnClickListener() {
                                @Override
                                public void onClick(View arg0) {
                                    SpeindAPI.sendPinboard(SpeindPinboard.this, getPackageName(), email, format, true);
                                    pinboard.clear();
                                    listAdapter.notifyDataSetChanged();
                                }
                            },
                            new Button.OnClickListener() {
                                @Override
                                public void onClick(View arg0) {
                                    SpeindAPI.sendPinboard(SpeindPinboard.this, getPackageName(), email, format, false);
                                }
                            },
                            null
                    );
                }
                return true;
            case R.id.clear_pinboard:
                if (confirmClear) {
                    DialogWindow.showConfirmDialog(this, R.id.main_wrap, R.layout.speind_confirm_dialog, R.string.clear_pinboard_confirm, new Button.OnClickListener() {
                        @Override
                        public void onClick(View arg0) {
                            SpeindAPI.clearPinboard(SpeindPinboard.this, getPackageName());
                            pinboard.clear();
                            listAdapter.notifyDataSetChanged();
                        }
                    }, null, SpeindPinboardSettings.CLEAR_CONFIRM_NANE);
                } else {
                    SpeindAPI.clearPinboard(SpeindPinboard.this, getPackageName());
                    pinboard.clear();
                    listAdapter.notifyDataSetChanged();
                }
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private String secoundsToTimeAgo(long l) {
        String result="";
        long d=l/(24*3600);
        long h=(l-24*3600*d)/3600;
        long m=(l-24*3600*d-3600*h)/60;
        if (d>0) result+=""+d+"d ";
        if (h>0||d>0) result+=""+h+"h ";
        result+=""+m+"m ago";
        return result;
    }

    public void refreshPostTimeAgoFunc() {
        handler.removeCallbacks(refreshPostTimeAgo);
        listAdapter.updateCachedTimeAgo();
        handler.postDelayed(refreshPostTimeAgo, 10000);
    }

    @Override
    public void onPause() {
        handler.removeCallbacks(refreshPostTimeAgo);
        SpeindAPI.setPlayerOnlyMode(this, getPackageName(), false);
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        SpeindAPI.setPlayerOnlyMode(this, getPackageName(), true);
        handler.postDelayed(refreshPostTimeAgo, 10000);
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

    private static void log(String s) {
        Log.e("[---SpeindPinBoard---]", Thread.currentThread().getName() + ": " + s);
    }

}
