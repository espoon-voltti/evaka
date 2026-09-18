ALTER TABLE assistance_factor
    DROP CONSTRAINT "fk$modified_by",
    ADD CONSTRAINT "fk$modified_by" FOREIGN KEY (modified_by) REFERENCES evaka_user(id);

ALTER TABLE daycare_assistance
    DROP CONSTRAINT "fk$modified_by",
    ADD CONSTRAINT "fk$modified_by" FOREIGN KEY (modified_by) REFERENCES evaka_user(id);

ALTER TABLE other_assistance_measure
    DROP CONSTRAINT "fk$modified_by",
    ADD CONSTRAINT "fk$modified_by" FOREIGN KEY (modified_by) REFERENCES evaka_user(id);

ALTER TABLE preschool_assistance
    DROP CONSTRAINT "fk$modified_by",
    ADD CONSTRAINT "fk$modified_by" FOREIGN KEY (modified_by) REFERENCES evaka_user(id);

ALTER TABLE voucher_value_decision
    DROP CONSTRAINT "fk$approved_by",
    ADD CONSTRAINT "fk$approved_by" FOREIGN KEY (approved_by) REFERENCES employee(id);
