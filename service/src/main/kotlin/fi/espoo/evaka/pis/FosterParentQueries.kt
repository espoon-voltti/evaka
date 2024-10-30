// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.pis

import fi.espoo.evaka.pis.controllers.CreateFosterParentRelationshipBody
import fi.espoo.evaka.pis.controllers.FosterParentRelationship
import fi.espoo.evaka.shared.FosterParentId
import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.BadRequest
import fi.espoo.evaka.shared.domain.DateRange
import fi.espoo.evaka.shared.domain.HelsinkiDateTime

fun Database.Read.getFosterChildren(parentId: PersonId) =
    getFosterParentRelationships(parentId = parentId)

fun Database.Read.getFosterParents(childId: PersonId) =
    getFosterParentRelationships(childId = childId)

private fun Database.Read.getFosterParentRelationships(
    parentId: PersonId? = null,
    childId: PersonId? = null,
): List<FosterParentRelationship> {
    if (parentId == null && childId == null) error("Either parentId or childId must be provided")

    return createQuery {
            sql(
                """
SELECT
    fp.id AS relationship_id,
    fp.valid_during,
    c.id AS child_id,
    c.first_name AS child_first_name,
    c.last_name AS child_last_name,
    c.date_of_birth AS child_date_of_birth,
    c.date_of_death AS child_date_of_death,
    c.social_security_number AS child_social_security_number,
    c.street_address AS child_street_address,
    c.restricted_details_enabled AS child_restricted_details_enabled,
    p.id AS parent_id,
    p.first_name AS parent_first_name,
    p.last_name AS parent_last_name,
    p.date_of_birth AS parent_date_of_birth,
    p.date_of_death AS parent_date_of_death,
    p.social_security_number AS parent_social_security_number,
    p.street_address AS parent_street_address,
    p.restricted_details_enabled AS parent_restricted_details_enabled,
    fp.create_source AS creation_modification_metadata_create_source,
    fp.created_at AS creation_modification_metadata_created_at,
    fp.created_by AS creation_modification_metadata_created_by,
    (SELECT name FROM evaka_user WHERE id = fp.created_by) AS creation_modification_metadata_created_by_name,
    fp.modify_source AS creation_modification_metadata_modify_source,
    fp.modified_at AS creation_modification_metadata_modified_at,
    fp.modified_by AS creation_modification_metadata_modified_by,
    (SELECT name FROM evaka_user WHERE id = fp.modified_by) AS creation_modification_metadata_modified_by_name,
    fp.created_from_application AS creation_modification_metadata_created_from_application,
    a.type AS creation_modification_metadata_created_from_application_type,
    a.created AS creation_modification_metadata_created_from_application_created
FROM foster_parent fp
JOIN person c ON fp.child_id = c.id
JOIN person p ON fp.parent_id = p.id
LEFT JOIN application a ON fp.created_from_application = a.id
WHERE fp.parent_id = ${bind(parentId)} OR fp.child_id = ${bind(childId)}
"""
            )
        }
        .toList<FosterParentRelationship>()
}

fun Database.Transaction.createFosterParentRelationship(
    data: CreateFosterParentRelationshipBody,
    creator: Creator,
    createDate: HelsinkiDateTime,
): FosterParentId {
    val (creatorId, applicationId) =
        when (creator) {
            is Creator.User -> Pair(creator.id.raw, null)
            is Creator.Application -> Pair(null, creator.id)
            is Creator.DVV -> Pair(null, null)
        }

    return createUpdate {
            sql(
                """
INSERT INTO foster_parent (child_id, parent_id, valid_during, created_by, created_at, created_from_application, create_source, modified_by, modified_at)
VALUES
    (${bind(data.childId)}, ${bind(data.parentId)}, ${bind(data.validDuring)}, ${bind(creatorId)}, ${bind(createDate)}, ${bind(applicationId)}, ${bind(creator.source)}, ${bind(creatorId)}, ${bind(createDate)})
RETURNING id
"""
            )
        }
        .executeAndReturnGeneratedKeys()
        .exactlyOne<FosterParentId>()
}

fun Database.Transaction.updateFosterParentRelationshipValidity(
    id: FosterParentId,
    validDuring: DateRange,
    modifiedAt: HelsinkiDateTime,
    modifier: Modifier,
) {
    val userId =
        when (modifier) {
            is Modifier.User -> modifier.id.raw
            is Modifier.DVV -> null
        }

    createUpdate {
            sql(
                """
UPDATE foster_parent SET
    valid_during = ${bind(validDuring)},
    modified_by = ${bind(userId)},
    modified_at = ${bind(modifiedAt)},
    modify_source = ${bind(modifier.source)}
WHERE id = ${bind(id)}
"""
            )
        }
        .execute()
        .also {
            if (it != 1)
                throw BadRequest("Could not update validity of foster_parent row with id $id")
        }
}

fun Database.Transaction.deleteFosterParentRelationship(id: FosterParentId) =
    createUpdate { sql("DELETE FROM foster_parent WHERE id = ${bind(id)}") }
        .execute()
        .also { if (it != 1) throw BadRequest("Could not delete foster_parent row with id $id") }
