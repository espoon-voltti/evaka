ALTER TABLE daycare
    ADD COLUMN daily_preschool_time timerange_non_nullable_range,
    ADD COLUMN daily_preparatory_time timerange_non_nullable_range;

UPDATE daycare
SET daily_preschool_time = ('09:00'::time, '13:00'::time)
WHERE 'PRESCHOOL' = ANY(type);

UPDATE daycare
SET daily_preparatory_time = ('09:00'::time, '14:00'::time)
WHERE 'PREPARATORY_EDUCATION' = ANY(type);

ALTER TABLE daycare
    ADD CONSTRAINT preschool_time_existence
        CHECK ( (daily_preschool_time IS NOT NULL) = ('PRESCHOOL' = ANY(type)) );

ALTER TABLE daycare
    ADD CONSTRAINT preparatory_time_existence
        CHECK ( (daily_preparatory_time IS NOT NULL) = ('PREPARATORY_EDUCATION' = ANY(type)) );
