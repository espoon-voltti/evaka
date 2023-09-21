CREATE INDEX CONCURRENTLY idx$application_note_message_content
    ON application_note (message_content_id)
    WHERE message_content_id IS NOT NULL;
CREATE INDEX CONCURRENTLY idx$assistance_need_decision_unit
    ON assistance_need_decision (selected_unit)
    WHERE selected_unit IS NOT NULL;
CREATE INDEX CONCURRENTLY idx$assistance_need_decision_decision_maker
    ON assistance_need_decision (decision_maker_employee_id)
    WHERE assistance_need_decision.decision_maker_employee_id IS NOT NULL;
CREATE INDEX CONCURRENTLY idx$assistance_need_decision_preparer_1
    ON assistance_need_decision (preparer_1_employee_id)
    WHERE assistance_need_decision.preparer_1_employee_id IS NOT NULL;
CREATE INDEX CONCURRENTLY idx$assistance_need_decision_preparer_2
    ON assistance_need_decision (preparer_2_employee_id)
    WHERE assistance_need_decision.preparer_2_employee_id IS NOT NULL;
CREATE INDEX CONCURRENTLY idx$assistance_need_guardian_person
    ON assistance_need_decision_guardian (person_id);
CREATE INDEX CONCURRENTLY idx$child_consent_given_by_employee
    ON child_consent (given_by_employee)
    WHERE given_by_employee IS NOT NULL;
CREATE INDEX CONCURRENTLY idx$child_consent_given_by_guardian
    ON child_consent (given_by_guardian)
    WHERE given_by_guardian IS NOT NULL;
CREATE INDEX CONCURRENTLY idx$curriculum_document_event_employee
    ON curriculum_document_event (employee_id);
CREATE INDEX CONCURRENTLY idx$family_contact_contact_person
    ON family_contact (contact_person_id);
CREATE INDEX CONCURRENTLY idx$fee_decision_approved_by
    ON fee_decision (approved_by_id)
    WHERE approved_by_id IS NOT NULL;
CREATE INDEX CONCURRENTLY idx$income_application
    ON income (application_id)
    WHERE application_id IS NOT NULL;
CREATE INDEX CONCURRENTLY idx$income_statement_handler
    ON income_statement (handler_id)
    WHERE handler_id IS NOT NULL;
CREATE INDEX CONCURRENTLY idx$invoice_correction_child
    ON invoice_correction (child_id);
CREATE INDEX CONCURRENTLY idx$messaging_blocklist_blocked_recipient
    ON messaging_blocklist (blocked_recipient);
CREATE INDEX CONCURRENTLY idx$mobile_device_employee
    ON mobile_device (employee_id)
    WHERE employee_id IS NOT NULL;
CREATE INDEX CONCURRENTLY idx$payment_unit
    ON payment (unit_id)
    WHERE unit_id IS NOT NULL;
CREATE INDEX CONCURRENTLY idx$pedagogical_document_read_person
    ON pedagogical_document_read (person_id);
CREATE INDEX CONCURRENTLY idx$staff_occupancy_coefficient_employee
    ON staff_occupancy_coefficient (employee_id);
CREATE INDEX CONCURRENTLY idx$varda_service_need_child
    ON varda_service_need (evaka_child_id);
CREATE INDEX CONCURRENTLY idx$voucher_value_decision_approved_by
    ON voucher_value_decision (approved_by)
    WHERE approved_by IS NOT NULL;
CREATE INDEX CONCURRENTLY idx$voucher_value_decision_decision_handler
    ON voucher_value_decision (decision_handler)
    WHERE decision_handler IS NOT NULL;
