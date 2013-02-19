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

import db.Queries;
import fi.vtt.activitylogger.Application;
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

//Jersey REST references:
//http://www.vogella.com/articles/REST/article.html
//http://www.ibm.com/developerworks/web/library/wa-aj-tomcat/
//http://coenraets.org/blog/2011/11/building-restful-services-with-java-using-jax-rs-and-jersey-sample-application/

// This class maps to requests http://localhost:8080/ActivityLogger/rest/applications/<application_name>
// (or https://<your server>:<port>/ActivityLogger/rest/applications/<application_name>).
// There is no @Path("/applications/{application_name}") annotation here because the DevicesResource class redirects that same request to this class.

public class ApplicationNameResource {

  @Context
  UriInfo uriInfo;
  @Context
  Request request;
  String applicationName;

  private final static Logger logger = LoggerFactory.getLogger(ApplicationNameResource.class);

  public ApplicationNameResource(UriInfo uriInfo, Request request, String applicationName ) {
    this.uriInfo = uriInfo;
    this.request = request;
    this.applicationName = applicationName;
  }

  @GET
  @Produces({MediaType.TEXT_XML, MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON})
    // Browser XML response, XML application response and JSON application response all in the same method automatically.
  public Application getApplication() {
      logger.debug("URL .../rest/applications/<application_name> called.");
      javax.naming.Context context = null; // JDBC
      DataSource dataSource = null;
      Application application = null;
      Connection connection = null;
      try {
          logger.debug("intializing JDBC connection.");
          context = new InitialContext();
          dataSource = (DataSource) context.lookup("java:comp/env/jdbc/postgres");
          connection = dataSource.getConnection();
          logger.debug("Got connection..");
          // Get named device from the database:
          application = Queries.findApplicationByName(connection, applicationName ); // Returns first match only or null if no application.
          logger.debug("Fetching application '" + applicationName + "' done.");
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
      if(application==null)
          // throw new RuntimeException("GET: Application with the name '" + applicationName +  "' not found!");
      	  throw new javax.ws.rs.WebApplicationException(javax.ws.rs.core.Response.Status.NOT_FOUND); // Proper HTTP 404 response when resource was not found.
      	/* No need to have the method introduce RuntimeExceptions in its signature, see inheritance:
		   java.lang.Object
  		    extended by java.lang.Throwable
      	     extended by java.lang.Exception
              extended by java.lang.RuntimeException
               extended by javax.ws.rs.WebApplicationException */
      return application;
  } 

  // POST, PUT and DELETE methods for a single device could be added here...
  
} 