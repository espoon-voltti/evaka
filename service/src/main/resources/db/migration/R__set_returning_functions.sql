DROP FUNCTION IF EXISTS realized_placement_one(today date);
DROP FUNCTION IF EXISTS realized_placement_all(today date);

-- Returns 0-1 placement rows per child.
-- If a child is in backup care, it has priority and only one row is returned.
CREATE FUNCTION realized_placement_one(today date) RETURNS
TABLE (
    child_id uuid, unit_id uuid, is_backup boolean,
    -- these come from the normal placement, even in backup care rows
    placement_id uuid, placement_type placement_type,
    -- these two ids are nullable
    group_id uuid, backup_care_id uuid
) AS $$
    SELECT
        p.child_id, (CASE WHEN bc.id IS NULL THEN p.unit_id ELSE bc.unit_id END) AS unit_id, (bc.id IS NOT NULL) AS is_backup,
        p.id AS placement_id, p.type AS placement_type,
        (CASE WHEN bc.id IS NULL THEN dgp.daycare_group_id ELSE bc.group_id END) AS group_id, bc.id AS backup_care_id
    FROM placement p
    LEFT JOIN backup_care bc
    ON p.child_id = bc.child_id AND daterange(bc.start_date, bc.end_date, '[]') @> today
    LEFT JOIN daycare_group_placement dgp
    ON p.id = dgp.daycare_placement_id AND daterange(dgp.start_date, dgp.end_date, '[]') @> today
    WHERE daterange(p.start_date, p.end_date, '[]') @> today
$$ LANGUAGE SQL STABLE;

-- Returns 0-2 placement rows per child.
-- If a child is in backup care, one row is returned for the normal placement and another one for backup care.
CREATE FUNCTION realized_placement_all(today date) RETURNS
TABLE (
    child_id uuid, unit_id uuid, is_backup boolean,
    -- these come from the normal placement, even in backup care rows
    placement_id uuid, placement_type placement_type,
    -- these two ids are nullable
    group_id uuid, backup_care_id uuid
) AS $$
    SELECT
        child_id, unit_id, FALSE AS is_backup,
        p.id AS placement_id, p.type AS placement_type,
        dgp.daycare_group_id AS group_id, NULL AS backup_care_id
    FROM placement p
    LEFT JOIN daycare_group_placement dgp
    ON p.id = dgp.daycare_placement_id AND daterange(dgp.start_date, dgp.end_date, '[]') @> today
    WHERE daterange(p.start_date, p.end_date, '[]') @> today

    UNION ALL

    SELECT
        child_id, bc.unit_id, TRUE AS is_backup,
        p.id AS placement_id, p.type AS placement_type,
        bc.group_id, bc.id AS backup_care_id
    FROM backup_care bc
    JOIN placement p USING (child_id)
    WHERE daterange(bc.start_date, bc.end_date, '[]') @> today
    AND daterange(p.start_date, p.end_date, '[]') @> today
$$ LANGUAGE SQL STABLE;
