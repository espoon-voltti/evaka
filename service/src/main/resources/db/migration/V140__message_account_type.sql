CREATE TYPE message_account_type AS ENUM ('PERSONAL', 'GROUP', 'CITIZEN');

ALTER TABLE message_account
ADD COLUMN type message_account_type
GENERATED ALWAYS AS (
    CASE
        WHEN employee_id IS NOT NULL THEN 'PERSONAL'::message_account_type
        WHEN daycare_group_id IS NOT NULL THEN 'GROUP'::message_account_type
        ELSE 'CITIZEN'::message_account_type
    END
) STORED;
