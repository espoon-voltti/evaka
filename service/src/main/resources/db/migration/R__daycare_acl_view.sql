DROP VIEW IF EXISTS daycare_acl_view;

CREATE OR REPLACE VIEW daycare_acl_view (
    "unit_name",
    "manager_email",
    "authorized_person_email",
    "unit_id",
    "authorized_person_id"
) AS
SELECT
    d.name AS "unit_name",
    unit_manager.email AS "manager_email",
    p.email AS "authorized_person_email",
    daycare_acl.daycare_id AS "unit_id",
    daycare_acl.employee_id AS "authorized_person_id"
FROM
    daycare_acl
    LEFT OUTER JOIN employee p ON daycare_acl.employee_id = p.id
    LEFT OUTER JOIN daycare d ON daycare_acl.daycare_id = d.id
    LEFT OUTER JOIN unit_manager ON d.unit_manager_id = unit_manager.id
ORDER BY
    unit_name;