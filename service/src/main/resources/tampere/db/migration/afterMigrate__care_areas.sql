-- SPDX-FileCopyrightText: 2021 City of Tampere
--
-- SPDX-License-Identifier: LGPL-2.1-or-later

INSERT INTO care_area
    (id, name, area_code, sub_cost_center, short_name)
VALUES
    ('6529e31e-9777-11eb-ba88-33a923255570', 'Etelä', 1, NULL, 'etela'),
    ('6529f5a2-9777-11eb-ba89-cfcda122ed3b', 'Itä', 2, NULL, 'ita'),
    ('6529f6ce-9777-11eb-ba8a-8f6495ec5104', 'Länsi', 3, NULL, 'lansi'),
    ('df7ac86c-120e-4586-818a-edb3f80349de', 'Keskusta', 4, NULL, 'keskusta')
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
