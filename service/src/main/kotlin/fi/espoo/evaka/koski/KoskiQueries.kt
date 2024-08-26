// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.koski

import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.KoskiStudyRightId
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.db.Predicate
import fi.espoo.evaka.shared.db.QuerySql
import java.time.LocalDate

data class KoskiStudyRightKey(
    val childId: ChildId,
    val unitId: DaycareId,
    val type: OpiskeluoikeudenTyyppiKoodi,
)

fun Database.Read.getPendingStudyRights(today: LocalDate): List<KoskiStudyRightKey> {
    val dataVersionCheck = Predicate {
        // intentionally doesn't use bind
        where("$it.data_version IS DISTINCT FROM $KOSKI_DATA_VERSION")
    }
    return createQuery {
            sql(
                """
SELECT kasr.child_id, kasr.unit_id, 'PRESCHOOL'::koski_study_right_type AS type
FROM koski_active_preschool_study_right(${bind(today)}) kasr
LEFT JOIN koski_study_right ksr
ON (kasr.child_id, kasr.unit_id, 'PRESCHOOL') = (ksr.child_id, ksr.unit_id, ksr.type)
WHERE (
    ksr.preschool_input_data IS DISTINCT FROM kasr.input_data OR
    ${predicate(dataVersionCheck.forTable("ksr"))}
)

UNION

SELECT kasr.child_id, kasr.unit_id, 'PREPARATORY'::koski_study_right_type AS type
FROM koski_active_preparatory_study_right(${bind(today)}) kasr
LEFT JOIN koski_study_right ksr
ON (kasr.child_id, kasr.unit_id, 'PREPARATORY') = (ksr.child_id, ksr.unit_id, ksr.type)
WHERE (
    ksr.preparatory_input_data IS DISTINCT FROM kasr.input_data OR
    ${predicate(dataVersionCheck.forTable("ksr"))}
)

UNION

SELECT kvsr.child_id, kvsr.unit_id, kvsr.type
FROM koski_voided_study_right(${bind(today)}) kvsr
WHERE kvsr.void_date IS NULL
"""
            )
        }
        .toList<KoskiStudyRightKey>()
}

private fun Database.Transaction.refreshStudyRight(
    key: KoskiStudyRightKey,
    today: LocalDate,
): Pair<KoskiStudyRightId, Boolean> {
    val studyRightQuery = QuerySql {
        when (key.type) {
            OpiskeluoikeudenTyyppiKoodi.PRESCHOOL ->
                sql(
                    """
SELECT
    child_id, unit_id, type,
    input_data AS preschool_input_data, NULL::koski_preparatory_input_data AS preparatory_input_data
FROM koski_active_preschool_study_right(${bind(today)}) kasr
"""
                )
            OpiskeluoikeudenTyyppiKoodi.PREPARATORY ->
                sql(
                    """
SELECT
    child_id, unit_id, type,
    NULL::koski_preschool_input_data AS preschool_input_data, input_data AS preparatory_input_data
FROM koski_active_preparatory_study_right(${bind(today)}) kasr
"""
                )
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
): KoskiData? {
    val (id, voided) = refreshStudyRight(key, today)
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
            OpiskeluoikeudenTyyppiKoodi.PRESCHOOL ->
                createQuery {
                        sql(
                            """
            SELECT
                kasr.child_id, kasr.unit_id, (kasr.input_data).*,
                ksr.id AS study_right_id, ksr.study_right_oid,
                d.unit_language, d.provider_type, d.approver_name,
                pr.ssn, pr.oph_person_oid, pr.first_name, pr.last_name
            FROM koski_study_right ksr
            JOIN koski_active_preschool_study_right(${bind(today)}) kasr
            USING (child_id, unit_id, type)
            JOIN koski_unit d ON ksr.unit_id = d.id
            JOIN koski_child pr ON ksr.child_id = pr.id
            WHERE ksr.id = ${bind(id)}
                    """
                        )
                    }
                    .exactlyOneOrNull<KoskiActivePreschoolDataRaw>()
            OpiskeluoikeudenTyyppiKoodi.PREPARATORY ->
                createQuery {
                        sql(
                            """
            SELECT
                kasr.child_id, kasr.unit_id, (kasr.input_data).*,
                ksr.id AS study_right_id, ksr.study_right_oid,
                d.unit_language, d.provider_type, d.approver_name,
                pr.ssn, pr.oph_person_oid, pr.first_name, pr.last_name,
                (
                    SELECT coalesce(array_agg(date ORDER BY date), '{}')
                    FROM holiday h
                    WHERE between_start_and_end(range_merge((kasr.input_data).placements), date)
                ) AS holidays
            FROM koski_study_right ksr
            JOIN koski_active_preparatory_study_right(${bind(today)}) kasr
            USING (child_id, unit_id, type)
            JOIN koski_unit d ON ksr.unit_id = d.id
            JOIN koski_child pr ON ksr.child_id = pr.id
            WHERE ksr.id = ${bind(id)}
                    """
                        )
                    }
                    .exactlyOneOrNull<KoskiActivePreparatoryDataRaw>()
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

fun Database.Transaction.finishKoskiUpload(response: KoskiUploadResponse) =
    createUpdate {
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
