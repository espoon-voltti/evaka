ALTER TABLE new_service_need DROP CONSTRAINT new_service_need_no_overlap;
ALTER TABLE new_service_need
    ADD CONSTRAINT new_service_need_no_overlap EXCLUDE USING gist (placement_id WITH =, daterange(start_date, end_date, '[]'::text) WITH &&) DEFERRABLE INITIALLY DEFERRED;
