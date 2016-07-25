package uk.hackeurope.wrekin;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseSettings;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.BatteryManager;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.util.Log;

import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.BeaconParser;
import org.altbeacon.beacon.BeaconTransmitter;
import org.altbeacon.beacon.Identifier;
import org.altbeacon.beacon.utils.UrlBeaconUrlCompressor;

import java.net.MalformedURLException;


public class BeaconService extends Service {
    protected static final String TAG = "BeaconService";
    private static final String TEST_URL = "https://goo.gl/L3kw4Q";
    private static final int NOTIFICATION_ID = 1;

    private BeaconTransmitter mBeaconTransmitter;
    private Beacon mBeacon;

    private void sendNotificaion() {
        NotificationManager nMN = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        Notification n  = new Notification.Builder(this)
                .setContentTitle("Mobile Campaigner")
                .setContentText("Currently campaigning")
                .setSmallIcon(R.drawable.ic_info_black_24dp)
                .build();

        n.contentView.setImageViewResource(android.R.id.icon, R.mipmap.ic_launcher);
        n.flags |= Notification.FLAG_NO_CLEAR;

        nMN.notify(NOTIFICATION_ID, n);
    }

    private void cancelNotification() {
        NotificationManager nMN = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        nMN.cancel(NOTIFICATION_ID);
    }

    @Override
    public void onCreate() {
        if (mBeaconTransmitter != null) {
            return;
        }

        byte[] urlBytes;
        Log.d(TAG, "service created");

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

        Identifier identifier;
        try {
            urlBytes = UrlBeaconUrlCompressor.compress(TEST_URL);
            identifier = Identifier.fromBytes(urlBytes, 0, urlBytes.length, false);
            Log.d(TAG, "Compressed: " + TEST_URL);
        } catch (MalformedURLException exc) {
            Log.e(TAG, exc.toString());
            return;
        }

        mBeacon = new Beacon.Builder()
                .setId1(identifier.toString())
                .setTxPower(-55)
                .build();

        BeaconParser beaconParser = new BeaconParser().
                setBeaconLayout(BeaconParser.EDDYSTONE_URL_LAYOUT);
        mBeaconTransmitter = new BeaconTransmitter(
                getApplicationContext(), beaconParser);

        String power_setting = prefs.getString("beacon_power", "balanced");
        if (power_setting.equals("latency")) mBeaconTransmitter.setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY);
        if (power_setting.equals("balanced")) mBeaconTransmitter.setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_BALANCED);
        if (power_setting.equals("power")) mBeaconTransmitter.setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_POWER);
        mBeaconTransmitter.setAdvertiseTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH);

        sendNotificaion();

        if (power_setting.equals("disabled")) {
            cancelNotification();
            return;
        }

        IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatus = getApplicationContext().registerReceiver(null, ifilter);
        int status = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
        boolean isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL;

        if (prefs.getBoolean("switch_power", true) && isCharging) {
            cancelNotification();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mBeaconTransmitter.stopAdvertising();
        cancelNotification();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (!mBeaconTransmitter.isStarted()) {
            mBeaconTransmitter.startAdvertising(mBeacon, new AdvertiseCallback() {
                @Override
                public void onStartFailure(int errorCode) {
                    Log.e(TAG, "Advertisement start failed with code: "+errorCode); }
                @Override
                public void onStartSuccess(AdvertiseSettings settingsInEffect) {
                    Log.i(TAG, "Advertisement start succeeded."); }
            });
        } else {
            Log.d(TAG, "start called for already running BTLE advertiser");
        }

        return START_STICKY;
    }
}