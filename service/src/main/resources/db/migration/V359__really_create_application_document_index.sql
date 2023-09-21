CREATE INDEX CONCURRENTLY idx$application_doc ON application USING gin (document jsonb_path_ops);
