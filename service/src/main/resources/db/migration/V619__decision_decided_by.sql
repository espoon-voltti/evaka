ALTER TABLE decision ADD COLUMN decided_by uuid REFERENCES evaka_user(id);

CREATE INDEX fk$decision_decided_by ON decision(decided_by);
