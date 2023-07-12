CREATE TABLE child_discussion (
    id              uuid PRIMARY KEY         NOT NULL DEFAULT ext.uuid_generate_v1mc(),
    created         timestamp with time zone NOT NULL DEFAULT now(),
    updated         timestamp with time zone NOT NULL DEFAULT now(),
    child_id        uuid                     NOT NULL REFERENCES person(id) ON DELETE CASCADE,
    offered_date    date,
    held_date       date,
    counseling_date date
);

CREATE TRIGGER set_timestamp BEFORE UPDATE ON child_discussion FOR EACH ROW EXECUTE PROCEDURE trigger_refresh_updated();

CREATE UNIQUE INDEX uniq$child_discussion_child_id ON child_discussion(child_id);
