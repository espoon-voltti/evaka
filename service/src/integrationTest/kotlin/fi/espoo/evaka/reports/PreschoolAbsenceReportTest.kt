// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.reports

import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.absence.AbsenceCategory
import fi.espoo.evaka.absence.AbsenceType
import fi.espoo.evaka.daycare.CareType
import fi.espoo.evaka.daycare.insertPreschoolTerm
import fi.espoo.evaka.placement.PlacementType
import fi.espoo.evaka.shared.AbsenceId
import fi.espoo.evaka.shared.AreaId
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.EmployeeId
import fi.espoo.evaka.shared.EvakaUserId
import fi.espoo.evaka.shared.GroupId
import fi.espoo.evaka.shared.GroupPlacementId
import fi.espoo.evaka.shared.PlacementId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.auth.insertDaycareAclRow
import fi.espoo.evaka.shared.data.DateSet
import fi.espoo.evaka.shared.dev.DevAbsence
import fi.espoo.evaka.shared.dev.DevCareArea
import fi.espoo.evaka.shared.dev.DevDaycare
import fi.espoo.evaka.shared.dev.DevDaycareGroup
import fi.espoo.evaka.shared.dev.DevDaycareGroupPlacement
import fi.espoo.evaka.shared.dev.DevEmployee
import fi.espoo.evaka.shared.dev.DevPerson
import fi.espoo.evaka.shared.dev.DevPersonType
import fi.espoo.evaka.shared.dev.DevPlacement
import fi.espoo.evaka.shared.dev.insert
import fi.espoo.evaka.shared.dev.insertTestChildAttendance
import fi.espoo.evaka.shared.domain.BadRequest
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.shared.domain.Forbidden
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.domain.MockEvakaClock
import fi.espoo.evaka.shared.domain.TimeRange
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired

internal class PreschoolAbsenceReportTest : FullApplicationTest(resetDbBeforeEach = true) {
    @Autowired private lateinit var preschoolAbsenceReport: PreschoolAbsenceReport

    private val admin = DevEmployee(roles = setOf(UserRole.ADMIN))
    private val adminLoginUser = AuthenticatedUser.Employee(admin.id, admin.roles)

    private val unitSupervisorA = DevEmployee(id = EmployeeId(UUID.randomUUID()), roles = setOf())
    private val unitSupervisorB = DevEmployee(id = EmployeeId(UUID.randomUUID()), roles = setOf())
    private val unitSupervisorC = DevEmployee(id = EmployeeId(UUID.randomUUID()), roles = setOf())

    // Monday
    private val monday: LocalDate = LocalDate.of(2022, 12, 12)
    private val tuesday: LocalDate = monday.plusDays(1)
    private val wednesday: LocalDate = monday.plusDays(2)
    private val thursday: LocalDate = monday.plusDays(3)
    private val friday: LocalDate = monday.plusDays(4)
    private val saturday: LocalDate = monday.plusDays(5)

    private val previousThursday: LocalDate = monday.minusDays(4)
    private val previousSunday: LocalDate = monday.minusDays(1)

    private val nextWednesday: LocalDate = wednesday.plusWeeks(1)

    private val holidayBefore: LocalDate = LocalDate.of(2022, 12, 6)
    private val holidayAfter: LocalDate = LocalDate.of(2023, 1, 6)

    private val mockClock = MockEvakaClock(HelsinkiDateTime.of(monday, LocalTime.of(12, 15)))

    @Test
    fun `Unit supervisor can see own unit's report results`() {
        val testData = initTestData()
        val results =
            preschoolAbsenceReport.getPreschoolAbsenceReport(
                dbInstance(),
                mockClock,
                unitSupervisorA.user,
                PreschoolAbsenceReport.PreschoolAbsenceReportBody(
                    term = testData.preschoolTerm,
                    areaId = null,
                    unitId = testData.daycareAId,
                    groupId = null,
                ),
            )

        assertThat(results.isNotEmpty())
    }

    @Test
    fun `Admin can see report results for unit`() {
        val testData = initTestData()
        val results =
            preschoolAbsenceReport.getPreschoolAbsenceReport(
                dbInstance(),
                mockClock,
                adminLoginUser,
                PreschoolAbsenceReport.PreschoolAbsenceReportBody(
                    term = testData.preschoolTerm,
                    areaId = null,
                    unitId = testData.daycareAId,
                    groupId = null,
                ),
            )
        assertThat(results.isNotEmpty())
    }

    @Test
    fun `Unit supervisor cannot see area report results`() {
        val testData = initTestData()
        assertThrows<Forbidden> {
            preschoolAbsenceReport.getPreschoolAbsenceReport(
                dbInstance(),
                mockClock,
                unitSupervisorA.user,
                PreschoolAbsenceReport.PreschoolAbsenceReportBody(
                    term = testData.preschoolTerm,
                    areaId = testData.areaAId,
                    unitId = null,
                    groupId = null,
                ),
            )
        }
    }

    @Test
    fun `Unit supervisor cannot see another unit's report results`() {
        val testData = initTestData()
        assertThrows<Forbidden> {
            preschoolAbsenceReport.getPreschoolAbsenceReport(
                dbInstance(),
                mockClock,
                unitSupervisorA.user,
                PreschoolAbsenceReport.PreschoolAbsenceReportBody(
                    term = testData.preschoolTerm,
                    areaId = null,
                    unitId = testData.daycareBId,
                    groupId = null,
                ),
            )
        }
    }

    @Test
    fun `Report returns correct absence hours for all groups`() {
        val testData = initTestData()

        val reportResults =
            preschoolAbsenceReport.getPreschoolAbsenceReport(
                dbInstance(),
                mockClock,
                adminLoginUser,
                PreschoolAbsenceReport.PreschoolAbsenceReportBody(
                    term = testData.preschoolTerm,
                    areaId = null,
                    unitId = testData.daycareAId,
                    groupId = null,
                ),
            )

        val (groupAExpectation, groupBExpectation, groupCExpectation) = getExpectedResults(testData)
        val childCResults =
            (groupAExpectation + groupBExpectation + groupCExpectation).filter {
                it.childId == testData.childC.id
            }
        val combinedResult =
            ChildPreschoolAbsenceRowWithUnitAndGroup(
                childCResults[0].childId,
                childCResults[0].firstName,
                childCResults[0].lastName,
                childCResults[0].placementType,
                "Preschool A",
                "Testiryhmä A",
                childCResults[0].hourlyTypeResults.toMutableMap().apply {
                    childCResults[1].hourlyTypeResults.forEach {
                        merge(it.key, it.value) { a, b -> a + b }
                    }
                },
            )
        assertThat(reportResults)
            .hasSameElementsAs(
                groupAExpectation.filter { it.childId != testData.childC.id } +
                    combinedResult +
                    groupCExpectation
            )
    }

    @Test
    fun `Report returns correct absence hours for just group B`() {
        val testData = initTestData()

        val reportResults =
            preschoolAbsenceReport.getPreschoolAbsenceReport(
                dbInstance(),
                mockClock,
                adminLoginUser,
                PreschoolAbsenceReport.PreschoolAbsenceReportBody(
                    term = testData.preschoolTerm,
                    areaId = null,
                    unitId = testData.daycareAId,
                    groupId = testData.groupBId,
                ),
            )

        val (_, groupBExpectation, _) = getExpectedResults(testData)

        assertThat(reportResults).hasSameElementsAs(groupBExpectation)
    }

    @Test
    fun `Report returns correct absence hours for preparatory placement type`() {
        val testData = initTestData()

        val reportResults =
            preschoolAbsenceReport.getPreschoolAbsenceReport(
                dbInstance(),
                mockClock,
                adminLoginUser,
                PreschoolAbsenceReport.PreschoolAbsenceReportBody(
                    term = testData.preschoolTerm,
                    areaId = null,
                    unitId = testData.daycareAId,
                    groupId = testData.groupCId,
                ),
            )

        val (_, _, groupCExpectation) = getExpectedResults(testData)

        assertThat(reportResults).hasSameElementsAs(groupCExpectation)
    }

    @Test
    fun `Report returns correct absence hours for area`() {
        val testData = initTestData()

        val reportResults =
            preschoolAbsenceReport.getPreschoolAbsenceReport(
                dbInstance(),
                mockClock,
                adminLoginUser,
                PreschoolAbsenceReport.PreschoolAbsenceReportBody(
                    term = testData.preschoolTerm,
                    areaId = testData.areaAId,
                    unitId = null,
                    groupId = null,
                ),
            )

        val (groupAExpectation, groupBExpectation, groupCExpectation) = getExpectedResults(testData)
        val childCResults =
            (groupAExpectation + groupBExpectation + groupCExpectation).filter {
                it.childId == testData.childC.id
            }
        val combinedResult =
            ChildPreschoolAbsenceRowWithUnitAndGroup(
                childCResults[0].childId,
                childCResults[0].firstName,
                childCResults[0].lastName,
                childCResults[0].placementType,
                "Preschool A",
                "Testiryhmä A",
                childCResults[0].hourlyTypeResults.toMutableMap().apply {
                    childCResults[1].hourlyTypeResults.forEach {
                        merge(it.key, it.value) { a, b -> a + b }
                    }
                },
            )
        assertThat(reportResults)
            .hasSameElementsAs(
                groupAExpectation.filter { it.childId != testData.childC.id } +
                    combinedResult +
                    groupCExpectation +
                    getExpectedGroupDResults(testData)
            )
    }

    @Test
    fun `fetching unit and group names should work and return the group of latest placement`() {
        val testData = initTestData()

        db.read { tx ->
            val unitAndGroupMap =
                getDaycareAndGroupForChildren(
                    tx,
                    listOf(
                        testData.childA.id,
                        testData.childB.id,
                        testData.childC.id,
                        testData.childD.id,
                    ),
                )

            assertThat(unitAndGroupMap).hasSize(4)
            assertThat(unitAndGroupMap[testData.childA.id]).isNotNull()
            assertThat(unitAndGroupMap[testData.childA.id]?.daycareName).isEqualTo("Preschool A")
            assertThat(unitAndGroupMap[testData.childA.id]?.groupName).isEqualTo("Testiryhmä A")
            assertThat(unitAndGroupMap[testData.childB.id]).isNotNull()
            assertThat(unitAndGroupMap[testData.childB.id]?.daycareName).isEqualTo("Preschool A")
            assertThat(unitAndGroupMap[testData.childB.id]?.groupName).isEqualTo("Testiryhmä A")
            assertThat(unitAndGroupMap[testData.childC.id]).isNotNull()
            assertThat(unitAndGroupMap[testData.childC.id]?.daycareName).isEqualTo("Preschool A")
            assertThat(unitAndGroupMap[testData.childC.id]?.groupName).isEqualTo("Testiryhmä A")
            assertThat(unitAndGroupMap[testData.childD.id]).isNotNull()
            assertThat(unitAndGroupMap[testData.childD.id]?.daycareName).isEqualTo("Preschool A")
            assertThat(unitAndGroupMap[testData.childD.id]?.groupName).isEqualTo("Testiryhmä C")
        }
    }

    @Test
    fun `should enfore choosing either area or unit`() {
        val testData = initTestData()

        assertThrows<BadRequest> {
            preschoolAbsenceReport.getPreschoolAbsenceReport(
                dbInstance(),
                mockClock,
                unitSupervisorA.user,
                PreschoolAbsenceReport.PreschoolAbsenceReportBody(
                    term = testData.preschoolTerm,
                    areaId = null,
                    unitId = null,
                    groupId = null,
                ),
            )
        }

        assertThrows<BadRequest> {
            preschoolAbsenceReport.getPreschoolAbsenceReport(
                dbInstance(),
                mockClock,
                unitSupervisorA.user,
                PreschoolAbsenceReport.PreschoolAbsenceReportBody(
                    term = testData.preschoolTerm,
                    areaId = testData.areaAId,
                    unitId = testData.daycareAId,
                    groupId = null,
                ),
            )
        }
    }

    private fun getExpectedResults(
        testData: PreschoolAbsenceReportTestData
    ): Triple<
        List<ChildPreschoolAbsenceRowWithUnitAndGroup>,
        List<ChildPreschoolAbsenceRowWithUnitAndGroup>,
        List<ChildPreschoolAbsenceRowWithUnitAndGroup>,
    > {
        val preschoolAbsenceTypes =
            AbsenceType.entries.filter {
                when (it) {
                    AbsenceType.OTHER_ABSENCE,
                    AbsenceType.SICKLEAVE,
                    AbsenceType.UNKNOWN_ABSENCE -> true

                    else -> false
                }
            }

        val groupAExpectation =
            listOf(
                ChildPreschoolAbsenceRowWithUnitAndGroup(
                    childId = testData.childA.id,
                    firstName = testData.childA.firstName,
                    lastName = testData.childA.lastName,
                    PlacementType.PRESCHOOL,
                    "Preschool A",
                    "Testiryhmä A",
                    hourlyTypeResults =
                        preschoolAbsenceTypes.associateWith { type ->
                            when (type) {
                                // 5h full day absence + arrived 1h late + left 1h early + arrived
                                // 45min late
                                AbsenceType.OTHER_ABSENCE -> 7

                                else -> 0
                            }
                        },
                ),
                ChildPreschoolAbsenceRowWithUnitAndGroup(
                    childId = testData.childB.id,
                    firstName = testData.childB.firstName,
                    lastName = testData.childB.lastName,
                    PlacementType.PRESCHOOL,
                    "Preschool A",
                    "Testiryhmä A",
                    hourlyTypeResults =
                        preschoolAbsenceTypes.associateWith { type ->
                            when (type) {
                                AbsenceType.SICKLEAVE -> 5
                                else -> 0
                            }
                        },
                ),
                ChildPreschoolAbsenceRowWithUnitAndGroup(
                    childId = testData.childC.id,
                    firstName = testData.childC.firstName,
                    lastName = testData.childC.lastName,
                    PlacementType.PRESCHOOL_DAYCARE,
                    "Preschool A",
                    "Testiryhmä A",
                    hourlyTypeResults =
                        // 5h full day absence + 3h early departure
                        preschoolAbsenceTypes.associateWith { type ->
                            when (type) {
                                AbsenceType.UNKNOWN_ABSENCE -> 5
                                AbsenceType.OTHER_ABSENCE -> 3
                                else -> 0
                            }
                        },
                ),
            )

        val groupBExpectation =
            listOf(
                ChildPreschoolAbsenceRowWithUnitAndGroup(
                    childId = testData.childC.id,
                    firstName = testData.childC.firstName,
                    lastName = testData.childC.lastName,
                    PlacementType.PRESCHOOL_DAYCARE,
                    "Preschool A",
                    "Testiryhmä A",
                    hourlyTypeResults =
                        // 5h full day absence + 1.5h late arrival
                        preschoolAbsenceTypes.associateWith { type ->
                            when (type) {
                                AbsenceType.UNKNOWN_ABSENCE -> 5
                                AbsenceType.OTHER_ABSENCE -> 1
                                else -> 0
                            }
                        },
                )
            )

        val groupCExpectation =
            listOf(
                ChildPreschoolAbsenceRowWithUnitAndGroup(
                    childId = testData.childD.id,
                    firstName = testData.childD.firstName,
                    lastName = testData.childD.lastName,
                    PlacementType.PREPARATORY,
                    "Preschool A",
                    "Testiryhmä C",
                    hourlyTypeResults =
                        // todo
                        preschoolAbsenceTypes.associateWith { type ->
                            when (type) {
                                AbsenceType.OTHER_ABSENCE -> 5
                                else -> 0
                            }
                        },
                )
            )
        return Triple(groupAExpectation, groupBExpectation, groupCExpectation)
    }

    private fun getExpectedGroupDResults(
        testData: PreschoolAbsenceReportTestData
    ): List<ChildPreschoolAbsenceRowWithUnitAndGroup> {
        val preschoolAbsenceTypes =
            AbsenceType.entries.filter {
                when (it) {
                    AbsenceType.OTHER_ABSENCE,
                    AbsenceType.SICKLEAVE,
                    AbsenceType.UNKNOWN_ABSENCE -> true

                    else -> false
                }
            }

        val groupDExpectation =
            listOf(
                ChildPreschoolAbsenceRowWithUnitAndGroup(
                    childId = testData.childE.id,
                    firstName = testData.childE.firstName,
                    lastName = testData.childE.lastName,
                    PlacementType.PRESCHOOL,
                    "Preschool C",
                    "Testiryhmä D",
                    hourlyTypeResults =
                        preschoolAbsenceTypes.associateWith { type ->
                            when (type) {
                                AbsenceType.OTHER_ABSENCE -> 5
                                else -> 0
                            }
                        },
                )
            )

        return groupDExpectation
    }

    private fun initTestData(): PreschoolAbsenceReportTestData {
        return db.transaction { tx ->
            tx.insert(admin)

            val testTerm = FiniteDateRange(monday.minusMonths(6), monday.plusMonths(6))
            tx.insertPreschoolTerm(
                testTerm,
                testTerm,
                testTerm,
                testTerm,
                DateSet.ofDates(previousThursday, nextWednesday),
            )

            val areaAId = tx.insert(DevCareArea(name = "Area A", shortName = "Area A"))
            val areaBId = tx.insert(DevCareArea(name = "Area B", shortName = "Area B"))

            val preschoolAId =
                tx.insert(
                    DevDaycare(
                        name = "Preschool A",
                        areaId = areaAId,
                        openingDate = monday.minusDays(7),
                        type = setOf(CareType.PRESCHOOL),
                        dailyPreschoolTime =
                            TimeRange(LocalTime.of(9, 0, 0), LocalTime.of(14, 0, 0)),
                        dailyPreparatoryTime =
                            TimeRange(LocalTime.of(10, 0, 0), LocalTime.of(15, 0, 0)),
                    )
                )
            val groupAId =
                tx.insert(
                    DevDaycareGroup(
                        GroupId(UUID.randomUUID()),
                        preschoolAId,
                        "Testiryhmä A",
                        startDate = monday.minusDays(7),
                    )
                )

            val groupBId =
                tx.insert(
                    DevDaycareGroup(
                        GroupId(UUID.randomUUID()),
                        preschoolAId,
                        "Testiryhmä B",
                        startDate = monday.minusDays(7),
                    )
                )

            val groupCId =
                tx.insert(
                    DevDaycareGroup(
                        GroupId(UUID.randomUUID()),
                        preschoolAId,
                        "Testiryhmä C",
                        startDate = monday.minusDays(7),
                    )
                )

            val preschoolBId =
                tx.insert(
                    DevDaycare(
                        name = "Preschool B",
                        areaId = areaBId,
                        openingDate = monday.minusDays(7),
                        type = setOf(CareType.PRESCHOOL),
                        dailyPreschoolTime =
                            TimeRange(LocalTime.of(9, 0, 0), LocalTime.of(13, 0, 0)),
                        dailyPreparatoryTime =
                            TimeRange(LocalTime.of(9, 0, 0), LocalTime.of(14, 0, 0)),
                    )
                )

            val preschoolCId =
                tx.insert(
                    DevDaycare(
                        name = "Preschool C",
                        areaId = areaAId,
                        openingDate = monday.minusDays(7),
                        type = setOf(CareType.PRESCHOOL),
                        dailyPreschoolTime =
                            TimeRange(LocalTime.of(9, 0, 0), LocalTime.of(14, 0, 0)),
                        dailyPreparatoryTime =
                            TimeRange(LocalTime.of(10, 0, 0), LocalTime.of(15, 0, 0)),
                    )
                )
            val groupDId =
                tx.insert(
                    DevDaycareGroup(
                        GroupId(UUID.randomUUID()),
                        preschoolCId,
                        "Testiryhmä D",
                        startDate = monday.minusDays(7),
                    )
                )
            tx.insert(unitSupervisorA)
            tx.insert(unitSupervisorB)
            tx.insert(unitSupervisorC)

            tx.insertDaycareAclRow(preschoolAId, unitSupervisorA.id, UserRole.UNIT_SUPERVISOR)
            tx.insertDaycareAclRow(preschoolBId, unitSupervisorB.id, UserRole.UNIT_SUPERVISOR)
            tx.insertDaycareAclRow(preschoolCId, unitSupervisorC.id, UserRole.UNIT_SUPERVISOR)

            val testChildAapo =
                DevPerson(
                    id = ChildId(UUID.randomUUID()),
                    dateOfBirth = monday.minusYears(6),
                    firstName = "Aapo",
                    lastName = "Aarnio",
                )
            tx.insert(testChildAapo, DevPersonType.CHILD)

            val testChildBertil =
                DevPerson(
                    ChildId(UUID.randomUUID()),
                    dateOfBirth = monday.minusYears(6),
                    firstName = "Bertil",
                    lastName = "Becker",
                )
            tx.insert(testChildBertil, DevPersonType.CHILD)

            val testChildCecil =
                DevPerson(
                    ChildId(UUID.randomUUID()),
                    dateOfBirth = monday.minusYears(6),
                    firstName = "Cecil",
                    lastName = "Cilliacus",
                )
            tx.insert(testChildCecil, DevPersonType.CHILD)

            val testChildDonald =
                DevPerson(
                    ChildId(UUID.randomUUID()),
                    dateOfBirth = monday.minusYears(6),
                    firstName = "Donald",
                    lastName = "Davis",
                )
            tx.insert(testChildDonald, DevPersonType.CHILD)

            val testChildEurus =
                DevPerson(
                    ChildId(UUID.randomUUID()),
                    dateOfBirth = monday.minusYears(6),
                    firstName = "Eurus",
                    lastName = "Eerikäinen",
                )
            tx.insert(testChildEurus, DevPersonType.CHILD)

            val defaultPlacementDuration =
                FiniteDateRange(monday.minusMonths(1), monday.plusMonths(1))
            val placementAId =
                tx.insert(
                    DevPlacement(
                        type = PlacementType.PRESCHOOL,
                        childId = testChildAapo.id,
                        unitId = preschoolAId,
                        startDate = defaultPlacementDuration.start,
                        endDate = defaultPlacementDuration.end,
                    )
                )

            val placementBId =
                tx.insert(
                    DevPlacement(
                        type = PlacementType.PRESCHOOL,
                        childId = testChildBertil.id,
                        unitId = preschoolAId,
                        startDate = defaultPlacementDuration.start,
                        endDate = defaultPlacementDuration.end,
                    )
                )

            val placementC1 =
                DevPlacement(
                    id = PlacementId(UUID.randomUUID()),
                    type = PlacementType.PRESCHOOL_DAYCARE,
                    childId = testChildCecil.id,
                    unitId = preschoolAId,
                    startDate = monday.minusMonths(1),
                    endDate = wednesday,
                )
            tx.insert(placementC1)

            val placementC2 =
                DevPlacement(
                    id = PlacementId(UUID.randomUUID()),
                    type = PlacementType.PRESCHOOL_DAYCARE,
                    childId = testChildCecil.id,
                    unitId = preschoolAId,
                    startDate = thursday,
                    endDate = thursday.plusMonths(1),
                )
            tx.insert(placementC2)

            val placementD =
                DevPlacement(
                    id = PlacementId(UUID.randomUUID()),
                    type = PlacementType.PREPARATORY,
                    childId = testChildDonald.id,
                    unitId = preschoolAId,
                    startDate = monday.minusWeeks(2),
                    endDate = friday.plusWeeks(2),
                )
            tx.insert(placementD)

            val placementE =
                DevPlacement(
                    id = PlacementId(UUID.randomUUID()),
                    type = PlacementType.PRESCHOOL,
                    childId = testChildEurus.id,
                    unitId = preschoolCId,
                    startDate = monday.minusWeeks(2),
                    endDate = friday.plusWeeks(2),
                )
            tx.insert(placementE)

            tx.insert(
                DevDaycareGroupPlacement(
                    GroupPlacementId(UUID.randomUUID()),
                    placementAId,
                    groupAId,
                    defaultPlacementDuration.start,
                    defaultPlacementDuration.end,
                )
            )

            tx.insert(
                DevDaycareGroupPlacement(
                    GroupPlacementId(UUID.randomUUID()),
                    placementBId,
                    groupAId,
                    defaultPlacementDuration.start,
                    defaultPlacementDuration.end,
                )
            )

            tx.insert(
                DevDaycareGroupPlacement(
                    GroupPlacementId(UUID.randomUUID()),
                    placementC1.id,
                    groupBId,
                    placementC1.startDate,
                    placementC1.endDate,
                )
            )

            tx.insert(
                DevDaycareGroupPlacement(
                    GroupPlacementId(UUID.randomUUID()),
                    placementC2.id,
                    groupAId,
                    placementC2.startDate,
                    placementC2.endDate,
                )
            )

            tx.insert(
                DevDaycareGroupPlacement(
                    GroupPlacementId(UUID.randomUUID()),
                    placementD.id,
                    groupCId,
                    placementD.startDate,
                    placementD.endDate,
                )
            )

            tx.insert(
                DevDaycareGroupPlacement(
                    GroupPlacementId(UUID.randomUUID()),
                    placementE.id,
                    groupDId,
                    placementE.startDate,
                    placementE.endDate,
                )
            )

            // Monday

            tx.insert(
                DevAbsence(
                    id = AbsenceId(UUID.randomUUID()),
                    testChildAapo.id,
                    monday,
                    AbsenceType.PLANNED_ABSENCE,
                    HelsinkiDateTime.atStartOfDay(monday),
                    EvakaUserId(admin.id.raw),
                    AbsenceCategory.NONBILLABLE,
                )
            )

            tx.insert(
                DevAbsence(
                    id = AbsenceId(UUID.randomUUID()),
                    testChildBertil.id,
                    monday,
                    AbsenceType.SICKLEAVE,
                    HelsinkiDateTime.atStartOfDay(monday),
                    EvakaUserId(admin.id.raw),
                    AbsenceCategory.NONBILLABLE,
                )
            )

            tx.insert(
                DevAbsence(
                    id = AbsenceId(UUID.randomUUID()),
                    testChildCecil.id,
                    monday,
                    AbsenceType.UNKNOWN_ABSENCE,
                    HelsinkiDateTime.atStartOfDay(monday),
                    EvakaUserId(admin.id.raw),
                    AbsenceCategory.NONBILLABLE,
                )
            )

            // billable absence shouldn't show up
            tx.insert(
                DevAbsence(
                    id = AbsenceId(UUID.randomUUID()),
                    testChildCecil.id,
                    monday,
                    AbsenceType.UNKNOWN_ABSENCE,
                    HelsinkiDateTime.atStartOfDay(monday),
                    EvakaUserId(admin.id.raw),
                    AbsenceCategory.BILLABLE,
                )
            )

            tx.insert(
                DevAbsence(
                    id = AbsenceId(UUID.randomUUID()),
                    testChildDonald.id,
                    monday,
                    AbsenceType.PLANNED_ABSENCE,
                    HelsinkiDateTime.atStartOfDay(monday),
                    EvakaUserId(admin.id.raw),
                    AbsenceCategory.NONBILLABLE,
                )
            )
            tx.insert(
                DevAbsence(
                    id = AbsenceId(UUID.randomUUID()),
                    testChildDonald.id,
                    monday,
                    AbsenceType.PLANNED_ABSENCE,
                    HelsinkiDateTime.atStartOfDay(monday),
                    EvakaUserId(admin.id.raw),
                    AbsenceCategory.BILLABLE,
                )
            )

            tx.insert(
                DevAbsence(
                    id = AbsenceId(UUID.randomUUID()),
                    testChildEurus.id,
                    monday,
                    AbsenceType.PLANNED_ABSENCE,
                    HelsinkiDateTime.atStartOfDay(monday),
                    EvakaUserId(admin.id.raw),
                    AbsenceCategory.NONBILLABLE,
                )
            )

            // Tuesday

            tx.insertTestChildAttendance(
                testChildAapo.id,
                preschoolAId,
                HelsinkiDateTime.of(tuesday, LocalTime.of(8, 0)),
                // left an hour early
                HelsinkiDateTime.of(tuesday, LocalTime.of(13, 0)),
            )

            tx.insertTestChildAttendance(
                testChildBertil.id,
                preschoolAId,
                HelsinkiDateTime.of(tuesday, LocalTime.of(9, 0)),
                HelsinkiDateTime.of(tuesday, LocalTime.of(16, 0)),
            )

            // arrives 1.5h late
            tx.insertTestChildAttendance(
                testChildCecil.id,
                preschoolAId,
                HelsinkiDateTime.of(tuesday, LocalTime.of(10, 30)),
                HelsinkiDateTime.of(tuesday, LocalTime.of(15, 30)),
            )

            // Wednesday

            tx.insertTestChildAttendance(
                testChildAapo.id,
                preschoolAId,
                // arrived an hour late
                HelsinkiDateTime.of(wednesday, LocalTime.of(10, 0)),
                HelsinkiDateTime.of(wednesday, LocalTime.of(16, 0)),
            )

            // Thursday

            tx.insertTestChildAttendance(
                testChildAapo.id,
                preschoolAId,
                // arrived 45 minutes late
                HelsinkiDateTime.of(thursday, LocalTime.of(9, 45)),
                HelsinkiDateTime.of(thursday, LocalTime.of(16, 0)),
            )

            tx.insert(
                DevAbsence(
                    id = AbsenceId(UUID.randomUUID()),
                    testChildCecil.id,
                    thursday,
                    AbsenceType.UNKNOWN_ABSENCE,
                    HelsinkiDateTime.atStartOfDay(thursday),
                    EvakaUserId(admin.id.raw),
                    AbsenceCategory.NONBILLABLE,
                )
            )

            // Friday

            // leaves 3 hours early
            tx.insertTestChildAttendance(
                testChildCecil.id,
                preschoolAId,
                HelsinkiDateTime.of(friday, LocalTime.of(9, 0)),
                HelsinkiDateTime.of(friday, LocalTime.of(11, 0)),
            )

            // Saturday

            // absence on the weekend shouldn't show up
            tx.insert(
                DevAbsence(
                    id = AbsenceId(UUID.randomUUID()),
                    testChildCecil.id,
                    saturday,
                    AbsenceType.UNKNOWN_ABSENCE,
                    HelsinkiDateTime.atStartOfDay(saturday),
                    EvakaUserId(admin.id.raw),
                    AbsenceCategory.NONBILLABLE,
                )
            )

            // next monday
            // unfinished attendance shouldn't show up
            tx.insertTestChildAttendance(
                testChildCecil.id,
                preschoolAId,
                HelsinkiDateTime.of(monday.plusWeeks(1), LocalTime.of(11, 0)),
                null,
            )

            // absence on holiday shouldn't show up
            tx.insert(
                DevAbsence(
                    id = AbsenceId(UUID.randomUUID()),
                    testChildCecil.id,
                    holidayAfter,
                    AbsenceType.OTHER_ABSENCE,
                    HelsinkiDateTime.atStartOfDay(holidayAfter),
                    EvakaUserId(admin.id.raw),
                    AbsenceCategory.NONBILLABLE,
                )
            )

            // next wednesday
            // absence on term break shouldn't show up
            tx.insert(
                DevAbsence(
                    id = AbsenceId(UUID.randomUUID()),
                    testChildCecil.id,
                    nextWednesday,
                    AbsenceType.OTHER_ABSENCE,
                    HelsinkiDateTime.atStartOfDay(nextWednesday),
                    EvakaUserId(admin.id.raw),
                    AbsenceCategory.NONBILLABLE,
                )
            )

            // previous thursday
            // absence on term break shouldn't show up
            tx.insert(
                DevAbsence(
                    id = AbsenceId(UUID.randomUUID()),
                    testChildCecil.id,
                    previousThursday,
                    AbsenceType.OTHER_ABSENCE,
                    HelsinkiDateTime.atStartOfDay(previousThursday),
                    EvakaUserId(admin.id.raw),
                    AbsenceCategory.NONBILLABLE,
                )
            )

            // absence on holiday shouldn't show up
            tx.insert(
                DevAbsence(
                    id = AbsenceId(UUID.randomUUID()),
                    testChildCecil.id,
                    holidayBefore,
                    AbsenceType.OTHER_ABSENCE,
                    HelsinkiDateTime.atStartOfDay(holidayBefore),
                    EvakaUserId(admin.id.raw),
                    AbsenceCategory.NONBILLABLE,
                )
            )

            // previous sunday
            // absence on the weekend shouldn't show up
            tx.insert(
                DevAbsence(
                    id = AbsenceId(UUID.randomUUID()),
                    testChildCecil.id,
                    previousSunday,
                    AbsenceType.UNKNOWN_ABSENCE,
                    HelsinkiDateTime.atStartOfDay(previousSunday),
                    EvakaUserId(admin.id.raw),
                    AbsenceCategory.NONBILLABLE,
                )
            )

            PreschoolAbsenceReportTestData(
                testTerm,
                testChildAapo,
                testChildBertil,
                testChildCecil,
                testChildDonald,
                testChildEurus,
                groupAId,
                groupBId,
                groupCId,
                groupDId,
                preschoolAId,
                preschoolBId,
                areaAId,
                areaBId,
            )
        }
    }

    data class PreschoolAbsenceReportTestData(
        val preschoolTerm: FiniteDateRange,
        val childA: DevPerson,
        val childB: DevPerson,
        val childC: DevPerson,
        val childD: DevPerson,
        val childE: DevPerson,
        val groupAId: GroupId,
        val groupBId: GroupId,
        val groupCId: GroupId,
        val groupDId: GroupId,
        val daycareAId: DaycareId,
        val daycareBId: DaycareId,
        val areaAId: AreaId,
        val areaBId: AreaId,
    )
}
