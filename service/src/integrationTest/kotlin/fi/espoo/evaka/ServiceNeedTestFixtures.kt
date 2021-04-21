package fi.espoo.evaka

import fi.espoo.evaka.placement.PlacementType
import fi.espoo.evaka.serviceneednew.ServiceNeedOption
import java.math.BigDecimal
import java.util.UUID

val snDefaultDaycare = ServiceNeedOption(
    id = UUID.randomUUID(),
    name = "Kokopäiväinen",
    validPlacementType = PlacementType.DAYCARE,
    defaultOption = true,
    feeCoefficient = BigDecimal("1.00"),
    voucherValueCoefficient = BigDecimal("1.00"),
    occupancyCoefficient = BigDecimal("1.00"),
    daycareHoursPerWeek = 35,
    partDay = false,
    partWeek = false
)

val snDefaultPartDayDaycare = ServiceNeedOption(
    id = UUID.randomUUID(),
    name = "Osapäiväinen",
    validPlacementType = PlacementType.DAYCARE_PART_TIME,
    defaultOption = true,
    feeCoefficient = BigDecimal("0.60"),
    voucherValueCoefficient = BigDecimal("0.60"),
    occupancyCoefficient = BigDecimal("0.54"),
    daycareHoursPerWeek = 25,
    partDay = true,
    partWeek = false
)

val snDefaultPreschool = ServiceNeedOption(
    id = UUID.randomUUID(),
    name = "Esiopetus",
    validPlacementType = PlacementType.PRESCHOOL,
    defaultOption = true,
    feeCoefficient = BigDecimal("0.00"),
    voucherValueCoefficient = BigDecimal("0.50"),
    occupancyCoefficient = BigDecimal("0.50"),
    daycareHoursPerWeek = 0,
    partDay = true,
    partWeek = true
)

val snDefaultPreschoolDaycare = ServiceNeedOption(
    id = UUID.randomUUID(),
    name = "Esiopetus ja liittyvä varhaiskasvatus",
    validPlacementType = PlacementType.PRESCHOOL_DAYCARE,
    defaultOption = true,
    feeCoefficient = BigDecimal("0.80"),
    voucherValueCoefficient = BigDecimal("0.50"),
    occupancyCoefficient = BigDecimal("1.00"),
    daycareHoursPerWeek = 25,
    partDay = false,
    partWeek = false
)

val snDefaultPreparatory = ServiceNeedOption(
    id = UUID.randomUUID(),
    name = "Valmistava opetus",
    validPlacementType = PlacementType.PREPARATORY,
    defaultOption = true,
    feeCoefficient = BigDecimal("0.00"),
    voucherValueCoefficient = BigDecimal("0.50"),
    occupancyCoefficient = BigDecimal("0.50"),
    daycareHoursPerWeek = 0,
    partDay = true,
    partWeek = true
)

val snDefaultPreparatoryDaycare = ServiceNeedOption(
    id = UUID.randomUUID(),
    name = "Valmistava opetus ja liittyvä varhaiskasvatus",
    validPlacementType = PlacementType.PREPARATORY_DAYCARE,
    defaultOption = true,
    feeCoefficient = BigDecimal("0.80"),
    voucherValueCoefficient = BigDecimal("0.50"),
    occupancyCoefficient = BigDecimal("1.00"),
    daycareHoursPerWeek = 25,
    partDay = false,
    partWeek = false
)

val snDefaultClub = ServiceNeedOption(
    id = UUID.randomUUID(),
    name = "Kerho",
    validPlacementType = PlacementType.CLUB,
    defaultOption = true,
    feeCoefficient = BigDecimal("0.00"),
    voucherValueCoefficient = BigDecimal("0.00"),
    occupancyCoefficient = BigDecimal("1.00"),
    daycareHoursPerWeek = 0,
    partDay = true,
    partWeek = true
)

val snDefaultTemporaryDaycare = ServiceNeedOption(
    id = UUID.randomUUID(),
    name = "Kokopäiväinen tilapäinen",
    validPlacementType = PlacementType.TEMPORARY_DAYCARE,
    defaultOption = true,
    feeCoefficient = BigDecimal("0.00"),
    voucherValueCoefficient = BigDecimal("0.00"),
    occupancyCoefficient = BigDecimal("1.00"),
    daycareHoursPerWeek = 35,
    partDay = false,
    partWeek = true
)

val snDefaultTemporaryPartDayDaycare = ServiceNeedOption(
    id = UUID.randomUUID(),
    name = "Osapäiväinen tilapäinen",
    validPlacementType = PlacementType.TEMPORARY_DAYCARE_PART_DAY,
    defaultOption = true,
    feeCoefficient = BigDecimal("0.00"),
    voucherValueCoefficient = BigDecimal("0.00"),
    occupancyCoefficient = BigDecimal("0.54"),
    daycareHoursPerWeek = 25,
    partDay = true,
    partWeek = true
)

val snDaycareFullDay35 = ServiceNeedOption(
    id = UUID.randomUUID(),
    name = "Kokopäiväinen, vähintään 35h",
    validPlacementType = PlacementType.DAYCARE,
    defaultOption = false,
    feeCoefficient = BigDecimal("1.00"),
    voucherValueCoefficient = BigDecimal("1.0"),
    occupancyCoefficient = BigDecimal("1.00"),
    daycareHoursPerWeek = 35,
    partDay = false,
    partWeek = false
)

val snDaycareFullDay25to35 = ServiceNeedOption(
    id = UUID.randomUUID(),
    name = "Kokopäiväinen, 25-35h",
    validPlacementType = PlacementType.DAYCARE,
    defaultOption = false,
    feeCoefficient = BigDecimal("0.80"),
    voucherValueCoefficient = BigDecimal("1.00"),
    occupancyCoefficient = BigDecimal("1.00"),
    daycareHoursPerWeek = 30,
    partDay = false,
    partWeek = false
)

val snDaycareFullDayPartWeek25 = ServiceNeedOption(
    id = UUID.randomUUID(),
    name = "Osaviikkoinen, enintään 25h",
    validPlacementType = PlacementType.DAYCARE,
    defaultOption = false,
    feeCoefficient = BigDecimal("0.60"),
    voucherValueCoefficient = BigDecimal("0.60"),
    occupancyCoefficient = BigDecimal("1.00"),
    daycareHoursPerWeek = 25,
    partDay = false,
    partWeek = true
)

val snDaycarePartDay25 = ServiceNeedOption(
    id = UUID.randomUUID(),
    name = "Osapäiväinen",
    validPlacementType = PlacementType.DAYCARE_PART_TIME,
    defaultOption = false,
    feeCoefficient = BigDecimal("0.60"),
    voucherValueCoefficient = BigDecimal("0.60"),
    occupancyCoefficient = BigDecimal("0.54"),
    daycareHoursPerWeek = 25,
    partDay = true,
    partWeek = false
)

val snPreschoolDaycare45 = ServiceNeedOption(
    id = UUID.randomUUID(),
    name = "Kokopäiväinen liittyvä, yhteensä vähintään 45h",
    validPlacementType = PlacementType.PRESCHOOL_DAYCARE,
    defaultOption = false,
    feeCoefficient = BigDecimal("0.80"),
    voucherValueCoefficient = BigDecimal("0.50"),
    occupancyCoefficient = BigDecimal("1.00"),
    daycareHoursPerWeek = 25,
    partDay = false,
    partWeek = false
)

val snPreschoolDaycarePartDay35to45 = ServiceNeedOption(
    id = UUID.randomUUID(),
    name = "Osapäiväinen liittyvä, yhteensä 35-45h",
    validPlacementType = PlacementType.PRESCHOOL_DAYCARE,
    defaultOption = false,
    feeCoefficient = BigDecimal("0.60"),
    voucherValueCoefficient = BigDecimal("0.50"),
    occupancyCoefficient = BigDecimal("1.00"),
    daycareHoursPerWeek = 20,
    partDay = true,
    partWeek = false
)

val snPreschoolDaycarePartDay35 = ServiceNeedOption(
    id = UUID.randomUUID(),
    name = "Osapäiväinen liittyvä, yhteensä enintään 35h",
    validPlacementType = PlacementType.PRESCHOOL_DAYCARE,
    defaultOption = false,
    feeCoefficient = BigDecimal("0.35"),
    voucherValueCoefficient = BigDecimal("0.50"),
    occupancyCoefficient = BigDecimal("1.00"),
    daycareHoursPerWeek = 15,
    partDay = true,
    partWeek = false
)

val snPreparatoryDaycare50 = ServiceNeedOption(
    id = UUID.randomUUID(),
    name = "Kokopäiväinen liittyvä, yhteensä vähintään 50h",
    validPlacementType = PlacementType.PREPARATORY_DAYCARE,
    defaultOption = false,
    feeCoefficient = BigDecimal("0.80"),
    voucherValueCoefficient = BigDecimal("0.50"),
    occupancyCoefficient = BigDecimal("1.00"),
    daycareHoursPerWeek = 25,
    partDay = false,
    partWeek = false
)

val snPreparatoryDaycarePartDay40to50 = ServiceNeedOption(
    id = UUID.randomUUID(),
    name = "Osapäiväinen liittyvä, yhteensä 40-50h",
    validPlacementType = PlacementType.PREPARATORY_DAYCARE,
    defaultOption = false,
    feeCoefficient = BigDecimal("0.60"),
    voucherValueCoefficient = BigDecimal("0.50"),
    occupancyCoefficient = BigDecimal("1.00"),
    daycareHoursPerWeek = 20,
    partDay = true,
    partWeek = false
)

val snPreparatoryDaycarePartDay40 = ServiceNeedOption(
    id = UUID.randomUUID(),
    name = "Osapäiväinen liittyvä, yhteensä enintään 40h",
    validPlacementType = PlacementType.PREPARATORY_DAYCARE,
    defaultOption = false,
    feeCoefficient = BigDecimal("0.35"),
    voucherValueCoefficient = BigDecimal("0.50"),
    occupancyCoefficient = BigDecimal("1.00"),
    daycareHoursPerWeek = 15,
    partDay = true,
    partWeek = false
)

val serviceNeedTestFixtures = listOf(
    snDefaultDaycare,
    snDefaultPartDayDaycare,
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
    snPreparatoryDaycarePartDay40
)
