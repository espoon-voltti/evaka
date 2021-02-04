ALTER TABLE decision
    ADD COLUMN pending_decision_emails_sent_count int DEFAULT 0,
    ADD COLUMN pending_decision_email_sent timestamp with time zone;