CREATE TABLE holiday_period
(
    id                           uuid PRIMARY KEY         DEFAULT ext.uuid_generate_v1mc(),
    created                      timestamp with time zone DEFAULT now() NOT NULL,
    updated                      timestamp with time zone DEFAULT now() NOT NULL,
    period                       daterange                              NOT NULL,
    reservation_deadline         date                                   NOT NULL,
    show_reservation_banner_from date                                   NOT NULL,
    description                  jsonb                                  NOT NULL,
    description_link             jsonb                                  NOT NULL,

    CONSTRAINT period$no_overlaps EXCLUDE USING gist ( period WITH && )
);

CREATE TRIGGER set_timestamp BEFORE UPDATE ON holiday_period FOR EACH ROW EXECUTE PROCEDURE trigger_refresh_updated();
