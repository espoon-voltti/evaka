// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.pis.controller

import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.pis.EmailNotificationSettings
import fi.espoo.evaka.pis.controllers.PersonalDataControllerCitizen
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.CitizenAuthLevel
import fi.espoo.evaka.shared.dev.DevPersonType
import fi.espoo.evaka.shared.dev.insert
import fi.espoo.evaka.shared.domain.RealEvakaClock
import fi.espoo.evaka.testAdult_1
import kotlin.test.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class PersonalDataControllerCitizenIntegrationTest : FullApplicationTest(resetDbBeforeEach = true) {
    @Autowired lateinit var personalDataController: PersonalDataControllerCitizen

    @Test
    fun `all notifications are enabled by default`() {
        db.transaction { tx -> tx.insert(testAdult_1, DevPersonType.RAW_ROW) }

        val settings =
            personalDataController.getNotificationSettings(
                dbInstance(),
                AuthenticatedUser.Citizen(testAdult_1.id, CitizenAuthLevel.WEAK),
                RealEvakaClock(),
            )
        assertEquals(
            EmailNotificationSettings(
                message = true,
                bulletin = true,
                outdatedIncome = true,
                calendarEvent = true,
                decision = true,
                document = true,
                informalDocument = true,
                missingAttendanceReservation = true,
                discussionTimeReservationConfirmation = true,
                discussionSurveyCreationNotification = true,
                discussionTimeReservationReminder = true,
            ),
            settings,
        )
    }

    @Test
    fun `notification settings can be updated`() {
        db.transaction { tx -> tx.insert(testAdult_1, DevPersonType.RAW_ROW) }

        personalDataController.updateNotificationSettings(
            dbInstance(),
            AuthenticatedUser.Citizen(testAdult_1.id, CitizenAuthLevel.WEAK),
            RealEvakaClock(),
            EmailNotificationSettings(
                message = true,
                bulletin = false,
                outdatedIncome = true,
                calendarEvent = false,
                decision = true,
                document = false,
                informalDocument = true,
                missingAttendanceReservation = false,
                discussionTimeReservationConfirmation = true,
                discussionSurveyCreationNotification = true,
                discussionTimeReservationReminder = true,
            )
        )

        val settings =
            personalDataController.getNotificationSettings(
                dbInstance(),
                AuthenticatedUser.Citizen(testAdult_1.id, CitizenAuthLevel.WEAK),
                RealEvakaClock(),
            )
        assertEquals(
            EmailNotificationSettings(
                message = true,
                bulletin = false,
                outdatedIncome = true,
                calendarEvent = false,
                decision = true,
                document = false,
                informalDocument = true,
                missingAttendanceReservation = false,
                discussionTimeReservationConfirmation = true,
                discussionSurveyCreationNotification = true,
                discussionTimeReservationReminder = true,
            ),
            settings,
        )
    }
}
