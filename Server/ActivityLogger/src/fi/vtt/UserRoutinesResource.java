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

// import javax.naming.Context;	// Name clash with javax.ws.rs.core.Context. Use full name.
import javax.naming.InitialContext;
import javax.sql.DataSource;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import db.Queries;
//import db.ReceiversCommon;

import java.util.ArrayList;
import java.util.List;

//import fi.vtt.activitylogger.Device;
//import fi.vtt.activitylogger.LoggerApplication;
import fi.vtt.activitylogger.UserRoutine;
//import fi.vtt.routinelib.common.RawLogData;

// Jersey REST references:
// http://www.vogella.com/articles/REST/article.html
// http://en.wikipedia.org/wiki/Java_API_for_RESTful_Web_Services
// http://www.ibm.com/developerworks/web/library/wa-aj-tomcat/
// http://coenraets.org/blog/2011/11/building-restful-services-with-java-using-jax-rs-and-jersey-sample-application/

// This class maps to requests http://localhost:8080/ActivityLogger/rest/user_routines
// (or https://<your server>:<port>/ActivityLogger/rest/user_routines):
@Path("/user_routines")
public class UserRoutinesResource {

	// Allows to insert contextual objects into the class, e.g. ServletContext, Request, Response, UriInfo.
	@Context
	UriInfo uriInfo;
	@Context
	Request request;

    private final static Logger logger = LoggerFactory.getLogger(UserRoutinesResource.class); // Log4Java.
    
    @GET
    @Produces({MediaType.TEXT_XML, MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
      // Browser XML response, XML application response and JSON application response all in the same method automatically.
    public List<UserRoutine> getUserRoutines() {
        logger.debug("URL .../rest/user_routines called.");
        javax.naming.Context context = null; // JDBC.
        DataSource dataSource = null;
        Connection connection =  null;
        List<UserRoutine> routines = new ArrayList<UserRoutine>(); // Return value to function and the corresponding REST service.
        try {
            logger.debug("intializing JDBC connection.");
            context = new InitialContext();
            dataSource = (DataSource) context.lookup("java:comp/env/jdbc/postgres");
            connection = dataSource.getConnection();
            logger.debug("Got connection..");
            // Get all user routines from the database and collect them into the list:
            routines.addAll(Queries.getAllUserRoutines(connection)); // getAllUserRoutines() also returns a List<UserRoutine> type.
            logger.debug("Fetching user routines done.");
        } catch (Exception e) {
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

        return routines;
    }

    /** Test adding a new user routine and return the updated list of user routines, then undo the transaction in the database (no permanent change) */
    @POST
    @Path("/test") // https://<your server>:<port>/ActivityLogger/rest/user_routines/test/.
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces({MediaType.TEXT_XML, MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON}) // Test response. Normally the response is empty.
    public List<UserRoutine> postUserRoutineTest( String content ) {
        logger.debug("URL .../rest/user_routines/test called (POST).");
        logger.debug("content:\n" + content); // Should be JSON data.

        String insertionError = null; // Result of the PostgreSQL transaction. null = ok.
        // Response response = null; // javax.ws.rs.core.Response.
        Gson gson = new Gson();
        UserRoutine userRoutineIn = null;
        
        // Attempt parsing the JSON:
        try {
        	userRoutineIn = gson.fromJson(content, UserRoutine.class);
        	// Normally call the method "String insertUserRoutine( Connection connection, UserRoutine userRoutine ) throws SQLException" here.
        } catch ( JsonSyntaxException jse ) {
        	// Bad JSON data, either in syntax or when mapped towards the UserRoutine class:
        	jse.printStackTrace();
        	// Return correct HTTP error message ("422 Unprocessable Entity", "400 Bad Request" is for the HTTP protocol itself?):
        	throw new javax.ws.rs.WebApplicationException(422); // Proper HTTP 422 response when message content is not suitable for the service.
        	//throw new javax.ws.rs.WebApplicationException(javax.ws.rs.core.Response.Status.BAD_REQUEST); // Proper HTTP 400 response when protocol error.
        	// javax.ws.rs.WebApplicationException() constructor can also take javax.ws.rs.core.Response object as an argument!
        }
        
        // Save a new user routine temporarily into the database and get the snapshot of the user routines then:
        javax.naming.Context context = null; // JDBC.
        DataSource dataSource = null;
        Connection connection = null;
        List<UserRoutine> routines = new ArrayList<UserRoutine>(); // Return value to function and the corresponding REST service.
        try {
            logger.debug("intializing JDBC connection.");
            context = new InitialContext();
            dataSource = (DataSource) context.lookup("java:comp/env/jdbc/postgres");
            connection = dataSource.getConnection();
            logger.debug("Got connection..");
            
            // Get all user routines from the database and collect them into the list:
            // routines.addAll(Queries.getAllUserRoutines(connection)); // getAllUserRoutines() also returns a List<UserRoutine> type.
            // logger.debug("Fetching user routines done.");
            
            // Add a new user routine and show the user routine list after that:
            routines.addAll( Queries.testInsertUserRoutine( connection, userRoutineIn ) );
            
        } catch ( SQLException se ) {
        	// Problems with the database, create a message for the user.
            se.printStackTrace();
            logger.warn(se.getMessage());
            insertionError = "DATABASE ERROR - Could not save the new user routine (bad JSON data values?):\n" + se.getMessage();
        } catch (Exception e) {
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

        /* Copy-paste code not yet modified for this method:
        javax.naming.Context context = null; // JDBC.
        DataSource dataSource = null;
        try {
            logger.debug("intializing JDBC connection.");
            context = new InitialContext();
            dataSource = (DataSource) context.lookup("java:comp/env/jdbc/postgres");
            String str = rawData.toString();
            logger.debug("Deserialized: " + str);
            Connection connection = dataSource.getConnection();
            logger.debug("Got connection..");
            // Inserting users's device
            ReceiversCommon.findOrCreateDevice(hash, connection);
            //
            Device device = Queries.findDevice(connection, hash);
            // We now should have a valid device
            logger.debug("deviceId: " + device.getDeviceId());

            LoggerApplication loggerApplication = ReceiversCommon.findOrCreateLoggerApp(connection,
                    ReceiversCommon.LOGGER_APP_NAME);
            long insertedId = Queries.insertRawMeasurement(connection, device.getDeviceId(),
                    loggerApplication, rawData);

            // Queries.insertUserRoutine(connection, userRoutine);
            // 2. insert routines if available
            // ...

            logger.debug("done.");
        } catch (Exception e) {
            e.printStackTrace();
            logger.warn(e.getMessage());
        } finally {
            if (context != null) {
                try {
                    context.close();
                } catch (NamingException e) {}
                context = null;
            }
        }
	*/
		
        if (insertionError!=null) {
	        // Failure (HTTP 500 Internal Server Error), return error message to the user:
	        Response response = Response.serverError().type(MediaType.TEXT_PLAIN).entity(insertionError).build();
	        throw new javax.ws.rs.WebApplicationException( response );
        } else
        	return routines; // "200 OK".
        
        /* A proper UserRoutine JSON object (for both POST input and GET output). userRoutinesId, applicationId and rawMeasurementId are ignored on POST:
    {
     "userRoutinesId":"3",
     "startTime":"2012-09-01 00:00:00+03",
     "endTime":"2012-09-30 23:59:59+03",
     "routineClassId":"3",
     "rawMeasurementId":"0",
     "confidence":"1.0",
     "application":[
       {
        "applicationId":"2",
        "applicationName":"Sovelluksen nimi 1",
        "packageClassName":"fi.vtt.testing.a"
       },
       {
        "applicationId":"3",
        "applicationName":"Sovelluksen nimi 2",
        "packageClassName":"fi.vtt.testing.b"
       }
     ],
     "routineClassName":"Sovellusrutiini 1",
     "latitude":"60.7617",
     "longitude":"23.4843",
     "cellId":"30",
     "deviceName":"Käyttäjä tai laite 1",
     "loggerApplicationName":"Samsung_Galaxy_S_log"
    }
         */
    }

    @POST
    // @Path("{shahash}") // We do not want URLs like: https://<your server>:<port>/ActivityLogger/rest/user_routines/<device_name>.
    @Consumes(MediaType.APPLICATION_JSON)
    // For the JSON echo service the method signature was:
    // @Produces({MediaType.TEXT_XML, MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON}) // Test response. Normally the response is empty.
    // public UserRoutine postUserRoutine( /*@PathParam("shahash") String hash ,*/ String content ) {
    @Produces(MediaType.TEXT_PLAIN) // This response type is kept in order to return an error message, if needed? Normally the response body is empty.
    public Response postUserRoutine( String content ) {
        logger.debug("URL .../rest/user_routines called (POST).");
        // logger.debug("hash: " + hash);
        logger.debug("content: " + content); // Should be JSON data.

        String insertionError = null; // Result of the PostgreSQL transaction. null = ok.
        Response response = null; // javax.ws.rs.core.Response.
        Gson gson = new Gson();
        UserRoutine userRoutineIn = null;
        // UserRoutine userRoutineOut = new UserRoutine(); // Return value for the echo service.
	        /* For a default UserRoutine the POST response is given as (no null items are listed): 
	           {"userRoutinesId":"0","routineClassId":"0","rawMeasurementId":"0","confidence":"0.0","latitude":"0.0","longitude":"0.0","cellId":"0"} */
        javax.naming.Context context = null; // JDBC.
        DataSource dataSource = null;
        Connection connection = null;
        
		// Test responses:
		if ("test error".equals(content)) {
			insertionError = content; // POSTed text is the "result".
		} else {
			try {
				userRoutineIn = gson.fromJson(content, UserRoutine.class);               // Throws JsonSyntaxException.
				// Connect to the database:
	            logger.debug("intializing JDBC connection.");
	            context = new InitialContext(); 										 // Throws namingException. 
	            dataSource = (DataSource) context.lookup("java:comp/env/jdbc/postgres"); //
	            connection = dataSource.getConnection(); 								 // Throws SQLException.
	            logger.debug("Got connection..");
	            // Save the new user routine (fields, like UserRoutinesId, on the userRoutine parameter object may be updated by the method!):
				insertionError = Queries.insertUserRoutine( connection, userRoutineIn ); // Throws SQLException.
			} catch ( JsonSyntaxException jse ) {
				// Bad JSON data, either in syntax or when mapped towards the UserRoutine class:
				jse.printStackTrace();
				// Return correct HTTP error message ("422 Unprocessable Entity", "400 Bad Request" is for the HTTP protocol itself):
		      	throw new javax.ws.rs.WebApplicationException(422); // Proper HTTP 422 response when message content is not suitable for the service.
		      	//throw new javax.ws.rs.WebApplicationException(javax.ws.rs.core.Response.Status.BAD_REQUEST);
		      	  // Proper HTTP 400 response when protocol error.
		      	// javax.ws.rs.WebApplicationException() constructor can also take javax.ws.rs.core.Response object as an argument!
	        } catch ( SQLException se ) {
	        	// Problems with the database, create a message for the user.
	            se.printStackTrace();
	            logger.warn(se.getMessage());
	            insertionError = "DATABASE ERROR - Could not save the new user routine (bad JSON data values?):\n" + se.getMessage();
	        } catch (Exception e) {
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
		}

		// Check if everything went well with the DB:
        if (insertionError==null) {
        // All went well (HTTP 201 Created):
	        response = Response.created( uriInfo.getRequestUriBuilder().path( Long.toString( userRoutineIn.getUserRoutinesId() ) ).build() ).build();
	          // Create a new URL for user routine. Method created(URI) produces the HTTP 201 response as a ResponseBuilder object.
	          // The new user routine id is added to the base URL by using the path() method of the UriBulder class.
        } else {
	        // Failure (HTTP 500 Internal Server Error):
	        response = Response.serverError().type(MediaType.TEXT_PLAIN).entity(insertionError).build();
        }
        
        // If needed, the response can be forced to return the "201 Created" message with the following code (although in the final version this
        // method should have a return value of Response instead of UserRoutine):
        /*
        Response response; // javax.ws.rs.core.Response.
        response = Response.created( uriInfo.getRequestUriBuilder().path( Long.toString( userRoutineOut.getUserRoutinesId() ) ).build() ).build();
          // Create a new URL for user routine. Method created(URI) produces the HTTP 201 response as a ResponseBuilder object.
          // The new user routine id is added to the base URL by using the path() method of the UriBulder class.
      	throw new javax.ws.rs.WebApplicationException( response );
        */        
        // return userRoutineIn; // or userRoutineOut;. This is used for the echo functionality. In the final version this method returns a Response object.
        
        return response; // "201 Created" (+ Location header & URL), "422 Unprocessable Entity" (+ error text) or "500 Internal Server Error" (empty).
    }
    
    // http://localhost:8080/ActivityLogger/rest/user_routines/<user_routine_id>
	@Path("{user_routine_id}")
  	public UserRoutineResource getUserRoutine(@PathParam("user_routine_id") String user_routine_id ) {
    	return new UserRoutineResource(uriInfo, request, user_routine_id );
    }

}
