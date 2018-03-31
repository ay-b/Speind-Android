package com.maple.speind;

import java.util.ArrayList;

import android.content.Context;
import android.os.Handler;
import android.util.Log;

abstract class TextReader implements ITextReader {	
	private Handler mHandler = new Handler();

	private int state = STATE_INITIALIZATION;
	public int id=0;
	private boolean isSingle = true;
	private boolean mWaitAfterRead = false;
	private IReaderEventsCallback mReaderEventsCallback=null; 
	
	private Runnable completeReadRunnable = new Runnable() {
		@Override
		public void run() {
			mReaderEventsCallback.onReaderCompleteRead();
		}	        			
	};
	
	private class MRunnuble implements Runnable{
		private String text="";
		private String lang="";
		public void setData(String pText, String pLang) {
			text=pText;
			lang=pLang;
		}
		@Override
		public void run() {
			speak(text, lang);
		}		
	}
	private MRunnuble startReadRunnable = new MRunnuble();
	
	public TextReader(Context context, IReaderEventsCallback readerEventsCallback, int rid) {
		mReaderEventsCallback=readerEventsCallback;
		id=rid;
	}
	
	public void setSingle(boolean single) {
		isSingle=single;
	}
	
	public boolean isSingle() {
		return isSingle;
	}
	
	public void setState(int newState) {
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
	
	public int getState() {
		return state;
	}
	
    public void sendReadPosition(final int pos, final int len) {
    	if (mReaderEventsCallback!=null) {
    		mHandler.post(new Runnable(){
				public void run() {
		    		mReaderEventsCallback.onReadPositionChanged(pos, len);
				}
			});
    	}
    }
    
    public void sendCompleteRead() {
    	if (mReaderEventsCallback!=null) {    		
        	if (mWaitAfterRead) {
        		mHandler.postDelayed(completeReadRunnable, 5000);
        	} else {
        		mHandler.postDelayed(completeReadRunnable, 1000);
        	}    		    		
    	}
    }

    private void sendBeforeStartRead() {
    	if (mReaderEventsCallback!=null) {
    		mHandler.post(new Runnable(){
				public void run() {
		    		mReaderEventsCallback.onReaderBeforeStartRead();
				}
			});
    	}
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

    public void sendLangListChanged() {
    	if (mReaderEventsCallback!=null) {
    		mHandler.post(new Runnable(){
				public void run() {
		    		mReaderEventsCallback.onReaderLangListChanged();  		    		
				}
			});
    	}
    }
    
	public void speak(final String text, final String lang, boolean waitAfterRead) {
		mHandler.removeCallbacks(startReadRunnable);
		if (!isSpeak()) {
			mWaitAfterRead=waitAfterRead;
			sendBeforeStartRead();
			startReadRunnable.setData(text, lang);
			mHandler.postDelayed(startReadRunnable, 1000);			
		}
	}
    
	public void stop() {
		mHandler.removeCallbacks(startReadRunnable);
		mHandler.removeCallbacks(completeReadRunnable);
		if (isSpeak()) stopSpeak();
	}

	public void pause() {
		mHandler.removeCallbacks(startReadRunnable);
		mHandler.removeCallbacks(completeReadRunnable);
		if (isSpeak()) pauseSpeak();
	}

	public abstract void speak(String text, String lang);
	public abstract void stopSpeak();
	public abstract void pauseSpeak();
	public abstract boolean containsDefaultVoice(String lang);
	
}

