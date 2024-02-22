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
import fi.espoo.evaka.shared.db.Row
import fi.espoo.evaka.shared.domain.BadRequest
import fi.espoo.evaka.shared.domain.DateRange
import java.time.LocalDate
import java.util.UUID

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

    @Suppress("DEPRECATION")
    return createQuery(sql).bind("id", id).exactlyOneOrNull(toParentship("child", "head"))
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

    @Suppress("DEPRECATION")
    return createQuery(sql)
        .bind("headOfChild", headOfChildId)
        .bind("child", childId)
        .bind("from", period?.start)
        .bind("to", period?.end)
        .bind("includeConflicts", includeConflicts)
        .toList(toParentship("child", "head"))
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

    @Suppress("DEPRECATION")
    return createQuery(sql)
        .bind("childId", childId)
        .bind("headOfChild", headOfChildId)
        .bind("startDate", startDate)
        .bind("endDate", endDate)
        .bind("conflict", conflict)
        .exactlyOne(toParentship("child", "head"))
}

fun Database.Transaction.updateParentshipDuration(
    id: ParentshipId,
    startDate: LocalDate,
    endDate: LocalDate
): Boolean {
    // language=sql
    val sql = "UPDATE fridge_child SET start_date = :startDate, end_date = :endDate WHERE id = :id"

    @Suppress("DEPRECATION")
    return createUpdate(sql)
        .bind("id", id)
        .bind("startDate", startDate)
        .bind("endDate", endDate)
        .execute() > 0
}

fun Database.Transaction.retryParentship(id: ParentshipId) {
    // language=SQL
    val sql = "UPDATE fridge_child SET conflict = false WHERE id = :id"
    @Suppress("DEPRECATION") createUpdate(sql).bind("id", id).execute()
}

fun Database.Transaction.deleteParentship(id: ParentshipId): Boolean {
    // language=SQL
    val sql = "DELETE FROM fridge_child WHERE id = :id RETURNING id"

    @Suppress("DEPRECATION")
    return createQuery(sql).bind("id", id).exactlyOneOrNull<ParentshipId>() != null
}

fun Database.Read.personIsHeadOfFamily(personId: PersonId, date: LocalDate): Boolean {
    @Suppress("DEPRECATION")
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
        .exactlyOne<Boolean>()
}

internal val aliasedPersonColumns: (String) -> String = { table ->
    personColumns.joinToString(", ") { column -> "$table.$column AS ${table}_$column" }
}

private val personColumns =
    listOf(
        "id",
        "duplicate_of",
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

private val toParentship: (String, String) -> Row.() -> Parentship = { childAlias, headAlias ->
    {
        Parentship(
            id = ParentshipId(column("id")),
            childId = ChildId(column("child_id")),
            child = toPersonJSON(childAlias),
            headOfChildId = PersonId(column("head_of_child")),
            headOfChild = toPersonJSON(headAlias),
            startDate = column("start_date"),
            endDate = column("end_date"),
            conflict = column("conflict")
        )
    }
}

internal val toPersonJSON: Row.(String) -> PersonJSON = { table ->
    PersonJSON(
        id = PersonId(column("${table}_id")),
        duplicateOf = column<UUID?>("${table}_duplicate_of")?.let { PersonId(it) },
        socialSecurityNumber = column("${table}_social_security_number"),
        ssnAddingDisabled = column("${table}_ssn_adding_disabled"),
        firstName = column("${table}_first_name"),
        lastName = column("${table}_last_name"),
        email = column("${table}_email"),
        phone = column("${table}_phone"),
        language = column("${table}_language"),
        dateOfBirth = column("${table}_date_of_birth"),
        dateOfDeath = column("${table}_date_of_death"),
        streetAddress = column("${table}_street_address"),
        postOffice = column("${table}_post_office"),
        postalCode = column("${table}_postal_code"),
        residenceCode = column("${table}_residence_code"),
        restrictedDetailsEnabled = column("${table}_restricted_details_enabled"),
        invoiceRecipientName = column("${table}_invoice_recipient_name"),
        invoicingStreetAddress = column("${table}_invoicing_street_address"),
        invoicingPostalCode = column("${table}_postal_code"),
        invoicingPostOffice = column("${table}_post_office"),
        forceManualFeeDecisions = column("${table}_force_manual_fee_decisions")
    )
}
