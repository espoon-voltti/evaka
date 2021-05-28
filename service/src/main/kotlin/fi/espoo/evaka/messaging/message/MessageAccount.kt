package fi.espoo.evaka.messaging.message

import fi.espoo.evaka.shared.db.Database
import java.util.UUID

fun deleteOrDeactivateDaycareGroupMessageAccount(tx: Database.Transaction, daycareGroupId: UUID) {
    val accountId = tx.getDaycareGroupMessageAccount(daycareGroupId)
    if (accountId != null && tx.accountHasMessages(accountId)) {
        tx.deactivateDaycareGroupMessageAccount(daycareGroupId)
    } else {
        tx.deleteDaycareGroupMessageAccount(daycareGroupId)
    }
}
