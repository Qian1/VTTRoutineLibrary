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

package fi.vtt;

import java.sql.Connection;
import java.sql.SQLException;
// import java.sql.Timestamp;
// import java.util.Date;

// import javax.naming.Context; // Name clash with javax.ws.core.Context! Use full name.
import javax.naming.InitialContext;
import javax.servlet.http.HttpServletRequest;
import javax.sql.DataSource;
import javax.ws.rs.GET;
// import javax.ws.rs.POST;
// import javax.ws.rs.DefaultValue;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriInfo;
// import javax.xml.bind.JAXBElement;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// import com.google.gson.Gson;

import db.Queries;
import db.ConditionalAccess;	// Database user name constants.

import java.util.ArrayList;
import java.util.List;

import fi.vtt.activitylogger.Device;

// For checking authentication:
import com.sun.jersey.core.util.Base64;
// import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;

// Jersey REST references:
// http://www.vogella.com/articles/REST/article.html
// http://www.ibm.com/developerworks/web/library/wa-aj-tomcat/
// http://coenraets.org/blog/2011/11/building-restful-services-with-java-using-jax-rs-and-jersey-sample-application/

// This class maps to the following requests (the query parameters are optional for an administrator but device_name is required for a user): 
// http://localhost:8080/ActivityLogger/rest/devices
// https://<your server>:<port>/ActivityLogger/rest/devices
// Example of optional query parameters that can be added to the URL (some spaces added around & characters for readability):
// ? timestamp=2012-08-14T22:13:27+02 & device_name=a619ed32f7d0de86a002757fbf8b29f4d1ab0aae & logger_application_name=Samsung_Galaxy_S_log
//   & start_time=2012-06-18T22:54:15.000Z & end_time=2012-06-19
// The above URL needs to be encoded before use because of the '+' (%2B) character.

@Path("/devices")
public class DevicesResource {

	// Fields
	
	// Allows to insert contextual objects into the class, e.g. ServletContext, Request, Response, UriInfo.
	@Context UriInfo uriInfo;
	@Context Request request;
    @Context HttpHeaders headers; // For checking authentication.
    // @Context HttpServletRequest req; // For traditional servlet handling.

    // Query parameters in the URL:
    // @DefaultValue("") // Default value not needed for device_name. It will be null if not included in the request.
    @QueryParam("device_name")
    String device_name = null;				// Optional request parameters for GET. This one identifies the user.
    @QueryParam("logger_application_name")	//
    String logger_application_name = null;	// This limits results to specific logging application of the user (does not apply to devices!). 
    @QueryParam("timestamp")				//
    String timestamp = null;			    // This gives the device time and time zone for formatting timestamps for output (not implemented).
    // String period = null;   	// Search within times. Use ISO 8601 format, e.g.: "2012-06-18T22:54:15.000Z/2012-08-13T17:10:48+03" (not implemented).
    @QueryParam("start_time")	// Alternatively:
    String start_time = null;	// Use these two fields with ISO 8601 timestamps.
    @QueryParam("end_time")		//
    String end_time = null;		// 
    // String datetime = null;		// Alternative search period using a single, possibly incomplete, timestamp, e.g. "2012-10" (not implemented).
    
    private final static Logger logger = LoggerFactory.getLogger(DevicesResource.class);

    // Methods
    
    /** Process HTTP GET request: returns the list of devices (users) in the routine_db database
     * @param sc Jersey Security Context information for determining the user role (access rights)
     * @return A list of device objects that can be serialized into JSON, text, XML or visual (browser) XML. */
    @GET
    @Produces({MediaType.TEXT_XML, MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
      // Browser XML response, XML application response and JSON application response all in the same method automatically.
    public List<Device> getDevices( @Context SecurityContext sc ) {
    	
        logger.debug("URL /devices called.");
        javax.naming.Context context = null; // JDBC
        DataSource dataSource = null;
        Connection connection = null;
        List<Device> devices = new ArrayList<Device>(); // Return value to function and the corresponding REST service.
        
        // Check access rights and filters
        
        /* Variables for parsing query parameters from a HttpServletRequest. Not used here because we use Jersey annotations instead.
        String device_name = null;				// Optional request parameters for GET. This one identifies the user.
        String logger_application_name = null;	// This limits results to specific logging application of the user (does not apply to devices!). 
        String timestamp = null;			    // This gives the device time and time zone for formatting timestamps for output (not implemented).
        // String period = null;   	// Search within times. Use ISO 8601 format, e.g.: "2012-06-18T22:54:15.000Z/2012-08-13T17:10:48+03".
        String start_time = null;	// Alternatively:
        String end_time = null;		// use these two fields with ISO 8601 timestamps.
        // String datetime = null;		// Alternative search period using a single, possibly incomplete, timestamp, e.g. "2012-10".
        */
        
        // Get login credentials for HTTP basic authentication (over HTTPS):
        String header = headers.getRequestHeader("authorization").get(0);
        header = header.substring("Basic ".length());
        String[] creds = new String(Base64.base64Decode(header)).split(":");
        String username = creds[0];
        String password = creds[1];
        System.out.println( "[" + this.getClass() + "] Basic HTTP authentication ('username','password'): '" + username + "','" + password + "'." );
          // Debug print. DO NOT LEAVE HERE BECAUSE EXPOSES THE CREDENTIALS TO CONSOLE/LOGS.

        // Check authorization in the case this service is accidentally installed on a non-secure Tomcat server:
        if ( username==null || username.length()==0 )
        	throw new javax.ws.rs.WebApplicationException(javax.ws.rs.core.Response.Status.UNAUTHORIZED); // Proper HTTP 401 response when invalid user.
  		/* No need to have the method introduce RuntimeExceptions in its signature, see inheritance:
   		   java.lang.Object
 	    	 extended by java.lang.Throwable
	     	  extended by java.lang.Exception
      		   extended by java.lang.RuntimeException
       		    extended by javax.ws.rs.WebApplicationException */
        
        // Test Tomcat user roles as an alternative to the user name & password (so that we do not need to tie such information to the application):
        System.out.println( "[" + this.getClass() + "] User role from the security context:" );
        System.out.println( "\tsc: " + sc.toString() );
        System.out.println( "\tIs activity_logger-administrator: " + sc.isUserInRole( "activity_logger-administrator" ) );
        System.out.println( "\tIs activity_logger-user: " + sc.isUserInRole( "activity_logger-user" ) );
        System.out.println( "\tuser principal: " + sc.getUserPrincipal() );
        System.out.println( "\tauthentication scheme: " + sc.getAuthenticationScheme() );

        // Parse request parameters from a HttpServletRequest (not the Jersey way of doing things):
        /*
        device_name =  req.getParameter("device_name");
        logger_application_name = req.getParameter("logger_application_name");
        timestamp = req.getParameter("timestamp");
        start_time = req.getParameter("start_time");
        end_time = req.getParameter("end_time");
        */
        // if (value == null) {
            // The request parameter 'param' was not present in the query string
            // e.g. http://hostname.com?a=b
        // } else if ("".equals(value)) {
            // The request parameter 'param' was present in the query string but has no value
            // e.g. http://hostname.com?param=&a=b
        // }
		
        // Test print of request parameters in different ways:
        System.out.println( "[" + this.getClass() + "] Full request URL: " + uriInfo.getRequestUri().toString());
        System.out.println( "[" + this.getClass() + "] URL request parameters ('name','value'):");
        System.out.println( "'device_name','" + device_name + "'");
        System.out.println( "'logger_application_name','" + logger_application_name + "'");
        System.out.println( "'timestamp','" + timestamp + "'");
        System.out.println( "'start_time','" + start_time + "'");
        System.out.println( "'end_time','" + end_time + "'");
        /*
        System.out.println( "[" + this.getClass() + "] URL request parameters from uriInfo.getQueryParameters() (MultivaluedMap):");
        StringBuilder buf = new StringBuilder();   
        for (String param: uriInfo.getQueryParameters().keySet()) {  
             buf.append(param);  // Only the parameter name is printed.
             buf.append("\n");  
        }
        System.out.println( buf.toString() );
        */
        
        // Console printouts for different URLs
        
        /* https://localhost:8181/ActivityLogger/rest/devices 
		[class fi.vtt.DevicesResource] Basic HTTP authentication ('username','password'): 'user','...'.
		[class fi.vtt.DevicesResource] Full request URL: https://localhost:8181/ActivityLogger/rest/devices
		[class fi.vtt.DevicesResource] URL request parameters ('name','value'):
		'device_name','null'):
		'logger_application_name','null'):
		'timestamp','null'):
		'start_time','null'):
		'end_time','null'):
		[class fi.vtt.DevicesResource] URL request parameters from uriInfo.getQueryParameters() (MultivaluedMap):
         */
        
        /* https://localhost:8181/ActivityLogger/rest/devices?timestamp=2012-08-14T22:13:27+02&device_name=a619ed32f7d0de86a002757fbf8b29f4d1ab0aae
           &logger_application_name=Samsung_Galaxy_S_log&start_time=2012-06-18T22:54:15.000Z&end_time=2012-06-19
        Without URL encoding (+ in timestamp disappears into a space):
		[class fi.vtt.DevicesResource] Basic HTTP authentication ('username','password'): 'administrator','...'.
		[class fi.vtt.DevicesResource] Full request URL: https://localhost:8181/ActivityLogger/rest/devices?timestamp=2012-08-14T22:13:27+02
		                                                 &device_name=a619ed32f7d0de86a002757fbf8b29f4d1ab0aae&logger_application_name=Samsung_Galaxy_S_log
		                                                 &start_time=2012-06-18T22:54:15.000Z&end_time=2012-06-19
		[class fi.vtt.DevicesResource] URL request parameters ('name','value'):
		'device_name','a619ed32f7d0de86a002757fbf8b29f4d1ab0aae'):
		'logger_application_name','Samsung_Galaxy_S_log'):
		'timestamp','2012-08-14T22:13:27 02'):
		'start_time','2012-06-18T22:54:15.000Z'):
		'end_time','2012-06-19'):
		[class fi.vtt.DevicesResource] URL request parameters from uriInfo.getQueryParameters() (MultivaluedMap):
		timestamp
		device_name
		end_time
		start_time
		logger_application_name
         */
        
        /* https://localhost:8181/ActivityLogger/rest/devices?timestamp=2012-08-14T22:13:27%2B02&device_name=a619ed32f7d0de86a002757fbf8b29f4d1ab0aae
           &logger_application_name=Samsung_Galaxy_S_log&start_time=2012-06-18T22:54:15.000Z&end_time=2012-06-19
        With URL encoding (+ in timestamp remains):
		[class fi.vtt.DevicesResource] Basic HTTP authentication ('username','password'): 'administrator','...'.
		[class fi.vtt.DevicesResource] Full request URL: https://localhost:8181/ActivityLogger/rest/devices?timestamp=2012-08-14T22:13:27%2B02
		                                                 &device_name=a619ed32f7d0de86a002757fbf8b29f4d1ab0aae&logger_application_name=Samsung_Galaxy_S_log
		                                                 &start_time=2012-06-18T22:54:15.000Z&end_time=2012-06-19
		[class fi.vtt.DevicesResource] URL request parameters ('name','value'):
		'device_name','a619ed32f7d0de86a002757fbf8b29f4d1ab0aae'):
		'logger_application_name','Samsung_Galaxy_S_log'):
		'timestamp','2012-08-14T22:13:27+02'):
		'start_time','2012-06-18T22:54:15.000Z'):
		'end_time','2012-06-19'):
		[class fi.vtt.DevicesResource] URL request parameters from uriInfo.getQueryParameters() (MultivaluedMap):
		timestamp
		device_name
		end_time
		start_time
		logger_application_name
         */
        
        // Determine search terms for PostgreSQL query:
        
        // Check access restrictions (not the recommended way):
        if ( ( device_name == null || device_name.length() == 0 ) && !ConditionalAccess.NAME_DB_ADMINISTRATOR.equals(username) )
        	throw new javax.ws.rs.WebApplicationException(javax.ws.rs.core.Response.Status.FORBIDDEN);
              // A proper HTTP 403 response when an authenticated user has insufficient rights to the resource.
              // Wikipedia (http://en.wikipedia.org/wiki/List_of_HTTP_status_codes):
        	  // "The request was a valid request, but the server is refusing to respond to it.[2] Unlike a 401 Unauthorized response, authenticating will
              // make no difference.[2] On servers where authentication is required, this commonly means that the provided credentials were successfully
              // authenticated but that the credentials still do not grant the client permission to access the resource (e.g. a recognized user attempting
              // to access restricted content)."

        // Check access restrictions (recommended way):
        if ( ( device_name == null || device_name.length() == 0 ) && !sc.isUserInRole( ConditionalAccess.NAME_DB_ADMINISTRATOR_ROLE ) )
        	throw new javax.ws.rs.WebApplicationException(javax.ws.rs.core.Response.Status.FORBIDDEN);
              // A proper HTTP 403 response when an authenticated user has insufficient rights to the resource.
              // Wikipedia (http://en.wikipedia.org/wiki/List_of_HTTP_status_codes):
        	  // "The request was a valid request, but the server is refusing to respond to it.[2] Unlike a 401 Unauthorized response, authenticating will
              // make no difference.[2] On servers where authentication is required, this commonly means that the provided credentials were successfully
              // authenticated but that the credentials still do not grant the client permission to access the resource (e.g. a recognized user attempting
              // to access restricted content)."
        
        // Run a database request and create a REST response 
        
        try {
            logger.debug("intializing JDBC connection.");
            context = new InitialContext();
            dataSource = (DataSource) context.lookup("java:comp/env/jdbc/postgres");
            connection = dataSource.getConnection();
            logger.debug("Got connection..");
            // Get all devices from the database and collect them into the list:
            devices.addAll( Queries.getAllDevices( connection , device_name , logger_application_name , start_time , end_time ) );
              // Queries.java checks parameter validity. getAllDevices() also returns a List<Device> type.
            // devices.add( new Device(0, "devicename", "platform", new Timestamp(System.currentTimeMillis()).toString(), 302)); // Test item, no timezone.
            logger.debug("Fetching devices done.");
        } catch ( SQLException se ) {
        	// Create a proper return value for the HTTP request:
            se.printStackTrace();
            logger.warn(se.getMessage());
        	throw new javax.ws.rs.WebApplicationException(javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR);
        	  // A proper HTTP 500 response when the server (or the service) is not working properly.
        } catch (Exception e) {
        	// Something else may have happened?
            e.printStackTrace();
            logger.warn(e.getMessage());
        } finally {
            if (context != null) {
                try {
                	connection.close();
                    context.close();
                } catch (Exception e) {
                	System.out.print(e.toString());
                }
                context = null;
            }
        }

        return devices;
    }
    
    // http://localhost:8080/ActivityLogger/rest/devices/<deviceName>
	@Path("{deviceName}")
  	public DeviceNameResource getDevice(@PathParam("deviceName") String deviceName , @Context HttpHeaders headers , @Context HttpServletRequest req ) {
    	return new DeviceNameResource(uriInfo, request, deviceName, headers, req);
    }

}
