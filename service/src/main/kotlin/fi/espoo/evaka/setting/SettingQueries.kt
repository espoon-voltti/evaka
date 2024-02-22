// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.setting

import fi.espoo.evaka.shared.db.Database

fun Database.Read.getSettings(): Map<SettingType, String> {
    // language=SQL
    val sql = "SELECT key, value FROM setting"

    @Suppress("DEPRECATION") return createQuery(sql).toMap { columnPair("key", "value") }
}

fun Database.Transaction.setSettings(settings: Map<SettingType, String>) {
    // language=SQL
    val deleteSql = "DELETE FROM setting WHERE key != ALL(:keys::setting_type[])"

    @Suppress("DEPRECATION") createUpdate(deleteSql).bind("keys", settings.keys).execute()

    // language=SQL
    val insertSql =
        """
        INSERT INTO setting (key, value) VALUES (:key, :value)
        ON CONFLICT (key) DO UPDATE SET value = EXCLUDED.value WHERE setting.value != EXCLUDED.value
    """
            .trimIndent()

    val batch = prepareBatch(insertSql)
    settings.forEach { (key, value) ->
        batch.bind("key", key)
        batch.bind("value", value)
        batch.add()
    }
    batch.execute()
}
