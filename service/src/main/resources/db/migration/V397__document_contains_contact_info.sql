ALTER TABLE assistance_need_decision ADD COLUMN document_contains_contact_info bool NOT NULL DEFAULT false;
ALTER TABLE assistance_need_preschool_decision ADD COLUMN document_contains_contact_info bool NOT NULL DEFAULT false;
ALTER TABLE decision ADD COLUMN document_contains_contact_info bool NOT NULL DEFAULT false;
ALTER TABLE fee_decision ADD COLUMN document_contains_contact_info bool NOT NULL DEFAULT false;
ALTER TABLE voucher_value_decision ADD COLUMN document_contains_contact_info bool NOT NULL DEFAULT false;

COMMENT ON column assistance_need_decision.document_contains_contact_info IS
    'true if the document is a legacy document that may contain guardian name and address';
COMMENT ON column assistance_need_preschool_decision.document_contains_contact_info IS
    'true if the document is a legacy document that may contain guardian name and address';
COMMENT ON column decision.document_contains_contact_info IS
    'true if the document is a legacy document that may contain guardian name and address';
COMMENT ON column fee_decision.document_contains_contact_info IS
    'true if the document is a legacy document that may contain guardian name and address';
COMMENT ON column voucher_value_decision.document_contains_contact_info IS
    'true if the document is a legacy document that may contain guardian name and address';

UPDATE assistance_need_decision SET document_contains_contact_info = true WHERE document_key IS NOT NULL;
UPDATE assistance_need_preschool_decision SET document_contains_contact_info = true WHERE document_key IS NOT NULL;
UPDATE decision SET document_contains_contact_info = true WHERE document_key IS NOT NULL;
UPDATE fee_decision SET document_contains_contact_info = true WHERE document_key IS NOT NULL;
UPDATE voucher_value_decision SET document_contains_contact_info = true WHERE document_key IS NOT NULL;
