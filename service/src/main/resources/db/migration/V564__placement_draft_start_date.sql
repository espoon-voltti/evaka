ALTER TABLE placement_draft ADD COLUMN start_date date;

UPDATE placement_draft pd
SET start_date = coalesce(
    (SELECT preferredstartdate FROM application_view a WHERE a.id = pd.application_id),
    current_date
);

ALTER TABLE placement_draft ALTER COLUMN start_date SET NOT NULL;

CREATE INDEX idx$placement_draft_start_date ON placement_draft(start_date);
