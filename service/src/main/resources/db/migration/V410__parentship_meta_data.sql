ALTER TYPE create_source ADD VALUE 'DVV';

ALTER TABLE fridge_child RENAME COLUMN created TO created_at;

ALTER TABLE fridge_child
    ADD COLUMN create_source create_source,
    ADD COLUMN created_by_user uuid REFERENCES evaka_user(id),
    ADD COLUMN created_by_application uuid REFERENCES application(id),
    ADD COLUMN modify_source modify_source,
    ADD COLUMN modified_by_user uuid REFERENCES evaka_user(id),
    ADD COLUMN modified_at timestamp with time zone;

ALTER TABLE fridge_child
    ADD CONSTRAINT check_created_by_user CHECK (
        (create_source = 'USER') = (created_by_user IS NOT NULL )
    ),
    ADD CONSTRAINT check_created_by_application CHECK (
        (create_source = 'APPLICATION') = (created_by_application IS NOT NULL )
    ),
    ADD CONSTRAINT check_modified_by_user CHECK (
        (modify_source = 'USER') = (modified_by_user IS NOT NULL )
    );
