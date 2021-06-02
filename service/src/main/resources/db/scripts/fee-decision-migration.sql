-- SPDX-FileCopyrightText: 2017-2021 City of Espoo
--
-- SPDX-License-Identifier: LGPL-2.1-or-later

BEGIN;

DELETE FROM new_fee_decision_child;
DELETE FROM new_fee_decision;

INSERT INTO new_fee_decision (id, created, status, valid_during, decision_type, head_of_family_id, head_of_family_income, partner_id, partner_income, family_size, pricing, decision_number, document_key, approved_at, approved_by_id, decision_handler_id, sent_at, cancelled_at)
SELECT id, created_at, status, daterange(valid_from, valid_to, '[]'), decision_type, head_of_family, head_of_family_income, partner, partner_income, family_size, pricing, decision_number, document_key, approved_at, approved_by, decision_handler, sent_at, cancelled_at
FROM fee_decision;

INSERT INTO new_fee_decision_child (id, fee_decision_id, child_id, child_date_of_birth, sibling_discount, placement_unit_id, placement_type, service_need_fee_coefficient, service_need_description_fi, service_need_description_sv, service_need_missing, base_fee, fee, fee_alterations, final_fee)
SELECT
    fee_decision_part.id,
    fee_decision_id,
    child,
    date_of_birth,
    sibling_discount,
    placement_unit,
    (CASE
        WHEN placement_type = 'FIVE_YEARS_OLD_DAYCARE' THEN 'DAYCARE_FIVE_YEAR_OLDS'::placement_type
        WHEN placement_type = 'PRESCHOOL_WITH_DAYCARE' THEN 'PRESCHOOL_DAYCARE'::placement_type
        WHEN placement_type = 'PREPARATORY_WITH_DAYCARE' THEN 'PREPARATORY_DAYCARE'::placement_type
        ELSE placement_type::placement_type
    END),
    (CASE
        WHEN placement_type = 'DAYCARE' AND service_need = 'MISSING' THEN 1.00
        WHEN service_need = 'GTE_35' THEN 1.00
        WHEN service_need = 'GT_25_LT_35' THEN 0.80
        WHEN service_need = 'LTE_25' THEN 0.60
        WHEN service_need = 'MISSING' THEN 0.80
        WHEN service_need = 'GTE_25' THEN 0.80
        WHEN service_need = 'GT_15_LT_25' THEN 0.60
        WHEN service_need = 'LTE_15' THEN 0.35
        WHEN service_need = 'LTE_0' THEN 0.00
    END),
    (CASE
        WHEN service_need = 'MISSING' THEN 'palveluntarve puuttuu, korkein maksu'
        WHEN service_need = 'GTE_35' THEN 'vähintään 35 h/vko'
        WHEN service_need = 'GT_25_LT_35' THEN '25-35 h/vko'
        WHEN service_need = 'LTE_25' THEN 'korkeintaan 25 h/vko'
        WHEN placement_type = 'FIVE_YEARS_OLD_DAYCARE' AND service_need = 'GTE_25' THEN 'maksullista varhaiskasvatusta vähintään 25 h/vko'
        WHEN placement_type = 'FIVE_YEARS_OLD_DAYCARE' AND service_need = 'GT_15_LT_25' THEN 'maksullista varhaiskasvatusta 15-25 h/vko'
        WHEN placement_type = 'FIVE_YEARS_OLD_DAYCARE' AND service_need = 'LTE_15' THEN 'maksullista varhaiskasvatusta korkeintaan 15 h/vko'
        WHEN service_need = 'GTE_25' THEN 'varhaiskasvatusta vähintään 25 h/vko'
        WHEN service_need = 'GT_15_LT_25' THEN 'varhaiskasvatusta 15-25 h/vko'
        WHEN service_need = 'LTE_15' THEN 'varhaiskasvatusta korkeintaan 15 h/vko'
        WHEN service_need = 'LTE_0' THEN 'ei maksullista varhaiskasvatusta'
    END),
    (CASE
        WHEN service_need = 'MISSING' THEN 'vårdbehovet saknas, högsta avgift'
        WHEN service_need = 'GTE_35' THEN 'minst 35 h/vecka'
        WHEN service_need = 'GT_25_LT_35' THEN '25-35 h/vecka'
        WHEN service_need = 'LTE_25' THEN 'högst 25 h/vecka'
        WHEN placement_type = 'FIVE_YEARS_OLD_DAYCARE' AND service_need = 'GTE_25' THEN 'avgiftsbelagd småbarnspedagogik minst 25 h/vecka'
        WHEN placement_type = 'FIVE_YEARS_OLD_DAYCARE' AND service_need = 'GT_15_LT_25' THEN 'avgiftsbelagd småbarnspedagogik 15-25 h/vecka'
        WHEN placement_type = 'FIVE_YEARS_OLD_DAYCARE' AND service_need = 'LTE_15' THEN 'avgiftsbelagd småbarnspedagogik högst 15 h/vecka'
        WHEN service_need = 'GTE_25' THEN 'småbarnspedagogik minst 25 h/vecka'
        WHEN service_need = 'GT_15_LT_25' THEN 'småbarnspedagogik 15-25 h/vecka'
        WHEN service_need = 'LTE_15' THEN 'småbarnspedagogik högst 15 h/vecka'
        WHEN service_need = 'LTE_0' THEN 'ingen avgiftsbelagd småbarnspedagogik'
    END),
    service_need = 'MISSING',
    base_fee,
    fee,
    fee_alterations,
    fee + coalesce(fee_alteration_effects, 0)
FROM fee_decision_part
LEFT JOIN (
    SELECT p.id, sum(effect) fee_alteration_effects
    FROM fee_decision_part p
    JOIN LATERAL (
        SELECT id, (jsonb_array_elements(fee_alterations)->>'effect')::integer effect
        FROM fee_decision_part WHERE id = p.id
    ) effects ON true
    GROUP BY p.id
) fee_alteration_effects ON fee_alteration_effects.id = fee_decision_part.id;
