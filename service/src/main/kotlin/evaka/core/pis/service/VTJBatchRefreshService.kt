// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.pis.service

import evaka.core.shared.async.AsyncJob
import evaka.core.shared.async.AsyncJobRunner
import evaka.core.shared.db.Database
import evaka.core.shared.domain.EvakaClock
import org.springframework.stereotype.Service

@Service
class VTJBatchRefreshService(
    private val fridgeFamilyService: FridgeFamilyService,
    asyncJobRunner: AsyncJobRunner<AsyncJob>,
) {

    init {
        asyncJobRunner.registerHandler(::doVTJRefresh)
    }

    fun doVTJRefresh(db: Database.Connection, clock: EvakaClock, msg: AsyncJob.VTJRefresh) {
        fridgeFamilyService.doVTJRefresh(db, msg, clock)
    }
}
