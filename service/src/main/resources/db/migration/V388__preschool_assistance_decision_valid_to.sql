ALTER TABLE assistance_need_preschool_decision
    ADD COLUMN valid_to DATE,
    ADD CONSTRAINT valid_from_before_valid_to CHECK (valid_from <= valid_to);

WITH fill_in_valid_to AS (SELECT previous.id, min(next.valid_from) - interval '1 day' AS new_valid_to
                          FROM assistance_need_preschool_decision previous,
                               assistance_need_preschool_decision next
                          WHERE previous.id != next.id
                            AND previous.child_id = next.child_id
                            AND previous.status = 'ACCEPTED'
                            AND next.status = 'ACCEPTED'
                            AND previous.valid_from < next.valid_from
                            AND previous.valid_to IS NULL
                          GROUP BY previous.id)
UPDATE assistance_need_preschool_decision
SET valid_to = fill_in_valid_to.new_valid_to
FROM fill_in_valid_to
WHERE fill_in_valid_to.id = assistance_need_preschool_decision.id;

ALTER TABLE assistance_need_preschool_decision
    ADD CONSTRAINT check$assistance_need_preschool_decision_no_overlap EXCLUDE USING gist (
        child_id WITH =,
        daterange(valid_from, valid_to, '[]') WITH &&
        ) WHERE (status = 'ACCEPTED');
