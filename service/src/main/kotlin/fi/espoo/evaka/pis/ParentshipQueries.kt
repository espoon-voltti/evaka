// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.pis

import fi.espoo.evaka.pis.service.Parentship
import fi.espoo.evaka.pis.service.PersonJSON
import fi.espoo.evaka.shared.db.PGConstants
import fi.espoo.evaka.shared.db.bindNullable
import fi.espoo.evaka.shared.db.getUUID
import fi.espoo.evaka.shared.domain.BadRequest
import fi.espoo.evaka.shared.domain.DateRange
import org.jdbi.v3.core.Handle
import org.jdbi.v3.core.kotlin.mapTo
import org.jdbi.v3.core.statement.StatementContext
import java.sql.ResultSet
import java.time.LocalDate
import java.util.UUID

fun Handle.getParentship(id: UUID): Parentship? {
    // language=SQL
    val sql =
        """
        SELECT
            fc.*,
            ${aliasedPersonColumns("child")},
            ${aliasedPersonColumns("head")}
        FROM fridge_child fc
        JOIN person child ON fc.child_id = child.id
        JOIN person head ON fc.head_of_child = head.id
        WHERE fc.id = :id
        """.trimIndent()

    return createQuery(sql)
        .bind("id", id)
        .map(toParentship("child", "head"))
        .firstOrNull()
}

fun Handle.getParentships(
    headOfChildId: UUID?,
    childId: UUID?,
    includeConflicts: Boolean = false,
    period: DateRange? = null
): List<Parentship> {
    if (headOfChildId == null && childId == null) throw BadRequest("Must give either headOfChildId or childId")

    // language=SQL
    val sql =
        """
        SELECT
            fc.*,
            ${aliasedPersonColumns("child")},
            ${aliasedPersonColumns("head")}
        FROM fridge_child fc
        JOIN person child ON fc.child_id = child.id
        JOIN person head ON fc.head_of_child = head.id
        WHERE (:headOfChild::uuid IS NULL OR head_of_child = :headOfChild)
        AND (:child::uuid IS NULL OR child_id = :child)
        AND daterange(fc.start_date, fc.end_date, '[]') && daterange(:from, :to, '[]')
        AND (:includeConflicts OR conflict = false)
        """.trimIndent()

    return createQuery(sql)
        .bindNullable("headOfChild", headOfChildId)
        .bindNullable("child", childId)
        .bindNullable("from", period?.start)
        .bindNullable("to", period?.end)
        .bind("includeConflicts", includeConflicts)
        .map(toParentship("child", "head"))
        .toList()
}

fun Handle.createParentship(
    childId: UUID,
    headOfChildId: UUID,
    startDate: LocalDate,
    endDate: LocalDate?,
    conflict: Boolean = false
): Parentship {
    // language=SQL
    val sql =
        """
        WITH new_fridge_child AS (
            INSERT INTO fridge_child (child_id, head_of_child, start_date, end_date, conflict)
            VALUES (:childId, :headOfChild, :startDate, :endDate, :conflict)
            RETURNING *
        )
        SELECT
            fc.*,
            ${aliasedPersonColumns("child")},
            ${aliasedPersonColumns("head")}
        FROM new_fridge_child fc
        JOIN person child ON fc.child_id = child.id
        JOIN person head ON fc.head_of_child = head.id
        """.trimIndent()

    return createQuery(sql)
        .bind("childId", childId)
        .bind("headOfChild", headOfChildId)
        .bind("startDate", startDate)
        .bind("endDate", endDate ?: PGConstants.infinity)
        .bind("conflict", conflict)
        .map(toParentship("child", "head"))
        .first()
}

fun Handle.updateParentshipDuration(id: UUID, startDate: LocalDate, endDate: LocalDate?): Boolean {
    // language=SQL
    val sql = "UPDATE fridge_child SET start_date = :startDate, end_date = :endDate WHERE id = :id RETURNING id"

    return createQuery(sql)
        .bind("id", id)
        .bind("startDate", startDate)
        .bind("endDate", endDate ?: PGConstants.infinity)
        .mapTo<UUID>()
        .firstOrNull() != null
}

fun Handle.retryParentship(id: UUID) {
    // language=SQL
    val sql = "UPDATE fridge_child SET conflict = false WHERE id = :id"
    createUpdate(sql)
        .bind("id", id)
        .execute()
}

fun Handle.deleteParentship(id: UUID): Boolean {
    // language=SQL
    val sql = "DELETE FROM fridge_child WHERE id = :id RETURNING id"

    return createQuery(sql)
        .bind("id", id)
        .mapTo<UUID>()
        .firstOrNull() != null
}

internal val aliasedPersonColumns: (String) -> String = { table ->
    personColumns.joinToString(", ") { column -> "$table.$column AS ${table}_$column" }
}

private val personColumns = listOf(
    "id",
    "customer_id",
    "social_security_number",
    "first_name",
    "last_name",
    "email",
    "phone",
    "language",
    "date_of_birth",
    "date_of_death",
    "street_address",
    "post_office",
    "postal_code",
    "residence_code",
    "restricted_details_enabled",
    "invoice_recipient_name",
    "invoicing_street_address",
    "postal_code",
    "post_office",
    "force_manual_fee_decisions"
)

private val toParentship: (String, String) -> (ResultSet, StatementContext) -> Parentship = { childAlias, headAlias ->
    { rs, _ ->
        Parentship(
            id = rs.getUUID("id"),
            childId = rs.getUUID("child_id"),
            child = toPersonJSON(childAlias, rs),
            headOfChildId = rs.getUUID("head_of_child"),
            headOfChild = toPersonJSON(headAlias, rs),
            startDate = rs.getDate("start_date").toLocalDate(),
            endDate = rs.getDate("end_date").toLocalDate()
                .let { if (it.isBefore(PGConstants.infinity)) it else null },
            conflict = rs.getBoolean("conflict")
        )
    }
}

internal val toPersonJSON: (String, ResultSet) -> PersonJSON = { table, rs ->
    PersonJSON(
        id = rs.getUUID("${table}_id"),
        customerId = rs.getLong("${table}_customer_id"),
        socialSecurityNumber = rs.getString("${table}_social_security_number"),
        firstName = rs.getString("${table}_first_name"),
        lastName = rs.getString("${table}_last_name"),
        email = rs.getString("${table}_email"),
        phone = rs.getString("${table}_phone"),
        language = rs.getString("${table}_language"),
        dateOfBirth = rs.getDate("${table}_date_of_birth").toLocalDate(),
        dateOfDeath = rs.getDate("${table}_date_of_death")?.toLocalDate(),
        streetAddress = rs.getString("${table}_street_address"),
        postOffice = rs.getString("${table}_post_office"),
        postalCode = rs.getString("${table}_postal_code"),
        residenceCode = rs.getString("${table}_residence_code"),
        restrictedDetailsEnabled = rs.getBoolean("${table}_restricted_details_enabled"),
        invoiceRecipientName = rs.getString("${table}_invoice_recipient_name"),
        invoicingStreetAddress = rs.getString("${table}_invoicing_street_address"),
        invoicingPostalCode = rs.getString("${table}_postal_code"),
        invoicingPostOffice = rs.getString("${table}_post_office"),
        forceManualFeeDecisions = rs.getBoolean("${table}_force_manual_fee_decisions")
    )
}
