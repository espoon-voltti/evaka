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
import fi.espoo.evaka.shared.db.Row
import fi.espoo.evaka.shared.db.freeTextSearchQuery
import fi.espoo.evaka.shared.domain.EvakaClock
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.utils.applyIf
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
        "updated_from_vtj",
        "vtj_guardians_queried",
        "vtj_dependants_queried",
        "invoice_recipient_name",
        "invoicing_street_address",
        "invoicing_postal_code",
        "invoicing_post_office",
        "force_manual_fee_decisions",
        "oph_person_oid",
        "updated_from_vtj"
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
    val keycloakEmail: String?,
)

fun Database.Read.getCitizenUserDetails(id: PersonId): CitizenUserDetails? =
    createQuery {
            sql(
                """
SELECT id, first_name, last_name, preferred_name, street_address, postal_code, post_office, phone, backup_phone, email, keycloak_email
FROM person WHERE id = ${bind(id)}
"""
            )
        }
        .exactlyOneOrNull()

fun Database.Read.getCitizenUserBySsn(ssn: String): CitizenUserIdentity? =
    @Suppress("DEPRECATION")
    createQuery("SELECT id FROM person WHERE social_security_number = :ssn")
        .bind("ssn", ssn)
        .exactlyOneOrNull<CitizenUserIdentity>()

fun Database.Read.getPersonById(id: PersonId): PersonDTO? {
    @Suppress("DEPRECATION")
    return createQuery(
            """
SELECT
$commaSeparatedPersonDTOColumns
FROM person
WHERE id = :id
        """
                .trimIndent()
        )
        .bind("id", id)
        .exactlyOneOrNull(toPersonDTO)
}

data class PersonNameDetails(val id: PersonId, val firstName: String, val lastName: String)

fun Database.Read.getPersonNameDetailsById(personIds: Set<PersonId>): List<PersonNameDetails> {
    @Suppress("DEPRECATION")
    return createQuery(
            """
SELECT
id, first_name, last_name
FROM person
WHERE id = ANY(:personIds)
        """
                .trimIndent()
        )
        .bind("personIds", personIds)
        .toList<PersonNameDetails>()
}

fun Database.Read.isDuplicate(id: PersonId): Boolean =
    @Suppress("DEPRECATION")
    createQuery("SELECT duplicate_of IS NOT NULL FROM person WHERE id = :id")
        .bind("id", id)
        .exactlyOneOrNull<Boolean>() ?: false

fun Database.Transaction.lockPersonBySSN(ssn: String): PersonDTO? =
    @Suppress("DEPRECATION")
    createQuery(
            """
SELECT
$commaSeparatedPersonDTOColumns
FROM person
WHERE social_security_number = :ssn
FOR UPDATE
    """
                .trimIndent()
        )
        .bind("ssn", ssn)
        .exactlyOneOrNull(toPersonDTO)

fun Database.Read.getPersonBySSN(ssn: String): PersonDTO? {
    @Suppress("DEPRECATION")
    return createQuery(
            """
SELECT
$commaSeparatedPersonDTOColumns
FROM person
WHERE social_security_number = :ssn
        """
                .trimIndent()
        )
        .bind("ssn", ssn)
        .exactlyOneOrNull(toPersonDTO)
}

fun Database.Read.listPersonByDuplicateOf(id: PersonId): List<PersonDTO> =
    @Suppress("DEPRECATION")
    createQuery("SELECT $commaSeparatedPersonDTOColumns FROM person WHERE duplicate_of = :id")
        .bind("id", id)
        .toList(toPersonDTO)

fun Database.Read.getPersonDuplicateOf(id: PersonId): PersonId? =
    @Suppress("DEPRECATION")
    createQuery("SELECT duplicate_of FROM person WHERE id = :id")
        .bind("id", id)
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
    val restrictedDetailsEnabled: Boolean
)

fun Database.Read.searchPeople(
    user: AuthenticatedUser.Employee,
    searchTerms: String,
    sortColumns: String,
    sortDirection: String,
    restricted: Boolean
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

    val (freeTextQuery, freeTextParams) = freeTextSearchQuery(listOf("person"), searchTerms)

    // language=SQL
    val sql =
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
        WHERE $freeTextQuery
        ${if (restricted) "AND id IN (SELECT person_id FROM person_acl_view acl WHERE acl.employee_id = :userId)" else ""}
        ORDER BY $orderBy
        LIMIT 100
    """
            .trimIndent()

    @Suppress("DEPRECATION")
    return createQuery(sql)
        .addBindings(freeTextParams)
        .applyIf(restricted) { this.bind("userId", user.id) }
        .toList<PersonSummary>()
}

fun Database.Transaction.createPerson(person: CreatePersonBody): PersonId {
    // language=SQL
    val sql =
        """
        INSERT INTO person (first_name, last_name, date_of_birth, street_address, postal_code, post_office, phone, email)
        VALUES (:firstName, :lastName, :dateOfBirth, :streetAddress, :postalCode, :postOffice, :phone, :email)
        RETURNING id
        """
            .trimIndent()

    @Suppress("DEPRECATION") return createQuery(sql).bindKotlin(person).exactlyOne<PersonId>()
}

fun Database.Transaction.createEmptyPerson(evakaClock: EvakaClock): PersonDTO {
    // language=SQL
    val sql =
        """
        INSERT INTO person (first_name, last_name, email, date_of_birth)
        VALUES (:firstName, :lastName, :email, :dateOfBirth)
        RETURNING *
        """
            .trimIndent()

    @Suppress("DEPRECATION")
    return createQuery(sql)
        .bind("firstName", "Etunimi")
        .bind("lastName", "Sukunimi")
        .bind("dateOfBirth", evakaClock.today())
        .bind("email", "")
        .exactlyOne(toPersonDTO)
}

fun Database.Transaction.createPersonFromVtj(person: PersonDTO): PersonDTO {
    val sql =
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
            restricted_details_enabled,
            restricted_details_end_date,
            updated_from_vtj
        )
        VALUES (
            :firstName,
            :lastName,
            :dateOfBirth,
            :dateOfDeath,
            :identity,
            :language,
            :nationalities,
            :streetAddress,
            :postalCode,
            :postOffice,
            :residenceCode,
            :restrictedDetailsEnabled,
            :restrictedDetailsEndDate,
            :updatedFromVtj
        )
        RETURNING *
        """

    @Suppress("DEPRECATION")
    return createQuery(sql)
        .bindKotlin(person.copy(updatedFromVtj = HelsinkiDateTime.now()))
        .exactlyOne(toPersonDTO)
}

fun Database.Transaction.duplicatePerson(id: PersonId): PersonId? =
    @Suppress("DEPRECATION")
    createUpdate(
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
FROM person WHERE id = :id
RETURNING id
"""
                .trimIndent()
        )
        .bind("id", id)
        .executeAndReturnGeneratedKeys()
        .exactlyOneOrNull<PersonId>()

fun Database.Transaction.updatePersonFromVtj(person: PersonDTO): PersonDTO {
    // language=SQL
    val sql =
        """
        UPDATE person SET
            first_name = :firstName,
            last_name = :lastName,
            social_security_number = :ssn,
            date_of_birth = :dateOfBirth,
            date_of_death = :dateOfDeath,
            language = :language,
            nationalities = :nationalities,
            street_address = :streetAddress,
            postal_code = :postalCode,
            post_office = :postOffice,
            residence_code = :residenceCode,
            restricted_details_enabled = :restrictedDetailsEnabled,
            restricted_details_end_date = :restrictedDetailsEndDate,
            updated_from_vtj = :updatedFromVtj
        WHERE id = :id
        RETURNING *
        """

    @Suppress("DEPRECATION")
    return createQuery(sql)
        .bindKotlin(person.copy(updatedFromVtj = HelsinkiDateTime.now()))
        .bind("ssn", person.identity)
        .exactlyOne(toPersonDTO)
}

fun Database.Transaction.updatePersonBasicContactInfo(
    id: PersonId,
    email: String,
    phone: String
): Boolean {
    // language=SQL
    val sql =
        """
        UPDATE person SET
            email = :email,
            phone = :phone
        WHERE id = :id
        RETURNING id
        """
            .trimIndent()

    @Suppress("DEPRECATION")
    return createQuery(sql)
        .bind("id", id)
        .bind("email", email)
        .bind("phone", phone)
        .exactlyOneOrNull<PersonId>() != null
}

// Update those person fields which do not come from VTJ
fun Database.Transaction.updatePersonNonVtjDetails(id: PersonId, patch: PersonPatch): Boolean {
    // language=SQL
    val sql =
        """
        UPDATE person SET
            email = coalesce(:email, email),
            phone = coalesce(:phone, phone),
            backup_phone = coalesce(:backupPhone, backup_phone),
            invoice_recipient_name = coalesce(:invoiceRecipientName, invoice_recipient_name),
            invoicing_street_address = coalesce(:invoicingStreetAddress, invoicing_street_address),
            invoicing_postal_code = coalesce(:invoicingPostalCode, invoicing_postal_code),
            invoicing_post_office = coalesce(:invoicingPostOffice, invoicing_post_office),
            force_manual_fee_decisions = coalesce(:forceManualFeeDecisions, force_manual_fee_decisions),
            oph_person_oid = coalesce(:ophPersonOid, oph_person_oid)
        WHERE id = :id
        RETURNING id
        """
            .trimIndent()

    @Suppress("DEPRECATION")
    return createQuery(sql).bind("id", id).bindKotlin(patch).exactlyOneOrNull<PersonId>() != null
}

fun Database.Transaction.updateNonSsnPersonDetails(id: PersonId, patch: PersonPatch): Boolean {
    // language=SQL
    val sql =
        """
        UPDATE person SET
            first_name = coalesce(:firstName, first_name),
            last_name = coalesce(:lastName, last_name),
            date_of_birth = coalesce(:dateOfBirth, date_of_birth),
            email = coalesce(:email, email),
            phone = coalesce(:phone, phone),
            backup_phone = coalesce(:backupPhone, backup_phone),
            street_address = coalesce(:streetAddress, street_address),
            postal_code = coalesce(:postalCode, postal_code),
            post_office = coalesce(:postOffice, post_office),
            invoice_recipient_name = coalesce(:invoiceRecipientName, invoice_recipient_name),
            invoicing_street_address = coalesce(:invoicingStreetAddress, invoicing_street_address),
            invoicing_postal_code = coalesce(:invoicingPostalCode, invoicing_postal_code),
            invoicing_post_office = coalesce(:invoicingPostOffice, invoicing_post_office),
            force_manual_fee_decisions = coalesce(:forceManualFeeDecisions, force_manual_fee_decisions),
            oph_person_oid = coalesce(:ophPersonOid, oph_person_oid)
        WHERE id = :id AND social_security_number IS NULL
        RETURNING id
        """
            .trimIndent()

    @Suppress("DEPRECATION")
    return createQuery(sql).bind("id", id).bindKotlin(patch).exactlyOneOrNull<PersonId>() != null
}

fun Database.Transaction.addSSNToPerson(id: PersonId, ssn: String) {
    // language=SQL
    val sql = "UPDATE person SET social_security_number = :ssn WHERE id = :id"

    @Suppress("DEPRECATION") createUpdate(sql).bind("id", id).bind("ssn", ssn).execute()
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
        updatedFromVtj = column("updated_from_vtj"),
        vtjGuardiansQueried = column("vtj_guardians_queried"),
        vtjDependantsQueried = column("vtj_dependants_queried"),
        invoiceRecipientName = column("invoice_recipient_name"),
        invoicingStreetAddress = column("invoicing_street_address"),
        invoicingPostalCode = column("invoicing_postal_code"),
        invoicingPostOffice = column("invoicing_post_office"),
        forceManualFeeDecisions = column("force_manual_fee_decisions"),
        ophPersonOid = column("oph_person_oid")
    )
}

fun Database.Transaction.updateCitizenOnLogin(
    clock: EvakaClock,
    id: PersonId,
    keycloakEmail: String?
) =
    createUpdate {
            sql(
                """
UPDATE person 
SET last_login = ${bind(clock.now())},
    keycloak_email = coalesce(${bind(keycloakEmail)}, keycloak_email)
WHERE id = ${bind(id)}
"""
            )
        }
        .updateExactlyOne()

data class PersonReference(val table: String, val column: String)

fun Database.Read.getTransferablePersonReferences(): List<PersonReference> {
    // language=sql
    val sql =
        """
        select source.relname as "table", attr.attname as "column"
        from pg_constraint const
            join pg_class source on source.oid = const.conrelid
            join pg_class target on target.oid = const.confrelid
            join pg_attribute attr on attr.attrelid = source.oid and attr.attnum = ANY(const.conkey)
        where const.contype = 'f' 
            and target.relname in ('person', 'child') 
            and source.relname not in ('person', 'child', 'child_images', 'guardian', 'guardian_blocklist', 'message_account')
            and source.relname not like 'old_%'
        order by source.relname, attr.attname
    """
            .trimIndent()
    @Suppress("DEPRECATION") return createQuery(sql).toList<PersonReference>()
}

fun Database.Read.getGuardianDependants(personId: PersonId) =
    @Suppress("DEPRECATION")
    createQuery(
            """
SELECT
$commaSeparatedPersonDTOColumns
FROM person
WHERE id IN (SELECT child_id FROM guardian WHERE guardian_id = :personId)
        """
                .trimIndent()
        )
        .bind("personId", personId)
        .toList(toPersonDTO)

fun Database.Read.getDependantGuardians(personId: ChildId) =
    @Suppress("DEPRECATION")
    createQuery(
            """
SELECT
$commaSeparatedPersonDTOColumns
FROM person
WHERE id IN (SELECT guardian_id FROM guardian WHERE child_id = :personId)
        """
                .trimIndent()
        )
        .bind("personId", personId)
        .toList(toPersonDTO)

fun Database.Transaction.updatePersonSsnAddingDisabled(id: PersonId, disabled: Boolean) {
    @Suppress("DEPRECATION")
    createUpdate("UPDATE person SET ssn_adding_disabled = :disabled WHERE id = :id")
        .bind("id", id)
        .bind("disabled", disabled)
        .execute()
}

fun Database.Transaction.updatePreferredName(id: PersonId, preferredName: String) {
    @Suppress("DEPRECATION")
    createUpdate("UPDATE person SET preferred_name = :preferredName WHERE id = :id")
        .bind("id", id)
        .bind("preferredName", preferredName)
        .execute()
}

fun Database.Transaction.updateOphPersonOid(id: PersonId, ophPersonOid: String) {
    @Suppress("DEPRECATION")
    createUpdate("UPDATE person SET oph_person_oid = :ophPersonOid WHERE id = :id")
        .bind("id", id)
        .bind("ophPersonOid", ophPersonOid)
        .updateExactlyOne()
}
