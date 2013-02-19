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

import com.google.gson.Gson;
import db.Queries;
import db.ReceiversCommon;
import fi.vtt.activitylogger.Device;
import fi.vtt.activitylogger.LoggerApplication;
import fi.vtt.routinelib.common.RoutineData;
import java.sql.Connection;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.sql.DataSource;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("routinerecognized/{shahash}")
public class RoutineRecognizedReceiver {
    private final static Logger logger = LoggerFactory.getLogger(RoutineRecognizedReceiver.class);

    // http://localhost:8080/ActivityLogger/rest/routinerecognized/123456
    // https://<your server>:<port>/ActivityLogger/rest/routinerecognized/123456

    @POST
    @Produces("text/html")
    public void postRoutineRecognizedData(@PathParam("shahash") String hash, String content) {
        logger.debug("hash: " + hash);
        logger.debug("content: " + content);
        Gson gson = new Gson();
        RoutineData routineData = gson.fromJson(content, RoutineData.class);
        Context context = null; // JDBC
        DataSource dataSource = null;
        Connection connection = null;
        try {
            context = new InitialContext();
            dataSource = (DataSource) context.lookup("java:comp/env/jdbc/postgres");
            connection = dataSource.getConnection();
            ReceiversCommon.findOrCreateDevice(hash, connection);
            Device device = Queries.findDevice(connection, hash);
            LoggerApplication loggerApplication = ReceiversCommon.findOrCreateLoggerApp(connection,
                    ReceiversCommon.LOGGER_APP_NAME);
            Queries.insertNewRecogizedRoutine(connection, device.getDeviceId(), loggerApplication,
                    routineData);
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
}