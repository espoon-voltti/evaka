UPDATE assistance_need_decision SET end_date = upper(assistance_services_time) WHERE 'ASSISTANCE_SERVICES_FOR_TIME' = ANY(assistance_levels);

DELETE FROM assistance_need_decision WHERE start_date IS NULL;

DELETE FROM assistance_need_decision ad WHERE EXISTS(
  SELECT 1 FROM assistance_need_decision c
  WHERE c.id <> ad.id
	  AND daterange(ad.start_date, ad.end_date, '[]') && daterange(c.start_date, c.end_date)
	  AND c.child_id = ad.child_id
);

ALTER TABLE assistance_need_decision
  ADD COLUMN validity_period daterange,
  ADD CONSTRAINT check$assistance_need_decision_no_validity_period_overlap EXCLUDE USING gist (
    child_id WITH =,
    validity_period WITH &&
  ) WHERE (status = 'ACCEPTED');

UPDATE assistance_need_decision
SET validity_period = daterange(start_date, end_date, '[]');

ALTER TABLE assistance_need_decision
  DROP COLUMN assistance_services_time,
  DROP COLUMN start_date,
  DROP COLUMN end_date;

CREATE INDEX idx$assistance_need_decision_validity_period ON assistance_need_decision USING GIST (validity_period);
