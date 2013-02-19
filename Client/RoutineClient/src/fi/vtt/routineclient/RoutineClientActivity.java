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
 * A simple Android application demonstrating the usage of the VTT Routine Library (RoutineLibrary) for Android. 
 * 
 * Version: 1.0 
 * 
 */

package fi.vtt.routineclient;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

/**
 * Application main (Activity) class. 
 * <p> 
 * Starts the RoutineClientService Android Service, no binding of the Service to this Activity is done however. 
 * <p> 
 * When this Activity is destroyed, an Intent is fired, which the RoutineClientService grabs and initiates shutdown. 
 * 
 * @see  android.app.Activity 
 * 
 */

public class RoutineClientActivity extends Activity {

	/**
	 * Basic onCreate() -method. 
	 * <p> 
	 * Starts the RoutineClientService Android Service, no binding to this Activity is done. 
	 * 
	 * @param  bundleIncoming  The incoming application state Bundle. 
	 * 
	 * @see  android.os.Bundle 
	 * 
	 */

    @Override
    protected void onCreate(Bundle bundleIncoming) {
        super.onCreate(bundleIncoming);

        Log.d(getString(R.string.RoutineClientActivityString), getString(R.string.OnCreateString));

        setContentView(R.layout.main);

        TextView statusTextView = (TextView)findViewById(R.id.StatusTextView);
        statusTextView.setText(getString(R.string.StatusString));

        Intent intent = new Intent(getApplicationContext(), RoutineClientService.class);

        startService(intent);
    }

    /**
	 * Basic onDestroy() -method. 
	 * <p> 
	 * When this Activity is destroyed, an Intent is fired, which the RoutineClientService grabs and initiates shutdown. 
	 * 
	 */

    @Override
    protected void onDestroy() {
        super.onDestroy();

        Log.d(getString(R.string.RoutineClientActivityString), getString(R.string.OnDestroyString));

        Intent intent = new Intent(getString(R.string.StopRoutineClientServiceString));

		sendBroadcast(intent);
    }

}