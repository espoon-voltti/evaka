// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.pis

import fi.espoo.evaka.pis.service.Parentship
import fi.espoo.evaka.pis.service.PersonJSON
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.ParentshipId
import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.db.mapColumn
import fi.espoo.evaka.shared.domain.BadRequest
import fi.espoo.evaka.shared.domain.DateRange
import java.time.LocalDate
import org.jdbi.v3.core.result.RowView

fun Database.Read.getParentship(id: ParentshipId): Parentship? {
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
        """
            .trimIndent()

    return createQuery(sql).bind("id", id).map(toParentship("child", "head")).firstOrNull()
}

fun Database.Read.getParentships(
    headOfChildId: PersonId?,
    childId: ChildId?,
    includeConflicts: Boolean = false,
    period: DateRange? = null
): List<Parentship> {
    if (headOfChildId == null && childId == null)
        throw BadRequest("Must give either headOfChildId or childId")

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
        """
            .trimIndent()

    return createQuery(sql)
        .bind("headOfChild", headOfChildId)
        .bind("child", childId)
        .bind("from", period?.start)
        .bind("to", period?.end)
        .bind("includeConflicts", includeConflicts)
        .map(toParentship("child", "head"))
        .toList()
}

fun Database.Transaction.createParentship(
    childId: ChildId,
    headOfChildId: PersonId,
    startDate: LocalDate,
    endDate: LocalDate,
    conflict: Boolean = false
): Parentship {
    // language=sql
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
        """
            .trimIndent()

    return createQuery(sql)
        .bind("childId", childId)
        .bind("headOfChild", headOfChildId)
        .bind("startDate", startDate)
        .bind("endDate", endDate)
        .bind("conflict", conflict)
        .map(toParentship("child", "head"))
        .exactlyOne()
}

fun Database.Transaction.updateParentshipDuration(
    id: ParentshipId,
    startDate: LocalDate,
    endDate: LocalDate
): Boolean {
    // language=sql
    val sql = "UPDATE fridge_child SET start_date = :startDate, end_date = :endDate WHERE id = :id"

    return createUpdate(sql)
        .bind("id", id)
        .bind("startDate", startDate)
        .bind("endDate", endDate)
        .execute() > 0
}

fun Database.Transaction.retryParentship(id: ParentshipId) {
    // language=SQL
    val sql = "UPDATE fridge_child SET conflict = false WHERE id = :id"
    createUpdate(sql).bind("id", id).execute()
}

fun Database.Transaction.deleteParentship(id: ParentshipId): Boolean {
    // language=SQL
    val sql = "DELETE FROM fridge_child WHERE id = :id RETURNING id"

    return createQuery(sql).bind("id", id).mapTo<ParentshipId>().firstOrNull() != null
}

fun Database.Read.personIsHeadOfFamily(personId: PersonId, date: LocalDate): Boolean {
    return createQuery(
            """
SELECT EXISTS(
    SELECT * FROM fridge_child WHERE head_of_child = :personId AND daterange(start_date, end_date, '[]') @> :date AND NOT conflict
)
        """
                .trimIndent()
        )
        .bind("personId", personId)
        .bind("date", date)
        .mapTo<Boolean>()
        .exactlyOne()
}

internal val aliasedPersonColumns: (String) -> String = { table ->
    personColumns.joinToString(", ") { column -> "$table.$column AS ${table}_$column" }
}

private val personColumns =
    listOf(
        "id",
        "social_security_number",
        "ssn_adding_disabled",
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

private val toParentship: (String, String) -> (RowView) -> Parentship = { childAlias, headAlias ->
    { row ->
        Parentship(
            id = ParentshipId(row.mapColumn("id")),
            childId = ChildId(row.mapColumn("child_id")),
            child = toPersonJSON(childAlias, row),
            headOfChildId = PersonId(row.mapColumn("head_of_child")),
            headOfChild = toPersonJSON(headAlias, row),
            startDate = row.mapColumn("start_date"),
            endDate = row.mapColumn("end_date"),
            conflict = row.mapColumn("conflict")
        )
    }
}

internal val toPersonJSON: (String, RowView) -> PersonJSON = { table, row ->
    PersonJSON(
        id = PersonId(row.mapColumn("${table}_id")),
        socialSecurityNumber = row.mapColumn("${table}_social_security_number"),
        ssnAddingDisabled = row.mapColumn("${table}_ssn_adding_disabled"),
        firstName = row.mapColumn("${table}_first_name"),
        lastName = row.mapColumn("${table}_last_name"),
        email = row.mapColumn("${table}_email"),
        phone = row.mapColumn("${table}_phone"),
        language = row.mapColumn("${table}_language"),
        dateOfBirth = row.mapColumn("${table}_date_of_birth"),
        dateOfDeath = row.mapColumn("${table}_date_of_death"),
        streetAddress = row.mapColumn("${table}_street_address"),
        postOffice = row.mapColumn("${table}_post_office"),
        postalCode = row.mapColumn("${table}_postal_code"),
        residenceCode = row.mapColumn("${table}_residence_code"),
        restrictedDetailsEnabled = row.mapColumn("${table}_restricted_details_enabled"),
        invoiceRecipientName = row.mapColumn("${table}_invoice_recipient_name"),
        invoicingStreetAddress = row.mapColumn("${table}_invoicing_street_address"),
        invoicingPostalCode = row.mapColumn("${table}_postal_code"),
        invoicingPostOffice = row.mapColumn("${table}_post_office"),
        forceManualFeeDecisions = row.mapColumn("${table}_force_manual_fee_decisions")
    )
}
