package com.sentiance.sdkstarter.controldetections;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.text.format.Formatter;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.sentiance.sdk.InitState;
import com.sentiance.sdk.OnStartFinishedHandler;
import com.sentiance.sdk.SdkStatus;
import com.sentiance.sdk.Sentiance;
import com.sentiance.sdk.trip.StartTripCallback;
import com.sentiance.sdk.trip.StopTripCallback;
import com.sentiance.sdk.trip.TripType;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements OnStartFinishedHandler {
    private Button controlDetectionsButton;
    private Button controlTripButton;
    private ListView statusList;

    private final TripStartStopCallback mTripStartStopCallback = new TripStartStopCallback();
    private final BroadcastReceiver statusUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive (Context context, Intent intent) {
            refreshStatus();
            updateButtonTexts();
        }
    };

    @Override
    protected void onCreate (Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (new PermissionManager(this).getNotGrantedPermissions().size() > 0) {
            startActivity(new Intent(this, PermissionCheckActivity.class));
        }

        setContentView(R.layout.activity_main);

        controlDetectionsButton = findViewById(R.id.controlDetectionsButton);
        controlTripButton = findViewById(R.id.controlTripButton);
        statusList = findViewById(R.id.statusList);
    }

    @Override
    protected void onResume () {
        super.onResume();

        // Our MyApplication broadcasts when the SDK authentication was successful
        LocalBroadcastManager.getInstance(this).registerReceiver(statusUpdateReceiver, new IntentFilter(MyApplication.ACTION_SENTIANCE_STATUS_UPDATE));

        refreshStatus();

        if (Sentiance.getInstance(this).getInitState() != InitState.INITIALIZED) {
            controlDetectionsButton.setEnabled(false);
            controlTripButton.setEnabled(false);
        }
        updateButtonTexts();
    }

    @Override
    protected void onPause () {
        super.onPause();

        LocalBroadcastManager.getInstance(getApplicationContext()).unregisterReceiver(statusUpdateReceiver);
    }

    private void refreshStatus () {
        List<String> statusItems = new ArrayList<>();

        if (Sentiance.getInstance(this).getInitState() == InitState.INITIALIZED) {
            controlDetectionsButton.setEnabled(true);
            controlTripButton.setEnabled(true);

            statusItems.add("SDK version: " + Sentiance.getInstance(this).getVersion());
            statusItems.add("User ID: " + Sentiance.getInstance(this).getUserId());

            SdkStatus sdkStatus = Sentiance.getInstance(this).getSdkStatus();

            statusItems.add("Start status: " + sdkStatus.startStatus.name());
            statusItems.add("Can detect: " + sdkStatus.canDetect);
            statusItems.add("Remote enabled: " + sdkStatus.isRemoteEnabled);
            statusItems.add("Activity perm granted: " + sdkStatus.isActivityRecognitionPermGranted);
            statusItems.add("Location perm granted: " + sdkStatus.isLocationPermGranted);
            statusItems.add("Location setting: " + sdkStatus.locationSetting.name());

            statusItems.add(formatQuota("Wi-Fi", sdkStatus.wifiQuotaStatus, Sentiance.getInstance(this).getWiFiQuotaUsage(), Sentiance.getInstance(this).getWiFiQuotaLimit()));
            statusItems.add(formatQuota("Mobile data", sdkStatus.mobileQuotaStatus, Sentiance.getInstance(this).getMobileQuotaUsage(), Sentiance.getInstance(this).getMobileQuotaLimit()));
            statusItems.add(formatQuota("Disk", sdkStatus.diskQuotaStatus, Sentiance.getInstance(this).getDiskQuotaUsage(), Sentiance.getInstance(this).getDiskQuotaLimit()));

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                statusItems.add("Battery optimization enabled: " + sdkStatus.isBatteryOptimizationEnabled);
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                statusItems.add("Battery saving enabled: " + sdkStatus.isBatterySavingEnabled);
            }
            if (Build.VERSION.SDK_INT >= 28) {
                statusItems.add("Background processing restricted: " + sdkStatus.isBackgroundProcessingRestricted);
            }
        }
        else {
            statusItems.add("SDK not initialized");
        }

        statusList.setAdapter(new ArrayAdapter<>(this, R.layout.list_item_status, R.id.textView, statusItems));
    }

    private String formatQuota (String name, SdkStatus.QuotaStatus status, long bytesUsed, long bytesLimit) {
        return String.format(Locale.US, "%s quota: %s / %s (%s)",
                name,
                Formatter.formatShortFileSize(this, bytesUsed),
                Formatter.formatShortFileSize(this, bytesLimit),
                status.name());
    }

    public void onControlDetectionsButtonClicked (View view) {
        Sentiance sentiance = Sentiance.getInstance(getApplicationContext());
        if (sentiance.getInitState() != InitState.INITIALIZED) {
            return;
        }

        if (sentiance.getSdkStatus().startStatus == SdkStatus.StartStatus.STARTED) {
            sentiance.stop();
        }
        else if (sentiance.getSdkStatus().startStatus == SdkStatus.StartStatus.NOT_STARTED) {
            sentiance.start(this);
        }

        updateButtonTexts();
    }

    public void onControlTripButtonClicked (View view) {
        Sentiance sentiance = Sentiance.getInstance(getApplicationContext());
        if (sentiance.getInitState() != InitState.INITIALIZED || !isSdkStarted()) {
            return;
        }

        if (sentiance.isTripOngoing(TripType.EXTERNAL_TRIP)) {
            stopTrip();
        }
        else {
            startTrip();
        }

        controlTripButton.setEnabled(false);
        updateButtonTexts();
    }

    private void startTrip () {
        Sentiance.getInstance(getApplicationContext()).startTrip(null, null, mTripStartStopCallback);
    }

    private void stopTrip () {
        Sentiance.getInstance(getApplicationContext()).startTrip(null, null, mTripStartStopCallback);
    }

    private boolean isSdkStarted () {
        return Sentiance.getInstance(this).getInitState() == InitState.INITIALIZED &&
                Sentiance.getInstance(this).getSdkStatus().startStatus == SdkStatus.StartStatus.STARTED;
    }

    @Override
    public void onStartFinished (SdkStatus sdkStatus) {
        controlDetectionsButton.setEnabled(true);
        updateButtonTexts();
    }

    private void updateButtonTexts () {
        if (Sentiance.getInstance(this).getInitState() != InitState.INITIALIZED) {
            return;
        }

        Sentiance sentiance = Sentiance.getInstance(getApplicationContext());
        SdkStatus sdkStatus = sentiance.getSdkStatus();

        if (sdkStatus.startStatus == SdkStatus.StartStatus.STARTED ||
                sdkStatus.startStatus == SdkStatus.StartStatus.PENDING) {
            controlDetectionsButton.setText(R.string.stop_detections);
        }
        else {
            controlDetectionsButton.setText(R.string.start_detections);
        }

        if (sentiance.isTripOngoing(TripType.EXTERNAL_TRIP)) {
            controlTripButton.setText(R.string.stop_trip);
        }
        else {
            controlTripButton.setText(R.string.start_trip);
        }
    }

    private class TripStartStopCallback implements StopTripCallback, StartTripCallback {

        @Override
        public void onSuccess () {
            handleTripStartStop();
        }

        @Override
        public void onFailure (@Nullable SdkStatus sdkStatus) {
            handleTripStartStop();
        }

        private void handleTripStartStop () {
            updateButtonTexts();
            controlTripButton.setEnabled(true);
        }
    }
}
