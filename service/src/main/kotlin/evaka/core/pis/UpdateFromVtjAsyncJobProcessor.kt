// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.pis

import evaka.core.identity.ExternalIdentifier
import evaka.core.pis.service.FridgeFamilyService
import evaka.core.pis.service.PersonService
import evaka.core.shared.async.AsyncJob
import evaka.core.shared.async.AsyncJobRunner
import evaka.core.shared.auth.AuthenticatedUser
import evaka.core.shared.db.Database
import evaka.core.shared.domain.EvakaClock
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component

private val logger = KotlinLogging.logger {}

@Component
class UpdateFromVtjAsyncJobProcessor(
    asyncJobRunner: AsyncJobRunner<AsyncJob>,
    private val personService: PersonService,
    private val fridgeFamilyService: FridgeFamilyService,
) {
    init {
        asyncJobRunner.registerHandler { db, clock, msg: AsyncJob.UpdateFromVtj ->
            updateFromVtj(db, clock, msg)
        }
    }

    fun updateFromVtj(
        db: Database.Connection,
        evakaClock: EvakaClock,
        msg: AsyncJob.UpdateFromVtj,
    ) {
        db.transaction { tx ->
                personService.getOrCreatePerson(
                    tx,
                    AuthenticatedUser.SystemInternalUser,
                    ExternalIdentifier.SSN.getInstance(msg.ssn),
                )
            }
            ?.let {
                logger.info { "Refreshing all VTJ information for person ${it.id}" }
                fridgeFamilyService.doVTJRefresh(db, AsyncJob.VTJRefresh(it.id), evakaClock)
            }
    }
}
