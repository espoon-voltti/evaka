DROP VIEW IF EXISTS location_view;

ALTER TABLE daycare
    ADD COLUMN daycare_apply_period daterange,
    ADD COLUMN preschool_apply_period daterange,
    ADD COLUMN club_apply_period daterange;

UPDATE daycare SET daycare_apply_period = daterange('2020-03-01'::date, NULL) WHERE can_apply_daycare = TRUE;
UPDATE daycare SET preschool_apply_period = daterange('2020-03-01'::date, NULL) WHERE can_apply_preschool = TRUE;
UPDATE daycare SET club_apply_period = daterange('2020-03-01'::date, NULL) WHERE can_apply_club = TRUE;

ALTER TABLE daycare
    DROP COLUMN can_apply_daycare,
    DROP COLUMN can_apply_preschool,
    DROP COLUMN can_apply_club;
