// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.msg.async

import com.fasterxml.jackson.annotation.JsonIgnore
import fi.espoo.evaka.msg.controllers.PdfSendMessage
import java.time.Duration
import java.time.Instant
import java.util.UUID

enum class AsyncJobType {
    SEND_MESSAGE
}

interface AsyncJobPayload {
    @get:JsonIgnore
    val asyncJobType: AsyncJobType
}

data class SendMessage(val msg: PdfSendMessage) : AsyncJobPayload {
    override val asyncJobType = AsyncJobType.SEND_MESSAGE
}

data class JobParams<T : AsyncJobPayload>(
    val payload: T,
    val retryCount: Int,
    val retryInterval: Duration,
    val runAt: Instant = Instant.now()
)

data class ClaimedJobRef(val jobId: UUID, val jobType: AsyncJobType, val txId: Long)
