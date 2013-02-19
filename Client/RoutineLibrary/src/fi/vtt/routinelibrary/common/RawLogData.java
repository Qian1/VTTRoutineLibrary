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

package fi.vtt.routinelibrary.common;

import java.util.ArrayList;

/**
 * A simple container class with getters and setters, extends Data. 
 * 
 * @see  fi.vtt.routinelibrary.common.Data 
 * 
 */

public class RawLogData extends Data {

	private ArrayList<Application> apps                   = new ArrayList<Application>();

	private GPSData                gpsData                = null;

	private int                    cellId                 = -1;

    private long                   timestamp              = -1;

    private String                 timeStringPresentation = "";

    public ArrayList<Application> getApps() {
        return apps;
    }

    public GPSData getGpsData() {
        return gpsData;
    }

    public int getCellId() {
        return cellId;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public RawLogData() {}

    public String getTimeStringPresentation() {
        return timeStringPresentation;
    }

    @Override
    public String toString() {
        StringBuilder stringBuilder = new StringBuilder(128);
        stringBuilder.append("Time: ");
        stringBuilder.append(getTimestamp());
        stringBuilder.append(", Time string for server: ");
        stringBuilder.append(getTimeStringPresentation());
        stringBuilder.append(", CellID: ");
        stringBuilder.append(getCellId());
        stringBuilder.append(", Applications:");
        stringBuilder.append("\r\n");
        for (Application application : apps) {
            stringBuilder.append(application.toString());
            stringBuilder.append("\r\n");
        }
        if (gpsData != null) {
            stringBuilder.append(gpsData.toString());
            stringBuilder.append("\r\n");
        }
        return stringBuilder.toString();
    }

    public void setApps(ArrayList<Application> applicationsArrayListIncoming) {
        apps = applicationsArrayListIncoming;
    }

    public void setCellId(int cellIDIntegerIncoming) {
        cellId = cellIDIntegerIncoming;
    }

    public void setGpsData(GPSData gpsDataIncoming) {
        gpsData = gpsDataIncoming;
    }

    public void setTimestamp(long timeStampLongIncoming) {
        timestamp = timeStampLongIncoming;
    }

    public void setTimeStringPresentation(String timeStringPresentationStringIncoming) {
        timeStringPresentation = timeStringPresentationStringIncoming;
    }

}