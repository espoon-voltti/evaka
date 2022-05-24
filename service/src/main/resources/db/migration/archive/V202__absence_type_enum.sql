CREATE TYPE absence_type AS ENUM (
    'OTHER_ABSENCE',
    'SICKLEAVE',
    'UNKNOWN_ABSENCE',
    'PLANNED_ABSENCE',
    'PARENTLEAVE',
    'FORCE_MAJEURE'
);

LOCK TABLE absence;
ALTER TABLE absence ALTER COLUMN absence_type TYPE absence_type USING absence_type::absence_type;
