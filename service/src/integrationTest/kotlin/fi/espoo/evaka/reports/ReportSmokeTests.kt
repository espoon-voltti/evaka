// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.reports

import com.github.kittinunf.fuel.core.Request
import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.insertGeneralTestFixtures
import fi.espoo.evaka.resetDatabase
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.auth.asUser
import fi.espoo.evaka.testAreaId
import fi.espoo.evaka.testDaycare
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.util.UUID
import kotlin.test.assertEquals

class ReportSmokeTests : FullApplicationTest() {
    private val testUser = AuthenticatedUser.Employee(UUID.randomUUID(), setOf(UserRole.ADMIN))

    @BeforeAll
    fun beforeEach() {
        db.transaction { tx ->
            tx.insertGeneralTestFixtures()
        }
    }

    @AfterAll
    fun afterEach() {
        db.transaction { tx ->
            tx.resetDatabase()
        }
    }

    @Test
    fun `applications report returns http 200`() {
        assertOkResponse(
            http.get(
                "/reports/applications",
                listOf("from" to "2020-05-01", "to" to "2020-08-01")
            )
        )
    }

    @Test
    fun `assistance needs and actions report returns http 200`() {
        assertOkResponse(
            http.get(
                "/reports/assistance-needs-and-actions",
                listOf("date" to "2020-08-01")
            )
        )
    }

    @Test
    fun `child-age-language report returns http 200`() {
        assertOkResponse(
            http.get(
                "/reports/child-age-language",
                listOf("date" to "2020-08-01")
            )
        )
    }

    @Test
    fun `children-in-different-address report returns http 200`() {
        assertOkResponse(
            http.get(
                "/reports/children-in-different-address",
                listOf("date" to "2020-08-01")
            )
        )
    }

    @Test
    fun `decisions report returns http 200`() {
        assertOkResponse(
            http.get(
                "/reports/decisions",
                listOf("from" to "2020-05-01", "to" to "2020-08-01")
            )
        )
    }

    @Test
    fun `duplicate-people report returns http 200`() {
        assertOkResponse(
            http.get(
                "/reports/duplicate-people"
            )
        )
    }

    @Test
    fun `ended-placements report returns http 200`() {
        assertOkResponse(
            http.get(
                "/reports/ended-placements",
                listOf("year" to 2020, "month" to 1)
            )
        )
    }

    @Test
    fun `family-conflicts report returns http 200`() {
        assertOkResponse(
            http.get(
                "/reports/family-conflicts"
            )
        )
    }

    @Test
    fun `family-contacts report returns http 200`() {
        assertOkResponse(
            http.get(
                "/reports/family-contacts?unitId=${testDaycare.id}"
            )
        )
    }

    @Test
    fun `invoice report returns http 200`() {
        assertOkResponse(
            http.get(
                "/reports/invoices",
                listOf("date" to "2020-08-01")
            )
        )
    }

    @Test
    fun `missing-head-of-family report returns http 200`() {
        assertOkResponse(
            http.get(
                "/reports/missing-head-of-family",
                listOf("from" to "2020-05-01", "to" to "2020-08-01")
            )
        )
    }

    @Test
    fun `missing-service-need report returns http 200`() {
        assertOkResponse(
            http.get(
                "/reports/missing-service-need",
                listOf("from" to "2020-05-01", "to" to "2020-08-01")
            )
        )
    }

    @Test
    fun `occupancy-by-unit report returns http 200`() {
        assertOkResponse(
            http.get(
                "/reports/occupancy-by-unit",
                listOf("type" to "PLANNED", "careAreaId" to testAreaId, "year" to 2020, "month" to 1)
            )
        )

        assertOkResponse(
            http.get(
                "/reports/occupancy-by-unit",
                listOf("type" to "CONFIRMED", "careAreaId" to testAreaId, "year" to 2020, "month" to 1)
            )
        )

        assertOkResponse(
            http.get(
                "/reports/occupancy-by-unit",
                listOf("type" to "REALIZED", "careAreaId" to testAreaId, "year" to 2020, "month" to 1)
            )
        )
    }

    @Test
    fun `occupancy-by-group report returns http 200`() {
        assertOkResponse(
            http.get(
                "/reports/occupancy-by-group",
                listOf("type" to "CONFIRMED", "careAreaId" to testAreaId, "year" to 2020, "month" to 1)
            )
        )

        assertOkResponse(
            http.get(
                "/reports/occupancy-by-group",
                listOf("type" to "REALIZED", "careAreaId" to testAreaId, "year" to 2020, "month" to 1)
            )
        )
    }

    @Test
    fun `partners-in-different-address report returns http 200`() {
        assertOkResponse(
            http.get(
                "/reports/partners-in-different-address"
            )
        )
    }

    @Test
    fun `presences report returns http 200`() {
        assertOkResponse(
            http.get(
                "/reports/presences",
                listOf("from" to "2020-05-01", "to" to "2020-05-02")
            )
        )
    }

    @Test
    fun `raw report returns http 200`() {
        assertOkResponse(
            http.get(
                "/reports/raw",
                listOf("from" to "2020-05-01", "to" to "2020-05-02")
            )
        )
    }

    @Test
    fun `service-need report returns http 200`() {
        assertOkResponse(
            http.get(
                "/reports/service-need",
                listOf("date" to "2020-08-01")
            )
        )
    }

    @Test
    fun `starting-placements report returns http 200`() {
        assertOkResponse(
            http.get(
                "/reports/starting-placements",
                listOf("year" to 2020, "month" to 1)
            )
        )
    }

    @Test
    fun `placement sketching report returns http 200`() {
        assertOkResponse(
            http.get(
                "/reports/placement-sketching",
                listOf("placementStartDate" to "2021-01-01", "earliestPreferredStartDate" to "2021-08-13")
            )
        )
    }

    private fun assertOkResponse(req: Request) {
        val (_, response, _) = req.asUser(testUser).response()
        assertEquals(200, response.statusCode)
    }
}
