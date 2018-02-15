package com.mercacortex.ad_trabajo_t2;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * Created by usuario on 15/02/18.
 */

public class MyBroadcastReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        switch (intent.getAction()) {
            case MyIntentService.INTENT_ACTION_SUCCESS:
                Log.d("SERVICE", "DESCARGA REALIZADA");
                break;
            case MyIntentService.INTENT_ACTION_FAILURE:
                Log.d("SERVICE", "DESCARGA FALLIDA");
                break;
        }
    }
}
