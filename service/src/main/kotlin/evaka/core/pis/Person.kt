// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.pis

import evaka.core.messaging.createPersonMessageAccount
import evaka.core.pis.controllers.CreatePersonBody
import evaka.core.shared.PersonId
import evaka.core.shared.async.AsyncJob
import evaka.core.shared.async.AsyncJobRunner
import evaka.core.shared.db.Database
import evaka.core.shared.domain.DateRange
import evaka.core.shared.domain.HelsinkiDateTime
import java.time.LocalDate

fun createPerson(tx: Database.Transaction, person: CreatePersonBody): PersonId {
    val personId = tx.createPerson(person)
    tx.createPersonMessageAccount(personId)
    return personId
}

private val whitespace = "\\s+".toRegex()

/** Splits the first name field, which holds all of a person's first names */
fun splitFirstNames(firstNames: String): List<String> =
    firstNames.trim().split(whitespace).filter { it.isNotEmpty() }

fun isMinor(dateOfBirth: LocalDate, today: LocalDate): Boolean = today < dateOfBirth.plusYears(18)

/**
 * Ends the fridge relations of a person who has died, and plans new finance decisions. Must be
 * called only once per death, when the stored date of death changes from NULL to a date.
 */
fun Database.Transaction.applyDeathEffects(
    asyncJobRunner: AsyncJobRunner<AsyncJob>,
    now: HelsinkiDateTime,
    personId: PersonId,
    dateOfDeath: LocalDate,
) {
    getPartnersForPerson(
            personId,
            includeConflicts = true,
            period = DateRange(dateOfDeath, dateOfDeath),
        )
        .forEach {
            updatePartnershipDuration(
                it.partnershipId,
                it.startDate,
                dateOfDeath,
                ModifySource.DVV,
                now,
                null,
            )
        }

    val parentships =
        getParentships(
            headOfChildId = personId,
            childId = null,
            includeConflicts = true,
            period = DateRange(dateOfDeath, dateOfDeath),
        ) +
            getParentships(
                headOfChildId = null,
                childId = personId,
                includeConflicts = true,
                period = DateRange(dateOfDeath, dateOfDeath),
            )
    parentships.forEach {
        updateParentshipDuration(
            id = it.id,
            startDate = it.startDate,
            endDate = dateOfDeath,
            now = now,
            modifier = Modifier.DVV,
        )
    }

    asyncJobRunner.plan(
        this,
        listOf(
            AsyncJob.GenerateFinanceDecisions.forAdult(personId, DateRange(dateOfDeath, null)),
            AsyncJob.GenerateFinanceDecisions.forChild(personId, DateRange(dateOfDeath, null)),
        ),
        runAt = now,
    )
}
