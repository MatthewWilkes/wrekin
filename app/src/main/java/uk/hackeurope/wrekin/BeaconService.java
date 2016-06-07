package uk.hackeurope.wrekin;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseSettings;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.BatteryManager;
import android.os.Binder;
import android.os.IBinder;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.BeaconConsumer;
import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.BeaconParser;
import org.altbeacon.beacon.BeaconTransmitter;
import org.altbeacon.beacon.Identifier;
import org.altbeacon.beacon.RangeNotifier;
import org.altbeacon.beacon.Region;
import org.altbeacon.beacon.utils.UrlBeaconUrlCompressor;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.util.Arrays;
import java.util.Collection;


public class BeaconService extends Service {
    protected static final String TAG = "BeaconService";
    private BeaconManager beaconManager;

    public class LocalBinder extends Binder {
        BeaconService getService() {
            return BeaconService.this;
        }
    }
    private final IBinder mBinder = new LocalBinder();

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public void onCreate() {
        byte[] urlBytes;
        Log.i(TAG, "STARTING UP");

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

        NotificationManager nMN = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            Notification n  = new Notification.Builder(this)
                    .setContentTitle("Mobile Campaigner")
                    .setContentText("Currently campaigning")
                    .setSmallIcon(R.drawable.ic_info_black_24dp)
                    .build();
        n.contentView.setImageViewResource(android.R.id.icon, R.mipmap.ic_launcher);
        n.flags |= Notification.FLAG_NO_CLEAR;
        nMN.notify(1, n);

        Identifier identifier;
        try {
            urlBytes = UrlBeaconUrlCompressor.compress("https://goo.gl/L3kw4Q");
            identifier = Identifier.fromBytes(urlBytes, 0, urlBytes.length, false);
            Log.i(TAG, "Compressed:");
        } catch (MalformedURLException exc) {
            Log.e(TAG, exc.toString());
            return;
        }

        Beacon beacon = new Beacon.Builder()
                .setId1(identifier.toString())
                .setTxPower(-55)
                .build();

        BeaconParser beaconParser = new BeaconParser().
                setBeaconLayout(BeaconParser.EDDYSTONE_URL_LAYOUT);
        BeaconTransmitter beaconTransmitter = new BeaconTransmitter(
                getApplicationContext(), beaconParser);

        String power_setting = prefs.getString("beacon_power", "balanced");
        if (power_setting.equals("latency")) beaconTransmitter.setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY);
        if (power_setting.equals("balanced")) beaconTransmitter.setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_BALANCED);
        if (power_setting.equals("power")) beaconTransmitter.setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_POWER);
        beaconTransmitter.setAdvertiseTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH);

        if (power_setting.equals("disabled")) {
            nMN.cancel(1);
            return;
        }

        IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatus = getApplicationContext().registerReceiver(null, ifilter);
        int status = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
        boolean isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL;

        if (prefs.getBoolean("switch_power", true) && isCharging) {
            nMN.cancel(1);
            return;
        }

        beaconTransmitter.startAdvertising(beacon, new AdvertiseCallback() {
            @Override
            public void onStartFailure(int errorCode) {
                Log.e(TAG, "Advertisement start failed with code: "+errorCode); }
            @Override
            public void onStartSuccess(AdvertiseSettings settingsInEffect) {
                Log.i(TAG, "Advertisement start succeeded."); }
        });


    }
    @Override
    public void onDestroy() {
        super.onDestroy();
        NotificationManager nMN = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        nMN.cancel(1);
    }


}