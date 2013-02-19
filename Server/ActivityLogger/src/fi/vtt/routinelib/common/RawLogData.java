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

package fi.vtt.routinelib.common;

import java.util.ArrayList;

public class RawLogData extends Data {
	
    private long                   timestamp              = -1;
    // because we use GSON for serialization, following time stamp presentation
    // should be in server readable format
    private String                 timeStringPresentation = "";
    private int                    cellId                 = -1;
    private ArrayList<Application> apps                   = new ArrayList<Application>();
    private GPSData                gpsData                = null;

    /**
     * Constructor
     */
    public RawLogData() {
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getTimeStringPresentation() {
        return timeStringPresentation;
    }

    public void setTimeStringPresentation(String timeStringPresentation) {
        this.timeStringPresentation = timeStringPresentation;
    }

    public int getCellId() {
        return cellId;
    }

    public void setCellId(int cellId) {
        this.cellId = cellId;
    }

    public ArrayList<Application> getApps() {
        return apps;
    }

    public void setApps(ArrayList<Application> apps) {
        this.apps = apps;
    }

    public GPSData getGpsData() {
        return gpsData;
    }

    public void setGpsData(GPSData gpsData) {
        this.gpsData = gpsData;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder(128);
        builder.append("time: ");
        builder.append(getTimestamp());
        builder.append(" time string for server: ");
        builder.append(getTimeStringPresentation());
        builder.append(" cellid: ");
        builder.append(getCellId());
        builder.append(" apps:");
        builder.append("\r\n");
        for (Application application : apps) {
            builder.append(application.toString());
            builder.append("\r\n");
        }
        if (gpsData != null) {
            builder.append(gpsData.toString());
            builder.append("\r\n");
        }
        return builder.toString();
    }

}
