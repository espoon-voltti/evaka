// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.espoo

import fi.espoo.evaka.espoo.bi.EspooBiJob
import fi.espoo.evaka.espoo.bi.EspooBiTable
import fi.espoo.evaka.linkity.LinkitySyncService
import fi.espoo.evaka.shared.async.AsyncJobPayload
import fi.espoo.evaka.shared.async.AsyncJobPool
import fi.espoo.evaka.shared.async.AsyncJobRunner
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.domain.FiniteDateRange

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
    espooBiJob: EspooBiJob,
    linkitySyncService: LinkitySyncService,
) {
    init {
        espooBiJob.let { runner.registerHandler(it::sendBiTable) }
        linkitySyncService.let { runner.registerHandler(it::getStaffAttendancePlans) }
    }
}
