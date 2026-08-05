-- SPDX-FileCopyrightText: 2017-2026 City of Espoo
--
-- SPDX-License-Identifier: LGPL-2.1-or-later

INSERT INTO assistance_action_option
    (value, name_fi, description_fi, display_order, category, valid_from, valid_to)
VALUES
    ('ASSISTANCE_SERVICE_CHILD', 'Avustamispalvelut yhdelle lapselle', NULL, 10, 'DAYCARE', NULL, NULL),
    ('ASSISTANCE_SERVICE_UNIT', 'Avustamispalvelut yksikköön', NULL, 20, 'DAYCARE', NULL, NULL),
    ('SMALLER_GROUP', 'Pedagogisesti vahvistettu ryhmä', NULL, 30, 'DAYCARE', NULL, NULL),
    ('SPECIAL_GROUP', 'Erityisryhmä', NULL, 40, 'DAYCARE', NULL, NULL),
    ('PERVASIVE_VEO_SUPPORT', 'Laaja-alaisen veon tuki', NULL, 50, 'DAYCARE', NULL, NULL),
    ('RESOURCE_PERSON', 'Resurssihenkilö', NULL, 60, 'DAYCARE', NULL, NULL),
    ('RATIO_DECREASE', 'Suhdeluvun väljennys', NULL, 70, 'DAYCARE', NULL, NULL),
    ('PERIODICAL_VEO_SUPPORT', 'Lisäresurssi hankerahoituksella', NULL, 80, 'DAYCARE', NULL, NULL),
    ('FULL_VEO_SUPPORT_IN_SMALLER_GROUP', 'Kokoaikainen erityisopettajan antama opetus pienryhmässä', NULL, 10, 'PRESCHOOL', '2025-08-01', NULL),
    ('REGULAR_VEO_SUPPORT_PARTIALLY_IN_SMALLER_GROUP', 'Säännöllinen erityisopettajan antama opetus osittain pienryhmässä ja muun opetuksen yhteydessä', NULL, 20, 'PRESCHOOL', '2025-08-01', NULL),
    ('PERSONAL_ASSISTANT', 'Lapsikohtainen avustaja', NULL, 30, 'PRESCHOOL', '2025-08-01', NULL),
    ('ASSISTIVE_DEVICES', 'Apuvälineet', NULL, 40, 'PRESCHOOL', '2025-08-01', NULL),
    ('INTERPRETATION_SERVICES', 'Tulkitsemispalvelut', NULL, 50, 'PRESCHOOL', '2025-08-01', NULL),
    ('PART_TIME_SPECIAL_EDUCATION', 'Osa-aikainen erityisopetus esiopetuksessa', NULL, 55, 'PRESCHOOL', NULL, '2025-07-31')
ON CONFLICT (value) DO
UPDATE SET
    name_fi = EXCLUDED.name_fi,
    description_fi = EXCLUDED.description_fi,
    display_order = EXCLUDED.display_order,
    category = EXCLUDED.category,
    valid_from = EXCLUDED.valid_from,
    valid_to = EXCLUDED.valid_to
WHERE
    assistance_action_option.name_fi IS DISTINCT FROM EXCLUDED.name_fi OR
    assistance_action_option.description_fi IS DISTINCT FROM EXCLUDED.description_fi OR
    assistance_action_option.display_order IS DISTINCT FROM EXCLUDED.display_order OR
    assistance_action_option.category IS DISTINCT FROM EXCLUDED.category OR
    assistance_action_option.valid_from IS DISTINCT FROM EXCLUDED.valid_from OR
    assistance_action_option.valid_to IS DISTINCT FROM EXCLUDED.valid_to;
