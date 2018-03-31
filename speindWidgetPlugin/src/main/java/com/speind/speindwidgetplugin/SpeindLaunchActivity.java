package com.speind.speindwidgetplugin;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import me.speind.SpeindAPI;


public class SpeindLaunchActivity extends ActionBarActivity {

    private class SpeindServiceInfo {
        public String packageName = "";
        public Drawable icon = null;
        public String title = "";
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.speind_launch);

        (new AsyncTask<Void, Void, Boolean>(){
            ArrayList<SpeindServiceInfo> service_infos = new ArrayList();

            @Override
            protected Boolean doInBackground(Void... params) {
                PackageManager mngr = getPackageManager();
                List<PackageInfo> list = mngr.getInstalledPackages(PackageManager.GET_SERVICES);
                for (PackageInfo packageInfo : list) {
                    ServiceInfo services[]=packageInfo.services;
                    if (services!=null) {
                        for (int i=0; i<services.length;i++) {
                            if (services[i].name.endsWith(".SpeindService")) {
                                SpeindServiceInfo serviceInfo = new SpeindServiceInfo();
                                serviceInfo.packageName = services[i].packageName;
                                serviceInfo.icon = packageInfo.applicationInfo.loadIcon(mngr);
                                serviceInfo.title = packageInfo.applicationInfo.loadLabel(mngr).toString();
                                service_infos.add(serviceInfo);
                            }
                        }
                    }
                }
                return service_infos.size()>0;
            }

            @Override
            protected void onPostExecute(Boolean param) {
                if (param) {
                    LinearLayout servicesItems = (LinearLayout) findViewById(R.id.servicesItems);
                    if (servicesItems!=null) {
                        for (final SpeindServiceInfo service_info : service_infos) {
                            View item=getLayoutInflater().inflate(R.layout.speind_service_item, null);
                            if (item!=null) {
                                ImageView logo = (ImageView) item.findViewById(R.id.logo);
                                TextView title = (TextView) item.findViewById(R.id.title);
                                if (logo!=null) {
                                    logo.setImageDrawable(service_info.icon);
                                }
                                if (title!=null) {
                                    title.setText(service_info.title);
                                }
                                item.setOnClickListener(new View.OnClickListener(){
                                    @Override
                                    public void onClick(View arg0) {
                                        Log.e("!!!", "! "+service_info.packageName);
                                        Intent intent = SpeindAPI.createIntent(service_info.packageName);
                                        intent.putExtra("isWidget", true);
                                        startService(intent);
                                        Intent intent1 = new Intent(SpeindLaunchActivity.this, SpeindWidget.class);
                                        intent1.setAction(SpeindWidgetService.STATE_CHANGED);
                                        intent1.putExtra(SpeindWidgetService.PARAM_SPEIND_STATE, 0);
                                        intent1.putExtra(SpeindAPI.PARAM_SERVICE_PACKAGE_NAME, service_info.packageName);
                                        sendBroadcast(intent1);
                                        finish();
                                    }
                                });
                                servicesItems.addView(item);
                            }
                        }
                    }
                }
            }
        }).execute();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        //int id = item.getItemId();
        return super.onOptionsItemSelected(item);
    }
}
