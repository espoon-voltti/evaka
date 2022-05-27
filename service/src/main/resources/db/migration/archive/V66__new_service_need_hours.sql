ALTER TABLE service_need_option ADD COLUMN daycare_hours_per_week numeric(4,2) NOT NULL;

ALTER TABLE new_service_need
    DROP CONSTRAINT new_service_need_placement_id_fkey,
    ADD CONSTRAINT new_service_need_placement_id_fkey FOREIGN KEY (placement_id) REFERENCES placement (id) ON DELETE CASCADE;
