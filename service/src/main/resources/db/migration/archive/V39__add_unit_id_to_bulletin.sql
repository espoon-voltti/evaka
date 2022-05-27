ALTER TABLE bulletin ADD COLUMN unit_id uuid REFERENCES daycare(id);

CREATE INDEX idx$bulletin_unit_id ON bulletin (unit_id);