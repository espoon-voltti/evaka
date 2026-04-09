-- SPDX-FileCopyrightText: 2024 Tampere region
--
-- SPDX-License-Identifier: LGPL-2.1-or-later

INSERT INTO assistance_action_option
    (value, name_fi, description_fi, display_order, category, valid_from, valid_to)
VALUES
    ('10', 'Avustajapalvelut', 'Lapsen ryhmässä työskentelee ryhmäavustaja tai lapsella on henkilökohtainen avustaja.', 30, 'DAYCARE', NULL, NULL),
    ('40', 'Henkilökuntalisäys tai -muutos', 'Henkilökuntalisäys: Lapsen ryhmässä työskentelee lain vaatimaan resurssia enemmän varhaiskasvatuksen lastenhoitajia/sosionomeja/opettajia. Henkilökuntamuutos: Ryhmää on vahvistettu pedagogisesti siten, että henkilöstöön kuuluu kaksi varhaiskasvatuksen opettajaa. Mikäli lapsi on erityisryhmässä tai integroidussa ryhmässä, tätä vaihtoehtoa ei valita.', 40, 'DAYCARE', NULL, NULL),
    ('60', 'Osa-aikainen erityisopetus', 'Lapsi saa osa-aikaista erityisopetusta.', 10, 'DAYCARE', NULL, NULL),
    ('65', 'Kokoaikainen erityisopetus', NULL, 15, 'DAYCARE', '2025-08-01'::date, NULL),
    ('70', 'Erityisopettajan konsultaatio', 'Lapsen ryhmän henkilökunta saa erityisopettajan konsultaatiota.', 20, 'DAYCARE', NULL, NULL),
    ('80', 'Tulkitsemispalvelut', 'Lapsi saa tulkitsemispalveluita kuulo- ja/tai näkövammansa vuoksi. Huoltajien kanssa käytettävät tulkkipalvelut eivät sisälly tähän.', 60, 'DAYCARE', NULL, NULL),
    ('100', 'Ryhmän pienennys', 'Ryhmän pienennys rakenteellisen tuen muotona eli lapsiryhmää pienennetään, jotta lasten tuki toteutuu tarkoituksenmukaisesti. Kirjaa myös tuen kerroin kohtaan Tuen tarve.', 50, 'DAYCARE', NULL, NULL),

    ('200', 'Kokoaikainen erityisopettajan antama opetus pienryhmässä', NULL, 200, 'PRESCHOOL', '2025-08-01'::date, NULL),
    ('210', 'Säännöllinen erityisopettajan antama opetus osittain pienryhmässä ja muun opetuksen yhteydessä', NULL, 210, 'PRESCHOOL', '2025-08-01'::date, NULL),
    ('220', 'Erityisopettajan antamaopetus muun opetuksen yhteydessä', NULL, 220, 'PRESCHOOL', '2025-08-01'::date, NULL),
    ('230', 'Avustajapalvelut', 'Lapsen ryhmässä työskentelee ryhmäavustaja tai lapsella on henkilökohtainen avustaja.', 230, 'PRESCHOOL', NULL, NULL),
    ('240', 'Apuvälineet', NULL, 240, 'PRESCHOOL', '2025-08-01'::date, NULL),
    ('250', 'Lapsikohtaiset tukitoimet', NULL, 250, 'PRESCHOOL', '2025-08-01'::date, NULL),
    ('260', 'Tulkitsemispalvelut', 'Lapsi saa tulkitsemispalveluita kuulo- ja/tai näkövammansa vuoksi. Huoltajien kanssa käytettävät tulkkipalvelut eivät sisälly tähän.', 260, 'PRESCHOOL', NULL, NULL)
ON CONFLICT (value) DO
UPDATE SET
    name_fi = EXCLUDED.name_fi,
    description_fi = EXCLUDED.description_fi,
    display_order = EXCLUDED.display_order,
    category = EXCLUDED.category,
    valid_from = EXCLUDED.valid_from,
    valid_to = EXCLUDED.valid_to
WHERE
    assistance_action_option.name_fi <> EXCLUDED.name_fi OR
    assistance_action_option.description_fi IS DISTINCT FROM EXCLUDED.description_fi OR
    assistance_action_option.display_order IS DISTINCT FROM EXCLUDED.display_order OR
    assistance_action_option.category <> EXCLUDED.category OR
    assistance_action_option.valid_from IS DISTINCT FROM EXCLUDED.valid_from OR
    assistance_action_option.valid_to IS DISTINCT FROM EXCLUDED.valid_to;
