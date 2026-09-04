// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.koski

import evaka.core.shared.ChildId
import evaka.core.shared.DaycareId
import evaka.core.shared.KoskiStudyRightId
import evaka.core.shared.db.Database
import evaka.core.shared.db.Predicate
import evaka.core.shared.db.QuerySql
import evaka.core.shared.domain.HelsinkiDateTime
import java.time.LocalDate

/** Child-keyed tables feeding the payload in `R__koski_views.sql`. Keep in sync by hand. */
val KOSKI_INPUT_TABLES =
    setOf("placement", "person", "preschool_assistance", "other_assistance_measure", "absence")

data class KoskiStudyRightKey(
    val childId: ChildId,
    val unitId: DaycareId,
    val type: OpiskeluoikeudenTyyppiKoodi,
)

/**
 * Koski is the system of record for the education history, so once retention has removed rows the
 * payload is built from, we must stop reconciling: rebuilding it from what is left would amend the
 * loss into the national register. Nothing lifts this again.
 */
private val koskiSyncActive = Predicate { where("$it.koski_data_first_removed_at IS NULL") }

private fun Database.Read.isKoskiSyncActive(childId: ChildId): Boolean = createQuery {
    sql(
        """
SELECT ${predicate(koskiSyncActive.forTable("child"))}
FROM child
WHERE id = ${bind(childId)}
"""
    )
}
    .exactlyOne()

fun Database.Read.getPendingStudyRights(
    today: LocalDate,
    syncRangeStart: LocalDate?,
): List<KoskiStudyRightKey> {
    val dataVersionCheck = Predicate {
        // intentionally doesn't use bind
        where("$it.data_version IS DISTINCT FROM $KOSKI_DATA_VERSION")
    }
    return createQuery {
        sql(
            """
SELECT kasr.child_id, kasr.unit_id, 'PRESCHOOL'::koski_study_right_type AS type
FROM koski_active_preschool_study_right(${bind(today)}, ${bind(syncRangeStart)}) kasr
JOIN child ch ON ch.id = kasr.child_id
LEFT JOIN koski_study_right ksr
ON (kasr.child_id, kasr.unit_id, 'PRESCHOOL') = (ksr.child_id, ksr.unit_id, ksr.type)
WHERE (
    ksr.preschool_input_data IS DISTINCT FROM kasr.input_data OR
    ${predicate(dataVersionCheck.forTable("ksr"))}
)
AND ${predicate(koskiSyncActive.forTable("ch"))}

UNION

SELECT kasr.child_id, kasr.unit_id, 'PREPARATORY'::koski_study_right_type AS type
FROM koski_active_preparatory_study_right(${bind(today)}, ${bind(syncRangeStart)}) kasr
JOIN child ch ON ch.id = kasr.child_id
LEFT JOIN koski_study_right ksr
ON (kasr.child_id, kasr.unit_id, 'PREPARATORY') = (ksr.child_id, ksr.unit_id, ksr.type)
WHERE (
    ksr.preparatory_input_data IS DISTINCT FROM kasr.input_data OR
    ${predicate(dataVersionCheck.forTable("ksr"))}
)
AND ${predicate(koskiSyncActive.forTable("ch"))}

UNION

SELECT kvsr.child_id, kvsr.unit_id, kvsr.type
FROM koski_voided_study_right(${bind(today)}) kvsr
JOIN child ch ON ch.id = kvsr.child_id
WHERE kvsr.void_date IS NULL
AND ${predicate(koskiSyncActive.forTable("ch"))}
"""
        )
    }
        .toList<KoskiStudyRightKey>()
}

private fun Database.Transaction.refreshStudyRight(
    key: KoskiStudyRightKey,
    today: LocalDate,
    syncRangeStart: LocalDate?,
): Pair<KoskiStudyRightId, Boolean> {
    val studyRightQuery = QuerySql {
        when (key.type) {
            OpiskeluoikeudenTyyppiKoodi.PRESCHOOL -> {
                sql(
                    """
SELECT
    child_id, unit_id, type,
    input_data AS preschool_input_data, NULL::koski_preparatory_input_data AS preparatory_input_data
FROM koski_active_preschool_study_right(${bind(today)}, ${bind(syncRangeStart)}) kasr
"""
                )
            }

            OpiskeluoikeudenTyyppiKoodi.PREPARATORY -> {
                sql(
                    """
SELECT
    child_id, unit_id, type,
    NULL::koski_preschool_input_data AS preschool_input_data, input_data AS preparatory_input_data
FROM koski_active_preparatory_study_right(${bind(today)}, ${bind(syncRangeStart)}) kasr
"""
                )
            }
        }
    }
    return createQuery {
        sql(
            """
INSERT INTO koski_study_right (child_id, unit_id, type, void_date, preschool_input_data, preparatory_input_data, data_version, payload, version)
SELECT
    child_id, unit_id, type,
    CASE WHEN kvsr.child_id IS NOT NULL THEN ${bind(today)} END AS void_date,
    preschool_input_data, preparatory_input_data,
    ${bind(KOSKI_DATA_VERSION)} AS data_version, '{}' AS payload, 0 AS version
FROM (${subquery(studyRightQuery)}) study_right
FULL JOIN koski_voided_study_right(${bind(today)}) kvsr
USING (child_id, unit_id, type)
WHERE (child_id, unit_id, type) = (${bind(key.childId)}, ${bind(key.unitId)}, ${bind(key.type)})
AND kvsr.void_date IS NULL

ON CONFLICT (child_id, unit_id, type)
DO UPDATE SET
    void_date = excluded.void_date,
    preschool_input_data = excluded.preschool_input_data,
    preparatory_input_data = excluded.preparatory_input_data,
    data_version = excluded.data_version,
    study_right_oid = CASE WHEN koski_study_right.void_date IS NULL THEN koski_study_right.study_right_oid END
RETURNING id, void_date IS NOT NULL AS voided
"""
        )
    }
        .exactlyOne { columnPair<KoskiStudyRightId, Boolean>("id", "voided") }
}

fun Database.Transaction.beginKoskiUpload(
    sourceSystem: String,
    ophOrganizationOid: String,
    ophMunicipalityCode: String,
    key: KoskiStudyRightKey,
    today: LocalDate,
    syncRangeStart: LocalDate?,
): KoskiData? {
    if (!isKoskiSyncActive(key.childId)) return null
    val (id, voided) = refreshStudyRight(key, today, syncRangeStart)
    return if (voided) {
        createQuery {
                sql(
                    """
            SELECT
                kvsr.*,
                ksr.id AS study_right_id, ksr.study_right_oid,
                d.unit_language, d.provider_type, d.approver_name,
                pr.ssn, pr.oph_person_oid, pr.first_name, pr.last_name
            FROM koski_study_right ksr
            JOIN koski_voided_study_right(${bind(today)}) kvsr
            ON (kvsr.child_id, kvsr.unit_id, kvsr.type) = (ksr.child_id, ksr.unit_id, ksr.type)
            JOIN koski_unit d ON ksr.unit_id = d.id
            JOIN koski_child pr ON ksr.child_id = pr.id
            WHERE ksr.id = ${bind(id)}
                    """
                )
            }
            .exactlyOneOrNull<KoskiVoidedDataRaw>()
            ?.toKoskiData(sourceSystem, ophOrganizationOid)
    } else {
        when (key.type) {
            OpiskeluoikeudenTyyppiKoodi.PRESCHOOL -> {
                createQuery {
                    sql(
                        """
            SELECT
                kasr.child_id, kasr.unit_id, (kasr.input_data).*,
                ksr.id AS study_right_id, ksr.study_right_oid,
                d.unit_language, d.provider_type, d.approver_name,
                pr.ssn, pr.oph_person_oid, pr.first_name, pr.last_name
            FROM koski_study_right ksr
            JOIN koski_active_preschool_study_right(${bind(today)}, ${bind(syncRangeStart)}) kasr
            USING (child_id, unit_id, type)
            JOIN koski_unit d ON ksr.unit_id = d.id
            JOIN koski_child pr ON ksr.child_id = pr.id
            WHERE ksr.id = ${bind(id)}
                    """
                    )
                }
                    .exactlyOneOrNull<KoskiActivePreschoolDataRaw>()
            }

            OpiskeluoikeudenTyyppiKoodi.PREPARATORY -> {
                createQuery {
                    sql(
                        """
            SELECT
                kasr.child_id, kasr.unit_id, (kasr.input_data).*,
                ksr.id AS study_right_id, ksr.study_right_oid,
                d.unit_language, d.provider_type, d.approver_name,
                pr.ssn, pr.oph_person_oid, pr.first_name, pr.last_name
            FROM koski_study_right ksr
            JOIN koski_active_preparatory_study_right(${bind(today)}, ${bind(syncRangeStart)}) kasr
            USING (child_id, unit_id, type)
            JOIN koski_unit d ON ksr.unit_id = d.id
            JOIN koski_child pr ON ksr.child_id = pr.id
            WHERE ksr.id = ${bind(id)}
                    """
                    )
                }
                    .exactlyOneOrNull<KoskiActivePreparatoryDataRaw>()
            }
        }?.toKoskiData(sourceSystem, ophOrganizationOid, ophMunicipalityCode, today)
    }
}

data class KoskiUploadResponse(
    val id: KoskiStudyRightId,
    val studyRightOid: String,
    val personOid: String,
    val version: Int,
    val payload: String,
)

fun Database.Read.isPayloadChanged(key: KoskiStudyRightKey, payload: String): Boolean =
    createQuery {
        sql(
            """
SELECT ksr.payload != ${bind(payload)}::jsonb
FROM (
    SELECT ${bind(key.childId)} AS child_id, ${bind(key.unitId)} AS unit_id, ${bind(key.type)} AS type
) params
LEFT JOIN koski_study_right ksr
USING (child_id, unit_id, type)
"""
        )
    }
    .exactlyOne<Boolean>()

fun Database.Transaction.finishKoskiUpload(response: KoskiUploadResponse) = createUpdate {
    sql(
        """
UPDATE koski_study_right
SET study_right_oid = ${bind(response.studyRightOid)}, person_oid = ${bind(response.personOid)},
    version = ${bind(response.version)}, payload = ${bind(response.payload)}::jsonb
WHERE id = ${bind(response.id)}
"""
    )
}
    .execute()

fun Database.Transaction.upsertKoskiUploadError(
    key: KoskiStudyRightKey,
    now: HelsinkiDateTime,
    statusCode: Int,
    error: String,
) = createUpdate {
    sql(
        """
INSERT INTO koski_upload_error (child_id, unit_id, type, error, status_code, errored_at, errored_since)
VALUES (${bind(key.childId)}, ${bind(key.unitId)}, ${bind(key.type)}, ${bind(error)}, ${bind(statusCode)}, ${bind(now)}, ${bind(now)})
ON CONFLICT (child_id, unit_id, type)
DO UPDATE SET error = excluded.error, status_code = excluded.status_code, errored_at = excluded.errored_at
"""
    )
}
    .execute()

fun Database.Transaction.deleteKoskiUploadError(key: KoskiStudyRightKey) = createUpdate {
    sql(
        """
DELETE FROM koski_upload_error
WHERE (child_id, unit_id, type) = (${bind(key.childId)}, ${bind(key.unitId)}, ${bind(key.type)})
"""
    )
}
    .execute()

fun Database.Transaction.deleteObsoleteKoskiUploadErrors(pending: List<KoskiStudyRightKey>) =
    createUpdate {
        sql(
            """
DELETE FROM koski_upload_error e
WHERE NOT EXISTS (
    SELECT FROM unnest(
        ${bind(pending.map { it.childId })},
        ${bind(pending.map { it.unitId })},
        ${bind(pending.map { it.type })}
    ) AS p (child_id, unit_id, type)
    WHERE (p.child_id, p.unit_id, p.type) = (e.child_id, e.unit_id, e.type)
)
"""
        )
    }
    .execute()
