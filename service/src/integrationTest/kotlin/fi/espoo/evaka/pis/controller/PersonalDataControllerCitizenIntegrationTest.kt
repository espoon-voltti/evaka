// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.pis.controller

import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.emailclient.MockEmailClient
import fi.espoo.evaka.pis.EmailMessageType
import fi.espoo.evaka.pis.PersonalDataUpdate
import fi.espoo.evaka.pis.controllers.PersonalDataControllerCitizen
import fi.espoo.evaka.shared.async.AsyncJob
import fi.espoo.evaka.shared.async.AsyncJobRunner
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.CitizenAuthLevel
import fi.espoo.evaka.shared.dev.DevPerson
import fi.espoo.evaka.shared.dev.DevPersonType
import fi.espoo.evaka.shared.dev.insert
import fi.espoo.evaka.shared.domain.RealEvakaClock
import fi.espoo.evaka.testAdult_1
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class PersonalDataControllerCitizenIntegrationTest : FullApplicationTest(resetDbBeforeEach = true) {
    @Autowired private lateinit var personalDataController: PersonalDataControllerCitizen
    @Autowired private lateinit var asyncJobRunner: AsyncJobRunner<AsyncJob>

    @Test
    fun `all notifications are enabled by default`() {
        db.transaction { tx -> tx.insert(testAdult_1, DevPersonType.RAW_ROW) }

        val disabledTypes =
            personalDataController.getNotificationSettings(
                dbInstance(),
                AuthenticatedUser.Citizen(testAdult_1.id, CitizenAuthLevel.WEAK),
                RealEvakaClock(),
            )
        assertEquals(emptySet(), disabledTypes)
    }

    @Test
    fun `notification settings can be updated`() {
        db.transaction { tx -> tx.insert(testAdult_1, DevPersonType.RAW_ROW) }

        personalDataController.updateNotificationSettings(
            dbInstance(),
            AuthenticatedUser.Citizen(testAdult_1.id, CitizenAuthLevel.WEAK),
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
                AuthenticatedUser.Citizen(testAdult_1.id, CitizenAuthLevel.WEAK),
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
    fun `email is sent to old and new address after updating email`() {
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
        assertEquals(2, MockEmailClient.emails.size)
        assertEquals(setOf(oldEmail, newEmail), MockEmailClient.emails.map { it.toAddress }.toSet())
        assertTrue(
            MockEmailClient.emails
                .map { it.content.subject }
                .all { it.startsWith("eVaka-sähköpostiosoitteesi on vaihdettu") }
        )
    }
}
