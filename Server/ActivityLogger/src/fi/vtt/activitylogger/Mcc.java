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
public class Mcc {
	
	// Example row: 302;"CA ";"Canada"
	private int   mcc;		  	// Mobile country code (three digits each but one duplicate was made distinct by multiplying it by 10 -> four digits).
    private String abbr; 		// Two character country code. 
    private String name;   		// Country name.

    /** Default constructor needed for annotations to avoid exception at run time */
    // "IllegalAnnotationException: fi.vtt.activitylogger.Mcc does not have a no-arg default constructor."
    public Mcc() {}
	
    /** Constructor
     */
      // Generate automatic comments in Eclipse with: Source, Generate element comment [ALT-SHIFT-J].  
    public Mcc( int mcc, String abbr, String name ) {
        this.mcc = mcc;
        this.abbr = abbr;
        this.name = name;
    }

    public int getMcc() {
        return mcc;
    }

    public void setMcc( int mcc ) {
        this.mcc = mcc;
    }

    public String getAbbr() {
        return abbr;
    }

    public void setAbbr( String abbr ) {
        this.abbr = abbr;
    }
    
    public String getName() {
        return name;
    }

    public void setName( String name ) {
        this.name = name;
    }
    
}
