-- SPDX-FileCopyrightText: 2021 City of Turku
--
-- SPDX-License-Identifier: LGPL-2.1-or-later

INSERT INTO assistance_action_option
    (value, name_fi, description_fi, display_order, category)
VALUES
    ('10', 'Lapsikohtainen avustaja', NULL, 10, 'DAYCARE'),
    ('20', 'Ryhmäkohtainen avustaja', NULL, 20, 'DAYCARE'),
    ('40', 'Ryhmäkohtainen varhaiskasvatuksen lastenhoitaja (ei mitoitukseen laskettavat)', 'Resurssi, mitä ei lasketa suhdelukuun. Pedagogisesti vahvistettu ryhmä.', 40, 'DAYCARE'),
    ('50', 'Pienennetty ryhmä', 'Ryhmän rakenne: VO, LH ja avustaja tai TH. Lapsiluku 13 kpl, joista kolmella lapsella tuen tarve', 50, 'DAYCARE'),
    ('60', 'Alueellinen integroitu ryhmä', 'Ryhmän rakenne: VEO/VO, VO ja kaksi LH. Lapsiluku 14 kpl, joista viidellä tuen tarve', 60, 'DAYCARE'),
    ('70', 'Erityisryhmä', 'Sarat-ryhmä. Ryhmän rakenne: VO, SH ja kaksi LH. Lapsiluku 8 kpl, joista kaikilla erityisen tuen tarve', 70, 'DAYCARE'),
    ('80', 'Ryhmän pienennyspäätös', 'Tehdään harkinnanvaraisesti', 80, 'DAYCARE'),
    ('90', 'KV-laki', NULL, 90, 'DAYCARE'),
    ('100', 'Varhaiskasvatuksen erityisopettajan konsultaatio', NULL, 100, 'DAYCARE'),
    ('110', 'Varhaiskasvatuksen erityisopettajan osa-aikainen opetus', NULL, 110, 'DAYCARE'),
    ('120', 'Varhaiskasvatuksen erityisopettajan kokoaikainen opetus', NULL, 120, 'DAYCARE')

ON CONFLICT (value) DO
UPDATE SET
    name_fi = EXCLUDED.name_fi,
    description_fi = EXCLUDED.description_fi,
    display_order = EXCLUDED.display_order,
    category = EXCLUDED.category
WHERE
    assistance_action_option.name_fi <> EXCLUDED.name_fi OR
    assistance_action_option.description_fi IS DISTINCT FROM EXCLUDED.description_fi OR
    assistance_action_option.display_order <> EXCLUDED.display_order OR
    assistance_action_option.category <> EXCLUDED.category;

