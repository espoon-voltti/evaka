ALTER TABLE bulletin_instance DROP COLUMN bulletin_receiver_id;
ALTER TABLE bulletin_instance ADD COLUMN bulletin_id uuid REFERENCES bulletin(id);
