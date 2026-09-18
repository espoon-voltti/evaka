CREATE INDEX "fk$fridge_child_created_by_application" ON fridge_child (created_by_application) WHERE created_by_application IS NOT NULL;
CREATE INDEX "fk$child_document_read_person_id" ON child_document_read (person_id);
CREATE INDEX "fk$message_thread_participant_folder_id" ON message_thread_participant (folder_id) WHERE folder_id IS NOT NULL;
CREATE INDEX "fk$daycare_assistance_modified_by" ON daycare_assistance (modified_by);
CREATE INDEX "fk$other_assistance_measure_modified_by" ON other_assistance_measure (modified_by);
CREATE INDEX "fk$preschool_assistance_modified_by" ON preschool_assistance (modified_by);

ALTER TABLE guardian
    DROP CONSTRAINT unique_guardian_child,
    ADD CONSTRAINT guardian_pkey PRIMARY KEY (guardian_id, child_id);
DROP INDEX "idx$guardian_guardian_id";

ALTER TABLE guardian_blocklist
    ADD CONSTRAINT guardian_blocklist_pkey PRIMARY KEY USING INDEX "uniq$guardian_blocklist_guardian_child";

ALTER TABLE pedagogical_document_read
    ALTER COLUMN pedagogical_document_id SET NOT NULL,
    ALTER COLUMN person_id SET NOT NULL,
    ADD CONSTRAINT pedagogical_document_read_pkey PRIMARY KEY USING INDEX "uniq$pedagogical_document_read_by_person";
