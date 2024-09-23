// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.serviceneed.application

import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.ServiceApplicationId
import fi.espoo.evaka.shared.ServiceNeedOptionId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.db.Predicate
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.security.actionrule.forTable
import java.time.LocalDate

fun Database.Read.getServiceApplications(where: Predicate): List<ServiceApplication> =
    createQuery {
            sql(
                """
        SELECT 
            sa.id,
            sa.sent_at,
            sa.person_id,
            pe.first_name || ' ' || pe.last_name AS person_name,
            sa.child_id,
            ch.first_name || ' ' || ch.last_name AS child_name,
            sa.start_date,
            sa.service_need_option_id,
            sno.name_fi AS service_need_option_name_fi,
            sno.name_sv AS service_need_option_name_sv,
            sno.name_en AS service_need_option_name_en,
            sno.valid_placement_type AS service_need_option_valid_placement_type,
            sno.part_week AS service_need_option_part_week,
            daterange(sno.valid_from, sno.valid_to, '[]') AS service_need_option_validity,
            sa.additional_info,
            sa.decision_status,
            sa.decided_by AS decision_decided_by,
            CASE 
                WHEN sa.decided_by IS NOT NULL THEN dm.first_name || ' ' || dm.last_name
            END AS decision_decided_by_name,
            sa.decided_at AS decision_decided_at,
            sa.rejected_reason AS decision_rejected_reason
        FROM service_application sa
        JOIN person pe ON pe.id = sa.person_id
        JOIN person ch ON ch.id = sa.child_id
        JOIN service_need_option sno ON sno.id = sa.service_need_option_id
        LEFT JOIN employee dm ON dm.id = sa.decided_by
        WHERE ${predicate(where.forTable("sa"))}
    """
            )
        }
        .toList()

fun Database.Read.getServiceApplication(id: ServiceApplicationId): ServiceApplication? =
    getServiceApplications(Predicate { where("$it.id = ${bind(id)}") }).firstOrNull()

fun Database.Read.getServiceApplicationsOfChild(childId: ChildId): List<ServiceApplication> =
    getServiceApplications(Predicate { where("$it.child_id = ${bind(childId)}") })

fun Database.Transaction.insertServiceApplication(
    sentAt: HelsinkiDateTime,
    personId: PersonId,
    childId: ChildId,
    startDate: LocalDate,
    serviceNeedOptionId: ServiceNeedOptionId,
    additionalInfo: String,
): ServiceApplicationId =
    createUpdate {
            sql(
                """
    INSERT INTO service_application (sent_at, person_id, child_id, start_date, service_need_option_id, additional_info, decision_status, decided_by, decided_at, rejected_reason)
    VALUES (${bind(sentAt)}, ${bind(personId)}, ${bind(childId)}, ${bind(startDate)}, ${bind(serviceNeedOptionId)}, ${bind(additionalInfo)}, NULL, NULL, NULL, NULL)
    RETURNING id
"""
            )
        }
        .executeAndReturnGeneratedKeys()
        .exactlyOne()

fun Database.Transaction.deleteUndecidedServiceApplication(id: ServiceApplicationId) = execute {
    sql(
        """
    DELETE FROM service_application
    WHERE id = ${bind(id)} AND decision_status IS NULL 
"""
    )
}

fun Database.Transaction.setServiceApplicationAccepted(
    id: ServiceApplicationId,
    now: HelsinkiDateTime,
    user: AuthenticatedUser.Employee,
) =
    createUpdate {
            sql(
                """
    UPDATE service_application
    SET decision_status = 'ACCEPTED', decided_by = ${bind(user.id)}, decided_at = ${bind(now)}
    WHERE id = ${bind(id)} AND decision_status IS NULL
"""
            )
        }
        .updateExactlyOne()

fun Database.Transaction.setServiceApplicationRejected(
    id: ServiceApplicationId,
    now: HelsinkiDateTime,
    user: AuthenticatedUser.Employee,
    rejectedReason: String,
) =
    createUpdate {
            sql(
                """
    UPDATE service_application
    SET decision_status = 'REJECTED', decided_by = ${bind(user.id)}, decided_at = ${bind(now)}, rejected_reason = ${bind(rejectedReason)}
    WHERE id = ${bind(id)} AND decision_status IS NULL
"""
            )
        }
        .updateExactlyOne()
