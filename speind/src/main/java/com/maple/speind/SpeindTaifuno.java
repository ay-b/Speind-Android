package com.maple.speind;

import java.io.IOException;

import android.support.v7.app.ActionBarActivity;
import android.util.Log;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Debug;
import android.os.Handler;
import android.view.Menu;
import android.view.MenuItem;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.gcm.GoogleCloudMessaging;

import me.taifuno.*;

//import ru.lifenews.speind.R;

public class SpeindTaifuno extends ActionBarActivity {
    public static final String PROPERTY_REG_ID = "registration_id";
    public static final String PROPERTY_APP_VERSION = "appVersion";
    private static final int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;

    String SENDER_ID = "346300744013";

    Handler handler = new Handler();

    Context context;
    GoogleCloudMessaging gcm;
    String regid="";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.speind_taifuno);

        context = getApplicationContext();

        Taifuno tf = Taifuno.getInstance();
        tf.setContext(context);
        tf.setApikey("a3e50355255741c0ade1643fbecf4596");

        if (checkPlayServices()) {
            gcm = GoogleCloudMessaging.getInstance(this);
            regid = getRegistrationId(context);
            if (regid.isEmpty()) {
                registerInBackground();
            } else {
                Taifuno.getInstance().regDeviceId(regid);
                Taifuno.getInstance().showChat(getSystemInfo());
            }
        } else {
            //TODO Show "No valid Google Play Services APK found."
        }

        handler.postDelayed(new Runnable(){
            @Override
            public void run() {
                finish();
            }
        }, 1000);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return super.onOptionsItemSelected(item);
    }

    private boolean checkPlayServices() {
        int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        if (resultCode != ConnectionResult.SUCCESS) {
            if (GooglePlayServicesUtil.isUserRecoverableError(resultCode)) {
                Dialog ed=GooglePlayServicesUtil.getErrorDialog(resultCode, this, PLAY_SERVICES_RESOLUTION_REQUEST);
                ed.setOnCancelListener(new OnCancelListener(){
                    @Override
                    public void onCancel(DialogInterface dialog) {
                        finish();
                    }
                });
                ed.show();
            }
            return false;
        }
        return true;
    }

    private void storeRegistrationId(Context context, String regId) {
        final SharedPreferences prefs = getGcmPreferences();
        int appVersion = getAppVersion(context);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(PROPERTY_REG_ID, regId);
        editor.putInt(PROPERTY_APP_VERSION, appVersion);
        editor.apply();
    }

    private String getRegistrationId(Context context) {
        final SharedPreferences prefs = getGcmPreferences();
        String registrationId = prefs.getString(PROPERTY_REG_ID, "");
        if (registrationId.isEmpty()) {
            return "";
        }
        int registeredVersion = prefs.getInt(PROPERTY_APP_VERSION, Integer.MIN_VALUE);
        int currentVersion = getAppVersion(context);
        if (registeredVersion != currentVersion) {
            return "";
        }
        return registrationId;
    }

    private void registerInBackground() {
        new AsyncTask<Void, Void, String>() {
            @Override
            protected String doInBackground(Void... params) {
                String msg;
                try {
                    if (gcm == null) {
                        gcm = GoogleCloudMessaging.getInstance(context);
                    }
                    regid = gcm.register(SENDER_ID);
                    msg = "Device registered, registration ID=" + regid;

                    sendRegistrationIdToBackend();

                    storeRegistrationId(context, regid);
                } catch (IOException ex) {
                    msg = "Error :" + ex.getMessage();
                }
                return msg;
            }

            @Override
            protected void onPostExecute(String msg) {
                Log.d("[---!!!---]", msg + "\n");
            }
        }.execute(null, null, null);
    }

    private static int getAppVersion(Context context) {
        try {
            PackageInfo packageInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            return packageInfo.versionCode;
        } catch (NameNotFoundException e) {
            throw new RuntimeException("Could not get package name: " + e);
        }
    }

    private static String getAppVersionStr(Context context) {
        try {
            PackageInfo packageInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            return packageInfo.versionName;
        } catch (NameNotFoundException e) {
            throw new RuntimeException("Could not get package name: " + e);
        }
    }

    private SharedPreferences getGcmPreferences() {
        return getSharedPreferences(SpeindTaifuno.class.getSimpleName(), Context.MODE_PRIVATE);
    }

    private void sendRegistrationIdToBackend() {
        if (regid != null) {
            handler.post(new Runnable(){
                @Override
                public void run() {
                    Taifuno.getInstance().regDeviceId(regid);
                    Taifuno.getInstance().showChat(getSystemInfo());
                }
            });
        }
    }

    private String getSystemInfo() {
        long max = Runtime.getRuntime().maxMemory(); //the maximum memory the app can use
        long heapSize = Runtime.getRuntime().totalMemory(); //current heap size
        long heapRemaining = Runtime.getRuntime().freeMemory(); //amount available in heap
        long nativeUsage = Debug.getNativeHeapAllocatedSize(); //is this right? I only want to account for native memory that my app is being "charged" for.  Is this the proper way to account for that?
        long remaining = max - (heapSize - heapRemaining + nativeUsage);
        String os="API Level "+android.os.Build.VERSION.SDK_INT;
        switch (android.os.Build.VERSION.SDK_INT) {
            case 14:
                os="";
                os="Android 4.0 - 4.0.2";
                break;
            case 15:
                os="Android 4.0.3 - 4.0.4";
                break;
            case 16:
                os="Android 4.1 - 4.1.2";
                break;
            case 17:
                os="Android 4.2 - 4.2.2";
                break;
            case 18:
                os="Android 4.3 - 4.3.1";
                break;
            case 19:
                os="Android 4.4 - 4.4.2";
                break;
            case 20:
                os="Android 4.4W - 4.4W.2";
                break;
            case 21:
                os="Android 5.0 - 5.0.2";
                break;
            case 22:
                os="Android 5.1 - 5.1.1";
                break;
        }
        String info = "App: "+getString(R.string.app_name)+" v"+getAppVersionStr(context)+";\n Device: "+android.os.Build.MANUFACTURER+" "+android.os.Build.MODEL+" "+android.os.Build.PRODUCT+";\n OS: "+os+";\n Free memory: "+remaining/(1024*1024)+"Mb of "+max/((1024*1024))+"Mb available for app";
        return info;
    }
}
