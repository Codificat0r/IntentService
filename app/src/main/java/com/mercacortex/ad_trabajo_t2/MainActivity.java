package com.mercacortex.ad_trabajo_t2;

import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Environment;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.FileAsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;
import com.loopj.android.http.TextHttpResponseHandler;
import com.mercacortex.ad_trabajo_t2.utils.Memoria;
import com.mercacortex.ad_trabajo_t2.utils.RestClient;
import com.mercacortex.ad_trabajo_t2.utils.Resultado;
import com.squareup.picasso.Picasso;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Date;

import cz.msebera.android.httpclient.Header;

/**
 * Crear una aplicación que permita mostrar una secuencia de imágenes y frases en pantalla.
 * Se introducirán las rutas al fichero con enlaces a imágenes en la red y
 * al fichero con frases para poder realizar su descarga.
 * Por defecto, los ficheros de imágenes y frases estarán situados en alumno.mobi/~alumno/superior/primerapellido/
 * (cada alumno preparará los ficheros necesarios en su carpeta).
 * <p>
 * El fichero de imágenes contendrá en cada línea un enlace a una imagen y
 * el de frases tendrá en cada línea una frase célebre. Pueden ser diferentes el número de imágenes y el de frases.
 * ejemplo de fichero imagenes.txt:
 * https://i.imgur.com/tGbaZCY.jpg
 * http://192.168.2.50/imagen/foto.jpg
 * http://192.168.2.50/noexiste.png
 * https://i.imgur.com/MU2dD8E.jpg
 * <p>
 * ejemplo de fichero frases.txt:
 * La mayoría de los sueños no se viven, se roncan (Poncela)
 * Que hablen de uno es espantoso, pero hay algo peor: que no hablen (Wilde)
 * La vida es aquello que te va sucediendo mientras te empeñas en hacer otros planes (Lennon)
 * Vive como si fueras a morir mañana. Aprende como si fueras a vivir siempre (Gandhi)
 * No esperes a ser valiente para actuar, actúa como si ya fueras valiente (Alfonso Alcántara)
 * <p>
 * Cuando se pulse el botón de descarga, se mostrarán una a una las imágenes y frases descargadas,
 * de forma automática cada cierto tiempo. Ese tiempo (en segundos) estará almacenado en el fichero /raw/intervalo.txt.
 * <p>
 * Además, se añadirán al fichero errores.txt situado en un servidor web
 * (en Internet:  alumno.mobi/~alumno/superior/primerapellido/)
 * los errores que se hayan producido:
 * no se puede descargar el fichero de imágenes o el de frases
 * no se puede descargar alguna imagen, etc.
 * <p>
 * Por cada error producido, se añadirá al fichero errores.txt una
 * línea con la ruta al archivo que se quiere descargar,
 * la fecha y hora de acceso y la causa del error
 * (fallo en el servidor web, no existe el fichero, . . . ).
 */
public class MainActivity extends AppCompatActivity {

    private EditText edtImagenes;
    private EditText edtFrases;
    private TextView txvFrases;
    private ImageView imvImagen;
    private Memoria memoria;
    private ProgressDialog progress;

    private String frases, imagenes;
    private boolean frasesDescargadas = false;
    private boolean imagenesDescargadas = false;
    private long intervalo = 5000L;

    private CountDownTimer timer;

    private static final String FICHERO_FRASES = "frases.txt";
    private static final String FICHERO_IMAGENES = "imagenes.txt";
    private static final String FICHERO_INTERVALO = "intervalo";

    private static final long DURACION = 120000; //120 segundos

    /**
     * Vamos a crear un "handler"/manejador de intent propio de la actividad. Esto es un
     * LocalBroadcast. Lo registraremos desde codigo
     */

    BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()) {
                case MyIntentService.INTENT_ACTION_SUCCESS:
                    //Le pasamos el fichero que ha fallado en su descarga al metodo que sube
                    //el error
                    //subirError(intent.getExtras().getString(MyIntentService.INTENT_DATA_SOURCE));
                    Log.d("SERVICE", "DESCARGA REALIZADA DE " + intent.getExtras().getString(MyIntentService.INTENT_DATA_SOURCE));
                    break;
                case MyIntentService.INTENT_ACTION_FAILURE:
                    Log.d("SERVICE", "DESCARGA FALLIDA DE " + intent.getExtras().getString(MyIntentService.INTENT_DATA_SOURCE));
                    //Ahora le decimos al metodo que cambiará las frases e imagenes. Le pasamos el fichero
                    //que ha tenido exito que viene en el intent para que sepa que va a mostrar.
                    //change()
                    break;
            }
        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        //Creamos el intent filter para los distintos intent que podemos recibir
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(MyIntentService.INTENT_ACTION_FAILURE);
        intentFilter.addAction(MyIntentService.INTENT_ACTION_SUCCESS);
        //Registramos el BroadcastReceiver de forma local, en tiempo de ejecución
        LocalBroadcastManager.getInstance(this).registerReceiver(broadcastReceiver, intentFilter);
    }

    //Para este caso al pausarse la actividad no tiene sentido que reciba nada ya que no se
    //está mostrando al usuario.
    @Override
    protected void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(broadcastReceiver);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        memoria = new Memoria(this);
        progress = new ProgressDialog(this);

        edtFrases = findViewById(R.id.edtFrases);
        edtImagenes = findViewById(R.id.edtImagenes);
        imvImagen = findViewById(R.id.imvImagen);
        txvFrases = findViewById(R.id.txvFrases);

        Resultado resultado = memoria.leerRaw(FICHERO_INTERVALO);
        if (resultado.getCodigo())
            intervalo = Long.parseLong(resultado.getContenido());
    }

    /**
     * Lo llama el botón de descarga
     *
     * @param view Botón
     */
    public void onClick(View view) {
        //Comprueba primero que se haga la descarga antes de cargar las imágenes
            download(edtFrases.getText().toString(), FICHERO_FRASES);
            download(edtImagenes.getText().toString(), FICHERO_IMAGENES);
    }

    /**
     * Crea un temporizador de 2 minutos que cambia de imagen y frase según
     * el intervalo indicado en el archivo intervalos.txt
     */
    private void onDownLoadFinished() {
        //Comprueba que se hayan realizado las descargas antes de mostrar nada
        if (frasesDescargadas && imagenesDescargadas)
            timer = new CountDownTimer(DURACION, intervalo) {
                String[] frasesArchivo = frases.split("\n");
                String[] rutasImagenes = imagenes.split("\n");
                int contador = 0;

                public void onTick(long millisUntilFinished) {
                    contador++;
                    /*cambiaImagenFrase(
                            frasesArchivo[contador % frasesArchivo.length],
                            rutasImagenes[contador % rutasImagenes.length]
                    );*/
                }

                public void onFinish() {
                    Toast.makeText(MainActivity.this, "Mostradas todas las imágenes. ¡2 veces!", Toast.LENGTH_LONG).show();
                }
            }.start();
    }

    @Override
    protected void onStop() {
        super.onStop();
        //timer.cancel();
    }

    /**
     * Descarga desde la ruta al fichero local indicados
     *
     */
    private void download(String origen, String destino) {
        if (!origen.startsWith("http://") && !origen.startsWith("https://")) {
            origen = "http://" + origen;
        }
        Intent intent = new Intent(MainActivity.this, ServiceAsyncHttpResponseHandler.class);
        intent.putExtra(MyIntentService.INTENT_DATA_SOURCE, origen);
        intent.putExtra(MyIntentService.INTENT_DATA_DESTINATION, destino);
        startService(intent);
    }

    /*private void cambiaImagenFrase(String frase, final String miRuta) {
        txvFrases.setText(frase);
        RestClient.get(miRuta, new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                Picasso.with(MainActivity.this)
                        .load(miRuta)
                        .error(R.drawable.error)
                        .placeholder(R.drawable.placeholder)
                        .into(imvImagen);
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
            }
        });
    }*/

    /**
     * Clase que inicia la descarga de los ficheros necesarios.
     */

    class FileAsyncTask extends AsyncTask<String, Void, String> {
        private File archivo;

        private RestClient restClient;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            restClient = new RestClient();
            progress.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            progress.setMessage("Conectando . . .");
            progress.setOnCancelListener(new DialogInterface.OnCancelListener() {
                public void onCancel(DialogInterface dialog) {
                    restClient.cancelRequests(MainActivity.this, true);
                }
            });
            progress.show();
        }

        @Override
        protected String doInBackground(String... args) {
            return null;
        }

        @Override
        protected void onPostExecute(String destino) {
            super.onPostExecute(destino);
            progress.dismiss();
            writeInMemory(archivo, destino);
        }

        @Override
        protected void onCancelled(String destino) {
            super.onCancelled(destino);
            progress.dismiss();
        }
    }

    /**
     * Escribe en memoria el contenido de un fichero
     */

    public void writeInMemory(File archivo, String localPath) {
        try {
            BufferedReader reader = new BufferedReader(new FileReader(archivo));
            StringBuilder content = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null)
                content.append(line).append("\n");
            reader.close();
            if (memoria.disponibleEscritura()) {
                memoria.escribirExterna(localPath, content.toString(), false, Memoria.UTF8);
            } else {}
        } catch (IOException e) {}
        catch (Exception e) {}
        progress.dismiss();
    }
}


