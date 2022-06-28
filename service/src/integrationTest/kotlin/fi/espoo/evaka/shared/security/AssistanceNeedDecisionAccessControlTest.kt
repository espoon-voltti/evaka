// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.security

import fi.espoo.evaka.assistanceneed.decision.AssistanceNeedDecisionLanguage
import fi.espoo.evaka.assistanceneed.decision.AssistanceNeedDecisionStatus
import fi.espoo.evaka.assistanceneed.decision.ServiceOptions
import fi.espoo.evaka.assistanceneed.decision.StructuralMotivationOptions
import fi.espoo.evaka.shared.AssistanceNeedDecisionId
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.dev.DevAssistanceNeedDecision
import fi.espoo.evaka.shared.dev.DevCareArea
import fi.espoo.evaka.shared.dev.DevChild
import fi.espoo.evaka.shared.dev.DevDaycare
import fi.espoo.evaka.shared.dev.DevPerson
import fi.espoo.evaka.shared.dev.DevPlacement
import fi.espoo.evaka.shared.dev.insertTestAssistanceNeedDecision
import fi.espoo.evaka.shared.dev.insertTestCareArea
import fi.espoo.evaka.shared.dev.insertTestChild
import fi.espoo.evaka.shared.dev.insertTestDaycare
import fi.espoo.evaka.shared.dev.insertTestPerson
import fi.espoo.evaka.shared.dev.insertTestPlacement
import fi.espoo.evaka.shared.security.actionrule.HasUnitRole
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.UUID
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AssistanceNeedDecisionAccessControlTest : AccessControlTest() {
    private lateinit var childId: ChildId
    private lateinit var assistanceNeedDecisionId: AssistanceNeedDecisionId

    @BeforeEach
    private fun beforeEach() {
        assistanceNeedDecisionId = db.transaction { tx ->
            childId = tx.insertTestPerson(DevPerson()).also { tx.insertTestChild(DevChild(id = it)) }
            tx.insertTestAssistanceNeedDecision(
                childId,
                DevAssistanceNeedDecision(
                    id = AssistanceNeedDecisionId(UUID.randomUUID()),
                    decisionNumber = 10000,
                    childId = childId,
                    startDate = LocalDate.of(2020, 1, 1),
                    endDate = LocalDate.of(2020, 6, 1),
                    status = AssistanceNeedDecisionStatus.DRAFT,
                    language = AssistanceNeedDecisionLanguage.FI,
                    decisionMade = LocalDate.of(2019, 9, 1),
                    sentForDecision = LocalDate.of(2019, 6, 1),
                    selectedUnit = null,
                    preparedBy1 = null,
                    preparedBy2 = null,
                    decisionMaker = null,
                    pedagogicalMotivation = null,
                    structuralMotivationOptions = StructuralMotivationOptions(
                        smallerGroup = false,
                        specialGroup = false,
                        smallGroup = false,
                        groupAssistant = false,
                        childAssistant = false,
                        additionalStaff = false
                    ),
                    structuralMotivationDescription = null,
                    careMotivation = null,
                    serviceOptions = ServiceOptions(
                        consultationSpecialEd = false,
                        partTimeSpecialEd = false,
                        fullTimeSpecialEd = false,
                        interpretationAndAssistanceServices = false,
                        specialAides = false
                    ),
                    servicesMotivation = null,
                    expertResponsibilities = null,
                    guardiansHeardOn = null,
                    guardianInfo = emptySet(),
                    viewOfGuardians = null,
                    otherRepresentativeHeard = false,
                    otherRepresentativeDetails = null,
                    assistanceLevel = null,
                    assistanceServicesTime = null,
                    motivationForDecision = null
                )
            )
        }
    }

    @Test
    fun `HasUnitRole inPlacementUnitOfChildOfAssistanceNeedDecision`() {
        val action = Action.AssistanceNeedDecision.READ
        rules.add(action, HasUnitRole(UserRole.UNIT_SUPERVISOR).inPlacementUnitOfChildOfAssistanceNeedDecision())
        val daycareId = db.transaction { tx ->
            val areaId = tx.insertTestCareArea(DevCareArea())
            val daycareId = tx.insertTestDaycare(DevDaycare(areaId = areaId))
            tx.insertTestPlacement(DevPlacement(childId = childId, unitId = daycareId, endDate = LocalDate.of(2100, 1, 1)))
            daycareId
        }
        val unitSupervisor = createTestEmployee(emptySet(), mapOf(daycareId to UserRole.UNIT_SUPERVISOR))
        assertTrue(accessControl.hasPermissionFor(unitSupervisor, action, assistanceNeedDecisionId))

        val staff = createTestEmployee(emptySet(), mapOf(daycareId to UserRole.STAFF))
        assertFalse(accessControl.hasPermissionFor(staff, action, assistanceNeedDecisionId))
    }
}
