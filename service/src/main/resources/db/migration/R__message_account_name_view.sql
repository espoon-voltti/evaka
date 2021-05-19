CREATE OR REPLACE VIEW message_account_name_view(
    id,
    account_name
) AS (
SELECT
    acc.id,
    COALESCE(
        nullif(concat_ws(' - ', d.name, dg.name), ''),
        nullif(concat_ws(' ', p.last_name, p.first_name), ''),
        nullif(concat_ws(' ', e.last_name, e.first_name), '')
    )
FROM message_account acc
    LEFT JOIN daycare_group dg ON acc.daycare_group_id = dg.id
    LEFT JOIN daycare d ON dg.daycare_id = d.id
    LEFT JOIN employee e ON acc.employee_id = e.id
    LEFT JOIN person p ON acc.person_id = p.id
);
