-- Needs to be run as flyway user because of the temporary changes to table constraints

ALTER TABLE placement
  DROP CONSTRAINT exclude$placement_no_overlap,
  ADD CONSTRAINT exclude$placement_no_overlap EXCLUDE USING gist (child_id WITH =, daterange(start_date, end_date, '[]'::text) WITH &&) DEFERRABLE INITIALLY DEFERRED;

ALTER TABLE daycare_group_placement
  DROP CONSTRAINT fk$placement_id,
  DROP CONSTRAINT no_overlap_within_daycare_placement,
  ADD CONSTRAINT fk$placement_id FOREIGN KEY (daycare_placement_id) REFERENCES placement(id) DEFERRABLE INITIALLY DEFERRED,
  ADD CONSTRAINT no_overlap_within_daycare_placement EXCLUDE USING gist (daycare_placement_id WITH =, daterange(start_date, end_date, '[]'::text) WITH &&) DEFERRABLE INITIALLY DEFERRED;

BEGIN;

WITH child_age_period AS (
  SELECT
    id,
    unnest(ARRAY[
      daterange(date_of_birth, make_date(date_part('year', date_of_birth)::int + 5, 7, 31), '[]'),
      daterange(make_date(date_part('year', date_of_birth)::int + 5, 8, 1), make_date(date_part('year', date_of_birth)::int + 6, 7, 31), '[]'),
      daterange(make_date(date_part('year', date_of_birth)::int + 6, 8, 1), null, '[]')
    ]) period,
    unnest(ARRAY[
      false,
      true,
      false
    ]) is_5yo
  FROM person
  JOIN child USING (id)
), split_placement AS (
  SELECT
    p.id,
    p.child_id,
    p.unit_id,
    (CASE
      WHEN cap.is_5yo AND p.type = 'DAYCARE' THEN 'DAYCARE_FIVE_YEAR_OLDS'
      WHEN cap.is_5yo AND p.type = 'DAYCARE_PART_TIME' THEN 'DAYCARE_PART_TIME_FIVE_YEAR_OLDS'
      ELSE p.type
    END) AS type,
    greatest(p.start_date, lower(cap.period)) AS start_date,
    least(p.end_date, (upper(cap.period) - interval '1 day')::date) AS end_date,
    rank() OVER (PARTITION BY p.id ORDER BY cap.period ASC)
  FROM placement p
  JOIN child_age_period cap ON p.child_id = cap.id AND daterange(p.start_date, p.end_date, '[]') && cap.period
  WHERE p.type = ANY('{DAYCARE, DAYCARE_PART_TIME}'::placement_type[])
), split_placement_with_new_id AS (
   SELECT
     p.*,
     (CASE WHEN p.rank = 1 THEN p.id ELSE ext.uuid_generate_v1mc() END) AS new_id
   FROM split_placement p
 ), split_placement_with_group AS (
  SELECT
    p.*,
    gp.id AS group_placement_id,
    greatest(p.start_date, gp.start_date) AS group_placement_start_date,
    least(p.end_date, gp.end_date) AS group_placement_end_date,
    gp.daycare_group_id AS group_id,
    rank() OVER (PARTITION BY gp.id ORDER BY p.start_date ASC, gp.start_date ASC) gp_rank
  FROM split_placement_with_new_id p
  JOIN daycare_group_placement gp ON p.id = gp.daycare_placement_id AND daterange(p.start_date, p.end_date, '[]') && daterange(gp.start_date, gp.end_date, '[]')
  ORDER BY p.id, p.rank, group_placement_start_date
), updated_placement AS (
  UPDATE placement orig SET id = p.new_id, start_date = p.start_date, end_date = p.end_date, type = p.type
  FROM split_placement_with_new_id p
  WHERE orig.id = p.id AND p.rank = 1
), new_placement AS (
  INSERT INTO placement (id, type, child_id, unit_id, start_date, end_date)
  SELECT p.new_id, p.type, p.child_id, p.unit_id, p.start_date, p.end_date
  FROM split_placement_with_new_id p
  WHERE p.rank > 1
), updated_group_placement AS (
  UPDATE daycare_group_placement orig SET daycare_placement_id = gp.new_id, start_date = gp.group_placement_start_date, end_date = gp.group_placement_end_date
  FROM split_placement_with_group gp
  WHERE orig.id = gp.group_placement_id AND gp.gp_rank = 1
)
INSERT INTO daycare_group_placement (daycare_placement_id, daycare_group_id, start_date, end_date)
SELECT gp.new_id, gp.group_id, gp.group_placement_start_date, gp.group_placement_end_date
FROM split_placement_with_group gp
WHERE gp.gp_rank > 1
;

ROLLBACK;

ALTER TABLE placement
  DROP CONSTRAINT exclude$placement_no_overlap,
  ADD CONSTRAINT exclude$placement_no_overlap EXCLUDE USING gist (child_id WITH =, daterange(start_date, end_date, '[]'::text) WITH &&);

ALTER TABLE daycare_group_placement
  DROP CONSTRAINT fk$placement_id,
  DROP CONSTRAINT no_overlap_within_daycare_placement,
  ADD CONSTRAINT fk$placement_id FOREIGN KEY (daycare_placement_id) REFERENCES placement(id),
  ADD CONSTRAINT no_overlap_within_daycare_placement EXCLUDE USING gist (daycare_placement_id WITH =, daterange(start_date, end_date, '[]'::text) WITH &&);
