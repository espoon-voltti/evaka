create index concurrently idx$application_doc
    on application using gin (document jsonb_path_ops);
