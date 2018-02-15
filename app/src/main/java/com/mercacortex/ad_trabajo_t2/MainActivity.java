package com.mercacortex.ad_trabajo_t2;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
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
 *  Crear una aplicación que permita mostrar una secuencia de imágenes y frases en pantalla.
 *  Se introducirán las rutas al fichero con enlaces a imágenes en la red y
 *  al fichero con frases para poder realizar su descarga.
 *  Por defecto, los ficheros de imágenes y frases estarán situados en alumno.mobi/~alumno/superior/primerapellido/
 *  (cada alumno preparará los ficheros necesarios en su carpeta).
 *
 *  El fichero de imágenes contendrá en cada línea un enlace a una imagen y
 *  el de frases tendrá en cada línea una frase célebre. Pueden ser diferentes el número de imágenes y el de frases.
 *  ejemplo de fichero imagenes.txt:
 *      https://i.imgur.com/tGbaZCY.jpg
 *      http://192.168.2.50/imagen/foto.jpg
 *      http://192.168.2.50/noexiste.png
 *      https://i.imgur.com/MU2dD8E.jpg
 *
 *  ejemplo de fichero frases.txt:
 *      La mayoría de los sueños no se viven, se roncan (Poncela)
 *      Que hablen de uno es espantoso, pero hay algo peor: que no hablen (Wilde)
 *      La vida es aquello que te va sucediendo mientras te empeñas en hacer otros planes (Lennon)
 *      Vive como si fueras a morir mañana. Aprende como si fueras a vivir siempre (Gandhi)
 *      No esperes a ser valiente para actuar, actúa como si ya fueras valiente (Alfonso Alcántara)
 *
 *  Cuando se pulse el botón de descarga, se mostrarán una a una las imágenes y frases descargadas,
 *  de forma automática cada cierto tiempo. Ese tiempo (en segundos) estará almacenado en el fichero /raw/intervalo.txt.
 *
 *  Además, se añadirán al fichero errores.txt situado en un servidor web
 *  (en Internet:  alumno.mobi/~alumno/superior/primerapellido/)
 *  los errores que se hayan producido:
 *      no se puede descargar el fichero de imágenes o el de frases
 *      no se puede descargar alguna imagen, etc.
 *
 *  Por cada error producido, se añadirá al fichero errores.txt una
 *  línea con la ruta al archivo que se quiere descargar,
 *  la fecha y hora de acceso y la causa del error
 *  (fallo en el servidor web, no existe el fichero, . . . ).
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
    private static final String FICHERO_ERROR = "errores.txt";

    private static final long DURACION = 120000; //120 segundos

    private static final String WEB = "192.168.0.139/acceso/php/upload.php";
    private static final String WEB_ERROR = "192.168.0.139/acceso/php/errores.txt";
    private static final String PASSWORD = "123";

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
        else
            descargaError("Error leyendo archivo de intervalo");
    }

    /**
     * Lo llama el botón de descarga
     * @param view Botón
     */
    public void onClick(View view) {
        //Comprueba primero que se haga la descarga antes de cargar las imágenes
        if(!frasesDescargadas || !imagenesDescargadas) {
            download(edtFrases.getText().toString(), FICHERO_FRASES);
            download(edtImagenes.getText().toString(), FICHERO_IMAGENES);
            onDownLoadFinished();
        } else
            onDownLoadFinished();
    }
    /**
     * Crea un temporizador de 2 minutos que cambia de imagen y frase según
     * el intervalo indicado en el archivo intervalos.txt
     */
    private void onDownLoadFinished() {
        //Comprueba que se hayan realizado las descargas antes de mostrar nada
        if(frasesDescargadas && imagenesDescargadas)
            timer = new CountDownTimer(DURACION, intervalo) {
                String[] frasesArchivo = frases.split("\n");
                String[] rutasImagenes = imagenes.split("\n");
                int contador = 0;

                public void onTick(long millisUntilFinished) {
                    contador++;
                    cambiaImagenFrase(
                            frasesArchivo[contador % frasesArchivo.length],
                            rutasImagenes[contador % rutasImagenes.length]
                    );
                }
                public void onFinish() {
                    Toast.makeText(getApplicationContext(), "Mostradas todas las imágenes. ¡2 veces!", Toast.LENGTH_LONG).show();
                }
            }.start();
    }

    @Override
    protected void onStop() {
        super.onStop();
        timer.cancel();
    }

    /**
     * Descarga desde la ruta al fichero local indicados
     * @param path Ruta remota del archivo de texto origen
     * @param localPath Ruta local del archivo de texto destino
     */
    private void download(String path, final String localPath) {
        if (!path.startsWith("http://") && !path.startsWith("https://"))
            path = "http://" + path;
        RestClient.get(path, new FileAsyncHttpResponseHandler(this) {
            @Override
            public void onStart() {
                progress.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                progress.setMessage("Conectando . . .");
                progress.setOnCancelListener(new DialogInterface.OnCancelListener() {
                    public void onCancel(DialogInterface dialog) {
                        RestClient.cancelRequests(getApplicationContext(), true);
                    }
                });
                progress.show();
            }
            @Override
            public void onFailure(int statusCode, Header[] headers, Throwable throwable, File file) {
                progress.dismiss();
                descargaError("Fallo en la conexión");
            }
            @Override
            public void onSuccess(int statusCode, Header[] headers, File response) {
                progress.dismiss();
                try {
                    BufferedReader reader = new BufferedReader(new FileReader(file));
                    StringBuilder content = new StringBuilder();
                    String line;
                    while((line = reader.readLine()) != null)
                        content.append(line).append("\n");
                    reader.close();
                    if (memoria.disponibleEscritura()) {
                        memoria.escribirExterna(localPath, content.toString(), false, Memoria.UTF8);
                        //Toast.makeText(MainActivity.this, "Escritura con éxito en: " + localPath, Toast.LENGTH_SHORT).show();
                        if (memoria.disponibleLectura()) {
                            Resultado resultado = memoria.leerExterna(localPath, Memoria.UTF8);
                            switch (localPath){
                                case FICHERO_FRASES:
                                    if(resultado.getCodigo()) {
                                        frases = resultado.getContenido();
                                        frasesDescargadas = resultado.getCodigo();
                                    }
                                    break;
                                case FICHERO_IMAGENES:
                                    if(resultado.getCodigo()) {
                                        imagenesDescargadas = resultado.getCodigo();
                                        imagenes = resultado.getContenido();
                                    }
                                    break;
                            }
                            onDownLoadFinished();
                        } else
                            descargaError("No se leyó el archivo");
                    } else
                        descargaError("No se escribió el archivo");
                } catch (IOException e) {
                    descargaError("Fallo de lectura del archivo");
                } catch (Exception e) {
                    descargaError("Error: " + e.getMessage());
                }
            }
        });
    }
    private void cambiaImagenFrase(String frase, final String miRuta) {
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
                    descargaError("No se puede descargar de: " + miRuta);
                }
            });
    }
    private void descargaError(final String errorMsg) {
        //Toast.makeText(MainActivity.this, errorMsg, Toast.LENGTH_SHORT).show();
        final StringBuilder content = new StringBuilder();
        final Resultado[] resultado = { new Resultado() };

        //Descarga el contenido del archivo de errores remoto
        RestClient.get(WEB_ERROR, new FileAsyncHttpResponseHandler(this) {
            @Override
            public void onStart() {
                //Toast.makeText(MainActivity.this, "Conexión correcta con el servidor", Toast.LENGTH_SHORT).show();
            }
            @Override
            public void onFailure(int statusCode, Header[] headers, Throwable throwable, File file) {
                Toast.makeText(MainActivity.this, "Fallo en la conexión", Toast.LENGTH_SHORT).show();
            }
            @Override
            public void onSuccess(int statusCode, Header[] headers, File response) {
                try {
                    BufferedReader reader = new BufferedReader(new FileReader(file));
                    String line;
                    while((line = reader.readLine()) != null)
                        content.append(line).append("\n");
                    reader.close();
                    //Añade el nuevo error al archivo de errores remoto
                    content.append("[").append(new Date()).append("]: ").append(errorMsg).append("\n");
                    if (memoria.disponibleEscritura()) {
                        memoria.escribirExterna(FICHERO_ERROR, content.toString(), true, Memoria.UTF8);
                        //Toast.makeText(MainActivity.this, "Escritura con éxito en: " + FICHERO_ERROR, Toast.LENGTH_SHORT).show();
                        if (memoria.disponibleLectura())
                            resultado[0] = memoria.leerExterna(FICHERO_ERROR, Memoria.UTF8);
                        else
                            Toast.makeText(MainActivity.this, "No se leyó el archivo", Toast.LENGTH_SHORT).show();
                    }
                    subidaError(resultado);
                } catch (IOException e) {
                    Toast.makeText(MainActivity.this, "Fallo de lectura del archivo", Toast.LENGTH_SHORT).show();
                } catch (Exception e) {
                    Toast.makeText(MainActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void subidaError(Resultado[] resultado) {
        if(resultado[0].getCodigo()) {
            RequestParams params = new RequestParams();
            try {
                params.put("fileToUpload", new File(Environment.getExternalStorageDirectory(), FICHERO_ERROR));
                params.put("password", PASSWORD);
                RestClient.post(WEB, params, new TextHttpResponseHandler() {
                    @Override
                    public void onStart() {
                        Toast.makeText(MainActivity.this, "Actualizando archivo de errores...", Toast.LENGTH_SHORT).show();
                    }
                    @Override
                    public void onSuccess(int statusCode, Header[] headers, String response) {
                        Toast.makeText(MainActivity.this, "Archivo errores actualizado. Código: " + statusCode + " " + response, Toast.LENGTH_SHORT).show();
                    }
                    @Override
                    public void onFailure(int statusCode, Header[] headers, String response, Throwable t) {
                        Toast.makeText(MainActivity.this, "Fallo actualizando archivo de errores. Código: " + statusCode + " " + t.getMessage() + " " + response, Toast.LENGTH_SHORT).show();
                    }
                });
            } catch (FileNotFoundException e) {
                Toast.makeText(MainActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        } else
            Toast.makeText(MainActivity.this, "Error leyendo archivo de errores local", Toast.LENGTH_SHORT).show();
    }

}
