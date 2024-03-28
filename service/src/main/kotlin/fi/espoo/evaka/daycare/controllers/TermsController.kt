// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.daycare.controllers

import fi.espoo.evaka.Audit
import fi.espoo.evaka.daycare.ClubTerm
import fi.espoo.evaka.daycare.PreschoolTerm
import fi.espoo.evaka.daycare.deleteFutureClubTerm
import fi.espoo.evaka.daycare.deleteFuturePreschoolTerm
import fi.espoo.evaka.daycare.getClubTerm
import fi.espoo.evaka.daycare.getClubTerms
import fi.espoo.evaka.daycare.getPreschoolTerm
import fi.espoo.evaka.daycare.getPreschoolTerms
import fi.espoo.evaka.daycare.insertClubTerm
import fi.espoo.evaka.daycare.insertPreschoolTerm
import fi.espoo.evaka.daycare.updateClubTerm
import fi.espoo.evaka.daycare.updatePreschoolTerm
import fi.espoo.evaka.shared.ClubTermId
import fi.espoo.evaka.shared.PreschoolTermId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.data.DateSet
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.BadRequest
import fi.espoo.evaka.shared.domain.EvakaClock
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.shared.domain.NotFound
import fi.espoo.evaka.shared.security.AccessControl
import fi.espoo.evaka.shared.security.Action
import mu.KotlinLogging
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

private val logger = KotlinLogging.logger {}

@RestController
class TermsController(private val accessControl: AccessControl) {

    @GetMapping(
        path =
            [
                "/public/club-terms", // deprecated
                "/citizen/public/club-terms",
                "/employee/public/club-terms",
            ]
    )
    fun getClubTerms(db: Database): List<ClubTerm> {
        return db.connect { dbc -> dbc.read { it.getClubTerms() } }
    }

    @GetMapping(
        path =
            [
                "/public/preschool-terms", // deprecated
                "/citizen/public/preschool-terms",
                "/employee/public/preschool-terms",
            ]
    )
    fun getPreschoolTerms(db: Database): List<PreschoolTerm> {
        return db.connect { dbc -> dbc.read { it.getPreschoolTerms() } }
    }

    @PostMapping("/club-terms")
    fun createClubTerm(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @RequestBody body: ClubTermRequest
    ) {
        db.connect { dbc ->
                dbc.transaction { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.Global.CREATE_CLUB_TERM
                    )

                    validateClubTermRequest(tx, body, null)

                    tx.insertClubTerm(body.term, body.applicationPeriod, body.termBreaks)
                }
            }
            .also { termId -> Audit.ClubTermCreate.log(objectId = termId) }
    }

    @PutMapping("/club-terms/{id}")
    fun updateClubTerm(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @PathVariable id: ClubTermId,
        @RequestBody body: ClubTermRequest
    ) {
        db.connect { dbc ->
                dbc.transaction { tx ->
                    accessControl.requirePermissionFor(tx, user, clock, Action.ClubTerm.UPDATE, id)

                    tx.getClubTerm(id) ?: throw NotFound("Club term $id does not exist")

                    validateClubTermRequest(tx, body, id)

                    tx.updateClubTerm(id, body.term, body.applicationPeriod, body.termBreaks)
                }
            }
            .also { termId -> Audit.ClubTermUpdate.log(objectId = termId) }
    }

    @DeleteMapping("/club-terms/{id}")
    fun deleteClubTerm(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @PathVariable id: ClubTermId
    ) {
        db.connect { dbc ->
                dbc.transaction { tx ->
                    accessControl.requirePermissionFor(tx, user, clock, Action.ClubTerm.DELETE, id)

                    val existingTerm =
                        tx.getClubTerm(id) ?: throw NotFound("Club term $id does not exist")

                    if (existingTerm.term.start.isBefore(clock.today())) {
                        throw BadRequest("Cannot delete term if it has started")
                    }

                    tx.deleteFutureClubTerm(clock, id)
                }
            }
            .also { termId -> Audit.ClubTermDelete.log(objectId = termId) }
    }

    @PostMapping("/preschool-terms")
    fun createPreschoolTerm(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @RequestBody body: PreschoolTermRequest
    ) {
        db.connect { dbc ->
                dbc.transaction { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.Global.CREATE_PRESCHOOL_TERM
                    )

                    validatePreschoolTermRequest(tx, body, null)

                    tx.insertPreschoolTerm(
                        body.finnishPreschool,
                        body.swedishPreschool,
                        body.extendedTerm,
                        body.applicationPeriod,
                        body.termBreaks
                    )
                }
            }
            .also { termId -> Audit.PreschoolTermCreate.log(objectId = termId) }
    }

    @PutMapping("/preschool-terms/{id}")
    fun updatePreschoolTerm(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @PathVariable id: PreschoolTermId,
        @RequestBody body: PreschoolTermRequest
    ) {
        db.connect { dbc ->
                dbc.transaction { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.PreschoolTerm.UPDATE,
                        id
                    )

                    tx.getPreschoolTerm(id) ?: throw NotFound("Preschool term $id does not exist")

                    validatePreschoolTermRequest(tx, body, id)

                    tx.updatePreschoolTerm(
                        id,
                        body.finnishPreschool,
                        body.swedishPreschool,
                        body.extendedTerm,
                        body.applicationPeriod,
                        body.termBreaks
                    )
                }
            }
            .also { termId -> Audit.PreschoolTermUpdate.log(objectId = termId) }
    }

    @DeleteMapping("/preschool-terms/{id}")
    fun deletePreschoolTerm(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @PathVariable id: PreschoolTermId
    ) {
        db.connect { dbc ->
                dbc.transaction { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.PreschoolTerm.DELETE,
                        id
                    )

                    val existingTerm =
                        tx.getPreschoolTerm(id)
                            ?: throw NotFound("Preschool term $id does not exist")

                    if (
                        existingTerm.finnishPreschool.start.isBefore(clock.today()) ||
                            existingTerm.swedishPreschool.start.isBefore(clock.today())
                    ) {
                        throw BadRequest("Cannot delete term if it has started")
                    }

                    tx.deleteFuturePreschoolTerm(clock, id)
                }
            }
            .also { termId -> Audit.PreschoolTermDelete.log(objectId = termId) }
    }

    private fun validateClubTermRequest(
        tx: Database.Transaction,
        termReq: ClubTermRequest,
        termIdToUpdate: ClubTermId?
    ) {
        val allTerms =
            when {
                termIdToUpdate != null ->
                    tx.getClubTerms().filter { term -> term.id != termIdToUpdate }
                else -> tx.getClubTerms()
            }

        allTerms.forEach {
            if (termReq.term.overlaps(it.term)) {
                throw BadRequest("Club term period ${termReq.term} overlaps with existing one")
            }
        }
    }

    private fun validatePreschoolTermRequest(
        tx: Database.Transaction,
        termReq: PreschoolTermRequest,
        termIdToUpdate: PreschoolTermId?
    ) {
        val allTermsToCompare =
            when {
                termIdToUpdate != null ->
                    tx.getPreschoolTerms().filter { term -> term.id != termIdToUpdate }
                else -> tx.getPreschoolTerms()
            }
        allTermsToCompare.forEach { term ->
            if (termReq.finnishPreschool.overlaps(term.finnishPreschool)) {
                throw BadRequest(
                    "Finnish term period ${termReq.finnishPreschool} overlaps with existing one"
                )
            }
            if (termReq.swedishPreschool.overlaps(term.swedishPreschool)) {
                throw BadRequest(
                    "Swedish term period ${termReq.swedishPreschool} overlaps with existing one"
                )
            }

            if (termReq.extendedTerm.overlaps(term.extendedTerm)) {
                throw BadRequest(
                    "Extended term period ${termReq.extendedTerm} overlaps with existing one"
                )
            }
        }

        if (
            !termReq.extendedTerm.contains(termReq.finnishPreschool) ||
                !termReq.extendedTerm.contains(termReq.swedishPreschool)
        ) {
            throw BadRequest(
                "Extended term ${termReq.extendedTerm} has to include the finnish and swedish term periods"
            )
        }
    }

    data class ClubTermRequest(
        val term: FiniteDateRange,
        val applicationPeriod: FiniteDateRange,
        val termBreaks: DateSet
    )

    data class PreschoolTermRequest(
        val finnishPreschool: FiniteDateRange,
        val swedishPreschool: FiniteDateRange,
        val extendedTerm: FiniteDateRange,
        val applicationPeriod: FiniteDateRange,
        val termBreaks: DateSet
    )
}
