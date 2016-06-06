package uk.hackeurope.wrekin;

import android.app.Activity;
import android.app.Service;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseSettings;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

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

    @Override
    public IBinder onBind (Intent intent) {
        return new Binder();
    }

    @Override
    public void onCreate() {
        byte[] urlBytes;
        Log.i(TAG, "STARTING UP");
        Identifier identifier;
        try {
            urlBytes = UrlBeaconUrlCompressor.compress("https://goo.gl/BgsudZ");
            identifier = Identifier.fromBytes(urlBytes, 0, urlBytes.length, false);
            Log.i(TAG, "Compressed:");
        } catch (MalformedURLException exc) {
            Log.e(TAG, exc.toString());
            return;
        }

        Beacon beacon = new Beacon.Builder()
                .setId1(identifier.toString())
                .setManufacturer(0x00E0)
                .setTxPower(10)
                .setDataFields(Arrays.asList(new Long[] {0l})) // Remove this for beacon layouts without d: fields
                .build();

        BeaconParser beaconParser = new BeaconParser().
                setBeaconLayout(BeaconParser.EDDYSTONE_URL_LAYOUT);
        BeaconTransmitter beaconTransmitter = new BeaconTransmitter(
                getApplicationContext(), beaconParser);
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
    }


}