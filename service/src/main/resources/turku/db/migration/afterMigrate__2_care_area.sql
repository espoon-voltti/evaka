-- SPDX-FileCopyrightText: 2023-2025 City of Turku
--
-- SPDX-License-Identifier: LGPL-2.1-or-later

INSERT INTO public.care_area (id, name, area_code, sub_cost_center, short_name)
VALUES
    ('3d21730c-9558-11ec-9cd7-9b2bb9834b1b', 'Eteläinen varhaiskasvatusalue', '1', NULL, 'etelainen'),
    ('3d21729e-9558-11ec-9cd6-5bb13cfcb1db', 'Itäinen varhaiskasvatusalue', '2', NULL, 'itainen'),
    ('3d217384-9558-11ec-9cd8-f74b58d05789', 'Läntinen varhaiskasvatusalue', '3', NULL, 'lantinen'),
    ('3d21700a-9558-11ec-9cd5-43d5752dad51', 'Pohjoinen varhaiskasvatusalue', '4', NULL, 'pohjoinen'),
    ('10460a0e-9563-11ec-8377-6f9dcbfb5285', 'Ruotsinkielinen varhaiskasvatusalue', '5', NULL, 'ruotsinkielinen'),
    ('79694e7e-544e-11ed-9f76-ab4476885861', 'Palveluseteli', '6', NULL, 'palveluseteli')
ON CONFLICT (id) DO NOTHING;