ALTER TABLE person ADD CONSTRAINT ssn_require_vtj_update CHECK (social_security_number IS NULL OR updated_from_vtj IS NOT NULL);
