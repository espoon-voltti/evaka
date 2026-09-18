ALTER TABLE sfi_message
    DROP CONSTRAINT sfi_message_decision_id_fkey,
    ADD CONSTRAINT sfi_message_decision_id_fkey FOREIGN KEY (decision_id) REFERENCES decision(id) ON DELETE CASCADE,
    DROP CONSTRAINT sfi_message_document_id_fkey,
    ADD CONSTRAINT sfi_message_document_id_fkey FOREIGN KEY (document_id) REFERENCES child_document(id) ON DELETE CASCADE,
    DROP CONSTRAINT sfi_message_fee_decision_id_fkey,
    ADD CONSTRAINT sfi_message_fee_decision_id_fkey FOREIGN KEY (fee_decision_id) REFERENCES fee_decision(id) ON DELETE CASCADE,
    DROP CONSTRAINT sfi_message_voucher_value_decision_id_fkey,
    ADD CONSTRAINT sfi_message_voucher_value_decision_id_fkey FOREIGN KEY (voucher_value_decision_id) REFERENCES voucher_value_decision(id) ON DELETE CASCADE,
    DROP CONSTRAINT sfi_message_guardian_id_fkey,
    ADD CONSTRAINT sfi_message_guardian_id_fkey FOREIGN KEY (guardian_id) REFERENCES person(id) ON DELETE CASCADE;

ALTER TABLE sfi_message_event
    DROP CONSTRAINT sfi_message_event_message_id_fkey,
    ADD CONSTRAINT sfi_message_event_message_id_fkey FOREIGN KEY (message_id) REFERENCES sfi_message(id) ON DELETE CASCADE;
