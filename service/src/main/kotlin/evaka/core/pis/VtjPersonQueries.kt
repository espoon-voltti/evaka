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
import evaka.core.shared.db.Predicate
import evaka.core.shared.domain.Conflict
import evaka.core.shared.domain.HelsinkiDateTime
import evaka.core.vtjclient.dto.VtjPerson
import io.github.oshai.kotlinlogging.KotlinLogging
import java.time.LocalDate
import kotlin.reflect.full.memberProperties

private val logger = KotlinLogging.logger {}

/** [newDateOfDeath] is set only when the stored date of death changed from NULL to a date. */
data class ApplyResult(
    val id: PersonId,
    val person: PersonDTO,
    val created: Boolean,
    val newDateOfDeath: LocalDate?,
)

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
        person = new,
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
    val new =
        createQuery {
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
WHERE id = ${bind(personId)} AND social_security_number IS NULL
RETURNING *
"""
                )
            }
            .exactlyOneOrNull(toPersonDTO) ?: throw Conflict("Person $personId already has an SSN")

    logChangedFields(old, new)
    return ApplyResult(
        id = new.id,
        person = new,
        created = false,
        newDateOfDeath = newDateOfDeath(old, new),
    )
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

fun Database.Transaction.clearDependantsSynced(personId: PersonId) = execute {
    sql("UPDATE person SET vtj_dependants_queried = NULL WHERE id = ${bind(personId)}")
}

/**
 * A person who should have synced dependants: a citizen user, or a head of family or a partner in a
 * current fridge family (conflicts are included). With [since], only a person whose citizen user
 * row or current fridge row is new or changed since [since].
 */
private fun dependantSyncPerson(today: LocalDate, since: HelsinkiDateTime? = null): Predicate {
    val citizenUserSince =
        if (since == null) Predicate.alwaysTrue()
        else Predicate { where("$it.created_at >= ${bind(since)}") }
    val fridgeRowSince =
        if (since == null) Predicate.alwaysTrue()
        else
            Predicate {
                where(
                    "($it.updated >= ${bind(since)} OR $it.start_date >= ${bind(since.toLocalDate())})"
                )
            }
    return Predicate {
        where(
            """
EXISTS (
    SELECT FROM citizen_user cu
    WHERE cu.id = $it.id AND ${predicate(citizenUserSince.forTable("cu"))}
)
OR EXISTS (
    SELECT FROM fridge_child fc
    WHERE
        fc.head_of_child = $it.id AND
        ${bind(today)} BETWEEN fc.start_date AND fc.end_date AND
        ${predicate(fridgeRowSince.forTable("fc"))}
)
OR EXISTS (
    SELECT FROM fridge_partner fp
    WHERE
        fp.person_id = $it.id AND
        ${bind(today)} BETWEEN fp.start_date AND coalesce(fp.end_date, 'infinity') AND
        ${predicate(fridgeRowSince.forTable("fp"))}
)
"""
        )
    }
}

/**
 * A minor with an SSN who has a placement that has not ended, or an application that is not in a
 * final state.
 */
private fun guardianSyncChild(today: LocalDate) = Predicate {
    where(
        """
$it.social_security_number IS NOT NULL
AND $it.date_of_birth + interval '18 years' > ${bind(today)}
AND (
    EXISTS (SELECT FROM placement pl WHERE pl.child_id = $it.id AND pl.end_date >= ${bind(today)})
    OR EXISTS (
        SELECT FROM application a
        WHERE a.child_id = $it.id
          AND a.status NOT IN ('ACTIVE', 'REJECTED', 'CANCELLED', 'TERMINATED', 'DEPRECATED')
    )
)
"""
    )
}

fun Database.Read.isDependantSyncPerson(personId: PersonId, today: LocalDate): Boolean =
    createQuery {
        sql(
            """
SELECT EXISTS (
    SELECT FROM person p
    WHERE p.id = ${bind(personId)} AND ${predicate(dependantSyncPerson(today).forTable("p"))}
)
"""
        )
    }
    .exactlyOne()

/** Clears the marker of a person who is no longer a dependant-sync person. */
fun Database.Transaction.clearStaleDependantsSynced(
    olderThan: HelsinkiDateTime,
    today: LocalDate,
): Int = createUpdate {
    sql(
        """
UPDATE person p SET vtj_dependants_queried = NULL
WHERE p.vtj_dependants_queried < ${bind(olderThan)}
  AND NOT (${predicate(dependantSyncPerson(today).forTable("p"))})
"""
    )
}
    .executeAndReturnCount()

/**
 * The nightly sync job syncs a person who became a dependant-sync person within this many days. An
 * older one is left to the normal triggers.
 */
private const val NEWLY_DEPENDANT_SYNC_DAYS = 7L

private fun dependantsNotSynced(today: LocalDate, since: HelsinkiDateTime?) = Predicate {
    where("$it.social_security_number IS NOT NULL AND $it.vtj_dependants_queried IS NULL")
}
    .and(dependantSyncPerson(today, since))

private fun guardiansNotSynced(today: LocalDate) = Predicate {
    where("$it.vtj_guardians_queried IS NULL")
}
    .and(guardianSyncChild(today))

/**
 * Newly dependant-sync persons whose dependants are not synced, then guardian-sync children whose
 * guardians are not synced.
 */
fun Database.Read.getPersonsToSync(now: HelsinkiDateTime, limit: Int): List<PersonId> {
    val today = now.toLocalDate()
    val since = now.minusDays(NEWLY_DEPENDANT_SYNC_DAYS)
    return createQuery {
        sql(
            """
SELECT id
FROM (
    SELECT p.id, 1 AS priority FROM person p
    WHERE ${predicate(dependantsNotSynced(today, since).forTable("p"))}
    UNION ALL
    SELECT p.id, 2 AS priority FROM person p
    WHERE ${predicate(guardiansNotSynced(today).forTable("p"))}
) persons_to_sync
GROUP BY id
ORDER BY min(priority), id
LIMIT ${bind(limit)}
"""
        )
    }
        .toList()
}

/**
 * [dependantSyncBacklog] counts the dependant-sync persons whose dependants are not synced, with no
 * time limit.
 */
data class PersonsToSyncCount(
    val newlyDependantSync: Int,
    val guardianSyncChildren: Int,
    val dependantSyncBacklog: Int,
)

fun Database.Read.countPersonsToSync(now: HelsinkiDateTime): PersonsToSyncCount {
    val today = now.toLocalDate()
    val since = now.minusDays(NEWLY_DEPENDANT_SYNC_DAYS)
    return createQuery {
        sql(
            """
SELECT
    (
        SELECT count(*) FROM person p
        WHERE ${predicate(dependantsNotSynced(today, since).forTable("p"))}
    ) AS newly_dependant_sync,
    (
        SELECT count(*) FROM person p
        WHERE ${predicate(guardiansNotSynced(today).forTable("p"))}
    ) AS guardian_sync_children,
    (
        SELECT count(*) FROM person p
        WHERE ${predicate(dependantsNotSynced(today, since = null).forTable("p"))}
    ) AS dependant_sync_backlog
"""
        )
    }
        .exactlyOne()
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
