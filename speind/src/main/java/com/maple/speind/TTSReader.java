package com.maple.speind;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import android.content.Context;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeech.EngineInfo;
import android.speech.tts.UtteranceProgressListener;
import android.util.Log;

public class TTSReader extends TextReader implements TextToSpeech.OnInitListener {
	private static final String PREFS_NAME = "voicesConfig";
	
	private Context mContext;
	
	private TextToSpeech init_speech=null;
	
	private ArrayList<ArrayList<String>> voicesData=new ArrayList<ArrayList<String>>();

	private float speech_rate = 1.0f;

	private ArrayList<String> enginesNames=new ArrayList<String>();
	private ArrayList<TextToSpeech> speeches=new ArrayList<TextToSpeech>();		
	private int onInitCount=0; 
	private TextToSpeech currentSpeech=null;
	private static HashMap<String, String> myHashAlarm = new HashMap<String, String>();
	static {
		myHashAlarm.put(TextToSpeech.Engine.KEY_PARAM_STREAM, String.valueOf(AudioManager.STREAM_MUSIC));
		myHashAlarm.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "speak text");				
	}
	
	private String [] readTask=null;
	private int taskIndex=0;
	private int totalTaskLen=0;
	private int taskReady=0;
	
	private boolean needSendCompleteRead=false;
	
	public TTSReader(Context context, IReaderEventsCallback readerEventsCallback, int id) {
		super(context, readerEventsCallback, id);
		mContext=context;					
	}

	@Override
	public boolean onDownloadSuccess(String voiceCode, String downloadedPath) {
		return false;		
	}
	
	@Override
	public void init() {
		// TODO check TTS available
		// If available
		init_speech = new TextToSpeech(mContext, null);
		List<EngineInfo> engines = init_speech.getEngines();
		if (engines.size()>0) {
			for (EngineInfo engine : engines) {
				TextToSpeech cspeech = new TextToSpeech(mContext, this, engine.name);
				cspeech.setOnUtteranceProgressListener(new UtteranceProgressListener(){
					@Override
					public void onStart(String utteranceId) {
						//sendReadPosition(0, 1);
					}
					@Override
					public void onDone(String utteranceId) {
						if (needSendCompleteRead) {
							if ((taskIndex+1)>=readTask.length) {
								sendReadPosition(totalTaskLen, totalTaskLen);
								readTask=null;
								taskIndex=0;
								currentSpeech=null;
								sendCompleteRead();
							} else {
								if (currentSpeech!=null) {
									taskReady+=readTask[taskIndex].length();
									sendReadPosition(taskReady, totalTaskLen);
									taskIndex+=1;
									currentSpeech.speak(readTask[taskIndex], TextToSpeech.QUEUE_FLUSH, myHashAlarm);
								} else {
									readTask=null;
									taskIndex=0;
									sendCompleteRead();
								}
							}
						}
					}
					@Override
					public void onError(String utteranceId) {
						currentSpeech=null;
						sendCompleteRead();
					}});
				speeches.add(cspeech);	
				enginesNames.add(engine.name);
			}
		} else {
			setState(STATE_NEED_DOWNLOAD);
		}
		// else
		//setState(STATE_UNAVAILABLE);
	}

	@Override
	public void speak(String text, String lang) {
		boolean langFound=false;
		int speechIndex=-1;
        ArrayList<ArrayList<String>> voicesDataLoc = getVoicesData();
    	for (ArrayList<String> voiceAr: voicesDataLoc) {
    		if (voiceAr.get(3).compareToIgnoreCase(lang)==0&&voiceAr.get(4).equals("4")) {
    			for (int i=0;i<enginesNames.size();i++) {
    				if (voiceAr.get(2).contains(enginesNames.get(i))) {
    					speechIndex=i;
    	    			langFound=true;
    	    			break;
    				}
    			}
    			if (langFound) break;
    		}
    	}
		
    	if (langFound) {
    		TextToSpeech speech = speeches.get(speechIndex);
			if (speech.isLanguageAvailable(new Locale(lang))>=TextToSpeech.LANG_AVAILABLE) {
				speech.setLanguage(new Locale(lang));

				readTask=text.split("[\\r\\n]+|[?!.]\\s*");
				taskIndex=0;
				if (readTask!=null&&readTask.length>0) {
					totalTaskLen=0;
					taskReady=0;
					for (int i=0;i<readTask.length;i++) {
						totalTaskLen+=readTask[i].length();
					}
					needSendCompleteRead=true;
					//speech.setPitch(0.5f);
					speech.setSpeechRate(speech_rate);
					speech.speak(readTask[taskIndex], TextToSpeech.QUEUE_FLUSH, myHashAlarm);
					currentSpeech=speech;
				} else {
					sendCompleteRead();
				}
			} else {
	    		sendUnsupportedLang(lang);    		
			}
    	} else {
    		sendUnsupportedLang(lang);    		
    	}
	}

	@Override
	public void stopSpeak() {
		readTask=null;
		taskIndex=0;
		needSendCompleteRead=false;
		if (currentSpeech!=null) {
			if (currentSpeech.isSpeaking()) {
				currentSpeech.stop();
			}
			currentSpeech=null;
		}
	}

	@Override
	public void pauseSpeak() {
		if (currentSpeech!=null) {
			needSendCompleteRead=false;
			currentSpeech.stop();
		}
	}

	@Override
	public void resume() {
		if (currentSpeech!=null) {
			if (readTask!=null&&taskIndex<readTask.length) {
				if (taskIndex>0) {
					taskReady-=readTask[taskIndex].length();
					taskIndex-=1;
				}
				needSendCompleteRead=true;
				currentSpeech.speak(readTask[taskIndex], TextToSpeech.QUEUE_FLUSH, myHashAlarm);
			} else {
				currentSpeech=null;
				sendCompleteRead();
			}
		} else {
            sendCompleteRead();
        }
	}

	@Override
	public boolean isSpeak() {
		if (currentSpeech!=null) {
			return currentSpeech.isSpeaking();
		}
		return false;
	}

	@Override
	public void unload() {
		stopSpeak();		
		for (TextToSpeech speech : speeches) {
			speech.shutdown();
		}
		if (init_speech!=null) {
			init_speech.shutdown();
		}
		speeches.clear();	
	}
	
	@Override
	public void onInit(int status) {
		onInitCount++;
		if (onInitCount==speeches.size()) {
			SharedPreferences settings = mContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
			String [] langs=Locale.getISOLanguages();
			for (String lang : langs) {
				Locale locale=new Locale(lang);
				for (int i=0; i<speeches.size();i++) {
					TextToSpeech speech=speeches.get(i);
					if (speech.isLanguageAvailable(locale)>=TextToSpeech.LANG_AVAILABLE) {
						String code=enginesNames.get(i)+"("+locale.getLanguage()+"_"+locale.getCountry()+")";
						ArrayList<String> data = new ArrayList<String>();
						data.add(locale.getDisplayLanguage(locale));
						data.add("TTS ("+enginesNames.get(i)+")");
						data.add(code);
						data.add(lang);
						data.add("3");
						data.add(" ");
						voicesData.add(data);
						setVoiceState(code, settings.getString(code, "3"));
					}
				}
			}
			if (voicesData.size()>0) {
				setState(STATE_READY);
			} else {
				setState(STATE_NEED_DOWNLOAD);
			}
		}	
	}
	
	@Override
	public String getDownloadFileByCode(String code) {
		return "";
	}

	@Override
	public ArrayList<ArrayList<String>> getVoicesData() {
		return new ArrayList<ArrayList<String>>(voicesData);
	}

	@Override
	public ArrayList<String> setVoiceState(String code, String string) {
		SharedPreferences settings = mContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
		SharedPreferences.Editor editor = settings.edit();
		ArrayList<String> voice=null;
        ArrayList<ArrayList<String>> voicesDataLoc = getVoicesData();
		for (ArrayList<String> voiceAr: voicesDataLoc) {
			if (voiceAr!=null&&code.equals(voiceAr.get(2))) {
				voiceAr.set(4, string);
				voice=voiceAr;
				editor.putString(code, string);
				break;
			}
		}
		if (isSingle()) {
			if (voice!=null&&string.equals("4")) {
				for (ArrayList<String> voiceAr: voicesDataLoc) {
					if (voiceAr.get(3).equals(voice.get(3))&&!code.equals(voiceAr.get(2))&&voiceAr.get(4).equals("4")) {
						voiceAr.set(4, "3");
						editor.putString(voiceAr.get(2), "3");
					}
				}
			}
			boolean deffound=false;
			if (voice!=null&&string.equals("3")) {
				for (ArrayList<String> voiceAr: voicesDataLoc) {
					if (voiceAr.get(3).equals(voice.get(3))&&voiceAr.get(4).equals("4")) {
						deffound=true;
						break;
					}
				}
				if (!deffound) {
					voice.set(4, "4");
					editor.putString(code, "4");
				}
			}
		}
		editor.commit();
		return voice;
	}

	@Override
	public ArrayList<String> getLangList() {
		ArrayList<String> langList=new ArrayList<String>();
        ArrayList<ArrayList<String>> voicesDataLoc = getVoicesData();
		for (ArrayList<String> voiceAr : voicesDataLoc) {
			if (voiceAr.get(4).equals("4")) {
				langList.add(voiceAr.get(3));
			}
		}
		return langList;
	}

	@Override
	public String getSKUForVoice(String code) { return ""; }

	@Override
	public void fixVoicesStates(ArrayList<String> purchaseSKUs) {}

	@Override
	public boolean containsDefaultVoice(String lang) {
        ArrayList<ArrayList<String>> voicesDataLoc = getVoicesData();
		for (ArrayList<String> voiceAr: voicesDataLoc) {
    		if (voiceAr.get(3).compareToIgnoreCase(lang)==0&&voiceAr.get(4).equals("4")) {
    			for (int i=0;i<enginesNames.size();i++) {
    				if (voiceAr.get(2).contains(enginesNames.get(i))) {
    					return true;
    				}
    			}
    		}
    	}
		return false;
	}

	@Override
	public void onVoiceDataChanged() {}	
	
	@Override
	public boolean onNetworkStateChanged() {
		voicesData.clear();
		SharedPreferences settings = mContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
		String [] langs=Locale.getISOLanguages();
		for (String lang : langs) {
			Locale locale=new Locale(lang);
			for (int i=0; i<speeches.size();i++) {
				TextToSpeech speech=speeches.get(i);
				if (speech.isLanguageAvailable(locale)>=TextToSpeech.LANG_AVAILABLE) {
					String code=enginesNames.get(i)+"("+locale.getLanguage()+"_"+locale.getCountry()+")";
					ArrayList<String> data = new ArrayList<String>();
					data.add(locale.getDisplayLanguage(locale));
					data.add("TTS ("+enginesNames.get(i)+")");
					data.add(code);
					data.add(lang);
					data.add("3");
					data.add(" ");
					voicesData.add(data);
					setVoiceState(code, settings.getString(code, "3"));
				}
			}
		}
		return true;
	}

	@Override
	public void setSpeechRate(float sr) {
		speech_rate=sr;
		if (currentSpeech!=null) currentSpeech.setSpeechRate(speech_rate);
	}

}
