// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.pis.service

import fi.espoo.evaka.shared.async.AsyncJobRunner
import fi.espoo.evaka.shared.async.VTJRefresh
import fi.espoo.evaka.shared.db.Database
import org.springframework.stereotype.Service

@Service
class VTJBatchRefreshService(
    private val fridgeFamilyService: FridgeFamilyService,
    asyncJobRunner: AsyncJobRunner
) {

    init {
        asyncJobRunner.vtjRefresh = ::doVTJRefresh
    }

    fun doVTJRefresh(db: Database, msg: VTJRefresh) {
        fridgeFamilyService.doVTJRefresh(db, msg)
    }
}
