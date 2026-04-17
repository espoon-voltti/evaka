// SPDX-FileCopyrightText: 2023-2024 Tampere region
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.instance.vesilahti.titania

import evaka.core.attendance.StaffAttendanceType
import evaka.core.shared.EmployeeId
import evaka.core.shared.db.Database
import evaka.core.shared.dev.DevEmployee
import evaka.core.shared.dev.DevStaffAttendance
import evaka.core.shared.dev.insert
import evaka.core.shared.domain.HelsinkiDateTime
import evaka.core.titania.GetStampedWorkingTimeEventsRequest
import evaka.core.titania.GetStampedWorkingTimeEventsResponse
import evaka.core.titania.TitaniaOccupation
import evaka.core.titania.TitaniaPeriod
import evaka.core.titania.TitaniaPerson
import evaka.core.titania.TitaniaSchedulingUnit
import evaka.core.titania.TitaniaService
import evaka.core.titania.TitaniaStampedPersonRequest
import evaka.core.titania.TitaniaStampedPersonResponse
import evaka.core.titania.TitaniaStampedUnitRequest
import evaka.core.titania.TitaniaStampedUnitResponse
import evaka.core.titania.TitaniaStampedWorkingTimeEvent
import evaka.core.titania.TitaniaStampedWorkingTimeEvents
import evaka.core.titania.TitaniaWorkingTimeEvent
import evaka.core.titania.TitaniaWorkingTimeEvents
import evaka.core.titania.UpdateWorkingTimeEventsRequest
import evaka.instance.vesilahti.AbstractVesilahtiIntegrationTest
import java.time.LocalDate
import java.time.LocalTime
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class TitaniaServiceTest : AbstractVesilahtiIntegrationTest() {

    @Autowired private lateinit var titaniaService: TitaniaService

    @Test
    fun `updateWorkingTimeEvents should ignore unknown employee number`() {
        val request =
            newUpdateRequest(
                TitaniaPeriod.from(LocalDate.of(2024, 3, 4)),
                TitaniaPerson(
                    employeeId = "1234",
                    name = "ESIMERKKI ELLI",
                    actualWorkingTimeEvents =
                        TitaniaWorkingTimeEvents(
                            event =
                                listOf(
                                    TitaniaWorkingTimeEvent(
                                        date = LocalDate.of(2024, 3, 4),
                                        beginTime = "0800",
                                        endTime = "1600",
                                    )
                                )
                        ),
                ),
            )
        val response = db.transaction { tx -> titaniaService.updateWorkingTimeEvents(tx, request) }
        assertThat(response).returns("OK") { it.updateWorkingTimeEventsResponse.message }

        val employees = db.read { tx -> tx.getDevEmployees() }
        assertThat(employees).isEmpty()
    }

    @Test
    fun `updateWorkingTimeEvents should ignore empty employee number`() {
        val request =
            newUpdateRequest(
                TitaniaPeriod.from(LocalDate.of(2024, 3, 4)),
                TitaniaPerson(
                    employeeId = "",
                    name = "ESIMERKKI ELLI",
                    actualWorkingTimeEvents =
                        TitaniaWorkingTimeEvents(
                            event =
                                listOf(
                                    TitaniaWorkingTimeEvent(
                                        date = LocalDate.of(2024, 3, 4),
                                        beginTime = "0800",
                                        endTime = "1600",
                                    )
                                )
                        ),
                ),
            )
        val response = db.transaction { tx -> titaniaService.updateWorkingTimeEvents(tx, request) }
        assertThat(response).returns("OK") { it.updateWorkingTimeEventsResponse.message }

        val employees = db.read { tx -> tx.getDevEmployees() }
        assertThat(employees).isEmpty()
    }

    @Test
    fun `updateWorkingTimeEvents should find employee without prefix`() {
        val employeeId = db.transaction { tx -> tx.insert(DevEmployee(employeeNumber = "ves1234")) }

        val request =
            newUpdateRequest(
                TitaniaPeriod.from(LocalDate.of(2024, 3, 4)),
                TitaniaPerson(
                    employeeId = "1234",
                    name = "ESIMERKKI ELLI",
                    actualWorkingTimeEvents =
                        TitaniaWorkingTimeEvents(
                            event =
                                listOf(
                                    TitaniaWorkingTimeEvent(
                                        date = LocalDate.of(2024, 3, 4),
                                        beginTime = "0800",
                                        endTime = "1600",
                                    )
                                )
                        ),
                ),
            )
        val response = db.transaction { tx -> titaniaService.updateWorkingTimeEvents(tx, request) }
        assertThat(response).returns("OK") { it.updateWorkingTimeEventsResponse.message }

        val employees = db.read { tx -> tx.getDevEmployees() }
        assertThat(employees).extracting<EmployeeId> { it.id }.containsExactly(employeeId)
    }

    @Test
    fun `updateWorkingTimeEvents should find employee with prefix`() {
        val employeeId = db.transaction { tx -> tx.insert(DevEmployee(employeeNumber = "ves1234")) }

        val request =
            newUpdateRequest(
                TitaniaPeriod.from(LocalDate.of(2024, 3, 4)),
                TitaniaPerson(
                    employeeId = "ves1234",
                    name = "ESIMERKKI ELLI",
                    actualWorkingTimeEvents =
                        TitaniaWorkingTimeEvents(
                            event =
                                listOf(
                                    TitaniaWorkingTimeEvent(
                                        date = LocalDate.of(2024, 3, 4),
                                        beginTime = "0800",
                                        endTime = "1600",
                                    )
                                )
                        ),
                ),
            )
        val response = db.transaction { tx -> titaniaService.updateWorkingTimeEvents(tx, request) }
        assertThat(response).returns("OK") { it.updateWorkingTimeEventsResponse.message }

        val employees = db.read { tx -> tx.getDevEmployees() }
        assertThat(employees).extracting<EmployeeId> { it.id }.containsExactly(employeeId)
    }

    @Test
    fun `getStampedWorkingTimeEvents should return data for employee without prefix`() {
        val date = LocalDate.of(2024, 4, 3)
        db.transaction { tx ->
            val employeeId = tx.insert(DevEmployee(employeeNumber = "ves1234"))
            tx.insert(
                DevStaffAttendance(
                    employeeId = employeeId,
                    arrived = HelsinkiDateTime.of(date = date, LocalTime.of(8, 0)),
                    departed = HelsinkiDateTime.of(date = date, LocalTime.of(16, 0)),
                    type = StaffAttendanceType.TRAINING,
                    modifiedAt = null,
                    modifiedBy = null,
                )
            )
        }

        val request =
            newGetRequest(
                TitaniaPeriod.from(date),
                TitaniaStampedPersonRequest(employeeId = "1234"),
            )
        val response =
            db.transaction { tx -> titaniaService.getStampedWorkingTimeEvents(tx, request) }
        assertThat(response)
            .isEqualTo(
                newGetResponse(
                    TitaniaStampedPersonResponse(
                        employeeId = "1234",
                        name = "PERSON TEST",
                        stampedWorkingTimeEvents =
                            TitaniaStampedWorkingTimeEvents(
                                event =
                                    listOf(
                                        TitaniaStampedWorkingTimeEvent(
                                            date = date,
                                            beginTime = "0800",
                                            beginReasonCode = "KO",
                                            endTime = "1600",
                                            endReasonCode = null,
                                        )
                                    )
                            ),
                    )
                )
            )
    }
}

private fun newUpdateRequest(period: TitaniaPeriod, vararg person: TitaniaPerson) =
    UpdateWorkingTimeEventsRequest(
        period = period,
        schedulingUnit =
            listOf(
                TitaniaSchedulingUnit(
                    code = "x",
                    occupation =
                        listOf(TitaniaOccupation(code = "y", name = "y", person = person.toList())),
                )
            ),
    )

private fun newGetRequest(period: TitaniaPeriod, vararg person: TitaniaStampedPersonRequest) =
    GetStampedWorkingTimeEventsRequest(
        period = period,
        schedulingUnit = listOf(TitaniaStampedUnitRequest(code = "x", person = person.toList())),
    )

private fun newGetResponse(vararg person: TitaniaStampedPersonResponse) =
    GetStampedWorkingTimeEventsResponse(
        schedulingUnit = listOf(TitaniaStampedUnitResponse(code = "x", person = person.toList()))
    )

private fun Database.Read.getDevEmployees(): List<DevEmployee> =
    createQuery { sql("SELECT * FROM employee") }.toList<DevEmployee>()
