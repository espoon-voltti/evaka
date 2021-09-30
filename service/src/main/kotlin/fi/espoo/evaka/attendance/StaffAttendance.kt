package fi.espoo.evaka.attendance

import fi.espoo.evaka.shared.EmployeeId
import fi.espoo.evaka.shared.GroupId
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import org.jdbi.v3.core.mapper.Nested
import org.jdbi.v3.core.mapper.PropagateNull

data class StaffAttendanceResponse(
    val staff: List<StaffMember>,
    val extraAttendances: List<ExternalStaffMember>,
)

data class ExternalStaffMember(
    val name: String,
    val groupId: GroupId,
    val arrived: HelsinkiDateTime,
)

data class StaffMember(
    val employeeId: EmployeeId,
    val firstName: String,
    val lastName: String,
    val groupIds: List<GroupId>,
    @Nested("attendance")
    val latestCurrentDayAttendance: StaffMemberAttendance?
) {
    val present: GroupId?
        get() = latestCurrentDayAttendance?.takeIf { it.departed == null }?.groupId
}

data class StaffMemberAttendance(
    @PropagateNull
    val employeeId: EmployeeId,
    val groupId: GroupId,
    val arrived: HelsinkiDateTime,
    val departed: HelsinkiDateTime?
)
