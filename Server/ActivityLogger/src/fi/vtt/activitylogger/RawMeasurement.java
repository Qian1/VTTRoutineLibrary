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
public class RawMeasurement {

	private long   measurementId;
    private String measurementTimestamp; // Store in PostgreSQL format "'2012-08-16 16:28:56.583+03'" or ISO-8061 format "2012-08-16T13:28:56.583+00"?
    private double latitude;
    private double longitude;
    private int cellId;
    // Foreign data fields of the table (id->name):
    private String loggerApplicationName;
    private String deviceName;

    /** Default constructor needed for annotations to avoid exception at run time */
    // "IllegalAnnotationException: fi.vtt.activitylogger.Device does not have a no-arg default constructor."
    public RawMeasurement() {}
	
    /** Constructor
     * @param measurementId
     * @param measurementTimestamp
     * @param latitude
     * @param longitude
     * @param cellId
     * @param loggerApplicationName
     * @param deviceName
     */
      // Generate automatic comments in Eclipse with: Source, Generate element comment [ALT-SHIFT-J].  
    public RawMeasurement( long measurementId, String measurementTimestamp, double latitude, double longitude, int cellId,
    					   String loggerApplicationName , String deviceName ) {
        this.measurementId = measurementId;
        this.measurementTimestamp = measurementTimestamp;
        this.latitude = latitude;
        this.longitude = longitude;
        this.cellId = cellId;
        this.loggerApplicationName = loggerApplicationName;
        this.deviceName = deviceName;
    }

    public long getMeasurementId() {
        return measurementId;
    }

    public void setMeasurementId( long measurementId ) {
        this.measurementId = measurementId;
    }


    public String getMeasurementTimestamp() {
        return measurementTimestamp;
    }

    public void setMeasurementTimestamp( String measurementTimestamp ) {
        this.measurementTimestamp = measurementTimestamp;
    }
    
    public double getLatitude() {
        return latitude;
    }

    public void setLatitude( double latitude ) {
        this.latitude = latitude;
    }

    
    public double getLongitude() {
        return longitude;
    }

    public void setLongitude( double longitude ) {
        this.longitude = longitude;
    }
    
    public int getCellId() {
        return cellId;
    }

    public void setCellId( int cellId ) {
        this.cellId = cellId;
    }

    public String getloggerApplicationName() {
        return loggerApplicationName;
    }

    public void setloggerApplicationName( String loggerApplicationName ) {
        this.loggerApplicationName = loggerApplicationName;
    }
    
    public String getDeviceName() {
        return deviceName;
    }

    public void setDeviceName( String deviceName ) {
        this.deviceName = deviceName;
    }
    
}
