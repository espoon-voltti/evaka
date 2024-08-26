// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.systemnotifications

import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.dev.DevCareArea
import fi.espoo.evaka.shared.dev.DevDaycare
import fi.espoo.evaka.shared.dev.DevEmployee
import fi.espoo.evaka.shared.dev.DevMobileDevice
import fi.espoo.evaka.shared.dev.insert
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.domain.MockEvakaClock
import java.time.LocalDate
import java.time.LocalTime
import kotlin.test.assertEquals
import kotlin.test.assertNull
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class SystemNotificationsControllerIntegrationTest : FullApplicationTest(resetDbBeforeEach = true) {
    @Autowired private lateinit var controller: SystemNotificationsController

    private val area = DevCareArea()
    private val daycare = DevDaycare(areaId = area.id)
    private val mobile = DevMobileDevice(unitId = daycare.id)
    private val admin = DevEmployee(roles = setOf(UserRole.ADMIN))

    @Test
    fun `system notification updates and reads work`() {
        db.transaction {
            it.insert(area)
            it.insert(daycare)
            it.insert(mobile)
            it.insert(admin)
        }
        val now = HelsinkiDateTime.of(LocalDate.of(2024, 3, 1), LocalTime.of(11, 0, 0))
        assertEquals(emptyList(), getAllSystemNotifications(now))
        assertEquals(
            SystemNotificationsController.CurrentNotificationResponse(null),
            getCurrentSystemNotificationCitizen(now),
        )
        assertEquals(
            SystemNotificationsController.CurrentNotificationResponse(null),
            getCurrentSystemNotificationEmployeeMobile(now),
        )

        val validTo = now.plusDays(1)

        putSystemNotification(
            now,
            SystemNotification(
                targetGroup = SystemNotificationTargetGroup.CITIZENS,
                text = "old text",
                validTo = now,
            ),
        )
        val citizenNotification =
            SystemNotification(
                targetGroup = SystemNotificationTargetGroup.CITIZENS,
                text = "new text for citizens",
                validTo = validTo,
            )
        putSystemNotification(now, citizenNotification)
        val employeeNotification =
            SystemNotification(
                targetGroup = SystemNotificationTargetGroup.EMPLOYEES,
                text = "text for employees",
                validTo = validTo,
            )
        putSystemNotification(now, employeeNotification)

        val beforeValidTo = validTo.minusHours(1)
        val afterValidTo = validTo.plusHours(1)

        assertEquals(
            setOf(citizenNotification, employeeNotification),
            getAllSystemNotifications(afterValidTo).toSet(),
        )
        assertEquals(
            citizenNotification,
            getCurrentSystemNotificationCitizen(beforeValidTo).notification,
        )
        assertNull(getCurrentSystemNotificationCitizen(afterValidTo).notification)
        assertEquals(
            employeeNotification,
            getCurrentSystemNotificationEmployeeMobile(beforeValidTo).notification,
        )
        assertNull(getCurrentSystemNotificationEmployeeMobile(afterValidTo).notification)

        deleteSystemNotification(now, SystemNotificationTargetGroup.CITIZENS)
        assertEquals(listOf(employeeNotification), getAllSystemNotifications(now))
        assertNull(getCurrentSystemNotificationCitizen(beforeValidTo).notification)
    }

    private fun putSystemNotification(now: HelsinkiDateTime, body: SystemNotification) =
        controller.putSystemNotification(dbInstance(), admin.user, MockEvakaClock(now), body)

    private fun deleteSystemNotification(
        now: HelsinkiDateTime,
        targetGroup: SystemNotificationTargetGroup,
    ) =
        controller.deleteSystemNotification(
            dbInstance(),
            admin.user,
            MockEvakaClock(now),
            targetGroup,
        )

    private fun getAllSystemNotifications(now: HelsinkiDateTime) =
        controller.getAllSystemNotifications(dbInstance(), admin.user, MockEvakaClock(now))

    private fun getCurrentSystemNotificationCitizen(now: HelsinkiDateTime) =
        controller.getCurrentSystemNotificationCitizen(dbInstance(), MockEvakaClock(now))

    private fun getCurrentSystemNotificationEmployeeMobile(now: HelsinkiDateTime) =
        controller.getCurrentSystemNotificationEmployeeMobile(
            dbInstance(),
            AuthenticatedUser.MobileDevice(mobile.id),
            MockEvakaClock(now),
        )
}
