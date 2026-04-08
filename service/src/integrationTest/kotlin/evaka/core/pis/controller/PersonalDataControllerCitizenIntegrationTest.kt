// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.pis.controller

import evaka.core.FullApplicationTest
import evaka.core.emailclient.MockEmailClient
import evaka.core.pis.EmailMessageType
import evaka.core.pis.PersonalDataUpdate
import evaka.core.pis.controllers.PersonalDataControllerCitizen
import evaka.core.shared.async.AsyncJob
import evaka.core.shared.async.AsyncJobRunner
import evaka.core.shared.auth.AuthenticatedUser
import evaka.core.shared.auth.CitizenAuthLevel
import evaka.core.shared.dev.DevPerson
import evaka.core.shared.dev.DevPersonType
import evaka.core.shared.dev.insert
import evaka.core.shared.domain.RealEvakaClock
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import org.junit.jupiter.api.Test
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
}
