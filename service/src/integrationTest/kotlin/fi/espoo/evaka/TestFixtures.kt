// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka

import fi.espoo.evaka.application.Address
import fi.espoo.evaka.application.ApplicationDetails
import fi.espoo.evaka.application.ApplicationForm
import fi.espoo.evaka.application.ApplicationOrigin
import fi.espoo.evaka.application.ApplicationStatus
import fi.espoo.evaka.application.ApplicationType
import fi.espoo.evaka.application.ChildDetails
import fi.espoo.evaka.application.Guardian
import fi.espoo.evaka.application.PersonBasics
import fi.espoo.evaka.application.Preferences
import fi.espoo.evaka.application.PreferredUnit
import fi.espoo.evaka.application.ServiceNeed
import fi.espoo.evaka.application.persistence.daycare.DaycareFormV0
import fi.espoo.evaka.daycare.CareType
import fi.espoo.evaka.daycare.domain.ProviderType
import fi.espoo.evaka.invoicing.domain.PersonData
import fi.espoo.evaka.invoicing.domain.UnitData
import fi.espoo.evaka.invoicing.domain.VoucherValue
import fi.espoo.evaka.placement.PlacementType
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.db.transaction
import fi.espoo.evaka.shared.dev.DevCareArea
import fi.espoo.evaka.shared.dev.DevChild
import fi.espoo.evaka.shared.dev.DevDaycare
import fi.espoo.evaka.shared.dev.DevEmployee
import fi.espoo.evaka.shared.dev.DevPerson
import fi.espoo.evaka.shared.dev.DevPricing
import fi.espoo.evaka.shared.dev.insertTestApplication
import fi.espoo.evaka.shared.dev.insertTestApplicationForm
import fi.espoo.evaka.shared.dev.insertTestCareArea
import fi.espoo.evaka.shared.dev.insertTestChild
import fi.espoo.evaka.shared.dev.insertTestDaycare
import fi.espoo.evaka.shared.dev.insertTestEmployee
import fi.espoo.evaka.shared.dev.insertTestPerson
import fi.espoo.evaka.shared.dev.insertTestPricing
import fi.espoo.evaka.shared.dev.insertTestVoucherValue
import fi.espoo.evaka.shared.domain.Period
import org.jdbi.v3.core.Handle
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

/*
 Queries and data classes for initializing integration tests with person and unit data
 */

val testAreaId = UUID.randomUUID()
val testAreaCode = 200
val testArea2Id = UUID.randomUUID()
val testArea2Code = 300
val svebiTestId = UUID.randomUUID()
val svebiTestCode = 400

val defaultMunicipalOrganizerOid = "1.2.246.562.10.888888888888"
val defaultPurchasedOrganizerOid = "1.2.246.562.10.66666666666"

val testDaycare =
    UnitData.Detailed(
        id = UUID.randomUUID(),
        name = "Test Daycare",
        areaId = testAreaId,
        areaName = "Test Area",
        language = "fi"
    )
val testDaycare2 =
    UnitData.Detailed(
        id = UUID.randomUUID(),
        name = "Test Daycare 2",
        areaId = testArea2Id,
        areaName = "Lwiz Foo",
        language = "fi"
    )
val testDaycareNotInvoiced = UnitData.InvoicedByMunicipality(id = UUID.randomUUID(), invoicedByMunicipality = false)
val testSvebiDaycare =
    UnitData.Detailed(
        id = UUID.randomUUID(),
        name = "Test Svebi Daycare",
        areaId = svebiTestId,
        areaName = "Svenska Bildningstjanster",
        language = "sv"
    )

val testPurchasedDaycare =
    UnitData.Detailed(
        id = UUID.randomUUID(),
        name = "Test Purchased Daycare",
        areaId = testAreaId,
        areaName = "Lwiz Foo",
        language = "fi"
    )

val testVoucherDaycare =
    UnitData.Detailed(
        id = UUID.randomUUID(),
        name = "Test Voucher Daycare",
        areaId = testAreaId,
        areaName = "Lwiz Foo",
        language = "fi"
    )

val testClub = DevDaycare(
    id = UUID.randomUUID(),
    name = "Test Club",
    areaId = testAreaId,
    type = setOf(CareType.CLUB),
    canApplyClub = true,
    uploadToVarda = false,
    uploadToKoski = false
)

val testDecisionMaker_1 = PersonData.WithName(
    id = UUID.randomUUID(),
    firstName = "Decision",
    lastName = "Maker"
)

val testDecisionMaker_2 = PersonData.WithName(
    id = UUID.randomUUID(),
    firstName = "Decision",
    lastName = "Maker"
)

val testAdult_1 = PersonData.Detailed(
    id = UUID.randomUUID(),
    dateOfBirth = LocalDate.of(1980, 1, 1),
    ssn = "010180-1232",
    firstName = "John",
    lastName = "Doe",
    streetAddress = "Kamreerintie 2",
    postalCode = "02770",
    postOffice = "Espoo",
    restrictedDetailsEnabled = false
)

val testAdult_2 = PersonData.Detailed(
    id = UUID.randomUUID(),
    dateOfBirth = LocalDate.of(1979, 2, 1),
    ssn = "010279-123L",
    firstName = "Joan",
    lastName = "Doe",
    streetAddress = "Kamreerintie 2",
    postalCode = "02770",
    postOffice = "Espoo",
    restrictedDetailsEnabled = false
)

val testAdult_3 = PersonData.Detailed(
    id = UUID.randomUUID(),
    dateOfBirth = LocalDate.of(1985, 6, 7),
    ssn = null,
    firstName = "Mark",
    lastName = "Foo",
    streetAddress = "",
    postalCode = "",
    postOffice = "",
    restrictedDetailsEnabled = false
)

val testAdult_4 = PersonData.Detailed(
    id = UUID.randomUUID(),
    dateOfBirth = LocalDate.of(1981, 3, 2),
    ssn = null,
    firstName = "Dork",
    lastName = "Aman",
    streetAddress = "Muutie 66",
    postalCode = "02230",
    postOffice = "Espoo",
    restrictedDetailsEnabled = false
)

// Matches VTJ mock person Johannes Karhula
val testAdult_5 = PersonData.Detailed(
    id = UUID.randomUUID(),
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
val testAdult_6 = PersonData.Detailed(
    id = UUID.randomUUID(),
    dateOfBirth = LocalDate.of(1999, 12, 31),
    ssn = "311299-999E",
    firstName = "Ville",
    lastName = "Vilkas",
    streetAddress = "Toistie 33",
    postalCode = "02230",
    postOffice = "Espoo",
    restrictedDetailsEnabled = false
)

val testChild_1 = PersonData.Detailed(
    id = UUID.randomUUID(),
    dateOfBirth = LocalDate.of(2017, 6, 1),
    ssn = "010617A123U",
    firstName = "Ricky",
    lastName = "Doe",
    streetAddress = "Kamreerintie 2",
    postalCode = "02770",
    postOffice = "Espoo",
    restrictedDetailsEnabled = false
)

val testChild_2 = PersonData.Detailed(
    id = UUID.randomUUID(),
    dateOfBirth = LocalDate.of(2016, 3, 1),
    ssn = "010316A1235",
    firstName = "Micky",
    lastName = "Doe",
    streetAddress = "Kamreerintie 2",
    postalCode = "02770",
    postOffice = "Espoo",
    restrictedDetailsEnabled = false
)

val testChild_3 = PersonData.Detailed(
    id = UUID.randomUUID(),
    dateOfBirth = LocalDate.of(2018, 9, 1),
    ssn = "120220A995L",
    firstName = "Hillary",
    lastName = "Foo",
    streetAddress = "",
    postalCode = "",
    postOffice = "",
    restrictedDetailsEnabled = false
)

val testChild_4 = PersonData.Detailed(
    id = UUID.randomUUID(),
    dateOfBirth = LocalDate.of(2019, 3, 2),
    ssn = "020319A990J",
    firstName = "Maisa",
    lastName = "Farang",
    streetAddress = "Mannerheimintie 1",
    postalCode = "00100",
    postOffice = "Helsinki",
    restrictedDetailsEnabled = false
)

val testChild_5 = PersonData.Detailed(
    id = UUID.randomUUID(),
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
val testChild_6 = PersonData.Detailed(
    id = UUID.randomUUID(),
    dateOfBirth = LocalDate.of(2018, 11, 13),
    ssn = "070714A9126",
    firstName = "Jari-Petteri Mukkelis-Makkelis Vetelä-Viljami Eelis-Juhani",
    lastName = "Karhula",
    streetAddress = "Kamreerintie 1",
    postalCode = "00340",
    postOffice = "Espoo",
    restrictedDetailsEnabled = false
)

val testChild_7 = PersonData.Detailed(
    id = UUID.randomUUID(),
    dateOfBirth = LocalDate.of(2018, 7, 28),
    ssn = null,
    firstName = "Heikki",
    lastName = "Hetuton",
    streetAddress = "Matinkatu 11",
    postalCode = "02230",
    postOffice = "Espoo",
    restrictedDetailsEnabled = false
)

val testChildWithNamelessGuardian = PersonData.Detailed(
    id = UUID.randomUUID(),
    dateOfBirth = LocalDate.of(2018, 12, 31),
    ssn = "311218A999J",
    firstName = "Niilo",
    lastName = "Nimettömänpoika",
    streetAddress = "Kankkulankaivo 1",
    postalCode = "00340",
    postOffice = "Espoo",
    restrictedDetailsEnabled = false
)

val allWorkers = setOf(testDecisionMaker_1)
val allAdults = setOf(testAdult_1, testAdult_2, testAdult_3, testAdult_4, testAdult_5, testAdult_6)
val allChildren = setOf(testChild_1, testChild_2, testChild_3, testChild_4, testChild_5, testChild_6, testChild_7, testChildWithNamelessGuardian)
val allBasicChildren = allChildren.map { PersonData.Basic(it.id, it.dateOfBirth, it.firstName, it.lastName, it.ssn) }
val allDaycares = setOf(testDaycare, testDaycare2)

fun insertGeneralTestFixtures(h: Handle) {
    insertTestVardaOrganizer(h)
    h.insertTestCareArea(DevCareArea(id = testAreaId, name = testDaycare.areaName, areaCode = testAreaCode))
    h.insertTestCareArea(
        DevCareArea(
            id = testArea2Id,
            name = testDaycare2.areaName,
            shortName = "short name 2",
            areaCode = testArea2Code
        )
    )
    h.insertTestDaycare(DevDaycare(areaId = testAreaId, id = testDaycare.id, name = testDaycare.name, ophOrganizerOid = defaultMunicipalOrganizerOid))
    h.insertTestDaycare(DevDaycare(areaId = testArea2Id, id = testDaycare2.id, name = testDaycare2.name))
    h.insertTestDaycare(
        DevDaycare(
            areaId = testArea2Id,
            id = testDaycareNotInvoiced.id,
            name = "Not Invoiced",
            invoicedByMunicipality = testDaycareNotInvoiced.invoicedByMunicipality
        )
    )
    h.insertTestDaycare(
        DevDaycare(
            areaId = testAreaId,
            id = testPurchasedDaycare.id,
            name = testPurchasedDaycare.name,
            providerType = ProviderType.PURCHASED,
            ophOrganizerOid = defaultPurchasedOrganizerOid,
            invoicedByMunicipality = false
        )
    )
    h.insertTestDaycare(
        DevDaycare(
            areaId = testVoucherDaycare.areaId,
            id = testVoucherDaycare.id,
            name = testVoucherDaycare.name,
            providerType = ProviderType.PRIVATE_SERVICE_VOUCHER,
            invoicedByMunicipality = false
        )
    )
    h.insertTestDaycare(testClub)

    testDecisionMaker_1.let {
        h.insertTestEmployee(
            DevEmployee(
                id = it.id,
                firstName = it.firstName,
                lastName = it.lastName
            )
        )
    }

    testDecisionMaker_2.let {
        h.insertTestEmployee(
            DevEmployee(
                id = it.id,
                firstName = it.firstName,
                lastName = it.lastName
            )
        )
    }

    allAdults.forEach {
        h.insertTestPerson(
            DevPerson(
                id = it.id,
                dateOfBirth = it.dateOfBirth,
                ssn = it.ssn,
                firstName = it.firstName,
                lastName = it.lastName,
                streetAddress = it.streetAddress ?: "",
                postalCode = it.postalCode ?: "",
                postOffice = it.postOffice ?: "",
                email = it.email
            )
        )
    }

    allChildren.forEach {
        h.insertTestPerson(
            DevPerson(
                id = it.id,
                dateOfBirth = it.dateOfBirth,
                ssn = it.ssn,
                firstName = it.firstName,
                lastName = it.lastName,
                streetAddress = it.streetAddress ?: "",
                postalCode = it.postalCode ?: "",
                postOffice = it.postOffice ?: ""
            )
        )
        h.insertTestChild(DevChild(id = it.id))
    }

    h.insertTestPricing(
        DevPricing(
            id = UUID.randomUUID(),
            validFrom = LocalDate.of(2000, 1, 1),
            validTo = null,
            multiplier = 0.107,
            maxThresholdDifference = 269700,
            minThreshold2 = 210200,
            minThreshold3 = 271300,
            minThreshold4 = 308000,
            minThreshold5 = 344700,
            minThreshold6 = 381300,
            thresholdIncrease6Plus = 14200
        )
    )

    h.insertTestVoucherValue(
        VoucherValue(id = UUID.randomUUID(), validity = Period(LocalDate.of(2000, 1, 1), null), voucherValue = 87000)
    )
}

fun Database.Transaction.resetDatabase() = execute("SELECT reset_database()")
fun resetDatabase(h: Handle) {
    h.transaction { it.execute("SELECT reset_database()") }
}

fun insertTestVardaOrganizer(h: Handle) {
    //language=SQL
    val sql =
        """
            INSERT INTO varda_organizer (organizer, email, phone, iban, varda_organizer_id, varda_organizer_oid, url, municipality_code)
            VALUES (:organizer, :email, :phone, :iban, :varda_organizer_id, :varda_organizer_oid, :url, :municipality_code)
        """.trimIndent()

    h.createUpdate(sql)
        .bindMap(
            mapOf(
                "organizer" to "Espoo",
                "email" to "test@espoo.fi",
                "phone" to "+358000000000",
                "iban" to "FI123456677555",
                "varda_organizer_id" to 1233,
                "varda_organizer_oid" to defaultMunicipalOrganizerOid,
                "url" to "http://path.to.organizer",
                "municipality_code" to "049"
            )
        ).execute()
}

fun insertApplication(
    h: Handle,
    guardian: PersonData.Detailed = testAdult_5,
    child: PersonData.Detailed = testChild_6,
    appliedType: PlacementType = PlacementType.PRESCHOOL_DAYCARE,
    urgent: Boolean = false,
    hasAdditionalInfo: Boolean = false,
    maxFeeAccepted: Boolean = false,
    preferredStartDate: LocalDate? = LocalDate.now().plusMonths(4),
    applicationId: UUID = UUID.randomUUID(),
    status: ApplicationStatus = ApplicationStatus.CREATED
): ApplicationDetails {
    insertTestApplication(
        h = h,
        id = applicationId,
        sentDate = null,
        dueDate = null,
        status = status,
        guardianId = guardian.id,
        childId = child.id
    )
    val application = ApplicationDetails(
        id = applicationId,
        type = when (appliedType) {
            PlacementType.PRESCHOOL, PlacementType.PRESCHOOL_DAYCARE, PlacementType.PREPARATORY, PlacementType.PREPARATORY_DAYCARE -> ApplicationType.PRESCHOOL
            PlacementType.DAYCARE, PlacementType.DAYCARE_PART_TIME -> ApplicationType.DAYCARE
            PlacementType.CLUB -> ApplicationType.CLUB
        },
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
                email = "abc@espoo.fi"
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
                    partTime = appliedType == PlacementType.DAYCARE_PART_TIME
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
        createdDate = OffsetDateTime.now(),
        modifiedDate = OffsetDateTime.now(),
        sentDate = null,
        dueDate = null,
        transferApplication = false,
        additionalDaycareApplication = false,
        otherGuardianId = null,
        checkedByAdmin = false,
        hideFromGuardian = false,
        attachments = listOf()
    )
    insertTestApplicationForm(
        h = h,
        applicationId = applicationId,
        document = DaycareFormV0.fromApplication2(application)
    )
    return application
}

private fun addressOf(person: PersonData.Detailed): Address =
    Address(street = person.streetAddress!!, postalCode = person.postalCode!!, postOffice = person.postOffice!!)
