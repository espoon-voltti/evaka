CREATE TYPE fee_decision_status AS ENUM ('DRAFT', 'WAITING_FOR_SENDING', 'WAITING_FOR_MANUAL_SENDING', 'SENT', 'ANNULLED');
CREATE TYPE fee_decision_type AS ENUM ('NORMAL', 'RELIEF_REJECTED', 'RELIEF_PARTLY_ACCEPTED', 'RELIEF_ACCEPTED');
ALTER TABLE fee_decision
    DROP CONSTRAINT exclude$feedecision_no_overlapping_draft,
    DROP CONSTRAINT exclude$feedecision_no_overlapping_sent;
ALTER TABLE fee_decision ALTER COLUMN status TYPE fee_decision_status USING status::fee_decision_status;
ALTER TABLE fee_decision
    ADD CONSTRAINT exclude$feedecision_no_overlapping_draft EXCLUDE USING gist (head_of_family WITH =, daterange(valid_from, valid_to, '[]') WITH &&) WHERE (status = 'DRAFT'),
    ADD CONSTRAINT exclude$feedecision_no_overlapping_sent EXCLUDE USING gist (head_of_family WITH =, daterange(valid_from, valid_to, '[]') WITH &&) WHERE (status = ANY('{SENT, WAITING_FOR_SENDING, WAITING_FOR_MANUAL_SENDING}'::fee_decision_status[]));
ALTER TABLE fee_decision
    ALTER COLUMN decision_type DROP DEFAULT,
    ALTER COLUMN decision_type TYPE fee_decision_type USING decision_type::fee_decision_type,
    ALTER COLUMN decision_type SET DEFAULT 'NORMAL';

CREATE TYPE voucher_value_decision_status AS ENUM ('DRAFT', 'WAITING_FOR_SENDING', 'WAITING_FOR_MANUAL_SENDING', 'SENT', 'ANNULLED');
ALTER TABLE voucher_value_decision
    DROP CONSTRAINT exclude$voucher_value_decision_no_overlapping_draft,
    DROP CONSTRAINT exclude$voucher_value_decision_no_overlapping_sent;
ALTER TABLE voucher_value_decision ALTER COLUMN status TYPE voucher_value_decision_status USING status::voucher_value_decision_status;
ALTER TABLE voucher_value_decision
    ADD CONSTRAINT exclude$voucher_value_decision_no_overlapping_draft EXCLUDE USING gist (head_of_family WITH =, daterange(valid_from, valid_to, '[]') WITH &&) WHERE (status = 'DRAFT'),
    ADD CONSTRAINT exclude$voucher_value_decision_no_overlapping_sent EXCLUDE USING gist (head_of_family WITH =, daterange(valid_from, valid_to, '[]') WITH &&) WHERE (status = ANY('{SENT, WAITING_FOR_SENDING, WAITING_FOR_MANUAL_SENDING}'::voucher_value_decision_status[]));

CREATE TYPE invoice_status AS ENUM ('DRAFT', 'WAITING_FOR_SENDING', 'SENT', 'CANCELED');
ALTER TABLE invoice ALTER COLUMN status TYPE invoice_status USING status::invoice_status;

CREATE TYPE income_effect AS ENUM ('INCOME', 'MAX_FEE_ACCEPTED', 'INCOMPLETE', 'NOT_AVAILABLE');
ALTER TABLE income
    ALTER COLUMN effect DROP DEFAULT,
    ALTER COLUMN effect TYPE income_effect USING effect::income_effect,
    ALTER COLUMN effect SET DEFAULT 'INCOME';

CREATE TYPE fee_alteration_type AS ENUM ('DISCOUNT', 'INCREASE', 'RELIEF');
ALTER TABLE fee_alteration ALTER COLUMN type TYPE fee_alteration_type USING type::fee_alteration_type;
