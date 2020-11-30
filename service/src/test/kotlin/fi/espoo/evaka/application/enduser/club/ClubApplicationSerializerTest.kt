// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.application.enduser.club

import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.eq
import fi.espoo.evaka.application.Address
import fi.espoo.evaka.application.ApplicationDetails
import fi.espoo.evaka.application.ApplicationForm
import fi.espoo.evaka.application.ApplicationOrigin
import fi.espoo.evaka.application.ApplicationStatus
import fi.espoo.evaka.application.ApplicationType
import fi.espoo.evaka.application.ChildDetails
import fi.espoo.evaka.application.ClubDetails
import fi.espoo.evaka.application.FutureAddress
import fi.espoo.evaka.application.Guardian
import fi.espoo.evaka.application.PersonBasics
import fi.espoo.evaka.application.Preferences
import fi.espoo.evaka.application.PreferredUnit
import fi.espoo.evaka.application.SiblingBasis
import fi.espoo.evaka.application.enduser.ApplicationSerializer
import fi.espoo.evaka.application.persistence.club.ClubFormV0
import fi.espoo.evaka.identity.ExternalIdentifier
import fi.espoo.evaka.pis.service.PersonDTO
import fi.espoo.evaka.pis.service.PersonService
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.db.Database
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.lenient
import org.mockito.internal.matchers.apachecommons.ReflectionEquals
import org.mockito.junit.jupiter.MockitoExtension
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

private val requestingUserId = UUID.randomUUID()
private val guardianId = UUID.randomUUID()
private val childId = UUID.randomUUID()

@ExtendWith(MockitoExtension::class)
class ClubApplicationSerializerTest {
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
    fun toClubFormFromEnduserJSON() {
        val user = AuthenticatedUser(requestingUserId, setOf(UserRole.END_USER))
        val clubForm = serializer.deserialize(user, ENDUSER_CLUBFORM_JSON)
        assertTrue(clubForm is ClubFormV0)
    }

    @Test
    fun toEnduserJSONFromClubApplication() {
        val user = AuthenticatedUser(requestingUserId, setOf(UserRole.END_USER))
        val clubJSON = serializer.serialize(tx, user, mockEnduserClubApplication())
        assertTrue(clubJSON.form is EnduserClubFormJSON)
    }

    @Test
    fun `enduser deserialized json matches expected`() {
        val user = AuthenticatedUser(requestingUserId, setOf(UserRole.END_USER))
        val form = (serializer.deserialize(user, ENDUSER_CLUBFORM_JSON) as ClubFormV0)
        val expectedApplication = mockEnduserClubApplication()
        val expectedForm = ClubFormV0.fromForm2(expectedApplication.form, expectedApplication.childRestricted, expectedApplication.guardianRestricted)
        assertTrue(ReflectionEquals(expectedForm).matches(form))
    }

    @Test
    fun `enduser serialized json matches expected`() {
        val user = AuthenticatedUser(requestingUserId, setOf(UserRole.END_USER))
        val expectedApplication = mockEnduserClubApplication()
        val applicationJson = serializer.serialize(tx, user, expectedApplication)
        assertTrue(ReflectionEquals(mockEnduserClubFormJson(expectedApplication)).matches(applicationJson.form))
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

    private fun mockEnduserClubApplication(): ApplicationDetails {
        return ApplicationDetails(
            id = UUID.randomUUID(),
            status = ApplicationStatus.WAITING_PLACEMENT,
            origin = ApplicationOrigin.ELECTRONIC,
            childId = childId,
            createdDate = OffsetDateTime.now(),
            dueDate = LocalDate.now().plusMonths(1),
            guardianId = guardianId,
            otherGuardianId = null,
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
                    address = Address(
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
                    assistanceNeeded = true,
                    assistanceDescription = "Lapseni ei osaa kävellä vielä.",
                    allergies = "",
                    diet = ""
                ),
                guardian = Guardian(
                    person = PersonBasics(
                        firstName = "Johannes Olavi Antero Tapio",
                        lastName = "Karhula",
                        socialSecurityNumber = "070644-937X"
                    ),
                    email = "johannes.karhula@espoo.fi",
                    phoneNumber = "0501234567",
                    address = Address(
                        street = "Kamreerintie 1",
                        postalCode = "02770",
                        postOffice = "Espoo"
                    ),
                    futureAddress = FutureAddress(
                        street = "Uusikatu 21",
                        postalCode = "00150",
                        postOffice = "Helsinki",
                        movingDate = LocalDate.of(2019, 3, 6)
                    )
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
                    serviceNeed = null,
                    urgent = false,
                    preparatory = false
                ),
                clubDetails = ClubDetails(
                    wasOnDaycare = true,
                    wasOnClubCare = true
                ),
                secondGuardian = null,
                otherPartner = null,
                otherChildren = emptyList(),
                otherInfo = "Lisätiedot ................",
                maxFeeAccepted = false
            ),
            checkedByAdmin = false,
            hideFromGuardian = false,
            transferApplication = false,
            additionalDaycareApplication = false,
            type = ApplicationType.CLUB,
            attachments = listOf()
        )
    }

    fun mockEnduserClubFormJson(expectedApplication: ApplicationDetails): EnduserClubFormJSON {
        with(expectedApplication.form) {
            return EnduserClubFormJSON(
                preferredStartDate = preferences.preferredStartDate,
                wasOnDaycare = clubDetails!!.wasOnDaycare,
                wasOnClubCare = clubDetails!!.wasOnClubCare,
                careDetails = EndUserClubCare(
                    assistanceNeeded = child.assistanceNeeded,
                    assistanceDescription = child.assistanceDescription
                ),
                apply = ApplyJSON(
                    preferredUnits = preferences.preferredUnits.map { it.id },
                    siblingBasis = preferences.siblingBasis != null,
                    siblingName = preferences.siblingBasis?.siblingName ?: ""
                ),
                child = ChildJSON(
                    firstName = child.person.firstName,
                    lastName = child.person.lastName,
                    socialSecurityNumber = child.person.socialSecurityNumber ?: "",
                    dateOfBirth = child.dateOfBirth,
                    address = AddressJSON(
                        street = child.address?.street ?: "",
                        postalCode = child.address?.postalCode ?: "",
                        city = child.address?.postOffice ?: "",
                        editable = false
                    ),
                    nationality = child.nationality,
                    language = child.language,
                    hasCorrectingAddress = child.futureAddress != null,
                    correctingAddress = AddressJSON(
                        street = child.futureAddress?.street ?: "",
                        postalCode = child.futureAddress?.postalCode ?: "",
                        city = child.futureAddress?.postOffice ?: "",
                        editable = true
                    ),
                    childMovingDate = child.futureAddress?.movingDate,
                    restricted = expectedApplication.childRestricted
                ),
                guardian = AdultJSON(
                    firstName = guardian.person.firstName,
                    lastName = guardian.person.lastName,
                    socialSecurityNumber = guardian.person.socialSecurityNumber ?: "",
                    address = AddressJSON(
                        street = guardian.address?.street ?: "",
                        postalCode = guardian.address?.postalCode ?: "",
                        city = guardian.address?.postOffice ?: "",
                        editable = false
                    ),
                    phoneNumber = guardian.phoneNumber,
                    email = guardian.email,
                    hasCorrectingAddress = guardian.futureAddress != null,
                    correctingAddress = AddressJSON(
                        street = guardian.futureAddress?.street ?: "",
                        postalCode = guardian.futureAddress?.postalCode ?: "",
                        city = guardian.futureAddress?.postOffice ?: "",
                        editable = true
                    ),
                    guardianMovingDate = guardian.futureAddress?.movingDate,
                    restricted = expectedApplication.guardianRestricted
                ),
                additionalDetails = ClubAdditionalDetailsJSON(
                    otherInfo = otherInfo
                ),
                docVersion = 0
            )
        }
    }
}

// language=json
private val ENDUSER_CLUBFORM_JSON =
    """
{
  "id": "bf466d4e-4004-11e9-9ed0-8384ff79b22a",
  "status": "CREATED",
  "origin": "ELECTRONIC",
  "form": {
    "type": "club",
    "preferredStartDate": "2019-03-06",
    "term": "b727d450-9368-45a8-ad17-24e7f63f99fc",
    "wasOnDaycare": true,
    "wasOnClubCare": true,
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
      "guardianMovingDate": "2019-03-06",
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
    "hasSecondGuardian": true,
    "guardiansSeparated": true,
    "guardian2": {
      "firstName": "Erihuoltaja",
      "lastName": "Meikäläinen",
      "socialSecurityNumber": "121283-111D",
      "email": "erihuoltaja@meikalainen.fi",
      "phoneNumber": "0401111111",
      "address": {
        "street": "Toinenkatu 1",
        "postalCode": "02760",
        "city": "Espoo",
        "editable": false
      },
      "guardianMovingDate": "2019-03-06",
      "hasCorrectingAddress": true,
      "correctingAddress": {
        "street": "Rantatie 212",
        "postalCode": "02740",
        "city": "Espoo",
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
    "additionalDetails": {
      "otherInfo": "Lisätiedot ................"
    },
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
