// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.pis.service

import fi.espoo.evaka.shared.async.AsyncJob
import fi.espoo.evaka.shared.async.AsyncJobRunner
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.EvakaClock
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
