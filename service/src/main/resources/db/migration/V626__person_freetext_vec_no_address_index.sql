CREATE INDEX CONCURRENTLY idx$person_freetext_vec_no_address ON person USING gin (freetext_vec_no_address);
