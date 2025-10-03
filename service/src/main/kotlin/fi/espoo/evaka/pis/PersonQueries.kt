// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.pis

import fi.espoo.evaka.identity.ExternalIdentifier
import fi.espoo.evaka.pis.controllers.CreatePersonBody
import fi.espoo.evaka.pis.service.PersonDTO
import fi.espoo.evaka.pis.service.PersonPatch
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.db.PredicateSql
import fi.espoo.evaka.shared.db.Row
import fi.espoo.evaka.shared.db.freeTextSearchPredicate
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import java.time.LocalDate
import java.util.UUID

val personDTOColumns =
    listOf(
        "id",
        "duplicate_of",
        "social_security_number",
        "ssn_adding_disabled",
        "first_name",
        "last_name",
        "preferred_name",
        "email",
        "phone",
        "backup_phone",
        "language",
        "date_of_birth",
        "date_of_death",
        "nationalities",
        "restricted_details_enabled",
        "restricted_details_end_date",
        "street_address",
        "postal_code",
        "post_office",
        "residence_code",
        "municipality_of_residence",
        "updated_from_vtj",
        "vtj_guardians_queried",
        "vtj_dependants_queried",
        "invoice_recipient_name",
        "invoicing_street_address",
        "invoicing_postal_code",
        "invoicing_post_office",
        "force_manual_fee_decisions",
        "oph_person_oid",
        "updated_from_vtj",
    )
val commaSeparatedPersonDTOColumns = personDTOColumns.joinToString()

data class CitizenUserIdentity(val id: PersonId)

data class CitizenUserDetails(
    val id: PersonId,
    val firstName: String,
    val lastName: String,
    val preferredName: String,
    val streetAddress: String,
    val postalCode: String,
    val postOffice: String,
    val phone: String,
    val backupPhone: String,
    val email: String?,
    val weakLoginUsername: String?,
)

fun Database.Read.getCitizenUserDetails(id: PersonId): CitizenUserDetails? =
    createQuery {
            sql(
                """
SELECT id, first_name, last_name, preferred_name, street_address, postal_code, post_office, phone, backup_phone, email, citizen_user.username AS weak_login_username
FROM person
LEFT JOIN citizen_user USING (id)
WHERE id = ${bind(id)}
"""
            )
        }
        .exactlyOneOrNull()

fun Database.Read.getCitizenUserBySsn(ssn: String): CitizenUserIdentity? =
    createQuery { sql("SELECT id FROM person WHERE social_security_number = ${bind(ssn)}") }
        .exactlyOneOrNull<CitizenUserIdentity>()

fun Database.Read.getPersonById(id: PersonId): PersonDTO? {
    return createQuery {
            sql(
                """
SELECT
$commaSeparatedPersonDTOColumns
FROM person
WHERE id = ${bind(id)}
        """
            )
        }
        .bind("id", id)
        .exactlyOneOrNull(toPersonDTO)
}

data class PersonNameDetails(val id: PersonId, val firstName: String, val lastName: String)

fun Database.Read.getPersonNameDetailsById(personIds: Set<PersonId>): List<PersonNameDetails> {
    return createQuery {
            sql(
                """
SELECT
id, first_name, last_name
FROM person
WHERE id = ANY(${bind(personIds)})
        """
            )
        }
        .toList<PersonNameDetails>()
}

fun Database.Read.isDuplicate(id: PersonId): Boolean =
    createQuery { sql("SELECT duplicate_of IS NOT NULL FROM person WHERE id = ${bind(id)}") }
        .exactlyOneOrNull<Boolean>() ?: false

fun Database.Transaction.lockPersonBySSN(ssn: String): PersonDTO? =
    createQuery {
            sql(
                """
SELECT
$commaSeparatedPersonDTOColumns
FROM person
WHERE social_security_number = ${bind(ssn)}
FOR UPDATE
    """
            )
        }
        .exactlyOneOrNull(toPersonDTO)

fun Database.Read.getPersonBySSN(ssn: String): PersonDTO? {
    return createQuery {
            sql(
                """
SELECT
$commaSeparatedPersonDTOColumns
FROM person
WHERE social_security_number = ${bind(ssn)}
        """
            )
        }
        .exactlyOneOrNull(toPersonDTO)
}

fun Database.Read.listPersonByDuplicateOf(id: PersonId): List<PersonDTO> =
    createQuery {
            sql(
                "SELECT $commaSeparatedPersonDTOColumns FROM person WHERE duplicate_of = ${bind(id)}"
            )
        }
        .toList(toPersonDTO)

fun Database.Read.getPersonDuplicateOf(id: PersonId): PersonId? =
    createQuery { sql("SELECT duplicate_of FROM person WHERE id = ${bind(id)}") }
        .mapTo<PersonId>()
        .exactlyOneOrNull()

private val personSortColumns =
    listOf("first_name", "last_name", "date_of_birth", "street_address", "social_security_number")

data class PersonSummary(
    val id: PersonId,
    val firstName: String,
    val lastName: String,
    val dateOfBirth: LocalDate,
    val dateOfDeath: LocalDate?,
    val socialSecurityNumber: String?,
    val streetAddress: String,
    val restrictedDetailsEnabled: Boolean,
)

fun Database.Read.searchPeople(
    user: AuthenticatedUser.Employee,
    searchTerms: String,
    sortColumns: String,
    sortDirection: String,
    restricted: Boolean,
): List<PersonSummary> {
    if (searchTerms.isBlank()) return listOf()

    val direction = if (sortDirection.equals("DESC", ignoreCase = true)) "DESC" else "ASC"
    val orderBy =
        sortColumns
            .split(",")
            .map { it.trim() }
            .let { columns ->
                if (personSortColumns.containsAll(columns)) {
                    columns.joinToString(", ") { column -> "$column $direction" }
                } else {
                    "last_name $direction"
                }
            }

    val freeTextPredicate = freeTextSearchPredicate(listOf("person"), searchTerms)
    val restrictedPredicate =
        if (restricted)
            PredicateSql {
                where(
                    "id IN (SELECT person_id FROM person_acl_view acl WHERE acl.employee_id = ${bind(user.id)})"
                )
            }
        else {
            PredicateSql.alwaysTrue()
        }

    return createQuery {
            sql(
                """
        SELECT
            id,
            social_security_number,
            first_name,
            last_name,
            date_of_birth,
            date_of_death,
            street_address,
            restricted_details_enabled
        FROM person
        WHERE
            ${predicate(freeTextPredicate)} AND
            ${predicate(restrictedPredicate)}
        ORDER BY $orderBy
        LIMIT 100
    """
            )
        }
        .toList()
}

fun Database.Transaction.createPerson(person: CreatePersonBody): PersonId {
    return createQuery {
            sql(
                """
INSERT INTO person (first_name, last_name, date_of_birth, street_address, postal_code, post_office, phone, email)
VALUES (${bind(person.firstName)}, ${bind(person.lastName)}, ${bind(person.dateOfBirth)}, ${bind(person.streetAddress)}, ${bind(person.postalCode)}, ${bind(person.postOffice)}, ${bind(person.phone)}, ${bind(person.email)})
RETURNING id
"""
            )
        }
        .exactlyOne<PersonId>()
}

fun Database.Transaction.createPersonFromVtj(person: PersonDTO): PersonDTO {
    val p = person.copy(updatedFromVtj = HelsinkiDateTime.now())
    return createQuery {
            sql(
                """
INSERT INTO person (
    first_name,
    last_name,
    date_of_birth,
    date_of_death,
    social_security_number,
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
    ${bind(p.firstName)},
    ${bind(p.lastName)},
    ${bind(p.dateOfBirth)},
    ${bind(p.dateOfDeath)},
    ${bind(p.identity)},
    ${bind(p.language)},
    ${bind(p.nationalities)},
    ${bind(p.streetAddress)},
    ${bind(p.postalCode)},
    ${bind(p.postOffice)},
    ${bind(p.residenceCode)},
    ${bind(p.municipalityOfResidence)},
    ${bind(p.restrictedDetailsEnabled)},
    ${bind(p.restrictedDetailsEndDate)},
    ${bind(p.updatedFromVtj)}
)
RETURNING *
"""
            )
        }
        .exactlyOne(toPersonDTO)
}

fun Database.Transaction.duplicatePerson(id: PersonId): PersonId? =
    createUpdate {
            sql(
                """
INSERT INTO person (
    first_name,
    last_name,
    email,
    aad_object_id,
    language,
    date_of_birth,
    street_address,
    postal_code,
    post_office,
    nationalities,
    restricted_details_enabled,
    restricted_details_end_date,
    phone,
    invoicing_street_address,
    invoicing_postal_code,
    invoicing_post_office,
    invoice_recipient_name,
    date_of_death,
    residence_code,
    force_manual_fee_decisions,
    backup_phone,
    oph_person_oid,
    ssn_adding_disabled,
    preferred_name,
    duplicate_of
)
SELECT
    first_name,
    last_name,
    email,
    aad_object_id,
    language,
    date_of_birth,
    street_address,
    postal_code,
    post_office,
    nationalities,
    restricted_details_enabled,
    restricted_details_end_date,
    phone,
    invoicing_street_address,
    invoicing_postal_code,
    invoicing_post_office,
    invoice_recipient_name,
    date_of_death,
    residence_code,
    force_manual_fee_decisions,
    backup_phone,
    oph_person_oid,
    TRUE AS ssn_adding_disabled,
    preferred_name,
    id AS duplicate_of
FROM person WHERE id = ${bind(id)}
RETURNING id
"""
            )
        }
        .executeAndReturnGeneratedKeys()
        .exactlyOneOrNull<PersonId>()

fun Database.Transaction.updatePersonFromVtj(person: PersonDTO): PersonDTO {
    val p = person.copy(updatedFromVtj = HelsinkiDateTime.now())
    return createQuery {
            sql(
                """
UPDATE person SET
    first_name = ${bind(p.firstName)},
    last_name = ${bind(p.lastName)},
    social_security_number = ${bind(p.identity)},
    date_of_birth = ${bind(p.dateOfBirth)},
    date_of_death = ${bind(p.dateOfDeath)},
    language = ${bind(p.language)},
    nationalities = ${bind(p.nationalities)},
    street_address = ${bind(p.streetAddress)},
    postal_code = ${bind(p.postalCode)},
    post_office = ${bind(p.postOffice)},
    residence_code = ${bind(p.residenceCode)},
    municipality_of_residence = ${bind(p.municipalityOfResidence)},
    restricted_details_enabled = ${bind(p.restrictedDetailsEnabled)},
    restricted_details_end_date = ${bind(p.restrictedDetailsEndDate)},
    updated_from_vtj = ${bind(p.updatedFromVtj)}
WHERE id = ${bind(p.id)}
RETURNING *
"""
            )
        }
        .exactlyOne(toPersonDTO)
}

// email is only updated if the user has no verified email
fun Database.Transaction.updatePersonBasicContactInfo(
    id: PersonId,
    email: String?,
    phone: String,
): Boolean {
    return createQuery {
            sql(
                """
UPDATE person SET
    email = CASE WHEN verified_email IS NULL THEN ${bind(email)} ELSE email END,
    phone = ${bind(phone)}
WHERE id = ${bind(id)}
RETURNING id
"""
            )
        }
        .exactlyOneOrNull<PersonId>() != null
}

// Update those person fields which do not come from VTJ
fun Database.Transaction.updatePersonNonVtjDetails(id: PersonId, patch: PersonPatch): Boolean {
    return createQuery {
            sql(
                """
UPDATE person SET
    email = coalesce(${bind(patch.email)}, email),
    phone = coalesce(${bind(patch.phone)}, phone),
    backup_phone = coalesce(${bind(patch.backupPhone)}, backup_phone),
    invoice_recipient_name = coalesce(${bind(patch.invoiceRecipientName)}, invoice_recipient_name),
    invoicing_street_address = coalesce(${bind(patch.invoicingStreetAddress)}, invoicing_street_address),
    invoicing_postal_code = coalesce(${bind(patch.invoicingPostalCode)}, invoicing_postal_code),
    invoicing_post_office = coalesce(${bind(patch.invoicingPostOffice)}, invoicing_post_office),
    force_manual_fee_decisions = coalesce(${bind(patch.forceManualFeeDecisions)}, force_manual_fee_decisions),
    oph_person_oid = coalesce(${bind(patch.ophPersonOid)}, oph_person_oid)
WHERE id = ${bind(id)}
RETURNING id
"""
            )
        }
        .exactlyOneOrNull<PersonId>() != null
}

fun Database.Transaction.updateNonSsnPersonDetails(id: PersonId, patch: PersonPatch): Boolean {
    return createQuery {
            sql(
                """
UPDATE person SET
    first_name = coalesce(${bind(patch.firstName)}, first_name),
    last_name = coalesce(${bind(patch.lastName)}, last_name),
    date_of_birth = coalesce(${bind(patch.dateOfBirth)}, date_of_birth),
    email = coalesce(${bind(patch.email)}, email),
    phone = coalesce(${bind(patch.phone)}, phone),
    backup_phone = coalesce(${bind(patch.backupPhone)}, backup_phone),
    street_address = coalesce(${bind(patch.streetAddress)}, street_address),
    postal_code = coalesce(${bind(patch.postalCode)}, postal_code),
    post_office = coalesce(${bind(patch.postOffice)}, post_office),
    municipality_of_residence = coalesce(${bind(patch.municipalityOfResidence)}, municipality_of_residence),
    invoice_recipient_name = coalesce(${bind(patch.invoiceRecipientName)}, invoice_recipient_name),
    invoicing_street_address = coalesce(${bind(patch.invoicingStreetAddress)}, invoicing_street_address),
    invoicing_postal_code = coalesce(${bind(patch.invoicingPostalCode)}, invoicing_postal_code),
    invoicing_post_office = coalesce(${bind(patch.invoicingPostOffice)}, invoicing_post_office),
    force_manual_fee_decisions = coalesce(${bind(patch.forceManualFeeDecisions)}, force_manual_fee_decisions),
    oph_person_oid = coalesce(${bind(patch.ophPersonOid)}, oph_person_oid)
WHERE id = ${bind(id)} AND social_security_number IS NULL
RETURNING id
"""
            )
        }
        .exactlyOneOrNull<PersonId>() != null
}

fun Database.Transaction.addSSNToPerson(id: PersonId, ssn: String) {
    createUpdate {
            sql("UPDATE person SET social_security_number = ${bind(ssn)} WHERE id = ${bind(id)}")
        }
        .execute()
}

private val toPersonDTO: Row.() -> PersonDTO = {
    PersonDTO(
        id = PersonId(column("id")),
        duplicateOf = column<UUID?>("duplicate_of")?.let { PersonId(it) },
        identity =
            column<String?>("social_security_number")?.let { ssn ->
                ExternalIdentifier.SSN.getInstance(ssn)
            } ?: ExternalIdentifier.NoID,
        ssnAddingDisabled = column("ssn_adding_disabled"),
        firstName = column("first_name"),
        lastName = column("last_name"),
        preferredName = column("preferred_name"),
        email = column("email"),
        phone = column("phone"),
        backupPhone = column("backup_phone"),
        language = column("language"),
        dateOfBirth = column("date_of_birth"),
        dateOfDeath = column("date_of_death"),
        nationalities = column("nationalities"),
        restrictedDetailsEnabled = column("restricted_details_enabled"),
        restrictedDetailsEndDate = column("restricted_details_end_date"),
        streetAddress = column("street_address"),
        postalCode = column("postal_code"),
        postOffice = column("post_office"),
        residenceCode = column("residence_code"),
        municipalityOfResidence = column("municipality_of_residence"),
        updatedFromVtj = column("updated_from_vtj"),
        vtjGuardiansQueried = column("vtj_guardians_queried"),
        vtjDependantsQueried = column("vtj_dependants_queried"),
        invoiceRecipientName = column("invoice_recipient_name"),
        invoicingStreetAddress = column("invoicing_street_address"),
        invoicingPostalCode = column("invoicing_postal_code"),
        invoicingPostOffice = column("invoicing_post_office"),
        forceManualFeeDecisions = column("force_manual_fee_decisions"),
        ophPersonOid = column("oph_person_oid"),
    )
}

fun Database.Transaction.updateCitizenOnLogin(now: HelsinkiDateTime, id: PersonId) =
    createUpdate {
            sql(
                """
UPDATE person 
SET last_login = ${bind(now)}
WHERE id = ${bind(id)}
"""
            )
        }
        .updateExactlyOne()

data class PersonReference(val table: String, val column: String)

fun Database.Read.getTransferablePersonReferences(): List<PersonReference> {
    return createQuery {
            sql(
                """
select source.relname as "table", attr.attname as "column"
from pg_constraint const
    join pg_class source on source.oid = const.conrelid
    join pg_class target on target.oid = const.confrelid
    join pg_attribute attr on attr.attrelid = source.oid and attr.attnum = ANY(const.conkey)
where const.contype = 'f' 
    and target.relname in ('person', 'child') 
    and source.relname not in ('person', 'child', 'child_images', 'guardian', 'guardian_blocklist', 'message_account', 'citizen_user')
    and source.relname not like 'old_%'
order by source.relname, attr.attname
"""
            )
        }
        .toList<PersonReference>()
}

fun Database.Read.getGuardianDependants(personId: PersonId) =
    createQuery {
            sql(
                """
SELECT
$commaSeparatedPersonDTOColumns
FROM person
WHERE id IN (SELECT child_id FROM guardian WHERE guardian_id = ${bind(personId)})
        """
            )
        }
        .toList(toPersonDTO)

fun Database.Read.getDependantGuardians(personId: ChildId) =
    createQuery {
            sql(
                """
SELECT
$commaSeparatedPersonDTOColumns
FROM person
WHERE id IN (SELECT guardian_id FROM guardian WHERE child_id = ${bind(personId)})
        """
            )
        }
        .toList(toPersonDTO)

fun Database.Transaction.updatePersonSsnAddingDisabled(id: PersonId, disabled: Boolean) {
    createUpdate {
            sql("UPDATE person SET ssn_adding_disabled = ${bind(disabled)} WHERE id = ${bind(id)}")
        }
        .execute()
}

fun Database.Transaction.updatePreferredName(id: PersonId, preferredName: String) {
    createUpdate {
            sql("UPDATE person SET preferred_name = ${bind(preferredName)} WHERE id = ${bind(id)}")
        }
        .execute()
}

fun Database.Transaction.updateOphPersonOid(id: PersonId, ophPersonOid: String) {
    createUpdate {
            sql("UPDATE person SET oph_person_oid = ${bind(ophPersonOid)} WHERE id = ${bind(id)}")
        }
        .updateExactlyOne()
}

data class PersonEmails(val email: String?, val verifiedEmail: String?)

fun Database.Read.getPersonEmails(id: PersonId): PersonEmails =
    createQuery { sql("SELECT email, verified_email FROM person WHERE id = ${bind(id)}") }
        .exactlyOne()
