
-- create tables script, usage: 
-- psql -U routine -d routine_db -f create_routine_tables.sql

-- 
-- mobile country codes
-- 
CREATE TABLE mccs (
    mcc              integer UNIQUE NOT NULL, -- mobile country code (e.g 244)
    abbr             text    NOT NULL,        -- 2 characters abbreviation of country code (FI)
    name             text    NOT NULL         -- human readable country name (Finland)
);


-- 
-- user's device
-- 
CREATE TABLE devices (
    device_id                 bigserial PRIMARY KEY,
    device_name               text      DEFAULT 'Default user'::text NOT NULL,     -- hex presentation of device hash "0896e92e6f44d1c701cac642ca3e66ebe199caea"
    platform                  text      DEFAULT 'Default platform'::text NOT NULL, -- platform (e.g "Android 2.3")
    device_creation_timestamp timestamp with time zone DEFAULT now() NOT NULL,     -- device creation timestamp
    mcc                       integer   REFERENCES mccs(mcc)
);



-- 
-- logger application name & identifiers. (We can have multiple different logger applications
-- e.g. "RoutineLogger", "3rd party foo logger" ...)
-- 
CREATE TABLE logger_applications (
    logger_application_id   bigserial PRIMARY KEY,
    logger_application_name text      DEFAULT 'Default_routine_log'::text NOT NULL -- "mylogger", "routinelogger" ...
);


-- 
-- application (describing one application running in the device)
-- 
CREATE TABLE application (
    application_id      bigserial PRIMARY KEY,
    application_name    text      NOT NULL,        -- application name in the device "Web browser"
    package_class_name  text      NOT NULL,        -- java package and class name (company.foo.bar)
	UNIQUE (application_name, package_class_name)  -- 
);


-- 
-- raw measurements, basic data
-- 
CREATE TABLE raw_measurements (
    measurement_id           bigserial PRIMARY KEY,
    measurement_timestamp    timestamp with time zone DEFAULT now() NOT NULL,                 -- time stamp of measurement
    latitude                 double precision,                                                -- GPS latitude if available
    longitude                double precision,                                                -- GPS longitude if available
    cell_id                  integer DEFAULT (-1) NOT NULL,                                   -- default value -1 == no data 
    logger_application_id_fk bigserial REFERENCES logger_applications(logger_application_id), -- from which logger application the data is from?
    device_id_fk             bigserial REFERENCES devices(device_id)                          -- each measurement must belong to only one device
);


-- 
-- map raw application information to raw_measurements.measurement_id and to application.application_id 
-- 
CREATE TABLE measurement_applications (
    measurement_applications_id bigserial PRIMARY KEY,                                 -- 
	measurement_id_fk           bigserial REFERENCES raw_measurements(measurement_id), -- for which user_measurement does this data belong to?
    application_id_fk           bigserial REFERENCES application(application_id)       -- which application
-- Do we really need following two? removed for now
--    launch_time                 text   NOT NULL,
--    checked                     boolean DEFAULT false  
);

-- ???
-- ADD CONSTRAINT user_application_list_fk FOREIGN KEY (user_application_list) REFERENCES user_measurements(user_application_list);

-- 
-- routine_classes table & define valid enum or routine_class_id types
-- 
CREATE TABLE routine_classes (
    id                 bigserial PRIMARY KEY,
    routine_type_id    integer,                                -- 0 == location, 1 == application, 2 == user defined ?
    routine_class_name text      NOT NULL,                     -- human readable name of routine?
    owner_device_id_fk bigserial REFERENCES devices(device_id) -- because every user/device may have different class ids and class names
-- constraints, or should we constraint the routine_class_id at all? CHECK(routine_class_id >= 0 AND routine_class_id <= 2)
);


-- 
-- user's routines
-- Note that if we force application_id_fk like following, it also means that routines cannot have zero apps
-- 
CREATE TABLE user_routines (
    user_routines_id      bigserial PRIMARY KEY,
    start_time            timestamp with time zone NOT NULL,                 -- routine start time
    end_time              timestamp with time zone NOT NULL,                -- routine end time
    routine_class_id_fk   bigserial REFERENCES routine_classes(id),         -- these routine applications belong to referenced routine_class
    application_id_fk     bigserial REFERENCES application(application_id), -- which application
    raw_measurement_id_fk bigserial REFERENCES raw_measurements(measurement_id),
    confidence            double precision DEFAULT(1.0) NOT NULL,           -- range [0.0, 1.0], 1.0 == full confidence
    CHECK(confidence >= 0.0 AND confidence <= 1.0),
    CHECK(start_time < end_time)
);


