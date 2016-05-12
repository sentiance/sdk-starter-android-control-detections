package com.sentiance.sdkstarter.controldetections;

import android.app.Application;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.NotificationCompat;
import android.util.Log;

import com.sentiance.sdk.AuthenticationListener;
import com.sentiance.sdk.Sdk;
import com.sentiance.sdk.modules.config.SdkConfig;

public class MyApplication extends Application {
    public static final String ACTION_SDK_AUTHENTICATION_SUCCESS = "ACTION_SDK_AUTHENTICATION_SUCCESS";


    @Override
    public void onCreate() {
        super.onCreate();
        initializeSentianceSdk();
    }

    private void initializeSentianceSdk() {
        // SDK configuration
        SdkConfig config = new SdkConfig(new SdkConfig.AppCredentials(
                "YOUR_APP_ID",
                "YOUR_APP_SECRET"
        ));

        // We want to let the user manually control when to run detections, so we disable autostart
        config.setAutoStart(false);

        // Let the SDK start the service foregrounded by showing a notification. This discourages Android from killing the process.
        Intent intent = new Intent(this, MainActivity.class).setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);
        Notification notification = new NotificationCompat.Builder(this)
                .setContentTitle(getString(R.string.app_name) + " is running")
                .setContentText("Touch to open.")
                .setShowWhen(false)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_MIN)
                .build();
        config.enableStartForegrounded(notification);

        // Define authentication listener
        AuthenticationListener authenticationListener = new AuthenticationListener() {
            @Override
            public void onAuthenticationSucceeded() {
                // Called when the SDK was able to create a platform user
                Log.i("SDKStarter", "Sentiance SDK started, version: "+Sdk.getInstance(getApplicationContext()).getVersion());
                Log.i("SDKStarter", "Sentiance platform user id for this install: "+Sdk.getInstance(getApplicationContext()).user().getId().get());
                Log.i("SDKStarter", "Authorization token that can be used to query the HTTP API: Bearer "+Sdk.getInstance(getApplicationContext()).user().getAccessToken());

                LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(new Intent(ACTION_SDK_AUTHENTICATION_SUCCESS));
            }

            @Override
            // Called when the SDK could not create a platform user
            public void onAuthenticationFailed(String s) {
                // Here you should wait, inform the user to ensure an internet connection and retry initializeSentianceSdk afterwards
                Log.e("SDKStarter", "Error launching Sentiance SDK: "+s);



                // Some SDK Starter specific help
                if(s.contains("Bad Request")) {
                    Log.e("SDKStarter", "You should create a developer account on https://audience.sentiance.com/developers and afterwards register a Sentiance application on https://audience.sentiance.com/apps\n" +
                            "This will give you an application ID and secret which you can use to replace YOUR_APP_ID and YOUR_APP_SECRET in AppDelegate.m");
                }
            }
        };

        // Register this instance as authentication listener
        Sdk.getInstance(this).setAuthenticationListener(authenticationListener);

        // Initialize and start the Sentiance SDK module
        // The first time an app installs on a device, the SDK requires internet to create a Sentiance platform userid
        Sdk.getInstance(this).init(config);
    }
}

