// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.pis

import fi.espoo.evaka.identity.ExternalIdentifier
import fi.espoo.evaka.identity.getDobFromSsn
import fi.espoo.evaka.pis.controllers.CreatePersonBody
import fi.espoo.evaka.pis.service.ContactInfo
import fi.espoo.evaka.pis.service.PersonDTO
import fi.espoo.evaka.pis.service.PersonIdentityRequest
import fi.espoo.evaka.pis.service.PersonPatch
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.db.freeTextSearchQuery
import fi.espoo.evaka.shared.db.getUUID
import fi.espoo.evaka.shared.utils.applyIf
import org.jdbi.v3.core.kotlin.bindKotlin
import org.jdbi.v3.core.kotlin.mapTo
import org.jdbi.v3.core.statement.StatementContext
import java.sql.ResultSet
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

fun Database.Read.getPersonById(id: UUID): PersonDTO? {
    // language=SQL
    val sql = "SELECT * FROM person WHERE id = :id"

    return createQuery(sql)
        .bind("id", id)
        .map(toPersonDTO)
        .firstOrNull()
}

fun Database.Transaction.lockPersonBySSN(ssn: String): PersonDTO? = createQuery(
    "SELECT * FROM person WHERE social_security_number = :ssn FOR UPDATE"
).bind("ssn", ssn).map(toPersonDTO).firstOrNull()

fun Database.Read.getPersonBySSN(ssn: String): PersonDTO? {
    // language=SQL
    val sql = "SELECT * FROM person WHERE social_security_number = :ssn"

    return createQuery(sql)
        .bind("ssn", ssn)
        .map(toPersonDTO)
        .firstOrNull()
}

private val personSortColumns =
    listOf("first_name", "last_name", "date_of_birth", "street_address", "social_security_number")

fun Database.Read.searchPeople(user: AuthenticatedUser, searchTerms: String, sortColumns: String, sortDirection: String): List<PersonDTO> {
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
    val scopedRole = !user.hasOneOfRoles(UserRole.ADMIN, UserRole.SERVICE_WORKER, UserRole.FINANCE_ADMIN)

    // language=SQL
    val sql = """
        SELECT DISTINCT person.*
        FROM person
        ${if (scopedRole) "JOIN child_acl_view acl ON acl.child_id = person.id AND acl.employee_id = :userId" else ""}
        WHERE $freeTextQuery
        ORDER BY $orderBy
        LIMIT 100
    """.trimIndent()

    return createQuery(sql)
        .bindMap(freeTextParams)
        .applyIf(scopedRole) { bind("userId", user.id) }
        .map(toPersonDTO)
        .toList()
}

fun Database.Transaction.createPerson(person: PersonIdentityRequest): PersonDTO {
    // language=SQL
    val sql =
        """
        INSERT INTO person (first_name, last_name, date_of_birth, social_security_number, language, email)
        VALUES (:firstName, :lastName, :dateOfBirth, :ssn, :language, :email)
        RETURNING *
        """.trimIndent()

    return createQuery(sql)
        .bind("firstName", person.firstName)
        .bind("lastName", person.lastName)
        .bind("dateOfBirth", getDobFromSsn(person.identity.ssn))
        .bind("ssn", person.identity.ssn)
        .bind("language", person.language)
        .bind("email", person.email)
        .map(toPersonDTO)
        .first()
}

fun Database.Transaction.createPerson(person: CreatePersonBody): UUID {
    // language=SQL
    val sql =
        """
        INSERT INTO person (first_name, last_name, date_of_birth, street_address, postal_code, post_office, phone, email)
        VALUES (:firstName, :lastName, :dateOfBirth, :streetAddress, :postalCode, :postOffice, :phone, :email)
        RETURNING id
        """.trimIndent()

    return createQuery(sql)
        .bindKotlin(person)
        .mapTo<UUID>()
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
        .bindKotlin(person.copy(updatedFromVtj = Instant.now()))
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
        .bindKotlin(person.copy(updatedFromVtj = Instant.now()))
        .map(toPersonDTO)
        .first()
}

fun Database.Transaction.updatePersonBasicContactInfo(id: UUID, email: String, phone: String): Boolean {
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
        .mapTo<UUID>()
        .firstOrNull() != null
}

fun Database.Transaction.updatePersonContactInfo(id: UUID, contactInfo: ContactInfo): Boolean {
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
        .mapTo<UUID>()
        .firstOrNull() != null
}

fun Database.Transaction.updatePersonDetails(id: UUID, patch: PersonPatch): Boolean {
    // language=SQL
    val sql =
        """
        UPDATE person SET
            first_name = coalesce(:firstName, first_name),
            last_name = coalesce(:lastName, last_name),
            date_of_birth = coalesce(:dateOfBirth, date_of_birth),
            email = coalesce(:email, email),
            phone = coalesce(:phone, phone),
            street_address = coalesce(:streetAddress, street_address),
            postal_code = coalesce(:postalCode, postal_code),
            post_office = coalesce(:postOffice, post_office),
            invoice_recipient_name = coalesce(:invoiceRecipientName, invoice_recipient_name),
            invoicing_street_address = coalesce(:invoicingStreetAddress, invoicing_street_address),
            invoicing_postal_code = coalesce(:invoicingPostalCode, invoicing_postal_code),
            invoicing_post_office = coalesce(:invoicingPostOffice, invoicing_post_office),
            force_manual_fee_decisions = coalesce(:forceManualFeeDecisions, force_manual_fee_decisions)
        WHERE id = :id AND social_security_number IS NULL
        RETURNING id
        """.trimIndent()

    return createQuery(sql)
        .bind("id", id)
        .bindKotlin(patch)
        .mapTo<UUID>()
        .firstOrNull() != null
}

fun Database.Transaction.addSSNToPerson(id: UUID, ssn: String) {
    // language=SQL
    val sql = "UPDATE person SET social_security_number = :ssn WHERE id = :id"

    createUpdate(sql)
        .bind("id", id)
        .bind("ssn", ssn)
        .execute()
}

fun Database.Read.getDeceasedPeople(since: LocalDate): List<PersonDTO> {
    // language=SQL
    val sql = "SELECT * FROM person WHERE date_of_death >= :since"

    return createQuery(sql)
        .bind("since", since)
        .map(toPersonDTO)
        .toList()
}

private val toPersonDTO: (ResultSet, StatementContext) -> PersonDTO = { rs, _ ->
    PersonDTO(
        id = rs.getUUID("id"),
        customerId = rs.getLong("customer_id"),
        identity = rs.getString("social_security_number")?.let { ssn -> ExternalIdentifier.SSN.getInstance(ssn) }
            ?: ExternalIdentifier.NoID(),
        firstName = rs.getString("first_name"),
        lastName = rs.getString("last_name"),
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
        updatedFromVtj = rs.getTimestamp("updated_from_vtj")?.toInstant(),
        invoiceRecipientName = rs.getString("invoice_recipient_name"),
        invoicingStreetAddress = rs.getString("invoicing_street_address"),
        invoicingPostalCode = rs.getString("invoicing_postal_code"),
        invoicingPostOffice = rs.getString("invoicing_post_office"),
        forceManualFeeDecisions = rs.getBoolean("force_manual_fee_decisions")
    )
}

fun Database.Transaction.markPersonLastLogin(id: UUID) = createUpdate(
    """
UPDATE person 
SET last_login = now()
WHERE id = :id
    """.trimIndent()
).bind("id", id)
    .execute()
