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

//import javax.xml.bind.annotation.XmlAccessorOrder;
//import javax.xml.bind.annotation.XmlAccessOrder;
import javax.xml.bind.annotation.XmlRootElement;
// import javax.xml.bind.annotation.XmlElement; // Disallowed for the whole class.
// import com.sun.xml.internal.txw2.annotation.XmlElement; // Do not confuse with this annotation!

// JAX-RS supports an automatic mapping from JAXB annotated class to XML and JSON.
// It is needed for automatic transforming of data object into a proper server response when using Jersey REST.
@XmlRootElement(name="application") // Seems to be ignored in an ArrayList.
// @XmlAccessorOrder(XmlAccessOrder.ALPHABETICAL) // The ordering of fields and properties in a class is in alphabetical order as determined by the method
                                                  // java.lang.String.compareTo(String anotherString). Default is UNDEFINED. NOT NEEDED TO BE SET.
public class Application {

    /** Default constructor needed for annotations to avoid exception at run time */
    // "IllegalAnnotationException: fi.vtt.activitylogger.Device does not have a no-arg default constructor."
    public Application() {}
	
	private long   applicationId;
    private String applicationName;
    private String packageClassName;

    /**
     * Constructor
     * 
     * @param applicationId
     * @param applicationName
     * @param packageClassName
     */
    public Application(long applicationId, String applicationName, String packageClassName) {
        this.applicationId = applicationId;
        this.applicationName = applicationName;
        this.packageClassName = packageClassName;
    }

    public long getApplicationId() {
        return applicationId;
    }

    public void setApplicationId( long applicationId ) {
        this.applicationId = applicationId;
    }
    
    public String getApplicationName() {
        return applicationName;
    }

    public void setApplicationName( String applicationName ) {
        this.applicationName = applicationName;
    }
    
    public String getPackageClassName() {
        return packageClassName;
    }

    public void setPackageClassName( String packageClassName ) {
        this.packageClassName = packageClassName;
    }
}
