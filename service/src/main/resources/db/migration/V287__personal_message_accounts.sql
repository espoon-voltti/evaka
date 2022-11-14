INSERT INTO message_account (employee_id)
SELECT DISTINCT employee_id FROM daycare_acl
ON CONFLICT (employee_id) WHERE employee_id IS NOT NULL DO UPDATE SET active = TRUE WHERE NOT message_account.active;
