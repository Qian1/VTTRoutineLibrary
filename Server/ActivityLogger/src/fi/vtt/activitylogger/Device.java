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

// import java.sql.Timestamp;

import javax.xml.bind.annotation.XmlRootElement;

// JAX-RS supports an automatic mapping from JAXB annotated class to XML and JSON.
// It is needed for automatic transforming of data object into a proper server response when using Jersey REST.
@XmlRootElement
public class Device {

	// We cannot have immutable (final) internal fields in order to be able to use this class also with Jersey REST API annotations:
    private long      deviceId;
    private String    deviceName;
    private String    platform;
    // private Timestamp deviceTimestamp; // Does not support time zones and cannot be used in Jersey REST annotations (no zero argument constructor)!
    private String    deviceTimestamp;    // Store in PostgreSQL format "'2012-08-16 16:28:56.583+03'" or ISO-8061 format "2012-08-16T13:28:56.583+00"?
                                          // PostgreSQL format is probably quite portable to and from the server since Timestamp.toString() produces it.
    
                                          // Use the code "Timestamp timestamp = Timestamp.valueOf("2007-09-23 10:10:10.0");" to parse, if needed?
                                          //  Converts a String object in JDBC timestamp escape format to a Timestamp value.
                                          //  s - timestamp in format yyyy-[m]m-[d]d hh:mm:ss[.f...].
                                          //  The fractional seconds may be omitted. The leading zero for mm  and dd may also be omitted.
                                          // Or: DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ"); // ISO-8061 example.
                                          //     Date date = dateFormat.parse("2001-07-04T12:08:56.235-0700"); 
                                          //     long time = date.getTime();
                                          //     new Timestamp(time);
                                          // See SimpleDateFormat javadoc...
    private int       mcc;

    /** Default constructor needed for annotations to avoid exception at run time */
    // "IllegalAnnotationException: fi.vtt.activitylogger.Device does not have a no-arg default constructor."
    public Device() {
    	// Need to initialize to some initial value? No.
    }
    
    /**
     * Constructor
     * 
     * @param deviceId
     * @param deviceName
     * @param platform
     * @param deviceTimestamp
     * @param mcc
     */
    public Device(long deviceId, String deviceName, String platform, /*Timestamp*/ String deviceTimestamp,
            int mcc) {
        this.deviceId = deviceId;
        this.deviceName = deviceName;
        this.platform = platform;
        this.deviceTimestamp = deviceTimestamp;
        this.mcc = mcc;
    }

    public long getDeviceId() {
        return deviceId;
    }

    public void setDeviceId( long deviceId ) {
        this.deviceId = deviceId;
    }


    public String getDeviceName() {
        return deviceName;
    }

    public void setDeviceName( String deviceName ) {
        this.deviceName = deviceName;
    }

    public String getPlatform() {
        return platform;
    }

    public void setPlatform( String platform ) {
        this.platform = platform;
    }

    public /*Timestamp*/ String getDeviceTimestamp() {
        return deviceTimestamp;
    }

    public void setDeviceTimestamp( /*Timestamp*/ String deviceTimestamp ) {
        this.deviceTimestamp = deviceTimestamp;
    }

    public int getMcc() {
        return mcc;
    }

    public void setMcc( int mcc ) {
        this.mcc = mcc;
    }

    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer(128);
        sb.append("deviceId: ");
        sb.append(getDeviceId());
        sb.append(" deviceName: ");
        sb.append(getDeviceName());
        sb.append(" platform: ");
        sb.append(getPlatform());
        sb.append(" deviceTimestamp: ");
        sb.append(getDeviceTimestamp());
        sb.append(" mcc: ");
        sb.append(getMcc());
        return sb.toString();
    }

}
