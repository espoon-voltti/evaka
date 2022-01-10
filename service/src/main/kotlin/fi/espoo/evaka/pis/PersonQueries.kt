// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.pis

import fi.espoo.evaka.identity.ExternalIdentifier
import fi.espoo.evaka.pis.controllers.CreatePersonBody
import fi.espoo.evaka.pis.service.ContactInfo
import fi.espoo.evaka.pis.service.PersonDTO
import fi.espoo.evaka.pis.service.PersonPatch
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.db.freeTextSearchQuery
import fi.espoo.evaka.shared.db.getUUID
import fi.espoo.evaka.shared.db.mapNullableColumn
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.utils.applyIf
import org.jdbi.v3.core.kotlin.bindKotlin
import org.jdbi.v3.core.kotlin.mapTo
import org.jdbi.v3.core.statement.StatementContext
import java.sql.ResultSet
import java.time.LocalDate

val personDTOColumns = listOf(
    "id",
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
    "vtj_guardians_queried",
    "invoice_recipient_name",
    "invoicing_street_address",
    "invoicing_postal_code",
    "invoicing_post_office",
    "force_manual_fee_decisions",
    "oph_person_oid",
)
val commaSeparatedPersonDTOColumns = personDTOColumns.joinToString()

data class CitizenUser(val id: PersonId)

fun Database.Read.getCitizenUserBySsn(ssn: String): CitizenUser? = createQuery(
    "SELECT id FROM person WHERE social_security_number = :ssn"
).bind("ssn", ssn).mapTo<CitizenUser>().firstOrNull()

fun Database.Read.getPersonById(id: PersonId): PersonDTO? {
    return createQuery(
        """
SELECT
$commaSeparatedPersonDTOColumns
FROM person
WHERE id = :id
        """.trimIndent()
    )
        .bind("id", id)
        .map(toPersonDTO)
        .firstOrNull()
}

fun Database.Transaction.lockPersonBySSN(ssn: String): PersonDTO? = createQuery(
    """
SELECT
$commaSeparatedPersonDTOColumns
FROM person
WHERE social_security_number = :ssn
FOR UPDATE
    """.trimIndent()
).bind("ssn", ssn).map(toPersonDTO).firstOrNull()

fun Database.Read.getPersonBySSN(ssn: String): PersonDTO? {
    return createQuery(
        """
SELECT
$commaSeparatedPersonDTOColumns
FROM person
WHERE social_security_number = :ssn
        """.trimIndent()
    )
        .bind("ssn", ssn)
        .map(toPersonDTO)
        .firstOrNull()
}

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

fun Database.Read.searchPeople(user: AuthenticatedUser, searchTerms: String, sortColumns: String, sortDirection: String): List<PersonSummary> {
    if (searchTerms.isBlank()) return listOf()

    val direction = if (sortDirection.equals("DESC", ignoreCase = true)) "DESC" else "ASC"
    val orderBy = sortColumns.split(",").map { it.trim() }.let { columns ->
        if (personSortColumns.containsAll(columns)) {
            columns.joinToString(", ") { column -> "$column $direction" }
        } else {
            "last_name $direction"
        }
    }

    val (freeTextQuery, freeTextParams) = freeTextSearchQuery(listOf("person"), searchTerms)
    @Suppress("DEPRECATION")
    val scopedRole = !user.hasOneOfRoles(UserRole.ADMIN, UserRole.SERVICE_WORKER, UserRole.FINANCE_ADMIN)

    // language=SQL
    val sql = """
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
        ${if (scopedRole) "AND id IN (SELECT person_id FROM person_acl_view acl WHERE acl.employee_id = :userId)" else ""}
        ORDER BY $orderBy
        LIMIT 100
    """.trimIndent()

    return createQuery(sql)
        .bindMap(freeTextParams)
        .applyIf(scopedRole) { bind("userId", user.id) }
        .mapTo<PersonSummary>()
        .toList()
}

fun Database.Transaction.createPerson(person: CreatePersonBody): PersonId {
    // language=SQL
    val sql =
        """
        INSERT INTO person (first_name, last_name, date_of_birth, street_address, postal_code, post_office, phone, email)
        VALUES (:firstName, :lastName, :dateOfBirth, :streetAddress, :postalCode, :postOffice, :phone, :email)
        RETURNING id
        """.trimIndent()

    return createQuery(sql)
        .bindKotlin(person)
        .mapTo<PersonId>()
        .first()
}

fun Database.Transaction.createEmptyPerson(): PersonDTO {
    // language=SQL
    val sql =
        """
        INSERT INTO person (first_name, last_name, email, date_of_birth)
        VALUES (:firstName, :lastName, :email, :dateOfBirth)
        RETURNING *
        """.trimIndent()

    return createQuery(sql)
        .bind("firstName", "Etunimi")
        .bind("lastName", "Sukunimi")
        .bind("dateOfBirth", LocalDate.now())
        .bind("email", "")
        .map(toPersonDTO)
        .first()
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

    return createQuery(sql)
        .bindKotlin(person.copy(updatedFromVtj = HelsinkiDateTime.now()))
        .map(toPersonDTO)
        .first()
}

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

    return createQuery(sql)
        .bindKotlin(person.copy(updatedFromVtj = HelsinkiDateTime.now()))
        .bind("ssn", person.identity)
        .map(toPersonDTO)
        .first()
}

fun Database.Transaction.updatePersonBasicContactInfo(id: PersonId, email: String, phone: String): Boolean {
    // language=SQL
    val sql =
        """
        UPDATE person SET
            email = :email,
            phone = :phone
        WHERE id = :id
        RETURNING id
        """.trimIndent()

    return createQuery(sql)
        .bind("id", id)
        .bind("email", email)
        .bind("phone", phone)
        .mapTo<PersonId>()
        .firstOrNull() != null
}

fun Database.Transaction.updatePersonContactInfo(id: PersonId, contactInfo: ContactInfo): Boolean {
    // language=SQL
    val sql =
        """
        UPDATE person SET
            email = :email,
            phone = :phone,
            backup_phone = :backupPhone,
            invoice_recipient_name = :invoiceRecipientName,
            invoicing_street_address = :invoicingStreetAddress,
            invoicing_postal_code = :invoicingPostalCode,
            invoicing_post_office = :invoicingPostOffice,
            force_manual_fee_decisions = :forceManualFeeDecisions
        WHERE id = :id
        RETURNING id
        """.trimIndent()

    return createQuery(sql)
        .bind("id", id)
        .bindKotlin(contactInfo)
        .mapTo<PersonId>()
        .firstOrNull() != null
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
        """.trimIndent()

    return createQuery(sql)
        .bind("id", id)
        .bindKotlin(patch)
        .mapTo<PersonId>()
        .firstOrNull() != null
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
        """.trimIndent()

    return createQuery(sql)
        .bind("id", id)
        .bindKotlin(patch)
        .mapTo<PersonId>()
        .firstOrNull() != null
}

fun Database.Transaction.addSSNToPerson(id: PersonId, ssn: String) {
    // language=SQL
    val sql = "UPDATE person SET social_security_number = :ssn WHERE id = :id"

    createUpdate(sql)
        .bind("id", id)
        .bind("ssn", ssn)
        .execute()
}

fun Database.Read.getDeceasedPeople(since: LocalDate): List<PersonDTO> {

    return createQuery(
        """
SELECT
$commaSeparatedPersonDTOColumns
FROM person WHERE date_of_death >= :since
        """.trimIndent()
    )
        .bind("since", since)
        .map(toPersonDTO)
        .toList()
}

private val toPersonDTO: (ResultSet, StatementContext) -> PersonDTO = { rs, ctx ->
    PersonDTO(
        id = PersonId(rs.getUUID("id")),
        identity = rs.getString("social_security_number")?.let { ssn -> ExternalIdentifier.SSN.getInstance(ssn) }
            ?: ExternalIdentifier.NoID(),
        ssnAddingDisabled = rs.getBoolean("ssn_adding_disabled"),
        firstName = rs.getString("first_name"),
        lastName = rs.getString("last_name"),
        preferredName = rs.getString("preferred_name"),
        email = rs.getString("email"),
        phone = rs.getString("phone"),
        backupPhone = rs.getString("backup_phone"),
        language = rs.getString("language"),
        dateOfBirth = rs.getDate("date_of_birth").toLocalDate(),
        dateOfDeath = rs.getDate("date_of_death")?.toLocalDate(),
        nationalities = (rs.getArray("nationalities").array as Array<*>).filterIsInstance<String>().toList(),
        restrictedDetailsEnabled = rs.getBoolean("restricted_details_enabled"),
        restrictedDetailsEndDate = rs.getDate("restricted_details_end_date")?.toLocalDate(),
        streetAddress = rs.getString("street_address"),
        postalCode = rs.getString("postal_code"),
        postOffice = rs.getString("post_office"),
        residenceCode = rs.getString("residence_code"),
        updatedFromVtj = ctx.mapNullableColumn(rs, "updated_from_vtj"),
        vtjGuardiansQueried = ctx.mapNullableColumn(rs, "vtj_guardians_queried"),
        vtjDependantsQueried = ctx.mapNullableColumn(rs, "vtj_guardians_queried"),
        invoiceRecipientName = rs.getString("invoice_recipient_name"),
        invoicingStreetAddress = rs.getString("invoicing_street_address"),
        invoicingPostalCode = rs.getString("invoicing_postal_code"),
        invoicingPostOffice = rs.getString("invoicing_post_office"),
        forceManualFeeDecisions = rs.getBoolean("force_manual_fee_decisions"),
        ophPersonOid = rs.getString("oph_person_oid")
    )
}

fun Database.Transaction.markPersonLastLogin(id: PersonId) = createUpdate(
    """
UPDATE person 
SET last_login = now()
WHERE id = :id
    """.trimIndent()
).bind("id", id)
    .execute()

data class PersonReference(
    val table: String,
    val column: String
)

fun Database.Read.getTransferablePersonReferences(): List<PersonReference> {
    // language=sql
    val sql = """
        select source.relname as "table", attr.attname as "column"
        from pg_constraint const
            join pg_class source on source.oid = const.conrelid
            join pg_class target on target.oid = const.confrelid
            join pg_attribute attr on attr.attrelid = source.oid and attr.attnum = ANY(const.conkey)
        where const.contype = 'f' 
            and target.relname in ('person', 'child') 
            and source.relname not in ('person', 'child', 'guardian', 'message_account')
            and source.relname not like 'old_%'
        order by source.relname, attr.attname
    """.trimIndent()
    return createQuery(sql).mapTo<PersonReference>().list()
}

fun Database.Read.getGuardianDependants(personId: PersonId) =
    createQuery(
        """
SELECT
$commaSeparatedPersonDTOColumns
FROM person
WHERE id IN (SELECT child_id FROM guardian WHERE guardian_id = :personId)
        """.trimIndent()
    )
        .bind("personId", personId)
        .map(toPersonDTO)
        .toList()

fun Database.Read.getDependantGuardians(personId: ChildId) =
    createQuery(
        """
SELECT
$commaSeparatedPersonDTOColumns
FROM person
WHERE id IN (SELECT guardian_id FROM guardian WHERE child_id = :personId)
        """.trimIndent()
    )
        .bind("personId", personId)
        .map(toPersonDTO)
        .toList()

fun Database.Transaction.updatePersonSsnAddingDisabled(id: PersonId, disabled: Boolean) {
    createUpdate("UPDATE person SET ssn_adding_disabled = :disabled WHERE id = :id")
        .bind("id", id)
        .bind("disabled", disabled)
        .execute()
}

fun Database.Transaction.updatePreferredName(id: PersonId, preferredName: String) {
    createUpdate("UPDATE person SET preferred_name = :preferredName WHERE id = :id")
        .bind("id", id)
        .bind("preferredName", preferredName)
        .execute()
}
