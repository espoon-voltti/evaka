// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.decision

import com.github.kittinunf.fuel.core.extensions.jsonBody
import com.github.kittinunf.fuel.core.isSuccessful
import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.application.AcceptDecisionRequest
import fi.espoo.evaka.application.ApplicationStatus
import fi.espoo.evaka.application.RejectDecisionRequest
import fi.espoo.evaka.application.persistence.daycare.Apply
import fi.espoo.evaka.application.persistence.daycare.CareDetails
import fi.espoo.evaka.application.persistence.daycare.DaycareFormV0
import fi.espoo.evaka.feeThresholds
import fi.espoo.evaka.insertGeneralTestFixtures
import fi.espoo.evaka.placement.PlacementType
import fi.espoo.evaka.preschoolTerm2020
import fi.espoo.evaka.shared.ApplicationId
import fi.espoo.evaka.shared.DecisionId
import fi.espoo.evaka.shared.EvakaUserId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.CitizenAuthLevel
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.auth.asUser
import fi.espoo.evaka.shared.dev.DevDaycare
import fi.espoo.evaka.shared.dev.DevPerson
import fi.espoo.evaka.shared.dev.DevPersonType
import fi.espoo.evaka.shared.dev.TestDecision
import fi.espoo.evaka.shared.dev.insert
import fi.espoo.evaka.shared.dev.insertTestApplication
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
import fi.espoo.evaka.toApplicationType
import fi.espoo.evaka.toDaycareFormAdult
import fi.espoo.evaka.toDaycareFormChild
import java.time.LocalDate
import java.util.UUID
import java.util.stream.Stream
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource

data class DecisionResolutionTestCase(val isServiceWorker: Boolean, val isAccept: Boolean)

class DecisionResolutionIntegrationTest : FullApplicationTest(resetDbBeforeEach = true) {
    private val serviceWorker =
        AuthenticatedUser.Employee(testDecisionMaker_1.id, setOf(UserRole.SERVICE_WORKER))
    private val endUser = AuthenticatedUser.Citizen(testAdult_1.id, CitizenAuthLevel.STRONG)
    private val applicationId = ApplicationId(UUID.randomUUID())

    @BeforeEach
    fun beforeEach() {
        db.transaction { tx ->
            tx.insertGeneralTestFixtures()
            tx.insert(testAdult_1, DevPersonType.ADULT)
            tx.insert(testChild_1, DevPersonType.CHILD)
            tx.insert(feeThresholds)
            tx.insert(preschoolTerm2020)
        }
    }

    @Suppress("unused")
    fun testCases(): Stream<Arguments> =
        listOf(
                Arguments.of(DecisionResolutionTestCase(isServiceWorker = true, isAccept = true)),
                Arguments.of(DecisionResolutionTestCase(isServiceWorker = true, isAccept = false)),
                Arguments.of(DecisionResolutionTestCase(isServiceWorker = false, isAccept = true)),
                Arguments.of(DecisionResolutionTestCase(isServiceWorker = false, isAccept = false))
            )
            .stream()

    @ParameterizedTest(name = "{0}")
    @MethodSource("testCases")
    fun testDaycareFullTime(test: DecisionResolutionTestCase) {
        val period = FiniteDateRange(LocalDate.of(2019, 5, 1), LocalDate.of(2019, 7, 1))
        val ids =
            insertInitialData(
                status = ApplicationStatus.WAITING_CONFIRMATION,
                type = PlacementType.DAYCARE,
                period = period
            )
        val user = if (test.isServiceWorker) serviceWorker else endUser
        if (test.isAccept) {
            acceptDecisionAndAssert(user, applicationId, ids.primaryId!!, period.start)
            db.read { r ->
                assertEquals(ApplicationStatus.ACTIVE, r.getApplicationStatus(ids.applicationId))
                r.getPlacementRowsByChild(testChild_1.id).exactlyOne().also {
                    assertEquals(PlacementType.DAYCARE, it.type)
                    assertEquals(testDaycare.id, it.unitId)
                    assertEquals(period, it.period())
                }
            }
        } else {
            rejectDecisionAndAssert(user, applicationId, ids.primaryId!!)
            db.read { r ->
                assertEquals(ApplicationStatus.REJECTED, r.getApplicationStatus(ids.applicationId))
                assertTrue(r.getPlacementRowsByChild(testChild_1.id).toList().isEmpty())
            }
        }
        db.read { r -> assertTrue(r.getPlacementPlanRowByApplication(ids.applicationId).deleted) }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("testCases")
    fun testDaycarePartTime(test: DecisionResolutionTestCase) {
        val period = FiniteDateRange(LocalDate.of(2019, 5, 18), LocalDate.of(2019, 7, 1))
        val ids =
            insertInitialData(
                status = ApplicationStatus.WAITING_CONFIRMATION,
                type = PlacementType.DAYCARE_PART_TIME,
                period = period
            )
        val user = if (test.isServiceWorker) serviceWorker else endUser
        if (test.isAccept) {
            acceptDecisionAndAssert(user, applicationId, ids.primaryId!!, period.start)
            db.read { r ->
                assertEquals(ApplicationStatus.ACTIVE, r.getApplicationStatus(ids.applicationId))
                r.getPlacementRowsByChild(testChild_1.id).exactlyOne().also {
                    assertEquals(PlacementType.DAYCARE_PART_TIME, it.type)
                    assertEquals(testDaycare.id, it.unitId)
                    assertEquals(period, it.period())
                }
            }
        } else {
            rejectDecisionAndAssert(user, applicationId, ids.primaryId!!)
            db.read {
                assertEquals(ApplicationStatus.REJECTED, it.getApplicationStatus(ids.applicationId))
                assertTrue(it.getPlacementRowsByChild(testChild_1.id).toList().isEmpty())
            }
        }
        db.read { assertTrue(it.getPlacementPlanRowByApplication(ids.applicationId).deleted) }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("testCases")
    fun testPreschoolOnly(test: DecisionResolutionTestCase) {
        val period = FiniteDateRange(LocalDate.of(2020, 8, 15), LocalDate.of(2021, 5, 31))
        val ids =
            insertInitialData(
                status = ApplicationStatus.WAITING_CONFIRMATION,
                type = PlacementType.PRESCHOOL,
                period = period
            )
        val user = if (test.isServiceWorker) serviceWorker else endUser
        if (test.isAccept) {
            acceptDecisionAndAssert(user, applicationId, ids.primaryId!!, period.start)
            db.read { r ->
                assertEquals(ApplicationStatus.ACTIVE, r.getApplicationStatus(ids.applicationId))
                r.getPlacementRowsByChild(testChild_1.id).exactlyOne().also {
                    assertEquals(PlacementType.PRESCHOOL, it.type)
                    assertEquals(testDaycare.id, it.unitId)
                    assertEquals(period, it.period())
                }
            }
        } else {
            rejectDecisionAndAssert(user, applicationId, ids.primaryId!!)
            db.read {
                assertEquals(ApplicationStatus.REJECTED, it.getApplicationStatus(ids.applicationId))
                assertTrue(it.getPlacementRowsByChild(testChild_1.id).toList().isEmpty())
            }
        }
        db.read { assertTrue(it.getPlacementPlanRowByApplication(ids.applicationId).deleted) }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("testCases")
    fun testPreschoolFull(test: DecisionResolutionTestCase) {
        val period = FiniteDateRange(LocalDate.of(2020, 8, 15), LocalDate.of(2021, 6, 4))
        val preschoolDaycarePeriod =
            FiniteDateRange(LocalDate.of(2020, 8, 1), LocalDate.of(2021, 7, 31))
        val ids =
            insertInitialData(
                status = ApplicationStatus.WAITING_CONFIRMATION,
                type = PlacementType.PRESCHOOL_DAYCARE,
                period = period,
                preschoolDaycarePeriod = preschoolDaycarePeriod
            )
        val user = if (test.isServiceWorker) serviceWorker else endUser
        if (test.isAccept) {
            acceptDecisionAndAssert(user, applicationId, ids.primaryId!!, period.start)
            db.read { r ->
                assertEquals(ApplicationStatus.ACTIVE, r.getApplicationStatus(ids.applicationId))
                r.getPlacementRowsByChild(testChild_1.id).exactlyOne().also {
                    assertEquals(PlacementType.PRESCHOOL, it.type)
                    assertEquals(testDaycare.id, it.unitId)
                    assertEquals(period, it.period())
                }
            }
            acceptDecisionAndAssert(
                user,
                applicationId,
                ids.preschoolDaycareId!!,
                preschoolDaycarePeriod.start
            )
            db.read { r ->
                r.getPlacementRowsByChild(testChild_1.id).toList().also {
                    assertEquals(2, it.size)
                    assertEquals(PlacementType.PRESCHOOL_DAYCARE, it[0].type)
                    assertEquals(testDaycare.id, it[0].unitId)
                    assertEquals(
                        FiniteDateRange(preschoolDaycarePeriod.start, period.end),
                        it[0].period()
                    )
                    assertEquals(PlacementType.DAYCARE, it[1].type)
                    assertEquals(testDaycare.id, it[1].unitId)
                    assertEquals(
                        FiniteDateRange(period.end.plusDays(1), preschoolDaycarePeriod.end),
                        it[1].period()
                    )
                }
                assertTrue(r.getPlacementPlanRowByApplication(ids.applicationId).deleted)
            }
        } else {
            rejectDecisionAndAssert(user, applicationId, ids.primaryId!!)
            db.read {
                assertEquals(ApplicationStatus.REJECTED, it.getApplicationStatus(ids.applicationId))
                assertTrue(it.getPlacementRowsByChild(testChild_1.id).toList().isEmpty())
                assertTrue(
                    it.getDecisionRowsByApplication(ids.applicationId).toList().all {
                        it.status == DecisionStatus.REJECTED
                    }
                )
            }
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("testCases")
    fun testPreparatoryFull(test: DecisionResolutionTestCase) {
        val period = FiniteDateRange(LocalDate.of(2020, 8, 15), LocalDate.of(2021, 6, 4))
        val preschoolDaycarePeriod =
            FiniteDateRange(LocalDate.of(2020, 8, 1), LocalDate.of(2021, 7, 31))
        val ids =
            insertInitialData(
                status = ApplicationStatus.WAITING_CONFIRMATION,
                type = PlacementType.PREPARATORY_DAYCARE,
                period = period,
                preschoolDaycarePeriod = preschoolDaycarePeriod
            )
        val user = if (test.isServiceWorker) serviceWorker else endUser
        if (test.isAccept) {
            acceptDecisionAndAssert(user, applicationId, ids.primaryId!!, period.start)
            db.read { r ->
                assertEquals(ApplicationStatus.ACTIVE, r.getApplicationStatus(ids.applicationId))
                r.getPlacementRowsByChild(testChild_1.id).exactlyOne().also {
                    assertEquals(PlacementType.PREPARATORY, it.type)
                    assertEquals(testDaycare.id, it.unitId)
                    assertEquals(period, it.period())
                }
            }
            acceptDecisionAndAssert(
                user,
                applicationId,
                ids.preschoolDaycareId!!,
                preschoolDaycarePeriod.start
            )
            db.read { r ->
                r.getPlacementRowsByChild(testChild_1.id).toList().also {
                    assertEquals(2, it.size)
                    assertEquals(PlacementType.PREPARATORY_DAYCARE, it[0].type)
                    assertEquals(testDaycare.id, it[0].unitId)
                    assertEquals(
                        FiniteDateRange(preschoolDaycarePeriod.start, period.end),
                        it[0].period()
                    )
                    assertEquals(PlacementType.DAYCARE, it[1].type)
                    assertEquals(testDaycare.id, it[1].unitId)
                    assertEquals(
                        FiniteDateRange(period.end.plusDays(1), preschoolDaycarePeriod.end),
                        it[1].period()
                    )
                }
            }
        } else {
            rejectDecisionAndAssert(user, applicationId, ids.primaryId!!)
            db.read { r ->
                assertEquals(ApplicationStatus.REJECTED, r.getApplicationStatus(ids.applicationId))
                assertTrue(r.getPlacementRowsByChild(testChild_1.id).toList().isEmpty())
                assertTrue(
                    r.getDecisionRowsByApplication(ids.applicationId).toList().all {
                        it.status == DecisionStatus.REJECTED
                    }
                )
            }
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("testCases")
    fun testPreschoolOnlyDaycare(test: DecisionResolutionTestCase) {
        val period = FiniteDateRange(LocalDate.of(2020, 8, 15), LocalDate.of(2021, 6, 4))
        val preschoolDaycarePeriod =
            FiniteDateRange(LocalDate.of(2020, 8, 1), LocalDate.of(2021, 7, 31))
        val ids =
            insertInitialData(
                status = ApplicationStatus.WAITING_CONFIRMATION,
                type = PlacementType.PRESCHOOL_DAYCARE,
                period = period,
                preschoolDaycarePeriod = preschoolDaycarePeriod,
                preschoolDaycareWithoutPreschool = true
            )
        val user = if (test.isServiceWorker) serviceWorker else endUser
        if (test.isAccept) {
            acceptDecisionAndAssert(
                user,
                applicationId,
                ids.preschoolDaycareId!!,
                preschoolDaycarePeriod.start
            )
            db.read { r ->
                assertEquals(ApplicationStatus.ACTIVE, r.getApplicationStatus(ids.applicationId))
                r.getPlacementRowsByChild(testChild_1.id).toList().also {
                    assertEquals(PlacementType.PRESCHOOL_DAYCARE, it[0].type)
                    assertEquals(testDaycare.id, it[0].unitId)
                    assertEquals(preschoolDaycarePeriod.copy(end = period.end), it[0].period())
                    assertEquals(PlacementType.DAYCARE, it[1].type)
                    assertEquals(testDaycare.id, it[1].unitId)
                    assertEquals(
                        preschoolDaycarePeriod.copy(start = period.end.plusDays(1)),
                        it[1].period()
                    )
                }
            }
        } else {
            rejectDecisionAndAssert(user, applicationId, ids.preschoolDaycareId!!)
            db.read { r ->
                assertEquals(ApplicationStatus.REJECTED, r.getApplicationStatus(ids.applicationId))
                assertTrue(r.getPlacementRowsByChild(testChild_1.id).toList().isEmpty())
                assertTrue(
                    r.getDecisionRowsByApplication(ids.applicationId).toList().all {
                        it.status == DecisionStatus.REJECTED
                    }
                )
            }
        }
    }

    private fun acceptDecisionAndAssert(
        user: AuthenticatedUser,
        applicationId: ApplicationId,
        decisionId: DecisionId,
        requestedStartDate: LocalDate
    ) {
        val path =
            "${if (user is AuthenticatedUser.Citizen) "/citizen" else "/v2"}/applications/$applicationId/actions/accept-decision"

        val (_, res, _) =
            http
                .post(path)
                .jsonBody(
                    jsonMapper.writeValueAsString(
                        AcceptDecisionRequest(decisionId, requestedStartDate)
                    )
                )
                .asUser(user)
                .response()
        assertTrue(res.isSuccessful)

        db.read { r ->
            r.getDecisionRowById(decisionId).also {
                assertEquals(DecisionStatus.ACCEPTED, it.status)
                assertNotNull(it.resolved)
                assertEquals(requestedStartDate, it.requestedStartDate)
                assertEquals(user.evakaUserId, it.resolvedBy)
            }
        }
    }

    private fun rejectDecisionAndAssert(
        user: AuthenticatedUser,
        applicationId: ApplicationId,
        decisionId: DecisionId
    ) {
        val path =
            "${if (user is AuthenticatedUser.Citizen) "/citizen" else "/v2"}/applications/$applicationId/actions/reject-decision"

        val (_, res, _) =
            http
                .post(path)
                .jsonBody(jsonMapper.writeValueAsString(RejectDecisionRequest(decisionId)))
                .asUser(user)
                .response()
        assertTrue(res.isSuccessful)

        db.read { r ->
            r.getDecisionRowById(decisionId).also {
                assertEquals(DecisionStatus.REJECTED, it.status)
                assertNotNull(it.resolved)
                assertNull(it.requestedStartDate)
                assertEquals(user.evakaUserId, it.resolvedBy)
            }
        }
    }

    private fun insertInitialData(
        status: ApplicationStatus,
        type: PlacementType,
        unit: DevDaycare = testDaycare,
        adult: DevPerson = testAdult_1,
        child: DevPerson = testChild_1,
        period: FiniteDateRange,
        preschoolDaycarePeriod: FiniteDateRange? = null,
        preschoolDaycareWithoutPreschool: Boolean = false
    ): DataIdentifiers =
        db.transaction { tx ->
            val preschoolDaycare =
                type in listOf(PlacementType.PRESCHOOL_DAYCARE, PlacementType.PREPARATORY_DAYCARE)
            tx.insertTestApplication(
                id = applicationId,
                status = status,
                guardianId = adult.id,
                childId = child.id,
                type = type.toApplicationType(),
                document =
                    DaycareFormV0(
                        type = type.toApplicationType(),
                        partTime = type == PlacementType.DAYCARE_PART_TIME,
                        connectedDaycare = preschoolDaycare,
                        serviceStart = "08:00".takeIf { preschoolDaycare },
                        serviceEnd = "16:00".takeIf { preschoolDaycare },
                        careDetails =
                            CareDetails(
                                preparatory =
                                    type in
                                        listOf(
                                            PlacementType.PREPARATORY,
                                            PlacementType.PREPARATORY_DAYCARE
                                        )
                            ),
                        child = child.toDaycareFormChild(),
                        guardian = adult.toDaycareFormAdult(),
                        apply = Apply(preferredUnits = listOf(unit.id)),
                        preferredStartDate = period.start
                    )
            )
            tx.insertTestPlacementPlan(
                applicationId = applicationId,
                unitId = unit.id,
                type = type,
                startDate = period.start,
                endDate = period.end,
                preschoolDaycareStartDate = preschoolDaycarePeriod?.start,
                preschoolDaycareEndDate = preschoolDaycarePeriod?.end
            )
            val primaryId =
                if (preschoolDaycareWithoutPreschool) {
                    null
                } else {
                    tx.insertTestDecision(
                        TestDecision(
                            createdBy = EvakaUserId(testDecisionMaker_1.id.raw),
                            unitId = unit.id,
                            applicationId = applicationId,
                            type =
                                when (type) {
                                    PlacementType.CLUB -> DecisionType.CLUB
                                    PlacementType.DAYCARE,
                                    PlacementType.DAYCARE_FIVE_YEAR_OLDS,
                                    PlacementType.PRESCHOOL_DAYCARE_ONLY,
                                    PlacementType.PREPARATORY_DAYCARE_ONLY -> DecisionType.DAYCARE
                                    PlacementType.DAYCARE_PART_TIME,
                                    PlacementType.DAYCARE_PART_TIME_FIVE_YEAR_OLDS ->
                                        DecisionType.DAYCARE_PART_TIME
                                    PlacementType.PRESCHOOL,
                                    PlacementType.PRESCHOOL_DAYCARE,
                                    PlacementType.PRESCHOOL_CLUB -> DecisionType.PRESCHOOL
                                    PlacementType.PREPARATORY,
                                    PlacementType.PREPARATORY_DAYCARE ->
                                        DecisionType.PREPARATORY_EDUCATION
                                    PlacementType.TEMPORARY_DAYCARE,
                                    PlacementType.TEMPORARY_DAYCARE_PART_DAY,
                                    PlacementType.SCHOOL_SHIFT_CARE ->
                                        error("Unsupported placement type ($type)")
                                },
                            startDate = period.start,
                            endDate = period.end
                        )
                    )
                }
            val preschoolDaycareId =
                preschoolDaycarePeriod?.let {
                    tx.insertTestDecision(
                        TestDecision(
                            createdBy = EvakaUserId(testDecisionMaker_1.id.raw),
                            unitId = unit.id,
                            applicationId = applicationId,
                            type = DecisionType.PRESCHOOL_DAYCARE,
                            startDate = it.start,
                            endDate = it.end
                        )
                    )
                }
            DataIdentifiers(applicationId, primaryId, preschoolDaycareId)
        }
}

private data class DataIdentifiers(
    val applicationId: ApplicationId,
    val primaryId: DecisionId?,
    val preschoolDaycareId: DecisionId?
)
