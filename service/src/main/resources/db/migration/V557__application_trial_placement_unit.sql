ALTER TABLE application ADD COLUMN trial_placement_unit uuid REFERENCES daycare(id);
CREATE INDEX fk$application_trial_placement_unit ON application(trial_placement_unit);
