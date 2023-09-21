ALTER TABLE fridge_partner ADD COLUMN other_indx smallint;
UPDATE fridge_partner fp1 SET other_indx = (
    SELECT indx
    FROM fridge_partner fp2
    WHERE fp1.partnership_id = fp2.partnership_id AND fp1.indx != fp2.indx
);

ALTER TABLE fridge_partner
    ADD CONSTRAINT fk$other_indx FOREIGN KEY (partnership_id, other_indx) REFERENCES fridge_partner(partnership_id, indx) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED,
    ADD CONSTRAINT chk$other_indx CHECK (indx != other_indx);

ALTER TABLE fridge_partner ALTER COLUMN other_indx SET NOT NULL;