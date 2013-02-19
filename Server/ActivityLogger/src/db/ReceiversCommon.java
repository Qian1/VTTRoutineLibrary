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

package db;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fi.vtt.activitylogger.Device;
import fi.vtt.activitylogger.LoggerApplication;

/** Helper methods to create some records into routine_db for ActivityLogger REST API */
public class ReceiversCommon {

	/** Log4Java instance for this class. */
    private final static Logger logger          = LoggerFactory.getLogger(ReceiversCommon.class);

    /** Default logger application name: {@value} */
    public final static String  LOGGER_APP_NAME = "RoutineLoggerX";

    /** Find or create a named logger application record from the logger_applications table in the routine_db
     * @param connection routine_db PostgreSQL connection.
     * @param loggerApplicationName Name of the searched/created logger application.
     * @return A new LoggerApplication object initialized with the values taken from the database. Null is returned if the name was not found.
     * @throws SQLException The query or DB connection failed.
     */
    public static LoggerApplication findOrCreateLoggerApp(Connection connection,
            String loggerApplicationName) throws SQLException {
        boolean exists = Queries.hasLoggerApplication(connection, loggerApplicationName);
        if (!exists) {
            final long idUnused = -1;
            LoggerApplication loggerApp = new LoggerApplication(idUnused, loggerApplicationName);
            Queries.insertLoggerApplication(connection, loggerApp);
        }

        LoggerApplication loggerApplication = Queries.findLoggerApplication(connection,
                loggerApplicationName);
        return loggerApplication;
    }

    /** Find or create a named device (user) record from the devices table in the routine_db
     * @param hash Name of the searched/ceated device (user). Usually a SHA1 hash.
     * @param connection  routine_db PostgreSQL connection.
     * @throws SQLException The query or DB connection failed.
     * @throws Exception If the device with the given name could not be created.
     */
    public static void findOrCreateDevice(String hash, Connection connection) throws SQLException,
            Exception {
        boolean deviceExists = Queries.hasDevice(connection, hash);
        if (!deviceExists) {
            logger.debug("No such device, inserting new device with hash: " + hash);
            final String TEST_PLATFORM = "plat";
            final int NO_MCC_SET = 244; // mcc
            Date now = new Date();
            Timestamp deviceCreationTimestamp = new Timestamp(now.getTime());
            Device newDevice = new Device(-1L, hash, TEST_PLATFORM,
                    deviceCreationTimestamp.toString(), // Should be an ISO-8061
                                                        // or PostgreSQL time
                                                        // string?
                    NO_MCC_SET);
            Queries.insertDevice(connection, newDevice);
            logger.debug("Inserted new device");
            deviceExists = Queries.hasDevice(connection, hash);
        }
        if (!deviceExists) {
            throw new Exception("Device with hash: " + hash + " does not exist!");
        }
    }

}
