// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.pis

import fi.espoo.evaka.pis.service.Parentship
import fi.espoo.evaka.pis.service.ParentshipDetailed
import fi.espoo.evaka.pis.service.PersonJSON
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.EvakaUserId
import fi.espoo.evaka.shared.ParentshipId
import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.db.Row
import fi.espoo.evaka.shared.domain.BadRequest
import fi.espoo.evaka.shared.domain.DateRange
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import java.time.LocalDate
import java.util.UUID

fun Database.Read.getParentship(id: ParentshipId): Parentship? {
    return createQuery {
            sql(
                """
                SELECT
                    fc.*,
                    ${aliasedPersonColumns("child")},
                    ${aliasedPersonColumns("head")}
                FROM fridge_child fc
                JOIN person child ON fc.child_id = child.id
                JOIN person head ON fc.head_of_child = head.id
                WHERE fc.id = ${bind(id)}
                """
            )
        }
        .exactlyOneOrNull(toParentship("child", "head"))
}

fun Database.Read.getParentships(
    headOfChildId: PersonId?,
    childId: ChildId?,
    includeConflicts: Boolean = false,
    period: DateRange? = null,
): List<ParentshipDetailed> {
    if (headOfChildId == null && childId == null)
        throw BadRequest("Must give either headOfChildId or childId")

    return createQuery {
            sql(
                """
SELECT
    fc.*,
    ${aliasedPersonColumns("child")},
    ${aliasedPersonColumns("head")},
    created_by_user.name AS created_by_user_name,
    modified_by_user.name AS modified_by_user_name,
    created_by_application.type AS created_by_application_type,
    created_by_application.created_at AS created_by_application_created
FROM fridge_child fc
JOIN person child ON fc.child_id = child.id
JOIN person head ON fc.head_of_child = head.id
LEFT JOIN application created_by_application ON fc.created_by_application = created_by_application.id
LEFT JOIN evaka_user created_by_user ON fc.created_by_user = created_by_user.id
LEFT JOIN evaka_user modified_by_user ON fc.modified_by_user = modified_by_user.id
WHERE (${bind(headOfChildId)}::uuid IS NULL OR head_of_child = ${bind(headOfChildId)})
AND (${bind(childId)}::uuid IS NULL OR fc.child_id = ${bind(childId)})
AND daterange(fc.start_date, fc.end_date, '[]') && daterange(${bind(period?.start)}, ${bind(period?.end)}, '[]')
AND (${bind(includeConflicts)} OR conflict = false)
"""
            )
        }
        .toList(toParentshipDetailed("child", "head"))
}

fun Database.Transaction.createParentship(
    childId: ChildId,
    headOfChildId: PersonId,
    startDate: LocalDate,
    endDate: LocalDate,
    creator: Creator,
    conflict: Boolean = false,
): Parentship {
    val (userId, applicationId) =
        when (creator) {
            is Creator.User -> Pair(creator.id.raw, null)
            is Creator.Application -> Pair(null, creator.id)
            is Creator.DVV -> Pair(null, null)
        }
    return createQuery {
            sql(
                """
WITH new_fridge_child AS (
    INSERT INTO fridge_child (child_id, head_of_child, start_date, end_date, create_source, created_by_user, created_by_application, modify_source, modified_by_user, modified_at, conflict)
    VALUES (${bind(childId)}, ${bind(headOfChildId)}, ${bind(startDate)}, ${bind(endDate)}, ${bind(creator.source)}, ${bind(userId)}, ${bind(applicationId)}, NULL, NULL, NULL, ${bind(conflict)})
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
            )
        }
        .exactlyOne(toParentship("child", "head"))
}

fun Database.Transaction.updateParentshipDuration(
    id: ParentshipId,
    startDate: LocalDate,
    endDate: LocalDate,
    now: HelsinkiDateTime,
    modifier: Modifier,
): Boolean {
    val userId =
        when (modifier) {
            is Modifier.User -> modifier.id.raw
            is Modifier.DVV -> null
        }

    return createUpdate {
            sql(
                """
                UPDATE fridge_child 
                SET 
                    start_date = ${bind(startDate)}, 
                    end_date = ${bind(endDate)},
                    modify_source = ${bind(modifier.source)},
                    modified_by_user = ${bind(userId)},
                    modified_at = ${bind(now)}
                WHERE id = ${bind(id)}
            """
            )
        }
        .execute() > 0
}

fun Database.Transaction.retryParentship(
    id: ParentshipId,
    now: HelsinkiDateTime,
    userId: EvakaUserId,
) {
    createUpdate {
            sql(
                """
        UPDATE fridge_child 
        SET conflict = false, modify_source = 'USER', modified_by_user = ${bind(userId)}, modified_at = ${bind(now)} 
        WHERE id = ${bind(id)}
    """
                    .trimIndent()
            )
        }
        .execute()
}

fun Database.Transaction.deleteParentship(id: ParentshipId): Boolean {
    return createQuery { sql("DELETE FROM fridge_child WHERE id = ${bind(id)} RETURNING id") }
        .exactlyOneOrNull<ParentshipId>() != null
}

fun Database.Read.personIsHeadOfFamily(personId: PersonId, date: LocalDate): Boolean {
    return createQuery {
            sql(
                """
SELECT EXISTS(
    SELECT * FROM fridge_child WHERE head_of_child = ${bind(personId)} AND daterange(start_date, end_date, '[]') @> ${bind(date)} AND NOT conflict
)
"""
            )
        }
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
        "force_manual_fee_decisions",
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
            conflict = column("conflict"),
        )
    }
}

private val toParentshipDetailed: (String, String) -> Row.() -> ParentshipDetailed =
    { childAlias, headAlias ->
        {
            ParentshipDetailed(
                id = ParentshipId(column("id")),
                childId = ChildId(column("child_id")),
                child = toPersonJSON(childAlias),
                headOfChildId = PersonId(column("head_of_child")),
                headOfChild = toPersonJSON(headAlias),
                startDate = column("start_date"),
                endDate = column("end_date"),
                conflict = column("conflict"),
                creationModificationMetadata =
                    CreationModificationMetadata(
                        createSource = column("create_source"),
                        createdAt = column("created_at"),
                        createdBy = column("created_by_user"),
                        createdByName = column("created_by_user_name"),
                        modifySource = column("modify_source"),
                        modifiedAt = column("modified_at"),
                        modifiedBy = column("modified_by_user"),
                        modifiedByName = column("modified_by_user_name"),
                        createdFromApplication = column("created_by_application"),
                        createdFromApplicationType = column("created_by_application_type"),
                        createdFromApplicationCreated = column("created_by_application_created"),
                    ),
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
        forceManualFeeDecisions = column("${table}_force_manual_fee_decisions"),
    )
}
