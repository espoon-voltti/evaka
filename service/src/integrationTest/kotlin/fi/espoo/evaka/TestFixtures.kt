// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka

import fi.espoo.evaka.daycare.ClubTerm
import fi.espoo.evaka.invoicing.domain.*
import fi.espoo.evaka.shared.ClubTermId
import fi.espoo.evaka.shared.PreschoolTermId
import fi.espoo.evaka.shared.data.DateSet
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.dev.DevPreschoolTerm
import fi.espoo.evaka.shared.domain.DateRange
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.shared.domain.TimeRange
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID

const val defaultMunicipalOrganizerOid = "1.2.246.562.10.888888888888"
const val defaultPurchasedOrganizerOid = "1.2.246.562.10.66666666666"

val allDayTimeRange = TimeRange(LocalTime.parse("00:00"), LocalTime.parse("23:59"))
val allWeekOpTimes =
    listOf(
        allDayTimeRange,
        allDayTimeRange,
        allDayTimeRange,
        allDayTimeRange,
        allDayTimeRange,
        allDayTimeRange,
        allDayTimeRange,
    )

val feeThresholds =
    FeeThresholds(
        validDuring = DateRange(LocalDate.of(2000, 1, 1), null),
        minIncomeThreshold2 = 210200,
        minIncomeThreshold3 = 271300,
        minIncomeThreshold4 = 308000,
        minIncomeThreshold5 = 344700,
        minIncomeThreshold6 = 381300,
        maxIncomeThreshold2 = 479900,
        maxIncomeThreshold3 = 541000,
        maxIncomeThreshold4 = 577700,
        maxIncomeThreshold5 = 614400,
        maxIncomeThreshold6 = 651000,
        incomeMultiplier2 = BigDecimal("0.1070"),
        incomeMultiplier3 = BigDecimal("0.1070"),
        incomeMultiplier4 = BigDecimal("0.1070"),
        incomeMultiplier5 = BigDecimal("0.1070"),
        incomeMultiplier6 = BigDecimal("0.1070"),
        incomeThresholdIncrease6Plus = 14200,
        siblingDiscount2 = BigDecimal("0.5"),
        siblingDiscount2Plus = BigDecimal("0.8"),
        maxFee = 28900,
        minFee = 2700,
        temporaryFee = 2900,
        temporaryFeePartDay = 1500,
        temporaryFeeSibling = 1500,
        temporaryFeeSiblingPartDay = 800,
    )

val preschoolTerm2020 =
    DevPreschoolTerm(
        PreschoolTermId(UUID.randomUUID()),
        FiniteDateRange(LocalDate.of(2020, 8, 13), LocalDate.of(2021, 6, 4)),
        FiniteDateRange(LocalDate.of(2020, 8, 18), LocalDate.of(2021, 6, 4)),
        FiniteDateRange(LocalDate.of(2020, 8, 1), LocalDate.of(2021, 6, 4)),
        FiniteDateRange(LocalDate.of(2020, 1, 8), LocalDate.of(2020, 1, 20)),
        DateSet.empty(),
    )
val preschoolTerm2021 =
    DevPreschoolTerm(
        PreschoolTermId(UUID.randomUUID()),
        FiniteDateRange(LocalDate.of(2021, 8, 11), LocalDate.of(2022, 6, 3)),
        FiniteDateRange(LocalDate.of(2021, 8, 13), LocalDate.of(2022, 6, 3)),
        FiniteDateRange(LocalDate.of(2021, 8, 1), LocalDate.of(2022, 6, 3)),
        FiniteDateRange(LocalDate.of(2021, 1, 8), LocalDate.of(2021, 1, 20)),
        DateSet.empty(),
    )
val preschoolTerm2022 =
    DevPreschoolTerm(
        PreschoolTermId(UUID.randomUUID()),
        FiniteDateRange(LocalDate.of(2022, 8, 11), LocalDate.of(2023, 6, 2)),
        FiniteDateRange(LocalDate.of(2022, 8, 11), LocalDate.of(2023, 6, 2)),
        FiniteDateRange(LocalDate.of(2022, 8, 1), LocalDate.of(2023, 6, 2)),
        FiniteDateRange(LocalDate.of(2022, 1, 10), LocalDate.of(2022, 1, 21)),
        DateSet.empty(),
    )
val preschoolTerm2023 =
    DevPreschoolTerm(
        PreschoolTermId(UUID.randomUUID()),
        FiniteDateRange(LocalDate.of(2023, 8, 11), LocalDate.of(2024, 6, 3)),
        FiniteDateRange(LocalDate.of(2023, 8, 13), LocalDate.of(2024, 6, 6)),
        FiniteDateRange(LocalDate.of(2023, 8, 1), LocalDate.of(2024, 6, 6)),
        FiniteDateRange(LocalDate.of(2023, 1, 8), LocalDate.of(2023, 1, 20)),
        DateSet.of(
            FiniteDateRange(LocalDate.of(2023, 10, 16), LocalDate.of(2023, 10, 20)),
            FiniteDateRange(LocalDate.of(2023, 12, 23), LocalDate.of(2024, 1, 7)),
            FiniteDateRange(LocalDate.of(2024, 2, 19), LocalDate.of(2024, 2, 23)),
        ),
    )
val preschoolTerm2024 =
    DevPreschoolTerm(
        PreschoolTermId(UUID.randomUUID()),
        FiniteDateRange(LocalDate.of(2024, 8, 8), LocalDate.of(2025, 5, 30)),
        FiniteDateRange(LocalDate.of(2024, 8, 8), LocalDate.of(2025, 5, 30)),
        FiniteDateRange(LocalDate.of(2024, 8, 1), LocalDate.of(2025, 5, 30)),
        FiniteDateRange(LocalDate.of(2024, 1, 9), LocalDate.of(2024, 1, 19)),
        DateSet.of(
            FiniteDateRange(LocalDate.of(2024, 10, 14), LocalDate.of(2024, 10, 18)),
            FiniteDateRange(LocalDate.of(2024, 12, 21), LocalDate.of(2025, 1, 6)),
            FiniteDateRange(LocalDate.of(2025, 2, 17), LocalDate.of(2025, 2, 21)),
        ),
    )

val preschoolTerm2025 =
    DevPreschoolTerm(
        PreschoolTermId(UUID.randomUUID()),
        FiniteDateRange(LocalDate.of(2025, 8, 7), LocalDate.of(2026, 5, 29)),
        FiniteDateRange(LocalDate.of(2025, 8, 7), LocalDate.of(2026, 5, 29)),
        FiniteDateRange(LocalDate.of(2025, 8, 1), LocalDate.of(2026, 5, 29)),
        FiniteDateRange(LocalDate.of(2025, 1, 8), LocalDate.of(2026, 5, 29)),
        DateSet.of(
            FiniteDateRange(LocalDate.of(2025, 10, 13), LocalDate.of(2025, 10, 17)),
            FiniteDateRange(LocalDate.of(2025, 12, 22), LocalDate.of(2026, 1, 6)),
            FiniteDateRange(LocalDate.of(2026, 2, 16), LocalDate.of(2026, 2, 20)),
        ),
    )

val preschoolTerms =
    listOf(
        preschoolTerm2020,
        preschoolTerm2021,
        preschoolTerm2022,
        preschoolTerm2023,
        preschoolTerm2024,
        preschoolTerm2025,
    )

val clubTerm2020 =
    ClubTerm(
        ClubTermId(UUID.randomUUID()),
        FiniteDateRange(LocalDate.of(2020, 8, 13), LocalDate.of(2021, 6, 4)),
        FiniteDateRange(LocalDate.of(2020, 1, 8), LocalDate.of(2020, 1, 20)),
        DateSet.empty(),
    )

val clubTerm2021 =
    ClubTerm(
        ClubTermId(UUID.randomUUID()),
        FiniteDateRange(LocalDate.of(2021, 8, 11), LocalDate.of(2022, 6, 3)),
        FiniteDateRange(LocalDate.of(2021, 1, 8), LocalDate.of(2021, 1, 20)),
        DateSet.empty(),
    )

val clubTerm2022 =
    ClubTerm(
        ClubTermId(UUID.randomUUID()),
        FiniteDateRange(LocalDate.of(2022, 8, 11), LocalDate.of(2023, 6, 2)),
        FiniteDateRange(LocalDate.of(2022, 1, 8), LocalDate.of(2022, 1, 20)),
        DateSet.empty(),
    )

val clubTerm2023 =
    ClubTerm(
        ClubTermId(UUID.randomUUID()),
        FiniteDateRange(LocalDate.of(2023, 8, 10), LocalDate.of(2024, 5, 31)),
        FiniteDateRange(LocalDate.of(2023, 3, 1), LocalDate.of(2023, 3, 1)),
        DateSet.of(
            FiniteDateRange(LocalDate.of(2023, 10, 16), LocalDate.of(2023, 10, 20)),
            FiniteDateRange(LocalDate.of(2023, 12, 23), LocalDate.of(2024, 1, 7)),
            FiniteDateRange(LocalDate.of(2024, 2, 19), LocalDate.of(2024, 2, 23)),
        ),
    )

val clubTerm2024 =
    ClubTerm(
        ClubTermId(UUID.randomUUID()),
        FiniteDateRange(LocalDate.of(2024, 8, 8), LocalDate.of(2025, 5, 30)),
        FiniteDateRange(LocalDate.of(2024, 3, 1), LocalDate.of(2024, 3, 1)),
        DateSet.of(
            FiniteDateRange(LocalDate.of(2024, 10, 14), LocalDate.of(2024, 10, 18)),
            FiniteDateRange(LocalDate.of(2024, 12, 21), LocalDate.of(2025, 1, 6)),
            FiniteDateRange(LocalDate.of(2025, 2, 17), LocalDate.of(2025, 2, 21)),
        ),
    )

val clubTerm2025 =
    ClubTerm(
        ClubTermId(UUID.randomUUID()),
        FiniteDateRange(LocalDate.of(2025, 8, 7), LocalDate.of(2026, 5, 29)),
        FiniteDateRange(LocalDate.of(2025, 3, 1), LocalDate.of(2026, 5, 29)),
        DateSet.of(
            FiniteDateRange(LocalDate.of(2025, 10, 13), LocalDate.of(2025, 10, 17)),
            FiniteDateRange(LocalDate.of(2025, 12, 22), LocalDate.of(2026, 1, 6)),
            FiniteDateRange(LocalDate.of(2026, 2, 16), LocalDate.of(2026, 2, 20)),
        ),
    )

val clubTerms =
    listOf(clubTerm2020, clubTerm2021, clubTerm2022, clubTerm2023, clubTerm2024, clubTerm2025)

fun Database.Transaction.insertServiceNeedOptions() {
    executeBatch(serviceNeedTestFixtures) {
        sql(
            """
INSERT INTO service_need_option (id, name_fi, name_sv, name_en, valid_placement_type, default_option, fee_coefficient, occupancy_coefficient, occupancy_coefficient_under_3y, realized_occupancy_coefficient, realized_occupancy_coefficient_under_3y, daycare_hours_per_week, contract_days_per_month, daycare_hours_per_month, part_day, part_week, fee_description_fi, fee_description_sv, voucher_value_description_fi, voucher_value_description_sv, valid_from, valid_to)
VALUES (${bind { it.id }}, ${bind { it.nameFi }}, ${bind { it.nameSv }}, ${bind { it.nameEn }}, ${bind { it.validPlacementType }}, ${bind { it.defaultOption }}, ${bind { it.feeCoefficient }}, ${bind { it.occupancyCoefficient }}, ${bind { it.occupancyCoefficientUnder3y }}, ${bind { it.realizedOccupancyCoefficient }}, ${bind { it.realizedOccupancyCoefficientUnder3y }}, ${bind { it.daycareHoursPerWeek }}, ${bind { it.contractDaysPerMonth }}, ${bind { it.daycareHoursPerMonth }}, ${bind { it.partDay }}, ${bind { it.partWeek }}, ${bind { it.feeDescriptionFi }}, ${bind { it.feeDescriptionSv }}, ${bind { it.voucherValueDescriptionFi }}, ${bind { it.voucherValueDescriptionSv }}, ${bind { it.validFrom }}, ${bind { it.validTo }})
"""
        )
    }
}

fun Database.Transaction.insertServiceNeedOptionVoucherValues() {
    executeBatch(serviceNeedOptionVoucherValueTestFixtures) {
        sql(
            """
INSERT INTO service_need_option_voucher_value (service_need_option_id, validity, base_value, coefficient, value, base_value_under_3y, coefficient_under_3y, value_under_3y)
VALUES (${bind { it.serviceNeedOptionId }}, ${bind { it.validity }}, ${bind { it.baseValue }}, ${bind { it.coefficient }}, ${bind { it.value }}, ${bind { it.baseValueUnder3y }}, ${bind { it.coefficientUnder3y }}, ${bind { it.valueUnder3y }})
"""
        )
    }
}

fun Database.Transaction.insertAssistanceActionOptions() {
    execute {
        sql(
            """
INSERT INTO assistance_action_option (value, name_fi, display_order, category, valid_from, valid_to) VALUES
    ('ASSISTANCE_SERVICE_CHILD', 'Avustamispalvelut yhdelle lapselle', 10, 'DAYCARE', NULL, NULL),
    ('ASSISTANCE_SERVICE_UNIT', 'Avustamispalvelut yksikköön', 20, 'DAYCARE', NULL, NULL),
    ('SMALLER_GROUP', 'Pedagogisesti vahvistettu ryhmä', 30, 'DAYCARE', NULL, NULL),
    ('SPECIAL_GROUP', 'Erityisryhmä', 40, 'DAYCARE', NULL, NULL),
    ('PERVASIVE_VEO_SUPPORT', 'Laaja-alaisen veon tuki', 50, 'DAYCARE', NULL, NULL),
    ('RESOURCE_PERSON', 'Resurssihenkilö', 60, 'DAYCARE', NULL, NULL),
    ('RATIO_DECREASE', 'Suhdeluvun väljennys', 70, 'DAYCARE', NULL, NULL),
    ('PERIODICAL_VEO_SUPPORT', 'Lisäresurssi hankerahoituksella', 80, 'DAYCARE', NULL, NULL),

    ('PART_TIME_SPECIAL_EDUCATION', 'Osa-aikainen erityisopetus esiopetuksessa', 0, 'PRESCHOOL', NULL, '2025-07-31'::date),
    ('FULL_VEO_SUPPORT_IN_SMALLER_GROUP', 'Kokoaikainen erityisopettajan antama opetus pienryhmässä', 10, 'PRESCHOOL', '2025-08-01'::date, NULL),
    ('REGULAR_VEO_SUPPORT_PARTIALLY_IN_SMALLER_GROUP', 'Säännöllinen erityisopettajan antama opetus osittain pienryhmässä ja muun opetuksen yhteydessä', 20, 'PRESCHOOL', '2025-08-01'::date, NULL),
    ('PERSONAL_ASSISTANT', 'Lapsikohtainen avustaja', 30, 'PRESCHOOL', '2025-08-01'::date, NULL),
    ('ASSISTIVE_DEVICES', 'Apuvälineet', 40, 'PRESCHOOL', '2025-08-01'::date, NULL),
    ('INTERPRETATION_SERVICES', 'Tulkitsemispalvelut', 50, 'PRESCHOOL', '2025-08-01'::date, NULL);
"""
        )
    }
}
