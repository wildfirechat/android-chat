package cn.wildfirechat.remote;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class RecoverReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        // do nothing
        Log.e("wfc", "main process crashed, to restart");
    }
}
