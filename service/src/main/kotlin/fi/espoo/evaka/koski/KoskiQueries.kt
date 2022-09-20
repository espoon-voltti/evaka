// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.koski

import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.KoskiStudyRightId
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.db.mapColumn
import java.time.LocalDate

data class KoskiStudyRightKey(
    val childId: ChildId,
    val unitId: DaycareId,
    val type: OpiskeluoikeudenTyyppiKoodi
)

fun Database.Read.getPendingStudyRights(
    today: LocalDate,
    params: KoskiSearchParams = KoskiSearchParams()
): List<KoskiStudyRightKey> =
    createQuery(
            // language=SQL
            """
SELECT kasr.child_id, kasr.unit_id, kasr.type
FROM koski_active_study_right(:today) kasr
LEFT JOIN koski_study_right ksr
ON (kasr.child_id, kasr.unit_id, kasr.type) = (ksr.child_id, ksr.unit_id, ksr.type)
WHERE to_jsonb(kasr) IS DISTINCT FROM ksr.input_data
AND (:personIds = '{}' OR kasr.child_id = ANY(:personIds))
AND (:daycareIds = '{}' OR kasr.unit_id = ANY(:daycareIds))

UNION

SELECT kvsr.child_id, kvsr.unit_id, kvsr.type
FROM koski_voided_study_right(:today) kvsr
WHERE kvsr.void_date IS NULL
AND (:personIds = '{}' OR kvsr.child_id = ANY(:personIds))
AND (:daycareIds = '{}' OR kvsr.unit_id = ANY(:daycareIds))
"""
        )
        .bind("personIds", params.personIds)
        .bind("daycareIds", params.daycareIds)
        .bind("today", today)
        .mapTo<KoskiStudyRightKey>()
        .list()

fun Database.Transaction.beginKoskiUpload(
    sourceSystem: String,
    ophOrganizationOid: String,
    ophMunicipalityCode: String,
    key: KoskiStudyRightKey,
    today: LocalDate
) =
    createQuery(
            // language=SQL
            """
INSERT INTO koski_study_right (child_id, unit_id, type, void_date, input_data, payload, version)
SELECT
    child_id, unit_id, type,
    CASE WHEN kvsr.child_id IS NOT NULL THEN :today END AS void_date,
    coalesce(to_jsonb(kasr), to_jsonb(kvsr)), '{}', 0
FROM (
    SELECT :childId AS child_id, :unitId AS unit_id, :type::koski_study_right_type AS type
) params
LEFT JOIN koski_active_study_right(:today) kasr
USING (child_id, unit_id, type)
LEFT JOIN koski_voided_study_right(:today) kvsr
USING (child_id, unit_id, type)
WHERE kvsr.void_date IS NULL

ON CONFLICT (child_id, unit_id, type)
DO UPDATE SET
    void_date = excluded.void_date,
    input_data = excluded.input_data,
    study_right_oid = CASE WHEN koski_study_right.void_date IS NULL THEN koski_study_right.study_right_oid END
RETURNING id, void_date IS NOT NULL AS voided
"""
        )
        .bindKotlin(key)
        .bind("today", today)
        .map { row ->
            Pair(row.mapColumn<KoskiStudyRightId>("id"), row.mapColumn<Boolean>("voided"))
        }
        .single()
        .let { (id, voided) ->
            if (voided) {
                createQuery(
                        // language=SQL
                        """
            SELECT
                kvsr.*,
                ksr.id AS study_right_id,
                ksr.study_right_oid,
                d.language AS daycare_language,
                d.provider_type AS daycare_provider_type,
                nullif(pr.social_security_number, '') ssn,
                nullif(pr.oph_person_oid, '') person_oid,
                pr.first_name,
                pr.last_name
            FROM koski_study_right ksr
            JOIN koski_voided_study_right(:today) kvsr
            ON (kvsr.child_id, kvsr.unit_id, kvsr.type) = (ksr.child_id, ksr.unit_id, ksr.type)
            JOIN daycare d ON ksr.unit_id = d.id
            JOIN person pr ON ksr.child_id = pr.id
            WHERE ksr.id = :id
                    """
                    )
                    .bind("id", id)
                    .bind("today", today)
                    .mapTo<KoskiVoidedDataRaw>()
                    .singleOrNull()
                    ?.toKoskiData(sourceSystem, ophOrganizationOid)
            } else {
                createQuery(
                        // language=SQL
                        """
            SELECT
                kasr.*,
                ksr.id AS study_right_id,
                ksr.study_right_oid,
                d.language AS daycare_language,
                d.provider_type AS daycare_provider_type,
                um.name AS approver_name,
                nullif(pr.social_security_number, '') ssn,
                nullif(pr.oph_person_oid, '') person_oid,
                pr.first_name,
                pr.last_name,
                holidays
            FROM koski_study_right ksr
            JOIN koski_active_study_right(:today) kasr
            ON (kasr.child_id, kasr.unit_id, kasr.type) = (ksr.child_id, ksr.unit_id, ksr.type)
            JOIN daycare d ON ksr.unit_id = d.id
            JOIN unit_manager um ON d.unit_manager_id = um.id
            JOIN person pr ON ksr.child_id = pr.id
            LEFT JOIN LATERAL (
                SELECT array_agg(date ORDER BY date) AS holidays
                FROM holiday h
                WHERE between_start_and_end(kasr.full_range, date)
            ) h ON ksr.type = 'PREPARATORY'
            WHERE ksr.id = :id
                    """
                    )
                    .bind("id", id)
                    .bind("today", today)
                    .mapTo<KoskiActiveDataRaw>()
                    .singleOrNull()
                    ?.toKoskiData(sourceSystem, ophOrganizationOid, ophMunicipalityCode, today)
            }
        }

data class KoskiUploadResponse(
    val id: KoskiStudyRightId,
    val studyRightOid: String,
    val personOid: String,
    val version: Int,
    val payload: String
)

fun Database.Read.isPayloadChanged(key: KoskiStudyRightKey, payload: String): Boolean =
    createQuery(
            // language=SQL
            """
SELECT ksr.payload != :payload::jsonb
FROM (
    SELECT :childId AS child_id, :unitId AS unit_id, :type::koski_study_right_type AS type
) params
LEFT JOIN koski_study_right ksr
USING (child_id, unit_id, type)
"""
        )
        .bindKotlin(key)
        .bind("payload", payload)
        .mapTo<Boolean>()
        .single()

fun Database.Transaction.finishKoskiUpload(response: KoskiUploadResponse) =
    createUpdate(
            // language=SQL
            """
UPDATE koski_study_right
SET study_right_oid = :studyRightOid, person_oid = :personOid, version = :version, payload = :payload::jsonb
WHERE id = :id
"""
        )
        .bindKotlin(response)
        .execute()
