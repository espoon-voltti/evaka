// SPDX-FileCopyrightText: 2021-2022 City of Tampere
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.titania

import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.attendance.StaffAttendanceType
import fi.espoo.evaka.attendance.upsertStaffAttendance
import fi.espoo.evaka.pis.createEmployee
import fi.espoo.evaka.pis.getEmployees
import fi.espoo.evaka.shared.EmployeeId
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.dev.*
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalTime
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.catchThrowable
import org.assertj.core.groups.Tuple
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.springframework.beans.factory.annotation.Autowired

internal class TitaniaServiceTest : FullApplicationTest(resetDbBeforeEach = true) {

    @Autowired private lateinit var titaniaService: TitaniaService

    @Test
    fun updateWorkingTimeEvents() {
        val employeeId =
            db.transaction { tx ->
                tx.createEmployee(testEmployee.copy(employeeNumber = "176716")).id
            }

        val response1 =
            db.transaction { tx ->
                titaniaService.updateWorkingTimeEvents(tx, titaniaUpdateRequestValidExampleData)
            }
        assertThat(response1).returns("OK") { it.updateWorkingTimeEventsResponse.message }
        val plans1 = db.transaction { tx -> tx.findStaffAttendancePlansBy() }
        assertThat(plans1).extracting<EmployeeId> { it.employeeId }.containsExactly(employeeId)

        val response2 =
            db.transaction { tx ->
                titaniaService.updateWorkingTimeEvents(tx, titaniaUpdateRequestValidExampleData)
            }
        assertThat(response2).returns("OK") { it.updateWorkingTimeEventsResponse.message }
        val plans2 = db.transaction { tx -> tx.findStaffAttendancePlansBy() }
        assertThat(plans2).extracting<EmployeeId> { it.employeeId }.containsExactly(employeeId)
    }

    @Test
    fun updateWorkingTimeEventsInternal() {
        val employeeId =
            db.transaction { tx ->
                tx.createEmployee(testEmployee.copy(employeeNumber = "176716")).id
            }

        val response1 =
            db.transaction { tx ->
                titaniaService.updateWorkingTimeEventsInternal(
                    tx,
                    titaniaUpdateRequestValidExampleData,
                )
            }
        assertThat(response1.deleted).isEmpty()
        assertThat(response1.inserted)
            .containsExactlyInAnyOrder(
                StaffAttendancePlan(
                    employeeId = employeeId,
                    type = StaffAttendanceType.PRESENT,
                    HelsinkiDateTime.of(LocalDate.of(2011, 1, 3), LocalTime.of(7, 0)),
                    HelsinkiDateTime.of(LocalDate.of(2011, 1, 3), LocalTime.of(23, 59)),
                    description = null,
                )
            )

        val response2 =
            db.transaction { tx ->
                titaniaService.updateWorkingTimeEventsInternal(
                    tx,
                    titaniaUpdateRequestValidExampleData,
                )
            }
        assertThat(response2.deleted).isEqualTo(response1.inserted)
        assertThat(response2.inserted).isEqualTo(response1.inserted)
    }

    @Test
    fun `updateWorkingTimeEventsInternal merge overnight events`() {
        val employee1Id =
            db.transaction { tx ->
                tx.createEmployee(testEmployee.copy(employeeNumber = "176716")).id
            }
        val employee2Id =
            db.transaction { tx ->
                tx.createEmployee(testEmployee.copy(employeeNumber = "949382")).id
            }

        val request =
            UpdateWorkingTimeEventsRequest(
                period =
                    TitaniaPeriod(
                        beginDate = LocalDate.of(2022, 10, 31),
                        endDate = LocalDate.of(2022, 11, 2),
                    ),
                schedulingUnit =
                    listOf(
                        TitaniaSchedulingUnit(
                            code = "",
                            occupation =
                                listOf(
                                    TitaniaOccupation(
                                        code = "",
                                        name = "",
                                        person =
                                            listOf(
                                                TitaniaPerson(
                                                    employeeId = "00176716",
                                                    name = "",
                                                    actualWorkingTimeEvents =
                                                        TitaniaWorkingTimeEvents(
                                                            event =
                                                                listOf(
                                                                    TitaniaWorkingTimeEvent(
                                                                        date =
                                                                            LocalDate.of(
                                                                                2022,
                                                                                11,
                                                                                1,
                                                                            ),
                                                                        beginTime = "0000",
                                                                        endTime = "0800",
                                                                    ),
                                                                    TitaniaWorkingTimeEvent(
                                                                        date =
                                                                            LocalDate.of(
                                                                                2022,
                                                                                11,
                                                                                1,
                                                                            ),
                                                                        beginTime = "2000",
                                                                        endTime = "2359",
                                                                    ),
                                                                    TitaniaWorkingTimeEvent(
                                                                        date =
                                                                            LocalDate.of(
                                                                                2022,
                                                                                10,
                                                                                31,
                                                                            ),
                                                                        beginTime = "2000",
                                                                        endTime = "2400",
                                                                    ),
                                                                    TitaniaWorkingTimeEvent(
                                                                        date =
                                                                            LocalDate.of(
                                                                                2022,
                                                                                11,
                                                                                2,
                                                                            ),
                                                                        beginTime = "0000",
                                                                        endTime = "0800",
                                                                    ),
                                                                )
                                                        ),
                                                ),
                                                TitaniaPerson(
                                                    employeeId = "00949382",
                                                    name = "",
                                                    actualWorkingTimeEvents =
                                                        TitaniaWorkingTimeEvents(
                                                            event =
                                                                listOf(
                                                                    TitaniaWorkingTimeEvent(
                                                                        date =
                                                                            LocalDate.of(
                                                                                2022,
                                                                                11,
                                                                                2,
                                                                            ),
                                                                        beginTime = "0000",
                                                                        endTime = "0600",
                                                                    ),
                                                                    TitaniaWorkingTimeEvent(
                                                                        date =
                                                                            LocalDate.of(
                                                                                2022,
                                                                                11,
                                                                                1,
                                                                            ),
                                                                        beginTime = "0000",
                                                                        endTime = "2359",
                                                                    ),
                                                                    TitaniaWorkingTimeEvent(
                                                                        date =
                                                                            LocalDate.of(
                                                                                2022,
                                                                                10,
                                                                                31,
                                                                            ),
                                                                        beginTime = "2300",
                                                                        endTime = "2358",
                                                                    ),
                                                                )
                                                        ),
                                                ),
                                            ),
                                    )
                                ),
                        )
                    ),
            )

        val response =
            db.transaction { tx -> titaniaService.updateWorkingTimeEventsInternal(tx, request) }

        assertThat(response.deleted).isEmpty()
        assertThat(response.inserted)
            .containsExactlyInAnyOrder(
                StaffAttendancePlan(
                    employeeId = employee1Id,
                    type = StaffAttendanceType.PRESENT,
                    HelsinkiDateTime.of(LocalDate.of(2022, 10, 31), LocalTime.of(20, 0)),
                    HelsinkiDateTime.of(LocalDate.of(2022, 11, 1), LocalTime.of(8, 0)),
                    description = null,
                ),
                StaffAttendancePlan(
                    employeeId = employee1Id,
                    type = StaffAttendanceType.PRESENT,
                    HelsinkiDateTime.of(LocalDate.of(2022, 11, 1), LocalTime.of(20, 0)),
                    HelsinkiDateTime.of(LocalDate.of(2022, 11, 2), LocalTime.of(8, 0)),
                    description = null,
                ),
                StaffAttendancePlan(
                    employeeId = employee2Id,
                    type = StaffAttendanceType.PRESENT,
                    HelsinkiDateTime.of(LocalDate.of(2022, 10, 31), LocalTime.of(23, 0)),
                    HelsinkiDateTime.of(LocalDate.of(2022, 10, 31), LocalTime.of(23, 58)),
                    description = null,
                ),
                StaffAttendancePlan(
                    employeeId = employee2Id,
                    type = StaffAttendanceType.PRESENT,
                    HelsinkiDateTime.of(LocalDate.of(2022, 11, 1), LocalTime.of(0, 0)),
                    HelsinkiDateTime.of(LocalDate.of(2022, 11, 2), LocalTime.of(6, 0)),
                    description = null,
                ),
            )
    }

    @ParameterizedTest
    @CsvSource(
        value =
            [
                // vakiokoodit
                "U, PRESENT",
                "K, TRAINING",
                // muut koodit (aina PRESENT)
                "X, PRESENT",
                "a, PRESENT",
            ]
    )
    fun `event code is mapped to attendance type correctly`(
        givenEventCode: String,
        expectedType: StaffAttendanceType,
    ) {
        val employeeId =
            db.transaction { tx ->
                tx.createEmployee(testEmployee.copy(employeeNumber = "176716")).id
            }

        val response =
            db.transaction { tx ->
                titaniaService.updateWorkingTimeEventsInternal(
                    tx,
                    UpdateWorkingTimeEventsRequest(
                        period =
                            TitaniaPeriod(
                                beginDate = LocalDate.of(2022, 10, 12),
                                endDate = LocalDate.of(2022, 10, 12),
                            ),
                        schedulingUnit =
                            listOf(
                                TitaniaSchedulingUnit(
                                    code = "",
                                    occupation =
                                        listOf(
                                            TitaniaOccupation(
                                                code = "",
                                                name = "",
                                                person =
                                                    listOf(
                                                        TitaniaPerson(
                                                            employeeId = "176716",
                                                            name = "",
                                                            actualWorkingTimeEvents =
                                                                TitaniaWorkingTimeEvents(
                                                                    event =
                                                                        listOf(
                                                                            TitaniaWorkingTimeEvent(
                                                                                date =
                                                                                    LocalDate.of(
                                                                                        2022,
                                                                                        10,
                                                                                        12,
                                                                                    ),
                                                                                code =
                                                                                    givenEventCode,
                                                                                beginTime = "0942",
                                                                                endTime = "0944",
                                                                            )
                                                                        )
                                                                ),
                                                        )
                                                    ),
                                            )
                                        ),
                                )
                            ),
                    ),
                )
            }

        assertThat(response.deleted).isEmpty()
        assertThat(response.inserted)
            .containsExactlyInAnyOrder(
                StaffAttendancePlan(
                    employeeId = employeeId,
                    type = expectedType,
                    HelsinkiDateTime.of(LocalDate.of(2022, 10, 12), LocalTime.of(9, 42)),
                    HelsinkiDateTime.of(LocalDate.of(2022, 10, 12), LocalTime.of(9, 44)),
                    description = null,
                )
            )
    }

    @Test
    fun `deduplicates identical events in the request`() {
        val employeeId =
            db.transaction { tx ->
                tx.createEmployee(testEmployee.copy(employeeNumber = "176716")).id
            }

        lateinit var response: TitaniaUpdateResponse
        assertDoesNotThrow {
            response =
                db.transaction { tx ->
                    titaniaService.updateWorkingTimeEventsInternal(
                        tx,
                        UpdateWorkingTimeEventsRequest(
                            period =
                                TitaniaPeriod(
                                    beginDate = LocalDate.of(2022, 10, 12),
                                    endDate = LocalDate.of(2022, 10, 12),
                                ),
                            schedulingUnit =
                                listOf(
                                    TitaniaSchedulingUnit(
                                        code = "",
                                        occupation =
                                            listOf(
                                                TitaniaOccupation(
                                                    code = "",
                                                    name = "",
                                                    person =
                                                        listOf(
                                                            TitaniaPerson(
                                                                employeeId = "176716",
                                                                name = "",
                                                                actualWorkingTimeEvents =
                                                                    TitaniaWorkingTimeEvents(
                                                                        event =
                                                                            listOf(
                                                                                TitaniaWorkingTimeEvent(
                                                                                    date =
                                                                                        LocalDate
                                                                                            .of(
                                                                                                2022,
                                                                                                10,
                                                                                                12,
                                                                                            ),
                                                                                    beginTime =
                                                                                        "0800",
                                                                                    endTime = "0900",
                                                                                ),
                                                                                TitaniaWorkingTimeEvent(
                                                                                    date =
                                                                                        LocalDate
                                                                                            .of(
                                                                                                2022,
                                                                                                10,
                                                                                                12,
                                                                                            ),
                                                                                    beginTime =
                                                                                        "0800",
                                                                                    endTime = "0900",
                                                                                ),
                                                                            )
                                                                    ),
                                                            )
                                                        ),
                                                )
                                            ),
                                    )
                                ),
                        ),
                    )
                }
        }

        assertThat(response.deleted).isEmpty()
        assertThat(response.inserted).hasSize(1)
        assertThat(response.inserted)
            .containsExactlyInAnyOrder(
                StaffAttendancePlan(
                    employeeId = employeeId,
                    type = StaffAttendanceType.PRESENT,
                    HelsinkiDateTime.of(LocalDate.of(2022, 10, 12), LocalTime.of(8, 0)),
                    HelsinkiDateTime.of(LocalDate.of(2022, 10, 12), LocalTime.of(9, 0)),
                    description = null,
                )
            )
    }

    @Test
    fun `updateWorkingTimeEvents throws when event date out of period`() {
        val request =
            UpdateWorkingTimeEventsRequest(
                period = TitaniaPeriod.from(LocalDate.of(2022, 6, 15)),
                schedulingUnit =
                    listOf(
                        TitaniaSchedulingUnit(
                            code = "sch1",
                            occupation =
                                listOf(
                                    TitaniaOccupation(
                                        code = "occ2",
                                        name = "Occupation 2",
                                        person =
                                            listOf(
                                                TitaniaPerson(
                                                    employeeId = "emp1",
                                                    name = "Employee 1",
                                                    actualWorkingTimeEvents =
                                                        TitaniaWorkingTimeEvents(
                                                            event =
                                                                listOf(
                                                                    TitaniaWorkingTimeEvent(
                                                                        date =
                                                                            LocalDate.of(
                                                                                2022,
                                                                                6,
                                                                                14,
                                                                            ),
                                                                        beginTime = "0906",
                                                                        endTime = "1522",
                                                                    )
                                                                )
                                                        ),
                                                )
                                            ),
                                    )
                                ),
                        )
                    ),
            )

        val response = catchThrowable {
            db.transaction { tx -> titaniaService.updateWorkingTimeEvents(tx, request) }
        }

        assertThat(response)
            .isExactlyInstanceOf(TitaniaException::class.java)
            .hasMessage("Event date 2022-06-14 is out of period (2022-06-15 - 2022-06-15)")
    }

    @Test
    fun `updateWorkingTimeEventsInternal with unknown employee numbers`() {
        val request =
            UpdateWorkingTimeEventsRequest(
                period = TitaniaPeriod.from(LocalDate.of(2022, 6, 15)),
                schedulingUnit =
                    listOf(
                        TitaniaSchedulingUnit(
                            code = "sch1",
                            occupation =
                                listOf(
                                    TitaniaOccupation(
                                        code = "occ2",
                                        name = "Occupation 2",
                                        person =
                                            listOf(
                                                TitaniaPerson(
                                                    employeeId = "001234",
                                                    name = "Employee 1",
                                                    actualWorkingTimeEvents =
                                                        TitaniaWorkingTimeEvents(
                                                            event =
                                                                listOf(
                                                                    TitaniaWorkingTimeEvent(
                                                                        date =
                                                                            LocalDate.of(
                                                                                2022,
                                                                                6,
                                                                                15,
                                                                            ),
                                                                        beginTime = "0906",
                                                                        endTime = "1522",
                                                                    )
                                                                )
                                                        ),
                                                )
                                            ),
                                    )
                                ),
                        )
                    ),
            )

        val response =
            db.transaction { tx -> titaniaService.updateWorkingTimeEventsInternal(tx, request) }

        assertThat(response.deleted).isEmpty()
        assertThat(response.inserted)
            .extracting({ it.type }, { it.startTime }, { it.endTime }, { it.description })
            .containsExactly(
                Tuple(
                    StaffAttendanceType.PRESENT,
                    HelsinkiDateTime.of(LocalDate.of(2022, 6, 15), LocalTime.of(9, 6)),
                    HelsinkiDateTime.of(LocalDate.of(2022, 6, 15), LocalTime.of(15, 22)),
                    null,
                )
            )

        val employees = db.transaction { tx -> tx.getEmployees() }

        assertThat(employees)
            .extracting({ it.firstName }, { it.lastName })
            .containsExactly(Tuple("1", "Employee"))

        val numbers = db.transaction { tx -> tx.getEmployeeIdsByNumbers(listOf("1234")) }

        assertThat(numbers).containsOnlyKeys("1234")

        assertThat(response.createdEmployees).containsExactly(employees[0].id)
    }

    @Test
    fun `checks for conflicting shifts`() {
        val employeeId1 =
            db.transaction { tx ->
                tx.createEmployee(testEmployee.copy(employeeNumber = "176716")).id
            }
        val employeeId2 =
            db.transaction { tx ->
                tx.createEmployee(testEmployee.copy(employeeNumber = "176167")).id
            }

        lateinit var response: TitaniaUpdateResponse
        assertDoesNotThrow {
            response =
                db.transaction { tx ->
                    titaniaService.updateWorkingTimeEventsInternal(
                        tx,
                        UpdateWorkingTimeEventsRequest(
                            period =
                                TitaniaPeriod(
                                    beginDate = LocalDate.of(2022, 10, 12),
                                    endDate = LocalDate.of(2022, 10, 13),
                                ),
                            schedulingUnit =
                                listOf(
                                    TitaniaSchedulingUnit(
                                        code = "",
                                        occupation =
                                            listOf(
                                                TitaniaOccupation(
                                                    code = "",
                                                    name = "",
                                                    person =
                                                        listOf(
                                                            TitaniaPerson(
                                                                employeeId = "176716",
                                                                name = "",
                                                                actualWorkingTimeEvents =
                                                                    TitaniaWorkingTimeEvents(
                                                                        event =
                                                                            listOf(
                                                                                TitaniaWorkingTimeEvent(
                                                                                    date =
                                                                                        LocalDate
                                                                                            .of(
                                                                                                2022,
                                                                                                10,
                                                                                                12,
                                                                                            ),
                                                                                    beginTime =
                                                                                        "0800",
                                                                                    endTime = "0900",
                                                                                ),
                                                                                TitaniaWorkingTimeEvent(
                                                                                    date =
                                                                                        LocalDate
                                                                                            .of(
                                                                                                2022,
                                                                                                10,
                                                                                                12,
                                                                                            ),
                                                                                    beginTime =
                                                                                        "0830",
                                                                                    endTime = "1400",
                                                                                ),
                                                                                TitaniaWorkingTimeEvent(
                                                                                    date =
                                                                                        LocalDate
                                                                                            .of(
                                                                                                2022,
                                                                                                10,
                                                                                                12,
                                                                                            ),
                                                                                    beginTime =
                                                                                        "1400",
                                                                                    endTime = "1530",
                                                                                ),
                                                                            )
                                                                    ),
                                                            ),
                                                            TitaniaPerson(
                                                                employeeId = "176167",
                                                                name = "",
                                                                actualWorkingTimeEvents =
                                                                    TitaniaWorkingTimeEvents(
                                                                        event =
                                                                            listOf(
                                                                                TitaniaWorkingTimeEvent(
                                                                                    date =
                                                                                        LocalDate
                                                                                            .of(
                                                                                                2022,
                                                                                                10,
                                                                                                12,
                                                                                            ),
                                                                                    beginTime =
                                                                                        "0800",
                                                                                    endTime = "0900",
                                                                                ),
                                                                                TitaniaWorkingTimeEvent(
                                                                                    date =
                                                                                        LocalDate
                                                                                            .of(
                                                                                                2022,
                                                                                                10,
                                                                                                12,
                                                                                            ),
                                                                                    beginTime =
                                                                                        "0800",
                                                                                    endTime = "0900",
                                                                                ),
                                                                                TitaniaWorkingTimeEvent(
                                                                                    date =
                                                                                        LocalDate
                                                                                            .of(
                                                                                                2022,
                                                                                                10,
                                                                                                13,
                                                                                            ),
                                                                                    beginTime =
                                                                                        "0700",
                                                                                    endTime = "1130",
                                                                                ),
                                                                                TitaniaWorkingTimeEvent(
                                                                                    date =
                                                                                        LocalDate
                                                                                            .of(
                                                                                                2022,
                                                                                                10,
                                                                                                13,
                                                                                            ),
                                                                                    beginTime =
                                                                                        "1045",
                                                                                    endTime = "1300",
                                                                                ),
                                                                            )
                                                                    ),
                                                            ),
                                                        ),
                                                )
                                            ),
                                    )
                                ),
                        ),
                    )
                }
        }

        assertThat(response.inserted).isEmpty()
        assertThat(response.deleted).isEmpty()
        assertThat(response.createdEmployees).isEmpty()
        assertThat(response.overLappingShifts)
            .containsExactlyInAnyOrder(
                TitaniaOverLappingShifts(
                    employeeId1,
                    LocalDate.of(2022, 10, 12),
                    LocalTime.of(8, 0),
                    LocalTime.of(9, 0),
                    LocalTime.of(8, 30),
                    LocalTime.of(14, 0),
                ),
                TitaniaOverLappingShifts(
                    employeeId2,
                    LocalDate.of(2022, 10, 13),
                    LocalTime.of(7, 0),
                    LocalTime.of(11, 30),
                    LocalTime.of(10, 45),
                    LocalTime.of(13, 0),
                ),
            )
    }

    @Test
    fun `stores conflicting shifts in the error report table`() {
        val employeeId1 =
            db.transaction { tx ->
                tx.createEmployee(testEmployee.copy(employeeNumber = "176716")).id
            }
        val employeeId2 =
            db.transaction { tx ->
                tx.createEmployee(testEmployee.copy(employeeNumber = "176167")).id
            }

        db.transaction { tx ->
            titaniaService.updateWorkingTimeEventsInternal(
                tx,
                UpdateWorkingTimeEventsRequest(
                    period =
                        TitaniaPeriod(
                            beginDate = LocalDate.of(2022, 10, 12),
                            endDate = LocalDate.of(2022, 10, 13),
                        ),
                    schedulingUnit =
                        listOf(
                            TitaniaSchedulingUnit(
                                code = "",
                                occupation =
                                    listOf(
                                        TitaniaOccupation(
                                            code = "",
                                            name = "",
                                            person =
                                                listOf(
                                                    TitaniaPerson(
                                                        employeeId = "176716",
                                                        name = "",
                                                        actualWorkingTimeEvents =
                                                            TitaniaWorkingTimeEvents(
                                                                event =
                                                                    listOf(
                                                                        TitaniaWorkingTimeEvent(
                                                                            date =
                                                                                LocalDate.of(
                                                                                    2022,
                                                                                    10,
                                                                                    12,
                                                                                ),
                                                                            beginTime = "0800",
                                                                            endTime = "0900",
                                                                        ),
                                                                        TitaniaWorkingTimeEvent(
                                                                            date =
                                                                                LocalDate.of(
                                                                                    2022,
                                                                                    10,
                                                                                    12,
                                                                                ),
                                                                            beginTime = "0830",
                                                                            endTime = "1400",
                                                                        ),
                                                                        TitaniaWorkingTimeEvent(
                                                                            date =
                                                                                LocalDate.of(
                                                                                    2022,
                                                                                    10,
                                                                                    12,
                                                                                ),
                                                                            beginTime = "1400",
                                                                            endTime = "1530",
                                                                        ),
                                                                    )
                                                            ),
                                                    ),
                                                    TitaniaPerson(
                                                        employeeId = "176167",
                                                        name = "",
                                                        actualWorkingTimeEvents =
                                                            TitaniaWorkingTimeEvents(
                                                                event =
                                                                    listOf(
                                                                        TitaniaWorkingTimeEvent(
                                                                            date =
                                                                                LocalDate.of(
                                                                                    2022,
                                                                                    10,
                                                                                    12,
                                                                                ),
                                                                            beginTime = "0800",
                                                                            endTime = "0900",
                                                                        ),
                                                                        TitaniaWorkingTimeEvent(
                                                                            date =
                                                                                LocalDate.of(
                                                                                    2022,
                                                                                    10,
                                                                                    12,
                                                                                ),
                                                                            beginTime = "0800",
                                                                            endTime = "0900",
                                                                        ),
                                                                        TitaniaWorkingTimeEvent(
                                                                            date =
                                                                                LocalDate.of(
                                                                                    2022,
                                                                                    10,
                                                                                    13,
                                                                                ),
                                                                            beginTime = "0700",
                                                                            endTime = "1130",
                                                                        ),
                                                                        TitaniaWorkingTimeEvent(
                                                                            date =
                                                                                LocalDate.of(
                                                                                    2022,
                                                                                    10,
                                                                                    13,
                                                                                ),
                                                                            beginTime = "1045",
                                                                            endTime = "1300",
                                                                        ),
                                                                    )
                                                            ),
                                                    ),
                                                ),
                                        )
                                    ),
                            )
                        ),
                ),
            )
        }

        val reportRows = db.read { tx -> tx.fetchReportRows() }

        assertThat(reportRows)
            .usingRecursiveFieldByFieldElementComparatorIgnoringFields("requestTime")
            .containsExactlyInAnyOrder(
                TitaniaTestDbRow(
                    HelsinkiDateTime.now(),
                    employeeId1,
                    LocalDate.of(2022, 10, 12),
                    LocalTime.of(8, 0),
                    LocalTime.of(9, 0),
                    LocalTime.of(8, 30),
                    LocalTime.of(14, 0),
                ),
                TitaniaTestDbRow(
                    HelsinkiDateTime.now(),
                    employeeId2,
                    LocalDate.of(2022, 10, 13),
                    LocalTime.of(7, 0),
                    LocalTime.of(11, 30),
                    LocalTime.of(10, 45),
                    LocalTime.of(13, 0),
                ),
            )
    }

    @Test
    fun `getStampedWorkingTimeEvents`() {
        db.transaction { tx ->
            val areaId = tx.insert(DevCareArea())
            val unitId = tx.insert(DevDaycare(areaId = areaId))
            val groupId = tx.insert(DevDaycareGroup(daycareId = unitId))
            tx.createEmployee(
                    testEmployee.copy(
                        firstName = "IINES",
                        lastName = "ANKKA",
                        employeeNumber = "177111",
                    )
                )
                .let { (employeeId) ->
                    tx.upsertStaffAttendance(
                        attendanceId = null,
                        employeeId = employeeId,
                        groupId = groupId,
                        arrivalTime =
                            HelsinkiDateTime.of(LocalDate.of(2014, 3, 3), LocalTime.of(7, 0)),
                        departureTime =
                            HelsinkiDateTime.of(LocalDate.of(2014, 3, 3), LocalTime.of(15, 0)),
                        occupancyCoefficient = BigDecimal("7.0"),
                        type = StaffAttendanceType.PRESENT,
                    )
                    tx.upsertStaffAttendance(
                        attendanceId = null,
                        employeeId = employeeId,
                        groupId = groupId,
                        arrivalTime =
                            HelsinkiDateTime.of(LocalDate.of(2014, 3, 4), LocalTime.of(6, 30)),
                        departureTime =
                            HelsinkiDateTime.of(LocalDate.of(2014, 3, 4), LocalTime.of(12, 0)),
                        occupancyCoefficient = BigDecimal("7.0"),
                        type = StaffAttendanceType.OVERTIME,
                    )
                }
            tx.createEmployee(
                    testEmployee.copy(
                        firstName = "HESSU",
                        lastName = "HOPO",
                        employeeNumber = "255145",
                    )
                )
                .let { (employeeId) ->
                    tx.upsertStaffAttendance(
                        attendanceId = null,
                        employeeId = employeeId,
                        groupId = groupId,
                        arrivalTime =
                            HelsinkiDateTime.of(LocalDate.of(2014, 3, 3), LocalTime.of(7, 0)),
                        departureTime =
                            HelsinkiDateTime.of(LocalDate.of(2014, 3, 3), LocalTime.of(11, 0)),
                        occupancyCoefficient = BigDecimal("7.0"),
                        type = StaffAttendanceType.PRESENT,
                    )
                    tx.upsertStaffAttendance(
                        attendanceId = null,
                        employeeId = employeeId,
                        groupId = groupId,
                        arrivalTime =
                            HelsinkiDateTime.of(LocalDate.of(2014, 3, 3), LocalTime.of(12, 5)),
                        departureTime =
                            HelsinkiDateTime.of(LocalDate.of(2014, 3, 3), LocalTime.of(16, 10)),
                        occupancyCoefficient = BigDecimal("7.0"),
                        type = StaffAttendanceType.PRESENT,
                    )
                    tx.upsertStaffAttendance(
                        attendanceId = null,
                        employeeId = employeeId,
                        groupId = groupId,
                        arrivalTime =
                            HelsinkiDateTime.of(LocalDate.of(2014, 3, 4), LocalTime.of(10, 15)),
                        departureTime =
                            HelsinkiDateTime.of(LocalDate.of(2014, 3, 4), LocalTime.of(17, 15)),
                        occupancyCoefficient = BigDecimal("7.0"),
                        type = StaffAttendanceType.OTHER_WORK,
                    )
                }
        }

        val response =
            db.transaction { tx ->
                titaniaService.getStampedWorkingTimeEvents(tx, titaniaGetRequestValidExampleData)
            }

        assertThat(response).isEqualTo(titaniaGetResponseValidExampleData)
    }

    @Test
    fun `getStampedWorkingTimeEvents without group`() {
        db.transaction { tx ->
            tx.createEmployee(
                    testEmployee.copy(
                        firstName = "IINES",
                        lastName = "ANKKA",
                        employeeNumber = "177111",
                    )
                )
                .let { (employeeId) ->
                    tx.upsertStaffAttendance(
                        attendanceId = null,
                        employeeId = employeeId,
                        groupId = null,
                        arrivalTime =
                            HelsinkiDateTime.of(LocalDate.of(2023, 2, 6), LocalTime.of(8, 0)),
                        departureTime =
                            HelsinkiDateTime.of(LocalDate.of(2023, 2, 6), LocalTime.of(15, 39)),
                        occupancyCoefficient = BigDecimal("7.0"),
                        type = StaffAttendanceType.OTHER_WORK,
                    )
                }
        }

        val response =
            db.transaction { tx ->
                titaniaService.getStampedWorkingTimeEvents(
                    tx,
                    GetStampedWorkingTimeEventsRequest(
                        period =
                            TitaniaPeriod(
                                beginDate = LocalDate.of(2023, 2, 6),
                                endDate = LocalDate.of(2023, 2, 6),
                            ),
                        schedulingUnit =
                            listOf(
                                TitaniaStampedUnitRequest(
                                    code = "from titania",
                                    person =
                                        listOf(TitaniaStampedPersonRequest(employeeId = "00177111")),
                                )
                            ),
                    ),
                )
            }

        assertThat(response)
            .isEqualTo(
                GetStampedWorkingTimeEventsResponse(
                    schedulingUnit =
                        listOf(
                            TitaniaStampedUnitResponse(
                                code = "from titania",
                                person =
                                    listOf(
                                        TitaniaStampedPersonResponse(
                                            employeeId = "00177111",
                                            name = "ANKKA IINES",
                                            stampedWorkingTimeEvents =
                                                TitaniaStampedWorkingTimeEvents(
                                                    event =
                                                        listOf(
                                                            TitaniaStampedWorkingTimeEvent(
                                                                date = LocalDate.of(2023, 2, 6),
                                                                beginTime = "0800",
                                                                beginReasonCode = "TA",
                                                                endTime = "1539",
                                                                endReasonCode = null,
                                                            )
                                                        )
                                                ),
                                        )
                                    ),
                            )
                        )
                )
            )
    }

    @Test
    fun `getStampedWorkingTimeEvents with plan and overtime`() {
        db.transaction { tx ->
            val areaId = tx.insert(DevCareArea())
            val unitId = tx.insert(DevDaycare(areaId = areaId))
            val groupId = tx.insert(DevDaycareGroup(daycareId = unitId))
            tx.createEmployee(
                    testEmployee.copy(
                        firstName = "IINES",
                        lastName = "ANKKA",
                        employeeNumber = "177111",
                    )
                )
                .let { (employeeId) ->
                    tx.insert(
                        DevStaffAttendancePlan(
                            employeeId = employeeId,
                            type = StaffAttendanceType.PRESENT,
                            startTime =
                                HelsinkiDateTime.of(LocalDate.of(2022, 10, 19), LocalTime.of(8, 0)),
                            endTime =
                                HelsinkiDateTime.of(
                                    LocalDate.of(2022, 10, 19),
                                    LocalTime.of(16, 0),
                                ),
                            description = null,
                        )
                    )
                    tx.upsertStaffAttendance(
                        attendanceId = null,
                        employeeId = employeeId,
                        groupId = groupId,
                        arrivalTime =
                            HelsinkiDateTime.of(LocalDate.of(2022, 10, 19), LocalTime.of(8, 0)),
                        departureTime =
                            HelsinkiDateTime.of(LocalDate.of(2022, 10, 19), LocalTime.of(13, 10)),
                        occupancyCoefficient = BigDecimal("7.0"),
                        type = StaffAttendanceType.PRESENT,
                    )
                    tx.upsertStaffAttendance(
                        attendanceId = null,
                        employeeId = employeeId,
                        groupId = groupId,
                        arrivalTime =
                            HelsinkiDateTime.of(LocalDate.of(2022, 10, 19), LocalTime.of(13, 10)),
                        departureTime =
                            HelsinkiDateTime.of(LocalDate.of(2022, 10, 19), LocalTime.of(16, 11)),
                        occupancyCoefficient = BigDecimal("7.0"),
                        type = StaffAttendanceType.OVERTIME,
                    )
                }
        }

        val response =
            db.transaction { tx ->
                titaniaService.getStampedWorkingTimeEvents(
                    tx,
                    GetStampedWorkingTimeEventsRequest(
                        period =
                            TitaniaPeriod(
                                beginDate = LocalDate.of(2022, 10, 19),
                                endDate = LocalDate.of(2022, 10, 19),
                            ),
                        schedulingUnit =
                            listOf(
                                TitaniaStampedUnitRequest(
                                    code = "from titania",
                                    person =
                                        listOf(TitaniaStampedPersonRequest(employeeId = "00177111")),
                                )
                            ),
                    ),
                )
            }

        assertThat(response)
            .isEqualTo(
                GetStampedWorkingTimeEventsResponse(
                    schedulingUnit =
                        listOf(
                            TitaniaStampedUnitResponse(
                                code = "from titania",
                                person =
                                    listOf(
                                        TitaniaStampedPersonResponse(
                                            employeeId = "00177111",
                                            name = "ANKKA IINES",
                                            stampedWorkingTimeEvents =
                                                TitaniaStampedWorkingTimeEvents(
                                                    event =
                                                        listOf(
                                                            TitaniaStampedWorkingTimeEvent(
                                                                date = LocalDate.of(2022, 10, 19),
                                                                beginTime = "0800",
                                                                beginReasonCode = null,
                                                                endTime = "1310",
                                                                endReasonCode = null,
                                                            ),
                                                            TitaniaStampedWorkingTimeEvent(
                                                                date = LocalDate.of(2022, 10, 19),
                                                                beginTime = "1310",
                                                                beginReasonCode = null,
                                                                endTime = "1611",
                                                                endReasonCode = "YT",
                                                            ),
                                                        )
                                                ),
                                        )
                                    ),
                            )
                        )
                )
            )
    }

    @Test
    fun `getStampedWorkingTimeEvents with plan and overtime without departed`() {
        db.transaction { tx ->
            val areaId = tx.insert(DevCareArea())
            val unitId = tx.insert(DevDaycare(areaId = areaId))
            val groupId = tx.insert(DevDaycareGroup(daycareId = unitId))
            tx.createEmployee(
                    testEmployee.copy(
                        firstName = "IINES",
                        lastName = "ANKKA",
                        employeeNumber = "177111",
                    )
                )
                .let { (employeeId) ->
                    tx.insert(
                        DevStaffAttendancePlan(
                            employeeId = employeeId,
                            type = StaffAttendanceType.PRESENT,
                            startTime =
                                HelsinkiDateTime.of(LocalDate.of(2022, 10, 19), LocalTime.of(8, 0)),
                            endTime =
                                HelsinkiDateTime.of(
                                    LocalDate.of(2022, 10, 19),
                                    LocalTime.of(16, 0),
                                ),
                            description = null,
                        )
                    )
                    tx.upsertStaffAttendance(
                        attendanceId = null,
                        employeeId = employeeId,
                        groupId = groupId,
                        arrivalTime =
                            HelsinkiDateTime.of(LocalDate.of(2022, 10, 19), LocalTime.of(8, 0)),
                        departureTime =
                            HelsinkiDateTime.of(LocalDate.of(2022, 10, 19), LocalTime.of(13, 10)),
                        occupancyCoefficient = BigDecimal("7.0"),
                        type = StaffAttendanceType.PRESENT,
                    )
                    tx.upsertStaffAttendance(
                        attendanceId = null,
                        employeeId = employeeId,
                        groupId = groupId,
                        arrivalTime =
                            HelsinkiDateTime.of(LocalDate.of(2022, 10, 19), LocalTime.of(13, 10)),
                        departureTime = null,
                        occupancyCoefficient = BigDecimal("7.0"),
                        type = StaffAttendanceType.OVERTIME,
                    )
                }
        }

        val response =
            db.transaction { tx ->
                titaniaService.getStampedWorkingTimeEvents(
                    tx,
                    GetStampedWorkingTimeEventsRequest(
                        period =
                            TitaniaPeriod(
                                beginDate = LocalDate.of(2022, 10, 19),
                                endDate = LocalDate.of(2022, 10, 19),
                            ),
                        schedulingUnit =
                            listOf(
                                TitaniaStampedUnitRequest(
                                    code = "from titania",
                                    person =
                                        listOf(TitaniaStampedPersonRequest(employeeId = "00177111")),
                                )
                            ),
                    ),
                )
            }

        assertThat(response)
            .isEqualTo(
                GetStampedWorkingTimeEventsResponse(
                    schedulingUnit =
                        listOf(
                            TitaniaStampedUnitResponse(
                                code = "from titania",
                                person =
                                    listOf(
                                        TitaniaStampedPersonResponse(
                                            employeeId = "00177111",
                                            name = "ANKKA IINES",
                                            stampedWorkingTimeEvents =
                                                TitaniaStampedWorkingTimeEvents(
                                                    event =
                                                        listOf(
                                                            TitaniaStampedWorkingTimeEvent(
                                                                date = LocalDate.of(2022, 10, 19),
                                                                beginTime = "0800",
                                                                beginReasonCode = null,
                                                                endTime = "1310",
                                                                endReasonCode = null,
                                                            ),
                                                            TitaniaStampedWorkingTimeEvent(
                                                                date = LocalDate.of(2022, 10, 19),
                                                                beginTime = "1310",
                                                                beginReasonCode = null,
                                                                endTime = null,
                                                                endReasonCode = null,
                                                            ),
                                                        )
                                                ),
                                        )
                                    ),
                            )
                        )
                )
            )
    }

    @Test
    fun `getStampedWorkingTimeEvents with plan and justified change inside plan`() {
        db.transaction { tx ->
            val areaId = tx.insert(DevCareArea())
            val unitId = tx.insert(DevDaycare(areaId = areaId))
            val groupId = tx.insert(DevDaycareGroup(daycareId = unitId))
            tx.createEmployee(
                    testEmployee.copy(
                        firstName = "IINES",
                        lastName = "ANKKA",
                        employeeNumber = "177111",
                    )
                )
                .let { (employeeId) ->
                    tx.insert(
                        DevStaffAttendancePlan(
                            employeeId = employeeId,
                            type = StaffAttendanceType.PRESENT,
                            startTime =
                                HelsinkiDateTime.of(LocalDate.of(2022, 10, 19), LocalTime.of(8, 0)),
                            endTime =
                                HelsinkiDateTime.of(
                                    LocalDate.of(2022, 10, 19),
                                    LocalTime.of(16, 0),
                                ),
                            description = null,
                        )
                    )
                    tx.upsertStaffAttendance(
                        attendanceId = null,
                        employeeId = employeeId,
                        groupId = groupId,
                        arrivalTime =
                            HelsinkiDateTime.of(LocalDate.of(2022, 10, 19), LocalTime.of(8, 21)),
                        departureTime =
                            HelsinkiDateTime.of(LocalDate.of(2022, 10, 19), LocalTime.of(9, 52)),
                        occupancyCoefficient = BigDecimal("7.0"),
                        type = StaffAttendanceType.JUSTIFIED_CHANGE,
                    )
                    tx.upsertStaffAttendance(
                        attendanceId = null,
                        employeeId = employeeId,
                        groupId = null,
                        arrivalTime =
                            HelsinkiDateTime.of(LocalDate.of(2022, 10, 19), LocalTime.of(9, 52)),
                        departureTime =
                            HelsinkiDateTime.of(LocalDate.of(2022, 10, 19), LocalTime.of(10, 48)),
                        occupancyCoefficient = BigDecimal("7.0"),
                        type = StaffAttendanceType.OTHER_WORK,
                    )
                    tx.upsertStaffAttendance(
                        attendanceId = null,
                        employeeId = employeeId,
                        groupId = groupId,
                        arrivalTime =
                            HelsinkiDateTime.of(LocalDate.of(2022, 10, 19), LocalTime.of(10, 48)),
                        departureTime =
                            HelsinkiDateTime.of(LocalDate.of(2022, 10, 19), LocalTime.of(15, 21)),
                        occupancyCoefficient = BigDecimal("7.0"),
                        type = StaffAttendanceType.JUSTIFIED_CHANGE,
                    )
                }
        }

        val response =
            db.transaction { tx ->
                titaniaService.getStampedWorkingTimeEvents(
                    tx,
                    GetStampedWorkingTimeEventsRequest(
                        period =
                            TitaniaPeriod(
                                beginDate = LocalDate.of(2022, 10, 19),
                                endDate = LocalDate.of(2022, 10, 19),
                            ),
                        schedulingUnit =
                            listOf(
                                TitaniaStampedUnitRequest(
                                    code = "from titania",
                                    person =
                                        listOf(TitaniaStampedPersonRequest(employeeId = "00177111")),
                                )
                            ),
                    ),
                )
            }

        assertThat(response)
            .isEqualTo(
                GetStampedWorkingTimeEventsResponse(
                    schedulingUnit =
                        listOf(
                            TitaniaStampedUnitResponse(
                                code = "from titania",
                                person =
                                    listOf(
                                        TitaniaStampedPersonResponse(
                                            employeeId = "00177111",
                                            name = "ANKKA IINES",
                                            stampedWorkingTimeEvents =
                                                TitaniaStampedWorkingTimeEvents(
                                                    event =
                                                        listOf(
                                                            TitaniaStampedWorkingTimeEvent(
                                                                date = LocalDate.of(2022, 10, 19),
                                                                beginTime = "0821",
                                                                beginReasonCode = "PM",
                                                                endTime = "0952",
                                                                endReasonCode = null,
                                                            ),
                                                            TitaniaStampedWorkingTimeEvent(
                                                                date = LocalDate.of(2022, 10, 19),
                                                                beginTime = "0952",
                                                                beginReasonCode = "TA",
                                                                endTime = "1048",
                                                                endReasonCode = null,
                                                            ),
                                                            TitaniaStampedWorkingTimeEvent(
                                                                date = LocalDate.of(2022, 10, 19),
                                                                beginTime = "1048",
                                                                beginReasonCode = null,
                                                                endTime = "1521",
                                                                endReasonCode = "PM",
                                                            ),
                                                        )
                                                ),
                                        )
                                    ),
                            )
                        )
                )
            )
    }

    @Test
    fun `getStampedWorkingTimeEvents with plan and justified change outside plan`() {
        db.transaction { tx ->
            val areaId = tx.insert(DevCareArea())
            val unitId = tx.insert(DevDaycare(areaId = areaId))
            val groupId = tx.insert(DevDaycareGroup(daycareId = unitId))
            tx.createEmployee(
                    testEmployee.copy(
                        firstName = "IINES",
                        lastName = "ANKKA",
                        employeeNumber = "177111",
                    )
                )
                .let { (employeeId) ->
                    tx.insert(
                        DevStaffAttendancePlan(
                            employeeId = employeeId,
                            type = StaffAttendanceType.PRESENT,
                            startTime =
                                HelsinkiDateTime.of(LocalDate.of(2022, 10, 19), LocalTime.of(8, 0)),
                            endTime =
                                HelsinkiDateTime.of(
                                    LocalDate.of(2022, 10, 19),
                                    LocalTime.of(16, 0),
                                ),
                            description = null,
                        )
                    )
                    tx.upsertStaffAttendance(
                        attendanceId = null,
                        employeeId = employeeId,
                        groupId = groupId,
                        arrivalTime =
                            HelsinkiDateTime.of(LocalDate.of(2022, 10, 19), LocalTime.of(7, 39)),
                        departureTime =
                            HelsinkiDateTime.of(LocalDate.of(2022, 10, 19), LocalTime.of(9, 52)),
                        occupancyCoefficient = BigDecimal("7.0"),
                        type = StaffAttendanceType.JUSTIFIED_CHANGE,
                    )
                    tx.upsertStaffAttendance(
                        attendanceId = null,
                        employeeId = employeeId,
                        groupId = null,
                        arrivalTime =
                            HelsinkiDateTime.of(LocalDate.of(2022, 10, 19), LocalTime.of(9, 52)),
                        departureTime =
                            HelsinkiDateTime.of(LocalDate.of(2022, 10, 19), LocalTime.of(10, 48)),
                        occupancyCoefficient = BigDecimal("7.0"),
                        type = StaffAttendanceType.OTHER_WORK,
                    )
                    tx.upsertStaffAttendance(
                        attendanceId = null,
                        employeeId = employeeId,
                        groupId = groupId,
                        arrivalTime =
                            HelsinkiDateTime.of(LocalDate.of(2022, 10, 19), LocalTime.of(10, 48)),
                        departureTime =
                            HelsinkiDateTime.of(LocalDate.of(2022, 10, 19), LocalTime.of(16, 39)),
                        occupancyCoefficient = BigDecimal("7.0"),
                        type = StaffAttendanceType.JUSTIFIED_CHANGE,
                    )
                }
        }

        val response =
            db.transaction { tx ->
                titaniaService.getStampedWorkingTimeEvents(
                    tx,
                    GetStampedWorkingTimeEventsRequest(
                        period =
                            TitaniaPeriod(
                                beginDate = LocalDate.of(2022, 10, 19),
                                endDate = LocalDate.of(2022, 10, 19),
                            ),
                        schedulingUnit =
                            listOf(
                                TitaniaStampedUnitRequest(
                                    code = "from titania",
                                    person =
                                        listOf(TitaniaStampedPersonRequest(employeeId = "00177111")),
                                )
                            ),
                    ),
                )
            }

        assertThat(response)
            .isEqualTo(
                GetStampedWorkingTimeEventsResponse(
                    schedulingUnit =
                        listOf(
                            TitaniaStampedUnitResponse(
                                code = "from titania",
                                person =
                                    listOf(
                                        TitaniaStampedPersonResponse(
                                            employeeId = "00177111",
                                            name = "ANKKA IINES",
                                            stampedWorkingTimeEvents =
                                                TitaniaStampedWorkingTimeEvents(
                                                    event =
                                                        listOf(
                                                            TitaniaStampedWorkingTimeEvent(
                                                                date = LocalDate.of(2022, 10, 19),
                                                                beginTime = "0739",
                                                                beginReasonCode = "PM",
                                                                endTime = "0952",
                                                                endReasonCode = null,
                                                            ),
                                                            TitaniaStampedWorkingTimeEvent(
                                                                date = LocalDate.of(2022, 10, 19),
                                                                beginTime = "0952",
                                                                beginReasonCode = "TA",
                                                                endTime = "1048",
                                                                endReasonCode = null,
                                                            ),
                                                            TitaniaStampedWorkingTimeEvent(
                                                                date = LocalDate.of(2022, 10, 19),
                                                                beginTime = "1048",
                                                                beginReasonCode = null,
                                                                endTime = "1639",
                                                                endReasonCode = "PM",
                                                            ),
                                                        )
                                                ),
                                        )
                                    ),
                            )
                        )
                )
            )
    }

    @Test
    fun `getStampedWorkingTimeEvents with plan and justified change without departed`() {
        db.transaction { tx ->
            val areaId = tx.insert(DevCareArea())
            val unitId = tx.insert(DevDaycare(areaId = areaId))
            val groupId = tx.insert(DevDaycareGroup(daycareId = unitId))
            tx.createEmployee(
                    testEmployee.copy(
                        firstName = "IINES",
                        lastName = "ANKKA",
                        employeeNumber = "177111",
                    )
                )
                .let { (employeeId) ->
                    tx.insert(
                        DevStaffAttendancePlan(
                            employeeId = employeeId,
                            type = StaffAttendanceType.PRESENT,
                            startTime =
                                HelsinkiDateTime.of(LocalDate.of(2022, 10, 19), LocalTime.of(8, 0)),
                            endTime =
                                HelsinkiDateTime.of(
                                    LocalDate.of(2022, 10, 19),
                                    LocalTime.of(16, 0),
                                ),
                            description = null,
                        )
                    )
                    tx.upsertStaffAttendance(
                        attendanceId = null,
                        employeeId = employeeId,
                        groupId = groupId,
                        arrivalTime =
                            HelsinkiDateTime.of(LocalDate.of(2022, 10, 19), LocalTime.of(8, 21)),
                        departureTime = null,
                        occupancyCoefficient = BigDecimal("7.0"),
                        type = StaffAttendanceType.JUSTIFIED_CHANGE,
                    )
                }
        }

        val response =
            db.transaction { tx ->
                titaniaService.getStampedWorkingTimeEvents(
                    tx,
                    GetStampedWorkingTimeEventsRequest(
                        period =
                            TitaniaPeriod(
                                beginDate = LocalDate.of(2022, 10, 19),
                                endDate = LocalDate.of(2022, 10, 19),
                            ),
                        schedulingUnit =
                            listOf(
                                TitaniaStampedUnitRequest(
                                    code = "from titania",
                                    person =
                                        listOf(TitaniaStampedPersonRequest(employeeId = "00177111")),
                                )
                            ),
                    ),
                )
            }

        assertThat(response)
            .isEqualTo(
                GetStampedWorkingTimeEventsResponse(
                    schedulingUnit =
                        listOf(
                            TitaniaStampedUnitResponse(
                                code = "from titania",
                                person =
                                    listOf(
                                        TitaniaStampedPersonResponse(
                                            employeeId = "00177111",
                                            name = "ANKKA IINES",
                                            stampedWorkingTimeEvents =
                                                TitaniaStampedWorkingTimeEvents(
                                                    event =
                                                        listOf(
                                                            TitaniaStampedWorkingTimeEvent(
                                                                date = LocalDate.of(2022, 10, 19),
                                                                beginTime = "0821",
                                                                beginReasonCode = "PM",
                                                                endTime = null,
                                                                endReasonCode = null,
                                                            )
                                                        )
                                                ),
                                        )
                                    ),
                            )
                        )
                )
            )
    }

    @Test
    fun `getStampedWorkingTimeEvents with overnight plan`() {
        db.transaction { tx ->
            val areaId = tx.insert(DevCareArea())
            val unitId = tx.insert(DevDaycare(areaId = areaId))
            val groupId = tx.insert(DevDaycareGroup(daycareId = unitId))
            tx.createEmployee(
                    testEmployee.copy(
                        firstName = "IINES",
                        lastName = "ANKKA",
                        employeeNumber = "177111",
                    )
                )
                .let { (employeeId) ->
                    tx.insert(
                        DevStaffAttendancePlan(
                            employeeId = employeeId,
                            type = StaffAttendanceType.PRESENT,
                            startTime =
                                HelsinkiDateTime.of(
                                    LocalDate.of(2022, 10, 20),
                                    LocalTime.of(20, 0),
                                ),
                            endTime =
                                HelsinkiDateTime.of(LocalDate.of(2022, 10, 21), LocalTime.of(8, 0)),
                            description = null,
                        )
                    )
                    tx.upsertStaffAttendance(
                        attendanceId = null,
                        employeeId = employeeId,
                        groupId = groupId,
                        arrivalTime =
                            HelsinkiDateTime.of(LocalDate.of(2022, 10, 20), LocalTime.of(20, 5)),
                        departureTime =
                            HelsinkiDateTime.of(LocalDate.of(2022, 10, 21), LocalTime.of(7, 56)),
                        occupancyCoefficient = BigDecimal("7.0"),
                        type = StaffAttendanceType.PRESENT,
                    )
                }
        }

        val response =
            db.transaction { tx ->
                titaniaService.getStampedWorkingTimeEvents(
                    tx,
                    GetStampedWorkingTimeEventsRequest(
                        period =
                            TitaniaPeriod(
                                beginDate = LocalDate.of(2022, 10, 20),
                                endDate = LocalDate.of(2022, 10, 20),
                            ),
                        schedulingUnit =
                            listOf(
                                TitaniaStampedUnitRequest(
                                    code = "from titania",
                                    person =
                                        listOf(TitaniaStampedPersonRequest(employeeId = "00177111")),
                                )
                            ),
                    ),
                )
            }

        assertThat(response)
            .isEqualTo(
                GetStampedWorkingTimeEventsResponse(
                    schedulingUnit =
                        listOf(
                            TitaniaStampedUnitResponse(
                                code = "from titania",
                                person =
                                    listOf(
                                        TitaniaStampedPersonResponse(
                                            employeeId = "00177111",
                                            name = "ANKKA IINES",
                                            stampedWorkingTimeEvents =
                                                TitaniaStampedWorkingTimeEvents(
                                                    event =
                                                        listOf(
                                                            TitaniaStampedWorkingTimeEvent(
                                                                date = LocalDate.of(2022, 10, 20),
                                                                beginTime = "2000",
                                                                beginReasonCode = null,
                                                                endTime = "2400",
                                                                endReasonCode = null,
                                                            )
                                                        )
                                                ),
                                        )
                                    ),
                            )
                        )
                )
            )
    }

    @Test
    fun `getStampedWorkingTimeEvents with overnight plan and justified change inside plan`() {
        db.transaction { tx ->
            val areaId = tx.insert(DevCareArea())
            val unitId = tx.insert(DevDaycare(areaId = areaId))
            val groupId = tx.insert(DevDaycareGroup(daycareId = unitId))
            tx.createEmployee(
                    testEmployee.copy(
                        firstName = "IINES",
                        lastName = "ANKKA",
                        employeeNumber = "177111",
                    )
                )
                .let { (employeeId) ->
                    tx.insert(
                        DevStaffAttendancePlan(
                            employeeId = employeeId,
                            type = StaffAttendanceType.PRESENT,
                            startTime =
                                HelsinkiDateTime.of(
                                    LocalDate.of(2022, 10, 20),
                                    LocalTime.of(20, 0),
                                ),
                            endTime =
                                HelsinkiDateTime.of(LocalDate.of(2022, 10, 21), LocalTime.of(8, 0)),
                            description = null,
                        )
                    )
                    tx.upsertStaffAttendance(
                        attendanceId = null,
                        employeeId = employeeId,
                        groupId = groupId,
                        arrivalTime =
                            HelsinkiDateTime.of(LocalDate.of(2022, 10, 20), LocalTime.of(20, 30)),
                        departureTime =
                            HelsinkiDateTime.of(LocalDate.of(2022, 10, 20), LocalTime.of(21, 15)),
                        occupancyCoefficient = BigDecimal("7.0"),
                        type = StaffAttendanceType.JUSTIFIED_CHANGE,
                    )
                    tx.upsertStaffAttendance(
                        attendanceId = null,
                        employeeId = employeeId,
                        groupId = null,
                        arrivalTime =
                            HelsinkiDateTime.of(LocalDate.of(2022, 10, 20), LocalTime.of(21, 15)),
                        departureTime =
                            HelsinkiDateTime.of(LocalDate.of(2022, 10, 20), LocalTime.of(23, 10)),
                        occupancyCoefficient = BigDecimal("7.0"),
                        type = StaffAttendanceType.OTHER_WORK,
                    )
                    tx.upsertStaffAttendance(
                        attendanceId = null,
                        employeeId = employeeId,
                        groupId = groupId,
                        arrivalTime =
                            HelsinkiDateTime.of(LocalDate.of(2022, 10, 20), LocalTime.of(23, 10)),
                        departureTime =
                            HelsinkiDateTime.of(LocalDate.of(2022, 10, 21), LocalTime.of(7, 30)),
                        occupancyCoefficient = BigDecimal("7.0"),
                        type = StaffAttendanceType.JUSTIFIED_CHANGE,
                    )
                }
        }

        val response =
            db.transaction { tx ->
                titaniaService.getStampedWorkingTimeEvents(
                    tx,
                    GetStampedWorkingTimeEventsRequest(
                        period =
                            TitaniaPeriod(
                                beginDate = LocalDate.of(2022, 10, 20),
                                endDate = LocalDate.of(2022, 10, 21),
                            ),
                        schedulingUnit =
                            listOf(
                                TitaniaStampedUnitRequest(
                                    code = "from titania",
                                    person =
                                        listOf(TitaniaStampedPersonRequest(employeeId = "00177111")),
                                )
                            ),
                    ),
                )
            }

        val actual =
            response.schedulingUnit.flatMap { unit ->
                unit.person.flatMap { person -> person.stampedWorkingTimeEvents.event }
            }
        val expected =
            listOf(
                TitaniaStampedWorkingTimeEvent(
                    date = LocalDate.of(2022, 10, 20),
                    beginTime = "2030",
                    beginReasonCode = "PM",
                    endTime = "2115",
                    endReasonCode = null,
                ),
                TitaniaStampedWorkingTimeEvent(
                    date = LocalDate.of(2022, 10, 20),
                    beginTime = "2115",
                    beginReasonCode = "TA",
                    endTime = "2310",
                    endReasonCode = null,
                ),
                TitaniaStampedWorkingTimeEvent(
                    date = LocalDate.of(2022, 10, 20),
                    beginTime = "2310",
                    beginReasonCode = null,
                    endTime = "2400",
                    endReasonCode = null,
                ),
                TitaniaStampedWorkingTimeEvent(
                    date = LocalDate.of(2022, 10, 21),
                    beginTime = "0000",
                    beginReasonCode = null,
                    endTime = "0730",
                    endReasonCode = "PM",
                ),
            )
        assertThat(actual).containsExactlyInAnyOrderElementsOf(expected)
    }

    @Test
    fun `getStampedWorkingTimeEvents with attendance not within plan`() {
        db.transaction { tx ->
            val areaId = tx.insert(DevCareArea())
            val unitId = tx.insert(DevDaycare(areaId = areaId))
            val groupId = tx.insert(DevDaycareGroup(daycareId = unitId))
            tx.createEmployee(
                    testEmployee.copy(
                        firstName = "IINES",
                        lastName = "ANKKA",
                        employeeNumber = "177111",
                    )
                )
                .let { (employeeId) ->
                    tx.insert(
                        DevStaffAttendancePlan(
                            employeeId = employeeId,
                            type = StaffAttendanceType.PRESENT,
                            startTime =
                                HelsinkiDateTime.of(LocalDate.of(2022, 10, 20), LocalTime.of(8, 0)),
                            endTime =
                                HelsinkiDateTime.of(
                                    LocalDate.of(2022, 10, 20),
                                    LocalTime.of(16, 0),
                                ),
                            description = null,
                        )
                    )
                    tx.upsertStaffAttendance(
                        attendanceId = null,
                        employeeId = employeeId,
                        groupId = groupId,
                        arrivalTime =
                            HelsinkiDateTime.of(LocalDate.of(2022, 10, 20), LocalTime.of(7, 54)),
                        departureTime =
                            HelsinkiDateTime.of(LocalDate.of(2022, 10, 20), LocalTime.of(16, 6)),
                        occupancyCoefficient = BigDecimal("7.0"),
                        type = StaffAttendanceType.PRESENT,
                    )
                }
        }

        val response =
            db.transaction { tx ->
                titaniaService.getStampedWorkingTimeEvents(
                    tx,
                    GetStampedWorkingTimeEventsRequest(
                        period =
                            TitaniaPeriod(
                                beginDate = LocalDate.of(2022, 10, 20),
                                endDate = LocalDate.of(2022, 10, 20),
                            ),
                        schedulingUnit =
                            listOf(
                                TitaniaStampedUnitRequest(
                                    code = "from titania",
                                    person =
                                        listOf(TitaniaStampedPersonRequest(employeeId = "00177111")),
                                )
                            ),
                    ),
                )
            }

        assertThat(response)
            .isEqualTo(
                GetStampedWorkingTimeEventsResponse(
                    schedulingUnit =
                        listOf(
                            TitaniaStampedUnitResponse(
                                code = "from titania",
                                person =
                                    listOf(
                                        TitaniaStampedPersonResponse(
                                            employeeId = "00177111",
                                            name = "ANKKA IINES",
                                            stampedWorkingTimeEvents =
                                                TitaniaStampedWorkingTimeEvents(
                                                    event =
                                                        listOf(
                                                            TitaniaStampedWorkingTimeEvent(
                                                                date = LocalDate.of(2022, 10, 20),
                                                                beginTime = "0754",
                                                                beginReasonCode = null,
                                                                endTime = "1606",
                                                                endReasonCode = null,
                                                            )
                                                        )
                                                ),
                                        )
                                    ),
                            )
                        )
                )
            )
    }
}

fun Database.Read.fetchReportRows(): List<TitaniaTestDbRow> =
    createQuery {
            sql(
                """
                SELECT request_time, employee_id, shift_date, shift_begins, shift_ends, overlapping_shift_begins, overlapping_shift_ends
                FROM titania_errors
            """
                    .trimIndent()
            )
        }
        .toList<TitaniaTestDbRow>()

data class TitaniaTestDbRow(
    val requestTime: HelsinkiDateTime,
    val employeeId: EmployeeId,
    val shiftDate: LocalDate,
    val shiftBegins: LocalTime,
    val shiftEnds: LocalTime,
    val overlappingShiftBegins: LocalTime,
    val overlappingShiftEnds: LocalTime,
)
