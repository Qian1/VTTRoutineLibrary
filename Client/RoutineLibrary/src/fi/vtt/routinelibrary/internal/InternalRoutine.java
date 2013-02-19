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

import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.util.Log;
import fi.vtt.routinelibrary.RoutineLibraryCallback;
import fi.vtt.routinelibrary.common.Application;
import fi.vtt.routinelibrary.common.Configuration;
import fi.vtt.routinelibrary.common.RawLogData;
import fi.vtt.routinelibrary.common.RoutineData;
import fi.vtt.routinelibrary.utils.MyTimer;
import fi.vtt.routinelibrary.utils.MyTimerListener;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Formatter;

/**
 * InternalRoutine.
 *
 */

public class InternalRoutine implements MyTimerListener {

	// Delay when getting the recorded data from LoggerService:
    private final static int SNAPSHOT_TIME_DELAY = 60 * 1000;

	private LoggerService loggerService = null;

	private MyTimer myTimer  = null;

    private RoutineLibraryCallback routineLibraryCallback = null;

    private RoutineService routineService = null;

    private State state = null;

    public Configuration configuration = null;

    public final static String CONFIGURATION_FILE_NAME = "configuration.txt";
    public final static String STATE_FILE_NAME         = "old_state.txt";

    private boolean loadConfiguration() {
        Log.d("InternalRoutine", "loadConfiguration");

        boolean okBoolean = false;

        try {
            configuration = (Configuration)FileSystem.loadFromFile(CONFIGURATION_FILE_NAME,new Configuration());

            okBoolean = true;
        }
        catch (IOException ioExceptionIncoming) {
            Log.d("loadConfiguration", ioExceptionIncoming.getMessage());

            ioExceptionIncoming.printStackTrace();
        }

        return okBoolean;
    }

    private boolean loadSavedState() {
        Log.d("InternalRoutine", "loadSavedState");

        boolean okBoolean = false;

        try {
            State loadedState = (State)FileSystem.loadFromFile(STATE_FILE_NAME, new State());

            setState(loadedState);

            okBoolean = true;
        }
        catch (IOException ioExceptionIncoming) {
            Log.d("loadSavedState", ioExceptionIncoming.getMessage());

            ioExceptionIncoming.printStackTrace();
        }

        return okBoolean;
    }

    private boolean saveConfiguration() {
        Log.d("InternalRoutine", "saveConfiguration");

        boolean okBoolean = false;

        try {
            FileSystem.saveToFile(CONFIGURATION_FILE_NAME, configuration);

            okBoolean = true;
        }
        catch (IOException ioExceptionIncoming) {
            Log.d("saveConfiguration", ioExceptionIncoming.getMessage());

            ioExceptionIncoming.printStackTrace();
        }

        return okBoolean;
    }

    private boolean saveCurrentState() {
        Log.d("InternalRoutine", "saveCurrentState");

        boolean okBoolean = false;

        try {
            FileSystem.saveToFile(STATE_FILE_NAME, state);

            okBoolean = true;
        }
        catch (IOException ioExceptionIncoming) {
            Log.d("saveCurrentState", ioExceptionIncoming.getMessage());

            ioExceptionIncoming.printStackTrace();
        }

        return okBoolean;
    }

    /**
     * Create unique user ID based on device Bluetooth id & current time. If Bluetooth is not available, then only current time is used to create the hash.
     * 
     * @return  Hash string identifying this device & user. 
     * 
     * @throws  NoSuchAlgorithmException
     * 
     */

    private String createUserIdHash() throws NoSuchAlgorithmException {
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        String hashString;

        if (bluetoothAdapter != null) {
            hashString = bluetoothAdapter.getAddress();
        }
        else {
            Calendar calendar = Calendar.getInstance();

            hashString = calendar.getTime().toString();
        }

        String saltString = "dummysalt" + System.currentTimeMillis();
 
        hashString += saltString;

        MessageDigest messageDigest;
        messageDigest = MessageDigest.getInstance("SHA1");

        byte[] byteArray = messageDigest.digest(hashString.getBytes());

        Formatter formatter = new Formatter();

        for (int i = 0; i < byteArray.length; i++) {
            formatter.format("%02x", byteArray[i]);
        }

        return formatter.toString();
    }

    public InternalRoutine(Context contextIncoming) {
        boolean okBoolean = loadSavedState();

        if (!okBoolean) {
            Log.d("InternalRoutine", "Could not load previous state, initializing..");

            state = new State();
        }

        okBoolean = loadConfiguration();

        if (!okBoolean) {
            Log.d("InternalRoutine", "Could not load configuration, setting defaults");

            configuration = new Configuration();

            String userIDHashString;

            try {
                userIDHashString = createUserIdHash();

                configuration.setUserIdHash(userIDHashString);
            }
            catch (NoSuchAlgorithmException noSuchAlgorithmExceptionIncoming) {
            	noSuchAlgorithmExceptionIncoming.printStackTrace();

                Log.w("InternalRoutine", "Failed to create user id hash, setting dummy default!");

                configuration.setUserIdHash("DefaultUserId");
            }

            saveConfiguration();
        }

        loggerService = new LoggerService(contextIncoming);

        routineService = new RoutineService();

        myTimer = new MyTimer(this);
    }

    public State getState() {
        return state;
    }

    public void setCallback(RoutineLibraryCallback routineLibraryCallbackIncoming) {
        routineLibraryCallback = routineLibraryCallbackIncoming;
    }

    public void setState(State stateIncoming) {
        state = stateIncoming;
    }

    public void start() {
        Log.d("InternalRoutine", "start()");

        loggerService.startLogging();

        myTimer.start(SNAPSHOT_TIME_DELAY);
    }

    public void stop() {
        Log.d("InternalRoutine", "stop()");

        myTimer.cancel();

        loggerService.stopLogging();
    }

    // From MyTimerListener:
    @Override
    public void timeout() {
        Log.d("InternalRoutine", "Timer timeout()");

        RawLogData rawLogData = loggerService.getCollectedData();

        if (routineLibraryCallback == null) {
            Log.w("InternalRoutine", "No callback set for RoutineLib !");
        }
        else {
            Log.d("InternalRoutine", "Callbacks set");

            long timeStampLong = System.currentTimeMillis();

            routineLibraryCallback.newRawLoggedData(rawLogData, timeStampLong, configuration.getUserIdHash());

            try {
                RoutineData routineData = routineService.learn(state, rawLogData, routineLibraryCallback);

                boolean stateChangedBoolean = routineData.isRoutineChanged();

                if (stateChangedBoolean) {
                    // When we get a state change we find the current apps.
                    // In a case where we do not already have routineData with
                    // same identifier, we have just learned a new routine:
                    ArrayList<Application> currentApplicationsArrayList = rawLogData.getApps();

                    routineData.setApps(currentApplicationsArrayList);

                    boolean containsRoutineBoolean = state.containsRoutine(routineData);

                    if (!containsRoutineBoolean) {
                        routineLibraryCallback.newRoutineLearned(routineData, timeStampLong, configuration.getUserIdHash());
                    }
                    else {
                        routineLibraryCallback.routineRecognized(routineData, timeStampLong, configuration.getUserIdHash());
                    }
                }
                
                if (stateChangedBoolean) {
                    saveCurrentState(); // new routine or recognition
                }
            }
            catch (Exception exceptionIncoming) {
            	exceptionIncoming.printStackTrace();

                Log.w("InternalRoutine", "Internal learn error!: " + exceptionIncoming.getMessage());
            }
        }

        myTimer.start(SNAPSHOT_TIME_DELAY);
    }

}