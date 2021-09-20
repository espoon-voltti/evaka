-- add indexes based on query analysis
CREATE INDEX CONCURRENTLY idx$absence_modified_by_employee ON absence (modified_by_employee_id) WHERE modified_by_employee_id IS NOT NULL;
CREATE INDEX CONCURRENTLY idx$absence_modified_by_guardian ON absence (modified_by_guardian_id) WHERE modified_by_guardian_id IS NOT NULL;


-- add indexes based on query analysis
CREATE INDEX CONCURRENTLY idx$application_sent_date ON application (sentdate, status) WHERE sentdate IS NOT NULL;
CREATE INDEX CONCURRENTLY idx$application_due_date ON application (duedate, status) WHERE duedate IS NOT NULL;
CREATE INDEX CONCURRENTLY idx$application_other_guardian ON application (other_guardian_id) WHERE other_guardian_id IS NOT NULL;


-- add missing index for commonly used foreign key
CREATE INDEX CONCURRENTLY idx$application_note_application ON application_note (application_id);


-- add index based on query analysis
CREATE INDEX CONCURRENTLY idx$async_job_completed ON async_job (completed_at) WHERE completed_at IS NOT NULL;


-- add index based on query analysis
CREATE INDEX CONCURRENTLY idx$attendance_reservation_child_date ON attendance_reservation (child_id, start_date);
CREATE INDEX CONCURRENTLY idx$attendance_reservation_employee ON attendance_reservation (created_by_employee_id) WHERE created_by_employee_id IS NOT NULL;
CREATE INDEX CONCURRENTLY idx$attendance_reservation_guardian ON attendance_reservation (created_by_guardian_id) WHERE created_by_guardian_id IS NOT NULL;


-- add gist indexes for daterange queries
CREATE INDEX CONCURRENTLY idx$backup_care_group_gist ON backup_care USING GIST (group_id, daterange(start_date, end_date, '[]')) WHERE group_id IS NOT NULL;
CREATE INDEX CONCURRENTLY idx$backup_care_unit_gist ON backup_care USING GIST (unit_id, daterange(start_date, end_date, '[]'));


-- add index based on query analysis
CREATE INDEX CONCURRENTLY idx$daycare_daily_note_group ON daycare_daily_note (group_id, date);


-- add gist index for daterange queries
CREATE INDEX CONCURRENTLY idx$daycare_group_placement_group ON daycare_group_placement USING GIST (daycare_group_id, daterange(start_date, end_date, '[]'));


-- add gist index for daterange queries
CREATE INDEX CONCURRENTLY idx$fee_alteration_person ON fee_alteration USING GIST (person_id, daterange(valid_from, valid_to, '[]'));


-- add gist index for daterange queries
CREATE INDEX CONCURRENTLY idx$fee_decision_valid_during ON fee_decision USING GIST(valid_during);


-- add btree index for normal date queries
CREATE INDEX CONCURRENTLY idx$income_statement_person ON income_statement (person_id, start_date, end_date);
-- add gist index for daterange queries
CREATE INDEX CONCURRENTLY idx$income_statement_person_gist ON income_statement USING GIST (person_id, daterange(start_date, end_date, '[]'));


-- add btree index for normal date queries
CREATE INDEX CONCURRENTLY idx$invoice_period ON invoice (period_start, period_end);
-- add gist index for daterange queries
CREATE INDEX CONCURRENTLY idx$invoice_period_gist ON invoice USING GIST (daterange(period_start, period_end, '[]'));


-- add index based on query analysis
CREATE INDEX CONCURRENTLY idx$invoice_row_child ON invoice_row (child);


-- add index based on query analysis
CREATE INDEX CONCURRENTLY idx$message_content_author ON message_content (author_id);


-- add gist index for daterange queries
CREATE INDEX CONCURRENTLY idx$placement_unit_gist ON placement USING GIST (unit_id, daterange(start_date, end_date, '[]'));


-- add gist index for daterange queries
CREATE INDEX CONCURRENTLY idx$placement_plan_unit_gist ON placement_plan USING GIST (unit_id, daterange(start_date, end_date, '[]'));


-- add indexes based on query analysis
CREATE INDEX CONCURRENTLY idx$voucher_value_decision_head_of_family ON voucher_value_decision (head_of_family_id);
CREATE INDEX CONCURRENTLY idx$voucher_value_decision_partner ON voucher_value_decision (partner_id) WHERE partner_id IS NOT NULL;
CREATE INDEX CONCURRENTLY idx$voucher_value_decision_valid ON voucher_value_decision USING GIST (daterange(valid_from, valid_to, '[]'));
