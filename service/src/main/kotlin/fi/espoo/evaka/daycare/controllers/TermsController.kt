// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.daycare.controllers

import fi.espoo.evaka.Audit
import fi.espoo.evaka.daycare.ClubTerm
import fi.espoo.evaka.daycare.PreschoolTerm
import fi.espoo.evaka.daycare.getClubTerms
import fi.espoo.evaka.daycare.getPreschoolTerms
import fi.espoo.evaka.daycare.insertPreschoolTerm
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.data.DateSet
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.BadRequest
import fi.espoo.evaka.shared.domain.EvakaClock
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.shared.security.AccessControl
import fi.espoo.evaka.shared.security.Action
import mu.KotlinLogging
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

private val logger = KotlinLogging.logger {}

@RestController
class TermsController(private val accessControl: AccessControl) {

    @GetMapping("/public/club-terms")
    fun getClubTerms(db: Database): List<ClubTerm> {
        return db.connect { dbc -> dbc.read { it.getClubTerms() } }
    }

    @GetMapping("/public/preschool-terms")
    fun getPreschoolTerms(db: Database): List<PreschoolTerm> {
        return db.connect { dbc -> dbc.read { it.getPreschoolTerms() } }
    }

    @PostMapping("/preschool-terms")
    fun createPreschoolTerm(
        db: Database,
        user: AuthenticatedUser,
        clock: EvakaClock,
        @RequestBody body: PreschoolTermRequest
    ) {
        db.connect { dbc ->
                dbc.transaction { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.Global.CREATE_PRESCHOOL_TERMS
                    )

                    validatePreschoolTermRequest(tx, body)

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

    private fun validatePreschoolTermRequest(
        tx: Database.Transaction,
        termReq: PreschoolTermRequest
    ) {
        val allTerms = tx.getPreschoolTerms()

        allTerms.forEach { term ->
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
        }

        if (
            !termReq.extendedTerm.contains(termReq.finnishPreschool) ||
                !termReq.extendedTerm.contains(termReq.swedishPreschool)
        ) {
            throw BadRequest(
                "Extended term ${termReq.extendedTerm} has to include the finnish and swedish term periods"
            )
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

    data class PreschoolTermRequest(
        val finnishPreschool: FiniteDateRange,
        val swedishPreschool: FiniteDateRange,
        val extendedTerm: FiniteDateRange,
        val applicationPeriod: FiniteDateRange,
        val termBreaks: DateSet
    )
}
