// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.pis.controller

import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.pis.EmailMessageType
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
}
