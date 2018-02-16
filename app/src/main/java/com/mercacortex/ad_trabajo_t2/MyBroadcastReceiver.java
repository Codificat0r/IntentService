package com.mercacortex.ad_trabajo_t2;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * Este Broadcast receiver lo hariamos cuando el origen es una notificacion.
 * Esto se debe porque podemos tener muchas notificaciones y el broadcastreceiver es
 * el que identifica de cual proviene cada intent
 *
 * Si queremos que un service sin notificacion se comunique directamente con la activity
 * para avisarle de que ha finalizado algo, la activity tendrá un LocalBroadcast que
 * recibirá el intent y será un objeto BroadcastReceiver que registraremos y tendrá el ciclo de vida
 * que tenga la actividad.
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
