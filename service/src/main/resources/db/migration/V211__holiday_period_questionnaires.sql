CREATE TYPE questionnaire_type AS ENUM (
    'FIXED_PERIOD', 'OPEN_RANGES'
);

CREATE TABLE holiday_period_questionnaire (
    id                             uuid PRIMARY KEY         DEFAULT ext.uuid_generate_v1mc(),
    created                        timestamp with time zone DEFAULT now() NOT NULL,
    updated                        timestamp with time zone DEFAULT now() NOT NULL,
    holiday_period_id              uuid
        CONSTRAINT fk$holiday_period REFERENCES holiday_period ON DELETE RESTRICT,
    type                           questionnaire_type                     NOT NULL,
    absence_type                   absence_type                           NOT NULL,
    requires_strong_auth           boolean                                NOT NULL,
    active                         daterange                              NOT NULL,
    title                          jsonb                                  NOT NULL,
    description                    jsonb                                  NOT NULL,
    description_link               jsonb                                  NOT NULL,
    condition_continuous_placement daterange,
    period_options                 daterange[],
    period_option_label            jsonb
);

CREATE TRIGGER set_timestamp BEFORE UPDATE ON holiday_period_questionnaire FOR EACH ROW EXECUTE PROCEDURE trigger_refresh_updated();

ALTER TABLE holiday_period
    DROP COLUMN show_reservation_banner_from,
    DROP COLUMN description,
    DROP COLUMN description_link,
    DROP COLUMN free_period_deadline,
    DROP COLUMN free_period_question_label,
    DROP COLUMN free_period_period_options,
    DROP COLUMN free_period_period_option_label,
    ALTER COLUMN reservation_deadline DROP NOT NULL;
