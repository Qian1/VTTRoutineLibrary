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

package fi.vtt.routinelibrary;

import android.content.Context;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import fi.vtt.routinelibrary.internal.InternalRoutine;

/**
 * VTT Routine Library for Android.
 * <p>
 * Version: 1.0
 * <p>
 * Provides routine detection methods. A routine comprises of location and application combinations.
 * <p>
 * <ul>
 * <li>Applications</li>
 * <li>Locations (Cell ID, GPS)</li>
 * </ul>
 * <p>
 * <b>Usage:</b>
 * <p>
 * Include this RoutineLibrary Android project in your Android application project and initialize the library in your application's main class (usually an Android Service):
 * <pre>
 * <code>
 * private VTTRoutineLibrary routineLibrary = null;
 * </code>
 * </pre>
 * In your Service class implement the RoutineLibraryCallback...
 * <pre>
 * <code>
 * public class YourService extends Service implements RoutineLibraryCallback {
 * 
 *     private VTTRoutineLibrary routineLibrary = null;
 * 
 *     {@literal @}Override
 *     public void onCreate() {
 *        super.onCreate();
 *        Notification notification = new Notification();
 *        startForeground(0, notification);
 *        routineLibrary = new VTTRoutineLibrary(this);
 *        routineLibrary.setRoutineCallback(this);
 *        routineLibrary.initializeWakeLock(getApplicationContext());
 *        routineLibrary.startRecognition();
 *    }
 * 
 * }
 * </code>
 * </pre>
 * ...more specifically, implement the callback methods to receive data through the (RoutineLibraryCallback) interface:
 * <pre>
 * <code>
 * {@literal @}Override
 * public void newRawLoggedData(RawLogData rawLogDataIncoming, long timeStampLongIncoming, String userHashStringIncoming) {
 *     // Do something with the raw data.
 * }
 * 
 * {@literal @}Override
 * public void newRoutineLearned(RoutineData routineDataIncoming, long timeStampLongIncoming, String userHashStringIncoming) {
 *     // Do something with the routine data.
 * }
 * 
 * {@literal @}Override
 * public void routineRecognized(RoutineData routineDataIncoming, long timeStampLongIncoming, String userHashStringIncoming) {
 *     // Do something with the routine data.
 * }
 * </code>
 * </pre>
 * <p>
 * This way, the library will then function as follows:
 * <p>
 * <ul>
 * <li>NewRawLoggedData() will called by the library when new raw data is available,</li>
 * <li>NewRoutineLearned() will called by the library when new routine data is available and</li>
 * <li>RoutineRecognized() will called by the library when the current situation is noticed to match a routine.</li>
 * </ul>
 * 
 */

public class VTTRoutineLibrary {

	/**
	 * The binary routine library is included with this Java library the using the following clause:
	 * <pre>
	 * <code>
	 * static {
     *     System.loadLibrary("routinelibrary");
     * }
     * </code>
     * </pre>
	 * 
	 */

	static {
        System.loadLibrary("routinelibrary");
    }

	/**
	 * The internal routine library class.
	 * 
	 * @see  fi.vtt.routinelibrary.internal.InternalRoutine
	 * 
	 */

    private InternalRoutine internalRoutine = null;

    /**
	 * Basic Android Wakelock.
	 * 
	 * @see  android.os.PowerManager.WakeLock
	 * 
	 */

    private WakeLock wakeLock = null;

    /**
     * Initializes wakelock for keeping recognition alive when phone screen is turned off. 
     * <p>
     * Call this from your application's main class (usually an Android Service).
     * 
     * @param  contextIncoming  Context of your application.
     * 
     * @return  true  If wakelock initialized, otherwise false.
     * 
     * @see  android.content.Context
     * 
     */

    public boolean initializeWakeLock(Context contextIncoming) {
        try {
            PowerManager powerManager = (PowerManager)contextIncoming.getSystemService(Context.POWER_SERVICE);

            wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "RoutineLibrary partial wakelock");

            return true;
        }
        catch (Exception exceptionIncoming) {
            return false;
        }
    }

    /**
     * Sets the callback for the routine library.
     * 
     * @param  routineLibraryCallbackIncoming  Your application class that implements the RoutineLibraryCallback.
     * 
     * @see  fi.vtt.routinelibrary.RoutineLibraryCallback
     * 
     */

    public void setRoutineCallback(RoutineLibraryCallback routineLibraryCallbackIncoming) {
        internalRoutine.setCallback(routineLibraryCallbackIncoming);
    }

    /**
     * Starts recognition algorithms and acquires a Android Wakelock.
     * 
     */

    public void startRecognition() {
        if (wakeLock != null) {
            wakeLock.acquire();
        }

        internalRoutine.start();
    }

    /**
     * Stops recognition algorithms and releases the Android Wakelock.
     * 
     */

    public void stopRecognition() {
        if (wakeLock != null) {
            wakeLock.release();
        }

        internalRoutine.stop();
    }

    /**
	 * Constructor.
	 * <p>
	 * Initializes the internal routine library and provides the application context for it.
	 * 
	 * @param  contextIncoming  Context of your application.
	 * 
	 * @see  android.content.Context
	 * 
	 */

    public VTTRoutineLibrary(Context contextIncoming) {
        internalRoutine = new InternalRoutine(contextIncoming);
    }

}