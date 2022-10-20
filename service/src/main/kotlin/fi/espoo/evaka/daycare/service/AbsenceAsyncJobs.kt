// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.daycare.service

import fi.espoo.evaka.shared.async.AsyncJob
import fi.espoo.evaka.shared.async.AsyncJobRunner
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.EvakaClock
import org.springframework.stereotype.Component

@Component
class AbsenceAsyncJobs(private val asyncJobRunner: AsyncJobRunner<AsyncJob>) {
    init {
        asyncJobRunner.registerHandler(::generateIrregularAbsences)
    }

    private fun generateIrregularAbsences(
        dbc: Database.Connection,
        clock: EvakaClock,
        payload: AsyncJob.UpdateIrregularAbsences
    ) {
        dbc.transaction { tx ->
            generateAbsencesFromIrregularDailyServiceTimes(tx, clock.now(), payload.childId)
        }
    }
}
