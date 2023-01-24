CREATE TYPE assistance_need_decision_status AS ENUM (
    'DRAFT',
    'NEEDS_WORK',
    'ACCEPTED',
    'REJECTED',
    'ANNULLED'
);

ALTER TABLE assistance_need_decision DROP CONSTRAINT check$assistance_need_decision_no_validity_period_overlap;

ALTER TABLE assistance_need_decision RENAME COLUMN status TO status_old;

ALTER TABLE assistance_need_decision
    ADD COLUMN status assistance_need_decision_status,
    ADD CONSTRAINT check$assistance_need_decision_no_validity_period_overlap EXCLUDE USING gist (
        child_id WITH =,
        validity_period WITH &&
    ) WHERE (status = 'ACCEPTED'),
    ADD COLUMN annulment_reason text NOT NULL DEFAULT '',
    ADD CONSTRAINT check$annulment_reason CHECK (
        CASE status
            WHEN 'ANNULLED' THEN annulment_reason <> ''
            ELSE annulment_reason = ''
        END
    );

UPDATE assistance_need_decision SET status = status_old::assistance_need_decision_status;

ALTER TABLE assistance_need_decision
    ALTER COLUMN status SET NOT NULL,
    DROP COLUMN status_old;
