INSERT INTO message_account (daycare_group_id)
SELECT id
FROM daycare_group
ON CONFLICT DO NOTHING;

INSERT INTO message_account (person_id)
SELECT id
FROM person
ON CONFLICT DO NOTHING;

INSERT INTO message_account (employee_id)
SELECT id
FROM employee e
WHERE EXISTS(
              SELECT 1
              FROM daycare_acl acl
              WHERE acl.employee_id = e.id
                AND acl.role = 'UNIT_SUPERVISOR'
          )
ON CONFLICT DO NOTHING;
