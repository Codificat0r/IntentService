package com.mercacortex.ad_trabajo_t2.handler;

import com.loopj.android.http.FileAsyncHttpResponseHandler;

import java.io.File;

import cz.msebera.android.httpclient.Header;

/**
 * Created by usuario on 15/02/18.
 */

public class MyFileAsyncHttpResponseHandler extends FileAsyncHttpResponseHandler {

    public MyFileAsyncHttpResponseHandler(File file) {
        super(file);
    }

    @Override
    public void onFailure(int statusCode, Header[] headers, Throwable throwable, File file) {

    }

    @Override
    public void onSuccess(int statusCode, Header[] headers, File file) {

    }
}