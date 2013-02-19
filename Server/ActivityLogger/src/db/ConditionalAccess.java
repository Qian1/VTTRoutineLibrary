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
import java.sql.SQLException;

/** List common settings, constants and (static) methods used when accessing the routine_db */
public class ConditionalAccess {
	
	// Constants
	
	// Explicit user names defined in the tomcat-users.xml file (not a recommended way of controlling access since changes must be hard-coded here):
	public static final String NAME_DB_ADMINISTRATOR = "administrator";
	public static final String NAME_DB_USER = "user";

	// Security role names defined in the tomcat-users.xml file (recommended way to provide additional access right checks because is user agnostic):
	public static final String NAME_DB_ADMINISTRATOR_ROLE = "activity_logger-administrator";
	public static final String NAME_DB_USER_ROLE = "activity_logger-user";
	
	// Fields
	
	// Helper methods
	
	// ALT-SHIFT-J generates skeleton documentation comments in Eclipse:
	/** Create a valid common WHERE clause using the supplied conditions for any routine_db query (also protect against SQL injection)
     * <p>
     *  Empty or null search parameters are ignored. Timestamp formats: PostgreSQL 2012-09-06 13:13:30.913+03 and ISO 8601 2012-09-06T13:13:30.913+03.
     *  Verify strings before insertion to avoid SQL injection attacks. Timestamps are protected with timestamp casts. All strings are inserted with
     *  <em>preparedStatement.setString()</em> methods.
     * </p>
     * <p>
     *  Note that you cannot use aliases for tables devices and logger_applications in sqlQuery as they would mask the search conditions on
     *  devices.device_name and logger_applications.logger_application_name. In addition, devices and logger_applications tables must always be
     *  included in the list of tables after FROM. Always add ", devices , logger_applications " to the last entries after FROM if those tables are not
     *  already included into the query, because makeFilteredFutuDBSqlQuery() needs those tables for handling possible device_name and
     *  logger_application_name query parameters.
     * </p>
     * <p>
     *  Alternatively, you can set device_name and/or logger_application_name to null if it would not affect
     *  the query result (e.g. requesting mccs data). NOTE: having devices or logger_applications after FROM while there is no corresponding condition
     *  on devices.device_name or logger_applications.logger_application_name, causes unpredictable joins that may repeat result lines! So make query
     *  parameter null in the caller's code if the parameter does not have real use in the query.
     * </p>
     * @param connection The PostgreSQL 9.0/9.1 routine database connection.
     * @param sqlQuery The base SQL query that needs additional query restrictions.
     * @param deviceName Limit results of the search only devices belonging to this user (only returns the same instance). Null or empty give all.
     * @param loggerApplicationName Limit results of the search only to logging context of the device/user. Null or empty give all.
     * @param startTimeName Name of the DB column against which the start time is compared in SQL. Can be the same as endTimeName.
     * @param startTime PostgreSQL/ISO8601 timestamp to limit returned devices to those added at the same or newer time than the given time. 
     * @param endTimeName Name of the DB column against which the end time is compared in SQL. Can be the same as startTimeName.
     * @param endTime PostgreSQL/ISO8601 timestamp to limit returned devices to those added at the same or newer time than the given time.
	 * @return The prepared SQL statement object created from the supplied SQL string concatenated with "WHERE ..." if any of the filter parameters is not
	 *         empty and the connection is not null. Null is returned if problems arose. 
	 * @throws SQLException Indicates problems in the database query. By throwing this up, we can issue a proper HTTP 500 "Internal server error" message.
	 */
	public static PreparedStatement makeFilteredFutuDBSqlQuery(
			Connection connection , String sqlQuery , String deviceName , String loggerApplicationName ,
			String startTimeName , String startTime , String endTimeName , String endTime )
					throws SQLException {
		// Sanity test:
		if ( connection == null || sqlQuery == null || sqlQuery.length() == 0 )
			return null;
		// Introduce and initialize variables:
		StringBuilder sb = new StringBuilder(sqlQuery); // StringBuilder = Single thread but faster StringBuffer.
        PreparedStatement preparedStatement = null;
        boolean dn = false; // Is
        boolean ln = false; // the
        boolean st = false; // corresponding query
        boolean et = false; // parameter defined?
        boolean first = true; // Determines if a filter clause is first in the SQL query and should add "WHERE" (or "AND" if not) before the condition.
        int i = 1; // Index of the added parameter '?' in the prepared statement.
        // Handle SQL exceptions:
        try {
        	// Determine available query parameters:
        	dn = deviceName!= null && deviceName.length() > 0;
        	ln = loggerApplicationName!= null && loggerApplicationName.length() > 0;
        	st = startTime != null && startTime.length() > 0;
        	et = endTime!=null && endTime.length() > 0;
        	// Test if we need to use query parameters (we need, if any parameter is defined):
        	// if ( dn || ln || st || et ) {
        	//   sb.append(" WHERE ");
        	// Append parametrized SQL:
            // device_name
            if (dn) {
                sb.append(addWHEREorAND(first));
                first = false;
                sb.append("devices.device_name = ?");
            }
            // logger_application_name
            if (ln) {
                sb.append(addWHEREorAND(first));
                first = false;
                sb.append("logger_applications.logger_application_name = ?");
            }
            // start_time
            if (st) {
                sb.append(addWHEREorAND(first));
                first = false;
                sb.append( "?::timestamp(0) with time zone <= " );
                sb.append(startTimeName); 
                  // Cast "::timestamp(0) with time zone" forces the string to be parsed as a valid timestamp.
            }
            // end_time
            if (et) {
                sb.append(addWHEREorAND(first));
                first = false;
                sb.append( endTimeName );
                sb.append(" <= ?::timestamp(0) with time zone");
            }
        	// Create and populate a prepared statement (a database query object) in the same order as the SQL was constructed above:
           	preparedStatement = connection.prepareStatement(sb.toString());
           	if (dn)
           		preparedStatement.setString(i++, deviceName); // Use index i and increment it for the next possible insertion.
           	      // This method will insert the name as a string literal that cannot be escaped to cause an injection attack?
           	if (ln)
           		preparedStatement.setString(i++, loggerApplicationName);
           	if (st)
           		preparedStatement.setString(i++, startTime);
           	if (et)
           		preparedStatement.setString(i++, endTime);
           	/* Example of a complete SQL query (logger_application_name would not make sense in the context of this query):
        	String sql = "SELECT device_id, device_name, platform, device_creation_timestamp, mcc " +
    	           "FROM devices " +
    	           "WHERE device_name = ? " +
    	           "AND ?::timestamp(0) with time zone <= device_creation_timestamp " + // Cast "::timestamp(0) with time zone" forces the string to
    	           "AND device_creation_timestamp <= ?::timestamp(0) with time zone ";  // be parsed as a valid timestamp.
    	    */
        } catch ( SQLException se ) {
        	System.out.println( "[ConditionalAccess.java:makeFilteredFutuDBSqlQuery()] SQLException:\n" + se ); // Error print.
           	// Clean up:
            if (preparedStatement != null) {
                try {
                    preparedStatement.close();
                } catch (Exception e) {}
                preparedStatement = null;
            }
            throw se;
        } finally {} // Normal cleaning up is made by the caller.
		return preparedStatement;
	}

    /** Create clause join operators for filter clauses in an SQL request (for the makeFilteredFutuDBSqlQuery() method)
     * @param first true returns "WHERE" and false "AND", with suitable whitespace padding
     * @return "\\nWHERE " or "\\nAND ". */
	private static String addWHEREorAND( boolean first ) {
		return (first)?"\nWHERE ":"\nAND ";
	}
	
	/* Example query:
	SELECT * FROM devices AS dv , logger_applications
	WHERE dv.device_id > 0
	AND logger_application_name = 'Tallennussovellus 1';
	=>
	1;"123456";"Java SE";"2012-10-03 06:14:36.19+03";302;1;"Tallennussovellus 1"
	2;"Käyttäjä tai laite 1";"Maemo";"2012-10-03 14:39:02+03";346;1;"Tallennussovellus 1"
	3;"a619ed32f7d0de86a002757fbf8b29f4d1ab0aae";"iOS";"2012-08-08 14:53:24.001+03";514;1;"Tallennussovellus 1"
	
	Another query:
	SELECT device_id FROM devices AS dv , logger_applications
	WHERE dv.device_id > 0
	AND logger_application_name = 'Tallennussovellus 1';
	=>
	device_id (bigint)
	1
	2
	3
	
	Query accidentally doubling the results (implicit join?):
	SELECT device_id, device_name, platform, device_creation_timestamp, mcc
	 FROM devices , logger_applications 
	WHERE '2012-08-08T17:54:15.000Z'::timestamp(0) with time zone <= device_creation_timestamp
	AND device_creation_timestamp <= '2012-10-03T12:00:00.0+03'::timestamp(0) with time zone
	=>
	1;"123456";"Java SE";"2012-10-03 06:14:36.19+03";302
	1;"123456";"Java SE";"2012-10-03 06:14:36.19+03";302
	*/

}
