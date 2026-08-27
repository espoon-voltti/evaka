// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.pis.controller

import evaka.core.FullApplicationTest
import evaka.core.emailclient.MockEmailClient
import evaka.core.pis.EmailMessageType
import evaka.core.pis.PersonalDataUpdate
import evaka.core.pis.controllers.PersonalDataControllerCitizen
import evaka.core.pis.getCitizenUserDetails
import evaka.core.pis.getPersonById
import evaka.core.shared.PersonId
import evaka.core.shared.async.AsyncJob
import evaka.core.shared.async.AsyncJobRunner
import evaka.core.shared.auth.AuthenticatedUser
import evaka.core.shared.auth.CitizenAuthLevel
import evaka.core.shared.dev.DevFridgeChild
import evaka.core.shared.dev.DevFridgePartnership
import evaka.core.shared.dev.DevPerson
import evaka.core.shared.dev.DevPersonType
import evaka.core.shared.dev.insert
import evaka.core.shared.domain.BadRequest
import evaka.core.shared.domain.HelsinkiDateTime
import evaka.core.shared.domain.MockEvakaClock
import evaka.core.shared.domain.RealEvakaClock
import evaka.core.shared.domain.UiLanguage
import evaka.core.user.updateLastStrongLogin
import java.time.LocalDate
import java.time.LocalTime
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired

class PersonalDataControllerCitizenIntegrationTest : FullApplicationTest(resetDbBeforeEach = true) {
    @Autowired private lateinit var personalDataController: PersonalDataControllerCitizen
    @Autowired private lateinit var asyncJobRunner: AsyncJobRunner<AsyncJob>

    private val adult = DevPerson()

    @Test
    fun `all notifications are enabled by default`() {
        db.transaction { tx -> tx.insert(adult, DevPersonType.RAW_ROW) }

        val disabledTypes =
            personalDataController.getNotificationSettings(
                dbInstance(),
                AuthenticatedUser.Citizen(adult.id, CitizenAuthLevel.WEAK),
                RealEvakaClock(),
            )
        assertEquals(emptySet(), disabledTypes)
    }

    @Test
    fun `notification settings can be updated`() {
        db.transaction { tx -> tx.insert(adult, DevPersonType.RAW_ROW) }

        personalDataController.updateNotificationSettings(
            dbInstance(),
            AuthenticatedUser.Citizen(adult.id, CitizenAuthLevel.WEAK),
            RealEvakaClock(),
            setOf(
                EmailMessageType.BULLETIN_NOTIFICATION,
                EmailMessageType.CALENDAR_EVENT_NOTIFICATION,
                EmailMessageType.DOCUMENT_NOTIFICATION,
                EmailMessageType.ATTENDANCE_RESERVATION_NOTIFICATION,
            ),
        )

        val settings =
            personalDataController.getNotificationSettings(
                dbInstance(),
                AuthenticatedUser.Citizen(adult.id, CitizenAuthLevel.WEAK),
                RealEvakaClock(),
            )
        assertEquals(
            setOf(
                EmailMessageType.BULLETIN_NOTIFICATION,
                EmailMessageType.CALENDAR_EVENT_NOTIFICATION,
                EmailMessageType.DOCUMENT_NOTIFICATION,
                EmailMessageType.ATTENDANCE_RESERVATION_NOTIFICATION,
            ),
            settings,
        )
    }

    @Test
    fun `email is sent to old address after updating email`() {
        val oldEmail = "vanha@example.com"
        val newEmail = "uusi@example.com"
        val person = DevPerson(email = oldEmail)
        db.transaction { tx -> tx.insert(person, DevPersonType.ADULT) }

        personalDataController.updatePersonalData(
            dbInstance(),
            AuthenticatedUser.Citizen(person.id, CitizenAuthLevel.STRONG),
            RealEvakaClock(),
            PersonalDataUpdate(person.firstName, "123456", person.backupPhone, newEmail),
        )

        asyncJobRunner.runPendingJobsSync(RealEvakaClock())
        assertEquals(1, MockEmailClient.emails.size)
        val email =
            MockEmailClient.emails.singleOrNull {
                it.toAddress == oldEmail &&
                    it.content.subject.startsWith("eVaka-sähköpostiosoitteesi on vaihdettu") &&
                    it.content.text.contains(newEmail)
            }
        assertNotNull(email, "Email should be sent to old address after email update")
    }

    @Test
    fun `null fields are left unchanged`() {
        val person =
            DevPerson(firstName = "Anna Maija", phone = "0501234567", email = "vanha@example.com")
        db.transaction { tx -> tx.insert(person, DevPersonType.ADULT) }

        personalDataController.updatePersonalData(
            dbInstance(),
            AuthenticatedUser.Citizen(person.id, CitizenAuthLevel.STRONG),
            RealEvakaClock(),
            PersonalDataUpdate(preferredName = "Maija"),
        )

        val updated = db.read { tx -> tx.getPersonById(person.id) }
        assertNotNull(updated)
        assertEquals("Maija", updated.preferredName)
        assertEquals(person.phone, updated.phone)
        assertEquals(person.backupPhone, updated.backupPhone)
        assertEquals(person.email, updated.email)
    }

    @Test
    fun `preferred name that is not one of the person's first names is rejected`() {
        val person = DevPerson(firstName = "Anna Maija")
        db.transaction { tx -> tx.insert(person, DevPersonType.ADULT) }

        assertThrows<BadRequest> {
            personalDataController.updatePersonalData(
                dbInstance(),
                AuthenticatedUser.Citizen(person.id, CitizenAuthLevel.STRONG),
                RealEvakaClock(),
                PersonalDataUpdate(preferredName = "Liisa"),
            )
        }
    }

    @Test
    fun `contact details can be updated without touching the preferred name`() {
        val person = DevPerson(firstName = "Anna", preferredName = "Anna")
        db.transaction { tx -> tx.insert(person, DevPersonType.ADULT) }

        personalDataController.updatePersonalData(
            dbInstance(),
            AuthenticatedUser.Citizen(person.id, CitizenAuthLevel.STRONG),
            RealEvakaClock(),
            PersonalDataUpdate(
                phone = "0501234567",
                backupPhone = "0507654321",
                email = "uusi@example.com",
            ),
        )

        val updated = db.read { tx -> tx.getPersonById(person.id) }
        assertNotNull(updated)
        assertEquals("0501234567", updated.phone)
        assertEquals("0507654321", updated.backupPhone)
        assertEquals("uusi@example.com", updated.email)
        assertEquals(person.preferredName, updated.preferredName)
    }

    @Test
    fun `invalid present fields are rejected even when other fields are null`() {
        val person = DevPerson()
        db.transaction { tx -> tx.insert(person, DevPersonType.ADULT) }

        assertThrows<BadRequest> {
            personalDataController.updatePersonalData(
                dbInstance(),
                AuthenticatedUser.Citizen(person.id, CitizenAuthLevel.STRONG),
                RealEvakaClock(),
                PersonalDataUpdate(phone = "", email = "not-an-email"),
            )
        }
    }

    @Test
    fun `email is sent to old address after a partial update of the email`() {
        val oldEmail = "vanha@example.com"
        val newEmail = "uusi@example.com"
        val person = DevPerson(email = oldEmail)
        db.transaction { tx -> tx.insert(person, DevPersonType.ADULT) }

        personalDataController.updatePersonalData(
            dbInstance(),
            AuthenticatedUser.Citizen(person.id, CitizenAuthLevel.STRONG),
            RealEvakaClock(),
            PersonalDataUpdate(email = newEmail),
        )

        asyncJobRunner.runPendingJobsSync(RealEvakaClock())
        val email =
            MockEmailClient.emails.singleOrNull {
                it.toAddress == oldEmail &&
                    it.content.subject.startsWith("eVaka-sähköpostiosoitteesi on vaihdettu") &&
                    it.content.text.contains(newEmail)
            }
        assertNotNull(email, "Email should be sent to old address after email update")
    }

    /** A citizen_user row exists for everyone who has logged in, and only for them. */
    private fun insertCitizenWhoHasLoggedIn(person: DevPerson, now: HelsinkiDateTime) =
        db.transaction { tx ->
            tx.insert(person, DevPersonType.RAW_ROW)
            tx.updateLastStrongLogin(now, person.id)
        }

    private fun preferredUiLanguageOf(person: DevPerson) =
        db.read { tx -> tx.getCitizenUserDetails(person.id) }?.preferredUiLanguage

    @Test
    fun `a weak login citizen can update their preferred UI language`() {
        val clock = RealEvakaClock()
        insertCitizenWhoHasLoggedIn(adult, clock.now())
        val user = AuthenticatedUser.Citizen(adult.id, CitizenAuthLevel.WEAK)

        assertNull(preferredUiLanguageOf(adult))

        personalDataController.updatePreferredUiLanguage(
            dbInstance(),
            user,
            clock,
            PersonalDataControllerCitizen.UpdatePreferredUiLanguageRequest(UiLanguage.SV),
        )
        assertEquals(UiLanguage.SV, preferredUiLanguageOf(adult))

        personalDataController.updatePreferredUiLanguage(
            dbInstance(),
            user,
            clock,
            PersonalDataControllerCitizen.UpdatePreferredUiLanguageRequest(UiLanguage.EN),
        )
        assertEquals(UiLanguage.EN, preferredUiLanguageOf(adult))
    }

    @Test
    fun `getFamily returns the citizen, their partner and children`() {
        val head = DevPerson(dateOfBirth = LocalDate.of(1980, 1, 1))
        val partner = DevPerson(dateOfBirth = LocalDate.of(1982, 1, 1))
        val olderChild = DevPerson(dateOfBirth = LocalDate.of(2018, 1, 1))
        val youngerChild = DevPerson(dateOfBirth = LocalDate.of(2021, 1, 1))
        val clock =
            MockEvakaClock(HelsinkiDateTime.of(LocalDate.of(2024, 1, 1), LocalTime.of(12, 0)))

        db.transaction { tx ->
            tx.insert(head, DevPersonType.ADULT)
            tx.insert(partner, DevPersonType.ADULT)
            tx.insert(olderChild, DevPersonType.CHILD)
            tx.insert(youngerChild, DevPersonType.CHILD)
            tx.insert(
                DevFridgePartnership(
                    first = head.id,
                    second = partner.id,
                    startDate = LocalDate.of(2024, 1, 1),
                    endDate = LocalDate.of(2024, 1, 1),
                    createdAt = clock.now(),
                )
            )
            listOf(olderChild, youngerChild).forEach { child ->
                tx.insert(
                    DevFridgeChild(
                        childId = child.id,
                        headOfChild = head.id,
                        startDate = LocalDate.of(2024, 1, 1),
                        endDate = LocalDate.of(2024, 1, 1),
                    )
                )
            }
        }

        fun getFamilyAs(citizenId: PersonId) =
            personalDataController.getFamily(
                dbInstance(),
                AuthenticatedUser.Citizen(citizenId, CitizenAuthLevel.WEAK),
                clock,
            )

        val response = getFamilyAs(head.id)
        assertEquals(setOf(head.id, partner.id), response.adults.map { it.personId }.toSet())
        // children are ordered by date of birth ascending (oldest first)
        assertEquals(listOf(olderChild.id, youngerChild.id), response.children.map { it.personId })

        // the partner is not head-of-child, but still sees the whole household
        val partnerResponse = getFamilyAs(partner.id)
        assertEquals(setOf(head.id, partner.id), partnerResponse.adults.map { it.personId }.toSet())
        assertEquals(
            listOf(olderChild.id, youngerChild.id),
            partnerResponse.children.map { it.personId },
        )
    }

    @Test
    fun `getFamily orders same-age children by last name then first name`() {
        val head = DevPerson(dateOfBirth = LocalDate.of(1980, 1, 1))
        val dob = LocalDate.of(2018, 1, 1)
        val korhonenAaron = DevPerson(dateOfBirth = dob, firstName = "Aaron", lastName = "Korhonen")
        val korhonenBertil =
            DevPerson(dateOfBirth = dob, firstName = "Bertil", lastName = "Korhonen")
        val virtanenAaron = DevPerson(dateOfBirth = dob, firstName = "Aaron", lastName = "Virtanen")
        val clock =
            MockEvakaClock(HelsinkiDateTime.of(LocalDate.of(2024, 1, 1), LocalTime.of(12, 0)))

        db.transaction { tx ->
            tx.insert(head, DevPersonType.ADULT)
            listOf(virtanenAaron, korhonenBertil, korhonenAaron).forEach { child ->
                tx.insert(child, DevPersonType.CHILD)
                tx.insert(
                    DevFridgeChild(
                        childId = child.id,
                        headOfChild = head.id,
                        startDate = LocalDate.of(2024, 1, 1),
                        endDate = LocalDate.of(2024, 1, 1),
                    )
                )
            }
        }

        val response =
            personalDataController.getFamily(
                dbInstance(),
                AuthenticatedUser.Citizen(head.id, CitizenAuthLevel.WEAK),
                clock,
            )

        assertEquals(
            listOf(korhonenAaron.id, korhonenBertil.id, virtanenAaron.id),
            response.children.map { it.personId },
        )
    }

    @Test
    fun `getFamily returns only the citizen for a solo adult`() {
        val solo = DevPerson()
        val clock =
            MockEvakaClock(HelsinkiDateTime.of(LocalDate.of(2024, 1, 1), LocalTime.of(12, 0)))
        db.transaction { tx -> tx.insert(solo, DevPersonType.ADULT) }

        val response =
            personalDataController.getFamily(
                dbInstance(),
                AuthenticatedUser.Citizen(solo.id, CitizenAuthLevel.WEAK),
                clock,
            )

        assertEquals(listOf(solo.id), response.adults.map { it.personId })
        assertEquals(emptyList(), response.children.map { it.personId })
    }
}
