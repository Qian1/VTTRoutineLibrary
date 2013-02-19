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
import android.util.Log;
import fi.vtt.routinelibrary.common.Application;
import fi.vtt.routinelibrary.common.RawLogData;
import fi.vtt.routinelibrary.utils.TimeUtils;
import java.util.ArrayList;

/**
 * LoggerService. 
 *
 */

public class LoggerService {

    private final ApplicationsService applicationsService;

    private final CellIDService cellIdService;

    private Object lockObject = new Object();

    public LoggerService(Context contextIncoming) {
        applicationsService = new ApplicationsService(contextIncoming);

        cellIdService = new CellIDService(contextIncoming);
    }

    public void startLogging() {
        Log.d("LoggerService", "startLogging()");
    }

    public void stopLogging() {
        Log.d("LoggerService", "stopLogging()");
    }

    public RawLogData getCollectedData() {

        synchronized (lockObject) {
            RawLogData rawLogData = null;

            long timeLong = System.currentTimeMillis();

            rawLogData = new RawLogData();
            rawLogData.setTimestamp(timeLong);

            String timeStringPresentationString = TimeUtils.timestampToUploadFormat(timeLong);

            rawLogData.setTimeStringPresentation(timeStringPresentationString);

            int cellIDInteger = cellIdService.getCellId();

            rawLogData.setCellId(cellIDInteger);

            ArrayList<Application> currentApplicationsArrayList = (ArrayList<Application>)applicationsService.findCurrentApplications();

            rawLogData.setApps(currentApplicationsArrayList);

            return rawLogData;
        }

    }

}