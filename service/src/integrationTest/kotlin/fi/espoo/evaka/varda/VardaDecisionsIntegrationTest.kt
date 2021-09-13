// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.varda

import fi.espoo.evaka.defaultMunicipalOrganizerOid
import fi.espoo.evaka.shared.db.Database
import java.time.Instant
import java.util.UUID

internal fun insertVardaChild(db: Database.Connection, childId: UUID, createdAt: Instant = Instant.now(), ophOrganizationOid: String = defaultMunicipalOrganizerOid) = db.transaction {
    it.createUpdate(
        """
INSERT INTO varda_child
    (id, person_id, varda_person_id, varda_person_oid, varda_child_id, created_at, modified_at, uploaded_at, oph_organizer_oid)
VALUES
    (:id, :personId, :vardaPersonId, :vardaPersonOid, :vardaChildId, :createdAt, :modifiedAt, :uploadedAt, :ophOrganizerOid)
"""
    )
        .bind("id", UUID.randomUUID())
        .bind("personId", childId)
        .bind("vardaPersonId", 123L)
        .bind("vardaPersonOid", "123")
        .bind("vardaChildId", 123L)
        .bind("createdAt", createdAt)
        .bind("modifiedAt", createdAt)
        .bind("uploadedAt", createdAt)
        .bind("ophOrganizerOid", ophOrganizationOid)
        .execute()
}
