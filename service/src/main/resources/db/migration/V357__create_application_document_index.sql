create index concurrently if not exists idx$application_doc
    on application using gin (document jsonb_path_ops);
