-- Deprecated assistance need decision tables, superseded by the child_document
-- system (MIGRATED_DAYCARE_ASSISTANCE_NEED_DECISION /
-- MIGRATED_PRESCHOOL_ASSISTANCE_NEED_DECISION). All decided decisions have been
-- migrated; the remaining rows are unmigrated drafts.

DROP TABLE assistance_need_decision_guardian;
DROP TABLE assistance_need_preschool_decision_guardian;
DROP TABLE assistance_need_decision;
DROP TABLE assistance_need_preschool_decision;

-- Sequences and enum types left orphaned by the dropped tables
DROP SEQUENCE assistance_need_decision_number_seq;
DROP SEQUENCE assistance_need_preschool_decision_number_seq;
DROP TYPE assistance_need_decision_status;
DROP TYPE assistance_need_preschool_decision_type;
