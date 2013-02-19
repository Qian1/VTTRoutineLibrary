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

import fi.vtt.routinelibrary.common.RawLogData;
import fi.vtt.routinelibrary.common.RoutineData;

/**
 * Interface for callbacks, implement this in your application.
 * 
 */

public interface RoutineLibraryCallback {

	/**
	 * This method is called when new raw data is available.
	 * <p>
	 * Implement this in your application.
	 * 
	 * @param  timeStampLongIncoming  The incoming time stamp as a long value.
	 * @param  rawLogDataIncoming  The incoming raw data.
	 * @param  userHashStringIncoming  The incoming user ID hash string.
	 * 
	 * @see  fi.vtt.routinelibrary.common.RawLogData
	 * 
	 */

    public void newRawLoggedData(RawLogData rawLogDataIncoming, long timeStampLongIncoming, String userHashStringIncoming);

    /**
	 * This method is called when new routine data is available.
	 * <p>
	 * Implement this in your application.
	 * 
	 * @param  timeStampLongIncoming  The incoming time stamp as a long value.
	 * @param  routineDataIncoming  The incoming routine data.
	 * @param  userHashStringIncoming  The incoming user ID hash string.
	 * 
	 * @see  fi.vtt.routinelibrary.common.RoutineData
	 * 
	 */

    public void newRoutineLearned(RoutineData routineDataIncoming, long timeStampLongIncoming, String userHashStringIncoming);

    /**
	 * This method is called when a routine is recognized.
	 * <p>
	 * Implement this however you wish, here the data is simply just uploaded to the server, if the upload is enabled.
	 * 
	 * @param  timeStampLongIncoming  The incoming time stamp as a long value.
	 * @param  routineDataIncoming  The incoming routine data.
	 * @param  userHashStringIncoming  The incoming user ID hash string.
	 * 
	 * @see  fi.vtt.routinelibrary.common.RoutineData
	 * 
	 */

    public void routineRecognized(RoutineData routineDataIncoming, long timeStampLongIncoming, String userHashStringIncoming);

}