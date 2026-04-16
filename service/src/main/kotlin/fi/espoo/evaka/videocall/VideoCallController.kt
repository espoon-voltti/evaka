// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.videocall

import fi.espoo.evaka.pis.getPersonById
import fi.espoo.evaka.pis.service.getChildGuardiansAndFosterParents
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.EvakaClock
import fi.espoo.evaka.shared.domain.Forbidden
import fi.espoo.evaka.shared.domain.NotFound
import java.util.UUID
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
class VideoCallController(
    private val store: VideoCallRoomStore,
    private val pushSender: VideoCallPushSender,
) {
    @PostMapping("/employee/video-call")
    fun startCall(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @RequestBody body: StartVideoCallRequest,
    ): StartVideoCallResponse {
        val now = clock.now()
        val (employeeName, childName, guardians) =
            db.connect { dbc ->
                dbc.read { tx ->
                    val employee =
                        tx.createQuery {
                                sql(
                                    "SELECT coalesce(preferred_first_name, first_name) || ' ' || last_name FROM employee WHERE id = ${bind(user.id)}"
                                )
                            }
                            .exactlyOneOrNull<String>() ?: "eVaka-henkilökunta"
                    val child = tx.getPersonById(body.childId) ?: throw NotFound("child not found")
                    val childName = "${child.firstName} ${child.lastName}"
                    val guardianIds =
                        tx.getChildGuardiansAndFosterParents(body.childId, now.toLocalDate())
                            .toSet()
                    Triple(employee, childName, guardianIds)
                }
            }
        if (guardians.isEmpty()) throw NotFound("child has no guardians")
        val room =
            store.create(
                employeeId = user.id,
                employeeName = employeeName,
                childId = body.childId,
                childName = childName,
                guardianIds = guardians,
                now = now,
            )
        for (guardianId in guardians) {
            pushSender.notifyIncomingCall(
                db = db,
                clock = clock,
                personId = guardianId,
                roomId = room.id,
                employeeName = employeeName,
                childName = childName,
            )
        }
        return StartVideoCallResponse(
            roomId = room.id,
            childName = childName,
            guardianIds = guardians.toList(),
        )
    }

    @GetMapping("/citizen/video-call/pending")
    fun listPending(user: AuthenticatedUser.Citizen, clock: EvakaClock): List<PendingVideoCall> =
        store.listPendingFor(user.id, clock.now()).map {
            PendingVideoCall(
                roomId = it.id,
                employeeName = it.employeeName,
                childName = it.childName,
                createdAt = it.createdAt,
            )
        }

    @GetMapping("/citizen/video-call/{roomId}")
    fun getRoomCitizen(
        user: AuthenticatedUser.Citizen,
        @PathVariable roomId: UUID,
    ): VideoCallRoomInfo {
        val room = store.get(roomId) ?: throw NotFound("room not found")
        if (user.id !in room.guardianIds) throw Forbidden("not a guardian of this room")
        return VideoCallRoomInfo(
            roomId = room.id,
            employeeName = room.employeeName,
            childName = room.childName,
            status = room.status,
            yourRole = VideoCallRole.CITIZEN,
        )
    }

    @GetMapping("/employee/video-call/{roomId}")
    fun getRoomEmployee(
        user: AuthenticatedUser.Employee,
        @PathVariable roomId: UUID,
    ): VideoCallRoomInfo {
        val room = store.get(roomId) ?: throw NotFound("room not found")
        if (room.employeeId != user.id) throw Forbidden("not the host of this room")
        return VideoCallRoomInfo(
            roomId = room.id,
            employeeName = room.employeeName,
            childName = room.childName,
            status = room.status,
            yourRole = VideoCallRole.EMPLOYEE,
        )
    }

    @PostMapping("/citizen/video-call/{roomId}/accept")
    fun acceptCall(user: AuthenticatedUser.Citizen, @PathVariable roomId: UUID): VideoCallRoomInfo {
        val room = store.get(roomId) ?: throw NotFound("room not found")
        if (user.id !in room.guardianIds) throw Forbidden("not a guardian of this room")
        val accepted = store.accept(roomId, user.id) ?: throw NotFound("room is no longer joinable")
        return VideoCallRoomInfo(
            roomId = accepted.id,
            employeeName = accepted.employeeName,
            childName = accepted.childName,
            status = accepted.status,
            yourRole = VideoCallRole.CITIZEN,
        )
    }

    @PostMapping("/citizen/video-call/{roomId}/end")
    fun endCitizen(user: AuthenticatedUser.Citizen, clock: EvakaClock, @PathVariable roomId: UUID) {
        val room = store.get(roomId) ?: return
        if (user.id !in room.guardianIds) throw Forbidden("not a guardian of this room")
        store.end(roomId, clock.now())
    }

    @PostMapping("/employee/video-call/{roomId}/end")
    fun endEmployee(
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @PathVariable roomId: UUID,
    ) {
        val room = store.get(roomId) ?: return
        if (room.employeeId != user.id) throw Forbidden("not the host of this room")
        store.end(roomId, clock.now())
    }

    @PostMapping("/citizen/video-call/{roomId}/signal")
    fun citizenPostSignal(
        user: AuthenticatedUser.Citizen,
        @PathVariable roomId: UUID,
        @RequestBody body: PostSignalRequest,
    ) {
        val room = store.get(roomId) ?: throw NotFound("room not found")
        if (user.id !in room.guardianIds) throw Forbidden("not a guardian of this room")
        store.postSignal(roomId, VideoCallRole.CITIZEN, body.kind, body.data)
    }

    @PostMapping("/employee/video-call/{roomId}/signal")
    fun employeePostSignal(
        user: AuthenticatedUser.Employee,
        @PathVariable roomId: UUID,
        @RequestBody body: PostSignalRequest,
    ) {
        val room = store.get(roomId) ?: throw NotFound("room not found")
        if (room.employeeId != user.id) throw Forbidden("not the host of this room")
        store.postSignal(roomId, VideoCallRole.EMPLOYEE, body.kind, body.data)
    }

    @GetMapping("/citizen/video-call/{roomId}/signals")
    fun citizenGetSignals(
        user: AuthenticatedUser.Citizen,
        @PathVariable roomId: UUID,
        @RequestParam(defaultValue = "0") since: Long,
    ): SignalsResponse {
        val room = store.get(roomId) ?: throw NotFound("room not found")
        if (user.id !in room.guardianIds) throw Forbidden("not a guardian of this room")
        val list = store.drainSignalsFor(roomId, VideoCallRole.CITIZEN, since)
        return SignalsResponse(signals = list, nextSince = list.maxOfOrNull { it.seq } ?: since)
    }

    @GetMapping("/employee/video-call/{roomId}/signals")
    fun employeeGetSignals(
        user: AuthenticatedUser.Employee,
        @PathVariable roomId: UUID,
        @RequestParam(defaultValue = "0") since: Long,
    ): SignalsResponse {
        val room = store.get(roomId) ?: throw NotFound("room not found")
        if (room.employeeId != user.id) throw Forbidden("not the host of this room")
        val list = store.drainSignalsFor(roomId, VideoCallRole.EMPLOYEE, since)
        return SignalsResponse(signals = list, nextSince = list.maxOfOrNull { it.seq } ?: since)
    }
}
