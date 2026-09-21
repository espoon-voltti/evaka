-- SPDX-FileCopyrightText: 2021 City of Turku
--
-- SPDX-License-Identifier: LGPL-2.1-or-later

INSERT INTO assistance_action_option
    (value, name_fi, name_sv, description_fi, description_sv, display_order, category)
VALUES
    ('10', 'Lapsikohtainen avustaja', 'Barnspecifik assistent', NULL, NULL, 10, 'DAYCARE'),
    ('20', 'Ryhmäkohtainen avustaja', 'Gruppspecifik assistent', NULL, NULL, 20, 'DAYCARE'),
    ('40', 'Ryhmäkohtainen varhaiskasvatuksen lastenhoitaja (ei mitoitukseen laskettavat)', 'Gruppspecifik barnskötare inom småbarnspedagogik (räknas inte in i dimensioneringen)', 'Resurssi, mitä ei lasketa suhdelukuun. Pedagogisesti vahvistettu ryhmä.', 'Resurs som inte räknas in i relationstalet. Pedagogiskt förstärkt grupp.', 40, 'DAYCARE'),
    ('50', 'Pienennetty ryhmä', 'Förminskad grupp', 'Ryhmän rakenne: VO, LH ja avustaja tai TH. Lapsiluku 13 kpl, joista kolmella lapsella tuen tarve', 'Gruppens sammansättning: VO, LH och assistent eller TH. Antal barn 13, varav tre barn har stödbehov', 50, 'DAYCARE'),
    ('60', 'Alueellinen integroitu ryhmä', 'Regional integrerad grupp', 'Ryhmän rakenne: VEO/VO, VO ja kaksi LH. Lapsiluku 14 kpl, joista viidellä tuen tarve', 'Gruppens sammansättning: VEO/VO, VO och två LH. Antal barn 14, varav fem har stödbehov', 60, 'DAYCARE'),
    ('70', 'Erityisryhmä', 'Specialgrupp', 'Sarat-ryhmä. Ryhmän rakenne: VO, SH ja kaksi LH. Lapsiluku 8 kpl, joista kaikilla erityisen tuen tarve', 'Sarat-grupp. Gruppens sammansättning: VO, SH och två LH. Antal barn 8, varav alla har behov av särskilt stöd', 70, 'DAYCARE'),
    ('80', 'Ryhmän pienennys', 'Förminskning av gruppen', NULL, NULL, 80, 'DAYCARE'),
    ('90', 'KV-laki', 'KV-lagen', NULL, NULL, 90, 'DAYCARE'),
    ('100', 'Varhaiskasvatuksen erityisopettajan konsultaatio', 'Konsultation av speciallärare inom småbarnspedagogik', NULL, NULL, 100, 'DAYCARE'),
    ('110', 'Varhaiskasvatuksen erityisopettajan osa-aikainen opetus', 'Deltidsundervisning av speciallärare inom småbarnspedagogik', NULL, NULL, 110, 'DAYCARE'),
    ('120', 'Varhaiskasvatuksen erityisopettajan kokoaikainen opetus', 'Heltidsundervisning av speciallärare inom småbarnspedagogik', NULL, NULL, 120, 'DAYCARE')

ON CONFLICT (value) DO
UPDATE SET
    name_fi = EXCLUDED.name_fi,
    name_sv = EXCLUDED.name_sv,
    description_fi = EXCLUDED.description_fi,
    description_sv = EXCLUDED.description_sv,
    display_order = EXCLUDED.display_order,
    category = EXCLUDED.category
WHERE
    assistance_action_option.name_fi IS DISTINCT FROM EXCLUDED.name_fi OR
    assistance_action_option.name_sv IS DISTINCT FROM EXCLUDED.name_sv OR
    assistance_action_option.description_fi IS DISTINCT FROM EXCLUDED.description_fi OR
    assistance_action_option.description_sv IS DISTINCT FROM EXCLUDED.description_sv OR
    assistance_action_option.display_order IS DISTINCT FROM EXCLUDED.display_order OR
    assistance_action_option.category IS DISTINCT FROM EXCLUDED.category;
