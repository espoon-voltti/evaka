-- SPDX-FileCopyrightText: 2017-2026 City of Espoo
--
-- SPDX-License-Identifier: LGPL-2.1-or-later

INSERT INTO care_area
    (id, name, area_code, sub_cost_center, short_name)
VALUES
    ('a01b0e03-b86e-4cbc-a744-6a35473b9628', 'Leppävaara (itä)', 249, '01', 'leppavaara-ita'),
    ('801a6cc7-e8a5-4279-b192-4e8192d82c18', 'Leppävaara (länsi)', 249, '01', 'leppavaara-lansi'),
    ('aede1c92-39a0-47b3-9f7a-45b4355f6c87', 'Tapiola', 250, '02', 'tapiola'),
    ('7f08ec20-3843-466e-807e-a8cddf5d5605', 'Matinkylä-Olari', 251, '03', 'matinkyla-olari'),
    ('d60f3dae-e164-4ad0-b5ec-af9c8c35a586', 'Espoonlahti', 252, '04', 'espoonlahti'),
    ('7119009f-ec26-45d2-be61-d3f802c1d1e5', 'Espoon keskus (eteläinen)', 253, '05', 'espoon-keskus-etela'),
    ('10842fdc-5750-447d-9b6b-50a1ca66864c', 'Espoon keskus (pohjoinen)', 253, '05', 'espoon-keskus-pohjoinen'),
    ('2daff112-788e-11e9-bd0e-07d5525c2890', 'Svenska bildningstjänster', 254, NULL, 'svenska-bildningstjanster'),
    ('b4c4e4ce-123c-11f1-8104-af3da164f107', 'Palveluseteli muut kunnat', NULL, NULL, 'palveluseteli-muut-kunnat')
ON CONFLICT (id) DO
UPDATE SET
    name = EXCLUDED.name,
    area_code = EXCLUDED.area_code,
    sub_cost_center = EXCLUDED.sub_cost_center,
    short_name = EXCLUDED.short_name
WHERE
    care_area.name IS DISTINCT FROM EXCLUDED.name OR
    care_area.area_code IS DISTINCT FROM EXCLUDED.area_code OR
    care_area.sub_cost_center IS DISTINCT FROM EXCLUDED.sub_cost_center OR
    care_area.short_name IS DISTINCT FROM EXCLUDED.short_name;
