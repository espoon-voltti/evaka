// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.application.enduser.daycare

import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.eq
import fi.espoo.evaka.application.ApplicationDetails
import fi.espoo.evaka.application.ApplicationForm
import fi.espoo.evaka.application.ApplicationOrigin
import fi.espoo.evaka.application.ApplicationStatus
import fi.espoo.evaka.application.ApplicationType
import fi.espoo.evaka.application.ChildDetails
import fi.espoo.evaka.application.FutureAddress
import fi.espoo.evaka.application.Guardian
import fi.espoo.evaka.application.PersonBasics
import fi.espoo.evaka.application.Preferences
import fi.espoo.evaka.application.PreferredUnit
import fi.espoo.evaka.application.SecondGuardian
import fi.espoo.evaka.application.ServiceNeed
import fi.espoo.evaka.application.SiblingBasis
import fi.espoo.evaka.application.enduser.ApplicationSerializer
import fi.espoo.evaka.application.persistence.daycare.DaycareFormV0
import fi.espoo.evaka.identity.ExternalIdentifier
import fi.espoo.evaka.pis.service.PersonDTO
import fi.espoo.evaka.pis.service.PersonService
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.db.Database
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.lenient
import org.mockito.junit.jupiter.MockitoExtension
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

private val requestingUserId = UUID.randomUUID()
private val guardianId = UUID.randomUUID()
private val childId = UUID.randomUUID()

@ExtendWith(MockitoExtension::class)
class DaycareApplicationSerializerTest {
    @Mock
    lateinit var personService: PersonService

    @Mock
    lateinit var tx: Database.Transaction

    @InjectMocks
    lateinit var serializer: ApplicationSerializer

    @BeforeEach
    internal fun setUp() {
        mockPersonService(guardianId)
        mockPersonService(childId)
    }

    @Test
    fun `enduser deserialized daycare json matches expected`() {
        val user = AuthenticatedUser(requestingUserId, setOf(UserRole.END_USER))
        val daycareForm = serializer.deserialize(user, DAYCARE_JSON)
        assertTrue(daycareForm is DaycareFormV0)
        val expectedForm = DaycareFormV0.fromForm2(
            expectedEnduserDaycareApplication.form,
            ApplicationType.DAYCARE,
            expectedEnduserDaycareApplication.childRestricted,
            expectedEnduserDaycareApplication.guardianRestricted
        )
        assertEquals(expectedForm, daycareForm)
    }

    @Test
    fun `enduser serialized daycare json matches expected`() {
        val user = AuthenticatedUser(requestingUserId, setOf(UserRole.END_USER))
        val json = serializer.serialize(tx, user, expectedEnduserDaycareApplication)
        assertTrue(json.form is EnduserDaycareFormJSON)
        assertEquals(expectedEnduserDaycareFormJSON, json.form)
    }

    private fun mockPersonService(personId: UUID, restricted: Boolean = false) {
        lenient().`when`(personService.getUpToDatePerson(any(), any(), eq(personId))).thenReturn(
            PersonDTO(
                id = personId,
                identity = ExternalIdentifier.NoID(),
                customerId = 1L,
                firstName = "",
                lastName = "",
                email = "",
                phone = "",
                language = "fi",
                dateOfBirth = LocalDate.now(),
                restrictedDetailsEnabled = restricted
            )
        )
    }
}

private val expectedEnduserDaycareApplication = ApplicationDetails(
    id = UUID.randomUUID(),
    type = ApplicationType.DAYCARE,
    status = ApplicationStatus.WAITING_PLACEMENT,
    origin = ApplicationOrigin.ELECTRONIC,
    childId = childId,
    createdDate = OffsetDateTime.now(),
    dueDate = LocalDate.now().plusMonths(1),
    guardianId = guardianId,
    otherGuardianId = null,
    otherGuardianLivesInSameAddress = null,
    modifiedDate = OffsetDateTime.now(),
    sentDate = LocalDate.now(),
    childRestricted = true,
    guardianRestricted = true,
    form = ApplicationForm(
        child = ChildDetails(
            person = PersonBasics(
                firstName = "Jari-Petteri Mukkelis-Makkelis Vetelä-Viljami Eelis-Juhani",
                lastName = "Karhula",
                socialSecurityNumber = "070714A9126"
            ),
            dateOfBirth = LocalDate.of(2015, 11, 11),
            address = fi.espoo.evaka.application.Address(
                street = "Kamreerintie 1",
                postalCode = "02770",
                postOffice = "Espoo"
            ),
            futureAddress = FutureAddress(
                street = "Uusikatu 21",
                postalCode = "00150",
                postOffice = "Helsinki",
                movingDate = LocalDate.of(2019, 3, 6)
            ),
            nationality = "FI",
            language = "fi",
            allergies = "Allergiat ................",
            diet = "Erityisruokavalio .........................",
            assistanceNeeded = true,
            assistanceDescription = "Lapseni ei osaa kävellä vielä."
        ),
        guardian = Guardian(
            person = PersonBasics(
                firstName = "Johannes Olavi Antero Tapio",
                lastName = "Karhula",
                socialSecurityNumber = "070644-937X"
            ),
            email = "johannes.karhula@espoo.fi",
            phoneNumber = "0501234567",
            address = fi.espoo.evaka.application.Address(
                street = "Kamreerintie 1",
                postalCode = "02770",
                postOffice = "Espoo"
            ),
            futureAddress = FutureAddress(
                street = "Uusikatu 21",
                postalCode = "00150",
                postOffice = "Helsinki",
                movingDate = LocalDate.of(2019, 5, 9)
            )
        ),
        secondGuardian = SecondGuardian(
            email = "erihuoltaja@meikalainen.fi",
            phoneNumber = "0401111111",
            agreementStatus = null
        ),
        preferences = Preferences(
            preferredUnits = listOf(
                PreferredUnit(UUID.fromString("1287ec53-070e-4a45-af92-90806d0d92e7"), ""),
                PreferredUnit(UUID.fromString("cf6583c6-c9ca-49aa-a7ac-d404b984a41b"), ""),
                PreferredUnit(UUID.fromString("02bcace3-e427-4bc6-9c52-6f1ee97d7462"), "")
            ),
            preferredStartDate = LocalDate.of(2019, 3, 6),
            siblingBasis = SiblingBasis(
                siblingName = "Sisarusperuste Meikäläinen",
                siblingSsn = ""
            ),
            serviceNeed = ServiceNeed(
                startTime = "08:00",
                endTime = "14:00",
                shiftCare = true,
                partTime = false
            ),
            urgent = true,
            preparatory = false
        ),
        otherPartner = PersonBasics(
            firstName = "Muuhenkilo1",
            lastName = "Meikäläinen",
            socialSecurityNumber = "121283-111D"
        ),
        otherChildren = listOf(
            PersonBasics(
                firstName = "Muulapsi1",
                lastName = "Meikäläinen",
                socialSecurityNumber = "121212A123F"
            ),
            PersonBasics(
                firstName = "Muulapsi2",
                lastName = "Meikäläinen",
                socialSecurityNumber = "121212A123F"
            )
        ),
        maxFeeAccepted = false,
        otherInfo = "Lisätiedot ................",
        clubDetails = null
    ),
    checkedByAdmin = false,
    hideFromGuardian = false,
    transferApplication = false,
    additionalDaycareApplication = false,
    attachments = listOf()
)

private val expectedEnduserDaycareFormJSON = with(expectedEnduserDaycareApplication.form) {
    EnduserDaycareFormJSON(
        type = ApplicationType.DAYCARE,
        urgent = preferences.urgent,
        preferredStartDate = preferences.preferredStartDate,
        serviceStart = preferences.serviceNeed!!.startTime,
        serviceEnd = preferences.serviceNeed!!.endTime,
        extendedCare = preferences.serviceNeed!!.shiftCare,
        careDetails = EndUserCareDetailsJSON(
            preparatory = null,
            assistanceNeeded = child.assistanceNeeded,
            assistanceDescription = child.assistanceDescription
        ),
        apply = ApplyJSON(
            preferredUnits = preferences.preferredUnits.map { it.id },
            siblingBasis = preferences.siblingBasis != null,
            siblingName = preferences.siblingBasis?.siblingName ?: ""
        ),
        child = with(child) {
            ChildJSON(
                firstName = person.firstName,
                lastName = person.lastName,
                socialSecurityNumber = person.socialSecurityNumber ?: "",
                dateOfBirth = dateOfBirth,
                address = AddressJSON(
                    street = address?.street ?: "",
                    postalCode = address?.postalCode ?: "",
                    city = address?.postOffice ?: "",
                    editable = false
                ),
                nationality = nationality,
                language = language,
                hasCorrectingAddress = futureAddress != null,
                correctingAddress = AddressJSON(
                    street = futureAddress?.street ?: "",
                    postalCode = futureAddress?.postalCode ?: "",
                    city = futureAddress?.postOffice ?: "",
                    editable = true
                ),
                childMovingDate = futureAddress?.movingDate,
                restricted = expectedEnduserDaycareApplication.childRestricted
            )
        },
        guardian = with(guardian) {
            AdultJSON(
                firstName = person.firstName,
                lastName = person.lastName,
                socialSecurityNumber = person.socialSecurityNumber ?: "",
                address = AddressJSON(
                    street = address?.street ?: "",
                    postalCode = address?.postalCode ?: "",
                    city = address?.postOffice ?: "",
                    editable = false
                ),
                phoneNumber = phoneNumber,
                email = email,
                hasCorrectingAddress = futureAddress != null,
                correctingAddress = AddressJSON(
                    street = futureAddress?.street ?: "",
                    postalCode = futureAddress?.postalCode ?: "",
                    city = futureAddress?.postOffice ?: "",
                    editable = true
                ),
                guardianMovingDate = futureAddress?.movingDate,
                restricted = expectedEnduserDaycareApplication.guardianRestricted
            )
        },
        guardian2 = AdultJSON(
            email = secondGuardian?.email,
            phoneNumber = secondGuardian?.phoneNumber
        ),
        hasOtherAdults = otherPartner != null,
        otherAdults = otherPartner?.let {
            listOf(
                OtherPersonJSON(
                    firstName = it.firstName,
                    lastName = it.lastName,
                    socialSecurityNumber = it.socialSecurityNumber ?: ""
                )
            )
        } ?: emptyList(),
        hasOtherChildren = otherChildren.isNotEmpty(),
        otherChildren = otherChildren.map {
            OtherPersonJSON(
                firstName = it.firstName,
                lastName = it.lastName,
                socialSecurityNumber = it.socialSecurityNumber ?: ""
            )
        },
        additionalDetails = DaycareAdditionalDetailsJSON(
            allergyType = child.allergies,
            dietType = child.diet,
            otherInfo = otherInfo
        ),
        docVersion = 0
    )
}

// language=json
private val DAYCARE_JSON =
    """
{
  "id": "",
  "status": "CREATED",
  "origin": "ELECTRONIC",
  "form": {
    "type": "DAYCARE",
    "daycare": false,
    "preschool": true,
    "preferredStartDate": "2019-03-06",
    "extendedCare": true,
    "urgent": true,
    "serviceStart": "08:00",
    "serviceEnd": "14:00",
    "careDetails": {
      "assistanceNeeded": true,
      "assistanceDescription": "Lapseni ei osaa kävellä vielä."
    },
    "apply": {
      "siblingBasis": true,
      "siblingName": "Sisarusperuste Meikäläinen",
      "preferredUnits": [
        "1287ec53-070e-4a45-af92-90806d0d92e7",
        "cf6583c6-c9ca-49aa-a7ac-d404b984a41b",
        "02bcace3-e427-4bc6-9c52-6f1ee97d7462"
      ]
    },
    "child": {
      "firstName": "Jari-Petteri Mukkelis-Makkelis Vetelä-Viljami Eelis-Juhani",
      "lastName": "Karhula",
      "dateOfBirth": "2015-11-11",
      "socialSecurityNumber": "070714A9126",
      "nationality": "FI",
      "language": "fi",
      "childMovingDate": "2019-03-06",
      "address": {
        "street": "Kamreerintie 1",
        "postalCode": "02770",
        "city": "Espoo",
        "editable": false
      },
      "hasCorrectingAddress": true,
      "correctingAddress": {
        "street": "Uusikatu 21",
        "postalCode": "00150",
        "city": "Helsinki",
        "editable": true
      },
      "restricted": true
    },
    "guardian": {
      "firstName": "Johannes Olavi Antero Tapio",
      "lastName": "Karhula",
      "socialSecurityNumber": "070644-937X",
      "email": "johannes.karhula@espoo.fi",
      "phoneNumber": "0501234567",
      "address": {
        "street": "Kamreerintie 1",
        "postalCode": "02770",
        "city": "Espoo",
        "editable": false
      },
      "guardianMovingDate": "2019-05-09",
      "hasCorrectingAddress": true,
      "correctingAddress": {
        "street": "Uusikatu 21",
        "postalCode": "00150",
        "city": "Helsinki",
        "editable": true
      },
      "workStatus": "unemployed",
      "workAddress": {
        "street": "",
        "postalCode": "",
        "city": "",
        "editable": true
      },
      "restricted": true
    },
    "guardian2": {
      "email": "erihuoltaja@meikalainen.fi",
      "phoneNumber": "0401111111"
    },
    "additionalDetails": {
      "allergyType": "Allergiat ................",
      "dietType": "Erityisruokavalio .........................",
      "otherInfo": "Lisätiedot ................"
    },
    "hasOtherAdults": true,
    "otherAdults": [
      {
        "firstName": "Muuhenkilo1",
        "lastName": "Meikäläinen",
        "socialSecurityNumber": "121283-111D"
      }
    ],
    "hasOtherChildren": true,
    "otherChildren": [
      {
        "firstName": "Muulapsi1",
        "lastName": "Meikäläinen",
        "socialSecurityNumber": "121212A123F"
      },
      {
        "firstName": "Muulapsi2",
        "lastName": "Meikäläinen",
        "socialSecurityNumber": "121212A123F"
      }
    ],
    "docVersion": 0
  },
  "checkedByAdmin": false,
  "dueDate": "2019-03-06",
  "sentDate": "2019-03-06",
  "createdDate": "2019-03-06T15:41:14.461788+02:00",
  "modifiedDate": "2019-03-07T14:19:06.077848+02:00",
  "placement": null,
  "childId": "$childId",
  "guardianId": "$guardianId"
}
    """.trimIndent()
