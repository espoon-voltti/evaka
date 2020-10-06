package fi.espoo.evaka.attendance

import fi.espoo.evaka.pis.dao.mapPSQLException
import fi.espoo.evaka.shared.db.transaction
import org.jdbi.v3.core.Jdbi
import org.jdbi.v3.core.JdbiException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.OffsetDateTime
import java.util.UUID

@RestController
@RequestMapping("/child-attendances")
class ChildAttendanceController(private val jdbi: Jdbi) {

    @PostMapping("/arrive")
    fun childArrives(
        @RequestBody body: ArrivalRequest
    ): ResponseEntity<ChildAttendance> {
        try {
            return jdbi.transaction {
                it.createAttendance(
                    childId = body.childId,
                    arrived = body.time ?: OffsetDateTime.now()
                )
            }.let { ResponseEntity.ok(it) }
        } catch (e: JdbiException) {
            throw mapPSQLException(e)
        }
    }

    @PostMapping("/depart")
    fun childDeparts(
        @RequestBody body: DepartureRequest
    ): ResponseEntity<Unit> {
        try {
            jdbi.transaction {
                it.updateCurrentAttendanceEnd(
                    childId = body.childId,
                    departed = body.time ?: OffsetDateTime.now()
                )
            }
            return ResponseEntity.status(HttpStatus.OK).build()
        } catch (e: JdbiException) {
            throw mapPSQLException(e)
        }
    }

    @GetMapping("/current")
    fun getChildrenInGroup(
        @RequestParam groupId: UUID
    ): ResponseEntity<List<ChildInGroup>> {
        return jdbi.transaction { it.getChildrenInGroup(groupId) }
            .let { ResponseEntity.ok(it) }
    }
}

data class ArrivalRequest(
    val childId: UUID,
    val time: OffsetDateTime? = null
)

data class DepartureRequest(
    val childId: UUID,
    val time: OffsetDateTime? = null
)

enum class AttendanceStatus {
    COMING, PRESENT, DEPARTED, ABSENT
}

data class ChildInGroup(
    val childId: UUID,
    val firstName: String,
    val lastName: String,
    val status: AttendanceStatus
)
