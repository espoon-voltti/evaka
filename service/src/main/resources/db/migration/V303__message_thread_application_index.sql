CREATE INDEX CONCURRENTLY idx$message_thread_application ON message_thread (application_id) WHERE application_id IS NOT NULL;
