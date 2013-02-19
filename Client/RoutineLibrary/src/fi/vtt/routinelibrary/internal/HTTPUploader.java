/*
 * Copyright (c) 2013, VTT Technical Research Centre of Finland 
 * All rights reserved. 
 * 
 * Redistribution and use in source and binary forms, with or without 
 * modification, are permitted provided that the following conditions are met: 
 * 
 * 1. Redistributions of source code must retain the above copyright 
 *    notice, this list of conditions and the following disclaimer. 
 * 2. Redistributions in binary form must reproduce the above copyright 
 *    notice, this list of conditions and the following disclaimer in the 
 *    documentation and/or other materials provided with the distribution. 
 * 3. Neither the name of the VTT Technical Research Centre of Finland nor the 
 *    names of its contributors may be used to endorse or promote products 
 *    derived from this software without specific prior written permission. 
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND 
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED 
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE 
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR 
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES 
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; 
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND 
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT 
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS 
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE. 
 * 
 */

package fi.vtt.routinelibrary.internal;

import android.content.Context;
import android.util.Base64;
import android.util.Log;
import fi.vtt.routinelibrary.R;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.params.ConnManagerParams;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.entity.StringEntity;
import org.apache.http.HttpResponse;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;

/**
 * HTTP uploader.
 *
 */

public class HTTPUploader implements Runnable {

	private Context context = null;

    private DefaultHttpClient defaultHttpClient = null;

    private String baseURLString = null;
    private String fullURLString = null; // baseURLString + additional path
    private String uploadDataString = null;

    private SSLSocketFactory mySSLSocketFactory() {
        try {
        	KeyStore keyStore = KeyStore.getInstance("BKS");

        	InputStream inputStream = context.getResources().openRawResource(R.raw.keystore);

        	try {
        		keyStore.load(inputStream, "myPassword".toCharArray());
        	}
        	finally {
        		inputStream.close();
        	}
        	return new SSLSocketFactory(keyStore);
        }
        catch (Exception exceptionIncoming) {
        	throw new AssertionError(exceptionIncoming);
        }
    }
 
    public HTTPUploader(Context contextIncoming, String baseURLStringIncoming) {
    	context = contextIncoming;

        baseURLString = baseURLStringIncoming;

        HttpParams httpParams = new BasicHttpParams();

        HttpConnectionParams.setConnectionTimeout(httpParams, 25000);
        HttpConnectionParams.setSoTimeout(httpParams, 30000);

        ConnManagerParams.setMaxTotalConnections(httpParams, 10);

        SchemeRegistry schemeRegistry = new SchemeRegistry();
        schemeRegistry.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));
        //schemeRegistry.register(new Scheme("https", SSLSocketFactory.getSocketFactory(), 443));
        schemeRegistry.register(new Scheme("https", mySSLSocketFactory(), 443));

        final ThreadSafeClientConnManager threadSafeClientConnManager = new ThreadSafeClientConnManager(httpParams, schemeRegistry);

        defaultHttpClient = new DefaultHttpClient(threadSafeClientConnManager, httpParams);
    }

    @Override
    public void run() {
    	Log.d("HTTP POST url", fullURLString);

        HttpPost httpPost = new HttpPost(fullURLString);

        try {
        	Log.d("HTTP POST data", uploadDataString);

            httpPost.setHeader(HTTP.USER_AGENT, "RoutineLibUploader");
            httpPost.setHeader("Authorization", "Basic " + Base64.encodeToString("user:r-h/r-t?".getBytes(), Base64.NO_WRAP));
            httpPost.setEntity(new StringEntity(uploadDataString));

            HttpResponse httpResponse = defaultHttpClient.execute(httpPost);
            //httpResponse.getEntity().consumeContent();

            String statusLineString = httpResponse.getStatusLine().toString();

            Log.d("HTTP POST RESPONSE statusLine", statusLineString);

            if (httpResponse.getEntity() != null) {
            	String responseString = EntityUtils.toString(httpResponse.getEntity(), HTTP.UTF_8);
            	Log.d("HTTP POST RESPONSE response", responseString);
            }
        }
        catch (IOException ioExceptionIncoming) {
        	ioExceptionIncoming.printStackTrace();
        }
    }

    public void sendString(String uploadDataStringIncoming, String additionalPathStringIncoming) throws IOException {
        uploadDataString = uploadDataStringIncoming;
        fullURLString = baseURLString + additionalPathStringIncoming;

        Thread thread = new Thread(this);
        thread.start();
    }

}