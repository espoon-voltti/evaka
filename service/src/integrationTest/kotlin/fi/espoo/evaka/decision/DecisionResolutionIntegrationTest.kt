// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.decision

import com.github.kittinunf.fuel.core.extensions.jsonBody
import com.github.kittinunf.fuel.core.isClientError
import com.github.kittinunf.fuel.core.isSuccessful
import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.application.AcceptDecisionRequest
import fi.espoo.evaka.application.ApplicationStatus
import fi.espoo.evaka.application.RejectDecisionRequest
import fi.espoo.evaka.application.persistence.daycare.Apply
import fi.espoo.evaka.application.persistence.daycare.CareDetails
import fi.espoo.evaka.application.persistence.daycare.DaycareFormV0
import fi.espoo.evaka.insertGeneralTestFixtures
import fi.espoo.evaka.invoicing.domain.PersonData
import fi.espoo.evaka.invoicing.domain.UnitData
import fi.espoo.evaka.placement.PlacementType
import fi.espoo.evaka.resetDatabase
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.auth.asUser
import fi.espoo.evaka.shared.db.handle
import fi.espoo.evaka.shared.dev.TestDecision
import fi.espoo.evaka.shared.dev.insertTestApplication
import fi.espoo.evaka.shared.dev.insertTestApplicationForm
import fi.espoo.evaka.shared.dev.insertTestDecision
import fi.espoo.evaka.shared.dev.insertTestPlacementPlan
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.test.getApplicationStatus
import fi.espoo.evaka.test.getDecisionRowById
import fi.espoo.evaka.test.getDecisionRowsByApplication
import fi.espoo.evaka.test.getPlacementPlanRowByApplication
import fi.espoo.evaka.test.getPlacementRowsByChild
import fi.espoo.evaka.testAdult_1
import fi.espoo.evaka.testChild_1
import fi.espoo.evaka.testDaycare
import fi.espoo.evaka.testDecisionMaker_1
import fi.espoo.evaka.toDaycareFormAdult
import fi.espoo.evaka.toDaycareFormChild
import fi.espoo.evaka.toFormType
import org.jdbi.v3.core.Handle
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.time.LocalDate
import java.util.UUID
import java.util.stream.Stream

data class DecisionResolutionTestCase(val isServiceWorker: Boolean, val isAccept: Boolean)

class DecisionResolutionIntegrationTest : FullApplicationTest() {
    private val serviceWorker = AuthenticatedUser(testDecisionMaker_1.id, setOf(UserRole.SERVICE_WORKER))
    private val endUser = AuthenticatedUser(testAdult_1.id, setOf(UserRole.END_USER))
    private val applicationId = UUID.randomUUID()

    @BeforeEach
    private fun beforeEach() {
        jdbi.handle { h ->
            resetDatabase(h)
            insertGeneralTestFixtures(h)
        }
    }

    @Suppress("unused")
    fun testCases(): Stream<Arguments> = listOf(
        Arguments.of(DecisionResolutionTestCase(isServiceWorker = true, isAccept = true)),
        Arguments.of(DecisionResolutionTestCase(isServiceWorker = true, isAccept = false)),
        Arguments.of(DecisionResolutionTestCase(isServiceWorker = false, isAccept = true)),
        Arguments.of(DecisionResolutionTestCase(isServiceWorker = false, isAccept = false))
    ).stream()

    @ParameterizedTest(name = "{0}")
    @MethodSource("testCases")
    fun testDaycareFullTime(test: DecisionResolutionTestCase): Unit = jdbi.handle { h ->
        val period = FiniteDateRange(
            LocalDate.of(2019, 5, 1),
            LocalDate.of(2019, 7, 1)
        )
        val ids = insertInitialData(
            h,
            status = ApplicationStatus.WAITING_CONFIRMATION,
            type = PlacementType.DAYCARE,
            period = period
        )
        val user = if (test.isServiceWorker) serviceWorker else endUser
        if (test.isAccept) {
            acceptDecisionAndAssert(h, user, applicationId, ids.primaryId!!, period.start)
            assertEquals(ApplicationStatus.ACTIVE, getApplicationStatus(h, ids.applicationId))
            getPlacementRowsByChild(h, testChild_1.id).one().also {
                assertEquals(PlacementType.DAYCARE, it.type)
                assertEquals(testDaycare.id, it.unitId)
                assertEquals(period, it.period())
            }
        } else {
            rejectDecisionAndAssert(h, user, applicationId, ids.primaryId!!)
            assertEquals(ApplicationStatus.REJECTED, getApplicationStatus(h, ids.applicationId))
            assertTrue(getPlacementRowsByChild(h, testChild_1.id).list().isEmpty())
        }
        assertTrue(getPlacementPlanRowByApplication(h, ids.applicationId).one().deleted)
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("testCases")
    fun testDaycarePartTime(test: DecisionResolutionTestCase): Unit = jdbi.handle { h ->
        val period = FiniteDateRange(
            LocalDate.of(2019, 5, 18),
            LocalDate.of(2019, 7, 1)
        )
        val ids = insertInitialData(
            h,
            status = ApplicationStatus.WAITING_CONFIRMATION,
            type = PlacementType.DAYCARE_PART_TIME,
            period = period
        )
        val user = if (test.isServiceWorker) serviceWorker else endUser
        if (test.isAccept) {
            acceptDecisionAndAssert(h, user, applicationId, ids.primaryId!!, period.start)
            assertEquals(ApplicationStatus.ACTIVE, getApplicationStatus(h, ids.applicationId))
            getPlacementRowsByChild(h, testChild_1.id).one().also {
                assertEquals(PlacementType.DAYCARE_PART_TIME, it.type)
                assertEquals(testDaycare.id, it.unitId)
                assertEquals(period, it.period())
            }
        } else {
            rejectDecisionAndAssert(h, user, applicationId, ids.primaryId!!)
            assertEquals(ApplicationStatus.REJECTED, getApplicationStatus(h, ids.applicationId))
            assertTrue(getPlacementRowsByChild(h, testChild_1.id).list().isEmpty())
        }
        assertTrue(getPlacementPlanRowByApplication(h, ids.applicationId).one().deleted)
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("testCases")
    fun testPreschoolOnly(test: DecisionResolutionTestCase): Unit = jdbi.handle { h ->
        val period = FiniteDateRange(
            LocalDate.of(2020, 8, 15),
            LocalDate.of(2021, 5, 31)
        )
        val ids = insertInitialData(
            h,
            status = ApplicationStatus.WAITING_CONFIRMATION,
            type = PlacementType.PRESCHOOL,
            period = period
        )
        val user = if (test.isServiceWorker) serviceWorker else endUser
        if (test.isAccept) {
            acceptDecisionAndAssert(h, user, applicationId, ids.primaryId!!, period.start)
            assertEquals(ApplicationStatus.ACTIVE, getApplicationStatus(h, ids.applicationId))
            getPlacementRowsByChild(h, testChild_1.id).one().also {
                assertEquals(PlacementType.PRESCHOOL, it.type)
                assertEquals(testDaycare.id, it.unitId)
                assertEquals(period, it.period())
            }
        } else {
            rejectDecisionAndAssert(h, user, applicationId, ids.primaryId!!)
            assertEquals(ApplicationStatus.REJECTED, getApplicationStatus(h, ids.applicationId))
            assertTrue(getPlacementRowsByChild(h, testChild_1.id).list().isEmpty())
        }
        assertTrue(getPlacementPlanRowByApplication(h, ids.applicationId).one().deleted)
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("testCases")
    fun testPreschoolFull(test: DecisionResolutionTestCase): Unit = jdbi.handle { h ->
        val period = FiniteDateRange(
            LocalDate.of(2020, 8, 15),
            LocalDate.of(2021, 6, 4)
        )
        val preschoolDaycarePeriod = FiniteDateRange(
            LocalDate.of(2020, 8, 1),
            LocalDate.of(2021, 7, 31)
        )
        val ids = insertInitialData(
            h,
            status = ApplicationStatus.WAITING_CONFIRMATION,
            type = PlacementType.PRESCHOOL_DAYCARE,
            period = period,
            preschoolDaycarePeriod = preschoolDaycarePeriod
        )
        val user = if (test.isServiceWorker) serviceWorker else endUser
        if (test.isAccept) {
            acceptDecisionAndAssert(h, user, applicationId, ids.primaryId!!, period.start)
            assertEquals(ApplicationStatus.ACTIVE, getApplicationStatus(h, ids.applicationId))
            getPlacementRowsByChild(h, testChild_1.id).one().also {
                assertEquals(PlacementType.PRESCHOOL, it.type)
                assertEquals(testDaycare.id, it.unitId)
                assertEquals(period, it.period())
            }
            acceptDecisionAndAssert(h, user, applicationId, ids.preschoolDaycareId!!, preschoolDaycarePeriod.start)
            getPlacementRowsByChild(h, testChild_1.id).list().also {
                assertEquals(2, it.size)
                assertEquals(PlacementType.PRESCHOOL_DAYCARE, it[0].type)
                assertEquals(testDaycare.id, it[0].unitId)
                assertEquals(FiniteDateRange(preschoolDaycarePeriod.start, period.end), it[0].period())
                assertEquals(PlacementType.DAYCARE, it[1].type)
                assertEquals(testDaycare.id, it[1].unitId)
                assertEquals(FiniteDateRange(period.end.plusDays(1), preschoolDaycarePeriod.end), it[1].period())
            }
            assertTrue(getPlacementPlanRowByApplication(h, ids.applicationId).one().deleted)
        } else {
            rejectDecisionAndAssert(h, user, applicationId, ids.primaryId!!)
            assertEquals(ApplicationStatus.REJECTED, getApplicationStatus(h, ids.applicationId))
            assertTrue(getPlacementRowsByChild(h, testChild_1.id).list().isEmpty())
            assertTrue(getDecisionRowsByApplication(h, ids.applicationId).all { it.status == DecisionStatus.REJECTED })
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("testCases")
    fun testPreparatoryFull(test: DecisionResolutionTestCase): Unit = jdbi.handle { h ->
        val period = FiniteDateRange(
            LocalDate.of(2020, 8, 15),
            LocalDate.of(2021, 6, 4)
        )
        val preschoolDaycarePeriod = FiniteDateRange(
            LocalDate.of(2020, 8, 1),
            LocalDate.of(2021, 7, 31)
        )
        val ids = insertInitialData(
            h,
            status = ApplicationStatus.WAITING_CONFIRMATION,
            type = PlacementType.PREPARATORY_DAYCARE,
            period = period,
            preschoolDaycarePeriod = preschoolDaycarePeriod
        )
        val user = if (test.isServiceWorker) serviceWorker else endUser
        if (test.isAccept) {
            acceptDecisionAndAssert(h, user, applicationId, ids.primaryId!!, period.start)
            assertEquals(ApplicationStatus.ACTIVE, getApplicationStatus(h, ids.applicationId))
            getPlacementRowsByChild(h, testChild_1.id).one().also {
                assertEquals(PlacementType.PREPARATORY, it.type)
                assertEquals(testDaycare.id, it.unitId)
                assertEquals(period, it.period())
            }
            acceptDecisionAndAssert(h, user, applicationId, ids.preschoolDaycareId!!, preschoolDaycarePeriod.start)
            getPlacementRowsByChild(h, testChild_1.id).list().also {
                assertEquals(2, it.size)
                assertEquals(PlacementType.PREPARATORY_DAYCARE, it[0].type)
                assertEquals(testDaycare.id, it[0].unitId)
                assertEquals(FiniteDateRange(preschoolDaycarePeriod.start, period.end), it[0].period())
                assertEquals(PlacementType.DAYCARE, it[1].type)
                assertEquals(testDaycare.id, it[1].unitId)
                assertEquals(FiniteDateRange(period.end.plusDays(1), preschoolDaycarePeriod.end), it[1].period())
            }
        } else {
            rejectDecisionAndAssert(h, user, applicationId, ids.primaryId!!)
            assertEquals(ApplicationStatus.REJECTED, getApplicationStatus(h, ids.applicationId))
            assertTrue(getPlacementRowsByChild(h, testChild_1.id).list().isEmpty())
            assertTrue(getDecisionRowsByApplication(h, ids.applicationId).all { it.status == DecisionStatus.REJECTED })
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("testCases")
    fun testPreschoolOnlyDaycare(test: DecisionResolutionTestCase): Unit = jdbi.handle { h ->
        val period = FiniteDateRange(
            LocalDate.of(2020, 8, 15),
            LocalDate.of(2021, 6, 4)
        )
        val preschoolDaycarePeriod = FiniteDateRange(
            LocalDate.of(2020, 8, 1),
            LocalDate.of(2021, 7, 31)
        )
        val ids = insertInitialData(
            h,
            status = ApplicationStatus.WAITING_CONFIRMATION,
            type = PlacementType.PRESCHOOL_DAYCARE,
            period = period,
            preschoolDaycarePeriod = preschoolDaycarePeriod,
            preschoolDaycareWithoutPreschool = true
        )
        val user = if (test.isServiceWorker) serviceWorker else endUser
        if (test.isAccept) {
            acceptDecisionAndAssert(h, user, applicationId, ids.preschoolDaycareId!!, preschoolDaycarePeriod.start)
            assertEquals(ApplicationStatus.ACTIVE, getApplicationStatus(h, ids.applicationId))
            getPlacementRowsByChild(h, testChild_1.id).list().also {
                assertEquals(PlacementType.PRESCHOOL_DAYCARE, it[0].type)
                assertEquals(testDaycare.id, it[0].unitId)
                assertEquals(preschoolDaycarePeriod.copy(end = period.end), it[0].period())
                assertEquals(PlacementType.DAYCARE, it[1].type)
                assertEquals(testDaycare.id, it[1].unitId)
                assertEquals(preschoolDaycarePeriod.copy(start = period.end.plusDays(1)), it[1].period())
            }
        } else {
            rejectDecisionAndAssert(h, user, applicationId, ids.preschoolDaycareId!!)
            assertEquals(ApplicationStatus.REJECTED, getApplicationStatus(h, ids.applicationId))
            assertTrue(getPlacementRowsByChild(h, testChild_1.id).list().isEmpty())
            assertTrue(getDecisionRowsByApplication(h, ids.applicationId).all { it.status == DecisionStatus.REJECTED })
        }
    }

    @Test
    fun testRequestedStartDateValidation(): Unit = jdbi.handle { h ->
        val period = FiniteDateRange(
            LocalDate.of(2020, 8, 15),
            LocalDate.of(2021, 6, 4)
        )
        val preschoolDaycarePeriod = FiniteDateRange(
            LocalDate.of(2020, 8, 1),
            LocalDate.of(2021, 7, 31)
        )
        val ids = insertInitialData(
            h,
            status = ApplicationStatus.WAITING_CONFIRMATION,
            type = PlacementType.PRESCHOOL_DAYCARE,
            period = period,
            preschoolDaycarePeriod = preschoolDaycarePeriod
        )
        acceptDecisionAndAssert(h, serviceWorker, applicationId, ids.primaryId!!, period.start)

        val decisionId = ids.preschoolDaycareId!!

        listOf(
            preschoolDaycarePeriod.start.minusDays(1),
            preschoolDaycarePeriod.start.plusDays(15)
        ).forEach { invalidDate ->
            http.post("/decisions2/$decisionId/accept")
                .jsonBody(objectMapper.writeValueAsString(EnduserAcceptDecisionRequest(invalidDate)))
                .asUser(serviceWorker)
                .response()
                .also { (_, res, _) -> assertTrue(res.isClientError) }
        }

        acceptDecisionAndAssert(h, serviceWorker, applicationId, decisionId, preschoolDaycarePeriod.start.plusDays(14))
    }

    private fun acceptDecisionAndAssert(h: Handle, user: AuthenticatedUser, applicationId: UUID, decisionId: UUID, requestedStartDate: LocalDate) {
        val path = "${if (user.roles.contains(UserRole.END_USER)) "/citizen" else "/v2"}/applications/$applicationId/actions/accept-decision"

        val (_, res, _) = http.post(path)
            .jsonBody(objectMapper.writeValueAsString(AcceptDecisionRequest(decisionId, requestedStartDate)))
            .asUser(user)
            .response()
        assertTrue(res.isSuccessful)

        getDecisionRowById(h, decisionId).one().also {
            assertEquals(DecisionStatus.ACCEPTED, it.status)
            assertNotNull(it.resolved)
            assertEquals(requestedStartDate, it.requestedStartDate)
            assertEquals(user.id, it.resolvedBy)
        }
    }

    private fun rejectDecisionAndAssert(h: Handle, user: AuthenticatedUser, applicationId: UUID, decisionId: UUID) {
        val path = "${if (user.roles.contains(UserRole.END_USER)) "/citizen" else "/v2"}/applications/$applicationId/actions/reject-decision"

        val (_, res, _) = http.post(path)
            .jsonBody(objectMapper.writeValueAsString(RejectDecisionRequest(decisionId)))
            .asUser(user)
            .response()
        assertTrue(res.isSuccessful)

        getDecisionRowById(h, decisionId).one().also {
            assertEquals(DecisionStatus.REJECTED, it.status)
            assertNotNull(it.resolved)
            assertNull(it.requestedStartDate)
            assertEquals(user.id, it.resolvedBy)
        }
    }

    private fun insertInitialData(
        h: Handle,
        status: ApplicationStatus,
        type: PlacementType,
        unit: UnitData.Detailed = testDaycare,
        adult: PersonData.Detailed = testAdult_1,
        child: PersonData.Detailed = testChild_1,
        period: FiniteDateRange,
        preschoolDaycarePeriod: FiniteDateRange? = null,
        preschoolDaycareWithoutPreschool: Boolean = false
    ): DataIdentifiers {
        insertTestApplication(
            h,
            id = applicationId,
            status = status,
            guardianId = adult.id,
            childId = child.id
        )
        val preschoolDaycare = type in listOf(PlacementType.PRESCHOOL_DAYCARE, PlacementType.PREPARATORY_DAYCARE)
        insertTestApplicationForm(
            h, applicationId,
            DaycareFormV0(
                type = type.toFormType(),
                partTime = type == PlacementType.DAYCARE_PART_TIME,
                connectedDaycare = preschoolDaycare,
                serviceStart = "08:00".takeIf { preschoolDaycare },
                serviceEnd = "16:00".takeIf { preschoolDaycare },
                careDetails = CareDetails(preparatory = type in listOf(PlacementType.PREPARATORY, PlacementType.PREPARATORY_DAYCARE)),
                child = child.toDaycareFormChild(),
                guardian = adult.toDaycareFormAdult(),
                apply = Apply(preferredUnits = listOf(unit.id)),
                preferredStartDate = period.start
            )
        )
        insertTestPlacementPlan(
            h,
            applicationId = applicationId,
            unitId = unit.id,
            type = type,
            startDate = period.start,
            endDate = period.end,
            preschoolDaycareStartDate = preschoolDaycarePeriod?.start,
            preschoolDaycareEndDate = preschoolDaycarePeriod?.end
        )
        val primaryId =
            if (preschoolDaycareWithoutPreschool) null
            else h.insertTestDecision(
                TestDecision(
                    createdBy = testDecisionMaker_1.id,
                    unitId = unit.id,
                    applicationId = applicationId,
                    type = when (type) {
                        PlacementType.CLUB -> DecisionType.CLUB
                        PlacementType.DAYCARE -> DecisionType.DAYCARE
                        PlacementType.DAYCARE_PART_TIME -> DecisionType.DAYCARE_PART_TIME
                        PlacementType.PRESCHOOL, PlacementType.PRESCHOOL_DAYCARE -> DecisionType.PRESCHOOL
                        PlacementType.PREPARATORY, PlacementType.PREPARATORY_DAYCARE -> DecisionType.PREPARATORY_EDUCATION
                    },
                    startDate = period.start,
                    endDate = period.end
                )
            )
        val preschoolDaycareId = preschoolDaycarePeriod?.let {
            h.insertTestDecision(
                TestDecision(
                    createdBy = testDecisionMaker_1.id,
                    unitId = unit.id,
                    applicationId = applicationId,
                    type = DecisionType.PRESCHOOL_DAYCARE,
                    startDate = it.start,
                    endDate = it.end
                )
            )
        }
        return DataIdentifiers(applicationId, primaryId, preschoolDaycareId)
    }
}

private data class DataIdentifiers(
    val applicationId: UUID,
    val primaryId: UUID?,
    val preschoolDaycareId: UUID?
)
