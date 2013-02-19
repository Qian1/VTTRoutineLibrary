
-- Example routine_db queries and inserts

-- NOTE: hard coding primary key serials just for testing purposes in these sql queries !
-- This will break sequence generation, so do not use this in the production environment
-- (fix the seq generators manually):
-- SELECT setval('logger_applications_logger_application_id_seq', (SELECT MAX(logger_application_id) FROM logger_applications) + 1);
-- SELECT setval('application_application_id_seq', (SELECT MAX(application_id) FROM application) + 1);
-- SELECT setval('devices_device_id_seq', (SELECT MAX(device_id) FROM devices) + 1);
-- SELECT setval('raw_measurements_measurement_id_seq', (SELECT MAX(measurement_id) FROM raw_measurements) + 1);
-- SELECT setval('measurement_applications_measurement_applications_id_seq', (SELECT MAX(measurement_applications_id) FROM measurement_applications) + 1);
-- SELECT setval('routine_classes_id_seq', (SELECT MAX(id) FROM routine_classes) + 1);
-- SELECT setval('user_routines_user_routines_id_seq', (SELECT MAX(user_routines_id) FROM user_routines) + 1);

-- logger_applications
INSERT INTO logger_applications (logger_application_id, logger_application_name) VALUES (100, 'MyTestLoggerApp');
-- DELETE FROM logger_applications;

-- device queries
INSERT INTO devices (device_id, device_name, platform, device_creation_timestamp, mcc)
  VALUES (777, 'InsertDeviceSHA1HashHere', 'TestPlatform', now(), 244);

SELECT device_id FROM devices WHERE device_name = 'InsertDeviceSHA1HashHere';
SELECT device_id, device_name, platform, device_creation_timestamp, mcc FROM devices WHERE device_name = 'InsertDeviceSHA1HashHere';

-- DELETE FROM devices WHERE device_name = 'InsertDeviceSHA1HashHere';

-- application queries
INSERT INTO application (application_id, application_name, package_class_name) VALUES(111, 'MyApp1', 'com.foo.company');
INSERT INTO application (application_id, application_name, package_class_name) VALUES(112, 'MyApp2', 'com.foo.company');
INSERT INTO application (application_id, application_name, package_class_name) VALUES(113, 'Game1', 'com.bar.company');
INSERT INTO application (application_id, application_name, package_class_name) VALUES(114, 'Game2', 'com.bar.company');

SELECT application_id, application_name, package_class_name FROM application 
  WHERE application_name = 'MyApp1' AND package_class_name = 'com.foo.company';
  
-- DELETE FROM application;
  
-- raw_measurements
-- for device 777, MyTestLoggerApp (100) add some basic data
INSERT INTO raw_measurements (measurement_id, latitude, longitude, cell_id, logger_application_id_fk, device_id_fk) 
  VALUES (5555, 11.11, 22.22, 456, 100, 777);

-- INSERT INTO raw_measurements ( latitude, longitude, cell_id, logger_application_id_fk, device_id_fk) 
--   VALUES (1.11, 2.22, 6, 100, 777) RETURNING measurement_id;
  
-- measurement_applications
-- put some applications (Myapp1 & Game1) to this raw_measurement 5555
INSERT INTO measurement_applications (measurement_applications_id, measurement_id_fk, application_id_fk)
  VALUES(80, 5555, 111);
INSERT INTO measurement_applications (measurement_applications_id, measurement_id_fk, application_id_fk)
  VALUES(81, 5555, 113);

-- query application data belonging to measurement 5555
SELECT measurement_id_fk, application.application_id, application.application_name, application.package_class_name
  FROM measurement_applications
  JOIN application
    ON measurement_applications.application_id_fk = application.application_id
 WHERE measurement_id_fk = 5555;


SELECT * FROM logger_applications;
SELECT * FROM application;
SELECT * FROM raw_measurements;
SELECT * FROM measurement_applications;

-- DELETE FROM application;
-- DELETE FROM measurement_applications;
-- DELETE FROM raw_measurements;

 -- add routine sql query examples here!

-- creating routine classes
-- device 777 owns this routine (type 1)
INSERT INTO routine_classes (id, routine_type_id, routine_class_name, owner_device_id_fk) 
  VALUES (44, 1, 'Home', 777);

SELECT * FROM routine_classes;

-- add a one routine for device 777, routine_class_id 44, game1 == 113
INSERT INTO user_routines (user_routines_id, start_time, end_time, routine_class_id_fk, application_id_fk, confidence) 
  VALUES(99, '2012-09-06 10:15:00', '2012-09-06 14:33:16', 44, 113, 1.0);
-- also add 'MyApp1' id 111 to device 777, routine_class_id 44
INSERT INTO user_routines (user_routines_id, start_time, end_time, routine_class_id_fk, application_id_fk, confidence) 
  VALUES(100, '2012-09-06 10:15:00', '2012-09-06 14:33:16', 44, 111, 1.0);

-- following should fail because start_time is after end_time
-- INSERT INTO user_routines (user_routines_id, start_time, end_time, routine_class_id_fk, application_id_fk, confidence) 
-- VALUES(100, '2012-09-06 17:15:00', '2012-09-06 14:33:16', 44, 111, 1.0);
  
SELECT * FROM user_routines;

-- so how to find out user's/device's routine classes
SELECT id FROM routine_classes WHERE owner_device_id_fk = 777;
SELECT id FROM routine_classes WHERE owner_device_id_fk = 777 AND routine_type_id = 1;

-- id from previous queries was for example 44
SELECT * FROM user_routines WHERE routine_class_id_fk = 44;

-- or just do that all in one line by using subquery
SELECT * FROM user_routines WHERE routine_class_id_fk = (SELECT id FROM routine_classes WHERE owner_device_id_fk = 777 AND routine_type_id = 1);

-- filter by some start/end times
SELECT * FROM user_routines 
WHERE routine_class_id_fk 
  = (SELECT id FROM routine_classes WHERE owner_device_id_fk = 777 AND routine_type_id = 1)
AND start_time >= '2012-09-06 00:00:00'
AND end_time <= '2012-09-06 23:59:59';



-- DELETE FROM routine_classes CASCADE;
-- DELETE FROM user_routines CASCADE;

