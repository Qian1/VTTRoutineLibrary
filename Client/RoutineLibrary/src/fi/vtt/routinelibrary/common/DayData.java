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

/**
 * A simple container class with getters and setters, extends Data. 
 * 
 * @see  fi.vtt.routinelibrary.common.Data 
 * 
 */

public class DayData extends Data {

    public final static int TIME_SLOT_MINUTES   = 15;
    public final static int TIME_SLOTS_IN_A_DAY = 24 * 60 / TIME_SLOT_MINUTES;
    public final static int UNKNOWN_LOCATION    = -1;

    private int[] locationIdentifiersIntegerArray  = new int[TIME_SLOTS_IN_A_DAY];

    public DayData() {
        for (int i = 0; i < locationIdentifiersIntegerArray.length; i++) {
            locationIdentifiersIntegerArray[i] = UNKNOWN_LOCATION;
        }
    }

    public int[] getLocationIdentifiers() {
        return locationIdentifiersIntegerArray;
    }

    public int getSlot(int slotIdIntegerIncoming) throws IllegalArgumentException {
        if (slotIdIntegerIncoming < 0 || slotIdIntegerIncoming >= locationIdentifiersIntegerArray.length) {
            throw new IllegalArgumentException("slotId: " + slotIdIntegerIncoming + " out of range !");
        }

        return locationIdentifiersIntegerArray[slotIdIntegerIncoming];
    }

    public void setLocationIdentifiers(int[] locationIdentifiersIntegerArrayIncoming) {
        locationIdentifiersIntegerArray = locationIdentifiersIntegerArrayIncoming;
    }

    public void setSlot(int slotIdIntegerIncoming, int newValueIntegerIncoming) throws IllegalArgumentException {
        if (slotIdIntegerIncoming < 0 || slotIdIntegerIncoming >= locationIdentifiersIntegerArray.length) {
            throw new IllegalArgumentException("slotId: " + slotIdIntegerIncoming + " out of range !");
        }

        locationIdentifiersIntegerArray[slotIdIntegerIncoming] = newValueIntegerIncoming;
    }

}