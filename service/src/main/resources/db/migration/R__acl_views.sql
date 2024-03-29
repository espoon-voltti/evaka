DROP VIEW IF EXISTS person_acl_view;
DROP VIEW IF EXISTS child_acl_view;
DROP VIEW IF EXISTS daycare_group_acl_view;
DROP VIEW IF EXISTS daycare_acl_view;
DROP VIEW IF EXISTS mobile_device_daycare_acl_view;

DROP FUNCTION IF EXISTS employee_child_group_acl(today date);
DROP FUNCTION IF EXISTS employee_child_daycare_acl(today date);
DROP FUNCTION IF EXISTS child_daycare_acl(today date);

-- Mapping between mobile devices and daycares
CREATE VIEW mobile_device_daycare_acl_view(mobile_device_id, daycare_id) AS (
    -- Unit devices
    SELECT mobile_device.id AS mobile_device_id, unit_id AS daycare_id
    FROM mobile_device
    WHERE unit_id IS NOT NULL

    UNION ALL

    -- Personal devices
    SELECT mobile_device.id AS mobile_device_id, acl.daycare_id
    FROM mobile_device
    JOIN daycare_acl acl
    ON mobile_device.employee_id = acl.employee_id
    WHERE unit_id IS NULL
);

-- Effective ACL role in a daycare for a user
--
-- Includes both inferred roles and actual configured ACL roles
CREATE VIEW daycare_acl_view(employee_id, daycare_id, role) AS (
    SELECT employee_id, daycare_id, role
    FROM daycare_acl

    UNION ALL

    SELECT mobile_device_id AS employee_id, daycare_id, 'MOBILE'
    FROM mobile_device_daycare_acl_view
);

CREATE FUNCTION employee_child_group_acl(today date) RETURNS
TABLE (
    employee_id uuid, child_id uuid, daycare_group_id uuid, daycare_id uuid, role user_role
) AS $$
    SELECT employee_id, child_id, dgp.daycare_group_id, daycare_id, daycare_acl.role
    FROM daycare_group_placement dgp
    JOIN placement dp ON dp.id = dgp.daycare_placement_id
    JOIN daycare_group_acl AS group_acl USING (daycare_group_id)
    JOIN daycare_group ON daycare_group_id = daycare_group.id
    JOIN daycare_acl USING (employee_id, daycare_id)
    WHERE daterange(dgp.start_date, dgp.end_date, '[]') @> today

    UNION ALL

    SELECT employee_id, child_id, group_id AS daycare_group_id, daycare_id, daycare_acl.role
    FROM backup_care bc
    JOIN daycare_group_acl AS group_acl ON bc.group_id = daycare_group_id
    JOIN daycare_group ON daycare_group_id = daycare_group.id
    JOIN daycare_acl USING (employee_id, daycare_id)
    WHERE bc.end_date > today - interval '1 month'
$$ LANGUAGE SQL STABLE;

CREATE FUNCTION child_daycare_acl(today date) RETURNS
TABLE (
    child_id uuid, daycare_id uuid, is_backup_care bool, from_application bool, is_assistance_needed bool
) AS $$
    SELECT pl.child_id, pl.unit_id AS daycare_id, FALSE AS is_backup_care, FALSE as from_application, FALSE AS is_assistance_needed
    FROM placement pl
    WHERE today < pl.end_date + interval '1 month'

    UNION ALL

    SELECT a.child_id, pp.unit_id AS daycare_id, FALSE AS is_backup_care, TRUE as from_application, COALESCE((a.document -> 'careDetails' ->> 'assistanceNeeded') :: BOOLEAN, FALSE) AS is_assistance_needed
    FROM placement_plan pp
    JOIN application a ON pp.application_id = a.id
    WHERE a.status = ANY ('{SENT,WAITING_PLACEMENT,WAITING_CONFIRMATION,WAITING_DECISION,WAITING_MAILING,WAITING_UNIT_CONFIRMATION}'::application_status_type[])

    UNION ALL

    SELECT child_id, bc.unit_id AS daycare_id, TRUE AS is_backup_care, FALSE as from_application, FALSE AS is_assistance_needed
    FROM backup_care bc
    WHERE today < bc.end_date + INTERVAL '1 month'
$$ LANGUAGE SQL STABLE;

CREATE FUNCTION employee_child_daycare_acl(today date) RETURNS
TABLE (
    employee_id uuid, child_id uuid, daycare_id uuid, role user_role
) AS $$
    SELECT employee_id, child_id, pl.unit_id AS daycare_id, role
    FROM placement pl
    JOIN daycare_acl ON pl.unit_id = daycare_acl.daycare_id
    WHERE today < pl.end_date + interval '1 month'

    UNION ALL

    SELECT employee_id, child_id, pp.unit_id AS daycare_id, role
    FROM placement_plan pp
    JOIN application a ON pp.application_id = a.id
    JOIN daycare_acl ON pp.unit_id = daycare_acl.daycare_id
    WHERE a.status = ANY ('{SENT,WAITING_PLACEMENT,WAITING_CONFIRMATION,WAITING_DECISION,WAITING_MAILING,WAITING_UNIT_CONFIRMATION}'::application_status_type[])
    AND NOT (role = 'SPECIAL_EDUCATION_TEACHER' AND coalesce((a.document -> 'careDetails' ->> 'assistanceNeeded')::boolean, FALSE) IS FALSE)

    UNION ALL

    SELECT employee_id, child_id, bc.unit_id AS daycare_id,
       (CASE
            WHEN role != 'UNIT_SUPERVISOR' THEN 'STAFF'
            ELSE role
        END) AS role
    FROM backup_care bc
    JOIN daycare_acl ON bc.unit_id = daycare_acl.daycare_id
    WHERE today < bc.end_date + interval '1 month'
$$ LANGUAGE SQL STABLE;

CREATE VIEW person_acl_view(employee_id, person_id, daycare_id, role) AS (
    SELECT *
    FROM employee_child_daycare_acl(current_date)

    UNION ALL

    SELECT acl.employee_id, guardian_id AS person_id, daycare_id, acl.role
    FROM employee_child_daycare_acl(current_date) acl
    JOIN guardian
    USING (child_id)

    UNION ALL

    SELECT acl.employee_id, head_of_child AS person_id, daycare_id, acl.role
    FROM employee_child_daycare_acl(current_date) acl
    JOIN fridge_child
    USING (child_id)
);
