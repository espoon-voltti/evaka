DROP VIEW IF EXISTS child_acl_view;

CREATE OR REPLACE VIEW child_acl_view (
    "employee_id",
    "child_id",
    "role"
) AS
SELECT
    acl.employee_id AS "employee_id",
    ch.id AS "child_id",
    acl.role AS "role"
FROM person ch
    LEFT JOIN placement pl ON pl.child_id = ch.id AND pl.end_date > current_date - INTERVAL '1 month'
    LEFT JOIN application a ON a.child_id = ch.id AND a.status = ANY ('{SENT,WAITING_PLACEMENT,WAITING_CONFIRMATION,WAITING_DECISION,WAITING_MAILING,WAITING_UNIT_CONFIRMATION, ACTIVE}'::application_status_type[])
    LEFT JOIN placement_plan pp ON pp.application_id = a.id
    JOIN daycare_acl acl ON acl.daycare_id = pl.unit_id OR acl.daycare_id = pp.unit_id
UNION SELECT
    acl.employee_id AS "employee_id",
    ch.id AS "child_id",
    CASE
        WHEN acl.role = 'UNIT_SUPERVISOR'::public.user_role THEN 'UNIT_SUPERVISOR'::public.user_role
        ELSE 'STAFF'::public.user_role
    END AS "role"
FROM person ch
    LEFT JOIN backup_care bc ON bc.child_id = ch.id AND bc.end_date > current_date - INTERVAL '1 month'
    JOIN daycare_acl acl ON acl.daycare_id = bc.unit_id
ORDER BY
    role;
