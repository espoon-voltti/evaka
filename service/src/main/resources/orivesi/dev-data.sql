-- SPDX-FileCopyrightText: 2024 Tampere region
--
-- SPDX-License-Identifier: LGPL-2.1-or-later

INSERT INTO preschool_term (finnish_preschool, swedish_preschool, extended_term, application_period, term_breaks) VALUES
    ('[2023-08-09,2024-05-31]', '[2023-08-09,2024-05-31]', '[2023-08-09,2024-05-31]', '[2022-10-01,2024-05-31]', datemultirange('[2023-10-16,2023-10-22]', '[2023-12-23,2024-01-07]', '[2024-02-26,2024-03-03]', '[2024-03-28,2024-04-01]', '[2024-05-09,2024-05-10]'));

INSERT INTO daycare (name, type, care_area_id, phone, url, backup_location, opening_date, closing_date, email, schedule, additional_info, unit_manager_name, unit_manager_phone, unit_manager_email, cost_center, upload_to_varda, capacity, decision_daycare_name, decision_preschool_name, decision_handler, decision_handler_address, street_address, postal_code, post_office, mailing_po_box, location, mailing_street_address, mailing_postal_code, mailing_post_office, invoiced_by_municipality, provider_type, language, upload_to_koski, oph_unit_oid, oph_organizer_oid, ghost_unit, daycare_apply_period, preschool_apply_period, club_apply_period, finance_decision_handler, operation_times, shift_care_operation_times, daily_preschool_time, daily_preparatory_time) VALUES
    ('Päiväkoti A', '{CENTRE}', (SELECT id FROM care_area WHERE name = 'Orivesi'), NULL, NULL, NULL, '2021-04-20', NULL, NULL, NULL, NULL, 'UNIT_MANAGER_NAME', 'UNIT_MANAGER_PHONE', 'UNIT_MANAGER_EMAIL@tampere.fi', '1', false, 0, 'Päiväkoti A', 'Päiväkoti A', 'Varhaiskasvatusyksikön johtaja', 'Suokatu 10', 'Runkokatu 5', '33340', 'Tampere', 'Runkokatu 5', '(23.60571,61.51667)', NULL, '33340', 'Tampere', TRUE, 'MUNICIPAL', 'fi', false, NULL, NULL, NULL, '[2021-04-20,)', NULL, NULL, NULL, '{"(00:00,23:59)","(00:00,23:59)","(00:00,23:59)","(00:00,23:59)","(00:00,23:59)", null, null}', NULL, NULL, NULL),
    ('Päiväkoti B', '{CENTRE}', (SELECT id FROM care_area WHERE name = 'Orivesi'), NULL, NULL, NULL, '2021-04-20', NULL, NULL, NULL, NULL, 'UNIT_MANAGER_NAME', 'UNIT_MANAGER_PHONE', 'UNIT_MANAGER_EMAIL@tampere.fi', '1', false, 0, 'Päiväkoti B', 'Päiväkoti B', 'Varhaiskasvatusyksikön johtaja', 'Suokatu 10', 'Kraatarinkatu 5', '33270', 'Tampere', 'Kraatarinkatu 5', '(23.68205,61.5078)', NULL, '33270', 'Tampere', TRUE, 'MUNICIPAL', 'fi', false, NULL, NULL, NULL, '[2021-04-20,)', NULL, NULL, NULL, '{"(00:00,23:59)","(00:00,23:59)","(00:00,23:59)","(00:00,23:59)","(00:00,23:59)", null, null}', NULL, NULL, NULL),
    ('Päiväkoti C', '{CENTRE}', (SELECT id FROM care_area WHERE name = 'Orivesi'), NULL, NULL, NULL, '2021-04-20', NULL, NULL, NULL, NULL, 'UNIT_MANAGER_NAME', 'UNIT_MANAGER_PHONE', 'UNIT_MANAGER_EMAIL@tampere.fi', '2', false, 0, 'Päiväkoti C', 'Päiväkoti C', 'Varhaiskasvatusyksikön johtaja', 'Suokatu 10', 'Maamiehentie 2', '33340', 'TAMPERE', NULL, '(23.59596,61.51136)', NULL, '33340', 'TAMPERE', TRUE, 'MUNICIPAL', 'fi', false, NULL, NULL, NULL, '[2021-04-20,)', NULL, NULL, NULL, '{"(00:00,23:59)","(00:00,23:59)","(00:00,23:59)","(00:00,23:59)","(00:00,23:59)", null, null}', NULL, NULL, NULL),
    ('Perhepäivähoito', '{FAMILY}', (SELECT id FROM care_area WHERE name = 'Orivesi'), NULL, NULL, NULL, '2021-04-20', NULL, NULL, NULL, NULL, 'UNIT_MANAGER_NAME', 'UNIT_MANAGER_PHONE', 'UNIT_MANAGER_EMAIL@tampere.fi', NULL, false, 0, 'Perhepäivähoito', 'Perhepäivähoito', 'Varhaiskasvatusyksikön johtaja', 'Suokatu 10', 'Sairaalankatu 7', '33100', 'TAMPERE', 'Sairaalankatu 7', '(23.78589,61.49301)', NULL, '33100', 'TAMPERE', false, 'MUNICIPAL', 'fi', false, NULL, NULL, NULL, '[2021-04-20,)', NULL, NULL, NULL, '{"(00:00,23:59)","(00:00,23:59)","(00:00,23:59)","(00:00,23:59)","(00:00,23:59)", null, null}', NULL, NULL, NULL),
    ('Perhepäivähoitaja A', '{FAMILY}', (SELECT id FROM care_area WHERE name = 'Orivesi'), NULL, NULL, NULL, '2021-04-20', NULL, NULL, NULL, NULL, 'UNIT_MANAGER_NAME', 'UNIT_MANAGER_PHONE', 'UNIT_MANAGER_EMAIL@tampere.fi', NULL, false, 0, 'Perhepäivähoitaja A', 'Perhepäivähoitaja A', 'Varhaiskasvatusyksikön johtaja', 'Suokatu 10', 'Sairaalankatu 7', '33100', 'TAMPERE', 'Sairaalankatu 7', '(23.78589,61.49301)', NULL, '33100', 'TAMPERE', false, 'MUNICIPAL', 'fi', false, NULL, NULL, NULL, NULL, NULL, NULL, NULL, '{"(00:00,23:59)","(00:00,23:59)","(00:00,23:59)","(00:00,23:59)","(00:00,23:59)", null, null}', NULL, NULL, NULL),
    ('Perhepäivähoitaja B', '{FAMILY}', (SELECT id FROM care_area WHERE name = 'Orivesi'), NULL, NULL, NULL, '2021-04-20', NULL, NULL, NULL, NULL, 'UNIT_MANAGER_NAME', 'UNIT_MANAGER_PHONE', 'UNIT_MANAGER_EMAIL@tampere.fi', NULL, false, 0, 'Perhepäivähoitaja B', 'Perhepäivähoitaja B', 'Varhaiskasvatusyksikön johtaja', 'Suokatu 10', 'Kuusitie 8', '36110', 'KANGASALA', 'Kuusitie 8', '(24.00936,61.53011)', NULL, '36110', 'KANGASALA', false, 'MUNICIPAL', 'fi', false, NULL, NULL, NULL, NULL, NULL, NULL, NULL, '{"(00:00,23:59)","(00:00,23:59)","(00:00,23:59)","(00:00,23:59)","(00:00,23:59)", null, null}', NULL, NULL, NULL),
    ('Perhepäivähoitaja C', '{FAMILY}', (SELECT id FROM care_area WHERE name = 'Orivesi'), NULL, NULL, NULL, '2021-04-20', NULL, NULL, NULL, NULL, 'UNIT_MANAGER_NAME', 'UNIT_MANAGER_PHONE', 'UNIT_MANAGER_EMAIL@tampere.fi', NULL, false, 0, 'Perhepäivähoitaja C', 'Perhepäivähoitaja C', 'Varhaiskasvatusyksikön johtaja', 'Suokatu 10', 'Tiiliruukinkatu 1', '33200', 'TAMPERE', 'Tiiliruukinkatu 1', '(23.75582,61.49336)', NULL, '33200', 'TAMPERE', false, 'MUNICIPAL', 'fi', false, NULL, NULL, NULL, NULL, NULL, NULL, NULL, '{"(00:00,23:59)","(00:00,23:59)","(00:00,23:59)","(00:00,23:59)","(00:00,23:59)", null, null}', NULL, NULL, NULL),
    ('Koulupolun mukainen esiopetus', '{PRESCHOOL}', (SELECT id FROM care_area WHERE name = 'Orivesi'), NULL, NULL, NULL, '2021-04-20', NULL, NULL, NULL, NULL, 'UNIT_MANAGER_NAME', 'UNIT_MANAGER_PHONE', 'UNIT_MANAGER_EMAIL@tampere.fi', NULL, false, 0, '', '', '', '', 'Runkokatu 5', '33340', 'Tampere', 'Runkokatu 5', '(23.60571,61.51667)', NULL, '33340', 'Tampere', false, 'MUNICIPAL', 'fi', false, NULL, NULL, NULL, NULL, '[2021-04-20,)', NULL, NULL, '{"(00:00,23:59)","(00:00,23:59)","(00:00,23:59)","(00:00,23:59)","(00:00,23:59)", null, null}', NULL, ('09:00'::time, '13:00'::time), NULL),
    ('Koulu A', '{PRESCHOOL}', (SELECT id FROM care_area WHERE name = 'Orivesi'), NULL, NULL, NULL, '2021-04-20', NULL, NULL, NULL, NULL, 'UNIT_MANAGER_NAME', 'UNIT_MANAGER_PHONE', 'UNIT_MANAGER_EMAIL@tampere.fi', NULL, false, 0, 'Koulu A', 'Koulu A', 'Varhaiskasvatusyksikön johtaja', 'Suokatu 10', 'Runkokatu 5', '33340', 'Tampere', 'Runkokatu 5', '(23.60571,61.51667)', NULL, '33340', 'Tampere', false, 'MUNICIPAL', 'fi', false, NULL, NULL, NULL, NULL, NULL, NULL, NULL, '{"(00:00,23:59)","(00:00,23:59)","(00:00,23:59)","(00:00,23:59)","(00:00,23:59)", null, null}', NULL, ('09:00'::time, '13:00'::time), NULL),
    ('Koulu B', '{PRESCHOOL}', (SELECT id FROM care_area WHERE name = 'Orivesi'), NULL, NULL, NULL, '2021-04-20', NULL, NULL, NULL, NULL, 'UNIT_MANAGER_NAME', 'UNIT_MANAGER_PHONE', 'UNIT_MANAGER_EMAIL@tampere.fi', NULL, false, 0, 'Koulu B', 'Koulu B', 'Varhaiskasvatusyksikön johtaja', 'Suokatu 10', 'Runkokatu 5', '33340', 'Tampere', 'Runkokatu 5', '(23.60571,61.51667)', NULL, '33340', 'Tampere', false, 'MUNICIPAL', 'fi', false, NULL, NULL, NULL, NULL, NULL, NULL, NULL, '{"(00:00,23:59)","(00:00,23:59)","(00:00,23:59)","(00:00,23:59)","(00:00,23:59)", null, null}', NULL, ('09:00'::time, '13:00'::time), NULL),
    ('Koulu C', '{PRESCHOOL}', (SELECT id FROM care_area WHERE name = 'Orivesi'), NULL, NULL, NULL, '2021-04-20', NULL, NULL, NULL, NULL, 'UNIT_MANAGER_NAME', 'UNIT_MANAGER_PHONE', 'UNIT_MANAGER_EMAIL@tampere.fi', NULL, false, 0, 'Koulu C', 'Koulu C', 'Varhaiskasvatusyksikön johtaja', 'Suokatu 10', 'Runkokatu 5', '33340', 'Tampere', 'Runkokatu 5', '(23.60571,61.51667)', NULL, '33340', 'Tampere', false, 'MUNICIPAL', 'fi', false, NULL, NULL, NULL, NULL, NULL, NULL, NULL, '{"(00:00,23:59)","(00:00,23:59)","(00:00,23:59)","(00:00,23:59)","(00:00,23:59)", null, null}', NULL, ('09:00'::time, '13:00'::time), NULL);

UPDATE daycare SET enabled_pilot_features = enum_range(null::pilot_feature);

INSERT INTO daycare_group (daycare_id, name, start_date, end_date)
SELECT id, 'Ryhmä ' || r, opening_date, COALESCE(closing_date, NULL)
FROM daycare CROSS JOIN generate_series(1, 3) AS r;

INSERT INTO daycare_caretaker (group_id, amount, start_date, end_date)
SELECT id, 3, start_date, end_date FROM daycare_group;

INSERT INTO message_account (daycare_group_id, type)
SELECT id, 'GROUP'::message_account_type FROM daycare_group;

INSERT INTO employee (first_name, last_name, email, roles, active) VALUES
    ('Päivi', 'Pääkäyttäjä', 'paivi.paakayttaja@tampere.fi', '{ADMIN, SERVICE_WORKER, FINANCE_ADMIN}'::user_role[], TRUE),
    ('Paula', 'Palveluohjaaja', 'paula.palveluohjaaja@tampere.fi', '{SERVICE_WORKER}'::user_role[], TRUE),
    ('Lasse', 'Laskuttaja', 'lasse.laskuttaja@tampere.fi', '{FINANCE_ADMIN}'::user_role[], TRUE),
    ('Raisa', 'Raportoija', 'raisa.raportoija@tampere.fi', '{REPORT_VIEWER}'::user_role[], TRUE),
    ('Harri', 'Hallinto', 'harri.hallinto@tampere.fi', '{DIRECTOR}'::user_role[], TRUE),
    ('Essi', 'Esimies', 'essi.esimies@tampere.fi', '{}'::user_role[], TRUE),
    ('Eemeli', 'Esimies', 'eemeli.esimies@tampere.fi', '{}'::user_role[], TRUE),
    ('Kaisa', 'Kasvattaja', 'kaisa.kasvattaja@tampere.fi', '{}'::user_role[], TRUE),
    ('Kalle', 'Kasvattaja', 'kalle.kasvattaja@tampere.fi', '{}'::user_role[], TRUE),
    ('Erkki', 'Erityisopettaja', 'erkki.erityisopettaja@tampere.fi', '{}'::user_role[], TRUE),
    ('Vallu', 'Varhaiskasvatussihteeri', 'vallu.varhaiskasvatussihteeri@tampere.fi', '{}'::user_role[], TRUE);
UPDATE employee SET external_id = 'nokia-ad:' || id WHERE external_id IS NULL;

INSERT INTO daycare_acl (daycare_id, employee_id, role) VALUES
    ((SELECT id FROM daycare WHERE name = 'Päiväkoti A'), (SELECT id FROM employee WHERE first_name = 'Essi'), 'UNIT_SUPERVISOR'),
    ((SELECT id FROM daycare WHERE name = 'Päiväkoti B'), (SELECT id FROM employee WHERE first_name = 'Essi'), 'UNIT_SUPERVISOR'),
    ((SELECT id FROM daycare WHERE name = 'Päiväkoti A'), (SELECT id FROM employee WHERE first_name = 'Eemeli'), 'UNIT_SUPERVISOR'),
    ((SELECT id FROM daycare WHERE name = 'Päiväkoti C'), (SELECT id FROM employee WHERE first_name = 'Eemeli'), 'UNIT_SUPERVISOR'),
    ((SELECT id FROM daycare WHERE name = 'Päiväkoti A'), (SELECT id FROM employee WHERE first_name = 'Kaisa'), 'STAFF'),
    ((SELECT id FROM daycare WHERE name = 'Päiväkoti B'), (SELECT id FROM employee WHERE first_name = 'Kaisa'), 'STAFF'),
    ((SELECT id FROM daycare WHERE name = 'Päiväkoti A'), (SELECT id FROM employee WHERE first_name = 'Kalle'), 'STAFF'),
    ((SELECT id FROM daycare WHERE name = 'Päiväkoti C'), (SELECT id FROM employee WHERE first_name = 'Kalle'), 'STAFF'),
    ((SELECT id FROM daycare WHERE name = 'Päiväkoti A'), (SELECT id FROM employee WHERE first_name = 'Erkki'), 'SPECIAL_EDUCATION_TEACHER'),
    ((SELECT id FROM daycare WHERE name = 'Päiväkoti B'), (SELECT id FROM employee WHERE first_name = 'Erkki'), 'SPECIAL_EDUCATION_TEACHER'),
    ((SELECT id FROM daycare WHERE name = 'Päiväkoti A'), (SELECT id FROM employee WHERE first_name = 'Vallu'), 'EARLY_CHILDHOOD_EDUCATION_SECRETARY'),
    ((SELECT id FROM daycare WHERE name = 'Päiväkoti B'), (SELECT id FROM employee WHERE first_name = 'Vallu'), 'EARLY_CHILDHOOD_EDUCATION_SECRETARY');

INSERT INTO message_account (employee_id, type)
SELECT DISTINCT employee_id, 'PERSONAL'::message_account_type FROM daycare_acl;

INSERT INTO fee_thresholds (
    valid_during,
    max_fee, min_fee,
    min_income_threshold_2, min_income_threshold_3, min_income_threshold_4, min_income_threshold_5, min_income_threshold_6,
    income_multiplier_2, income_multiplier_3, income_multiplier_4, income_multiplier_5, income_multiplier_6,
    max_income_threshold_2, max_income_threshold_3, max_income_threshold_4, max_income_threshold_5, max_income_threshold_6,
    income_threshold_increase_6_plus,
    sibling_discount_2, sibling_discount_2_plus,
    temporary_fee, temporary_fee_part_day, temporary_fee_sibling, temporary_fee_sibling_part_day
)
VALUES
(
    daterange('2023-03-01', NULL, '[]'),
    29500, 2800,
    387400, 499800, 567500, 635300, 702800,
    0.1070, 0.1070, 0.1070, 0.1070, 0.1070,
    662640, 775040, 842740, 910540, 978040,
    26200,
    0.6, 0.8,
    2000, 2000, 2000, 2000
),
(
    daterange('2022-08-01', '2023-02-28', '[]'),
    29500, 2800,
    291300, 375800, 426700, 477700, 528400,
    0.1070, 0.1070, 0.1070, 0.1070, 0.1070,
    566600, 651100, 702000, 753000, 803700,
    19700,
    0.6, 0.8,
    2000, 2000, 2000, 2000
),
(
    daterange('2021-08-01', '2022-07-31', '[]'),
    28800, 2700,
    279800, 361000, 409900, 458800, 507500,
    0.1070, 0.1070, 0.1070, 0.1070, 0.1070,
    548500, 629700, 678600, 727500, 776200,
    18900,
    0.6, 0.8,
    2000, 2000, 2000, 2000
),
(
    daterange('2020-08-01', '2021-07-31', '[]'),
    28800, 2700,
    213600, 275600, 312900, 350200, 387400,
    0.1070, 0.1070, 0.1070, 0.1070, 0.1070,
    482300, 544300, 581600, 618900, 656100,
    14400,
    0.5, 0.8,
    2000, 2000, 2000, 2000
),
(
    daterange('2018-08-01', '2020-07-31', '[]'),
    28900, 2700,
    210200, 271300, 308000, 344700, 381300,
    0.1070, 0.1070, 0.1070, 0.1070, 0.1070,
    479900, 541000, 577700, 614400, 651000,
    14200,
    0.5, 0.8,
    2000, 2000, 2000, 2000
);

INSERT INTO service_need_option_voucher_value
(id, service_need_option_id, validity, base_value, coefficient, value, base_value_under_3y, coefficient_under_3y, value_under_3y)
VALUES
    -- 2023-08-01 - 2024-07-31
    ('03ae36de-7058-45a1-ba72-514afca39dcd', 'f9e7d841-49bf-43e5-8c65-028dad590a76', daterange('2023-08-01', '2024-07-31', '[]'), 90100, 0.60, 54060, 140600, 0.60, 84360),
    ('0ec6c2c9-ae7c-46a1-b111-f0be593abcd0', '94e44ef1-106b-401d-81b6-8e5c31cd0437', daterange('2023-08-01', '2024-07-31', '[]'), 0, 0, 0, 0, 0, 0),
    -- 2024-08-01 -
    ('6cf9cc60-8c94-4e15-a8a5-bc3692ac601a', '86ef70a0-bf85-11eb-91e6-1fb57a101165', daterange('2024-08-01', NULL, '[]'), 94900, 0.80, 75920, 148000, 0.80, 118400),
    ('c9007b40-a70a-4e82-97b9-88a791dc2071', '503590f0-b961-11eb-b520-53740af3f7ef', daterange('2024-08-01', NULL, '[]'), 94900, 1.00, 94900, 148000, 1.00, 148000),
    ('3129e833-6768-4466-8f68-97eab63ccf0b', '503591ae-b961-11eb-b521-1fca99358eed', daterange('2024-08-01', NULL, '[]'), 94900, 1.00, 94900, 148000, 1.00, 148000),
    ('e0a5384b-cd8a-4a44-84ad-7e8d9bfc2f61', '94e44ef1-106b-401d-81b6-8e5c31cd0437', daterange('2024-08-01', NULL, '[]'), 0, 0, 0, 0, 0, 0),
    ('d9ee44f3-0cb0-4f66-a4df-7dfd0f56bcbf', '21a0589d-d4db-4978-9a37-8e6993a7dafd', daterange('2024-08-01', NULL, '[]'), 94900, 0.50, 47450, 148000, 0.50, 74000),
    ('92cb65db-d2b2-476f-a872-d7fc03fb2d21', '1c5d7ea4-669f-4b4e-8593-353be4c9cea0', daterange('2024-08-01', NULL, '[]'), 94900, 0.50, 47450, 148000, 0.50, 74000),
    ('2811d68a-f909-4530-9097-68f882a93e8f', '9e6a4660-2f83-40e3-bf27-d9590e93dbf2', daterange('2024-08-01', NULL, '[]'), 94900, 0.50, 47450, 148000, 0.50, 74000),
    ('1f07e892-4e9f-4d37-9acc-c81df42f3fad', '3b94630b-e01e-4b61-b040-8910baf96e97', daterange('2024-08-01', NULL, '[]'), 94900, 0.50, 47450, 148000, 0.50, 74000),
    ('1cb0cc18-59ff-4a8d-8c36-2d54a90d0c02', '593d8cbf-fcf8-41a7-a5fa-8fe96a7b93d8', daterange('2024-08-01', NULL, '[]'), 94900, 0.50, 47450, 148000, 0.50, 74000),
    ('b8c5c2f2-217f-48eb-99f7-175d83d20c74', 'c169bce7-6533-4409-8acd-445061f1ff34', daterange('2024-08-01', NULL, '[]'), 94900, 0.50, 47450, 148000, 0.50, 74000),
    ('c6eb0cdb-c9c0-40e7-8206-b425cf78991d', '9cdef927-f31a-45cb-af49-3ea5b1c1cb8a', daterange('2024-08-01', NULL, '[]'), 94900, 0.50, 47450, 148000, 0.50, 74000),
    ('9b3b7fb2-bbd8-488e-ad14-7f2e1e77fbc2', '0a58d934-6fd1-11ed-a75e-c353faef5858', daterange('2024-08-01', NULL, '[]'), 94900, 0.50, 47450, 148000, 0.50, 74000),
    ('7e848986-6472-4d1b-a9af-8be257980a02', '0a58db0a-6fd1-11ed-a75e-bbde95c1aded', daterange('2024-08-01', NULL, '[]'), 94900, 0.50, 47450, 148000, 0.50, 74000),
    ('7d22aabc-a8f7-4925-882c-693b62a8ce81', '0a58dbe6-6fd1-11ed-a75e-5335f2b9a91c', daterange('2024-08-01', NULL, '[]'), 94900, 0.50, 47450, 148000, 0.50, 74000),
    ('97b07714-be83-480d-b8b1-219a960e34b0', '0a58da38-6fd1-11ed-a75e-9b2790b0b4f5', daterange('2024-08-01', NULL, '[]'), 94900, 0.50, 47450, 148000, 0.50, 74000),
    ('87328f80-d4f3-45ef-a2c0-8c654d23afe7', '0a58dcae-6fd1-11ed-a75e-b3e10433b949', daterange('2024-08-01', NULL, '[]'), 94900, 0.50, 47450, 148000, 0.50, 74000)
ON CONFLICT (id) DO
    UPDATE SET
               service_need_option_id = EXCLUDED.service_need_option_id,
               validity = EXCLUDED.validity,
               base_value = EXCLUDED.base_value,
               coefficient = EXCLUDED.coefficient,
               value = EXCLUDED.value,
               base_value_under_3y = EXCLUDED.base_value_under_3y,
               coefficient_under_3y = EXCLUDED.coefficient_under_3y,
               value_under_3y = EXCLUDED.value_under_3y
WHERE
    service_need_option_voucher_value.service_need_option_id <> EXCLUDED.service_need_option_id OR
    service_need_option_voucher_value.validity <> EXCLUDED.validity OR
    service_need_option_voucher_value.base_value <> EXCLUDED.base_value OR
    service_need_option_voucher_value.coefficient <> EXCLUDED.coefficient OR
    service_need_option_voucher_value.value <> EXCLUDED.value OR
    service_need_option_voucher_value.base_value_under_3y <> EXCLUDED.base_value_under_3y OR
    service_need_option_voucher_value.coefficient_under_3y <> EXCLUDED.coefficient_under_3y OR
    service_need_option_voucher_value.value_under_3y <> EXCLUDED.value_under_3y;
