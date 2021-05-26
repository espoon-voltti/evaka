ALTER TABLE fee_alteration ADD CONSTRAINT fk$person_id FOREIGN KEY (person_id) REFERENCES person(id);

ALTER TABLE income ADD CONSTRAINT fk$person_id FOREIGN KEY (person_id) REFERENCES person(id);
