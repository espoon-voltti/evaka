// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.pis

import fi.espoo.evaka.identity.ExternalIdentifier
import fi.espoo.evaka.pis.service.FridgeFamilyService
import fi.espoo.evaka.pis.service.PersonService
import fi.espoo.evaka.shared.async.AsyncJob
import fi.espoo.evaka.shared.async.AsyncJobRunner
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.EvakaClock
import mu.KotlinLogging
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
                logger.info("Refreshing all VTJ information for person ${it.id}")
                fridgeFamilyService.doVTJRefresh(db, AsyncJob.VTJRefresh(it.id), evakaClock)
            }
    }
}
