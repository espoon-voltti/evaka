-- SPDX-FileCopyrightText: 2021 City of Oulu
--
-- SPDX-License-Identifier: LGPL-2.1-or-later

INSERT INTO assistance_action_option
    (value, name_fi, description_fi, display_order, category)
VALUES
    ('10', 'Avustamispalvelut: ryhmäkohtainen', 'Merkitään alueellisen tuen vastaavan päätöksen jälkeen.', 10, 'DAYCARE'),
    ('20', 'Avustamispalvelut: lapsikohtainen', 'Merkitään alueellisen tuen vastaavan päätöksen jälkeen.', 20, 'DAYCARE'),
    ('30', 'Tuki huomioidaan lapsiryhmän koossa', 'Merkitään alueellisen tuen vastaavan päätöksen jälkeen', 30, 'DAYCARE'),
    ('40', 'Varhaiskasvatuksen erityisopettajan konsultaatio', 'Merkitään, kun lapsikohtainen konsultaatio on säännöllistä.', 40, 'DAYCARE'),
    ('50', 'Osa-aikainen erityisopetus', 'Lapsi saa osa-aikaisesti varhaiskasvatuksen erityisopettajan antamaa opetusta.', 50, 'DAYCARE'),
    ('60', 'Kokoaikainen erityisopetus', 'Lapsi saa kokoaikaisesti varhaiskasvatuksen erityisopettajan antamaa opetusta.', 60, 'DAYCARE'),
    ('70', 'Ryhmän henkilöstömäärän lisäys', 'Esim. neljäs kasvattaja ryhmässä, resurssiveo ryhmässä.', 70, 'DAYCARE'),
    ('90', 'Alueellinen pienryhmä', 'Ryhmässä kokoaikainen varhaiskasvatuksen erityisopettaja.', 90, 'DAYCARE'),
    ('130', 'Apuvälineet', 'Merkitään hallintopäätöksen jälkeen.', 130, 'DAYCARE'),
    ('140', 'Tulkitsemispalvelut', 'Lapselle järjestetään kommunikaation tueksi erillisiä tulkitsemispalveluita. Viittomakieltä käyttävien lasten kommunikaation tukena voidaan tarvittaessa käyttää viittomakielen tulkkia tai viittomakielentaitoista avustajaa. Tulkitsemispalvelut on mahdollista järjestää avustamispalvelun yhteydessä.', 140, 'DAYCARE'),
    ('180', 'S2 opetus', 'Merkitse tähän, jos lapsi saa S2 opetusta', 180, 'DAYCARE')
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
