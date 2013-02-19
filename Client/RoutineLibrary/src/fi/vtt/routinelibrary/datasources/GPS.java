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

package fi.vtt.routinelibrary.datasources;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import java.util.Timer;
import java.util.TimerTask;

/**
 * GPS listener class, extends Service and implements LocationListener. 
 * 
 * @see  android.app.Service 
 * @see  android.location.LocationListener 
 * 
 */

public class GPS extends Service implements LocationListener {

	private GPSCallback     gpsCallback;

	private IBinder         iBinder;

    private LocationManager locationManager;

    private Timer           timer;

    private TimerTask       timerTask;

    /**
     * Basic Android Service binding. 
     *
     */

    public class iBinder extends Binder {
        public GPS getService() {
            return GPS.this;
        }
    }

    /**
     * Basic Android Service binding. 
     *
     */

    @Override
    public IBinder onBind(Intent intentIncoming) {
        return iBinder;
    }

    /**
	 * Basic onCreate() -method. 
	 * <p> 
	 * Creates the LocationManager and Timer (+ TimerTask). 
	 * 
	 */

    @Override
    public void onCreate() {
        super.onCreate();

        locationManager = (LocationManager)getSystemService(Context.LOCATION_SERVICE);

        timer = new Timer();

        timerTask = new TimerTask() {

            @Override
            public void run() {
                timeOut();
            }

        };
    }

    /**
	 * Basic onDestroy() -method. 
	 * 
	 */

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    /**
	 * When a GPS fix is available. 
	 * <p>
	 * A class implementing GPSCallBack will then be called with the coordinates. 
	 *  
	 * @see  fi.vtt.routinelibrary.GPSCallback 
	 * 
	 */

    @Override
    public void onLocationChanged(Location locationIncoming) {
        Log.d("GPS", "onLocationChanged()");

        timerTask.cancel();

        locationManager.removeUpdates(this);

        double latitudeDouble = locationIncoming.getLatitude();
        double longitudeDouble = locationIncoming.getLongitude();

        Log.d("GPS", "lat: " + latitudeDouble + " lon: " + longitudeDouble);

        gpsCallback.newLocation(latitudeDouble, longitudeDouble);
    }

    /** 
     * Ignore. 
     * 
     */

    @Override
    public void onProviderDisabled(String providerStringIncoming) {}

    /** 
     * Ignore. 
     * 
     */

    @Override
    public void onProviderEnabled(String providerStringIncoming) {}

    /** 
     * Ignore. 
     * 
     */

    @Override
    public void onStatusChanged(String providerStringIncoming, int statusIntegerIncoming, Bundle bundleIncoming) {}

    /** 
     * Request GPS coordinates. 
     * <p>
     * Starts the LocationManager and Timer. 
     * 
     */

    public void requestLocation() {
        Log.d("GPS", "requestLocation()");

        final long MIN_TIME = 60 * 1000; // ms

        final float MIN_DISTANCE = 0.0f; // meters

        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, MIN_TIME, MIN_DISTANCE, this);

        final long DELAY = 55 * 1000; // ms
        final long period = 60 * 1000; // ms

        timer.schedule(timerTask, DELAY, period);
    }

    /**
     * 
     * Set the GPSCallback. 
     * 
     * @param  gpsCallbackIncoming  The class implementing GPSCallback. 
     *  
     */

    public void setCallback(GPSCallback gpsCallbackIncoming) {
        gpsCallback = gpsCallbackIncoming;
    }

    /**
     * Called when the timer times out. 
     * 
     */

    public void timeOut() {
        Log.d("GPS", "timeOut()");

        locationManager.removeUpdates(this);

        timerTask.cancel();

        gpsCallback.failedToGetLocation("GPS timer timeout");
    }

}