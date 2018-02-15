package com.mercacortex.ad_trabajo_t2;

import android.app.IntentService;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.Nullable;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.FileAsyncHttpResponseHandler;
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


    public ServiceAsyncHttpResponseHandler() {
        super("ServiceAsyncHttpResponseHandler");
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        Bundle b = intent.getExtras();
        String origin = b.getString(MyIntentService.INTENT_DATA_SOURCE);
        String destination = b.getString(MyIntentService.INTENT_DATA_DESTINATION);
        File archivo = new File(Environment.getExternalStorageDirectory().getAbsolutePath(), destination);
        AsyncHttpClient cliente = new AsyncHttpClient();
        cliente.get(origin, new FileAsyncHttpResponseHandler(archivo) {
            @Override
            public void onFailure(int statusCode, Header[] headers, Throwable throwable, File file) {
                //Nos comunicamos con las aplicaciones desde el service con intents mandados
                //con broadcast.
                Intent i = new Intent(MyIntentService.INTENT_ACTION_FAILURE);
                sendBroadcast(i);
            }

            @Override
            public void onSuccess(int statusCode, Header[] headers, File respuesta) {
                Intent i = new Intent(MyIntentService.INTENT_ACTION_SUCCESS);
                sendBroadcast(i);
            }

        });
    }

}
