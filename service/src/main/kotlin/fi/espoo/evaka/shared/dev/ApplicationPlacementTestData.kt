// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.dev

import fi.espoo.evaka.application.Address
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
import fi.espoo.evaka.shared.ApplicationId
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.Coordinate
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

private val careArea = DevCareArea(name = "Espoonlahti")

private data class DC(
    val name: String,
    val capacity: Int,
    val location: Coordinate,
    val serviceWorkerNote: String = "",
)

private val daycares =
    listOf(
            DC("Aallonhuipun päiväkoti", 56, Coordinate(24.6390243, 60.1503933)),
            DC("Eestinkallion esiopetus", 42, Coordinate(24.684307, 60.165959)),
            DC("Eestinmalmin päiväkoti", 42, Coordinate(24.6767257, 60.1634534)),
            DC("Eestinmetsän päiväkoti", 77, Coordinate(24.6908129, 60.1648689)),
            DC("Espoonlahden päiväkoti", 63, Coordinate(24.6590006, 60.1477979)),
            DC("Iivisniemen koulun esiopetus", 42, Coordinate(24.7029182, 60.145936)),
            DC(
                "Iivisniemen päiväkoti",
                42,
                Coordinate(24.6961952, 60.1497311),
                serviceWorkerNote = "Väistössä, tilat eivät ole esteettömät",
            ),
            DC("Järvitorpan päiväkoti", 119, Coordinate(24.6728804, 60.1484406)),
            DC("Kaitaanniityn päiväkoti", 133, Coordinate(24.6853654, 60.1434305)),
            DC("Kantokasken esiopetus", 21, Coordinate(24.6631665, 60.1808246)),
            DC("Kaskipihan päiväkoti", 42, Coordinate(24.65432, 60.17709)),
            DC("Kastevuoren päiväkoti", 77, Coordinate(24.6687897, 60.1363471)),
            DC("Kipparin päiväkoti", 112, Coordinate(24.663467, 60.149125)),
            DC("Latokasken päiväkoti", 70, Coordinate(24.6530347, 60.1778875)),
            DC("Laurinlahden päiväkoti", 77, Coordinate(24.6413315, 60.1453537)),
            DC("Lehtikasken päiväkoti", 161, Coordinate(24.6679295, 60.1805008)),
            DC("Mainingin esiopetus", 35, Coordinate(24.6440145, 60.1530511)),
            DC("Mainingin päiväkoti", 91, Coordinate(24.6440145, 60.1530511)),
            DC("Martinkallion esiopetus", 42, Coordinate(24.6769319, 60.157151)),
            DC("Merenkulkijan päiväkoti", 91, Coordinate(24.653092, 60.148848)),
            DC("Nöykkiön esiopetus", 63, Coordinate(24.6640285, 60.166734)),
            DC("Nöykkiönniityn päiväkoti", 170, Coordinate(24.66271, 60.16862)),
            DC("Nöykkiön päiväkoti", 119, Coordinate(24.6640285, 60.166734)),
            DC("Ohrakasken päiväkoti", 77, Coordinate(24.6604036, 60.1742625)),
            DC("Paapuurin päiväkoti", 126, Coordinate(24.6224114, 60.1642583)),
            DC("Pisan päiväkoti", 98, Coordinate(24.67939, 60.17463)),
            DC("Saapasaukion päiväkoti", 35, Coordinate(24.6435525, 60.1473454)),
            DC("Saunalahden päiväkoti", 112, Coordinate(24.61568, 60.16439)),
            DC("Soukan koulun esiopetus", 42, Coordinate(24.6714335, 60.1387804)),
            DC("Soukankujan päiväkoti", 63, Coordinate(24.6714809, 60.1391643)),
            DC("Suomenojan päiväkoti", 91, Coordinate(24.7003783, 60.1635143)),
            DC("Tillinmäen päiväkoti", 112, Coordinate(24.6325148, 60.1673843)),
            DC("Tähystäjän päiväkoti", 147, Coordinate(24.6572, 60.15142)),
            DC("Yläkartanon päiväkoti", 105, Coordinate(24.6628174, 60.1410851)),
        )
        .map {
            DevDaycare(
                areaId = careArea.id,
                name = it.name,
                capacity = it.capacity,
                location = it.location,
                serviceWorkerNote = it.serviceWorkerNote,
            )
        }

private val guardian =
    DevPerson(
        firstName = "Testi",
        lastName = "Henkilo",
        dateOfBirth = LocalDate.now().minusYears(30),
        streetAddress = "Testikatu 1",
        postalCode = "00200",
        postOffice = "Espoo",
    )

private fun createTestChild(firstName: String, lastName: String, age: Double) =
    DevPerson(
        firstName = firstName,
        lastName = lastName,
        dateOfBirth = LocalDate.now().minusDays(Math.round(age * 365)),
        streetAddress = "Testikatu 1",
        postalCode = "00200",
        postOffice = "Espoo",
    )

private val children =
    listOf(
        createTestChild("Minna Matilda", "Möttönen", 1.2),
        createTestChild("Matti Teponpoika", "Turkulainen", 2.3),
        createTestChild("Sauli Jussi", "Ahtisaari", 2.7),
        createTestChild("Markku Väinö", "Niinistö", 3.3),
        createTestChild("Tarja Kaarina Helinä", "Mannerheim", 4.5),
    )

private val applications =
    listOf(
        createTestApplication(
            children[0],
            guardian,
            listOf(daycares[6], daycares[0], daycares[13]),
            assistanceNeed = "Käyttää pyörätuolia",
        ),
        createTestApplication(children[1], guardian, listOf(daycares[2])),
        createTestApplication(
            children[2],
            guardian,
            listOf(daycares[4], daycares[5]),
            transferApplication = true,
        ),
        createTestApplication(
            children[3],
            guardian,
            listOf(daycares[6], daycares[7], daycares[0]),
            partTime = true,
            startTime = "08:30",
            endTime = "12:45",
        ),
        createTestApplication(children[4], guardian, listOf(daycares[6], daycares[9])),
    )

private fun createTestApplication(
    child: DevPerson,
    guardian: DevPerson,
    preferredUnits: List<DevDaycare>,
    transferApplication: Boolean = false,
    assistanceNeed: String = "",
    otherInfo: String = "",
    startInDays: Long = 50,
    partTime: Boolean = false,
    startTime: String = "08:00",
    endTime: String = "16:00",
) =
    DevApplicationWithForm(
        id = ApplicationId(UUID.randomUUID()),
        type = ApplicationType.DAYCARE,
        createdAt = HelsinkiDateTime.now().minusDays(55),
        createdBy = guardian.evakaUserId(),
        modifiedAt = HelsinkiDateTime.now().minusDays(55),
        modifiedBy = guardian.evakaUserId(),
        sentDate = LocalDate.now().minusDays(55),
        dueDate = LocalDate.now().plusDays(18),
        status = ApplicationStatus.WAITING_PLACEMENT,
        guardianId = guardian.id,
        childId = child.id,
        origin = ApplicationOrigin.ELECTRONIC,
        checkedByAdmin = true,
        confidential = true,
        hideFromGuardian = false,
        transferApplication = transferApplication,
        otherGuardians = emptyList(),
        form =
            ApplicationForm(
                child =
                    ChildDetails(
                        person =
                            PersonBasics(
                                child.firstName,
                                child.lastName,
                                socialSecurityNumber = null,
                            ),
                        dateOfBirth = child.dateOfBirth,
                        address = Address("Testikatu 1", "00200", "Espoo"),
                        futureAddress = null,
                        nationality = "fi",
                        language = "fi",
                        allergies = "",
                        diet = "",
                        assistanceNeeded = assistanceNeed.isNotBlank(),
                        assistanceDescription = assistanceNeed,
                    ),
                guardian =
                    Guardian(
                        person =
                            PersonBasics(
                                guardian.firstName,
                                guardian.lastName,
                                socialSecurityNumber = null,
                            ),
                        address = Address("Testikatu 1", "00200", "Espoo"),
                        futureAddress = null,
                        phoneNumber = "+358 50 1234567",
                        email = "testitesti@gmail.com",
                    ),
                secondGuardian = null,
                otherPartner = null,
                otherChildren = emptyList(),
                preferences =
                    Preferences(
                        preferredUnits = preferredUnits.map { PreferredUnit(it.id, it.name) },
                        preferredStartDate = LocalDate.now().plusDays(startInDays),
                        connectedDaycarePreferredStartDate = null,
                        serviceNeed =
                            ServiceNeed(
                                startTime = startTime,
                                endTime = endTime,
                                shiftCare = false,
                                partTime = partTime,
                                serviceNeedOption = null,
                            ),
                        siblingBasis = null,
                        preparatory = false,
                        urgent = false,
                    ),
                maxFeeAccepted = true,
                otherInfo = otherInfo,
                clubDetails = null,
            ),
    )

fun insertApplicationPlacementTestData(tx: Database.Transaction) {
    tx.insert(careArea)
    daycares.forEach { daycare ->
        tx.insert(daycare)
        val group = DevDaycareGroup(daycareId = daycare.id, name = "Ryhmä 1")
        tx.insert(group)
        tx.insert(
            DevDaycareCaretaker(groupId = group.id, amount = BigDecimal(daycare.capacity / 7))
        )
    }

    tx.insert(guardian, DevPersonType.ADULT)
    children.forEach {
        tx.insert(it, DevPersonType.CHILD)
        tx.insert(DevGuardian(guardian.id, it.id))
    }
    applications.forEach { tx.insertApplication(it) }
}
