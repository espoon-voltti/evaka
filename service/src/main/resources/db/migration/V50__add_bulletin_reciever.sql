ALTER TABLE bulletin DROP COLUMN unit_id;
ALTER TABLE bulletin DROP COLUMN group_id;
ALTER TABLE bulletin ADD COLUMN sender text;
CREATE TABLE bulletin_receiver (id uuid PRIMARY KEY DEFAULT ext.uuid_generate_v1mc(),
                                bulletin_id uuid NOT NULL REFERENCES bulletin(id) ON DELETE CASCADE,
                                unit_id uuid REFERENCES daycare(id),
                                group_id uuid REFERENCES daycare_group(id),
                                child_id uuid REFERENCES person(id));
ALTER TABLE bulletin_instance DROP COLUMN bulletin_id;
ALTER TABLE bulletin_instance DROP COLUMN receiver_id;
ALTER TABLE bulletin_instance ADD COLUMN bulletin_receiver_id uuid REFERENCES bulletin_receiver(id);
ALTER TABLE bulletin_instance ADD COLUMN receiver_person_id uuid REFERENCES person(id);

CREATE INDEX idx$bulletin_receiver_bulletin_id ON bulletin_receiver (bulletin_id);
