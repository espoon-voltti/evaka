package fi.espoo.evaka.messaging.message

import fi.espoo.evaka.shared.db.Database
import java.util.UUID

fun deleteOrDeactivateDaycareGroupMessageAccount(tx: Database.Transaction, daycareGroupId: UUID) {
    val account = tx.getMessageAccountForDaycareGroup(daycareGroupId)
    if (account != null && tx.accountHasMessages(account.id)) {
        tx.deactivateDaycareGroupMessageAccount(daycareGroupId)
    } else {
        tx.deleteDaycareGroupMessageAccount(daycareGroupId)
    }
}
