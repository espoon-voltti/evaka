CREATE TABLE sfi_get_events_continuation_token(
    continuation_token text PRIMARY KEY,
    created_at timestamp with time zone DEFAULT now() NOT NULL
);
