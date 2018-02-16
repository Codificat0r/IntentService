package com.mercacortex.ad_trabajo_t2;

import android.app.IntentService;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.Nullable;
import android.util.Log;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.FileAsyncHttpResponseHandler;
import com.loopj.android.http.SyncHttpClient;
import com.mercacortex.ad_trabajo_t2.utils.Memoria;
import com.mercacortex.ad_trabajo_t2.utils.RestClient;
import com.mercacortex.ad_trabajo_t2.utils.Resultado;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import cz.msebera.android.httpclient.Header;

/**
 * ¡¡¡¡¡HAY QUE DECLARAR EL SERVICE EN EL MANIFEST!!!!!
 */

public class ServiceAsyncHttpResponseHandler extends IntentService {
    private int NOTIFICATION_ID;

    public ServiceAsyncHttpResponseHandler() {
        super("ServiceAsyncHttpResponseHandler");
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        Bundle b = intent.getExtras();
        final String origin = b.getString(MyIntentService.INTENT_DATA_SOURCE);
        String destination = b.getString(MyIntentService.INTENT_DATA_DESTINATION);
        if (destination.equals("frases.txt")) {
            NOTIFICATION_ID = 1;
            Log.d("SERVICE", "onHandleIntent: " + destination);
        } else {
            NOTIFICATION_ID = 0;
            Log.d("SERVICE", "onHandleIntent: " + destination);
        }
        File archivo = new File(Environment.getExternalStorageDirectory().getAbsolutePath(), destination);
        //Debe ser sincrono, no asincrono, para que se espere el servicio a la respuesta. Es
        //decir, que se ejecute dentro del service y no de manera ASINCRONA fuera del service.
        //Aunque el HANDLER sea asincrono, el cliente si lo es y se quedará en la linea del
        //client.get esperando. Si el cliente fuera asincrono sigue ejecutando las lineas del service
        //en lugar de esperarse en esa linea y el service termina y no hay cliente http que reciba
        //respuesta alguna del HANDLER asincrono.
        SyncHttpClient cliente = new SyncHttpClient();
        //Para no fundir la bateria con el service ponemos los timeout y las retries
        cliente.setTimeout(2500);
        cliente.setMaxRetriesAndTimeout(2, 3000);
        cliente.get(origin, new FileAsyncHttpResponseHandler(archivo) {

            @Override
            public void onProgress(int bytesWritten, int totalSize) {
                super.onProgress(bytesWritten, totalSize);
                long progressPercentage = (long)100*bytesWritten/totalSize;
                Log.d("SERVICE", "onProgress: " + progressPercentage);

                showForegroundNotification("Descargando archivo . . ." + origin, (int)progressPercentage);
            }

            @Override
            public void onFailure(int statusCode, org.apache.http.Header[] headers, Throwable throwable, File file) {
                //Nos comunicamos con las aplicaciones desde el service con intents mandados
                //con broadcast.
                Intent i = new Intent(MyIntentService.INTENT_ACTION_FAILURE);
                //Decimos cual ha fallado
                i.putExtra(MyIntentService.INTENT_DATA_SOURCE, origin);
                sendBroadcast(i);
            }

            @Override
            public void onSuccess(int statusCode, org.apache.http.Header[] headers, File file) {
                Intent i = new Intent(MyIntentService.INTENT_ACTION_SUCCESS);
                //Decimos cual ha ido bien
                i.putExtra(MyIntentService.INTENT_DATA_SOURCE, origin);
                sendBroadcast(i);
            }
        });
    }

    private void showForegroundNotification(String contentText, int progress) {
        //Creamos el intent que dirá que
        Intent showTaskIntent = new Intent(getApplicationContext(), MainActivity.class);
        showTaskIntent.setAction(Intent.ACTION_MAIN);
        showTaskIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        showTaskIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        PendingIntent contentIntent = PendingIntent.getActivity(
                getApplicationContext(),
                0,
                showTaskIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);

        Notification notification = new Notification.Builder(getApplicationContext())
                .setContentTitle(getString(R.string.app_name))
                .setContentText(contentText)
                .setSmallIcon(R.drawable.placeholder)
                .setWhen(System.currentTimeMillis())
                .setContentIntent(contentIntent)
                .setProgress(100, progress, false)
                .build();
        Log.d("SERVICE", "showForegroundNotification: " + progress);
        startForeground(NOTIFICATION_ID, notification);
    }

}
