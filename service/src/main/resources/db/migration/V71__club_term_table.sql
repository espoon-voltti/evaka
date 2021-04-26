CREATE TABLE club_term (
    id uuid PRIMARY KEY DEFAULT ext.uuid_generate_v1mc(),
    created timestamp with time zone DEFAULT NOW() NOT NULL,
    updated timestamp with time zone DEFAULT NOW() NOT NULL,
    term daterange NOT NULL,
    application_period daterange NOT NULL,

    CONSTRAINT club_term$no_overlaps EXCLUDE USING gist ( term WITH && )
);
