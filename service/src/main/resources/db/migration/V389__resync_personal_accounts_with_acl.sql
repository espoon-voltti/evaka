UPDATE message_account SET active = false
WHERE active AND type = 'PERSONAL' AND employee_id IS NOT NULL AND NOT EXISTS (
    SELECT 1 FROM daycare_acl WHERE daycare_acl.employee_id = message_account.employee_id
);

INSERT INTO message_account (employee_id, type)
SELECT DISTINCT employee_id, 'PERSONAL'::message_account_type
FROM daycare_acl acl
WHERE NOT EXISTS (
    SELECT 1
    FROM message_account ma
    WHERE ma.employee_id = acl.employee_id AND ma.active
)
ON CONFLICT (employee_id) WHERE employee_id IS NOT NULL DO UPDATE SET active = true;
