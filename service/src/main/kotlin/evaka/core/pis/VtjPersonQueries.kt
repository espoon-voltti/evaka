// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.pis

import evaka.core.daycare.controllers.AdditionalInformation
import evaka.core.daycare.controllers.Child
import evaka.core.daycare.createChild
import evaka.core.daycare.getChild
import evaka.core.identity.Ssn
import evaka.core.identity.getDobFromSsn
import evaka.core.messaging.createPersonMessageAccount
import evaka.core.pis.service.PersonDTO
import evaka.core.pis.service.isGuardianBlocked
import evaka.core.pis.service.replaceChildGuardians
import evaka.core.pis.service.replaceGuardianChildren
import evaka.core.shared.ChildId
import evaka.core.shared.PersonId
import evaka.core.shared.db.Database
import evaka.core.shared.domain.HelsinkiDateTime
import evaka.core.vtjclient.dto.VtjPerson
import io.github.oshai.kotlinlogging.KotlinLogging
import java.time.LocalDate
import kotlin.reflect.full.memberProperties

private val logger = KotlinLogging.logger {}

/** [newDateOfDeath] is set only when the stored date of death changed from NULL to a date. */
data class ApplyResult(val id: PersonId, val created: Boolean, val newDateOfDeath: LocalDate?)

/** Creates or updates the person row that has the SSN of [person]. */
fun Database.Transaction.applyPerson(person: VtjPerson, now: HelsinkiDateTime): ApplyResult {
    val ssn = Ssn.parse(person.socialSecurityNumber) ?: error("VTJ returned an invalid SSN")
    val old = lockPersonBySSN(ssn.value)
    val (new, created) =
        createQuery {
            sql(
                """
INSERT INTO person (
    social_security_number,
    first_name,
    last_name,
    date_of_birth,
    date_of_death,
    language,
    nationalities,
    street_address,
    postal_code,
    post_office,
    residence_code,
    municipality_of_residence,
    restricted_details_enabled,
    restricted_details_end_date,
    updated_from_vtj
)
VALUES (
    ${bind(ssn)},
    ${bind(person.firstNames)},
    ${bind(person.lastName)},
    ${bind(getDobFromSsn(ssn.value))},
    ${bind(person.dateOfDeath)},
    ${bind(person.nativeLanguage?.code ?: "")},
    ${bind(person.nationalities.map { it.countryCode })},
    ${bind(person.streetAddressByLanguage())},
    ${bind(person.address?.postalCode ?: "")},
    ${bind(person.postOfficeByLanguage())},
    ${bind(person.residenceCode ?: "")},
    ${bind(person.municipalityOfResidenceByLanguage())},
    ${bind(person.restrictedDetails?.enabled ?: false)},
    ${bind(person.restrictedDetails?.endDate)},
    ${bind(now)}
)
ON CONFLICT (social_security_number) DO UPDATE SET
    first_name = excluded.first_name,
    last_name = excluded.last_name,
    date_of_birth = excluded.date_of_birth,
    date_of_death = excluded.date_of_death,
    language = excluded.language,
    nationalities = excluded.nationalities,
    street_address = excluded.street_address,
    postal_code = excluded.postal_code,
    post_office = excluded.post_office,
    residence_code = excluded.residence_code,
    municipality_of_residence = excluded.municipality_of_residence,
    restricted_details_enabled = excluded.restricted_details_enabled,
    restricted_details_end_date = excluded.restricted_details_end_date,
    updated_from_vtj = excluded.updated_from_vtj
RETURNING *, old.id IS NULL AS inserted
"""
            )
        }
            .exactlyOne { toPersonDTO() to column<Boolean>("inserted") }

    if (created) {
        val _ = createPersonMessageAccount(new.id)
    }
    if (old != null) logChangedFields(old, new)
    return ApplyResult(
        id = new.id,
        created = created,
        newDateOfDeath = newDateOfDeath(old, new),
    )
}

/**
 * Sets the SSN of a person who has none, together with the VTJ fields. The SSN and updated_from_vtj
 * must be written in the same statement because of the ssn_require_vtj_update constraint.
 */
fun Database.Transaction.setSsnFromVtj(
    personId: PersonId,
    person: VtjPerson,
    now: HelsinkiDateTime,
): ApplyResult {
    val ssn = Ssn.parse(person.socialSecurityNumber) ?: error("VTJ returned an invalid SSN")
    val old = getPersonById(personId) ?: error("Person $personId not found")
    val new = createQuery {
        sql(
            """
UPDATE person SET
    social_security_number = ${bind(ssn)},
    first_name = ${bind(person.firstNames)},
    last_name = ${bind(person.lastName)},
    date_of_birth = ${bind(getDobFromSsn(ssn.value))},
    date_of_death = ${bind(person.dateOfDeath)},
    language = ${bind(person.nativeLanguage?.code ?: "")},
    nationalities = ${bind(person.nationalities.map { it.countryCode })},
    street_address = ${bind(person.streetAddressByLanguage())},
    postal_code = ${bind(person.address?.postalCode ?: "")},
    post_office = ${bind(person.postOfficeByLanguage())},
    residence_code = ${bind(person.residenceCode ?: "")},
    municipality_of_residence = ${bind(person.municipalityOfResidenceByLanguage())},
    restricted_details_enabled = ${bind(person.restrictedDetails?.enabled ?: false)},
    restricted_details_end_date = ${bind(person.restrictedDetails?.endDate)},
    updated_from_vtj = ${bind(now)}
WHERE id = ${bind(personId)}
RETURNING *
"""
        )
    }
        .exactlyOne(toPersonDTO)

    logChangedFields(old, new)
    return ApplyResult(id = new.id, created = false, newDateOfDeath = newDateOfDeath(old, new))
}

/** Replaces the guardians of a child, skipping blocked guardians, and marks them synced. */
fun Database.Transaction.applyGuardians(
    childId: ChildId,
    guardianIds: Set<PersonId>,
    now: HelsinkiDateTime,
) {
    initChildIfNotExists(childId, now)
    val allowed = guardianIds.filterNot { isGuardianBlocked(it, childId) }.toSet()
    val changes = replaceChildGuardians(childId, allowed)
    execute {
        sql("UPDATE person SET vtj_guardians_queried = ${bind(now)} WHERE id = ${bind(childId)}")
    }
    logger.info {
        "Created or replaced child $childId guardians as $allowed (added ${changes.added}, removed ${changes.removed})"
    }
}

/** Replaces the dependants of a guardian, skipping blocked ones, and marks them synced. */
fun Database.Transaction.applyDependants(
    guardianId: PersonId,
    childIds: Set<ChildId>,
    now: HelsinkiDateTime,
) {
    childIds.forEach { initChildIfNotExists(it, now) }
    val allowed = childIds.filterNot { isGuardianBlocked(guardianId, it) }.toSet()
    val changes = replaceGuardianChildren(guardianId, allowed)
    execute {
        sql(
            "UPDATE person SET vtj_dependants_queried = ${bind(now)} WHERE id = ${bind(guardianId)}"
        )
    }
    logger.info {
        "Created or replaced guardian $guardianId children as $allowed (added ${changes.added}, removed ${changes.removed})"
    }
}

private fun Database.Transaction.initChildIfNotExists(childId: ChildId, now: HelsinkiDateTime) {
    if (getChild(childId) == null) {
        createChild(Child(id = childId, additionalInformation = AdditionalInformation()), now)
    }
}

private fun newDateOfDeath(old: PersonDTO?, new: PersonDTO): LocalDate? =
    new.dateOfDeath.takeIf { old != null && old.dateOfDeath == null }

private fun logChangedFields(old: PersonDTO, new: PersonDTO) {
    val changedFields =
        PersonDTO::class
            .memberProperties
            .filter { it != PersonDTO::updatedFromVtj && it.get(old) != it.get(new) }
            .map { it.name }
    if (changedFields.isNotEmpty()) {
        logger.info { "Updated person ${old.id} from VTJ, changed fields: $changedFields" }
    }
}

private fun VtjPerson.prefersSwedish(): Boolean = nativeLanguage?.code == "sv"

private fun VtjPerson.streetAddressByLanguage(): String =
    address?.streetAddressSe?.takeIf { prefersSwedish() && it.isNotEmpty() }
        ?: address?.streetAddress
        ?: ""

private fun VtjPerson.postOfficeByLanguage(): String =
    address?.postOfficeSe?.takeIf { prefersSwedish() && it.isNotEmpty() }
        ?: address?.postOffice
        ?: ""

private fun VtjPerson.municipalityOfResidenceByLanguage(): String =
    municipalityOfResidenceSe?.takeIf { prefersSwedish() && it.isNotEmpty() }
        ?: municipalityOfResidence
        ?: ""
