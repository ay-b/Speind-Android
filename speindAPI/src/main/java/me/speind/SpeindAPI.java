package me.speind;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.CRC32;
import java.util.zip.Checksum;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;

import com.cybozu.labs.langdetect.Detector;
import com.cybozu.labs.langdetect.DetectorFactory;
import com.cybozu.labs.langdetect.LangDetectException;
import com.cybozu.labs.langdetect.Language;

import android.support.v7.app.ActionBarActivity;
import android.app.ActivityManager;
import android.app.Service;
import android.app.ActivityManager.RunningServiceInfo;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Base64;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;

public class SpeindAPI {
	public final static String VOICE_DEMOS_URL			= "http://speind.me/voices/demos/";
	
	public final static String BROADCAST_ACTION			= "com.maple.speind.servicebackbroadcast";
	//public final static String SERVICE_PACKAGE			= "com.maple.speind";
	public final static String SERVICE_NAME				= "com.maple.speind.SpeindService";

	public final static String SPEIND_NAME				= "com.maple.speindui.Speind";
	public final static String SETTINGS_NAME			= "com.maple.speindui.SpeindSettings";
	public final static String VOICES_SETTINGS_NAME		= "com.maple.speindui.VoicesSettings";
	public final static String PINBOARD_NAME			= "com.maple.speindui.SpeindPinboard";
	public final static String PINBOARD_SETTINGS_NAME	= "com.maple.speindui.SpeindPinboardSettings";

	public final static String SPEIND_DIR			= Environment.getExternalStorageDirectory() + File.separator + "speind" + File.separator;
    //public final static String SPEIND_DIR			= Environment.get + File.separator + "speind" + File.separator;
	public final static String SPEIND_IMAGES_DIR	= SPEIND_DIR + "cache" + File.separator + "images" + File.separator;
	
	public final static int SPEIND_STATE_INITIALIZATION			= 0; 
	public final static int SPEIND_STATE_NEED_DOWNLOAD			= 1; 
	public final static int SPEIND_STATE_DOWNLOADING			= 2; 
	public final static int SPEIND_STATE_UNPUCKING				= 3; 
	public final static int SPEIND_STATE_NEED_PROFILE			= 4; 
	public final static int SPEIND_STATE_PREPARING				= 5; 
	public final static int SPEIND_STATE_STOP_PLAYER			= 6; 
	public final static int SPEIND_STATE_STOP_READER			= 7;
	public final static int SPEIND_STATE_PLAY_PLAYER			= 8; 
	public final static int SPEIND_STATE_PLAY_READER			= 9;

	public final static int SPEIND_EMAIL_FORMAT_HTML			= 0;
	public final static int SPEIND_EMAIL_FORMAT_TEXT			= 1;

	public final static int PLAY_NO_LIMIT	= -1;
	public final static int PLAY_ONE_TRACK	= 0;
	
	public static final String SERVICE_CMD						= "service_cmd";	
	public static final int SC_GET_STATE						= 0;	
	public static final int SC_SET_PROFILE						= 2;
	public static final int SC_PLAY								= 3;
	public static final int SC_STOP								= 4;
	public static final int SC_PREV								= 5;
	public static final int SC_NEXT								= 6;
	public static final int SC_REPLAY							= 7;
	public static final String PARAM_PROFILE_NAME				= "profile_name";
	public static final String PARAM_PROFILE_PASS				= "profile_pass";
	public static final int SC_NEW_PROFILE						= 8;
	public static final int SC_REMOVE_PROFILE					= 9;
    public static final int SC_POST								= 10;
	public static final int SC_LIKE								= 11;
	public static final int SC_DATA_FEED_SETTINGS_REQUEST		= 12;
	public static final int SC_SETTINGS_CHANGED					= 13;
	public static final int SC_PLAY_USER_FILE					= 14;
	public static final String PARAM_FILE_NAME					= "user_file_file";
	public static final int SC_PLAY_USER_TEXT					= 15;
	public static final String PARAM_USER_TEXT					= "user_text";
	public static final int SC_INFOPOINT_DETAILS				= 16;
	public static final int SC_SKIPNEWS							= 17;
	public static final int SC_PLAY_INFOPOINT					= 18;
	public static final String PARAM_INFOPOINT_POS				= "infpoint_pos";
	public static final int SC_PLAY_PAUSE						= 19;

	public static final int SC_READ_CURRENT_INFOPOINT_ARTICLE	= 21;
	public static final int SC_SETTINGS_REQUEST					= 22;
	public static final int SC_VOICES_SETTINGS_REQUEST			= 23;
	public static final int SC_DOWNLOAD_VOICE_REQUEST			= 24;
	public static final String PARAM_VOICE_CODE					= "voice_code";
	public static final int SC_SET_DEFAULT_VOICE_REQUEST		= 25;
	public static final int SC_BUY_VOICE_REQUEST				= 26;
	public static final int SC_BUY_VOICE_RESULT					= 27;
	public static final String PARAM_RESULT_CODE				= "result_code";
	public static final String PARAM_RESULT_DATA				= "result_data";
	public static final int SC_QUIT								= 28;
	public static final int SC_DATA_FEED_SUSPEND_REQUEST		= 29;
	public static final int SC_DATA_FEED_RESUME_REQUEST			= 30;
	public static final int SC_MEDIA_BUTTON_PRESS				= 31;
	public static final String PARAM_MEDIA_BUTTON_PRESS_COUNT	= "press_count";
	public static final int SC_RESTORE_PURCHASES				= 32;
	public static final String PARAM_WIFI_ONLY					= "wifi_only";
	public static final int SC_PACKAGE_CHANGED					= 33;
	public static final int SC_PLAYER_ONLY						= 34;
	public static final int SC_PAUSE							= 35;
	public static final int SC_RESUME							= 36;
	public static final String PARAM_PLAYER_ONLY				= "player_only";
	public static final int SC_PINBOARD_REQUEST					= 37;
	public static final int SC_ADD_TO_PINBOARD					= 38;
	public static final int SC_REMOVE_FROM_PINBOARD				= 39;
	public static final int SC_CLEAR_PINBOARD					= 40;
	public static final int SC_SEND_PINBOARD					= 41;
	public static final String PARAM_EMAIL						= "email";
	public static final String PARAM_FORMAT						= "format";
	public static final String PARAM_CLEAR						= "clear";
	public static final int SC_PINBOARD_SETTINGS_REQUEST		= 42;

	public static final String CLIENT_CMD						= "client_cmd";
	public static final int CC_ERROR							= 0;
	public static final String PARAM_ERROR_CODE 				= "err_code";
	public static final String PARAM_ERROR_STR 					= "err_str";
    public static final int CC_INFO_MESSAGE                     = 1;
    public static final String PARAM_MESSAGE_STR 				= "message_str";
    public static final int CC_DOWLOAD_PROGRESS					= 4;
	public static final String PARAM_DWLD_PRG					= "download_progress";
	public static final int CC_PROFILES							= 5;
	public static final int CC_FULL_STATE						= 6;
	public static final String PARAM_INFOPOINTS_COUNT			= "infopoints_count";
	public static final String PARAM_CURRENT_INFOPOINT			= "infopoins_current";
	public static final String PARAM_DATAFEED_SETTINGS_INFOS_COUNT	= "datafeed_settings_infos_count";	
	public static final String PARAM_WHATNEW_STR				= "what_new_str";
	public static final int CC_STATE_CHANGED					= 7;
	public static final String PARAM_OLD_STATE					= "speind_old_state";
	public static final String PARAM_NEW_STATE					= "speind_new_state";
	public static final int CC_PLAYING_INFO						= 9;
	public static final String PARAM_PLAY_SORCE					= "play_source";
	public static final String PARAM_STATE						= "curr_state";
	public static final int SOURCE_MP3							=0;
	public static final int SOURCE_INFOPOINT					=1;
	public static final String PARAM_PLAY_FILE					= "mp3_file";
	public static final String PARAM_NEXT_PLAY_FILE				= "next_mp3_file";
	public static final String PARAM_PLAY_LENGTH				= "mp3_file_length";
	public static final String PARAM_PLAY_POSITION				= "mp3_file_position";
	public static final String PARAM_COVER_BMP					= "mp3_cover_bmp";
	public static final String PARAM_INFOPOINT_PLAY_FULL		= "infopoint_play_full";
	public static final String PARAM_INFOPOINT_ID				= "infopoin_id";
	public static final String PARAM_INFOPOINT_PRIORITY			= "infopoin_priority";
	public static final String PARAM_INFOPOINT_READARTICLE		= "infopoin_readArticle";
	public static final String PARAM_INFOPOINT_TITLEEXISTS		= "infopoint_titleExist";
	public static final String PARAM_INFOPOINT_TEXTEXISTS		= "infopoint_textExists";
	public static final String PARAM_INFOPOINT_ARTICLEEXISTS	= "infopoint_articleExists";
	public static final String PARAM_INFOPOINT_POST_TIME		= "infopoin_post_time";
	public static final String PARAM_PROCESS_PLUGIN				= "infopoin_process_plugin";
	public static final int CC_ADD_INFOPOINT					= 11;
	public static final String PARAM_ADD_POSITION				= "infopoint_add_position";
	public static final int CC_WRONG_PROFILE_DATA				= 12;
	public static final int CC_WRONG_PROFILE_EXISTS				= 13;
	public static final int CC_NEW_DATAFEED_SETTINGS_INFO		= 15;
	public static final int CC_PLAYING_POSITION					= 16;
	public static final int CC_SETTINGS							= 17;
	public static final int CC_EXIT								= 18;
	public static final String PARAM_VOICES_DATA				= "voices_data";
	public static final int CC_VOICES_DATA_UPDATED				= 19;
	public static final int CC_INFOPOIN_POSITION				= 20;
	public static final int CC_DATAFEED_INFO_CHANGED    		= 31;
	public static final int CC_VOICES_BUY_CMD					= 33;
	public static final int CC_REMOVE_DATAFEED_SETTINGS_INFOS	= 35;
	public static final String PARAM_DATAFEED_PACKAGES			= "packages_array";
	
	public static final String DATAFEED_SERVICE_CMD				= "datafeed_service_cmd";
	public static final int DF_SC_DATAFEED_INFO 				= 0;
	public static final int DF_DATAFEED_INFO_CHANGED 			= 1;
    public static final String PARAM_ERROR                      = "error";
	public static final int DF_SC_NEW_INFO_POINT 				= 100;
	
	public static final String DATAFEED_CLIENT_CMD				= "datafeed_client_cmd";
    public static final String PARAM_PACKAGE_NAME				= "package_name";
	public static final int DF_CC_SET_PROFILE					= 0;
    public static final int DF_CC_POST							= 1;
	public static final int DF_CC_DATA_FEED_SETTINGS_REQUEST	= 2;
	public static final int DF_CC_LIKE							= 3;
	public static final int DF_CC_INFOPOINT_DETAILS				= 4;
	public static final int DF_CC_LANG_LIST_CHANGED				= 5;
	public static final int DF_CC_LOAD_IMAGES_CHANGED			= 6;
	public static final int DF_CC_START							= 7;
	public static final int DF_CC_SUSPEND						= 8;
	public static final int DF_CC_RESUME						= 9;
	public static final int DF_CC_STORE_INFOPOINTS_TIME_CHANGED	= 10;
	public static final int DF_CC_INIT							= 11;
	public static final String PARAM_SERVICE_PACKAGE_NAME		= "sevice_package_name";
	public static final int DF_CC_STOP							= 100;
	public static final String PARAM_LANG_LIST					= "lang_list";
	public static final String PARAM_LOAD_IMAGES				= "load_images";
	public static final String PARAM_STORE_TIME					= "store_time";
	
	public static void log(String s2){
    	Log.e("[---SpeindAPI---]", Thread.currentThread().getName()+": "+s2);
	}
	
	public static boolean isURL(String word) {
		try {
			new URL(word);
			return true;
		} catch (MalformedURLException e) {
			if (word.matches("((https?)?://)?([a-zA-Z0-9-]{1,128}\\.)+([a-zA-Z]{2,4})+(:[0-9]{0,5})?(/[a-zA-Z0-9.,_@%&?+=\\~/#-]*)?")) {
				return true;
			} else {
				return false;
			}
		} catch (Exception e) {
			return false;
		}			
	}

 	public static int getConnectionStatus(Context context) {
		ConnectivityManager aConnectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
	    NetworkInfo aNetworkInfo = aConnectivityManager.getActiveNetworkInfo();
	    if (aNetworkInfo != null && aNetworkInfo.isConnected()){
	        return aNetworkInfo.getType();
	    }else{
	        return -1;
	    }		
	}

 	public static String BitMapToString(Bitmap bitmap){
		if (bitmap==null) return "";
        ByteArrayOutputStream baos=new  ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG,100, baos);
        byte [] b=baos.toByteArray();
        String temp=Base64.encodeToString(b, Base64.DEFAULT);
        return temp;
	}

	public static Bitmap StringToBitMap(String encodedString){
		if (encodedString.equals("")) return null;
	     try{
	       byte [] encodeByte=Base64.decode(encodedString,Base64.DEFAULT);
	       Bitmap bitmap=BitmapFactory.decodeByteArray(encodeByte, 0, encodeByte.length);
	       return bitmap;
	     }catch(Exception e){
	       e.getMessage();
	       return null;
	     }
	}
    
	public static String getHash(String str) {
    	Checksum checksum = new CRC32();
    	byte bytes[] = str.getBytes();
    	checksum.update(bytes,0,bytes.length);		
		return SpeindAPI.md5(str)+checksum.getValue();
	}	
	
	public static void saveStringToFile(String dirName, String fileName, String data) {
    	File dirToSave = new File(dirName);
    	dirToSave.mkdirs();
    	File file = new File(dirToSave, fileName);
    	if (!file.exists()) {
	    	try {
		    	file.createNewFile();
		    	FileOutputStream fos = new FileOutputStream(file);
				fos.write(data.getBytes());
		    	fos.flush();
		    	fos.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
    	}		
	}
	
	private static Boolean isCachedBitmap(String src) {
		String fileName=getHash(src);
    	File dirToSave = new File(SPEIND_IMAGES_DIR);
    	dirToSave.mkdirs();
    	File file = new File(dirToSave, fileName);
    	return file.exists();
    }
		
    public static Bitmap getBitmapFromURL(Context context, String src, boolean cache_only) {
        try {
        	if (isCachedBitmap(src)) {
        		return BitmapFactory.decodeFile(SPEIND_IMAGES_DIR+getHash(src));
        	} else {
        		if (!cache_only) {
	        			        		
		            URL url = new URL(src);
		            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
		            connection.setDoInput(true);
		            connection.connect();
		            
	        		File file = File.createTempFile(getHash(src), "dwnl");
	        		FileOutputStream fos = new FileOutputStream(file);

		            InputStream input = connection.getInputStream();
		            
		            //int totalSize = connection.getContentLength();
		            //int downloadedSize = 0;
		            
		            byte[] buffer = new byte[1024];
		            int bufferLength;

		            while ((bufferLength = input.read(buffer)) > 0) {
		            	fos.write(buffer, 0, bufferLength);
		            	//downloadedSize += bufferLength;
		            }
		        
		            fos.close();
		            input.close();
		            connection.disconnect();
		            
		            BitmapFactory.Options bmOptions = new BitmapFactory.Options();
		    	    bmOptions.inJustDecodeBounds = true;
		    	    BitmapFactory.decodeFile(file.getAbsolutePath(), bmOptions);
		    	    
		    	    int photoW = bmOptions.outWidth;
		    		int photoH = bmOptions.outHeight;
		    		
		            //Bitmap myBitmap = BitmapFactory/decodeStream(input);
		            
		            WindowManager mWindowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
	    	        DisplayMetrics dm=new DisplayMetrics();
	    	        mWindowManager.getDefaultDisplay().getMetrics(dm);
		            
	    	        int maxLen=Math.min(Math.min(dm.widthPixels, dm.heightPixels), 720);
	    	        
	    	        float scaleFactor=(float)photoH/(float)maxLen;
	    	        
	    	        bmOptions.inJustDecodeBounds = false;
	    			bmOptions.inSampleSize = (int)scaleFactor;
	    			bmOptions.inPurgeable = true;
	    			
	    			Bitmap myBitmap=BitmapFactory.decodeFile(file.getAbsolutePath(), bmOptions);
	    			
		            if (myBitmap.getHeight()>maxLen) {
		            	scaleFactor=(float)myBitmap.getHeight()/(float)maxLen;
		            	myBitmap=Bitmap.createScaledBitmap(myBitmap, (int)(((float)myBitmap.getWidth())/scaleFactor+0.5), maxLen, true);
		            }
	    	        
		            //if (myBitmap!=null) {
			        //	String pathData=SPEIND_IMAGES_DIR+getHash(BitMapToString(myBitmap));		        	
			        //	String fileName=getHash(src);
			        //	saveStringToFile(SPEIND_IMAGES_DIR+"links"+File.separator, fileName, pathData);
		            //}
		            
		            file.delete();
		            
		            return myBitmap;
        		} else return null;
        		
        	}
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
	
    public static final String md5(final String s) {
        try {
            // Create MD5 Hash
            MessageDigest digest = java.security.MessageDigest.getInstance("MD5");
            digest.update(s.getBytes());
            byte messageDigest[] = digest.digest();
            // Create Hex String
            StringBuilder hexString = new StringBuilder();
            for (int i = 0; i < messageDigest.length; i++) {
                String h = Integer.toHexString(0xFF & messageDigest[i]);
                while (h.length() < 2)
                    h = "0" + h;
                hexString.append(h);
            }
            return hexString.toString();
     
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return "";
    }
    
    public static void saveBitmapToSdcard(String dirName, String fileName, Bitmap bitmap) throws IOException {
    	File dirToSave = new File(dirName);
    	dirToSave.mkdirs();
    	File file = new File(dirToSave, fileName);
    	if (!file.exists()) {
	    	file.createNewFile();
	    	FileOutputStream fos = new FileOutputStream(file);
	    	bitmap.compress(Bitmap.CompressFormat.PNG, 80, fos);
	    	fos.flush();
	    	fos.close();
    	}
    }
    
    public static Bitmap GetScaledBitmap(Bitmap bmp, int targetW, int targetH) {
    	if (bmp==null) return null;
    	
    	int photoW = bmp.getWidth();
		int photoH = bmp.getHeight();
		
		if (targetH==-1) {
			targetH=(int)((float)targetW*((float)photoH/(float)photoW));
		}
		
		Bitmap srcBmp = null;
		
		if (!(photoW==targetW&&photoH>=targetH||photoH==targetH&&photoW>=targetW)) {
			float scaleFactor = (float) Math.min((float)photoW/(float)targetW, (float)photoH/(float)targetH);
	    	srcBmp = Bitmap.createScaledBitmap(bmp, (int)(((float)photoW)/scaleFactor+0.5), (int)(((float)photoH)/scaleFactor+0.5), true);
		} else {
			srcBmp=bmp;
		}
		
	    if (srcBmp.getWidth() >= srcBmp.getHeight()){
			  return Bitmap.createBitmap(srcBmp, (srcBmp.getWidth() - targetW)/2, 0, targetW, srcBmp.getHeight() );
		}else{
			  return Bitmap.createBitmap(srcBmp, 0, (srcBmp.getHeight() - targetH)/2, srcBmp.getWidth(), targetH);
		}
    }
    
    public static Bitmap GetScaledBitmap(String mCurrentPhotoPath, int targetW, int targetH) {
		Bitmap bmp=BitmapFactory.decodeFile(mCurrentPhotoPath);
		return GetScaledBitmap(bmp, targetW, targetH);
    }
    
    public static Bitmap GetScaledResourceBitmap(Context context, int id, int targetW, int targetH) {			    			
		Bitmap bmp=BitmapFactory.decodeResource(context.getResources(), id);
		return GetScaledBitmap(bmp, targetW, targetH);
    }
    
    public static Bitmap GetScaledBitmap(byte[] data, int offset, int length, int targetW, int targetH) {
		Bitmap bmp = BitmapFactory.decodeByteArray(data, offset, length);
		return GetScaledBitmap(bmp, targetW, targetH);

    }
    
    public static String getArticleFromURL(String urlStr) {
    	if (urlStr==null||urlStr.isEmpty()) return "";
    	
    	List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
        nameValuePairs.add(new BasicNameValuePair("url", urlStr));
        
        HttpClient httpclient = new DefaultHttpClient();
        HttpPost httppost = new HttpPost("http://api.speind.me/getArticle/");
        try {
            httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
            HttpResponse response = httpclient.execute(httppost);
            
            HttpEntity entity = response.getEntity();
            StringBuilder sb = new StringBuilder();
            try {
                BufferedReader reader = new BufferedReader(new InputStreamReader(entity.getContent()), 65728);
                String line = null;

                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
            } catch (Exception e) {
            	e.printStackTrace(); 
            }
            
            if (response.getStatusLine().getStatusCode()!=200) return "";
            return sb.toString();
            
        } catch (Exception e) {
        	e.printStackTrace(); 
        }        

    	return "";    	
    }

    public static String getLang(String fulltext) {
    	String res="";
    	try {
			DetectorFactory.loadProfile(SPEIND_DIR+"lang_profiles");
		} catch (LangDetectException e) {
			//e.printStackTrace();
		}
		try {
			
	    	Detector detector;
			detector = DetectorFactory.create();
	    	detector.append(fulltext);
	    	ArrayList<Language> detLangList=detector.getProbabilities();    	
	    	for (Language detLang : detLangList) {
	    		return detLang.lang;	    		
	    	}
	    	
		} catch (LangDetectException e) {
			e.printStackTrace();
		}
    	
		return res;
    }
    
    public static String getLang(String fulltext, ArrayList<String> langList) {
    	String res="";
    	try {
			DetectorFactory.loadProfile(SPEIND_DIR+"lang_profiles");
		} catch (LangDetectException e) {
			//e.printStackTrace();
		}
		try {
			
	    	Detector detector;
			detector = DetectorFactory.create();
	    	detector.append(fulltext);
	    	ArrayList<Language> detLangList=detector.getProbabilities();    	
	    	for (Language detLang : detLangList) {
	    		String dLang=detLang.lang;	    		
	    		for (String lang : langList) {
	    			if (lang.compareToIgnoreCase(dLang)==0) {
	    				res=lang;
	    				if (res.compareToIgnoreCase("en")!=0) return res;
	    			}
	    		}
	    	}
	    	
		} catch (LangDetectException e) {
			e.printStackTrace();
		}
    	
		return res;
    }
    
    public static void executeIfSpeindStarted(final Context context, final String service_package, final Runnable func, final boolean async) {
    	(new AsyncTask<Void, Void, Boolean>(){
			@Override
			protected Boolean doInBackground(Void... params) {
				boolean res=false;
				ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
		    	List<RunningServiceInfo> list = manager.getRunningServices(Integer.MAX_VALUE);    	
		    	for (RunningServiceInfo service : list) {						        	
	        		if (service.service.getPackageName().equals(service_package)&&service.service.getClassName().equals("com.maple.speind.SpeindService")) {
	        			res=true;
	        			break;
	        		} 
		    	}
		    	if (res&&async) {
		    		func.run();
		    	}
				return res;
			}
			@Override
			protected void onPostExecute(Boolean param) {	
				if (param&&!async) {
					func.run();
				}
			}
		}).execute();
    }
    
    public static class DataFeedSettingsInfo {
        public static int DATAFEED_STATE_READY = 0;
        public static int DATAFEED_STATE_SUSPENDED = 1;

        private static final String PARAM_LOGO_PATH					= "logo_path";
        private static final String PARAM_TITLE						= "title";
        private static final String PARAM_READ_CONFIRM				= "read_confirm";
        private static final String PARAM_PROCESSING				= "processing";
        private static final String PARAM_AUTHORIZATION				= "authorization";
        private static final String PARAM_POST      				= "post";

        public String packageName;
        private String title;
        private String bmpPath;
        private int state;
        private boolean isWorking;
        private boolean isNeedAuthorization;
        private boolean isNeedConfirmRead;
        private boolean canPost;

        DataFeedSettingsInfo(String packageName, String title, String bmpPath, int state, boolean isNeedConfirmRead, boolean isWorking, boolean isNeedAuthorization, boolean canPost) {
            this.packageName            = packageName;
            this.title                  = title;
            this.bmpPath                = bmpPath;
            this.state                  = state;
            this.isNeedConfirmRead      = isNeedConfirmRead;
            this.isWorking              = isWorking;
            this.isNeedAuthorization    = isNeedAuthorization;
            this.canPost                = canPost;
        }

    	public DataFeedSettingsInfo(Context context, String title, Bitmap bmp, boolean canPost) {
    		this.packageName            = context.getPackageName();
    		this.title                  = title;
    		this.bmpPath                = "";
            this.state                  = DATAFEED_STATE_READY;
    		this.isNeedConfirmRead      = false;
    		this.isWorking              = false;
            this.isNeedAuthorization    = true;
            this.canPost                = canPost;
            if (bmp!=null) {
                setBmp(bmp);
            }
    	}
    	
    	public Bitmap getBmp() {
    		if (this.bmpPath.equals(""))
    			return null;
    		else {
    			return BitmapFactory.decodeFile(this.bmpPath);
    		}
    	}

    	public void setBmp(Bitmap bmp) {
    		if (bmp==null) return;
            String dir = SPEIND_DIR;
            String file=getHash(BitMapToString(bmp));
    		    		
    		this.bmpPath=dir+file;
    		try {
				saveBitmapToSdcard(dir, file, bmp);
			} catch (IOException e) {
				this.bmpPath="";
				e.printStackTrace();
			}
    	}
    	
        private void putToIntent(Intent intent, int idx) {
        	String append="";
        	if (idx>-1) append="_"+idx;
        	intent.putExtra(PARAM_PACKAGE_NAME+append, packageName);
        	intent.putExtra(PARAM_TITLE+append, title);
        	intent.putExtra(PARAM_LOGO_PATH+append, bmpPath);
            intent.putExtra(PARAM_STATE+append, state);
        	intent.putExtra(PARAM_READ_CONFIRM+append, isNeedConfirmRead);
        	intent.putExtra(PARAM_PROCESSING+append, isWorking);
            intent.putExtra(PARAM_AUTHORIZATION+append, isNeedAuthorization);
            intent.putExtra(PARAM_POST+append, canPost);
        }

        public void putToIntent(Intent intent) {
        	putToIntent(intent, -1);        	
        }

        public static void putListToIntent(Intent intent, ArrayList<DataFeedSettingsInfo> infos) {
        	int cnt=infos.size();
        	intent.putExtra(PARAM_DATAFEED_SETTINGS_INFOS_COUNT, cnt);
        	for (int i=0;i<cnt;i++) {
        		DataFeedSettingsInfo info=infos.get(i);
        		info.putToIntent(intent, i);
        	}        	
        }
        
        private static DataFeedSettingsInfo getFromIntent(Intent intent, int idx) {
        	String append="";
        	if (idx>-1) append="_"+idx;
        	String packageName	        = intent.getStringExtra(PARAM_PACKAGE_NAME+append);
        	String title		        = intent.getStringExtra(PARAM_TITLE+append);
        	String bmpPath		        = intent.getStringExtra(PARAM_LOGO_PATH + append);
            int state = intent.getIntExtra(PARAM_STATE + append, DATAFEED_STATE_READY);
        	boolean isNeedConfirmRead	= intent.getBooleanExtra(PARAM_READ_CONFIRM + append, false);
        	boolean isWorking	        = intent.getBooleanExtra(PARAM_PROCESSING + append, false);
            boolean isNeedAuthorization = intent.getBooleanExtra(PARAM_AUTHORIZATION+append, false);
            boolean canPost             = intent.getBooleanExtra(PARAM_POST+append, false);
        	return new DataFeedSettingsInfo(packageName, title, bmpPath, state, isNeedConfirmRead, isWorking, isNeedAuthorization, canPost);
        }

        public static DataFeedSettingsInfo getFromIntent(Intent intent) {
        	return (getFromIntent(intent, -1));
        }

        public static ArrayList<DataFeedSettingsInfo> getListFromIntent(Intent intent) {
        	ArrayList<DataFeedSettingsInfo> infos=new ArrayList<DataFeedSettingsInfo>();
        	int cnt=intent.getIntExtra(PARAM_DATAFEED_SETTINGS_INFOS_COUNT, -1);
        	for (int i=0;i<cnt;i++) {
        		infos.add(getFromIntent(intent, i));
        	}
        	return infos;
        }

        public String getTitle() {
            return title;
        }

        public boolean isWorking() {
            return isWorking;
        }

        public boolean isNeedAuthorization() {
            return isNeedAuthorization;
        }

        public boolean isNeedConfirmRead() {
            return isNeedConfirmRead;
        }

        public int getState() {
            return state;
        }

        public void setState(int state) {
            this.state = state;
        }

        public boolean canPost() {
            return canPost;
        }
    }
       
    public static class InfoPointData implements Serializable {
		private static final long serialVersionUID = -381127942816006431L;
		public String id;   
    	public String postSender;
    	public String senderBmpPath;
		public String senderBmpURL;
    	public String postBmpPath;
		public String postBmpURL;
    	public String postTitle;
    	public String postOriginText;
    	public String postLink;    	
    	public String postPluginData;     	
    	public String postSenderVocalizing;
    	public String postTitleVocalizing;
    	public String postTextVocalizing;
    	public String postArticle;     	
    	public String postLang;
		public String postURL;
		public String pluginBmpPath;
    	public ArrayList<String> postTokens=new ArrayList<String>();
    }
    
    public static class InfoPoint {
    	public static int PRIORITY_HIGHEST	= 0;
    	public static int PRIORITY_HIGH		= 1;
    	public static int PRIORITY_MEDIUM	= 2;
    	public static int PRIORITY_NORMAL	= 3;
    	
    	private static final double filterJaccardIndex=0.5;
    	private String saveDir=SPEIND_DIR + "infopoints" + File.separator;
    	public String id;    	
    	public String processingPlugin;    	
    	public Date postTime;
    	public boolean dataCreated=false;
    	public int priority;
    	public boolean readArticle;
		public boolean titleExists;
		public boolean textExists;
		public boolean articleExists;

		public InfoPoint(String id) {
			this.id = id;
		}

    	public InfoPoint(String id, String processingPlugin, Date postTime, int priority, boolean readArticle, boolean titleExists, boolean textExists, boolean articleExists) {
    		this.id = id;
    		this.processingPlugin=processingPlugin;
    		this.postTime=postTime;
    		this.priority=priority;
    		this.readArticle=readArticle;
			this.titleExists = titleExists;
			this.textExists = textExists;
			this.articleExists = articleExists;
    		if (this.priority<PRIORITY_HIGH) {
    			this.priority=PRIORITY_HIGH;
    		} else if (this.priority>PRIORITY_NORMAL) {
    			this.priority=PRIORITY_NORMAL;
    		}
    	}
    	
    	public InfoPoint(String profile, String processingPlugin, Date postTime, String postSender, String postTitle, String postOriginText, String postLink, String postPluginData, String postSenderVocalizing, String postTitleVocalizing, String postTextVocalizing, String lang, int priority, boolean readArticle, String postURL, String pluginBmpPath) {
    		String str=postTitle+" "+postOriginText+" "+postTime.toString();
        	this.id=getHash(str);
        	this.processingPlugin=processingPlugin;
        	this.postTime=postTime; 
        	if (this.postTime==null) this.postTime=new Date();        	
        	this.dataCreated=false;
        	this.priority=priority;
        	this.readArticle=readArticle;
    		if (this.priority<PRIORITY_HIGH) {
    			this.priority=PRIORITY_HIGH;
    		} else if (this.priority>PRIORITY_NORMAL) {
    			this.priority=PRIORITY_NORMAL;
    		}
        	if (!dataExists(profile)) {
	        	InfoPointData ipd=new InfoPointData();
	        	ipd.id=id;    	
	        	ipd.postSender=postSender;
	        	ipd.postTitle=postTitle;
	        	ipd.postOriginText=postOriginText;
	        	ipd.postLink=postLink;    	
	        	ipd.postPluginData=postPluginData; 
	        	ipd.postSenderVocalizing=postSenderVocalizing;
	        	ipd.postTitleVocalizing=postTitleVocalizing;
	        	ipd.postTextVocalizing=postTextVocalizing;
	        	ipd.postArticle="";
	        	ipd.postLang=lang;
				ipd.postURL=postURL;
				ipd.pluginBmpPath=pluginBmpPath;
	        	String subStrs[]=str.split("[^[:alpha:]]+");
	        	int cnt=subStrs.length;
	        	for (int i=0;i<cnt;i++) {
	        		if (subStrs[i].length()>3) {
	        			// TODO add stemmig
	        			if (!ipd.postTokens.contains(subStrs[i]))
	        				ipd.postTokens.add(subStrs[i]);
	        		}
	        	}
	        	
	        	saveData(profile, ipd);
        		this.dataCreated=true;
        	}
    	}
    	
    	public boolean dataExists(String profile) {	    	
	    	File file=new File(saveDir+profile+File.separator+id);
	    	return file.exists();
    	}
    	
    	private void saveData(String profile, InfoPointData ipd) {
        	FileOutputStream fos;
			try {
				File dirToSave = new File(saveDir+profile+File.separator);
		    	dirToSave.mkdirs();
		    	
				fos = new FileOutputStream(saveDir+profile+File.separator+this.id);
	        	ObjectOutputStream oos;
				oos = new ObjectOutputStream(fos);
	        	oos.writeObject(ipd);
	        	oos.flush();
	        	oos.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
    	}

    	public void deleteData(String profile) {
			InfoPointData data = getData(profile);
			if (data!=null) {
				File postBMP = new File(data.postBmpPath);
				postBMP.delete();
				File file = new File(saveDir + profile + File.separator + this.id);
				file.delete();
			}
    	}
    	
    	public InfoPointData getData(String profile) {
			try {
	    		FileInputStream fis;
				fis = new FileInputStream(saveDir+profile+File.separator+this.id);
				ObjectInputStream oin = new ObjectInputStream(fis);
				InfoPointData ipd=(InfoPointData) oin.readObject();
				if (ipd!=null) {
					if (ipd.id==null) ipd.id=id;   
					if (ipd.postSender==null) ipd.postSender="";
			    	if (ipd.senderBmpPath==null) ipd.senderBmpPath="";
			    	if (ipd.postBmpPath==null) ipd.postBmpPath="";
			    	if (ipd.postTitle==null) ipd.postTitle="";
			    	if (ipd.postOriginText==null) ipd.postOriginText="";
			    	if (ipd.postLink==null) ipd.postLink="";    	
			    	if (ipd.postPluginData==null) ipd.postPluginData="";     	
			    	if (ipd.postSenderVocalizing==null) ipd.postSenderVocalizing="";
			    	if (ipd.postTitleVocalizing==null) ipd.postTitleVocalizing="";
			    	if (ipd.postTextVocalizing==null) ipd.postTextVocalizing="";
					if (ipd.postArticle==null) ipd.postArticle=""; 
					if (ipd.postLang==null) ipd.postLang="";
					if (ipd.postURL==null) ipd.postURL="";
					if (ipd.pluginBmpPath==null) ipd.pluginBmpPath="";
				}
				oin.close();
				return ipd;
			} catch (FileNotFoundException e) {
				//e.printStackTrace();
				return null;
			} catch (ClassNotFoundException e) {
				//e.printStackTrace();
				return null;
			} catch (IOException e) {
				//e.printStackTrace();
				return null;
			}
    	}
    	
    	private double getJaccardIndex(ArrayList<String> x, ArrayList<String> y) {
    		if( x.size() == 0 || y.size() == 0 ) {
                return 0.0;
            }
    		Set<String> unionXY = new HashSet<String>(x);
            unionXY.addAll(y);            
            Set<String> intersectionXY = new HashSet<String>(x);
            intersectionXY.retainAll(y);
            return (double) intersectionXY.size() / (double) unionXY.size();
    	}
    	
    	public double getMaxJaccardIndex(String profile){
    		double res=0;
    		InfoPointData ipd=getData(profile);
    		if (ipd!=null) {
    			
    	    	File dir = new File(saveDir+File.separator+profile);
    	    	String  name;
    	    	for(File f : dir.listFiles()) {
    	    		if(f.isFile()) {
    	    			name = f.getName();
    	    			if (!name.equals(ipd.id)) {
    	    				InfoPoint infopoint=new InfoPoint(name);
    	    				InfoPointData ipd1=infopoint.getData(profile);
    	    				if (ipd1!=null) {
    	    					double ji=getJaccardIndex(ipd.postTokens, ipd1.postTokens);
    	    					if (ji>res) res=ji;
    	    					if (res>filterJaccardIndex)
    	    						return res;
    	    				} else {
    	    				}
    	    			}    	    			
    	    		}
    	    	}

    		}
    		return res;
    	}
    	
    	public Bitmap getSenderBmp(String profile) {
    		InfoPointData data=getData(profile);
    		if (data!=null) {
        		if (data.senderBmpPath.equals(""))
        			return null;
        		else {
        			return BitmapFactory.decodeFile(data.senderBmpPath);
        		}
    		} else {
    			return null;
    		}
    	}

    	public void setSenderBmp(String profile, String dir, Bitmap bmp, String url) {
    		InfoPointData data=getData(profile);
    		if (data!=null) {
        		if (bmp==null) {
        			data.senderBmpPath="";
        			saveData(profile, data);
        			return;
        		}
        		
	        	String file="";
	        	if (url!=null&&!url.isEmpty()) {
	        		file=getHash(url);
	        	} else {
	        		file=getHash(BitMapToString(bmp));
	        	}
        		    		
        		data.senderBmpPath=dir+file;
				data.senderBmpURL=url;
        		try {
    				saveBitmapToSdcard(dir, file, bmp);
    			} catch (IOException e) {
    				data.senderBmpPath="";
    				e.printStackTrace();
    			}
        		saveData(profile, data);
    		}    		
    	}

    	public void setArticle(String profile, String article) {
    		if (article==null||article.equals("")) return;
    		InfoPointData data=getData(profile);
    		if (data!=null) {
        		data.postArticle=article;
        		saveData(profile, data);
    		}    		
    	}
    	
    	public Bitmap getPostBmp(String profile, int width, int height) {
    		InfoPointData data=getData(profile);
    		if (data!=null) {
	    		if (data.postBmpPath.equals(""))
	    			return null;
	    		else {
	    			return GetScaledBitmap(data.postBmpPath, width, height);//BitmapFactory.decodeFile(data.postBmpPath);
	    		}
    		} else {
    			return null;
    		}
    	}

    	public void setPostBmp(String profile, String dir, Bitmap bmp, String url) {
    		InfoPointData data=getData(profile);
    		if (data!=null) {    		
        		if (bmp==null) {
        			data.postBmpPath="";
        			saveData(profile, data);
        			return;
        		}
	        	String file;
	        	if (url!=null&&!url.isEmpty()) {
	        		file=getHash(url);
	        	} else {
	        		file=getHash(BitMapToString(bmp));
	        	}
	
	    		data.postBmpPath=dir+file;
				data.postBmpURL=url;
	    		try {
					saveBitmapToSdcard(dir, file, bmp);
				} catch (IOException e) {
					data.postBmpPath="";
					e.printStackTrace();
				}
	    		saveData(profile, data);
    		}
    	}
    	
        public void putToIntent(Intent intent) {
        	putToIntent(intent, -1);
        }
        
        private void putToIntent(Intent intent, int idx) {
        	String append="";
        	if (idx>-1) append="_"+idx;
        	intent.putExtra(PARAM_INFOPOINT_ID+append,  id);
        	intent.putExtra(PARAM_PROCESS_PLUGIN+append,  processingPlugin);
        	intent.putExtra(PARAM_INFOPOINT_POST_TIME+append,  (new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'")).format(postTime));
        	intent.putExtra(PARAM_INFOPOINT_PRIORITY+append,  priority);
        	intent.putExtra(PARAM_INFOPOINT_READARTICLE+append, readArticle);

			intent.putExtra(PARAM_INFOPOINT_TITLEEXISTS+append, titleExists);
			intent.putExtra(PARAM_INFOPOINT_TEXTEXISTS+append, textExists);
			intent.putExtra(PARAM_INFOPOINT_ARTICLEEXISTS+append, articleExists);
        }

        public static void putListToIntent(Intent intent, ArrayList<InfoPoint> infopoints) {
    		int cnt=infopoints.size();
    		intent.putExtra(PARAM_INFOPOINTS_COUNT, cnt);
    		for (int i=0; i<cnt;i++) {
    			InfoPoint infopoint=infopoints.get(i);
    			infopoint.putToIntent(intent, i);
    		}
        }
        
        public static InfoPoint getFromIntent(Intent intent) {
        	return getFromIntent(intent, -1);
        }
        
        private static InfoPoint getFromIntent(Intent intent, int idx) {
        	String append="";
        	if (idx>-1) append="_"+idx;
        	String id				= intent.getStringExtra(PARAM_INFOPOINT_ID+append);
            if (id==null||id.isEmpty()) return null;
        	String processingPlugin	= intent.getStringExtra(PARAM_PROCESS_PLUGIN+append);
        	Date postTime=new Date();
			try {
				postTime = (new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'")).parse(intent.getStringExtra(PARAM_INFOPOINT_POST_TIME + append));
			} catch (ParseException e) {}
        	int priority			= intent.getIntExtra(PARAM_INFOPOINT_PRIORITY+append, InfoPoint.PRIORITY_NORMAL);
        	boolean readArticle=intent.getBooleanExtra(PARAM_INFOPOINT_READARTICLE + append, false);
			boolean titleExists=intent.getBooleanExtra(PARAM_INFOPOINT_TITLEEXISTS+append, false);
			boolean textExists=intent.getBooleanExtra(PARAM_INFOPOINT_TEXTEXISTS+append, false);
			boolean articleExists=intent.getBooleanExtra(PARAM_INFOPOINT_ARTICLEEXISTS+append, false);
        	return new InfoPoint(id, processingPlugin, postTime, priority, readArticle, titleExists, textExists, articleExists);
        }

        public static ArrayList<InfoPoint> getListFromIntent(Intent intent) {
        	ArrayList<InfoPoint> infopoints=new ArrayList<>();
			int cnt = intent.getIntExtra(PARAM_INFOPOINTS_COUNT, 0);
			for (int i=0;i<cnt;i++) {				    	
		    	InfoPoint infopoint = getFromIntent(intent, i);
		    	infopoints.add(infopoint);
			}
        	return infopoints;
        }        
    }

	public static class SpeindSettings {
        public static class PostSettings {
            public boolean ask_before_post = true;
            public Map<String, Boolean> post_plugins_data = new HashMap<>();
        }

		public int infopoints_store_time=24*60*60;
		public int max_play_time=15*60;
		public boolean read_full_article=false;
		public boolean not_download_images_on_mobile_net=true;
		public boolean not_off_screen=false;
		public float speech_rate = 1.0f;

        public PostSettings post_settings = new PostSettings();

		SpeindSettings (int ipst, int mpt, boolean rfa, boolean diomn, boolean dos, float sr, PostSettings ps) {
			infopoints_store_time = ipst;
			max_play_time = mpt;
			read_full_article = rfa;
			not_download_images_on_mobile_net = diomn;
			not_off_screen = dos;
			speech_rate = sr;
            post_settings = ps;
		}
		
		public static SpeindSettings getFromIntent(Intent intent) {
			int ipst=intent.getIntExtra("infopoints_store_time", 24*60*60);
			int mpt=intent.getIntExtra("max_play_time", 15*60);
			boolean rfa=intent.getBooleanExtra("read_full_article", false);
			boolean diomn=intent.getBooleanExtra("not_download_images_on_mobile_net", true);
			boolean dos=intent.getBooleanExtra("not_off_screen", false);
			float sr=intent.getFloatExtra("speech_rate", 1.0f);
            PostSettings ps = new PostSettings();
            ps.ask_before_post = intent.getBooleanExtra("ask_before_post", true);
            int pdc = intent.getIntExtra("plugins_count", 0);
            for (int i=0;i<pdc; i++) {
                String plugin_package = intent.getStringExtra("plugin_package_"+i);
                boolean enable = intent.getBooleanExtra("plugin_post_enable_"+i, true);
                ps.post_plugins_data.put(plugin_package, enable);
            }
			if (ipst>0)
				return new SpeindSettings(ipst, mpt, rfa, diomn, dos, sr, ps);
			else 
				return null;
		}
		
		public void putToIntent(Intent intent) {
			intent.putExtra("infopoints_store_time", infopoints_store_time);			
			intent.putExtra("max_play_time", max_play_time);			
			intent.putExtra("read_full_article", read_full_article);					
			intent.putExtra("not_download_images_on_mobile_net", not_download_images_on_mobile_net);					
			intent.putExtra("not_off_screen", not_off_screen);
			intent.putExtra("speech_rate", speech_rate);
            intent.putExtra("ask_before_post", post_settings.ask_before_post);
            intent.putExtra("plugins_count", post_settings.post_plugins_data.size());
            int i=0;
            for (Map.Entry<String, Boolean> entry : post_settings.post_plugins_data.entrySet()) {
                intent.putExtra("plugin_package_"+i, entry.getKey());
                intent.putExtra("plugin_post_enable_"+i, entry.getValue());
                i++;
            }
		}
		
	}
	
    public static class SpeindData {
    	public String currentProfile="";
		public String service_package="";
    	public ArrayList<InfoPoint> infopoints = new ArrayList<>();
    	public int currentInfoPoint=0;
    	public SpeindSettings speindConfig=new SpeindSettings(24*60*60, 15*60, false, true, false, 1.0f, new SpeindSettings.PostSettings());
    	public ArrayList<DataFeedSettingsInfo> dataFeedsettingsInfos = new ArrayList<>();
    	public int state = -1;
    	
    	public void putToIintent(Intent intent) {
    		intent.putExtra(PARAM_PROFILE_NAME, currentProfile);
    		InfoPoint.putListToIntent(intent, infopoints);
    		intent.putExtra(PARAM_CURRENT_INFOPOINT, currentInfoPoint);
    		speindConfig.putToIntent(intent);
        	DataFeedSettingsInfo.putListToIntent(intent, dataFeedsettingsInfos);    		
    	}
    	
    	public void getFronIntent(Intent intent) {
    		this.currentProfile=intent.getStringExtra(PARAM_PROFILE_NAME);
    		this.infopoints=InfoPoint.getListFromIntent(intent);
    		this.currentInfoPoint=intent.getIntExtra(PARAM_CURRENT_INFOPOINT, -1);
    		this.speindConfig=SpeindSettings.getFromIntent(intent);
    		this.dataFeedsettingsInfos=DataFeedSettingsInfo.getListFromIntent(intent);
    	}

//    	public static SpeindData getFronIntent(Intent intent) {
//    		SpeindData res=new SpeindData();
//    		res.currentProfile=intent.getStringExtra(PARAM_PROFILE_NAME);
//    		res.infopoints=InfoPoint.getListFromIntent(intent);
//    		res.currentInfoPoint=intent.getIntExtra(PARAM_CURRENT_INFOPOINT, -1);
//    		res.speindConfig=SpeindSettings.getFromIntent(intent);
//    		res.dataFeedsettingsInfos=DataFeedSettingsInfo.getListFromIntent(intent);
//    		return res;
//    	}
    } 	
	
    public static class SpeindDataFeedData {

        private Map<String, DataFeedSettingsInfo>   settings            = new HashMap<>();
		private Map<String, String>                 profiles            = new HashMap<>();
		private Map<String, Long>                   storeInfopointTimes = new HashMap<>();
        private int                                 pr                  = 0;

		public ArrayList<String> langList=new ArrayList<>();
		public boolean loadImagesOnMobileInet=false;

        public void registerDataFeed(final Context context, final String service_package, final DataFeedSettingsInfo info) {
            settings.put(service_package, info);
            storeInfopointTimes.put(service_package, (long)(3*24*60*60*1000));
            executeIfSpeindStarted(context, service_package, new Runnable() {
                @Override
                public void run() {
                    Intent intent = new Intent();
                    intent.setClassName(service_package, SERVICE_NAME);
                    intent.putExtra(DATAFEED_SERVICE_CMD, DF_SC_DATAFEED_INFO);
                    info.putToIntent(intent);
                    context.startService(intent);
                }
            }, true);
        }

        public void unRegisterDataFeed(String service_package) {
            settings.remove(service_package);
            profiles.remove(service_package);
            storeInfopointTimes.remove(service_package);
        }

		public boolean setState(final Context context, final String service_package, int state, final boolean error) {
			log("SpeindDataFeedData.setState "+service_package+" "+state);

			if (settings.containsKey(service_package)) {
                final DataFeedSettingsInfo info = settings.get(service_package);
		    	if (state!=info.state||error) {
                    info.state = state;
		    		executeIfSpeindStarted(context, service_package, new Runnable(){
						@Override
						public void run() {
					    	Intent intent = new Intent();
					    	intent.setClassName(service_package, SERVICE_NAME);
					    	intent.putExtra(DATAFEED_SERVICE_CMD, DF_DATAFEED_INFO_CHANGED);
                            intent.putExtra(PARAM_ERROR, error);
                            info.putToIntent(intent);
					    	context.startService(intent);
						}
		    		},true);
			    	return true;
		    	}
	    	}
			return false;
		}
		
		//public int getState(String service_package){
		//	log("SpeindDataFeedData.getState");
        //    if (settings.containsKey(service_package)) {
        //        return settings.get(service_package).state;
        //    }
		//	return DataFeedSettingsInfo.DATAFEED_STATE_SUSPENDED;
		//}

        public void setReadConfirm(final Context context, final String service_package, boolean isNeedConfirmRead){
            if (settings.containsKey(service_package)) {
                final DataFeedSettingsInfo info = settings.get(service_package);
                if (info.isNeedConfirmRead!=isNeedConfirmRead) {
                    info.isNeedConfirmRead=isNeedConfirmRead;
                    executeIfSpeindStarted(context, service_package, new Runnable() {
                        @Override
                        public void run() {
                            Intent intent = new Intent();
                            intent.setClassName(service_package, SERVICE_NAME);
                            intent.putExtra(DATAFEED_SERVICE_CMD, DF_DATAFEED_INFO_CHANGED);
                            info.putToIntent(intent);
                            context.startService(intent);
                        }
                    }, true);
                }
            }
        }

        public void setProcessingState(final Context context, boolean isWorking){
            pr=pr+(isWorking ? 1 : -1);

            if (pr>0) {
                isWorking = true;
            } else {
                pr = 0;
                isWorking = false;
            }
            for (Map.Entry<String, DataFeedSettingsInfo> entry : settings.entrySet()) {
                final String service_package = entry.getKey();
                final DataFeedSettingsInfo info = entry.getValue();
                if (info.state!=DataFeedSettingsInfo.DATAFEED_STATE_SUSPENDED||!isWorking) {
                    if (info.isWorking!=isWorking) {
                        info.isWorking = isWorking;
                        executeIfSpeindStarted(context, service_package, new Runnable(){
                            @Override
                            public void run() {
                                Intent intent = new Intent();
                                intent.setClassName(service_package, SERVICE_NAME);
                                intent.putExtra(DATAFEED_SERVICE_CMD, DF_DATAFEED_INFO_CHANGED);
                                info.putToIntent(intent);
                                context.startService(intent);
                            }
                        },true);
                    }
                }
            }
        }

        public boolean setNeedAuthorization(final Context context, final String service_package, boolean isNeedAuthorization){
            if (settings.containsKey(service_package)) {
                final DataFeedSettingsInfo info = settings.get(service_package);
                if (info.isNeedAuthorization!=isNeedAuthorization) {
                    info.isNeedAuthorization=isNeedAuthorization;
                    executeIfSpeindStarted(context, service_package, new Runnable() {
                        @Override
                        public void run() {
                            Intent intent = new Intent();
                            intent.setClassName(service_package, SERVICE_NAME);
                            intent.putExtra(DATAFEED_SERVICE_CMD, DF_DATAFEED_INFO_CHANGED);
                            info.putToIntent(intent);
                            context.startService(intent);
                        }
                    }, true);
                    return true;
                }
            }
            return false;
        }

		public String getProfile(String service_package) {
			log("SpeindDataFeedData.getProfile");
			String profile = profiles.get(service_package);
			if (profile==null) profile="";
			return profile;
		}

		public long getStoreInfopointTime(String service_package) {
			Long res = storeInfopointTimes.get(service_package);
			if (res==null) res = (long)3*24*60*60*1000;
			return res;
		}

		public long getMaxStoreInfopointTime() {
			Long res = (long)0;
			Collection<Long> storeTimes = (new HashMap<>(storeInfopointTimes)).values();
			for (Long storeTime : storeTimes) {
				if (storeTime>res) {
					res=storeTime;
				}
			}
			return res;
		}

    }

	public static class LooperThread extends Thread {
        public Handler mHandler = null;
		private String name = "worker";
        public void run() {
        	setName(name);
            Looper.prepare();
            mHandler = new Handler();
            Looper.loop();
        }
		public void setThreadName(String threadName) {
			name = threadName;
		}
    }
    
 // ------------------------------------------------------------------------------------
    

    public static class SendInfoPointParams {
    	public Date postTime = new Date();
    	public String postSender = "";
    	public Bitmap senderBmp = null;
    	public String senderBmpURL = "";
    	public Bitmap postBmp = null;
    	public String postBmpURL = "";
    	public String postTitle = "";
    	public String postOriginText = "";
    	public String postLink = "";
    	public String postPluginData = "";
    	public String postSenderVocalizing = "";
    	public String postTitleVocalizing = "";
    	public String postTextVocalizing = "";
    	public String lang = "";
    	public boolean checkForDuplicate = true;
		public int priority = InfoPoint.PRIORITY_NORMAL;
    	public boolean readArticle = false;
    	public boolean startRead = false;
		public String postURL = "";
    }
    
    public static void sendInfoPoint(final Context context, final String service_package, String mProfile, ArrayList<String> langList, DataFeedSettingsInfo dfsi, final SendInfoPointParams sendInfoPointParams, boolean loadImagesOnMobileInet) {
    	if (context==null||dfsi==null||sendInfoPointParams==null) return;
    	
    	boolean langFound=false;
    	for (String lang : langList) {
			if (lang.compareToIgnoreCase(sendInfoPointParams.lang)==0) {
				langFound=true;
			}
		}
    	
    	if (langFound) {
	    	final InfoPoint infopoint=new InfoPoint(mProfile, context.getPackageName(), sendInfoPointParams.postTime, sendInfoPointParams.postSender, sendInfoPointParams.postTitle, sendInfoPointParams.postOriginText, sendInfoPointParams.postLink, sendInfoPointParams.postPluginData, sendInfoPointParams.postSenderVocalizing, sendInfoPointParams.postTitleVocalizing, sendInfoPointParams.postTextVocalizing, sendInfoPointParams.lang, sendInfoPointParams.priority, sendInfoPointParams.readArticle, sendInfoPointParams.postURL, dfsi.bmpPath);
	    	if (infopoint.dataCreated) {
	    		double maxJaccard=0;
	    		if (sendInfoPointParams.checkForDuplicate) maxJaccard=infopoint.getMaxJaccardIndex(mProfile);
	    		if (maxJaccard<=InfoPoint.filterJaccardIndex) {
	    			
					String article=getArticleFromURL(sendInfoPointParams.postLink);
					
			    	infopoint.setArticle(mProfile, article);
	    			
			    	if (!sendInfoPointParams.postBmpURL.equals("")||!sendInfoPointParams.senderBmpURL.equals("")) {
				    	int netType=getConnectionStatus(context);
				    	boolean loadImages=loadImagesOnMobileInet||netType==ConnectivityManager.TYPE_WIFI||netType==ConnectivityManager.TYPE_WIMAX||netType==8;
					    	
				    	if (loadImages) {
					    	infopoint.setSenderBmp(mProfile, SPEIND_IMAGES_DIR, getBitmapFromURL(context, sendInfoPointParams.senderBmpURL, !loadImages), sendInfoPointParams.senderBmpURL);
					    	infopoint.setPostBmp(mProfile, SPEIND_IMAGES_DIR, getBitmapFromURL(context, sendInfoPointParams.postBmpURL, !loadImages), sendInfoPointParams.postBmpURL);
				    	} else {
					    	infopoint.setSenderBmp(mProfile, SPEIND_IMAGES_DIR, sendInfoPointParams.senderBmp, "");
					    	infopoint.setPostBmp(mProfile, SPEIND_IMAGES_DIR, sendInfoPointParams.postBmp, "");			    		
				    	}
			    	}	
			    	executeIfSpeindStarted(context, service_package, new Runnable(){
						@Override
						public void run() {
					    	Intent intent = new Intent();
					    	intent.setClassName(service_package, SERVICE_NAME);
					    	intent.putExtra(DATAFEED_SERVICE_CMD, DF_SC_NEW_INFO_POINT);
					    	infopoint.putToIntent(intent);
					    	if (sendInfoPointParams.startRead) {
						    	intent.putExtra("start_read", true);
					    	}
					    	context.startService(intent);
						}
					},true);
	    		} else {
	    			infopoint.deleteData(mProfile);
	    		}
	    	}
    	}
    }

    public static void sendInfoPoint(Context context, String service_package, SpeindDataFeedData speindDFData, SendInfoPointParams sendInfoPointParams) {
    	if (speindDFData==null) return;
    	sendInfoPoint(context, service_package, speindDFData.getProfile(service_package), speindDFData.langList, speindDFData.settings.get(service_package), sendInfoPointParams, speindDFData.loadImagesOnMobileInet);
    }
    
    public static String getLang(String fulltext, SpeindDataFeedData speindDFData) {
    	String lang=SpeindAPI.getLang(fulltext, speindDFData.langList);
    	if (lang.equals("")) {
    		ArrayList<String> enAr=new ArrayList<>();
    		enAr.add("en");
    		lang=SpeindAPI.getLang(fulltext, enAr);
    	}
    	return lang;
	}
   
 // ------------------------------------------------------------------------------------
    public interface SpeindDataFeedReceiverListener {
        boolean isAuthorized(String servicePackage);
	 	DataFeedSettingsInfo onInit(String servicePackage);
		void onSetProfile(String servicePackage, String profile);
		void onStart(String servicePackage, int state);
		void onSuspend(String servicePackage);
		void onResume(String servicePackage);
		void onStop(String servicePackage);
		void onShowSettings(String servicePackage);
		void onLike(String servicePackage, InfoPoint infopoint);
        void onPost(String servicePackage, InfoPoint infopoint);
		void onInfoPointDetails(String servicePackage, InfoPoint infopoint);
		void onLangListChanged(String servicePackage, ArrayList<String> langs);
		void onLoadImagesOnMobileInetChanged(String servicePackage, boolean loadImages);
		void onStoreInfopointTimeChanged(String servicePackage, long store_time);
	}
    
    public static class SpeindDataFeedReceiver extends BroadcastReceiver {
    	public LooperThread worker;
    	
    	private Handler handler = new Handler();
    	private SpeindDataFeedReceiverListener mListener=null;
    	public SpeindDataFeedData speindData = null;
    	
    	SpeindDataFeedReceiver(SpeindDataFeedReceiverListener listener, SpeindDataFeedData speindDFData){
			worker = new LooperThread();
			worker.setThreadName("SDFworker");
	    	worker.start();
	    	
    		mListener=listener;
    		speindData=speindDFData;
    		if (speindData==null) {
    			speindData=new SpeindDataFeedData();
    		}
    	}

		@Override
		public void onReceive(final Context context, final Intent intent) {
			if (context==null||intent==null) return;
			if (worker.mHandler==null) {
				handler.postDelayed(new Runnable(){
					@Override
					public void run() {
						onReceive(context, intent);
					}
				}, 1000);
			} else {	
				worker.mHandler.post(new Runnable(){
					@Override
					public void run() {
                        log("SpeindDataFeedReceiver.onReceive");
                        int cmd=intent.getIntExtra(DATAFEED_CLIENT_CMD, -1);
						final String service_package = intent.getStringExtra(PARAM_SERVICE_PACKAGE_NAME);
						switch (cmd) {
						case DF_CC_INIT:
							if (intent.getStringExtra(PARAM_PACKAGE_NAME).equals(context.getPackageName())) {
								log("DF_CC_INIT " + service_package + " " + context.getPackageName());
								handler.post(new Runnable(){
									@Override
									public void run() {
                                        DataFeedSettingsInfo info = mListener.onInit(service_package);
										if (info!=null) {
                                            if (info.isNeedAuthorization) {
                                                info.isWorking = false;
                                                info.state = DataFeedSettingsInfo.DATAFEED_STATE_SUSPENDED;
                                            }
                                            speindData.registerDataFeed(context, service_package, info);
										}
									}
								});
							}
							break;
						case DF_CC_STOP: {
							log("DF_CC_STOP " + service_package);
								speindData.unRegisterDataFeed(service_package);
								handler.post(new Runnable() {
									@Override
									public void run() {
										mListener.onStop(service_package);
									}
								});
							}
							break;
						case DF_CC_SET_PROFILE: {
								log("DF_CC_SET_PROFILE "+service_package);
								speindData.profiles.put(service_package, intent.getStringExtra(PARAM_PROFILE_NAME));
								final DataFeedSettingsInfo info = DataFeedSettingsInfo.getFromIntent(intent);
								if (info.packageName.equals(context.getPackageName())) {
									handler.post(new Runnable() {
										@Override
										public void run() {
											mListener.onSetProfile(service_package, intent.getStringExtra(PARAM_PROFILE_NAME));
											if (info.isNeedAuthorization()&&mListener.isAuthorized(service_package)) {
												speindData.setNeedAuthorization(context, service_package, false);
											}
                                            if (info.isNeedAuthorization&&info.state==DataFeedSettingsInfo.DATAFEED_STATE_READY&&!mListener.isAuthorized(service_package)) {
                                                info.state = DataFeedSettingsInfo.DATAFEED_STATE_SUSPENDED;
                                            }
                                            worker.mHandler.post(new Runnable() {
                                                @Override
                                                public void run() {
                                                    speindData.setState(context, service_package, info.state, false);
                                                }
                                            });
                                            handler.post(new Runnable() {
                                                @Override
                                                public void run() {
                                                    mListener.onStart(service_package, info.state);
                                                }
                                            });
										}
									});
								}
							}
							break;
						case DF_CC_SUSPEND: {
								log("DF_CC_SUSPEND " + service_package);
								DataFeedSettingsInfo info1=DataFeedSettingsInfo.getFromIntent(intent);
								if (info1.packageName.equals(context.getPackageName())) {
									if (speindData.setState(context, service_package, DataFeedSettingsInfo.DATAFEED_STATE_SUSPENDED, false)) {
										handler.post(new Runnable(){
											@Override
											public void run() {
												mListener.onSuspend(service_package);
											}
										});
									}
								}
							}
							break;
						case DF_CC_RESUME: {
								log("DF_CC_RESUME " + service_package);
								final DataFeedSettingsInfo info1 = DataFeedSettingsInfo.getFromIntent(intent);
								if (info1.packageName.equals(context.getPackageName())) {
                                    handler.post(new Runnable() {
                                        @Override
                                        public void run() {
                                            if (mListener.isAuthorized(service_package)) {
                                                worker.mHandler.post(new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        speindData.setNeedAuthorization(context, service_package, false);
                                                        if (speindData.setState(context, service_package, DataFeedSettingsInfo.DATAFEED_STATE_READY, false)) {
                                                            handler.post(new Runnable() {
                                                                @Override
                                                                public void run() {
                                                                    mListener.onResume(service_package);
                                                                }
                                                            });
                                                        }
                                                    }
                                                });
                                            } else {
                                                worker.mHandler.post(new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        speindData.setState(context, service_package, DataFeedSettingsInfo.DATAFEED_STATE_SUSPENDED, true);
                                                    }
                                                });
                                            }
                                        }
                                    });
								}
							}
							break;
						case DF_CC_DATA_FEED_SETTINGS_REQUEST: {
								log("DF_CC_DATA_FEED_SETTINGS_REQUEST "+service_package);
								if (DataFeedSettingsInfo.getFromIntent(intent).packageName.equals(context.getPackageName())) {
									handler.post(new Runnable() {
										@Override
										public void run() {
											mListener.onShowSettings(service_package);
										}
									});
								}
							}
							break;
						case DF_CC_LIKE: {
								log("DF_CC_LIKE "+service_package);
                                if (DataFeedSettingsInfo.getFromIntent(intent).packageName.equals(context.getPackageName())) {
                                    final InfoPoint infopoint = InfoPoint.getFromIntent(intent);
                                    if (infopoint != null) {
                                        handler.post(new Runnable() {
                                            @Override
                                            public void run() {
                                                mListener.onLike(service_package, infopoint);
                                            }
                                        });
                                    }
                                }
							}
							break;
                        case DF_CC_POST: {
                                log("DF_CC_POST "+service_package+" "+DataFeedSettingsInfo.getFromIntent(intent).packageName);
                                if (DataFeedSettingsInfo.getFromIntent(intent).packageName.equals(context.getPackageName())) {
                                    final InfoPoint infopoint = InfoPoint.getFromIntent(intent);
                                    if (infopoint != null) {
                                        handler.post(new Runnable() {
                                            @Override
                                            public void run() {
                                                mListener.onPost(service_package, infopoint);
                                            }
                                        });
                                    }
                                }
                            }
                            break;
						case DF_CC_INFOPOINT_DETAILS:
							log("DF_CC_INFOPOINT_DETAILS "+service_package);
							final InfoPoint infopoint1=InfoPoint.getFromIntent(intent);
							if (infopoint1!=null) {
								if (infopoint1.processingPlugin.equals(context.getPackageName())) {
									handler.post(new Runnable(){
										@Override
										public void run() {
											mListener.onInfoPointDetails(service_package, infopoint1);
										}
									});
								}
							}
							break;
						case DF_CC_LOAD_IMAGES_CHANGED:
							log("DF_CC_LOAD_IMAGES_CHANGED "+service_package);
							final boolean loadImages=intent.getBooleanExtra(PARAM_LOAD_IMAGES, false);
							if (speindData.loadImagesOnMobileInet!=loadImages) {
								speindData.loadImagesOnMobileInet=loadImages;
								if (!speindData.getProfile(service_package).equals("")) {
									handler.post(new Runnable(){
										@Override
										public void run() {
											mListener.onLoadImagesOnMobileInetChanged(service_package, loadImages);
										}
									});
								}
							}
							break;
						case DF_CC_STORE_INFOPOINTS_TIME_CHANGED:
							log("DF_CC_STORE_INFOPOINTS_TIME_CHANGED "+service_package);
							final long store_time=intent.getLongExtra(PARAM_STORE_TIME, 3 * 24 * 60 * 60 * 1000);
							if (speindData.getStoreInfopointTime(service_package)!=store_time) {
								speindData.storeInfopointTimes.put(service_package, store_time);
								if (!speindData.getProfile(service_package).equals("")) {
									handler.post(new Runnable(){
										@Override
										public void run() {
											mListener.onStoreInfopointTimeChanged(service_package, store_time);
										}
									});
								}
							}
							break;
						case DF_CC_LANG_LIST_CHANGED:
							log("DF_CC_LANG_LIST_CHANGED "+service_package);
							final ArrayList<String> langs=intent.getStringArrayListExtra(PARAM_LANG_LIST);
							String package_name = intent.getStringExtra(PARAM_PACKAGE_NAME);
							if (package_name!=null&&(package_name.equals("")||package_name.equals(context.getPackageName()))) {
								if (speindData.langList.size()!=langs.size()) {
									speindData.langList=langs;
									if (!speindData.getProfile(service_package).equals("")) {
										handler.post(new Runnable(){
											@Override
											public void run() {
												mListener.onLangListChanged(service_package, langs);
											}
										});
									}
								} else {
									for (String lang : speindData.langList) {
										boolean found=false;
										for (String lang1 : langs) {
											if (lang1.equals(lang)) {
												found=true;
												break;
											}
										}
										if (!found) {
											speindData.langList=langs;
											if (!speindData.getProfile(service_package).equals("")) {
												handler.post(new Runnable(){
													@Override
													public void run() {
														mListener.onLangListChanged(service_package, langs);
													}
												});								}
											break;
										}
									}
								}
							}
							break;
						default:
							break;
						}
					}
				});
			}
		}
    }
    
	public static abstract class DatafeedService extends Service implements SpeindDataFeedReceiverListener {
		public SpeindDataFeedReceiver receiver;		
		public SpeindDataFeedData speindDFData=new SpeindDataFeedData();
		@Override
	    public void onCreate() {
			super.onCreate();
			receiver = new SpeindDataFeedReceiver(this, speindDFData);
			IntentFilter intFilt = new IntentFilter(SpeindAPI.BROADCAST_ACTION);
			registerReceiver(receiver, intFilt);
		}
		
		@Override
	    public void onDestroy() {
			unregisterReceiver(receiver);
			super.onDestroy();
		}
	    	
	    public void setReadConfirm(String service_package, boolean readConfirm){
            speindDFData.setReadConfirm(this, service_package, readConfirm);
	    }

	    public void setProcessingState(boolean processing){
            speindDFData.setProcessingState(this, processing);
	    }
	        
	    public void sendInfoPoint(String service_package, SendInfoPointParams sendInfoPointParams) {
	    	SpeindAPI.sendInfoPoint(this, service_package, speindDFData, sendInfoPointParams);
	    }
	    
	    public String getLang(String fulltext) {
	    	return SpeindAPI.getLang(fulltext, speindDFData);
		}
		
	}

// -----------------------------------------------------------------------
	public static Intent createIntent(String service_package) {
		Intent intent = new Intent();
		intent.setClassName(service_package, SERVICE_NAME);
		return intent;
	}
	
	public static Intent createStopSpeindServiceIntent(String service_package) {
		Intent intent=createIntent(service_package);
		intent.putExtra(SpeindAPI.SERVICE_CMD, SpeindAPI.SC_QUIT);
		return intent;
	}
	
	public static Intent createSetProfileIntent(String service_package, String login, String password) {
		Intent intent=createIntent(service_package);
    	intent.putExtra(SpeindAPI.SERVICE_CMD, SpeindAPI.SC_SET_PROFILE);
    	intent.putExtra(SpeindAPI.PARAM_PROFILE_NAME, login);
    	intent.putExtra(SpeindAPI.PARAM_PROFILE_PASS, password);
		return intent;
	}
	
	public static Intent createLikeIntent(String service_package, InfoPoint infopoint) {
		Intent intent=createIntent(service_package);
    	intent.putExtra(SpeindAPI.SERVICE_CMD, SpeindAPI.SC_LIKE);
    	infopoint.putToIntent(intent);
		return intent;
	}

    public static Intent createPostIntent(String service_package, InfoPoint infopoint) {
        Intent intent=createIntent(service_package);
        intent.putExtra(SpeindAPI.SERVICE_CMD, SpeindAPI.SC_POST);
        infopoint.putToIntent(intent);
        return intent;
    }

    public static Intent createPlayIntent(String service_package) {
		Intent intent=createIntent(service_package);
    	intent.putExtra(SpeindAPI.SERVICE_CMD, SpeindAPI.SC_PLAY);
    	return intent;
    }
    
    public static Intent createPlayPauseIntent(String service_package) {
		Intent intent=createIntent(service_package);
    	intent.putExtra(SpeindAPI.SERVICE_CMD, SpeindAPI.SC_PLAY_PAUSE);
    	return intent;
    }

    public static Intent createPlayInfoPointIntent(String service_package, int pos) {
		Intent intent=createIntent(service_package);
    	intent.putExtra(SpeindAPI.SERVICE_CMD, SpeindAPI.SC_PLAY_INFOPOINT);
    	intent.putExtra(SpeindAPI.PARAM_INFOPOINT_POS, pos);
    	return intent;												
    }

    public static Intent createPlayUserAudioFileIntent(String service_package, String fileName) {
    	Intent intent=createIntent(service_package);
    	intent.putExtra(SpeindAPI.SERVICE_CMD, SpeindAPI.SC_PLAY_USER_FILE);
    	intent.putExtra(SpeindAPI.PARAM_FILE_NAME, fileName);
    	return intent;
    }

    public static Intent createPlayUserTextIntent(String service_package, String text) {
    	Intent intent=createIntent(service_package);
    	intent.putExtra(SpeindAPI.SERVICE_CMD, SpeindAPI.SC_PLAY_USER_TEXT);
    	intent.putExtra(SpeindAPI.PARAM_USER_TEXT, text);
    	return intent;
    }
    
    public static Intent createStopIntent(String service_package) {
		Intent intent=createIntent(service_package);
    	intent.putExtra(SpeindAPI.SERVICE_CMD, SpeindAPI.SC_STOP);
    	return intent;
    }

	public static Intent createPauseIntent(String service_package) {
		Intent intent=createIntent(service_package);
		intent.putExtra(SpeindAPI.SERVICE_CMD, SpeindAPI.SC_PAUSE);
		return intent;
	}

	public static Intent createResumeIntent(String service_package) {
		Intent intent=createIntent(service_package);
		intent.putExtra(SpeindAPI.SERVICE_CMD, SpeindAPI.SC_RESUME);
		return intent;
	}

    public static Intent createReplayIntent(String service_package) {
		Intent intent=createIntent(service_package);
    	intent.putExtra(SpeindAPI.SERVICE_CMD, SpeindAPI.SC_REPLAY);
    	return intent;
    }

    public static Intent createNextIntent(String service_package) {
		Intent intent=createIntent(service_package);
    	intent.putExtra(SpeindAPI.SERVICE_CMD, SpeindAPI.SC_NEXT);
    	return intent;
    }
   
    public static Intent createPrevIntent(String service_package) {
		Intent intent=createIntent(service_package);
    	intent.putExtra(SpeindAPI.SERVICE_CMD, SpeindAPI.SC_PREV);
    	return intent;
    }
   
    public static Intent createReadCurrentInfopointArticleIntent(String service_package) {
		Intent intent=createIntent(service_package);
    	intent.putExtra(SpeindAPI.SERVICE_CMD, SpeindAPI.SC_READ_CURRENT_INFOPOINT_ARTICLE);
    	return intent;
    }
   
    public static Intent createSkipNewsIntent(String service_package) {
		Intent intent=createIntent(service_package);
    	intent.putExtra(SpeindAPI.SERVICE_CMD, SpeindAPI.SC_SKIPNEWS);
    	return intent;
    }
    
    public static Intent createSaveSettingsIntent(String service_package, SpeindSettings config) {
		Intent intent=createIntent(service_package);
    	intent.putExtra(SpeindAPI.SERVICE_CMD, SpeindAPI.SC_SETTINGS_CHANGED);
    	config.putToIntent(intent);
    	return intent;												
    }
    
    public static Intent createInfoPointDetaisRequestIntent(String service_package) {
		Intent intent=createIntent(service_package);
    	intent.putExtra(SpeindAPI.SERVICE_CMD, SpeindAPI.SC_INFOPOINT_DETAILS);
    	return intent;
    }

	public static Intent createOpenPinboardRequestIntent(String service_package) {
		Intent intent=createIntent(service_package);
		intent.putExtra(SpeindAPI.SERVICE_CMD, SpeindAPI.SC_PINBOARD_REQUEST);
		return intent;
	}

	public static Intent createOpenPinboardSettingsRequestIntent(String service_package) {
		Intent intent=createIntent(service_package);
		intent.putExtra(SpeindAPI.SERVICE_CMD, SpeindAPI.SC_PINBOARD_SETTINGS_REQUEST);
		return intent;
	}

	public static Intent createAddToPinboardIntent(String service_package, String infopointID) {
		Intent intent=createIntent(service_package);
		intent.putExtra(SpeindAPI.SERVICE_CMD, SpeindAPI.SC_ADD_TO_PINBOARD);
		intent.putExtra(SpeindAPI.PARAM_INFOPOINT_ID, infopointID);
		return intent;
	}

	public static Intent createRemoveFromPinboardIntent(String service_package, String infopointID) {
		Intent intent=createIntent(service_package);
		intent.putExtra(SpeindAPI.SERVICE_CMD, SpeindAPI.SC_REMOVE_FROM_PINBOARD);
		intent.putExtra(SpeindAPI.PARAM_INFOPOINT_ID, infopointID);
		return intent;
	}

	public static Intent createClearPinboardIntent(String service_package) {
		Intent intent=createIntent(service_package);
		intent.putExtra(SpeindAPI.SERVICE_CMD, SpeindAPI.SC_CLEAR_PINBOARD);
		return intent;
	}

	public static Intent createSendPinboardIntent(String service_package, String email, int format, boolean clearAfter) {
		Intent intent=createIntent(service_package);
		intent.putExtra(SpeindAPI.SERVICE_CMD, SpeindAPI.SC_SEND_PINBOARD);
		intent.putExtra(PARAM_EMAIL, email);
		intent.putExtra(PARAM_FORMAT, format);
		intent.putExtra(PARAM_CLEAR, clearAfter);
		return intent;
	}

    public static Intent createOpenSettingsRequestIntent(String service_package) {
		Intent intent=createIntent(service_package);
    	intent.putExtra(SpeindAPI.SERVICE_CMD, SpeindAPI.SC_SETTINGS_REQUEST);
    	return intent;
    }
    
    public static Intent createOpenVoicesSettingsRequestIntent(String service_package) {
		Intent intent=createIntent(service_package);
		intent.putExtra(SpeindAPI.SERVICE_CMD, SpeindAPI.SC_VOICES_SETTINGS_REQUEST);
    	return intent;
    }

    public static Intent createBuyVoiceRequestIntent(String service_package, String code) {
		Intent intent=createIntent(service_package);
		intent.putExtra(SpeindAPI.SERVICE_CMD, SpeindAPI.SC_BUY_VOICE_REQUEST);
		intent.putExtra(SpeindAPI.PARAM_VOICE_CODE, code);
    	return intent;
    }
    
    public static Intent createDownloadVoiceRequestIntent(String service_package, String code, boolean wifiOnly) {
		Intent intent=createIntent(service_package);
		intent.putExtra(SpeindAPI.SERVICE_CMD, SpeindAPI.SC_DOWNLOAD_VOICE_REQUEST);
		intent.putExtra(SpeindAPI.PARAM_VOICE_CODE, code);
		intent.putExtra(SpeindAPI.PARAM_WIFI_ONLY, wifiOnly);
    	return intent;
    }

    public static Intent createSetDefaultVoiceRequestIntent(String service_package, String code) {
		Intent intent=createIntent(service_package);
		intent.putExtra(SpeindAPI.SERVICE_CMD, SpeindAPI.SC_SET_DEFAULT_VOICE_REQUEST);
		intent.putExtra(SpeindAPI.PARAM_VOICE_CODE, code);
    	return intent;
    }
    
    public static Intent createPluginSettingsIntent(String service_package, DataFeedSettingsInfo info) {
    	Intent intent=createIntent(service_package);
    	intent.putExtra(SpeindAPI.SERVICE_CMD, SpeindAPI.SC_DATA_FEED_SETTINGS_REQUEST);
		info.putToIntent(intent);
		return intent;
    }

    public static Intent createResumePluginIntent(String service_package, DataFeedSettingsInfo info) {
    	Intent intent=createIntent(service_package);
    	intent.putExtra(SpeindAPI.SERVICE_CMD, SpeindAPI.SC_DATA_FEED_RESUME_REQUEST);
		info.putToIntent(intent);
		return intent;
    }

    public static Intent createSuspendPluginIntent(String service_package, DataFeedSettingsInfo info) {
    	Intent intent=createIntent(service_package);
    	intent.putExtra(SpeindAPI.SERVICE_CMD, SpeindAPI.SC_DATA_FEED_SUSPEND_REQUEST);
		info.putToIntent(intent);
		return intent;
    }
    
    public static Intent createRestorePurchasesRequestIntent(String service_package, Boolean wifiOnly) {
		Intent intent=createIntent(service_package);
		intent.putExtra(SpeindAPI.SERVICE_CMD, SpeindAPI.SC_RESTORE_PURCHASES);
		intent.putExtra(SpeindAPI.PARAM_WIFI_ONLY, wifiOnly);
    	return intent;
    }

    public static Intent createSetPlayerOnlyMode(String service_package, Boolean playerOnly) {
		Intent intent=createIntent(service_package);
		intent.putExtra(SpeindAPI.SERVICE_CMD, SpeindAPI.SC_PLAYER_ONLY);
		intent.putExtra(SpeindAPI.PARAM_PLAYER_ONLY, playerOnly);
    	return intent;
    }
    
	public static void stopSpeindService(Context context, String service_package) {
		Intent intent=createStopSpeindServiceIntent(service_package);
		context.startService(intent);
	}

    public static void setProfile(Context context, String service_package, String login, String password) {
		Intent intent=createSetProfileIntent(service_package, login, password);
		context.startService(intent);												    												
    }
	
    public static void like(Context context, String service_package, InfoPoint infopoint) {
		Intent intent=createLikeIntent(service_package, infopoint);
		context.startService(intent);												
    }

    public static void post(Context context, String service_package, InfoPoint infopoint) {
        Intent intent=createPostIntent(service_package, infopoint);
        context.startService(intent);
    }

    public static void play(Context context, String service_package) {
		Intent intent=createPlayIntent(service_package);
		context.startService(intent);												
    }
    
    public static void playPause(Context context, String service_package) {
		Intent intent=createPlayPauseIntent(service_package);
		context.startService(intent);												
    }
    
    public static void playInfoPoint(Context context, String service_package, int pos) {
		Intent intent=createPlayInfoPointIntent(service_package, pos);
		context.startService(intent);	
    }
        
    public static void playUserAudioFile(Context context, String service_package, String fileName) {
		Intent intent=createPlayUserAudioFileIntent(service_package, fileName);
		context.startService(intent);												
    }
    
    public static void playUserText(Context context, String service_package, String text) {
		Intent intent=createPlayUserTextIntent(service_package, text);
		context.startService(intent);												
    }
    
    public static void stop(Context context, String service_package) {
    	Intent intent=createStopIntent(service_package);
    	context.startService(intent);												
    }

	public static void pause(Context context, String service_package) {
		Intent intent=createPauseIntent(service_package);
		context.startService(intent);
	}

	public static void resume(Context context, String service_package) {
		Intent intent=createResumeIntent(service_package);
		context.startService(intent);
	}

    public static void replay(Context context, String service_package) {
		Intent intent=createReplayIntent(service_package);
		context.startService(intent);												
    }

    public static void next(Context context, String service_package) {
		Intent intent=createNextIntent(service_package);
		context.startService(intent);												
    }
    
    public static void prev(Context context, String service_package) {
		Intent intent=createPrevIntent(service_package);
		context.startService(intent);												
    }

    public static void readCurrentInfopointArticle(Context context, String service_package) {
		Intent intent=createReadCurrentInfopointArticleIntent(service_package);
		context.startService(intent);												
    }
    
    public static void skipNews(Context context, String service_package) {
		Intent intent=createSkipNewsIntent(service_package);
		context.startService(intent);												
    }

    public static void saveSettings(Context context, String service_package, SpeindSettings config) {
		Intent intent=createSaveSettingsIntent(service_package, config);
		context.startService(intent);												
    }

    public static void infoPointDetaisRequest(Context context, String service_package) {
		Intent intent=createInfoPointDetaisRequestIntent(service_package);
		context.startService(intent);												
    }

	public static void openPinboardRequest(Context context, String service_package) {
		Intent intent=createOpenPinboardRequestIntent(service_package);
		context.startService(intent);
	}

	public static void openPinboardSettingsRequest(Context context, String service_package) {
		Intent intent=createOpenPinboardSettingsRequestIntent(service_package);
		context.startService(intent);
	}

	public static void addToPinboard(Context context, String service_package, String infopointID) {
		Intent intent=createAddToPinboardIntent(service_package, infopointID);
		context.startService(intent);
	}

	public static void removeFromPinboard(Context context, String service_package, String infopointID) {
		Intent intent=createRemoveFromPinboardIntent(service_package, infopointID);
		context.startService(intent);
	}

	public static void clearPinboard(Context context, String service_package) {
		Intent intent=createClearPinboardIntent(service_package);
		context.startService(intent);
	}

	public static void sendPinboard(Context context, String service_package, String email, int format, boolean clearAfter) {
		Intent intent=createSendPinboardIntent(service_package, email, format, clearAfter);
		context.startService(intent);
	}

    public static void openSettingsRequest(Context context, String service_package) {
		Intent intent=createOpenSettingsRequestIntent(service_package);
		context.startService(intent);												
    }
    
    public static void openVoicesSettingsRequest(Context context, String service_package) {
		Intent intent=createOpenVoicesSettingsRequestIntent(service_package);
		context.startService(intent);												
    }

    public static void downloadVoiceRequest(Context context, String service_package, String code, boolean wifiOnly) {
		Intent intent=createDownloadVoiceRequestIntent(service_package, code, wifiOnly);
		context.startService(intent);												
    }	    

    public static void setDefaultVoiceRequest(Context context, String service_package, String code) {
		Intent intent=createSetDefaultVoiceRequestIntent(service_package, code);
		context.startService(intent);												
    }	    
    
    public static void buyVoiceRequest(Context context, String service_package, String code) {
		Intent intent=createBuyVoiceRequestIntent(service_package, code);
		context.startService(intent);												
    }	   

	public static void requestPluginSettings(Context context, String service_package, DataFeedSettingsInfo info) {
		Intent intent=createPluginSettingsIntent(service_package, info);
		context.startService(intent);		
	}
    
	public static void requestResumePlugin(Context context, String service_package, DataFeedSettingsInfo info) {
		Intent intent=createResumePluginIntent(service_package, info);
		context.startService(intent);		
	}

	public static void requestSuspendPlugin(Context context, String service_package, DataFeedSettingsInfo info) {
		Intent intent=createSuspendPluginIntent(service_package, info);
		context.startService(intent);		
	}
	
	public static void restorePurchasesRequest(Context context, String service_package, boolean wifiOnly) {
		Intent intent=createRestorePurchasesRequestIntent(service_package, wifiOnly);
		context.startService(intent);		
	}

	public static void setPlayerOnlyMode(Context context, String service_package, boolean playerOnly) {
		Intent intent=createSetPlayerOnlyMode(service_package, playerOnly);
		context.startService(intent);		
	}
	
// -----------------------------------------------------------------------
    public interface SpeindSettingsChangeListener {
        void onSettingsChanged(SpeindSettings oldConfig, SpeindSettings newConfig);
        void onDataFeedsListChanged();

    }

    public static class SpeindSettingsReceiver extends BroadcastReceiver {
        private SpeindSettingsChangeListener mListener=null;
        private ArrayList<DataFeedSettingsInfo> mDataFeedsettingsInfos=null;
        private SpeindSettings mConfig = null;

        public SpeindSettingsReceiver( SpeindSettingsChangeListener listener, SpeindSettings config, ArrayList<DataFeedSettingsInfo> dataFeedsettingsInfos) {
            mListener = listener;
            mConfig = config;
            mDataFeedsettingsInfos = dataFeedsettingsInfos;
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent==null) return;
            int cmd = intent.getIntExtra(SpeindAPI.CLIENT_CMD, -1);
            switch (cmd) {
                case CC_SETTINGS:
                    log("onSettings");
                    final SpeindSettings newConfig = SpeindSettings.getFromIntent(intent);
                    if (newConfig!=null) {
                        mListener.onSettingsChanged(mConfig, newConfig);
                        mConfig = newConfig;
                    }
                    break;
                case SpeindAPI.CC_NEW_DATAFEED_SETTINGS_INFO:
                    log("onAddDataFeed");
                    DataFeedSettingsInfo info=DataFeedSettingsInfo.getFromIntent(intent);
                    if (info!=null) {
                        mDataFeedsettingsInfos.add(info);
                        mListener.onDataFeedsListChanged();
                    }
                    break;
                case SpeindAPI.CC_REMOVE_DATAFEED_SETTINGS_INFOS:
                    log("onRemoveDataFeeds");
                    ArrayList<String> removePackages = intent.getStringArrayListExtra(PARAM_DATAFEED_PACKAGES);
                    if (removePackages!=null&&removePackages.size()>0) {
                        ArrayList<SpeindAPI.DataFeedSettingsInfo> removeInfos = new ArrayList<>();
                        for (SpeindAPI.DataFeedSettingsInfo inf : mDataFeedsettingsInfos) {
                            if (removePackages.contains(inf.packageName)) {
                                removeInfos.add(inf);
                            }
                        }
                        mDataFeedsettingsInfos.removeAll(removeInfos);
                        mListener.onDataFeedsListChanged();
                    }
                    break;
            }
        }

    }

	public interface SpeindUIReceiverListener extends SpeindSettingsChangeListener {
		void onError(int code, String error);
        void onInfoMessage(String message);
		void onStateChanged(int oldState, int newState);
		void onDownloadProgress(double progress, int fileNumber, int fileTotal);
		void onProfiles(ArrayList<String> profiles);
		void onReady();
		void onStartPlay();
		void onStopPlay();
		void onPlayMP3Info(String fileName, String nextFileName);
		void onPlayTextInfo(int infopointPos, boolean full);
		void onPlayPositionChanged(int position, int length);
		void onInfopointsChanged(int startPosition);
		void onDataFeedsProcessingChanged();
		void onDataFeedsStateChanged(String packageName, int state, boolean error);
		void onShowUpdates(String whatNewStr);
		void onExit();
	}
	
	public static class SpeindUIReceiver extends BroadcastReceiver {

		private SpeindUIReceiverListener mListener=null;
		private SpeindData speindData;
		private boolean dataReceived=false;
		private ArrayList<InfopointQueueItem> ipq=new ArrayList<>();
		
		private Handler handler = new Handler();
		public LooperThread worker;
				
		private class InfopointQueueItem {
			int pos;
			InfoPoint infopoint;
			public  InfopointQueueItem(int pPos, InfoPoint pInfopoint) {
				pos=pPos;
				infopoint=pInfopoint;
			}
		}
		
		public SpeindUIReceiver(SpeindUIReceiverListener listener, SpeindData pSpeindData) {
			//Thread.currentThread().setName("mainThread");
			worker = new LooperThread();
			worker.setThreadName("SUIworker");
			worker.start();
	    	
			mListener=listener;
			speindData=pSpeindData;
			if (speindData==null) {
				speindData=new SpeindData(); 
			}
		}
				
		@Override
		public void onReceive(final Context context, final Intent intent) {
			log("SpeindUIReceiver.onReceive");
			if (intent==null) return;
			if (speindData.service_package.equals("")) speindData.service_package = intent.getStringExtra(PARAM_SERVICE_PACKAGE_NAME);
			if (!speindData.service_package.equals(intent.getStringExtra(PARAM_SERVICE_PACKAGE_NAME))) return;

			if (worker.mHandler==null) {
				handler.postDelayed(new Runnable(){
					@Override
					public void run() {
						onReceive(context, intent);
					}
				}, 1000);
			} else {	
				worker.mHandler.post(new Runnable(){
					@Override
					public void run() {
						int cmd = intent.getIntExtra(SpeindAPI.CLIENT_CMD, -1);
						switch (cmd) {
						case SpeindAPI.CC_ERROR:
							log("onError");
							final int code = intent.getIntExtra(SpeindAPI.PARAM_ERROR_CODE, 0);
							final String descr = intent.getStringExtra(SpeindAPI.PARAM_ERROR_STR);
							handler.post(new Runnable(){
								@Override
								public void run() {
									mListener.onError(code, descr);
								}
							});
							break;
                        case SpeindAPI.CC_INFO_MESSAGE:
                            final String message = intent.getStringExtra(SpeindAPI.PARAM_MESSAGE_STR);
                            log("onInfoMessage "+"! "+message);
                            handler.post(new Runnable(){
                                @Override
                                public void run() {
                                    mListener.onInfoMessage(message);
                                }
                            });
                            break;
						case SpeindAPI.CC_STATE_CHANGED:
							log("onStateChanged");
							final int oldState=intent.getIntExtra(PARAM_OLD_STATE, -1);
							final int newState=intent.getIntExtra(PARAM_NEW_STATE, -1);
							if (oldState>=0&&newState>=0) {
								if (newState==SPEIND_STATE_PLAY_PLAYER||newState==SPEIND_STATE_PLAY_READER) {
									handler.post(new Runnable(){
										@Override
										public void run() {
											mListener.onStartPlay();
										}
									});
								} else if (newState==SPEIND_STATE_STOP_PLAYER||newState==SPEIND_STATE_STOP_READER) {
									handler.post(new Runnable(){
										@Override
										public void run() {
											mListener.onStopPlay();							
										}
									});
								}
								speindData.state=newState;
								handler.post(new Runnable(){
									@Override
									public void run() {
										mListener.onStateChanged(oldState, newState);
									}
								});
							}
							break;
						case SpeindAPI.CC_DOWLOAD_PROGRESS:
							log("onDownloadProgress");
							final int progress[]=intent.getIntArrayExtra(SpeindAPI.PARAM_DWLD_PRG);
							handler.post(new Runnable(){
								@Override
								public void run() {
									mListener.onDownloadProgress((double)progress[0]/2, progress[1], progress[2]);
								}
							});
				            break;
						case SpeindAPI.CC_FULL_STATE:
							log("onReady");
							speindData.getFronIntent(intent);
							dataReceived=true;
							for (int i=0;i<ipq.size();i++) {
								speindData.infopoints.add(ipq.get(i).pos, ipq.get(i).infopoint);
							}
							ipq.clear();
							handler.post(new Runnable(){
								@Override
								public void run() {
									mListener.onReady();
								}
							});					
							final String whatNewStr=intent.getStringExtra(PARAM_WHATNEW_STR);
							if (!TextUtils.isEmpty(whatNewStr)) {
								handler.post(new Runnable(){
									@Override
									public void run() {
										mListener.onShowUpdates(whatNewStr);
									}
								});
							}
				            break;
						case SpeindAPI.CC_INFOPOIN_POSITION:
							log("onCurrentInfopointChanged");
							int infopointpos=intent.getIntExtra(SpeindAPI.PARAM_CURRENT_INFOPOINT, 0);
							if (infopointpos!=speindData.currentInfoPoint) { 
								speindData.currentInfoPoint=infopointpos; 
							}
							break;
						case SpeindAPI.CC_PLAYING_INFO:
							int playSrc = intent.getIntExtra(SpeindAPI.PARAM_PLAY_SORCE, SpeindAPI.SOURCE_MP3);
							int state=intent.getIntExtra(SpeindAPI.PARAM_STATE, -1);						
							if (playSrc==SpeindAPI.SOURCE_MP3) {
								log("onPlayMP3Info");
								final String fileName = intent.getStringExtra(SpeindAPI.PARAM_PLAY_FILE);
								final String nextFileName = intent.getStringExtra(SpeindAPI.PARAM_NEXT_PLAY_FILE);
								handler.post(new Runnable(){
									@Override
									public void run() {
										mListener.onPlayMP3Info(fileName, nextFileName);
									}
								});
							} else if (playSrc==SpeindAPI.SOURCE_INFOPOINT) {
								log("onPlayTextInfo");
								final int infopointpos1=intent.getIntExtra(SpeindAPI.PARAM_CURRENT_INFOPOINT, 0);
								if (infopointpos1!=speindData.currentInfoPoint) { 
									speindData.currentInfoPoint=infopointpos1; 
								}
								final boolean playfull = intent.getBooleanExtra(SpeindAPI.PARAM_INFOPOINT_PLAY_FULL, false);
								handler.post(new Runnable(){
									@Override
									public void run() {
										mListener.onPlayTextInfo(infopointpos1, playfull);
									}
								});						
							}
							if (state==SPEIND_STATE_PLAY_PLAYER||state==SPEIND_STATE_PLAY_READER) {
								handler.post(new Runnable(){
									@Override
									public void run() {
										mListener.onStartPlay();
									}
								});	
							} else {
								handler.post(new Runnable(){
									@Override
									public void run() {
										mListener.onStopPlay();
									}
								});	
							}
							break;				    	
						case SpeindAPI.CC_PLAYING_POSITION:
							log("onPlayPositionChanged");
							final int playPosition = intent.getIntExtra(SpeindAPI.PARAM_PLAY_POSITION, 0);
							final int totalLen = intent.getIntExtra(SpeindAPI.PARAM_PLAY_LENGTH, 0);
							handler.post(new Runnable(){
								@Override
								public void run() {
									mListener.onPlayPositionChanged(playPosition, totalLen);
								}
							});						
							break;
						case SpeindAPI.CC_ADD_INFOPOINT:
							log("onAddInfopoint");
							InfoPoint infopoint=InfoPoint.getFromIntent(intent);
							if (infopoint!=null) {
								final int pos=intent.getIntExtra(SpeindAPI.PARAM_ADD_POSITION, speindData.infopoints.size());
								if (dataReceived) {
									speindData.infopoints.add(pos, infopoint);
									handler.post(new Runnable(){
										@Override
										public void run() {
											mListener.onInfopointsChanged(pos);
										}
									});									
								} else {
									ipq.add(new InfopointQueueItem(pos, infopoint));
								}
							}
							break;
						case SpeindAPI.CC_PROFILES:
							log("onProfiles");
							handler.post(new Runnable(){
								@Override
								public void run() {
									mListener.onProfiles(null);
								}
							});									
							break;
						case SpeindAPI.CC_NEW_DATAFEED_SETTINGS_INFO:
                                log("onAddDataFeed");
                                DataFeedSettingsInfo info=DataFeedSettingsInfo.getFromIntent(intent);
                                if (info!=null) {
                                    speindData.dataFeedsettingsInfos.add(info);
                                    handler.post(new Runnable(){
                                        @Override
                                        public void run() {
                                            mListener.onDataFeedsListChanged();
                                        }
                                    });
                                }
                                break;
						case SpeindAPI.CC_REMOVE_DATAFEED_SETTINGS_INFOS:
							log("onRemoveDataFeeds");
							ArrayList<String> removePackages = intent.getStringArrayListExtra(PARAM_DATAFEED_PACKAGES);
							if (removePackages!=null&&removePackages.size()>0) {
								ArrayList<SpeindAPI.DataFeedSettingsInfo> removeInfos = new ArrayList<>();
								for (SpeindAPI.DataFeedSettingsInfo inf : speindData.dataFeedsettingsInfos) {
									if (removePackages.contains(inf.packageName)) {
										removeInfos.add(inf);
									}
								}
								speindData.dataFeedsettingsInfos.removeAll(removeInfos);
								handler.post(new Runnable(){
									@Override
									public void run() {
										mListener.onDataFeedsListChanged();
									}
								});									
							}							
							break;	
						case CC_SETTINGS:
							log("onSettings");
							final SpeindSettings newConfig = SpeindSettings.getFromIntent(intent);
							if (newConfig!=null) {
								final SpeindSettings oldConfig=speindData.speindConfig;
								speindData.speindConfig=newConfig;		
								handler.post(new Runnable(){
									@Override
									public void run() {
										mListener.onSettingsChanged(oldConfig, newConfig);
									}
								});									
							}
							break;
						case CC_EXIT:
							log("onExit");
							handler.post(new Runnable(){
								@Override
								public void run() {
									mListener.onExit();
								}
							});									
							break;
						case CC_DATAFEED_INFO_CHANGED:
							final DataFeedSettingsInfo info_new=DataFeedSettingsInfo.getFromIntent(intent);
                            final boolean error = intent.getBooleanExtra(PARAM_ERROR, false);
							int cnt=speindData.dataFeedsettingsInfos.size();
							for (int i=0; i<cnt; i++) {
								if (speindData.dataFeedsettingsInfos.get(i).packageName.equals(info_new.packageName)&&speindData.dataFeedsettingsInfos.get(i).title.equals(info_new.title)) {
                                    final DataFeedSettingsInfo info_old = speindData.dataFeedsettingsInfos.get(i);
									speindData.dataFeedsettingsInfos.set(i, info_new);
									handler.post(new Runnable(){
										@Override
										public void run() {
                                            if (info_new.getState()!=info_old.getState()||error) mListener.onDataFeedsStateChanged(info_new.packageName, info_new.state, error);
                                            if (info_new.isWorking()!=info_old.isWorking()) mListener.onDataFeedsProcessingChanged();
										}
									});									
						    		break;
								}
							}
							break;
						}
					}
				});
			}								
		}
	}
	
	public static abstract class SpeindActivivty extends ActionBarActivity implements SpeindUIReceiverListener {
		private SpeindUIReceiver uiReceiver=null;

		public SpeindData speindData=new SpeindData();
		private BroadcastReceiver batteryInfoReceiver;
		private boolean chargerOn=false;
		private boolean isScreenDimmed=false;
		private boolean isNeedDimm=false;
		private final Handler handler = new Handler();
		private final Runnable dimmScreen = new Runnable() { public void run() { dimmBrightness(); } };
								
		@Override
		protected void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);		
			batteryInfoReceiver = new BroadcastReceiver() {
				@Override
				public void onReceive(Context context, Intent intent) {
					int status= intent.getIntExtra(BatteryManager.EXTRA_PLUGGED,0);
					if (status>0) {
						chargerOn=true;
						changeKeepScreen();
					} else {
						chargerOn=false;
						changeKeepScreen();
					}
				}
			};
			IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
			registerReceiver(batteryInfoReceiver, ifilter);

			uiReceiver = new SpeindUIReceiver(this, speindData);
			IntentFilter intFilt = new IntentFilter(SpeindAPI.BROADCAST_ACTION);
			registerReceiver(uiReceiver, intFilt);				
		}
		
		@Override
	    protected void onStart() {
			super.onStart();
			// TODO make service custom
			speindData.service_package = getPackageName();
			Intent intent = createIntent(getPackageName());
			intent.putExtra(SERVICE_CMD, SC_GET_STATE);
	    	startService(intent);
	    }
		
	    @Override
	    protected void onPause() {
			restoreBrightness(false);
	        super.onPause();
	    }
	    
		@Override
	    protected void onResume() {
			super.onResume();
			changeKeepScreen();
			if (chargerOn||speindData.speindConfig.not_off_screen) handler.postDelayed(dimmScreen, 15000);
		}
		
		@Override
	    protected void onDestroy() {
	    	unregisterReceiver(batteryInfoReceiver);
	    	if (uiReceiver!=null) {
	    		unregisterReceiver(uiReceiver);
	    		uiReceiver=null;
	    	}   		    	
	        super.onDestroy();
	    }    
		
	    private void changeKeepScreen() {
	    	boolean newIsNeedDimm=chargerOn||speindData.speindConfig.not_off_screen;
	    	if (isNeedDimm!=newIsNeedDimm) {
	    		isNeedDimm=newIsNeedDimm;
		    	if (isNeedDimm) {
		    		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON); 
		    		handler.postDelayed(dimmScreen, 15000);
		    	} else {
		    		restoreBrightness(false);
					getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);  
		    	}
	    	}
	    }

	    private void dimmBrightness() {
	    	handler.removeCallbacks(dimmScreen);
	    	final Window win = getWindow();
	    	final WindowManager.LayoutParams winParams = win.getAttributes();
	    	winParams.screenBrightness = 0.01f;
	    	win.setAttributes(winParams);
	    	isScreenDimmed=true;
	    }

	    private void restoreBrightness(boolean needDimmTimer) {
	    	handler.removeCallbacks(dimmScreen);
	    	final Window win = getWindow();
	    	final WindowManager.LayoutParams winParams = win.getAttributes();
	    	if (isScreenDimmed) {
		    	winParams.screenBrightness = -1;
		    	win.setAttributes(winParams);
		    	isScreenDimmed=false;
	    	}
	    	if (needDimmTimer) {
		    	handler.postDelayed(dimmScreen, 10000);
	    	}
	    }
	    
	    @Override
		public void onUserInteraction() {
	    	restoreBrightness(chargerOn||speindData.speindConfig.not_off_screen);
		}	
	    	    
	}
	
}
