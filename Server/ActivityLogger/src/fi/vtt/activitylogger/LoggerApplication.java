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

package fi.vtt.activitylogger;

import javax.xml.bind.annotation.XmlRootElement;

// JAX-RS supports an automatic mapping from JAXB annotated class to XML and JSON.
// It is needed for automatic transforming of data object into a proper server response when using Jersey REST.
@XmlRootElement
public class LoggerApplication {

    /** Default constructor needed for annotations to avoid exception at run time */
    // "IllegalAnnotationException: fi.vtt.activitylogger.Device does not have a no-arg default constructor."
    public LoggerApplication() {}
	
	// We cannot have immutable (final) internal fields in order to be able to use this class also with Jersey REST API annotations:
	private long   loggerApplicationId;
    private String loggerApplicationName;

    /**
     * Constructor
     * 
     * @param loggerApplicationId
     * @param loggerApplicationName
     */
    public LoggerApplication(long loggerApplicationId, String loggerApplicationName) {
        this.loggerApplicationId = loggerApplicationId;
        this.loggerApplicationName = loggerApplicationName;
    }

    public long getLoggerApplicationId() {
        return loggerApplicationId;
    }

    public void setLoggerApplicationId( long loggerApplicationId ) {
        this.loggerApplicationId = loggerApplicationId;
    }
    
    public String getLoggerApplicationName() {
        return loggerApplicationName;
    }

    public void setLoggerApplicationName( String loggerApplicationName ) {
        this.loggerApplicationName = loggerApplicationName;
    }

}
