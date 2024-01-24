// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka

import fi.espoo.evaka.invoicing.domain.FeeDecisionServiceNeed
import fi.espoo.evaka.invoicing.domain.ServiceNeedOptionVoucherValue
import fi.espoo.evaka.invoicing.domain.VoucherValueDecisionServiceNeed
import fi.espoo.evaka.placement.PlacementType
import fi.espoo.evaka.serviceneed.ServiceNeedOption
import fi.espoo.evaka.serviceneed.ServiceNeedOptionFee
import fi.espoo.evaka.shared.ServiceNeedOptionId
import fi.espoo.evaka.shared.ServiceNeedOptionVoucherValueId
import fi.espoo.evaka.shared.domain.DateRange
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

val snDefaultDaycare =
    ServiceNeedOption(
        id = ServiceNeedOptionId(UUID.randomUUID()),
        nameFi = "Kokopäiväinen",
        nameSv = "Kokopäiväinen",
        nameEn = "Kokopäiväinen",
        validPlacementType = PlacementType.DAYCARE,
        defaultOption = true,
        feeCoefficient = BigDecimal("1.00"),
        occupancyCoefficient = BigDecimal("1.00"),
        occupancyCoefficientUnder3y = BigDecimal("1.75"),
        realizedOccupancyCoefficient = BigDecimal("1.00"),
        realizedOccupancyCoefficientUnder3y = BigDecimal("1.75"),
        daycareHoursPerWeek = 35,
        contractDaysPerMonth = null,
        daycareHoursPerMonth = null,
        partDay = false,
        partWeek = false,
        feeDescriptionFi = "",
        feeDescriptionSv = "",
        voucherValueDescriptionFi = "Kokopäiväinen",
        voucherValueDescriptionSv = "Kokopäiväinen",
        active = true
    )

val snDefaultPartDayDaycare =
    ServiceNeedOption(
        id = ServiceNeedOptionId(UUID.randomUUID()),
        nameFi = "Kokopäiväinen",
        nameSv = "Kokopäiväinen",
        nameEn = "Kokopäiväinen",
        validPlacementType = PlacementType.DAYCARE_PART_TIME,
        defaultOption = true,
        feeCoefficient = BigDecimal("1.00"),
        occupancyCoefficient = BigDecimal("0.54"),
        occupancyCoefficientUnder3y = BigDecimal("1.75"),
        realizedOccupancyCoefficient = BigDecimal("0.54"),
        realizedOccupancyCoefficientUnder3y = BigDecimal("1.75"),
        daycareHoursPerWeek = 25,
        contractDaysPerMonth = null,
        daycareHoursPerMonth = null,
        partDay = true,
        partWeek = false,
        feeDescriptionFi = "",
        feeDescriptionSv = "",
        voucherValueDescriptionFi = "",
        voucherValueDescriptionSv = "",
        active = true
    )

val snDefaultFiveYearOldsDaycare =
    ServiceNeedOption(
        id = ServiceNeedOptionId(UUID.randomUUID()),
        nameFi = "Kokopäiväinen",
        nameSv = "Kokopäiväinen",
        nameEn = "Kokopäiväinen",
        validPlacementType = PlacementType.DAYCARE_FIVE_YEAR_OLDS,
        defaultOption = true,
        feeCoefficient = BigDecimal("0.80"),
        occupancyCoefficient = BigDecimal("1.00"),
        occupancyCoefficientUnder3y = BigDecimal("1.75"),
        realizedOccupancyCoefficient = BigDecimal("1.00"),
        realizedOccupancyCoefficientUnder3y = BigDecimal("1.75"),
        daycareHoursPerWeek = 45,
        contractDaysPerMonth = null,
        daycareHoursPerMonth = null,
        partDay = false,
        partWeek = false,
        feeDescriptionFi = "",
        feeDescriptionSv = "",
        voucherValueDescriptionFi = "",
        voucherValueDescriptionSv = "",
        active = true
    )

val snDefaultFiveYearOldsPartDayDaycare =
    ServiceNeedOption(
        id = ServiceNeedOptionId(UUID.randomUUID()),
        nameFi = "Viisivuotiaiden osapäiväinen",
        nameSv = "Viisivuotiaiden osapäiväinen",
        nameEn = "Viisivuotiaiden osapäiväinen",
        validPlacementType = PlacementType.DAYCARE_PART_TIME_FIVE_YEAR_OLDS,
        defaultOption = true,
        feeCoefficient = BigDecimal("0.80"),
        occupancyCoefficient = BigDecimal("0.54"),
        occupancyCoefficientUnder3y = BigDecimal("1.75"),
        realizedOccupancyCoefficient = BigDecimal("0.54"),
        realizedOccupancyCoefficientUnder3y = BigDecimal("1.75"),
        daycareHoursPerWeek = 25,
        contractDaysPerMonth = null,
        daycareHoursPerMonth = null,
        partDay = true,
        partWeek = false,
        feeDescriptionFi = "",
        feeDescriptionSv = "",
        voucherValueDescriptionFi = "",
        voucherValueDescriptionSv = "",
        active = true
    )

val snDefaultPreschool =
    ServiceNeedOption(
        id = ServiceNeedOptionId(UUID.randomUUID()),
        nameFi = "Esiopetus",
        nameSv = "Esiopetus",
        nameEn = "Esiopetus",
        validPlacementType = PlacementType.PRESCHOOL,
        defaultOption = true,
        feeCoefficient = BigDecimal("0.00"),
        occupancyCoefficient = BigDecimal("0.50"),
        occupancyCoefficientUnder3y = BigDecimal("1.75"),
        realizedOccupancyCoefficient = BigDecimal("0.50"),
        realizedOccupancyCoefficientUnder3y = BigDecimal("1.75"),
        daycareHoursPerWeek = 0,
        contractDaysPerMonth = null,
        daycareHoursPerMonth = null,
        partDay = true,
        partWeek = true,
        feeDescriptionFi = "",
        feeDescriptionSv = "",
        voucherValueDescriptionFi = "",
        voucherValueDescriptionSv = "",
        active = true
    )

val snDefaultPreschoolDaycare =
    ServiceNeedOption(
        id = ServiceNeedOptionId(UUID.randomUUID()),
        nameFi = "Esiopetus ja liittyvä varhaiskasvatus",
        nameSv = "Esiopetus ja liittyvä varhaiskasvatus",
        nameEn = "Esiopetus ja liittyvä varhaiskasvatus",
        validPlacementType = PlacementType.PRESCHOOL_DAYCARE,
        defaultOption = true,
        feeCoefficient = BigDecimal("0.80"),
        occupancyCoefficient = BigDecimal("1.00"),
        occupancyCoefficientUnder3y = BigDecimal("1.75"),
        realizedOccupancyCoefficient = BigDecimal("1.00"),
        realizedOccupancyCoefficientUnder3y = BigDecimal("1.75"),
        daycareHoursPerWeek = 25,
        contractDaysPerMonth = null,
        daycareHoursPerMonth = null,
        partDay = false,
        partWeek = false,
        feeDescriptionFi = "",
        feeDescriptionSv = "",
        voucherValueDescriptionFi = "",
        voucherValueDescriptionSv = "",
        active = true
    )

val snDefaultPreparatory =
    ServiceNeedOption(
        id = ServiceNeedOptionId(UUID.randomUUID()),
        nameFi = "Valmistava opetus",
        nameSv = "Valmistava opetus",
        nameEn = "Valmistava opetus",
        validPlacementType = PlacementType.PREPARATORY,
        defaultOption = true,
        feeCoefficient = BigDecimal("0.00"),
        occupancyCoefficient = BigDecimal("0.50"),
        occupancyCoefficientUnder3y = BigDecimal("1.75"),
        realizedOccupancyCoefficient = BigDecimal("0.50"),
        realizedOccupancyCoefficientUnder3y = BigDecimal("1.75"),
        daycareHoursPerWeek = 0,
        contractDaysPerMonth = null,
        daycareHoursPerMonth = null,
        partDay = true,
        partWeek = true,
        feeDescriptionFi = "",
        feeDescriptionSv = "",
        voucherValueDescriptionFi = "",
        voucherValueDescriptionSv = "",
        active = true
    )

val snDefaultPreparatoryDaycare =
    ServiceNeedOption(
        id = ServiceNeedOptionId(UUID.randomUUID()),
        nameFi = "Valmistava opetus ja liittyvä varhaiskasvatus",
        nameSv = "Valmistava opetus ja liittyvä varhaiskasvatus",
        nameEn = "Valmistava opetus ja liittyvä varhaiskasvatus",
        validPlacementType = PlacementType.PREPARATORY_DAYCARE,
        defaultOption = true,
        feeCoefficient = BigDecimal("0.80"),
        occupancyCoefficient = BigDecimal("1.00"),
        occupancyCoefficientUnder3y = BigDecimal("1.75"),
        realizedOccupancyCoefficient = BigDecimal("1.00"),
        realizedOccupancyCoefficientUnder3y = BigDecimal("1.75"),
        daycareHoursPerWeek = 25,
        contractDaysPerMonth = null,
        daycareHoursPerMonth = null,
        partDay = false,
        partWeek = false,
        feeDescriptionFi = "",
        feeDescriptionSv = "",
        voucherValueDescriptionFi = "",
        voucherValueDescriptionSv = "",
        active = true
    )

val snDefaultClub =
    ServiceNeedOption(
        id = ServiceNeedOptionId(UUID.randomUUID()),
        nameFi = "Kerho",
        nameSv = "Kerho",
        nameEn = "Kerho",
        validPlacementType = PlacementType.CLUB,
        defaultOption = true,
        feeCoefficient = BigDecimal("0.00"),
        occupancyCoefficient = BigDecimal("1.00"),
        occupancyCoefficientUnder3y = BigDecimal("1.75"),
        realizedOccupancyCoefficient = BigDecimal("1.00"),
        realizedOccupancyCoefficientUnder3y = BigDecimal("1.75"),
        daycareHoursPerWeek = 0,
        contractDaysPerMonth = null,
        daycareHoursPerMonth = null,
        partDay = true,
        partWeek = true,
        feeDescriptionFi = "",
        feeDescriptionSv = "",
        voucherValueDescriptionFi = "",
        voucherValueDescriptionSv = "",
        active = true
    )

val snDefaultTemporaryDaycare =
    ServiceNeedOption(
        id = ServiceNeedOptionId(UUID.randomUUID()),
        nameFi = "Kokopäiväinen tilapäinen",
        nameSv = "Kokopäiväinen tilapäinen",
        nameEn = "Kokopäiväinen tilapäinen",
        validPlacementType = PlacementType.TEMPORARY_DAYCARE,
        defaultOption = true,
        feeCoefficient = BigDecimal("0.00"),
        occupancyCoefficient = BigDecimal("1.00"),
        occupancyCoefficientUnder3y = BigDecimal("1.75"),
        realizedOccupancyCoefficient = BigDecimal("1.00"),
        realizedOccupancyCoefficientUnder3y = BigDecimal("1.75"),
        daycareHoursPerWeek = 35,
        contractDaysPerMonth = null,
        daycareHoursPerMonth = null,
        partDay = false,
        partWeek = true,
        feeDescriptionFi = "",
        feeDescriptionSv = "",
        voucherValueDescriptionFi = "",
        voucherValueDescriptionSv = "",
        active = true
    )

val snDefaultTemporaryPartDayDaycare =
    ServiceNeedOption(
        id = ServiceNeedOptionId(UUID.randomUUID()),
        nameFi = "Osapäiväinen tilapäinen",
        nameSv = "Osapäiväinen tilapäinen",
        nameEn = "Osapäiväinen tilapäinen",
        validPlacementType = PlacementType.TEMPORARY_DAYCARE_PART_DAY,
        defaultOption = true,
        feeCoefficient = BigDecimal("0.00"),
        occupancyCoefficient = BigDecimal("0.54"),
        occupancyCoefficientUnder3y = BigDecimal("1.75"),
        realizedOccupancyCoefficient = BigDecimal("0.54"),
        realizedOccupancyCoefficientUnder3y = BigDecimal("1.75"),
        daycareHoursPerWeek = 25,
        contractDaysPerMonth = null,
        daycareHoursPerMonth = null,
        partDay = true,
        partWeek = true,
        feeDescriptionFi = "",
        feeDescriptionSv = "",
        voucherValueDescriptionFi = "",
        voucherValueDescriptionSv = "",
        active = true
    )

val snDaycareFullDay35 =
    ServiceNeedOption(
        id = ServiceNeedOptionId(UUID.randomUUID()),
        nameFi = "Kokopäiväinen, vähintään 35h",
        nameSv = "Kokopäiväinen, vähintään 35h",
        nameEn = "Kokopäiväinen, vähintään 35h",
        validPlacementType = PlacementType.DAYCARE,
        defaultOption = false,
        feeCoefficient = BigDecimal("1.00"),
        occupancyCoefficient = BigDecimal("1.00"),
        occupancyCoefficientUnder3y = BigDecimal("1.75"),
        realizedOccupancyCoefficient = BigDecimal("1.00"),
        realizedOccupancyCoefficientUnder3y = BigDecimal("1.75"),
        daycareHoursPerWeek = 35,
        contractDaysPerMonth = null,
        daycareHoursPerMonth = null,
        partDay = false,
        partWeek = false,
        feeDescriptionFi = "",
        feeDescriptionSv = "",
        voucherValueDescriptionFi = "Kokopäiväinen, vähintään 35h",
        voucherValueDescriptionSv = "Kokopäiväinen, vähintään 35h",
        active = true
    )

val snDaycareFullDay25to35 =
    ServiceNeedOption(
        id = ServiceNeedOptionId(UUID.randomUUID()),
        nameFi = "Kokopäiväinen, 25-35h",
        nameSv = "Kokopäiväinen, 25-35h",
        nameEn = "Kokopäiväinen, 25-35h",
        validPlacementType = PlacementType.DAYCARE,
        defaultOption = false,
        feeCoefficient = BigDecimal("0.80"),
        occupancyCoefficient = BigDecimal("1.00"),
        occupancyCoefficientUnder3y = BigDecimal("1.75"),
        realizedOccupancyCoefficient = BigDecimal("1.00"),
        realizedOccupancyCoefficientUnder3y = BigDecimal("1.75"),
        daycareHoursPerWeek = 30,
        contractDaysPerMonth = null,
        daycareHoursPerMonth = null,
        partDay = false,
        partWeek = false,
        feeDescriptionFi = "",
        feeDescriptionSv = "",
        voucherValueDescriptionFi = "",
        voucherValueDescriptionSv = "",
        active = true
    )

val snDaycareFullDayPartWeek25 =
    ServiceNeedOption(
        id = ServiceNeedOptionId(UUID.randomUUID()),
        nameFi = "Osaviikkoinen, enintään 25h",
        nameSv = "Osaviikkoinen, enintään 25h",
        nameEn = "Osaviikkoinen, enintään 25h",
        validPlacementType = PlacementType.DAYCARE,
        defaultOption = false,
        feeCoefficient = BigDecimal("0.60"),
        occupancyCoefficient = BigDecimal("1.00"),
        occupancyCoefficientUnder3y = BigDecimal("1.75"),
        realizedOccupancyCoefficient = BigDecimal("1.00"),
        realizedOccupancyCoefficientUnder3y = BigDecimal("1.75"),
        daycareHoursPerWeek = 25,
        contractDaysPerMonth = null,
        daycareHoursPerMonth = null,
        partDay = false,
        partWeek = true,
        feeDescriptionFi = "",
        feeDescriptionSv = "",
        voucherValueDescriptionFi = "",
        voucherValueDescriptionSv = "",
        active = true
    )

val snDaycarePartDay25 =
    ServiceNeedOption(
        id = ServiceNeedOptionId(UUID.randomUUID()),
        nameFi = "Osapäiväinen",
        nameSv = "Osapäiväinen",
        nameEn = "Osapäiväinen",
        validPlacementType = PlacementType.DAYCARE_PART_TIME,
        defaultOption = false,
        feeCoefficient = BigDecimal("0.60"),
        occupancyCoefficient = BigDecimal("0.54"),
        occupancyCoefficientUnder3y = BigDecimal("1.75"),
        realizedOccupancyCoefficient = BigDecimal("0.54"),
        realizedOccupancyCoefficientUnder3y = BigDecimal("1.75"),
        daycareHoursPerWeek = 25,
        contractDaysPerMonth = null,
        daycareHoursPerMonth = null,
        partDay = true,
        partWeek = false,
        feeDescriptionFi = "",
        feeDescriptionSv = "",
        voucherValueDescriptionFi = "",
        voucherValueDescriptionSv = "",
        active = true
    )

val snPreschoolDaycare45 =
    ServiceNeedOption(
        id = ServiceNeedOptionId(UUID.randomUUID()),
        nameFi = "Kokopäiväinen liittyvä, yhteensä vähintään 45h",
        nameSv = "Kokopäiväinen liittyvä, yhteensä vähintään 45h",
        nameEn = "Kokopäiväinen liittyvä, yhteensä vähintään 45h",
        validPlacementType = PlacementType.PRESCHOOL_DAYCARE,
        defaultOption = false,
        feeCoefficient = BigDecimal("0.80"),
        occupancyCoefficient = BigDecimal("1.00"),
        occupancyCoefficientUnder3y = BigDecimal("1.75"),
        realizedOccupancyCoefficient = BigDecimal("1.00"),
        realizedOccupancyCoefficientUnder3y = BigDecimal("1.75"),
        daycareHoursPerWeek = 25,
        contractDaysPerMonth = null,
        daycareHoursPerMonth = null,
        partDay = false,
        partWeek = false,
        feeDescriptionFi = "",
        feeDescriptionSv = "",
        voucherValueDescriptionFi = "",
        voucherValueDescriptionSv = "",
        active = true
    )

val snPreschoolDaycarePartDay35to45 =
    ServiceNeedOption(
        id = ServiceNeedOptionId(UUID.randomUUID()),
        nameFi = "Osapäiväinen liittyvä, yhteensä 35-45h",
        nameSv = "Osapäiväinen liittyvä, yhteensä 35-45h",
        nameEn = "Osapäiväinen liittyvä, yhteensä 35-45h",
        validPlacementType = PlacementType.PRESCHOOL_DAYCARE,
        defaultOption = false,
        feeCoefficient = BigDecimal("0.60"),
        occupancyCoefficient = BigDecimal("1.00"),
        occupancyCoefficientUnder3y = BigDecimal("1.75"),
        realizedOccupancyCoefficient = BigDecimal("1.00"),
        realizedOccupancyCoefficientUnder3y = BigDecimal("1.75"),
        daycareHoursPerWeek = 20,
        contractDaysPerMonth = null,
        daycareHoursPerMonth = null,
        partDay = true,
        partWeek = false,
        feeDescriptionFi = "",
        feeDescriptionSv = "",
        voucherValueDescriptionFi = "",
        voucherValueDescriptionSv = "",
        active = true
    )

val snPreschoolDaycarePartDay35 =
    ServiceNeedOption(
        id = ServiceNeedOptionId(UUID.randomUUID()),
        nameFi = "Osapäiväinen liittyvä, yhteensä enintään 35h",
        nameSv = "Osapäiväinen liittyvä, yhteensä enintään 35h",
        nameEn = "Osapäiväinen liittyvä, yhteensä enintään 35h",
        validPlacementType = PlacementType.PRESCHOOL_DAYCARE,
        defaultOption = false,
        feeCoefficient = BigDecimal("0.35"),
        occupancyCoefficient = BigDecimal("1.00"),
        occupancyCoefficientUnder3y = BigDecimal("1.75"),
        realizedOccupancyCoefficient = BigDecimal("1.00"),
        realizedOccupancyCoefficientUnder3y = BigDecimal("1.75"),
        daycareHoursPerWeek = 15,
        contractDaysPerMonth = null,
        daycareHoursPerMonth = null,
        partDay = true,
        partWeek = false,
        feeDescriptionFi = "",
        feeDescriptionSv = "",
        voucherValueDescriptionFi = "",
        voucherValueDescriptionSv = "",
        active = true
    )

val snPreschoolClub45 =
    ServiceNeedOption(
        id = ServiceNeedOptionId(UUID.randomUUID()),
        nameFi = "Kokopäiväinen liittyvä kerho, yhteensä vähintään 45h",
        nameSv = "Kokopäiväinen liittyvä kerho, yhteensä vähintään 45h",
        nameEn = "Kokopäiväinen liittyvä kerho, yhteensä vähintään 45h",
        validPlacementType = PlacementType.PRESCHOOL_CLUB,
        defaultOption = false,
        feeCoefficient = BigDecimal("0.80"),
        occupancyCoefficient = BigDecimal("1.00"),
        occupancyCoefficientUnder3y = BigDecimal("1.75"),
        realizedOccupancyCoefficient = BigDecimal("1.00"),
        realizedOccupancyCoefficientUnder3y = BigDecimal("1.75"),
        daycareHoursPerWeek = 25,
        contractDaysPerMonth = null,
        daycareHoursPerMonth = null,
        partDay = false,
        partWeek = false,
        feeDescriptionFi = "",
        feeDescriptionSv = "",
        voucherValueDescriptionFi = "",
        voucherValueDescriptionSv = "",
        active = true
    )

val snPreparatoryDaycare50 =
    ServiceNeedOption(
        id = ServiceNeedOptionId(UUID.randomUUID()),
        nameFi = "Kokopäiväinen liittyvä, yhteensä vähintään 50h",
        nameSv = "Kokopäiväinen liittyvä, yhteensä vähintään 50h",
        nameEn = "Kokopäiväinen liittyvä, yhteensä vähintään 50h",
        validPlacementType = PlacementType.PREPARATORY_DAYCARE,
        defaultOption = false,
        feeCoefficient = BigDecimal("0.80"),
        occupancyCoefficient = BigDecimal("1.00"),
        occupancyCoefficientUnder3y = BigDecimal("1.75"),
        realizedOccupancyCoefficient = BigDecimal("1.00"),
        realizedOccupancyCoefficientUnder3y = BigDecimal("1.75"),
        daycareHoursPerWeek = 25,
        contractDaysPerMonth = null,
        daycareHoursPerMonth = null,
        partDay = false,
        partWeek = false,
        feeDescriptionFi = "",
        feeDescriptionSv = "",
        voucherValueDescriptionFi = "",
        voucherValueDescriptionSv = "",
        active = true
    )

val snPreparatoryDaycarePartDay40to50 =
    ServiceNeedOption(
        id = ServiceNeedOptionId(UUID.randomUUID()),
        nameFi = "Osapäiväinen liittyvä, yhteensä 40-50h",
        nameSv = "Osapäiväinen liittyvä, yhteensä 40-50h",
        nameEn = "Osapäiväinen liittyvä, yhteensä 40-50h",
        validPlacementType = PlacementType.PREPARATORY_DAYCARE,
        defaultOption = false,
        feeCoefficient = BigDecimal("0.60"),
        occupancyCoefficient = BigDecimal("1.00"),
        occupancyCoefficientUnder3y = BigDecimal("1.75"),
        realizedOccupancyCoefficient = BigDecimal("1.00"),
        realizedOccupancyCoefficientUnder3y = BigDecimal("1.75"),
        daycareHoursPerWeek = 20,
        contractDaysPerMonth = null,
        daycareHoursPerMonth = null,
        partDay = true,
        partWeek = false,
        feeDescriptionFi = "",
        feeDescriptionSv = "",
        voucherValueDescriptionFi = "",
        voucherValueDescriptionSv = "",
        active = true
    )

val snPreparatoryDaycarePartDay40 =
    ServiceNeedOption(
        id = ServiceNeedOptionId(UUID.randomUUID()),
        nameFi = "Osapäiväinen liittyvä, yhteensä enintään 40h",
        nameSv = "Osapäiväinen liittyvä, yhteensä enintään 40h",
        nameEn = "Osapäiväinen liittyvä, yhteensä enintään 40h",
        validPlacementType = PlacementType.PREPARATORY_DAYCARE,
        defaultOption = false,
        feeCoefficient = BigDecimal("0.35"),
        occupancyCoefficient = BigDecimal("1.00"),
        occupancyCoefficientUnder3y = BigDecimal("1.75"),
        realizedOccupancyCoefficient = BigDecimal("1.00"),
        realizedOccupancyCoefficientUnder3y = BigDecimal("1.75"),
        daycareHoursPerWeek = 15,
        contractDaysPerMonth = null,
        daycareHoursPerMonth = null,
        partDay = true,
        partWeek = false,
        feeDescriptionFi = "",
        feeDescriptionSv = "",
        voucherValueDescriptionFi = "",
        voucherValueDescriptionSv = "",
        active = true
    )

val snDaycareFiveYearOldsFullDayPartWeek25 =
    ServiceNeedOption(
        id = ServiceNeedOptionId(UUID.randomUUID()),
        nameFi = "5-vuotiaiden osaviikkoinen, yli 20h enintään 25h",
        nameSv = "5-vuotiaiden osaviikkoinen, yli 20h enintään 25h",
        nameEn = "5-vuotiaiden osaviikkoinen, yli 20h enintään 25h",
        validPlacementType = PlacementType.DAYCARE_FIVE_YEAR_OLDS,
        defaultOption = false,
        feeCoefficient = BigDecimal("0.60"),
        occupancyCoefficient = BigDecimal("1.00"),
        occupancyCoefficientUnder3y = BigDecimal("1.75"),
        realizedOccupancyCoefficient = BigDecimal("1.00"),
        realizedOccupancyCoefficientUnder3y = BigDecimal("1.75"),
        daycareHoursPerWeek = 25,
        contractDaysPerMonth = null,
        daycareHoursPerMonth = null,
        partDay = false,
        partWeek = true,
        feeDescriptionFi = "",
        feeDescriptionSv = "",
        voucherValueDescriptionFi = "",
        voucherValueDescriptionSv = "",
        active = true
    )

val snDaycareContractDays15 =
    ServiceNeedOption(
        id = ServiceNeedOptionId(UUID.randomUUID()),
        nameFi = "Kokopäivähoito 15 pv/kk",
        nameSv = "Kokopäivähoito 15 pv/kk",
        nameEn = "Kokopäivähoito 15 pv/kk",
        validPlacementType = PlacementType.DAYCARE,
        defaultOption = false,
        feeCoefficient = BigDecimal("0.75"),
        occupancyCoefficient = BigDecimal("1.00"),
        occupancyCoefficientUnder3y = BigDecimal("1.75"),
        realizedOccupancyCoefficient = BigDecimal("1.00"),
        realizedOccupancyCoefficientUnder3y = BigDecimal("1.75"),
        daycareHoursPerWeek = 30,
        contractDaysPerMonth = 15,
        daycareHoursPerMonth = null,
        partDay = false,
        partWeek = true,
        feeDescriptionFi = "",
        feeDescriptionSv = "",
        voucherValueDescriptionFi = "",
        voucherValueDescriptionSv = "",
        active = true
    )

val snPreschoolDaycareContractDays13 =
    ServiceNeedOption(
        id = ServiceNeedOptionId(UUID.randomUUID()),
        nameFi = "Varhaiskasvatuksen liittyvä 15 pv/kk",
        nameSv = "Varhaiskasvatuksen liittyvä 15 pv/kk",
        nameEn = "Varhaiskasvatuksen liittyvä 15 pv/kk",
        validPlacementType = PlacementType.PRESCHOOL_DAYCARE,
        defaultOption = false,
        feeCoefficient = BigDecimal("0.60"),
        occupancyCoefficient = BigDecimal("0.25"),
        occupancyCoefficientUnder3y = BigDecimal("1.75"),
        realizedOccupancyCoefficient = BigDecimal("0.5"),
        realizedOccupancyCoefficientUnder3y = BigDecimal("1.75"),
        daycareHoursPerWeek = 18,
        contractDaysPerMonth = 13,
        daycareHoursPerMonth = null,
        partDay = false,
        partWeek = true,
        feeDescriptionFi = "",
        feeDescriptionSv = "",
        voucherValueDescriptionFi = "",
        voucherValueDescriptionSv = "",
        active = true
    )

val snDaycareContractDays10 =
    ServiceNeedOption(
        id = ServiceNeedOptionId(UUID.randomUUID()),
        nameFi = "Kokopäivähoito 10 pv/kk",
        nameSv = "Kokopäivähoito 10 pv/kk",
        nameEn = "Kokopäivähoito 10 pv/kk",
        validPlacementType = PlacementType.DAYCARE,
        defaultOption = false,
        feeCoefficient = BigDecimal("0.5"),
        occupancyCoefficient = BigDecimal("0.5"),
        occupancyCoefficientUnder3y = BigDecimal("0.88"),
        realizedOccupancyCoefficient = BigDecimal("0.5"),
        realizedOccupancyCoefficientUnder3y = BigDecimal("1.25"),
        daycareHoursPerWeek = 20,
        contractDaysPerMonth = 10,
        daycareHoursPerMonth = null,
        partDay = false,
        partWeek = true,
        feeDescriptionFi = "",
        feeDescriptionSv = "",
        voucherValueDescriptionFi = "",
        voucherValueDescriptionSv = "",
        active = true
    )

val snDefaultSchoolShiftcare =
    ServiceNeedOption(
        id = ServiceNeedOptionId(UUID.randomUUID()),
        nameFi = "Koululaisten vuorohoito",
        nameSv = "Koululaisten vuorohoito",
        nameEn = "Koululaisten vuorohoito",
        validPlacementType = PlacementType.SCHOOL_SHIFT_CARE,
        defaultOption = true,
        feeCoefficient = BigDecimal("0.00"),
        occupancyCoefficient = BigDecimal("1.00"),
        occupancyCoefficientUnder3y = BigDecimal("1.75"),
        realizedOccupancyCoefficient = BigDecimal("1.00"),
        realizedOccupancyCoefficientUnder3y = BigDecimal("1.75"),
        daycareHoursPerWeek = 0,
        contractDaysPerMonth = null,
        daycareHoursPerMonth = null,
        partDay = true,
        partWeek = true,
        feeDescriptionFi = "",
        feeDescriptionSv = "",
        voucherValueDescriptionFi = "",
        voucherValueDescriptionSv = "",
        active = true
    )

val serviceNeedTestFixtures =
    listOf(
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
        snPreschoolClub45,
        snPreparatoryDaycare50,
        snPreparatoryDaycarePartDay40to50,
        snPreparatoryDaycarePartDay40,
        snDaycareFiveYearOldsFullDayPartWeek25,
        snDaycareContractDays15,
        snDaycareContractDays10,
        snDefaultSchoolShiftcare
    )

val serviceNeedOptionFeeTestFixtures =
    listOf(
        ServiceNeedOptionFee(
            serviceNeedOptionId = snPreschoolClub45.id,
            validity = DateRange(LocalDate.of(2000, 1, 1), null),
            baseFee = 14000,
            siblingDiscount2 = BigDecimal("0.4"),
            siblingFee2 = 8000,
            siblingDiscount2Plus = BigDecimal("0.4"),
            siblingFee2Plus = 8000
        )
    )

val serviceNeedOptionVoucherValueCoefficients =
    mapOf(
        snDefaultDaycare.id to BigDecimal("1.00"),
        snDefaultPartDayDaycare.id to BigDecimal("0.60"),
        snDefaultFiveYearOldsDaycare.id to BigDecimal("1.00"),
        snDefaultFiveYearOldsPartDayDaycare.id to BigDecimal("0.60"),
        snDefaultPreschool.id to BigDecimal("0.00"),
        snDefaultPreschoolDaycare.id to BigDecimal("0.50"),
        snDefaultPreparatory.id to BigDecimal("0.50"),
        snDefaultPreparatoryDaycare.id to BigDecimal("0.50"),
        snDefaultClub.id to BigDecimal("0.00"),
        snDefaultTemporaryDaycare.id to BigDecimal("0.00"),
        snDefaultTemporaryPartDayDaycare.id to BigDecimal("0.00"),
        snDaycareFullDay35.id to BigDecimal("1.00"),
        snDaycareFullDay25to35.id to BigDecimal("1.00"),
        snDaycareFullDayPartWeek25.id to BigDecimal("0.60"),
        snDaycarePartDay25.id to BigDecimal("0.60"),
        snPreschoolDaycare45.id to BigDecimal("0.50"),
        snPreschoolDaycarePartDay35to45.id to BigDecimal("0.50"),
        snPreschoolDaycarePartDay35.id to BigDecimal("0.50"),
        snPreschoolClub45.id to BigDecimal("0.50"),
        snPreparatoryDaycare50.id to BigDecimal("0.50"),
        snPreparatoryDaycarePartDay40to50.id to BigDecimal("0.50"),
        snPreparatoryDaycarePartDay40.id to BigDecimal("0.50"),
        snDaycareFiveYearOldsFullDayPartWeek25.id to BigDecimal("0.60"),
        snDaycareContractDays15.id to BigDecimal("0.75"),
        snDaycareContractDays10.id to BigDecimal("0.50"),
        snDefaultSchoolShiftcare.id to BigDecimal("0.00")
    )

val serviceNeedOptionVoucherValueTestFixtures =
    serviceNeedTestFixtures.map {
        val baseValue = 87000
        val baseValueUnder3y = 134850 // 87000 * 1.55
        val coefficient = serviceNeedOptionVoucherValueCoefficients.get(it.id)!!

        ServiceNeedOptionVoucherValue(
            id = ServiceNeedOptionVoucherValueId(UUID.randomUUID()),
            serviceNeedOptionId = it.id,
            validity = DateRange(LocalDate.of(2020, 1, 1), null),
            baseValue = baseValue,
            coefficient = coefficient,
            value = (BigDecimal(baseValue) * coefficient).toInt(),
            baseValueUnder3y = baseValueUnder3y,
            coefficientUnder3y = coefficient,
            valueUnder3y = (BigDecimal(baseValueUnder3y) * coefficient).toInt()
        )
    }

fun ServiceNeedOption.toFeeDecisionServiceNeed() =
    FeeDecisionServiceNeed(
        optionId = this.id,
        feeCoefficient = this.feeCoefficient,
        contractDaysPerMonth = this.contractDaysPerMonth,
        descriptionFi = this.feeDescriptionFi,
        descriptionSv = this.feeDescriptionSv,
        missing = this.defaultOption
    )

fun ServiceNeedOption.toValueDecisionServiceNeed() =
    VoucherValueDecisionServiceNeed(
        feeCoefficient = this.feeCoefficient,
        voucherValueCoefficient = serviceNeedOptionVoucherValueCoefficients[this.id]!!,
        feeDescriptionFi = this.feeDescriptionFi,
        feeDescriptionSv = this.feeDescriptionSv,
        voucherValueDescriptionFi = this.voucherValueDescriptionFi,
        voucherValueDescriptionSv = this.voucherValueDescriptionSv,
        missing = this.defaultOption
    )
