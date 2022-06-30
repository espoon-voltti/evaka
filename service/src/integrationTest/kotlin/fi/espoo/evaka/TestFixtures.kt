// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka

import fi.espoo.evaka.application.Address
import fi.espoo.evaka.application.ApplicationDetails
import fi.espoo.evaka.application.ApplicationForm
import fi.espoo.evaka.application.ApplicationOrigin
import fi.espoo.evaka.application.ApplicationStatus
import fi.espoo.evaka.application.ChildDetails
import fi.espoo.evaka.application.Guardian
import fi.espoo.evaka.application.PersonBasics
import fi.espoo.evaka.application.Preferences
import fi.espoo.evaka.application.PreferredUnit
import fi.espoo.evaka.application.ServiceNeed
import fi.espoo.evaka.application.ServiceNeedOption
import fi.espoo.evaka.application.persistence.daycare.DaycareFormV0
import fi.espoo.evaka.daycare.CareType
import fi.espoo.evaka.daycare.PreschoolTerm
import fi.espoo.evaka.daycare.domain.Language
import fi.espoo.evaka.daycare.domain.ProviderType
import fi.espoo.evaka.identity.ExternalId
import fi.espoo.evaka.invoicing.domain.EmployeeWithName
import fi.espoo.evaka.invoicing.domain.FeeThresholds
import fi.espoo.evaka.invoicing.domain.PersonBasic
import fi.espoo.evaka.invoicing.domain.PersonDetailed
import fi.espoo.evaka.invoicing.domain.UnitData
import fi.espoo.evaka.placement.PlacementType
import fi.espoo.evaka.shared.ApplicationId
import fi.espoo.evaka.shared.AreaId
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.EmployeeId
import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.dev.DevCareArea
import fi.espoo.evaka.shared.dev.DevChild
import fi.espoo.evaka.shared.dev.DevDaycare
import fi.espoo.evaka.shared.dev.DevEmployee
import fi.espoo.evaka.shared.dev.DevPerson
import fi.espoo.evaka.shared.dev.insertTestApplication
import fi.espoo.evaka.shared.dev.insertTestApplicationForm
import fi.espoo.evaka.shared.dev.insertTestCareArea
import fi.espoo.evaka.shared.dev.insertTestChild
import fi.espoo.evaka.shared.dev.insertTestDaycare
import fi.espoo.evaka.shared.dev.insertTestEmployee
import fi.espoo.evaka.shared.dev.insertTestFeeThresholds
import fi.espoo.evaka.shared.dev.insertTestPerson
import fi.espoo.evaka.shared.dev.updateDaycareAcl
import fi.espoo.evaka.shared.domain.DateRange
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.security.PilotFeature
import fi.espoo.evaka.shared.security.upsertCitizenUser
import org.jdbi.v3.core.kotlin.bindKotlin
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

/*
 Queries and data classes for initializing integration tests with person and unit data
 */

val testArea = DevCareArea(id = AreaId(UUID.randomUUID()), name = "Test Area", areaCode = 200)
val testArea2 = DevCareArea(id = AreaId(UUID.randomUUID()), name = "Lwiz Foo", shortName = "short name 2", areaCode = 300)
val testAreaSvebi = DevCareArea(
    id = AreaId(UUID.randomUUID()),
    name = "Svenska Bildningstjanster",
    shortName = "svenska-bildningstjanster",
    areaCode = 400
)

val allAreas = listOf(testArea, testArea2, testAreaSvebi)

val defaultMunicipalOrganizerOid = "1.2.246.562.10.888888888888"
val defaultPurchasedOrganizerOid = "1.2.246.562.10.66666666666"

val unitSupervisorExternalId = ExternalId.of("test", UUID.randomUUID().toString())

val testDecisionMaker_1 = DevEmployee(
    id = EmployeeId(UUID.randomUUID()),
    firstName = "Decision",
    lastName = "Maker"
)

val testDecisionMaker_2 = DevEmployee(
    id = EmployeeId(UUID.randomUUID()),
    firstName = "Decision2",
    lastName = "Maker2"
)

val testDecisionMaker_3 = DevEmployee(
    id = EmployeeId(UUID.randomUUID()),
    firstName = "Decision3",
    lastName = "Maker3"
)

val unitSupervisorOfTestDaycare = DevEmployee(
    id = EmployeeId(UUID.randomUUID()),
    firstName = "Sammy",
    lastName = "Supervisor"
)

val testDaycare = DevDaycare(
    id = DaycareId(UUID.randomUUID()),
    name = "Test Daycare",
    areaId = testArea.id,
    ophOrganizerOid = defaultMunicipalOrganizerOid,
    enabledPilotFeatures = setOf(PilotFeature.MESSAGING, PilotFeature.MOBILE, PilotFeature.RESERVATIONS, PilotFeature.PLACEMENT_TERMINATION),
)

val testDaycare2 = DevDaycare(
    id = DaycareId(UUID.randomUUID()),
    name = "Test Daycare 2",
    areaId = testArea2.id,
    enabledPilotFeatures = setOf(PilotFeature.MESSAGING),
    financeDecisionHandler = testDecisionMaker_2.id
)

val testDaycareNotInvoiced = DevDaycare(
    id = DaycareId(UUID.randomUUID()),
    name = "Not Invoiced",
    areaId = testArea2.id,
    invoicedByMunicipality = false
)

val testSvebiDaycare = DevDaycare(
    id = DaycareId(UUID.randomUUID()),
    name = "Test Svebi Daycare",
    areaId = testAreaSvebi.id,
    language = Language.sv
)

val testPurchasedDaycare = DevDaycare(
    id = DaycareId(UUID.randomUUID()),
    name = "Test Purchased Daycare",
    areaId = testArea.id,
    providerType = ProviderType.PURCHASED,
    ophOrganizerOid = defaultPurchasedOrganizerOid,
    invoicedByMunicipality = false
)

val testExternalPurchasedDaycare = DevDaycare(
    id = DaycareId(UUID.randomUUID()),
    name = "Test External Purchased Daycare",
    areaId = testArea.id,
    providerType = ProviderType.EXTERNAL_PURCHASED,
    ophOrganizerOid = defaultPurchasedOrganizerOid,
    invoicedByMunicipality = false
)

val testVoucherDaycare = DevDaycare(
    id = DaycareId(UUID.randomUUID()),
    name = "Test Voucher Daycare",
    areaId = testArea.id,
    providerType = ProviderType.PRIVATE_SERVICE_VOUCHER,
    ophOrganizerOid = defaultPurchasedOrganizerOid,
    invoicedByMunicipality = false,
    financeDecisionHandler = testDecisionMaker_2.id,
    businessId = "1234567-8",
    iban = "FI12 3456 7891 2345 67",
    providerId = "1234"
)

val testVoucherDaycare2 = DevDaycare(
    id = DaycareId(UUID.randomUUID()),
    name = "Test Voucher Daycare 2",
    areaId = testArea.id,
    providerType = ProviderType.PRIVATE_SERVICE_VOUCHER,
    ophOrganizerOid = defaultPurchasedOrganizerOid,
    invoicedByMunicipality = false,
    businessId = "8765432-1-8",
    iban = "FI98 7654 3210 9876 54",
    providerId = "4321"
)

val testClub = DevDaycare(
    id = DaycareId(UUID.randomUUID()),
    name = "Test Club",
    areaId = testArea.id,
    type = setOf(CareType.CLUB),
    clubApplyPeriod = DateRange(LocalDate.of(2020, 3, 1), null),
    daycareApplyPeriod = null,
    preschoolApplyPeriod = null,
    uploadToVarda = false,
    uploadChildrenToVarda = false,
    uploadToKoski = false
)

val testGhostUnitDaycare = DevDaycare(
    id = DaycareId(UUID.randomUUID()),
    name = "Test Ghost Unit Daycare",
    areaId = testArea.id,
    type = setOf(CareType.CENTRE),
    uploadToVarda = false,
    uploadChildrenToVarda = false,
    uploadToKoski = false,
    ghostUnit = true,
    invoicedByMunicipality = false
)

val testRoundTheClockDaycare = DevDaycare(
    id = DaycareId(UUID.randomUUID()),
    name = "Test Ghost Unit Daycare",
    areaId = testArea.id,
    type = setOf(CareType.CENTRE),
    roundTheClock = true,
    operationDays = setOf(1, 2, 3, 4, 5, 6, 7)
)

val testAdult_1 = DevPerson(
    id = PersonId(UUID.randomUUID()),
    dateOfBirth = LocalDate.of(1980, 1, 1),
    ssn = "010180-1232",
    firstName = "John",
    lastName = "Doe",
    streetAddress = "Kamreerintie 2",
    postalCode = "02770",
    postOffice = "Espoo",
    restrictedDetailsEnabled = false
)

val testAdult_2 = DevPerson(
    id = PersonId(UUID.randomUUID()),
    dateOfBirth = LocalDate.of(1979, 2, 1),
    ssn = "010279-123L",
    firstName = "Joan",
    lastName = "Doe",
    streetAddress = "Kamreerintie 2",
    postalCode = "02770",
    postOffice = "Espoo",
    restrictedDetailsEnabled = false,
    email = "joan.doe@example.com"
)

val testAdult_3 = DevPerson(
    id = PersonId(UUID.randomUUID()),
    dateOfBirth = LocalDate.of(1985, 6, 7),
    ssn = null,
    firstName = "Mark",
    lastName = "Foo",
    streetAddress = "",
    postalCode = "",
    postOffice = "",
    restrictedDetailsEnabled = false,
    email = "mark.foo@example.com"
)

val testAdult_4 = DevPerson(
    id = PersonId(UUID.randomUUID()),
    dateOfBirth = LocalDate.of(1981, 3, 2),
    ssn = null,
    firstName = "Dork",
    lastName = "Aman",
    streetAddress = "Muutie 66",
    postalCode = "02230",
    postOffice = "Espoo",
    restrictedDetailsEnabled = false,
    email = "dork.aman@example.com"
)

// Matches VTJ mock person Johannes Karhula
val testAdult_5 = DevPerson(
    id = PersonId(UUID.randomUUID()),
    dateOfBirth = LocalDate.of(1944, 6, 7),
    ssn = "070644-937X",
    firstName = "Johannes Olavi Antero Tapio",
    lastName = "Karhula",
    streetAddress = "Kamreerintie 1",
    postalCode = "00340",
    postOffice = "Espoo",
    restrictedDetailsEnabled = false
)

// Matches VTJ mock person Ville Vilkas
val testAdult_6 = DevPerson(
    id = PersonId(UUID.randomUUID()),
    dateOfBirth = LocalDate.of(1999, 12, 31),
    ssn = "311299-999E",
    email = "ville.vilkas@test.com",
    firstName = "Ville",
    lastName = "Vilkas",
    streetAddress = "Toistie 33",
    postalCode = "02230",
    postOffice = "Espoo",
    restrictedDetailsEnabled = false
)

val testAdult_7 = DevPerson(
    id = PersonId(UUID.randomUUID()),
    dateOfBirth = LocalDate.of(1980, 1, 1),
    ssn = "010180-969B",
    firstName = "Tepi",
    lastName = "Turvakiellollinen",
    streetAddress = "Suojatie 112",
    postalCode = "02230",
    postOffice = "Espoo",
    restrictedDetailsEnabled = true
)

val testChild_1 = DevPerson(
    id = ChildId(UUID.randomUUID()),
    dateOfBirth = LocalDate.of(2017, 6, 1),
    ssn = "010617A123U",
    firstName = "Ricky",
    lastName = "Doe",
    streetAddress = "Kamreerintie 2",
    postalCode = "02770",
    postOffice = "Espoo",
    restrictedDetailsEnabled = false
)

val testChild_2 = DevPerson(
    id = ChildId(UUID.randomUUID()),
    dateOfBirth = LocalDate.of(2016, 3, 1),
    ssn = "010316A1235",
    firstName = "Micky",
    lastName = "Doe",
    streetAddress = "Kamreerintie 2",
    postalCode = "02770",
    postOffice = "Espoo",
    restrictedDetailsEnabled = false
)

val testChild_3 = DevPerson(
    id = ChildId(UUID.randomUUID()),
    dateOfBirth = LocalDate.of(2018, 9, 1),
    ssn = "120220A995L",
    firstName = "Hillary",
    lastName = "Foo",
    streetAddress = "Kankkulankaivo 1",
    postalCode = "00340",
    postOffice = "Espoo",
    restrictedDetailsEnabled = false
)

val testChild_4 = DevPerson(
    id = ChildId(UUID.randomUUID()),
    dateOfBirth = LocalDate.of(2019, 3, 2),
    ssn = "020319A990J",
    firstName = "Maisa",
    lastName = "Farang",
    streetAddress = "Mannerheimintie 1",
    postalCode = "00100",
    postOffice = "Helsinki",
    restrictedDetailsEnabled = false
)

val testChild_5 = DevPerson(
    id = ChildId(UUID.randomUUID()),
    dateOfBirth = LocalDate.of(2018, 11, 13),
    ssn = "131118A111F",
    firstName = "Visa",
    lastName = "Virén",
    streetAddress = "Matinkatu 22",
    postalCode = "02230",
    postOffice = "Espoo",
    restrictedDetailsEnabled = false
)

// Matches vtj mock child Jari-Petteri Karhula
val testChild_6 = DevPerson(
    id = ChildId(UUID.randomUUID()),
    dateOfBirth = LocalDate.of(2018, 11, 13),
    ssn = "070714A9126",
    firstName = "Jari-Petteri Mukkelis-Makkelis Vetelä-Viljami Eelis-Juhani",
    lastName = "Karhula",
    streetAddress = "Kamreerintie 1",
    postalCode = "00340",
    postOffice = "Espoo",
    restrictedDetailsEnabled = false
)

val testChild_7 = DevPerson(
    id = ChildId(UUID.randomUUID()),
    dateOfBirth = LocalDate.of(2018, 7, 28),
    ssn = null,
    firstName = "Heikki",
    lastName = "Hetuton",
    streetAddress = "Matinkatu 11",
    postalCode = "02230",
    postOffice = "Espoo",
    restrictedDetailsEnabled = false
)

val testChild_8 = DevPerson(
    id = ChildId(UUID.randomUUID()),
    dateOfBirth = LocalDate.of(2016, 3, 10),
    ssn = "010316A1237",
    firstName = "Captain",
    lastName = "America",
    streetAddress = "Kamreerintie 2",
    postalCode = "02770",
    postOffice = "Espoo",
    restrictedDetailsEnabled = false
)

val testChildWithNamelessGuardian = DevPerson(
    id = ChildId(UUID.randomUUID()),
    dateOfBirth = LocalDate.of(2018, 12, 31),
    ssn = "311218A999J",
    firstName = "Niilo",
    lastName = "Nimettömänpoika",
    streetAddress = "Kankkulankaivo 1",
    postalCode = "00340",
    postOffice = "Espoo",
    restrictedDetailsEnabled = false
)

val allWorkers = setOf(testDecisionMaker_1, testDecisionMaker_2, testDecisionMaker_3)
val allAdults = setOf(testAdult_1, testAdult_2, testAdult_3, testAdult_4, testAdult_5, testAdult_6, testAdult_7)
val allChildren = setOf(
    testChild_1,
    testChild_2,
    testChild_3,
    testChild_4,
    testChild_5,
    testChild_6,
    testChild_7,
    testChild_8,
    testChildWithNamelessGuardian
)
val allDaycares = setOf(testDaycare, testDaycare2)

fun Database.Transaction.insertGeneralTestFixtures() {
    insertTestCareArea(testArea)
    insertTestCareArea(testArea2)
    insertTestCareArea(testAreaSvebi)

    testDecisionMaker_1.let {
        insertTestEmployee(
            DevEmployee(
                id = it.id,
                firstName = it.firstName,
                lastName = it.lastName,
                roles = setOf(UserRole.SERVICE_WORKER)
            )
        )
    }

    testDecisionMaker_2.let {
        insertTestEmployee(
            DevEmployee(
                id = it.id,
                firstName = it.firstName,
                lastName = it.lastName
            )
        )
    }

    testDecisionMaker_3.let {
        insertTestEmployee(
            DevEmployee(
                id = it.id,
                firstName = it.firstName,
                lastName = it.lastName
            )
        )
    }

    insertTestDaycare(testSvebiDaycare)
    insertTestDaycare(testDaycare)
    insertTestDaycare(testDaycare2)
    insertTestDaycare(testDaycareNotInvoiced)
    insertTestDaycare(testPurchasedDaycare)
    insertTestDaycare(testExternalPurchasedDaycare)
    insertTestDaycare(testVoucherDaycare)
    insertTestDaycare(testVoucherDaycare2)

    insertTestDaycare(testClub)
    insertTestDaycare(testGhostUnitDaycare)
    insertTestDaycare(testRoundTheClockDaycare)

    unitSupervisorOfTestDaycare.let {
        insertTestEmployee(
            DevEmployee(
                id = it.id,
                firstName = it.firstName,
                lastName = it.lastName,
                externalId = unitSupervisorExternalId
            )
        )
        updateDaycareAcl(
            testDaycare.id,
            unitSupervisorExternalId,
            UserRole.UNIT_SUPERVISOR
        )
    }

    allAdults.forEach {
        insertTestPerson(
            DevPerson(
                id = it.id,
                dateOfBirth = it.dateOfBirth,
                ssn = it.ssn,
                firstName = it.firstName,
                lastName = it.lastName,
                streetAddress = it.streetAddress,
                postalCode = it.postalCode,
                postOffice = it.postOffice,
                email = it.email,
                restrictedDetailsEnabled = it.restrictedDetailsEnabled
            )
        )
        upsertCitizenUser(it.id)
    }

    allChildren.forEach {
        insertTestPerson(
            DevPerson(
                id = it.id,
                dateOfBirth = it.dateOfBirth,
                ssn = it.ssn,
                firstName = it.firstName,
                lastName = it.lastName,
                streetAddress = it.streetAddress,
                postalCode = it.postalCode,
                postOffice = it.postOffice
            )
        )
        insertTestChild(DevChild(id = it.id))
    }

    insertTestFeeThresholds(
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
            temporaryFeeSiblingPartDay = 800
        )
    )

    insertPreschoolTerms()
    insertClubTerms()

    insertServiceNeedOptions()
    insertServiceNeedOptionVoucherValues()
    insertAssistanceActionOptions()
    insertAssistanceBasisOptions()
}

val preschoolTerms = listOf(
    // 2020-2021
    PreschoolTerm(
        FiniteDateRange(LocalDate.of(2020, 8, 13), LocalDate.of(2021, 6, 4)),
        FiniteDateRange(LocalDate.of(2020, 8, 18), LocalDate.of(2021, 6, 4)),
        FiniteDateRange(LocalDate.of(2020, 8, 1), LocalDate.of(2021, 6, 4)),
        FiniteDateRange(LocalDate.of(2020, 1, 8), LocalDate.of(2020, 1, 20))
    ),
    // 2021-2022
    PreschoolTerm(
        FiniteDateRange(LocalDate.of(2021, 8, 11), LocalDate.of(2022, 6, 3)),
        FiniteDateRange(LocalDate.of(2021, 8, 13), LocalDate.of(2022, 6, 3)),
        FiniteDateRange(LocalDate.of(2021, 8, 1), LocalDate.of(2022, 6, 3)),
        FiniteDateRange(LocalDate.of(2021, 1, 8), LocalDate.of(2021, 1, 20))
    ),
    // 2022-2023
    PreschoolTerm(
        FiniteDateRange(LocalDate.of(2022, 8, 11), LocalDate.of(2023, 6, 2)),
        FiniteDateRange(LocalDate.of(2022, 8, 11), LocalDate.of(2023, 6, 2)),
        FiniteDateRange(LocalDate.of(2022, 8, 1), LocalDate.of(2023, 6, 2)),
        FiniteDateRange(LocalDate.of(2022, 1, 10), LocalDate.of(2022, 1, 21))
    ),
    // 2023-2024
    PreschoolTerm(
        FiniteDateRange(LocalDate.of(2023, 8, 11), LocalDate.of(2024, 6, 3)),
        FiniteDateRange(LocalDate.of(2023, 8, 13), LocalDate.of(2024, 6, 6)),
        FiniteDateRange(LocalDate.of(2023, 8, 1), LocalDate.of(2024, 6, 6)),
        FiniteDateRange(LocalDate.of(2023, 1, 8), LocalDate.of(2023, 1, 20))
    )
)

fun Database.Transaction.insertPreschoolTerms() {
    prepareBatch(
        """
INSERT INTO preschool_term (finnish_preschool, swedish_preschool, extended_term, application_period)
VALUES (:finnishPreschool, :swedishPreschool, :extendedTerm, :applicationPeriod)
"""
    ).let { batch ->
        preschoolTerms.forEach { term -> batch.bindKotlin(term).add() }
        batch.execute()
    }
}

fun Database.Transaction.insertClubTerms() {
    createUpdate(
        """
INSERT INTO club_term (term, application_period)
VALUES
    -- 2020-2021
    ('[2020-08-13, 2021-06-04]', '[2020-01-08, 2020-01-20]'),
    -- 2021-2022
    ('[2021-08-11, 2022-06-03]', '[2021-01-08, 2021-01-20]');
"""
    ).execute()
}

fun Database.Transaction.insertServiceNeedOptions() {
    val batch = prepareBatch(
        // language=sql
        """
INSERT INTO service_need_option (id, name_fi, name_sv, name_en, valid_placement_type, default_option, fee_coefficient, occupancy_coefficient, occupancy_coefficient_under_3y, daycare_hours_per_week, part_day, part_week, fee_description_fi, fee_description_sv, voucher_value_description_fi, voucher_value_description_sv, contract_days_per_month)
VALUES (:id, :nameFi, :nameSv, :nameEn, :validPlacementType, :defaultOption, :feeCoefficient, :occupancyCoefficient, :occupancyCoefficientUnder3y, :daycareHoursPerWeek, :partDay, :partWeek, :feeDescriptionFi, :feeDescriptionSv, :voucherValueDescriptionFi, :voucherValueDescriptionSv, :contractDaysPerMonth)
"""
    )
    serviceNeedTestFixtures.forEach { fixture ->
        batch.bindKotlin(fixture).add()
    }
    batch.execute()
}

fun Database.Transaction.insertServiceNeedOptionVoucherValues() {
    val batch = prepareBatch(
        """
INSERT INTO service_need_option_voucher_value (service_need_option_id, validity, base_value, coefficient, value, base_value_under_3y, coefficient_under_3y, value_under_3y)
VALUES (:serviceNeedOptionId, :validity, :baseValue, :coefficient, :value, :baseValueUnder3y, :coefficientUnder3y, :valueUnder3y)
"""
    )
    serviceNeedOptionVoucherValueTestFixtures.forEach { fixture ->
        batch.bindKotlin(fixture).add()
    }
    batch.execute()
}

fun Database.Transaction.insertAssistanceActionOptions() {
    // language=sql
    val sql = """
INSERT INTO assistance_action_option (value, name_fi, display_order) VALUES
    ('ASSISTANCE_SERVICE_CHILD', 'Avustamispalvelut yhdelle lapselle', 10),
    ('ASSISTANCE_SERVICE_UNIT', 'Avustamispalvelut yksikköön', 20),
    ('SMALLER_GROUP', 'Pedagogisesti vahvistettu ryhmä', 30),
    ('SPECIAL_GROUP', 'Erityisryhmä', 40),
    ('PERVASIVE_VEO_SUPPORT', 'Laaja-alaisen veon tuki', 50),
    ('RESOURCE_PERSON', 'Resurssihenkilö', 60),
    ('RATIO_DECREASE', 'Suhdeluvun väljennys', 70),
    ('PERIODICAL_VEO_SUPPORT', 'Jaksottainen veon tuki (2–6 kk)', 80);
"""

    createUpdate(sql).execute()
}

fun Database.Transaction.insertAssistanceBasisOptions() {
    // language=sql
    val sql = """
INSERT INTO assistance_basis_option (value, name_fi, description_fi, display_order) VALUES
    ('DEVELOPMENTAL_DISABILITY_1', 'Kehitysvamma 1', NULL, 15),
    ('DEVELOPMENTAL_DISABILITY_2', 'Kehitysvamma 2', 'Käytetään silloin, kun esiopetuksessa oleva lapsi on vaikeasti kehitysvammainen.', 20);
"""

    createUpdate(sql).execute()
}

fun Database.Transaction.insertApplication(
    guardian: DevPerson = testAdult_5,
    child: DevPerson = testChild_6,
    appliedType: PlacementType = PlacementType.PRESCHOOL_DAYCARE,
    urgent: Boolean = false,
    hasAdditionalInfo: Boolean = false,
    maxFeeAccepted: Boolean = false,
    preferredStartDate: LocalDate? = LocalDate.now().plusMonths(4),
    applicationId: ApplicationId = ApplicationId(UUID.randomUUID()),
    status: ApplicationStatus = ApplicationStatus.CREATED,
    guardianEmail: String = "abc@espoo.fi",
    serviceNeedOption: ServiceNeedOption? = null,
    transferApplication: Boolean = false
): ApplicationDetails {
    insertTestApplication(
        id = applicationId,
        sentDate = null,
        dueDate = null,
        status = status,
        guardianId = guardian.id,
        childId = child.id,
        transferApplication = transferApplication,
        type = appliedType.toApplicationType()
    )
    val application = ApplicationDetails(
        id = applicationId,
        type = appliedType.toApplicationType(),
        form = ApplicationForm(
            child = ChildDetails(
                person = PersonBasics(
                    firstName = child.firstName,
                    lastName = child.lastName,
                    socialSecurityNumber = child.ssn
                ),
                dateOfBirth = child.dateOfBirth,
                address = addressOf(child),
                futureAddress = null,
                nationality = "fi",
                language = "fi",
                allergies = if (hasAdditionalInfo) "allergies" else "",
                diet = if (hasAdditionalInfo) "diet" else "",
                assistanceNeeded = false,
                assistanceDescription = ""
            ),
            guardian = Guardian(
                person = PersonBasics(
                    firstName = guardian.firstName,
                    lastName = guardian.lastName,
                    socialSecurityNumber = guardian.ssn
                ),
                address = addressOf(guardian),
                futureAddress = null,
                phoneNumber = "0501234567",
                email = guardianEmail
            ),
            preferences = Preferences(
                preferredUnits = listOf(PreferredUnit(testDaycare.id, testDaycare.name)),
                preferredStartDate = preferredStartDate,
                serviceNeed = if (appliedType in listOf(
                        PlacementType.PRESCHOOL,
                        PlacementType.PREPARATORY
                    )
                ) null else ServiceNeed(
                    startTime = "09:00",
                    endTime = "15:00",
                    shiftCare = false,
                    partTime = appliedType == PlacementType.DAYCARE_PART_TIME,
                    serviceNeedOption = serviceNeedOption
                ),
                siblingBasis = null,
                preparatory = appliedType in listOf(PlacementType.PREPARATORY, PlacementType.PREPARATORY_DAYCARE),
                urgent = urgent
            ),
            secondGuardian = null,
            otherPartner = null,
            otherChildren = emptyList(),
            otherInfo = if (hasAdditionalInfo) "foobar" else "",
            maxFeeAccepted = maxFeeAccepted,
            clubDetails = null
        ),
        status = status,
        origin = ApplicationOrigin.PAPER,
        childId = child.id,
        childRestricted = false,
        guardianId = guardian.id,
        guardianRestricted = false,
        guardianDateOfDeath = null,
        createdDate = HelsinkiDateTime.now(),
        modifiedDate = HelsinkiDateTime.now(),
        sentDate = null,
        dueDate = null,
        dueDateSetManuallyAt = null,
        transferApplication = transferApplication,
        additionalDaycareApplication = false,
        otherGuardianId = null,
        otherGuardianLivesInSameAddress = null,
        checkedByAdmin = false,
        hideFromGuardian = false,
        attachments = listOf()
    )
    insertTestApplicationForm(
        applicationId = applicationId,
        document = DaycareFormV0.fromApplication2(application)
    )
    return application
}

private fun addressOf(person: DevPerson): Address =
    Address(street = person.streetAddress, postalCode = person.postalCode, postOffice = person.postOffice)

fun DevPerson.toPersonBasic() = PersonBasic(
    id = this.id,
    dateOfBirth = this.dateOfBirth,
    firstName = this.firstName,
    lastName = this.lastName,
    ssn = this.ssn
)

fun DevPerson.toPersonDetailed() = PersonDetailed(
    id = this.id,
    dateOfBirth = this.dateOfBirth,
    dateOfDeath = this.dateOfDeath,
    firstName = this.firstName,
    lastName = this.lastName,
    ssn = this.ssn,
    streetAddress = this.streetAddress,
    postalCode = this.postalCode,
    postOffice = this.postOffice,
    residenceCode = this.residenceCode,
    email = this.email,
    phone = this.phone,
    language = this.language,
    invoiceRecipientName = this.invoiceRecipientName,
    invoicingStreetAddress = this.invoicingStreetAddress,
    invoicingPostalCode = this.invoicingPostalCode,
    invoicingPostOffice = this.invoicingPostOffice,
    restrictedDetailsEnabled = this.restrictedDetailsEnabled,
    forceManualFeeDecisions = this.forceManualFeeDecisions
)

fun DevEmployee.toEmployeeWithName() = EmployeeWithName(
    id = this.id,
    firstName = this.firstName,
    lastName = this.lastName
)

fun DevDaycare.toUnitData() = UnitData(
    id = this.id,
    name = this.name,
    areaId = this.areaId,
    areaName = allAreas.find { it.id == this.areaId }?.name ?: "",
    language = this.language.name
)
