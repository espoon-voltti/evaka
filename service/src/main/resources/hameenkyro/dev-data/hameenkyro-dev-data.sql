-- SPDX-FileCopyrightText: 2024 Tampere region
--
-- SPDX-License-Identifier: LGPL-2.1-or-later

INSERT INTO preschool_term (finnish_preschool, swedish_preschool, extended_term, application_period, term_breaks) VALUES
    ('[2023-08-09,2024-05-31]', '[2023-08-09,2024-05-31]', '[2023-08-09,2024-05-31]', '[2022-10-01,2024-05-31]', datemultirange('[2023-10-16,2023-10-22]', '[2023-12-23,2024-01-07]', '[2024-02-26,2024-03-03]', '[2024-03-28,2024-04-01]', '[2024-05-09,2024-05-10]'));

INSERT INTO daycare (name, type, care_area_id, phone, url, backup_location, opening_date, closing_date, email, schedule, additional_info, unit_manager_name, unit_manager_phone, unit_manager_email, cost_center, upload_to_varda, capacity, decision_daycare_name, decision_preschool_name, decision_handler, decision_handler_address, street_address, postal_code, post_office, mailing_po_box, location, mailing_street_address, mailing_postal_code, mailing_post_office, invoiced_by_municipality, provider_type, language, upload_to_koski, oph_unit_oid, oph_organizer_oid, ghost_unit, daycare_apply_period, preschool_apply_period, club_apply_period, finance_decision_handler, operation_times, shift_care_operation_times, daily_preschool_time, daily_preparatory_time) VALUES
    ('Päiväkoti A', '{CENTRE}', (SELECT id FROM care_area WHERE name = 'Hämeenkyrö'), NULL, NULL, NULL, '2021-04-20', NULL, NULL, NULL, NULL, 'UNIT_MANAGER_NAME', 'UNIT_MANAGER_PHONE', 'UNIT_MANAGER_EMAIL@tampere.fi', '1', false, 0, 'Päiväkoti A', 'Päiväkoti A', 'Varhaiskasvatusyksikön johtaja', 'Suokatu 10', 'Runkokatu 5', '33340', 'Tampere', 'Runkokatu 5', '(23.60571,61.51667)', NULL, '33340', 'Tampere', TRUE, 'MUNICIPAL', 'fi', false, NULL, NULL, NULL, '[2021-04-20,)', NULL, NULL, NULL, '{"(00:00,23:59)","(00:00,23:59)","(00:00,23:59)","(00:00,23:59)","(00:00,23:59)", null, null}', NULL, NULL, NULL),
    ('Päiväkoti B', '{CENTRE}', (SELECT id FROM care_area WHERE name = 'Hämeenkyrö'), NULL, NULL, NULL, '2021-04-20', NULL, NULL, NULL, NULL, 'UNIT_MANAGER_NAME', 'UNIT_MANAGER_PHONE', 'UNIT_MANAGER_EMAIL@tampere.fi', '1', false, 0, 'Päiväkoti B', 'Päiväkoti B', 'Varhaiskasvatusyksikön johtaja', 'Suokatu 10', 'Kraatarinkatu 5', '33270', 'Tampere', 'Kraatarinkatu 5', '(23.68205,61.5078)', NULL, '33270', 'Tampere', TRUE, 'MUNICIPAL', 'fi', false, NULL, NULL, NULL, '[2021-04-20,)', NULL, NULL, NULL, '{"(00:00,23:59)","(00:00,23:59)","(00:00,23:59)","(00:00,23:59)","(00:00,23:59)", null, null}', NULL, NULL, NULL),
    ('Päiväkoti C', '{CENTRE}', (SELECT id FROM care_area WHERE name = 'Hämeenkyrö'), NULL, NULL, NULL, '2021-04-20', NULL, NULL, NULL, NULL, 'UNIT_MANAGER_NAME', 'UNIT_MANAGER_PHONE', 'UNIT_MANAGER_EMAIL@tampere.fi', '2', false, 0, 'Päiväkoti C', 'Päiväkoti C', 'Varhaiskasvatusyksikön johtaja', 'Suokatu 10', 'Maamiehentie 2', '33340', 'TAMPERE', NULL, '(23.59596,61.51136)', NULL, '33340', 'TAMPERE', TRUE, 'MUNICIPAL', 'fi', false, NULL, NULL, NULL, '[2021-04-20,)', NULL, NULL, NULL, '{"(00:00,23:59)","(00:00,23:59)","(00:00,23:59)","(00:00,23:59)","(00:00,23:59)", null, null}', NULL, NULL, NULL),
    ('Palveluseteli A', '{CENTRE}', (SELECT id FROM care_area WHERE name = 'Hämeenkyrö'), NULL, NULL, NULL, '2021-04-20', NULL, NULL, NULL, NULL, 'UNIT_MANAGER_NAME', 'UNIT_MANAGER_PHONE', 'UNIT_MANAGER_EMAIL@tampere.fi', NULL, false, 0, 'Palveluseteli A', 'Palveluseteli A', 'Varhaiskasvatusyksikön johtaja', 'Suokatu 10', 'Sairaalankatu 7', '33100', 'TAMPERE', 'Sairaalankatu 7', '(23.78589,61.49301)', NULL, '33100', 'TAMPERE', false, 'PRIVATE_SERVICE_VOUCHER', 'fi', false, NULL, NULL, NULL, '[2021-04-20,)', NULL, NULL, NULL, '{"(00:00,23:59)","(00:00,23:59)","(00:00,23:59)","(00:00,23:59)","(00:00,23:59)", null, null}', NULL, NULL, NULL),
    ('Palveluseteli B', '{CENTRE}', (SELECT id FROM care_area WHERE name = 'Hämeenkyrö'), NULL, NULL, NULL, '2021-04-20', NULL, NULL, NULL, NULL, 'UNIT_MANAGER_NAME', 'UNIT_MANAGER_PHONE', 'UNIT_MANAGER_EMAIL@tampere.fi', NULL, false, 0, 'Palveluseteli B', 'Palveluseteli B', 'Varhaiskasvatusyksikön johtaja', 'Suokatu 10', 'Kuusitie 8', '36110', 'KANGASALA', 'Kuusitie 8', '(24.00936,61.53011)', NULL, '36110', 'KANGASALA', false, 'PRIVATE_SERVICE_VOUCHER', 'fi', false, NULL, NULL, NULL, '[2021-04-20,)', NULL, NULL, NULL, '{"(00:00,23:59)","(00:00,23:59)","(00:00,23:59)","(00:00,23:59)","(00:00,23:59)", null, null}', NULL, NULL, NULL),
    ('Palveluseteli C', '{CENTRE}', (SELECT id FROM care_area WHERE name = 'Hämeenkyrö'), NULL, NULL, NULL, '2021-04-20', NULL, NULL, NULL, NULL, 'UNIT_MANAGER_NAME', 'UNIT_MANAGER_PHONE', 'UNIT_MANAGER_EMAIL@tampere.fi', NULL, false, 0, 'Palveluseteli C', 'Palveluseteli C', 'Varhaiskasvatusyksikön johtaja', 'Suokatu 10', 'Tiiliruukinkatu 1', '33200', 'TAMPERE', 'Tiiliruukinkatu 1', '(23.75582,61.49336)', NULL, '33200', 'TAMPERE', false, 'PRIVATE_SERVICE_VOUCHER', 'fi', false, NULL, NULL, NULL, '[2021-04-20,)', NULL, NULL, NULL, '{"(00:00,23:59)","(00:00,23:59)","(00:00,23:59)","(00:00,23:59)","(00:00,23:59)", null, null}', NULL, NULL, NULL),
    ('Perhepäivähoito', '{FAMILY}', (SELECT id FROM care_area WHERE name = 'Hämeenkyrö'), NULL, NULL, NULL, '2021-04-20', NULL, NULL, NULL, NULL, 'UNIT_MANAGER_NAME', 'UNIT_MANAGER_PHONE', 'UNIT_MANAGER_EMAIL@tampere.fi', NULL, false, 0, 'Perhepäivähoito', 'Perhepäivähoito', 'Varhaiskasvatusyksikön johtaja', 'Suokatu 10', 'Sairaalankatu 7', '33100', 'TAMPERE', 'Sairaalankatu 7', '(23.78589,61.49301)', NULL, '33100', 'TAMPERE', false, 'MUNICIPAL', 'fi', false, NULL, NULL, NULL, '[2021-04-20,)', NULL, NULL, NULL, '{"(00:00,23:59)","(00:00,23:59)","(00:00,23:59)","(00:00,23:59)","(00:00,23:59)", null, null}', NULL, NULL, NULL),
    ('Perhepäivähoitaja A', '{FAMILY}', (SELECT id FROM care_area WHERE name = 'Hämeenkyrö'), NULL, NULL, NULL, '2021-04-20', NULL, NULL, NULL, NULL, 'UNIT_MANAGER_NAME', 'UNIT_MANAGER_PHONE', 'UNIT_MANAGER_EMAIL@tampere.fi', NULL, false, 0, 'Perhepäivähoitaja A', 'Perhepäivähoitaja A', 'Varhaiskasvatusyksikön johtaja', 'Suokatu 10', 'Sairaalankatu 7', '33100', 'TAMPERE', 'Sairaalankatu 7', '(23.78589,61.49301)', NULL, '33100', 'TAMPERE', false, 'MUNICIPAL', 'fi', false, NULL, NULL, NULL, NULL, NULL, NULL, NULL, '{"(00:00,23:59)","(00:00,23:59)","(00:00,23:59)","(00:00,23:59)","(00:00,23:59)", null, null}', NULL, NULL, NULL),
    ('Perhepäivähoitaja B', '{FAMILY}', (SELECT id FROM care_area WHERE name = 'Hämeenkyrö'), NULL, NULL, NULL, '2021-04-20', NULL, NULL, NULL, NULL, 'UNIT_MANAGER_NAME', 'UNIT_MANAGER_PHONE', 'UNIT_MANAGER_EMAIL@tampere.fi', NULL, false, 0, 'Perhepäivähoitaja B', 'Perhepäivähoitaja B', 'Varhaiskasvatusyksikön johtaja', 'Suokatu 10', 'Kuusitie 8', '36110', 'KANGASALA', 'Kuusitie 8', '(24.00936,61.53011)', NULL, '36110', 'KANGASALA', false, 'MUNICIPAL', 'fi', false, NULL, NULL, NULL, NULL, NULL, NULL, NULL, '{"(00:00,23:59)","(00:00,23:59)","(00:00,23:59)","(00:00,23:59)","(00:00,23:59)", null, null}', NULL, NULL, NULL),
    ('Perhepäivähoitaja C', '{FAMILY}', (SELECT id FROM care_area WHERE name = 'Hämeenkyrö'), NULL, NULL, NULL, '2021-04-20', NULL, NULL, NULL, NULL, 'UNIT_MANAGER_NAME', 'UNIT_MANAGER_PHONE', 'UNIT_MANAGER_EMAIL@tampere.fi', NULL, false, 0, 'Perhepäivähoitaja C', 'Perhepäivähoitaja C', 'Varhaiskasvatusyksikön johtaja', 'Suokatu 10', 'Tiiliruukinkatu 1', '33200', 'TAMPERE', 'Tiiliruukinkatu 1', '(23.75582,61.49336)', NULL, '33200', 'TAMPERE', false, 'MUNICIPAL', 'fi', false, NULL, NULL, NULL, NULL, NULL, NULL, NULL, '{"(00:00,23:59)","(00:00,23:59)","(00:00,23:59)","(00:00,23:59)","(00:00,23:59)", null, null}', NULL, NULL, NULL),
    ('Kerho A', '{CLUB}', (SELECT id FROM care_area WHERE name = 'Hämeenkyrö'), NULL, NULL, NULL, '2021-04-20', NULL, NULL, NULL, NULL, 'UNIT_MANAGER_NAME', 'UNIT_MANAGER_PHONE', 'UNIT_MANAGER_EMAIL@tampere.fi', NULL, false, 0, 'Kerho A', 'Kerho A', 'Varhaiskasvatusyksikön johtaja', 'Suokatu 10', 'Ollinojankatu 25', '33400', 'TAMPERE', 'Ollinojankatu 25', '(23.6701,61.525)', NULL, '33400', 'TAMPERE', false, 'MUNICIPAL', 'fi', false, NULL, NULL, NULL, NULL, NULL, '[2021-04-20,)', NULL, '{"(00:00,23:59)","(00:00,23:59)","(00:00,23:59)","(00:00,23:59)","(00:00,23:59)", null, null}', NULL, NULL, NULL),
    ('Kerho B', '{CLUB}', (SELECT id FROM care_area WHERE name = 'Hämeenkyrö'), NULL, NULL, NULL, '2021-04-20', NULL, NULL, NULL, NULL, 'UNIT_MANAGER_NAME', 'UNIT_MANAGER_PHONE', 'UNIT_MANAGER_EMAIL@tampere.fi', NULL, false, 0, 'Kerho B', 'Kerho B', 'Varhaiskasvatusyksikön johtaja', 'Suokatu 10', 'Pispalan valtatie 79', '33270', 'TAMPERE', NULL, '(23.69093,61.50705)', NULL, '33270', 'TAMPERE', false, 'MUNICIPAL', 'fi', false, '', '', false, NULL, NULL, '[2021-04-20,)', NULL, '{"(00:00,23:59)","(00:00,23:59)","(00:00,23:59)","(00:00,23:59)","(00:00,23:59)", null, null}', NULL, NULL, NULL),
    ('Kerho C', '{CLUB}', (SELECT id FROM care_area WHERE name = 'Hämeenkyrö'), NULL, NULL, NULL, '2021-04-20', NULL, NULL, NULL, NULL, 'UNIT_MANAGER_NAME', 'UNIT_MANAGER_PHONE', 'UNIT_MANAGER_EMAIL@tampere.fi', NULL, false, 0, 'Kerho C', 'Kerho C', 'Varhaiskasvatusyksikön johtaja', 'Suokatu 10', 'Hämeenpuisto 10', '33210', 'TAMPERE', NULL, '(23.7493,61.50095)', NULL, '33210', 'TAMPERE', false, 'MUNICIPAL', 'fi', false, '', '', false, NULL, NULL, '[2021-04-20,)', NULL, '{"(00:00,23:59)","(00:00,23:59)","(00:00,23:59)","(00:00,23:59)","(00:00,23:59)", null, null}', NULL, NULL, NULL),
    ('Koulupolun mukainen esiopetus', '{PRESCHOOL}', (SELECT id FROM care_area WHERE name = 'Hämeenkyrö'), NULL, NULL, NULL, '2021-04-20', NULL, NULL, NULL, NULL, 'UNIT_MANAGER_NAME', 'UNIT_MANAGER_PHONE', 'UNIT_MANAGER_EMAIL@tampere.fi', NULL, false, 0, '', '', '', '', 'Runkokatu 5', '33340', 'Tampere', 'Runkokatu 5', '(23.60571,61.51667)', NULL, '33340', 'Tampere', false, 'MUNICIPAL', 'fi', false, NULL, NULL, NULL, NULL, '[2021-04-20,)', NULL, NULL, '{"(00:00,23:59)","(00:00,23:59)","(00:00,23:59)","(00:00,23:59)","(00:00,23:59)", null, null}', NULL, ('09:00'::time, '13:00'::time), NULL),
    ('Valmistava opetus', '{PREPARATORY_EDUCATION}', (SELECT id FROM care_area WHERE name = 'Hämeenkyrö'), NULL, NULL, NULL, '2021-04-20', NULL, NULL, NULL, NULL, 'UNIT_MANAGER_NAME', 'UNIT_MANAGER_PHONE', 'UNIT_MANAGER_EMAIL@tampere.fi', NULL, false, 0, '', '', '', '', 'Runkokatu 5', '33340', 'Tampere', 'Runkokatu 5', '(23.60531,61.51627)', NULL, '33340', 'Tampere', false, 'MUNICIPAL', 'fi', false, NULL, NULL, NULL, NULL, '[2021-04-20,)', NULL, NULL, '{"(00:00,23:59)","(00:00,23:59)","(00:00,23:59)","(00:00,23:59)","(00:00,23:59)", null, null}', NULL, NULL, ('09:00'::time, '13:00'::time)),
    ('Koulu A', '{PRESCHOOL}', (SELECT id FROM care_area WHERE name = 'Hämeenkyrö'), NULL, NULL, NULL, '2021-04-20', NULL, NULL, NULL, NULL, 'UNIT_MANAGER_NAME', 'UNIT_MANAGER_PHONE', 'UNIT_MANAGER_EMAIL@tampere.fi', NULL, false, 0, 'Koulu A', 'Koulu A', 'Varhaiskasvatusyksikön johtaja', 'Suokatu 10', 'Runkokatu 5', '33340', 'Tampere', 'Runkokatu 5', '(23.60571,61.51667)', NULL, '33340', 'Tampere', false, 'MUNICIPAL', 'fi', false, NULL, NULL, NULL, NULL, NULL, NULL, NULL, '{"(00:00,23:59)","(00:00,23:59)","(00:00,23:59)","(00:00,23:59)","(00:00,23:59)", null, null}', NULL, ('09:00'::time, '13:00'::time), NULL),
    ('Koulu B', '{PRESCHOOL}', (SELECT id FROM care_area WHERE name = 'Hämeenkyrö'), NULL, NULL, NULL, '2021-04-20', NULL, NULL, NULL, NULL, 'UNIT_MANAGER_NAME', 'UNIT_MANAGER_PHONE', 'UNIT_MANAGER_EMAIL@tampere.fi', NULL, false, 0, 'Koulu B', 'Koulu B', 'Varhaiskasvatusyksikön johtaja', 'Suokatu 10', 'Runkokatu 5', '33340', 'Tampere', 'Runkokatu 5', '(23.60571,61.51667)', NULL, '33340', 'Tampere', false, 'MUNICIPAL', 'fi', false, NULL, NULL, NULL, NULL, NULL, NULL, NULL, '{"(00:00,23:59)","(00:00,23:59)","(00:00,23:59)","(00:00,23:59)","(00:00,23:59)", null, null}', NULL, ('09:00'::time, '13:00'::time), NULL),
    ('Koulu C', '{PRESCHOOL}', (SELECT id FROM care_area WHERE name = 'Hämeenkyrö'), NULL, NULL, NULL, '2021-04-20', NULL, NULL, NULL, NULL, 'UNIT_MANAGER_NAME', 'UNIT_MANAGER_PHONE', 'UNIT_MANAGER_EMAIL@tampere.fi', NULL, false, 0, 'Koulu C', 'Koulu C', 'Varhaiskasvatusyksikön johtaja', 'Suokatu 10', 'Runkokatu 5', '33340', 'Tampere', 'Runkokatu 5', '(23.60571,61.51667)', NULL, '33340', 'Tampere', false, 'MUNICIPAL', 'fi', false, NULL, NULL, NULL, NULL, NULL, NULL, NULL, '{"(00:00,23:59)","(00:00,23:59)","(00:00,23:59)","(00:00,23:59)","(00:00,23:59)", null, null}', NULL, ('09:00'::time, '13:00'::time), NULL);

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
UPDATE employee SET external_id = 'hameenkyro-ad:' || id WHERE external_id IS NULL;

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
    ('03ae36de-7058-45a1-ba72-514afca39dcd', 'f9e7d841-49bf-43e5-8c65-028dad590a76', daterange('2023-08-01', '2024-07-31', '[]'), 90100, 0.60, 54060, 140600, 0.60, 84330),
    ('b279657c-3f63-4aa1-92e8-d99f47ba1581', '36c8c2ed-7543-47de-bc42-14d163a6277d', daterange('2023-08-01', NULL, '[]'), 90100, 0.60, 54060, 140600, 0.60, 84330),
    ('92658f48-0320-4d19-8d56-6fe50c8bf285', '9eb82822-82ef-46fb-a123-29cf0af757b7', daterange('2023-08-01', NULL, '[]'), 90100, 0.80, 72080, 140600, 0.80, 112480),
    ('3c3c1e2b-cac4-418e-941d-2db046d4ced9', 'f5d32585-2c78-4434-95d6-30b446db7d4d', daterange('2023-08-01', NULL, '[]'), 90100, 1.00, 90100, 140600, 1.00, 140600),
    ('4eae9dc5-660b-4661-82b9-9200ca045820', '1b1e6e91-8d54-405c-88f8-2a95d88f8962', daterange('2023-08-01', '2024-07-31', '[]'), 78300, 0.50, 39150, 78300, 0.50, 39150),
    ('df00080f-e403-441c-a1c7-28adaed60ae5', '4d6d632d-d8cf-4b5a-8437-decade30d0c0', daterange('2023-08-01', '2024-07-31', '[]'), 78300, 1.00, 78300, 78300, 1.00, 78300),
    ('0ec6c2c9-ae7c-46a1-b111-f0be593abcd0', '94e44ef1-106b-401d-81b6-8e5c31cd0437', daterange('2023-08-01', '2024-07-31', '[]'), 0, 0, 0, 0, 0, 0),
    ('2c62904b-f187-4909-a21b-ffb399a350dd', '21a0589d-d4db-4978-9a37-8e6993a7dafd', daterange('2023-08-01', '2024-07-31', '[]'), 90100, 0.50, 45050, 140600, 0.50, 70300),
    ('dc39549c-f24f-4ca8-b5a3-da41174b3c06', '1c5d7ea4-669f-4b4e-8593-353be4c9cea0', daterange('2023-08-01', '2024-07-31', '[]'), 90100, 0.50, 45050, 140600, 0.50, 70300),
    ('be4e0582-585b-48a3-b7a5-237e6bd3db08', '9e6a4660-2f83-40e3-bf27-d9590e93dbf2', daterange('2023-08-01', '2024-07-31', '[]'), 90100, 0.50, 45050, 140600, 0.50, 70300),
    ('58732d96-6d0b-4c49-ae03-b916ef108497', '3b94630b-e01e-4b61-b040-8910baf96e97', daterange('2023-08-01', '2024-07-31', '[]'), 90100, 0.50, 45050, 140600, 0.50, 70300),
    ('80caa444-a957-4153-bb2b-2c18f01f17be', '593d8cbf-fcf8-41a7-a5fa-8fe96a7b93d8', daterange('2023-08-01', '2024-07-31', '[]'), 90100, 0.50, 45050, 140600, 0.50, 70300),
    ('42d94a69-ea81-403a-a03e-99a1e6343437', 'c169bce7-6533-4409-8acd-445061f1ff34', daterange('2023-08-01', '2024-07-31', '[]'), 90100, 0.50, 45050, 140600, 0.50, 70300),
    ('3d32b3f0-9033-437f-928d-3167def161aa', '93a50270-3f75-4672-b17d-db721bcb8ed2', daterange('2023-08-01', NULL, '[]'), 90100, 0.50, 45050, 140600, 0.50, 70300),
    ('ada0df70-7441-4bf0-9b0b-f176d62985c0', '9cdef927-f31a-45cb-af49-3ea5b1c1cb8a', daterange('2023-08-01', '2024-07-31', '[]'), 90100, 0.50, 45050, 140600, 0.50, 70300),
    ('fb811658-6d0a-4552-bb07-7ea53cfa690f', '0a58d934-6fd1-11ed-a75e-c353faef5858', daterange('2023-08-01', '2024-07-31', '[]'), 90100, 0.50, 45050, 140600, 0.50, 70300),
    ('cd8f8c24-db93-4643-9ccd-655e7148b019', '0a58db0a-6fd1-11ed-a75e-bbde95c1aded', daterange('2023-08-01', '2024-07-31', '[]'), 90100, 0.50, 45050, 140600, 0.50, 70300),
    ('d8326cbd-a910-4c8e-95cf-446da5838df0', '0a58dbe6-6fd1-11ed-a75e-5335f2b9a91c', daterange('2023-08-01', '2024-07-31', '[]'), 90100, 0.50, 45050, 140600, 0.50, 70300),
    ('e9350573-8030-47ca-9b76-0e1483665aeb', '0a58da38-6fd1-11ed-a75e-9b2790b0b4f5', daterange('2023-08-01', '2024-07-31', '[]'), 90100, 0.50, 45050, 140600, 0.50, 70300),
    ('89f68398-68ae-4890-8fc5-e895a62135f7', '0a58dcae-6fd1-11ed-a75e-b3e10433b949', daterange('2023-08-01', '2024-07-31', '[]'), 90100, 0.50, 45050, 140600, 0.50, 70300),
    ('920c3c3d-e4e1-4226-8207-78135eabacbb', 'fe0972a5-6ce9-41cc-a635-82fb22e7891b', daterange('2023-08-01', NULL, '[]'), 90100, 0.50, 45050, 140600, 0.50, 70300),
    -- 2024-08-01 -
    ('5782b5a0-c673-4d46-8885-131292aeee55', 'f9e7d841-49bf-43e5-8c65-028dad590a76', daterange('2024-08-01', NULL, '[]'), 94900, 0.50, 47450, 148000, 0.50, 74000),
    ('9ec2d5c4-a9b3-4dbc-a59c-9781316dc6e7', '50358394-b961-11eb-b51f-67ac436e5637', daterange('2024-08-01', NULL, '[]'), 94900, 0.50, 47450, 148000, 0.50, 74000),
    ('dab7710c-cb1a-4148-9890-95bc8fa33202', '86ef70a0-bf85-11eb-91e6-1fb57a101165', daterange('2024-08-01', NULL, '[]'), 94900, 0.80, 75920, 148000, 0.80, 118400),
    ('fa8770a4-ee72-4662-9d31-ce55f41d7ba4', '503590f0-b961-11eb-b520-53740af3f7ef', daterange('2024-08-01', NULL, '[]'), 94900, 1.00, 94900, 148000, 1.00, 148000),
    ('eda1c3b4-2de5-4b1b-871a-955892c7a315', '503591ae-b961-11eb-b521-1fca99358eed', daterange('2024-08-01', NULL, '[]'), 94900, 1.00, 94900, 148000, 1.00, 148000),
    ('a80afc4a-93d4-49fc-9894-30c5317d5ce0', '1a2e76e8-64a1-4058-9d86-90753debbead', daterange('2024-08-01', NULL, '[]'), 82500, 1.00, 82500, 82500, 1.00, 82500),
    ('b126ac25-8c5e-41d8-b9df-4e0d2fd9ff8b', '4d6d632d-d8cf-4b5a-8437-decade30d0c0', daterange('2024-08-01', NULL, '[]'), 82500, 1.00, 82500, 82500, 1.00, 82500),
    ('f3c098f1-bcbb-477a-aea0-693f361e6b79', '94e44ef1-106b-401d-81b6-8e5c31cd0437', daterange('2024-08-01', NULL, '[]'), 0, 0, 0, 0, 0, 0),
    ('8298092e-45ee-4106-a4e0-1302ce040978', '21a0589d-d4db-4978-9a37-8e6993a7dafd', daterange('2024-08-01', NULL, '[]'), 94900, 0.50, 47450, 148000, 0.50, 74000),
    ('c1a5c27c-4efb-4417-9f3b-09a8a5fe6b68', '1c5d7ea4-669f-4b4e-8593-353be4c9cea0', daterange('2024-08-01', NULL, '[]'), 94900, 0.50, 47450, 148000, 0.50, 74000),
    ('e0b6297b-4d20-417a-a117-9ab29cee80b1', '9e6a4660-2f83-40e3-bf27-d9590e93dbf2', daterange('2024-08-01', NULL, '[]'), 94900, 0.50, 47450, 148000, 0.50, 74000),
    ('b44372d4-dfeb-4d62-9af6-c92290d1b420', '3b94630b-e01e-4b61-b040-8910baf96e97', daterange('2024-08-01', NULL, '[]'), 94900, 0.50, 47450, 148000, 0.50, 74000),
    ('f7b0ff9d-b415-4092-8033-da470347edea', '593d8cbf-fcf8-41a7-a5fa-8fe96a7b93d8', daterange('2024-08-01', NULL, '[]'), 94900, 0.50, 47450, 148000, 0.50, 74000),
    ('8e242e5b-4881-43af-b96b-63e6813e69ff', 'c169bce7-6533-4409-8acd-445061f1ff34', daterange('2024-08-01', NULL, '[]'), 94900, 0.50, 47450, 148000, 0.50, 74000),
    ('3d05b12f-f335-44ca-95f5-f1a4df303bc4', '9cdef927-f31a-45cb-af49-3ea5b1c1cb8a', daterange('2024-08-01', NULL, '[]'), 94900, 0.50, 47450, 148000, 0.50, 74000),
    ('585e4963-5020-4551-a566-f912d0439b33', '0a58d934-6fd1-11ed-a75e-c353faef5858', daterange('2024-08-01', NULL, '[]'), 94900, 0.50, 47450, 148000, 0.50, 74000),
    ('a4c38a54-1428-415d-a74b-e6e1897b3ee4', '0a58db0a-6fd1-11ed-a75e-bbde95c1aded', daterange('2024-08-01', NULL, '[]'), 94900, 0.50, 47450, 148000, 0.50, 74000),
    ('b19a3191-2f7e-480b-a15e-12e88513471a', '0a58dbe6-6fd1-11ed-a75e-5335f2b9a91c', daterange('2024-08-01', NULL, '[]'), 94900, 0.50, 47450, 148000, 0.50, 74000),
    ('b971038d-a52c-4207-9d9f-003cd994a189', '0a58da38-6fd1-11ed-a75e-9b2790b0b4f5', daterange('2024-08-01', NULL, '[]'), 94900, 0.50, 47450, 148000, 0.50, 74000),
    ('b9284a5b-4671-48d6-8587-67ff89b2553b', '0a58dcae-6fd1-11ed-a75e-b3e10433b949', daterange('2024-08-01', NULL, '[]'), 94900, 0.50, 47450, 148000, 0.50, 74000)
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
