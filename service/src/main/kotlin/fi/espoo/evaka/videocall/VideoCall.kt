// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.videocall

import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.EmployeeId
import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import java.util.UUID

enum class VideoCallRole {
    EMPLOYEE,
    CITIZEN,
}

enum class VideoCallRoomStatus {
    RINGING,
    ACTIVE,
    ENDED,
}

data class VideoCallRoom(
    val id: UUID,
    val employeeId: EmployeeId,
    val employeeName: String,
    val childId: ChildId,
    val childName: String,
    val guardianIds: Set<PersonId>,
    val createdAt: HelsinkiDateTime,
    val status: VideoCallRoomStatus,
    val answeredBy: PersonId?,
    val endedAt: HelsinkiDateTime?,
)

data class VideoCallSignal(
    val seq: Long,
    val from: VideoCallRole,
    val kind: String,
    val data: String,
)

data class StartVideoCallRequest(val childId: ChildId)

data class StartVideoCallResponse(
    val roomId: UUID,
    val childName: String,
    val guardianIds: List<PersonId>,
)

data class PendingVideoCall(
    val roomId: UUID,
    val employeeName: String,
    val childName: String,
    val createdAt: HelsinkiDateTime,
)

data class VideoCallRoomInfo(
    val roomId: UUID,
    val employeeName: String,
    val childName: String,
    val status: VideoCallRoomStatus,
    val yourRole: VideoCallRole,
)

data class PostSignalRequest(val kind: String, val data: String)

data class SignalsResponse(val signals: List<VideoCallSignal>, val nextSince: Long)
