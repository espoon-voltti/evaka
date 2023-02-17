CREATE INDEX CONCURRENTLY idx$fee_decision_waiting_for_sending
    ON fee_decision
    USING GIST (valid_during)
    WHERE status = 'WAITING_FOR_SENDING';
CREATE INDEX CONCURRENTLY idx$fee_decision_waiting_for_manual_sending
    ON fee_decision
    USING GIST (valid_during)
    WHERE status = 'WAITING_FOR_MANUAL_SENDING';
CREATE INDEX CONCURRENTLY idx$voucher_value_decision_waiting_for_sending
    ON voucher_value_decision
    USING GIST (daterange(valid_from, valid_to, '[]'))
    WHERE status = 'WAITING_FOR_SENDING';
CREATE INDEX CONCURRENTLY idx$voucher_value_decision_waiting_for_manual_sending
    ON voucher_value_decision
    USING GIST (daterange(valid_from, valid_to, '[]'))
    WHERE status = 'WAITING_FOR_MANUAL_SENDING';
