CREATE INDEX CONCURRENTLY idx$absence_modified_by ON absence (modified_by);

CREATE INDEX CONCURRENTLY idx$application_note_created_by ON application_note (created_by);

CREATE INDEX CONCURRENTLY idx$application_note_updated_by ON application_note (updated_by);

CREATE INDEX CONCURRENTLY idx$assistance_action_updated_by ON assistance_action (updated_by);

CREATE INDEX CONCURRENTLY idx$assistance_need_updated_by ON assistance_need (updated_by);

CREATE INDEX CONCURRENTLY idx$attachment_uploaded_by ON attachment (uploaded_by);

CREATE INDEX CONCURRENTLY idx$attendance_reservation_created_by ON attendance_reservation (created_by);

CREATE INDEX CONCURRENTLY idx$decision_created_by ON decision (created_by);

CREATE INDEX CONCURRENTLY idx$fee_alteration_updated_by ON fee_alteration (updated_by);

CREATE INDEX CONCURRENTLY idx$invoice_sent_by ON invoice (sent_by);

CREATE INDEX CONCURRENTLY idx$pedagogical_document_created_by ON pedagogical_document (created_by);

CREATE INDEX CONCURRENTLY idx$pedagogical_document_updated_by ON pedagogical_document (updated_by);

CREATE INDEX CONCURRENTLY idx$service_need_confirmed_by ON service_need (confirmed_by);
