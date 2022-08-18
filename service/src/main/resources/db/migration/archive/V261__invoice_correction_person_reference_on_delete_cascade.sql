ALTER TABLE invoice_correction DROP CONSTRAINT invoice_correction_head_of_family_id_fkey;
ALTER TABLE invoice_correction ADD CONSTRAINT invoice_correction_head_of_family_id_fkey FOREIGN KEY (head_of_family_id) REFERENCES person(id) ON DELETE CASCADE;
