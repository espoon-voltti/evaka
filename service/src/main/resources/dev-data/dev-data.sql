-- SPDX-FileCopyrightText: 2017-2023 City of Espoo
--
-- SPDX-License-Identifier: LGPL-2.1-or-later

INSERT INTO care_area (id, name, created, updated, area_code, sub_cost_center, short_name) VALUES
    ('a01b0e03-b86e-4cbc-a744-6a35473b9628', 'Alue A', '2019-04-16 05:26:06.303078+00', '2020-04-02 11:40:46.780692+00', 249, '01', 'alue-a'),
    ('801a6cc7-e8a5-4279-b192-4e8192d82c18', 'Alue B', '2019-04-16 05:26:06.303078+00', '2020-04-02 11:40:46.780692+00', 249, '01', 'alue-b');

INSERT INTO daycare (id, name, type, daily_preschool_time, daily_preparatory_time, care_area_id, phone, url, created, updated, backup_location, language_emphasis_id, opening_date, closing_date, email, schedule, additional_info, unit_manager_name, unit_manager_phone, unit_manager_email, cost_center, upload_to_varda, capacity, decision_daycare_name, decision_preschool_name, decision_handler, decision_handler_address, street_address, postal_code, post_office, mailing_po_box, location, mailing_street_address, mailing_postal_code, mailing_post_office, invoiced_by_municipality, provider_type, language, upload_to_koski, oph_unit_oid, oph_organizer_oid, operation_times, ghost_unit, daycare_apply_period, preschool_apply_period, club_apply_period, round_the_clock) VALUES
    ('2dcf0fc0-788e-11e9-bd12-db78e886e666', 'Päiväkoti ja esikoulu A', '{PREPARATORY_EDUCATION,PRESCHOOL,CENTRE}', ('09:00'::time, '13:00'::time), ('09:00'::time, '14:00'::time), 'a01b0e03-b86e-4cbc-a744-6a35473b9628', 'UNIT_PHONE', 'https://www.espoo.fi/fi/toimipisteet/15853', '2019-05-17 10:26:06.066821+00', '2021-09-06 08:34:27.596059+00', null, null, '2004-01-01', null, 'UNIT_EMAIL@espoo.fi', null, '', 'UNIT_MANAGER_NAME_A', 'UNIT_MANAGER_PHONE_A', 'UNIT_MANAGER_EMAIL_A@espoo.fi', '31606', true, 175, 'Päiväkoti ja esiopetus A', '', 'DECISION_HANDLER', 'DECISION_HANDLER_ADDRESS', 'Osoite 1', '02920', 'Espoo', 'PL 3536', '(24.7514109,60.2766848)', null, '02070', 'ESPOON KAUPUNKI', true, 'MUNICIPAL', 'fi', false, null, null, '{"(00:00,23:59)","(00:00,23:59)","(00:00,23:59)","(00:00,23:59)","(00:00,23:59)", null, null}', false, '[2020-03-01,)', '[2021-08-01,)', null, false),
    ('2dd6e5f6-788e-11e9-bd72-9f1cfe2d8405', 'Päiväkoti ja esikoulu B', '{CENTRE,PREPARATORY_EDUCATION,PRESCHOOL}', ('09:00'::time, '13:00'::time), ('09:00'::time, '14:00'::time), '801a6cc7-e8a5-4279-b192-4e8192d82c18', 'UNIT_PHONE', 'https://www.espoo.fi/fi/toimipisteet/15586', '2019-05-17 10:26:06.066821+00', '2021-09-06 08:34:27.596059+00', null, null, '1984-01-01', null, 'UNIT_EMAIL@espoo.fi', null, '', 'UNIT_MANAGER_NAME_B', 'UNIT_MANAGER_PHONE_B', 'UNIT_MANAGER_EMAIL_B@espoo.fi', '31548', true, 56, 'Päiväkoti ja esiopetus B', 'Päiväkoti ja esiopetus B', 'DECISION_HANDLER', 'DECISION_HANDLER_ADDRESS', 'Osoite 2', '02320', 'Espoo', 'PL 32317', '(24.6390243,60.1503933)', '', '02070', 'ESPOON KAUPUNKI', true, 'MUNICIPAL', 'fi', true, null, null, '{"(00:00,23:59)","(00:00,23:59)","(00:00,23:59)","(00:00,23:59)","(00:00,23:59)", null, null}', false, '[2020-03-01,)', '[2020-03-01,)', null, false),
    ('2dd6e5f6-788e-11e9-bd72-9f1cfe999999', 'Päiväkoti ja esikoulu C (palveluseteli)', '{CENTRE,PREPARATORY_EDUCATION,PRESCHOOL}', ('09:00'::time, '13:00'::time), ('09:00'::time, '14:00'::time), '801a6cc7-e8a5-4279-b192-4e8192d82c18', 'UNIT_PHONE', 'https://www.espoo.fi/fi/toimipisteet/99999', '2019-05-17 10:26:06.066821+00', '2021-09-06 08:34:27.596059+00', null, null, '1984-01-01', null, 'UNIT_EMAIL@espoo.fi', null, '', 'UNIT_MANAGER_NAME_B', 'UNIT_MANAGER_PHONE_B', 'UNIT_MANAGER_EMAIL_B@espoo.fi', '31548', true, 56, 'Päiväkoti ja esiopetus C (palveluseteli)', 'Päiväkoti ja esiopetus C (palveluseteli)', 'DECISION_HANDLER', 'DECISION_HANDLER_ADDRESS', 'Osoite 3', '02320', 'Espoo', 'PL 32317', '(24.6390243,60.1503933)', '', '02070', 'ESPOON KAUPUNKI', false, 'PRIVATE_SERVICE_VOUCHER', 'fi', true, null, null, '{"(00:00,23:59)","(00:00,23:59)","(00:00,23:59)","(00:00,23:59)","(00:00,23:59)", null, null}', false, '[2020-03-01,)', '[2020-03-01,)', null, false),
    ('54928f69-2a93-43b3-a080-27ff8682c4f1', 'Päiväkoti vuorohoito', '{CENTRE}', NULL, NULL, '801a6cc7-e8a5-4279-b192-4e8192d82c18', 'UNIT_PHONE', 'UNIT_WEBSITE', '2019-05-17 10:26:06.066821+00', '2021-09-06 08:34:27.596059+00', null, null, '1984-01-01', null, 'UNIT_EMAIL@espoo.fi', null, '', 'UNIT_MANAGER_NAME_B', 'UNIT_MANAGER_PHONE_B', 'UNIT_MANAGER_EMAIL_B@espoo.fi', '31548', true, 56, 'Päiväkoti vuorohoito', 'Päiväkoti vuorohoito', 'DECISION_HANDLER', 'DECISION_HANDLER_ADDRESS', 'Osoite 2', '02320', 'Espoo', 'PL 32317', '(24.6390243,60.33)', '', '02070', 'ESPOON KAUPUNKI', true, 'MUNICIPAL', 'fi', true, null, null, '{"(00:00,23:59)","(00:00,23:59)","(00:00,23:59)","(00:00,23:59)","(00:00,23:59)", null, null}', false, '[2020-03-01,)', '[2020-03-01,)', null, true);

INSERT INTO fee_thresholds (id, valid_during, min_income_threshold_2, min_income_threshold_3, min_income_threshold_4, min_income_threshold_5, min_income_threshold_6, max_income_threshold_2, max_income_threshold_3, max_income_threshold_4, max_income_threshold_5, max_income_threshold_6, income_multiplier_2, income_multiplier_3, income_multiplier_4, income_multiplier_5, income_multiplier_6, income_threshold_increase_6_plus, sibling_discount_2, sibling_discount_2_plus, max_fee, min_fee, temporary_fee, temporary_fee_part_day, temporary_fee_sibling, temporary_fee_sibling_part_day) VALUES
    ('51c2ec8a-bc76-40b3-9b5e-abba4042e361', daterange('2000-01-01', '2020-07-31', '[]'), 210200, 271300, 308000, 344700, 381300, 210200 + 269700, 271300 + 269700, 308000 + 269700, 344700 + 269700, 381300 + 269700, 0.1070, 0.1070, 0.1070, 0.1070, 0.1070, 14200, 0.5, 0.8, 28900, 2700, 2800, 1500, 1500, 800),
    ('236e3ee8-a97f-11ea-889d-eb365ac53e7c', daterange('2020-08-01', NULL), 213600, 275600, 312900, 350200, 387400, 213600 + 268700, 275600 + 268700, 312900 + 268700, 350200 + 268700, 387400 + 268700, 0.1070, 0.1070, 0.1070, 0.1070, 0.1070, 14200, 0.5, 0.8, 28800, 2700, 2800, 1500, 1500, 800);

INSERT INTO daycare_group (id, daycare_id, name, start_date, end_date) VALUES
    ('6f82b730-5963-11ea-b4d8-6f19186c8118', '2dcf0fc0-788e-11e9-bd12-db78e886e666', 'Ryhmä 1', '2020-03-01', NULL),
    ('b4bd39f6-5963-11ea-b4da-ebed8135a791', '2dcf0fc0-788e-11e9-bd12-db78e886e666', 'Ryhmä 2', '2020-03-01', NULL),
    ('4539f160-a616-41cb-ac51-6a12ba2c9645', '2dd6e5f6-788e-11e9-bd72-9f1cfe2d8405', 'Ryhmä 1', '2020-03-01', NULL);

INSERT INTO daycare_caretaker (group_id, amount, start_date, end_date) VALUES
    ('6f82b730-5963-11ea-b4d8-6f19186c8118', 3, '2020-03-01', NULL),
    ('b4bd39f6-5963-11ea-b4da-ebed8135a791', 3, '2020-03-01', NULL),
    ('4539f160-a616-41cb-ac51-6a12ba2c9645', 3, '2020-03-01', NULL);

INSERT INTO message_account (daycare_group_id, type) SELECT id, 'GROUP'::message_account_type as type FROM daycare_group;

INSERT INTO assistance_action_option (value, name_fi, display_order) VALUES
    ('ASSISTANCE_SERVICE_CHILD', 'Avustamispalvelut yhdelle lapselle', 10),
    ('ASSISTANCE_SERVICE_UNIT', 'Avustamispalvelut yksikköön', 20),
    ('SMALLER_GROUP', 'Pedagogisesti vahvistettu ryhmä', 30),
    ('SPECIAL_GROUP', 'Erityisryhmä', 40),
    ('PERVASIVE_VEO_SUPPORT', 'Laaja-alaisen veon tuki', 50),
    ('PART_TIME_SPECIAL_EDUCATION', 'Osa-aikainen erityisopetus esiopetuksessa', 55),
    ('RESOURCE_PERSON', 'Resurssihenkilö', 60),
    ('RATIO_DECREASE', 'Suhdeluvun väljennys', 70),
    ('PERIODICAL_VEO_SUPPORT', 'Lisäresurssi hankerahoituksella', 80);

UPDATE daycare SET enabled_pilot_features = '{MESSAGING, MOBILE, RESERVATIONS, VASU_AND_PEDADOC, MOBILE_MESSAGING}';
