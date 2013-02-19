
-- Add b-tree index fields to routine_db
-- usage: 
-- psql -U routine -d routine_db -f add_routine_indexes.sql

-- add needed index fields here

CREATE INDEX CONCURRENTLY device_id_fk_index ON raw_measurements(device_id_fk);

