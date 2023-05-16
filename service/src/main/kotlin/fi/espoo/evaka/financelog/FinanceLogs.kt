package fi.espoo.evaka.financelog

import fi.espoo.evaka.shared.ParentshipId
import fi.espoo.evaka.shared.PartnershipId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database

enum class LogOperation {
    INSERT,
    DELETE
}

fun Database.Transaction.logParentship(user: AuthenticatedUser, operation: LogOperation, id: ParentshipId) {
    createUpdate(
        """
            INSERT INTO finance_log (created_by, operation, table_name, person_id, start_date, end_date, data)
            SELECT :userId, :operation, 'fridge_child', head_of_child, start_date, end_date, json_build_object(
                'childId', child_id
            )
            FROM fridge_child
            WHERE id = :id
        """
    )
        .bind("userId", user.evakaUserId)
        .bind("operation", operation)
        .bind("id", id)
        .execute()
}

fun Database.Transaction.logPartnership(user: AuthenticatedUser, operation: LogOperation, id: PartnershipId) {
    createUpdate(
        """
            INSERT INTO finance_log (created_by, operation, table_name, person_id, start_date, end_date, data)
            SELECT :userId, :operation, 'fridge_partner', person_id, start_date, end_date, json_build_object(
                'partner_id', (
                    SELECT fp2.person_id
                    FROM fridge_partner fp2
                    WHERE fp2.partnership_id = fp1.partnership_id AND fp2.indx <> fp1.indx
                )
            )
            FROM fridge_partner fp1
            WHERE partnership_id = :id
        """
    )
        .bind("userId", user.evakaUserId)
        .bind("operation", operation)
        .bind("id", id)
        .execute()
}
