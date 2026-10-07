-- SPDX-FileCopyrightText: 2017-2026 City of Espoo
--
-- SPDX-License-Identifier: LGPL-2.1-or-later

INSERT INTO assistance_action_option
    (value, name_fi, name_sv, description_fi, description_sv, display_order, category, valid_from, valid_to)
VALUES
    ('ASSISTANCE_SERVICE_CHILD', 'Lapsikohtainen avustamispalvelu', 'Stödtjänster för ett enskilt barn', NULL, NULL, 10, 'DAYCARE', NULL, NULL),
    ('ASSISTANCE_SERVICE_UNIT', 'Ryhmäkohtaiset avustamispalvelut', 'Stödtjänster för en barngrupp', NULL, NULL, 20, 'DAYCARE', NULL, NULL),
    ('SMALLER_GROUP', 'Pienennetty ryhmä', 'Mindre grupp', NULL, NULL, 30, 'DAYCARE', NULL, NULL),
    ('SPECIAL_GROUP', 'Erityisryhmä', 'Specialgrupp', NULL, NULL, 40, 'DAYCARE', NULL, NULL),
    ('PERVASIVE_VEO_SUPPORT', 'Varhaiskasvatuksen erityisopettajan tuki', 'Stöd av en speciallärare inom småbarnspedagogik', NULL, NULL, 50, 'DAYCARE', NULL, NULL),
    ('RESOURCE_PERSON', 'Tuen lastenhoitaja', 'Stödets barnskötare', NULL, NULL, 60, 'DAYCARE', NULL, NULL),
    ('RATIO_DECREASE', 'Ryhmäkoon pienennys', 'Minskning av gruppstorlek', NULL, NULL, 70, 'DAYCARE', NULL, NULL),
    ('PERIODICAL_VEO_SUPPORT', 'Lisäresurssi hankerahoituksella', 'Tilläggsresurs med projektfinansiering', NULL, NULL, 80, 'DAYCARE', NULL, NULL),
    ('FULL_VEO_SUPPORT_IN_SMALLER_GROUP', 'Kokoaikainen erityisopettajan antama opetus pienryhmässä', 'Undervisning som ges av speciallärare i smågrupp på heltid', NULL, NULL, 10, 'PRESCHOOL', '2025-08-01', NULL),
    ('REGULAR_VEO_SUPPORT_PARTIALLY_IN_SMALLER_GROUP', 'Säännöllinen erityisopettajan antama opetus osittain pienryhmässä ja muun opetuksen yhteydessä', 'Regelbunden undervisning som ges av en speciallärare delvis i smågrupp och i samband med annan undervisning', NULL, NULL, 20, 'PRESCHOOL', '2025-08-01', NULL),
    ('PERSONAL_ASSISTANT', 'Lapsikohtainen avustaja', 'Personlig assistent', NULL, NULL, 30, 'PRESCHOOL', '2025-08-01', NULL),
    ('ASSISTIVE_DEVICES', 'Apuvälineet', 'Hjälpmedel', NULL, NULL, 40, 'PRESCHOOL', '2025-08-01', NULL),
    ('INTERPRETATION_SERVICES', 'Tulkitsemispalvelut', 'Tolkningstjänster', NULL, NULL, 50, 'PRESCHOOL', '2025-08-01', NULL),
    ('PART_TIME_SPECIAL_EDUCATION', 'Osa-aikainen erityisopetus esiopetuksessa', NULL, NULL, NULL, 55, 'PRESCHOOL', NULL, '2025-07-31')
ON CONFLICT (value) DO
UPDATE SET
    name_fi = EXCLUDED.name_fi,
    name_sv = EXCLUDED.name_sv,
    description_fi = EXCLUDED.description_fi,
    description_sv = EXCLUDED.description_sv,
    display_order = EXCLUDED.display_order,
    category = EXCLUDED.category,
    valid_from = EXCLUDED.valid_from,
    valid_to = EXCLUDED.valid_to
WHERE
    assistance_action_option.name_fi IS DISTINCT FROM EXCLUDED.name_fi OR
    assistance_action_option.name_sv IS DISTINCT FROM EXCLUDED.name_sv OR
    assistance_action_option.description_fi IS DISTINCT FROM EXCLUDED.description_fi OR
    assistance_action_option.description_sv IS DISTINCT FROM EXCLUDED.description_sv OR
    assistance_action_option.display_order IS DISTINCT FROM EXCLUDED.display_order OR
    assistance_action_option.category IS DISTINCT FROM EXCLUDED.category OR
    assistance_action_option.valid_from IS DISTINCT FROM EXCLUDED.valid_from OR
    assistance_action_option.valid_to IS DISTINCT FROM EXCLUDED.valid_to;
