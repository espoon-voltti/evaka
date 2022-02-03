CREATE TYPE absence_category AS ENUM (
    'BILLABLE',
    'NONBILLABLE'
);

LOCK TABLE absence;
ALTER TABLE absence ADD COLUMN category absence_category;

UPDATE absence SET category = CASE care_type
    WHEN 'DAYCARE' THEN 'BILLABLE'::absence_category
    WHEN 'PRESCHOOL_DAYCARE' THEN 'BILLABLE'

    WHEN 'SCHOOL_SHIFT_CARE' THEN 'NONBILLABLE'
    WHEN 'PRESCHOOL' THEN 'NONBILLABLE'
    WHEN 'DAYCARE_5YO_FREE' THEN 'NONBILLABLE'
    WHEN 'CLUB' THEN 'NONBILLABLE'
END;

ALTER TABLE absence ALTER COLUMN category SET NOT NULL;

ALTER TABLE absence DROP CONSTRAINT absence_child_id_date_placement_type_key;
ALTER TABLE absence ADD CONSTRAINT uniq$absence_child_date_category UNIQUE (child_id, date, category);

ALTER TABLE absence DROP COLUMN care_type;