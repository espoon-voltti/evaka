package fi.espoo.evaka.setting

import fi.espoo.evaka.shared.db.Database
import org.jdbi.v3.core.kotlin.mapTo

fun Database.Read.getSettings(): Map<SettingType, String> {
    // language=SQL
    val sql = "SELECT key, value FROM setting"

    return createQuery(sql).mapTo<Setting>().associateBy({ it.key }, { it.value })
}

fun Database.Transaction.setSettings(settings: Map<SettingType, String>) {
    // language=SQL
    val deleteSql = "DELETE FROM setting WHERE key != ALL(:keys::setting_type[])"

    createUpdate(deleteSql)
        .bind("keys", settings.keys.toTypedArray())
        .execute()

    // language=SQL
    val insertSql = """
        INSERT INTO setting (key, value) VALUES (:key, :value)
        ON CONFLICT (key) DO UPDATE SET value = EXCLUDED.value WHERE setting.value != EXCLUDED.value
    """.trimIndent()

    val batch = prepareBatch(insertSql)
    settings.forEach { (key, value) ->
        batch.bind("key", key)
        batch.bind("value", value)
        batch.add()
    }
    batch.execute()
}
