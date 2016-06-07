package uk.hackeurope.wrekin;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.BatteryManager;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.widget.Toast;

public class BatteryStatusReceiver extends BroadcastReceiver {
    public BatteryStatusReceiver() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        int status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
        boolean isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL;

        int chargePlug = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);
        boolean usbCharge = chargePlug == BatteryManager.BATTERY_PLUGGED_USB;
        boolean acCharge = chargePlug == BatteryManager.BATTERY_PLUGGED_AC;

        SharedPreferences app_preferences =
                PreferenceManager.getDefaultSharedPreferences(context);

        if (isCharging && app_preferences.getBoolean("switch_power", true)) {
            context.stopService(new Intent(context, BeaconService.class));
        } else {
            context.startService(new Intent(context, BeaconService.class));
        }
    }
}
