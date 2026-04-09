-- SPDX-FileCopyrightText: 2023-2024 Tampere region
--
-- SPDX-License-Identifier: LGPL-2.1-or-later

INSERT INTO service_need_option
    (id, name_fi, name_sv, name_en, valid_placement_type, default_option, fee_coefficient, occupancy_coefficient, occupancy_coefficient_under_3y, realized_occupancy_coefficient, realized_occupancy_coefficient_under_3y, daycare_hours_per_week, contract_days_per_month, daycare_hours_per_month, part_day, part_week, fee_description_fi, fee_description_sv, voucher_value_description_fi, voucher_value_description_sv, valid_from, valid_to, show_for_citizen, display_order)
VALUES
    ('4b07f3ec-503c-4493-bef2-c5be84fc92b3', 'Varhaiskasvatus 85 tuntia / kuukausi', 'Varhaiskasvatus 85 tuntia / kuukausi', 'Daycare - 85 h per month', 'DAYCARE', TRUE, 0.5, 1.0, 1.75, 1.0, 1.75, 20, NULL, 85, FALSE, TRUE, 'Varhaiskasvatus 85 tuntia / kuukausi', 'Varhaiskasvatus 85 tuntia / kuukausi', 'Varhaiskasvatus 85 tuntia / kuukausi', 'Varhaiskasvatus 85 tuntia / kuukausi', '2000-01-01', NULL, FALSE, NULL),
    ('489dcf01-e11a-4ab8-8c36-1e672581eb6d', 'Varhaiskasvatus 85 tuntia / kuukausi', 'Varhaiskasvatus 85 tuntia / kuukausi', 'Daycare - 85 h per month', 'DAYCARE', FALSE, 0.5, 1.0, 1.75, 1.0, 1.75, 20, NULL, 85, FALSE, TRUE, 'Varhaiskasvatus 85 tuntia / kuukausi', 'Varhaiskasvatus 85 tuntia / kuukausi', 'Varhaiskasvatus 85 tuntia / kuukausi', 'Varhaiskasvatus 85 tuntia / kuukausi', '2000-01-01', NULL, TRUE, 0),
    ('f24a1b37-0be9-4004-8a00-eefda8ed925a', 'Varhaiskasvatus 86–120 tuntia / kuukausi', 'Varhaiskasvatus 86–120 tuntia / kuukausi', 'Daycare - 86-120 h per month', 'DAYCARE', FALSE, 0.8, 1.0, 1.75, 1.0, 1.75, 34, NULL, 120, FALSE, TRUE, 'Varhaiskasvatus 86–120 tuntia / kuukausi', 'Varhaiskasvatus 86–120 tuntia / kuukausi', 'Varhaiskasvatus 86–120 tuntia / kuukausi', 'Varhaiskasvatus 86–120 tuntia / kuukausi', '2000-01-01', NULL, TRUE, 1),
    ('a92cf108-0939-45f5-8976-ab31e456b84d', 'Varhaiskasvatus 121-150 tuntia / kuukausi', 'Varhaiskasvatus 121-150 tuntia / kuukausi', 'Daycare - 121-150 h per month', 'DAYCARE', FALSE, 0.9, 1.0, 1.75, 1.0, 1.75, 40, NULL, 150, FALSE, FALSE, 'Varhaiskasvatus 121-150 tuntia / kuukausi', 'Varhaiskasvatus 121-150 tuntia / kuukausi', 'Varhaiskasvatus 121-150 tuntia / kuukausi', 'Varhaiskasvatus 121-150 tuntia / kuukausi', '2000-01-01', NULL, TRUE, 2),
    ('22adbfd9-50d8-4192-9a35-1c5ce872e1a7', 'Varhaiskasvatus yli 151 tuntia / kuukausi', 'Varhaiskasvatus yli 151 tuntia / kuukausi', 'Daycare - over 151 h per month', 'DAYCARE', FALSE, 1.0, 1.0, 1.75, 1.0, 1.75, 40, NULL, 210, FALSE, FALSE, 'Varhaiskasvatus yli 151 tuntia / kuukausi', 'Varhaiskasvatus yli 151 tuntia / kuukausi', 'Varhaiskasvatus yli 151 tuntia / kuukausi', 'Varhaiskasvatus yli 151 tuntia / kuukausi', '2000-01-01', NULL, TRUE, 3),
    ('18a83b84-51e5-49a3-9bc7-52fd7563944a', 'Palveluseteli 85 tuntia / kuukausi', 'Palveluseteli 85 tuntia / kuukausi', 'Voucher value - 85 h per month', 'DAYCARE', FALSE, 0.5, 1.0, 1.75, 1.0, 1.75, 20, NULL, 85, FALSE, TRUE, 'Varhaiskasvatus 85 tuntia / kuukausi', 'Varhaiskasvatus 85 tuntia / kuukausi', 'Varhaiskasvatus 85 tuntia / kuukausi', 'Varhaiskasvatus 85 tuntia / kuukausi', '2000-01-01', '2024-07-31', TRUE, 4),
    ('6a08d6ae-4580-4d70-b303-179be13e77f8', 'Palveluseteli 86–120 tuntia / kuukausi', 'Palveluseteli 86–120 tuntia / kuukausi', 'Voucher value - 86-120 h per month', 'DAYCARE', FALSE, 0.75, 1.0, 1.75, 1.0, 1.75, 34, NULL, 120, FALSE, TRUE, 'Varhaiskasvatus 86–120 tuntia / kuukausi', 'Varhaiskasvatus 86–120 tuntia / kuukausi', 'Varhaiskasvatus 86–120 tuntia / kuukausi', 'Varhaiskasvatus 86–120 tuntia / kuukausi', '2000-01-01', '2024-07-31', TRUE, 5),
    ('2bf66308-3bf3-442b-81c4-cdc41b5a7fcd', 'Palveluseteli 121-150 tuntia / kuukausi', 'Palveluseteli 121-150 tuntia / kuukausi', 'Voucher value - 121-150 h per month', 'DAYCARE', FALSE, 0.85, 1.0, 1.75, 1.0, 1.75, 40, NULL, 150, FALSE, FALSE, 'Varhaiskasvatus 121-150 tuntia / kuukausi', 'Varhaiskasvatus 121-150 tuntia / kuukausi', 'Varhaiskasvatus 121-150 tuntia / kuukausi', 'Varhaiskasvatus 121-150 tuntia / kuukausi', '2000-01-01', '2024-07-31', TRUE, 6),
    ('dad9361a-1400-4cf0-8ade-f8772f2a299d', 'Palveluseteli yli 151 tuntia / kuukausi', 'Palveluseteli yli 151 tuntia / kuukausi', 'Voucher value - over 151 h per month', 'DAYCARE', FALSE, 1.0, 1.0, 1.75, 1.0, 1.75, 40, NULL, 210, FALSE, FALSE, 'Varhaiskasvatus yli 151 tuntia / kuukausi', 'Varhaiskasvatus yli 151 tuntia / kuukausi', 'Varhaiskasvatus yli 151 tuntia / kuukausi', 'Varhaiskasvatus yli 151 tuntia / kuukausi', '2000-01-01', '2024-07-31', TRUE, 7),
    ('e945728f-2671-4158-8941-1d39a3a36dce', 'Tilapäinen varhaiskasvatus', 'Tilapäinen varhaiskasvatus', 'Temporary daycare', 'TEMPORARY_DAYCARE', TRUE, 1.0, 1.0, 1.75, 1.0, 1.75, 40, NULL, NULL, FALSE, FALSE, 'Tilapäinen varhaiskasvatus', 'Tilapäinen varhaiskasvatus', 'Tilapäinen varhaiskasvatus', 'Tilapäinen varhaiskasvatus', '2000-01-01', NULL, FALSE, NULL),
    ('ef535b56-4f03-4113-bf10-a3ffce8c9407', 'Tilapäinen varhaiskasvatus', 'Tilapäinen varhaiskasvatus', 'Temporary daycare', 'TEMPORARY_DAYCARE', FALSE, 1.0, 1.0, 1.75, 1.0, 1.75, 40, NULL, NULL, FALSE, FALSE, 'Tilapäinen varhaiskasvatus', 'Tilapäinen varhaiskasvatus', 'Tilapäinen varhaiskasvatus', 'Tilapäinen varhaiskasvatus', '2000-01-01', NULL, FALSE, NULL),
    ('492da309-843d-4202-b0ac-8cdf722412a4', 'Esiopetus', 'Esiopetus', 'Preschool', 'PRESCHOOL', TRUE, 0.0, 0.5, 0.5, 0.5, 0.5, 0, NULL, NULL, TRUE, TRUE, 'Esiopetus', 'Esiopetus', 'Esiopetus', 'Esiopetus', '2000-01-01', NULL, FALSE, NULL),
    ('4c98dcb7-a5a6-4a17-8a63-6e72870bf476', 'Esiopetusta täydentävä varhaiskasvatus', 'Esiopetusta täydentävä varhaiskasvatus', 'Preschool daycare', 'PRESCHOOL_DAYCARE', TRUE, 0.6, 1.0, 1.0, 1.0, 1.0, 40, NULL, 210, FALSE, FALSE, 'Esiopetusta täydentävä varhaiskasvatus', 'Esiopetusta täydentävä varhaiskasvatus', 'Esiopetusta täydentävä varhaiskasvatus', 'Esiopetusta täydentävä varhaiskasvatus', '2000-01-01', NULL, FALSE, NULL),
    ('db035f8b-e9e7-4d58-a770-be0d426eb59e', 'Esiopetusta täydentävä varhaiskasvatus', 'Esiopetusta täydentävä varhaiskasvatus', 'Preschool daycare', 'PRESCHOOL_DAYCARE', FALSE, 0.6, 1.0, 1.0, 1.0, 1.0, 40, NULL, 210, FALSE, FALSE, 'Esiopetusta täydentävä varhaiskasvatus', 'Esiopetusta täydentävä varhaiskasvatus', 'Esiopetusta täydentävä varhaiskasvatus', 'Esiopetusta täydentävä varhaiskasvatus', '2000-01-01', '2024-07-31', FALSE, 1),
    ('e3d8e93b-f497-403b-a2b8-40172f323be1', 'Esiopetusta täydentävä varhaiskasvatus', 'Esiopetusta täydentävä varhaiskasvatus', 'Preschool daycare', 'PRESCHOOL_DAYCARE', FALSE, 0.5, 1.0, 1.0, 1.0, 1.0, 40, NULL, 210, FALSE, FALSE, 'Esiopetusta täydentävä varhaiskasvatus', 'Esiopetusta täydentävä varhaiskasvatus', 'Esiopetusta täydentävä varhaiskasvatus', 'Esiopetusta täydentävä varhaiskasvatus', '2024-08-01', '2025-07-31', FALSE, 2),
    ('71c087b2-0c27-44b6-8c17-cf0f516415a8', 'Täydentävä varhaiskasvatus 85 tuntia / kuukausi', 'Täydentävä varhaiskasvatus 85 tuntia / kuukausi', 'Preschool daycare 85 h per month', 'PRESCHOOL_DAYCARE', FALSE, 0.5, 1.0, 1.0, 1.0, 1.0, 20, NULL, 85, FALSE, FALSE, 'Täydentävä varhaiskasvatus 85 tuntia / kuukausi', 'Täydentävä varhaiskasvatus 85 tuntia / kuukausi', 'Täydentävä varhaiskasvatus 85 tuntia / kuukausi', 'Täydentävä varhaiskasvatus 85 tuntia / kuukausi', '2025-08-01', NULL, FALSE, 3),
    ('a7bc3415-fcc0-4376-a59d-75f12b5090d1', 'Täydentävä varhaiskasvatus 86-120 tuntia / kuukausi', 'Täydentävä varhaiskasvatus 86-120 tuntia / kuukausi', 'Preschool daycare 86-120 h per month', 'PRESCHOOL_DAYCARE', FALSE, 0.8, 1.0, 1.0, 1.0, 1.0, 34, NULL, 120, FALSE, FALSE, 'Täydentävä varhaiskasvatus 86-120 tuntia / kuukausi', 'Täydentävä varhaiskasvatus 86-120 tuntia / kuukausi', 'Täydentävä varhaiskasvatus 86-120 tuntia / kuukausi', 'Täydentävä varhaiskasvatus 86-120 tuntia / kuukausi', '2025-08-01', NULL, FALSE, 4),
    ('f23997b4-28aa-4903-967c-d049e25d91ed', 'Täydentävä varhaiskasvatus 121-150 tuntia / kuukausi', 'Täydentävä varhaiskasvatus 121-150 tuntia / kuukausi', 'Preschool daycare 121-150 h per month', 'PRESCHOOL_DAYCARE', FALSE, 0.9, 1.0, 1.0, 1.0, 1.0, 40, NULL, 150, FALSE, FALSE, 'Täydentävä varhaiskasvatus 121-150 tuntia / kuukausi', 'Täydentävä varhaiskasvatus 121-150 tuntia / kuukausi', 'Täydentävä varhaiskasvatus 121-150 tuntia / kuukausi', 'Täydentävä varhaiskasvatus 121-150 tuntia / kuukausi', '2025-08-01', NULL, FALSE, 5),
    ('2fe20b1a-8ef6-486c-9568-93f95a043a31', 'Täydentävä varhaiskasvatus yli 151 tuntia / kuukausi', 'Täydentävä varhaiskasvatus yli 151 tuntia / kuukausi', 'Preschool daycare over 151 h per month', 'PRESCHOOL_DAYCARE', FALSE, 1.0, 1.0, 1.0, 1.0, 1.0, 40, NULL, 210, FALSE, FALSE, 'Täydentävä varhaiskasvatus yli 151 tuntia / kuukausi', 'Täydentävä varhaiskasvatus yli 151 tuntia / kuukausi', 'Täydentävä varhaiskasvatus yli 151 tuntia / kuukausi', 'Täydentävä varhaiskasvatus yli 151 tuntia / kuukausi', '2025-08-01', NULL, FALSE, 6),
    ('e067df64-7ff0-42c1-a409-537db7c202dd', 'Esiopetusta täydentävä varhaiskasvatus', 'Esiopetusta täydentävä varhaiskasvatus', 'Preschool daycare', 'PRESCHOOL_DAYCARE_ONLY', TRUE, 0.6, 1.0, 1.0, 1.0, 1.0, 40, NULL, 210, FALSE, FALSE, 'Esiopetusta täydentävä varhaiskasvatus', 'Esiopetusta täydentävä varhaiskasvatus', 'Esiopetusta täydentävä varhaiskasvatus', 'Esiopetusta täydentävä varhaiskasvatus', '2000-01-01', NULL, FALSE, NULL),
    ('2108670b-4401-4881-88f7-4d5635de3c0a', 'Esiopetusta täydentävä varhaiskasvatus', 'Esiopetusta täydentävä varhaiskasvatus', 'Preschool daycare', 'PRESCHOOL_DAYCARE_ONLY', FALSE, 0.6, 1.0, 1.0, 1.0, 1.0, 40, NULL, 210, FALSE, FALSE, 'Esiopetusta täydentävä varhaiskasvatus', 'Esiopetusta täydentävä varhaiskasvatus', 'Esiopetusta täydentävä varhaiskasvatus', 'Esiopetusta täydentävä varhaiskasvatus', '2000-01-01', '2024-07-31', FALSE, 1),
    ('87bf95c2-d168-473e-8c36-8c2c18a49e99', 'Esiopetusta täydentävä varhaiskasvatus', 'Esiopetusta täydentävä varhaiskasvatus', 'Preschool daycare', 'PRESCHOOL_DAYCARE_ONLY', FALSE, 0.5, 1.0, 1.0, 1.0, 1.0, 40, NULL, 210, FALSE, FALSE, 'Esiopetusta täydentävä varhaiskasvatus', 'Esiopetusta täydentävä varhaiskasvatus', 'Esiopetusta täydentävä varhaiskasvatus', 'Esiopetusta täydentävä varhaiskasvatus', '2024-08-01', '2025-07-31', FALSE, 2),
    ('d005c548-df74-44ce-8612-f8cb06485603', 'Täydentävä varhaiskasvatus 85 tuntia / kuukausi', 'Täydentävä varhaiskasvatus 85 tuntia / kuukausi', 'Preschool daycare 85 h per month', 'PRESCHOOL_DAYCARE_ONLY', FALSE, 0.5, 1.0, 1.0, 1.0, 1.0, 20, NULL, 85, FALSE, FALSE, 'Täydentävä varhaiskasvatus 85 tuntia / kuukausi', 'Täydentävä varhaiskasvatus 85 tuntia / kuukausi', 'Täydentävä varhaiskasvatus 85 tuntia / kuukausi', 'Täydentävä varhaiskasvatus 85 tuntia / kuukausi', '2025-08-01', NULL, FALSE, 3),
    ('7516fb16-6dd3-40f7-9dec-7279ffabcf86', 'Täydentävä varhaiskasvatus 86-120 tuntia / kuukausi', 'Täydentävä varhaiskasvatus 86-120 tuntia / kuukausi', 'Preschool daycare 86-120 h per month', 'PRESCHOOL_DAYCARE_ONLY', FALSE, 0.8, 1.0, 1.0, 1.0, 1.0, 34, NULL, 120, FALSE, FALSE, 'Täydentävä varhaiskasvatus 86-120 tuntia / kuukausi', 'Täydentävä varhaiskasvatus 86-120 tuntia / kuukausi', 'Täydentävä varhaiskasvatus 86-120 tuntia / kuukausi', 'Täydentävä varhaiskasvatus 86-120 tuntia / kuukausi', '2025-08-01', NULL, FALSE, 4),
    ('bfa18510-7dcc-4fe4-b266-ed0bfa2c415a', 'Täydentävä varhaiskasvatus 121-150 tuntia / kuukausi', 'Täydentävä varhaiskasvatus 121-150 tuntia / kuukausi', 'Preschool daycare 121-150 h per month', 'PRESCHOOL_DAYCARE_ONLY', FALSE, 0.9, 1.0, 1.0, 1.0, 1.0, 40, NULL, 150, FALSE, FALSE, 'Täydentävä varhaiskasvatus 121-150 tuntia / kuukausi', 'Täydentävä varhaiskasvatus 121-150 tuntia / kuukausi', 'Täydentävä varhaiskasvatus 121-150 tuntia / kuukausi', 'Täydentävä varhaiskasvatus 121-150 tuntia / kuukausi', '2025-08-01', NULL, FALSE, 5),
    ('b947b282-ddff-47f7-95ec-28f9721c4739', 'Täydentävä varhaiskasvatus yli 151 tuntia / kuukausi', 'Täydentävä varhaiskasvatus yli 151 tuntia / kuukausi', 'Preschool daycare over 151 h per month', 'PRESCHOOL_DAYCARE_ONLY', FALSE, 1.0, 1.0, 1.0, 1.0, 1.0, 40, NULL, 210, FALSE, FALSE, 'Täydentävä varhaiskasvatus yli 151 tuntia / kuukausi', 'Täydentävä varhaiskasvatus yli 151 tuntia / kuukausi', 'Täydentävä varhaiskasvatus yli 151 tuntia / kuukausi', 'Täydentävä varhaiskasvatus yli 151 tuntia / kuukausi', '2025-08-01', NULL, FALSE, 6),
    ('ff6ddcd4-fa8a-11eb-8592-2f2b4e398fcb', 'Kerho', 'Kerho', 'Club', 'CLUB', TRUE, 0.0, 1.0, 1.0, 1.0, 1.0, 0, NULL, NULL, TRUE, TRUE, 'Kerho', 'Kerho', 'Kerho', 'Kerho', '2000-01-01', NULL, FALSE, NULL)
ON CONFLICT (id) DO
UPDATE SET
    name_fi = EXCLUDED.name_fi,
    name_sv = EXCLUDED.name_sv,
    name_en = EXCLUDED.name_en,
    valid_placement_type = EXCLUDED.valid_placement_type,
    default_option = EXCLUDED.default_option,
    fee_coefficient = EXCLUDED.fee_coefficient,
    occupancy_coefficient = EXCLUDED.occupancy_coefficient,
    occupancy_coefficient_under_3y = EXCLUDED.occupancy_coefficient_under_3y,
    realized_occupancy_coefficient = EXCLUDED.realized_occupancy_coefficient,
    realized_occupancy_coefficient_under_3y = EXCLUDED.realized_occupancy_coefficient_under_3y,
    daycare_hours_per_week = EXCLUDED.daycare_hours_per_week,
    contract_days_per_month = EXCLUDED.contract_days_per_month,
    daycare_hours_per_month = EXCLUDED.daycare_hours_per_month,
    part_day = EXCLUDED.part_day,
    part_week = EXCLUDED.part_week,
    fee_description_fi = EXCLUDED.fee_description_fi,
    fee_description_sv = EXCLUDED.fee_description_sv,
    voucher_value_description_fi = EXCLUDED.voucher_value_description_fi,
    voucher_value_description_sv = EXCLUDED.voucher_value_description_sv,
    valid_from = EXCLUDED.valid_from,
    valid_to = EXCLUDED.valid_to,
    show_for_citizen = EXCLUDED.show_for_citizen,
    display_order = EXCLUDED.display_order
WHERE
    service_need_option.name_fi <> EXCLUDED.name_fi OR
    service_need_option.name_sv <> EXCLUDED.name_sv OR
    service_need_option.name_en <> EXCLUDED.name_en OR
    service_need_option.valid_placement_type <> EXCLUDED.valid_placement_type OR
    service_need_option.default_option <> EXCLUDED.default_option OR
    service_need_option.fee_coefficient <> EXCLUDED.fee_coefficient OR
    service_need_option.occupancy_coefficient <> EXCLUDED.occupancy_coefficient OR
    service_need_option.occupancy_coefficient_under_3y <> EXCLUDED.occupancy_coefficient_under_3y OR
    service_need_option.realized_occupancy_coefficient <> EXCLUDED.realized_occupancy_coefficient OR
    service_need_option.realized_occupancy_coefficient_under_3y <> EXCLUDED.realized_occupancy_coefficient_under_3y OR
    service_need_option.daycare_hours_per_week <> EXCLUDED.daycare_hours_per_week OR
    service_need_option.contract_days_per_month IS DISTINCT FROM EXCLUDED.contract_days_per_month OR
    service_need_option.daycare_hours_per_month IS DISTINCT FROM EXCLUDED.daycare_hours_per_month OR
    service_need_option.part_day <> EXCLUDED.part_day OR
    service_need_option.part_week <> EXCLUDED.part_week OR
    service_need_option.fee_description_fi <> EXCLUDED.fee_description_fi OR
    service_need_option.fee_description_sv <> EXCLUDED.fee_description_sv OR
    service_need_option.voucher_value_description_fi <> EXCLUDED.voucher_value_description_fi OR
    service_need_option.voucher_value_description_sv <> EXCLUDED.voucher_value_description_sv OR
    service_need_option.valid_from <> EXCLUDED.valid_from OR
    service_need_option.valid_to IS DISTINCT FROM EXCLUDED.valid_to OR
    service_need_option.show_for_citizen <> EXCLUDED.show_for_citizen OR
    service_need_option.display_order <> EXCLUDED.display_order;
