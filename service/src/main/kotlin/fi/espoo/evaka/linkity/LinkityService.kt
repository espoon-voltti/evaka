// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.linkity

import fi.espoo.evaka.attendance.StaffAttendancePlan
import fi.espoo.evaka.attendance.StaffAttendanceType
import fi.espoo.evaka.attendance.deleteStaffAttendancePlansBy
import fi.espoo.evaka.attendance.insertStaffAttendancePlans
import fi.espoo.evaka.pis.getEmployeeIdsByNumbers
import fi.espoo.evaka.shared.EmployeeId
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.shared.utils.partitionIndexed
import io.github.oshai.kotlinlogging.KotlinLogging

private val logger = KotlinLogging.logger {}

fun updateStaffAttendancePlansFromLinkity(
    period: FiniteDateRange,
    tx: Database.Transaction,
    client: LinkityClient,
) {
    val linkityShifts = client.getShifts(period)

    val sarastiaIds = linkityShifts.map { it.sarastiaId }.toSet()
    val sarastiaIdToEmployeeId = tx.getEmployeeIdsByNumbers(sarastiaIds)

    val shifts = filterValidShifts(linkityShifts, sarastiaIdToEmployeeId)

    val staffAttendancePlans =
        shifts.mapNotNull {
            StaffAttendancePlan(
                employeeId = sarastiaIdToEmployeeId[it.sarastiaId] ?: return@mapNotNull null,
                type =
                    when (it.type) {
                        ShiftType.PRESENT -> StaffAttendanceType.PRESENT
                        ShiftType.TRAINING -> StaffAttendanceType.TRAINING
                    },
                startTime = it.startDateTime,
                endTime = it.endDateTime,
                description = it.notes,
            )
        }

    tx.deleteStaffAttendancePlansBy(period = period)
    logger.debug { "Deleted all staff attendance plans for period $period" }

    tx.insertStaffAttendancePlans(staffAttendancePlans)
    logger.debug { "Inserted ${staffAttendancePlans.size} staff attendance plans" }
}

private fun filterValidShifts(
    shifts: List<Shift>,
    sarastiaIdToEmployeeId: Map<String, EmployeeId>,
): List<Shift> {
    val (withKnownSarastiaId, withUnknownSarastiaId) =
        shifts.partition { sarastiaIdToEmployeeId.containsKey(it.sarastiaId) }
    if (withUnknownSarastiaId.isNotEmpty()) {
        logger.info {
            "No employee found for Sarastia IDs: ${withUnknownSarastiaId.map { it.sarastiaId }}"
        }
    }
    val (validTimesShifts, invalidTimesShifts) =
        withKnownSarastiaId.partition { it.startDateTime < it.endDateTime }
    if (invalidTimesShifts.isNotEmpty()) {
        logger.info { "Shifts with invalid times: ${invalidTimesShifts.map { it.workShiftId }}" }
    }
    val sortedShifts =
        validTimesShifts.sortedWith(compareBy<Shift> { it.sarastiaId }.thenBy { it.startDateTime })
    val (validShifts, overlappingShifts) =
        sortedShifts.partitionIndexed { i, shift ->
            i == 0 ||
                shift.sarastiaId != sortedShifts[i - 1].sarastiaId ||
                shift.startDateTime >= sortedShifts[i - 1].endDateTime
        }
    if (overlappingShifts.isNotEmpty()) {
        logger.info { "Overlapping shifts: ${overlappingShifts.map { it.workShiftId }}" }
    }
    return validShifts
}
