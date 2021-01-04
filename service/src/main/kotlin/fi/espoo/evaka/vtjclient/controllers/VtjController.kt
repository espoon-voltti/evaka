// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.vtjclient.controllers

import fi.espoo.evaka.Audit
import fi.espoo.evaka.identity.ExternalIdentifier
import fi.espoo.evaka.pis.getPersonById
import fi.espoo.evaka.pis.service.PersonDTO
import fi.espoo.evaka.pis.service.PersonService
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.db.getEnum
import fi.espoo.evaka.shared.db.getUUID
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.vtjclient.dto.NativeLanguage
import fi.espoo.evaka.vtjclient.dto.PersonDataSource
import fi.espoo.evaka.vtjclient.dto.Placement
import fi.espoo.evaka.vtjclient.dto.VtjPersonDTO
import fi.espoo.evaka.vtjclient.service.persondetails.PersonStorageService
import fi.espoo.evaka.vtjclient.usecases.dto.PersonResult
import mu.KotlinLogging
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.AccessDeniedException
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

private val logger = KotlinLogging.logger { }

class UseCaseDeniedException : AccessDeniedException("Use case not allowed for current user")

@Deprecated("Use PersonController instead")
@RestController
@RequestMapping("/persondetails")
class VtjController(
    private val personStorageService: PersonStorageService,
    private val personService: PersonService
) {
    @GetMapping("/uuid/{personId}")
    fun getDetails(
        db: Database.Connection,
        user: AuthenticatedUser,
        @PathVariable(value = "personId") personId: UUID
    ): ResponseEntity<VtjPersonDTO> {
        val vtjData = getPersonDataWithChildren(db, user, personId)
            .let { db.transaction { tx -> personStorageService.upsertVtjGuardianAndChildren(tx, it) } }

        return when (vtjData) {
            is PersonResult.Error -> ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
            is PersonResult.NotFound -> ResponseEntity.notFound().build()
            is PersonResult.Result -> ResponseEntity.ok(db.read { withChildPlacements(it, vtjData.vtjPersonDTO) })
        }
    }

    private fun getPersonDataWithChildren(
        db: Database.Connection,
        user: AuthenticatedUser,
        personId: UUID
    ): PersonResult {
        Audit.VtjRequest.log(targetId = personId)
        return when {
            user.isEndUser() -> {
                if (personId != user.id) {
                    PersonResult.Error("Query not allowed")
                        .also { logger.error { "Error preparing request for person data: ${it.msg}" } }
                } else {
                    personResult(db, user, personId)
                }
            }
            else -> failAuthentication()
        }
    }

    private fun personResult(db: Database.Connection, user: AuthenticatedUser, personId: UUID): PersonResult {
        val guardianResult = db.read { it.handle.getPersonById(personId) }
            ?.let { person ->
                when (person.identity) {
                    is ExternalIdentifier.NoID -> mapPisPerson(person)
                    is ExternalIdentifier.SSN -> personService.getPersonWithDependants(user, person.identity)
                }
            }

        return guardianResult
            ?.let(PersonResult::Result)
            ?: PersonResult.NotFound()
    }

    private fun withChildPlacements(tx: Database.Read, guardian: VtjPersonDTO): VtjPersonDTO {
        val sql =
            "SELECT child_id, start_date, end_date, type FROM placement WHERE child_id = ANY(:children) AND end_date > current_date"

        val placements = tx.createQuery(sql)
            .bind("children", guardian.children.map { it.id }.toTypedArray())
            .map { rs, _ ->
                rs.getUUID("child_id") to Placement(
                    period = FiniteDateRange(
                        rs.getDate("start_date").toLocalDate(),
                        rs.getDate("end_date").toLocalDate()
                    ),
                    type = rs.getEnum("type")
                )
            }

        return guardian.copy(
            children = guardian.children.map { child ->
                val childPlacements = placements
                    .filter { (id, _) -> id == child.id }
                    .map { (_, placement) -> placement }
                    .sortedBy { it.period.start }

                child.copy(existingPlacements = childPlacements)
            }.toMutableList()
        )
    }

    private fun mapPisPerson(person: PersonDTO): VtjPersonDTO {
        return VtjPersonDTO(
            id = person.id,
            firstName = person.firstName ?: "",
            lastName = person.lastName ?: "",
            socialSecurityNumber = (person.identity as? ExternalIdentifier.SSN)?.ssn ?: "",
            children = mutableListOf(),
            guardians = mutableListOf(),
            restrictedDetailsEndDate = null,
            nationalities = emptyList(),
            nativeLanguage = person.language?.let { NativeLanguage(languageName = it, code = it) },
            dateOfBirth = person.dateOfBirth,
            source = PersonDataSource.DATABASE
        )
    }

    private fun <T : Any> failAuthentication(): T {
        throw UseCaseDeniedException()
    }
}
