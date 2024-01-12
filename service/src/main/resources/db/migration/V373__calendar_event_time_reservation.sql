CREATE TABLE calendar_event_time
(
    id                uuid PRIMARY KEY                  DEFAULT ext.uuid_generate_v1mc(),
    created_at        timestamp with time zone NOT NULL,
    created_by        uuid                     NOT NULL REFERENCES evaka_user (id),
    updated_at        timestamp with time zone NOT NULL,
    modified_at       timestamp with time zone NOT NULL,
    modified_by       uuid                     NOT NULL REFERENCES evaka_user (id),
    calendar_event_id uuid                     NOT NULL REFERENCES calendar_event (id) ON DELETE CASCADE,
    date              date                     NOT NULL,
    start_time        time without time zone   NOT NULL,
    end_time          time without time zone   NOT NULL,
    child_id          uuid REFERENCES child (id)
);

CREATE OR REPLACE FUNCTION public.trigger_refresh_updated_at() RETURNS trigger
    LANGUAGE plpgsql
AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$;

CREATE TRIGGER set_timestamp
    BEFORE UPDATE
    ON calendar_event_time
    FOR EACH ROW
EXECUTE PROCEDURE trigger_refresh_updated_at();

CREATE VIEW calendar_event_attendee_child_view(calendar_event_id, child_id) AS (
    -- child-specific
    SELECT calendar_event_id, child_id
    FROM calendar_event_attendee
    WHERE child_id IS NOT NULL

    UNION

    -- group-wide
    SELECT DISTINCT ce.id AS calendar_event_id, p.child_id
    FROM calendar_event_attendee cea
    JOIN calendar_event ce ON cea.calendar_event_id = ce.id
    JOIN daycare_group_placement dgp ON cea.group_id = dgp.daycare_group_id AND daterange(dgp.start_date, dgp.end_date, '[]') && ce.period
    JOIN placement p ON dgp.daycare_placement_id = p.id AND daterange(p.start_date, p.end_date, '[]') && ce.period
    WHERE cea.child_id IS NULL

    UNION

    -- unit-wide
    SELECT DISTINCT ce.id AS calendar_event_id, p.child_id
    FROM calendar_event_attendee cea
    JOIN calendar_event ce ON cea.calendar_event_id = ce.id
    JOIN placement p ON cea.unit_id = p.unit_id AND daterange(p.start_date, p.end_date, '[]') && ce.period
    WHERE cea.group_id IS NULL AND cea.child_id IS NULL
);
