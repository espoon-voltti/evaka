ALTER TABLE calendar_event
    ADD COLUMN created_by UUID REFERENCES evaka_user,
    ADD COLUMN modified_by UUID REFERENCES evaka_user,
    ADD COLUMN content_modified_by UUID REFERENCES evaka_user;

UPDATE calendar_event SET
    created_by = COALESCE(created_by, '00000000-0000-0000-0000-000000000000'::UUID),
    modified_by = COALESCE(modified_by, '00000000-0000-0000-0000-000000000000'::UUID),
    content_modified_by = COALESCE(content_modified_by, '00000000-0000-0000-0000-000000000000'::UUID);

ALTER TABLE calendar_event
    ALTER COLUMN created_by SET NOT NULL,
    ALTER COLUMN modified_by SET NOT NULL,
    ALTER COLUMN content_modified_by SET NOT NULL;

CREATE INDEX fk$calendar_event_created_by ON calendar_event(created_by);
CREATE INDEX fk$calendar_event_modified_by ON calendar_event(modified_by);
CREATE INDEX fk$calendar_event_content_modified_by ON calendar_event(content_modified_by);
