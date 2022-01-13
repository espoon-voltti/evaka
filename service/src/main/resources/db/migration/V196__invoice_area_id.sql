ALTER TABLE invoice ADD COLUMN area_id uuid CONSTRAINT fk$area REFERENCES care_area (id);

UPDATE invoice SET area_id = (
    SELECT id
    FROM care_area
    WHERE area_code = agreement_type
    ORDER BY id
    LIMIT 1
);

ALTER TABLE invoice ALTER COLUMN area_id SET NOT NULL;
ALTER TABLE invoice DROP COLUMN agreement_type;