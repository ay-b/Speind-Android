package com.speind.vkplugin;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.IBinder;
import android.text.Html;
import android.util.Log;

import me.speind.SpeindAPI;
import me.speind.SpeindAPI.InfoPoint;
import com.vk.sdk.VKAccessToken;
import com.vk.sdk.VKScope;
import com.vk.sdk.VKSdk;
import com.vk.sdk.VKSdkListener;
import com.vk.sdk.api.VKApi;
import com.vk.sdk.api.VKApiConst;
import com.vk.sdk.api.VKError;
import com.vk.sdk.api.VKParameters;
import com.vk.sdk.api.VKRequest;
import com.vk.sdk.api.VKRequest.VKRequestListener;
import com.vk.sdk.api.VKResponse;


public class SpeindDataFeed extends SpeindAPI.DatafeedService {
    public static final String VK_FEED_SETTINGS_COMMAND = "vk_settings_command";
    public static final int VK_REFRESH = 0;

	public final static String BROADCAST_ACTION	="com.maple.speind.vkplugin.servicebackbroadcast";
	public static final String PREFS_NAME = "vkReceiverConfig";
    public static String[] sMyScope = new String[]{VKScope.FRIENDS, VKScope.WALL, VKScope.NOHTTPS};
	public static String sTokenKey = "VK_ACCESS_TOKEN";

    private String curProfile = "profile";
	private Map<String, String> profiles = new HashMap<>();

	//private boolean isStopReceived=false;
	//private boolean isSuspended=true;
	
	private final Handler handler = new Handler();
	private final Runnable updatePostsRunnable = new Runnable() { public void run() { refreshPosts(); } };

    private Map<String, Boolean> services = new HashMap<>();

	private void log(String message) {
		Log.e("[---VKPlugin---]", message);
	}

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent!=null) {
            int cmd=intent.getIntExtra(VK_FEED_SETTINGS_COMMAND, -1);
            switch (cmd) {
                case VK_REFRESH:
                    if (VKSdk.isLoggedIn()) {
                        Map<String, Boolean> services_loc = new HashMap<>(services);
                        Collection<String> service_packages = services_loc.keySet();
                        for (String service_package : service_packages) {
                            if (speindDFData.setNeedAuthorization(this, service_package, false)) {
                                speindDFData.setState(this, service_package, SpeindAPI.DataFeedSettingsInfo.DATAFEED_STATE_READY, false);
                                services.put(service_package, false);
                            }
                        }
                    } else {
                        Map<String, Boolean> services_loc = new HashMap<>(services);
                        Collection<String> service_packages = services_loc.keySet();
                        for (String service_package : service_packages) {
                            speindDFData.setNeedAuthorization(this, service_package, true);
                            speindDFData.setState(this, service_package, SpeindAPI.DataFeedSettingsInfo.DATAFEED_STATE_SUSPENDED, false);
                            services.put(service_package, true);
                        }
                    }
                    SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
                    SharedPreferences.Editor editor = settings.edit();
                    editor.putLong("lastRefreshTime", 0);
                    editor.commit();
                    refreshPosts();
                    break;
            }
        }
        return super.onStartCommand(intent, flags, startId);
    }

	@Override
	public SpeindAPI.DataFeedSettingsInfo onInit(String service_package) {
        services.put(service_package, true);
        SpeindAPI.DataFeedSettingsInfo info = new SpeindAPI.DataFeedSettingsInfo(this, getString(R.string.vk_title), BitmapFactory.decodeResource(getResources(), R.drawable.ic_launcher), true);
		return info;
	}

	@Override
	public void onInfoPointDetails(String service_package, InfoPoint arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onLangListChanged(String service_package, ArrayList<String> arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onLike(String service_package, InfoPoint arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onPost(String service_package, InfoPoint infopoint) {
        log("onPost "+service_package+" "+profiles.get(service_package));
        SpeindAPI.InfoPointData data = infopoint.getData(profiles.get(service_package));
        if (data!=null) {
            VKRequest request = VKApi.wall().post(VKParameters.from(VKApiConst.ATTACHMENTS, ""+data.postURL+"", VKApiConst.MESSAGE, getString(R.string.shared_via_speind)));
            request.setPreferredLang(getResources().getConfiguration().locale.getLanguage());
            request.attempts = 0;
            request.executeWithListener(new VKRequestListener() {
                @Override
                public void onComplete(VKResponse response) {
                    super.onComplete(response);
                }

                @Override
                public void onError(VKError error) {
                    log("Ошибка. Сообщаем пользователю об error. " + error.errorMessage);
                }

                @Override
                public void attemptFailed(VKRequest request, int attemptNumber, int totalAttempts) {
                    log("Неудачная попытка. В аргументах имеется номер попытки и общее их количество.");
                }
            });
        }
    }

	@Override
	public void onLoadImagesOnMobileInetChanged(String service_package, boolean arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onSetProfile(String service_package, String profile) {
		log("onSetProfile: "+profile);
        profiles.put(service_package, profile);
		initVKSDK();
	}

	private void initVKSDK() {
		VKSdkListener listener=new VKSdkListener(){
			@Override
			public void onCaptchaError(VKError captchaError) {
				log("onCaptchaError");
				//new VKCaptchaDialog(captchaError).show();
			}

			@Override
			public void onTokenExpired(VKAccessToken expiredToken) { 
				log("onTokenExpired");
				//VKSdk.authorize(sMyScope);
	            Intent intent = new Intent(BROADCAST_ACTION).putExtra("cmd", 0);
	            intent.putExtra("error_code", 2);
	    		sendBroadcast(intent);
			}

			@Override
			public void onAccessDenied(VKError authorizationError) {
				log("onAccessDenied");
	            Intent intent = new Intent(BROADCAST_ACTION).putExtra("cmd", 0);
	            intent.putExtra("error_code", 1);
	    		sendBroadcast(intent);
				//new AlertDialog.Builder(LoginActivity.this).setMessage(authorizationError.errorMessage).show();
			}
			
			@Override
	        public void onReceiveNewToken(VKAccessToken newToken) {
				log("onReceiveNewToken");
	            newToken.saveTokenToSharedPreferences(SpeindDataFeed.this, sTokenKey+"_"+curProfile);
	            //Intent i = new Intent(LoginActivity.this, MainActivity.class);
	            //startActivity(i);
	            Intent intent = new Intent(BROADCAST_ACTION).putExtra("cmd", 0);
	            intent.putExtra("error_code", 0);
	    		sendBroadcast(intent);
	        }

	        @Override
	        public void onAcceptUserToken(VKAccessToken token) {
	        	log("onAcceptUserToken");
	            //Intent i = new Intent(LoginActivity.this, MainActivity.class);
	            //startActivity(i);
	        }
	        
		};

		VKSdk.initialize(listener, "4760405", VKAccessToken.tokenFromSharedPreferences(this, sTokenKey+"_"+curProfile));
		VKSdk.authorize(sMyScope, true, false);
	}
	
	private class ContactInfo {
		public String name="";
		public String screen_name="";
		public String photo_50="";
		public String photo_100="";
		public int sex=1;
	}	
 	
 	private String prepareText(String fulltext) {
 		String text="<p>";

 		fulltext=fulltext.replaceAll("\\[[^\\[]+\\|([^\\[]+)\\]", "$1");
 				
 		int cnt=fulltext.length();
 		StringBuffer word=new StringBuffer(cnt); 		
 		for (int i=0;i<cnt;i++) {
			char ch = fulltext.charAt(i);
			if ((ch==' ')||(ch=='\n')||(i==cnt-1)) {
				word.append(ch);
				if (word.charAt(0)=='#') {					
				} else {
					if (SpeindAPI.isURL(word.toString())) {
						text+="</p><p>"+getApplicationContext().getString(R.string.link)+"</p><p>";
					} else {
						text+=word.toString();
					}					
				}
				word=new StringBuffer(cnt);
			} else {
				word.append(ch);
			}
		}		
 		return ""+Html.fromHtml(text+"</p>");
 	}
 	
	public void refreshPosts() {
		log("refreshPosts");
		handler.removeCallbacks(updatePostsRunnable);
		
		if (!(services.size()==0||isSuspended())&&VKSdk.isLoggedIn()) {
			SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
			int refreshInterval = settings.getInt(curProfile+"_refreshInterval", 15*60);
			long lastRefreshTime = settings.getLong("lastRefreshTime", 0);
			long delta=refreshInterval*1000-(new Date()).getTime()-lastRefreshTime;
			
			if (delta<5000) {
				log("refreshPosts 1");
				SharedPreferences.Editor editor = settings.edit();
				editor.putLong("lastRefreshTime", (new Date()).getTime());
				editor.commit();

				int aType = SpeindAPI.getConnectionStatus(SpeindDataFeed.this);
    			if (aType!=-1) {

                    setProcessingState(true);
    				
					//if (((new Date()).getTime()-sendInfoPointParams.postTime.getTime())>receiver.speindData.storeInfopointTime) continue;
    				// TODO get only need time interval
					VKRequest request = new VKRequest("newsfeed.get", VKParameters.from());
                    request.setPreferredLang(getResources().getConfiguration().locale.getLanguage());
					request.parseModel=false;
					request.executeWithListener(new VKRequestListener() {
					    @Override
					    public void onComplete(VKResponse response) {
					    	if (services.size()==0||isSuspended()) {
                                setProcessingState(false);
	    			    		return;
	    			    	}
					        try {
					        	//log(response.responseString);
					        	final JSONObject obj=response.json.getJSONObject("response");
					        	new Thread(new Runnable() {
					        		JSONArray items=null;
					        		JSONArray profiles=null;
					        		JSONArray groups=null;
					        		
							        public void run() {
							        	if (services.size()==0||isSuspended()) {
                                            setProcessingState(false);
		    	    			    		return;
		    	    			    	}
	
							        	try {
							        		items=obj.getJSONArray("items");
							        		profiles=obj.getJSONArray("profiles");
							        		groups=obj.getJSONArray("groups");
										} catch (JSONException e) {}					        	
							        		
						        		Map<Integer, ContactInfo> contactInfos=new HashMap<Integer, ContactInfo>(); 
						        				
						        		if (profiles!=null) {
							        		for (int i=0; i<profiles.length(); i++) {
							        			if (services.size()==0||isSuspended()) {
                                                    setProcessingState(false);
				    	    			    		return;
				    	    			    	}
									        	try {
								        			JSONObject profile=profiles.getJSONObject(i);
								        			ContactInfo contactInfo=new ContactInfo();
								        			contactInfo.name=profile.getString("first_name")+" "+profile.getString("last_name");
								        			contactInfo.screen_name=profile.getString("screen_name");
								        			contactInfo.photo_50=profile.getString("photo_50");
								        			contactInfo.photo_100=profile.getString("photo_100");
								        			contactInfo.sex=profile.getInt("sex");
								        			contactInfos.put(profile.getInt("id"), contactInfo);
												} catch (JSONException e) {}					        	
							        		}
						        		}
						        		if (groups!=null) {
							        		for (int i=0; i<groups.length(); i++) {
							        			if (services.size()==0||isSuspended()) {
                                                    setProcessingState(false);
				    	    			    		return;
				    	    			    	}
									        	try {
								        			JSONObject group=groups.getJSONObject(i);
								        			ContactInfo contactInfo=new ContactInfo();
								        			contactInfo.name=group.getString("name");
								        			contactInfo.screen_name=group.getString("screen_name");
								        			contactInfo.photo_50=group.getString("photo_50");
								        			contactInfo.photo_100=group.getString("photo_100");
								        			contactInfo.sex=1;
								        			contactInfos.put(-group.getInt("id"), contactInfo);
							        			} catch (JSONException e) {}					        	
							        		}
						        		}
										for (int i=0; i<items.length(); i++) {
											if (services.size()==0||isSuspended()) {
                                                setProcessingState(false);
			    	    			    		return;
			    	    			    	}
											String type="";
											JSONObject item=null;
								        	try {
								        		item=items.getJSONObject(i);
												type=item.getString("type");
											} catch (JSONException e) {}
								        	if (item==null) continue;

											String postURL = "http://vk.com/wall";
											try {
												postURL+=item.getInt("source_id")+"_"+item.getString("post_id");
											} catch (JSONException e) {}

								        	if (type.equals("post")) {
								        		boolean isRepost=false;
								        		int sourceUser=0;
								        		int postUser=0;
								        		try {
								        			// Get post type
								        			String postType=item.getString("post_type");
								        			if (postType.equals("post")) {
														SpeindAPI.SendInfoPointParams sendInfoPointParams=new SpeindAPI.SendInfoPointParams();
														// Get post time
														sendInfoPointParams.postTime=new Date(item.getLong("date")*1000);
														
														if (((new Date()).getTime()-sendInfoPointParams.postTime.getTime())>receiver.speindData.getMaxStoreInfopointTime()) continue;

														sendInfoPointParams.postURL = postURL;

																// Get sender id
														sourceUser=item.getInt("source_id");
														try {
															JSONArray copy_history=item.getJSONArray("copy_history");
															if (copy_history!=null) {
								    	    			       	sendInfoPointParams.postTitle=item.getString("text");
																isRepost=true;
																item=copy_history.getJSONObject(0);
																postUser=item.getInt("owner_id");
															}	
														} catch (JSONException e) {}	
										        		sendInfoPointParams.postOriginText=item.getString("text");	
										        		
										        		String lang="";
										        		if (!sendInfoPointParams.postTitle.equals("")||!sendInfoPointParams.postOriginText.equals("")) {
										        			lang=getLang(sendInfoPointParams.postTitle+" "+sendInfoPointParams.postOriginText);
										        		}
										        		if (lang.equalsIgnoreCase("")) {
										        			String name=contactInfos.get(sourceUser).name;
										        			lang=getLang(name+" "+name.toLowerCase()+" "+name+" "+name.toLowerCase());
										        		}
										        		
										        		Configuration conf = getResources().getConfiguration();
										        		Locale localeOld=conf.locale;
						    	    			    	conf.locale = new Locale(lang);
						    	    			    	Resources resources = new Resources(getAssets(), getResources().getDisplayMetrics(), conf);
						    	    			    	
										        		// Get sender picture
										        		String senderBmpURL=contactInfos.get(sourceUser).photo_50;
										        		String postBmpURL="";
										        		if (isRepost) {
										        			// Get post picture as origin sender picture
										        			postBmpURL=contactInfos.get(postUser).photo_100;
										        			
										        			sendInfoPointParams.postSender=contactInfos.get(sourceUser).name+" quote "+contactInfos.get(postUser).name;					    	    			    	
							    	    			    	if (!sendInfoPointParams.postTitle.equals("")) 
							    	    			    		sendInfoPointParams.postTitleVocalizing=resources.getString(R.string.with_label)+" "+sendInfoPointParams.postTitle;
							    	    			    	String name=contactInfos.get(sourceUser).name;
							    	    			    	String nameLang=getLang(name+" "+name.toLowerCase()+" "+name+" "+name.toLowerCase());
							    	    			    	log("nameLang: "+nameLang+" ["+name+"]");
							    	    			    	if (!(lang.equals(nameLang)||nameLang.equals("en"))) name=contactInfos.get(sourceUser).screen_name;
							    	    			    	String postName=contactInfos.get(postUser).name;
							    	    			    	String postNameLang=getLang(postName+" "+postName.toLowerCase()+" "+postName+" "+postName.toLowerCase());
							    	    			    	log("postNameLang: "+postNameLang+" ["+postName+"]");
							    	    			    	if (!(lang.equals(postNameLang)||postNameLang.equals("en"))) postName=contactInfos.get(postUser).screen_name;
							    	    			    	
							    	    			    	
							    	    			    	sendInfoPointParams.postSenderVocalizing=name;
										        			if (sourceUser>0) {
											        			if (contactInfos.get(sourceUser).sex==1) {
											        				sendInfoPointParams.postSenderVocalizing+=" "+resources.getString(R.string.w_quotes)+" ";
											        			} else {
											        				sendInfoPointParams.postSenderVocalizing+=" "+resources.getString(R.string.quotes)+" ";
											        			}
											        			if (postUser<0) {
											        				sendInfoPointParams.postSenderVocalizing+=" "+resources.getString(R.string.of_group)+" ";
											        			} else {
											        				sendInfoPointParams.postSenderVocalizing+=" "+resources.getString(R.string.of_user)+" ";
											        			}
										        			} else {
										        				sendInfoPointParams.postSenderVocalizing=resources.getString(R.string.in_group)+" "+sendInfoPointParams.postSenderVocalizing;
										        				
											        			if (postUser<0) {
											        				sendInfoPointParams.postSenderVocalizing+=" "+resources.getString(R.string.of_group1)+" ";
											        			} else {
											        				sendInfoPointParams.postSenderVocalizing+=" "+resources.getString(R.string.of_user1)+" ";
											        			}
										        			}
										        			
							    	    			    	sendInfoPointParams.postSenderVocalizing+=postName;
							    	    			    	if (!sendInfoPointParams.postOriginText.equals("")) 
							    	    			    		sendInfoPointParams.postTextVocalizing=prepareText(resources.getString(R.string.original_post_text)+": "+sendInfoPointParams.postOriginText);										
							    	    			    	
										        		} else {
										        			postBmpURL=contactInfos.get(sourceUser).photo_100;
							    	    			    	sendInfoPointParams.postTitleVocalizing="";
							    	    			    	sendInfoPointParams.postSender=contactInfos.get(sourceUser).name;
							    	    			    	String name=contactInfos.get(sourceUser).name;
							    	    			    	String nameLang=getLang(name+" "+name.toLowerCase()+" "+name+" "+name.toLowerCase()+" "+name);
							    	    			    	log("nameLang: "+nameLang+" ["+name+"]");
							    	    			    	if (!(lang.equals(nameLang)||nameLang.equals("en"))) name=contactInfos.get(sourceUser).screen_name;
							    	    			    	if (sourceUser>0) {
							    	    			    		if (contactInfos.get(sourceUser).sex==1) {
							    	    			    			sendInfoPointParams.postSenderVocalizing=name+" "+resources.getString(R.string.w_write);
							    	    			    		} else {
							    	    			    			sendInfoPointParams.postSenderVocalizing=name+" "+resources.getString(R.string.write);
							    	    			    		}
							    	    			    	} else {
							    	    			    		sendInfoPointParams.postSenderVocalizing=resources.getString(R.string.new_post_in_group)+" "+name;
							    	    			    	}
							    	    			    	sendInfoPointParams.postTextVocalizing=prepareText(sendInfoPointParams.postOriginText);										
										        		}
										        		
										        		
										        		try {
															JSONArray attachments=item.getJSONArray("attachments");
															if (attachments!=null) {
																for (int j=0;j<attachments.length();j++) {
																	JSONObject attachment=attachments.getJSONObject(j);
																	if (attachment.getString("type").equals("photo")) {
																		postBmpURL=attachment.getJSONObject("photo").getString("photo_604");
																		break;
																	}
																}
															}	
														} catch (JSONException e) {}	
										        		
										        		sendInfoPointParams.senderBmp=null;
						    	    			    	sendInfoPointParams.senderBmpURL=senderBmpURL;
						    	    			    	sendInfoPointParams.postBmp=null;
						    	    			    	sendInfoPointParams.postBmpURL=postBmpURL;
										        		
										        		sendInfoPointParams.postOriginText=item.getString("text");
						    	    			    	sendInfoPointParams.postPluginData="";		    	    			    	
						    	    			    	sendInfoPointParams.postLink="";
						    	    			    	sendInfoPointParams.checkForDuplicate=false;
						    	    			    	sendInfoPointParams.lang=lang;			    	    			    	
						    	    			    								        		
										        		//log(sendInfoPointParams.postSender+" "+sendInfoPointParams.postTitle+" "+sendInfoPointParams.postOriginText);
										        		
										        		conf.locale=localeOld;
						    	    			    	resources = new Resources(getAssets(), getResources().getDisplayMetrics(), conf);
						    	    			    	
						    	    			    	sendInfoPointParams.postOriginText=sendInfoPointParams.postOriginText.replaceAll("\\[[^\\[]+\\|([^\\[]+)\\]", "$1");

                                                        Map<String, Boolean> services_loc = new HashMap<>(services);
                                                        Collection<String> service_packages = services_loc.keySet();
                                                        for (String service_package : service_packages) {
                                                            if (!services_loc.get(service_package)) sendInfoPoint(service_package, sendInfoPointParams);
                                                        }
								        			}
												} catch (JSONException e) {}
								        	} else if (type.equals("photo")) {
								        		int sourceUser=0;
								        		try {
								        			SpeindAPI.SendInfoPointParams sendInfoPointParams=new SpeindAPI.SendInfoPointParams();
													sendInfoPointParams.postTime=new Date(item.getLong("date")*1000);
													if (((new Date()).getTime()-sendInfoPointParams.postTime.getTime())>receiver.speindData.getMaxStoreInfopointTime()) continue;

                                                    sendInfoPointParams.postURL = postURL;

													sourceUser=item.getInt("source_id");
													
													sendInfoPointParams.postTitle="";
													sendInfoPointParams.postOriginText="";											
													sendInfoPointParams.postTitleVocalizing="";
					    	    			    	sendInfoPointParams.postTextVocalizing="";
													
													String senderBmpURL=contactInfos.get(sourceUser).photo_50;
											    	String postBmpURL=contactInfos.get(sourceUser).photo_100;
											    	try {
											    		JSONObject photos=item.getJSONObject("photos");
														JSONArray photosItems=photos.getJSONArray("items");
														if (photosItems!=null) {
															for (int j=0;j<photosItems.length();j++) {
																JSONObject photosItem=photosItems.getJSONObject(j);
																postBmpURL=photosItem.getString("photo_604");
																break;
															}
														}	
													} catch (JSONException e) {}
													
					    	    			    	String name=contactInfos.get(sourceUser).name;
													String lang=getLang(name+" "+name.toLowerCase()+" "+name+" "+name.toLowerCase()+" "+name);
					    	    			    	String nameLang=lang;
					        						  
									        		Configuration conf = getResources().getConfiguration();
									        		Locale localeOld=conf.locale;
					    	    			    	conf.locale = new Locale(lang);
					    	    			    	Resources resources = new Resources(getAssets(), getResources().getDisplayMetrics(), conf);
		
					    	    			    	sendInfoPointParams.postSender=contactInfos.get(sourceUser).name;
					    	    			    	log("nameLang: "+nameLang+" ["+name+"]");
					    	    			    	if (!(lang.equals(nameLang)||nameLang.equals("en"))) name=contactInfos.get(sourceUser).screen_name;
					    	    			    	if (sourceUser>0) {
					    	    			    		if (contactInfos.get(sourceUser).sex==1) {
					    	    			    			sendInfoPointParams.postSenderVocalizing=name+" "+resources.getString(R.string.w_posted_photo);
					    	    			    		} else {
					    	    			    			sendInfoPointParams.postSenderVocalizing=name+" "+resources.getString(R.string.posted_photo);
					    	    			    		}
					    	    			    	} else {
					    	    			    		sendInfoPointParams.postSenderVocalizing=resources.getString(R.string.new_photo_group)+" "+name;
					    	    			    	}
					    	    			    	
					    	    			    	sendInfoPointParams.senderBmp=null;
					    	    			    	sendInfoPointParams.senderBmpURL=senderBmpURL;
					    	    			    	sendInfoPointParams.postBmp=null;
					    	    			    	sendInfoPointParams.postBmpURL=postBmpURL;
									        		
					    	    			    	sendInfoPointParams.postPluginData="";		    	    			    	
					    	    			    	sendInfoPointParams.postLink="";
					    	    			    	sendInfoPointParams.checkForDuplicate=false;
					    	    			    	sendInfoPointParams.lang=lang;		
					    	    			    	
					    	    			    	conf.locale=localeOld;
					    	    			    	resources = new Resources(getAssets(), getResources().getDisplayMetrics(), conf);
					    	    			    	
					    	    			    	if (!(services.size()==0||isSuspended())) {
                                                        setProcessingState(false);
					    	    			    		return;
					    	    			    	}

                                                    Map<String, Boolean> services_loc = new HashMap<>(services);
                                                    Collection<String> service_packages = services_loc.keySet();
                                                    for (String service_package : service_packages) {
                                                        if (!services_loc.get(service_package)) sendInfoPoint(service_package, sendInfoPointParams);
                                                    }
													
								        		} catch (JSONException e) {}
								        	} else if (type.equals("wall_photo")) {
								        		
								        	}									
										}
                                        setProcessingState(false);
							        }
							    }).start();						
							} catch (JSONException e) {
                                setProcessingState(false);
							}
					    }
					    @Override
					    public void onError(VKError error) {
					        log("Ошибка. Сообщаем пользователю об error. " + error.errorMessage);
                            setProcessingState(false);
					    }
					    @Override
					    public void attemptFailed(VKRequest request, int attemptNumber, int totalAttempts) {
					        log("Неудачная попытка. В аргументах имеется номер попытки и общее их количество.");
                            setProcessingState(false);
					    }		    
					});
				}
			}
            if (!(services.size()==0||isSuspended())) {
				if (delta<0) delta=0;
				handler.postDelayed(updatePostsRunnable, (refreshInterval*1000-delta));
			}
		}
	}
	
	@Override
	public void onStop(String service_package) {
        services.remove(service_package);
        profiles.remove(service_package);
        if (services.size()==0) {
            handler.removeCallbacks(updatePostsRunnable);
            this.stopSelf();
        } else if (isSuspended()) {
            handler.removeCallbacks(updatePostsRunnable);
        }
	}

	@Override
	public void onShowSettings(String service_package) {
		Intent intent=new Intent(SpeindDataFeed.this, VKSettings.class);
		intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		intent.putExtra("profile", curProfile);
		getApplication().startActivity(intent);
	}

	@Override
	public IBinder onBind(Intent intent) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void onResume(String service_package) {
        //isSuspended=false;
        services.put(service_package, false);
		refreshPosts();
	}

	@Override
	public void onStart(String service_package, int state) {
		if (state==SpeindAPI.DataFeedSettingsInfo.DATAFEED_STATE_READY) {
			//isSuspended=false;
            services.put(service_package, false);
    		refreshPosts();
		} else {
			//isSuspended=true;
            services.put(service_package, true);
		}
	}

	@Override
	public void onSuspend(String service_package) {
        //isSuspended=true;
        services.put(service_package, true);
        if (isSuspended()) handler.removeCallbacks(updatePostsRunnable);
	}

	@Override
	public void onStoreInfopointTimeChanged(String service_package, long arg0) {
		// TODO Auto-generated method stub
		
	}

    private boolean isSuspended() {
        Map<String, Boolean> services_loc = new HashMap<>(services);
        Collection<Boolean> suspendeds = services_loc.values();
        for (Boolean suspended : suspendeds) {
            if (!suspended) return false;
        }
        return true;
    }

	@Override
	public boolean isAuthorized(String service_package) {
		return VKSdk.isLoggedIn();
	}
}
