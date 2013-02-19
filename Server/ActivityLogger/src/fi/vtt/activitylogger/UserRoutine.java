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

import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlSeeAlso;

// import fi.vtt.routinelib.common.Application; // Wrong (old) version.
import fi.vtt.activitylogger.Application;

import java.util.ArrayList;

// This class is used for requesting usable user routine data from the database. Therefore it concatenates fields from several tables together.
// For uploading user routine entry back to server and to DB, use different class & JSON structure (see classes in package fi.vtt.routinelib.common).

// JAX-RS supports an automatic mapping from JAXB annotated class to XML and JSON.
// It is needed for automatic transforming of data object into a proper server response when using Jersey REST.

// NOTE: Testing the same URL in the Tomcat server from Eclipse sometimes shows old (cached) results even between code modifications!
// Try a different URL first then the old one will surely be updated.

@XmlAccessorType(XmlAccessType.FIELD) // Restrict published elements to only class fields (without this both fields and get/set-properties are taken
                                      // ad the default vale is PUBLIC_MEMBER).
@XmlRootElement	// Makes the class UserRoutine serializable for JAXB and therefore the REST API.
@XmlSeeAlso(Application.class) // This line is needed to create proper sub-elements for the applications list in the output XL&JSON. DOES NOT WORK!
                               // See http://stackoverflow.com/questions/1603404/using-jaxb-to-unmarshal-marshal-a-liststring
public class UserRoutine {

    private long                   userRoutinesId;
	// Store in PostgreSQL format "'2012-08-16 16:28:56.583+03'" or ISO-8061 format "2012-08-16T13:28:56.583+00"? FORMER, NO 'T'.
    private String                 startTime;
    private String                 endTime;       
    private long                   routineClassId;
    // private long applicationId; // Show these values in the ArrayList<Application> applications instead.
    private long                   rawMeasurementId;
    private double                 confidence;
    // Foreign data fields of the table (id->value):
    // @XmlElement // This annotation will create a container applications around the list application items? PRODUCES DUPLICATE PROPERTY ERROR.
                   // http://docs.oracle.com/javaee/5/api/javax/xml/bind/annotation/XmlList.html says quite opposite. Is it the default behavior? %
    @XmlElementWrapper(name="applications") // Extra element around applications.
                                            // See http://docs.oracle.com/javaee/5/api/javax/xml/bind/annotation/XmlElementWrapper.html.
    // @XmlElement(name="application")         // Seems to be ignored.
    private ArrayList<Application> application; // (id,name,package) x n. Need to have "application" without s here because it will become the XML
                                                // element name for REST output.
    private int    				   routineTypeId; // -1 = undefined (causes error in DB), 0=location, 1=application, 2 = user defined.
    private String                 routineClassName;
    private double                 latitude;
    private double                 longitude;
    private int                    cellId;
    // Second hand foreign data fields (id->id->value):
    private String                 deviceName;
    private String                 loggerApplicationName;
    
    /** Default constructor needed for annotations to avoid exception at run time */
    // "IllegalAnnotationException: fi.vtt.activitylogger.UserRoutine does not have a no-arg default constructor."
    public UserRoutine() {
    }
	
    /**
     * Constructor
     * 
     * @param userRoutinesId
     * @param startTime
     * @param endTime
     * @param routineClassId
     * @param confidence
     * @param applications
     * @param routineTypeId Added to support inserting new user routine classes implicitly.
     * @param routineClassName
     * @param latitude
     * @param longitude
     * @param cellId
     * @param deviceName
     * @param loggerApplicationName
     */
    public UserRoutine(long userRoutinesId, String startTime, String endTime, long routineClassId, long rawMeasurementId ,
            double confidence, ArrayList<Application> applications, int routineTypeId , String routineClassName,
            double latitude, double longitude, int cellId, String deviceName,
            String loggerApplicationName) {
        this.userRoutinesId = userRoutinesId;
        this.startTime = startTime;
        this.endTime = endTime;
        this.routineClassId = routineClassId;
        this.rawMeasurementId = rawMeasurementId; // Added because was forgotten!
        this.confidence = confidence;
        this.application = applications;
        this.routineTypeId = routineTypeId;			// Added to determine what kind of routine to add.
        this.routineClassName = routineClassName;
        this.latitude = latitude;
        this.longitude = longitude;
        this.cellId = cellId;
        this.deviceName = deviceName;
        this.loggerApplicationName = loggerApplicationName;
    }

    public long getUserRoutinesId() {
        return userRoutinesId;
    }

    public void setUserRoutinesId(long userRoutinesId) {
        this.userRoutinesId = userRoutinesId;
    }

    public String getStartTime() {
        return startTime;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    public String getEndTime() {
        return endTime;
    }

    public void setEndTime(String endTime) {
        this.endTime = endTime;
    }

    public long getRoutineClassId() {
        return routineClassId;
    }

    public void setRoutineClassId(long routineClassId) {
        this.routineClassId = routineClassId;
    }

    public long getRawMeasurementId() {
        return rawMeasurementId;
    }

    public void setRawMeasurementId(long rawMeasurementId) {
        this.rawMeasurementId = rawMeasurementId;
    }

    public double getConfidence() {
        return confidence;
    }

    public void setConfidence(double confidence) {
        this.confidence = confidence;
    }

    // Get&set foreign data fields of the table

    public ArrayList<Application> getApplication() {
        return application;
    }

    public void setApplication(ArrayList<Application> application) {
        this.application = application;
    }

    public int getRoutineTypeId() {
        return routineTypeId;
    }

    public void setRoutineTypeId(int routineTypeId) {
        this.routineTypeId = routineTypeId;
    }
    
    public String getRoutineClassName() {
        return routineClassName;
    }

    public void setRoutineClassName(String routineClassName) {
        this.routineClassName = routineClassName;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public int getCellId() {
        return cellId;
    }

    public void setCellId(int cellId) {
        this.cellId = cellId;
    }

    // Get&set second hand foreign data fields (id->id->value)

    public String getDeviceName() {
        return deviceName;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }

    public String getLoggerApplicationName() {
        return loggerApplicationName;
    }

    public void setLoggerApplicationName(String loggerApplicationName) {
        this.loggerApplicationName = loggerApplicationName;
    }

}
