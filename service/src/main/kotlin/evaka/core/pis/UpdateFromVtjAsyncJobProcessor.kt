// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.pis

import evaka.core.identity.Ssn
import evaka.core.pis.service.FridgeFamilyService
import evaka.core.pis.service.VtjSyncService
import evaka.core.shared.async.AsyncJob
import evaka.core.shared.async.AsyncJobRunner
import evaka.core.shared.async.PermanentAsyncJobFailure
import evaka.core.shared.auth.AuthenticatedUser
import evaka.core.shared.db.Database
import evaka.core.shared.domain.EvakaClock
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component

private val logger = KotlinLogging.logger {}

@Component
class UpdateFromVtjAsyncJobProcessor(
    asyncJobRunner: AsyncJobRunner<AsyncJob>,
    private val vtjSyncService: VtjSyncService,
    private val fridgeFamilyService: FridgeFamilyService,
) {
    init {
        asyncJobRunner.registerHandler { db, clock, msg: AsyncJob.UpdateFromVtj ->
            updateFromVtj(db, clock, msg)
        }
    }

    fun updateFromVtj(
        db: Database.Connection,
        clock: EvakaClock,
        msg: AsyncJob.UpdateFromVtj,
    ) {
        val ssn =
            Ssn.parse(msg.ssn)
                ?: throw PermanentAsyncJobFailure("Invalid SSN in UpdateFromVtj payload")
        val person = db.read { it.getPersonBySSN(ssn.value) }
        if (person == null) {
            logger.info { "Person of an UpdateFromVtj job is not in eVaka, nothing to refresh" }
            return
        }
        vtjSyncService.refreshPerson(db, AuthenticatedUser.SystemInternalUser, clock, person.id)
        fridgeFamilyService.addChildrenToFridgeFamilies(db, clock, person.id)
    }
}
