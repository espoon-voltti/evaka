-- SPDX-FileCopyrightText: 2021 City of Tampere
--
-- SPDX-License-Identifier: LGPL-2.1-or-later

INSERT INTO setting (key, value)
VALUES ('DECISION_MAKER_NAME', 'Paula Palvelupäällikkö'),
       ('DECISION_MAKER_TITLE', 'Asiakaspalvelupäällikkö');

INSERT INTO preschool_term (finnish_preschool, swedish_preschool, extended_term, application_period, term_breaks) VALUES
    ('[2023-08-09,2024-05-31]', '[2023-08-09,2024-05-31]', '[2023-08-09,2024-05-31]', '[2022-10-01,2024-05-31]', datemultirange('[2023-10-16,2023-10-22]', '[2023-12-23,2024-01-07]', '[2024-02-26,2024-03-03]', '[2024-03-28,2024-04-01]', '[2024-05-09,2024-05-10]')),
    ('[2024-08-07,2025-05-28]', '[2024-08-07,2025-05-28]', '[2024-08-07,2025-05-28]', '[2024-01-23,2025-05-28]', datemultirange('[2024-10-14,2024-10-20]', '[2024-12-21,2025-01-06]', '[2025-02-24,2025-03-02]', '[2025-04-17,2025-04-21]')),
    ('[2025-08-06,2026-05-29]', '[2025-08-06,2026-05-29]', '[2025-08-06,2026-05-29]', '[2024-08-07,2026-05-29]', datemultirange());

INSERT INTO club_term (term, application_period, term_breaks) VALUES
    ('[2021-08-10,2022-06-03]', '[2021-01-01,2021-08-10]', datemultirange()),
    ('[2022-08-10,2023-06-02]', '[2021-01-01,2022-08-10]', datemultirange()),
    ('[2023-08-09,2024-05-31]', '[2021-01-01,2023-08-09]', datemultirange('[2023-10-16,2023-10-22]', '[2023-12-23,2024-01-07]', '[2024-02-26,2024-03-03]', '[2024-03-28,2024-04-01]', '[2024-05-09,2024-05-10]')),
    ('[2024-08-07,2025-05-28]', '[2024-01-23,2025-05-28]', datemultirange('[2024-10-14,2024-10-20]', '[2024-12-21,2025-01-06]', '[2025-02-24,2025-03-02]', '[2025-04-17,2025-04-21]'));

INSERT INTO daycare (name, type, care_area_id, phone, url, backup_location, opening_date, closing_date, email, schedule, additional_info, unit_manager_name, unit_manager_phone, unit_manager_email, preschool_manager_name, preschool_manager_phone, preschool_manager_email, cost_center, upload_to_varda, capacity, decision_daycare_name, decision_preschool_name, decision_handler, decision_handler_address, street_address, postal_code, post_office, mailing_po_box, location, mailing_street_address, mailing_postal_code, mailing_post_office, invoiced_by_municipality, provider_type, language, upload_to_koski, oph_unit_oid, oph_organizer_oid, ghost_unit, daycare_apply_period, preschool_apply_period, club_apply_period, finance_decision_handler, operation_times, shift_care_operation_times, daily_preschool_time, daily_preparatory_time) VALUES
    ('Päiväkoti A', '{CENTRE}', (SELECT id FROM care_area WHERE name = 'Etelä'), NULL, NULL, NULL, '2021-04-20', NULL, NULL, NULL, NULL, 'Päivi Päiväkodinjohtaja', '0451231234', 'paivi.paivakodinjohtaja@example.com', 'Konsta Koulunjohtaja', '0509879876', 'konsta.koulunjohtaja@example.com', '1', false, 0, 'Päiväkoti A', 'Päiväkoti A', 'Varhaiskasvatusyksikön johtaja', 'Suokatu 10', 'Runkokatu 5', '33340', 'Tampere', 'Runkokatu 5', '(23.60571,61.51667)', NULL, '33340', 'Tampere', TRUE, 'MUNICIPAL', 'fi', false, NULL, NULL, NULL, '[2021-04-20,)', NULL, NULL, NULL, '{"(00:00,23:59)","(00:00,23:59)","(00:00,23:59)","(00:00,23:59)","(00:00,23:59)", null, null}', NULL, NULL, NULL),
    ('Päiväkoti B', '{CENTRE}', (SELECT id FROM care_area WHERE name = 'Etelä'), NULL, NULL, NULL, '2021-04-20', NULL, NULL, NULL, NULL, 'Päivi Päiväkodinjohtaja', '0451231234', 'paivi.paivakodinjohtaja@example.com', 'Konsta Koulunjohtaja', '0509879876', 'konsta.koulunjohtaja@example.com', '1', false, 0, 'Päiväkoti B', 'Päiväkoti B', 'Varhaiskasvatusyksikön johtaja', 'Suokatu 10', 'Kraatarinkatu 5', '33270', 'Tampere', 'Kraatarinkatu 5', '(23.68205,61.5078)', NULL, '33270', 'Tampere', TRUE, 'MUNICIPAL', 'fi', false, NULL, NULL, NULL, '[2021-04-20,)', NULL, NULL, NULL, '{"(00:00,23:59)","(00:00,23:59)","(00:00,23:59)","(00:00,23:59)","(00:00,23:59)", null, null}', NULL, NULL, NULL),
    ('Päiväkoti C', '{CENTRE}', (SELECT id FROM care_area WHERE name = 'Itä'), NULL, NULL, NULL, '2021-04-20', NULL, NULL, NULL, NULL, 'Päivi Päiväkodinjohtaja', '0451231234', 'paivi.paivakodinjohtaja@example.com', 'Konsta Koulunjohtaja', '0509879876', 'konsta.koulunjohtaja@example.com', '2', false, 0, 'Päiväkoti C', 'Päiväkoti C', 'Varhaiskasvatusyksikön johtaja', 'Suokatu 10', 'Maamiehentie 2', '33340', 'TAMPERE', NULL, '(23.59596,61.51136)', NULL, '33340', 'TAMPERE', TRUE, 'MUNICIPAL', 'fi', false, NULL, NULL, NULL, '[2021-04-20,)', NULL, NULL, NULL, '{"(00:00,23:59)","(00:00,23:59)","(00:00,23:59)","(00:00,23:59)","(00:00,23:59)", null, null}', NULL, NULL, NULL),
    ('Palveluseteli A', '{CENTRE}', (SELECT id FROM care_area WHERE name = 'Etelä'), NULL, NULL, NULL, '2021-04-20', NULL, NULL, NULL, NULL, 'Päivi Päiväkodinjohtaja', '0451231234', 'paivi.paivakodinjohtaja@example.com', 'Konsta Koulunjohtaja', '0509879876', 'konsta.koulunjohtaja@example.com', NULL, false, 0, 'Palveluseteli A', 'Palveluseteli A', 'Varhaiskasvatusyksikön johtaja', 'Suokatu 10', 'Sairaalankatu 7', '33100', 'TAMPERE', 'Sairaalankatu 7', '(23.78589,61.49301)', NULL, '33100', 'TAMPERE', false, 'PRIVATE_SERVICE_VOUCHER', 'fi', false, NULL, NULL, NULL, '[2021-04-20,)', NULL, NULL, NULL, '{"(00:00,23:59)","(00:00,23:59)","(00:00,23:59)","(00:00,23:59)","(00:00,23:59)", null, null}', NULL, NULL, NULL),
    ('Palveluseteli B', '{CENTRE}', (SELECT id FROM care_area WHERE name = 'Etelä'), NULL, NULL, NULL, '2021-04-20', NULL, NULL, NULL, NULL, 'Päivi Päiväkodinjohtaja', '0451231234', 'paivi.paivakodinjohtaja@example.com', 'Konsta Koulunjohtaja', '0509879876', 'konsta.koulunjohtaja@example.com', NULL, false, 0, 'Palveluseteli B', 'Palveluseteli B', 'Varhaiskasvatusyksikön johtaja', 'Suokatu 10', 'Kuusitie 8', '36110', 'KANGASALA', 'Kuusitie 8', '(24.00936,61.53011)', NULL, '36110', 'KANGASALA', false, 'PRIVATE_SERVICE_VOUCHER', 'fi', false, NULL, NULL, NULL, '[2021-04-20,)', NULL, NULL, NULL, '{"(00:00,23:59)","(00:00,23:59)","(00:00,23:59)","(00:00,23:59)","(00:00,23:59)", null, null}', NULL, NULL, NULL),
    ('Palveluseteli C', '{CENTRE}', (SELECT id FROM care_area WHERE name = 'Itä'), NULL, NULL, NULL, '2021-04-20', NULL, NULL, NULL, NULL, 'Päivi Päiväkodinjohtaja', '0451231234', 'paivi.paivakodinjohtaja@example.com', 'Konsta Koulunjohtaja', '0509879876', 'konsta.koulunjohtaja@example.com', NULL, false, 0, 'Palveluseteli C', 'Palveluseteli C', 'Varhaiskasvatusyksikön johtaja', 'Suokatu 10', 'Tiiliruukinkatu 1', '33200', 'TAMPERE', 'Tiiliruukinkatu 1', '(23.75582,61.49336)', NULL, '33200', 'TAMPERE', false, 'PRIVATE_SERVICE_VOUCHER', 'fi', false, NULL, NULL, NULL, '[2021-04-20,)', NULL, NULL, NULL, '{"(00:00,23:59)","(00:00,23:59)","(00:00,23:59)","(00:00,23:59)","(00:00,23:59)", null, null}', NULL, NULL, NULL),
    ('Perhepäivähoito', '{FAMILY}', (SELECT id FROM care_area WHERE name = 'Länsi'), NULL, NULL, NULL, '2021-04-20', NULL, NULL, NULL, NULL, 'Päivi Päiväkodinjohtaja', '0451231234', 'paivi.paivakodinjohtaja@example.com', 'Konsta Koulunjohtaja', '0509879876', 'konsta.koulunjohtaja@example.com', NULL, false, 0, 'Perhepäivähoito', 'Perhepäivähoito', 'Varhaiskasvatusyksikön johtaja', 'Suokatu 10', 'Sairaalankatu 7', '33100', 'TAMPERE', 'Sairaalankatu 7', '(23.78589,61.49301)', NULL, '33100', 'TAMPERE', false, 'MUNICIPAL', 'fi', false, NULL, NULL, NULL, '[2021-04-20,)', NULL, NULL, NULL, '{"(00:00,23:59)","(00:00,23:59)","(00:00,23:59)","(00:00,23:59)","(00:00,23:59)", null, null}', NULL, NULL, NULL),
    ('Perhepäivähoitaja A', '{FAMILY}', (SELECT id FROM care_area WHERE name = 'Etelä'), NULL, NULL, NULL, '2021-04-20', NULL, NULL, NULL, NULL, 'Päivi Päiväkodinjohtaja', '0451231234', 'paivi.paivakodinjohtaja@example.com', 'Konsta Koulunjohtaja', '0509879876', 'konsta.koulunjohtaja@example.com', NULL, false, 0, 'Perhepäivähoitaja A', 'Perhepäivähoitaja A', 'Varhaiskasvatusyksikön johtaja', 'Suokatu 10', 'Sairaalankatu 7', '33100', 'TAMPERE', 'Sairaalankatu 7', '(23.78589,61.49301)', NULL, '33100', 'TAMPERE', false, 'MUNICIPAL', 'fi', false, NULL, NULL, NULL, NULL, NULL, NULL, NULL, '{"(00:00,23:59)","(00:00,23:59)","(00:00,23:59)","(00:00,23:59)","(00:00,23:59)", null, null}', NULL, NULL, NULL),
    ('Perhepäivähoitaja B', '{FAMILY}', (SELECT id FROM care_area WHERE name = 'Etelä'), NULL, NULL, NULL, '2021-04-20', NULL, NULL, NULL, NULL, 'Päivi Päiväkodinjohtaja', '0451231234', 'paivi.paivakodinjohtaja@example.com', 'Konsta Koulunjohtaja', '0509879876', 'konsta.koulunjohtaja@example.com', NULL, false, 0, 'Perhepäivähoitaja B', 'Perhepäivähoitaja B', 'Varhaiskasvatusyksikön johtaja', 'Suokatu 10', 'Kuusitie 8', '36110', 'KANGASALA', 'Kuusitie 8', '(24.00936,61.53011)', NULL, '36110', 'KANGASALA', false, 'MUNICIPAL', 'fi', false, NULL, NULL, NULL, NULL, NULL, NULL, NULL, '{"(00:00,23:59)","(00:00,23:59)","(00:00,23:59)","(00:00,23:59)","(00:00,23:59)", null, null}', NULL, NULL, NULL),
    ('Perhepäivähoitaja C', '{FAMILY}', (SELECT id FROM care_area WHERE name = 'Itä'), NULL, NULL, NULL, '2021-04-20', NULL, NULL, NULL, NULL, 'Päivi Päiväkodinjohtaja', '0451231234', 'paivi.paivakodinjohtaja@example.com', 'Konsta Koulunjohtaja', '0509879876', 'konsta.koulunjohtaja@example.com', NULL, false, 0, 'Perhepäivähoitaja C', 'Perhepäivähoitaja C', 'Varhaiskasvatusyksikön johtaja', 'Suokatu 10', 'Tiiliruukinkatu 1', '33200', 'TAMPERE', 'Tiiliruukinkatu 1', '(23.75582,61.49336)', NULL, '33200', 'TAMPERE', false, 'MUNICIPAL', 'fi', false, NULL, NULL, NULL, NULL, NULL, NULL, NULL, '{"(00:00,23:59)","(00:00,23:59)","(00:00,23:59)","(00:00,23:59)","(00:00,23:59)", null, null}', NULL, NULL, NULL),
    ('Kerho A', '{CLUB}', (SELECT id FROM care_area WHERE name = 'Etelä'), NULL, NULL, NULL, '2021-04-20', NULL, NULL, NULL, NULL, 'Päivi Päiväkodinjohtaja', '0451231234', 'paivi.paivakodinjohtaja@example.com', 'Konsta Koulunjohtaja', '0509879876', 'konsta.koulunjohtaja@example.com', NULL, false, 0, 'Kerho A', 'Kerho A', 'Varhaiskasvatusyksikön johtaja', 'Suokatu 10', 'Ollinojankatu 25', '33400', 'TAMPERE', 'Ollinojankatu 25', '(23.6701,61.525)', NULL, '33400', 'TAMPERE', false, 'MUNICIPAL', 'fi', false, NULL, NULL, NULL, NULL, NULL, '[2021-04-20,)', NULL, '{"(00:00,23:59)","(00:00,23:59)","(00:00,23:59)","(00:00,23:59)","(00:00,23:59)", null, null}', NULL, NULL, NULL),
    ('Kerho B', '{CLUB}', (SELECT id FROM care_area WHERE name = 'Etelä'), NULL, NULL, NULL, '2021-04-20', NULL, NULL, NULL, NULL, 'Päivi Päiväkodinjohtaja', '0451231234', 'paivi.paivakodinjohtaja@example.com', 'Konsta Koulunjohtaja', '0509879876', 'konsta.koulunjohtaja@example.com', NULL, false, 0, 'Kerho B', 'Kerho B', 'Varhaiskasvatusyksikön johtaja', 'Suokatu 10', 'Pispalan valtatie 79', '33270', 'TAMPERE', NULL, '(23.69093,61.50705)', NULL, '33270', 'TAMPERE', false, 'MUNICIPAL', 'fi', false, '', '', false, NULL, NULL, '[2021-04-20,)', NULL, '{"(00:00,23:59)","(00:00,23:59)","(00:00,23:59)","(00:00,23:59)","(00:00,23:59)", null, null}', NULL, NULL, NULL),
    ('Kerho C', '{CLUB}', (SELECT id FROM care_area WHERE name = 'Itä'), NULL, NULL, NULL, '2021-04-20', NULL, NULL, NULL, NULL, 'Päivi Päiväkodinjohtaja', '0451231234', 'paivi.paivakodinjohtaja@example.com', 'Konsta Koulunjohtaja', '0509879876', 'konsta.koulunjohtaja@example.com', NULL, false, 0, 'Kerho C', 'Kerho C', 'Varhaiskasvatusyksikön johtaja', 'Suokatu 10', 'Hämeenpuisto 10', '33210', 'TAMPERE', NULL, '(23.7493,61.50095)', NULL, '33210', 'TAMPERE', false, 'MUNICIPAL', 'fi', false, '', '', false, NULL, NULL, '[2021-04-20,)', NULL, '{"(00:00,23:59)","(00:00,23:59)","(00:00,23:59)","(00:00,23:59)","(00:00,23:59)", null, null}', NULL, NULL, NULL),
    ('Koulupolun mukainen esiopetus', '{PRESCHOOL}', (SELECT id FROM care_area WHERE name = 'Länsi'), NULL, NULL, NULL, '2021-04-20', NULL, NULL, NULL, NULL, 'Päivi Päiväkodinjohtaja', '0451231234', 'paivi.paivakodinjohtaja@example.com', 'Konsta Koulunjohtaja', '0509879876', 'konsta.koulunjohtaja@example.com', NULL, false, 0, '', '', '', '', 'Runkokatu 5', '33340', 'Tampere', 'Runkokatu 5', '(23.60571,61.51667)', NULL, '33340', 'Tampere', false, 'MUNICIPAL', 'fi', false, NULL, NULL, NULL, NULL, '[2021-04-20,)', NULL, NULL, '{"(00:00,23:59)","(00:00,23:59)","(00:00,23:59)","(00:00,23:59)","(00:00,23:59)", null, null}', NULL, ('09:00'::time, '13:00'::time), NULL),
    ('Koulu A', '{PRESCHOOL}', (SELECT id FROM care_area WHERE name = 'Etelä'), NULL, NULL, NULL, '2021-04-20', NULL, NULL, NULL, NULL, 'Päivi Päiväkodinjohtaja', '0451231234', 'paivi.paivakodinjohtaja@example.com', 'Konsta Koulunjohtaja', '0509879876', 'konsta.koulunjohtaja@example.com', NULL, false, 0, 'Koulu A', 'Koulu A', 'Varhaiskasvatusyksikön johtaja', 'Suokatu 10', 'Runkokatu 5', '33340', 'Tampere', 'Runkokatu 5', '(23.60571,61.51667)', NULL, '33340', 'Tampere', false, 'MUNICIPAL', 'fi', false, NULL, NULL, NULL, NULL, NULL, NULL, NULL, '{"(00:00,23:59)","(00:00,23:59)","(00:00,23:59)","(00:00,23:59)","(00:00,23:59)", null, null}', NULL, ('09:00'::time, '13:00'::time), NULL),
    ('Koulu B', '{PRESCHOOL}', (SELECT id FROM care_area WHERE name = 'Etelä'), NULL, NULL, NULL, '2021-04-20', NULL, NULL, NULL, NULL, 'Päivi Päiväkodinjohtaja', '0451231234', 'paivi.paivakodinjohtaja@example.com', 'Konsta Koulunjohtaja', '0509879876', 'konsta.koulunjohtaja@example.com', NULL, false, 0, 'Koulu B', 'Koulu B', 'Varhaiskasvatusyksikön johtaja', 'Suokatu 10', 'Runkokatu 5', '33340', 'Tampere', 'Runkokatu 5', '(23.60571,61.51667)', NULL, '33340', 'Tampere', false, 'MUNICIPAL', 'fi', false, NULL, NULL, NULL, NULL, NULL, NULL, NULL, '{"(00:00,23:59)","(00:00,23:59)","(00:00,23:59)","(00:00,23:59)","(00:00,23:59)", null, null}', NULL, ('09:00'::time, '13:00'::time), NULL),
    ('Koulu C', '{PRESCHOOL}', (SELECT id FROM care_area WHERE name = 'Itä'), NULL, NULL, NULL, '2021-04-20', NULL, NULL, NULL, NULL, 'Päivi Päiväkodinjohtaja', '0451231234', 'paivi.paivakodinjohtaja@example.com', 'Konsta Koulunjohtaja', '0509879876', 'konsta.koulunjohtaja@example.com', NULL, false, 0, 'Koulu C', 'Koulu C', 'Varhaiskasvatusyksikön johtaja', 'Suokatu 10', 'Runkokatu 5', '33340', 'Tampere', 'Runkokatu 5', '(23.60571,61.51667)', NULL, '33340', 'Tampere', false, 'MUNICIPAL', 'fi', false, NULL, NULL, NULL, NULL, NULL, NULL, NULL, '{"(00:00,23:59)","(00:00,23:59)","(00:00,23:59)","(00:00,23:59)","(00:00,23:59)", null, null}', NULL, ('09:00'::time, '13:00'::time), NULL);

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
UPDATE employee SET external_id = 'tampere-ad:' || id WHERE external_id IS NULL;

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
    2000, 800, 2000, 800
),
(
    daterange('2022-08-01', '2023-02-28', '[]'),
    29500, 2800,
    291300, 375800, 426700, 477700, 528400,
    0.1070, 0.1070, 0.1070, 0.1070, 0.1070,
    566600, 651100, 702000, 753000, 803700,
    19700,
    0.6, 0.8,
    2000, 800, 2000, 800
),
(
    daterange('2021-08-01', '2022-07-31', '[]'),
    28800, 2700,
    279800, 361000, 409900, 458800, 507500,
    0.1070, 0.1070, 0.1070, 0.1070, 0.1070,
    548500, 629700, 678600, 727500, 776200,
    18900,
    0.6, 0.8,
    2000, 800, 2000, 800
),
(
    daterange('2020-08-01', '2021-07-31', '[]'),
    28800, 2700,
    213600, 275600, 312900, 350200, 387400,
    0.1070, 0.1070, 0.1070, 0.1070, 0.1070,
    482300, 544300, 581600, 618900, 656100,
    14400,
    0.5, 0.8,
    2000, 800, 2000, 800
),
(
    daterange('2018-08-01', '2020-07-31', '[]'),
    28900, 2700,
    210200, 271300, 308000, 344700, 381300,
    0.1070, 0.1070, 0.1070, 0.1070, 0.1070,
    479900, 541000, 577700, 614400, 651000,
    14200,
    0.5, 0.8,
    2000, 800, 2000, 800
);

INSERT INTO service_need_option_voucher_value
    (id, service_need_option_id, validity, base_value, coefficient, value, base_value_under_3y, coefficient_under_3y, value_under_3y)
VALUES
    -- 2000-01-01 - 2022-07-31
    ('422ee7f0-e7ea-11ec-8813-872424fb3290', '50358394-b961-11eb-b51f-67ac436e5636', daterange('2000-01-01', '2022-07-31', '[]'), 82600, 1.00, 82600, 124700, 1.00, 124700),
    ('422f00d2-e7ea-11ec-8813-cb592eaf8fcf', '86ef70a0-bf85-11eb-91e6-1fb57a101161', daterange('2000-01-01', '2022-07-31', '[]'), 82600, 1.00, 82600, 124700, 1.00, 124700),
    ('422f05dc-e7ea-11ec-8813-b3668e81d223', '503590f0-b961-11eb-b520-53740af3f7ee', daterange('2000-01-01', '2022-07-31', '[]'), 82600, 0.60, 49560, 124700, 0.60, 74820),
    ('422f09ec-e7ea-11ec-8813-f329e57bcb32', '503591ae-b961-11eb-b521-1fca99358eef', daterange('2000-01-01', '2022-07-31', '[]'), 82600, 0.80, 66080, 124700, 0.80, 99760),
    ('422f0d5c-e7ea-11ec-8813-cb1cd6093b78', '50359212-b961-11eb-b522-074fb05f7086', daterange('2000-01-01', '2022-07-31', '[]'), 82600, 0.60, 49560, 124700, 0.60, 74820),
    ('422f1130-e7ea-11ec-8813-cbfb64c2f2bf', '86ef7370-bf85-11eb-91e7-6fcd728c518d', daterange('2000-01-01', '2022-07-31', '[]'), 82600, 0.60, 49560, 124700, 0.60, 74820),
    ('422f1504-e7ea-11ec-8813-4b2e990ac90a', '50359280-b961-11eb-b523-237115533645', daterange('2000-01-01', '2022-07-31', '[]'), 82600, 0.36, 29736, 124700, 0.36, 44892),
    ('422f19be-e7ea-11ec-8813-43a22fd8beaf', '503592da-b961-11eb-b524-7f27c780d83a', daterange('2000-01-01', '2022-07-31', '[]'), 82600, 0.48, 39648, 124700, 0.48, 59856),
    ('422f1dba-e7ea-11ec-8813-1fdc7fa64ae4', '50359334-b961-11eb-b525-f3febdfea5d3', daterange('2000-01-01', '2022-07-31', '[]'), 82600, 0.60, 49560, 124700, 0.60, 74820),
    ('422f20f8-e7ea-11ec-8813-6fdb782754ed', '5035938e-b961-11eb-b526-6b30323c87a8', daterange('2000-01-01', '2022-07-31', '[]'), 82600, 1.00, 82600, 124700, 1.00, 124700),
    ('9eea7de7-80f8-4d02-bc86-e90946b47b3a', 'cfeae50c-20c3-45ff-be28-e78f71b8bed1', daterange('2000-01-01', '2022-07-31', '[]'), 82600, 1.00, 82600, 124700, 1.00, 124700),
    ('422f2440-e7ea-11ec-8813-3720f593ba77', '0bfc6c92-ff2a-11eb-a785-2724e8e5e7ee', daterange('2000-01-01', '2022-07-31', '[]'), 82600, 0.50, 41300, 124700, 0.50, 62350),
    ('422f2800-e7ea-11ec-8813-e7347cbbb31b', '503593e8-b961-11eb-b527-a3dcdfb628ec', daterange('2000-01-01', '2022-07-31', '[]'), 82600, 0.50, 41300, 124700, 0.50, 62350),
    ('422f2be8-e7ea-11ec-8813-2bff85b439a2', '50359442-b961-11eb-b528-df3290c0d63e', daterange('2000-01-01', '2022-07-31', '[]'), 82600, 0.50, 41300, 124700, 0.50, 62350),
    ('422f300c-e7ea-11ec-8813-d33333dc7f46', 'bc6a42d0-fa74-11eb-9a2b-d315a7916074', daterange('2000-01-01', '2022-07-31', '[]'), 82600, 0.30, 24780, 124700, 0.30, 37410),
    ('422f33cc-e7ea-11ec-8813-cf5d2fbb4dfe', 'bc6a44ec-fa74-11eb-9a2c-73b53c2af869', daterange('2000-01-01', '2022-07-31', '[]'), 82600, 0.40, 33040, 124700, 0.40, 49880),
    ('422f3796-e7ea-11ec-8813-a7107e0ebf0f', 'bc6a4550-fa74-11eb-9a2d-035acd5db9aa', daterange('2000-01-01', '2022-07-31', '[]'), 82600, 0.30, 24780, 124700, 0.30, 37410),
    ('422f3b60-e7ea-11ec-8813-f3318d9cf8c1', 'bc6a45a0-fa74-11eb-9a2e-fb411a8588da', daterange('2000-01-01', '2022-07-31', '[]'), 82600, 0.40, 33040, 124700, 0.40, 49880),
    ('422f3f52-e7ea-11ec-8813-bbc69dcf3860', 'ff6ddcd4-fa8a-11eb-8592-2f2b4e398fcb', daterange('2000-01-01', '2022-07-31', '[]'), 82600, 0.00, 0, 124700, 0.00, 0),
    ('422f43f8-e7ea-11ec-8813-af10257c13ae', '1b4413f6-d99d-11eb-89ac-a3a978104bce', daterange('2000-01-01', '2022-07-31', '[]'), 82600, 1.00, 82600, 124700, 1.00, 124700),
    ('6c06939b-c129-460d-8c90-bdf8466c7d18', 'e20929c1-719a-42e1-93ca-da74fce495f8', daterange('2000-01-01', '2022-07-31', '[]'), 82600, 1.00, 82600, 124700, 1.00, 124700),
    -- 2022-08-01 - 2023-07-31
    ('422f47ea-e7ea-11ec-8813-cbbf8463041a', '50358394-b961-11eb-b51f-67ac436e5636', daterange('2022-08-01', '2023-07-31', '[]'), 86200, 1.00, 86200, 130200, 1.00, 130200),
    ('422f4be6-e7ea-11ec-8813-1b45ad37a47d', '86ef70a0-bf85-11eb-91e6-1fb57a101161', daterange('2022-08-01', '2023-07-31', '[]'), 86200, 1.00, 86200, 130200, 1.00, 130200),
    ('422f4fba-e7ea-11ec-8813-7bf3e6b54280', '503590f0-b961-11eb-b520-53740af3f7ee', daterange('2022-08-01', '2023-07-31', '[]'), 86200, 0.60, 51720, 130200, 0.60, 78120),
    ('422f538e-e7ea-11ec-8813-0b41ec608608', '503591ae-b961-11eb-b521-1fca99358eef', daterange('2022-08-01', '2023-07-31', '[]'), 86200, 1.00, 86200, 130200, 1.00, 130200),
    ('422f5762-e7ea-11ec-8813-9b28d449fc32', '50359212-b961-11eb-b522-074fb05f7086', daterange('2022-08-01', '2023-07-31', '[]'), 86200, 0.60, 51720, 130200, 0.60, 78120),
    ('422f5b40-e7ea-11ec-8813-bf2e9fdae637', '86ef7370-bf85-11eb-91e7-6fcd728c518d', daterange('2022-08-01', '2023-07-31', '[]'), 86200, 0.60, 51720, 130200, 0.60, 78120),
    ('422f5f46-e7ea-11ec-8813-db5e80cf9a6b', '50359280-b961-11eb-b523-237115533645', daterange('2022-08-01', '2023-07-31', '[]'), 86200, 0.36, 31032, 130200, 0.36, 46872),
    ('422f6324-e7ea-11ec-8813-bfb1f547b2b4', '503592da-b961-11eb-b524-7f27c780d83a', daterange('2022-08-01', '2023-07-31', '[]'), 86200, 0.60, 51720, 130200, 0.60, 78120),
    ('422f6752-e7ea-11ec-8813-ebc71789683c', '50359334-b961-11eb-b525-f3febdfea5d3', daterange('2022-08-01', '2023-07-31', '[]'), 86200, 0.60, 51720, 130200, 0.60, 78120),
    ('422f6b58-e7ea-11ec-8813-3f6c93576cd8', '5035938e-b961-11eb-b526-6b30323c87a8', daterange('2022-08-01', '2023-07-31', '[]'), 86200, 1.00, 86200, 130200, 1.00, 130200),
    ('0c58aaf4-e4e8-46a3-8fe9-70ad1b2b1c3d', 'cfeae50c-20c3-45ff-be28-e78f71b8bed1', daterange('2022-08-01', '2023-07-31', '[]'), 86200, 1.00, 86200, 130200, 1.00, 130200),
    ('422f6f18-e7ea-11ec-8813-2f4bf5f3fdff', '0bfc6c92-ff2a-11eb-a785-2724e8e5e7ee', daterange('2022-08-01', '2023-07-31', '[]'), 86200, 0.50, 43100, 130200, 0.50, 65100),
    ('422f72ec-e7ea-11ec-8813-f7fcf18156cd', '503593e8-b961-11eb-b527-a3dcdfb628ec', daterange('2022-08-01', '2023-07-31', '[]'), 86200, 0.50, 43100, 130200, 0.50, 65100),
    ('422f76d4-e7ea-11ec-8813-1f7c5066085e', '50359442-b961-11eb-b528-df3290c0d63e', daterange('2022-08-01', '2023-07-31', '[]'), 86200, 0.50, 43100, 130200, 0.50, 65100),
    ('422f7ac6-e7ea-11ec-8813-d79f12b35fe8', 'bc6a42d0-fa74-11eb-9a2b-d315a7916074', daterange('2022-08-01', '2023-07-31', '[]'), 86200, 0.30, 25860, 130200, 0.30, 39060),
    ('422f7eae-e7ea-11ec-8813-87c6c3962177', 'bc6a44ec-fa74-11eb-9a2c-73b53c2af869', daterange('2022-08-01', '2023-07-31', '[]'), 86200, 0.50, 43100, 130200, 0.50, 65100),
    ('422f8296-e7ea-11ec-8813-27f63223bec5', 'bc6a4550-fa74-11eb-9a2d-035acd5db9aa', daterange('2022-08-01', '2023-07-31', '[]'), 86200, 0.30, 25860, 130200, 0.30, 39060),
    ('422f867e-e7ea-11ec-8813-0fc228bb529d', 'bc6a45a0-fa74-11eb-9a2e-fb411a8588da', daterange('2022-08-01', '2023-07-31', '[]'), 86200, 0.50, 43100, 130200, 0.50, 65100),
    ('f91b730a-6fd2-11ed-a75e-378477307155', '0a58d934-6fd1-11ed-a75e-c353faef5857', daterange('2022-08-01', '2023-07-31', '[]'), 86200, 0.50, 43100, 130200, 0.50, 65100),
    ('f91b8390-6fd2-11ed-a75e-7bb95afe470a', '0a58db0a-6fd1-11ed-a75e-bbde95c1adef', daterange('2022-08-01', '2023-07-31', '[]'), 86200, 0.30, 25860, 130200, 0.30, 39060),
    ('f91b8728-6fd2-11ed-a75e-3f57230a539d', '0a58dbe6-6fd1-11ed-a75e-5335f2b9a91b', daterange('2022-08-01', '2023-07-31', '[]'), 86200, 0.50, 43100, 130200, 0.50, 65100),
    ('f91b7f12-6fd2-11ed-a75e-539b788b60bb', '0a58da38-6fd1-11ed-a75e-9b2790b0b4f5', daterange('2022-08-01', '2023-07-31', '[]'), 86200, 0.50, 43100, 130200, 0.50, 65100),
    ('f91b8a66-6fd2-11ed-a75e-5fbc4afc5fd3', '0a58dcae-6fd1-11ed-a75e-b3e10433b948', daterange('2022-08-01', '2023-07-31', '[]'), 86200, 0.30, 25860, 130200, 0.30, 39060),
    ('f91b8d90-6fd2-11ed-a75e-a7c9e8f18799', '0a58dd94-6fd1-11ed-a75e-8390cdc6af62', daterange('2022-08-01', '2023-07-31', '[]'), 86200, 0.50, 43100, 130200, 0.50, 65100),
    ('a1fa1430-6647-11ed-8202-bb4dc37378c9', '88f3bf1e-6646-11ed-8202-8f213a9146c2', daterange('2022-08-01', '2023-07-31', '[]'), 86200, 0.50, 43100, 130200, 0.50, 65100),
    ('a1fb440e-6647-11ed-8202-2b499279fe71', '88f3dfd0-6646-11ed-8202-4fe90b8e5485', daterange('2022-08-01', '2023-07-31', '[]'), 86200, 0.50, 43100, 130200, 0.50, 65100),
    ('a1fb4828-6647-11ed-8202-d7d64698f305', '88f3e214-6646-11ed-8202-f77aa4749644', daterange('2022-08-01', '2023-07-31', '[]'), 86200, 0.50, 43100, 130200, 0.50, 65100),
    ('a1fb4b8e-6647-11ed-8202-b72f51e3f77d', '88f3e3e0-6646-11ed-8202-3bc1b45aaa73', daterange('2022-08-01', '2023-07-31', '[]'), 86200, 0.30, 25860, 130200, 0.30, 39060),
    ('a1fb4ee0-6647-11ed-8202-db7d7e99fa43', '88f3e5a2-6646-11ed-8202-0b8db1a29ca5', daterange('2022-08-01', '2023-07-31', '[]'), 86200, 0.50, 43100, 130200, 0.50, 65100),
    ('a1fb5250-6647-11ed-8202-5ba5a6f01f31', '88f3e75a-6646-11ed-8202-b7867ae6358a', daterange('2022-08-01', '2023-07-31', '[]'), 86200, 0.30, 25860, 130200, 0.30, 39060),
    ('a1fb5606-6647-11ed-8202-972da272f877', '88f3e912-6646-11ed-8202-bb9fe8059b4a', daterange('2022-08-01', '2023-07-31', '[]'), 86200, 0.50, 43100, 130200, 0.50, 65100),
    ('422f8a52-e7ea-11ec-8813-87e075796747', 'ff6ddcd4-fa8a-11eb-8592-2f2b4e398fcb', daterange('2022-08-01', '2023-07-31', '[]'), 86200, 0.00, 0, 130200, 0.00, 0),
    ('422f8ee4-e7ea-11ec-8813-e7010d4c8ab9', '1b4413f6-d99d-11eb-89ac-a3a978104bce', daterange('2022-08-01', '2023-07-31', '[]'), 86200, 1.00, 86200, 130200, 1.00, 130200),
    ('5a6c3bda-5c01-4841-8ed1-a257990fee55', 'e20929c1-719a-42e1-93ca-da74fce495f8', daterange('2022-08-01', '2023-07-31', '[]'), 86200, 1.00, 86200, 130200, 1.00, 130200),
    -- 2023-08-01 - 2024-07-31
    ('0690a5e2-f948-477f-9532-c88381c28082', '50358394-b961-11eb-b51f-67ac436e5636', daterange('2023-08-01', '2024-07-31', '[]'), 90100, 1.00, 90100, 144100, 1.00, 144100),
    ('18d41b75-018c-49c3-92fe-f787afe3ac69', '86ef70a0-bf85-11eb-91e6-1fb57a101161', daterange('2023-08-01', '2024-07-31', '[]'), 90100, 1.00, 90100, 144100, 1.00, 144100),
    ('07a1faaa-5fb5-4737-b966-210bf39ea023', '503590f0-b961-11eb-b520-53740af3f7ee', daterange('2023-08-01', '2024-07-31', '[]'), 90100, 0.60, 54060, 144100, 0.60, 86460),
    ('a7080fb4-06cb-41fc-9e59-7ca11c554c69', '503591ae-b961-11eb-b521-1fca99358eef', daterange('2023-08-01', '2024-07-31', '[]'), 90100, 1.00, 90100, 144100, 1.00, 144100),
    ('96aa6c4a-1bbf-40a5-a207-047d7d59534b', '50359212-b961-11eb-b522-074fb05f7086', daterange('2023-08-01', '2024-07-31', '[]'), 90100, 0.60, 54060, 144100, 0.60, 86460),
    ('6b9b54c2-1741-4707-81b3-a3aee54b251e', '86ef7370-bf85-11eb-91e7-6fcd728c518d', daterange('2023-08-01', '2024-07-31', '[]'), 90100, 0.60, 54060, 144100, 0.60, 86460),
    ('d0135557-f852-453b-b883-adfb091a9b40', '50359280-b961-11eb-b523-237115533645', daterange('2023-08-01', '2024-07-31', '[]'), 90100, 0.36, 32436, 144100, 0.36, 51876),
    ('6adb7434-09a9-4eaa-8400-c33be074f0d1', '503592da-b961-11eb-b524-7f27c780d83a', daterange('2023-08-01', '2024-07-31', '[]'), 90100, 0.60, 54060, 144100, 0.60, 86460),
    ('6ba92ca9-1b1f-4519-83ca-954746becc90', '50359334-b961-11eb-b525-f3febdfea5d3', daterange('2023-08-01', '2024-07-31', '[]'), 90100, 0.60, 54060, 144100, 0.60, 86460),
    ('09c031e8-0381-4174-9c83-d0dc581b123f', '5035938e-b961-11eb-b526-6b30323c87a8', daterange('2023-08-01', '2024-07-31', '[]'), 90100, 1.00, 90100, 144100, 1.00, 144100),
    ('c93354a3-027b-4584-b834-a2beda142f73', 'cfeae50c-20c3-45ff-be28-e78f71b8bed1', daterange('2023-08-01', '2024-07-31', '[]'), 90100, 1.00, 90100, 144100, 1.00, 144100),
    ('43e2fd79-e481-4c75-bab2-01fc2a4a2737', '0bfc6c92-ff2a-11eb-a785-2724e8e5e7ee', daterange('2023-08-01', '2024-07-31', '[]'), 90100, 0.50, 45050, 144100, 0.50, 72050),
    ('1c5fd8d2-fa99-4cd0-bf6e-fa54e5b724c8', '503593e8-b961-11eb-b527-a3dcdfb628ec', daterange('2023-08-01', '2024-07-31', '[]'), 90100, 0.50, 45050, 144100, 0.50, 72050),
    ('2df06661-2311-4ea4-a24c-638dc8d4046f', '50359442-b961-11eb-b528-df3290c0d63e', daterange('2023-08-01', '2024-07-31', '[]'), 90100, 0.50, 45050, 144100, 0.50, 72050),
    ('2f3ce401-c228-415a-9d9c-ea32665d662c', 'bc6a42d0-fa74-11eb-9a2b-d315a7916074', daterange('2023-08-01', '2024-07-31', '[]'), 90100, 0.30, 27030, 144100, 0.30, 43230),
    ('3e59985e-2f6c-46bb-848a-70b1c05b57ac', 'bc6a44ec-fa74-11eb-9a2c-73b53c2af869', daterange('2023-08-01', '2024-07-31', '[]'), 90100, 0.50, 45050, 144100, 0.50, 72050),
    ('db128ece-ec2d-469c-a642-9bea70887537', 'bc6a4550-fa74-11eb-9a2d-035acd5db9aa', daterange('2023-08-01', '2024-07-31', '[]'), 90100, 0.30, 27030, 144100, 0.30, 43230),
    ('35d15d5e-92d9-452c-b8d0-bbb5c7f796fe', 'bc6a45a0-fa74-11eb-9a2e-fb411a8588da', daterange('2023-08-01', '2024-07-31', '[]'), 90100, 0.50, 45050, 144100, 0.50, 72050),
    ('c4fb1d53-24aa-48a6-b168-3f9f2c67f727', '0a58d934-6fd1-11ed-a75e-c353faef5857', daterange('2023-08-01', '2024-07-31', '[]'), 90100, 0.50, 45050, 144100, 0.50, 72050),
    ('22b4139f-3717-409e-8a43-1208442aa7aa', '0a58db0a-6fd1-11ed-a75e-bbde95c1adef', daterange('2023-08-01', '2024-07-31', '[]'), 90100, 0.30, 27030, 144100, 0.30, 43230),
    ('f310794c-868f-4d6e-8a89-0cebe039c0c0', '0a58dbe6-6fd1-11ed-a75e-5335f2b9a91b', daterange('2023-08-01', '2024-07-31', '[]'), 90100, 0.50, 45050, 144100, 0.50, 72050),
    ('e2b8fe16-a79c-44ff-bbae-4ebd14d9dbb2', '0a58da38-6fd1-11ed-a75e-9b2790b0b4f5', daterange('2023-08-01', '2024-07-31', '[]'), 90100, 0.50, 45050, 144100, 0.50, 72050),
    ('f336080f-c067-4010-9697-a04d369bd1f6', '0a58dcae-6fd1-11ed-a75e-b3e10433b948', daterange('2023-08-01', '2024-07-31', '[]'), 90100, 0.30, 27030, 144100, 0.30, 43230),
    ('9e51cb23-f712-4c74-9651-5a7cc7de13c5', '0a58dd94-6fd1-11ed-a75e-8390cdc6af62', daterange('2023-08-01', '2024-07-31', '[]'), 90100, 0.50, 45050, 144100, 0.50, 72050),
    ('8fc6d2da-5a8d-4fbf-8c37-df3a0232753e', '9cc7713c-f153-489a-a7c2-e001640f5c29', daterange('2023-08-01', '2024-07-31', '[]'), 90100, 0.50, 45050, 144100, 0.50, 72050),
    ('977dfe0b-e9b8-42a9-b470-4ad803d41978', '816d36b5-29b3-494b-8ac4-b2298764c803', daterange('2023-08-01', '2024-07-31', '[]'), 90100, 0.50, 45050, 144100, 0.50, 72050),
    ('55ab92c4-80e9-4ac9-9b1e-769ffa468e58', '9a2b393b-ebb6-49d9-aa1e-b86d80ba9eb5', daterange('2023-08-01', '2024-07-31', '[]'), 90100, 0.30, 27030, 144100, 0.30, 43230),
    ('88f26cb8-4d10-46d8-9498-2a6e88e835a2', 'efcfd8f4-92d9-43a4-91e0-a7e2fde9c4e5', daterange('2023-08-01', '2024-07-31', '[]'), 90100, 0.50, 45050, 144100, 0.50, 72050),
    ('e818e30c-9c98-4344-850d-cbc6e347676f', '313744dc-ac9e-46b1-8004-940d429664e3', daterange('2023-08-01', '2024-07-31', '[]'), 90100, 0.50, 45050, 144100, 0.50, 72050),
    ('90d0b635-f894-478a-a5c5-b369cddf5d7b', '9e1e61da-3362-4f8f-bbf9-ea1eb0d0206c', daterange('2023-08-01', '2024-07-31', '[]'), 90100, 0.30, 27030, 144100, 0.30, 43230),
    ('8ffb819e-d2fc-44af-94a8-6ccb14a10fdc', 'b5269ff0-a047-496f-9bda-6286520ef1a5', daterange('2023-08-01', '2024-07-31', '[]'), 90100, 0.50, 45050, 144100, 0.50, 72050),
    ('61803666-bb5e-421d-bc62-81b94811514f', '88f3bf1e-6646-11ed-8202-8f213a9146c2', daterange('2023-08-01', '2024-07-31', '[]'), 90100, 0.50, 45050, 144100, 0.50, 72050),
    ('ae4fc78f-b76d-4af6-b473-4e3b61dbcaf4', '88f3dfd0-6646-11ed-8202-4fe90b8e5485', daterange('2023-08-01', '2024-07-31', '[]'), 90100, 0.50, 45050, 144100, 0.50, 72050),
    ('af58a9aa-40e8-4086-b578-b4b35dcb7319', '88f3e214-6646-11ed-8202-f77aa4749644', daterange('2023-08-01', '2024-07-31', '[]'), 90100, 0.50, 45050, 144100, 0.50, 72050),
    ('f4b8c2a3-7967-441f-921c-da3e872085ef', '88f3e3e0-6646-11ed-8202-3bc1b45aaa73', daterange('2023-08-01', '2024-07-31', '[]'), 90100, 0.30, 27030, 144100, 0.30, 43230),
    ('f5d22daf-b34d-4e9e-9c20-7719f1028bca', '88f3e5a2-6646-11ed-8202-0b8db1a29ca5', daterange('2023-08-01', '2024-07-31', '[]'), 90100, 0.50, 45050, 144100, 0.50, 72050),
    ('c1ecd850-ebef-443b-aa48-a4b3459d15e3', '88f3e75a-6646-11ed-8202-b7867ae6358a', daterange('2023-08-01', '2024-07-31', '[]'), 90100, 0.30, 27030, 144100, 0.30, 43230),
    ('9f7b84ec-c4e1-4859-8b43-afa7bba1e0bc', '88f3e912-6646-11ed-8202-bb9fe8059b4a', daterange('2023-08-01', '2024-07-31', '[]'), 90100, 0.50, 45050, 144100, 0.50, 72050),
    ('93140aa5-2da1-4cdf-931d-530127e9352c', 'ff6ddcd4-fa8a-11eb-8592-2f2b4e398fcb', daterange('2023-08-01', '2024-07-31', '[]'), 90100, 0.00, 0, 144100, 0.00, 0),
    ('637a788d-b2d5-467b-ab3b-9f9549dcf1c9', '1b4413f6-d99d-11eb-89ac-a3a978104bce', daterange('2023-08-01', '2024-07-31', '[]'), 90100, 1.00, 90100, 144100, 1.00, 144100),
    ('7e0c104a-8ab4-4c2a-92bc-e5f64a05a1ff', 'e20929c1-719a-42e1-93ca-da74fce495f8', daterange('2023-08-01', '2024-07-31', '[]'), 90100, 1.00, 90100, 144100, 1.00, 144100),
    -- 2024-08-01 -
    ('69125a0a-093f-11ef-bcd2-63643645eb52', '50358394-b961-11eb-b51f-67ac436e5636', daterange('2024-08-01', NULL, '[]'), 94900, 1.00, 94900, 151900, 1.00, 151900),
    ('69125a64-093f-11ef-bcd2-536c12dda383', '86ef70a0-bf85-11eb-91e6-1fb57a101161', daterange('2024-08-01', NULL, '[]'), 94900, 1.00, 94900, 151900, 1.00, 151900),
    ('69125a78-093f-11ef-bcd2-57db29092b4e', '503590f0-b961-11eb-b520-53740af3f7ee', daterange('2024-08-01', NULL, '[]'), 94900, 0.60, 56940, 151900, 0.60, 91140),
    ('69125a8c-093f-11ef-bcd2-d7c3de264a75', '503591ae-b961-11eb-b521-1fca99358eef', daterange('2024-08-01', NULL, '[]'), 94900, 1.00, 94900, 151900, 1.00, 151900),
    ('69125aa0-093f-11ef-bcd2-833edc3d0652', '50359212-b961-11eb-b522-074fb05f7086', daterange('2024-08-01', NULL, '[]'), 94900, 0.60, 56940, 151900, 0.60, 91140),
    ('69125abe-093f-11ef-bcd2-c7c29edca3a2', '86ef7370-bf85-11eb-91e7-6fcd728c518d', daterange('2024-08-01', NULL, '[]'), 94900, 0.60, 56940, 151900, 0.60, 91140),
    ('69125ad2-093f-11ef-bcd2-ab6dcbe5c5a2', '50359280-b961-11eb-b523-237115533645', daterange('2024-08-01', NULL, '[]'), 56940, 0.50, 28470, 91140, 0.50, 45570),
    ('69125ae6-093f-11ef-bcd2-4b666441a7fd', '503592da-b961-11eb-b524-7f27c780d83a', daterange('2024-08-01', NULL, '[]'), 56940, 0.75, 42705, 91140, 0.75, 68355),
    ('69125afa-093f-11ef-bcd2-dffff21dcedb', '50359334-b961-11eb-b525-f3febdfea5d3', daterange('2024-08-01', NULL, '[]'), 94900, 0.60, 56940, 151900, 0.60, 91140),
    ('69125b0e-093f-11ef-bcd2-939e49610ba2', '5035938e-b961-11eb-b526-6b30323c87a8', daterange('2024-08-01', NULL, '[]'), 94900, 1.00, 94900, 151900, 1.00, 151900),
    ('69125b22-093f-11ef-bcd2-7bba0d97728f', 'cfeae50c-20c3-45ff-be28-e78f71b8bed1', daterange('2024-08-01', NULL, '[]'), 94900, 1.00, 94900, 151900, 1.00, 151900),
    ('69125b36-093f-11ef-bcd2-832d0e5e7d38', '0bfc6c92-ff2a-11eb-a785-2724e8e5e7ee', daterange('2024-08-01', NULL, '[]'), 94900, 0.50, 47450, 151900, 0.50, 75950),
    ('69125b4a-093f-11ef-bcd2-97585e6d5c20', '503593e8-b961-11eb-b527-a3dcdfb628ec', daterange('2024-08-01', NULL, '[]'), 94900, 0.50, 47450, 151900, 0.50, 75950),
    ('69125b5e-093f-11ef-bcd2-ef3be3c4e499', '50359442-b961-11eb-b528-df3290c0d63e', daterange('2024-08-01', NULL, '[]'), 94900, 0.50, 47450, 151900, 0.50, 75950),
    ('69125b72-093f-11ef-bcd2-0f98b562a33d', 'bc6a42d0-fa74-11eb-9a2b-d315a7916074', daterange('2024-08-01', NULL, '[]'), 47450, 0.50, 23725, 75950, 0.50, 37975),
    ('69125b86-093f-11ef-bcd2-f780cef947b6', 'bc6a44ec-fa74-11eb-9a2c-73b53c2af869', daterange('2024-08-01', NULL, '[]'), 47450, 0.75, 35588, 75950, 0.75, 56963),
    ('69125b9a-093f-11ef-bcd2-13d086f1ee32', 'bc6a4550-fa74-11eb-9a2d-035acd5db9aa', daterange('2024-08-01', NULL, '[]'), 47450, 0.50, 23725, 75950, 0.50, 37975),
    ('69125bae-093f-11ef-bcd2-5f92c3fa7b7f', 'bc6a45a0-fa74-11eb-9a2e-fb411a8588da', daterange('2024-08-01', NULL, '[]'), 47450, 0.75, 35588, 75950, 0.75, 56963),
    ('69125bc2-093f-11ef-bcd2-577343f88e7c', '0a58d934-6fd1-11ed-a75e-c353faef5857', daterange('2024-08-01', NULL, '[]'), 94900, 0.50, 47450, 151900, 0.50, 75950),
    ('69125bcc-093f-11ef-bcd2-4bd2aa76e6f6', '0a58db0a-6fd1-11ed-a75e-bbde95c1adef', daterange('2024-08-01', NULL, '[]'), 47450, 0.50, 23725, 75950, 0.50, 37975),
    ('69125be0-093f-11ef-bcd2-3fab7d14ef45', '0a58dbe6-6fd1-11ed-a75e-5335f2b9a91b', daterange('2024-08-01', NULL, '[]'), 47450, 0.75, 35588, 75950, 0.75, 56963),
    ('69125bf4-093f-11ef-bcd2-cf9b534d5635', '0a58da38-6fd1-11ed-a75e-9b2790b0b4f5', daterange('2024-08-01', NULL, '[]'), 94900, 0.50, 47450, 151900, 0.50, 75950),
    ('69125c08-093f-11ef-bcd2-0bd78822d2c9', '0a58dcae-6fd1-11ed-a75e-b3e10433b948', daterange('2024-08-01', NULL, '[]'), 47450, 0.50, 23725, 75950, 0.50, 37975),
    ('69125c1c-093f-11ef-bcd2-4ffe9763ee7e', '0a58dd94-6fd1-11ed-a75e-8390cdc6af62', daterange('2024-08-01', NULL, '[]'), 47450, 0.75, 35588, 75950, 0.75, 56963),
    ('69125c30-093f-11ef-bcd2-8b2b113b2a9d', '9cc7713c-f153-489a-a7c2-e001640f5c29', daterange('2024-08-01', NULL, '[]'), 94900, 0.50, 47450, 151900, 0.50, 75950),
    ('69125c44-093f-11ef-bcd2-db0e9f5fece5', '816d36b5-29b3-494b-8ac4-b2298764c803', daterange('2024-08-01', NULL, '[]'), 94900, 0.50, 47450, 151900, 0.50, 75950),
    ('69125c58-093f-11ef-bcd2-078dd2603f47', '9a2b393b-ebb6-49d9-aa1e-b86d80ba9eb5', daterange('2024-08-01', NULL, '[]'), 47450, 0.50, 23725, 75950, 0.50, 37975),
    ('69125c6c-093f-11ef-bcd2-df7cbdee8c66', 'efcfd8f4-92d9-43a4-91e0-a7e2fde9c4e5', daterange('2024-08-01', NULL, '[]'), 47450, 0.75, 35588, 75950, 0.75, 56963),
    ('69125c80-093f-11ef-bcd2-7f1d1e69c2ca', '313744dc-ac9e-46b1-8004-940d429664e3', daterange('2024-08-01', NULL, '[]'), 94900, 0.50, 47450, 151900, 0.50, 75950),
    ('69125c94-093f-11ef-bcd2-5710e580d8de', '9e1e61da-3362-4f8f-bbf9-ea1eb0d0206c', daterange('2024-08-01', NULL, '[]'), 47450, 0.50, 23725, 75950, 0.50, 37975),
    ('69125ca8-093f-11ef-bcd2-ffdb09a4c015', 'b5269ff0-a047-496f-9bda-6286520ef1a5', daterange('2024-08-01', NULL, '[]'), 47450, 0.75, 35588, 75950, 0.75, 56963),
    ('69125d48-093f-11ef-bcd2-bf02fecec3ee', 'ff6ddcd4-fa8a-11eb-8592-2f2b4e398fcb', daterange('2024-08-01', NULL, '[]'), 94900, 0.00, 0, 151900, 0.00, 0)
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
