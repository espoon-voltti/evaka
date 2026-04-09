-- SPDX-FileCopyrightText: 2024 Tampere region
--
-- SPDX-License-Identifier: LGPL-2.1-or-later

INSERT INTO care_area
    (id, name, area_code, sub_cost_center, short_name)
VALUES
    ('e23ed34d-1b35-48f0-b9a5-252d633be5b9', 'Lempäälä', NULL, NULL, 'lempaala')
ON CONFLICT (id) DO
UPDATE SET
    name = EXCLUDED.name,
    area_code = EXCLUDED.area_code,
    sub_cost_center = EXCLUDED.sub_cost_center,
    short_name = EXCLUDED.short_name
WHERE
    care_area.name <> EXCLUDED.name OR
    care_area.area_code IS DISTINCT FROM EXCLUDED.area_code OR
    care_area.sub_cost_center IS DISTINCT FROM EXCLUDED.sub_cost_center OR
    care_area.short_name <> EXCLUDED.short_name;
