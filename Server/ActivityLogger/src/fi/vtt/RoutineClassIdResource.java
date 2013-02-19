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

import javax.naming.InitialContext;
import javax.sql.DataSource;
import javax.ws.rs.GET;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.UriInfo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import db.Queries;
import fi.vtt.activitylogger.RoutineClass;

//Jersey REST references:
//http://www.vogella.com/articles/REST/article.html
//http://www.ibm.com/developerworks/web/library/wa-aj-tomcat/
//http://coenraets.org/blog/2011/11/building-restful-services-with-java-using-jax-rs-and-jersey-sample-application/

// This class maps to requests http://localhost:8080/ActivityLogger/rest/routine_classes/<routine_id>
// (or https://<your server>:<port>/ActivityLogger/rest/routine_classes/<routine_id>).
// There is no @Path("routine_classes/{routine_id}") annotation here because the RoutineClassesResource class redirects that same request to this
// class.

public class RoutineClassIdResource {

  @Context
  UriInfo uriInfo;
  @Context
  Request request;
  String routineId;

  private final static Logger logger = LoggerFactory.getLogger(RoutineClassIdResource.class);

  public RoutineClassIdResource(UriInfo uriInfo, Request request, String routineId ) {
    this.uriInfo = uriInfo;
    this.request = request;
    this.routineId = routineId;
  }

  @GET
  @Produces({MediaType.TEXT_XML, MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    // Browser XML response, XML application response and JSON application response all in the same method automatically.
  public RoutineClass getRoutineClass() {
      logger.debug("URL .../rest/routine_classes/<routine_id> called.");
      javax.naming.Context context = null; // JDBC
      DataSource dataSource = null;
      Connection connection = null;
      RoutineClass routine = null;
      try {
          logger.debug("intializing JDBC connection.");
          context = new InitialContext();
          dataSource = (DataSource) context.lookup("java:comp/env/jdbc/postgres");
          connection = dataSource.getConnection();
          logger.debug("Got connection..");
          // Get identified routine class from the database:
          routine = Queries.findRoutineClassById(connection, Integer.parseInt(routineId) ); // Returns null if no match.
          logger.debug("Fetching routine class " + routineId + " done.");
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
      if(routine==null)
          // throw new RuntimeException("GET: Routine class with the id " + routineId +  " not found!");
      	  throw new javax.ws.rs.WebApplicationException(javax.ws.rs.core.Response.Status.NOT_FOUND); // Proper HTTP 404 response when resource was not found.
      	/* No need to have the method introduce RuntimeExceptions in its signature, see inheritance:
		   java.lang.Object
  		    extended by java.lang.Throwable
      	     extended by java.lang.Exception
              extended by java.lang.RuntimeException
               extended by javax.ws.rs.WebApplicationException */
      return routine;
  } 

  // POST, PUT and DELETE methods for a single device could be added here...
  
} 