package com.maple.speind;


import com.yandex.metrica.YandexMetrica;

import android.app.Application;

import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;

public class SpeindApplication extends Application {
    private final static Thread.UncaughtExceptionHandler mDefaultExceptionHandler = Thread.getDefaultUncaughtExceptionHandler();
    private static GoogleAnalytics analytics = null;
    private static Tracker tracker = null;

    @Override
    public void onCreate() {
        super.onCreate();
        if (getPackageName().equals("com.maple.speind")) {
            YandexMetrica.initialize(getApplicationContext(), "24913");
            analytics = GoogleAnalytics.getInstance(this);
            tracker = analytics.newTracker("UA-42364279-2");
            tracker.enableExceptionReporting(true);
            tracker.enableAdvertisingIdCollection(true);
            tracker.enableAutoActivityTracking(true);
        }
    }

    static Thread.UncaughtExceptionHandler getDefaultExceptionHandler() {
        return mDefaultExceptionHandler;
    }

    public static void reportEvent(String category, String action, String label) {
        if (tracker!=null) {
            tracker.send(new HitBuilders.EventBuilder(category, action).setLabel(label).build());
            YandexMetrica.reportEvent(action);
        }
    }

}