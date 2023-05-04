// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.espoobi

import fi.espoo.evaka.PureJdbiTest
import fi.espoo.evaka.application.ApplicationType
import fi.espoo.evaka.application.persistence.daycare.Adult
import fi.espoo.evaka.application.persistence.daycare.Apply
import fi.espoo.evaka.application.persistence.daycare.CareDetails
import fi.espoo.evaka.application.persistence.daycare.Child
import fi.espoo.evaka.application.persistence.daycare.DaycareFormV0
import fi.espoo.evaka.daycare.service.AbsenceCategory
import fi.espoo.evaka.decision.DecisionStatus
import fi.espoo.evaka.decision.DecisionType
import fi.espoo.evaka.shared.AbsenceId
import fi.espoo.evaka.shared.ApplicationId
import fi.espoo.evaka.shared.AreaId
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.DaycareCaretakerId
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.DecisionId
import fi.espoo.evaka.shared.GroupId
import fi.espoo.evaka.shared.GroupPlacementId
import fi.espoo.evaka.shared.Id
import fi.espoo.evaka.shared.PlacementId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.dev.DevAbsence
import fi.espoo.evaka.shared.dev.DevCareArea
import fi.espoo.evaka.shared.dev.DevChild
import fi.espoo.evaka.shared.dev.DevDaycare
import fi.espoo.evaka.shared.dev.DevDaycareCaretaker
import fi.espoo.evaka.shared.dev.DevDaycareGroup
import fi.espoo.evaka.shared.dev.DevDaycareGroupPlacement
import fi.espoo.evaka.shared.dev.DevPerson
import fi.espoo.evaka.shared.dev.DevPlacement
import fi.espoo.evaka.shared.dev.TestDecision
import fi.espoo.evaka.shared.dev.insertTestAbsence
import fi.espoo.evaka.shared.dev.insertTestApplication
import fi.espoo.evaka.shared.dev.insertTestApplicationForm
import fi.espoo.evaka.shared.dev.insertTestCareArea
import fi.espoo.evaka.shared.dev.insertTestChild
import fi.espoo.evaka.shared.dev.insertTestDaycare
import fi.espoo.evaka.shared.dev.insertTestDaycareCaretaker
import fi.espoo.evaka.shared.dev.insertTestDaycareGroup
import fi.espoo.evaka.shared.dev.insertTestDaycareGroupPlacement
import fi.espoo.evaka.shared.dev.insertTestDecision
import fi.espoo.evaka.shared.dev.insertTestPerson
import fi.espoo.evaka.shared.dev.insertTestPlacement
import java.time.LocalDate
import kotlin.test.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.http.converter.HttpMessageConverter
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse

class EspooBiPocTest : PureJdbiTest(resetDbBeforeEach = true) {
    private val user = AuthenticatedUser.Integration

    @Test
    fun getAreas() {
        val id = db.transaction { it.insertTestArea() }
        assertSingleRowContainingId(EspooBiPoc.getAreas, id)
    }

    @Test
    fun getUnits() {
        val id = db.transaction { it.insertTestDaycare() }
        assertSingleRowContainingId(EspooBiPoc.getUnits, id)
    }

    @Test
    fun getGroups() {
        val id = db.transaction { it.insertTestGroup() }
        assertSingleRowContainingId(EspooBiPoc.getGroups, id)
    }

    @Test
    fun getChildren() {
        val id = db.transaction { it.insertTestChild() }
        assertSingleRowContainingId(EspooBiPoc.getChildren, id)
    }

    @Test
    fun getPlacements() {
        val id = db.transaction { it.insertTestPlacement() }
        assertSingleRowContainingId(EspooBiPoc.getPlacements, id)
    }

    @Test
    fun getGroupPlacements() {
        val id = db.transaction { it.insertTestGroupPlacement() }
        assertSingleRowContainingId(EspooBiPoc.getGroupPlacements, id)
    }

    @Test
    fun getAbsences() {
        val id = db.transaction { it.insertTestAbsence() }
        assertSingleRowContainingId(EspooBiPoc.getAbsences, id)
    }

    @Test
    fun getGroupCaretakerAllocations() {
        val id = db.transaction { it.insertTestGroupCaretakerAllocation() }
        assertSingleRowContainingId(EspooBiPoc.getGroupCaretakerAllocations, id)
    }

    @Test
    fun getApplications() {
        val id = db.transaction { it.insertTestApplicationWithForm() }
        assertSingleRowContainingId(EspooBiPoc.getApplications, id)
    }

    @Test
    fun getDecisions() {
        val id = db.transaction { it.insertTestDecision() }
        assertSingleRowContainingId(EspooBiPoc.getDecisions, id)
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
    private fun Database.Transaction.insertTestAbsence(): AbsenceId =
        insertTestAbsence(
            DevAbsence(
                childId = insertTestChild(),
                date = LocalDate.of(2020, 1, 1),
                absenceCategory = AbsenceCategory.BILLABLE,
            )
        )

    private fun Database.Transaction.insertTestGroupCaretakerAllocation(): DaycareCaretakerId =
        insertTestDaycareCaretaker(
            DevDaycareCaretaker(
                groupId = insertTestGroup(),
            )
        )

    private fun Database.Transaction.insertTestApplicationWithForm(
        daycare: DaycareId? = null
    ): ApplicationId =
        insertTestApplication(
                type = ApplicationType.DAYCARE,
                childId = insertTestChild(),
                guardianId = insertTestPerson(DevPerson())
            )
            .also {
                insertTestApplicationForm(
                    it,
                    DaycareFormV0(
                        type = ApplicationType.DAYCARE,
                        connectedDaycare = false,
                        urgent = true,
                        careDetails =
                            CareDetails(
                                assistanceNeeded = true,
                            ),
                        extendedCare = true,
                        child = Child(dateOfBirth = null),
                        guardian = Adult(),
                        apply = Apply(preferredUnits = listOf(daycare ?: insertTestDaycare())),
                        preferredStartDate = LocalDate.of(2019, 1, 1)
                    )
                )
            }

    private fun Database.Transaction.insertTestDecision(): DecisionId {
        val daycare = insertTestDaycare()
        return insertTestDecision(
            TestDecision(
                applicationId = insertTestApplicationWithForm(daycare),
                createdBy = AuthenticatedUser.SystemInternalUser.evakaUserId,
                startDate = LocalDate.of(2019, 3, 1),
                endDate = LocalDate.of(2019, 4, 1),
                type = DecisionType.DAYCARE,
                unitId = daycare,
                status = DecisionStatus.ACCEPTED,
            )
        )
    }
}
