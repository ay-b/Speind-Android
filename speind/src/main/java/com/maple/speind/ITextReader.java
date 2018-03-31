package com.maple.speind;

import java.util.ArrayList;

public interface ITextReader {
	//public static int STATE_UNAVAILABLE 	= -1;
	public static int STATE_INITIALIZATION	= 0;
	public static int STATE_NEED_DOWNLOAD 	= 1;
	public static int STATE_READY		 	= 2;
	
	public interface IReaderEventsCallback {
	    public void onReaderStateChanged(int id, int state);
	    public void onReadPositionChanged(int pos, int len);
	    public void onReaderBeforeStartRead();
	    public void onReaderCompleteRead();
	    public void onReaderUnsupportedLang(String lang);
	    public void onReaderLangListChanged();
	};		
	public void init();
	public int getState();
	public void speak(final String text, final String lang, boolean waitAfterRead);
	public void stop();
	public void pause();
    public void resume();        
    public boolean isSpeak();
    public String getDownloadFileByCode(String voiceCode);
    public boolean onDownloadSuccess(String voiceCode, String downloadedPath);
	public ArrayList<ArrayList<String>> getVoicesData();
	public ArrayList<String> setVoiceState(String voiceCode, String string);
	public ArrayList<String> getLangList();
	public String getSKUForVoice(String voiceCode);
	public void fixVoicesStates(ArrayList<String> purchaseSKUs);
	public void onVoiceDataChanged();
    public void unload();
    public boolean onNetworkStateChanged();
	public void setSpeechRate(float speech_rate);
}
