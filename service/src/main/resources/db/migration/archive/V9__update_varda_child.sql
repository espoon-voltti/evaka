ALTER TABLE varda_child ADD COLUMN oph_organizer_oid text;
ALTER TABLE varda_child ADD CONSTRAINT unique_varda_child_organizer UNIQUE (person_id, oph_organizer_oid);
ALTER TABLE varda_child DROP CONSTRAINT varda_child_person_id_key;
