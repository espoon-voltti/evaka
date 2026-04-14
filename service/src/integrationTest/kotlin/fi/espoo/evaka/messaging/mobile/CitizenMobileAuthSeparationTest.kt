// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.messaging.mobile

import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.CitizenAuthLevel
import fi.espoo.evaka.shared.auth.asUser
import fi.espoo.evaka.shared.dev.DevCareArea
import fi.espoo.evaka.shared.dev.DevDaycare
import fi.espoo.evaka.shared.dev.DevDaycareGroup
import fi.espoo.evaka.shared.dev.DevDaycareGroupPlacement
import fi.espoo.evaka.shared.dev.DevPerson
import fi.espoo.evaka.shared.dev.DevPersonType
import fi.espoo.evaka.shared.dev.DevPlacement
import fi.espoo.evaka.shared.dev.insert
import fi.espoo.evaka.shared.security.PilotFeature
import fi.espoo.evaka.pis.service.insertGuardian
import java.time.LocalDate
import kotlin.test.assertEquals
import okhttp3.OkHttpClient
import okhttp3.Request
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class CitizenMobileAuthSeparationTest : FullApplicationTest(resetDbBeforeEach = true) {
    private val http = OkHttpClient()

    private val area = DevCareArea()
    private val daycare =
        DevDaycare(areaId = area.id, enabledPilotFeatures = setOf(PilotFeature.MESSAGING))
    private val group = DevDaycareGroup(daycareId = daycare.id)
    private val citizen = DevPerson()
    private val child = DevPerson()

    @BeforeEach
    fun setup() {
        val placementStart = LocalDate.of(2024, 1, 1)
        val placementEnd = LocalDate.of(2024, 12, 31)

        db.transaction { tx ->
            tx.insert(area)
            tx.insert(daycare)
            tx.insert(group)

            tx.insert(citizen, DevPersonType.ADULT)
            tx.insert(child, DevPersonType.CHILD)
            tx.insertGuardian(citizen.id, child.id)

            val placementId =
                tx.insert(
                    DevPlacement(
                        childId = child.id,
                        unitId = daycare.id,
                        startDate = placementStart,
                        endDate = placementEnd,
                    )
                )
            tx.insert(
                DevDaycareGroupPlacement(
                    daycarePlacementId = placementId,
                    daycareGroupId = group.id,
                    startDate = placementStart,
                    endDate = placementEnd,
                )
            )
        }
    }

    @Test
    fun `web citizen cannot call citizen-mobile endpoint`() {
        val response =
            http
                .newCall(
                    Request.Builder()
                        .url(
                            "http://localhost:$httpPort/citizen-mobile/messages/unread-count/v1"
                        )
                        .asUser(AuthenticatedUser.Citizen(citizen.id, CitizenAuthLevel.WEAK))
                        .build()
                )
                .execute()
        assertEquals(401, response.code)
    }

    @Test
    fun `mobile citizen cannot call web citizen endpoint`() {
        val response =
            http
                .newCall(
                    Request.Builder()
                        .url("http://localhost:$httpPort/citizen/messages/unread-count")
                        .asUser(AuthenticatedUser.CitizenMobile(citizen.id, CitizenAuthLevel.WEAK))
                        .build()
                )
                .execute()
        assertEquals(403, response.code)
    }

    @Test
    fun `mobile citizen can call citizen-mobile endpoint`() {
        val response =
            http
                .newCall(
                    Request.Builder()
                        .url(
                            "http://localhost:$httpPort/citizen-mobile/messages/unread-count/v1"
                        )
                        .asUser(AuthenticatedUser.CitizenMobile(citizen.id, CitizenAuthLevel.WEAK))
                        .build()
                )
                .execute()
        assertEquals(200, response.code)
    }
}
