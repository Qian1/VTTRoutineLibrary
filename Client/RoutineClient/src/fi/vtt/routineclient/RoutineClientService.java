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

/*
 * RoutineClient. 
 * 
 * A simple Android application demonstrating the usage of the VTT Routine Library (RoutineLib) for Android. 
 * 
 * Version: 1.0 
 * 
 */

package fi.vtt.routineclient;

import android.app.Notification;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.util.Log;
import com.google.gson.Gson;
import fi.vtt.routinelibrary.RoutineLibraryCallback;
import fi.vtt.routinelibrary.VTTRoutineLibrary;
import fi.vtt.routinelibrary.common.RawLogData;
import fi.vtt.routinelibrary.common.RoutineData;
import fi.vtt.routinelibrary.internal.HTTPUploader;
import java.io.IOException;

/**
 * Application main (Service) class. 
 * <p> 
 * Creates the RoutineLibrary object, implements and sets call backs and starts the routine logging and recognition. 
 * <p> 
 * When a shutdown Intent from the application Activity class is received, the RoutineLibrary is stopped and this Service is destroyed. 
 * 
 * @see  android.app.Service 
 * @see  fi.vtt.routinelibrary.RoutineLibraryCallback 
 * 
 */

public class RoutineClientService extends Service implements RoutineLibraryCallback {

	/**
	 * Basic HTTP uploader for uploading the data to a server. 
	 * 
	 * @see  fi.vtt.routinelibrary.internal.HTTPUploader 
	 * 
	 */

	private HTTPUploader newRoutineHttpUploader = null;

	/**
	 * Basic HTTP uploader for uploading the data to a server. 
	 * 
	 * @see  fi.vtt.routinelibrary.internal.HTTPUploader 
	 * 
	 */

	private HTTPUploader rawDataHttpUploader = null;

	/**
	 * Basic HTTP uploader for uploading the data to a server. 
	 * 
	 * @see  fi.vtt.routinelibrary.internal.HTTPUploader 
	 * 
	 */

    private HTTPUploader recognizedRoutineHttpUploader = null;

    /**
	 * Basic BroadcastReceiver for receiving Intents. 
	 * 
	 * @see  android.content.BroadcastReceiver 
	 * 
	 */

    private RoutineBroadcastReceiver routineBroadcastReceiver = null;

    /**
	 * VTTRoutineLibrary. 
	 * 
	 * @see  fi.vtt.routinelibrary.VTTRoutineLibrary 
	 * 
	 */

	private VTTRoutineLibrary routineLibrary = null;

	/**
	 * Basic way of receiving broadcasted Intents. 
	 * <p> 
	 * Grabs the shutdown Intent from the main Activity, stops recognition and shuts down. 
	 * <p> 
	 * Simple demonstration, you can implement stopping etc. any way you like, just remember to call stopRecognition(). 
	 * 
	 * @see  android.content.BroadcastReceiver 
	 * 
	 */

	public class RoutineBroadcastReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context contextIncoming, Intent intentIncoming) {
			Log.d(getString(R.string.RoutineClientServiceString), getString(R.string.OnReceiveString));

			if (intentIncoming.getAction().equals(getString(R.string.StopRoutineClientServiceString))) {
				Log.d(getString(R.string.RoutineClientServiceString), getString(R.string.StoppingRecognitionString));

				routineLibrary.stopRecognition();

				RoutineClientService.this.stopSelf();
			}
		}

	}

	/**
	 * Set to true if you want to upload data to a server. 
	 * 
	 */

    public final static boolean UPLOAD_ENABLED = true;

    /**
     * If you are using the provided server-side components, your server upload URL is base URL + additional path. 
     * <p> 
     * For example: 
     * <p> 
     * <ul> 
     * <li>https://[ip address or URL]:[port number]/ActivityLogger/rest/rawdata/</li> 
     * <li>https://[ip address or URL]:[port number]/ActivityLogger/rest/user_routines/</li> 
     * <li>https://[ip address or URL]:[port number]/ActivityLogger/rest/routinerecognized/</li> 
     * </ul> 
     * 
     */

    public final static String BASE_UPLOAD_URL = "https://<your_server>:<your_port>/ActivityLogger/rest/";

    /**
     * If you are using the provided server-side components, your server upload URL is base URL + additional path. 
     * <p> 
     * For example: 
     * <p> 
     * <ul> 
     * <li>https://[ip address or URL]:[port number]/ActivityLogger/rest/rawdata/</li> 
     * <li>https://[ip address or URL]:[port number]/ActivityLogger/rest/user_routines/</li> 
     * <li>https://[ip address or URL]:[port number]/ActivityLogger/rest/routinerecognized/</li> 
     * </ul> 
     * 
     */

    public final static String RAW_PATH = "rawdata/";

    /**
     * If you are using the provided server-side components, your server upload URL is base URL + additional path. 
     * <p> 
     * For example: 
     * <p> 
     * <ul> 
     * <li>https://[ip address or URL]:[port number]/ActivityLogger/rest/rawdata/</li> 
     * <li>https://[ip address or URL]:[port number]/ActivityLogger/rest/user_routines/</li> 
     * <li>https://[ip address or URL]:[port number]/ActivityLogger/rest/routinerecognized/</li> 
     * </ul> 
     * 
     */

    public final static String NEW_ROUTINE_PATH = "user_routines/";

    /**
     * If you are using the provided server-side components, your server upload URL is base URL + additional path. 
     * <p> 
     * For example: 
     * <p> 
     * <ul> 
     * <li>https://[ip address or URL]:[port number]/ActivityLogger/rest/rawdata/</li> 
     * <li>https://[ip address or URL]:[port number]/ActivityLogger/rest/user_routines/</li> 
     * <li>https://[ip address or URL]:[port number]/ActivityLogger/rest/routinerecognized/</li> 
     * </ul> 
     * 
     */

    public final static String RECOGNIZED_ROUTINE_PATH = "routinerecognized/";

	/**
	 * Basic onBind() -method. 
	 * <p> 
	 * Needed if you want to bind this Service to some Activity or Service. 
	 * 
	 * @param  intentIncoming  The incoming Intent. 
	 * 
	 * @see  android.content.Intent 
	 * 
	 */

    @Override
    public IBinder onBind(Intent intentIncoming) {
        return null;
    }

    /**
	 * From RoutineLibraryCallback. 
	 * <p> 
	 * This method is called when new raw data is available. 
	 * <p> 
	 * Implement this however you wish, here the raw data is simply just uploaded to the server, if the upload is enabled. 
	 * 
	 * @param  timeStampLongIncoming  The incoming time stamp as a long value. 
	 * @param  rawLogDataIncoming  The incoming raw data. 
	 * @param  userHashStringIncoming  The incoming user ID hash string. 
	 * 
	 * @see  fi.vtt.routinelibrary.common.RawLogData 
	 * @see  fi.vtt.routinelibrary.RoutineLibraryCallback 
	 * 
	 */

    @Override
    public void newRawLoggedData(RawLogData rawLogDataIncoming, long timeStampLongIncoming, String userHashStringIncoming) {
        Log.d("newRawLoggedData", rawLogDataIncoming.toString());

        if (UPLOAD_ENABLED) {
            Log.d("newRawLoggedData", "Upload enabled");

            String rawUploadPath = RAW_PATH + userHashStringIncoming;

            uploadData(rawDataHttpUploader, rawUploadPath, rawLogDataIncoming);
        } 
        else {
            Log.d("newRawLoggedData", "Upload disabled");
        }
    }

    /**
	 * From RoutineLibraryCallback. 
	 * <p> 
	 * This method is called when new routine data is available. 
	 * <p> 
	 * Implement this however you wish, here the data is simply just uploaded to the server, if the upload is enabled. 
	 * 
	 * @param  timeStampLongIncoming  The incoming time stamp as a long value. 
	 * @param  routineDataIncoming  The incoming routine data. 
	 * @param  userHashStringIncoming  The incoming user ID hash string. 
	 * 
	 * @see  fi.vtt.routinelibrary.common.RoutineData 
	 * @see  fi.vtt.routinelibrary.RoutineLibraryCallback 
	 * 
	 */

    @Override
    public void newRoutineLearned(RoutineData routineDataIncoming, long timeStampLongIncoming, String userHashStringIncoming) {
        Log.d("newRoutineLearned", routineDataIncoming.toString());

        if (UPLOAD_ENABLED) {
            Log.d("newRoutineLearned", "Upload enabled");

            String learnedPath = NEW_ROUTINE_PATH;

            uploadData(newRoutineHttpUploader, learnedPath, routineDataIncoming);
        } 
        else {
            Log.d("newRoutineLearned", "Upload disabled");
        }
    }

    /**
	 * Basic onCreate() -method. 
	 * <p> 
	 * Creates: 
	 * <p> 
	 * <ol start="1"> 
	 * <li>The RoutineLibrary object,</li> 
	 * <li>If upload is enabled, creates the HTTP uploader objects,</li> 
	 * <li>Registers this Service to listen to the shutdown Intent,</li> 
	 * <li>And finally, starts the routine logging and recognition.</li> 
	 * </ol> 
	 * 
	 */

    @Override
	public void onCreate() {
        super.onCreate();

        Log.d("RoutineClientService", "onCreate");

        Notification notification = new Notification();

        startForeground(0, notification);

        routineLibrary = new VTTRoutineLibrary(this);
        routineLibrary.setRoutineCallback(this);
        routineLibrary.initializeWakeLock(getApplicationContext());

        if (UPLOAD_ENABLED) {
            rawDataHttpUploader = new HTTPUploader(this, BASE_UPLOAD_URL);
            newRoutineHttpUploader = new HTTPUploader(this, BASE_UPLOAD_URL);
            recognizedRoutineHttpUploader = new HTTPUploader(this, BASE_UPLOAD_URL);
        }

		IntentFilter intentFilter = new IntentFilter(getString(R.string.StopRoutineClientServiceString));

		routineBroadcastReceiver = new RoutineBroadcastReceiver();

		registerReceiver(routineBroadcastReceiver, intentFilter);

        routineLibrary.startRecognition();
    }

    /**
	 * Basic onDestroy() -method. 
	 * <p> 
	 * Unregisters this Service from listening to the shutdown Intent. 
	 * 
	 */

    @Override
    public void onDestroy() {
        super.onDestroy();

        Log.d("RoutineClientService", "onDestroy");

        if (routineBroadcastReceiver != null) {
        	unregisterReceiver(routineBroadcastReceiver);
        }
    }

    /**
	 * From RoutineLibraryCallback. 
	 * <p> 
	 * This method is called when a routine is recognized. 
	 * <p> 
	 * Implement this however you wish, here the data is simply just uploaded to the server, if the upload is enabled. 
	 * 
	 * @param  timeStampLongIncoming  The incoming time stamp as a long value. 
	 * @param  routineDataIncoming  The incoming routine data. 
	 * @param  userHashStringIncoming  The incoming user ID hash string. 
	 * 
	 * @see  fi.vtt.routinelibrary.common.RoutineData 
	 * @see  fi.vtt.routinelibrary.RoutineLibraryCallback 
	 * 
	 */

    @Override
    public void routineRecognized(RoutineData routineDataIncoming, long timeStampLongIncoming, String userHashStringIncoming) {
        Log.d("routineRecognized", routineDataIncoming.toString());

        if (UPLOAD_ENABLED) {
            Log.d("routineRecognized", "Upload enabled");

            String recognizedPath = RECOGNIZED_ROUTINE_PATH + userHashStringIncoming;

            uploadData(recognizedRoutineHttpUploader, recognizedPath, routineDataIncoming);
        }
        else {
            Log.d("routineRecognized", "Upload DISABLED");
        }
    }

    /**
	 * Upload method, makes a JSON object of the data and uploads it using the given HTTPUploader. 
	 * 
	 * @param  httpUploaderIncoming  The HTTPUploader object to be user for data uploading. 
	 * @param  uploadDataObjectIncoming  The data to be uploaded in an object. 
	 * @param  additionalPathStringIncoming  Additional URL path. 
	 * 
	 * @see  fi.vtt.routinelibrary.internal.HTTPUploader 
	 * 
	 */

    public void uploadData(HTTPUploader httpUploaderIncoming, String additionalPathStringIncoming, Object uploadDataObjectIncoming) {
        Gson gson = new Gson();

        String json = gson.toJson(uploadDataObjectIncoming);

        Log.d("upload json", json);

        try {
        	httpUploaderIncoming.sendString(json, additionalPathStringIncoming);
        }
        catch (IOException e) {
            e.printStackTrace();
            Log.w("HttpUpload", e.getMessage());
        }
    }

}