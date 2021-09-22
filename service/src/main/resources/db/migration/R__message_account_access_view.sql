DROP VIEW IF EXISTS message_account_access_view;

CREATE VIEW message_account_access_view(account_id, employee_id, person_id) AS (
    SELECT acc.id AS account_id, NULL AS employee_id, person.id AS person_id
    FROM message_account acc
        JOIN person ON acc.person_id = person.id
    WHERE acc.active = TRUE

    UNION

    SELECT acc.id AS account_id, employee.id AS employee_id, NULL AS person_id
    FROM message_account acc
        JOIN employee ON acc.employee_id = employee.id
    WHERE acc.active = TRUE

    UNION

    SELECT acc.id as account_id, acl.employee_id, NULL AS person_id
    FROM message_account acc
        JOIN daycare_group dg ON acc.daycare_group_id = dg.id
        JOIN daycare_acl acl ON acl.daycare_id = dg.daycare_id AND acl.role = 'UNIT_SUPERVISOR'
    WHERE acc.active = TRUE

    UNION

    SELECT acc.id as account_id, gacl.employee_id, NULL AS person_id
    FROM message_account acc
        JOIN daycare_group_acl gacl ON gacl.daycare_group_id = acc.daycare_group_id
    WHERE acc.active = TRUE
);
