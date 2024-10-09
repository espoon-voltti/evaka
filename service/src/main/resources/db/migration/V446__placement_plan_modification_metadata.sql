ALTER TABLE placement_plan
    ADD COLUMN modified_at TIMESTAMP WITH TIME ZONE,
    ADD COLUMN modified_by uuid REFERENCES evaka_user;

UPDATE placement_plan SET modified_at = updated;


ALTER TABLE placement_plan ALTER COLUMN modified_at SET NOT NULL;

CREATE INDEX fk$placement_plan_modified_by ON placement_plan(modified_by);
