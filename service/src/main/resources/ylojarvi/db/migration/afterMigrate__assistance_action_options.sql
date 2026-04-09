-- SPDX-FileCopyrightText: 2024 Tampere region
--
-- SPDX-License-Identifier: LGPL-2.1-or-later

INSERT INTO assistance_action_option
    (value, name_fi, description_fi, display_order, category, valid_from, valid_to)
VALUES
    ('10', 'Avustajapalvelut', 'Lapsen ryhmässä työskentelee ryhmäavustaja tai lapsella on henkilökohtainen avustaja.', 80, 'DAYCARE', NULL, NULL),
    ('40', 'Henkilöstön mitoitukseen ja/tai rakenteeseen liittyvät ratkaisut', 'Henkilökuntalisäys: Lapsen ryhmässä työskentelee lain vaatimaan resurssia enemmän varhaiskasvatuksen lastenhoitajia/sosionomeja/opettajia. Henkilökuntamuutos: Ryhmää on vahvistettu pedagogisesti siten, että henkilöstöön kuuluu kaksi varhaiskasvatuksen opettajaa. Mikäli lapsi on erityisryhmässä tai integroidussa ryhmässä, tätä vaihtoehtoa ei valita.', 90, 'DAYCARE', NULL, NULL),
    ('50', 'Tuettu varhaiskasvatusryhmä', 'Lapsi on tuetussa varhaiskasvatusryhmässä.', 30, 'DAYCARE', NULL, NULL),
    ('57', 'Vaativan erityisen tuen varhaiskasvatusryhmä', 'Lapsen ryhmässä työskentelee yhtenä työntekijänä varhaiskasvatuksen erityisopettaja.', 50, 'DAYCARE', NULL, NULL),
    ('80', 'Tulkitsemispalvelut', NULL, 80, 'DAYCARE', '2025-08-01'::date, NULL),
    ('90', 'Apuvälineet', NULL, 90, 'DAYCARE', '2025-08-01'::date, NULL),
    ('100', 'Ryhmän lapsimäärän pienentäminen', NULL, 100, 'DAYCARE', '2025-08-01'::date, NULL),
    ('110', 'Erityisopettajan antama konsultaatio', NULL, 110, 'DAYCARE', '2025-08-01'::date, NULL),
    ('120', 'Erityisopettajan antama opetus', NULL, 120, 'DAYCARE', '2025-08-01'::date, NULL),

    -- ended DAYCARE
    ('70', 'Erityisopettajan antama säännöllinen tuki', 'Ryhmän pienennys rakenteellisen tuen muotona eli lapsiryhmää pienennetään, jotta lasten tuki toteutuu tarkoituksenmukaisesti. Kirjaa myös tuen kerroin kohtaan Tuen tarve.', 70, 'DAYCARE', NULL, '2025-07-31'::date),

    -- ended PRESCHOOL
    ('55', 'Tuettu esiopetusryhmä', 'Lapsi on tuetussa esiopetusryhmässä.', 40, 'PRESCHOOL', NULL, '2025-07-31'::date),
    ('60', 'Vaativan erityisen tuen perusopetuksen ryhmä/esiopetus', 'Lapsi saa osa-aikaista erityisopetusta.', 60, 'PRESCHOOL', NULL, '2025-07-31'::date),

    ('200', 'Lapsikohtaiset tukitoimet', NULL, 200, 'PRESCHOOL', '2025-08-01'::date, NULL),
    ('210', 'Erityisopettajan antama opetus osittain pienryhmässä ja muun opetuksen yhteydessä', NULL, 210, 'PRESCHOOL', '2025-08-01'::date, NULL),
    ('220', 'Erityisopettajan antama opetus pienryhmässä (kokoaikainen)', NULL, 220, 'PRESCHOOL', '2025-08-01'::date, NULL),
    ('230', 'Tulkitsemispalvelut', NULL, 230, 'PRESCHOOL', '2025-08-01'::date, NULL),
    ('240', 'Avustajapalvelut', NULL, 240, 'PRESCHOOL', NULL, NULL),
    ('250', 'Apuvälineet', NULL, 250, 'PRESCHOOL', '2025-08-01'::date, NULL)

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
