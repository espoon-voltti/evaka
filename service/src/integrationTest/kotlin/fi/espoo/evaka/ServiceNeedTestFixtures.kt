// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka

import fi.espoo.evaka.invoicing.domain.FeeDecisionServiceNeed
import fi.espoo.evaka.invoicing.domain.VoucherValueDecisionServiceNeed
import fi.espoo.evaka.placement.PlacementType
import fi.espoo.evaka.serviceneed.ServiceNeedOption
import fi.espoo.evaka.shared.ServiceNeedOptionId
import java.math.BigDecimal
import java.util.UUID

val snDefaultDaycare = ServiceNeedOption(
    id = ServiceNeedOptionId(UUID.randomUUID()),
    name = "Kokopäiväinen",
    validPlacementType = PlacementType.DAYCARE,
    defaultOption = true,
    feeCoefficient = BigDecimal("1.00"),
    voucherValueCoefficient = BigDecimal("1.00"),
    occupancyCoefficient = BigDecimal("1.00"),
    daycareHoursPerWeek = 35,
    partDay = false,
    partWeek = false,
    feeDescriptionFi = "",
    feeDescriptionSv = "",
    voucherValueDescriptionFi = "",
    voucherValueDescriptionSv = ""
)

val snDefaultPartDayDaycare = ServiceNeedOption(
    id = ServiceNeedOptionId(UUID.randomUUID()),
    name = "Osapäiväinen",
    validPlacementType = PlacementType.DAYCARE_PART_TIME,
    defaultOption = true,
    feeCoefficient = BigDecimal("1.00"),
    voucherValueCoefficient = BigDecimal("0.60"),
    occupancyCoefficient = BigDecimal("0.54"),
    daycareHoursPerWeek = 25,
    partDay = true,
    partWeek = false,
    feeDescriptionFi = "",
    feeDescriptionSv = "",
    voucherValueDescriptionFi = "",
    voucherValueDescriptionSv = ""
)

val snDefaultFiveYearOldsDaycare = ServiceNeedOption(
    id = ServiceNeedOptionId(UUID.randomUUID()),
    name = "Viisivuotiaiden kokopäiväinen",
    validPlacementType = PlacementType.DAYCARE_FIVE_YEAR_OLDS,
    defaultOption = true,
    feeCoefficient = BigDecimal("0.80"),
    voucherValueCoefficient = BigDecimal("1.00"),
    occupancyCoefficient = BigDecimal("1.00"),
    daycareHoursPerWeek = 45,
    partDay = false,
    partWeek = false,
    feeDescriptionFi = "",
    feeDescriptionSv = "",
    voucherValueDescriptionFi = "",
    voucherValueDescriptionSv = ""
)

val snDefaultFiveYearOldsPartDayDaycare = ServiceNeedOption(
    id = ServiceNeedOptionId(UUID.randomUUID()),
    name = "Viisivuotiaiden osapäiväinen",
    validPlacementType = PlacementType.DAYCARE_PART_TIME_FIVE_YEAR_OLDS,
    defaultOption = true,
    feeCoefficient = BigDecimal("0.80"),
    voucherValueCoefficient = BigDecimal("0.60"),
    occupancyCoefficient = BigDecimal("0.54"),
    daycareHoursPerWeek = 25,
    partDay = true,
    partWeek = false,
    feeDescriptionFi = "",
    feeDescriptionSv = "",
    voucherValueDescriptionFi = "",
    voucherValueDescriptionSv = ""
)

val snDefaultPreschool = ServiceNeedOption(
    id = ServiceNeedOptionId(UUID.randomUUID()),
    name = "Esiopetus",
    validPlacementType = PlacementType.PRESCHOOL,
    defaultOption = true,
    feeCoefficient = BigDecimal("0.00"),
    voucherValueCoefficient = BigDecimal("0.50"),
    occupancyCoefficient = BigDecimal("0.50"),
    daycareHoursPerWeek = 0,
    partDay = true,
    partWeek = true,
    feeDescriptionFi = "",
    feeDescriptionSv = "",
    voucherValueDescriptionFi = "",
    voucherValueDescriptionSv = ""
)

val snDefaultPreschoolDaycare = ServiceNeedOption(
    id = ServiceNeedOptionId(UUID.randomUUID()),
    name = "Esiopetus ja liittyvä varhaiskasvatus",
    validPlacementType = PlacementType.PRESCHOOL_DAYCARE,
    defaultOption = true,
    feeCoefficient = BigDecimal("0.80"),
    voucherValueCoefficient = BigDecimal("0.50"),
    occupancyCoefficient = BigDecimal("1.00"),
    daycareHoursPerWeek = 25,
    partDay = false,
    partWeek = false,
    feeDescriptionFi = "",
    feeDescriptionSv = "",
    voucherValueDescriptionFi = "",
    voucherValueDescriptionSv = ""
)

val snDefaultPreparatory = ServiceNeedOption(
    id = ServiceNeedOptionId(UUID.randomUUID()),
    name = "Valmistava opetus",
    validPlacementType = PlacementType.PREPARATORY,
    defaultOption = true,
    feeCoefficient = BigDecimal("0.00"),
    voucherValueCoefficient = BigDecimal("0.50"),
    occupancyCoefficient = BigDecimal("0.50"),
    daycareHoursPerWeek = 0,
    partDay = true,
    partWeek = true,
    feeDescriptionFi = "",
    feeDescriptionSv = "",
    voucherValueDescriptionFi = "",
    voucherValueDescriptionSv = ""
)

val snDefaultPreparatoryDaycare = ServiceNeedOption(
    id = ServiceNeedOptionId(UUID.randomUUID()),
    name = "Valmistava opetus ja liittyvä varhaiskasvatus",
    validPlacementType = PlacementType.PREPARATORY_DAYCARE,
    defaultOption = true,
    feeCoefficient = BigDecimal("0.80"),
    voucherValueCoefficient = BigDecimal("0.50"),
    occupancyCoefficient = BigDecimal("1.00"),
    daycareHoursPerWeek = 25,
    partDay = false,
    partWeek = false,
    feeDescriptionFi = "",
    feeDescriptionSv = "",
    voucherValueDescriptionFi = "",
    voucherValueDescriptionSv = ""
)

val snDefaultClub = ServiceNeedOption(
    id = ServiceNeedOptionId(UUID.randomUUID()),
    name = "Kerho",
    validPlacementType = PlacementType.CLUB,
    defaultOption = true,
    feeCoefficient = BigDecimal("0.00"),
    voucherValueCoefficient = BigDecimal("0.00"),
    occupancyCoefficient = BigDecimal("1.00"),
    daycareHoursPerWeek = 0,
    partDay = true,
    partWeek = true,
    feeDescriptionFi = "",
    feeDescriptionSv = "",
    voucherValueDescriptionFi = "",
    voucherValueDescriptionSv = ""
)

val snDefaultTemporaryDaycare = ServiceNeedOption(
    id = ServiceNeedOptionId(UUID.randomUUID()),
    name = "Kokopäiväinen tilapäinen",
    validPlacementType = PlacementType.TEMPORARY_DAYCARE,
    defaultOption = true,
    feeCoefficient = BigDecimal("0.00"),
    voucherValueCoefficient = BigDecimal("0.00"),
    occupancyCoefficient = BigDecimal("1.00"),
    daycareHoursPerWeek = 35,
    partDay = false,
    partWeek = true,
    feeDescriptionFi = "",
    feeDescriptionSv = "",
    voucherValueDescriptionFi = "",
    voucherValueDescriptionSv = ""
)

val snDefaultTemporaryPartDayDaycare = ServiceNeedOption(
    id = ServiceNeedOptionId(UUID.randomUUID()),
    name = "Osapäiväinen tilapäinen",
    validPlacementType = PlacementType.TEMPORARY_DAYCARE_PART_DAY,
    defaultOption = true,
    feeCoefficient = BigDecimal("0.00"),
    voucherValueCoefficient = BigDecimal("0.00"),
    occupancyCoefficient = BigDecimal("0.54"),
    daycareHoursPerWeek = 25,
    partDay = true,
    partWeek = true,
    feeDescriptionFi = "",
    feeDescriptionSv = "",
    voucherValueDescriptionFi = "",
    voucherValueDescriptionSv = ""
)

val snDaycareFullDay35 = ServiceNeedOption(
    id = ServiceNeedOptionId(UUID.randomUUID()),
    name = "Kokopäiväinen, vähintään 35h",
    validPlacementType = PlacementType.DAYCARE,
    defaultOption = false,
    feeCoefficient = BigDecimal("1.00"),
    voucherValueCoefficient = BigDecimal("1.0"),
    occupancyCoefficient = BigDecimal("1.00"),
    daycareHoursPerWeek = 35,
    partDay = false,
    partWeek = false,
    feeDescriptionFi = "",
    feeDescriptionSv = "",
    voucherValueDescriptionFi = "",
    voucherValueDescriptionSv = ""
)

val snDaycareFullDay25to35 = ServiceNeedOption(
    id = ServiceNeedOptionId(UUID.randomUUID()),
    name = "Kokopäiväinen, 25-35h",
    validPlacementType = PlacementType.DAYCARE,
    defaultOption = false,
    feeCoefficient = BigDecimal("0.80"),
    voucherValueCoefficient = BigDecimal("1.00"),
    occupancyCoefficient = BigDecimal("1.00"),
    daycareHoursPerWeek = 30,
    partDay = false,
    partWeek = false,
    feeDescriptionFi = "",
    feeDescriptionSv = "",
    voucherValueDescriptionFi = "",
    voucherValueDescriptionSv = ""
)

val snDaycareFullDayPartWeek25 = ServiceNeedOption(
    id = ServiceNeedOptionId(UUID.randomUUID()),
    name = "Osaviikkoinen, enintään 25h",
    validPlacementType = PlacementType.DAYCARE,
    defaultOption = false,
    feeCoefficient = BigDecimal("0.60"),
    voucherValueCoefficient = BigDecimal("0.60"),
    occupancyCoefficient = BigDecimal("1.00"),
    daycareHoursPerWeek = 25,
    partDay = false,
    partWeek = true,
    feeDescriptionFi = "",
    feeDescriptionSv = "",
    voucherValueDescriptionFi = "",
    voucherValueDescriptionSv = ""
)

val snDaycarePartDay25 = ServiceNeedOption(
    id = ServiceNeedOptionId(UUID.randomUUID()),
    name = "Osapäiväinen",
    validPlacementType = PlacementType.DAYCARE_PART_TIME,
    defaultOption = false,
    feeCoefficient = BigDecimal("0.60"),
    voucherValueCoefficient = BigDecimal("0.60"),
    occupancyCoefficient = BigDecimal("0.54"),
    daycareHoursPerWeek = 25,
    partDay = true,
    partWeek = false,
    feeDescriptionFi = "",
    feeDescriptionSv = "",
    voucherValueDescriptionFi = "",
    voucherValueDescriptionSv = ""
)

val snPreschoolDaycare45 = ServiceNeedOption(
    id = ServiceNeedOptionId(UUID.randomUUID()),
    name = "Kokopäiväinen liittyvä, yhteensä vähintään 45h",
    validPlacementType = PlacementType.PRESCHOOL_DAYCARE,
    defaultOption = false,
    feeCoefficient = BigDecimal("0.80"),
    voucherValueCoefficient = BigDecimal("0.50"),
    occupancyCoefficient = BigDecimal("1.00"),
    daycareHoursPerWeek = 25,
    partDay = false,
    partWeek = false,
    feeDescriptionFi = "",
    feeDescriptionSv = "",
    voucherValueDescriptionFi = "",
    voucherValueDescriptionSv = ""
)

val snPreschoolDaycarePartDay35to45 = ServiceNeedOption(
    id = ServiceNeedOptionId(UUID.randomUUID()),
    name = "Osapäiväinen liittyvä, yhteensä 35-45h",
    validPlacementType = PlacementType.PRESCHOOL_DAYCARE,
    defaultOption = false,
    feeCoefficient = BigDecimal("0.60"),
    voucherValueCoefficient = BigDecimal("0.50"),
    occupancyCoefficient = BigDecimal("1.00"),
    daycareHoursPerWeek = 20,
    partDay = true,
    partWeek = false,
    feeDescriptionFi = "",
    feeDescriptionSv = "",
    voucherValueDescriptionFi = "",
    voucherValueDescriptionSv = ""
)

val snPreschoolDaycarePartDay35 = ServiceNeedOption(
    id = ServiceNeedOptionId(UUID.randomUUID()),
    name = "Osapäiväinen liittyvä, yhteensä enintään 35h",
    validPlacementType = PlacementType.PRESCHOOL_DAYCARE,
    defaultOption = false,
    feeCoefficient = BigDecimal("0.35"),
    voucherValueCoefficient = BigDecimal("0.50"),
    occupancyCoefficient = BigDecimal("1.00"),
    daycareHoursPerWeek = 15,
    partDay = true,
    partWeek = false,
    feeDescriptionFi = "",
    feeDescriptionSv = "",
    voucherValueDescriptionFi = "",
    voucherValueDescriptionSv = ""
)

val snPreparatoryDaycare50 = ServiceNeedOption(
    id = ServiceNeedOptionId(UUID.randomUUID()),
    name = "Kokopäiväinen liittyvä, yhteensä vähintään 50h",
    validPlacementType = PlacementType.PREPARATORY_DAYCARE,
    defaultOption = false,
    feeCoefficient = BigDecimal("0.80"),
    voucherValueCoefficient = BigDecimal("0.50"),
    occupancyCoefficient = BigDecimal("1.00"),
    daycareHoursPerWeek = 25,
    partDay = false,
    partWeek = false,
    feeDescriptionFi = "",
    feeDescriptionSv = "",
    voucherValueDescriptionFi = "",
    voucherValueDescriptionSv = ""
)

val snPreparatoryDaycarePartDay40to50 = ServiceNeedOption(
    id = ServiceNeedOptionId(UUID.randomUUID()),
    name = "Osapäiväinen liittyvä, yhteensä 40-50h",
    validPlacementType = PlacementType.PREPARATORY_DAYCARE,
    defaultOption = false,
    feeCoefficient = BigDecimal("0.60"),
    voucherValueCoefficient = BigDecimal("0.50"),
    occupancyCoefficient = BigDecimal("1.00"),
    daycareHoursPerWeek = 20,
    partDay = true,
    partWeek = false,
    feeDescriptionFi = "",
    feeDescriptionSv = "",
    voucherValueDescriptionFi = "",
    voucherValueDescriptionSv = ""
)

val snPreparatoryDaycarePartDay40 = ServiceNeedOption(
    id = ServiceNeedOptionId(UUID.randomUUID()),
    name = "Osapäiväinen liittyvä, yhteensä enintään 40h",
    validPlacementType = PlacementType.PREPARATORY_DAYCARE,
    defaultOption = false,
    feeCoefficient = BigDecimal("0.35"),
    voucherValueCoefficient = BigDecimal("0.50"),
    occupancyCoefficient = BigDecimal("1.00"),
    daycareHoursPerWeek = 15,
    partDay = true,
    partWeek = false,
    feeDescriptionFi = "",
    feeDescriptionSv = "",
    voucherValueDescriptionFi = "",
    voucherValueDescriptionSv = ""
)

val snDaycareFiveYearOldsFullDayPartWeek25 = ServiceNeedOption(
    id = ServiceNeedOptionId(UUID.randomUUID()),
    name = "5-vuotiaiden osaviikkoinen, yli 20h enintään 25h",
    validPlacementType = PlacementType.DAYCARE_FIVE_YEAR_OLDS,
    defaultOption = false,
    feeCoefficient = BigDecimal("0.60"),
    voucherValueCoefficient = BigDecimal("0.60"),
    occupancyCoefficient = BigDecimal("1.00"),
    daycareHoursPerWeek = 25,
    partDay = false,
    partWeek = true,
    feeDescriptionFi = "",
    feeDescriptionSv = "",
    voucherValueDescriptionFi = "",
    voucherValueDescriptionSv = ""
)

val serviceNeedTestFixtures = listOf(
    snDefaultDaycare,
    snDefaultPartDayDaycare,
    snDefaultFiveYearOldsDaycare,
    snDefaultFiveYearOldsPartDayDaycare,
    snDefaultPreschool,
    snDefaultPreschoolDaycare,
    snDefaultPreparatory,
    snDefaultPreparatoryDaycare,
    snDefaultClub,
    snDefaultTemporaryDaycare,
    snDefaultTemporaryPartDayDaycare,
    snDaycareFullDay35,
    snDaycareFullDay25to35,
    snDaycareFullDayPartWeek25,
    snDaycarePartDay25,
    snPreschoolDaycare45,
    snPreschoolDaycarePartDay35to45,
    snPreschoolDaycarePartDay35,
    snPreparatoryDaycare50,
    snPreparatoryDaycarePartDay40to50,
    snPreparatoryDaycarePartDay40,
    snDaycareFiveYearOldsFullDayPartWeek25
)

fun ServiceNeedOption.toFeeDecisionServiceNeed() = FeeDecisionServiceNeed(
    feeCoefficient = this.feeCoefficient,
    descriptionFi = this.feeDescriptionFi,
    descriptionSv = this.feeDescriptionSv,
    missing = this.defaultOption
)

fun ServiceNeedOption.toValueDecisionServiceNeed() = VoucherValueDecisionServiceNeed(
    feeCoefficient = this.feeCoefficient,
    voucherValueCoefficient = this.voucherValueCoefficient,
    feeDescriptionFi = this.feeDescriptionFi,
    feeDescriptionSv = this.feeDescriptionSv,
    voucherValueDescriptionFi = this.voucherValueDescriptionFi,
    voucherValueDescriptionSv = this.voucherValueDescriptionSv,
)
