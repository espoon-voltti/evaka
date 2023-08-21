ALTER TABLE assistance_need_preschool_decision
    ADD COLUMN basis_document_pedagogical_report_date date DEFAULT NULL,
    ADD COLUMN basis_document_psychologist_statement_date date DEFAULT NULL,
    ADD COLUMN basis_document_social_report_date date DEFAULT NULL,
    ADD COLUMN basis_document_doctor_statement_date date DEFAULT NULL;
