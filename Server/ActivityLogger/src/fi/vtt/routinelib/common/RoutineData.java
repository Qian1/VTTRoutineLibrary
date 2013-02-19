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

public class RoutineData extends Data {
    public final static int        UNKNOWN_ROUTINE = -1;
    private int                    routineId       = UNKNOWN_ROUTINE;
    private boolean                routineChanged  = false;
    private ArrayList<Application> apps            = new ArrayList<Application>();

    /**
     * Constructor
     */
    public RoutineData() {
    }

    public int getRoutineId() {
        return routineId;
    }

    public void setRoutineId(int routineId) {
        this.routineId = routineId;
    }

    public boolean isRoutineChanged() {
        return routineChanged;
    }

    public void setRoutineChanged(boolean routineChanged) {
        this.routineChanged = routineChanged;
    }

    public ArrayList<Application> getApps() {
        return apps;
    }

    public void setApps(ArrayList<Application> apps) {
        this.apps = apps;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder(128);
        builder.append("routine: ");
        builder.append(getRoutineId());
        builder.append(" changed : ");
        builder.append(isRoutineChanged());
        builder.append(" apps:");
        builder.append("\r\n");
        for (Application application : apps) {
            builder.append(application.toString());
            builder.append("\r\n");
        }
        builder.append("\r\n");
        return builder.toString();
    }

}
