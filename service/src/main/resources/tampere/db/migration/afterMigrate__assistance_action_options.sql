-- SPDX-FileCopyrightText: 2021 City of Tampere
--
-- SPDX-License-Identifier: LGPL-2.1-or-later

INSERT INTO assistance_action_option
    (value, name_fi, description_fi, display_order, category, valid_from, valid_to)
VALUES
    ('10', 'Avustajapalvelut', 'Lapsen ryhmässä työskentelee ryhmäavustaja tai lapsella on henkilökohtainen avustaja.', 40, 'DAYCARE', NULL, NULL),
    ('20', 'Erho-yksikkö', NULL, 30, 'DAYCARE', NULL, NULL),
    ('30', 'Varhaiskasvatuksen erityisryhmä', 'Lapsi on erityisryhmässä.', 20, 'DAYCARE', NULL, NULL),
    ('40', 'Henkilökuntalisäys tai -muutos', 'Henkilökuntalisäys: Lapsen ryhmässä työskentelee lain vaatimaan resurssia enemmän varhaiskasvatuksen lastenhoitajia/sosionomeja/opettajia. Henkilökuntamuutos: Ryhmää on vahvistettu pedagogisesti siten, että henkilöstöön kuuluu kaksi varhaiskasvatuksen opettajaa. Mikäli lapsi on erityisryhmässä tai integroidussa ryhmässä, tätä vaihtoehtoa ei valita.', 60, 'DAYCARE', NULL, NULL),
    ('50', 'Integroitu varhaiskasvatusryhmä', 'Lapsi on integroidussa varhaiskasvatusryhmässä.', 10, 'DAYCARE', NULL, NULL),
    ('57', 'Kokoaikainen erityisopetus', 'Lapsen ryhmässä työskentelee yhtenä työntekijänä varhaiskasvatuksen erityisopettaja.', 90, 'DAYCARE', NULL, NULL),
    ('60', 'Osa-aikainen erityisopetus', 'Lapsi saa osa-aikaista erityisopetusta.', 80, 'DAYCARE', NULL, NULL),
    ('70', 'Erityisopettajan konsultaatio', 'Lapsen ryhmän henkilökunta saa erityisopettajan konsultaatiota.', 70, 'DAYCARE', NULL, NULL),
    ('80', 'Tulkitsemispalvelut', 'Lapsi saa tulkitsemispalveluita kuulo- ja/tai näkövammansa vuoksi. Huoltajien kanssa käytettävät tulkkipalvelut eivät sisälly tähän.', 100, 'DAYCARE', NULL, NULL),
    ('100', 'Ryhmäkoon pienennys', 'Ryhmän pienennys rakenteellisen tuen muotona eli lapsiryhmää pienennetään, jotta lasten tuki toteutuu tarkoituksenmukaisesti. Kirjaa myös tuen kerroin kohtaan Tuen tarve.', 50, 'DAYCARE', NULL, NULL),
    ('110', 'Apuvälineet', NULL, 110, 'DAYCARE', '2025-08-01'::date, NULL),

    ('35', 'Esiopetuksen erityisryhmä', NULL, 210, 'PRESCHOOL', NULL, NULL),
    ('55', 'Integroitu esiopetusryhmä', 'Lapsi on integroidussa esiopetusryhmässä.', 200, 'PRESCHOOL', NULL, NULL),
    ('200', 'Erho', NULL, 220, 'PRESCHOOL', NULL, NULL),
    ('210', 'Erityisopettajan antama opetus muun opetuksen yhteydessä', 'Ryhmäkohtainen tukimuoto', 230, 'PRESCHOOL', '2025-08-01'::date, NULL),
    ('220', 'Säännöllinen erityisopettajan antama opetus osittain pienryhmässä ja muun opetuksen yhteydessä', 'Lapsikohtainen tukitoimi', 240, 'PRESCHOOL', '2025-08-01'::date, NULL),
    ('230', 'Kokoaikainen erityisopettajan antama opetus pienryhmässä', 'Lapsikohtainen tukitoimi', 240, 'PRESCHOOL', '2025-08-01'::date, NULL),
    ('240', 'Lapsikohtainen avustaja', 'Lapsikohtainen tukitoimi', 250, 'PRESCHOOL', '2025-08-01'::date, NULL),
    ('250', 'Tulkitsemispalvelut', 'Lapsikohtainen tukitoimi', 260, 'PRESCHOOL', NULL, NULL),
    ('260', 'Apuvälineet', 'Lapsikohtainen tukitoimi', 270, 'PRESCHOOL', '2025-08-01'::date, NULL)


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
