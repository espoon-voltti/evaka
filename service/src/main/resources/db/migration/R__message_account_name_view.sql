DROP VIEW IF EXISTS message_account_name_view;
CREATE VIEW message_account_name_view(
    id,
    account_name
) AS (
SELECT
    acc.id,
    COALESCE(
        nullif(concat_ws(' - ', d.name, dg.name), ''),
        nullif(concat_ws(' ', p.last_name, p.first_name), ''),
        concat_ws(' ', e.last_name, coalesce(e.preferred_first_name, e.first_name))
    )
FROM message_account acc
    LEFT JOIN daycare_group dg ON acc.daycare_group_id = dg.id
    LEFT JOIN daycare d ON dg.daycare_id = d.id
    LEFT JOIN employee e ON acc.employee_id = e.id
    LEFT JOIN person p ON acc.person_id = p.id
);

DROP VIEW IF EXISTS message_account_view;
CREATE VIEW message_account_view(id, type, name) AS (
    SELECT
        acc.id,
        acc.type,
        CASE acc.type
            WHEN 'GROUP' THEN (
                SELECT d.name || ' - ' || dg.name
                FROM daycare_group dg
                JOIN daycare d ON dg.daycare_id = d.id
                WHERE dg.id = acc.daycare_group_id
            )
            WHEN 'PERSONAL' THEN (
                SELECT e.last_name || ' ' || coalesce(e.preferred_first_name, e.first_name)
                FROM employee e
                WHERE e.id = acc.employee_id
            )
            WHEN 'CITIZEN' THEN (
                SELECT p.last_name || ' ' || p.first_name
                FROM person p
                WHERE p.id = acc.person_id
            )
        END
    FROM message_account acc
);
