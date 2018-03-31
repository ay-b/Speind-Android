package com.speind.facebookplugin;

import android.content.Intent;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.FacebookSdk;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.facebook.HttpMethod;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;

import java.util.Arrays;

import me.speind.SpeindAPI;


public class PermissionRequest extends ActionBarActivity {

    private CallbackManager callbackManager = null;

    private String profile="";
    private SpeindAPI.InfoPoint infoPoint = null;
    private String permission = "";

    private FacebookCallback<LoginResult> callback = new FacebookCallback<LoginResult>() {
        @Override
        public void onSuccess(LoginResult loginResult) {
            if (permission!=null) {
                if (permission.equals("publish_actions")) {
                    if (infoPoint != null) {
                        SpeindAPI.InfoPointData data = infoPoint.getData(profile);
                        if (data!=null) {
                            Bundle params = new Bundle();
                            params.putString("message", getString(R.string.shared_via_speind));
                            params.putString("link", data.postURL);
                            GraphRequest request = new GraphRequest(loginResult.getAccessToken(), "/me/feed", params, HttpMethod.POST, new GraphRequest.Callback() {
                                @Override
                                public void onCompleted(GraphResponse response) {

                                }
                            });
                            request.executeAsync();
                        }
                    }
                }
            }
            finish();
        }

        @Override
        public void onCancel() {
            finish();
        }

        @Override
        public void onError(FacebookException e) {
            finish();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.permission_request_activity);
        Intent intent=getIntent();
        profile = intent.getStringExtra("profile");
        infoPoint = SpeindAPI.InfoPoint.getFromIntent(intent);
        permission = intent.getStringExtra("permission");

        if (!FacebookSdk.isInitialized()) FacebookSdk.sdkInitialize(getApplicationContext());
        callbackManager = CallbackManager.Factory.create();
        LoginManager.getInstance().registerCallback(callbackManager, callback);

        LoginManager.getInstance().logInWithPublishPermissions(PermissionRequest.this, Arrays.asList(permission));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        callbackManager.onActivityResult(requestCode, resultCode, data);
    }

}
