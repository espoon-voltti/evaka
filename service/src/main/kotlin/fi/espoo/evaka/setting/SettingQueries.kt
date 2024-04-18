// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.setting

import fi.espoo.evaka.shared.db.Database

fun Database.Read.getSettings(): Map<SettingType, String> {
    return createQuery { sql("SELECT key, value FROM setting") }
        .toMap { columnPair("key", "value") }
}

fun Database.Transaction.setSettings(settings: Map<SettingType, String>) {
    createUpdate {
            sql("DELETE FROM setting WHERE key != ALL(${bind(settings.keys)}::setting_type[])")
        }
        .execute()

    executeBatch(settings.entries) {
        sql(
            """
INSERT INTO setting (key, value) VALUES (${bind { it.key }}, ${bind { it.value }})
ON CONFLICT (key) DO UPDATE SET value = EXCLUDED.value WHERE setting.value != EXCLUDED.value
        """
        )
    }
}
