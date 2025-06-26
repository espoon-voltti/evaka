ALTER TABLE child_document_decision ADD COLUMN daycare_id uuid REFERENCES daycare(id);
CREATE INDEX fk_child_document_decision_daycare_id ON child_document_decision(daycare_id);
