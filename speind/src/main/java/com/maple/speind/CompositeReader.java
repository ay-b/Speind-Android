package com.maple.speind;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import android.content.Context;
import android.os.Handler;
import android.util.Log;

public class CompositeReader implements ITextReader, ITextReader.IReaderEventsCallback {
	int state = STATE_INITIALIZATION; 
	int id = 0;
	private ArrayList<TextReader> textReaders=new ArrayList<TextReader>();
	private TextReader currentReader=null;
	private IReaderEventsCallback mReaderEventsCallback=null;
	private Handler mHandler=new Handler();

    private static void log(String s) {
        Log.e("[---CompositeReader---]", Thread.currentThread().getName()+": "+s);
    }

	public CompositeReader(Context context, IReaderEventsCallback readerEventsCallback, int rid) {
		mReaderEventsCallback=readerEventsCallback;
		id=rid;
		TextReader reader=null;
		reader=new TTSReader(context, this, textReaders.size());
		reader.setSingle(false);
		textReaders.add(reader);
		reader=new AcapelaReader(context, this, textReaders.size());
		reader.setSingle(false);
		textReaders.add(reader);
	}

	public void sendUnsupportedLang(final String lang) {
    	if (mReaderEventsCallback!=null) {
    		mHandler.post(new Runnable(){
				public void run() {
		    		mReaderEventsCallback.onReaderUnsupportedLang(lang);
				}
			});
    	}
    }
	
	public void speak(final String text, final String lang, boolean waitAfterRead) {
		for (TextReader textReader : textReaders) {
			if (textReader.getState()!=STATE_READY) continue;
			if (textReader.containsDefaultVoice(lang)) {
				currentReader=textReader;
				textReader.speak(text, lang, waitAfterRead);
				return;
			}
		}
		sendUnsupportedLang(lang);
	}
    
	public void stop() {
		if (currentReader!=null) {
			currentReader.stop();
			currentReader=null;
		}
	}
    
    public String getDownloadFileByCode(String voiceCode){
    	for (TextReader textReader : textReaders) {
    		String res=textReader.getDownloadFileByCode(voiceCode);
			if (res!=null&&res.length()>0) {
				return res;
			}
		}
    	return "";
    }
    
    public boolean onDownloadSuccess(String voiceCode, String downloadedUriString){
    	for (TextReader textReader : textReaders) {
			if (textReader.onDownloadSuccess(voiceCode, downloadedUriString)) {
				return true;
			}
		}
    	return false;
    }
    
	public void init(){
		for (TextReader textReader : textReaders) {
			textReader.init();
		}
    }
        
	public void pause(){
		if (currentReader!=null) 
			currentReader.pause();
    }
    
    public void resume(){
		if (currentReader!=null) 
			currentReader.resume();
    }
            
    public boolean isSpeak(){
    	for (TextReader textReader : textReaders) {
    		if (textReader.getState()!=STATE_READY) continue;
			if (textReader.isSpeak()) {
				return true;
			}
		}
		return false;
    }
    
    public void unload(){
    	for (TextReader textReader : textReaders) {
    		textReader.unload();
		}
    	textReaders.clear();
    }
    
	public String [] getDownloadVoicesVariants(){
    	return null;
    }
    
	public ArrayList<ArrayList<String>> getVoicesData(){
		ArrayList<ArrayList<String>> res = new ArrayList<ArrayList<String>>();
		for (TextReader textReader : textReaders) {
			if (textReader.getState()==STATE_INITIALIZATION) continue;
			ArrayList<ArrayList<String>> voicesData=textReader.getVoicesData();
			if (voicesData!=null) res.addAll(voicesData);
		}
		Collections.sort(res, new Comparator<ArrayList<String>>() {
	        @Override
	        public int compare(ArrayList<String> voiceData1, ArrayList<String> voiceData2) {
	        	return voiceData1.get(0).compareToIgnoreCase(voiceData2.get(0));
	        }
	    });
		return res;
    }
    
	public ArrayList<String> setVoiceState(String code, String string){
		ArrayList<String> voice=null;
		for (TextReader textReader : textReaders) {
			if (textReader.getState()==STATE_INITIALIZATION) continue;
			ArrayList<String> voice1=textReader.setVoiceState(code, string);
			if (voice1!=null) voice=voice1;
		}
		if (voice!=null&&string.equals("4")) {
			for (TextReader textReader : textReaders) {
				if (textReader.getState()==STATE_INITIALIZATION) continue;
				ArrayList<ArrayList<String>> voicesData = new ArrayList<ArrayList<String>>(textReader.getVoicesData());
				for (ArrayList<String> voiceAr: voicesData) {
                    if (voiceAr==null) continue;
					if (voiceAr.get(3).equals(voice.get(3))&&!code.equals(voiceAr.get(2))&&voiceAr.get(4).equals("4")) {
						textReader.setVoiceState(voiceAr.get(2), "3");
					}
				}
			}
		}		
		boolean deffound=false;
		if (voice!=null&&string.equals("3")) {
			for (TextReader textReader : textReaders) {
				if (textReader.getState()==STATE_INITIALIZATION) continue;
				ArrayList<ArrayList<String>> voicesData = textReader.getVoicesData();
				for (ArrayList<String> voiceAr: voicesData) {
					if (voiceAr.get(3).equals(voice.get(3))&&voiceAr.get(4).equals("4")) {
						deffound=true;
						break;
					}
				}
			}
			if (!deffound) {
				for (TextReader textReader : textReaders) {
					if (textReader.getState()==STATE_INITIALIZATION) continue;
					textReader.setVoiceState(code, "4");
				}
			}
		}
		return voice;
    }
    
	public ArrayList<String> getLangList(){
		ArrayList<String> res = new ArrayList<String>();
		for (TextReader textReader : textReaders) {
			if (textReader.getState()!=STATE_READY) continue;
			ArrayList<String> langs=textReader.getLangList();
			if (langs!=null) {
				for (String lang : langs) {
					if (!res.contains(lang)) {
						res.add(lang);
					}
				}
			}
		}		
		return res;		
    }
    	
	public String getSKUForVoice(String code){
		for (TextReader textReader : textReaders) {
			String res=textReader.getSKUForVoice(code);
			if (!res.equals("")) {
				return res;
			}
		}
		return "";
    }
    
	public void fixVoicesStates(ArrayList<String> purchaseSKUs){
		for (TextReader textReader : textReaders) {
			textReader.fixVoicesStates(purchaseSKUs);
		}
    }
    
    public void clearDuplicateSelection() {
        log("clearDuplicateSelection");
		for (TextReader textReader : textReaders) {
			if (textReader.getState()!=STATE_READY) continue;
			ArrayList<ArrayList<String>> voicesData = new ArrayList<ArrayList<String>>(textReader.getVoicesData());
			for (ArrayList<String> voiceAr: voicesData) {
				setVoiceState(voiceAr.get(2), voiceAr.get(4));
			}
		}
    }

	@Override
	public void onVoiceDataChanged() {
		clearDuplicateSelection();
	}

	@Override
	public int getState() {
		return state;
	}

	private void setState(int newState) {
		if (state!=newState) {
			state=newState;
            if (mReaderEventsCallback!=null) {
                mHandler.post(new Runnable() {
                    public void run() {
                        mReaderEventsCallback.onReaderStateChanged(id, state);
                    }
                });
            }
		}
	}
	
	@Override
	public void onReaderStateChanged(int id, int newState) {
		if (state!=STATE_READY) {
			if (newState==STATE_READY) {
				setState(newState);
			} else if (newState==STATE_NEED_DOWNLOAD) {
				boolean needWait=false;
				for (TextReader textReader : textReaders) {
					if (textReader.getState()==STATE_INITIALIZATION) {
						needWait=true;
					}
				}
				if (!needWait) {
					setState(STATE_NEED_DOWNLOAD);
				}
			}
		}
		if (newState==STATE_READY) {
			clearDuplicateSelection();
			onReaderLangListChanged();
		}
	}

	@Override
	public boolean onNetworkStateChanged() {
		boolean res=false;
		for (TextReader textReader : textReaders) {
			if (textReader.onNetworkStateChanged()) {
				onReaderLangListChanged();
				res=true;
			}
		}
		return res;
	}

	@Override
	public void setSpeechRate(float speech_rate) {
		for (TextReader textReader : textReaders) {
			textReader.setSpeechRate(speech_rate);
		}
	}

	@Override
	public void onReadPositionChanged(final int pos, final int len) {
		if (mReaderEventsCallback!=null) {
            mHandler.post(new Runnable(){
                public void run() {
                    mReaderEventsCallback.onReadPositionChanged(pos, len);
                }
            });
		}
	}

	@Override
	public void onReaderBeforeStartRead() {
        mHandler.post(new Runnable(){
            public void run() {
                mReaderEventsCallback.onReaderBeforeStartRead();
            }
        });
	}

	@Override
	public void onReaderCompleteRead() {
		if (mReaderEventsCallback!=null) {
            mHandler.post(new Runnable(){
                public void run() {
		        	mReaderEventsCallback.onReaderCompleteRead();
                }
            });
		}
	}

	@Override
	public void onReaderUnsupportedLang(final String lang) {
		if (mReaderEventsCallback!=null) {
            mHandler.post(new Runnable(){
                public void run() {
                    mReaderEventsCallback.onReaderUnsupportedLang(lang);
                }
            });
		}
	}

	@Override
	public void onReaderLangListChanged() {
		if (mReaderEventsCallback!=null) {
            mHandler.post(new Runnable(){
                public void run() {
                    mReaderEventsCallback.onReaderLangListChanged();
                }
            });
		}
	}
		
}

