// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.pis

import fi.espoo.evaka.pis.service.CreateType
import fi.espoo.evaka.pis.service.ModifyType
import fi.espoo.evaka.pis.service.Partner
import fi.espoo.evaka.pis.service.Partnership
import fi.espoo.evaka.shared.ApplicationId
import fi.espoo.evaka.shared.EvakaUserId
import fi.espoo.evaka.shared.PartnershipId
import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.db.Row
import fi.espoo.evaka.shared.domain.DateRange
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import java.time.LocalDate
import java.util.UUID

fun Database.Read.getPartnership(id: PartnershipId): Partnership? {
    // language=SQL
    val sql =
        """
        SELECT
            fp1.partnership_id,
            fp1.start_date,
            fp1.end_date,
            fp1.conflict,
            ${aliasedPersonColumns("p1")},
            ${aliasedPersonColumns("p2")},
            fp1.created_at,
            fp1.created_by,
            fp1.modified_at,
            fp1.modified_by
        FROM fridge_partner fp1
        JOIN fridge_partner fp2 ON fp1.partnership_id = fp2.partnership_id AND fp1.indx = 1 AND fp2.indx = 2
        JOIN person p1 ON fp1.person_id = p1.id
        JOIN person p2 ON fp2.person_id = p2.id
        WHERE fp1.partnership_id = :id
        """
            .trimIndent()

    return createQuery(sql).bind("id", id).exactlyOneOrNull(toPartnership("p1", "p2"))
}

fun Database.Read.getPartnershipsForPerson(
    personId: PersonId,
    includeConflicts: Boolean = false
): List<Partnership> {
    // language=SQL
    val sql =
        """
        SELECT
            fp1.partnership_id,
            fp1.start_date,
            fp1.end_date,
            fp1.conflict,
            ${aliasedPersonColumns("p1")},
            ${aliasedPersonColumns("p2")},
            fp1.created_at,
            fp1.created_by,
            fp1.modified_at,
            fp1.modified_by
        FROM fridge_partner fp1
        JOIN fridge_partner fp2 ON fp1.partnership_id = fp2.partnership_id AND fp1.indx = 1 AND fp2.indx = 2
        JOIN person p1 ON fp1.person_id = p1.id
        JOIN person p2 ON fp2.person_id = p2.id
        WHERE fp1.person_id = :personId OR fp2.person_id = :personId
        AND (:includeConflicts OR fp1.conflict = false)
        """
            .trimIndent()

    return createQuery(sql)
        .bind("personId", personId)
        .bind("includeConflicts", includeConflicts)
        .toList(toPartnership("p1", "p2"))
}

fun Database.Read.getPartnersForPerson(
    personId: PersonId,
    includeConflicts: Boolean,
    period: DateRange? = null
): List<Partner> {
    // language=SQL
    val sql =
        """
        SELECT
            fp.*,
            ${aliasedPersonColumns("p")},
            (SELECT name FROM evaka_user WHERE id = fp.created_by) AS created_by_name,
            (SELECT name FROM evaka_user WHERE id = fp.modified_by) AS modified_by_name,
            a.type AS created_from_application_type,
            a.created AS created_from_application_created
        FROM fridge_partner fp
        JOIN fridge_partner partner ON fp.partnership_id = partner.partnership_id AND fp.indx != partner.indx
        JOIN person p ON partner.person_id = p.id
        LEFT JOIN application a ON fp.created_from_application = a.id
        WHERE fp.person_id = :personId
        AND daterange(fp.start_date, fp.end_date, '[]') && daterange(:from, :to, '[]')
        AND (:includeConflicts OR fp.conflict = false)
        """
            .trimIndent()

    return createQuery(sql)
        .bind("personId", personId)
        .bind("from", period?.start)
        .bind("to", period?.end)
        .bind("includeConflicts", includeConflicts)
        .toList(toPartner("p"))
}

fun Database.Transaction.createPartnership(
    personId1: PersonId,
    personId2: PersonId,
    startDate: LocalDate,
    endDate: LocalDate?,
    conflict: Boolean = false,
    creatorId: EvakaUserId?,
    applicationId: ApplicationId?,
    createDate: HelsinkiDateTime
): Partnership {
    val createType =
        if (creatorId != null) CreateType.USER
        else if (applicationId != null) CreateType.APPLICATION else null
    val partnershipId = UUID.randomUUID()
    return createQuery<Any> {
            sql(
                """
        WITH new_fridge_partner AS (
            INSERT INTO fridge_partner (partnership_id, indx, other_indx, person_id, start_date, end_date, conflict, created_by, created_at, created_from_application, create_type)
            VALUES
                (${bind(partnershipId)}, 1, 2, ${bind(personId1)}, ${bind(startDate)}, ${bind(endDate)}, ${bind(conflict)}, ${bind(creatorId?.raw)}, ${bind(createDate)}, ${bind(applicationId)}, ${bind(createType)}),
                (${bind(partnershipId)}, 2, 1, ${bind(personId2)}, ${bind(startDate)}, ${bind(endDate)}, ${bind(conflict)}, ${bind(creatorId?.raw)}, ${bind(createDate)}, ${bind(applicationId)}, ${bind(createType)})
            RETURNING *
        )
        SELECT
            fp1.partnership_id,
            fp1.start_date,
            fp1.end_date,
            fp1.conflict,
            ${aliasedPersonColumns("p1")},
            ${aliasedPersonColumns("p2")},
            fp1.create_type,
            fp1.created_at,
            fp1.created_by,
            fp1.modify_type,
            fp1.modified_at,
            fp1.modified_by,
            fp1.created_from_application
        FROM new_fridge_partner fp1
        JOIN new_fridge_partner fp2 ON fp1.partnership_id = fp2.partnership_id AND fp1.indx = 1 AND fp2.indx = 2
        JOIN person p1 ON fp1.person_id = p1.id
        JOIN person p2 ON fp2.person_id = p2.id
        """
            )
        }
        .exactlyOne(toPartnership("p1", "p2"))
}

fun Database.Transaction.updatePartnershipDuration(
    id: PartnershipId,
    startDate: LocalDate,
    endDate: LocalDate?,
    modifyType: ModifyType,
    modifiedAt: HelsinkiDateTime,
    modifiedBy: EvakaUserId?
): Boolean {
    return createQuery<Any> {
            sql(
                """
        UPDATE fridge_partner SET start_date = ${bind(startDate)}, end_date = ${bind(endDate)}, modify_type = ${bind(modifyType)}, modified_at = ${bind(modifiedAt)}, modified_by = ${bind(modifiedBy)}
        WHERE partnership_id = ${bind(id)}
        RETURNING partnership_id
        """
            )
        }
        .mapTo<PartnershipId>()
        .useIterable { it.firstOrNull() } != null
}

fun Database.Transaction.retryPartnership(
    id: PartnershipId,
    modifiedById: EvakaUserId?,
    modificationDate: HelsinkiDateTime
) {

    createUpdate<Any> {
            sql(
                """
        |UPDATE fridge_partner SET conflict = false
        |WHERE partnership_id = ${bind(id)}, modified_by = ${bind(modifiedById)}, modified_at = ${bind(modificationDate)}
        |
    """
                    .trimMargin()
            )
        }
        .execute()
}

fun Database.Transaction.deletePartnership(id: PartnershipId): Boolean {
    // language=SQL
    val sql = "DELETE FROM fridge_partner WHERE partnership_id = :id RETURNING partnership_id"

    return createQuery(sql).bind("id", id).mapTo<PartnershipId>().useIterable {
        it.firstOrNull()
    } != null
}

private val toPartnership: (String, String) -> Row.() -> Partnership =
    { partner1Alias, partner2Alias ->
        {
            Partnership(
                id = column("partnership_id"),
                partners = setOf(toPersonJSON(partner1Alias), toPersonJSON(partner2Alias)),
                startDate = column("start_date"),
                endDate = column("end_date"),
                conflict = column("conflict")
            )
        }
    }

private val toPartner: (String) -> Row.() -> Partner = { tableAlias ->
    {
        Partner(
            partnershipId = column("partnership_id"),
            person = toPersonJSON(tableAlias),
            startDate = column("start_date"),
            endDate = column("end_date"),
            conflict = column("conflict"),
            createType = column("create_type"),
            createdAt = column("created_at"),
            createdBy = column("created_by"),
            createdByName = column("created_by_name"),
            modifyType = column("modify_type"),
            modifiedAt = column("modified_at"),
            modifiedBy = column("modified_by"),
            modifiedByName = column("modified_by_name"),
            createdFromApplication = column("created_from_application"),
            createdFromApplicationType = column("created_from_application_type"),
            createdFromApplicationCreated = column("created_from_application_created")
        )
    }
}
