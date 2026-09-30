-- SPDX-FileCopyrightText: 2021 City of Turku
--
-- SPDX-License-Identifier: LGPL-2.1-or-later

INSERT INTO assistance_action_option
    (value, name_fi, name_sv, description_fi, description_sv, display_order, category, valid_from, valid_to)
VALUES
    ('10', 'Lapsikohtainen avustaja', 'Personlig assistent', NULL, NULL, 10, 'DAYCARE', NULL, NULL),
    ('20', 'Ryhmäkohtainen avustaja', 'Gruppassistent', NULL, NULL, 20, 'DAYCARE', NULL, NULL),
    ('40', 'Ryhmäkohtainen varhaiskasvatuksen lastenhoitaja (ei mitoitukseen laskettavat)', 'Gruppspecifik barnskötare inom småbarnspedagogik (räknas inte till personaldimensioneringen)', 'Resurssi, mitä ei lasketa suhdelukuun. Pedagogisesti vahvistettu ryhmä.', 'Resurs som inte räknas i proportion till övrig personal. Pedagogiskt förstärkt grupp.', 40, 'DAYCARE', NULL, NULL),
    ('50', 'Pienennetty ryhmä', NULL, 'Ryhmän rakenne: VO, LH ja avustaja tai TH. Lapsiluku 13 kpl, joista kolmella lapsella tuen tarve', NULL, 50, 'DAYCARE', NULL, '2025-07-31'),
    ('60', 'Alueellinen integroitu ryhmä', NULL, 'Ryhmän rakenne: VEO/VO, VO ja kaksi LH. Lapsiluku 14 kpl, joista viidellä tuen tarve', NULL, 60, 'DAYCARE', NULL, '2025-07-31'),
    ('70', 'Erityisryhmä', 'Specialgrupp', 'Sarat-ryhmä. Ryhmän rakenne: VO, SH ja kaksi LH. Lapsiluku 8 kpl, joista kaikilla erityisen tuen tarve', 'Barngruppen Sarat. Gruppens struktur: lärare inom småbarnspedagogik, sjukskötare och två barnskötare. Barnantalet 8, varav alla har behov av särskilt stöd.', 70, 'DAYCARE', NULL, NULL),
    ('80', 'Ryhmän pienennys', 'Minskad gruppstorlek', NULL, NULL, 80, 'DAYCARE', NULL, NULL),
    ('90', 'KV-laki', 'Lag angående specialomsorger om utvecklingsstörda', NULL, NULL, 90, 'DAYCARE', NULL, NULL),
    ('100', 'Varhaiskasvatuksen erityisopettajan konsultaatio', 'Konsultation av speciallärare inom småbarnspedagogik', NULL, NULL, 100, 'DAYCARE', NULL, NULL),
    ('110', 'Varhaiskasvatuksen erityisopettajan osa-aikainen opetus', 'Undervisning på deltid av speciallärare inom småbarnspedagogik', NULL, NULL, 110, 'DAYCARE', NULL, NULL),
    ('120', 'Varhaiskasvatuksen erityisopettajan kokoaikainen opetus', 'Undervisning på heltid av speciallärare inom småbarnspedagogik', NULL, NULL, 120, 'DAYCARE', NULL, NULL),
    ('INTERPRETATION_SERVICE_DAYCARE', 'Tulkitsemispalvelut', 'Tolkningstjänster', NULL, NULL, 130, 'DAYCARE', '2025-08-01', NULL),
    ('CHILD_ASSISTANT', 'Lapsikohtainen avustaja', 'Personlig assistent', NULL, NULL, 10, 'PRESCHOOL', '2025-08-01', NULL),
    ('KV_LAW', 'KV laki', 'Lag angående specialomsorger om utvecklingsstörda', NULL, NULL, 20, 'PRESCHOOL', '2025-08-01', NULL),
    ('SPECIAL_EDUCATION_TEACHER_PARTIAL_SUPPORT_IN_CLASS', 'Säännöllinen erityisopettajan antama opetus osittain pienryhmässä ja muun opetuksen yhteydessä', 'Regelbunden undervisning som ges av speciallärare delvis i grupp och i samband med annan undervisning', NULL, NULL, 30, 'PRESCHOOL', '2025-08-01', NULL),
    ('SPECIAL_EDUCATION_TEACHER_FULL_TIME_SUPPORT_IN_CLASS', 'Kokoaikainen erityisopettajan antama opetus pienryhmässä', 'Undervisning i smågrupp på heltid av speciallärare', NULL, NULL, 40, 'PRESCHOOL', '2025-08-01', NULL),
    ('INTERPRETATION_SERVICE_PRESCHOOL', 'Tulkitsemispalvelut', 'Tolkningstjänster', NULL, NULL, 140, 'PRESCHOOL', '2025-08-01', NULL)

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
