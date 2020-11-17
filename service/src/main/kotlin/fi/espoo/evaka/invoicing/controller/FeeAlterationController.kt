// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.invoicing.controller

import fi.espoo.evaka.Audit
import fi.espoo.evaka.invoicing.data.deleteFeeAlteration
import fi.espoo.evaka.invoicing.data.getFeeAlteration
import fi.espoo.evaka.invoicing.data.getFeeAlterationsForPerson
import fi.espoo.evaka.invoicing.data.upsertFeeAlteration
import fi.espoo.evaka.invoicing.domain.FeeAlteration
import fi.espoo.evaka.shared.async.AsyncJobRunner
import fi.espoo.evaka.shared.async.NotifyFeeAlterationUpdated
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.config.Roles
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.BadRequest
import fi.espoo.evaka.shared.domain.Period
import fi.espoo.evaka.shared.domain.maxEndDate
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/fee-alterations")
class FeeAlterationController(private val asyncJobRunner: AsyncJobRunner) {
    @GetMapping
    fun getFeeAlterations(db: Database.Connection, user: AuthenticatedUser, @RequestParam personId: String?): ResponseEntity<Wrapper<List<FeeAlteration>>> {
        Audit.ChildFeeAlterationsRead.log(targetId = personId)
        user.requireOneOfRoles(Roles.FINANCE_ADMIN)
        val parsedId = personId?.let { parseUUID(personId) }
            ?: throw BadRequest("Query parameter personId is mandatory")

        val feeAlterations = db.read { getFeeAlterationsForPerson(it.handle, parsedId) }
        return ResponseEntity.ok(Wrapper(feeAlterations))
    }

    @PostMapping
    fun createFeeAlteration(db: Database.Connection, user: AuthenticatedUser, @RequestBody feeAlteration: FeeAlteration): ResponseEntity<Unit> {
        Audit.ChildFeeAlterationsCreate.log(targetId = feeAlteration.personId)
        user.requireOneOfRoles(Roles.FINANCE_ADMIN)
        db.transaction { tx ->
            upsertFeeAlteration(tx.handle, feeAlteration.copy(id = UUID.randomUUID(), updatedBy = user.id))
            asyncJobRunner.plan(
                tx,
                listOf(
                    NotifyFeeAlterationUpdated(
                        feeAlteration.personId,
                        feeAlteration.validFrom,
                        feeAlteration.validTo
                    )
                )
            )
        }

        asyncJobRunner.scheduleImmediateRun()
        return ResponseEntity.noContent().build()
    }

    @PutMapping("/{feeAlterationId}")
    fun updateFeeAlteration(db: Database.Connection, user: AuthenticatedUser, @PathVariable feeAlterationId: String, @RequestBody feeAlteration: FeeAlteration): ResponseEntity<Unit> {
        Audit.ChildFeeAlterationsUpdate.log(targetId = feeAlterationId)
        user.requireOneOfRoles(Roles.FINANCE_ADMIN)
        val parsedId = parseUUID(feeAlterationId)
        db.transaction { tx ->
            val existing = getFeeAlteration(tx.handle, parsedId)
            upsertFeeAlteration(tx.handle, feeAlteration.copy(id = parsedId, updatedBy = user.id))

            val expandedPeriod = existing?.let {
                Period(minOf(it.validFrom, feeAlteration.validFrom), maxEndDate(it.validTo, feeAlteration.validTo))
            } ?: Period(feeAlteration.validFrom, feeAlteration.validTo)

            asyncJobRunner.plan(
                tx,
                listOf(NotifyFeeAlterationUpdated(feeAlteration.personId, expandedPeriod.start, expandedPeriod.end))
            )
        }

        asyncJobRunner.scheduleImmediateRun()
        return ResponseEntity.noContent().build()
    }

    @DeleteMapping("/{feeAlterationId}")
    fun deleteFeeAlteration(db: Database.Connection, user: AuthenticatedUser, @PathVariable feeAlterationId: String): ResponseEntity<Unit> {
        Audit.ChildFeeAlterationsDelete.log(targetId = feeAlterationId)
        user.requireOneOfRoles(Roles.FINANCE_ADMIN)
        val parsedId = parseUUID(feeAlterationId)
        db.transaction { tx ->
            val existing = getFeeAlteration(tx.handle, parsedId)
            deleteFeeAlteration(tx.handle, parsedId)

            existing?.let {
                asyncJobRunner.plan(
                    tx,
                    listOf(NotifyFeeAlterationUpdated(existing.personId, existing.validFrom, existing.validTo))
                )
            }
        }

        asyncJobRunner.scheduleImmediateRun()
        return ResponseEntity.noContent().build()
    }
}
