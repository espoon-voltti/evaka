// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.reports

import fi.espoo.evaka.Audit
import fi.espoo.evaka.pis.getTransferablePersonReferences
import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.EvakaClock
import fi.espoo.evaka.shared.security.AccessControl
import fi.espoo.evaka.shared.security.Action
import java.time.LocalDate
import org.jdbi.v3.json.Json
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
class DuplicatePeopleReportController(private val accessControl: AccessControl) {
    @GetMapping("/reports/duplicate-people")
    fun getDuplicatePeopleReport(
        db: Database,
        user: AuthenticatedUser,
        clock: EvakaClock,
        @RequestParam showIntentionalDuplicates: Boolean?,
    ): List<DuplicatePeopleReportRow> {
        return db.connect { dbc ->
                dbc.read {
                    accessControl.requirePermissionFor(
                        it,
                        user,
                        clock,
                        Action.Global.READ_DUPLICATE_PEOPLE_REPORT
                    )
                    it.setStatementTimeout(REPORT_STATEMENT_TIMEOUT)
                    it.getDuplicatePeople(showIntentionalDuplicates ?: false)
                }
            }
            .also { Audit.DuplicatePeopleReportRead.log(meta = mapOf("count" to it.size)) }
    }
}

private fun Database.Read.getDuplicatePeople(
    showIntentionalDuplicates: Boolean
): List<DuplicatePeopleReportRow> {
    val personReferences = getTransferablePersonReferences()
    val intentionalDuplicateFilterClause =
        if (showIntentionalDuplicates) "" else "AND p.duplicate_of IS NULL"
    return createQuery {
            sql(
                """
        WITH people AS (
            SELECT
                p.id,
                p.social_security_number,
                concat(
                    lower(trim(unaccent(last_name))),
                    ',',
                    split_part(trim(lower(unaccent(first_name))), ' ', 1),
                    ',',
                    date_of_birth
                ) AS key
            FROM person p
            WHERE p.first_name IS NOT NULL AND p.last_name IS NOT NULL 
                AND p.first_name <> '' AND p.last_name <> '' AND lower(last_name) <> 'testaaja'
                $intentionalDuplicateFilterClause
        ), duplicate_keys AS (
            SELECT key
            FROM people p
            GROUP BY key
            HAVING count(id) > 1 AND array_position(array_agg(social_security_number), NULL) IS NOT NULL
        ), duplicate_ids AS (
            SELECT p.key, p.id
            FROM people p
            JOIN duplicate_keys d ON p.key = d.key
        )
        SELECT
            dense_rank() OVER (ORDER BY key) as group_index,
            row_number() OVER (PARTITION BY key ORDER BY social_security_number, p.id) AS duplicate_number,
            p.id,
            p.first_name,
            p.last_name,
            p.social_security_number,
            p.date_of_birth,
            p.street_address,
        
            jsonb_build_array(
                ${personReferences.joinToString(separator = ", ") { (table, column) ->
                    """
                    json_build_object(
                        'table', '$table',
                        'column', '$column',
                        'count', (SELECT count(*) FROM $table WHERE $column = p.id)
                    )
                    """
                }},
                json_build_object(
                    'table', 'message',
                    'column', 'sender_id',
                    'count', (SELECT count(*) FROM message WHERE sender_id = (SELECT id FROM message_account WHERE person_id = p.id))
                ),
                json_build_object(
                    'table', 'message_content',
                    'column', 'author_id',
                    'count', (SELECT count(*) FROM message_content WHERE author_id = (SELECT id FROM message_account WHERE person_id = p.id))
                ),
                json_build_object(
                    'table', 'message_recipients',
                    'column', 'recipient_id',
                    'count', (SELECT count(*) FROM message_recipients WHERE recipient_id = (SELECT id FROM message_account WHERE person_id = p.id))
                ),
                json_build_object(
                    'table', 'message_draft',
                    'column', 'account_id',
                    'count', (SELECT count(*) FROM message_draft WHERE account_id = (SELECT id FROM message_account WHERE person_id = p.id))
                )
            ) AS reference_counts
        FROM duplicate_ids dups
        JOIN person p ON p.id = dups.id
        ORDER BY key, social_security_number, p.id
        """
            )
        }
        .toList<DuplicatePeopleReportRow>()
}

data class ReferenceCount(val table: String, val column: String, val count: Int)

data class DuplicatePeopleReportRow(
    val groupIndex: Int,
    val duplicateNumber: Int,
    val id: PersonId,
    val firstName: String?,
    val lastName: String?,
    val socialSecurityNumber: String?,
    val dateOfBirth: LocalDate,
    val streetAddress: String?,
    @Json val referenceCounts: List<ReferenceCount>
)
