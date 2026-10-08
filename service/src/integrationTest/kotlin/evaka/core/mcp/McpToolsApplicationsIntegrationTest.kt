// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.mcp

import evaka.core.AuditContext
import evaka.core.FullApplicationTest
import evaka.core.application.ApplicationStateService
import evaka.core.application.ApplicationStatus
import evaka.core.application.ApplicationType
import evaka.core.application.DaycarePlacementPlan
import evaka.core.application.persistence.daycare.Adult
import evaka.core.application.persistence.daycare.Apply
import evaka.core.application.persistence.daycare.CareDetails
import evaka.core.application.persistence.daycare.Child
import evaka.core.application.persistence.daycare.DaycareFormV0
import evaka.core.decision.DecisionStatus
import evaka.core.decision.DecisionType
import evaka.core.decision.getDecisionsByApplication
import evaka.core.preschoolTerm2020
import evaka.core.shared.ApplicationId
import evaka.core.shared.McpAuthorizationId
import evaka.core.shared.PlacementId
import evaka.core.shared.auth.UserRole
import evaka.core.shared.db.Database
import evaka.core.shared.dev.DevCareArea
import evaka.core.shared.dev.DevDaycare
import evaka.core.shared.dev.DevEmployee
import evaka.core.shared.dev.DevGuardian
import evaka.core.shared.dev.DevPerson
import evaka.core.shared.dev.DevPersonType
import evaka.core.shared.dev.DevPlacement
import evaka.core.shared.dev.insert
import evaka.core.shared.dev.insertDefaultDecisionGenericReasonings
import evaka.core.shared.dev.insertTestApplication
import evaka.core.shared.domain.DateRange
import evaka.core.shared.domain.FiniteDateRange
import evaka.core.shared.domain.HelsinkiDateTime
import evaka.core.shared.domain.MockEvakaClock
import evaka.core.shared.security.actionrule.AccessControlFilter
import evaka.core.vtjclient.service.persondetails.MockPersonDetailsService
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID
import kotlin.test.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class McpToolsApplicationsIntegrationTest : FullApplicationTest(resetDbBeforeEach = true) {
    @Autowired private lateinit var tools: McpTools
    @Autowired private lateinit var config: McpServerConfig
    @Autowired private lateinit var applicationStateService: ApplicationStateService

    private val now = HelsinkiDateTime.of(LocalDate.of(2020, 3, 2), LocalTime.of(12, 0))
    private val clock = MockEvakaClock(now)
    private val admin = DevEmployee(roles = setOf(UserRole.ADMIN))
    private val area = DevCareArea()
    private val unit =
        DevDaycare(
            areaId = area.id,
            daycareApplyPeriod = DateRange(LocalDate.of(2020, 1, 1), null),
            preschoolApplyPeriod = DateRange(LocalDate.of(2020, 1, 1), null),
        )
    private val otherUnit = DevDaycare(areaId = area.id, name = "Other unit")
    private val guardian = DevPerson(ssn = "070644-937X")
    private val child = DevPerson(ssn = "070714A9126", dateOfBirth = LocalDate.of(2014, 7, 7))
    private val preschoolPeriod = preschoolTerm2020.finnishPreschool
    private val connectedDaycarePeriod =
        FiniteDateRange(preschoolPeriod.start.minusDays(12), preschoolPeriod.end.plusDays(15))
    private lateinit var authorizationId: McpAuthorizationId

    @BeforeEach
    fun beforeEach() {
        authorizationId = db.transaction { tx ->
            tx.insertDefaultDecisionGenericReasonings()
            tx.insert(admin)
            tx.insert(area)
            tx.insert(unit)
            tx.insert(otherUnit)
            tx.insert(guardian, DevPersonType.ADULT)
            tx.insert(child, DevPersonType.CHILD)
            tx.insert(DevGuardian(guardianId = guardian.id, childId = child.id))
            tx.insert(preschoolTerm2020)
            val clientId =
                tx.insertMcpClient(
                    "Test",
                    null,
                    null,
                    null,
                    listOf(CALLBACK),
                    "none",
                    null,
                    null,
                    now,
                )
            tx.insertMcpAuthorization(
                clientId,
                admin.id,
                McpServerConfig.MCP_SCOPE,
                now.plusDays(1),
                "code",
                now,
                "challenge",
                CALLBACK,
                null,
                now,
            )
        }
        MockPersonDetailsService.addPersons(guardian, child)
        MockPersonDetailsService.addDependants(guardian, child)
    }

    @Test
    fun `rejecting preschool decisions also handles the connected daycare decision`() {
        val applicationId = sendPreschoolDecisions(preparatory = false)

        advance(applicationId, "REJECT_DECISIONS")

        assertEquals(
            mapOf(
                DecisionType.PRESCHOOL to DecisionStatus.REJECTED,
                DecisionType.PRESCHOOL_DAYCARE to DecisionStatus.REJECTED,
            ),
            decisionStatuses(applicationId),
        )
    }

    @Test
    fun `accepting preparatory decisions accepts the primary decision first`() {
        val applicationId = sendPreschoolDecisions(preparatory = true)

        advance(applicationId, "ACCEPT_DECISIONS")

        assertEquals(
            mapOf(
                DecisionType.PREPARATORY_EDUCATION to DecisionStatus.ACCEPTED,
                DecisionType.PRESCHOOL_DAYCARE to DecisionStatus.ACCEPTED,
            ),
            decisionStatuses(applicationId),
        )
    }

    @Test
    fun `the remainder of a split placement is not tracked as test data`() {
        val existingPlacement =
            DevPlacement(
                childId = child.id,
                unitId = otherUnit.id,
                startDate = LocalDate.of(2020, 1, 1),
                endDate = LocalDate.of(2022, 12, 31),
            )
        val applicationId = db.transaction { tx ->
            tx.insert(existingPlacement)
            insertApplication(
                tx,
                ApplicationType.DAYCARE,
                ApplicationStatus.SENT,
                preferredStartDate = LocalDate.of(2020, 8, 1),
            )
        }

        advance(applicationId, "MOVE_TO_WAITING_PLACEMENT")
        advance(applicationId, "CREATE_PLACEMENT_PLAN")
        advance(applicationId, "SEND_DECISIONS_WITHOUT_PROPOSAL")
        advance(applicationId, "ACCEPT_DECISIONS")

        val placements = db.read { tx ->
            tx.createQuery {
                    sql(
                        """
SELECT id, source_application_id IS NOT DISTINCT FROM ${bind(applicationId)} AS from_application
FROM placement
WHERE child_id = ${bind(child.id)} AND id != ${bind(existingPlacement.id)}
"""
                    )
                }
                .toMap { column<PlacementId>("id") to column<Boolean>("from_application") }
        }
        assertEquals(listOf(false, true), placements.values.sorted(), "remainder + new placement")
        assertEquals(
            placements.filterValues { it }.keys.map { it.raw }.toSet(),
            trackedIds("placement"),
        )
    }

    /** A preschool application with connected daycare, waiting for the guardian's acceptance */
    private fun sendPreschoolDecisions(preparatory: Boolean): ApplicationId {
        val applicationId = db.transaction { tx ->
            val applicationId =
                insertApplication(
                    tx,
                    ApplicationType.PRESCHOOL,
                    ApplicationStatus.WAITING_PLACEMENT,
                    preferredStartDate = preschoolPeriod.start,
                    preparatory = preparatory,
                )
            // The tool cannot create a placement plan with a connected daycare period, so the plan
            // is made like in the UI
            applicationStateService.setVerified(
                tx,
                admin.user,
                clock,
                AuditContext(),
                applicationId,
                confidential = false,
            )
            applicationStateService.createPlacementPlan(
                tx,
                admin.user,
                clock,
                AuditContext(),
                applicationId,
                DaycarePlacementPlan(
                    unitId = unit.id,
                    period = preschoolPeriod,
                    preschoolDaycarePeriod = connectedDaycarePeriod,
                ),
            )
            applicationId
        }
        advance(applicationId, "SEND_DECISIONS_WITHOUT_PROPOSAL")
        assertEquals(
            setOf(DecisionStatus.PENDING),
            decisionStatuses(applicationId).values.toSet(),
        )
        return applicationId
    }

    private fun insertApplication(
        tx: Database.Transaction,
        type: ApplicationType,
        status: ApplicationStatus,
        preferredStartDate: LocalDate,
        preparatory: Boolean = false,
    ): ApplicationId {
        val preschool = type == ApplicationType.PRESCHOOL
        val applicationId =
            tx.insertTestApplication(
                type = type,
                status = status,
                guardianId = guardian.id,
                childId = child.id,
                document =
                    DaycareFormV0(
                        type = type,
                        child = Child(dateOfBirth = child.dateOfBirth),
                        guardian = Adult(),
                        apply = Apply(preferredUnits = listOf(unit.id)),
                        preferredStartDate = preferredStartDate,
                        connectedDaycare = true.takeIf { preschool },
                        serviceStart = "09:00",
                        serviceEnd = "14:00",
                        careDetails = CareDetails(preparatory = preparatory.takeIf { preschool }),
                    ),
            )
        val batchId = McpTestDataService.getOrCreateBatch(tx, "demo", admin.evakaUserId, null, now)
        tx.insertMcpTestDataEntities(batchId, "application", listOf(applicationId.raw), "", now)
        return applicationId
    }

    private fun advance(applicationId: ApplicationId, action: String) = db.transaction { tx ->
        val ctx = McpToolContext(tx, admin.user, clock, authorizationId, config, AuditContext())
        tools
            .findTool("advance_application")!!
            .call(
                ctx,
                jsonMapper.readTree("""{"applicationId":"$applicationId","action":"$action"}"""),
                jsonMapper,
            )
    }

    private fun decisionStatuses(applicationId: ApplicationId) = db.read { tx ->
        tx.getDecisionsByApplication(applicationId, AccessControlFilter.PermitAll).associate {
            it.type to it.status
        }
    }

    private fun trackedIds(table: String) = db.read { tx ->
        tx.createQuery {
                sql("SELECT entity_id FROM mcp_test_data_entity WHERE table_name = ${bind(table)}")
            }
            .toSet<UUID>()
    }

    companion object {
        private const val CALLBACK = "http://localhost/callback"
    }
}
