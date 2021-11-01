// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.setting

import fi.espoo.evaka.ConstList

data class Setting(
    val key: SettingType,
    val value: String
)

@ConstList("settings")
enum class SettingType {
    DECISION_MAKER_NAME,
    DECISION_MAKER_TITLE,
}
