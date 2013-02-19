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
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
//import java.sql.Time;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import fi.vtt.activitylogger.Device;
import fi.vtt.activitylogger.LoggerApplication;
import fi.vtt.activitylogger.RawMeasurement;
import fi.vtt.activitylogger.RoutineClass;
import fi.vtt.activitylogger.UserRoutine;
import fi.vtt.activitylogger.Mcc;
// import fi.vtt.activitylogger.Application; // Name clash! Jersey REST API supporting class.
import fi.vtt.routinelib.common.Application; 
import fi.vtt.routinelib.common.GPSData;
import fi.vtt.routinelib.common.RawLogData;
import fi.vtt.routinelib.common.RoutineData;

/** Elementary helper PostgreSQL database operations for accessing the routine_db from a single session
 * <p>This class is heavily used by the REST services of ActivityLogger. These methods also call each other in some cases.</p>*/
public class Queries {
	
    /** Error message for using a "deprecated" device meta data field when adding a user routine the old way. */
    public final static String LAUNCH_TIME_NOT_USED = "launchTimeNotUsed!";
    
    // devices table of routine_db (DB helper methods)
    
    /** Check for an existing device record from the devices table of the routine_db data base
     * @param connection routine_db PostgreSQL connection.
     * @param deviceName Name of the searched device (user). Usually a SHA1 hash.
     * @return true, if found, false otherwise.
     * @throws SQLException The query or DB connection failed.
     */
    public static boolean hasDevice(Connection connection, String deviceName) throws SQLException {
        PreparedStatement preparedStatement = null;
        ResultSet rs = null;
        boolean contains = false;
        try {
            String sql = "SELECT device_id FROM devices WHERE device_name = ?";
            preparedStatement = connection.prepareStatement(sql);
            preparedStatement.setString(1, deviceName);
            rs = preparedStatement.executeQuery();
            if (rs.next()) {
                contains = true;
            }
        } finally {
            if (rs != null) {
                try {
                    rs.close();
                } catch (Exception e) {
                }
            }
            if (preparedStatement != null) {
                try {
                    preparedStatement.close();
                } catch (Exception e) {
                }
            }
        }
        return contains;
    }

    /** Insert a new device record into the devices table of the routine_db
     * @param connection routine_db PostgreSQL connection.
     * @param device Java object containing the device record values.
     * @return true, if insertion was successful, false otherwise (also if the exact record already existed as the records assume uniqueness).
     * @throws SQLException The query or DB connection failed.
     */
    public static boolean insertDevice(Connection connection, Device device) throws SQLException {
        PreparedStatement preparedStatement = null;
        boolean ret = false;
        try {
            String sql = "INSERT INTO devices (device_name, platform, device_creation_timestamp, mcc) "
                    + "VALUES(?, ?, ?, ?)";
            preparedStatement = connection.prepareStatement(sql);
            preparedStatement.setString(1, device.getDeviceName());
            preparedStatement.setString(2, device.getPlatform());
            
            // The following statement needs to be rewritten if we change the timestamp type in Device class, for example for the type long:
            // preparedStatement.setTimestamp(3, new Timestamp( device.getDeviceTimestamp() ) );
            // String format would be trickier but then we would have the timezone information..
            // Specify time string in PostgreSQL: '2012-08-16 16:28:56.583+03' (with quotes). This is the most transparent and portable way! 
            // preparedStatement.setTimestamp(3, device.getDeviceTimestamp()); // Original code with type Timestamp.
            // preparedStatement.setString(3, device.getDeviceTimestamp());
            String timeStampStr = device.getDeviceTimestamp();
            Timestamp tz = java.sql.Timestamp.valueOf(timeStampStr);
            preparedStatement.setTimestamp(3, tz); // The time zone information is probably lost (and set the DB default) with this form.
            // preparedStatement.setString(3, "'2012-08-16 16:28:56.583+03'"); // Test.
            
            preparedStatement.setInt(4, device.getMcc());
            int insertCount = preparedStatement.executeUpdate();
            if (insertCount > 0) {
                ret = true;
            }
        } finally {
            if (preparedStatement != null) {
                preparedStatement.close();
            }
        }
        return ret;
    }

    /** Get an existing device record from the devices table of the routine_db data base
     * @param connection routine_db PostgreSQL connection.
     * @param deviceName Name of the searched device (user). Usually a SHA1 hash.
     * @return Java object containing the device record values. Null is returned if the device is not found or some other error happens.
     * @throws SQLException The query or DB connection failed.
     */
    public static Device findDevice(Connection connection, String deviceName) throws SQLException {
        PreparedStatement preparedStatement = null;
        ResultSet rs = null;
        Device device = null;
        try {
            String sql = "SELECT device_id, device_name, platform, device_creation_timestamp, mcc "
                    + "FROM devices WHERE device_name = ?";
            preparedStatement = connection.prepareStatement(sql);
            preparedStatement.setString(1, deviceName);
            rs = preparedStatement.executeQuery();
            if (rs.next()) {
                long deviceId = rs.getLong(1);
                String name = rs.getString(2);
                String platform = rs.getString(3);
                
                // Get the value of PostgreSQL type "time stamp with time zone":
                //Timestamp deviceTimestamp_sql = rs.getTimestamp(4); // Not a very useful type as it has no timezone information & does not work with
                                                                    // annotations.
                // System.out.println("deviceTimestamp_sql: " + deviceTimestamp_sql); // deviceTimestamp_sql: 2012-09-06 13:13:30.913
                // long deviceTimestamp_long = rs.getLong(4);
                  // UNIX time but in which time zone? Causes exception:
                  // org.postgresql.util.PSQLException: Bad value for type long : 2012-09-06 13:13:30.913+03
                // System.out.println("deviceTimestamp_long: " + deviceTimestamp_long);
                String deviceTimestamp_string = rs.getString(4);    // Text version of the time stamp. Time is given in the default DB time zone.
                // System.out.println("deviceTimestamp_string: " + deviceTimestamp_string);
                  //  deviceTimestamp_string: 2012-09-06 13:13:30.913+03
                int mcc = rs.getInt(5);
                device = new Device(deviceId, name, platform, deviceTimestamp_string, mcc);
            }
        } finally {
            if (rs != null) {
                try {
                    rs.close();
                } catch (Exception e) {
                }
            }
            if (preparedStatement != null) {
                try {
                    preparedStatement.close();
                } catch (Exception e) {
                }
            }
        }
        return device;
    }

    /** Remove a device record from the routine_db devices table
     * @param connection routine_db PostgreSQL connection.
     * @param deviceId Value of the devices.device_id field in the DB record.
     * @return true, if removed, false otherwise.
     * @throws SQLException The query or DB connection failed.
     */
    public static boolean deleteDevice(Connection connection, long deviceId) throws SQLException {
        boolean removed = false;
        PreparedStatement preparedStatement = null;
        String sql = "DELETE FROM devices WHERE device_id = ?";
        try {
            preparedStatement = connection.prepareStatement(sql);
            preparedStatement.setLong(1, deviceId);
            int removedCount = preparedStatement.executeUpdate();
            if (removedCount > 0) {
                removed = true;
            }
        } finally {
            if (preparedStatement != null) {
                preparedStatement.close();
            }
        }
        return removed;
    }

    /** Get a list of all devices (users) from the routine database, to be returned by the .../rest/devices REST URL
     * <p>
     *  Empty or null search parameters are ignored. Timestamp formats: PostgreSQL 2012-09-06 13:13:30.913+03 and ISO 8601 2012-09-06T13:13:30.913+03.
     *  Verify strings before insertion to avoid SQL injection attacks. Timestamps are protected already. All strings are inserted with
     *  <em>preparedStatement.setString()</em> methods.
     * </p> 
     * @param connection The PostgreSQL 9.0/9.1 routine database connection.
     * @param deviceName Limit results of the search only devices belonging to this user (only returns the same instance). Null or empty give all.
     * @param loggerApplicationName Limit results of the search only to logging context of the device/user. Null or empty give all.
     * @param startTime PostgreSQL/ISO8601 timestamp to limit returned devices to those added at the same or newer time than the given time. 
     * @param endTime PostgreSQL/ISO8601 timestamp to limit returned devices to those added at the same or newer time than the given time.
     * @return A list of found devices (may be empty if none was found).
     * @throws SQLException If problems arose with the query. */
    public static List<Device> getAllDevices(
    		Connection connection , String deviceName , String loggerApplicationName , String startTime , String endTime ) throws SQLException {
    	// Check that timestamps are not empty or null, and if not, replace them with reasonable search values:
    	// TBD.
        PreparedStatement preparedStatement = null;
        ResultSet rs = null;
        Device device = null;
        List<Device> devices = new ArrayList<Device>(); // Return value to function and the corresponding REST service.
        try {
        	// Original SQL query before using makeFilteredFutuDBSqlQuery():
        	/*
            String sql = "SELECT device_id, device_name, platform, device_creation_timestamp, mcc " +
                         "FROM devices " +
                         "WHERE device_name = ? " +
                         "AND ?::timestamp(0) with time zone <= device_creation_timestamp " + // Cast "::timestamp(0) with time zone" forces the string to
                         "AND device_creation_timestamp <= ?::timestamp(0) with time zone ";  // be parsed as a valid timestamp.
            preparedStatement = connection.prepareStatement(sql);
            preparedStatement.setString(1, deviceName); // This will make the name a string literal that cannot be escaped to cause an injection attack?
            preparedStatement.setString(2, startTime);
            preparedStatement.setString(3, endTime);
            System.out.println( "[Queries.java:getAllDevices()] PostgreSQL prepared statement: " + preparedStatement.toString() ); // Debug print.
            */
            // New way to create the prepared statement
        	// (always add ", devices , logger_applications" to the last entries after FROM if those tables are not already included into the query
        	// because makeFilteredFutuDBSqlQuery() needs those tables for handling possible device_name and logger_application_name query parameters;
        	// alternatively you can set device_name and/or logger_application_name to null if it would not affect the query):
        	loggerApplicationName = null; // Make sure this is null because it is not needed in the query!
            PreparedStatement newPreparedStatement = db.ConditionalAccess.makeFilteredFutuDBSqlQuery(
            		connection,
            		"SELECT device_id, device_name, platform, device_creation_timestamp, mcc\n FROM devices ", // ",logger_applications" removed because
            		deviceName, loggerApplicationName,                                                         // it duplicates the results!
            		"device_creation_timestamp", startTime, // Specify what columns are used for filtering results by time
            		"device_creation_timestamp", endTime ); // and specify the time stamps.
            System.out.println( "[Queries.java:getAllDevices()] New PostgreSQL prepared statement: " + newPreparedStatement.toString() ); // Debug print.
            rs = newPreparedStatement.executeQuery(); // preparedStatement.executeQuery()
            while (rs.next()) {
                long deviceId = rs.getLong(1);
                String name = rs.getString(2);
                String platform = rs.getString(3);
                
                // Get the value of PostgreSQL type "time stamp with time zone":
                // Timestamp deviceTimestamp_sql = rs.getTimestamp(4); // Not a useful type as it has no timezone information & does not work with annotations.
                // System.out.println("deviceTimestamp_sql: " + deviceTimestamp_sql); // deviceTimestamp_sql: 2012-09-06 13:13:30.913
                // long deviceTimestamp_long = rs.getLong(4);          // UNIX time but in which time zone? Causes exception:
                  // org.postgresql.util.PSQLException: Bad value for type long : 2012-09-06 13:13:30.913+03
                // System.out.println("deviceTimestamp_long: " + deviceTimestamp_long);
                String deviceTimestamp_string = rs.getString(4);    // Text version of the timestamp. deviceTimestamp_string: 2012-09-06 13:13:30.913+03
                // System.out.println("deviceTimestamp_string: " + deviceTimestamp_string);
                
                int mcc = rs.getInt(5);
                device = new Device(deviceId, name, platform, deviceTimestamp_string, mcc);
                
                devices.add(device);
            }
        } finally {
            if (rs != null) {
                try {
                    rs.close();
                } catch (Exception e) {
                }
            }
            if (preparedStatement != null) {
                try {
                    preparedStatement.close();
                } catch (Exception e) {
                }
            }
        }
        return devices;
    }
    
    // logger_applications table (helper DB methods)

    /** Check for an existing logger application record from the logger_applications table of the routine_db data base
     * @param connection routine_db PostgreSQL connection.
     * @param loggerApplicationName Name of the searched logger application.
     * @return true, if found, false otherwise.
     * @throws SQLException The query or DB connection failed.
     */
    public static boolean hasLoggerApplication(Connection connection, String loggerApplicationName)
            throws SQLException {
        PreparedStatement preparedStatement = null;
        ResultSet rs = null;
        boolean contains = false;
        try {
            String sql = "SELECT logger_application_id FROM logger_applications WHERE logger_application_name = ?";
            preparedStatement = connection.prepareStatement(sql);
            preparedStatement.setString(1, loggerApplicationName);
            rs = preparedStatement.executeQuery();
            if (rs.next()) {
                contains = true;
            }
        } finally {
            if (rs != null) {
                try {
                    rs.close();
                } catch (Exception e) {
                }
            }
            if (preparedStatement != null) {
                try {
                    preparedStatement.close();
                } catch (Exception e) {
                }
            }
        }
        return contains;
    }

    /** Insert a new logger application record into the logger_applications table of the routine_db
     * @param connection PostgreSQL connection.
     * @param loggerApp The friendly name of the application that provides the routine data. Enables having multiple user/profiles per device.
     * @return true, if insertion was successful, false otherwise (also if the exact record already existed as the records assume uniqueness).
     * @throws SQLException
     */
    public static boolean insertLoggerApplication(Connection connection, LoggerApplication loggerApp)
            throws SQLException {
        PreparedStatement preparedStatement = null;
        boolean ret = false;
        try {
            String sql = "INSERT INTO logger_applications (logger_application_name) VALUES (?)";
            preparedStatement = connection.prepareStatement(sql);
            preparedStatement.setString(1, loggerApp.getLoggerApplicationName());
            int insertCount = preparedStatement.executeUpdate();
            if (insertCount > 0) {
                ret = true;
            }
        } finally {
            if (preparedStatement != null) {
                preparedStatement.close();
            }
        }
        return ret;
    }

    /** Fetch details of the named logger application from the database and create an instance
     *  (note that logger application is not tied to any specific device in the database!)
     * @param connection A postgreSQL database connection to the routine tables.
     * @param loggerApplicationName A routine database wide identifier for a data logger application (context).
     * @return A new LoggerApplication object initialized with the values taken from the database. Null is returned if the name was not found.
     * @throws SQLException If database errors take place. */
    public static LoggerApplication findLoggerApplication(Connection connection,
            String loggerApplicationName) throws SQLException {
        PreparedStatement preparedStatement = null;
        ResultSet rs = null;
        LoggerApplication loggerApplication = null;
        try {
            String sql = "SELECT logger_application_id, logger_application_name FROM logger_applications WHERE logger_application_name = ?";
            preparedStatement = connection.prepareStatement(sql);
            preparedStatement.setString(1, loggerApplicationName);
            rs = preparedStatement.executeQuery();
            if (rs.next()) {
                long id = rs.getLong(1);
                String name = rs.getString(2);
                loggerApplication = new LoggerApplication(id, name);
            }
        } finally {
            if (rs != null) {
                try {
                    rs.close();
                } catch (Exception e) {
                }
            }
            if (preparedStatement != null) {
                try {
                    preparedStatement.close();
                } catch (Exception e) {
                }
            }
        }
        return loggerApplication;
    }

    /** Remove a logger application record from the routine_db logger_applications table
     * @param connection PostgreSQL connection.
     * @param loggerApplicationId Value of the logger_applications.logger_application_id field in the DB record.
     * @return true, if removed, false otherwise.
     * @throws SQLException The query or DB connection failed.
     */
    public static boolean deleteLoggerApplication(Connection connection, long loggerApplicationId)
            throws SQLException {
        boolean removed = false;
        PreparedStatement preparedStatement = null;
        try {
            String sql = "DELETE FROM logger_applications WHERE logger_application_id = ?";
            preparedStatement = connection.prepareStatement(sql);
            preparedStatement.setLong(1, loggerApplicationId);
            int removedCount = preparedStatement.executeUpdate();
            if (removedCount > 0) {
                removed = true;
            }
        } finally {
            if (preparedStatement != null) {
                preparedStatement.close();
            }
        }
        return removed;
    }

    /** Get a list of all logger applications from the routine database, to be returned by the .../rest/logger_applications REST URL 
     * @param connection The PostgreSQL 9.0/9.1 routine database connection.
     * @return A list of found logger applications (may be empty if none was found).
     * @throws SQLException If problems arose with the query. */
    public static List<LoggerApplication> getAllLoggerApplications(Connection connection) throws SQLException {
        PreparedStatement preparedStatement = null;
        ResultSet rs = null;
        LoggerApplication application = null;
        List<LoggerApplication> applications = new ArrayList<LoggerApplication>(); // Return value to function and the corresponding REST service.
        try {
            String sql = "SELECT logger_application_id, logger_application_name " +
                         "FROM logger_applications";
            preparedStatement = connection.prepareStatement(sql);
            rs = preparedStatement.executeQuery();
            while (rs.next()) {
                long id = rs.getLong(1);
                String name = rs.getString(2);
                application = new LoggerApplication(id, name);
                applications.add(application);
            }
        } finally {
            if (rs != null) {
                try {
                    rs.close();
                } catch (Exception e) {
                }
            }
            if (preparedStatement != null) {
                try {
                    preparedStatement.close();
                } catch (Exception e) {
                }
            }
        }
        return applications;
    }
    
    // application table of routine_db (DB helper methods)
    
    /** Check for an existing application record from the application table of the routine_db data base
     * @param connection routine_db PostgreSQL connection.
     * @param name Name of the searched application. It may be any of the applications that the user has run in his device.
     * @param className The Java package and class name of the application.
     * @return true, if found, false otherwise.
     * @throws SQLException The query or DB connection failed.
     */
    public static boolean hasApplication(Connection connection, String name, String className)
            throws SQLException {
        PreparedStatement preparedStatement = null;
        ResultSet rs = null;
        boolean contains = false;
        try {
            String sql = "SELECT application_id FROM application WHERE application_name = ? AND package_class_name = ?";
            preparedStatement = connection.prepareStatement(sql);
            preparedStatement.setString(1, name);
            preparedStatement.setString(2, className);
            rs = preparedStatement.executeQuery();
            if (rs.next()) {
                contains = true;
            }
        } finally {
            if (rs != null) {
                try {
                    rs.close();
                } catch (Exception e) {
                }
            }
            if (preparedStatement != null) {
                try {
                    preparedStatement.close();
                } catch (Exception e) {

                }
            }
        }
        return contains;
    }

    /** Insert a new application record into the application table of the routine_db
     * @param connection routine_db PostgreSQL connection.
     * @param application Java object containing the application record values.
     * @return true, if insertion was successful, false otherwise (also if the exact record already existed as the records assume uniqueness).
     * @throws SQLException The query or DB connection failed.
     */
    public static boolean insertApplication(Connection connection, Application application)
            throws SQLException {
        PreparedStatement preparedStatement = null;
        boolean ret = false;
        try {
            String sql = "INSERT INTO application (application_name, package_class_name) VALUES(?, ?)";
            preparedStatement = connection.prepareStatement(sql);
            preparedStatement.setString(1, application.getName());
            preparedStatement.setString(2, application.getClassName());
            int insertCount = preparedStatement.executeUpdate();
            if (insertCount > 0) {
                ret = true;
            } else {
                throw new SQLException("Application insert failure!");
            }
        } finally {
            if (preparedStatement != null) {
                preparedStatement.close();
            }
        }
        return ret;
    }

    /** Get the record id of an existing application record from the application table of the routine_db data base
     * @param connection routine_db PostgreSQL connection.
     * @param name Name of the application.
     * @param className The Java package and class name of the application.
     * @return The value of the field application.application_id corresponding to the search parameter values, or -1 if not found.
     * @throws SQLException The query or DB connection failed.
     */
    public static long findApplicationId(Connection connection, String name, String className)
            throws SQLException {
        PreparedStatement preparedStatement = null;
        ResultSet rs = null;
        long appId = -1;
        try {
            String sql = "SELECT application_id FROM application WHERE application_name = ? AND package_class_name = ?";
            preparedStatement = connection.prepareStatement(sql);
            preparedStatement.setString(1, name);
            preparedStatement.setString(2, className);
            rs = preparedStatement.executeQuery();
            if (rs.next()) {
                appId = rs.getLong(1);
            }
        } finally {
            if (rs != null) {
                try {
                    rs.close();
                } catch (Exception e) {
                }
            }
            if (preparedStatement != null) {
                try {
                    preparedStatement.close();
                } catch (Exception e) {

                }
            }
        }
        return appId;
    }

    /** Get the record of an existing application as a Java object from the application table of the routine_db data base
     * @param connection routine_db PostgreSQL connection.
     * @param appId Value of the application.application_id field in the DB record.
     * @return Java object containing the application record values. Null is returned if the application is not found or some other error happens.
     * @throws SQLException The query or DB connection failed.
     */
    public static Application findApplicationById(Connection connection, long appId)
            throws SQLException {
        PreparedStatement preparedStatement = null;
        ResultSet rs = null;
        Application app = null;
        try {
            String sql = "SELECT application_id, application_name, package_class_name FROM application WHERE application_id = ?";
            preparedStatement = connection.prepareStatement(sql);
            preparedStatement.setLong(1, appId);
            rs = preparedStatement.executeQuery();
            if (rs.next()) {
                long id = rs.getLong(1);
                String name = rs.getString(2);
                String className = rs.getString(3);
                app = new Application(name, className, LAUNCH_TIME_NOT_USED);
                app.setId(id);
            }
        } finally {
            if (rs != null) {
                try {
                    rs.close();
                } catch (Exception e) {
                }
            }
            if (preparedStatement != null) {
                try {
                    preparedStatement.close();
                } catch (Exception e) {

                }
            }
        }
        return app;
    }

    /** Get the first Jersey REST API compliant Application object found from the database
     * @param connection PostgreSQL database connection.
     * @param applicationName Name of the application stored in the database (may not be unique!). */
    public static fi.vtt.activitylogger.Application findApplicationByName(Connection connection, String applicationName )
            throws SQLException {
        PreparedStatement preparedStatement = null;
        ResultSet rs = null;
        fi.vtt.activitylogger.Application app = null;
        try {
            String sql = "SELECT application_id, application_name, package_class_name FROM application WHERE application_name = ?";
            preparedStatement = connection.prepareStatement(sql);
            preparedStatement.setString(1, applicationName );
            rs = preparedStatement.executeQuery();
            if (rs.next()) {
                long id = rs.getLong(1);
                String name = rs.getString(2);
                String className = rs.getString(3);
                app = new fi.vtt.activitylogger.Application(id, name, className);
            }
        } finally {
            if (rs != null) {
                try {
                    rs.close();
                } catch (Exception e) {
                }
            }
            if (preparedStatement != null) {
                try {
                    preparedStatement.close();
                } catch (Exception e) {

                }
            }
        }
        return app;
    }
    
    /** Remove an application record from the routine_db application table
     * @param connection routine_db PostgreSQL connection.
     * @param appId Value of the application.application_id field in the DB record.
     * @return true, if removed, false otherwise.
     * @throws SQLException The query or DB connection failed.
     */
    public static boolean deleteApplication(Connection connection, long appId) throws SQLException {
        boolean removed = false;
        PreparedStatement preparedStatement = null;
        try {
            String sql = "DELETE FROM application WHERE application_id = ?";
            preparedStatement = connection.prepareStatement(sql);
            preparedStatement.setLong(1, appId);
            int removedCount = preparedStatement.executeUpdate();
            if (removedCount > 0) {
                removed = true;
            }
        } finally {
            if (preparedStatement != null) {
                preparedStatement.close();
            }
        }
        return removed;
    }

    /** Get a list of all applications from the routine database, to be returned by the .../rest/applications REST URL 
     * @param connection The PostgreSQL 9.0/9.1 routine database connection.
     * @return A list of found applications (may be empty if none was found).
     * @throws SQLException If problems arose with the query.
     * <p>
     *  <b>Note:</b>
     *  This method uses class <em>fi.vtt.activitylogger.Application</em> instead of <em>fi.vtt.routinelib.common.Application</em>
     *  (to support Jersey REST API)! The classes should be unified in the future?
     * </p> */
    public static List<fi.vtt.activitylogger.Application> getAllApplications(Connection connection) throws SQLException {
        PreparedStatement preparedStatement = null;
        ResultSet rs = null;
        fi.vtt.activitylogger.Application application = null;
        List<fi.vtt.activitylogger.Application> applications = new ArrayList<fi.vtt.activitylogger.Application>();
          // Return value to function and the corresponding REST service.
        try {
            String sql = "SELECT application_id, application_name, package_class_name " +
                         "FROM application";
            preparedStatement = connection.prepareStatement(sql);
            rs = preparedStatement.executeQuery();
            while (rs.next()) {
                long id = rs.getLong(1);
                String name = rs.getString(2);
                String package_class_name = rs.getString(3);
                application = new fi.vtt.activitylogger.Application(id, name, package_class_name);
                applications.add(application);
            }
        } finally {
            if (rs != null) {
                try {
                    rs.close();
                } catch (Exception e) {
                }
            }
            if (preparedStatement != null) {
                try {
                    preparedStatement.close();
                } catch (Exception e) {
                }
            }
        }
        return applications;
    }
    
    // Basic raw measurement info (time, lat, lon, cellid, logger app id, device)

    /** Insert raw data (but without the list of running applications) into the raw_measurements table in the routine DB
     * @param connection routine_db PostgreSQL connection.
     * @param deviceId Value of the device_id_fk in the DB record, pointing to devices.device_id field.
     * @param loggerApp The logger application record values as a Java object (REST compliant version of the class).
     * @param rawLogData Most of the raw_measurements record values as a Java object.
     * @return raw_measurements.measurement_id for just inserted row
     * @throws SQLException The query or DB connection failed.
     * @see #insertRawMeasurement(Connection, long, LoggerApplication, RawLogData)
     * <p>raw_measurements.measurement_timestamp will get the value now(), i.e., the time of insertion into the database.</p>
     * <p>This method is called by insertRawMeasurement().</p> */
    public static long insertBasicRawData(Connection connection, long deviceId,
            LoggerApplication loggerApp, RawLogData rawLogData) throws SQLException {
        PreparedStatement preparedStatement = null;
        ResultSet rs = null;
        try {
            String sql = "INSERT INTO raw_measurements (latitude, longitude, cell_id, logger_application_id_fk, device_id_fk) "
                    + "VALUES (?, ?, ?, ?, ?) RETURNING measurement_id";
            preparedStatement = connection.prepareStatement(sql);

            // A proper null handling for latitude & longitude would be nice instead of the magic number:
            final double LATLONG_NOT_AVAILABLE = -1000.0;
            double latitude = LATLONG_NOT_AVAILABLE;
            double longitude = LATLONG_NOT_AVAILABLE;
            
            GPSData gpsData = rawLogData.getGpsData(); // Location may not be available.
            if (gpsData != null) {
                latitude = gpsData.getLatitude();
                longitude = gpsData.getLongitude();
            }
            
            preparedStatement.setDouble(1, latitude);
            preparedStatement.setDouble(2, longitude);
            preparedStatement.setInt(3, rawLogData.getCellId());
            preparedStatement.setLong(4, loggerApp.getLoggerApplicationId());
            preparedStatement.setLong(5, deviceId);
            
            // raw_measurements.measurement_timestamp will get the value now() by default, i.e., the time of insertion.

            rs = preparedStatement.executeQuery();
            if (rs.next()) {
                long insertedMeasurentId = rs.getLong(1);
                return insertedMeasurentId;
            } else {
                throw new SQLException("insertBasicRawData failed!");
            }

        } finally {
            if (rs != null) {
                try {
                    rs.close();
                } catch (Exception e) {
                }
            }
            if (preparedStatement != null) {
                preparedStatement.close();
            }
        }
    }

    // raw_measurement table (helper DB methods)

    /** Check for an existing raw measurement record from the raw_measurements table of the routine_db data base
     * @param connection routine_db PostgreSQL connection.
     * @param measurementId Value of the raw_measurements.measurement_id field in the DB record.
     * @return true, if found, false otherwise.
     * @throws SQLException The query or DB connection failed.
     */
    /* WAS NOT NEEDED:
    public static boolean hasRawMeasurement(Connection connection, long measurementId)
            throws SQLException {
        throw new SQLException("hasRawMeasurement");
    }
    */

    /** Get the record of an existing raw measurement as a Java object from the raw_measurements table of the routine_db data base
     * @param connection  routine_db PostgreSQL connection.
     * ...
     * @return Java object containing the raw_measurent record values. Null is returned if the measurement is not found or some other error happens. 
     * @throws SQLException The query or DB connection failed.
     */
    /* SHOULD BE ADDED WITH QUERY PARAMETERS SUCH AS TIME PERIOD, DEVICE NAME/ID AND LOGGER APPLICATION NAME/ID:
    public static RawLogData findMeasurementId(Connection connection) throws SQLException {
        throw new SQLException("findMeasurementId");
    }
    */

    /** Insert raw data (including the list of running applications) into the raw_measurements table in the routine DB
     * @param connection routine_db PostgreSQL connection.
     * @param deviceId Value of the device_id_fk in the DB record, pointing to devices.device_id field.
     * @param loggerApp The logger application record values as a Java object (REST compliant version of the class).
     * @param rawLogData Most of the raw_measurements record values as a Java object.
     * @return raw_measurements.measurement_id for just inserted row.
     * @throws SQLException The query or DB connection failed.
     * @see #insertBasicRawData(Connection, long, LoggerApplication, RawLogData)	Is called from this method. 
     * <p>This method calls insertBasicRawData().</p> */
    public static long insertRawMeasurement(Connection connection, long deviceId,
            LoggerApplication loggerApp, RawLogData rawLogData) throws SQLException {
        // First insert the basic data
        long measurementId = Queries
                .insertBasicRawData(connection, deviceId, loggerApp, rawLogData);
        // Then insert all applications belonging to this raw data set
        ArrayList<Application> apps = rawLogData.getApps();
        for (Application application : apps) {
            String name = application.getName();
            String className = application.getClassName();
            boolean appExists = Queries.hasApplication(connection, name, className);
            if (!appExists) {
                Queries.insertApplication(connection, application);
            }
            long applicationId = Queries.findApplicationId(connection, name, className);
            boolean insertOk = Queries.insertMeasurementApplication(connection, measurementId,
                    applicationId);
            if (!insertOk) {
                throw new SQLException("Failed to insert application with id: " + applicationId
                        + " to measurement: " + measurementId);
            }
        }
        return measurementId;
    }

    /** Find measurement by its id from the routine_db database
     * @param connection PostgreSQL database connection.
     * @param measurement_id Database id of a raw measurement row.
     * @return The raw measurement record values as a Java object (REST compliant version of the class).
     * @throws SQLException The query or DB connection failed.
     */
    public static RawMeasurement findRawMeasurementById(Connection connection, long measurement_id )
            throws SQLException {
        PreparedStatement preparedStatement = null;
        ResultSet rs = null;
        RawMeasurement measurement = null;
        try {
            String sql = "SELECT rm.measurement_id, rm.measurement_timestamp, rm.latitude, rm.longitude, rm.cell_id, " +	// Which columns to take.
      	                 "la.logger_application_name, devices.device_name " +												//
                         "FROM raw_measurements AS rm " +																	// Main table and its alias.
      	                 "INNER JOIN logger_applications AS la ON rm.logger_application_id_fk=la.logger_application_id " +	// Merge with this table matching
                         "INNER JOIN devices ON rm.device_id_fk=devices.device_id " +										// foreign key value and id.
      	                 "WHERE rm.measurement_id = ?";																		// On this condition.
            preparedStatement = connection.prepareStatement(sql);
            // preparedStatement.setString(1, String.valueOf(measurement_id) );
              // "org.postgresql.util.PSQLException: ERROR: operator does not exist: bigint = character varying"
            preparedStatement.setLong(1, measurement_id );
            rs = preparedStatement.executeQuery();
            if (rs.next()) {
                long id = rs.getLong(1);
                String time = rs.getString(2);
                double latitude = rs.getDouble(3);
                double longitude = rs.getDouble(4);
                int cell = rs.getInt(5);
                String application = rs.getString(6);
                String device = rs.getString(7);
                measurement = new RawMeasurement(id, time, latitude, longitude, cell, application, device );
            }
        } finally {
            if (rs != null) {
                try {
                    rs.close();
                } catch (Exception e) {
                }
            }
            if (preparedStatement != null) {
                try {
                    preparedStatement.close();
                } catch (Exception e) {

                }
            }
        }
        return measurement;
    }
    
    // Probably not needed (the current REST API does not allow removing any records as there is no DELETE request handling):
    /** Remove a raw measurement record from the routine_db raw_measurements table
     * @param connection routine_db PostgreSQL connection.
     * @param measurementId Value of the raw_measurements.measurement_id field in the DB record.
     * @return true, if removed, false otherwise.
     * @throws SQLException The query or DB connection failed.
     */
    public static boolean deleteRawMeasurement(Connection connection, long measurementId)
            throws SQLException {
        boolean removed = false;
        PreparedStatement preparedStatement = null;
        String sql = "DELETE FROM raw_measurements WHERE measurement_id = ?";
        try {
            preparedStatement = connection.prepareStatement(sql);
            preparedStatement.setLong(1, measurementId);
            int removedCount = preparedStatement.executeUpdate();
            if (removedCount > 0) {
                removed = true;
            }
        } finally {
            if (preparedStatement != null) {
                preparedStatement.close();
            }
        }
        return removed;
    }

    /** Get a list of all raw measurements (resolving references to other tables) from the routine database, to be returned by the
     *  .../rest/raw_measurements REST URL 
     * @param connection The PostgreSQL 9.0/9.1 routine database connection.
     * @return A list of found raw_measurements (may be empty if none was found).
     * @throws SQLException If problems arose with the query.
     */
    public static List<RawMeasurement> getAllRawMeasurements(Connection connection) throws SQLException {
        PreparedStatement preparedStatement = null;
        ResultSet rs = null;
        RawMeasurement measurement = null;
        List<RawMeasurement> measurements = new ArrayList<RawMeasurement>();
          // Return value to function and the corresponding REST service.
        try {
        	// For info about a filtered multi table PostgreSQL query, see http://www.postgresql.org/docs/9.1/static/tutorial-join.html.
            String sql = "SELECT rm.measurement_id, rm.measurement_timestamp, rm.latitude, rm.longitude, rm.cell_id, " +	// Which columns to take.
        	             "la.logger_application_name, devices.device_name " +												//
                         "FROM raw_measurements AS rm " +																	// Main table and its alias.
        	             "INNER JOIN logger_applications AS la ON rm.logger_application_id_fk=la.logger_application_id " +	// Merge with this table matching
                         "INNER JOIN devices ON rm.device_id_fk=devices.device_id";											// foreign key value and id.
            preparedStatement = connection.prepareStatement(sql);
            rs = preparedStatement.executeQuery();
            while (rs.next()) {
                long id = rs.getLong(1);
                String time = rs.getString(2);
                double latitude = rs.getDouble(3);
                double longitude = rs.getDouble(4);
                int cell = rs.getInt(5);
                String application = rs.getString(6);
                String device = rs.getString(7);
                measurement = new RawMeasurement(id, time, latitude, longitude, cell, application, device );
                measurements.add(measurement);
            }
        } finally {
            if (rs != null) {
                try {
                    rs.close();
                } catch (Exception e) {
                }
            }
            if (preparedStatement != null) {
                try {
                    preparedStatement.close();
                } catch (Exception e) {
                }
            }
        }
        return measurements;
    }
	
    // measurement_applications relation table helper methods (many measurement_applications records may belong to one raw measurement)

    /** Insert a new measurement application record into the measurement_applications table of the routine_db
     *  (this table ties many applications to a single raw measurement)
     * @param connection PostgreSQL connection.
     * @param measurementId Database id of a raw measurement row (record).
     * @param applicationId Reference to one of the application records.
     * @return true, if adding the reference was successful, false otherwise.
     * @throws SQLException The query or DB connection failed.
     */
    public static boolean insertMeasurementApplication(Connection connection, long measurementId,
            long applicationId) throws SQLException {
        PreparedStatement preparedStatement = null;
        boolean ret = false;
        try {
            String sql = "INSERT INTO measurement_applications (measurement_id_fk, application_id_fk) "
                    + "VALUES(?, ?)";
            preparedStatement = connection.prepareStatement(sql);
            preparedStatement.setLong(1, measurementId);
            preparedStatement.setLong(2, applicationId);
            int insertCount = preparedStatement.executeUpdate();
            if (insertCount > 0) {
                ret = true;
            }
        } finally {
            if (preparedStatement != null) {
                preparedStatement.close();
            }
        }
        return ret;
    }

    // routine_classes table of routine_db (DB helper methods: has, insert, delete, find, search by range, findId)

    /** Check for an existing routine class record from the routine_classes table of the routine_db data base
     * @param connection routine_db PostgreSQL connection.
     * @param routineClass Java object containing the routine class values.
     * @return true, if insertion was successful, false otherwise (also if the exact record already existed as the records assume uniqueness).
     * @throws SQLException The query or DB connection failed.
     */
    public static boolean hasRoutineClass(Connection connection, RoutineClass routineClass)
            throws SQLException {
        RoutineClass rc = findRoutineClass(connection, routineClass.getRoutineTypeId(),
                routineClass.getRoutineClassName(), routineClass.getDeviceName());
        return (rc != null);
    }

    /** Fetch details of the named routine class from the database and return a Java instance of them
     * @param connection A postgreSQL database connection to the routine_db tables.
     * @param typeId Types of routine classes desired: 0 = location routine, 1 = application routine and 2 = user defined and named (recognized)
     *               routine.
     * @param routineClassName User given name for the routine class, for example "At home".
     * @param deviceName Routine class owner device/user (hash).
     * @return Java object containing the routine class record values. Null is returned if the class is not found or some other error happens.
     * @throws SQLException The query or DB connection failed. Also thrown if the device name is not found from the routine_db.
     */
    public static RoutineClass findRoutineClass(Connection connection, int typeId,
            String routineClassName, String deviceName) throws SQLException {
        Device device = findDevice(connection, deviceName);
        if (device == null) {
            throw new SQLException("Did not find device with deviceName: " + deviceName);
        }

        PreparedStatement preparedStatement = null;
        ResultSet rs = null;
        RoutineClass rc = null;
        try {
            String sql = "SELECT id FROM routine_classes WHERE "
                    + "routine_type_id = ? AND routine_class_name = ? AND owner_device_id_fk = ?";
            preparedStatement = connection.prepareStatement(sql);
            preparedStatement.setInt(1, typeId);
            preparedStatement.setString(2, routineClassName);
            preparedStatement.setLong(3, device.getDeviceId());
            rs = preparedStatement.executeQuery();
            if (rs.next()) {
                long id = rs.getLong(1);
                rc = new RoutineClass(id, typeId, routineClassName, device.getDeviceId(),
                        device.getDeviceName());
            }
        } finally {
            if (rs != null) {
                try {
                    rs.close();
                } catch (Exception e) {
                }
            }
            if (preparedStatement != null) {
                try {
                    preparedStatement.close();
                } catch (Exception e) {
                }
            }
        }
        return rc;
    }

    /** Insert a new routine class into the routine DB
     * @param connection routine_db PostgreSQL connection.
     * @param routineClass Values to insert except for the id which is ignored.
     * @return The id of the created routine class or -1 on insertion failure.
     * @throws SQLException The query or DB connection failed. */
    public static long insertRoutineClass( Connection connection, RoutineClass routineClass )
            throws SQLException {
        PreparedStatement preparedStatement = null;
        long id = -1;
        try {
            String sql = "INSERT INTO routine_classes ( routine_type_id, routine_class_name, owner_device_id_fk) "
                    + "VALUES ( ?, ?, ?) RETURNING id";
            preparedStatement = connection.prepareStatement(sql);
            preparedStatement.setInt(1, routineClass.getRoutineTypeId());
            preparedStatement.setString(2, routineClass.getRoutineClassName());
            preparedStatement.setLong(3, routineClass.getDeviceId());
            ResultSet rs = preparedStatement.executeQuery();
            if (rs.next()) {
                id = rs.getLong(1);
            }
        } finally {
            if (preparedStatement != null) {
                preparedStatement.close();
            }
        }
        return id;
    }
    
    /** Remove a routine class record from the routine_db routine classes table
     * @param connection routine_db PostgreSQL connection.
     * @param routineClassId The id of the routine class record.
     * @return true, if removed, false otherwise.
     * @throws SQLException
     */
    public static boolean deleteRoutineClassById(Connection connection, long routineClassId)
            throws SQLException {
        boolean removed = false;
        PreparedStatement preparedStatement = null;
        String sql = "DELETE FROM routine_classes WHERE id = ?";
        try {
            preparedStatement = connection.prepareStatement(sql);
            preparedStatement.setLong(1, routineClassId);
            int removedCount = preparedStatement.executeUpdate();
            if (removedCount > 0) {
                removed = true;
            }
        } finally {
            if (preparedStatement != null) {
                preparedStatement.close();
            }
        }
        return removed;
    }
    
    /** Find routine class by its id from the routine_db database
     * @param connection PostgreSQL database connection.
     * @param routineClassId Database id of a routine class row.
     * @return A new RoutineClass object initialized with the values taken from the database.
     *         Null is returned if the record with the id wasn't found.
     * @throws SQLException If database errors take place.
     */
    public static RoutineClass findRoutineClassById(Connection connection, int routineClassId)
            throws SQLException {
        PreparedStatement preparedStatement = null;
        ResultSet rs = null;
        RoutineClass routine = null;
        try {
            String sql = "SELECT rc.id, rc.routine_type_id, rc.routine_class_name, rc.owner_device_id_fk, devices.device_name " +
            			 "FROM routine_classes AS rc " +
            			 "INNER JOIN devices ON rc.owner_device_id_fk=devices.device_id " +
            			 "WHERE rc.id = ?";
            preparedStatement = connection.prepareStatement(sql);
            preparedStatement.setInt(1, routineClassId );
            rs = preparedStatement.executeQuery();
            long id = -1;
            int type = -1;
            String className = null;
            long deviceId = -1;
            String deviceName = null;
            // Get first match for the query (1 line expected anyway):
            if (rs.next()) {
                id = rs.getLong(1);
                type = rs.getInt(2);
                className = rs.getString(3);
                deviceId = rs.getLong(4);
                deviceName = rs.getString(5); // We want to see the human readable name also for debugging and verification purposes... 
                routine = new RoutineClass(id, type, className, deviceId, deviceName );
            }
        } finally {
            if (rs != null) {
                try {
                    rs.close();
                } catch (Exception e) {
                }
            }
            if (preparedStatement != null) {
                try {
                    preparedStatement.close();
                } catch (Exception e) {

                }
            }
        }
        return routine;
    }
    
    /** Get a list of all routine classes (resolving references to other tables) from the routine database, to be returned by the
     *  .../rest/routine_classes REST URL 
     * @param connection The PostgreSQL 9.0/9.1 routine database connection.
     * @return A list of found routine classes (may be empty if none was found).
     * @throws SQLException If problems arose with the query.
     */
    public static List<RoutineClass> getAllRoutineClasses(Connection connection) throws SQLException {
        PreparedStatement preparedStatement = null;
        ResultSet rs = null;
        RoutineClass routine = null;
        List<RoutineClass> routines = new ArrayList<RoutineClass>();
          // Return value to function and the corresponding REST service.
        try {
            String sql = "SELECT rc.id, rc.routine_type_id, rc.routine_class_name, rc.owner_device_id_fk, devices.device_name " +
                         "FROM routine_classes AS rc " +
                         "INNER JOIN devices ON rc.owner_device_id_fk=devices.device_id";
            preparedStatement = connection.prepareStatement(sql);
            rs = preparedStatement.executeQuery();
            // Introduce variables out of loop to save object allocating burden from the CPU and to avoid memory fragmentation: 
            long id = -1;
            int type = -1;
            String className = null;
            long deviceId = -1;
            String deviceName = null;
            // Populate routines list:
            while (rs.next()) {
                id = rs.getLong(1);
                type = rs.getInt(2);
                className = rs.getString(3);
                deviceId = rs.getLong(4);
                deviceName = rs.getString(5); // We want to see the human readable name also for debugging and verification purposes... 
                routine = new RoutineClass(id, type, className, deviceId, deviceName );
                routines.add(routine);
            }
        } finally {
            if (rs != null) {
                try {
                    rs.close();
                } catch (Exception e) {
                }
            }
            if (preparedStatement != null) {
                try {
                    preparedStatement.close();
                } catch (Exception e) {
                }
            }
        }
        return routines;
    }
    
    // user_routines table of routine_db (DB helper methods)
    
    /** Check for an existing user routine record from the user_routines table of the routine_db data base (filtered search)
     * @param connection routine_db PostgreSQL connection.
     * ...
     * @return true, if found, false otherwise.
     * @throws SQLException The query or DB connection failed.
     */
    /* WAS NOT NEEDED:
    public static boolean hasUserRoutine(Connection connection) throws SQLException {
        throw new SQLException("hasUserRoutine");
    }
    */

    /** Get the record of an existing user routine as a Java object from the user_routines table of the routine_db data base (filtered search)
     * @param connection routine_db PostgreSQL connection.
     * ...
     * @return Java object containing the user routine record values and some additional reference fields.
     *         Null is returned if the measurement is not found or some other error happens.
     * @throws SQLException The query or DB connection failed.
     */
    /* SHOULD BE ADDED WITH QUERY PARAMETERS SUCH AS TIME PERIOD, DEVICE NAME/ID AND LOGGER APPLICATION NAME/ID:
    public static UserRoutine findUserRoutine(Connection connection) throws SQLException {
        throw new SQLException("findUserRoutine");
    }
    */

    /** Older way to insert user routine data into the database (not fully compliant with the intended routine_db use)
     * @param connection routine_db PostgreSQL connection.
     * @param deviceId Value of the device_id_fk in the DB record, pointing to devices.device_id field.
     * @param userRoutine Java object containing the user routine record and reference data values.
     * @throws SQLException The query or DB connection failed.
     */
    public static void insertUserRoutine(Connection connection, long deviceId, UserRoutine userRoutine)
            throws SQLException {
        PreparedStatement preparedStatement = null;
        try {
            // First insert all applications which do not already exist in application table
            ArrayList<fi.vtt.activitylogger.Application> apps = userRoutine.getApplication();
            for(int i = 0 ; i < apps.size(); i++) {
                // Should probably refactor this to use only the newer fi.vtt.activitylogger.Application class:
                fi.vtt.activitylogger.Application app = apps.get(i);
                Application sqlApp = new Application(app.getApplicationName(), app.getPackageClassName(), LAUNCH_TIME_NOT_USED);
                if (!hasApplication(connection, sqlApp.getName(), sqlApp.getClassName())) {
                    insertApplication(connection, sqlApp);
                }
            }
            
            String loggerApplicationName = userRoutine.getLoggerApplicationName();
            if (!hasLoggerApplication(connection, loggerApplicationName)) {
                LoggerApplication newLoggerApp = new LoggerApplication(-1, loggerApplicationName);
                insertLoggerApplication(connection, newLoggerApp);
            }
            LoggerApplication loggerApp = findLoggerApplication(connection, loggerApplicationName);
            
            // We should not need raw measurements id for user routines but this hack was needed to overcome the design flaw in the DB schema:
            RawLogData dummyRawMeasurent = new RawLogData();
            long rawMeasurementId = insertRawMeasurement(connection, deviceId, loggerApp, dummyRawMeasurent);
            
            // routine_type_id for device/user x -> routine_classes.id
            // WRONG: user_routines.user_routines_id cannot be the same as routine_clasess.id! 
            // Changed insertRoutineClass() method to return the new routine class DB id. 
            RoutineClass rc = new RoutineClass( userRoutine.getUserRoutinesId(), (int)userRoutine.getRoutineClassId(),
            		                            userRoutine.getRoutineClassName(), deviceId, userRoutine.getDeviceName() );
            if (!hasRoutineClass(connection, rc)) {
                insertRoutineClass(connection, rc);
            }
            rc = findRoutineClass(connection, (int)userRoutine.getRoutineClassId(), userRoutine.getRoutineClassName(), userRoutine.getDeviceName());
              // This method should use the id returned by insertRoutineClass(connection, rc); above... 
            
            String sql =
            		"INSERT INTO user_routines (start_time, end_time, routine_class_id_fk, application_id_fk, raw_measurement_id_fk, confidence) " +
                    "VALUES (?, ?, ?, ?, ?, ?)";
            preparedStatement = connection.prepareStatement(sql);
            // Now that all applications are in the application table, insert the same user routine applications count times (a hack):
            for(int i = 0 ; i < apps.size(); i++) {
                fi.vtt.activitylogger.Application app = apps.get(i);
                long applicationId = findApplicationId(connection, app.getApplicationName(), app.getPackageClassName());
                Timestamp startTimeStamp = java.sql.Timestamp.valueOf(userRoutine.getStartTime()); // Time zone information is lost here?
                Timestamp endTimeStamp = java.sql.Timestamp.valueOf(userRoutine.getEndTime());
                preparedStatement.setTimestamp(1, startTimeStamp);
                preparedStatement.setTimestamp(2, endTimeStamp);
                preparedStatement.setLong(3, rc.getId());
                preparedStatement.setLong(4, applicationId);
                preparedStatement.setLong(5, rawMeasurementId);
                preparedStatement.setDouble(6, userRoutine.getConfidence());
                preparedStatement.executeUpdate();
            }
        } finally {
            if (preparedStatement != null) {
                preparedStatement.close();
            }
        }
    }

    /** Test inserting a POSTed (.../rest/user_routines/test) and GSON deserialized UserRoutine JSON object into the routine database TEMPORARILY and
     *  return all user routines (after that remove the added user routine and all the references it created)
     * @param connection The PostgreSQL 9.0/9.1 routine database connection.
     * @return A list of found user routines (may be empty if none was found).
     * @throws SQLException If unexpected problems arose with the query (which then produces an internal server error HTTP 500 response).
     * <p>
     *  Referenced records in tables <i>devices<i>, <i>logger_applications<i>, <i>raw_measurements<i>, <i>routine_classes<i> and <i>application<i>
     *  are created implicitly as needed. The whole insertion takes place in a transaction block so everything is stored if data is fine but nothing
     *  is stored on error.
     * </p>
     * <p>
     *  It seems that COMMIT and ROLLBACK (or using a DB transaction in the first place) prevent the RETURNING command from returning any results!
     *  After COMMIT data is still saved into the database, though. Without transactions the insertion and return values work as expected.
     *  <b>So we cannot really implement this test method easily.</b>
     * </p>
     * <p>
     *  This method is now left unfinished. It just adds 'testInsertUserRoutine device name' to the devices table and returns that record for further
     *  processing into the user routines list (which obviously fails and makes the server return HTTP 500 Internal Server Error for the POST request).
     * </p> */
    public static List<UserRoutine> testInsertUserRoutine( Connection connection, UserRoutine userRoutine ) throws SQLException {
        PreparedStatement preparedStatement = null;
        ResultSet rs = null;
        List<UserRoutine> routines = new ArrayList<UserRoutine>();
          // Return value to function and the corresponding REST service.
        StringBuilder sb = new StringBuilder();
        String sql = null;
        /*final String returningSql = "       ur.user_routines_id, ur.start_time, ur.end_time, ur.routine_class_id_fk, " +
        					        "       ur.application_id_fk, ur.raw_measurement_id_fk, ur.confidence, " +
        					        "       a.application_name, a.package_class_name, " +
        					        "       rc.routine_class_name, " +
        					        "       rm.latitude, rm.longitude, rm.cell_id, " +
        					        "       d.device_name, la.logger_application_name " +
        					        "FROM user_routines AS ur " +
        					        "INNER JOIN application AS a ON ur.application_id_fk=a.application_id " +
        					        "INNER JOIN routine_classes AS rc ON ur.routine_class_id_fk=rc.id " +
        					        "INNER JOIN raw_measurements AS rm ON ur.raw_measurement_id_fk=rm.measurement_id " +
        					        "INNER JOIN devices AS d ON rm.device_id_fk=d.device_id " +
        					        "INNER JOIN logger_applications AS la ON rm.logger_application_id_fk=la.logger_application_id " +
        					        "ORDER BY ur.user_routines_id ASC";
          // This we use with the RETURNING statement (SELECT removed).
        final String searchingSql = "SELECT ur.user_routines_id, ur.start_time, ur.end_time, ur.routine_class_id_fk, " +
		        					"       ur.application_id_fk, ur.raw_measurement_id_fk, ur.confidence, " +
		        					"       a.application_name, a.package_class_name, " +
		        					"       rc.routine_class_name, " +
		        					"       rm.latitude, rm.longitude, rm.cell_id, " +
		        					"       d.device_name, la.logger_application_name " +
		        					"FROM user_routines AS ur " +
		        					"INNER JOIN application AS a ON ur.application_id_fk=a.application_id " +
		        					"INNER JOIN routine_classes AS rc ON ur.routine_class_id_fk=rc.id " +
		        					"INNER JOIN raw_measurements AS rm ON ur.raw_measurement_id_fk=rm.measurement_id " +
		        					"INNER JOIN devices AS d ON rm.device_id_fk=d.device_id " +
		        					"INNER JOIN logger_applications AS la ON rm.logger_application_id_fk=la.logger_application_id " +
		        					"ORDER BY ur.user_routines_id ASC";*/
          // This is the same PostgreSQL code that is used in the method getAllUserRoutines(), repeated here for reference.
        
    	// Inserting a new user routine potentionally updates up to six tables in the routine_db database (as referenced items may need to be implicitly
    	// created)! We perform this feature in a PostgreSQL transaction (BEGIN ... COMMIT) to keep the DB consistent. Each table is updated in turn
    	// below. If a conflict or impossible situation occurs, we reject the transaction with the ROLLBACK command and return some non null string
    	// to advice the user. If an SQLException takes place the transaction should be automatically rejected?
        try {
        	// General form of our query:
        	// "BEGIN; INSERT INTO table ( column1 , column2 , ... ) VALUES ( value1 , value 2 , ... ) RETURNING column1, column2, ...; ROLLBACK;"

        	// Start the database transaction with BEGIN (make sure each added piece has white space in the end):
        	// sb.append("BEGIN;\n");
        	
        	// 1) devices:
        	sb.append("INSERT INTO devices ( device_name, platform, device_creation_timestamp, mcc )\n");
        	sb.append("VALUES ( 'testInsertUserRoutine device name' , 'platform name' , '2012-11-18 21:40:09.012+02' , 346 )\n"); // Test.
        	  // 346;"KY ";"Cayman Islands (UK)".
        	// sb.append(";\n");			// Normal case.
        	sb.append("RETURNING *;\n");	// Debug.
        	
        	// 2) logger_applications
        	sb.append("");
        	
        	// 3) raw_measurements
        	sb.append("");
        	
        	// 4) routine_classes
        	sb.append("");
        	
        	// 5) application
        	sb.append("");
        	
        	// 6) user_routines
        	sb.append("");
        	
        	// After adding everything, get the current situation (list of user routines) as a return value for INSERT:
        	// sb.append( "RETURNING\n" + returningSql + ";\n" );
        	// We cannot just call getAllUserRoutines() because that would take place in a different database session that cannot see the incomplete
        	// transaction? CONNECTION IS A SESSION SO THAT MIGHT WORK AFTER ALL. BUT IT MIGHT BE ALSO BE A WASTE OF EFFORT.
        	
        	// COMMIT the changes if everything is fine (WE ARE ONLY TESTING THE INSERTING HERE):
        	// sb.append("COMMIT; ");
        	// ROLLBACK the changes if we don't want to change the database after all:
        	// sb.append("ROLLBACK; ");
        	// It seems that COMMIT and ROLLBACK (or using a DB transaction in the first place) prevent the RETURNING command from returning any
        	// results!
        	// After COMMIT data is still saved into the database, though. Without transactions the insertion and return values work as expected.
        	
        	// Execute the created SQL:
        	sql = sb.toString();
        	System.out.println( "\n[Queries.testInsertUserRoutine()] Query sql:\n" + sql ); // Debug.
            preparedStatement = connection.prepareStatement(sql);
            rs = preparedStatement.executeQuery();
            
            /* Debug examples from the console output:
            
			http://127.0.0.1:8080/ActivityLogger/rest/user_routines/test POST => HTTP 500 Internal Server Error
			DATABASE ERROR - Could not save the new user routine (bad JSON data values?):
			No results were returned by the query.
			->
			[Queries.testInsertUserRoutine()] Query sql:
			BEGIN;
			INSERT INTO devices ( device_name, platform, device_creation_timestamp, mcc )
			VALUES ( 'testInsertUserRoutine device name' , 'platform name' , '2012-11-18 21:40:09.012+02' , 346 )
			RETURNING *;
			ROLLBACK; 
			org.postgresql.util.PSQLException: No results were returned by the query.
			
			http://127.0.0.1:8080/ActivityLogger/rest/user_routines/test POST => HTTP 500 Internal Server Error
			DATABASE ERROR - Could not save the new user routine (bad JSON data values?):
			Bad value for type long : 2012-11-18 21:40:09.012+02
			->
			[Queries.testInsertUserRoutine()] Query sql:
			INSERT INTO devices ( device_name, platform, device_creation_timestamp, mcc )
			VALUES ( 'testInsertUserRoutine device name' , 'platform name' , '2012-11-18 21:40:09.012+02' , 346 )
			RETURNING *;
			Processing DB result set: org.apache.tomcat.dbcp.dbcp.DelegatingResultSet@8277c9
			 (first item = '10'.
			Processing user routine record 10...
			org.postgresql.util.PSQLException: Bad value for type long : 2012-11-18 21:40:09.012+02
			
            */
        	
        	// Construct the response array (same code as in getAllUserRoutines()
        	
            // Introduce working variables. Array index 0 contains the previous value and 1 the current value?:
            int ci = 1; // Current index. Which index, 0 or 1, denotes the current data in the tables below. Is inverted on each line read from the DB.
            int pi = 0; // Previous index.
            long  userRoutinesId = -1;
            String [] startTime = new String[2]; // Store format: PostgreSQL "'2012-08-16 16:28:56.583+03'", not ISO-8601 "2012-08-16T13:28:56.583+00".
            String [] endTime = new String[2];   //
            long [] routineClassId = new long [2];
            long applicationId; 		// Put these values into ArrayList<Application>.
            String applicationName;		//
            String packageClassName;	//
            long [] rawMeasurementId = new long [] {0L,0L};
            double [] confidence = { 0.0 , 0.0 };
            ArrayList<fi.vtt.activitylogger.Application> applications = null; // Initialized in the loop instead, as needed.
              // = new ArrayList<fi.vtt.activitylogger.Application>(); // (id,name,package) x n.
            String [] routineClassName = new String [] {null,null};
            double [] latitude = new double [] {-1,-1};
            double [] longitude = {-1.0,-1.0};
            int [] cellId = new int [] {-1,-1};
            String [] deviceName = new String [2];
            String [] loggerApplicationName = new String [2];

            // Process found rows. Application list is appended an entry when other fields are constant except
            // user_routines_id and application_id_fk (application_name or package_class_name may have an old value in some cases):
            while (rs.next()) {
            	System.out.println( "\nProcessing DB result set: " + rs.toString() + "\n (first item = '" + rs.getString(1) + "'.\n" ); // Debug.
            	System.out.println( "Processing user routine record " + rs.getLong(1) + "..." );
            	// Switch indices for current and previous values (faster than copying values):
            	ci = (ci==1)?0:1;
            	pi = (ci==1)?0:1;
            	// Read new values to variables from the next row (read in the order of the result values, differs from variable introduction):
              	userRoutinesId = rs.getLong(1); // No need to save and compare current to previous value, they are always different.
              	startTime[ci] = rs.getString(2);
              	endTime[ci] = rs.getString(3);
              	routineClassId[ci] = rs.getLong(4);
              	applicationId = rs.getLong(5); // Will be different on every result row.
              	rawMeasurementId[ci] = rs.getLong(6);
              	confidence[ci] = rs.getDouble(7);
              	applicationName = rs.getString(8);
              	packageClassName = rs.getString(9);
              	routineClassName[ci] = rs.getString(10);
              	latitude[ci] = rs.getDouble(11);
              	longitude[ci] = rs.getDouble(12);
              	cellId[ci] = rs.getInt(13);
              	deviceName[ci] = rs.getString(14);
              	loggerApplicationName[ci] = rs.getString(15);
            	
            	// Check if we need to add an application entry to an existing routine (the ArrayList<Application> should already exist):
            	if ( startTime[ci].equals(startTime[pi]) && endTime[ci].equals(endTime[pi]) && routineClassId[ci]==routineClassId[pi] &&
            		 rawMeasurementId[ci]==rawMeasurementId[pi] && confidence[ci]==confidence[pi] && routineClassName[ci].equals(routineClassName[pi]) &&
            		 latitude[ci]==latitude[pi] && longitude[ci]==longitude[pi] && cellId[ci]==cellId[pi] && deviceName[ci].equals(deviceName[pi]) &&
            		 loggerApplicationName[pi].equals(loggerApplicationName[pi]) &&
            		 applications != null &&
            		 applicationId > 1 ) { // Application reference with id 1 means that the routine has no application usage data ("No applications").
            		System.out.println("Adding application to existing user routine list: applicationId = " + applicationId );
            		applications.add( new fi.vtt.activitylogger.Application(applicationId,applicationName,packageClassName) );
            	} else {
            		// Append as a new routine instead:
            		System.out.println("Creating a new user routine: userRoutinesId = " + userRoutinesId );
            		applications = new ArrayList<fi.vtt.activitylogger.Application>(); // Create a new ArrayList for the new routine.
            		if (applicationId>1) // Add application reference in the current routine entry, if it is a valid application record.
            			applications.add( new fi.vtt.activitylogger.Application(applicationId,applicationName,packageClassName) );
            		routines.add(
            			new UserRoutine( userRoutinesId, startTime[ci], endTime[ci], routineClassId[ci], rawMeasurementId[ci] ,
            					         confidence[ci], applications,
            					         -1 /*routineTypeId*/ ,
            					         routineClassName[ci], latitude[ci], longitude[ci], cellId[ci], deviceName[ci], loggerApplicationName[ci] ) );
                  // public UserRoutine( long userRoutinesId, String startTime, String endTime, long routineClassId, /*long applicationId,*/ double confidence,
       		      //         ArrayList<Application> applications, String routineClassName, double latitude, double longitude, int cellId,
    		      //         String deviceName, String loggerApplicationName ) {
            	}
            }
        	
        } finally {
        	// Clean up:
            if (rs != null) {
                try {
                    rs.close();
                } catch (Exception e) {}
            }
            if (preparedStatement != null) {
                try {
                    preparedStatement.close();
                } catch (Exception e) {}
            }
        }

        return routines;
    }

    /** Insert a POSTed (.../rest/user_routines) and GSON deserialized UserRoutine JSON object into the routine database
     * @param connection The PostgreSQL 9.0/9.1 routine database connection.
     * @param userRoutine Details of the user routine to add. Field <em>userRoutinesId</em> is updated to contain the actual DB id value when method
     *                    returns.
     * @return null if everything went well, text to describe problems (that can be shown to the end user) if not.
     * @throws SQLException If unexpected problems arose with the query (which produces an internal server error HTTP 500 response).
     * <p>This method replaces the earlier insertUserRoutine() above.</p>
     * <p>
     *  Referenced records in tables <i>devices<i>, <i>logger_applications<i>, <i>raw_measurements<i>, <i>routine_classes<i> and <i>application<i>
     *  are created implicitly as needed. The whole insertion takes place in a transaction block so everything is stored if data is fine but nothing
     *  is stored on error.
     * </p>
     * <p>
     *  <b>Note:</b> This method calls other database manipulation methods in the <em>Queries</em> class from inside a transaction block.
     *  Therefore those other methods cannot use transactions themselves!
     * </p> */
    public static String insertUserRoutine( Connection connection, UserRoutine userRoutine ) throws SQLException {

    	String errorResponse = ""; // Not sure if null value works with the operator += .
        // StringBuilder sb = new StringBuilder(); // Faster than StringBuffer but not thread safe. Same API.
        boolean transactionOk = true; // true => COMMIT, false => ROLLBACK.
        String sql = null;
        PreparedStatement preparedStatement = null;
        ResultSet rs = null;
        // Database references (others are included in the UserRoutine class):
        Device device = null;
        LoggerApplication loggerApplication = null;
        String loggerApplicationName = null;
        long routineClassid = -1;

    	// Inserting a new user routine potentionally updates up to six tables in the routine_db database (as referenced items may need to be implicitly
    	// created)! We perform this feature in a PostgreSQL transaction (BEGIN ... COMMIT) to keep the DB consistent. Each table is updated in turn
    	// below. If a conflict or impossible situation occurs, we reject the transaction with the ROLLBACK command and return some non null string
    	// to advice the user. If an SQLException takes place the transaction should be automatically rejected.
        try {
        	// General form of our query:
        	// "BEGIN; INSERT INTO table ( column1 , column2 , ... ) VALUES ( value1 , value 2 , ... ) RETURNING column1, column2, ...; COMMIT/ROLLBACK;"

        	// Start the database transaction with BEGIN (make sure that for compound SQL in sb each added piece has white space in the end):
        	// sb.append("BEGIN;\n");
            preparedStatement = connection.prepareStatement("BEGIN;");
            preparedStatement.execute(); // Ignore the boolean return value (false) as we don't use the (empty) result. May throw SQLException.
            
        	// 1) devices
            
        	// Try finding the specified device and if it does not exist in the database, create it:
        	System.out.print( "\n[Queries.insertUserRoutine()] Finding or creating a device with the name: '" +
        			          userRoutine.getDeviceName() + "'.." ); // Debug.
            try {
				ReceiversCommon.findOrCreateDevice( userRoutine.getDeviceName() , connection ); // Method calls back to this class for DB manipulation.
			} catch (Exception e) {
            	transactionOk = false;
            	errorResponse += e.getMessage(); // Should be non null.
			}
            device = Queries.findDevice( connection, userRoutine.getDeviceName() ); // Get the full instance (although we only need the device_id value).
            // Check the result although the method call is more likely to throw an SQLException on any problems:
            if (device==null) {
            	transactionOk = false;
            	errorResponse += "The device name '" + userRoutine.getDeviceName() + "' was not found from the database!\n";
            }
        	System.out.println( "\n .. Device part done." ); // Debug.
            	
            /*
        	sb.append("INSERT INTO devices ( device_name, platform, device_creation_timestamp, mcc )\n");
        	sb.append("VALUES ( 'testInsertUserRoutine device name' , 'platform name' , '2012-11-18 21:40:09.012+02' , 346 )\n"); // Test.
        	  // 346;"KY ";"Cayman Islands (UK)".
        	// sb.append(";\n");			// Normal case.
        	sb.append("RETURNING *;\n");	// Debug.
        	*/
            
        	// 2) logger_applications

        	// Try finding the specified logger application and if it does not exist in the database, create it:
        	System.out.print( "\n[Queries.insertUserRoutine()] Finding or creating a logger application with the name: '" +
        			          userRoutine.getLoggerApplicationName() + "'.." ); // Debug.
        	loggerApplicationName = userRoutine.getLoggerApplicationName();
            if (!hasLoggerApplication(connection, loggerApplicationName)) {
                LoggerApplication newLoggerApp = new LoggerApplication(-1, loggerApplicationName);
                insertLoggerApplication(connection, newLoggerApp);
            }
            loggerApplication = findLoggerApplication(connection, loggerApplicationName);
            // Check the result although the method call is more likely to throw an SQLException on any problems:
            if (loggerApplication==null) {
            	transactionOk = false;
            	errorResponse += "The logger application name '" + loggerApplicationName + "' was not found from the database!";
            }
        	System.out.println( "\n .. Logger application part done." ); // Debug.
        	
        	// 3) raw_measurements

        	// Re-use insertBasicRawData() method although it uses fi.vtt.routinelib.RawLogData class instead of REST serve specific class
        	// fi.vtt.acitvitylogger.RawMeasurement. We don't need to add applications here so calling parent method
        	// insertRawMeasurement() is not needed.
        	System.out.print( "\n[Queries.insertUserRoutine()] Creating a raw measurement to store metadata for the user routine..." ); // Debug.
            RawLogData rawLogData = new RawLogData(); // Default values.
            rawLogData.setCellId( userRoutine.getCellId() );
            rawLogData.setGpsData( new GPSData( -1L , userRoutine.getLatitude() , userRoutine.getLongitude() ) );
            long rawMeasurementId = insertBasicRawData(connection, device.getDeviceId(), loggerApplication, rawLogData );
            userRoutine.setRawMeasurementId(rawMeasurementId); // Save just in case it is needed for the the REST service response.
        	System.out.println( "\n .. Raw measurement part done. measurement_id=" + rawMeasurementId ); // Debug.
        	
        	// 4) routine_classes
        	        	
        	System.out.print( "\n[Queries.insertUserRoutine()] Finding or creating a routine class with routine type " +
        	                  userRoutine.getRoutineTypeId() +
        			          ", routine class name '" + userRoutine.getRoutineClassName() +
        					  "' and owner device '" + userRoutine.getDeviceName() + "'..." ); // Debug.
            RoutineClass routineClass = new RoutineClass( -1 , userRoutine.getRoutineTypeId() , userRoutine.getRoutineClassName() ,
            		                                      device.getDeviceId() , device.getDeviceName() );
            // Try to find existing user routine class type: // 0=location, 1=application, 2=combination routine type (informative classification).
            RoutineClass routineClassInDB = findRoutineClass(
            		connection, userRoutine.getRoutineTypeId(), userRoutine.getRoutineClassName(), device.getDeviceName() ); 
            if ( routineClassInDB==null ) {
            	// Create a new routine class:
            	// routineClass.setId( insertRoutineClass(connection, routineClass) ); // Returns the DB id or -1 on error.
            	/* => (I managed to insert a routine class with id -1 earlier!)
            	   DATABASE ERROR - Could not save the new user routine (bad JSON data values?):
            	   ERROR: duplicate key value violates unique constraint "routine_classes_pkey"
            	     Detail: Key (id)=(1) already exists.*/
            	routineClassid = insertRoutineClass(connection, routineClass); // Returns the DB id or -1 on error.
            	routineClass.setId(routineClassid);
            	if (  routineClass.getId() == -1 ) {
            		transactionOk = false; // Failed to insert user routine class.
            		errorResponse += "Could not create a new routine class!\n"; // Append to possible other error messages.
            	}
            } else {
            	routineClass.setId( routineClassInDB.getId() ); // Copy id for printing out.
            }
            userRoutine.setRoutineClassId(routineClass.getId()); // Save just in case it is needed for the the REST service response.
        	System.out.println( "\n .. Routine class part done. id=" + routineClass.getId() );
        	
        	// 5) application

        	System.out.println( "\n[Queries.insertUserRoutine()] Creating entries for all used applications..." ); // Debug.
        	  // We will create this many entries to both application and user_routines tables.
        	// Add each application entry to the database and save its id for later (note that the DB enforces unique entries!):
            ArrayList<fi.vtt.activitylogger.Application> applications = userRoutine.getApplication();
        	long [] applicationIndeces = null;
            if (applications!= null && applications.size() > 0 ) {
	        	applicationIndeces = new long [ applications.size() ];
	        	fi.vtt.activitylogger.Application application = null;
	            for( int i = 0 ; i < applications.size(); i++ ) {
	                application = applications.get(i);
	                // First try to find an already existing id for the application description:
	                try {
	                    sql = "SELECT application_id FROM application WHERE application_name = ? AND package_class_name = ?";
	                    preparedStatement = connection.prepareStatement(sql);
	                    preparedStatement.setString(1, application.getApplicationName());
	                    preparedStatement.setString(2, application.getPackageClassName());
	                    rs = preparedStatement.executeQuery();
	                    if (rs.next()) {
	                        applicationIndeces[i] = rs.getLong(1);
	                    	System.out.println( "[Queries.insertUserRoutine()] Found application DB entry: " +
	                    			          applicationIndeces[i] + " '" + application.getApplicationName() + "' '" +
	                    			          application.getPackageClassName() + "'" ); // Debug.
	                    	application.setApplicationId(applicationIndeces[i]); // Save just in case it is needed for the the REST service response.
	                    	continue; // Jump over the remaining code and advance to the next application.
	                    }
	                } finally {
	                	// Overkill clean-up?
	                    if (rs != null) {
	                        try {
	                            rs.close();
	                        } catch (Exception e) {}
	                    }
	                    if (preparedStatement != null) {
	                        try {
	                            preparedStatement.close();
	                        } catch (Exception e) {}
	                    }
	                }
	                // No existing entry was found. Create a new entry:
	                try {
	                    sql = "INSERT INTO application ( application_name, package_class_name ) VALUES(?, ?) RETURNING application_id";
	                    preparedStatement = connection.prepareStatement(sql);
	                    preparedStatement.setString(1, application.getApplicationName());
	                    preparedStatement.setString(2, application.getPackageClassName());
	                    rs = preparedStatement.executeQuery();
	                    if (rs.next()) {
	                        applicationIndeces[i] = rs.getLong(1);
	                    	System.out.println( "[Queries.insertUserRoutine()] Created application DB entry: " +
	                    			          applicationIndeces[i] + " '" + application.getApplicationName() + "' '" +
	                    			          application.getPackageClassName() + "'" ); // Debug.
	                    	application.setApplicationId(applicationIndeces[i]); // Save just in case it is needed for the the REST service response.
	                    }
	                } finally {
	                    if (preparedStatement != null) {
	                        preparedStatement.close();
	                    }
	                }
	            }
            } else {
	        	applicationIndeces = new long [1];
	        	applicationIndeces[0] = 1; // Default value meaning no applications.
            }
            System.out.println( "\n .. Applications part done." );
        	
        	// 6) user_routines

        	System.out.println( "\n[Queries.insertUserRoutine()] Finally creating entries for user routines..." ); // Debug.
            sql = "INSERT INTO user_routines ( start_time, end_time, routine_class_id_fk, application_id_fk, raw_measurement_id_fk, confidence )\n" +
                    "VALUES ( ?::timestamp(3) with time zone, ?::timestamp(3) with time zone, ?, ?, ?, ? )\n" +
              	  "RETURNING user_routines_id";
            // Now that all applications are in the application table we can insert the same user routine for each application
            // (it must be done this way due to DB restrictions):
            // for( int i = 0 ; i < applicationIndeces.length; i++ ) // WE NEED TO RUN THE BLOCK AT LEAST ONCE EVEN IF THERE ARE NO APPLICATIONS.
           preparedStatement = connection.prepareStatement(sql);
           System.out.println( "\n[Queries.insertUserRoutine()] preparedStatement = \n" + preparedStatement.toString() ); // Debug.
        	long [] userRoutineIndices = new long [ applicationIndeces.length ];
        	int i = 0 ;
        	try {
        		do {
        			// About timestamps:
        			// SimpleDateFormat class timestamp patterns (but it does not create any useful timestamp or date object!):
        			// "yyyy-MM-dd'T'HH:mm:ss.SSSZ"  	2001-07-04T12:08:56.235-0700
        			// "yyyy-MM-dd'T'HH:mm:ss.SSSXXX" 	2001-07-04T12:08:56.235-07:00
	                // Timestamp startTimeStamp = java.sql.Timestamp.valueOf(userRoutine.getStartTime());
	                // Timestamp startTimeStamp = java.sql.Timestamp.valueOf("2012-11-28 13:00:00");
                    // Does not support timezones: java.lang.NumberFormatException: For input string: "00+02"

	                // preparedStatement.setTimestamp(1, startTimeStamp);
	            	preparedStatement.setString( 1, userRoutine.getStartTime() );
	                // Timestamp endTimeStamp = java.sql.Timestamp.valueOf(userRoutine.getEndTime());
	                // Timestamp endTimeStamp = java.sql.Timestamp.valueOf("2012-11-28 21:54:28");
	                // preparedStatement.setTimestamp(2, endTimeStamp);
	            	preparedStatement.setString( 2, userRoutine.getEndTime() );
	            	preparedStatement.setLong( 3, routineClass.getId() );
	                preparedStatement.setLong( 4, applicationIndeces[i] );
	                preparedStatement.setLong( 5, rawMeasurementId );
	                preparedStatement.setDouble( 6, userRoutine.getConfidence() );
	            	System.out.println( "\n[Queries.insertUserRoutine()] preparedStatement = \n" + preparedStatement.toString() ); // Debug.

	            	// preparedStatement.executeUpdate(); // CAUSES DB EXCEPTIONS IN THIS CONTEXT.
                    rs = preparedStatement.executeQuery();
        			/* Using preparedStatement.executeUpdate() instead of preparedStatement.executeQuery() produced error:
					[Queries.insertUserRoutine()] preparedStatement = 
					INSERT INTO user_routines ( start_time, end_time, routine_class_id_fk, application_id_fk, raw_measurement_id_fk, confidence )
					VALUES ( '2012-11-28 13:00:00.000000 +02:00:00', '2012-11-28 21:54:28.000000 +02:00:00', '19', '255', '18120', '1.0' )
					RETURNING user_routines_id
					org.postgresql.util.PSQLException: A result was returned when none was expected.        			 */

                    if (rs.next()) {
	                    userRoutineIndices[i] = rs.getLong(1);
	                	System.out.println( "[Queries.insertUserRoutine()] user_routines_id=" + userRoutineIndices[i] ); // Debug.
	                }
	            	i++;
	            } while ( i < applicationIndeces.length );
        		
            } finally {
            	// Overkill clean-up?
                if (rs != null) {
                    try {
                        rs.close();
                    } catch (Exception e) {}
                }
                if (preparedStatement != null) {
                    try {
                        preparedStatement.close();
                    } catch (Exception e) {}
                }
            }
            //  Save the first actual userRoutinesId from database statement to the userRoutine argument for returning the id to the user:
            userRoutine.setUserRoutinesId( userRoutineIndices[0] ); // The first one must be returned because it is the starting point in searching.
        	System.out.println( "\n .. User routines done!" );      // references to the application table.
        	
        	// Execute the created SQL:
        	/* Not needed for now.
        	sql = sb.toString();
        	System.out.println( "\n[Queries.testInsertUserRoutine()] Query sql:\n" + sql ); // Debug.
            preparedStatement = connection.prepareStatement(sql);
            rs = preparedStatement.executeQuery();
            */
            
            // Final judgment:
      	    System.out.println( "\nEnding the transaction.. " );
            if (transactionOk) {
            	// Save everything permanently into the database:
                preparedStatement = connection.prepareStatement("COMMIT;");
                preparedStatement.execute();
                errorResponse = null;
          	    System.out.println( "COMMIT done!" );
            } else {
            	// Undo everything:
                preparedStatement = connection.prepareStatement("ROLLBACK;");
                preparedStatement.execute();
                errorResponse += "\nTransaction cancelled: User routine was not saved."; // Append to any already existing explanation.
          	    System.out.println( "ROLLBACK done!" );
            }
        	
        } finally {
        	// Clean up:
            if (rs != null) {
                try {
                    rs.close();
                } catch (Exception e) {}
            }
            if (preparedStatement != null) {
                try {
                    preparedStatement.close();
                } catch (Exception e) {}
            }
        }

        return errorResponse; // null means everything went fine => HTTP 201 Created is returned with Location header set to new URL.
    }
    
    /** Remove a user routine record from the routine_db user_routines table
     * @param connection routine_db PostgreSQL connection.
     * @param userRoutinesId Value of the user_routines.user_routines_id field in the DB record.
     * @return true, if removed, false otherwise.
     * @throws SQLException The query or DB connection failed.
     */
    public static boolean deleteUserRoutine(Connection connection, long userRoutinesId)
            throws SQLException {
        boolean removed = false;
        PreparedStatement preparedStatement = null;
        String sql = "DELETE FROM user_routines WHERE user_routines_id = ?";
        try {
            preparedStatement = connection.prepareStatement(sql);
            preparedStatement.setLong(1, userRoutinesId);
            int removedCount = preparedStatement.executeUpdate();
            if (removedCount > 0) {
                removed = true;
            }
        } finally {
            if (preparedStatement != null) {
                preparedStatement.close();
            }
        }
        return removed;
    }
    
    /** Find user routine by its class id from the routine_db database
     * @param connection PostgreSQL database connection.
     * @param routineClassId Integer value for one user routine class. */
    /* Unused method, don't want to fix:
    public static UserRoutine findUserRoutineByClass(Connection connection, int routineClassId )
            throws SQLException {
        PreparedStatement preparedStatement = null;
        ResultSet rs = null;
        UserRoutine routine = null;
        try {
            String sql = "SELECT * " +									// Which columns to take (all).
                         "FROM user_routines AS ur " +					// From which table.
                         "INNER JOIN routine_classes.routine_class_name ON ur.routine_class_id_fk=routine_classes.id" +
      	                 "WHERE user_routines.routine_class_id_fk = ?";	// On this condition.
            preparedStatement = connection.prepareStatement(sql);
            // preparedStatement.setString(1, String.valueOf(measurement_id) );
              // "org.postgresql.util.PSQLException: ERROR: operator does not exist: bigint = character varying"
            preparedStatement.setInt(1, routineClassId );
            rs = preparedStatement.executeQuery();
            if (rs.next()) {
                long id = rs.getLong(1);
                String start = rs.getString(2);
                String end = rs.getString(3);
                int routineClass = rs.getLong(4);
                String data = rs.getString(5);
                double confidence = rs.getDouble(6);
                routine = new UserRoutine(id, start, end, routineClass, data, confidence );
            }
        } finally {
            if (rs != null) {
                try {
                    rs.close();
                } catch (Exception e) {
                }
            }
            if (preparedStatement != null) {
                try {
                    preparedStatement.close();
                } catch (Exception e) {

                }
            }
        }
        return routine;
    }
    */

    /** Find a (momentary) user routine record by its id from the routine_db database with full context data from other DB tables
     * @param connection PostgreSQL database connection.
     * @param userRoutineId Long value for index of one user routine entry.
     * @return A new UserRoutine object initialized with the values taken from the database. Null is returned if the name was not found.
     * @throws SQLException If database errors take place.
     * <p>
     *   Note that for the full context data, we may need to read some records following the requested index in order to construct the application
     *   use list properly.
     * </p>
     */
    public static UserRoutine findUserRoutineById(Connection connection, long userRoutineId )
            throws SQLException {
        PreparedStatement preparedStatement = null;
        ResultSet rs = null;
        UserRoutine routine = null;        // Return value to function and the corresponding REST service.
      try {
      	// Monster sized user routine query (from six tables at the same time to get all context information together):
          String sql = "SELECT ur.user_routines_id, ur.start_time, ur.end_time, ur.routine_class_id_fk, " +
          			   "       ur.application_id_fk, ur.raw_measurement_id_fk, ur.confidence, " +
          		       "       a.application_name, a.package_class_name, " +
          			   "       rc.routine_type_id, rc.routine_class_name, " +	// rc.routine_type_id added here to help adding new routine types.
          		       "       rm.latitude, rm.longitude, rm.cell_id, " +
          			   "       d.device_name, la.logger_application_name " +
                       "FROM user_routines AS ur " +
          			   "INNER JOIN application AS a ON ur.application_id_fk=a.application_id " +
                       "INNER JOIN routine_classes AS rc ON ur.routine_class_id_fk=rc.id " +
                       "INNER JOIN raw_measurements AS rm ON ur.raw_measurement_id_fk=rm.measurement_id " +
          			   "INNER JOIN devices AS d ON rm.device_id_fk=d.device_id " +
                       "INNER JOIN logger_applications AS la ON rm.logger_application_id_fk=la.logger_application_id " +
                       "WHERE ur.user_routines_id >= ? " +	// Get all rows from this onwards (we don't know how many applications there are listed).
          			   "ORDER BY ur.user_routines_id ASC";
          /* Results from the test data set with id=3 (3 rows, the first two we need to process):
			3;"2012-09-01 00:00:00+03";"2012-09-30 23:59:59+03";3;2;5;1;"Sovelluksen nimi 1";"fi.vtt.testing.a";"Sovellusrutiini 1";
			 60.7617;23.4843;30;"Käyttäjä tai laite 1";"Samsung_Galaxy_S_log"
			4;"2012-09-01 00:00:00+03";"2012-09-30 23:59:59+03";3;3;5;1;"Sovelluksen nimi 2";"fi.vtt.testing.b";"Sovellusrutiini 1";
			 60.7617;23.4843;30;"Käyttäjä tai laite 1";"Samsung_Galaxy_S_log"
			5;"2012-10-04 10:00:00+03";"2012-10-05 07:30:00+03";4;4;6;0.85;"RoutineClient";"fi.vtt.testing.routine_client";"Yhdustelmärutiini kotona";
			 61.31835;24.39843;40;"a619ed32f7d0de86a002757fbf8b29f4d1ab0aae";"Samsung_Galaxy_S_log"
          */
          preparedStatement = connection.prepareStatement(sql);
          preparedStatement.setLong(1, userRoutineId ); // Add search condition to query at the first '?'.
          rs = preparedStatement.executeQuery();

          // Introduce working variables. Array index 0 contains the previous value and 1 the current value?:
          int ci = 1; // Current index. Which index, 0 or 1, denotes the current data in the tables below. Is inverted on each line read from the DB.
          int pi = 0; // Previous index.
          long  userRoutinesId = -1;
          String [] startTime = new String[2]; // Store format: PostgreSQL "'2012-08-16 16:28:56.583+03'", not ISO-8601 "2012-08-16T13:28:56.583+00".
          String [] endTime = new String[2];   //
          long [] routineClassId = new long [2];
          long applicationId; 		// Put these values into ArrayList<Application>.
          String applicationName;		//
          String packageClassName;	//
          long [] rawMeasurementId = new long [] {0L,0L};
          double [] confidence = { 0.0 , 0.0 };
          ArrayList<fi.vtt.activitylogger.Application> applications = null; // Initialized in the loop instead, as needed.
            // = new ArrayList<fi.vtt.activitylogger.Application>(); // (id,name,package) x n.
          int [] routineTypeId = new int [] { -1 , -1 }; // Added.
          String [] routineClassName = new String [] {null,null};
          double [] latitude = new double [] {-1,-1};
          double [] longitude = {-1.0,-1.0};
          int [] cellId = new int [] {-1,-1};
          String [] deviceName = new String [2];
          String [] loggerApplicationName = new String [2];

          // Process found rows. Application list is appended an entry when other fields are constant except
          // user_routines_id and application_id_fk (application_name or package_class_name may have an old value in some cases):
          while (rs.next()) {
          	// Switch indices for current and previous values (faster than copying values):
          	ci = (ci==1)?0:1;
          	pi = (ci==1)?0:1;
          	// Read new values to variables from the next row (read in the order of the result values, differs from variable introduction):
          	userRoutinesId = rs.getLong(1); // No need to save and compare current to previous value, they are always different.
          	startTime[ci] = rs.getString(2);
          	endTime[ci] = rs.getString(3);
          	routineClassId[ci] = rs.getLong(4);
          	applicationId = rs.getLong(5); // Will be different on every result row.
          	rawMeasurementId[ci] = rs.getLong(6);
          	confidence[ci] = rs.getDouble(7);
          	applicationName = rs.getString(8);
          	packageClassName = rs.getString(9);
          	routineTypeId[ci] = rs.getInt(10);	// Added.
          	routineClassName[ci] = rs.getString(11);
          	latitude[ci] = rs.getDouble(12);
          	longitude[ci] = rs.getDouble(13);
          	cellId[ci] = rs.getInt(14);
          	deviceName[ci] = rs.getString(15);
          	loggerApplicationName[ci] = rs.getString(16);
          	
          	// Check if we need to add an application entry to an existing routine (the ArrayList<Application> should already exist):
          	if ( startTime[ci].equals(startTime[pi]) && endTime[ci].equals(endTime[pi]) && routineClassId[ci]==routineClassId[pi] &&
          		 rawMeasurementId[ci]==rawMeasurementId[pi] && confidence[ci]==confidence[pi] && routineTypeId[ci]==routineTypeId[pi] &&
          		 routineClassName[ci].equals(routineClassName[pi]) &&
          		 latitude[ci]==latitude[pi] && longitude[ci]==longitude[pi] && cellId[ci]==cellId[pi] && deviceName[ci].equals(deviceName[pi]) &&
          		 loggerApplicationName[pi].equals(loggerApplicationName[pi]) &&
          		 applications != null &&
          		 applicationId > 1 ) { // Application reference with id 1 means that the routine has no application usage data ("No applications"). 
          		applications.add( new fi.vtt.activitylogger.Application(applicationId,applicationName,packageClassName) );
          	} else if (routine==null) {
          		// Create a new routine instead:
          		applications = new ArrayList<fi.vtt.activitylogger.Application>(); // Create a new ArrayList for the new routine.
          		if (applicationId>1) // Add application reference in the current routine entry, if it is a valid application record.
          			applications.add( new fi.vtt.activitylogger.Application(applicationId,applicationName,packageClassName) );
          		routine =
          			new UserRoutine( userRoutinesId, startTime[ci], endTime[ci], routineClassId[ci], rawMeasurementId[ci] ,
          					         confidence[ci], applications, routineTypeId[ci],
          					         routineClassName[ci], latitude[ci], longitude[ci], cellId[ci], deviceName[ci], loggerApplicationName[ci] );
          		// public UserRoutine( long userRoutinesId, String startTime, String endTime, long routineClassId, /*long applicationId,*/ double confidence,
          		//         ArrayList<Application> applications, String routineClassName, double latitude, double longitude, int cellId,
          		//         String deviceName, String loggerApplicationName ) {
          	} else {
          		break; // Do not read another routine.
          	}
          }
      } finally {
          if (rs != null) {
              try {
                  rs.close();
              } catch (Exception e) {}
          }
          if (preparedStatement != null) {
              try {
                  preparedStatement.close();
              } catch (Exception e) {}
          }
      }
        
      return routine;
    }
    
    /* Get a list of all user routines from the routine database, to be returned by the .../rest/user_routines
     * REST URL 
     * @param connection The PostgreSQL 9.0/9.1 routine database connection.
     * @return A list of found user routines (may be empty if none was found).
     * @throws SQLException If problems arose with the query.
     */
    public static List<UserRoutine> getAllUserRoutines(Connection connection) throws SQLException {
        PreparedStatement preparedStatement = null;
        ResultSet rs = null;
        // UserRoutine routine = null;
        List<UserRoutine> routines = new ArrayList<UserRoutine>();
          // Return value to function and the corresponding REST service.
        try {
        	// Monster sized user routine query (from six tables at the same time to get all context information together):
            String sql = "SELECT ur.user_routines_id, ur.start_time, ur.end_time, ur.routine_class_id_fk, " +
            			 "       ur.application_id_fk, ur.raw_measurement_id_fk, ur.confidence, " +
            		     "       a.application_name, a.package_class_name, " +
            			 "       rc.routine_type_id, rc.routine_class_name, " +	// rc.routine_type_id added here to help adding new routine types.
            		     "       rm.latitude, rm.longitude, rm.cell_id, " +
            			 "       d.device_name, la.logger_application_name " +
                         "FROM user_routines AS ur " +
            			 "INNER JOIN application AS a ON ur.application_id_fk=a.application_id " +
                         "INNER JOIN routine_classes AS rc ON ur.routine_class_id_fk=rc.id " +
                         "INNER JOIN raw_measurements AS rm ON ur.raw_measurement_id_fk=rm.measurement_id " +
            			 "INNER JOIN devices AS d ON rm.device_id_fk=d.device_id " +
                         "INNER JOIN logger_applications AS la ON rm.logger_application_id_fk=la.logger_application_id " +
            			 "ORDER BY ur.user_routines_id ASC";
            // Note that you cannot use aliases for tables devices and logger_applications in makeFilteredFutuDBSqlQuery() as they would mask the
            // search conditions on devices.device_name and logger_applications.logger_application_name.
            /* Results from the test data set (5 rows) but missing routine_type_id:
			1;"2012-10-02 19:00:00+03";"2012-10-03 11:00:00+03";1;1;3;0.8;"No applications";"";"Paikkarutiini 1";
			 62.519;25.945;-1;"Käyttäjä tai laite 1";"Tallennussovellus 1"
			2;"2012-10-03 12:00:00+03";"2012-10-03 16:30:00+03";2;1;4;0.5;"No applications";"";"Paikkarutiini 2";
			 -1;-1;20;"Käyttäjä tai laite 1";"Tallennussovellus 1"
			3;"2012-09-01 00:00:00+03";"2012-09-30 23:59:59+03";3;2;5;1;"Sovelluksen nimi 1";"fi.vtt.testing.a";"Sovellusrutiini 1";
			 60.7617;23.4843;30;"Käyttäjä tai laite 1";"Samsung_Galaxy_S_log"
			4;"2012-09-01 00:00:00+03";"2012-09-30 23:59:59+03";3;3;5;1;"Sovelluksen nimi 2";"fi.vtt.testing.b";"Sovellusrutiini 1";
			 60.7617;23.4843;30;"Käyttäjä tai laite 1";"Samsung_Galaxy_S_log"
			5;"2012-10-04 10:00:00+03";"2012-10-05 07:30:00+03";4;4;6;0.85;"RoutineClient";"fi.vtt.testing.routine_client";"Yhdustelmärutiini kotona";
			 61.31835;24.39843;40;"a619ed32f7d0de86a002757fbf8b29f4d1ab0aae";"Samsung_Galaxy_S_log"
            */
            preparedStatement = connection.prepareStatement(sql);
            rs = preparedStatement.executeQuery();

            // Introduce working variables. Array index 0 contains the previous value and 1 the current value?:
            int ci = 1; // Current index. Which index, 0 or 1, denotes the current data in the tables below. Is inverted on each line read from the DB.
            int pi = 0; // Previous index.
            long  userRoutinesId = -1;
            String [] startTime = new String[2]; // Store format: PostgreSQL "'2012-08-16 16:28:56.583+03'", not ISO-8601 "2012-08-16T13:28:56.583+00".
            String [] endTime = new String[2];   //
            long [] routineClassId = new long [2];
            long applicationId; 		// Put these values into ArrayList<Application>.
            String applicationName;		//
            String packageClassName;	//
            long [] rawMeasurementId = new long [] {0L,0L};
            double [] confidence = { 0.0 , 0.0 };
            ArrayList<fi.vtt.activitylogger.Application> applications = null; // Initialized in the loop instead, as needed.
              // = new ArrayList<fi.vtt.activitylogger.Application>(); // (id,name,package) x n.
            int [] routineTypeId = new int [] { -1 , -1 }; // Added.
            String [] routineClassName = new String [] {null,null};
            double [] latitude = new double [] {-1,-1};
            double [] longitude = {-1.0,-1.0};
            int [] cellId = new int [] {-1,-1};
            String [] deviceName = new String [2];
            String [] loggerApplicationName = new String [2];

            // Process found rows. Application list is appended an entry when other fields are constant except
            // user_routines_id and application_id_fk (application_name or package_class_name may have an old value in some cases):
            while (rs.next()) {
            	System.out.println( "Processing user routine record " + rs.getLong(1) + "..." );
            	// Switch indices for current and previous values (faster than copying values):
            	ci = (ci==1)?0:1;
            	pi = (ci==1)?0:1;
            	// Read new values to variables from the next row (read in the order of the result values, differs from variable introduction):
              	userRoutinesId = rs.getLong(1); // No need to save and compare current to previous value, they are always different.
              	startTime[ci] = rs.getString(2);
              	endTime[ci] = rs.getString(3);
              	routineClassId[ci] = rs.getLong(4);
              	applicationId = rs.getLong(5); // Will be different on every result row.
              	rawMeasurementId[ci] = rs.getLong(6);
              	confidence[ci] = rs.getDouble(7);
              	applicationName = rs.getString(8);
              	packageClassName = rs.getString(9);
              	routineTypeId[ci] = rs.getInt(10);	// Added.
              	routineClassName[ci] = rs.getString(11);
              	latitude[ci] = rs.getDouble(12);
              	longitude[ci] = rs.getDouble(13);
              	cellId[ci] = rs.getInt(14);
              	deviceName[ci] = rs.getString(15);
              	loggerApplicationName[ci] = rs.getString(16);
            	
            	// Check if we need to add an application entry to an existing routine (the ArrayList<Application> should already exist):
            	if ( startTime[ci].equals(startTime[pi]) && endTime[ci].equals(endTime[pi]) && routineClassId[ci]==routineClassId[pi] &&
            		 rawMeasurementId[ci]==rawMeasurementId[pi] && confidence[ci]==confidence[pi] && routineTypeId[ci]==routineTypeId[pi] &&
            		 routineClassName[ci].equals(routineClassName[pi]) &&
            		 latitude[ci]==latitude[pi] && longitude[ci]==longitude[pi] && cellId[ci]==cellId[pi] && deviceName[ci].equals(deviceName[pi]) &&
            		 loggerApplicationName[pi].equals(loggerApplicationName[pi]) &&
            		 applications != null &&
            		 applicationId > 1 ) { // Application reference with id 1 means that the routine has no application usage data ("No applications").
            		System.out.println("Adding application to existing user routine list: applicationId = " + applicationId );
            		applications.add( new fi.vtt.activitylogger.Application(applicationId,applicationName,packageClassName) );
            	} else {
            		// Append as a new routine instead:
            		System.out.println("Creating a new user routine: userRoutinesId = " + userRoutinesId );
            		applications = new ArrayList<fi.vtt.activitylogger.Application>(); // Create a new ArrayList for the new routine.
            		if (applicationId>1) // Add application reference in the current routine entry, if it is a valid application record.
            			applications.add( new fi.vtt.activitylogger.Application(applicationId,applicationName,packageClassName) );
            		routines.add(
            			new UserRoutine( userRoutinesId, startTime[ci], endTime[ci], routineClassId[ci], rawMeasurementId[ci] , // Added to constructor.
            					         confidence[ci], applications, routineTypeId[ci],
            					         routineClassName[ci], latitude[ci], longitude[ci], cellId[ci], deviceName[ci], loggerApplicationName[ci] ) );
                  // public UserRoutine( long userRoutinesId, String startTime, String endTime, long routineClassId, /*long applicationId,*/ double confidence,
       		      //         ArrayList<Application> applications, String routineClassName, double latitude, double longitude, int cellId,
    		      //         String deviceName, String loggerApplicationName ) {
            	}
            }
        } finally {
            if (rs != null) {
                try {
                    rs.close();
                } catch (Exception e) {
                }
            }
            if (preparedStatement != null) {
                try {
                    preparedStatement.close();
                } catch (Exception e) {
                }
            }
        }
        return routines;
    }
    
/* -- filter by some start/end times
104 	SELECT * FROM user_routines
105 	WHERE routine_class_id_fk
106 	  = (SELECT id FROM routine_classes WHERE owner_device_id_fk = 777 AND routine_type_id = 1)
107 	AND start_time >= '2012-09-06 00:00:00'
108 	AND end_time <= '2012-09-06 23:59:59'; */

    // Mccs (mobile country codes / location)
    
    /** Find mcc (mobile country code) details by its number from the routine_db database
     * @param connection PostgreSQL database connection.
     * @param code Integer value for one user routine class.
     * @return A new Mcc object initialized with the values taken from the database. Null is returned if the name was not found.
     * @throws SQLException If database or connection errors take place.
     */
    public static Mcc findMcc(Connection connection, int code )
            throws SQLException {
        PreparedStatement preparedStatement = null;
        ResultSet rs = null;
        Mcc mcc = null;
        try {
            String sql = "SELECT * " +			// Which columns to take (all).
                         "FROM mccs " +			// From which table.
      	                 "WHERE mccs.mcc = ?";	// On this condition.
            preparedStatement = connection.prepareStatement(sql);
            preparedStatement.setInt(1, code );
            rs = preparedStatement.executeQuery();
            if (rs.next()) {
                int m = rs.getInt(1);
                String abbr = rs.getString(2);
                String name = rs.getString(3);
                mcc = new Mcc(m, abbr, name );
            }
        } finally {
            if (rs != null) {
                try {
                    rs.close();
                } catch (Exception e) {
                }
            }
            if (preparedStatement != null) {
                try {
                    preparedStatement.close();
                } catch (Exception e) {

                }
            }
        }
        return mcc;
    }
    
    /* Get a list of all mobile country codes from the routine database, to be returned by the .../rest/mccs
     * REST URL 
     * @param connection The PostgreSQL 9.0/9.1 routine database connection.
     * @return A list of found mccs (may be empty if none was found).
     * @throws SQLException If problems arose with the query.
     */
    public static List<Mcc> getAllMccs(Connection connection) throws SQLException {
        PreparedStatement preparedStatement = null;
        ResultSet rs = null;
        Mcc mcc = null;
        List<Mcc> mccs = new ArrayList<Mcc>();
          // Return value to function and the corresponding REST service.
        try {
            String sql = "SELECT mccs.mcc, mccs.abbr, mccs.name " +	// Which columns to take (mccs.* = ok also).
                         "FROM mccs";								// From which table.
            preparedStatement = connection.prepareStatement(sql);
            rs = preparedStatement.executeQuery();
            while (rs.next()) {
                int m = rs.getInt(1);
                String abbr = rs.getString(2);
                String name = rs.getString(3);
                mcc = new Mcc(m, abbr, name );
                mccs.add(mcc);
            }
        } finally {
            if (rs != null) {
                try {
                    rs.close();
                } catch (Exception e) {
                }
            }
            if (preparedStatement != null) {
                try {
                    preparedStatement.close();
                } catch (Exception e) {
                }
            }
        }
        return mccs;
    }

    // Other methods
    
    /** A method stub for adding a new learned (user defined) routine and a corresponding routine class into the routine_db database
     * @param connection A postgreSQL database connection to the routine_db tables.
     * @param deviceId Value of the routine_classes.owner_device_id_fk in the DB record, pointing to devices.device_id field.
     * @param loggerApplication The logger application record values as a Java object (REST compliant version of the class).
     * @param routineData The user routine record values as a Java object (not the REST compliant version of the class).
     * @throws SQLException The query or DB connection failed.
     */
    public static void insertNewLearnedRoutine(Connection connection, long deviceId,
            LoggerApplication loggerApplication, RoutineData routineData) throws SQLException {
        throw new SQLException("insertNewLearnedRoutine");
    }

    /** A method stub for adding a new entry for a previously leaned (user defined) routine into the routine_db database
     * @param connection A postgreSQL database connection to the routine_db tables.
     * @param deviceId Value of the routine_classes.owner_device_id_fk in the DB record, pointing to devices.device_id field.
     * @param loggerApplication The logger application record values as a Java object (REST compliant version of the class).
     * @param routineData The user routine record values as a Java object (not the REST compliant version of the class).
     * @throws SQLException The query or DB connection failed.
     */
    public static void insertNewRecogizedRoutine(Connection connection, long deviceId,
            LoggerApplication loggerApplication, RoutineData routineData) throws SQLException {

        throw new SQLException("insertNewRecogizedRoutine");
    }
    
    // Helper functions
    
    /** Run the provided SQL sequence
     * @param connection A postgreSQL database connection to the routine_db tables.
     * @param sql SQL text (commands&data).
     * @throws SQLException The query or DB connection failed.
     */
    public static void executeSql(Connection connection, String sql) throws SQLException {
        Statement statement = null;
        try {
            statement = connection.createStatement();
            statement.execute(sql);
        } finally {
            if (statement != null) {
                statement.close();
            }
        }
    }

}
