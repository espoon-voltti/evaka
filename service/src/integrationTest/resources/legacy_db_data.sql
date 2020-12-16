-- SPDX-FileCopyrightText: 2017-2020 City of Espoo
--
-- SPDX-License-Identifier: LGPL-2.1-or-later

INSERT INTO care_area(id, name, short_name)
VALUES
  ('d60f3dae-e164-4ad0-b5ec-af9c8c35a586', 'Espoonlahti', 'espoonlahti'),
  ('801a6cc7-e8a5-4279-b192-4e8192d82c18', 'Leppävaara (länsi)', 'leppavaara-lansi'),
  ('a01b0e03-b86e-4cbc-a744-6a35473b9628', 'Leppävaara (itä)', 'leppavaara-ita'),
  ('7f08ec20-3843-466e-807e-a8cddf5d5605', 'Matinkylä-Olari', 'matinkyla-olari'),
  ('aede1c92-39a0-47b3-9f7a-45b4355f6c87', 'Tapiola', 'tapiola'),
  ('7119009f-ec26-45d2-be61-d3f802c1d1e5', 'Espoon keskus (eteläinen)', 'espoon-keskus-etela'),
  ('10842fdc-5750-447d-9b6b-50a1ca66864c', 'Espoon keskus (pohjoinen)', 'espoon-keskus-pohjoinen'),
  ('aa65d808-4f38-11ea-ba70-bf78328155c5', 'Svenska bildningstjänster', 'svenska-bildningstjanster'),
  ('f0243ab9-f393-477c-9df4-fd0defeb924f', 'Area 52', '');

INSERT INTO unit_manager (id, name, phone, email)
VALUES
  ('fc919fa4-28c1-4dc5-a227-09d11eb6b491', 'Tessa Toimihenkilö', '+358 00 123 4567', 'tessa@example.com');

INSERT INTO daycare (id, name, type, care_area_id, phone, unit_manager_id, street_address, postal_code, mailing_po_box, location, post_office, mailing_street_address, mailing_postal_code, mailing_post_office)
VALUES
  ('aa7c5ab0-4f38-11ea-ba70-87ac94467af3', 'Ajurinmäen päiväkoti', '{CENTRE}', 'a01b0e03-b86e-4cbc-a744-6a35473b9628', NULL, 'fc919fa4-28c1-4dc5-a227-09d11eb6b491', 'Porarinkatu 9', '02650', 'PL 65315', '(24.8163809999999998,60.2250980000000027)', 'Espoo', NULL, '02070', 'Espoon Kaupunki'),
  ('aa7f0cd8-4f38-11ea-ba70-7bc014bc5d7e', 'Vallipuiston päiväkoti', '{CENTRE}', 'a01b0e03-b86e-4cbc-a744-6a35473b9628', NULL, 'fc919fa4-28c1-4dc5-a227-09d11eb6b491', 'Ratapölkky 1', '02650', 'PL 65309', '(24.8185840000000013,60.2289120000000011)', 'Espoo', NULL, '02070', 'Espoon Kaupunki'),
  ('aa7f3032-4f38-11ea-ba70-a34b203f9547', 'Auroran päiväkoti', '{CENTRE}', '801a6cc7-e8a5-4279-b192-4e8192d82c18', NULL, 'fc919fa4-28c1-4dc5-a227-09d11eb6b491', 'Lippajärventie 44', '02940', 'PL 3592', '(24.7113930000000011,60.2452800000000011)', 'Espoo', NULL, '02070', 'Espoon Kaupunki'),
  ('aa7f7722-4f38-11ea-ba70-7b65c139f076', 'Heiniemen päiväkoti', '{CENTRE}', '801a6cc7-e8a5-4279-b192-4e8192d82c18', NULL, 'fc919fa4-28c1-4dc5-a227-09d11eb6b491', 'Heiniitty 6', '02940', 'PL 94308', '(24.7222919999999995,60.2363849999999985)', 'Espoo', NULL, '02070', 'Espoon Kaupunki'),
  ('aa934748-4f38-11ea-ba70-d38c1069046f', 'Lasten Montessorikoulu', '{CENTRE}', 'aede1c92-39a0-47b3-9f7a-45b4355f6c87', '+3589 541 8538', 'fc919fa4-28c1-4dc5-a227-09d11eb6b491', 'Kirvuntie 22', '02140', '', NULL, 'Espoo', NULL, '', ''),
  ('aa800138-4f38-11ea-ba70-4308545c61b6', 'Karamäen päiväkoti', '{CENTRE}', '801a6cc7-e8a5-4279-b192-4e8192d82c18', NULL, 'fc919fa4-28c1-4dc5-a227-09d11eb6b491', 'Nuumäentie 2', '02710', 'PL 71315', '(24.7724289999999989,60.2304099999999991)', 'Espoo', NULL, '02070', 'Espoon Kaupunki'),
  ('aa80247e-4f38-11ea-ba70-17b2f13ee3fa', 'Kilon päiväkoti', '{CENTRE}', '801a6cc7-e8a5-4279-b192-4e8192d82c18', NULL, 'fc919fa4-28c1-4dc5-a227-09d11eb6b491', 'Aspelinintie 3 D', '02630', 'PL 63303', '(24.7699299999999987,60.2092600000000004)', 'Espoo', NULL, '02070', 'Espoon Kaupunki'),
  ('aa826478-4f38-11ea-ba70-93403132e7bd', 'Kuninkaisten päiväkoti', '{CENTRE}', '801a6cc7-e8a5-4279-b192-4e8192d82c18', NULL, 'fc919fa4-28c1-4dc5-a227-09d11eb6b491', 'Kuninkaistenportti 20', '02610', 'PL 61206', '(24.774163999999999,60.2226700000000008)', 'Espoo', NULL, '02070', 'Espoon Kaupunki'),
  ('aa94b3ee-4f38-11ea-ba70-ebb1ef181cdb', 'Lagstads daghem och förskola', '{CENTRE,PRESCHOOL}', 'aa65d808-4f38-11ea-ba70-bf78328155c5', NULL, 'fc919fa4-28c1-4dc5-a227-09d11eb6b491', 'Esbogatan 7', '02770', '', '(24.6559279999999994,60.20749)', 'Esbo', NULL, '', ''),
  ('aa831986-4f38-11ea-ba70-b38e70b42848', 'Leppäsillan päiväkoti', '{CENTRE}', '801a6cc7-e8a5-4279-b192-4e8192d82c18', NULL, 'fc919fa4-28c1-4dc5-a227-09d11eb6b491', 'Pääskyskuja 11 A ja 11 B', '02620', 'PL 62307', '(24.7839220000000005,60.227646)', 'Espoo', NULL, '02070', 'Espoon Kaupunki');

INSERT INTO daycare (id, name, care_area_id, type, unit_manager_id, street_address, postal_code, mailing_po_box, location, club_apply_period)
VALUES
  ('a9fb114e-3ff5-44fb-b0e7-647fd038fc37', '2-vuotiaiden testikerho, Ti ja To klo 14-16, Tessin Testikerho', '7119009f-ec26-45d2-be61-d3f802c1d1e5', '{CLUB}', 'fc919fa4-28c1-4dc5-a227-09d11eb6b491', 'Testitie 1', '02970', 'PL 97306, 02070 Espoon kaupunki', '(24.7378,60.3027999999999977)', '[2020-03-01,)');

INSERT INTO daycare (id, name, care_area_id, phone, url, type, street_address, postal_code, mailing_po_box, location)
VALUES ('3e5e9b7c-7e1e-11e9-ba34-afc4b10bad6f', 'Tessin Testipäiväkoti', (SELECT id FROM care_area WHERE name = 'Espoon keskus (eteläinen)'), null, null, '{CENTRE}', 'Testitie 1', '02970', 'PL 97306, 02070 Espoon kaupunki', POINT(24.7378000, 60.3028000));

INSERT INTO person (id, date_of_birth) VALUES ('2b929594-13ab-4042-9b13-74e84921e6f0', '2020-01-01');
INSERT INTO child VALUES ('2b929594-13ab-4042-9b13-74e84921e6f0');

INSERT INTO person (id, date_of_birth) VALUES ('4bfebea7-d176-4cbb-b403-26e6a1160b61', '2020-01-02');
INSERT INTO child VALUES ('4bfebea7-d176-4cbb-b403-26e6a1160b61', 'Pähkinäallergia', 'Vegaani', 'Jotain lisätietoa');

INSERT INTO unit_manager (id, name, phone, email)
VALUES ('df3229bb-c48a-4bf4-b8d1-520cdfe520a6', 'Minna Manageri', '+358 80 111 1111', 'minna.manageri@example.org');

INSERT INTO unit_manager (id, name, phone, email)
VALUES ('a7b8ee14-9b59-4afb-b157-37f9a45e5908', null, null, null);

INSERT INTO daycare (id, name, type, care_area_id, phone, url, created, updated,
                     backup_location, provider_type, language, language_emphasis_id, opening_date, closing_date,
                     email, schedule, additional_info, unit_manager_id, cost_center, street_address, postal_code, mailing_po_box, location)
VALUES ('68851e10-eb86-443e-b28d-0f6ee9642a3c', 'Kauhutalon lastenkoti', '{CENTRE}',
        'f0243ab9-f393-477c-9df4-fd0defeb924f', null,
        'https://www.espoo.fi/fi-FI/Kasvatus_ja_opetus/Varhaiskasvatus/Varhaiskasvatuksen_toimipaikat/Kunnalliset_paivakodit/Espoon_keskus/Juvanpuiston_lastentalon_paivakoti',
        '2019-09-09 11:54:14.476602', '2019-09-09 11:54:14.476602', null, 'MUNICIPAL', 'fi', null, '2004-01-01', null, null,
        null, null, 'df3229bb-c48a-4bf4-b8d1-520cdfe520a6', null, 'Pohjoisentie 1', '02970', 'PL 97306, 02070 Espoon kaupunki', POINT(24.7378687, 60.3028715));

INSERT INTO daycare (id, name, type, care_area_id, phone, url, created, updated,
                     backup_location, provider_type, language, language_emphasis_id, opening_date, closing_date,
                     email, schedule, additional_info, unit_manager_id, cost_center, street_address, postal_code, mailing_po_box, location)
VALUES ('ba6fa7cb-7dfa-4629-b0c1-ae3a721c8a91', 'Peilipuiston päiväkoti', '{CENTRE}',
        'f0243ab9-f393-477c-9df4-fd0defeb924f', null,
        'https://www.espoo.fi/fi-FI/Kasvatus_ja_opetus/Varhaiskasvatus/Varhaiskasvatuksen_toimipaikat/Kunnalliset_paivakodit/Espoon_keskus/Juvanpuiston_lastentalon_paivakoti',
        '2019-09-09 11:54:14.476602', '2019-09-09 11:54:14.476602', null, 'MUNICIPAL', 'fi', null, '2004-01-01', null, null,
        null, null, 'a7b8ee14-9b59-4afb-b157-37f9a45e5908', null, 'Pohjoisentie 1', '02970', 'PL 97306, 02070 Espoon kaupunki', POINT(24.7378687, 60.3028715));

INSERT INTO daycare_group (id, daycare_id, name, start_date, end_date)
VALUES ('ff4fbaad-d02f-4f7a-9f7f-4c4e0f2a2b21',
        '68851e10-eb86-443e-b28d-0f6ee9642a3c',
        'Ryhmä1',
        '2016-03-10',
        'infinity'),
       ('caf6c325-2abd-40bf-b5a6-e223f88573e6',
        '68851e10-eb86-443e-b28d-0f6ee9642a3c',
        'Ryhmä2',
        '2017-02-20',
        '2022-12-31');

INSERT INTO varda_organizer (organizer, email, phone, iban)
VALUES ('Espoo', 'test@espoo.fi', '+358981624', 'FI1680001370795552');

INSERT INTO employee(id, first_name, last_name, external_id, email)
VALUES('00000000-0000-0000-0000-000000000000', 'Ally', 'Aardvark', 'espoo-ad:2014e23e-17c2-482f-ba4d-f8b9edc9d5c9', 'ally.aardvark@espoo.fi');
