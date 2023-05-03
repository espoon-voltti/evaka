// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.espoobi

import fi.espoo.evaka.PureJdbiTest
import fi.espoo.evaka.shared.AreaId
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.GroupId
import fi.espoo.evaka.shared.GroupPlacementId
import fi.espoo.evaka.shared.Id
import fi.espoo.evaka.shared.PlacementId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.dev.DevCareArea
import fi.espoo.evaka.shared.dev.DevChild
import fi.espoo.evaka.shared.dev.DevDaycare
import fi.espoo.evaka.shared.dev.DevDaycareGroup
import fi.espoo.evaka.shared.dev.DevDaycareGroupPlacement
import fi.espoo.evaka.shared.dev.DevPerson
import fi.espoo.evaka.shared.dev.DevPlacement
import fi.espoo.evaka.shared.dev.insertTestCareArea
import fi.espoo.evaka.shared.dev.insertTestChild
import fi.espoo.evaka.shared.dev.insertTestDaycare
import fi.espoo.evaka.shared.dev.insertTestDaycareGroup
import fi.espoo.evaka.shared.dev.insertTestDaycareGroupPlacement
import fi.espoo.evaka.shared.dev.insertTestPerson
import fi.espoo.evaka.shared.dev.insertTestPlacement
import kotlin.test.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.http.converter.HttpMessageConverter
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse

class EspooBiPocTest : PureJdbiTest(resetDbBeforeEach = true) {
    private val user = AuthenticatedUser.Integration

    @Test
    fun getAreas() {
        val areaId = db.transaction { it.insertTestArea() }
        assertSingleRowContainingId(EspooBiPoc.getAreas, areaId)
    }

    @Test
    fun getUnits() {
        val unitId = db.transaction { it.insertTestDaycare() }
        assertSingleRowContainingId(EspooBiPoc.getUnits, unitId)
    }

    @Test
    fun getGroups() {
        val groupId = db.transaction { it.insertTestGroup() }
        assertSingleRowContainingId(EspooBiPoc.getGroups, groupId)
    }

    @Test
    fun getChildren() {
        val childId = db.transaction { it.insertTestChild() }
        assertSingleRowContainingId(EspooBiPoc.getChildren, childId)
    }

    @Test
    fun getPlacements() {
        val placementId = db.transaction { it.insertTestPlacement() }
        assertSingleRowContainingId(EspooBiPoc.getPlacements, placementId)
    }

    @Test
    fun getGroupPlacements() {
        val groupPlacementId = db.transaction { it.insertTestGroupPlacement() }
        assertSingleRowContainingId(EspooBiPoc.getGroupPlacements, groupPlacementId)
    }

    private fun assertSingleRowContainingId(route: StreamingCsvRoute, id: Id<*>) {
        val request = MockHttpServletRequest()
        val response = MockHttpServletResponse()
        val messageConverters = emptyList<HttpMessageConverter<*>>()
        route(dbInstance(), user).writeTo(request, response) { messageConverters }

        val content = response.contentAsString
        val lines = content.lineSequence().map { it.trim() }.filter { it.isNotEmpty() }
        assertTrue(lines.first().looksLikeHeaderRow())
        assertTrue(lines.drop(1).single().contains(id.toString()))
    }

    private fun String.looksLikeHeaderRow() = trim().contains(',')

    private fun Database.Transaction.insertTestArea(): AreaId = insertTestCareArea(DevCareArea())
    private fun Database.Transaction.insertTestDaycare(): DaycareId =
        insertTestDaycare(DevDaycare(areaId = insertTestArea()))
    private fun Database.Transaction.insertTestGroup(daycare: DaycareId? = null): GroupId =
        insertTestDaycareGroup(DevDaycareGroup(daycareId = daycare ?: insertTestDaycare()))
    private fun Database.Transaction.insertTestChild(): ChildId =
        insertTestPerson(DevPerson()).also { insertTestChild(DevChild(it)) }
    private fun Database.Transaction.insertTestPlacement(daycare: DaycareId? = null): PlacementId =
        insertTestPlacement(
            DevPlacement(childId = insertTestChild(), unitId = daycare ?: insertTestDaycare())
        )
    private fun Database.Transaction.insertTestGroupPlacement(): GroupPlacementId =
        insertTestDaycare().let { daycare ->
            insertTestDaycareGroupPlacement(
                DevDaycareGroupPlacement(
                    daycarePlacementId = insertTestPlacement(daycare),
                    daycareGroupId = insertTestGroup(daycare),
                )
            )
        }
}
