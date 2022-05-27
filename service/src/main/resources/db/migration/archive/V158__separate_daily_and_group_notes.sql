-- create separate table for group notes

CREATE TABLE group_note (
    id uuid DEFAULT ext.uuid_generate_v1mc() PRIMARY KEY,
    created timestamp with time zone DEFAULT now() NOT NULL,
    updated timestamp with time zone DEFAULT now() NOT NULL,
    group_id uuid NOT NULL REFERENCES daycare_group,
    note text NOT NULL,
    modified_at timestamp with time zone NOT NULL DEFAULT now()
);

CREATE TRIGGER set_timestamp BEFORE UPDATE ON group_note FOR EACH ROW EXECUTE PROCEDURE trigger_refresh_updated();

CREATE INDEX idx$group_note_group_id ON group_note(group_id);

-- add technical tiestamps to old table

ALTER TABLE daycare_daily_note
    ADD COLUMN created timestamp with time zone DEFAULT now() NOT NULL,
    ADD COLUMN updated timestamp with time zone DEFAULT now() NOT NULL;

UPDATE daycare_daily_note SET created = modified_at, updated = modified_at;

CREATE TRIGGER set_timestamp BEFORE UPDATE ON daycare_daily_note FOR EACH ROW EXECUTE PROCEDURE trigger_refresh_updated();

-- drop modified_by until evaka_user can be referenced
ALTER TABLE daycare_daily_note DROP COLUMN modified_by;

-- copy group notes to new table

INSERT INTO group_note (id, created, updated, group_id, note, modified_at)
SELECT id, created, updated, group_id, coalesce(note, ''), modified_at
FROM daycare_daily_note
WHERE child_id IS NULL AND group_id IS NOT NULL;

-- remove group notes from old table

DELETE FROM daycare_daily_note
WHERE child_id IS NULL AND group_id IS NOT NULL;

-- rename

ALTER TABLE daycare_daily_note RENAME TO child_daily_note;
ALTER TYPE daycare_daily_note_level_info RENAME TO child_daily_note_level;
ALTER TYPE daycare_daily_note_reminder RENAME TO child_daily_note_reminder;

-- drop group_id from old table and make child_id not null

ALTER TABLE child_daily_note DROP CONSTRAINT daycare_daily_note_check;
ALTER TABLE child_daily_note DROP CONSTRAINT unique_daycare_daily_note;
ALTER TABLE child_daily_note DROP COLUMN group_id;
ALTER TABLE child_daily_note ALTER COLUMN child_id SET NOT NULL;

-- no date, only one note per child
DELETE FROM child_daily_note WHERE id IN (
    SELECT id
    FROM (
         SELECT id, rank() OVER (PARTITION BY child_id ORDER BY date DESC) AS rank
         FROM child_daily_note
    ) AS ranked
    WHERE ranked.rank > 1
);
ALTER TABLE child_daily_note DROP COLUMN date;
CREATE UNIQUE INDEX uniq$child_daily_note_child_id ON child_daily_note(child_id);

-- text columns as not null
UPDATE child_daily_note SET note = '' WHERE note IS NULL;
ALTER TABLE child_daily_note ALTER COLUMN note SET NOT NULL;
UPDATE child_daily_note SET reminder_note = '' WHERE reminder_note IS NULL;
ALTER TABLE child_daily_note ALTER COLUMN reminder_note SET NOT NULL;
