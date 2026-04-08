// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.instance.espoo

import evaka.core.linkity.LinkitySyncService
import evaka.core.shared.async.AsyncJobPayload
import evaka.core.shared.async.AsyncJobPool
import evaka.core.shared.async.AsyncJobRunner
import evaka.core.shared.auth.AuthenticatedUser
import evaka.core.shared.domain.FiniteDateRange
import evaka.instance.espoo.bi.EspooBiJob
import evaka.instance.espoo.bi.EspooBiTable

sealed interface EspooAsyncJob : AsyncJobPayload {
    data class SendBiTable(val table: EspooBiTable) : EspooAsyncJob {
        override val user: AuthenticatedUser? = null
    }

    data class GetStaffAttendancePlansFromLinkity(val period: FiniteDateRange) : EspooAsyncJob {
        override val user: AuthenticatedUser? = null
    }

    companion object {
        val pool =
            AsyncJobRunner.Pool(
                AsyncJobPool.Id(EspooAsyncJob::class, "espoo"),
                AsyncJobPool.Config(concurrency = 1),
                setOf(SendBiTable::class, GetStaffAttendancePlansFromLinkity::class),
            )
    }
}

class EspooAsyncJobRegistration(
    runner: AsyncJobRunner<EspooAsyncJob>,
    espooBiJob: EspooBiJob?,
    linkitySyncService: LinkitySyncService,
) {
    init {
        espooBiJob?.let { runner.registerHandler(it::sendBiTable) }
        linkitySyncService.let { runner.registerHandler(it::getStaffAttendancePlans) }
    }
}
