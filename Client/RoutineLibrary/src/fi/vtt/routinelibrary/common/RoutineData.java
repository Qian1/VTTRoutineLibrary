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

public class RoutineData extends Data {

	private ArrayList<Application> applications     = new ArrayList<Application>();

	private boolean                routineChanged  = false;

	public final static int        UNKNOWN_ROUTINE = -1;

	private int                    routineId       = UNKNOWN_ROUTINE;

	public ArrayList<Application> getApps() {
        return applications;
    }

	public boolean isRoutineChanged() {
        return routineChanged;
    }

	public int getRoutineId() {
        return routineId;
    }

    public RoutineData() {}

    @Override
    public String toString() {
        StringBuilder stringBuilder = new StringBuilder(128);
        stringBuilder.append("Routine: ");
        stringBuilder.append(getRoutineId());
        stringBuilder.append(", Changed : ");
        stringBuilder.append(isRoutineChanged());
        stringBuilder.append(", Applications:");
        stringBuilder.append("\r\n");
        for (Application application : applications) {
            stringBuilder.append(application.toString());
            stringBuilder.append("\r\n");
        }
        stringBuilder.append("\r\n");
        return stringBuilder.toString();
    }

    public void setApps(ArrayList<Application> applicationsArrayListIncoming) {
        applications = applicationsArrayListIncoming;
    }

    public void setRoutineChanged(boolean routineChangedBooleanIncoming) {
        routineChanged = routineChangedBooleanIncoming;
    }

    public void setRoutineId(int routineIDIntegerIncoming) {
        routineId = routineIDIntegerIncoming;
    }

}